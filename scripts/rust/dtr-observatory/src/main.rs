//! dtr-observe — DTR Observatory CLI.
//!
//! Usage:
//!   dtr-observe [--root <dir>] [--output <dir>] [--quiet] [--json-summary]
//!
//! Generates six compact JSON fact files describing the codebase state.
//! Designed to run at session start and via `make observe`.

use anyhow::Result;
use dtr_observatory::{
    gather_guard_status, gather_java_profile, gather_modules, gather_rust_capabilities,
    gather_source_stats, gather_tests, write_facts,
};
use serde_json::json;
use std::env;
use std::path::{Path, PathBuf};
use std::process;

fn main() {
    if let Err(e) = run() {
        eprintln!("dtr-observe error: {e}");
        process::exit(1);
    }
}

fn run() -> Result<()> {
    let args: Vec<String> = env::args().collect();

    let mut root: Option<PathBuf> = None;
    let mut output: Option<PathBuf> = None;
    let mut quiet = false;
    let mut json_summary = false;

    let mut i = 1;
    while i < args.len() {
        match args[i].as_str() {
            "--root" => {
                i += 1;
                if i < args.len() {
                    root = Some(PathBuf::from(&args[i]));
                }
            }
            "--output" => {
                i += 1;
                if i < args.len() {
                    output = Some(PathBuf::from(&args[i]));
                }
            }
            "--quiet" => quiet = true,
            "--json-summary" => json_summary = true,
            arg if arg.starts_with('-') => {
                eprintln!("Unknown flag: {arg}");
                print_usage(&args[0]);
                process::exit(1);
            }
            _ => {}
        }
        i += 1;
    }

    let root =
        root.unwrap_or_else(|| env::current_dir().expect("Cannot determine current directory"));
    let output = output.unwrap_or_else(|| root.join("docs").join("facts"));

    if !quiet {
        eprintln!("Observatory: scanning {}", root.display());
    }

    // Run all six gatherers
    let modules = run_gatherer("modules", &root, gather_modules, quiet);
    let java_profile = run_gatherer("java-profile", &root, gather_java_profile, quiet);
    let rust_caps = run_gatherer("rust-capabilities", &root, gather_rust_capabilities, quiet);
    let source_stats = run_gatherer("source-stats", &root, gather_source_stats, quiet);
    let tests = run_gatherer("tests", &root, gather_tests, quiet);
    let guard_status = run_gatherer("guard-status", &root, gather_guard_status, quiet);

    let facts = vec![
        ("modules.json", modules),
        ("java-profile.json", java_profile),
        ("rust-capabilities.json", rust_caps),
        ("source-stats.json", source_stats),
        ("tests.json", tests),
        ("guard-status.json", guard_status),
    ];

    write_facts(&output, &facts)?;

    if json_summary {
        let summary = json!({
            "facts_written": facts.len(),
            "output_dir": output.display().to_string()
        });
        println!("{}", serde_json::to_string(&summary)?);
    } else if !quiet {
        eprintln!(
            "Observatory: {} facts written to {}",
            facts.len(),
            output.display()
        );
        eprintln!("  modules.json, java-profile.json, rust-capabilities.json,");
        eprintln!("  source-stats.json, tests.json, guard-status.json");
    }

    Ok(())
}

fn run_gatherer<F>(name: &str, root: &Path, gatherer: F, quiet: bool) -> serde_json::Value
where
    F: Fn(&Path) -> Result<serde_json::Value>,
{
    match gatherer(root) {
        Ok(value) => {
            if !quiet {
                eprintln!("  ✓ {name}");
            }
            value
        }
        Err(e) => {
            eprintln!("  ✗ {name}: {e}");
            serde_json::json!({"error": e.to_string()})
        }
    }
}

fn print_usage(prog: &str) {
    eprintln!("Usage: {prog} [--root <dir>] [--output <dir>] [--quiet] [--json-summary]");
    eprintln!();
    eprintln!("Flags:");
    eprintln!("  --root <dir>     Project root (default: current directory)");
    eprintln!("  --output <dir>   Output directory (default: <root>/docs/facts)");
    eprintln!("  --quiet          Suppress progress output");
    eprintln!("  --json-summary   Print JSON summary to stdout on completion");
    eprintln!();
    eprintln!("Generates docs/facts/{{modules,java-profile,rust-capabilities,source-stats,tests,guard-status}}.json");
}
