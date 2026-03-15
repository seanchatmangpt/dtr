use cct_oracle::{NaiveBayesOracle, RiskScorer, ViolationRecord};
use chrono::Utc;
use criterion::{black_box, criterion_group, criterion_main, BenchmarkId, Criterion};
use rayon::prelude::*;

fn create_test_violation(pattern: &str, days_ago: i64) -> ViolationRecord {
    let timestamp = Utc::now() - chrono::Duration::days(days_ago);
    ViolationRecord {
        pattern: pattern.to_string(),
        timestamp,
    }
}

fn bench_score_single_file(c: &mut Criterion) {
    let scorer = RiskScorer::new();

    let violation_history = black_box(vec![
        create_test_violation("H_TODO", 5),
        create_test_violation("H_STUB_NULL", 10),
        create_test_violation("H_MOCK", 3),
        create_test_violation("H_EMPTY", 20),
        create_test_violation("H_SILENT", 15),
    ]);

    c.bench_function("score_single_file_5violations", |b| {
        b.iter(|| scorer.score_risk(&violation_history));
    });
}

fn bench_score_batch_100(c: &mut Criterion) {
    let scorer = black_box(RiskScorer::new());

    // Create 100 files with varying violation histories
    let files: Vec<Vec<ViolationRecord>> = (0..100)
        .map(|i| {
            vec![
                create_test_violation("H_TODO", 5 + (i % 10) as i64),
                create_test_violation("H_STUB_NULL", 10 + (i % 20) as i64),
                create_test_violation("H_MOCK", 3 + (i % 5) as i64),
            ]
        })
        .collect();

    let files = black_box(files);

    c.bench_with_input(
        BenchmarkId::new("score_batch_parallel", "100_files"),
        &100,
        |b, &_n| {
            b.iter(|| {
                files
                    .par_iter()
                    .map(|violations| scorer.score_risk(violations))
                    .collect::<Vec<_>>()
            });
        },
    );
}

fn bench_score_batch_scaling(c: &mut Criterion) {
    let scorer = RiskScorer::new();

    for batch_size in [1, 10, 100].iter() {
        let files: Vec<Vec<ViolationRecord>> = (0..*batch_size)
            .map(|i| {
                vec![
                    create_test_violation("H_TODO", 5 + (i % 10) as i64),
                    create_test_violation("H_STUB_NULL", 10 + (i % 20) as i64),
                    create_test_violation("H_MOCK", 3 + (i % 5) as i64),
                ]
            })
            .collect();

        let files = black_box(files);
        let scorer = black_box(&scorer);

        c.bench_with_input(
            BenchmarkId::new("score_batch_scaling", format!("{}_files", batch_size)),
            batch_size,
            |b, _| {
                b.iter(|| {
                    files
                        .par_iter()
                        .map(|violations| scorer.score_risk(violations))
                        .collect::<Vec<_>>()
                });
            },
        );
    }
}

fn bench_model_training(c: &mut Criterion) {
    let mut oracle = NaiveBayesOracle::new();

    // Create 1000 training samples
    for i in 0..1000 {
        let history = vec![
            create_test_violation("H_TODO", 10 + (i % 50) as i64),
            create_test_violation("H_STUB_NULL", 20 + (i % 30) as i64),
            create_test_violation("H_MOCK", 5 + (i % 15) as i64),
            create_test_violation("H_EMPTY", 30 + (i % 60) as i64),
        ];
        oracle.add_training_sample(&format!("src/File{}.java", i), &history);
    }

    c.bench_function("naive_bayes_train_1000_samples", |b| {
        b.iter(|| {
            oracle.train();
        });
    });
}

fn bench_predict_after_training(c: &mut Criterion) {
    let mut oracle = NaiveBayesOracle::new();

    // Train on 500 samples
    for i in 0..500 {
        let history = vec![
            create_test_violation("H_TODO", 10 + (i % 30) as i64),
            create_test_violation("H_STUB_NULL", 20 + (i % 20) as i64),
        ];
        oracle.add_training_sample(&format!("src/Train{}.java", i), &history);
    }
    oracle.train();

    let test_history = black_box(vec![
        create_test_violation("H_TODO", 7),
        create_test_violation("H_STUB_NULL", 15),
        create_test_violation("H_MOCK", 2),
    ]);

    c.bench_function("naive_bayes_predict_single", |b| {
        b.iter(|| oracle.predict(&test_history));
    });
}

criterion_group!(
    benches,
    bench_score_single_file,
    bench_score_batch_100,
    bench_score_batch_scaling,
    bench_model_training,
    bench_predict_after_training
);

criterion_main!(benches);
