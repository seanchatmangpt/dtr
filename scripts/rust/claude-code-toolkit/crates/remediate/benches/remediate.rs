use cct_remediate::{apply_edits, atomic_write, Edit, RemediationPlan};
use criterion::{black_box, criterion_group, criterion_main, BenchmarkId, Criterion};
use tempfile::TempDir;

fn sample_java_source() -> Vec<u8> {
    let content = r#"public class Example {
    public void method1() {
        // TODO: implement this
        return null;
    }

    public void method2() {
        String mockData = "data";
        System.out.println(mockData);
    }

    public void method3() {
        List<String> items = Collections.emptyList();
        for (String item : items) {
            System.out.println(item);
        }
    }
}
"#;
    content.as_bytes().to_vec()
}

// Create a 10KB source file
fn large_java_source() -> Vec<u8> {
    let base = sample_java_source();
    let mut result = Vec::new();
    for _ in 0..20 {
        result.extend_from_slice(&base);
    }
    result
}

fn bench_apply_single_edit(c: &mut Criterion) {
    let source = black_box(sample_java_source());

    c.bench_function("apply_single_edit_5byte_replacement", |b| {
        b.iter(|| {
            let mut plan = RemediationPlan::new("/tmp/test.java");
            plan.add_edit(Edit::new(10, 15, "fixed"));
            plan.sort_edits();
            apply_edits(&source, &plan).ok()
        });
    });
}

fn bench_apply_10_edits(c: &mut Criterion) {
    let source = black_box(sample_java_source());

    c.bench_function("apply_10_edits_sequential", |b| {
        b.iter(|| {
            let mut plan = RemediationPlan::new("/tmp/test.java");
            for i in 0..10 {
                let start = i * 10;
                let end = start + 5;
                if end < source.len() {
                    plan.add_edit(Edit::new(start, end, "fix"));
                }
            }
            plan.sort_edits();
            apply_edits(&source, &plan).ok()
        });
    });
}

fn bench_atomic_write(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");

    // 100KB file
    let large_data = vec![b'x'; 100 * 1024];
    let data = black_box(large_data);

    c.bench_function("atomic_write_100kb_file", |b| {
        let mut counter = 0;
        b.iter(|| {
            counter += 1;
            let path = temp_dir.path().join(format!("output_{}.java", counter));
            atomic_write(&path, &data).ok()
        });
    });

    drop(temp_dir);
}

fn bench_apply_edits_with_diff(c: &mut Criterion) {
    let source = black_box(large_java_source()); // 10KB

    c.bench_function("apply_edits_large_file_with_diff", |b| {
        b.iter(|| {
            let mut plan = RemediationPlan::new("/tmp/test.java");
            // Apply 5 edits across the file
            for i in 0..5 {
                let start = (i * source.len()) / 5;
                let end = start + 10;
                if end < source.len() {
                    plan.add_edit(Edit::new(start, end, "FIXED"));
                }
            }
            plan.sort_edits();
            apply_edits(&source, &plan).ok()
        });
    });
}

// Scaling benchmark: test edit count from 1 → 10 → 100
fn bench_scaling_edits(c: &mut Criterion) {
    let source = black_box(large_java_source()); // 10KB

    let mut group = c.benchmark_group("edit_scaling");
    for edit_count in [1, 10, 100].iter() {
        group.bench_with_input(
            BenchmarkId::from_parameter(format!("{}_edits", edit_count)),
            edit_count,
            |b, &count| {
                b.iter(|| {
                    let mut plan = RemediationPlan::new("/tmp/test.java");
                    for i in 0..count {
                        let start = (i * (source.len() / count)).saturating_sub(1);
                        let end = (start + 5).min(source.len());
                        if start < end {
                            plan.add_edit(Edit::new(start, end, "X"));
                        }
                    }
                    plan.sort_edits();
                    apply_edits(&source, &plan).ok()
                });
            },
        );
    }
    group.finish();
}

// Batch write benchmark: test 10KB, 100KB, 1MB files
fn bench_atomic_write_scaling(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");

    let mut group = c.benchmark_group("atomic_write_scaling");
    for size_kb in [10, 100, 1024].iter() {
        let data = vec![b'x'; size_kb * 1024];
        let data = black_box(data);

        group.bench_with_input(
            BenchmarkId::from_parameter(format!("{}kb", size_kb)),
            size_kb,
            |b, &_size| {
                let mut counter = 0;
                b.iter(|| {
                    counter += 1;
                    let path = temp_dir.path().join(format!("file_{}.bin", counter));
                    atomic_write(&path, &data).ok()
                });
            },
        );
    }
    group.finish();
    drop(temp_dir);
}

// Diff generation benchmark on large file
fn bench_diff_generation(c: &mut Criterion) {
    // Create a 100KB source file
    let mut large_source = Vec::new();
    for _ in 0..50 {
        large_source.extend_from_slice(&large_java_source());
    }
    let source = black_box(large_source);

    c.bench_function("diff_generation_100kb_file", |b| {
        b.iter(|| {
            let mut plan = RemediationPlan::new("/tmp/test.java");
            // Apply sparse edits (5 across 100KB)
            for i in 0..5 {
                let start = (i * source.len()) / 5;
                let end = (start + 20).min(source.len());
                if start < end {
                    plan.add_edit(Edit::new(start, end, "OPTIMIZED"));
                }
            }
            plan.sort_edits();
            apply_edits(&source, &plan).ok()
        });
    });
}

criterion_group!(
    benches,
    bench_apply_single_edit,
    bench_apply_10_edits,
    bench_atomic_write,
    bench_apply_edits_with_diff,
    bench_scaling_edits,
    bench_atomic_write_scaling,
    bench_diff_generation
);

criterion_main!(benches);
