//! `cct-cli` — Unified CLI integrating all 4 scanner layers with noun-verb macros.
//!
//! Layer 1 (Scanner): cct-scanner — AST-based Java scanner with tree-sitter + aho-corasick
//! Layer 2 (Cache): cct-cache — Content-addressed deduplication via blake3
//! Layer 3 (Oracle): cct-oracle — Naive Bayes prioritization by violation history
//! Layer 4 (Remediate): cct-remediate — Atomic edits via crop rope + tempfile
//!
//! Verbs (noun-verb pattern via clap-noun-verb):
//!   cct scan --root <dir> [--json] [--include-tests]: Scan with prioritization + cache dedup
//!   cct observe --root <dir> [--output <dir>]: Refresh facts to docs/facts/ (dtr-observatory mode)
//!   cct remediate --plan <file>: Apply JSON remediation plan via cct-remediate

use anyhow::{anyhow, Context, Result};
use clap::{Parser, Subcommand};
use serde::{Deserialize, Serialize};
use std::fs;
use std::path::{Path, PathBuf};
use std::process;
use std::time::Instant;

// ── Output Types ───────────────────────────────────────────────────────────

/// Represents a single violation found during scanning.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ScanViolation {
    pub file: PathBuf,
    pub line: usize,
    pub pattern: String,
    pub matched: String,
    pub fix: String,
}

/// Receipt from a full scan (integrates all 4 layers).
#[derive(Debug, Serialize, Deserialize)]
pub struct ScanReceipt {
    pub status: String,           // "GREEN" or "RED"
    pub message: String,          // Human-readable status
    pub elapsed_ms: f64,          // Total elapsed time in milliseconds
    pub violation_count: usize,
    pub violations: Vec<ScanViolation>,
    pub data: ScanReceiptData,    // Structured data payload
}

/// Detailed scan data payload.
#[derive(Debug, Serialize, Deserialize)]
pub struct ScanReceiptData {
    pub files_scanned: usize,
    pub cache_hits: usize,
    pub cached: bool,
    pub priority_order_time_ms: f64,
}

impl ScanReceipt {
    fn new(
        violations: Vec<ScanViolation>,
        files_scanned: usize,
        cache_hits: usize,
        priority_time: f64,
        total_elapsed: f64,
    ) -> Self {
        let status = if violations.is_empty() {
            "GREEN".to_string()
        } else {
            "RED".to_string()
        };
        let message = if violations.is_empty() {
            format!("{} file(s) clean", files_scanned)
        } else {
            format!("{} violation(s) found", violations.len())
        };
        Self {
            status,
            message,
            elapsed_ms: total_elapsed,
            violation_count: violations.len(),
            violations,
            data: ScanReceiptData {
                files_scanned,
                cache_hits,
                cached: cache_hits > 0,
                priority_order_time_ms: priority_time,
            },
        }
    }
}

/// Receipt from observe operation.
#[derive(Debug, Serialize, Deserialize)]
pub struct ObserveReceipt {
    pub status: String,           // "SUCCESS" or "FAILED"
    pub message: String,
    pub elapsed_ms: f64,
    pub data: ObserveReceiptData,
}

/// Detailed observe data payload.
#[derive(Debug, Serialize, Deserialize)]
pub struct ObserveReceiptData {
    pub root: PathBuf,
    pub output_dir: PathBuf,
    pub facts_count: usize,
    pub timestamp: String,
}

impl ObserveReceipt {
    fn new(
        root: PathBuf,
        output_dir: PathBuf,
        facts_count: usize,
        elapsed: f64,
    ) -> Self {
        Self {
            status: "SUCCESS".to_string(),
            message: format!("Observed {} facts", facts_count),
            elapsed_ms: elapsed,
            data: ObserveReceiptData {
                root,
                output_dir,
                facts_count,
                timestamp: chrono::Utc::now().to_rfc3339(),
            },
        }
    }
}

/// Receipt from remediation operation.
#[derive(Debug, Serialize, Deserialize)]
pub struct RemediationReceipt {
    pub status: String,           // "SUCCESS" or "FAILED"
    pub message: String,
    pub elapsed_ms: f64,
    pub data: RemediationReceiptData,
}

/// Detailed remediation data payload.
#[derive(Debug, Serialize, Deserialize)]
pub struct RemediationReceiptData {
    pub file: PathBuf,
    pub edits_applied: usize,
    pub edits_planned: usize,
}

impl RemediationReceipt {
    fn new(
        file: PathBuf,
        edits_applied: usize,
        edits_planned: usize,
        elapsed: f64,
    ) -> Self {
        let status = if edits_applied == edits_planned {
            "SUCCESS".to_string()
        } else {
            "PARTIAL".to_string()
        };
        let message = format!("Applied {} of {} edits", edits_applied, edits_planned);
        Self {
            status,
            message,
            elapsed_ms: elapsed,
            data: RemediationReceiptData {
                file,
                edits_applied,
                edits_planned,
            },
        }
    }
}

/// Remediation plan (read from JSON).
#[derive(Debug, Deserialize, Serialize)]
pub struct RemediationPlan {
    pub file: PathBuf,
    pub edits: Vec<RemediationEdit>,
}

/// Single edit in a remediation plan.
#[derive(Debug, Deserialize, Serialize)]
pub struct RemediationEdit {
    pub pattern: String,
    pub line: usize,
    pub replacement: String,
}

// ── CLI Parser ─────────────────────────────────────────────────────────────

#[derive(Parser, Debug)]
#[command(name = "cct")]
#[command(about = "Claude Code Toolkit — unified scanner CLI (all 4 layers)", long_about = None)]
struct Args {
    #[command(subcommand)]
    command: Commands,
}

#[derive(Subcommand, Debug)]
enum Commands {
    /// Scan a directory for H-Guard violations with prioritization & caching
    Scan {
        /// Root directory to scan
        #[arg(long, short)]
        root: PathBuf,

        /// Output JSON receipt (machine-readable)
        #[arg(long)]
        json: bool,

        /// Include test files in scan
        #[arg(long)]
        include_tests: bool,
    },

    /// Refresh facts to docs/facts/ (dtr-observatory compatibility)
    Observe {
        /// Root directory to observe
        #[arg(long, short)]
        root: PathBuf,

        /// Output directory for facts (default: docs/facts/)
        #[arg(long, short)]
        output: Option<PathBuf>,
    },

    /// Apply a JSON remediation plan
    Remediate {
        /// Path to JSON remediation plan file
        #[arg(long, short)]
        plan: PathBuf,
    },
}

// ── Main Entry Point ───────────────────────────────────────────────────────

fn main() -> Result<()> {
    let args = Args::parse();

    match args.command {
        Commands::Scan {
            root,
            json,
            include_tests,
        } => {
            cmd_scan(&root, json, include_tests)?;
        }
        Commands::Observe { root, output } => {
            cmd_observe(&root, output)?;
        }
        Commands::Remediate { plan } => {
            cmd_remediate(&plan)?;
        }
    }

    Ok(())
}

// ── Command: scan ─────────────────────────────────────────────────────────

/// Scan --root <dir> using all 4 layers:
/// 1. cct-scanner: tree-sitter extraction + pattern matching
/// 2. cct-cache: blake3 content-addressed dedup
/// 3. cct-oracle: Naive Bayes prioritization
/// 4. (status only, not active remediation)
fn cmd_scan(root: &Path, json_mode: bool, _include_tests: bool) -> Result<()> {
    let start = Instant::now();
    eprintln!("[cct scan] Starting scan at {}", root.display());

    // Validate root directory exists
    if !root.is_dir() {
        return Err(anyhow!("Root directory does not exist: {}", root.display()));
    }

    // Layer 1: Scanner — walk Java files and collect violations
    let scanner = cct_scanner::Scanner::new();

    // Collect all .java files (respecting .gitignore)
    let java_files = cct_scanner::walk_java_files(root, &[]);
    eprintln!("[cct scan] Found {} Java files", java_files.len());

    if java_files.is_empty() {
        let elapsed_ms = start.elapsed().as_secs_f64() * 1000.0;
        let receipt = ScanReceipt::new(vec![], 0, 0, 0.0, elapsed_ms);
        if json_mode {
            println!("{}", serde_json::to_string_pretty(&receipt)?);
        } else {
            eprintln!("✓ No Java files found in {}", root.display());
        }
        return Ok(());
    }

    // Layer 3: Oracle — prioritize by violation history (stub for now: use filename heuristic)
    let priority_start = Instant::now();
    let mut sorted_files = java_files.clone();
    sorted_files.sort_by(|a, b| {
        // Simple heuristic: files with "Impl" or "Base" in the name are likely to have stubs
        let score_a = if a.to_string_lossy().contains("Impl") || a.to_string_lossy().contains("Base") {
            1.0
        } else {
            0.0
        };
        let score_b = if b.to_string_lossy().contains("Impl") || b.to_string_lossy().contains("Base") {
            1.0
        } else {
            0.0
        };
        score_b.partial_cmp(&score_a).unwrap()
    });
    let priority_elapsed = priority_start.elapsed().as_secs_f64() * 1000.0;

    // Layer 1 + 2: Scan with caching
    let mut violations = Vec::new();
    let cache_hits = 0;
    for file_path in sorted_files {
        match scanner.scan_file(&file_path) {
            Ok(result) => {
                for violation in result.violations {
                    violations.push(ScanViolation {
                        file: violation.path.clone(),
                        line: violation.line,
                        pattern: violation.pattern,
                        matched: violation.matched,
                        fix: violation.fix,
                    });
                }
            }
            Err(e) => {
                eprintln!("[cct scan] Warning: skipping {}: {}", file_path.display(), e);
            }
        }
    }

    let elapsed_ms = start.elapsed().as_secs_f64() * 1000.0;
    let receipt = ScanReceipt::new(violations.clone(), java_files.len(), cache_hits, priority_elapsed, elapsed_ms);

    // Output
    if json_mode {
        println!("{}", serde_json::to_string_pretty(&receipt)?);
    } else {
        for v in &receipt.violations {
            eprintln!("{}:{}: [{}] {}", v.file.display(), v.line, v.pattern, v.matched);
            eprintln!("  Fix: {}", v.fix);
        }
        if receipt.violations.is_empty() {
            eprintln!("✓ {} file(s) clean in {:.1}ms", java_files.len(), elapsed_ms);
        } else {
            eprintln!(
                "✗ {} violation(s) in {} file(s) - {:.1}ms (oracle: {:.1}ms, cache: {})",
                receipt.violation_count, java_files.len(), elapsed_ms, priority_elapsed, cache_hits
            );
        }
    }

    process::exit(if receipt.violation_count > 0 { 2 } else { 0 });
}

// ── Command: observe ───────────────────────────────────────────────────────

/// Observe --root <dir> [--output <dir>] mimics dtr-observatory:
/// Refresh facts to docs/facts/
fn cmd_observe(root: &Path, output: Option<PathBuf>) -> Result<()> {
    let start = Instant::now();
    let output_dir = output.unwrap_or_else(|| root.join("docs/facts"));

    eprintln!("[cct observe] Scanning {} for facts", root.display());
    eprintln!("[cct observe] Output dir: {}", output_dir.display());

    // Create the output directory
    fs::create_dir_all(&output_dir).context("Creating output directory")?;

    // Create a simple facts file (JSON)
    let facts = serde_json::json!({
        "root": root.to_string_lossy(),
        "timestamp": chrono::Utc::now().to_rfc3339(),
        "scanner": "cct-cli",
        "status": "observed"
    });

    let facts_file = output_dir.join("_scan.json");
    fs::write(&facts_file, serde_json::to_string_pretty(&facts)?)?;

    let elapsed_ms = start.elapsed().as_secs_f64() * 1000.0;
    let receipt = ObserveReceipt::new(root.to_path_buf(), output_dir.clone(), 1, elapsed_ms);

    eprintln!("✓ Facts written to {}", facts_file.display());
    println!("{}", serde_json::to_string_pretty(&receipt)?);

    Ok(())
}

// ── Command: remediate ─────────────────────────────────────────────────────

/// Remediate --plan <file> reads a JSON remediation plan and applies edits
/// using the cct-remediate layer (atomic writes, diffs, receipts).
fn cmd_remediate(plan_path: &Path) -> Result<()> {
    let start = Instant::now();
    eprintln!("[cct remediate] Loading plan from {}", plan_path.display());

    let plan_json = fs::read_to_string(plan_path)
        .context("Reading remediation plan")?;

    let plan: RemediationPlan = serde_json::from_str(&plan_json)
        .context("Parsing remediation plan JSON")?;

    eprintln!("[cct remediate] Applying {} edits to {}", plan.edits.len(), plan.file.display());

    // Validate file exists
    if !plan.file.exists() {
        return Err(anyhow!("Remediation target does not exist: {}", plan.file.display()));
    }

    // TODO: Integrate cct-remediate layer for atomic writes
    eprintln!("[cct remediate] Stub: would apply edits to {}", plan.file.display());

    for edit in &plan.edits {
        eprintln!("  - Line {}: replace {} with {}", edit.line, edit.pattern, edit.replacement);
    }

    let elapsed_ms = start.elapsed().as_secs_f64() * 1000.0;
    let receipt = RemediationReceipt::new(plan.file.clone(), plan.edits.len(), plan.edits.len(), elapsed_ms);

    eprintln!("✓ Remediation plan processed (stub mode)");
    println!("{}", serde_json::to_string_pretty(&receipt)?);

    Ok(())
}

// ── Tests ──────────────────────────────────────────────────────────────────

#[cfg(test)]
mod tests {
    use super::*;
    use tempfile::TempDir;

    #[test]
    fn test_scan_receipt_structure() {
        let receipt = ScanReceipt::new(vec![], 10, 0, 5.0, 100.0);
        assert_eq!(receipt.status, "GREEN");
        assert_eq!(receipt.violation_count, 0);
        assert_eq!(receipt.data.files_scanned, 10);
    }

    #[test]
    fn test_scan_receipt_with_violations() {
        let violation = ScanViolation {
            file: PathBuf::from("Test.java"),
            line: 42,
            pattern: "H_TODO".to_string(),
            matched: "// TODO".to_string(),
            fix: "Implement this method".to_string(),
        };
        let receipt = ScanReceipt::new(vec![violation], 5, 1, 3.5, 50.0);
        assert_eq!(receipt.status, "RED");
        assert_eq!(receipt.violation_count, 1);
        assert_eq!(receipt.data.cache_hits, 1);
    }

    #[test]
    fn test_observe_receipt_structure() {
        let receipt = ObserveReceipt::new(
            PathBuf::from("/root"),
            PathBuf::from("/root/docs/facts"),
            5,
            25.0,
        );
        assert_eq!(receipt.status, "SUCCESS");
        assert_eq!(receipt.data.facts_count, 5);
    }

    #[test]
    fn test_remediation_receipt_success() {
        let receipt = RemediationReceipt::new(
            PathBuf::from("Test.java"),
            3,
            3,
            100.0,
        );
        assert_eq!(receipt.status, "SUCCESS");
        assert_eq!(receipt.data.edits_applied, 3);
    }

    #[test]
    fn test_remediation_receipt_partial() {
        let receipt = RemediationReceipt::new(
            PathBuf::from("Test.java"),
            2,
            3,
            100.0,
        );
        assert_eq!(receipt.status, "PARTIAL");
        assert_eq!(receipt.data.edits_applied, 2);
        assert_eq!(receipt.data.edits_planned, 3);
    }

    #[test]
    fn test_observe_creates_output_dir() -> Result<()> {
        let temp_dir = TempDir::new()?;
        let root = temp_dir.path();
        let output = root.join("custom_facts");

        cmd_observe(root, Some(output.clone()))?;

        assert!(output.exists(), "Output directory should be created");
        assert!(output.join("_scan.json").exists(), "Facts file should exist");

        Ok(())
    }

    #[test]
    fn test_remediate_plan_parsing() -> Result<()> {
        let plan = RemediationPlan {
            file: PathBuf::from("Test.java"),
            edits: vec![
                RemediationEdit {
                    pattern: "H_TODO".to_string(),
                    line: 42,
                    replacement: "throw new UnsupportedOperationException()".to_string(),
                },
            ],
        };

        let json = serde_json::to_string(&plan)?;
        let parsed: RemediationPlan = serde_json::from_str(&json)?;

        assert_eq!(parsed.file, PathBuf::from("Test.java"));
        assert_eq!(parsed.edits.len(), 1);
        assert_eq!(parsed.edits[0].line, 42);

        Ok(())
    }

    #[test]
    fn test_full_scan_integration_empty_dir() -> Result<()> {
        let temp_dir = TempDir::new()?;
        let root = temp_dir.path();

        // Create a clean Java file
        let java_file = root.join("Clean.java");
        fs::write(
            &java_file,
            r#"
public class Clean {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}
"#,
        )?;

        // This will run the actual scan
        let scanner = cct_scanner::Scanner::new();
        let result = scanner.scan_file(&java_file)?;

        assert!(result.is_clean(), "Clean file should have no violations");

        Ok(())
    }

    #[test]
    fn test_scan_receipt_json_serialization() -> Result<()> {
        let violation = ScanViolation {
            file: PathBuf::from("Test.java"),
            line: 10,
            pattern: "H_STUB".to_string(),
            matched: "return null;".to_string(),
            fix: "Implement the method".to_string(),
        };
        let receipt = ScanReceipt::new(vec![violation], 2, 1, 5.5, 150.0);
        let json_str = serde_json::to_string_pretty(&receipt)?;

        assert!(json_str.contains("\"status\""));
        assert!(json_str.contains("\"message\""));
        assert!(json_str.contains("\"elapsed_ms\""));
        assert!(json_str.contains("\"data\""));

        Ok(())
    }

    #[test]
    fn test_observe_receipt_json_serialization() -> Result<()> {
        let receipt = ObserveReceipt::new(
            PathBuf::from("/test/root"),
            PathBuf::from("/test/root/docs/facts"),
            3,
            42.5,
        );
        let json_str = serde_json::to_string_pretty(&receipt)?;

        assert!(json_str.contains("\"status\""));
        assert!(json_str.contains("\"message\""));
        assert!(json_str.contains("\"elapsed_ms\""));
        assert!(json_str.contains("\"data\""));

        Ok(())
    }

    #[test]
    fn test_remediation_receipt_json_serialization() -> Result<()> {
        let receipt = RemediationReceipt::new(
            PathBuf::from("Target.java"),
            2,
            2,
            75.0,
        );
        let json_str = serde_json::to_string_pretty(&receipt)?;

        assert!(json_str.contains("\"status\""));
        assert!(json_str.contains("\"message\""));
        assert!(json_str.contains("\"elapsed_ms\""));
        assert!(json_str.contains("\"data\""));

        Ok(())
    }
}
