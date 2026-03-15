use crate::model::ViolationRecord;
use chrono::Utc;
use std::collections::HashMap;

/// Risk scorer that computes a normalized [0, 1] risk score.
/// Uses temporal decay: recent violations weight more than old ones.
pub struct RiskScorer {
    /// Decay factor (0-1): how much weight to give to violations older than the decay window
    decay_factor: f64,
    /// Window in days: violations older than this decay more aggressively
    decay_window_days: i64,
}

impl RiskScorer {
    /// Create a new risk scorer with default parameters.
    /// Decay window: 90 days. Decay factor: 0.5.
    pub fn new() -> Self {
        Self {
            decay_factor: 0.5,
            decay_window_days: 90,
        }
    }

    /// Create a new risk scorer with custom decay parameters.
    pub fn with_decay(decay_factor: f64, decay_window_days: i64) -> Self {
        Self {
            decay_factor: decay_factor.clamp(0.0, 1.0),
            decay_window_days: decay_window_days.max(1),
        }
    }

    /// Compute the risk score for a file with the given violation history.
    /// Score combines:
    /// - Total violation count
    /// - Distinct pattern count
    /// - Temporal decay (recent violations weight more)
    /// Returns a normalized [0, 1] score.
    pub fn score_risk(&self, violations: &[ViolationRecord]) -> f64 {
        if violations.is_empty() {
            return 0.0;
        }

        let now = Utc::now();

        // Compute weighted violation count with temporal decay
        let mut weighted_count = 0.0;
        let mut pattern_weights: HashMap<String, f64> = HashMap::new();

        for violation in violations {
            let age_days = (now - violation.timestamp).num_days();
            let decay_weight = self.compute_decay_weight(age_days);

            weighted_count += decay_weight;
            *pattern_weights
                .entry(violation.pattern.clone())
                .or_insert(0.0) += decay_weight;
        }

        // Distinct pattern contribution
        let pattern_count = pattern_weights.len() as f64;

        // Temporal diversity: how spread out are the violations?
        let temporal_spread = self.compute_temporal_spread(violations);

        // Combine factors: total weight + pattern variety + temporal concentration
        let base_score = weighted_count + (pattern_count * 0.5) + (temporal_spread * 0.3);

        // Normalize to [0, 1] with sigmoid-like curve
        // Use log scale for better distribution
        let normalized = 1.0 - (-base_score / 10.0).exp();

        normalized.clamp(0.0, 1.0)
    }

    /// Compute temporal decay weight for a violation of a given age (in days).
    /// Recent violations (age < decay_window) get weight 1.0.
    /// Older violations decay exponentially towards decay_factor.
    fn compute_decay_weight(&self, age_days: i64) -> f64 {
        if age_days <= 0 {
            // Recent violations (within the last day)
            1.0
        } else if age_days < self.decay_window_days {
            // Linear decay from 1.0 to decay_factor
            let t = age_days as f64 / self.decay_window_days as f64;
            1.0 - (t * (1.0 - self.decay_factor))
        } else {
            // Beyond decay window, use floor decay_factor
            self.decay_factor
        }
    }

    /// Compute temporal spread: measure how concentrated violations are in time.
    /// Returns 0 if all violations are at the same time, 1 if spread over a long period.
    fn compute_temporal_spread(&self, violations: &[ViolationRecord]) -> f64 {
        if violations.len() <= 1 {
            return 0.0;
        }

        let mut timestamps: Vec<_> = violations.iter().map(|v| v.timestamp).collect();
        timestamps.sort();

        let earliest = timestamps.first().unwrap();
        let latest = timestamps.last().unwrap();

        let span_days = (*latest - *earliest).num_days() as f64;
        let max_span = 365.0; // 1 year

        // Recent, concentrated violations are riskier
        // So we return low spread for concentrated violations
        if span_days < 1.0 {
            0.0
        } else {
            (span_days / max_span).min(1.0)
        }
    }
}

impl Default for RiskScorer {
    fn default() -> Self {
        Self::new()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    fn create_violation_days_ago(pattern: &str, days: i64) -> ViolationRecord {
        let timestamp = Utc::now() - chrono::Duration::days(days);
        ViolationRecord {
            pattern: pattern.to_string(),
            timestamp,
        }
    }

    #[test]
    fn test_scorer_empty_returns_zero() {
        let scorer = RiskScorer::new();
        let score = scorer.score_risk(&[]);
        assert_eq!(score, 0.0);
    }

    #[test]
    fn test_scorer_recent_higher_than_old() {
        let scorer = RiskScorer::new();

        let recent = vec![create_violation_days_ago("pattern", 5)];
        let old = vec![create_violation_days_ago("pattern", 180)];

        let recent_score = scorer.score_risk(&recent);
        let old_score = scorer.score_risk(&old);

        assert!(recent_score > old_score, "Recent violations should score higher");
    }

    #[test]
    fn test_scorer_more_violations_higher_score() {
        let scorer = RiskScorer::new();

        let few = vec![create_violation_days_ago("pattern", 10)];
        let many = vec![
            create_violation_days_ago("pattern", 10),
            create_violation_days_ago("pattern", 8),
            create_violation_days_ago("pattern", 6),
        ];

        let few_score = scorer.score_risk(&few);
        let many_score = scorer.score_risk(&many);

        assert!(many_score > few_score, "More violations should increase score");
    }

    #[test]
    fn test_scorer_multiple_patterns_higher_score() {
        let scorer = RiskScorer::new();

        let single = vec![
            create_violation_days_ago("pattern-a", 10),
            create_violation_days_ago("pattern-a", 5),
        ];

        let multiple = vec![
            create_violation_days_ago("pattern-a", 10),
            create_violation_days_ago("pattern-b", 8),
            create_violation_days_ago("pattern-c", 6),
        ];

        let single_score = scorer.score_risk(&single);
        let multiple_score = scorer.score_risk(&multiple);

        assert!(
            multiple_score > single_score,
            "Multiple patterns should increase score"
        );
    }

    #[test]
    fn test_scorer_normalized_to_01() {
        let scorer = RiskScorer::new();

        let minimal = vec![create_violation_days_ago("pattern", 365)];
        let heavy = vec![
            create_violation_days_ago("p1", 1),
            create_violation_days_ago("p2", 1),
            create_violation_days_ago("p3", 1),
            create_violation_days_ago("p4", 1),
            create_violation_days_ago("p5", 1),
            create_violation_days_ago("p6", 1),
        ];

        let minimal_score = scorer.score_risk(&minimal);
        let heavy_score = scorer.score_risk(&heavy);

        assert!(minimal_score >= 0.0 && minimal_score <= 1.0);
        assert!(heavy_score >= 0.0 && heavy_score <= 1.0);
    }

    #[test]
    fn test_scorer_decay_window() {
        let scorer_90 = RiskScorer::new();
        let scorer_30 = RiskScorer::with_decay(0.5, 30);

        let violation_at_60_days = vec![create_violation_days_ago("pattern", 60)];

        let score_90 = scorer_90.score_risk(&violation_at_60_days);
        let score_30 = scorer_30.score_risk(&violation_at_60_days);

        assert!(
            score_90 > score_30,
            "Longer decay window should weight older violations higher"
        );
    }
}
