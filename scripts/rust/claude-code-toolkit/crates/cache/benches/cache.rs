use cct_cache::hasher::hash_method_body;
use criterion::{black_box, criterion_group, criterion_main, Criterion, Throughput};

fn bench_blake3_hash_micro(c: &mut Criterion) {
    let mut group = c.benchmark_group("blake3_hash");
    group.measurement_time(std::time::Duration::from_secs(30));

    // Small method (128 bytes)
    let small_body = black_box(b"public void foo() { return null; }");
    group.throughput(Throughput::Bytes(small_body.len() as u64));
    group.bench_function("method_128b", |b| b.iter(|| hash_method_body(small_body)));

    // Medium method (512 bytes)
    let medium_src = "public void processData(String input) {
            if (input == null) return null;
            List<String> items = new ArrayList<>();
            for (String item : items) {
                System.out.println(item);
            }
        }".repeat(4);
    let medium_body = black_box(medium_src.as_bytes());
    group.throughput(Throughput::Bytes(medium_body.len() as u64));
    group.bench_function("method_512b", |b| b.iter(|| hash_method_body(medium_body)));

    // Large method (1KB+)
    let large_src = "public void largeMethod() {
            for (int i = 0; i < 1000; i++) {
                System.out.println(i);
            }
        }".repeat(8);
    let large_body = black_box(large_src.as_bytes());
    group.throughput(Throughput::Bytes(large_body.len() as u64));
    group.bench_function("method_1kb", |b| b.iter(|| hash_method_body(large_body)));

    group.finish();
}

criterion_group!(benches, bench_blake3_hash_micro);

criterion_main!(benches);
