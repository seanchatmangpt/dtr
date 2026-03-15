//! Concurrent cache stress test benchmark.
//!
//! Measures cache layer performance under concurrent load:
//! - 10 simultaneous cache writers with varying payloads
//! - Cache contention and lock-free performance
//! - Hit/miss ratio tracking under concurrent access
//! - Latency percentiles (p50/p95/p99) under concurrent pressure
//!
//! Validates write buffer backpressure design and concurrent access patterns.
//! Run with: `cargo bench --release concurrent_cache_stress -- --nocapture --verbose`

use cct_cache::manager::CacheManager;
use criterion::{black_box, criterion_group, criterion_main, Criterion, BenchmarkId};
use std::sync::Arc;
use std::time::Duration;
use tempfile::TempDir;

/// Helper to generate test scan results with varying payload sizes.
fn create_test_result(index: usize, violation_count: usize) -> cct_cache::store::CachedScanResult {
    use std::time::{SystemTime, UNIX_EPOCH};
    let cached_at = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .unwrap()
        .as_secs();
    cct_cache::store::CachedScanResult {
        path: format!("src/test/file_{}.java", index),
        violation_count,
        cached_at,
    }
}

/// Benchmark: 10 concurrent cache writers (parallel inserts).
/// Measures contention on L1 memory cache (DashMap lock-free writes).
fn bench_concurrent_writers(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("concurrent_cache.db");

    let mut group = c.benchmark_group("cache_concurrent_10_writers");
    group.sample_size(50); // 50 samples for percentile accuracy under load
    group.measurement_time(Duration::from_secs(15));

    // Create cache manager (shared across threads)
    let cache_manager = Arc::new(CacheManager::new(&db_path).expect("create cache manager"));

    group.bench_function("parallel_insert_10x", |b| {
        b.iter(|| {
            // Simulate 10 concurrent writers using rayon
            use rayon::prelude::*;

            (0..10)
                .into_par_iter()
                .for_each(|i| {
                    let mgr = Arc::clone(&cache_manager);
                    let method_body = format!("public void method{}() {{ return {}; }}", i, i)
                        .into_bytes();
                    let result = create_test_result(i, i % 5);
                    let _ = mgr.insert(&method_body, &result);
                });

            // Ensure all writes are flushed
            let _ = cache_manager.flush();
        });
    });

    group.finish();
}

/// Benchmark: Mixed read/write workload (50% reads, 50% writes).
/// Measures cache contention with realistic access patterns.
fn bench_mixed_read_write(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("mixed_cache.db");

    let cache_manager = Arc::new(CacheManager::new(&db_path).expect("create cache manager"));

    // Pre-populate cache with 50 entries
    for i in 0..50 {
        let body = format!("public void preload{}() {{ }}", i).into_bytes();
        let result = create_test_result(i, 0);
        let _ = cache_manager.insert(&body, &result);
    }
    let _ = cache_manager.flush();

    let mut group = c.benchmark_group("cache_mixed_rw");
    group.sample_size(40);
    group.measurement_time(Duration::from_secs(20));

    group.bench_function("50_read_50_write_10_threads", |b| {
        b.iter(|| {
            use rayon::prelude::*;

            (0..10)
                .into_par_iter()
                .for_each(|thread_id| {
                    let mgr = Arc::clone(&cache_manager);

                    // Each thread does 5 operations (3 reads, 2 writes)
                    for op in 0..5 {
                        if op % 2 == 0 {
                            // Read operation
                            let read_index = (thread_id * 5 + op) % 50;
                            let body = format!("public void preload{}() {{ }}", read_index)
                                .into_bytes();
                            let _ = mgr.contains(&body);
                        } else {
                            // Write operation
                            let write_index = 50 + thread_id * 5 + op;
                            let body = format!("public void dynamic{}() {{ }}", write_index)
                                .into_bytes();
                            let result = create_test_result(write_index, op % 3);
                            let _ = mgr.insert(&body, &result);
                        }
                    }
                });

            let _ = cache_manager.flush();
        });
    });

    group.finish();
}

/// Benchmark: Cache hit rate under concurrent load.
/// Pre-populate cache, then measure hit ratio with 10 concurrent readers.
fn bench_cache_hit_ratio_concurrent(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("hitrate_cache.db");

    let cache_manager = Arc::new(CacheManager::new(&db_path).expect("create cache manager"));

    // Pre-populate with 100 identical method bodies (high hit rate expected)
    let shared_body = b"public String getName() { return name; }";
    for i in 0..100 {
        let result = create_test_result(i, 0);
        let _ = cache_manager.insert(shared_body, &result);
    }
    let _ = cache_manager.flush();

    let mut group = c.benchmark_group("cache_hit_ratio");
    group.sample_size(60); // Higher samples for accurate percentile reporting
    group.measurement_time(Duration::from_secs(10));

    group.bench_function("100_identical_bodies_10_concurrent", |b| {
        b.iter(|| {
            use rayon::prelude::*;

            let hits = (0..10)
                .into_par_iter()
                .map(|_| {
                    let mgr = Arc::clone(&cache_manager);
                    let body = black_box(shared_body);

                    // Each thread queries the same body 10 times
                    let mut count = 0;
                    for _ in 0..10 {
                        if let Ok(Some(_)) = mgr.query(body) {
                            count += 1;
                        }
                    }
                    count
                })
                .sum::<usize>();

            eprintln!("  Hit ratio: {}/100 = {:.1}%", hits, (hits as f64 / 100.0) * 100.0);
        });
    });

    group.finish();
}

/// Benchmark: Write buffer saturation test.
/// Measures latency as write buffer approaches auto-flush threshold (100 entries).
fn bench_write_buffer_saturation(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("saturation_cache.db");

    let cache_manager = Arc::new(CacheManager::new(&db_path).expect("create cache manager"));

    let mut group = c.benchmark_group("cache_write_buffer");
    group.sample_size(30);
    group.measurement_time(Duration::from_secs(15));

    for buffer_fill_pct in [25, 50, 75, 100].iter() {
        let id = format!("{}pct_full", buffer_fill_pct);

        group.bench_with_input(BenchmarkId::from_parameter(&id), buffer_fill_pct, |b, fill| {
            b.iter(|| {
                // Pre-fill buffer to desired level (approximate)
                let fill_count = (fill * 100) / 100;
                for i in 0..fill_count {
                    let body = format!("public void fill{}() {{ }}", i).into_bytes();
                    let result = create_test_result(i, 0);
                    let _ = cache_manager.insert(&body, &result);
                }

                // Measure latency of insert at this fill level
                let body = b"public void measure() { }".to_vec();
                let result = create_test_result(9999, 1);
                let start = std::time::Instant::now();
                let _ = cache_manager.insert(&body, &result);
                let _elapsed = start.elapsed();

                let _ = cache_manager.flush();
            });
        });
    }

    group.finish();
}

criterion_group!(
    benches,
    bench_concurrent_writers,
    bench_mixed_read_write,
    bench_cache_hit_ratio_concurrent,
    bench_write_buffer_saturation
);

criterion_main!(benches);
