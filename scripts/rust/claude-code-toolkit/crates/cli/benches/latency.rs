//! Concurrency optimization benchmarks for cct scanner stack.
//!
//! Measures end-to-end latency improvements from parallel processing:
//! - Single file: baseline (1-10ms expected)
//! - 10 files: measure parallel overhead (10-50ms expected)
//! - 100 files: target 4-8x speedup on 4-core CPU
//! - 1000 files: extrapolate to production scale
//!
//! Uses Criterion framework for statistical analysis with percentile reporting.
//! Run with: `cargo bench --release latency -- --nocapture --verbose`

use cct_scanner::Scanner;
use criterion::{black_box, criterion_group, criterion_main, BenchmarkId, Criterion};
use std::fs;
use std::io::Write;
use std::path::PathBuf;
use tempfile::TempDir;

/// Generate a clean Java file for benchmarking.
fn create_clean_java_file(path: &PathBuf, method_count: usize) {
    let mut methods = String::new();
    for i in 0..method_count {
        methods.push_str(&format!(
            "    public int method{}(int x, int y) {{\n        return x + {} + y;\n    }}\n",
            i, i
        ));
    }

    let java_code = format!(
        r#"public class BenchmarkClass {{
{}
}}
"#,
        methods
    );

    let mut file = fs::File::create(path).unwrap();
    file.write_all(java_code.as_bytes()).unwrap();
}

/// Generate a Java file with violations for realistic benchmarks.
fn create_violated_java_file(path: &PathBuf, violation_count: usize) {
    let mut methods = String::new();
    for i in 0..violation_count {
        methods.push_str(&format!(
            "    public void method{}() {{\n        // TODO: implement {}\n        return null;\n    }}\n",
            i, i
        ));
    }

    let java_code = format!(
        r#"public class BenchmarkViolated {{
{}
}}
"#,
        methods
    );

    let mut file = fs::File::create(path).unwrap();
    file.write_all(java_code.as_bytes()).unwrap();
}

/// Criterion-based benchmark: Single file scan latency (baseline).
fn criterion_single_file(c: &mut Criterion) {
    let temp_dir = TempDir::new().unwrap();
    let file_path = black_box(temp_dir.path().join("Single.java"));
    create_clean_java_file(&file_path, 10);

    let scanner = Scanner::new();

    c.bench_function("latency_single_file_baseline", |b| {
        b.iter(|| {
            let _ = scanner.scan_file(&file_path);
        });
    });
}

/// Criterion-based benchmark: 10 files parallel scan.
fn criterion_ten_files(c: &mut Criterion) {
    let temp_dir = TempDir::new().unwrap();
    let mut files = Vec::new();

    for i in 0..10 {
        let file_path = temp_dir.path().join(format!("File{}.java", i));
        create_clean_java_file(&file_path, 5);
        files.push(file_path);
    }

    let files = black_box(files);
    let scanner = Scanner::new();

    c.bench_function("latency_ten_files_parallel", |b| {
        b.iter(|| {
            let _ = scanner.scan_files_parallel(&files);
        });
    });
}

/// Criterion-based benchmark: 100 files parallel scan with statistical percentiles.
fn criterion_hundred_files(c: &mut Criterion) {
    let temp_dir = TempDir::new().unwrap();
    let mut files = Vec::new();

    for i in 0..100 {
        let file_path = temp_dir.path().join(format!("File{:03}.java", i));
        if i % 5 == 0 {
            create_violated_java_file(&file_path, 3);
        } else {
            create_clean_java_file(&file_path, 8);
        }
        files.push(file_path);
    }

    let files = black_box(files);
    let scanner = Scanner::new();

    let mut group = c.benchmark_group("latency_100_files");
    group.sample_size(20); // 20 samples for statistical analysis

    group.bench_function("parallel_scan", |b| {
        b.iter(|| {
            let _ = scanner.scan_files_parallel(&files);
        });
    });

    group.finish();
}

/// Criterion-based benchmark: 1000 files parallel scan (production scale).
fn criterion_thousand_files(c: &mut Criterion) {
    let temp_dir = TempDir::new().unwrap();
    let mut files = Vec::new();

    for i in 0..1000 {
        let file_path = temp_dir.path().join(format!("File{:04}.java", i));
        if i % 10 == 0 {
            create_violated_java_file(&file_path, 2);
        } else {
            create_clean_java_file(&file_path, 5);
        }
        files.push(file_path);
    }

    let files = black_box(files);
    let scanner = Scanner::new();

    let mut group = c.benchmark_group("latency_1000_files");
    group.sample_size(10); // Smaller sample for large workload

    group.bench_function("parallel_scan", |b| {
        b.iter(|| {
            let _ = scanner.scan_files_parallel(&files);
        });
    });

    group.finish();
}

criterion_group!(
    benches,
    criterion_single_file,
    criterion_ten_files,
    criterion_hundred_files,
    criterion_thousand_files
);
criterion_main!(benches);
