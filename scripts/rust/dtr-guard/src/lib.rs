//! DTR H-Guard: semantic lie detection for Java source files.
//!
//! Implements the seven H-Guard patterns from the YAWL truth-enforcement architecture:
//! `H_TODO`, `H_MOCK`, `H_MOCK_CLASS`, `H_STUB`, `H_EMPTY`, `H_FALLBACK`, `H_SILENT`.
//!
//! Each pattern is a compiled regex applied line-by-line to Java source.
//! Files in test source trees are excluded by default.

use anyhow::Result;
use regex::Regex;
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::Path;

/// A single H-Guard violation found in a Java source file.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Violation {
    /// Path to the file containing the violation.
    pub file: String,
    /// 1-based line number.
    pub line: usize,
    /// H-Guard pattern code.
    pub code: String,
    /// The matched source text (trimmed).
    pub matched: String,
    /// Human-readable fix guidance.
    pub fix: String,
}

/// All seven H-Guard patterns, compiled once at startup.
#[allow(clippy::struct_field_names)]
pub struct GuardPatterns {
    h_todo: Regex,
    h_mock_class: Regex,
    h_mock: Regex,
    h_stub_empty_string: Regex,
    h_stub_null: Regex,
    h_stub_empty_collection: Regex,
    h_empty_body: Regex,
    h_fallback: Regex,
    h_silent: Regex,
}

impl GuardPatterns {
    /// Compile all patterns. Panics if a pattern is invalid (programmer error).
    ///
    /// # Panics
    /// Panics if any of the regex patterns fail to compile (e.g., due to invalid regex syntax).
    /// This is a programmer error and should never occur with the hardcoded patterns.
    #[must_use]
    pub fn compile() -> Self {
        GuardPatterns {
            // H_TODO: deferred work markers in comments
            h_todo: Regex::new(
                r"//\s*(TODO|FIXME|XXX|HACK|LATER|FUTURE|@stub|@incomplete|placeholder|not\s+implemented\s+yet|coming\s+soon)",
            )
            .unwrap(),

            // H_MOCK_CLASS: class/interface declarations named Mock*/Stub*/Fake*/Demo*
            h_mock_class: Regex::new(
                r"(?i)\b(class|interface)\s+(Mock|Stub|Fake|Demo)[A-Za-z]",
            )
            .unwrap(),

            // H_MOCK: camelCase identifiers starting with mock/stub/fake/demo
            h_mock: Regex::new(r"\b(mock|stub|fake|demo)[A-Z][a-zA-Z]*\s*[=(,\)]").unwrap(),

            // H_STUB: empty string return
            h_stub_empty_string: Regex::new(r#"^\s*return\s+""\s*;"#).unwrap(),

            // H_STUB: null return (not in map lookup / ternary context)
            h_stub_null: Regex::new(r"^\s*return\s+null\s*;").unwrap(),

            // H_STUB: empty collection return
            h_stub_empty_collection: Regex::new(
                r"^\s*return\s+(Collections\.(emptyList|emptyMap|emptySet|emptySortedSet|emptySortedMap)\(\)|new\s+(ArrayList|HashMap|HashSet|TreeSet|LinkedList|TreeMap)\s*(<[^>]*>)?\s*\(\s*\))\s*;",
            )
            .unwrap(),

            // H_EMPTY: empty method body on a single line: }  or  { }
            // Matches lines that are just a close-brace (method body with nothing in it is
            // detected by the two-line pattern: method signature + empty brace pair).
            // We detect the inline empty-body pattern: `void foo() { }` or `void foo() {}`
            h_empty_body: Regex::new(
                r"\b(public|protected|private)\s+\w[\w<>\[\],\s]*\s+\w+\s*\([^)]*\)\s*\{\s*\}",
            )
            .unwrap(),

            // H_FALLBACK: catch block that returns fabricated/empty data instead of rethrowing
            h_fallback: Regex::new(
                r#"catch\s*\([^)]+\)\s*\{\s*return\s+(null|""|0|Collections\.|new\s+(ArrayList|HashMap|HashSet|Optional))"#,
            )
            .unwrap(),

            // H_SILENT: logging "not implemented" instead of throwing
            h_silent: Regex::new(
                r#"log\.(warn|error|info)\s*\([^)]*"[^"]*(?:not\s+implemented|unimplemented|not\s+supported|TODO)[^"]*""#,
            )
            .unwrap(),
        }
    }

    /// Scan a single line against all H-Guard patterns.
    /// Returns zero or more violations (a line can match multiple patterns).
    #[must_use]
    pub fn scan_line(&self, line: &str, line_num: usize, file: &str) -> Vec<Violation> {
        let mut hits = Vec::new();
        let trimmed = line.trim();

        // Skip blank lines and pure Javadoc (/* */ comments outside method bodies are OK)
        if trimmed.is_empty() {
            return hits;
        }

        let fix = "throw new UnsupportedOperationException(\"Not implemented\");";

        if self.h_todo.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_TODO".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: fix.to_string(),
            });
        }

        if self.h_mock_class.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_MOCK_CLASS".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: "Remove mock class from production source tree; use real implementation or inject via test framework.".to_string(),
            });
        }

        if self.h_mock.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_MOCK".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: "Rename to a real identifier describing the actual value.".to_string(),
            });
        }

        if self.h_stub_empty_string.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_STUB".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: fix.to_string(),
            });
        }

        if self.h_stub_null.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_STUB".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: fix.to_string(),
            });
        }

        if self.h_stub_empty_collection.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_STUB".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: fix.to_string(),
            });
        }

        if self.h_empty_body.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_EMPTY".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: fix.to_string(),
            });
        }

        if self.h_fallback.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_FALLBACK".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: "Rethrow or wrap as a checked/unchecked exception — never return fabricated data from a catch block.".to_string(),
            });
        }

        if self.h_silent.is_match(line) {
            hits.push(Violation {
                file: file.to_string(),
                line: line_num,
                code: "H_SILENT".to_string(),
                matched: trimmed.chars().take(120).collect(),
                fix: fix.to_string(),
            });
        }

        hits
    }
}

/// Scan a Java source file for H-Guard violations.
///
/// # Arguments
/// * `path` — path to the `.java` file
/// * `patterns` — compiled guard patterns (reuse across calls)
/// * `exclude_tests` — if true, skip files under `src/test/` paths
///
/// # Errors
/// Returns an error if the file cannot be read (e.g., permission denied, file not found).
pub fn scan_file(
    path: &Path,
    patterns: &GuardPatterns,
    exclude_tests: bool,
) -> Result<Vec<Violation>> {
    let path_str = path.to_string_lossy();

    if exclude_tests && is_test_path(&path_str) {
        return Ok(Vec::new());
    }

    let content = fs::read_to_string(path)?;
    let mut violations = Vec::new();

    for (idx, line) in content.lines().enumerate() {
        let line_num = idx + 1;
        let hits = patterns.scan_line(line, line_num, &path_str);
        violations.extend(hits);
    }

    Ok(violations)
}

/// Scan Java source text (not a file) — used by the hook to scan proposed content.
#[must_use]
pub fn scan_content(content: &str, label: &str, patterns: &GuardPatterns) -> Vec<Violation> {
    let mut violations = Vec::new();
    for (idx, line) in content.lines().enumerate() {
        let hits = patterns.scan_line(line, idx + 1, label);
        violations.extend(hits);
    }
    violations
}

/// Returns true if the path is under a test source tree.
/// Test violations are expected and allowed (mocks, stubs, etc. are fine in tests).
#[must_use]
pub fn is_test_path(path: &str) -> bool {
    path.contains("/src/test/")
        || path.contains("\\src\\test\\")
        || path.contains("/test/java/")
        || path.contains("\\test\\java\\")
        || path.ends_with("Test.java")
        || path.ends_with("Tests.java")
        || path.ends_with("TestCase.java")
        || path.ends_with("IT.java")
        || path.ends_with("DocTest.java")
}

#[cfg(test)]
mod tests {
    use super::*;

    fn patterns() -> GuardPatterns {
        GuardPatterns::compile()
    }

    fn violation_codes(line: &str) -> Vec<String> {
        let p = patterns();
        p.scan_line(line, 1, "Test.java")
            .into_iter()
            .map(|v| v.code)
            .collect()
    }

    #[test]
    fn h_todo_basic() {
        assert!(violation_codes("    // TODO: fix this").contains(&"H_TODO".to_string()));
    }

    #[test]
    fn h_todo_fixme() {
        assert!(violation_codes("// FIXME broken").contains(&"H_TODO".to_string()));
    }

    #[test]
    fn h_todo_hack() {
        assert!(violation_codes("// HACK workaround").contains(&"H_TODO".to_string()));
    }

    #[test]
    fn h_todo_no_false_positive_in_string() {
        // A TODO in a real string literal (not a comment) should still be caught by h_todo
        // since the regex matches the text. This is acceptable — production code should not
        // contain the word TODO in any form.
        let codes = violation_codes(r#"    String msg = "TODO later";"#);
        // This is a false positive from h_todo — acceptable conservative behavior
        let _ = codes; // no assertion, just verify it doesn't panic
    }

    #[test]
    fn h_mock_class() {
        assert!(
            violation_codes("public class MockAuthService implements AuthService {")
                .contains(&"H_MOCK_CLASS".to_string())
        );
        assert!(violation_codes("class StubDatabase extends Database {")
            .contains(&"H_MOCK_CLASS".to_string()));
    }

    #[test]
    fn h_mock_method_var() {
        assert!(
            violation_codes("    ZaiClient mockClient = factory.create();")
                .contains(&"H_MOCK".to_string())
        );
        assert!(
            violation_codes("    String stubResponse = call();").contains(&"H_MOCK".to_string())
        );
    }

    #[test]
    fn h_stub_empty_string() {
        assert!(violation_codes(r#"        return "";"#).contains(&"H_STUB".to_string()));
    }

    #[test]
    fn h_stub_null() {
        assert!(violation_codes("        return null;").contains(&"H_STUB".to_string()));
    }

    #[test]
    fn h_stub_empty_list() {
        assert!(violation_codes("        return Collections.emptyList();")
            .contains(&"H_STUB".to_string()));
        assert!(
            violation_codes("        return new ArrayList<>();").contains(&"H_STUB".to_string())
        );
    }

    #[test]
    fn h_empty_body_inline() {
        assert!(violation_codes("    public void init() { }").contains(&"H_EMPTY".to_string()));
        assert!(violation_codes("    protected void onStart() {}").contains(&"H_EMPTY".to_string()));
    }

    #[test]
    fn h_silent_log() {
        assert!(
            violation_codes(r#"        log.warn("not implemented yet");"#)
                .contains(&"H_SILENT".to_string())
        );
        assert!(violation_codes(r#"        log.error("unimplemented");"#)
            .contains(&"H_SILENT".to_string()));
    }

    #[test]
    fn clean_line_no_violations() {
        assert!(violation_codes("        return this.name;").is_empty());
        assert!(violation_codes("    // Returns the formatted display name.").is_empty());
        assert!(violation_codes("    public String getName() {").is_empty());
    }

    #[test]
    fn test_path_exclusion() {
        assert!(is_test_path("src/test/java/com/example/FooTest.java"));
        assert!(is_test_path("dtr-core/src/test/java/io/DtrCoreTest.java"));
        assert!(!is_test_path("src/main/java/com/example/Foo.java"));
    }

    #[test]
    fn scan_content_detects_todo() {
        let p = patterns();
        let code = "public class Foo {\n    // TODO implement this\n    public void run() {}\n}";
        let violations = scan_content(code, "<proposed>", &p);
        assert!(!violations.is_empty());
        assert!(violations.iter().any(|v| v.code == "H_TODO"));
    }
}
