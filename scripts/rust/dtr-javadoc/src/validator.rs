/// TPS Jidoka violation detection.
///
/// This module enforces the TPS rule "stop the line on every missing doc"
/// by detecting public types and methods that lack an immediately preceding
/// Javadoc comment.

use std::path::Path;
use super::error::{DocViolation, ViolationKind};
use super::parser::child_text_by_kind;

/// Find all public, non-@Override methods and top-level type declarations that
/// lack an immediately preceding `/** ... */` Javadoc comment.
///
/// TPS rule: stop the line on every missing doc. The binary exits 1 if this
/// list is non-empty.
#[must_use]
pub fn find_violations(
    source: &[u8],
    root: tree_sitter::Node,
    fqcn: &str,
    path: &Path,
) -> Vec<DocViolation> {
    let mut violations = Vec::new();

    // Walk top-level children of the compilation_unit
    let mut cursor = root.walk();
    for child in root.children(&mut cursor) {
        match child.kind() {
            "class_declaration"
            | "interface_declaration"
            | "record_declaration"
            | "enum_declaration" => {
                // Check class-level doc
                if !has_javadoc_predecessor(&child, source) {
                    violations.push(DocViolation {
                        file: path.to_path_buf(),
                        fqcn: fqcn.to_string(),
                        kind: ViolationKind::MissingClassDoc,
                    });
                }
                // Check public non-@Override methods inside this type
                violations.extend(find_method_violations(source, child, fqcn, path));
            }
            _ => {}
        }
    }

    violations
}

/// Walk a type body and report public non-@Override methods without Javadoc.
fn find_method_violations(
    source: &[u8],
    type_node: tree_sitter::Node,
    fqcn: &str,
    path: &Path,
) -> Vec<DocViolation> {
    let mut violations = Vec::new();
    let body_kinds = ["class_body", "interface_body", "record_body", "enum_body"];

    let mut tc = type_node.walk();
    for child in type_node.children(&mut tc) {
        if body_kinds.contains(&child.kind()) {
            let mut bc = child.walk();
            for member in child.children(&mut bc) {
                match member.kind() {
                    "method_declaration" | "constructor_declaration" => {
                        if is_public(&member, source)
                            && !has_override_annotation(&member, source)
                            && !has_javadoc_predecessor(&member, source)
                        {
                            let method_name = method_name_of(&member, source);
                            violations.push(DocViolation {
                                file: path.to_path_buf(),
                                fqcn: fqcn.to_string(),
                                kind: ViolationKind::MissingMethodDoc {
                                    method: method_name,
                                },
                            });
                        }
                    }
                    // Recurse into inner classes
                    "class_declaration"
                    | "interface_declaration"
                    | "record_declaration"
                    | "enum_declaration" => {
                        let inner_name = child_text_by_kind(&member, source, "identifier")
                            .unwrap_or_else(|| "Inner".to_string());
                        let inner_fqcn = format!("{fqcn}.{inner_name}");
                        violations.extend(find_method_violations(
                            source,
                            member,
                            &inner_fqcn,
                            path,
                        ));
                    }
                    _ => {}
                }
            }
        }
    }

    violations
}

/// Returns true if the node's previous named sibling is a `block_comment`
/// starting with `/**`.
#[must_use]
pub fn has_javadoc_predecessor(node: &tree_sitter::Node, source: &[u8]) -> bool {
    if let Some(prev) = node.prev_named_sibling() {
        if prev.kind() == "block_comment" {
            return prev
                .utf8_text(source)
                .map(|t| t.trim_start().starts_with("/**"))
                .unwrap_or(false);
        }
    }
    false
}

/// Returns true if the method/constructor node has a `public` modifier.
#[must_use]
pub fn is_public(node: &tree_sitter::Node, source: &[u8]) -> bool {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == "modifiers" {
            let text = child.utf8_text(source).unwrap_or("");
            return text.split_whitespace().any(|w| w == "public");
        }
    }
    false
}

/// Returns true if the method node has `@Override` in its modifiers.
#[must_use]
pub fn has_override_annotation(node: &tree_sitter::Node, source: &[u8]) -> bool {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == "modifiers" {
            let mut mc = child.walk();
            for modifier in child.children(&mut mc) {
                if modifier.kind() == "marker_annotation" {
                    let name = child_text_by_kind(&modifier, source, "identifier");
                    if name.as_deref() == Some("Override") {
                        return true;
                    }
                }
            }
        }
    }
    false
}

/// Extract the name identifier from a method or constructor declaration node.
fn method_name_of(node: &tree_sitter::Node, source: &[u8]) -> String {
    child_text_by_kind(node, source, "identifier").unwrap_or_else(|| "<unknown>".to_string())
}
