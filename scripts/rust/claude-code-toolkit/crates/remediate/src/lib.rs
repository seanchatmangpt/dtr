//! `cct-remediate` — Atomic remediation engine using crop rope byte-offsets, similar diffs, and tempfile.
//!
//! Architecture:
//! - [`editor`]: `RemediationPlan` captures a sequence of byte-range edits; uses crop::Rope for
//!   byte-accurate offset handling (tree-sitter gives byte offsets, rope preserves them).
//! - [`differ`]: `apply_edits` applies the plan to source bytes and returns unified diff via similar crate.
//! - [`writer`]: Atomic write with `tempfile::NamedTempFile` — write to temp, rename into place,
//!   never partial-write to the source file.
//! - [`RemediationReceipt`]: Captures before_hash, after_hash, diff, and success flag for auditability.

use serde::{Deserialize, Serialize};

pub mod editor;
pub mod differ;
pub mod writer;

pub use editor::{Edit, RemediationPlan};
pub use differ::apply_edits;
pub use writer::atomic_write;

/// Proof-of-remediation: hashes before/after, diff output, and success flag.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RemediationReceipt {
    /// SHA256 or blake3 hash of source before edits.
    pub before_hash: String,
    /// SHA256 or blake3 hash of source after edits.
    pub after_hash: String,
    /// Unified diff (human-readable) showing the changes.
    pub diff: String,
    /// True if edits were successfully applied and written atomically.
    pub success: bool,
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_remediation_receipt_structure() {
        let receipt = RemediationReceipt {
            before_hash: "abc123".to_owned(),
            after_hash: "def456".to_owned(),
            diff: "--- before\n+++ after\n".to_owned(),
            success: true,
        };
        assert_eq!(receipt.before_hash, "abc123");
        assert_eq!(receipt.after_hash, "def456");
        assert!(receipt.success);
    }
}
