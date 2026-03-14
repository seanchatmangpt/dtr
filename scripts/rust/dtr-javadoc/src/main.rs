use clap::Parser;
use rayon::prelude::*;
use tree_sitter::StreamingIterator;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::{Path, PathBuf};
use walkdir::WalkDir;

#[derive(Parser)]
#[command(about = "Extract Javadoc from Java source files into JSON")]
struct Args {
    /// Source directory to scan (e.g. src/main/java)
    #[arg(short, long, default_value = "src/main/java")]
    source: PathBuf,

    /// Output JSON file
    #[arg(short, long, default_value = "docs/meta/javadoc.json")]
    output: PathBuf,
}

#[derive(Serialize, Deserialize, Debug, Default)]
struct JavadocEntry {
    description: String,
    params: Vec<ParamDoc>,
    #[serde(skip_serializing_if = "Option::is_none")]
    returns: Option<String>,
    throws: Vec<ThrowsDoc>,
    #[serde(skip_serializing_if = "Option::is_none")]
    since: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    deprecated: Option<String>,
    see: Vec<String>,
}

#[derive(Serialize, Deserialize, Debug)]
struct ParamDoc {
    name: String,
    description: String,
}

#[derive(Serialize, Deserialize, Debug)]
struct ThrowsDoc {
    exception: String,
    description: String,
}

fn main() -> anyhow::Result<()> {
    let args = Args::parse();

    // Collect all .java files
    let files: Vec<PathBuf> = WalkDir::new(&args.source)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().map_or(false, |ext| ext == "java"))
        .map(|e| e.path().to_owned())
        .collect();

    eprintln!("dtr-javadoc: scanning {} .java files in {}", files.len(), args.source.display());

    // Parse in parallel
    let all_entries: Vec<(String, JavadocEntry)> = files
        .par_iter()
        .flat_map(|path| extract_from_file(path))
        .collect();

    // Collect into map (last writer wins for overloaded methods sharing a name)
    let results: HashMap<String, JavadocEntry> = all_entries.into_iter().collect();

    // Emit to output path
    if let Some(parent) = args.output.parent() {
        std::fs::create_dir_all(parent)?;
    }
    let json = serde_json::to_string_pretty(&results)?;
    std::fs::write(&args.output, json)?;

    eprintln!(
        "dtr-javadoc: extracted {} entries → {}",
        results.len(),
        args.output.display()
    );

    Ok(())
}

/// Scan source bytes for `package foo.bar.baz;` and return `foo.bar.baz`.
fn derive_package(source: &[u8]) -> String {
    let text = std::str::from_utf8(source).unwrap_or("");
    for line in text.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("package ") && trimmed.ends_with(';') {
            return trimmed["package ".len()..trimmed.len() - 1].trim().to_string();
        }
    }
    String::new()
}

/// Extract the file stem (class name) from a path.
fn class_name_from_path(path: &Path) -> &str {
    path.file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Unknown")
}

/// Tree-sitter query to find `(block_comment) . (method_declaration)` pairs.
/// The `.` anchor means the comment must be the immediate preceding sibling.
const JAVA_QUERY: &str = r#"
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

fn extract_from_file(path: &Path) -> Vec<(String, JavadocEntry)> {
    let source = match std::fs::read(path) {
        Ok(b) => b,
        Err(_) => return vec![],
    };

    let package = derive_package(&source);
    let class_name = class_name_from_path(path);

    // Build fully-qualified class name prefix
    let fqcn = if package.is_empty() {
        class_name.to_string()
    } else {
        format!("{}.{}", package, class_name)
    };

    // Pass 1: tree-sitter-java — find doc comment / method declaration pairs
    let mut java_parser = tree_sitter::Parser::new();
    if java_parser
        .set_language(&tree_sitter_java::LANGUAGE.into())
        .is_err()
    {
        return vec![];
    }

    let java_tree = match java_parser.parse(&source, None) {
        Some(t) => t,
        None => return vec![],
    };

    let java_query = match tree_sitter::Query::new(
        &tree_sitter_java::LANGUAGE.into(),
        JAVA_QUERY,
    ) {
        Ok(q) => q,
        Err(_) => return vec![],
    };

    let doc_idx = java_query
        .capture_names()
        .iter()
        .position(|n| *n == "doc")
        .unwrap_or(0);
    let name_idx = java_query
        .capture_names()
        .iter()
        .position(|n| *n == "method_name")
        .unwrap_or(1);

    let mut cursor = tree_sitter::QueryCursor::new();
    let mut qmatches = cursor.matches(&java_query, java_tree.root_node(), source.as_slice());

    let mut results = Vec::new();

    while let Some(m) = qmatches.next() {
        let doc_node = m.captures.iter().find(|c| c.index as usize == doc_idx);
        let name_node = m.captures.iter().find(|c| c.index as usize == name_idx);

        if let (Some(doc), Some(name)) = (doc_node, name_node) {
            let comment_text = match doc.node.utf8_text(&source) {
                Ok(t) => t,
                Err(_) => continue,
            };
            let method_name = match name.node.utf8_text(&source) {
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

/// Pass 2: parse `/** ... */` comment text with tree-sitter-javadoc.
///
/// CST structure (from tree-sitter-javadoc grammar):
/// - document
///   - "/**" (anonymous)
///   - description (named) — text before first block tag
///   - param_tag (named): tag_name + identifier + description
///   - return_tag (named): tag_name + description
///   - throws_tag (named): tag_name + type? + description
///   - since_tag, deprecated_tag, see_tag ... similar patterns
///   - "*/" (anonymous)
fn parse_javadoc_comment(comment: &str) -> Option<JavadocEntry> {
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
                // Children: tag_name, identifier (param name), description
                let name = child_text_by_kind(&child, src, "identifier");
                let desc = child_text_by_kind(&child, src, "description");
                if !name.is_empty() {
                    entry.params.push(ParamDoc {
                        name,
                        description: clean_comment_text(&desc),
                    });
                }
            }
            "return_tag" | "returns_tag" => {
                let desc = child_text_by_kind(&child, src, "description");
                let cleaned = clean_comment_text(&desc);
                if !cleaned.is_empty() {
                    entry.returns = Some(cleaned);
                }
            }
            "throws_tag" | "exception_tag" => {
                // Children: tag_name, type/identifier (exception), description
                let exc = child_text_by_kind(&child, src, "type_identifier")
                    .or_else(|| child_text_by_kind(&child, src, "identifier"))
                    .unwrap_or_default();
                let desc = child_text_by_kind(&child, src, "description");
                if !exc.is_empty() {
                    entry.throws.push(ThrowsDoc {
                        exception: exc,
                        description: clean_comment_text(&desc),
                    });
                }
            }
            "since_tag" => {
                let desc = child_text_by_kind(&child, src, "description");
                let cleaned = clean_comment_text(&desc);
                if !cleaned.is_empty() {
                    entry.since = Some(cleaned);
                }
            }
            "deprecated_tag" => {
                let desc = child_text_by_kind(&child, src, "description");
                let cleaned = clean_comment_text(&desc);
                if !cleaned.is_empty() {
                    entry.deprecated = Some(cleaned);
                }
            }
            "see_tag" => {
                let val = child.utf8_text(src).unwrap_or("");
                // Strip "@see " prefix
                let v = val.trim_start_matches("@see").trim().to_string();
                if !v.is_empty() {
                    entry.see.push(v);
                }
            }
            _ => {} // "/**", "*/", whitespace, unknown tags — skip
        }
    }

    // Only return if there's meaningful content
    if entry.description.is_empty()
        && entry.params.is_empty()
        && entry.returns.is_none()
        && entry.throws.is_empty()
    {
        return None;
    }

    Some(entry)
}

/// Find the first child of `node` with the given kind and return its text.
fn child_text_by_kind(node: &tree_sitter::Node, source: &[u8], kind: &str) -> String {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == kind {
            return child.utf8_text(source).unwrap_or("").to_string();
        }
    }
    String::new()
}

// Helper that returns Option<String> for use in or_else chains
trait ToOption {
    fn or_else(self, f: impl FnOnce() -> String) -> String;
    fn unwrap_or_default(self) -> String;
}

impl ToOption for String {
    fn or_else(self, f: impl FnOnce() -> String) -> String {
        if self.is_empty() { f() } else { self }
    }
    fn unwrap_or_default(self) -> String { self }
}

/// Strip Javadoc comment delimiters and leading `*` from text.
fn clean_comment_text(text: &str) -> String {
    text.lines()
        .map(|line| {
            let t = line.trim();
            // Strip leading * from continuation lines
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
