//! Property-Based Tests using proptest
//!
//! Validates invariants across a wide range of inputs:
//! - Hash stability: same input → same blake3 output
//! - Diff generation: verify diffs are valid hunks
//! - Pattern matching: fuzzy/exact matching edge cases
//! - Scanner robustness: extract methods from varied Java code

use proptest::prelude::*;

/// Property: Hash stability — same input always produces same hash
#[test]
fn prop_hash_stability() {
    proptest!(|(body in ".*")| {
        let hash1 = cct_cache::hasher::hash_method_body(body.as_bytes());
        let hash2 = cct_cache::hasher::hash_method_body(body.as_bytes());

        prop_assert_eq!(hash1, hash2, "Same input must produce identical hash");
    });
}

/// Property: Hash collision-resistance — different inputs rarely produce same hash
#[test]
fn prop_hash_collision_resistance() {
    proptest!(|(input1 in ".*", input2 in ".*")| {
        if input1 != input2 {
            let hash1 = cct_cache::hasher::hash_method_body(input1.as_bytes());
            let hash2 = cct_cache::hasher::hash_method_body(input2.as_bytes());

            // blake3 has 256-bit output; collision is extremely unlikely
            prop_assert_ne!(
                hash1, hash2,
                "Different inputs should produce different hashes (for all practical purposes)"
            );
        }
    });
}

/// Property: Hash output always 32 bytes
#[test]
fn prop_hash_always_32_bytes() {
    proptest!(|(data in ".*")| {
        let hash = cct_cache::hasher::hash_method_body(data.as_bytes());
        prop_assert_eq!(hash.len(), 32, "blake3 hash must always be 32 bytes");
    });
}

/// Property: Pattern matching completeness — all matches are found
#[test]
fn prop_pattern_matching_finds_todos() {
    proptest!(|(prefix in "[ \t]*", rest in "[^\n]*")| {
        let body = format!("{}// TODO{}", prefix, rest);

        let pattern_set = cct_scanner::PatternSet::new();
        let hits = pattern_set.match_body(&body);

        // Should find at least one H_TODO match
        prop_assert!(
            hits.iter().any(|h| h.pattern == "H_TODO"),
            "Should detect H_TODO in: {}",
            body
        );
    });
}

/// Property: Pattern matching for return null
#[test]
fn prop_pattern_matching_return_null() {
    proptest!(|(prefix in "[ \t]*", suffix in "[^\n]*")| {
        let body = format!("{}return null;{}", prefix, suffix);

        let pattern_set = cct_scanner::PatternSet::new();
        let hits = pattern_set.match_body(&body);

        // Should find H_STUB_NULL pattern
        prop_assert!(
            hits.iter().any(|h| h.pattern == "H_STUB_NULL"),
            "Should detect H_STUB_NULL in: {}",
            body
        );
    });
}

/// Property: Pattern matching for empty collections
#[test]
fn prop_pattern_matching_empty_collections() {
    proptest!(|(prefix in "[ \t]*", suffix in "[^\n]*")| {
        let body = format!("{}return Collections.emptyList();{}", prefix, suffix);

        let pattern_set = cct_scanner::PatternSet::new();
        let hits = pattern_set.match_body(&body);

        // Should find H_STUB_EMPTY_COLLECTION
        prop_assert!(
            hits.iter().any(|h| h.pattern == "H_STUB_EMPTY_COLLECTION"),
            "Should detect H_STUB_EMPTY_COLLECTION"
        );
    });
}

/// Property: Method extraction always yields consistent structure
#[test]
fn prop_method_extraction_structure() {
    proptest!(|(class_name in "[A-Za-z][A-Za-z0-9]*", method_name in "[a-z][a-zA-Z0-9]*")| {
        let java_code = format!(
            r#"public class {} {{
    public void {}() {{
        System.out.println("test");
    }}
}}"#,
            class_name, method_name
        );

        let methods = cct_scanner::extract_methods(java_code.as_bytes());

        // Should extract at least one method
        prop_assert!(!methods.is_empty(), "Should extract methods from valid Java");

        for method in &methods {
            // Each method should have basic structure
            prop_assert!(!method.name.is_empty(), "Method name must not be empty");
            prop_assert!(!method.body.is_empty(), "Method body must not be empty");
            prop_assert!(method.start_line > 0, "Start line must be positive");
            prop_assert!(method.end_line >= method.start_line, "End >= start");
        }
    });
}

/// Property: Scanner handles malformed input gracefully
#[test]
fn prop_scanner_robustness() {
    proptest!(|(input in ".*")| {
        // Scanner should never panic, even on invalid input
        let methods = cct_scanner::extract_methods(input.as_bytes());

        // Just verify it completed (doesn't crash)
        // May return empty vec for non-Java or fragments, which is fine
        prop_assert!(methods.len() >= 0);
    });
}

/// Property: Pattern set matching never crashes
#[test]
fn prop_pattern_matching_robustness() {
    proptest!(|(input in ".*")| {
        let pattern_set = cct_scanner::PatternSet::new();

        // Should never panic, even on arbitrary strings
        let _hits = pattern_set.match_body(&input);

        // Just verify completion
        prop_assert!(true);
    });
}

/// Property: Risk scoring returns normalized values
#[test]
fn prop_risk_scoring_normalized() {
    proptest!(|(num_violations in 0usize..100)| {
        let scorer = cct_oracle::scorer::RiskScorer::new();

        // Create variable number of violations
        let violations: Vec<_> = (0..num_violations)
            .map(|i| {
                let days_ago = (i % 365) as i64;
                cct_oracle::model::ViolationRecord {
                    pattern: format!("pattern-{}", i % 5),
                    timestamp: chrono::Utc::now() - chrono::Duration::days(days_ago),
                }
            })
            .collect();

        let score = scorer.score_risk(&violations);

        // Risk score must always be in [0, 1]
        prop_assert!(
            (0.0..=1.0).contains(&score),
            "Risk score {} must be in [0, 1]",
            score
        );
    });
}

/// Property: Oracle temporal decay — more recent violations score higher
#[test]
fn prop_temporal_decay_monotonic() {
    proptest!(|(days_ago_1 in 1i64..365, days_ago_2 in 1i64..365)| {
        let scorer = cct_oracle::scorer::RiskScorer::new();

        let violation1 = cct_oracle::model::ViolationRecord {
            pattern: "test-pattern".to_string(),
            timestamp: chrono::Utc::now() - chrono::Duration::days(days_ago_1),
        };

        let violation2 = cct_oracle::model::ViolationRecord {
            pattern: "test-pattern".to_string(),
            timestamp: chrono::Utc::now() - chrono::Duration::days(days_ago_2),
        };

        let score1 = scorer.score_risk(&vec![violation1]);
        let score2 = scorer.score_risk(&vec![violation2]);

        // More recent (lower days_ago) should have higher score
        if days_ago_1 < days_ago_2 {
            prop_assert!(
                score1 > score2,
                "More recent violation ({} days) should score higher than older ({} days): {} > {}",
                days_ago_1, days_ago_2, score1, score2
            );
        } else if days_ago_1 > days_ago_2 {
            prop_assert!(
                score1 < score2,
                "Older violation ({} days) should score lower than newer ({} days): {} < {}",
                days_ago_1, days_ago_2, score1, score2
            );
        }
    });
}

/// Property: Multiple patterns increase risk
#[test]
fn prop_multiple_patterns_increase_risk() {
    proptest!(|(num_patterns in 1usize..10)| {
        let scorer = cct_oracle::scorer::RiskScorer::new();

        let single_pattern = vec![cct_oracle::model::ViolationRecord {
            pattern: "pattern-0".to_string(),
            timestamp: chrono::Utc::now() - chrono::Duration::days(5),
        }];

        let multi_pattern: Vec<_> = (0..num_patterns)
            .map(|i| cct_oracle::model::ViolationRecord {
                pattern: format!("pattern-{}", i),
                timestamp: chrono::Utc::now() - chrono::Duration::days(5),
            })
            .collect();

        let single_score = scorer.score_risk(&single_pattern);
        let multi_score = scorer.score_risk(&multi_pattern);

        // More patterns should increase risk (with same recency)
        prop_assert!(
            multi_score >= single_score,
            "Multiple patterns ({}) should have >= risk than single ({}): {} >= {}",
            num_patterns, 1, multi_score, single_score
        );
    });
}

/// Property: Empty violations always score 0
#[test]
fn prop_empty_violations_zero_risk() {
    let scorer = cct_oracle::scorer::RiskScorer::new();
    let empty_violations: Vec<cct_oracle::model::ViolationRecord> = vec![];

    let score = scorer.score_risk(&empty_violations);
    assert_eq!(score, 0.0, "Empty violations should always score 0.0");
}

/// Property: Hash determinism with streaming input
#[test]
fn prop_hash_streaming_equivalence() {
    proptest!(|(chunk1 in ".*", chunk2 in ".*", chunk3 in ".*")| {
        // Direct hash of concatenated input
        let mut full_input = chunk1.clone();
        full_input.push_str(&chunk2);
        full_input.push_str(&chunk3);

        let hash_direct = cct_cache::hasher::hash_method_body(full_input.as_bytes());

        // Streaming hash (simulated)
        let mut hasher = blake3::Hasher::new();
        hasher.update(chunk1.as_bytes());
        hasher.update(chunk2.as_bytes());
        hasher.update(chunk3.as_bytes());
        let digest = hasher.finalize();

        let mut hash_streamed = [0u8; 32];
        hash_streamed.copy_from_slice(digest.as_bytes());

        prop_assert_eq!(
            hash_direct, hash_streamed,
            "Streaming and direct hashing must be equivalent"
        );
    });
}

/// Property: Method extraction idempotent across identical re-parses
#[test]
fn prop_extraction_idempotent() {
    proptest!(|(method_name in "[a-z][a-zA-Z0-9]{0,20}")| {
        let java_code = format!(
            r#"public class Test {{
    public void {}() {{
        int x = 42;
        String s = "test";
        System.out.println(x);
    }}
}}"#,
            method_name
        );

        let java_bytes = java_code.as_bytes();

        // Extract methods twice from identical input
        let methods1 = cct_scanner::extract_methods(java_bytes);
        let methods2 = cct_scanner::extract_methods(java_bytes);

        // Results must be identical
        prop_assert_eq!(
            methods1.len(), methods2.len(),
            "Extraction should be idempotent"
        );

        for (m1, m2) in methods1.iter().zip(methods2.iter()) {
            prop_assert_eq!(m1.name, m2.name);
            prop_assert_eq!(m1.body, m2.body);
            prop_assert_eq!(m1.start_line, m2.start_line);
            prop_assert_eq!(m1.end_line, m2.end_line);
        }
    });
}
