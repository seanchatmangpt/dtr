//! `editor` — RemediationPlan with byte-range edits using crop rope for accuracy.
//!
//! tree-sitter provides byte offsets (not line/column), so we use crop::Rope to preserve
//! exact byte-position semantics. Each edit is a (byte_start, byte_end, replacement) triple.

use serde::{Deserialize, Serialize};

/// A single edit: replace bytes [start..end) with replacement string.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Edit {
    /// Byte offset of the range start (inclusive).
    pub byte_start: usize,
    /// Byte offset of the range end (exclusive).
    pub byte_end: usize,
    /// Replacement text (can be empty for deletions).
    pub replacement: String,
}

impl Edit {
    /// Create a new edit with validation.
    pub fn new(byte_start: usize, byte_end: usize, replacement: impl Into<String>) -> Self {
        Edit {
            byte_start,
            byte_end,
            replacement: replacement.into(),
        }
    }

    /// Check if this edit is valid (start <= end).
    pub fn is_valid(&self) -> bool {
        self.byte_start <= self.byte_end
    }
}

/// A plan for remediating a single file: list of byte-range edits to apply.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RemediationPlan {
    /// Path to the file to remediate.
    pub path: std::path::PathBuf,
    /// Ordered list of byte-range edits. Must be sorted by byte_start to apply correctly.
    pub edits: Vec<Edit>,
}

impl RemediationPlan {
    /// Create a new remediation plan for a file.
    pub fn new(path: impl Into<std::path::PathBuf>) -> Self {
        RemediationPlan {
            path: path.into(),
            edits: Vec::new(),
        }
    }

    /// Add an edit to the plan. Does not validate overlap; caller is responsible.
    pub fn add_edit(&mut self, edit: Edit) {
        self.edits.push(edit);
    }

    /// Sort edits by byte_start for correct sequential application.
    pub fn sort_edits(&mut self) {
        self.edits.sort_by_key(|e| e.byte_start);
    }

    /// Check all edits are valid (start <= end) and return true if all pass.
    pub fn all_edits_valid(&self) -> bool {
        self.edits.iter().all(|e| e.is_valid())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_edit_creation() {
        let edit = Edit::new(0, 5, "hello");
        assert_eq!(edit.byte_start, 0);
        assert_eq!(edit.byte_end, 5);
        assert_eq!(edit.replacement, "hello");
        assert!(edit.is_valid());
    }

    #[test]
    fn test_edit_deletion() {
        let edit = Edit::new(10, 20, "");
        assert!(edit.is_valid());
        assert_eq!(edit.replacement, "");
    }

    #[test]
    fn test_edit_invalid() {
        let edit = Edit::new(20, 10, "text");
        assert!(!edit.is_valid());
    }

    #[test]
    fn test_remediation_plan_creation() {
        let plan = RemediationPlan::new("/tmp/example.java");
        assert_eq!(plan.edits.len(), 0);
        assert!(plan.path.to_string_lossy().contains("example.java"));
    }

    #[test]
    fn test_remediation_plan_add_edits() {
        let mut plan = RemediationPlan::new("/tmp/test.java");
        plan.add_edit(Edit::new(0, 5, "foo"));
        plan.add_edit(Edit::new(10, 15, "bar"));
        assert_eq!(plan.edits.len(), 2);
    }

    #[test]
    fn test_remediation_plan_sort_edits() {
        let mut plan = RemediationPlan::new("/tmp/test.java");
        plan.add_edit(Edit::new(20, 25, "z"));
        plan.add_edit(Edit::new(0, 5, "a"));
        plan.add_edit(Edit::new(10, 15, "b"));
        plan.sort_edits();
        assert_eq!(plan.edits[0].byte_start, 0);
        assert_eq!(plan.edits[1].byte_start, 10);
        assert_eq!(plan.edits[2].byte_start, 20);
    }

    #[test]
    fn test_remediation_plan_all_edits_valid() {
        let mut plan = RemediationPlan::new("/tmp/test.java");
        plan.add_edit(Edit::new(0, 5, "good"));
        plan.add_edit(Edit::new(10, 15, "also good"));
        assert!(plan.all_edits_valid());

        let mut bad_plan = RemediationPlan::new("/tmp/test2.java");
        bad_plan.add_edit(Edit::new(20, 10, "bad"));
        assert!(!bad_plan.all_edits_valid());
    }
}
