//! DTR Observatory — codebase fact generator.
//!
//! Produces six compact JSON fact files to `docs/facts/`:
//! - modules.json        Maven modules, version, Java profile
//! - java-profile.json   Compiler settings from .mvn/maven.config
//! - rust-capabilities.json  Rust crates and binaries
//! - source-stats.json   Java file counts by module
//! - tests.json          Test class counts by module
//! - guard-status.json   H-Guard scan result

use anyhow::{Context, Result};
use serde_json::{json, Value};
use std::fs;
use std::path::{Path, PathBuf};
use std::process::Command;
use walkdir::WalkDir;

// ─── Modules ─────────────────────────────────────────────────────────────────

/// Parse root pom.xml for version, groupId, and module list.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn gather_modules(root: &Path) -> Result<Value> {
    let pom = root.join("pom.xml");
    let content =
        fs::read_to_string(&pom).with_context(|| format!("Cannot read {}", pom.display()))?;

    let version = extract_first_tag(&content, "version").unwrap_or_else(|| "unknown".to_string());
    let group_id = extract_first_tag(&content, "groupId").unwrap_or_else(|| "unknown".to_string());

    let modules: Vec<String> = extract_all_tags(&content, "module");

    // Determine main deployable module (first non-benchmarks module)
    let main_module = modules
        .iter()
        .find(|m| !m.contains("benchmark") && !m.contains("integration"))
        .cloned()
        .unwrap_or_else(|| modules.first().cloned().unwrap_or_default());

    // Read Java release from .mvn/maven.config
    let (java_release, enable_preview) = read_maven_config(root);

    Ok(json!({
        "version": version,
        "groupId": group_id,
        "modules": modules,
        "main_module": main_module,
        "java_release": java_release,
        "enable_preview": enable_preview
    }))
}

// ─── Java Profile ─────────────────────────────────────────────────────────────

/// Read compiler settings from .mvn/maven.config.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn gather_java_profile(root: &Path) -> Result<Value> {
    let config_path = root.join(".mvn").join("maven.config");
    let content = fs::read_to_string(&config_path)
        .with_context(|| format!("Cannot read {}", config_path.display()))?;

    let lines: Vec<String> = content
        .lines()
        .map(|l| l.trim().to_string())
        .filter(|l| !l.is_empty())
        .collect();

    let (java_release, enable_preview) = read_maven_config(root);

    Ok(json!({
        "java_release": java_release,
        "enable_preview": enable_preview,
        "maven_config": lines
    }))
}

// ─── Rust Capabilities ───────────────────────────────────────────────────────

/// Inventory all Rust crates under scripts/rust/.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn gather_rust_capabilities(root: &Path) -> Result<Value> {
    let rust_dir = root.join("scripts").join("rust");

    if !rust_dir.exists() {
        return Ok(json!({"crates": [], "total_tests": 0}));
    }

    let mut crates = Vec::new();
    let mut total_tests: usize = 0;

    // Find all Cargo.toml files (one level deep: scripts/rust/<crate>/Cargo.toml)
    for entry in fs::read_dir(&rust_dir).context("Cannot read scripts/rust/")? {
        let entry = entry?;
        let cargo_toml = entry.path().join("Cargo.toml");
        if !cargo_toml.exists() {
            continue;
        }

        let content = fs::read_to_string(&cargo_toml)?;
        let crate_name = extract_first_tag_toml(&content, "name")
            .unwrap_or_else(|| entry.file_name().to_string_lossy().to_string());

        // Extract [[bin]] names
        let bins = extract_bin_names(&content);

        // Check if release binaries are built
        let release_dir = entry.path().join("target").join("release");
        let bins_built: Vec<Value> = bins
            .iter()
            .map(|b| {
                let built = release_dir.join(b).exists();
                json!({"name": b, "built": built})
            })
            .collect();

        // Count #[test] in src/**/*.rs
        let crate_tests = count_rust_tests(&entry.path());
        total_tests += crate_tests;

        crates.push(json!({
            "name": crate_name,
            "bins": bins_built,
            "tests": crate_tests
        }));
    }

    Ok(json!({
        "crates": crates,
        "total_tests": total_tests
    }))
}

// ─── Source Stats ─────────────────────────────────────────────────────────────

/// Count Java files by module and directory type.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn gather_source_stats(root: &Path) -> Result<Value> {
    let pom = root.join("pom.xml");
    let content = fs::read_to_string(&pom).context("Cannot read pom.xml")?;
    let modules = extract_all_tags(&content, "module");

    let mut by_module = serde_json::Map::new();
    let mut total_main: usize = 0;
    let mut total_test: usize = 0;

    for module in &modules {
        let module_dir = root.join(module);
        let main_dir = module_dir.join("src").join("main").join("java");
        let test_dir = module_dir.join("src").join("test").join("java");

        let main_count = count_java_files(&main_dir);
        let test_count = count_java_files(&test_dir);
        let package_count = count_packages(&main_dir);

        total_main += main_count;
        total_test += test_count;

        by_module.insert(
            module.clone(),
            json!({
                "main_java": main_count,
                "test_java": test_count,
                "packages": package_count
            }),
        );
    }

    Ok(json!({
        "by_module": by_module,
        "total": {
            "main_java": total_main,
            "test_java": total_test
        }
    }))
}

// ─── Tests ────────────────────────────────────────────────────────────────────

/// Count test classes (files containing @Test) per module.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn gather_tests(root: &Path) -> Result<Value> {
    let pom = root.join("pom.xml");
    let content = fs::read_to_string(&pom).context("Cannot read pom.xml")?;
    let modules = extract_all_tags(&content, "module");

    // Also include dtr-integration-test if present
    let mut all_modules = modules.clone();
    let integration = root.join("dtr-integration-test");
    if integration.exists() && !all_modules.iter().any(|m| m == "dtr-integration-test") {
        all_modules.push("dtr-integration-test".to_string());
    }

    let mut by_module = serde_json::Map::new();
    let mut total_classes: usize = 0;

    for module in &all_modules {
        let test_dir = root.join(module).join("src").join("test").join("java");
        let test_classes = count_test_classes(&test_dir);
        total_classes += test_classes;
        by_module.insert(module.clone(), json!({"test_classes": test_classes}));
    }

    Ok(json!({
        "by_module": by_module,
        "total_classes": total_classes
    }))
}

// ─── Guard Status ─────────────────────────────────────────────────────────────

/// Run dtr-guard-scan over main sources and capture result.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn gather_guard_status(root: &Path) -> Result<Value> {
    let guard_bin = root
        .join("scripts")
        .join("rust")
        .join("dtr-guard")
        .join("target")
        .join("release")
        .join("dtr-guard-scan");

    let generated = iso8601_now();

    if !guard_bin.exists() {
        return Ok(json!({
            "status": "UNKNOWN",
            "note": "run: make build-guard",
            "generated": generated
        }));
    }

    // Collect main Java files
    let main_java: Vec<PathBuf> = {
        let pom = root.join("pom.xml");
        let pom_content = fs::read_to_string(&pom).unwrap_or_default();
        let modules = extract_all_tags(&pom_content, "module");

        let mut files = Vec::new();
        for module in &modules {
            let main_dir = root.join(module).join("src").join("main").join("java");
            if main_dir.exists() {
                for entry in WalkDir::new(&main_dir)
                    .into_iter()
                    .filter_map(std::result::Result::ok)
                    .filter(|e| e.path().extension().and_then(|x| x.to_str()) == Some("java"))
                {
                    files.push(entry.path().to_path_buf());
                }
            }
        }
        files
    };

    let scanned_files = main_java.len();

    if scanned_files == 0 {
        return Ok(json!({
            "status": "GREEN",
            "violations": 0,
            "scanned_files": 0,
            "generated": generated
        }));
    }

    // Run guard scan with --json flag
    let file_args: Vec<&str> = main_java.iter().map(|p| p.to_str().unwrap_or("")).collect();

    let output = Command::new(&guard_bin)
        .arg("--json")
        .args(&file_args)
        .output();

    match output {
        Ok(out) => {
            // Parse JSON from stdout
            let stdout = String::from_utf8_lossy(&out.stdout);
            if let Ok(parsed) = serde_json::from_str::<Value>(&stdout) {
                let status = parsed
                    .get("status")
                    .and_then(|v| v.as_str())
                    .unwrap_or("UNKNOWN");
                let violations = parsed
                    .get("violation_count")
                    .and_then(serde_json::Value::as_u64)
                    .unwrap_or(0);

                Ok(json!({
                    "status": status,
                    "violations": violations,
                    "scanned_files": scanned_files,
                    "generated": generated
                }))
            } else {
                // Guard ran but produced no parseable JSON (clean = exit 0, no output)
                let status = if out.status.code() == Some(0) {
                    "GREEN"
                } else {
                    "RED"
                };
                Ok(json!({
                    "status": status,
                    "violations": 0,
                    "scanned_files": scanned_files,
                    "generated": generated
                }))
            }
        }
        Err(e) => Ok(json!({
            "status": "ERROR",
            "note": format!("guard scan failed: {e}"),
            "generated": generated
        })),
    }
}

// ─── Write Facts ──────────────────────────────────────────────────────────────

/// Write all facts as minified JSON to `output_dir`.
///
/// # Errors
///
/// Returns an error if the directory cannot be read or files cannot be parsed.
pub fn write_facts(output_dir: &Path, facts: &[(&str, Value)]) -> Result<()> {
    fs::create_dir_all(output_dir)
        .with_context(|| format!("Cannot create {}", output_dir.display()))?;

    for (filename, value) in facts {
        let path = output_dir.join(filename);
        let json_str =
            serde_json::to_string(value).with_context(|| format!("Cannot serialize {filename}"))?;
        fs::write(&path, &json_str).with_context(|| format!("Cannot write {}", path.display()))?;
    }

    Ok(())
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

fn extract_first_tag(xml: &str, tag: &str) -> Option<String> {
    let open = format!("<{tag}>");
    let close = format!("</{tag}>");
    let start = xml.find(&open)? + open.len();
    let end = xml[start..].find(&close)?;
    Some(xml[start..start + end].trim().to_string())
}

fn extract_all_tags(xml: &str, tag: &str) -> Vec<String> {
    let open = format!("<{tag}>");
    let close = format!("</{tag}>");
    let mut results = Vec::new();
    let mut remaining = xml;

    while let Some(start_pos) = remaining.find(&open) {
        let after_open = start_pos + open.len();
        if let Some(end_pos) = remaining[after_open..].find(&close) {
            results.push(
                remaining[after_open..after_open + end_pos]
                    .trim()
                    .to_string(),
            );
            remaining = &remaining[after_open + end_pos + close.len()..];
        } else {
            break;
        }
    }

    results
}

fn extract_first_tag_toml(toml: &str, key: &str) -> Option<String> {
    for line in toml.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with(&format!("{key} =")) || trimmed.starts_with(&format!("{key}=")) {
            // Extract value between quotes
            if let Some(start) = trimmed.find('"') {
                if let Some(end) = trimmed[start + 1..].find('"') {
                    return Some(trimmed[start + 1..start + 1 + end].to_string());
                }
            }
        }
    }
    None
}

fn extract_bin_names(cargo_toml: &str) -> Vec<String> {
    let mut names = Vec::new();
    let mut in_bin = false;

    for line in cargo_toml.lines() {
        let trimmed = line.trim();
        if trimmed == "[[bin]]" {
            in_bin = true;
        } else if trimmed.starts_with("[[") {
            in_bin = false;
        } else if in_bin && (trimmed.starts_with("name =") || trimmed.starts_with("name=")) {
            if let Some(start) = trimmed.find('"') {
                if let Some(end) = trimmed[start + 1..].find('"') {
                    names.push(trimmed[start + 1..start + 1 + end].to_string());
                }
            }
        }
    }

    names
}

fn count_rust_tests(crate_dir: &Path) -> usize {
    let src_dir = crate_dir.join("src");
    if !src_dir.exists() {
        return 0;
    }

    WalkDir::new(&src_dir)
        .into_iter()
        .filter_map(std::result::Result::ok)
        .filter(|e| e.path().extension().and_then(|x| x.to_str()) == Some("rs"))
        .filter_map(|e| fs::read_to_string(e.path()).ok())
        .map(|content| content.matches("#[test]").count())
        .sum()
}

fn count_java_files(dir: &Path) -> usize {
    if !dir.exists() {
        return 0;
    }
    WalkDir::new(dir)
        .into_iter()
        .filter_map(std::result::Result::ok)
        .filter(|e| e.path().extension().and_then(|x| x.to_str()) == Some("java"))
        .count()
}

fn count_packages(main_java_dir: &Path) -> usize {
    if !main_java_dir.exists() {
        return 0;
    }
    WalkDir::new(main_java_dir)
        .min_depth(1)
        .into_iter()
        .filter_map(std::result::Result::ok)
        .filter(|e| e.file_type().is_dir())
        .count()
}

fn count_test_classes(test_dir: &Path) -> usize {
    if !test_dir.exists() {
        return 0;
    }
    WalkDir::new(test_dir)
        .into_iter()
        .filter_map(std::result::Result::ok)
        .filter(|e| e.path().extension().and_then(|x| x.to_str()) == Some("java"))
        .filter_map(|e| fs::read_to_string(e.path()).ok())
        .filter(|content| content.contains("@Test"))
        .count()
}

fn read_maven_config(root: &Path) -> (String, bool) {
    let config_path = root.join(".mvn").join("maven.config");
    let content = fs::read_to_string(config_path).unwrap_or_default();

    let java_release = content
        .lines()
        .find_map(|l| {
            let l = l.trim();
            if l.starts_with("-Dmaven.compiler.release=") {
                Some(
                    l.trim_start_matches("-Dmaven.compiler.release=")
                        .to_string(),
                )
            } else {
                None
            }
        })
        .unwrap_or_else(|| "unknown".to_string());

    let enable_preview = content
        .lines()
        .any(|l| l.trim() == "-Dmaven.compiler.enablePreview=true");

    (java_release, enable_preview)
}

fn iso8601_now() -> String {
    // Use system date command for portability (no chrono dependency)
    Command::new("date")
        .arg("-u")
        .arg("+%Y-%m-%dT%H:%M:%SZ")
        .output()
        .ok()
        .and_then(|o| String::from_utf8(o.stdout).ok())
        .map_or_else(|| "unknown".to_string(), |s| s.trim().to_string())
}

// ─── Tests ────────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use std::fs;

    fn project_root() -> PathBuf {
        // Navigate from scripts/rust/dtr-observatory/ up to project root
        PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .join("..") // scripts/rust/
            .join("..") // scripts/
            .join("..") // project root
    }

    #[test]
    fn test_gather_modules_reads_pom() {
        let root = project_root();
        let result = gather_modules(&root).expect("gather_modules failed");
        assert!(result["version"].is_string());
        assert!(result["modules"].is_array());
        let modules = result["modules"].as_array().unwrap();
        assert!(modules.iter().any(|m| m.as_str() == Some("dtr-core")));
    }

    #[test]
    fn test_gather_java_profile_reads_maven_config() {
        let root = project_root();
        let result = gather_java_profile(&root).expect("gather_java_profile failed");
        assert_eq!(result["java_release"], "26");
        assert_eq!(result["enable_preview"], true);
        assert!(result["maven_config"].is_array());
    }

    #[test]
    fn test_gather_rust_capabilities_finds_crates() {
        let root = project_root();
        let result = gather_rust_capabilities(&root).expect("gather_rust_capabilities failed");
        assert!(result["crates"].is_array());
        let crates = result["crates"].as_array().unwrap();
        // dtr-guard and dtr-javadoc and dtr-observatory should be found
        assert!(crates.len() >= 2, "Expected at least 2 Rust crates");
        let names: Vec<&str> = crates.iter().filter_map(|c| c["name"].as_str()).collect();
        assert!(names.contains(&"dtr-guard"), "Expected dtr-guard crate");
    }

    #[test]
    fn test_gather_source_stats_counts_files() {
        let root = project_root();
        let result = gather_source_stats(&root).expect("gather_source_stats failed");
        assert!(result["total"]["main_java"].as_u64().unwrap_or(0) > 0);
    }

    #[test]
    fn test_gather_tests_counts_test_classes() {
        let root = project_root();
        let result = gather_tests(&root).expect("gather_tests failed");
        assert!(result["total_classes"].as_u64().unwrap_or(0) > 0);
    }

    #[test]
    fn test_extract_first_tag() {
        let xml = "<project><version>1.2.3</version></project>";
        assert_eq!(extract_first_tag(xml, "version"), Some("1.2.3".to_string()));
    }

    #[test]
    fn test_extract_all_tags() {
        let xml = "<modules><module>a</module><module>b</module></modules>";
        let modules = extract_all_tags(xml, "module");
        assert_eq!(modules, vec!["a".to_string(), "b".to_string()]);
    }

    #[test]
    fn test_extract_bin_names() {
        let toml = r#"
[[bin]]
name = "my-tool"
path = "src/main.rs"

[[bin]]
name = "other-tool"
"#;
        let names = extract_bin_names(toml);
        assert_eq!(names, vec!["my-tool", "other-tool"]);
    }

    #[test]
    fn test_write_facts_creates_files() {
        let tmp = std::env::temp_dir().join("dtr-obs-test");
        let _ = fs::remove_dir_all(&tmp);
        let facts = vec![("test.json", json!({"key": "value"}))];
        write_facts(&tmp, &facts).expect("write_facts failed");
        assert!(tmp.join("test.json").exists());
        let content = fs::read_to_string(tmp.join("test.json")).unwrap();
        assert!(content.contains("\"key\""));
        let _ = fs::remove_dir_all(&tmp);
    }
}
