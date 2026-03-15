use chrono::{DateTime, Utc};
use serde::{Deserialize, Serialize};

/// A single violation record for a file, including the pattern and timestamp.
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq, Eq, Hash)]
pub struct ViolationRecord {
    pub pattern: String,
    pub timestamp: DateTime<Utc>,
}

/// Statistics for a file, tracking its violation history and computed risk score.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct FileStats {
    pub path: String,
    pub violation_history: Vec<ViolationRecord>,
    pub risk_score: f64,
}

impl FileStats {
    /// Create a new FileStats entry for a file.
    pub fn new(path: String, violation_history: Vec<ViolationRecord>) -> Self {
        Self {
            path,
            violation_history,
            risk_score: 0.0,
        }
    }

    /// Update the risk score for this file.
    pub fn set_risk_score(&mut self, score: f64) {
        self.risk_score = score.clamp(0.0, 1.0);
    }

    /// Get the number of distinct violation patterns.
    pub fn pattern_count(&self) -> usize {
        let mut patterns = std::collections::HashSet::new();
        for record in &self.violation_history {
            patterns.insert(&record.pattern);
        }
        patterns.len()
    }

    /// Get violation count for a specific pattern.
    pub fn pattern_violation_count(&self, pattern: &str) -> usize {
        self.violation_history
            .iter()
            .filter(|v| v.pattern == pattern)
            .count()
    }

    /// Get total violation count.
    pub fn total_violations(&self) -> usize {
        self.violation_history.len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_file_stats_creation() {
        let history = vec![ViolationRecord {
            pattern: "test-pattern".to_string(),
            timestamp: Utc::now(),
        }];

        let stats = FileStats::new("test.java".to_string(), history);
        assert_eq!(stats.path, "test.java");
        assert_eq!(stats.total_violations(), 1);
        assert_eq!(stats.risk_score, 0.0);
    }

    #[test]
    fn test_file_stats_set_risk_score() {
        let mut stats = FileStats::new("test.java".to_string(), vec![]);
        stats.set_risk_score(0.75);
        assert_eq!(stats.risk_score, 0.75);

        // Test clamping
        stats.set_risk_score(1.5);
        assert_eq!(stats.risk_score, 1.0);

        stats.set_risk_score(-0.5);
        assert_eq!(stats.risk_score, 0.0);
    }

    #[test]
    fn test_file_stats_pattern_count() {
        let now = Utc::now();
        let history = vec![
            ViolationRecord {
                pattern: "pattern-a".to_string(),
                timestamp: now,
            },
            ViolationRecord {
                pattern: "pattern-a".to_string(),
                timestamp: now,
            },
            ViolationRecord {
                pattern: "pattern-b".to_string(),
                timestamp: now,
            },
        ];

        let stats = FileStats::new("test.java".to_string(), history);
        assert_eq!(stats.pattern_count(), 2);
        assert_eq!(stats.pattern_violation_count("pattern-a"), 2);
        assert_eq!(stats.pattern_violation_count("pattern-b"), 1);
    }
}
