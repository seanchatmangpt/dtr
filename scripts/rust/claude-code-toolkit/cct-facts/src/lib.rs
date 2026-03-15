//! Generic codebase fact gatherer.
//!
//! Generates compact JSON fact files (~50 tokens each) for agent consumption.
//! One fact file = one grep sweep = 100× token efficiency gain.
//!
//! # Project types detected
//! - Maven (`pom.xml`) → `maven.json`
//! - Cargo (`Cargo.toml`) → `cargo.json`
//! - npm (`package.json`) → `npm.json`
//! - Generic → `source-stats.json`, `git.json`
//!
//! # Usage
//! ```no_run
//! use cct_facts::{gather_all, write_facts};
//! use std::path::Path;
//!
//! let root = Path::new(".");
//! let facts = gather_all(root);
//! write_facts(Path::new("docs/facts"), &facts).unwrap();
//! ```

use anyhow::Result;
use serde_json::{json, Value};
use std::path::Path;
use std::process::Command;

// ─── Fact gatherers ──────────────────────────────────────────────────────────

/// Gather all available facts for the project at `root`.
pub fn gather_all(root: &Path) -> Vec<(&'static str, Value)> {
    let mut facts = Vec::new();

    if let Ok(v) = gather_project_type(root) {
        facts.push(("project.json", v));
    }
    if let Ok(v) = gather_source_stats(root) {
        facts.push(("source-stats.json", v));
    }
    if let Ok(v) = gather_git_info(root) {
        facts.push(("git.json", v));
    }
    if root.join("pom.xml").exists() {
        if let Ok(v) = gather_maven(root) {
            facts.push(("maven.json", v));
        }
    }
    if root.join("Cargo.toml").exists() {
        if let Ok(v) = gather_cargo(root) {
            facts.push(("cargo.json", v));
        }
    }
    if root.join("package.json").exists() {
        if let Ok(v) = gather_npm(root) {
            facts.push(("npm.json", v));
        }
    }
    facts
}

/// Detect the primary project type.
///
/// # Errors
/// Returns an error if JSON serialization fails.
pub fn gather_project_type(root: &Path) -> Result<Value> {
    let has_pom = root.join("pom.xml").exists();
    let has_cargo = root.join("Cargo.toml").exists();
    let has_npm = root.join("package.json").exists();
    let has_gradle = root.join("build.gradle").exists() || root.join("build.gradle.kts").exists();
    let has_pyproject = root.join("pyproject.toml").exists();

    let kind = if has_pom {
        "maven"
    } else if has_cargo {
        "cargo"
    } else if has_gradle {
        "gradle"
    } else if has_npm {
        "npm"
    } else if has_pyproject {
        "python"
    } else {
        "unknown"
    };

    Ok(json!({
        "type": kind,
        "has_pom": has_pom,
        "has_cargo": has_cargo,
        "has_gradle": has_gradle,
        "has_npm": has_npm,
        "has_pyproject": has_pyproject,
        "generated": iso8601_now()
    }))
}

/// Count source files by extension.
///
/// # Errors
/// Returns an error if JSON serialization fails.
pub fn gather_source_stats(root: &Path) -> Result<Value> {
    use walkdir::WalkDir;
    let mut counts: std::collections::HashMap<String, u64> = std::collections::HashMap::new();
    let mut total = 0u64;

    for entry in WalkDir::new(root)
        .follow_links(false)
        .into_iter()
        .filter_map(|e| e.ok())
        .filter(|e| e.file_type().is_file())
    {
        let path = entry.path().to_string_lossy();
        // skip hidden dirs and build output
        if path.contains("/.") || path.contains("/target/") || path.contains("/node_modules/") {
            continue;
        }
        if let Some(ext) = entry.path().extension().and_then(|e| e.to_str()) {
            *counts.entry(ext.to_owned()).or_insert(0) += 1;
            total += 1;
        }
    }

    Ok(json!({
        "by_extension": counts,
        "total_files": total,
        "generated": iso8601_now()
    }))
}

/// Collect git metadata.
///
/// # Errors
/// Returns an error if JSON serialization fails.
pub fn gather_git_info(root: &Path) -> Result<Value> {
    let r = root.to_string_lossy();

    let branch =
        git_output(&["-C", &r, "branch", "--show-current"]).unwrap_or_else(|| "unknown".into());
    let last_commit =
        git_output(&["-C", &r, "log", "--oneline", "-1"]).unwrap_or_else(|| "(no commits)".into());
    let is_dirty = Command::new("git")
        .args(["-C", &r, "diff", "--quiet"])
        .status()
        .map(|s| !s.success())
        .unwrap_or(false);
    let remote = git_output(&["-C", &r, "remote", "get-url", "origin"])
        .unwrap_or_else(|| "(no remote)".into());

    Ok(json!({
        "branch": branch.trim(),
        "last_commit": last_commit.trim(),
        "is_dirty": is_dirty,
        "remote": remote.trim(),
        "generated": iso8601_now()
    }))
}

/// Gather Maven project metadata from pom.xml.
pub fn gather_maven(root: &Path) -> Result<Value> {
    let pom = std::fs::read_to_string(root.join("pom.xml"))?;

    let version = extract_between(&pom, "<version>", "</version>").unwrap_or("unknown");
    let group_id = extract_between(&pom, "<groupId>", "</groupId>").unwrap_or("unknown");
    let artifact_id = extract_between(&pom, "<artifactId>", "</artifactId>").unwrap_or("unknown");
    let java_release = extract_between(
        &pom,
        "<maven.compiler.release>",
        "</maven.compiler.release>",
    )
    .or_else(|| extract_between(&pom, "<release>", "</release>"))
    .unwrap_or("unknown");

    // Collect module names
    let modules: Vec<&str> = pom
        .lines()
        .filter_map(|l| {
            let t = l.trim();
            if t.starts_with("<module>") {
                extract_between(t, "<module>", "</module>")
            } else {
                None
            }
        })
        .collect();

    Ok(json!({
        "version": version.trim(),
        "groupId": group_id.trim(),
        "artifactId": artifact_id.trim(),
        "java_release": java_release.trim(),
        "modules": modules,
        "generated": iso8601_now()
    }))
}

/// Gather Cargo workspace metadata from root Cargo.toml.
pub fn gather_cargo(root: &Path) -> Result<Value> {
    let cargo_toml = std::fs::read_to_string(root.join("Cargo.toml"))?;

    let version = extract_between(&cargo_toml, "version = \"", "\"").unwrap_or("unknown");
    let name = extract_between(&cargo_toml, "name = \"", "\"").unwrap_or("unknown");

    // Count members
    let members: Vec<&str> = cargo_toml
        .lines()
        .filter_map(|l| {
            let t = l.trim().trim_matches(',').trim_matches('"');
            if l.contains('"') && !l.contains('[') && !l.contains('#') && !l.contains('=') {
                Some(t)
            } else {
                None
            }
        })
        .collect();

    let rust_edition = extract_between(&cargo_toml, "edition = \"", "\"").unwrap_or("unknown");

    Ok(json!({
        "name": name.trim(),
        "version": version.trim(),
        "edition": rust_edition.trim(),
        "members": members,
        "generated": iso8601_now()
    }))
}

/// Gather npm package metadata from package.json.
pub fn gather_npm(root: &Path) -> Result<Value> {
    let content = std::fs::read_to_string(root.join("package.json"))?;
    let pkg: Value = serde_json::from_str(&content)?;
    Ok(json!({
        "name": pkg.get("name").and_then(Value::as_str).unwrap_or("unknown"),
        "version": pkg.get("version").and_then(Value::as_str).unwrap_or("unknown"),
        "description": pkg.get("description").and_then(Value::as_str).unwrap_or(""),
        "generated": iso8601_now()
    }))
}

// ─── Writer ──────────────────────────────────────────────────────────────────

/// Write fact files to `output_dir` as minified JSON.
pub fn write_facts(output_dir: &Path, facts: &[(&str, Value)]) -> Result<()> {
    std::fs::create_dir_all(output_dir)?;
    for (name, value) in facts {
        let path = output_dir.join(name);
        let json = serde_json::to_string(value)?;
        std::fs::write(&path, json)?;
    }
    Ok(())
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fn extract_between<'a>(s: &'a str, start: &str, end: &str) -> Option<&'a str> {
    let start_idx = s.find(start)? + start.len();
    let end_idx = s[start_idx..].find(end)? + start_idx;
    Some(&s[start_idx..end_idx])
}

fn git_output(args: &[&str]) -> Option<String> {
    Command::new("git")
        .args(args)
        .output()
        .ok()
        .filter(|o| o.status.success())
        .map(|o| String::from_utf8_lossy(&o.stdout).into_owned())
}

pub fn iso8601_now() -> String {
    Command::new("date")
        .args(["-u", "+%Y-%m-%dT%H:%M:%SZ"])
        .output()
        .ok()
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_owned())
        .unwrap_or_else(|| "unknown".into())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    fn dtr_root() -> PathBuf {
        // cct-facts/ → claude-code-toolkit/ → rust/ → scripts/ → dtr/
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .to_path_buf()
    }

    #[test]
    fn project_type_detects_maven() {
        let v = gather_project_type(&dtr_root()).unwrap();
        assert_eq!(v["type"], "maven");
        assert_eq!(v["has_pom"], true);
    }

    #[test]
    fn source_stats_counts_files() {
        let v = gather_source_stats(&dtr_root()).unwrap();
        assert!(v["total_files"].as_u64().unwrap_or(0) > 0);
    }

    #[test]
    fn maven_facts_has_version() {
        let v = gather_maven(&dtr_root()).unwrap();
        assert!(v["version"].as_str().unwrap_or("").contains("2026"));
    }

    #[test]
    fn git_info_has_branch() {
        let v = gather_git_info(&dtr_root()).unwrap();
        assert_ne!(v["branch"].as_str().unwrap_or(""), "");
    }

    #[test]
    fn extract_between_works() {
        let s = "<version>1.2.3</version>";
        assert_eq!(extract_between(s, "<version>", "</version>"), Some("1.2.3"));
    }

    #[test]
    fn write_and_read_facts() {
        let tmp = std::env::temp_dir().join("cct_facts_test");
        std::fs::create_dir_all(&tmp).unwrap();
        let facts = vec![("test.json", serde_json::json!({"key": "value"}))];
        write_facts(&tmp, &facts).unwrap();
        let content = std::fs::read_to_string(tmp.join("test.json")).unwrap();
        assert!(content.contains("value"));
    }
}
