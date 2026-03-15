//! Javadoc extraction from Java source files.
//!
//! This module handles parsing Java source files, extracting method-level and
//! module-level documentation via tree-sitter queries, and assembling per-file
//! results.

use std::collections::HashMap;
use std::path::Path;
use rayon::prelude::*;
use tree_sitter::StreamingIterator;
use walkdir::WalkDir;

use super::model::{JavadocEntry, ModuleDoc, FileDocResult};
use super::parser::parse_javadoc_comment;
use super::util::{derive_package, class_name_from_path};
use super::validator::find_violations;

/// Finds `(block_comment) . (method_declaration | constructor_declaration)` pairs.
/// The `.` anchor enforces immediate adjacency — the Javadoc spec.
pub const JAVA_METHOD_QUERY: &str = r#"
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

/// Finds `(block_comment) . (class|interface|record|enum declaration)` pairs.
pub const JAVA_CLASS_QUERY: &str = r#"
(
  (block_comment) @doc
  .
  (class_declaration name: (identifier) @class_name) @decl
  (#match? @doc "^\\/\\*\\*")
)

(
  (block_comment) @doc
  .
  (interface_declaration name: (identifier) @class_name) @decl
  (#match? @doc "^\\/\\*\\*")
)

(
  (block_comment) @doc
  .
  (record_declaration name: (identifier) @class_name) @decl
  (#match? @doc "^\\/\\*\\*")
)

(
  (block_comment) @doc
  .
  (enum_declaration name: (identifier) @class_name) @decl
  (#match? @doc "^\\/\\*\\*")
)
"#;

/// Process all `.java` files under `source_dir` in parallel.
/// Returns `(method_docs, module_docs, violations)`.
#[must_use]
pub fn process_all(
    source_dir: &Path,
) -> (
    HashMap<String, JavadocEntry>,
    Vec<ModuleDoc>,
    Vec<super::error::DocViolation>,
) {
    let files: Vec<std::path::PathBuf> = WalkDir::new(source_dir)
        .into_iter()
        .filter_map(Result::ok)
        .filter(|e| e.path().extension().is_some_and(|ext| ext == "java"))
        .map(|e| e.path().to_owned())
        .collect();

    let results: Vec<FileDocResult> = files
        .par_iter()
        .filter_map(|path| {
            let source = std::fs::read(path).ok()?;
            Some(process_file_source(&source, path))
        })
        .collect();

    let mut method_docs: HashMap<String, JavadocEntry> = HashMap::new();
    let mut module_docs: Vec<ModuleDoc> = Vec::new();
    let mut violations: Vec<super::error::DocViolation> = Vec::new();

    for r in results {
        for (k, v) in r.method_docs {
            method_docs.insert(k, v);
        }
        if let Some(md) = r.module_doc {
            module_docs.push(md);
        }
        violations.extend(r.violations);
    }

    // Sort violations for deterministic output
    violations.sort_by(|a, b| a.fqcn.cmp(&b.fqcn));

    (method_docs, module_docs, violations)
}

/// Kept for backward compatibility and tests.
#[must_use]
pub fn extract_all(source_dir: &Path) -> HashMap<String, JavadocEntry> {
    let (method_docs, _, _) = process_all(source_dir);
    method_docs
}

/// Process a single file from disk.
#[must_use]
pub fn extract_from_file(path: &Path) -> Vec<(String, JavadocEntry)> {
    let Ok(source) = std::fs::read(path) else { return vec![] };
    extract_from_source(&source, path)
}

/// Extract method-level docs from an in-memory source buffer (used by tests).
#[must_use]
pub fn extract_from_source(source: &[u8], path: &Path) -> Vec<(String, JavadocEntry)> {
    process_file_source(source, path).method_docs
}

/// Full per-file processing: module doc + method docs + violations.
#[must_use]
pub fn process_file_source(source: &[u8], path: &Path) -> FileDocResult {
    let package = derive_package(source);
    let class_name = class_name_from_path(path);
    let fqcn = if package.is_empty() {
        class_name.to_string()
    } else {
        format!("{package}.{class_name}")
    };

    let mut java_parser = tree_sitter::Parser::new();
    if java_parser
        .set_language(&tree_sitter_java::LANGUAGE.into())
        .is_err()
    {
        return FileDocResult {
            module_doc: None,
            method_docs: vec![],
            violations: vec![],
        };
    }

    let Some(java_tree) = java_parser.parse(source, None) else {
        return FileDocResult {
            module_doc: None,
            method_docs: vec![],
            violations: vec![],
        }
    };

    let root = java_tree.root_node();

    // --- Method-level extraction ---
    let method_docs = extract_method_docs(source, root, &fqcn);

    // --- Class-level extraction ---
    let module_doc = extract_module_doc(source, root, &fqcn, &package, class_name, &method_docs);

    // --- Violation detection (TPS Jidoka) ---
    let violations = find_violations(source, root, &fqcn, path);

    FileDocResult {
        module_doc,
        method_docs,
        violations,
    }
}

/// Extract method-level documentation from a tree-sitter AST.
fn extract_method_docs(
    source: &[u8],
    root: tree_sitter::Node,
    fqcn: &str,
) -> Vec<(String, JavadocEntry)> {
    let Ok(java_query) =
        tree_sitter::Query::new(&tree_sitter_java::LANGUAGE.into(), JAVA_METHOD_QUERY) else {
        return vec![]
    };

    let capture_names = java_query.capture_names();
    let doc_idx = capture_names.iter().position(|n| *n == "doc").unwrap_or(0);
    let name_idx = capture_names
        .iter()
        .position(|n| *n == "method_name")
        .unwrap_or(1);

    let mut cursor = tree_sitter::QueryCursor::new();
    let mut qmatches = cursor.matches(&java_query, root, source);
    let mut results = Vec::new();

    while let Some(m) = <_ as StreamingIterator>::next(&mut qmatches) {
        let doc_node = m.captures.iter().find(|c| c.index as usize == doc_idx);
        let name_node = m.captures.iter().find(|c| c.index as usize == name_idx);

        if let (Some(doc), Some(name)) = (doc_node, name_node) {
            let Ok(comment_text) = doc.node.utf8_text(source) else { continue };
            let Ok(method_name) = name.node.utf8_text(source) else { continue };
            let key = format!("{fqcn}#{method_name}");
            if let Some(entry) = parse_javadoc_comment(comment_text) {
                results.push((key, entry));
            }
        }
    }

    results
}

/// Extract module-level (class/interface/record/enum) documentation.
fn extract_module_doc(
    source: &[u8],
    root: tree_sitter::Node,
    fqcn: &str,
    package: &str,
    class_name: &str,
    method_docs: &[(String, JavadocEntry)],
) -> Option<ModuleDoc> {
    let Ok(java_query) =
        tree_sitter::Query::new(&tree_sitter_java::LANGUAGE.into(), JAVA_CLASS_QUERY) else {
        return None
    };

    let capture_names = java_query.capture_names();
    let doc_idx = capture_names.iter().position(|n| *n == "doc").unwrap_or(0);
    let decl_idx = capture_names.iter().position(|n| *n == "decl").unwrap_or(2);

    let mut cursor = tree_sitter::QueryCursor::new();
    let mut qmatches = cursor.matches(&java_query, root, source);

    while let Some(m) = <_ as StreamingIterator>::next(&mut qmatches) {
        let doc_node = m.captures.iter().find(|c| c.index as usize == doc_idx);
        let decl_node = m.captures.iter().find(|c| c.index as usize == decl_idx);

        if let (Some(doc), Some(decl)) = (doc_node, decl_node) {
            let Ok(comment_text) = doc.node.utf8_text(source) else { continue };

            let entry = parse_javadoc_comment(comment_text);
            let signature = extract_type_signature(decl.node, source);
            let kind = decl
                .node
                .kind()
                .trim_end_matches("_declaration")
                .to_string();

            // Collect method names for the code block summary
            let method_names: Vec<String> = method_docs
                .iter()
                .filter(|(k, _)| k.starts_with(&format!("{fqcn}#")))
                .map(|(k, _)| k.split('#').nth(1).unwrap_or("").to_string())
                .collect();

            let module = ModuleDoc {
                fqcn: fqcn.to_string(),
                simple_name: class_name.to_string(),
                package: package.to_string(),
                description: entry
                    .as_ref()
                    .map_or(String::new(), |e| e.description.clone()),
                since: entry.as_ref().and_then(|e| e.since.clone()),
                deprecated: entry.as_ref().and_then(|e| e.deprecated.clone()),
                see: entry.as_ref().map_or(vec![], |e| e.see.clone()),
                signature,
                kind,
                method_count: method_names.len(),
                method_names,
            };

            return Some(module);
        }
    }

    None
}

/// Extract the type declaration header (everything before the `{` body).
#[must_use]
pub fn extract_type_signature(decl_node: tree_sitter::Node, source: &[u8]) -> String {
    let body_kinds = ["class_body", "interface_body", "record_body", "enum_body"];
    let body_start = {
        let mut cursor = decl_node.walk();
        let result = decl_node
            .children(&mut cursor)
            .find(|n| body_kinds.contains(&n.kind()))
            .map_or(decl_node.end_byte(), |n| n.start_byte());
        result
    };

    let sig_bytes = &source[decl_node.start_byte()..body_start];
    std::str::from_utf8(sig_bytes)
        .unwrap_or("")
        .split_whitespace()
        .collect::<Vec<_>>()
        .join(" ")
        .trim()
        .to_string()
}
