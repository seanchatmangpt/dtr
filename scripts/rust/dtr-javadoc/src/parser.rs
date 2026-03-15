//! Javadoc comment parsing via tree-sitter-javadoc.
//!
//! This module uses the tree-sitter-javadoc grammar to parse `/** ... */` comments
//! into structured `JavadocEntry` types with description, parameters, returns,
//! throws, and meta-tags.

use super::model::{JavadocEntry, ParamDoc, ThrowsDoc};
use super::util::clean_comment_text;

/// Parse a `/** ... */` comment into a structured `JavadocEntry`.
///
/// The tree-sitter-javadoc grammar produces a `document` node with typed
/// children: `description`, `param_tag`, `return_tag`, `throws_tag`, etc.
/// This is an injection grammar — it expects to parse the comment in isolation,
/// not embedded in Java source.
///
/// Returns `None` if the comment has no description, params, returns, or throws.
#[must_use]
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
                        description: desc.map(|d| clean_comment_text(&d)).unwrap_or_default(),
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
                let exc = {
                    let mut c = child.walk();
                    let outer_result = child
                        .children(&mut c)
                        .find(|n| n.kind() == "type")
                        .and_then(|type_node| {
                            let mut tc = type_node.walk();
                            let inner_result = type_node
                                .children(&mut tc)
                                .find(|n| n.kind() == "identifier")
                                .and_then(|id_node| id_node.utf8_text(src).ok())
                                .map(|s| s.trim().to_string());
                            inner_result
                        })
                        .or_else(|| {
                            child_text_by_kind(&child, src, "identifier")
                                .map(|s| s.trim().to_string())
                        });
                    outer_result
                };
                let desc = child_text_by_kind(&child, src, "description");
                if let Some(exc) = exc {
                    let exc_trimmed = exc.trim().to_string();
                    if !exc_trimmed.is_empty() {
                        entry.throws.push(ThrowsDoc {
                            exception: exc_trimmed,
                            description: desc.map(|d| clean_comment_text(&d)).unwrap_or_default(),
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

/// Find the first named child of `node` with the given kind and return its text.
#[must_use]
pub fn child_text_by_kind(node: &tree_sitter::Node, source: &[u8], kind: &str) -> Option<String> {
    let mut cursor = node.walk();
    for child in node.children(&mut cursor) {
        if child.kind() == kind {
            return Some(child.utf8_text(source).unwrap_or("").to_string());
        }
    }
    None
}
