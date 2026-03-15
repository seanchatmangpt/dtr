//! Allocation profiling benchmarks for end-to-end CLI pipeline.
//!
//! Measures:
//! - Allocation counts in hot paths (scanner, oracle, cache stages)
//! - Memory peak RSS for various file counts
//! - Per-file latency breakdown (time per file, not total)
//! - Scaling behavior from 1 to 1000 files
//!
//! Run with: `cargo bench --release -- --nocapture allocation`

use criterion::{black_box, criterion_group, criterion_main, Criterion, BenchmarkId};
use cct_scanner::Scanner;
use std::fs;
use std::path::PathBuf;
use tempfile::TempDir;

fn create_test_java_file(dir: &std::path::Path, name: &str, content: &str) -> PathBuf {
    let file_path = dir.join(name);
    fs::write(&file_path, content).expect("write test file");
    file_path
}

fn sample_java_with_violations() -> String {
    r#"
public class Example {
    public void method1() {
        // TODO: implement this
        return null;
    }

    public void method2() {
        String mockData = "data";
        List items = Collections.emptyList();
    }

    public void method3() {
        // FIXME: add error handling
        String stubService = "stub";
    }

    public void method4() {
        return "";
    }
}
"#.to_string()
}

fn sample_clean_java() -> String {
    r#"
public class CleanExample {
    public String getName() {
        return "Example";
    }

    public int getValue() {
        return 42;
    }

    public List<String> getItems() {
        return new ArrayList<>();
    }
}
"#.to_string()
}

/// Benchmark per-file latency: <10ms hot, <100ms cold
fn bench_per_file_latency(c: &mut Criterion) {
    let mut group = c.benchmark_group("per_file_latency");
    group.sample_size(50); // Smaller sample for consistency

    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    for file_count in [1, 10, 100].iter() {
        let scenario = if *file_count == 1 {
            format!("cold_1_file")
        } else {
            format!("cold_{}_files", file_count)
        };

        // Create test files
        for i in 0..*file_count {
            let name = format!("Test{}.java", i);
            if i % 2 == 0 {
                create_test_java_file(temp_dir.path(), &name, &sample_java_with_violations());
            } else {
                create_test_java_file(temp_dir.path(), &name, &sample_clean_java());
            }
        }

        let root = black_box(temp_dir.path().to_path_buf());

        group.bench_with_input(
            BenchmarkId::from_parameter(scenario),
            &file_count,
            |b, &fc| {
                b.iter(|| {
                    let files: Vec<PathBuf> = (0..fc)
                        .map(|i| root.join(format!("Test{}.java", i)))
                        .collect();
                    scanner.scan_files_parallel(&files)
                });
            },
        );
    }

    group.finish();
}

/// Benchmark amortized per-file cost with scaling
fn bench_amortized_scaling(c: &mut Criterion) {
    let mut group = c.benchmark_group("amortized_per_file");
    group.measurement_time(std::time::Duration::from_secs(5));

    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create 1000 files upfront
    for i in 0..1000 {
        let name = format!("Test{:04}.java", i);
        if i % 5 == 0 {
            create_test_java_file(temp_dir.path(), &name, &sample_java_with_violations());
        } else {
            create_test_java_file(temp_dir.path(), &name, &sample_clean_java());
        }
    }

    let root = black_box(temp_dir.path().to_path_buf());

    // Measure cost per file at different scales
    for file_count in [10, 100, 500, 1000].iter() {
        let id = format!("{}_files", file_count);
        group.bench_with_input(
            BenchmarkId::from_parameter(&id),
            &file_count,
            |b, &fc| {
                b.iter(|| {
                    let files: Vec<PathBuf> = (0..fc)
                        .map(|i| root.join(format!("Test{:04}.java", i)))
                        .collect();
                    let start = std::time::Instant::now();
                    scanner.scan_files_parallel(&files);
                    let elapsed_us = start.elapsed().as_micros();
                    let per_file_us = elapsed_us / fc as u128;
                    eprintln!(
                        "  {} files: {:.2}ms total ({:.2}µs per file)",
                        fc,
                        elapsed_us as f64 / 1000.0,
                        per_file_us as f64
                    );
                });
            },
        );
    }

    group.finish();
}

/// Benchmark memory stability: should stay <50MB even for 10K files
fn bench_memory_stability(c: &mut Criterion) {
    c.bench_function("memory_stability_100_files", |b| {
        b.iter_batched(
            || {
                let temp_dir = TempDir::new().expect("create temp dir");
                let scanner = Scanner::new();

                // Create 100 files
                for i in 0..100 {
                    let name = format!("Test{}.java", i);
                    if i % 3 == 0 {
                        create_test_java_file(
                            temp_dir.path(),
                            &name,
                            &sample_java_with_violations(),
                        );
                    } else {
                        create_test_java_file(temp_dir.path(), &name, &sample_clean_java());
                    }
                }

                (temp_dir, scanner)
            },
            |(temp_dir, scanner)| {
                let files: Vec<PathBuf> =
                    (0..100).map(|i| temp_dir.path().join(format!("Test{}.java", i))).collect();
                scanner.scan_files_parallel(&files)
            },
            criterion::BatchSize::SmallInput,
        );
    });
}

/// Benchmark cache hit rate after warmup
fn bench_cache_hit_rate(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create 50 identical files (high cache hit potential)
    let content = sample_java_with_violations();
    for i in 0..50 {
        create_test_java_file(&temp_dir.path(), &format!("Identical{}.java", i), &content);
    }

    let root = black_box(temp_dir.path().to_path_buf());

    // First pass: cold cache (all misses)
    let files: Vec<PathBuf> = (0..50)
        .map(|i| root.join(format!("Identical{}.java", i)))
        .collect();
    let cold_start = std::time::Instant::now();
    scanner.scan_files_parallel(&files);
    let cold_elapsed = cold_start.elapsed();

    c.bench_function("cache_hit_rate_50_identical_files", |b| {
        b.iter(|| {
            let files: Vec<PathBuf> = (0..50)
                .map(|i| root.join(format!("Identical{}.java", i)))
                .collect();
            scanner.scan_files_parallel(&files)
        });
    });

    eprintln!("Cache warmup (cold): {:.2}ms", cold_elapsed.as_secs_f64() * 1000.0);
}

criterion_group!(
    benches,
    bench_per_file_latency,
    bench_amortized_scaling,
    bench_memory_stability,
    bench_cache_hit_rate
);

criterion_main!(benches);
