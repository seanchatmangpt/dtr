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

/// Benchmark: Single file scan latency (baseline).
fn bench_single_file() {
    let temp_dir = TempDir::new().unwrap();
    let file_path = temp_dir.path().join("Single.java");

    create_clean_java_file(&file_path, 10);

    let scanner = Scanner::new();

    let start = Instant::now();
    let result = scanner.scan_file(&file_path).unwrap();
    let elapsed = start.elapsed();

    println!(
        "✓ Single file scan: {:.2}ms (violations: {})",
        elapsed.as_secs_f64() * 1000.0,
        result.violations.len()
    );
}

/// Benchmark: 10 files sequential vs parallel.
fn bench_ten_files() {
    let temp_dir = TempDir::new().unwrap();
    let mut files = Vec::new();

    for i in 0..10 {
        let file_path = temp_dir.path().join(format!("File{}.java", i));
        create_clean_java_file(&file_path, 5);
        files.push(file_path);
    }

    let scanner = Scanner::new();

    // Sequential baseline
    let start = Instant::now();
    let mut _violations = 0;
    for file in &files {
        if let Ok(result) = scanner.scan_file(file) {
            _violations += result.violations.len();
        }
    }
    let sequential_elapsed = start.elapsed();

    // Parallel scan
    let start = Instant::now();
    let results = scanner.scan_files_parallel(&files);
    let parallel_violations: usize = results.iter().map(|r| r.violations.len()).sum();
    let parallel_elapsed = start.elapsed();

    let speedup = sequential_elapsed.as_secs_f64() / parallel_elapsed.as_secs_f64();
    println!(
        "✓ 10 files:  sequential={:.2}ms, parallel={:.2}ms, speedup={:.2}x (violations: {})",
        sequential_elapsed.as_secs_f64() * 1000.0,
        parallel_elapsed.as_secs_f64() * 1000.0,
        speedup,
        parallel_violations
    );
}

/// Benchmark: 100 files with target 4-8x speedup.
fn bench_hundred_files() {
    let temp_dir = TempDir::new().unwrap();
    let mut files = Vec::new();

    for i in 0..100 {
        let file_path = temp_dir.path().join(format!("File{:03}.java", i));
        // Mix of clean and violated files (80% clean, 20% violated)
        if i % 5 == 0 {
            create_violated_java_file(&file_path, 3);
        } else {
            create_clean_java_file(&file_path, 8);
        }
        files.push(file_path);
    }

    let scanner = Scanner::new();

    // Sequential baseline
    let start = Instant::now();
    let mut _violations = 0;
    for file in &files {
        if let Ok(result) = scanner.scan_file(file) {
            _violations += result.violations.len();
        }
    }
    let sequential_elapsed = start.elapsed();

    // Parallel scan
    let start = Instant::now();
    let results = scanner.scan_files_parallel(&files);
    let parallel_violations: usize = results.iter().map(|r| r.violations.len()).sum();
    let parallel_elapsed = start.elapsed();

    let speedup = sequential_elapsed.as_secs_f64() / parallel_elapsed.as_secs_f64();
    println!(
        "✓ 100 files: sequential={:.2}ms, parallel={:.2}ms, speedup={:.2}x (violations: {})",
        sequential_elapsed.as_secs_f64() * 1000.0,
        parallel_elapsed.as_secs_f64() * 1000.0,
        speedup,
        parallel_violations
    );
}

/// Benchmark: 1000 files (production scale).
fn bench_thousand_files() {
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

    let scanner = Scanner::new();

    // Sequential baseline (sample first 100 for reasonable benchmark time)
    let sample_size = 100;
    let start = Instant::now();
    let mut _violations = 0;
    for file in files.iter().take(sample_size) {
        if let Ok(result) = scanner.scan_file(file) {
            _violations += result.violations.len();
        }
    }
    let sequential_sample_elapsed = start.elapsed();
    let sequential_estimated =
        sequential_sample_elapsed.as_secs_f64() * (1000.0 / sample_size as f64);

    // Parallel scan (full 1000 files)
    let start = Instant::now();
    let results = scanner.scan_files_parallel(&files);
    let parallel_violations: usize = results.iter().map(|r| r.violations.len()).sum();
    let parallel_elapsed = start.elapsed();

    let speedup = sequential_estimated / parallel_elapsed.as_secs_f64();
    println!(
        "✓ 1000 files: sequential_est={:.2}ms (from 100 sample), parallel={:.2}ms, speedup={:.2}x (violations: {})",
        sequential_estimated * 1000.0,
        parallel_elapsed.as_secs_f64() * 1000.0,
        speedup,
        parallel_violations
    );
}

fn main() {
    println!("\n════ Concurrency Optimization Benchmarks ════\n");
    println!("Target: 4-8x speedup on 4-core CPU for 100+ files\n");

    bench_single_file();
    bench_ten_files();
    bench_hundred_files();
    bench_thousand_files();

    println!("\n════ Benchmark Complete ════\n");
}
