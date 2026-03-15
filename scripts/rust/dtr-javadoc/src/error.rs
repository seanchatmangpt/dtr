//! Documentation violation types and TPS Jidoka enforcement.
//!
//! This module defines violation detection types used to enforce the TPS rule:
//! "stop the line on every missing doc". A violation indicates a public type or
//! method that lacks an immediately preceding Javadoc comment.

use std::path::PathBuf;

/// A documentation violation: a public type or method missing a Javadoc comment.
/// The build fails if any violations are present.
#[derive(Debug, Clone)]
pub struct DocViolation {
    pub file: PathBuf,
    pub fqcn: String,
    pub kind: ViolationKind,
}

#[derive(Debug, Clone)]
pub enum ViolationKind {
    MissingClassDoc,
    MissingMethodDoc { method: String },
}

impl std::fmt::Display for DocViolation {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        let file = self.file.to_string_lossy();
        match &self.kind {
            ViolationKind::MissingClassDoc => {
                write!(f, "  MISSING CLASS DOC   {}  ({})", self.fqcn, file)
            }
            ViolationKind::MissingMethodDoc { method } => {
                write!(
                    f,
                    "  MISSING METHOD DOC  {}#{}  ({})",
                    self.fqcn, method, file
                )
            }
        }
    }
}
