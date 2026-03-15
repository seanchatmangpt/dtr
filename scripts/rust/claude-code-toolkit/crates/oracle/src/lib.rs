pub mod model;
pub mod naive_bayes;
pub mod scorer;

pub use model::{FileStats, ViolationRecord};
pub use naive_bayes::NaiveBayesOracle;
pub use scorer::RiskScorer;

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;

    fn create_test_violation(pattern: &str, days_ago: i64) -> ViolationRecord {
        let timestamp = Utc::now() - chrono::Duration::days(days_ago);
        ViolationRecord {
            pattern: pattern.to_string(),
            timestamp,
        }
    }

    #[test]
    fn test_naive_bayes_train_and_predict() {
        let mut oracle = NaiveBayesOracle::new();

        // Training data: files with violation history
        let file1_history = vec![
            create_test_violation("unsafe-cast", 30),
            create_test_violation("unsafe-cast", 15),
            create_test_violation("hardcoded-secret", 10),
        ];

        let file2_history = vec![create_test_violation("hardcoded-secret", 20)];

        let file3_history = vec![];

        oracle.add_training_sample("src/Main.java", &file1_history);
        oracle.add_training_sample("src/Utils.java", &file2_history);
        oracle.add_training_sample("src/Clean.java", &file3_history);

        oracle.train();

        // Predict on new file with history similar to file1
        let new_history = vec![
            create_test_violation("unsafe-cast", 5),
            create_test_violation("hardcoded-secret", 2),
        ];

        let score = oracle.predict(&new_history);
        assert!(
            score > 0.5,
            "File with multiple violations should have high risk"
        );
    }

    #[test]
    fn test_temporal_decay_recent_violations() {
        let scorer = RiskScorer::new();

        let recent_violation = create_test_violation("unsafe-cast", 1);
        let old_violation = create_test_violation("unsafe-cast", 90);

        let recent_history = vec![recent_violation];
        let old_history = vec![old_violation];

        let recent_score = scorer.score_risk(&recent_history);
        let old_score = scorer.score_risk(&old_history);

        assert!(
            recent_score > old_score,
            "Recent violations should weigh more than old ones"
        );
    }

    #[test]
    fn test_risk_scorer_normalizes_to_01() {
        let scorer = RiskScorer::new();

        let minimal_history = vec![create_test_violation("minor-issue", 365)];
        let heavy_history = vec![
            create_test_violation("critical-bug", 1),
            create_test_violation("critical-bug", 2),
            create_test_violation("critical-bug", 3),
            create_test_violation("security-flaw", 1),
            create_test_violation("security-flaw", 2),
        ];

        let minimal_score = scorer.score_risk(&minimal_history);
        let heavy_score = scorer.score_risk(&heavy_history);

        assert!((0.0..=1.0).contains(&minimal_score));
        assert!((0.0..=1.0).contains(&heavy_score));
        assert!(
            heavy_score > minimal_score,
            "More violations should increase risk score"
        );
    }

    #[test]
    fn test_empty_history_zero_risk() {
        let scorer = RiskScorer::new();
        let empty_history: Vec<ViolationRecord> = vec![];

        let score = scorer.score_risk(&empty_history);
        assert_eq!(score, 0.0, "Empty history should have zero risk");
    }

    #[test]
    fn test_multiple_patterns_increase_risk() {
        let scorer = RiskScorer::new();

        let single_pattern = vec![
            create_test_violation("pattern1", 10),
            create_test_violation("pattern1", 5),
        ];

        let multi_pattern = vec![
            create_test_violation("pattern1", 10),
            create_test_violation("pattern2", 8),
            create_test_violation("pattern3", 6),
        ];

        let single_score = scorer.score_risk(&single_pattern);
        let multi_score = scorer.score_risk(&multi_pattern);

        assert!(
            multi_score > single_score,
            "Multiple patterns should increase risk"
        );
    }

    #[test]
    fn test_naive_bayes_laplace_smoothing() {
        let mut oracle = NaiveBayesOracle::new();

        // Small training set to test smoothing
        let history1 = vec![create_test_violation("rare-pattern", 10)];
        let history2 = vec![];

        oracle.add_training_sample("file1.java", &history1);
        oracle.add_training_sample("file2.java", &history2);
        oracle.train();

        // Predict on a file with the rare pattern
        let new_history = vec![create_test_violation("rare-pattern", 5)];
        let score = oracle.predict(&new_history);

        // Should not be NaN or infinity due to Laplace smoothing
        assert!(
            score.is_finite(),
            "Score should be finite with Laplace smoothing"
        );
        assert!((0.0..=1.0).contains(&score), "Score should be normalized");
    }
}
