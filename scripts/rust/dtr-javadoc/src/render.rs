/// Markdown generation and file output.
///
/// This module handles rendering extracted documentation to Markdown format
/// and writing API docs to disk.

use std::collections::HashMap;
use std::fmt::Write as FmtWrite;
use std::path::{Path, PathBuf};
use super::model::{JavadocEntry, ModuleDoc};

/// Render a module's documentation as a markdown string.
#[must_use]
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
        writeln!(out, "> **Since:** `{since}`  ").unwrap();
    }
    if let Some(dep) = &module.deprecated {
        writeln!(out, "> [!WARNING]  ").unwrap();
        writeln!(out, "> **Deprecated:** {dep}  ").unwrap();
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
        writeln!(out, "    // {names_preview}{suffix}").unwrap();
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
            writeln!(out, "### `{method_name}`").unwrap();
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
                writeln!(out, "> **Returns:** {ret}").unwrap();
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
                writeln!(out, "> **Since:** {since}").unwrap();
                writeln!(out).unwrap();
            }

            if let Some(dep) = &entry.deprecated {
                writeln!(out, "> [!WARNING]").unwrap();
                writeln!(out, "> **Deprecated:** {dep}").unwrap();
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
///
/// # Errors
/// Returns an error if creating directories or writing files fails.
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
