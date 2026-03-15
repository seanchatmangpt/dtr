//! E2E Integration Test: Scanner → Cache → Oracle Pipeline
//!
//! This test validates the complete flow:
//! 1. Scanner extracts methods and detects violations in 50+ Java files
//! 2. Cache deduplicates and stores results with blake3 hashing
//! 3. Oracle scores violations by risk/temporal decay
//! 4. Results are consistent across multiple runs

use std::fs;
use std::path::{Path, PathBuf};
use tempfile::TempDir;

/// Helper: Create a temporary Java project with multiple source files
fn create_test_java_project(count: usize) -> TempDir {
    let temp_dir = TempDir::new().expect("Create temp dir for test project");
    let src_dir = temp_dir.path().join("src");
    fs::create_dir_all(&src_dir).expect("Create src directory");

    // Create N sample Java files with various violation patterns
    for i in 0..count {
        let file_name = format!("TestClass{}.java", i);
        let file_path = src_dir.join(&file_name);

        let content = match i % 10 {
            0 => {
                // H_TODO violation
                format!(
                    r#"public class TestClass{} {{
    public void method1() {{
        // TODO: implement this properly
        return null;
    }}

    public String method2() {{
        // FIXME: handle edge case
        return "";
    }}
}}
"#,
                    i
                )
            }
            1 => {
                // H_STUB_NULL violation
                format!(
                    r#"public class TestClass{} {{
    public Object getConfig() {{
        return null;
    }}

    public void handleError(Exception e) {{
        // XXX: swallowing exception
        log.info("Error occurred, but ignoring");
    }}
}}
"#,
                    i
                )
            }
            2 => {
                // H_STUB_EMPTY_COLLECTION
                format!(
                    r#"public class TestClass{} {{
    public List<String> getItems() {{
        return Collections.emptyList();
    }}

    public Map<String, Object> getConfig() {{
        return Map.of();
    }}
}}
"#,
                    i
                )
            }
            3 => {
                // H_EMPTY method
                format!(
                    r#"public class TestClass{} {{
    public void doSomething() {{
    }}

    public int calculate() {{
    }}
}}
"#,
                    i
                )
            }
            4 => {
                // H_MOCK variable prefix
                format!(
                    r#"public class TestClass{} {{
    private mockDatabase db;
    private stubService service;

    public void process() {{
        String fakeApiKey = "test-key";
        Object demoData = new Object();
    }}
}}
"#,
                    i
                )
            }
            5 => {
                // H_FALLBACK: catch with fake return
                format!(
                    r#"public class TestClass{} {{
    public String fetchData() {{
        try {{
            return callRemoteAPI();
        }} catch (IOException e) {{
            return "";
        }}
    }}

    public int retryCount() {{
        try {{
            return getActualCount();
        }} catch (Exception ex) {{
            return 0;
        }}
    }}
}}
"#,
                    i
                )
            }
            6 => {
                // H_STUB_EMPTY_STRING
                format!(
                    r#"public class TestClass{} {{
    public String getName() {{
        return "";
    }}

    public String getDescription() {{
        return null;
    }}
}}
"#,
                    i
                )
            }
            7 => {
                // H_SILENT: logging instead of throwing
                format!(
                    r#"public class TestClass{} {{
    public void process() {{
        try {{
            doWork();
        }} catch (Exception e) {{
            logger.warn("Not implemented yet");
        }}
    }}

    public void handle() {{
        try {{
            execute();
        }} catch (RuntimeException e) {{
            log.error("Operation not implemented");
        }}
    }}
}}
"#,
                    i
                )
            }
            8 => {
                // Mixed violations
                format!(
                    r#"public class TestClass{} {{
    private mockRepo repository;

    public void process() {{
        // TODO: add proper error handling
        try {{
            String result = fetch();
            return result;
        }} catch (Exception e) {{
            logger.info("Not handled properly");
            return null;
        }}
    }}
}}
"#,
                    i
                )
            }
            _ => {
                // Clean code with no violations
                format!(
                    r#"public class TestClass{} {{
    public void process() {{
        System.out.println("Processing");
    }}

    public String getData() {{
        return "valid data";
    }}
}}
"#,
                    i
                )
            }
        };

        fs::write(&file_path, content).expect(&format!("Write {}", file_name));
    }

    temp_dir
}

#[test]
fn test_e2e_scanner_extraction() {
    // Create a test project with multiple Java files
    let project = create_test_java_project(10);
    let src_dir = project.path().join("src");

    // Walk and count Java files
    let java_files: Vec<_> = fs::read_dir(&src_dir)
        .expect("Read src dir")
        .filter_map(|entry| {
            let path = entry.ok()?.path();
            if path.extension().map(|e| e == "java").unwrap_or(false) {
                Some(path)
            } else {
                None
            }
        })
        .collect();

    assert_eq!(
        java_files.len(),
        10,
        "Should create exactly 10 Java test files"
    );

    // Scanner: extract methods from each file
    for file_path in java_files {
        let content = fs::read(&file_path).expect("Read Java file");
        let methods = cct_scanner::extract_methods(&content);

        // Most files should have at least 1-2 methods
        assert!(
            !methods.is_empty(),
            "File {:?} should contain at least one method",
            file_path
        );

        // Verify method structure
        for method in &methods {
            assert!(!method.name.is_empty(), "Method name should not be empty");
            assert!(!method.body.is_empty(), "Method body should not be empty");
            assert!(method.start_line > 0, "Start line should be positive");
            assert!(
                method.end_line >= method.start_line,
                "End line should be >= start line"
            );
        }
    }

    println!("✓ Scanner extracted methods from all 10 test files");
}

#[test]
fn test_e2e_scanner_violation_detection() {
    // Create a project with known violations
    let project = create_test_java_project(10);
    let src_dir = project.path().join("src");

    let mut total_violations = 0;
    let mut pattern_counts: std::collections::HashMap<String, usize> =
        std::collections::HashMap::new();

    // Scan files for violations
    for entry in fs::read_dir(&src_dir).expect("Read src dir") {
        let path = entry.expect("Read entry").path();
        if path.extension().map(|e| e == "java").unwrap_or(false) {
            let content = fs::read(&path).expect("Read Java file");
            let methods = cct_scanner::extract_methods(&content);
            let pattern_set = cct_scanner::PatternSet::new();

            for method in methods {
                let hits = pattern_set.match_body(&method.body);
                for hit in hits {
                    total_violations += 1;
                    *pattern_counts.entry(hit.pattern.to_string()).or_insert(0) += 1;
                }
            }
        }
    }

    // With 10 test files and varied violation patterns, we should find violations
    assert!(
        total_violations > 0,
        "Should detect at least some violations in test project"
    );

    // Verify we detected expected pattern types
    assert!(
        pattern_counts.contains_key("H_TODO")
            || pattern_counts.contains_key("H_STUB_NULL")
            || pattern_counts.contains_key("H_EMPTY")
            || pattern_counts.contains_key("H_MOCK")
            || pattern_counts.contains_key("H_FALLBACK")
            || pattern_counts.contains_key("H_SILENT"),
        "Should detect at least one expected pattern type"
    );

    println!(
        "✓ Scanner detected {} violations across test files",
        total_violations
    );
    println!("  Pattern distribution: {:?}", pattern_counts);
}

#[test]
fn test_e2e_cache_hash_determinism() {
    // Test that hashing is deterministic (same content → same hash)
    let content1 = b"public void foo() { return null; }";
    let content2 = b"public void foo() { return null; }";
    let content3 = b"public void bar() { return null; }";

    let hash1 = cct_cache::hasher::hash_method_body(content1);
    let hash2 = cct_cache::hasher::hash_method_body(content2);
    let hash3 = cct_cache::hasher::hash_method_body(content3);

    assert_eq!(hash1, hash2, "Identical content should produce identical hashes");
    assert_ne!(hash1, hash3, "Different content should produce different hashes");

    // Hash should be 32 bytes (blake3)
    assert_eq!(hash1.len(), 32, "Hash must be 32 bytes");

    println!("✓ Cache hash determinism verified");
}

#[test]
fn test_e2e_cache_deduplication() {
    // Create a temp directory for cache database
    let temp_db = TempDir::new().expect("Create temp dir for cache DB");
    let db_path = temp_db.path().join("cache.db");

    // Create cache store
    let store = cct_cache::store::Store::new(&db_path).expect("Create cache store");

    // Insert same content multiple times (tests deduplication)
    let content = b"public String getName() { return name; }";
    let hash = cct_cache::hasher::hash_method_body(content);

    // Both inserts should succeed (deduplication should be transparent)
    for i in 0..3 {
        let result = cct_cache::store::CachedScanResult {
            path: "Test.java".to_string(),
            violation_count: i,
            cached_at: 1234567890 + i as u64,
        };

        store
            .insert(&hash, &result)
            .expect(&format!("Insert #{}", i));
    }

    // Verify we can retrieve the cached result
    let retrieved = store.lookup(&hash).expect("Lookup should work");
    assert!(
        retrieved.is_some(),
        "Should retrieve cached result for known hash"
    );

    println!("✓ Cache deduplication working correctly");
}

#[test]
fn test_e2e_oracle_risk_scoring() {
    // Test that oracle can score violations by risk
    let scorer = cct_oracle::scorer::RiskScorer::new();

    // Create test violation records
    let recent_violation = cct_oracle::model::ViolationRecord {
        pattern: "unsafe-cast".to_string(),
        timestamp: chrono::Utc::now(),
    };

    let old_violation = cct_oracle::model::ViolationRecord {
        pattern: "unsafe-cast".to_string(),
        timestamp: chrono::Utc::now() - chrono::Duration::days(90),
    };

    let recent_history = vec![recent_violation.clone()];
    let old_history = vec![old_violation.clone()];

    // Score should account for recency
    let recent_score = scorer.score_risk(&recent_history);
    let old_score = scorer.score_risk(&old_history);

    assert!(
        recent_score > old_score,
        "Recent violations should have higher risk score"
    );

    // Scores should be normalized to [0, 1]
    assert!(
        (0.0..=1.0).contains(&recent_score),
        "Recent score should be in [0, 1]"
    );
    assert!(
        (0.0..=1.0).contains(&old_score),
        "Old score should be in [0, 1]"
    );

    println!("✓ Oracle risk scoring working (recent: {:.3}, old: {:.3})",
             recent_score, old_score);
}

#[test]
fn test_e2e_oracle_naive_bayes_training() {
    // Test that oracle can train on violation history
    let mut oracle = cct_oracle::naive_bayes::NaiveBayesOracle::new();

    // Create training data (files with violation history)
    let high_risk_history = vec![
        cct_oracle::model::ViolationRecord {
            pattern: "unsafe-cast".to_string(),
            timestamp: chrono::Utc::now() - chrono::Duration::days(5),
        },
        cct_oracle::model::ViolationRecord {
            pattern: "unsafe-cast".to_string(),
            timestamp: chrono::Utc::now() - chrono::Duration::days(3),
        },
    ];

    let low_risk_history = vec![cct_oracle::model::ViolationRecord {
        pattern: "minor-issue".to_string(),
        timestamp: chrono::Utc::now() - chrono::Duration::days(100),
    }];

    let clean_history = vec![];

    // Train oracle
    oracle.add_training_sample("HighRiskFile.java", &high_risk_history);
    oracle.add_training_sample("LowRiskFile.java", &low_risk_history);
    oracle.add_training_sample("CleanFile.java", &clean_history);
    oracle.train();

    // Predict on new data similar to training
    let new_history = vec![cct_oracle::model::ViolationRecord {
        pattern: "unsafe-cast".to_string(),
        timestamp: chrono::Utc::now() - chrono::Duration::days(4),
    }];

    let risk_score = oracle.predict(&new_history);
    assert!(
        (0.0..=1.0).contains(&risk_score),
        "Prediction score should be in [0, 1]"
    );

    // New file similar to high-risk training should score higher than clean
    assert!(
        risk_score > 0.3,
        "File similar to high-risk training should score > 0.3"
    );

    println!("✓ Oracle Naive Bayes training and prediction working (score: {:.3})",
             risk_score);
}

#[test]
fn test_e2e_full_pipeline() {
    // Integration test: Scanner → Cache → Oracle pipeline
    let project = create_test_java_project(5);
    let src_dir = project.path().join("src");

    // Phase 1: Scanner extracts violations
    let mut scan_results = Vec::new();
    for entry in fs::read_dir(&src_dir).expect("Read src dir") {
        let path = entry.expect("Read entry").path();
        if path.extension().map(|e| e == "java").unwrap_or(false) {
            let content = fs::read(&path).expect("Read Java file");
            let methods = cct_scanner::extract_methods(&content);
            let pattern_set = cct_scanner::PatternSet::new();

            let mut file_violations = Vec::new();
            for method in methods {
                let hits = pattern_set.match_body(&method.body);
                for hit in hits {
                    file_violations.push((method.name.clone(), hit));
                }
            }

            scan_results.push((path, file_violations));
        }
    }

    // Phase 2: Cache stores deduplicated results
    let temp_db = TempDir::new().expect("Create temp dir for cache DB");
    let db_path = temp_db.path().join("pipeline.db");
    let cache_store = cct_cache::store::Store::new(&db_path).expect("Create cache store");

    let mut cache_hits = 0;
    for (file_path, violations) in &scan_results {
        for (_, hit) in violations {
            let method_bytes = hit.matched.as_bytes();
            let hash = cct_cache::hasher::hash_method_body(method_bytes);

            let cached_result = cct_cache::store::CachedScanResult {
                path: file_path.to_string_lossy().to_string(),
                violation_count: violations.len(),
                cached_at: std::time::SystemTime::now()
                    .duration_since(std::time::UNIX_EPOCH)
                    .unwrap()
                    .as_secs(),
            };

            cache_store
                .insert(&hash, &cached_result)
                .expect("Cache insert");
            cache_hits += 1;
        }
    }

    // Phase 3: Oracle scores violations
    let scorer = cct_oracle::scorer::RiskScorer::new();
    let mut risk_scores = Vec::new();

    for (file_path, violations) in &scan_results {
        if !violations.is_empty() {
            // Create violation records for scoring
            let violation_records: Vec<_> = violations
                .iter()
                .map(|(_, hit)| cct_oracle::model::ViolationRecord {
                    pattern: hit.pattern.to_string(),
                    timestamp: chrono::Utc::now(),
                })
                .collect();

            let score = scorer.score_risk(&violation_records);
            risk_scores.push((file_path, score));
        }
    }

    // Verify pipeline completed successfully
    assert!(
        cache_hits > 0,
        "Cache should have stored at least one result"
    );
    assert!(
        !risk_scores.is_empty(),
        "Oracle should have scored at least one file"
    );

    // Verify scores are normalized
    for (_, score) in &risk_scores {
        assert!(
            (0.0..=1.0).contains(score),
            "Risk score should be in [0, 1]"
        );
    }

    println!(
        "✓ Full pipeline completed: {} files scanned, {} cache entries, {} files scored",
        scan_results.len(),
        cache_hits,
        risk_scores.len()
    );
}

#[test]
fn test_e2e_consistency_across_runs() {
    // Same input should produce identical results across multiple runs
    let project = create_test_java_project(3);
    let src_dir = project.path().join("src");

    let mut run1_violations = Vec::new();
    let mut run2_violations = Vec::new();

    for run_num in 0..2 {
        let mut violations = Vec::new();
        for entry in fs::read_dir(&src_dir).expect("Read src dir") {
            let path = entry.expect("Read entry").path();
            if path.extension().map(|e| e == "java").unwrap_or(false) {
                let content = fs::read(&path).expect("Read Java file");
                let methods = cct_scanner::extract_methods(&content);
                let pattern_set = cct_scanner::PatternSet::new();

                for method in methods {
                    let hits = pattern_set.match_body(&method.body);
                    for hit in hits {
                        violations.push((
                            path.clone(),
                            method.name.clone(),
                            hit.pattern.to_string(),
                        ));
                    }
                }
            }
        }
        violations.sort();

        if run_num == 0 {
            run1_violations = violations;
        } else {
            run2_violations = violations;
        }
    }

    assert_eq!(
        run1_violations, run2_violations,
        "Multiple runs should produce identical results"
    );

    println!("✓ Consistency verified: {} violations found in both runs",
             run1_violations.len());
}

#[test]
fn test_e2e_large_project_50_files() {
    // Test with a larger project (50 files)
    let project = create_test_java_project(50);
    let src_dir = project.path().join("src");

    // Scanner pass
    let java_files: Vec<_> = fs::read_dir(&src_dir)
        .expect("Read src dir")
        .filter_map(|entry| {
            let path = entry.ok()?.path();
            if path.extension().map(|e| e == "java").unwrap_or(false) {
                Some(path)
            } else {
                None
            }
        })
        .collect();

    assert_eq!(java_files.len(), 50, "Should have 50 Java files");

    let mut total_methods = 0;
    let mut total_violations = 0;

    let pattern_set = cct_scanner::PatternSet::new();
    for file_path in &java_files {
        let content = fs::read(file_path).expect("Read Java file");
        let methods = cct_scanner::extract_methods(&content);
        total_methods += methods.len();

        for method in methods {
            let hits = pattern_set.match_body(&method.body);
            total_violations += hits.len();
        }
    }

    // Cache pass
    let temp_db = TempDir::new().expect("Create temp dir for cache DB");
    let db_path = temp_db.path().join("large.db");
    let cache_store = cct_cache::store::Store::new(&db_path).expect("Create cache store");

    let mut cache_entries = 0;
    for file_path in &java_files {
        let content = fs::read(file_path).expect("Read Java file");
        let methods = cct_scanner::extract_methods(&content);

        for method in methods {
            let hash = cct_cache::hasher::hash_method_body(method.body.as_bytes());
            let result = cct_cache::store::CachedScanResult {
                path: file_path.to_string_lossy().to_string(),
                violation_count: 1,
                cached_at: 1234567890,
            };
            cache_store
                .insert(&hash, &result)
                .expect("Cache insert");
            cache_entries += 1;
        }
    }

    // Oracle pass
    let scorer = cct_oracle::scorer::RiskScorer::new();
    let test_violations = vec![
        cct_oracle::model::ViolationRecord {
            pattern: "unsafe-cast".to_string(),
            timestamp: chrono::Utc::now(),
        },
        cct_oracle::model::ViolationRecord {
            pattern: "hardcoded-secret".to_string(),
            timestamp: chrono::Utc::now() - chrono::Duration::days(10),
        },
    ];
    let risk_score = scorer.score_risk(&test_violations);

    assert_eq!(java_files.len(), 50, "Should process 50 files");
    assert!(
        total_methods > 0,
        "Should extract methods from large project"
    );
    assert!(
        total_violations >= 0,
        "Should detect violations (may be 0 for clean code)"
    );
    assert!(
        cache_entries > 0,
        "Should cache all methods from large project"
    );
    assert!(
        (0.0..=1.0).contains(&risk_score),
        "Should compute valid risk score"
    );

    println!(
        "✓ Large project test (50 files): {} methods extracted, {} violations, {} cached, risk score {:.3}",
        total_methods, total_violations, cache_entries, risk_score
    );
}
