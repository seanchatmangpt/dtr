//! `differ` — Apply edits to source bytes and generate unified diffs.
//!
//! Uses crop::Rope for byte-accurate rope operations, then similar crate for unified diff.

use crate::editor::RemediationPlan;
use anyhow::{anyhow, Result};
use similar::TextDiff;

/// Apply edits from a remediation plan to source bytes.
/// Returns the modified source and a unified diff string.
///
/// # Arguments
/// * `source` - Original source bytes
/// * `plan` - RemediationPlan with sorted edits (caller must ensure sort_edits() was called)
///
/// # Returns
/// Tuple of (new_source_bytes, unified_diff_string)
pub fn apply_edits(source: &[u8], plan: &RemediationPlan) -> Result<(Vec<u8>, String)> {
    // Validate all edits are well-formed
    if !plan.all_edits_valid() {
        return Err(anyhow!("RemediationPlan contains invalid edits (start > end)"));
    }

    // Ensure edits are sorted by byte_start for sequential application
    let mut sorted_edits = plan.edits.clone();
    sorted_edits.sort_by_key(|e| e.byte_start);

    // Apply edits in reverse order (highest byte_start first) to avoid offset drift
    let mut result = source.to_vec();
    for edit in sorted_edits.iter().rev() {
        // Validate byte ranges are within bounds
        if edit.byte_end > result.len() {
            return Err(anyhow!(
                "Edit end offset {} exceeds source length {}",
                edit.byte_end,
                result.len()
            ));
        }

        // Remove the old bytes and insert the replacement
        result.drain(edit.byte_start..edit.byte_end);
        result.splice(edit.byte_start..edit.byte_start, edit.replacement.as_bytes().iter().cloned());
    }

    // Generate unified diff by converting to owned strings
    let before_str = String::from_utf8_lossy(source).into_owned();
    let after_str = String::from_utf8_lossy(&result).into_owned();

    // Create diff using owned strings
    let diff = TextDiff::from_lines(before_str.as_str(), after_str.as_str());

    // Collect changes into owned format for diff output
    let mut unified_diff = String::new();
    for change in diff.iter_all_changes() {
        let line = change.value();
        match change.tag() {
            similar::ChangeTag::Delete => unified_diff.push_str(&format!("-{}", line)),
            similar::ChangeTag::Insert => unified_diff.push_str(&format!("+{}", line)),
            similar::ChangeTag::Equal => unified_diff.push_str(&format!(" {}", line)),
        }
    }

    Ok((result, unified_diff))
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::editor::Edit;

    #[test]
    fn test_apply_single_edit() {
        let source = b"hello world";
        let mut plan = RemediationPlan::new("/tmp/test.txt");
        plan.add_edit(Edit::new(0, 5, "goodbye"));
        plan.sort_edits();

        let (result, diff) = apply_edits(source, &plan).expect("apply_edits failed");
        let result_str = str::from_utf8(&result).expect("UTF-8");
        assert_eq!(result_str, "goodbye world");
        assert!(diff.contains("goodbye"));
    }

    #[test]
    fn test_apply_multiple_edits_reverse_order() {
        let source = b"one two three";
        let mut plan = RemediationPlan::new("/tmp/test.txt");
        plan.add_edit(Edit::new(8, 13, "3"));  // "three" -> "3"
        plan.add_edit(Edit::new(4, 7, "2"));   // "two" -> "2"
        plan.add_edit(Edit::new(0, 3, "1"));   // "one" -> "1"
        plan.sort_edits();

        let (result, _diff) = apply_edits(source, &plan).expect("apply_edits failed");
        let result_str = str::from_utf8(&result).expect("UTF-8");
        assert_eq!(result_str, "1 2 3");
    }

    #[test]
    fn test_apply_deletion_edit() {
        let source = b"hello cruel world";
        let mut plan = RemediationPlan::new("/tmp/test.txt");
        plan.add_edit(Edit::new(6, 12, ""));  // Remove " cruel"
        plan.sort_edits();

        let (result, _diff) = apply_edits(source, &plan).expect("apply_edits failed");
        let result_str = str::from_utf8(&result).expect("UTF-8");
        assert_eq!(result_str, "hello world");
    }

    #[test]
    fn test_apply_edits_generates_diff() {
        let source = b"aaa\nbbb\nccc\n";
        let mut plan = RemediationPlan::new("/tmp/test.txt");
        plan.add_edit(Edit::new(4, 7, "BBB"));  // "bbb" -> "BBB"
        plan.sort_edits();

        let (_result, diff) = apply_edits(source, &plan).expect("apply_edits failed");
        assert!(diff.len() > 0, "diff should not be empty");
        assert!(diff.contains("BBB") || diff.contains("-"), "diff should contain change");
    }

    #[test]
    fn test_apply_edits_out_of_bounds() {
        let source = b"hello";
        let mut plan = RemediationPlan::new("/tmp/test.txt");
        plan.add_edit(Edit::new(0, 100, "hi"));  // End is beyond source
        plan.sort_edits();

        let result = apply_edits(source, &plan);
        assert!(result.is_err(), "should fail with out-of-bounds edit");
    }

    #[test]
    fn test_apply_edits_invalid_plan() {
        let source = b"hello";
        let mut plan = RemediationPlan::new("/tmp/test.txt");
        plan.add_edit(Edit::new(20, 10, "bad"));  // Invalid: start > end
        plan.sort_edits();

        let result = apply_edits(source, &plan);
        assert!(result.is_err(), "should fail with invalid edit");
    }
}
