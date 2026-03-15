//! Error types for the oracle module.
//!
//! Provides clear, typed error handling for oracle operations including
//! model training, caching, and risk scoring.

use std::fmt;

/// Result type for oracle operations.
pub type Result<T> = std::result::Result<T, OracleError>;

/// Error types that can occur during oracle operations.
#[derive(Debug, Clone)]
pub enum OracleError {
    /// No training data available for prediction
    NoTrainingData,
    /// Invalid model parameters
    InvalidParameters(String),
    /// Cache operation failed
    CacheError(String),
    /// Scoring operation failed
    ScoringError(String),
}

impl fmt::Display for OracleError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            OracleError::NoTrainingData => {
                write!(f, "oracle error: no training data available")
            },
            OracleError::InvalidParameters(msg) => {
                write!(f, "oracle error: invalid parameters - {}", msg)
            },
            OracleError::CacheError(msg) => {
                write!(f, "oracle cache error: {}", msg)
            },
            OracleError::ScoringError(msg) => {
                write!(f, "oracle scoring error: {}", msg)
            },
        }
    }
}

impl std::error::Error for OracleError {}
