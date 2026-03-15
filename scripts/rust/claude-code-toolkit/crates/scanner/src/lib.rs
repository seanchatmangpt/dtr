//! `cct-scanner` — AST-based Java scanner using tree-sitter + aho-corasick.
//!
//! Architecture:
//! - [`extract_methods`]: tree-sitter-java parses source into an AST; method bodies are
//!   extracted by S-expression query — no brace-counting heuristics.
//! - [`PatternSet`]: aho-corasick handles literal keyword patterns (H_TODO, H_MOCK prefixes)
//!   in a single O(n) pass; regex handles structural patterns (H_STUB_NULL, H_EMPTY, etc.).
//! - [`walk_java_files`]: ignore-based walker that respects `.gitignore` and skips test paths.
//! - [`Scanner`]: composes the above into a `scan_source` / `scan_file` / `scan_files_parallel`
//!   public API, using memmap2 for zero-copy file reads and rayon for parallel scanning.

use aho_corasick::AhoCorasick;
use anyhow::Result;
use memchr::memchr;
use memmap2::Mmap;
use rayon::prelude::*;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::fs::File;
use std::path::{Path, PathBuf};
use std::sync::OnceLock;
use streaming_iterator::StreamingIterator;
use tree_sitter::{Parser, Query, QueryCursor};

// ── Types ──────────────────────────────────────────────────────────────────────

/// A single Java method body extracted from the AST.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MethodBody {
    /// Method name (identifier node text).
    pub name: String,
    /// Full block text including braces.
    pub body: String,
    /// 1-based start line of the block in the source file.
    pub start_line: usize,
    /// 1-based end line of the block in the source file.
    pub end_line: usize,
}

/// A single H-Guard violation found in a method body.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Violation {
    /// Source file path.
    pub path: PathBuf,
    /// Method containing the violation (`"<class>"` for class-level patterns).
    pub method: String,
    /// 1-based line number in the source file.
    pub line: usize,
    /// Pattern name (e.g. `"H_TODO"`, `"H_STUB_NULL"`).
    pub pattern: String,
    /// The matched text fragment.
    pub matched: String,
    /// Remediation hint.
    pub fix: String,
}

/// All violations found in one file.
#[derive(Debug, Serialize, Deserialize)]
pub struct ScanResult {
    pub path: PathBuf,
    pub violations: Vec<Violation>,
}

impl ScanResult {
    pub fn is_clean(&self) -> bool {
        self.violations.is_empty()
    }
}

// ── Extractor ─────────────────────────────────────────────────────────────────

/// tree-sitter S-expression query capturing method names and bodies.
const METHOD_QUERY: &str = "(method_declaration
  name: (identifier) @method_name
  body: (block) @method_body)";

/// Cached tree-sitter parser — reused across multiple files to reduce overhead.
fn get_cached_parser() -> &'static std::sync::Mutex<Parser> {
    static PARSER: OnceLock<std::sync::Mutex<Parser>> = OnceLock::new();
    PARSER.get_or_init(|| {
        let mut parser = Parser::new();
        let language = tree_sitter_java::LANGUAGE;
        parser
            .set_language(&language.into())
            .expect("tree-sitter-java language load");
        std::sync::Mutex::new(parser)
    })
}

/// Cached tree-sitter query for method extraction.
fn get_cached_method_query() -> &'static Query {
    static QUERY: OnceLock<Query> = OnceLock::new();
    QUERY.get_or_init(|| {
        let lang: tree_sitter::Language = tree_sitter_java::LANGUAGE.into();
        Query::new(&lang, METHOD_QUERY).expect("tree-sitter method query")
    })
}

/// Parse `source` with tree-sitter-java and return all method bodies found.
///
/// Returns an empty vec on parse failure (e.g. fragment, not a full compilation unit).
///
/// # Panics
/// Panics if the parser lock is poisoned.
///
/// # Examples
///
/// ```
/// use cct_scanner::extract_methods;
///
/// let java_code = r#"
/// public class Greeter {
///     public String greet(String name) {
///         return "Hello, " + name;
///     }
///
///     public void sayGoodbye() {
///         System.out.println("Goodbye");
///     }
/// }
/// "#;
///
/// let methods = extract_methods(java_code.as_bytes());
/// assert_eq!(methods.len(), 2);
/// assert!(methods[0].name == "greet" || methods[0].name == "sayGoodbye");
/// assert!(methods[0].body.contains("{") && methods[0].body.contains("}"));
/// ```
pub fn extract_methods(source: &[u8]) -> Vec<MethodBody> {
    let mut parser = get_cached_parser().lock().unwrap();
    let tree = match parser.parse(source, None) {
        Some(t) => t,
        None => return vec![],
    };

    let query = get_cached_method_query();

    let name_idx = query
        .capture_index_for_name("method_name")
        .expect("capture 'method_name'");
    let body_idx = query
        .capture_index_for_name("method_body")
        .expect("capture 'method_body'");

    let mut cursor = QueryCursor::new();
    let mut matches = cursor.matches(query, tree.root_node(), source);

    let mut methods = Vec::new();
    while let Some(m) = matches.next() {
        let name_node = m
            .captures
            .iter()
            .find(|c| c.index == name_idx)
            .map(|c| c.node);
        let body_node = m
            .captures
            .iter()
            .find(|c| c.index == body_idx)
            .map(|c| c.node);

        if let (Some(name), Some(body)) = (name_node, body_node) {
            let name_str: &str = name.utf8_text(source).unwrap_or("");
            let body_str: &str = body.utf8_text(source).unwrap_or("");
            let start_line = body.start_position().row + 1;
            let end_line = body.end_position().row + 1;
            methods.push(MethodBody {
                name: name_str.to_owned(),
                body: body_str.to_owned(),
                start_line,
                end_line,
            });
        }
    }
    methods
}

// ── Matcher ────────────────────────────────────────────────────────────────────

/// Literal TODO/FIXME/HACK keywords — aho-corasick single-pass.
const TODO_LITERALS: &[&str] = &[
    "// TODO",
    "// FIXME",
    "// HACK",
    "// XXX",
    "//TODO",
    "//FIXME",
    "//HACK",
    "//XXX",
    "@stub",
    "@incomplete",
];

/// Mock/stub/fake/demo variable prefixes — aho-corasick prefix scan.
const MOCK_PREFIXES: &[&str] = &["mock", "stub", "fake", "demo"];

struct RegexPattern {
    name: &'static str,
    re: Regex,
    fix: &'static str,
}

/// Compiled pattern set: aho-corasick for literals, regex for structural patterns.
pub struct PatternSet {
    todo_ac: &'static AhoCorasick,
    mock_ac: &'static AhoCorasick,
    structural: &'static [RegexPattern],
}

/// A single match hit from [`PatternSet::match_body`].
#[derive(Debug)]
pub struct PatternHit {
    pub pattern: &'static str,
    pub matched: String,
    pub fix: &'static str,
    /// Line offset within the method body (1-based).
    pub body_line: usize,
}

/// Cached aho-corasick TODO literals automaton.
fn get_cached_todo_ac() -> &'static AhoCorasick {
    static AC: OnceLock<AhoCorasick> = OnceLock::new();
    AC.get_or_init(|| AhoCorasick::new(TODO_LITERALS).expect("aho-corasick TODO build"))
}

/// Cached aho-corasick MOCK prefix automaton.
fn get_cached_mock_ac() -> &'static AhoCorasick {
    static AC: OnceLock<AhoCorasick> = OnceLock::new();
    AC.get_or_init(|| AhoCorasick::new(MOCK_PREFIXES).expect("aho-corasick mock-prefix build"))
}

/// Cached structural regex patterns.
fn get_cached_structural_patterns() -> &'static [RegexPattern] {
    static PATTERNS: OnceLock<Vec<RegexPattern>> = OnceLock::new();
    PATTERNS.get_or_init(|| {
        vec![
            RegexPattern {
                name: "H_STUB_NULL",
                re: Regex::new(r"\breturn\s+null\s*;").unwrap(),
                fix: "Return a real value or throw UnsupportedOperationException.",
            },
            RegexPattern {
                name: "H_STUB_EMPTY_STRING",
                re: Regex::new(r#"\breturn\s+""\s*;"#).unwrap(),
                fix: "Return meaningful data, not an empty string.",
            },
            RegexPattern {
                name: "H_STUB_EMPTY_COLLECTION",
                re: Regex::new(
                    r"\breturn\s+(?:Collections\.empty\w+\(\)|List\.of\(\)|Map\.of\(\)|Set\.of\(\))\s*;",
                )
                .unwrap(),
                fix: "Return real data, not an empty collection.",
            },
            RegexPattern {
                name: "H_EMPTY",
                re: Regex::new(r"^\s*\{\s*\}\s*$").unwrap(),
                fix: "Implement the method body.",
            },
            RegexPattern {
                name: "H_FALLBACK",
                re: Regex::new(
                    r#"(?s)catch\s*\([^)]+\)\s*\{[^}]*\breturn\s+(?:null|""|0|false|Collections\.empty\w+\(\))\s*;"#,
                )
                .unwrap(),
                fix: "Rethrow or handle the exception; do not swallow it with a fake return.",
            },
            RegexPattern {
                name: "H_SILENT",
                re: Regex::new(
                    r#"(?i)(?:log|logger|System\.out)\.(?:warn|info|debug|error)\s*\([^)]*not\s+implemented[^)]*\)"#,
                )
                .unwrap(),
                fix: "Throw UnsupportedOperationException instead of logging.",
            },
        ]
    }).as_slice()
}

impl PatternSet {
    /// Create a new pattern set with all H-Guard patterns pre-compiled.
    ///
    /// Patterns are cached using `OnceLock` for efficiency, so multiple
    /// instantiations reuse the same compiled automata.
    ///
    /// # Examples
    ///
    /// ```
    /// use cct_scanner::PatternSet;
    ///
    /// let patterns = PatternSet::new();
    /// let body = "{ // TODO: implement\n    return null;\n}";
    /// let hits = patterns.match_body(body);
    ///
    /// assert!(!hits.is_empty());
    /// assert!(hits.iter().any(|h| h.pattern == "H_TODO"));
    /// ```
    pub fn new() -> Self {
        PatternSet {
            todo_ac: get_cached_todo_ac(),
            mock_ac: get_cached_mock_ac(),
            structural: get_cached_structural_patterns(),
        }
    }

    /// Count newlines in a slice using memchr for speed.
    #[inline]
    fn count_newlines(data: &[u8]) -> usize {
        let mut count = 0;
        let mut offset = 0;
        while let Some(pos) = memchr(b'\n', &data[offset..]) {
            count += 1;
            offset += pos + 1;
        }
        count
    }

    /// Scan a method body string and return all pattern hits.
    ///
    /// Detects H-Guard violations including:
    /// - `H_TODO`: TODO/FIXME/HACK/XXX comments
    /// - `H_MOCK`: mock/stub/fake/demo variable prefixes (camelCase)
    /// - `H_STUB_NULL`: `return null;` statements
    /// - `H_STUB_EMPTY_STRING`: `return "";` statements
    /// - `H_STUB_EMPTY_COLLECTION`: empty collection returns
    /// - `H_EMPTY`: completely empty method bodies
    /// - `H_FALLBACK`: catch blocks with fake returns
    /// - `H_SILENT`: logging instead of throwing exceptions
    ///
    /// # Examples
    ///
    /// ```
    /// use cct_scanner::PatternSet;
    ///
    /// let patterns = PatternSet::new();
    ///
    /// // Detect H_TODO
    /// let body_todo = "{\n    // TODO: implement this\n    return 0;\n}";
    /// let hits = patterns.match_body(body_todo);
    /// assert!(hits.iter().any(|h| h.pattern == "H_TODO"));
    ///
    /// // Detect H_STUB_NULL
    /// let body_null = "{\n    return null;\n}";
    /// let hits = patterns.match_body(body_null);
    /// assert!(hits.iter().any(|h| h.pattern == "H_STUB_NULL"));
    ///
    /// // Detect H_EMPTY
    /// let body_empty = "{}";
    /// let hits = patterns.match_body(body_empty);
    /// assert!(hits.iter().any(|h| h.pattern == "H_EMPTY"));
    ///
    /// // Detect H_MOCK variables (camelCase)
    /// let body_mock = "{\n    String mockService = null;\n}";
    /// let hits = patterns.match_body(body_mock);
    /// assert!(hits.iter().any(|h| h.pattern == "H_MOCK"));
    /// ```
    pub fn match_body(&self, body: &str) -> Vec<PatternHit> {
        let body_bytes = body.as_bytes();
        let mut hits = Vec::new();

        // ── aho-corasick pass: H_TODO literals ────────────────────────────────
        for mat in self.todo_ac.find_iter(body) {
            let body_line = Self::count_newlines(&body_bytes[..mat.start()]) + 1;
            hits.push(PatternHit {
                pattern: "H_TODO",
                matched: body[mat.start()..mat.end()].to_owned(),
                fix: "Implement the method body or remove the placeholder comment.",
                body_line,
            });
        }

        // ── aho-corasick pass: H_MOCK variable prefixes ───────────────────────
        // Flag only camelCase: mockFoo, stubRepo, fakeService — not "mocking" or "stubbing"
        for mat in self.mock_ac.find_iter(body) {
            let after_char = body[mat.end()..].chars().next();
            if after_char.map(|c| c.is_uppercase()).unwrap_or(false) {
                let body_line = Self::count_newlines(&body_bytes[..mat.start()]) + 1;
                let end_pos = mat.end() + after_char.map(char::len_utf8).unwrap_or(0);
                hits.push(PatternHit {
                    pattern: "H_MOCK",
                    matched: body[mat.start()..end_pos].to_owned(),
                    fix: "Use real objects instead of mocks in production code.",
                    body_line,
                });
            }
        }

        // ── Regex pass: structural patterns ──────────────────────────────────
        for pat in self.structural {
            if let Some(m) = pat.re.find(body) {
                let body_line = Self::count_newlines(&body_bytes[..m.start()]) + 1;
                hits.push(PatternHit {
                    pattern: pat.name,
                    matched: m.as_str().to_owned(),
                    fix: pat.fix,
                    body_line,
                });
            }
        }

        hits
    }
}

impl Default for PatternSet {
    fn default() -> Self {
        Self::new()
    }
}

// ── Walker ─────────────────────────────────────────────────────────────────────

/// Paths excluded by default — matches the existing cct-patterns and dtr-guard behavior.
const DEFAULT_EXCLUDES: &[&str] = &["/src/test/", "Test.java", "DocTest.java", "/target/"];

/// Walk `root` for `.java` files, skipping test paths and `.gitignore`-d entries.
///
/// `extra_excludes` extends the default exclusion list with additional path substrings.
///
/// Default exclusions: `/src/test/`, `Test.java`, `DocTest.java`, `/target/`
///
/// # Examples
///
/// ```
/// use cct_scanner::walk_java_files;
/// use std::path::PathBuf;
/// use tempfile::TempDir;
/// use std::fs;
///
/// let temp = TempDir::new().unwrap();
/// let src = temp.path().join("src");
/// fs::create_dir_all(&src).unwrap();
/// fs::write(src.join("App.java"), "public class App {}").unwrap();
///
/// let files = walk_java_files(temp.path(), &[]);
/// assert_eq!(files.len(), 1);
/// assert!(files[0].ends_with("App.java"));
/// ```
pub fn walk_java_files(root: &Path, extra_excludes: &[&str]) -> Vec<PathBuf> {
    let all_excludes: Vec<&str> = DEFAULT_EXCLUDES
        .iter()
        .chain(extra_excludes.iter())
        .copied()
        .collect();

    let mut paths = Vec::new();
    for result in ignore::Walk::new(root) {
        let entry = match result {
            Ok(e) => e,
            Err(_) => continue,
        };
        let path = entry.path();
        if path.extension().map(|e| e == "java").unwrap_or(false) {
            let path_str = path.to_string_lossy();
            if !all_excludes.iter().any(|exc| path_str.contains(*exc)) {
                paths.push(path.to_path_buf());
            }
        }
    }
    paths
}

// ── Scanner ────────────────────────────────────────────────────────────────────

/// Compiled class-level mock/stub/fake detector.
/// Separate from [`PatternSet`] because it runs over the full source, not per-method.
struct ClassLevelPatterns {
    mock_class_re: Regex,
}

impl ClassLevelPatterns {
    fn new() -> Self {
        ClassLevelPatterns {
            mock_class_re: Regex::new(r"\b(?:class|interface)\s+(?:Mock|Stub|Fake|Demo)[A-Z]\w*")
                .unwrap(),
        }
    }

    fn check(&self, path: &Path, source: &str) -> Vec<Violation> {
        self.mock_class_re
            .find_iter(source)
            .map(|m| {
                let line = source[..m.start()].bytes().filter(|&b| b == b'\n').count() + 1;
                Violation {
                    path: path.to_path_buf(),
                    method: "<class>".to_owned(),
                    line,
                    pattern: "H_MOCK_CLASS".to_owned(),
                    matched: m.as_str().to_owned(),
                    fix: "Use real implementations, not mock/stub/fake class declarations."
                        .to_owned(),
                }
            })
            .collect()
    }
}

/// Main scanner: tree-sitter extraction + aho-corasick/regex matching + memmap2 I/O.
///
/// `Scanner` is `Send + Sync` — safe to share across rayon threads.
pub struct Scanner {
    patterns: PatternSet,
    class_patterns: ClassLevelPatterns,
}

impl Scanner {
    /// Create a new scanner with all H-Guard patterns ready to use.
    ///
    /// # Examples
    ///
    /// ```
    /// use cct_scanner::Scanner;
    /// use std::path::Path;
    ///
    /// let scanner = Scanner::new();
    /// let java_code = b"public class Test { public void foo() { return null; } }";
    /// let result = scanner.scan_source(Path::new("Test.java"), java_code);
    /// assert!(result.violations.iter().any(|v| v.pattern == "H_STUB_NULL"));
    /// ```
    pub fn new() -> Self {
        Scanner {
            patterns: PatternSet::new(),
            class_patterns: ClassLevelPatterns::new(),
        }
    }

    /// Scan in-memory source bytes. Used directly in tests and by `scan_file`.
    ///
    /// Performs both per-method scanning (H_TODO, H_STUB_*, H_MOCK, etc.)
    /// and class-level scanning (H_MOCK_CLASS for mock/stub/fake class declarations).
    ///
    /// # Examples
    ///
    /// ```
    /// use cct_scanner::Scanner;
    /// use std::path::Path;
    ///
    /// let scanner = Scanner::new();
    /// let code = r#"
    /// public class UserService {
    ///     public User getUser() {
    ///         // TODO: fetch from database
    ///         return null;
    ///     }
    /// }
    /// "#;
    ///
    /// let result = scanner.scan_source(
    ///     Path::new("UserService.java"),
    ///     code.as_bytes()
    /// );
    /// assert!(!result.violations.is_empty());
    /// assert!(result.violations.iter().any(|v| v.pattern == "H_TODO"));
    /// assert!(result.violations.iter().any(|v| v.pattern == "H_STUB_NULL"));
    /// ```
    pub fn scan_source(&self, path: &Path, source: &[u8]) -> ScanResult {
        let methods = extract_methods(source);
        let mut violations = Vec::new();

        // Per-method body scan (tree-sitter extraction → aho-corasick + regex)
        for method in &methods {
            for hit in self.patterns.match_body(&method.body) {
                let abs_line = method.start_line + hit.body_line.saturating_sub(1);
                violations.push(Violation {
                    path: path.to_path_buf(),
                    method: method.name.clone(),
                    line: abs_line,
                    pattern: hit.pattern.to_string(),
                    matched: hit.matched.clone(),
                    fix: hit.fix.to_string(),
                });
            }
        }

        // Class-level scan (H_MOCK_CLASS over full source text)
        if let Ok(source_str) = std::str::from_utf8(source) {
            violations.extend(self.class_patterns.check(path, source_str));
        }

        ScanResult {
            path: path.to_path_buf(),
            violations,
        }
    }

    /// Scan a file on disk using memmap2 for zero-copy reading.
    ///
    /// # Errors
    /// Returns an error if the file cannot be opened or memory-mapped.
    ///
    /// # Examples
    ///
    /// ```
    /// use cct_scanner::Scanner;
    /// use tempfile::NamedTempFile;
    /// use std::io::Write;
    ///
    /// let mut file = NamedTempFile::new().unwrap();
    /// writeln!(file, "public class Test {{ public void foo() {{ return null; }} }}").unwrap();
    ///
    /// let scanner = Scanner::new();
    /// let result = scanner.scan_file(file.path()).unwrap();
    /// assert!(!result.violations.is_empty());
    /// ```
    pub fn scan_file(&self, path: &Path) -> Result<ScanResult> {
        let file = File::open(path)?;
        // SAFETY: we do not mutate the backing file while the mapping is live.
        let mmap = unsafe { Mmap::map(&file)? };
        Ok(self.scan_source(path, &mmap))
    }

    /// Scan a list of files in parallel with rayon. Errors per-file are silently skipped.
    pub fn scan_files_parallel(&self, paths: &[PathBuf]) -> Vec<ScanResult> {
        paths
            .par_iter()
            .filter_map(|p| self.scan_file(p).ok())
            .collect()
    }
}

impl Default for Scanner {
    fn default() -> Self {
        Self::new()
    }
}

// ── Tests ──────────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use std::io::Write;

    // ── Fixtures ───────────────────────────────────────────────────────────────

    const CLEAN_JAVA: &str = r#"
public class Greeter {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
    public int add(int a, int b) {
        return a + b;
    }
}
"#;

    const STUB_NULL_JAVA: &str = r#"
public class UserService {
    public User findById(long id) {
        return null;
    }
}
"#;

    const TODO_JAVA: &str = r#"
public class Calculator {
    public int multiply(int a, int b) {
        // TODO: implement multiplication
        return 0;
    }
}
"#;

    const MOCK_CLASS_JAVA: &str = r#"
public class MockUserRepository {
    public User find(long id) {
        return new User();
    }
}
"#;

    // ── Extractor tests ────────────────────────────────────────────────────────

    #[test]
    fn test_extract_real_java_method() {
        let methods = extract_methods(CLEAN_JAVA.as_bytes());
        assert!(!methods.is_empty(), "should find at least one method");
        let greet = methods.iter().find(|m| m.name == "greet");
        assert!(greet.is_some(), "should find method named 'greet'");
        let greet = greet.unwrap();
        assert!(
            greet.body.contains("Hello"),
            "body should contain the return expression: {:?}",
            greet.body
        );
        assert!(greet.start_line > 0);
        assert!(greet.end_line >= greet.start_line);
    }

    #[test]
    fn test_extract_multiple_methods() {
        let methods = extract_methods(CLEAN_JAVA.as_bytes());
        assert_eq!(methods.len(), 2, "should find both greet and add methods");
        let names: Vec<&str> = methods.iter().map(|m| m.name.as_str()).collect();
        assert!(names.contains(&"greet"));
        assert!(names.contains(&"add"));
    }

    // ── Matcher tests ──────────────────────────────────────────────────────────

    #[test]
    fn test_aho_corasick_hits_todo() {
        let patterns = PatternSet::new();
        let body = "{\n    // TODO: implement this\n    return 0;\n}";
        let hits = patterns.match_body(body);
        assert!(
            hits.iter().any(|h| h.pattern == "H_TODO"),
            "H_TODO should be detected in TODO comment, got: {:?}",
            hits.iter().map(|h| h.pattern).collect::<Vec<_>>()
        );
    }

    #[test]
    fn test_regex_hits_stub_null() {
        let patterns = PatternSet::new();
        let body = "{\n    return null;\n}";
        let hits = patterns.match_body(body);
        assert!(
            hits.iter().any(|h| h.pattern == "H_STUB_NULL"),
            "H_STUB_NULL should be detected for 'return null'"
        );
    }

    #[test]
    fn test_clean_body_no_hits() {
        let patterns = PatternSet::new();
        let body = "{\n    return \"Hello, \" + name + \"!\";\n}";
        let hits = patterns.match_body(body);
        assert!(
            hits.is_empty(),
            "clean body should produce no hits, got: {:?}",
            hits.iter().map(|h| h.pattern).collect::<Vec<_>>()
        );
    }

    // ── Walker tests ───────────────────────────────────────────────────────────

    #[test]
    fn test_walker_respects_test_exclusion() {
        let dir = tempfile::tempdir().unwrap();
        let test_dir = dir.path().join("src").join("test").join("java");
        std::fs::create_dir_all(&test_dir).unwrap();
        let mut f = std::fs::File::create(test_dir.join("FooTest.java")).unwrap();
        f.write_all(b"class FooTest {}").unwrap();

        let files = walk_java_files(dir.path(), &[]);
        assert!(
            files.is_empty(),
            "test files should be excluded by default, found: {:?}",
            files
        );
    }

    #[test]
    fn test_walker_finds_main_java() {
        let dir = tempfile::tempdir().unwrap();
        let main_dir = dir.path().join("src").join("main").join("java");
        std::fs::create_dir_all(&main_dir).unwrap();
        let mut f = std::fs::File::create(main_dir.join("Greeter.java")).unwrap();
        f.write_all(CLEAN_JAVA.as_bytes()).unwrap();

        let files = walk_java_files(dir.path(), &[]);
        assert_eq!(files.len(), 1, "should find one main Java file");
    }

    // ── Scanner integration tests ──────────────────────────────────────────────

    #[test]
    fn test_scan_source_clean() {
        let scanner = Scanner::new();
        let result = scanner.scan_source(Path::new("Greeter.java"), CLEAN_JAVA.as_bytes());
        assert!(
            result.is_clean(),
            "clean implementation should produce no violations, got: {:?}",
            result.violations
        );
    }

    #[test]
    fn test_scan_source_h_stub_null() {
        let scanner = Scanner::new();
        let result = scanner.scan_source(Path::new("UserService.java"), STUB_NULL_JAVA.as_bytes());
        assert!(
            result.violations.iter().any(|v| v.pattern == "H_STUB_NULL"),
            "return null should trigger H_STUB_NULL, got: {:?}",
            result.violations
        );
    }

    #[test]
    fn test_scan_source_h_todo() {
        let scanner = Scanner::new();
        let result = scanner.scan_source(Path::new("Calculator.java"), TODO_JAVA.as_bytes());
        assert!(
            result.violations.iter().any(|v| v.pattern == "H_TODO"),
            "TODO comment should trigger H_TODO, got: {:?}",
            result.violations
        );
    }

    #[test]
    fn test_scan_source_h_mock_class() {
        let scanner = Scanner::new();
        let result = scanner.scan_source(
            Path::new("MockUserRepository.java"),
            MOCK_CLASS_JAVA.as_bytes(),
        );
        assert!(
            result
                .violations
                .iter()
                .any(|v| v.pattern == "H_MOCK_CLASS"),
            "Mock class declaration should trigger H_MOCK_CLASS, got: {:?}",
            result.violations
        );
    }

    #[test]
    fn test_scan_file_roundtrip() {
        let mut tmp = tempfile::NamedTempFile::new().unwrap();
        tmp.write_all(STUB_NULL_JAVA.as_bytes()).unwrap();
        let scanner = Scanner::new();
        let result = scanner.scan_file(tmp.path()).unwrap();
        assert!(
            result.violations.iter().any(|v| v.pattern == "H_STUB_NULL"),
            "scan_file should find H_STUB_NULL via memmap2"
        );
    }
}
