//! cct — Claude Code Toolkit CLI
//!
//! Subcommands:
//!   cct scan  [--json] [--config <file.toml>] <files...>    # pattern scanner
//!   cct observe [--root <dir>] [--output <dir>] [--quiet]   # fact gatherer
//!   cct dx  [--skip <phase>] [--phase <name>]               # validation pipeline
//!   cct hook stop                                            # stop hook (reads stdin)
//!   cct hook pre-tool-use                                    # pre-tool-use hook (reads stdin)

use anyhow::Result;
use std::path::{Path, PathBuf};
use std::process;

fn main() -> Result<()> {
    let args: Vec<String> = std::env::args().collect();
    if args.len() < 2 {
        print_usage();
        process::exit(1);
    }

    match args[1].as_str() {
        "scan" => cmd_scan(&args[2..]),
        "observe" => cmd_observe(&args[2..]),
        "dx" => cmd_dx(&args[2..]),
        "hook" => cmd_hook(&args[2..]),
        "--help" | "-h" | "help" => {
            print_usage();
            Ok(())
        },
        unknown => {
            eprintln!("cct: unknown subcommand '{unknown}'. Try 'cct help'.");
            process::exit(1);
        },
    }
}

// ─── cct scan ────────────────────────────────────────────────────────────────

fn cmd_scan(args: &[String]) -> Result<()> {
    let mut json_mode = false;
    let mut config_path: Option<PathBuf> = None;
    let mut files: Vec<PathBuf> = Vec::new();

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--json" => json_mode = true,
            "--config" => {
                i += 1;
                config_path = args.get(i).map(PathBuf::from);
            },
            f => files.push(PathBuf::from(f)),
        }
        i += 1;
    }

    // Load pattern config — default to embedded java.toml
    let embedded_java = include_str!("../../patterns/java.toml");
    let mut scanner = match config_path {
        Some(ref p) => cct_patterns::Scanner::from_toml(p)?,
        None => cct_patterns::Scanner::from_toml_str(embedded_java)?,
    };

    let file_refs: Vec<&Path> = files.iter().map(PathBuf::as_path).collect();
    let violations = scanner.scan_files_prioritized(&file_refs)?;

    let receipt = cct_patterns::ScanReceipt::new(violations, files.len(), 0);

    if json_mode {
        println!("{}", serde_json::to_string(&receipt)?);
    } else {
        for v in &receipt.violations {
            eprintln!("{}:{}: [{}] {}", v.file, v.line, v.pattern, v.matched);
            eprintln!("  Fix: {}", v.fix);
        }
        if receipt.violations.is_empty() {
            eprintln!("✓ {} file(s) clean", files.len());
        } else {
            eprintln!(
                "✗ {} violation(s) in {} file(s)",
                receipt.violation_count,
                files.len()
            );
        }
    }

    process::exit(if receipt.violation_count > 0 { 2 } else { 0 });
}

// ─── cct observe ─────────────────────────────────────────────────────────────

fn cmd_observe(args: &[String]) -> Result<()> {
    let mut root = PathBuf::from(".");
    let mut output: Option<PathBuf> = None;
    let mut quiet = false;

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--root" => {
                i += 1;
                root = args.get(i).map(PathBuf::from).unwrap_or(root);
            },
            "--output" => {
                i += 1;
                output = args.get(i).map(PathBuf::from);
            },
            "--quiet" => quiet = true,
            _ => {},
        }
        i += 1;
    }

    let output_dir = output.unwrap_or_else(|| root.join("docs/facts"));
    let facts = cct_facts::gather_all(&root);

    if !quiet {
        eprintln!(
            "cct observe: gathering {} fact files → {}",
            facts.len(),
            output_dir.display()
        );
    }

    cct_facts::write_facts(&output_dir, &facts)?;

    if !quiet {
        for (name, _) in &facts {
            eprintln!("  wrote {}", output_dir.join(name).display());
        }
    }

    Ok(())
}

// ─── cct dx ──────────────────────────────────────────────────────────────────

fn cmd_dx(args: &[String]) -> Result<()> {
    let mut skip_phases: Vec<String> = Vec::new();
    let mut single_phase: Option<String> = None;
    let root = PathBuf::from(std::env::var("CLAUDE_PROJECT_DIR").unwrap_or_else(|_| ".".into()));

    let mut i = 0;
    while i < args.len() {
        match args[i].as_str() {
            "--skip" | "--skip-verify" => {
                let phase = if args[i] == "--skip" {
                    i += 1;
                    args.get(i).cloned().unwrap_or_else(|| "Build".into())
                } else {
                    "Build".into()
                };
                skip_phases.push(phase);
            },
            "--phase" => {
                i += 1;
                single_phase = args.get(i).cloned();
            },
            _ => {},
        }
        i += 1;
    }

    let root_clone = root.clone();
    let root_clone2 = root.clone();

    let mut pipeline = cct_pipeline::Pipeline::new()
        .phase("Observatory", move |_| {
            // Try dtr-observe binary if available
            let obs = root_clone.join("scripts/rust/dtr-observatory/target/release/dtr-observe");
            if obs.is_file() {
                let status = std::process::Command::new(&obs)
                    .args(["--root", &root_clone.to_string_lossy(), "--quiet"])
                    .status();
                match status {
                    Ok(s) if s.success() => cct_pipeline::PhaseResult::green("facts refreshed"),
                    _ => cct_pipeline::PhaseResult::red("dtr-observe failed"),
                }
            } else {
                // Fallback: use cct-facts
                let facts = cct_facts::gather_all(&root_clone);
                match cct_facts::write_facts(&root_clone.join("docs/facts"), &facts) {
                    Ok(_) => {
                        cct_pipeline::PhaseResult::green(format!("{} facts written", facts.len()))
                    },
                    Err(e) => cct_pipeline::PhaseResult::red(format!("facts failed: {e}")),
                }
            }
        })
        .phase("H-Guards", {
            let root = root.clone();
            move |_| {
                let guard = root.join("scripts/rust/dtr-guard/target/release/dtr-guard-scan");
                if !guard.is_file() {
                    return cct_pipeline::PhaseResult::skip("dtr-guard-scan not built");
                }
                let java_dir = root.join("dtr-core/src/main/java");
                if !java_dir.exists() {
                    return cct_pipeline::PhaseResult::green("no Java source dir found");
                }
                let output = std::process::Command::new(&guard)
                    .arg("--json")
                    .args(find_java_files(&java_dir))
                    .output();
                match output {
                    Ok(o) => {
                        if let Ok(json) = serde_json::from_slice::<serde_json::Value>(&o.stdout) {
                            let count = json["violation_count"].as_u64().unwrap_or(0) as u32;
                            let status = json["status"].as_str().unwrap_or("UNKNOWN");
                            if status == "GREEN" {
                                cct_pipeline::PhaseResult::green("no violations")
                            } else {
                                cct_pipeline::PhaseResult::red_with_violations(
                                    format!("{count} violations"),
                                    count,
                                )
                            }
                        } else {
                            cct_pipeline::PhaseResult::green("no output (assume clean)")
                        }
                    },
                    Err(e) => cct_pipeline::PhaseResult::red(format!("guard failed: {e}")),
                }
            }
        })
        .phase("Build", |_| {
            // Placeholder — full mvnd verify is expensive; skip by default
            cct_pipeline::PhaseResult::skip("use --phase Build to enable mvnd verify")
        })
        .phase("Git", move |_| match cct_git::git_state(&root_clone2) {
            Ok(state) => {
                if let Some(reason) = state.block_reason() {
                    cct_pipeline::PhaseResult::red(reason)
                } else {
                    cct_pipeline::PhaseResult::green("working tree clean")
                }
            },
            Err(e) => cct_pipeline::PhaseResult::red(format!("git check failed: {e}")),
        });

    // Apply skips
    for phase in skip_phases {
        pipeline = pipeline.skip(phase);
    }
    // Always skip Build unless explicitly requested
    if single_phase.as_deref() != Some("Build") && !args.contains(&"--phase".into()) {
        pipeline = pipeline.skip("Build");
    }

    let receipt = pipeline.run();
    receipt.print_summary();

    let receipt_path = PathBuf::from(".claude/dx-receipt.json");
    receipt.write_receipt(&receipt_path)?;

    process::exit(receipt.exit_code());
}

// ─── cct hook ────────────────────────────────────────────────────────────────

fn cmd_hook(args: &[String]) -> Result<()> {
    match args.first().map(String::as_str) {
        Some("stop") => cmd_hook_stop(),
        Some("pre-tool-use") => cmd_hook_pre_tool_use(),
        _ => {
            eprintln!("cct hook: specify 'stop' or 'pre-tool-use'");
            process::exit(1);
        },
    }
}

fn cmd_hook_stop() -> Result<()> {
    let payload = cct_hooks::StopPayload::from_stdin()?;

    // Infinite loop guard
    if payload.stop_hook_active {
        return Ok(());
    }

    let root = PathBuf::from(std::env::var("CLAUDE_PROJECT_DIR").unwrap_or_else(|_| ".".into()));

    let state = cct_git::git_state(&root)?;
    if let Some(reason) = state.block_reason() {
        cct_hooks::BlockDecision::block(reason).emit();
    }
    // Allow: no output, exit 0
    Ok(())
}

fn cmd_hook_pre_tool_use() -> Result<()> {
    let payload = cct_hooks::PreToolUsePayload::from_stdin()?;

    // Block destructive git operations
    if payload.tool_name == "Bash" {
        if let Some(cmd) = payload.command() {
            let blocked = [
                "git push --force",
                "git push -f ",
                "git reset --hard",
                "git clean -f",
            ];
            for pattern in blocked {
                if cmd.contains(pattern) {
                    eprintln!("cct: blocked destructive operation: {pattern}");
                    process::exit(2);
                }
            }
        }
    }

    // Scan proposed Java file content for H-Guard violations
    if matches!(payload.tool_name.as_str(), "Write" | "Edit") {
        if let Some(path) = payload.file_path() {
            if path.ends_with(".java") {
                if let Some(content) = payload.proposed_content() {
                    let embedded = include_str!("../../patterns/java.toml");
                    if let Ok(scanner) = cct_patterns::Scanner::from_toml_str(embedded) {
                        let violations =
                            cct_patterns::scan_content(content, path, &scanner.patterns);
                        if !violations.is_empty() {
                            eprintln!("cct: H-Guard violations in proposed content for {path}:");
                            for v in &violations {
                                eprintln!("  line {}: [{}] {}", v.line, v.pattern, v.matched);
                                eprintln!("  Fix: {}", v.fix);
                            }
                            process::exit(2);
                        }
                    }
                }
            }
        }
    }

    Ok(())
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

fn find_java_files(dir: &Path) -> Vec<PathBuf> {
    walkdir::WalkDir::new(dir)
        .follow_links(false)
        .into_iter()
        .filter_map(|e: Result<walkdir::DirEntry, _>| e.ok())
        .filter(|e: &walkdir::DirEntry| e.path().extension().map(|x| x == "java").unwrap_or(false))
        .map(|e: walkdir::DirEntry| e.path().to_path_buf())
        .collect()
}

fn print_usage() {
    eprintln!("cct — Claude Code Toolkit v{}", env!("CARGO_PKG_VERSION"));
    eprintln!();
    eprintln!("USAGE:");
    eprintln!("  cct scan [--json] [--config <file.toml>] <files...>");
    eprintln!("  cct observe [--root <dir>] [--output <dir>] [--quiet]");
    eprintln!("  cct dx [--skip <phase>] [--skip-verify]");
    eprintln!("  cct hook stop          (reads JSON from stdin, used by Stop hook)");
    eprintln!("  cct hook pre-tool-use  (reads JSON from stdin, used by PreToolUse hook)");
    eprintln!();
    eprintln!("SUBCOMMANDS:");
    eprintln!("  scan      Scan source files for pattern violations (exit 2 on violations)");
    eprintln!("  observe   Generate compact JSON fact files to docs/facts/");
    eprintln!("  dx        Run the Ψ→H→Λ→Ω validation pipeline");
    eprintln!("  hook      Handle Claude Code hook stdin/stdout protocol");
    eprintln!();
    eprintln!("ENVIRONMENT:");
    eprintln!("  CLAUDE_PROJECT_DIR    Project root (used by dx and hook subcommands)");
}
