use rayon::prelude::*;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use tree_sitter::StreamingIterator;
use walkdir::WalkDir;

// ============================================================================
// Data model
// ============================================================================

#[derive(Serialize, Deserialize, Debug, Default, PartialEq)]
pub struct JavadocEntry {
    pub description: String,
    pub params: Vec<ParamDoc>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub returns: Option<String>,
    pub throws: Vec<ThrowsDoc>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub since: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub deprecated: Option<String>,
    pub see: Vec<String>,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
pub struct ParamDoc {
    pub name: String,
    pub description: String,
}

#[derive(Serialize, Deserialize, Debug, PartialEq)]
pub struct ThrowsDoc {
    pub exception: String,
    pub description: String,
}

// ============================================================================
// Tree-sitter Java query
// ============================================================================

/// Query to find `block_comment . method_declaration` and
/// `block_comment . constructor_declaration` pairs.
///
/// The `.` operator is the immediate-sibling anchor: the comment must be
/// the direct preceding sibling of the declaration — which is the Javadoc
/// specification for doc comment association.
pub const JAVA_QUERY: &str = r#"
(
  (block_comment) @doc
  .
  (method_declaration
    name: (identifier) @method_name)
  (#match? @doc "^\\/\\*\\*")
)

(
  (block_comment) @doc
  .
  (constructor_declaration
    name: (identifier) @method_name)
  (#match? @doc "^\\/\\*\\*")
)
"#;

// ============================================================================
// Public API
// ============================================================================

/// Scan all `.java` files under `source_dir` in parallel and return a map of
/// `fully.qualified.ClassName#methodName` → `JavadocEntry`.
pub fn extract_all(source_dir: &Path) -> HashMap<String, JavadocEntry> {
    let files: Vec<PathBuf> = WalkDir::new(source_dir)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().map_or(false, |ext| ext == "java"))
        .map(|e| e.path().to_owned())
        .collect();

    files
        .par_iter()
        .flat_map(|path| extract_from_file(path))
        .collect()
}

// ============================================================================
// Per-file extraction
// ============================================================================

/// Extract all Javadoc entries from a single `.java` file.
pub fn extract_from_file(path: &Path) -> Vec<(String, JavadocEntry)> {
    let source = match std::fs::read(path) {
        Ok(b) => b,
        Err(_) => return vec![],
    };
    extract_from_source(&source, path)
}

/// Extract from an in-memory source buffer (used by both `extract_from_file`
/// and tests).
pub fn extract_from_source(source: &[u8], path: &Path) -> Vec<(String, JavadocEntry)> {
    let package = derive_package(source);
    let class_name = class_name_from_path(path);

    let fqcn = if package.is_empty() {
        class_name.to_string()
    } else {
        format!("{}.{}", package, class_name)
    };

    let mut java_parser = tree_sitter::Parser::new();
    if java_parser
        .set_language(&tree_sitter_java::LANGUAGE.into())
        .is_err()
    {
        return vec![];
    }

    let java_tree = match java_parser.parse(source, None) {
        Some(t) => t,
        None => return vec![],
    };

    let java_query = match tree_sitter::Query::new(&tree_sitter_java::LANGUAGE.into(), JAVA_QUERY) {
        Ok(q) => q,
        Err(_) => return vec![],
    };

    let capture_names = java_query.capture_names();
    let doc_idx = capture_names
        .iter()
        .position(|n| *n == "doc")
        .unwrap_or(0);
    let name_idx = capture_names
        .iter()
        .position(|n| *n == "method_name")
        .unwrap_or(1);

    let mut cursor = tree_sitter::QueryCursor::new();
    let mut qmatches = cursor.matches(&java_query, java_tree.root_node(), source);

    let mut results = Vec::new();

    while let Some(m) = qmatches.next() {
        let doc_node = m.captures.iter().find(|c| c.index as usize == doc_idx);
        let name_node = m.captures.iter().find(|c| c.index as usize == name_idx);

        if let (Some(doc), Some(name)) = (doc_node, name_node) {
            let comment_text = match doc.node.utf8_text(source) {
                Ok(t) => t,
                Err(_) => continue,
            };
            let method_name = match name.node.utf8_text(source) {
                Ok(t) => t,
                Err(_) => continue,
            };

            let key = format!("{}#{}", fqcn, method_name);

            if let Some(entry) = parse_javadoc_comment(comment_text) {
                results.push((key, entry));
            }
        }
    }

    results
}

// ============================================================================
// Javadoc comment parser (tree-sitter-javadoc)
// ============================================================================

/// Parse a `/** ... */` comment into a structured `JavadocEntry`.
///
/// The tree-sitter-javadoc grammar produces a `document` node with typed
/// children: `description`, `param_tag`, `return_tag`, `throws_tag`, etc.
/// This is an injection grammar — it expects to parse the comment in isolation,
/// not embedded in Java source.
///
/// Returns `None` if the comment has no description, params, returns, or throws.
pub fn parse_javadoc_comment(comment: &str) -> Option<JavadocEntry> {
    let mut jd_parser = tree_sitter::Parser::new();
    jd_parser
        .set_language(&tree_sitter_javadoc::LANGUAGE.into())
        .ok()?;

    let tree = jd_parser.parse(comment.as_bytes(), None)?;
    let root = tree.root_node();
    let src = comment.as_bytes();

    let mut entry = JavadocEntry::default();

    let mut cursor = root.walk();
    for child in root.children(&mut cursor) {
        match child.kind() {
            "description" => {
                let text = child.utf8_text(src).unwrap_or("");
                let cleaned = clean_comment_text(text);
                if !cleaned.is_empty() {
                    entry.description = cleaned;
                }
            }
            "param_tag" => {
                let name = child_text_by_kind(&child, src, "identifier");
                let desc = child_text_by_kind(&child, src, "description");
                if let Some(name) = name {
                    entry.params.push(ParamDoc {
                        name,
                        description: desc
                            .map(|d| clean_comment_text(&d))
                            .unwrap_or_default(),
                    });
                }
            }
            "return_tag" | "returns_tag" => {
                let desc = child_text_by_kind(&child, src, "description");
                if let Some(d) = desc {
                    let cleaned = clean_comment_text(&d);
                    if !cleaned.is_empty() {
                        entry.returns = Some(cleaned);
                    }
                }
            }
            "throws_tag" | "exception_tag" => {
                // Grammar: throws_tag → tag_name, type { identifier }, description
                // The exception class is wrapped in a `type` node containing `identifier`.
                let exc = child_text_by_kind(&child, src, "type")
                    .and_then(|_| {
                        // Get the `type` child node and extract its identifier
                        let mut c = child.walk();
                        child.children(&mut c)
                            .find(|n| n.kind() == "type")
                            .and_then(|type_node| {
                                let mut tc = type_node.walk();
                                type_node.children(&mut tc)
                                    .find(|n| n.kind() == "identifier")
                                    .and_then(|id_node| id_node.utf8_text(src).ok())
                                    .map(|s| s.trim().to_string())
                            })
                    })
                    .or_else(|| child_text_by_kind(&child, src, "identifier").map(|s| s.trim().to_string()));
                let desc = child_text_by_kind(&child, src, "description");
                if let Some(exc) = exc {
                    let exc_trimmed = exc.trim().to_string();
                    if !exc_trimmed.is_empty() {
                        entry.throws.push(ThrowsDoc {
                            exception: exc_trimmed,
                            description: desc
                                .map(|d| clean_comment_text(&d))
                                .unwrap_or_default(),
                        });
                    }
                }
            }
            "since_tag" => {
                let desc = child_text_by_kind(&child, src, "description");
                if let Some(d) = desc {
                    let cleaned = clean_comment_text(&d);
                    if !cleaned.is_empty() {
                        entry.since = Some(cleaned);
                    }
                }
            }
            "deprecated_tag" => {
                let desc = child_text_by_kind(&child, src, "description");
                if let Some(d) = desc {
                    let cleaned = clean_comment_text(&d);
                    if !cleaned.is_empty() {
                        entry.deprecated = Some(cleaned);
                    }
                }
            }
            "see_tag" => {
                let val = child.utf8_text(src).unwrap_or("");
                let v = val.trim_start_matches("@see").trim().to_string();
                if !v.is_empty() {
                    entry.see.push(v);
                }
            }
            _ => {}
        }
    }

    if entry.description.is_empty()
        && entry.params.is_empty()
        && entry.returns.is_none()
        && entry.throws.is_empty()
    {
        return None;
    }

    Some(entry)
}

// ============================================================================
// Helpers
// ============================================================================

/// Find the first named child of `node` with the given kind.
pub fn child_text_by_kind(
    node: &tree_sitter::Node,
    source: &[u8],
    kind: &str,
) -> Option<String> {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == kind {
            return Some(child.utf8_text(source).unwrap_or("").to_string());
        }
    }
    None
}

/// Extract the `package foo.bar;` declaration from Java source bytes.
/// Returns an empty string if no package declaration is found.
pub fn derive_package(source: &[u8]) -> String {
    let text = std::str::from_utf8(source).unwrap_or("");
    for line in text.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("package ") && trimmed.ends_with(';') {
            return trimmed["package ".len()..trimmed.len() - 1]
                .trim()
                .to_string();
        }
    }
    String::new()
}

/// Return the file stem (expected to match the class name) from a path.
pub fn class_name_from_path(path: &Path) -> &str {
    path.file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Unknown")
}

/// Strip Javadoc delimiters (`/**`, `*/`) and leading ` * ` markers from text.
///
/// Joins multiple lines into a single string separated by spaces.
pub fn clean_comment_text(text: &str) -> String {
    text.lines()
        .map(|line| {
            let t = line.trim();
            if t.starts_with("* ") {
                &t[2..]
            } else if t == "*" {
                ""
            } else if t.starts_with("/**") {
                t.trim_start_matches("/**").trim()
            } else if t.starts_with("*/") {
                ""
            } else {
                t
            }
        })
        .filter(|s| !s.is_empty())
        .collect::<Vec<_>>()
        .join(" ")
        .trim()
        .to_string()
}

// ============================================================================
// Tests
// ============================================================================

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::Path;

    // -----------------------------------------------------------------------
    // clean_comment_text
    // -----------------------------------------------------------------------

    #[test]
    fn clean_simple_description() {
        let input = "A text that will be rendered as a paragraph.";
        assert_eq!(clean_comment_text(input), "A text that will be rendered as a paragraph.");
    }

    #[test]
    fn clean_multiline_with_star_prefix() {
        let input = "* First line.\n* Second line.";
        assert_eq!(clean_comment_text(input), "First line. Second line.");
    }

    #[test]
    fn clean_strips_javadoc_delimiters() {
        let input = "/**\n * Body.\n */";
        assert_eq!(clean_comment_text(input), "Body.");
    }

    #[test]
    fn clean_blank_star_lines_dropped() {
        let input = "First.\n*\nSecond.";
        assert_eq!(clean_comment_text(input), "First. Second.");
    }

    #[test]
    fn clean_empty_string() {
        assert_eq!(clean_comment_text(""), "");
    }

    #[test]
    fn clean_only_whitespace() {
        assert_eq!(clean_comment_text("   \n  \n  "), "");
    }

    #[test]
    fn clean_inline_tags_preserved() {
        // {@code} inline tags are passed through as-is by tree-sitter-javadoc
        let input = "* Use {@code System.nanoTime()} for timing.";
        let result = clean_comment_text(input);
        assert!(result.contains("{@code System.nanoTime()}"), "got: {}", result);
    }

    // -----------------------------------------------------------------------
    // derive_package
    // -----------------------------------------------------------------------

    #[test]
    fn package_normal() {
        let src = b"package io.github.seanchatmangpt.dtr.rendermachine;\n\npublic interface Foo {}";
        assert_eq!(derive_package(src), "io.github.seanchatmangpt.dtr.rendermachine");
    }

    #[test]
    fn package_with_leading_whitespace() {
        let src = b"  package com.example.util;\n\npublic class Bar {}";
        assert_eq!(derive_package(src), "com.example.util");
    }

    #[test]
    fn package_default_package() {
        let src = b"public class Bare {}";
        assert_eq!(derive_package(src), "");
    }

    #[test]
    fn package_after_license_comment() {
        let src = b"/* Apache License */\n\npackage org.example;\n\nclass X {}";
        assert_eq!(derive_package(src), "org.example");
    }

    #[test]
    fn package_ignores_partial_match() {
        // "packages" should not match "package "
        let src = b"// packages are cool\npublic class X {}";
        assert_eq!(derive_package(src), "");
    }

    // -----------------------------------------------------------------------
    // class_name_from_path
    // -----------------------------------------------------------------------

    #[test]
    fn class_name_simple() {
        assert_eq!(class_name_from_path(Path::new("src/RenderMachine.java")), "RenderMachine");
    }

    #[test]
    fn class_name_deep_path() {
        assert_eq!(
            class_name_from_path(Path::new(
                "/home/user/dtr/dtr-core/src/main/java/io/github/Foo.java"
            )),
            "Foo"
        );
    }

    #[test]
    fn class_name_no_extension() {
        assert_eq!(class_name_from_path(Path::new("MyClass")), "MyClass");
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — description only
    // -----------------------------------------------------------------------

    #[test]
    fn parse_description_only() {
        let comment = "/**\n * Renders a paragraph of text.\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.description, "Renders a paragraph of text.");
        assert!(entry.params.is_empty());
        assert!(entry.returns.is_none());
    }

    #[test]
    fn parse_multiline_description() {
        let comment = "/**\n * First sentence.\n * Second sentence.\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(entry.description.contains("First sentence."), "got: {}", entry.description);
        assert!(entry.description.contains("Second sentence."), "got: {}", entry.description);
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — @param
    // -----------------------------------------------------------------------

    #[test]
    fn parse_single_param() {
        let comment = "/**\n * Say something.\n *\n * @param text the text to say\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.params.len(), 1);
        assert_eq!(entry.params[0].name, "text");
        assert_eq!(entry.params[0].description, "the text to say");
    }

    #[test]
    fn parse_multiple_params() {
        let comment = r#"/**
     * Benchmark with explicit rounds.
     *
     * @param label  a human-readable label
     * @param task   the code to benchmark
     * @param warmupRounds number of warmup iterations
     * @param measureRounds number of measured iterations
     */"#;
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.params.len(), 4);
        assert_eq!(entry.params[0].name, "label");
        assert_eq!(entry.params[1].name, "task");
        assert_eq!(entry.params[2].name, "warmupRounds");
        assert_eq!(entry.params[3].name, "measureRounds");
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — @return
    // -----------------------------------------------------------------------

    #[test]
    fn parse_return_tag() {
        let comment = "/**\n * Gets the name.\n *\n * @return the name string\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.returns.as_deref(), Some("the name string"));
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — @throws
    // -----------------------------------------------------------------------

    #[test]
    fn parse_throws_tag() {
        let comment = r#"/**
     * Risky operation.
     *
     * @throws IllegalStateException if not ready
     */"#;
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.throws.len(), 1);
        assert_eq!(entry.throws[0].exception, "IllegalStateException");
        assert!(
            entry.throws[0].description.contains("if not ready"),
            "got: {}",
            entry.throws[0].description
        );
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — @since
    // -----------------------------------------------------------------------

    #[test]
    fn parse_since_tag() {
        let comment = "/**\n * Some feature.\n *\n * @since 1.0.0\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.since.as_deref(), Some("1.0.0"));
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — @deprecated
    // -----------------------------------------------------------------------

    #[test]
    fn parse_deprecated_tag() {
        let comment = "/**\n * Old API.\n *\n * @deprecated use newMethod() instead\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(entry.deprecated.is_some(), "deprecated should be set");
        assert!(
            entry.deprecated.as_deref().unwrap().contains("newMethod"),
            "got: {:?}",
            entry.deprecated
        );
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — combined
    // -----------------------------------------------------------------------

    #[test]
    fn parse_all_tags_combined() {
        let comment = r#"/**
     * Full-featured method.
     *
     * @param input the input value
     * @return the transformed value
     * @throws NullPointerException if input is null
     * @since 2.0.0
     * @deprecated use betterMethod() instead
     */"#;
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(!entry.description.is_empty());
        assert_eq!(entry.params.len(), 1);
        assert_eq!(entry.params[0].name, "input");
        assert!(entry.returns.is_some());
        assert_eq!(entry.throws.len(), 1);
        assert!(entry.since.is_some());
        assert!(entry.deprecated.is_some());
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment — edge cases
    // -----------------------------------------------------------------------

    #[test]
    fn parse_empty_comment_returns_none() {
        // A comment with only delimiters and no meaningful content
        let comment = "/** */";
        // Should return None because description, params, returns, throws are all empty
        assert!(parse_javadoc_comment(comment).is_none());
    }

    #[test]
    fn parse_non_javadoc_comment_returns_none() {
        // A block comment without ** prefix — should still parse but have no content
        // (tree-sitter-javadoc receives the raw text, so /* */ is valid input)
        let comment = "/* not a javadoc */";
        // No meaningful content → None
        assert!(parse_javadoc_comment(comment).is_none());
    }

    #[test]
    fn parse_only_see_tag_returns_none() {
        // @see alone doesn't count as "meaningful content" in our definition
        let comment = "/**\n * @see SomeOtherClass\n */";
        // description, params, returns, throws are all empty → None
        assert!(parse_javadoc_comment(comment).is_none());
    }

    // -----------------------------------------------------------------------
    // extract_from_source — integration against synthetic Java source
    // -----------------------------------------------------------------------

    const SIMPLE_JAVA: &[u8] = br#"
package com.example;

public class MyService {

    /**
     * Greets someone.
     *
     * @param name the name of the person
     * @return a greeting
     */
    public String greet(String name) {
        return "Hello, " + name;
    }

    /**
     * Constructs a new service.
     *
     * @param config configuration object
     */
    public MyService(Config config) {
    }

    /* Not a Javadoc comment - should be ignored */
    public void notDocumented() {
    }

    // Non-block comment
    public void alsoIgnored() {
    }
}
"#;

    #[test]
    fn extract_finds_method_and_constructor() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        assert_eq!(entries.len(), 2, "expected method + constructor, got: {:?}", entries.iter().map(|(k,_)| k).collect::<Vec<_>>());

        let keys: Vec<&str> = entries.iter().map(|(k, _)| k.as_str()).collect();
        assert!(keys.contains(&"com.example.MyService#greet"), "keys: {:?}", keys);
        assert!(keys.contains(&"com.example.MyService#MyService"), "keys: {:?}", keys);
    }

    #[test]
    fn extract_method_has_correct_data() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        let greet = entries
            .iter()
            .find(|(k, _)| k.ends_with("#greet"))
            .map(|(_, v)| v)
            .expect("greet not found");

        assert!(greet.description.contains("Greets"), "desc: {}", greet.description);
        assert_eq!(greet.params.len(), 1);
        assert_eq!(greet.params[0].name, "name");
        assert_eq!(greet.returns.as_deref(), Some("a greeting"));
    }

    #[test]
    fn extract_ignores_non_javadoc_comment() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        let keys: Vec<&str> = entries.iter().map(|(k, _)| k.as_str()).collect();
        assert!(!keys.contains(&"com.example.MyService#notDocumented"), "keys: {:?}", keys);
        assert!(!keys.contains(&"com.example.MyService#alsoIgnored"), "keys: {:?}", keys);
    }

    #[test]
    fn extract_default_package() {
        let src = b"public class Bare {\n    /** Says hi. */\n    public void hi() {}\n}";
        let entries = extract_from_source(src, Path::new("Bare.java"));
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].0, "Bare#hi");
    }

    #[test]
    fn extract_empty_source_returns_empty() {
        let entries = extract_from_source(b"", Path::new("Empty.java"));
        assert!(entries.is_empty());
    }

    #[test]
    fn extract_invalid_utf8_is_skipped() {
        // Parser should handle gracefully and return empty
        let bad_bytes = b"\xff\xfe public class X {}";
        let entries = extract_from_source(bad_bytes, Path::new("X.java"));
        // Either empty or contains no panics — just must not crash
        let _ = entries;
    }

    // -----------------------------------------------------------------------
    // Integration: extract_all on the actual dtr-core source tree
    // -----------------------------------------------------------------------

    #[test]
    fn extract_all_dtr_core_produces_known_entries() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()    // scripts/rust
            .unwrap()
            .parent()    // scripts
            .unwrap()
            .parent()    // project root
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            // Running outside the DTR project — skip gracefully
            eprintln!("Skipping dtr-core integration test: {:?} not found", source_dir);
            return;
        }

        let entries = extract_all(&source_dir);

        // Must find a substantial number of entries
        assert!(
            entries.len() > 50,
            "expected >50 entries from dtr-core, got {}",
            entries.len()
        );

        // Spot-check: RenderMachineCommands#say must be present with correct data
        let key = "io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#say";
        let say = entries.get(key).unwrap_or_else(|| panic!("missing key: {}", key));
        assert!(
            say.description.contains("paragraph"),
            "say description: {}",
            say.description
        );
        assert_eq!(say.params.len(), 1, "say should have 1 @param");
        assert_eq!(say.params[0].name, "text");

        // Spot-check: sayBenchmark (overloaded — 4-arg version has 4 params)
        let bench_key = "io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayBenchmark";
        let bench = entries.get(bench_key).expect("sayBenchmark missing");
        // Last writer wins for overloads; the 4-arg version has 4 params
        assert!(
            bench.params.len() >= 2,
            "sayBenchmark should have params, got: {:?}",
            bench.params
        );

        // Spot-check: sayNextSection has headline param
        let section_key = "io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayNextSection";
        let section = entries.get(section_key).expect("sayNextSection missing");
        assert_eq!(section.params.len(), 1);
        assert_eq!(section.params[0].name, "headline");
    }
}
