//! dtr-guard-scan — H-Guard enforcement CLI for DTR Java projects.
//!
//! Usage:
//!   dtr-guard-scan [--json] [--include-tests] [--content <file>] <file.java> [...]
//!
//! Exits:
//!   0  — all files clean
//!   2  — one or more H-Guard violations found
//!
//! Designed to run as a Claude Code `PreToolUse` hook and as a Makefile build gate.

use anyhow::Result;
use dtr_guard::{scan_content, scan_file, GuardPatterns, Violation};
use std::env;
use std::fs;
use std::path::Path;
use std::process;

fn main() {
    if let Err(e) = run() {
        eprintln!("dtr-guard-scan error: {e}");
        process::exit(1);
    }
}

#[allow(clippy::case_sensitive_file_extension_comparisons, clippy::map_unwrap_or)]
fn run() -> Result<()> {
    let args: Vec<String> = env::args().collect();

    let mut json_output = false;
    let mut include_tests = false;
    let mut content_file: Option<String> = None;
    let mut files: Vec<String> = Vec::new();

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--json" => json_output = true,
            "--include-tests" => include_tests = true,
            "--content" => {
                i += 1;
                if i < args.len() {
                    content_file = Some(args[i].clone());
                }
            }
            arg if arg.starts_with('-') => {
                eprintln!("Unknown flag: {arg}");
                print_usage(&args[0]);
                process::exit(1);
            }
            file => files.push(file.to_string()),
        }
        i += 1;
    }

    if files.is_empty() && content_file.is_none() {
        print_usage(&args[0]);
        process::exit(1);
    }

    let patterns = GuardPatterns::compile();
    let exclude_tests = !include_tests;
    let mut all_violations: Vec<Violation> = Vec::new();

    // Scan content from a temp file (used by hook to scan proposed writes)
    if let Some(ref content_path) = content_file {
        let label = files.first().map(String::as_str).unwrap_or("<stdin>");
        if label.ends_with(".java") || label == "<stdin>" {
            let content = fs::read_to_string(content_path)?;
            if !exclude_tests || !dtr_guard::is_test_path(label) {
                let violations = scan_content(&content, label, &patterns);
                all_violations.extend(violations);
            }
        }
    } else {
        // Scan actual files
        for file_path in &files {
            let path = Path::new(file_path);

            if !path.exists() {
                eprintln!("dtr-guard-scan: file not found: {file_path}");
                continue;
            }

            if !file_path.ends_with(".java") {
                continue;
            }

            match scan_file(path, &patterns, exclude_tests) {
                Ok(violations) => all_violations.extend(violations),
                Err(e) => eprintln!("dtr-guard-scan: cannot read {file_path}: {e}"),
            }
        }
    }

    if json_output {
        print_json(&all_violations)?;
    } else {
        print_human(&all_violations);
    }

    if all_violations.is_empty() {
        process::exit(0);
    } else {
        process::exit(2);
    }
}

fn print_human(violations: &[Violation]) {
    if violations.is_empty() {
        return;
    }

    eprintln!();
    eprintln!("╔══════════════════════════════════════════════════════════════╗");
    eprintln!("║              DTR H-Guard: Semantic Lies Detected             ║");
    eprintln!("╚══════════════════════════════════════════════════════════════╝");
    eprintln!();

    // Group by file
    let mut current_file = String::new();
    for v in violations {
        if v.file != current_file {
            eprintln!("  📄 {}", v.file);
            current_file.clone_from(&v.file);
        }
        eprintln!("     ❌ Line {:>4}  [{}]  {}", v.line, v.code, v.matched);
        eprintln!("            Fix: {}", v.fix);
        eprintln!();
    }

    let count = violations.len();
    eprintln!(
        "  {} violation{} found.",
        count,
        if count == 1 { "" } else { "s" }
    );
    eprintln!();
    eprintln!("  H-Guard enforces: no TODOs, no mocks in production, no stub returns,");
    eprintln!("  no empty bodies, no silent fallbacks, no logging instead of throwing.");
    eprintln!();
    eprintln!("  The canonical fix for H_TODO / H_STUB / H_EMPTY / H_SILENT:");
    eprintln!(r#"    throw new UnsupportedOperationException("Not implemented");"#);
    eprintln!();
}

fn print_json(violations: &[Violation]) -> Result<()> {
    use serde_json::json;

    let output = json!({
        "status": if violations.is_empty() { "GREEN" } else { "RED" },
        "violation_count": violations.len(),
        "violations": violations,
        "fix_guidance": "Replace stub/empty/TODO with: throw new UnsupportedOperationException(\"Not implemented\");"
    });

    println!("{}", serde_json::to_string_pretty(&output)?);
    Ok(())
}

fn print_usage(prog: &str) {
    eprintln!("Usage: {prog} [--json] [--include-tests] [--content <tempfile>] <file.java> [...]");
    eprintln!();
    eprintln!("Flags:");
    eprintln!("  --json            Output violations as JSON receipt (machine-readable)");
    eprintln!("  --include-tests   Also scan test source files (default: skip test paths)");
    eprintln!("  --content <file>  Scan content from <file>, use first positional arg as label");
    eprintln!();
    eprintln!("Exit codes:");
    eprintln!("  0  clean — no violations");
    eprintln!("  2  RED   — violations found, write blocked");
}
