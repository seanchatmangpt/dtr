//! Multi-phase validation pipeline framework.
//!
//! Generalizes `scripts/dx.sh` into a typed Rust API. Each phase is a named
//! function that returns [`PhaseResult`]. The pipeline collects results, writes
//! a machine-readable JSON receipt, and returns an overall status.
//!
//! # Usage
//! ```no_run
//! use cct_pipeline::{Pipeline, PhaseResult};
//!
//! let result = Pipeline::new()
//!     .phase("Observatory", |_| PhaseResult::green("facts refreshed"))
//!     .phase("H-Guards",    |_| PhaseResult::red("98 violations"))
//!     .phase("Build",       |_| PhaseResult::skip("--skip-verify"))
//!     .phase("Git",         |_| PhaseResult::green("clean"))
//!     .skip("Build")
//!     .run();
//!
//! println!("Overall: {}", result.overall);
//! result.write_receipt(std::path::Path::new(".claude/dx-receipt.json")).unwrap();
//! ```

use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::Path;
use std::time::Instant;

// ─── Phase result ─────────────────────────────────────────────────────────────

/// Status of a single pipeline phase.
#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(rename_all = "lowercase")]
pub enum Status {
    Green,
    Red,
    Skip,
}

impl Status {
    pub fn as_str(&self) -> &'static str {
        match self {
            Status::Green => "green",
            Status::Red => "red",
            Status::Skip => "skip",
        }
    }

    pub fn is_failure(&self) -> bool {
        matches!(self, Status::Red)
    }
}

/// Result of a single pipeline phase.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct PhaseResult {
    pub status: Status,
    pub message: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub elapsed_ms: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub violations: Option<u32>,
}

impl PhaseResult {
    pub fn green(msg: impl Into<String>) -> Self {
        Self {
            status: Status::Green,
            message: msg.into(),
            elapsed_ms: None,
            violations: None,
        }
    }
    pub fn red(msg: impl Into<String>) -> Self {
        Self {
            status: Status::Red,
            message: msg.into(),
            elapsed_ms: None,
            violations: None,
        }
    }
    pub fn red_with_violations(msg: impl Into<String>, count: u32) -> Self {
        Self {
            status: Status::Red,
            message: msg.into(),
            elapsed_ms: None,
            violations: Some(count),
        }
    }
    pub fn skip(msg: impl Into<String>) -> Self {
        Self {
            status: Status::Skip,
            message: msg.into(),
            elapsed_ms: None,
            violations: None,
        }
    }
}

// ─── Pipeline ─────────────────────────────────────────────────────────────────

type PhaseFn = Box<dyn Fn(&PipelineContext) -> PhaseResult>;

/// Context passed to each phase function.
#[derive(Debug, Default)]
pub struct PipelineContext {
    /// Phases explicitly requested to skip
    pub skip_phases: Vec<String>,
    /// Extra key-value config passed from CLI
    pub config: HashMap<String, String>,
}

/// Builder for a multi-phase validation pipeline.
pub struct Pipeline {
    phases: Vec<(String, PhaseFn)>,
    context: PipelineContext,
}

impl Pipeline {
    pub fn new() -> Self {
        Self {
            phases: Vec::new(),
            context: PipelineContext::default(),
        }
    }

    /// Register a named phase function.
    pub fn phase(
        mut self,
        name: impl Into<String>,
        f: impl Fn(&PipelineContext) -> PhaseResult + 'static,
    ) -> Self {
        self.phases.push((name.into(), Box::new(f)));
        self
    }

    /// Mark a phase to be skipped (returns PhaseResult::skip automatically).
    pub fn skip(mut self, name: impl Into<String>) -> Self {
        self.context.skip_phases.push(name.into());
        self
    }

    /// Add config value available to all phases.
    pub fn config(mut self, key: impl Into<String>, value: impl Into<String>) -> Self {
        self.context.config.insert(key.into(), value.into());
        self
    }

    /// Run all phases and return the aggregate receipt.
    pub fn run(self) -> PipelineReceipt {
        let start = Instant::now();
        let mut phase_results: HashMap<String, PhaseResult> = HashMap::new();
        let mut overall_green = true;
        let mut total_violations = 0u32;

        for (name, phase_fn) in &self.phases {
            let phase_start = Instant::now();
            let mut result = if self.context.skip_phases.contains(name) {
                PhaseResult::skip(format!("phase '{}' skipped", name))
            } else {
                phase_fn(&self.context)
            };

            result.elapsed_ms = Some(phase_start.elapsed().as_millis() as u64);

            if result.status.is_failure() {
                overall_green = false;
            }
            if let Some(v) = result.violations {
                total_violations += v;
            }

            phase_results.insert(name.clone(), result);
        }

        PipelineReceipt {
            overall: if overall_green { "green" } else { "red" },
            elapsed_ms: start.elapsed().as_millis() as u64,
            violations: total_violations,
            phases: phase_results,
            generated: iso8601_now(),
        }
    }
}

impl Default for Pipeline {
    fn default() -> Self {
        Self::new()
    }
}

// ─── Receipt ──────────────────────────────────────────────────────────────────

/// Machine-readable pipeline result. Written to `.claude/dx-receipt.json`.
#[derive(Debug, Serialize, Deserialize)]
pub struct PipelineReceipt {
    pub overall: &'static str,
    pub generated: String,
    pub elapsed_ms: u64,
    pub violations: u32,
    pub phases: HashMap<String, PhaseResult>,
}

impl PipelineReceipt {
    pub fn is_green(&self) -> bool {
        self.overall == "green"
    }

    /// Write the receipt as minified JSON.
    pub fn write_receipt(&self, path: &Path) -> Result<()> {
        if let Some(parent) = path.parent() {
            std::fs::create_dir_all(parent)?;
        }
        let json = serde_json::to_string(self)?;
        std::fs::write(path, json)?;
        Ok(())
    }

    /// Print a human-readable summary to stdout.
    pub fn print_summary(&self) {
        let green = "\x1b[0;32m";
        let red = "\x1b[0;31m";
        let yellow = "\x1b[0;33m";
        let reset = "\x1b[0m";

        for (name, result) in &self.phases {
            let (sym, color) = match result.status {
                Status::Green => ("✓", green),
                Status::Red => ("✗", red),
                Status::Skip => ("—", yellow),
            };
            let elapsed = result
                .elapsed_ms
                .map(|ms| format!(" ({}ms)", ms))
                .unwrap_or_default();
            eprintln!(
                "  {color}{sym}{reset} Phase {name}: {}{elapsed}",
                result.message
            );
        }

        eprintln!();
        if self.is_green() {
            eprintln!("  {green}● ALL PHASES GREEN{reset} ({}ms)", self.elapsed_ms);
        } else {
            eprintln!(
                "  {red}● RED — one or more phases failed{reset} ({}ms)",
                self.elapsed_ms
            );
        }
    }

    /// Exit code: 0 if green, 2 if red (matches dx.sh convention).
    pub fn exit_code(&self) -> i32 {
        if self.is_green() {
            0
        } else {
            2
        }
    }
}

fn iso8601_now() -> String {
    std::process::Command::new("date")
        .args(["-u", "+%Y-%m-%dT%H:%M:%SZ"])
        .output()
        .ok()
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_owned())
        .unwrap_or_else(|| "unknown".into())
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn all_green_pipeline() {
        let receipt = Pipeline::new()
            .phase("A", |_| PhaseResult::green("ok"))
            .phase("B", |_| PhaseResult::green("ok"))
            .run();
        assert!(receipt.is_green());
        assert_eq!(receipt.exit_code(), 0);
    }

    #[test]
    fn one_red_phase_makes_overall_red() {
        let receipt = Pipeline::new()
            .phase("A", |_| PhaseResult::green("ok"))
            .phase("B", |_| PhaseResult::red("failed"))
            .run();
        assert!(!receipt.is_green());
        assert_eq!(receipt.exit_code(), 2);
    }

    #[test]
    fn skipped_phase_does_not_fail() {
        let receipt = Pipeline::new()
            .phase("Build", |_| PhaseResult::red("would fail"))
            .skip("Build")
            .run();
        assert!(receipt.is_green());
    }

    #[test]
    fn violations_are_summed() {
        let receipt = Pipeline::new()
            .phase("A", |_| PhaseResult::red_with_violations("violations", 10))
            .phase("B", |_| PhaseResult::red_with_violations("violations", 5))
            .run();
        assert_eq!(receipt.violations, 15);
    }

    #[test]
    fn receipt_round_trips_json() {
        let receipt = Pipeline::new()
            .phase("X", |_| PhaseResult::green("ok"))
            .run();
        let json = serde_json::to_string(&receipt).unwrap();
        let parsed: serde_json::Value = serde_json::from_str(&json).unwrap();
        assert_eq!(parsed["overall"].as_str().unwrap(), receipt.overall);
    }

    #[test]
    fn phase_result_elapsed_is_recorded() {
        let receipt = Pipeline::new()
            .phase("Slow", |_| PhaseResult::green("ok"))
            .run();
        let phase = receipt.phases.get("Slow").unwrap();
        assert!(phase.elapsed_ms.is_some());
    }
}
