use criterion::{black_box, criterion_group, criterion_main, Criterion, BenchmarkId};
use cct_cache::hasher::hash_method_body;
use cct_cache::store::{Store, CachedScanResult};
use dashmap::DashMap;
use std::sync::Arc;
use std::time::{SystemTime, UNIX_EPOCH};
use tempfile::TempDir;

fn bench_blake3_hash(c: &mut Criterion) {
    // 1KB method body
    let method_body = black_box(
        b"public void processData(String input) {
            if (input == null) {
                return null;
            }
            String mockData = \"mock\";
            List<String> items = Collections.emptyList();
            for (String item : items) {
                System.out.println(item);
            }
            try {
                // More code to reach 1KB
                for (int i = 0; i < 100; i++) {
                    System.out.println(i);
                    System.out.println(i);
                    System.out.println(i);
                    System.out.println(i);
                    System.out.println(i);
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }".repeat(2).as_bytes()
    );

    c.bench_function("blake3_hash_1kb", |b| {
        b.iter(|| {
            hash_method_body(method_body)
        });
    });
}

fn bench_redb_insert(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("test.redb");
    let store = Store::new(&db_path).expect("create store");

    let mut hash = [0u8; 32];
    hash[0] = 1;

    let result = black_box(CachedScanResult {
        path: "src/test/Example.java".to_string(),
        violation_count: 3,
        cached_at: SystemTime::now()
            .duration_since(UNIX_EPOCH)
            .unwrap()
            .as_secs(),
    });

    c.bench_function("redb_insert_scan_result", |b| {
        let mut counter = 0;
        b.iter(|| {
            counter += 1;
            let mut hash_mut = hash;
            hash_mut[31] = (counter % 256) as u8;
            store.insert(&hash_mut, &result).ok()
        });
    });

    drop(temp_dir);
}

fn bench_redb_lookup(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let db_path = temp_dir.path().join("test.redb");
    let store = Store::new(&db_path).expect("create store");

    // Pre-populate with some values
    for i in 0..100 {
        let mut hash = [0u8; 32];
        hash[0] = i as u8;
        let result = CachedScanResult {
            path: format!("src/test/Test{}.java", i),
            violation_count: i,
            cached_at: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .unwrap()
                .as_secs(),
        };
        store.insert(&hash, &result).ok();
    }

    let mut lookup_hash = [0u8; 32];
    lookup_hash[0] = 42;

    c.bench_function("redb_lookup_by_hash", |b| {
        b.iter(|| {
            store.query(&black_box(lookup_hash)).ok()
        });
    });

    drop(temp_dir);
}

fn bench_concurrent_dashmap(c: &mut Criterion) {
    let map = Arc::new(DashMap::<[u8; 32], String>::new());

    c.bench_function("dashmap_8threads_concurrent_writes_100_ops", |b| {
        b.iter(|| {
            let threads: Vec<_> = (0..8)
                .map(|thread_id| {
                    let map_clone = Arc::clone(&map);
                    std::thread::spawn(move || {
                        for i in 0..100 {
                            let mut key = [0u8; 32];
                            key[0] = thread_id as u8;
                            key[1] = (i % 256) as u8;
                            map_clone.insert(key, format!("value_{}", i));
                        }
                    })
                })
                .collect();

            for t in threads {
                t.join().ok();
            }
        });
    });
}

criterion_group!(
    benches,
    bench_blake3_hash,
    bench_redb_insert,
    bench_redb_lookup,
    bench_concurrent_dashmap
);

criterion_main!(benches);
