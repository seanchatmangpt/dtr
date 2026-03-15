use criterion::{black_box, criterion_group, criterion_main, Criterion, Throughput};
use cct_cache::hasher::hash_method_body;
use cct_cache::manager::CacheManager;
use cct_cache::store::CachedScanResult;
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};
use tempfile::TempDir;

fn bench_blake3_hash_micro(c: &mut Criterion) {
    let mut group = c.benchmark_group("blake3_hash");
    group.measurement_time(std::time::Duration::from_secs(30));

    // Small method (128 bytes) — target <5µs
    let small_body = black_box(b"public void foo() { return null; }");
    group.throughput(Throughput::Bytes(small_body.len() as u64));
    group.bench_function("method_128b", |b| {
        b.iter(|| hash_method_body(small_body))
    });

    // Medium method (512 bytes) — target <8µs
    let medium_body = black_box(
        b"public void processData(String input) {
            if (input == null) return null;
            List<String> items = new ArrayList<>();
            for (String item : items) {
                System.out.println(item);
            }
        }".repeat(4).as_bytes()
    );
    group.throughput(Throughput::Bytes(medium_body.len() as u64));
    group.bench_function("method_512b", |b| {
        b.iter(|| hash_method_body(medium_body))
    });

    // Large method (1KB+) — target <10µs
    let large_body = black_box(
        b"public void largeMethod() {
            for (int i = 0; i < 1000; i++) {
                System.out.println(i);
            }
        }".repeat(8).as_bytes()
    );
    group.throughput(Throughput::Bytes(large_body.len() as u64));
    group.bench_function("method_1kb", |b| {
        b.iter(|| hash_method_body(large_body))
    });

    group.finish();
}

fn bench_cache_manager_l1_hit(c: &mut Criterion) {
    let mut group = c.benchmark_group("cache_l1_hit");
    group.measurement_time(std::time::Duration::from_secs(30));

    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("l1_hit.db");
    let manager = CacheManager::new(&db_path).expect("create manager");

    let body = black_box(b"public void cached() { }");
    let result = CachedScanResult {
        path: "Test.java".to_string(),
        violation_count: 0,
        cached_at: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs(),
    };

    manager.insert(body, &result).expect("insert");

    // L1 cache hit: target <2µs
    group.throughput(Throughput::Bytes(body.len() as u64));
    group.bench_function("l1_hit_memory_cache", |b| {
        b.iter(|| manager.query(black_box(body)))
    });

    drop(temp_dir);
}

fn bench_cache_manager_l2_miss(c: &mut Criterion) {
    let mut group = c.benchmark_group("cache_l2_miss");
    group.measurement_time(std::time::Duration::from_secs(30));

    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("l2_miss.db");
    let manager = CacheManager::new(&db_path).expect("create manager");

    // Pre-populate L2 with 100 entries
    for i in 0..100 {
        let body = format!("public void method{}() {{ }}", i).into_bytes();
        let result = CachedScanResult {
            path: format!("File{}.java", i),
            violation_count: i % 5,
            cached_at: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs(),
        };
        manager.insert(&body, &result).expect("insert");
    }

    // Flush batch writes to ensure L2 persistence
    manager.flush().expect("flush");

    // Create new manager instance to clear L1 cache
    let manager2 = CacheManager::new(&db_path).expect("create manager");
    let lookup_body = b"public void method50() { }";

    // L2 miss (promotes to L1): target <50µs
    group.throughput(Throughput::Bytes(lookup_body.len() as u64));
    group.bench_function("l2_miss_disk_promotion", |b| {
        b.iter(|| manager2.query(black_box(lookup_body)))
    });

    drop(temp_dir);
}

fn bench_cache_manager_insert_l1(c: &mut Criterion) {
    let mut group = c.benchmark_group("cache_insert");
    group.measurement_time(std::time::Duration::from_secs(30));

    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("insert.db");
    let manager = Arc::new(CacheManager::new(&db_path).expect("create manager"));

    let result = CachedScanResult {
        path: "Insert.java".to_string(),
        violation_count: 1,
        cached_at: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs(),
    };

    let mut counter = 0;

    // Single-threaded L1 insert: target <5µs
    group.throughput(Throughput::Bytes(32)); // hash size
    group.bench_function("insert_l1_buffered", |b| {
        b.iter(|| {
            counter += 1;
            let body = format!("public void method{}() {{ }}", counter).into_bytes();
            manager.insert(black_box(&body), &result)
        })
    });

    drop(temp_dir);
}

fn bench_concurrent_writes(c: &mut Criterion) {
    let mut group = c.benchmark_group("concurrent_writes");
    group.measurement_time(std::time::Duration::from_secs(30));

    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("concurrent.db");
    let manager = Arc::new(CacheManager::new(&db_path).expect("create manager"));

    let result = CachedScanResult {
        path: "Concurrent.java".to_string(),
        violation_count: 2,
        cached_at: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs(),
    };

    // 32-thread concurrent writes: target <100µs total
    group.throughput(Throughput::Elements(32 * 10)); // 32 threads × 10 ops each
    group.bench_function("concurrent_32threads_10ops_each", |b| {
        b.iter(|| {
            use rayon::prelude::*;

            (0..32).into_par_iter().for_each(|thread_id| {
                for i in 0..10 {
                    let body = format!("public void t{}m{}() {{ }}", thread_id, i).into_bytes();
                    let _ = manager.insert(&body, &result);
                }
            });
        })
    });

    drop(temp_dir);
}

criterion_group!(
    benches,
    bench_blake3_hash_micro,
    bench_cache_manager_l1_hit,
    bench_cache_manager_l2_miss,
    bench_cache_manager_insert_l1,
    bench_concurrent_writes
);

criterion_main!(benches);
