/// Oracle manager for coordinating risk assessment operations.
///
/// Provides a unified interface to coordinate model training, caching,
/// and batch risk scoring across the oracle subsystem.
use crate::cache::ModelCache;
use crate::model::ViolationRecord;
use crate::naive_bayes::NaiveBayesOracle;
use crate::scorer::RiskScorer;

/// High-level orchestrator for oracle operations.
///
/// Manages the lifecycle of models, caches, and scoring operations.
/// Provides convenient APIs for common workflows like training and batch scoring.
#[derive(Clone, Debug)]
pub struct OracleManager {
    /// Cached model instances
    cache: ModelCache,
    /// Trained classifier for pattern recognition
    model: Option<NaiveBayesOracle>,
}

impl OracleManager {
    /// Create a new oracle manager with default configuration.
    pub fn new() -> Self {
        Self {
            cache: ModelCache::new(),
            model: None,
        }
    }

    /// Create a new oracle manager with a pre-trained model.
    pub fn with_model(model: NaiveBayesOracle) -> Self {
        Self {
            cache: ModelCache::new(),
            model: Some(model),
        }
    }

    /// Get or create a risk scorer with default parameters.
    pub fn get_default_scorer(&self) -> RiskScorer {
        self.cache.get_default_scorer()
    }

    /// Get or create a risk scorer with custom decay parameters.
    pub fn get_scorer(&self, decay_factor: f64, decay_window_days: i64) -> RiskScorer {
        self.cache.get_scorer(decay_factor, decay_window_days)
    }

    /// Compute risk scores for multiple files' violation histories.
    ///
    /// Uses the default scorer and parallelizes computation across histories.
    /// Histories are scored independently; returned scores maintain input order.
    pub fn score_files(&self, histories: &[Vec<ViolationRecord>]) -> Vec<f64> {
        let scorer = self.get_default_scorer();
        scorer.score_risks_parallel(histories)
    }

    /// Compute risk scores with custom scorer parameters.
    pub fn score_files_with_params(
        &self,
        histories: &[Vec<ViolationRecord>],
        decay_factor: f64,
        decay_window_days: i64,
    ) -> Vec<f64> {
        let scorer = self.get_scorer(decay_factor, decay_window_days);
        scorer.score_risks_parallel(histories)
    }

    /// Compute risk score for a single file.
    pub fn score_file(&self, violations: &[ViolationRecord]) -> f64 {
        let scorer = self.get_default_scorer();
        scorer.score_risk(violations)
    }

    /// Update the internal model.
    pub fn set_model(&mut self, model: NaiveBayesOracle) {
        self.model = Some(model);
    }

    /// Get reference to the current model (if any).
    pub fn model(&self) -> Option<&NaiveBayesOracle> {
        self.model.as_ref()
    }

    /// Get mutable reference to the current model (if any).
    pub fn model_mut(&mut self) -> Option<&mut NaiveBayesOracle> {
        self.model.as_mut()
    }

    /// Clear the scorer cache.
    pub fn clear_cache(&self) {
        self.cache.clear();
    }

    /// Get cache statistics.
    pub fn cache_size(&self) -> usize {
        self.cache.size()
    }
}

impl Default for OracleManager {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use chrono::Utc;

    fn create_violation(pattern: &str, days_ago: i64) -> ViolationRecord {
        let timestamp = Utc::now() - chrono::Duration::days(days_ago);
        ViolationRecord {
            pattern: pattern.to_string(),
            timestamp,
        }
    }

    #[test]
    fn test_manager_default_scorer() {
        let manager = OracleManager::new();
        let scorer = manager.get_default_scorer();
        assert_eq!(scorer.decay_factor, 0.5);
        assert_eq!(scorer.decay_window_days, 90);
    }

    #[test]
    fn test_manager_custom_scorer() {
        let manager = OracleManager::new();
        let scorer = manager.get_scorer(0.3, 60);
        assert_eq!(scorer.decay_factor, 0.3);
        assert_eq!(scorer.decay_window_days, 60);
    }

    #[test]
    fn test_manager_score_file() {
        let manager = OracleManager::new();
        let violations = vec![
            create_violation("pattern-a", 5),
            create_violation("pattern-b", 10),
        ];
        let score = manager.score_file(&violations);
        assert!((0.0..=1.0).contains(&score));
    }

    #[test]
    fn test_manager_score_files_parallel() {
        let manager = OracleManager::new();
        let histories = vec![
            vec![create_violation("pattern", 5)],
            vec![create_violation("pattern", 10)],
            vec![],
        ];
        let scores = manager.score_files(&histories);
        assert_eq!(scores.len(), 3);
        assert!(scores[0] > scores[1]);
        assert_eq!(scores[2], 0.0);
    }

    #[test]
    fn test_manager_with_model() {
        let model = NaiveBayesOracle::new();
        let manager = OracleManager::with_model(model);
        assert!(manager.model().is_some());
    }

    #[test]
    fn test_manager_cache_operations() {
        let manager = OracleManager::new();
        assert_eq!(manager.cache_size(), 0);

        let _scorer1 = manager.get_scorer(0.5, 90);
        assert_eq!(manager.cache_size(), 1);

        let _scorer2 = manager.get_scorer(0.3, 60);
        assert_eq!(manager.cache_size(), 2);

        manager.clear_cache();
        assert_eq!(manager.cache_size(), 0);
    }
}
