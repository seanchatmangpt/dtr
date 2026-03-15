//! Claude Code hook payload types.
//!
//! Replaces the `python3 -c "import json..."` pattern used in bash hooks with
//! typed Rust structs that can be deserialized from stdin and serialized to stdout.
//!
//! # Hook types
//! - [`PreToolUsePayload`] — received by PreToolUse hooks
//! - [`StopPayload`] — received by Stop hooks
//! - [`SessionStartPayload`] — received by SessionStart hooks
//! - [`BlockDecision`] — returned by hooks to block Claude
//!
//! # Usage in a hook binary
//! ```no_run
//! use cct_hooks::{StopPayload, BlockDecision};
//!
//! fn main() -> anyhow::Result<()> {
//!     let payload = StopPayload::from_stdin()?;
//!     if payload.stop_hook_active {
//!         return Ok(()); // prevent infinite loop
//!     }
//!     // ... validation logic ...
//!     // To block:
//!     BlockDecision::block("Uncommitted changes detected.").emit();
//!     Ok(())
//! }
//! ```

use anyhow::Result;
use serde::{Deserialize, Serialize};
use std::io::{self, Read};

/// Payload delivered to PreToolUse hooks via stdin.
///
/// The hook receives this before every tool invocation. Exit 2 to block.
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct PreToolUsePayload {
    /// Name of the tool being invoked (e.g. "Bash", "Write", "Edit")
    pub tool_name: String,
    /// JSON-encoded tool input (parse with serde_json::from_str)
    pub tool_input: serde_json::Value,
    /// Session identifier
    #[serde(default)]
    pub session_id: String,
    /// Optional hook event metadata
    #[serde(flatten)]
    pub extra: serde_json::Map<String, serde_json::Value>,
}

impl PreToolUsePayload {
    /// Deserialize the hook payload from stdin.
    ///
    /// # Errors
    /// Returns an error if reading from stdin fails (though parsing errors are treated as default payloads).
    pub fn from_stdin() -> Result<Self> {
        let mut buf = String::new();
        io::stdin().read_to_string(&mut buf)?;
        let payload: Self = serde_json::from_str(buf.trim()).unwrap_or_default();
        Ok(payload)
    }

    /// Extract `file_path` from tool_input (for Write/Edit tools).
    pub fn file_path(&self) -> Option<&str> {
        self.tool_input.get("file_path").and_then(|v| v.as_str())
    }

    /// Extract `command` from tool_input (for Bash tool).
    pub fn command(&self) -> Option<&str> {
        self.tool_input.get("command").and_then(|v| v.as_str())
    }

    /// Extract proposed file content (for Write tool: `content`; Edit tool: `new_string`).
    pub fn proposed_content(&self) -> Option<&str> {
        self.tool_input
            .get("content")
            .or_else(|| self.tool_input.get("new_string"))
            .and_then(|v| v.as_str())
    }
}

/// Payload delivered to Stop hooks via stdin.
///
/// The hook receives this when Claude tries to end a session.
/// Output a [`BlockDecision::block`] to prevent session from ending.
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct StopPayload {
    /// Session identifier
    #[serde(default)]
    pub session_id: String,
    /// When true, Claude is already in a forced-continuation state from a prior block.
    /// Exit 0 immediately to prevent an infinite loop.
    #[serde(default)]
    pub stop_hook_active: bool,
    /// Optional timestamp
    #[serde(default)]
    pub timestamp: String,
    #[serde(flatten)]
    pub extra: serde_json::Map<String, serde_json::Value>,
}

impl StopPayload {
    /// Deserialize the hook payload from stdin.
    ///
    /// # Errors
    /// Returns an error if reading from stdin fails (though parsing errors are treated as default payloads).
    pub fn from_stdin() -> Result<Self> {
        let mut buf = String::new();
        io::stdin().read_to_string(&mut buf)?;
        let payload: Self = serde_json::from_str(buf.trim()).unwrap_or_default();
        Ok(payload)
    }
}

/// Payload delivered to SessionStart hooks via stdin.
#[derive(Debug, Deserialize, Serialize, Default)]
pub struct SessionStartPayload {
    #[serde(default)]
    pub session_id: String,
    #[serde(default)]
    pub timestamp: String,
    #[serde(flatten)]
    pub extra: serde_json::Map<String, serde_json::Value>,
}

impl SessionStartPayload {
    /// Deserialize the hook payload from stdin.
    ///
    /// # Errors
    /// Returns an error if reading from stdin fails (though parsing errors are treated as default payloads).
    pub fn from_stdin() -> Result<Self> {
        let mut buf = String::new();
        io::stdin().read_to_string(&mut buf)?;
        let payload: Self = serde_json::from_str(buf.trim()).unwrap_or_default();
        Ok(payload)
    }
}

/// The decision a hook returns to Claude Code.
///
/// Print this to stdout and exit 0 to signal a block.
/// Exit non-zero to allow (or signal a hook error).
#[derive(Debug, Serialize)]
pub struct BlockDecision {
    pub decision: &'static str,
    pub reason: String,
}

impl BlockDecision {
    /// Construct a block decision with the given reason.
    pub fn block(reason: impl Into<String>) -> Self {
        Self {
            decision: "block",
            reason: reason.into(),
        }
    }

    /// Construct an allow decision (rarely needed — just exit 0).
    pub fn allow(reason: impl Into<String>) -> Self {
        Self {
            decision: "allow",
            reason: reason.into(),
        }
    }

    /// Print the decision JSON to stdout. Call before exit 0.
    ///
    /// # Panics
    /// Panics if JSON serialization of the decision fails.
    pub fn emit(&self) {
        println!("{}", serde_json::to_string(self).unwrap());
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn deserialize_stop_payload_with_active_flag() {
        let json = r#"{"session_id":"abc","stop_hook_active":true}"#;
        let p: StopPayload = serde_json::from_str(json).unwrap();
        assert!(p.stop_hook_active);
        assert_eq!(p.session_id, "abc");
    }

    #[test]
    fn deserialize_stop_payload_defaults() {
        let p: StopPayload = serde_json::from_str("{}").unwrap();
        assert!(!p.stop_hook_active);
    }

    #[test]
    fn deserialize_pre_tool_use_bash() {
        let json = r#"{"tool_name":"Bash","tool_input":{"command":"git push --force"}}"#;
        let p: PreToolUsePayload = serde_json::from_str(json).unwrap();
        assert_eq!(p.tool_name, "Bash");
        assert_eq!(p.command(), Some("git push --force"));
    }

    #[test]
    fn deserialize_pre_tool_use_write() {
        let json =
            r#"{"tool_name":"Write","tool_input":{"file_path":"Foo.java","content":"// TODO"}}"#;
        let p: PreToolUsePayload = serde_json::from_str(json).unwrap();
        assert_eq!(p.file_path(), Some("Foo.java"));
        assert_eq!(p.proposed_content(), Some("// TODO"));
    }

    #[test]
    fn block_decision_serializes_correctly() {
        let d = BlockDecision::block("Uncommitted changes.");
        let s = serde_json::to_string(&d).unwrap();
        assert!(s.contains(r#""decision":"block""#));
        assert!(s.contains("Uncommitted changes."));
    }
}
