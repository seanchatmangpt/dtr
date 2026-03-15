//! Git state utilities for Claude Code hooks and pipeline Ω phase.
//!
//! Used by Stop hooks to block on dirty state and by dx-pipeline for Phase Ω.

use anyhow::Result;
use std::path::Path;
use std::process::Command;

/// Full git state snapshot.
#[derive(Debug, Default)]
pub struct GitState {
    /// Uncommitted staged or unstaged changes exist
    pub has_uncommitted: bool,
    /// Untracked files (not .gitignored) exist
    pub untracked_files: Vec<String>,
    /// Number of commits not yet pushed to origin
    pub unpushed_count: u32,
    /// Current branch name
    pub branch: String,
}

impl GitState {
    /// Returns true if any condition would block a Stop hook.
    pub fn is_dirty(&self) -> bool {
        self.has_uncommitted || !self.untracked_files.is_empty() || self.unpushed_count > 0
    }

    /// First block reason, if any.
    pub fn block_reason(&self) -> Option<String> {
        if self.has_uncommitted {
            return Some(
                "Uncommitted changes detected. Commit and push before ending the session.".into(),
            );
        }
        if !self.untracked_files.is_empty() {
            let list = self.untracked_files.join(", ");
            return Some(format!(
                "Untracked files detected: {list}. Commit and push before ending."
            ));
        }
        if self.unpushed_count > 0 {
            return Some(format!(
                "{} unpushed commit(s) on '{}'. Push before ending the session.",
                self.unpushed_count, self.branch
            ));
        }
        None
    }
}

/// Collect the current git state from the given project root.
pub fn git_state(root: &Path) -> Result<GitState> {
    if !is_git_repo(root) {
        return Ok(GitState::default());
    }

    let has_uncommitted = has_uncommitted_changes(root);
    let untracked_files = untracked_files(root);
    let branch = current_branch(root).unwrap_or_default();
    let unpushed_count = unpushed_count(root, &branch).unwrap_or(0);

    Ok(GitState {
        has_uncommitted,
        untracked_files,
        unpushed_count,
        branch,
    })
}

fn is_git_repo(root: &Path) -> bool {
    Command::new("git")
        .args(["-C", &root.to_string_lossy(), "rev-parse", "--git-dir"])
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
}

fn has_uncommitted_changes(root: &Path) -> bool {
    let r = root.to_string_lossy();
    let unstaged = Command::new("git")
        .args(["-C", &r, "diff", "--quiet"])
        .status()
        .map(|s| !s.success())
        .unwrap_or(false);
    let staged = Command::new("git")
        .args(["-C", &r, "diff", "--cached", "--quiet"])
        .status()
        .map(|s| !s.success())
        .unwrap_or(false);
    unstaged || staged
}

fn untracked_files(root: &Path) -> Vec<String> {
    Command::new("git")
        .args([
            "-C",
            &root.to_string_lossy(),
            "ls-files",
            "--others",
            "--exclude-standard",
        ])
        .output()
        .map(|o| {
            String::from_utf8_lossy(&o.stdout)
                .lines()
                .take(5)
                .map(str::to_owned)
                .collect()
        })
        .unwrap_or_default()
}

fn current_branch(root: &Path) -> Option<String> {
    Command::new("git")
        .args(["-C", &root.to_string_lossy(), "branch", "--show-current"])
        .output()
        .ok()
        .filter(|o| o.status.success())
        .map(|o| String::from_utf8_lossy(&o.stdout).trim().to_owned())
        .filter(|s| !s.is_empty())
}

fn unpushed_count(root: &Path, branch: &str) -> Option<u32> {
    if branch.is_empty() {
        return None;
    }
    let r = root.to_string_lossy();
    // Try origin/<branch> first, fall back to origin/HEAD
    let range = if Command::new("git")
        .args(["-C", &r, "rev-parse", &format!("origin/{branch}")])
        .output()
        .map(|o| o.status.success())
        .unwrap_or(false)
    {
        format!("origin/{branch}..HEAD")
    } else {
        "origin/HEAD..HEAD".into()
    };

    Command::new("git")
        .args(["-C", &r, "rev-list", &range, "--count"])
        .output()
        .ok()
        .filter(|o| o.status.success())
        .and_then(|o| String::from_utf8_lossy(&o.stdout).trim().parse().ok())
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::PathBuf;

    #[test]
    fn dirty_state_with_uncommitted() {
        let state = GitState {
            has_uncommitted: true,
            ..Default::default()
        };
        assert!(state.is_dirty());
        assert!(state.block_reason().is_some());
    }

    #[test]
    fn dirty_state_with_unpushed() {
        let state = GitState {
            unpushed_count: 2,
            branch: "main".into(),
            ..Default::default()
        };
        assert!(state.is_dirty());
        let reason = state.block_reason().unwrap();
        assert!(reason.contains("2 unpushed"));
    }

    #[test]
    fn clean_state_not_dirty() {
        let state = GitState {
            branch: "main".into(),
            ..Default::default()
        };
        assert!(!state.is_dirty());
        assert!(state.block_reason().is_none());
    }

    #[test]
    fn git_state_on_real_repo() {
        // This test only checks that git_state doesn't panic on the current repo
        let path = PathBuf::from(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .to_path_buf();
        let state = git_state(&path);
        assert!(state.is_ok());
    }
}
