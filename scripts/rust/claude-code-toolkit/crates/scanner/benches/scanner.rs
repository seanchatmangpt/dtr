use criterion::{black_box, criterion_group, criterion_main, Criterion};
use cct_scanner::{extract_methods, PatternSet, Scanner, walk_java_files};
use std::fs;
use std::path::Path;
use tempfile::TempDir;

// Helper: Create a test Java file
fn create_test_java_file(dir: &Path, name: &str, content: &str) -> std::path::PathBuf {
    let file_path = dir.join(name);
    fs::write(&file_path, content).expect("write test file");
    file_path
}

// Sample Java method with various patterns
fn sample_method() -> String {
    r#"
    public void processData(String input) {
        // TODO: validate input
        if (input == null) {
            return null;  // H_STUB_NULL pattern
        }

        String mockData = "mock";  // H_MOCK prefix

        List<String> items = Collections.emptyList();  // H_STUB_EMPTY_COLLECTION

        // FIXME: add error handling
        for (String item : items) {
            System.out.println(item);
        }
    }
    "#.to_string()
}

// 10KB Java file
fn create_10kb_java_file() -> String {
    let base = r#"
public class TestClass {
    public void method1() { /* TODO */ }
    public void method2() { return null; }
    public void method3() { /* FIXME */ }
    public void method4() { String mock = "data"; }
    public void method5() { List stub = Collections.emptyList(); }
    public void method6() { /* TODO */ }
    public void method7() { return null; }
    public void method8() { /* HACK */ }
    public void method9() { String fake = "x"; }
    public void method10() { /* XXX */ }
"#;

    let method = sample_method();
    let mut content = base.to_string();

    // Repeat the method to reach ~10KB
    for i in 0..30 {
        content.push_str(&format!("    public void repeatedMethod{}() {{\n        {}\n    }}\n", i, &method));
    }
    content.push_str("}\n");
    content
}

fn bench_extract_methods(c: &mut Criterion) {
    let java_content = black_box(create_10kb_java_file());

    c.bench_function("extract_methods_10kb", |b| {
        b.iter(|| {
            extract_methods(java_content.as_bytes())
        });
    });
}

fn bench_pattern_match(c: &mut Criterion) {
    let pattern_set = PatternSet::new();
    let body = black_box(sample_method());

    c.bench_function("pattern_match_7patterns_on_100methods", |b| {
        b.iter(|| {
            for _ in 0..100 {
                pattern_set.match_body(&body);
            }
        });
    });
}

fn bench_walk_files(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");

    // Create 100 test Java files
    for i in 0..100 {
        let content = create_10kb_java_file();
        create_test_java_file(temp_dir.path(), &format!("Test{}.java", i), &content);
    }

    let root_path = black_box(temp_dir.path().to_path_buf());

    c.bench_function("walk_files_100_java_files", |b| {
        b.iter(|| {
            let files = walk_java_files(&root_path, &[]);
            files.len()
        });
    });

    // Keep temp_dir alive
    drop(temp_dir);
}

fn bench_scanner_single_file(c: &mut Criterion) {
    let temp_dir = TempDir::new().expect("create temp dir");
    let file_path = create_test_java_file(temp_dir.path(), "Test.java", &create_10kb_java_file());

    c.bench_function("scanner_single_file_10kb", |b| {
        b.iter(|| {
            let scanner = Scanner::new();
            scanner.scan_file(&file_path).ok()
        });
    });

    drop(temp_dir);
}

criterion_group!(
    benches,
    bench_extract_methods,
    bench_pattern_match,
    bench_walk_files,
    bench_scanner_single_file
);

criterion_main!(benches);
