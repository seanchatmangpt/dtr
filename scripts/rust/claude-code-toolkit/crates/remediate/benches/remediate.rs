use criterion::{black_box, criterion_group, criterion_main, Criterion, BenchmarkId};
use cct_remediate::{Edit, RemediationPlan, apply_edits, atomic_write};
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

criterion_group!(
    benches,
    bench_apply_single_edit,
    bench_apply_10_edits,
    bench_atomic_write,
    bench_apply_edits_with_diff
);

criterion_main!(benches);
