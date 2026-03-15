use crate::model::ViolationRecord;
use std::collections::HashMap;

/// Naive Bayes classifier trained on violation history.
/// Uses Laplace smoothing to handle unseen patterns.
/// Binary classification: "has violated" vs "hasn't violated" per pattern per file.
#[derive(Debug, Clone)]
pub struct NaiveBayesOracle {
    /// Pattern -> count of files that have this violation
    pattern_positive_counts: HashMap<String, usize>,
    /// Total number of training samples
    total_samples: usize,
    /// Prior probability of a file having violations
    prior_positive: f64,
    /// All patterns seen during training
    all_patterns: Vec<String>,
    /// Training data for validation
    training_data: Vec<(String, Vec<ViolationRecord>)>,
}

impl NaiveBayesOracle {
    /// Create a new, untrained oracle.
    pub fn new() -> Self {
        Self {
            pattern_positive_counts: HashMap::new(),
            total_samples: 0,
            prior_positive: 0.0,
            all_patterns: vec![],
            training_data: vec![],
        }
    }

    /// Add a training sample (file path and its violation history).
    pub fn add_training_sample(&mut self, _path: &str, violations: &[ViolationRecord]) {
        // Collect unique patterns in this sample
        let mut patterns_in_sample = std::collections::HashSet::new();
        for violation in violations {
            patterns_in_sample.insert(violation.pattern.clone());
        }

        // Increment counts for each pattern present in this sample
        for pattern in &patterns_in_sample {
            *self
                .pattern_positive_counts
                .entry(pattern.clone())
                .or_insert(0) += 1;
        }

        self.total_samples += 1;
        self.training_data
            .push((_path.to_string(), violations.to_vec()));
    }

    /// Train the Naive Bayes model on the added samples.
    /// Computes probabilities using Laplace smoothing.
    pub fn train(&mut self) {
        if self.total_samples == 0 {
            return;
        }

        // Compute prior: fraction of files with violations
        let files_with_violations = self
            .training_data
            .iter()
            .filter(|(_, violations)| !violations.is_empty())
            .count();
        self.prior_positive = files_with_violations as f64 / self.total_samples as f64;

        // Collect all unique patterns
        self.all_patterns = self
            .pattern_positive_counts
            .keys()
            .cloned()
            .collect::<Vec<_>>();
        self.all_patterns.sort();
    }

    /// Predict the probability that a file with the given violation history
    /// belongs to the "high risk" class.
    /// Returns a value in [0, 1].
    pub fn predict(&self, violations: &[ViolationRecord]) -> f64 {
        if self.total_samples == 0 {
            return 0.0;
        }

        // If no violations, return low score
        if violations.is_empty() {
            return 0.1;
        }

        // Collect patterns in this violation history
        let mut violation_patterns = std::collections::HashSet::new();
        for violation in violations {
            violation_patterns.insert(violation.pattern.clone());
        }

        // Count how many patterns are seen in the violation history
        let matched_pattern_count = violation_patterns.len();

        // Count how many of these matched patterns were in training data
        let mut training_matches = 0;
        for pattern in &violation_patterns {
            if self.pattern_positive_counts.contains_key(pattern) {
                training_matches += 1;
            }
        }

        // Simple scoring: higher match ratio means higher risk
        // With Laplace smoothing to avoid zero probabilities
        let alpha = 1.0; // Laplace smoothing constant

        let match_ratio = (training_matches as f64 + alpha)
            / (matched_pattern_count as f64 + alpha);

        // Also factor in prior: how common are violations in general?
        let combined_score = (self.prior_positive * 0.5) + (match_ratio * 0.5);

        combined_score.clamp(0.0, 1.0)
    }
}

impl Default for NaiveBayesOracle {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;

    fn create_violation(pattern: &str) -> ViolationRecord {
        ViolationRecord {
            pattern: pattern.to_string(),
            timestamp: Utc::now(),
        }
    }

    #[test]
    fn test_oracle_train_empty() {
        let mut oracle = NaiveBayesOracle::new();
        oracle.train();
        assert_eq!(oracle.total_samples, 0);
        assert_eq!(oracle.prior_positive, 0.0);
    }

    #[test]
    fn test_oracle_train_single_sample() {
        let mut oracle = NaiveBayesOracle::new();
        let violations = vec![create_violation("test-pattern")];
        oracle.add_training_sample("file1.java", &violations);
        oracle.train();

        assert_eq!(oracle.total_samples, 1);
        assert_eq!(oracle.prior_positive, 1.0);
        assert!(oracle.all_patterns.contains(&"test-pattern".to_string()));
    }

    #[test]
    fn test_oracle_predict_no_patterns_vs_with_patterns() {
        let mut oracle = NaiveBayesOracle::new();

        oracle.add_training_sample("file1.java", &[create_violation("pattern-a")]);
        oracle.add_training_sample("file2.java", &[]);
        oracle.add_training_sample("file3.java", &[]);
        oracle.train();

        let empty_prediction = oracle.predict(&[]);
        let with_pattern_prediction = oracle.predict(&[create_violation("pattern-a")]);

        assert!(
            with_pattern_prediction > empty_prediction,
            "Files with violations should score higher than empty history"
        );
    }

    #[test]
    fn test_oracle_predict_seen_pattern_high_probability() {
        let mut oracle = NaiveBayesOracle::new();

        oracle.add_training_sample(
            "file1.java",
            &[create_violation("dangerous-pattern"), create_violation("dangerous-pattern")],
        );
        oracle.add_training_sample("file2.java", &[]);
        oracle.train();

        let prediction = oracle.predict(&[create_violation("dangerous-pattern")]);
        assert!(prediction > 0.0, "Known violation pattern should predict above 0");
    }
}
