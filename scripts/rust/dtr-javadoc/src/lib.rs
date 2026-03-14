use rayon::prelude::*;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::fmt::Write as FmtWrite;
use std::path::{Path, PathBuf};
use tree_sitter::StreamingIterator;
use walkdir::WalkDir;

// ============================================================================
// Data model — method-level
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
// Data model — module (class/interface) level
// ============================================================================

/// Documentation for a top-level Java type (class, interface, record, enum).
#[derive(Debug, Default)]
pub struct ModuleDoc {
    /// Fully qualified class name, e.g. `io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands`
    pub fqcn: String,
    /// Simple class name
    pub simple_name: String,
    /// Package
    pub package: String,
    /// The cleaned Javadoc description (license headers stripped)
    pub description: String,
    /// `@since` value, if present
    pub since: Option<String>,
    /// `@deprecated` text, if present
    pub deprecated: Option<String>,
    /// `@see` references
    pub see: Vec<String>,
    /// Java signature of the type declaration (everything before the opening `{`)
    pub signature: String,
    /// Kind: "class", "interface", "record", "enum"
    pub kind: String,
    /// Method count (for the summary comment in the code block)
    pub method_count: usize,
    /// Method names (first 10, for the summary)
    pub method_names: Vec<String>,
}

// ============================================================================
// TPS violation model — Jidoka: stop the line on missing docs
// ============================================================================

/// A documentation violation: a public type or method missing a Javadoc comment.
/// The build fails if any violations are present.
#[derive(Debug, Clone)]
pub struct DocViolation {
    pub file: PathBuf,
    pub fqcn: String,
    pub kind: ViolationKind,
}

#[derive(Debug, Clone)]
pub enum ViolationKind {
    MissingClassDoc,
    MissingMethodDoc { method: String },
}

impl std::fmt::Display for DocViolation {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let file = self.file.to_string_lossy();
        match &self.kind {
            ViolationKind::MissingClassDoc => {
                write!(f, "  MISSING CLASS DOC   {}  ({})", self.fqcn, file)
            }
            ViolationKind::MissingMethodDoc { method } => {
                write!(f, "  MISSING METHOD DOC  {}#{}  ({})", self.fqcn, method, file)
            }
        }
    }
}

// ============================================================================
// Per-file result
// ============================================================================

pub struct FileDocResult {
    pub module_doc: Option<ModuleDoc>,
    pub method_docs: Vec<(String, JavadocEntry)>,
    pub violations: Vec<DocViolation>,
}

// ============================================================================
// Tree-sitter queries
// ============================================================================

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

// ============================================================================
// Public API — batch processing
// ============================================================================

/// Process all `.java` files under `source_dir` in parallel.
/// Returns `(method_docs, module_docs, violations)`.
pub fn process_all(
    source_dir: &Path,
) -> (HashMap<String, JavadocEntry>, Vec<ModuleDoc>, Vec<DocViolation>) {
    let files: Vec<PathBuf> = WalkDir::new(source_dir)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.path().extension().map_or(false, |ext| ext == "java"))
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
    let mut violations: Vec<DocViolation> = Vec::new();

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
pub fn extract_all(source_dir: &Path) -> HashMap<String, JavadocEntry> {
    let (method_docs, _, _) = process_all(source_dir);
    method_docs
}

// ============================================================================
// Per-file processing
// ============================================================================

/// Process a single file from disk.
pub fn extract_from_file(path: &Path) -> Vec<(String, JavadocEntry)> {
    let source = match std::fs::read(path) {
        Ok(b) => b,
        Err(_) => return vec![],
    };
    extract_from_source(&source, path)
}

/// Extract method-level docs from an in-memory source buffer (used by tests).
pub fn extract_from_source(source: &[u8], path: &Path) -> Vec<(String, JavadocEntry)> {
    process_file_source(source, path).method_docs
}

/// Full per-file processing: module doc + method docs + violations.
pub fn process_file_source(source: &[u8], path: &Path) -> FileDocResult {
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
        return FileDocResult { module_doc: None, method_docs: vec![], violations: vec![] };
    }

    let java_tree = match java_parser.parse(source, None) {
        Some(t) => t,
        None => return FileDocResult { module_doc: None, method_docs: vec![], violations: vec![] },
    };

    let root = java_tree.root_node();

    // --- Method-level extraction ---
    let method_docs = extract_method_docs(source, root, &fqcn);

    // --- Class-level extraction ---
    let module_doc = extract_module_doc(source, root, &fqcn, &package, class_name, &method_docs);

    // --- Violation detection (TPS Jidoka) ---
    let violations = find_violations(source, root, &fqcn, path);

    FileDocResult { module_doc, method_docs, violations }
}

// ============================================================================
// Method-level extraction
// ============================================================================

fn extract_method_docs(
    source: &[u8],
    root: tree_sitter::Node,
    fqcn: &str,
) -> Vec<(String, JavadocEntry)> {
    let java_query =
        match tree_sitter::Query::new(&tree_sitter_java::LANGUAGE.into(), JAVA_METHOD_QUERY) {
            Ok(q) => q,
            Err(_) => return vec![],
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
// Module-level extraction
// ============================================================================

fn extract_module_doc(
    source: &[u8],
    root: tree_sitter::Node,
    fqcn: &str,
    package: &str,
    class_name: &str,
    method_docs: &[(String, JavadocEntry)],
) -> Option<ModuleDoc> {
    let java_query =
        match tree_sitter::Query::new(&tree_sitter_java::LANGUAGE.into(), JAVA_CLASS_QUERY) {
            Ok(q) => q,
            Err(_) => return None,
        };

    let capture_names = java_query.capture_names();
    let doc_idx = capture_names.iter().position(|n| *n == "doc").unwrap_or(0);
    let decl_idx = capture_names.iter().position(|n| *n == "decl").unwrap_or(2);

    let mut cursor = tree_sitter::QueryCursor::new();
    let mut qmatches = cursor.matches(&java_query, root, source);

    while let Some(m) = qmatches.next() {
        let doc_node = m.captures.iter().find(|c| c.index as usize == doc_idx);
        let decl_node = m.captures.iter().find(|c| c.index as usize == decl_idx);

        if let (Some(doc), Some(decl)) = (doc_node, decl_node) {
            let comment_text = match doc.node.utf8_text(source) {
                Ok(t) => t,
                Err(_) => continue,
            };

            let entry = parse_javadoc_comment(comment_text);
            let signature = extract_type_signature(decl.node, source);
            let kind = decl.node.kind().trim_end_matches("_declaration").to_string();

            // Collect method names for the code block summary
            let method_names: Vec<String> = method_docs
                .iter()
                .filter(|(k, _)| k.starts_with(&format!("{}#", fqcn)))
                .map(|(k, _)| k.split('#').nth(1).unwrap_or("").to_string())
                .collect();

            let module = ModuleDoc {
                fqcn: fqcn.to_string(),
                simple_name: class_name.to_string(),
                package: package.to_string(),
                description: entry.as_ref().map_or(String::new(), |e| e.description.clone()),
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
pub fn extract_type_signature(decl_node: tree_sitter::Node, source: &[u8]) -> String {
    let body_kinds = ["class_body", "interface_body", "record_body", "enum_body"];
    let body_start = {
        let mut cursor = decl_node.walk();
        decl_node
            .children(&mut cursor)
            .find(|n| body_kinds.contains(&n.kind()))
            .map(|n| n.start_byte())
            .unwrap_or(decl_node.end_byte())
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

// ============================================================================
// TPS Jidoka: violation detection
// ============================================================================

/// Find all public, non-@Override methods and top-level type declarations that
/// lack an immediately preceding `/** ... */` Javadoc comment.
///
/// TPS rule: stop the line on every missing doc. The binary exits 1 if this
/// list is non-empty.
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
                                kind: ViolationKind::MissingMethodDoc { method: method_name },
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
                        let inner_fqcn = format!("{}.{}", fqcn, inner_name);
                        violations.extend(find_method_violations(
                            source, member, &inner_fqcn, path,
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

// ============================================================================
// Markdown generation
// ============================================================================

/// Render a module's documentation as a markdown string.
pub fn render_module_markdown(
    module: &ModuleDoc,
    method_docs: &HashMap<String, JavadocEntry>,
) -> String {
    let mut out = String::new();

    // ── Header ──
    writeln!(out, "# `{}`", module.simple_name).unwrap();
    writeln!(out).unwrap();
    writeln!(out, "> **Package:** `{}`  ", module.package).unwrap();
    if let Some(since) = &module.since {
        writeln!(out, "> **Since:** `{}`  ", since).unwrap();
    }
    if let Some(dep) = &module.deprecated {
        writeln!(out, "> [!WARNING]  ").unwrap();
        writeln!(out, "> **Deprecated:** {}  ", dep).unwrap();
    }
    writeln!(out).unwrap();

    // ── Description ──
    if !module.description.is_empty() {
        writeln!(out, "{}", module.description).unwrap();
        writeln!(out).unwrap();
    }

    // ── Java code block: class signature ──
    writeln!(out, "```java").unwrap();
    writeln!(out, "{} {{", module.signature).unwrap();
    if module.method_count > 0 {
        let names_preview: String = module
            .method_names
            .iter()
            .take(8)
            .cloned()
            .collect::<Vec<_>>()
            .join(", ");
        let suffix = if module.method_count > 8 {
            format!(", ... ({} total)", module.method_count)
        } else {
            String::new()
        };
        writeln!(out, "    // {}{}", names_preview, suffix).unwrap();
    }
    writeln!(out, "}}").unwrap();
    writeln!(out, "```").unwrap();
    writeln!(out).unwrap();

    // ── Methods ──
    let prefix = format!("{}#", module.fqcn);
    let mut methods: Vec<(&str, &JavadocEntry)> = method_docs
        .iter()
        .filter(|(k, _)| k.starts_with(&prefix))
        .map(|(k, v)| (k.split('#').nth(1).unwrap_or(""), v))
        .collect();
    methods.sort_by_key(|(name, _)| *name);

    if !methods.is_empty() {
        writeln!(out, "---").unwrap();
        writeln!(out).unwrap();
        writeln!(out, "## Methods").unwrap();
        writeln!(out).unwrap();

        for (method_name, entry) in methods {
            writeln!(out, "### `{}`", method_name).unwrap();
            writeln!(out).unwrap();

            if !entry.description.is_empty() {
                writeln!(out, "{}", entry.description).unwrap();
                writeln!(out).unwrap();
            }

            if !entry.params.is_empty() {
                writeln!(out, "| Parameter | Description |").unwrap();
                writeln!(out, "| --- | --- |").unwrap();
                for p in &entry.params {
                    writeln!(out, "| `{}` | {} |", p.name, p.description).unwrap();
                }
                writeln!(out).unwrap();
            }

            if let Some(ret) = &entry.returns {
                writeln!(out, "> **Returns:** {}", ret).unwrap();
                writeln!(out).unwrap();
            }

            if !entry.throws.is_empty() {
                writeln!(out, "| Exception | Description |").unwrap();
                writeln!(out, "| --- | --- |").unwrap();
                for t in &entry.throws {
                    writeln!(out, "| `{}` | {} |", t.exception, t.description).unwrap();
                }
                writeln!(out).unwrap();
            }

            if let Some(since) = &entry.since {
                writeln!(out, "> **Since:** {}", since).unwrap();
                writeln!(out).unwrap();
            }

            if let Some(dep) = &entry.deprecated {
                writeln!(out, "> [!WARNING]").unwrap();
                writeln!(out, "> **Deprecated:** {}", dep).unwrap();
                writeln!(out).unwrap();
            }

            writeln!(out, "---").unwrap();
            writeln!(out).unwrap();
        }
    }

    out
}

/// Write one markdown file per module doc to `docs/api/`, using the FQCN as
/// the path (`io/github/.../ClassName.md`).
pub fn write_api_docs(
    module_docs: &[ModuleDoc],
    method_docs: &HashMap<String, JavadocEntry>,
    docs_root: &Path,
) -> anyhow::Result<()> {
    std::fs::create_dir_all(docs_root)?;

    for module in module_docs {
        // Convert FQCN to path: `io.github.seanchatmangpt.Foo` → `io/github/seanchatmangpt/Foo.md`
        let rel_path: PathBuf = module.fqcn.replace('.', "/").into();
        let md_path = docs_root.join(rel_path).with_extension("md");

        if let Some(parent) = md_path.parent() {
            std::fs::create_dir_all(parent)?;
        }

        let content = render_module_markdown(module, method_docs);
        std::fs::write(&md_path, content)?;
    }

    Ok(())
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
                let exc = {
                    let mut c = child.walk();
                    child
                        .children(&mut c)
                        .find(|n| n.kind() == "type")
                        .and_then(|type_node| {
                            let mut tc = type_node.walk();
                            type_node
                                .children(&mut tc)
                                .find(|n| n.kind() == "identifier")
                                .and_then(|id_node| id_node.utf8_text(src).ok())
                                .map(|s| s.trim().to_string())
                        })
                        .or_else(|| {
                            child_text_by_kind(&child, src, "identifier")
                                .map(|s| s.trim().to_string())
                        })
                };
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

/// Find the first named child of `node` with the given kind and return its text.
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

/// Return the file stem (expected class name) from a path.
pub fn class_name_from_path(path: &Path) -> &str {
    path.file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Unknown")
}

/// Strip Javadoc delimiters and leading ` * ` markers. Joins lines into one string.
///
/// License header blocks (Apache License, Copyright notices) are stripped
/// because the Javadoc grammar places them in the description when they appear
/// at the top of a file-level comment. We detect them by presence of
/// "Licensed under the Apache License" or "Copyright" and return empty.
pub fn clean_comment_text(text: &str) -> String {
    // Strip license headers
    if text.contains("Licensed under the Apache License")
        || text.contains("Copyright (C)")
        || text.contains("Copyright 2")
    {
        return String::new();
    }

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
        assert_eq!(
            clean_comment_text(input),
            "A text that will be rendered as a paragraph."
        );
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
        let input = "* Use {@code System.nanoTime()} for timing.";
        let result = clean_comment_text(input);
        assert!(result.contains("{@code System.nanoTime()}"), "got: {}", result);
    }

    #[test]
    fn clean_strips_license_header() {
        let license = "Licensed under the Apache License, Version 2.0 (the \"License\")";
        assert_eq!(clean_comment_text(license), "");
    }

    #[test]
    fn clean_strips_copyright() {
        let copyright = "Copyright (C) 2013 the original author or authors.";
        assert_eq!(clean_comment_text(copyright), "");
    }

    // -----------------------------------------------------------------------
    // derive_package
    // -----------------------------------------------------------------------

    #[test]
    fn package_normal() {
        let src = b"package io.github.seanchatmangpt.dtr.rendermachine;\n\npublic interface Foo {}";
        assert_eq!(
            derive_package(src),
            "io.github.seanchatmangpt.dtr.rendermachine"
        );
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
        let src = b"// packages are cool\npublic class X {}";
        assert_eq!(derive_package(src), "");
    }

    // -----------------------------------------------------------------------
    // class_name_from_path
    // -----------------------------------------------------------------------

    #[test]
    fn class_name_simple() {
        assert_eq!(
            class_name_from_path(Path::new("src/RenderMachine.java")),
            "RenderMachine"
        );
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

    // -----------------------------------------------------------------------
    // parse_javadoc_comment
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
        assert!(
            entry.description.contains("First sentence."),
            "got: {}",
            entry.description
        );
        assert!(
            entry.description.contains("Second sentence."),
            "got: {}",
            entry.description
        );
    }

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
     * Benchmark.
     *
     * @param label  label
     * @param task   task
     * @param warmupRounds warmup
     * @param measureRounds measure
     */"#;
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.params.len(), 4);
        assert_eq!(entry.params[0].name, "label");
        assert_eq!(entry.params[2].name, "warmupRounds");
    }

    #[test]
    fn parse_return_tag() {
        let comment = "/**\n * Gets the name.\n *\n * @return the name string\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.returns.as_deref(), Some("the name string"));
    }

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

    #[test]
    fn parse_since_tag() {
        let comment = "/**\n * Some feature.\n *\n * @since 1.0.0\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.since.as_deref(), Some("1.0.0"));
    }

    #[test]
    fn parse_deprecated_tag() {
        let comment = "/**\n * Old API.\n *\n * @deprecated use newMethod() instead\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(entry.deprecated.is_some());
        assert!(entry.deprecated.as_deref().unwrap().contains("newMethod"));
    }

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
        assert!(entry.returns.is_some());
        assert_eq!(entry.throws.len(), 1);
        assert!(entry.since.is_some());
        assert!(entry.deprecated.is_some());
    }

    #[test]
    fn parse_empty_comment_returns_none() {
        assert!(parse_javadoc_comment("/** */").is_none());
    }

    #[test]
    fn parse_non_javadoc_comment_returns_none() {
        assert!(parse_javadoc_comment("/* not a javadoc */").is_none());
    }

    #[test]
    fn parse_only_see_tag_returns_none() {
        assert!(parse_javadoc_comment("/**\n * @see SomeOtherClass\n */").is_none());
    }

    // -----------------------------------------------------------------------
    // extract_from_source (method docs)
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
}
"#;

    #[test]
    fn extract_finds_method_and_constructor() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        assert_eq!(entries.len(), 2);
        let keys: Vec<&str> = entries.iter().map(|(k, _)| k.as_str()).collect();
        assert!(keys.contains(&"com.example.MyService#greet"));
        assert!(keys.contains(&"com.example.MyService#MyService"));
    }

    #[test]
    fn extract_method_has_correct_data() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        let greet = entries
            .iter()
            .find(|(k, _)| k.ends_with("#greet"))
            .map(|(_, v)| v)
            .expect("greet not found");
        assert!(greet.description.contains("Greets"));
        assert_eq!(greet.params.len(), 1);
        assert_eq!(greet.params[0].name, "name");
        assert_eq!(greet.returns.as_deref(), Some("a greeting"));
    }

    #[test]
    fn extract_ignores_non_javadoc_comment() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        let keys: Vec<&str> = entries.iter().map(|(k, _)| k.as_str()).collect();
        assert!(!keys.contains(&"com.example.MyService#notDocumented"));
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
        let bad_bytes = b"\xff\xfe public class X {}";
        let _ = extract_from_source(bad_bytes, Path::new("X.java"));
    }

    // -----------------------------------------------------------------------
    // Module doc extraction
    // -----------------------------------------------------------------------

    #[test]
    fn module_doc_extracted_for_interface() {
        let src = br#"
package com.example;

/**
 * The core documentation API.
 *
 * @since 1.0
 */
public interface MyApi {
    /** Does something. @param x input */
    void doThing(String x);
}
"#;
        let result = process_file_source(src, Path::new("MyApi.java"));
        let module = result.module_doc.expect("should have module doc");
        assert_eq!(module.simple_name, "MyApi");
        assert_eq!(module.package, "com.example");
        assert_eq!(module.kind, "interface");
        assert!(module.description.contains("core documentation API"), "desc: {}", module.description);
        assert_eq!(module.since.as_deref(), Some("1.0"));
        assert!(module.signature.contains("interface MyApi"), "sig: {}", module.signature);
    }

    #[test]
    fn module_doc_missing_returns_none() {
        let src = br#"
package com.example;

public interface NoDoc {
    void doThing();
}
"#;
        let result = process_file_source(src, Path::new("NoDoc.java"));
        assert!(result.module_doc.is_none());
    }

    #[test]
    fn module_doc_signature_strips_body() {
        let src = br#"
package com.example;

/**
 * A record type.
 */
public record Point(int x, int y) {
    /** Gets distance. @return distance */
    public double distance() { return Math.sqrt(x*x + y*y); }
}
"#;
        let result = process_file_source(src, Path::new("Point.java"));
        let module = result.module_doc.expect("should have module doc");
        assert!(module.signature.contains("record Point"), "sig: {}", module.signature);
        // Body should NOT be in signature
        assert!(!module.signature.contains("distance"), "sig should not have body: {}", module.signature);
    }

    // -----------------------------------------------------------------------
    // Violation detection (TPS Jidoka)
    // -----------------------------------------------------------------------

    #[test]
    fn violation_missing_class_doc() {
        let src = br#"
package com.example;

public class NoDocs {
    public void doThing() {}
}
"#;
        let result = process_file_source(src, Path::new("NoDocs.java"));
        let class_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingClassDoc))
            .collect();
        assert_eq!(class_violations.len(), 1, "expected 1 class violation");
    }

    #[test]
    fn violation_missing_method_doc() {
        let src = br#"
package com.example;

/**
 * Well-documented class.
 */
public class WellDocs {
    /** Has docs. */
    public void documented() {}

    public void notDocumented() {}
}
"#;
        let result = process_file_source(src, Path::new("WellDocs.java"));
        let method_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert_eq!(method_violations.len(), 1, "expected 1 method violation");
        if let ViolationKind::MissingMethodDoc { method } = &method_violations[0].kind {
            assert_eq!(method, "notDocumented");
        }
    }

    #[test]
    fn no_violation_for_override_methods() {
        let src = br#"
package com.example;

/**
 * Implementation.
 */
public class Impl implements Runnable {
    @Override
    public void run() {}
}
"#;
        let result = process_file_source(src, Path::new("Impl.java"));
        let method_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert!(method_violations.is_empty(), "Override methods should not be violations: {:?}", method_violations);
    }

    #[test]
    fn no_violation_for_private_methods() {
        let src = br#"
package com.example;

/**
 * Has private stuff.
 */
public class Private {
    private void hidden() {}
    protected void alsoHidden() {}
}
"#;
        let result = process_file_source(src, Path::new("Private.java"));
        let violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert!(violations.is_empty(), "private/protected should not be violations");
    }

    #[test]
    fn fully_documented_class_has_no_violations() {
        let result = process_file_source(SIMPLE_JAVA, Path::new("MyService.java"));
        // SIMPLE_JAVA has notDocumented() without docs — should be a violation
        let method_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert_eq!(method_violations.len(), 1);
    }

    // -----------------------------------------------------------------------
    // Markdown rendering
    // -----------------------------------------------------------------------

    #[test]
    fn markdown_contains_class_name() {
        let module = ModuleDoc {
            fqcn: "com.example.MyApi".to_string(),
            simple_name: "MyApi".to_string(),
            package: "com.example".to_string(),
            description: "The core API.".to_string(),
            signature: "public interface MyApi".to_string(),
            kind: "interface".to_string(),
            method_count: 1,
            method_names: vec!["doThing".to_string()],
            ..Default::default()
        };
        let methods = HashMap::new();
        let md = render_module_markdown(&module, &methods);
        assert!(md.contains("# `MyApi`"), "md: {}", md);
        assert!(md.contains("The core API."), "md: {}", md);
        assert!(md.contains("```java"), "md: {}", md);
        assert!(md.contains("public interface MyApi {"), "md: {}", md);
        assert!(md.contains("doThing"), "md: {}", md);
    }

    #[test]
    fn markdown_includes_method_docs() {
        let module = ModuleDoc {
            fqcn: "com.example.MyApi".to_string(),
            simple_name: "MyApi".to_string(),
            package: "com.example".to_string(),
            description: "The core API.".to_string(),
            signature: "public interface MyApi".to_string(),
            kind: "interface".to_string(),
            method_count: 1,
            method_names: vec!["doThing".to_string()],
            ..Default::default()
        };
        let mut methods = HashMap::new();
        methods.insert(
            "com.example.MyApi#doThing".to_string(),
            JavadocEntry {
                description: "Does a thing.".to_string(),
                params: vec![ParamDoc {
                    name: "x".to_string(),
                    description: "the input".to_string(),
                }],
                returns: Some("result".to_string()),
                ..Default::default()
            },
        );
        let md = render_module_markdown(&module, &methods);
        assert!(md.contains("### `doThing`"), "md: {}", md);
        assert!(md.contains("Does a thing."), "md: {}", md);
        assert!(md.contains("| `x` |"), "md: {}", md);
        assert!(md.contains("**Returns:**"), "md: {}", md);
    }

    // -----------------------------------------------------------------------
    // Integration: full DTR source tree
    // -----------------------------------------------------------------------

    #[test]
    fn integration_dtr_core_zero_violations() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            eprintln!("Skipping integration test: {:?} not found", source_dir);
            return;
        }

        let (_, _, violations) = process_all(&source_dir);

        if !violations.is_empty() {
            let msg: String = violations.iter().map(|v| format!("\n{}", v)).collect();
            panic!(
                "\n\nTPS VIOLATION: {} missing doc(s) found in dtr-core:\n{}\n",
                violations.len(),
                msg
            );
        }
    }

    #[test]
    fn integration_dtr_core_produces_module_docs() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            eprintln!("Skipping integration test: {:?} not found", source_dir);
            return;
        }

        let (method_docs, module_docs, _) = process_all(&source_dir);

        assert!(
            module_docs.len() > 30,
            "expected >30 module docs, got {}",
            module_docs.len()
        );

        let rmc = module_docs
            .iter()
            .find(|m| m.simple_name == "RenderMachineCommands")
            .expect("RenderMachineCommands module doc missing");
        assert!(
            rmc.description.contains("DTR") || rmc.description.contains("documentation"),
            "desc: {}",
            rmc.description
        );
        assert!(rmc.signature.contains("interface RenderMachineCommands"), "sig: {}", rmc.signature);
        assert!(rmc.method_count > 0);

        // Spot-check markdown output
        let md = render_module_markdown(rmc, &method_docs);
        assert!(md.contains("# `RenderMachineCommands`"), "md header missing");
        assert!(md.contains("```java"), "code block missing");
        assert!(md.contains("### `say`"), "method section missing");
    }

    #[test]
    fn integration_extract_all_backward_compat() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            return;
        }

        let entries = extract_all(&source_dir);
        assert!(entries.len() > 50);
        let key = "io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#say";
        let say = entries.get(key).expect("say missing");
        assert!(say.description.contains("paragraph"));
        assert_eq!(say.params[0].name, "text");
    }
}
