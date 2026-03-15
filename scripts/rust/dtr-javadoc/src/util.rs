/// Utility functions for Java source processing.
///
/// This module provides helpers for extracting package names, class names,
/// cleaning comment text, and navigating tree-sitter ASTs.

use std::path::Path;

/// Extract the `package foo.bar;` declaration from Java source bytes.
#[must_use]
pub fn derive_package(source: &[u8]) -> String {
    let text = std::str::from_utf8(source).unwrap_or("");
    for line in text.lines() {
        let trimmed = line.trim();
        if trimmed.starts_with("package ") && trimmed.ends_with(';') {
            return trimmed["package ".len()..trimmed.len() - 1]
                .trim()
                .to_string();
        }
    }
    String::new()
}

/// Return the file stem (expected class name) from a path.
#[must_use]
pub fn class_name_from_path(path: &Path) -> &str {
    path.file_stem()
        .and_then(|s| s.to_str())
        .unwrap_or("Unknown")
}

/// Strip Javadoc delimiters and leading ` * ` markers. Joins lines into one string.
///
/// License header blocks (Apache License, Copyright notices) are stripped
/// because the Javadoc grammar places them in the description when they appear
/// at the top of a file-level comment. We detect them by presence of
/// "Licensed under the Apache License" or "Copyright" and return empty.
#[must_use]
pub fn clean_comment_text(text: &str) -> String {
    // Strip license headers
    if text.contains("Licensed under the Apache License")
        || text.contains("Copyright (C)")
        || text.contains("Copyright 2")
    {
        return String::new();
    }

    text.lines()
        .map(|line| {
            let t = line.trim();
            if let Some(stripped) = t.strip_prefix("* ") {
                stripped
            } else if t == "*" {
                ""
            } else if t.starts_with("/**") {
                t.trim_start_matches("/**").trim()
            } else if t.starts_with("*/") {
                ""
            } else {
                t
            }
        })
        .filter(|s| !s.is_empty())
        .collect::<Vec<_>>()
        .join(" ")
        .trim()
        .to_string()
}
