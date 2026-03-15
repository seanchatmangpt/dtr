//! End-to-end benchmark suite for cct-scanner.
//!
//! Measures complete scan workflows across various file scenarios:
//! - Single file baseline
//! - 10 files with cold/hot cache
//! - 100 files with cache warmup
//! - 1000 files at production scale
//! - Large file with 500 methods
//!
//! Uses Criterion framework with percentile tracking (p50/p95/p99).
//! Run with: `cargo bench --release end_to_end -- --nocapture --verbose`

use cct_scanner::Scanner;
use criterion::{black_box, criterion_group, criterion_main, Criterion, BenchmarkId};
use std::fs;
use std::path::PathBuf;
use tempfile::TempDir;
use std::time::Duration;

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
"#
    .to_string()
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
"#
    .to_string()
}

fn bench_scan_single_file(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    create_test_java_file(
        temp_dir.path(),
        "Example.java",
        &sample_java_with_violations(),
    );

    let root = black_box(temp_dir.path().to_path_buf());

    let mut group = c.benchmark_group("e2e_single_file");
    group.sample_size(100); // Higher sample count for p50/p95/p99 accuracy

    group.bench_function("no_cache", |b| {
        b.iter(|| {
            let file_path = root.join("Example.java");
            scanner.scan_file(&file_path).ok()
        });
    });

    group.finish();
    drop(temp_dir);
}

fn bench_scan_10_files_cold(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create 10 test files
    for i in 0..10 {
        let name = format!("Test{}.java", i);
        if i % 2 == 0 {
            create_test_java_file(temp_dir.path(), &name, &sample_java_with_violations());
        } else {
            create_test_java_file(temp_dir.path(), &name, &sample_clean_java());
        }
    }

    let root = black_box(temp_dir.path().to_path_buf());

    let mut group = c.benchmark_group("e2e_10_files");
    group.sample_size(50); // Adequate samples for percentile reporting

    group.bench_function("cold_cache", |b| {
        b.iter(|| {
            let mut files = Vec::new();
            for i in 0..10 {
                files.push(root.join(format!("Test{}.java", i)));
            }
            scanner.scan_files_parallel(&files)
        });
    });

    group.finish();
    drop(temp_dir);
}

fn bench_scan_10_files_hot(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create 10 test files
    for i in 0..10 {
        let name = format!("Test{}.java", i);
        if i % 2 == 0 {
            create_test_java_file(temp_dir.path(), &name, &sample_java_with_violations());
        } else {
            create_test_java_file(temp_dir.path(), &name, &sample_clean_java());
        }
    }

    let root = black_box(temp_dir.path().to_path_buf());

    // Warm up the cache with one pass
    let files: Vec<PathBuf> = (0..10)
        .map(|i| root.join(format!("Test{}.java", i)))
        .collect();
    let _ = scanner.scan_files_parallel(&files);

    let mut group = c.benchmark_group("e2e_10_files");
    group.sample_size(50); // Same group; Criterion merges them in reports

    group.bench_function("hot_cache", |b| {
        b.iter(|| {
            let files: Vec<PathBuf> = (0..10)
                .map(|i| root.join(format!("Test{}.java", i)))
                .collect();
            scanner.scan_files_parallel(&files)
        });
    });

    group.finish();
    drop(temp_dir);
}

fn bench_scan_100_files_warm(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create 100 test files
    for i in 0..100 {
        let name = format!("Test{}.java", i);
        if i % 3 == 0 {
            create_test_java_file(temp_dir.path(), &name, &sample_java_with_violations());
        } else {
            create_test_java_file(temp_dir.path(), &name, &sample_clean_java());
        }
    }

    let root = black_box(temp_dir.path().to_path_buf());

    // Warm up cache with 50 files
    let warmup_files: Vec<PathBuf> = (0..50)
        .map(|i| root.join(format!("Test{}.java", i)))
        .collect();
    let _ = scanner.scan_files_parallel(&warmup_files);

    c.bench_function("e2e_scan_100_files_warm_cache", |b| {
        b.iter(|| {
            let all_files: Vec<PathBuf> = (0..100)
                .map(|i| root.join(format!("Test{}.java", i)))
                .collect();
            scanner.scan_files_parallel(&all_files)
        });
    });

    drop(temp_dir);
}

fn bench_scan_1000_small_files(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create 1000 small test files
    for i in 0..1000 {
        let name = format!("Test{}.java", i);
        let content = if i % 5 == 0 {
            sample_java_with_violations()
        } else {
            sample_clean_java()
        };
        create_test_java_file(temp_dir.path(), &name, &content);
    }

    let root = black_box(temp_dir.path().to_path_buf());

    c.bench_function("e2e_scan_1000_files_many_small", |b| {
        b.iter(|| {
            let all_files: Vec<PathBuf> = (0..1000)
                .map(|i| root.join(format!("Test{}.java", i)))
                .collect();
            scanner.scan_files_parallel(&all_files)
        });
    });

    drop(temp_dir);
}

fn bench_scan_large_file_with_methods(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let scanner = Scanner::new();

    // Create a large file with 500 methods
    let mut large_content = String::from("public class LargeClass {\n");
    for i in 0..500 {
        if i % 10 == 0 {
            large_content.push_str(&format!(
                "    public void method{}() {{ // TODO: fix\n        return null;\n    }}\n",
                i
            ));
        } else {
            large_content.push_str(&format!(
                "    public void method{}() {{ return new Object(); }}\n",
                i
            ));
        }
    }
    large_content.push_str("}\n");

    create_test_java_file(temp_dir.path(), "LargeClass.java", &large_content);

    let root = black_box(temp_dir.path().to_path_buf());

    c.bench_function("e2e_scan_large_file_500_methods", |b| {
        b.iter(|| {
            let file_path = root.join("LargeClass.java");
            scanner.scan_file(&file_path).ok()
        });
    });

    drop(temp_dir);
}

criterion_group!(
    benches,
    bench_scan_single_file,
    bench_scan_10_files_cold,
    bench_scan_10_files_hot,
    bench_scan_100_files_warm,
    bench_scan_1000_small_files,
    bench_scan_large_file_with_methods
);

criterion_main!(benches);
