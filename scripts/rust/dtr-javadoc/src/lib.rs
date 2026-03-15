//! DTR Javadoc Extraction and Validation
//!
//! This crate provides tools for parsing Java source files, extracting Javadoc
//! comments, and enforcing TPS quality rules via violation detection.
//!
//! # Modules
//!
//! - **`model`**: Data types for Javadoc entries, module documentation, and results
//! - **`error`**: Violation types for TPS enforcement
//! - **`parser`**: Javadoc comment parsing via tree-sitter-javadoc
//! - **`extractor`**: Java source parsing and documentation extraction
//! - **`validator`**: TPS Jidoka violation detection
//! - **`util`**: Helper functions for Java source processing
//! - **`render`**: Markdown generation and output
//!
//! # Quick Start
//!
//! Extract documentation from a directory:
//!
//! ```ignore
//! let (method_docs, module_docs, violations) = process_all(&Path::new("/path/to/src"));
//! if !violations.is_empty() {
//!     eprintln!("Documentation violations found:");
//!     for v in violations {
//!         eprintln!("{v}");
//!     }
//! }
//! ```

pub mod error;
pub mod extractor;
pub mod model;
pub mod parser;
pub mod render;
pub mod util;
pub mod validator;

// ============================================================================
// Public API Re-exports
// ============================================================================

// Data model types
pub use model::{FileDocResult, JavadocEntry, ModuleDoc, ParamDoc, ThrowsDoc};

// Error/violation types
pub use error::{DocViolation, ViolationKind};

// Extraction functions
pub use extractor::{
    extract_all, extract_from_file, extract_from_source, extract_type_signature, process_all,
    process_file_source, JAVA_CLASS_QUERY, JAVA_METHOD_QUERY,
};

// Parser functions
pub use parser::{child_text_by_kind, parse_javadoc_comment};

// Validator functions
pub use validator::{find_violations, has_javadoc_predecessor, has_override_annotation, is_public};

// Utility functions
pub use util::{class_name_from_path, clean_comment_text, derive_package};

// Rendering functions
pub use render::{render_module_markdown, write_api_docs};

// ============================================================================
// Tests
// ============================================================================

#[cfg(test)]
mod tests {
    use super::*;
    use std::path::Path;

    // -----------------------------------------------------------------------
    // clean_comment_text
    // -----------------------------------------------------------------------

    #[test]
    fn clean_simple_description() {
        let input = "A text that will be rendered as a paragraph.";
        assert_eq!(
            clean_comment_text(input),
            "A text that will be rendered as a paragraph."
        );
    }

    #[test]
    fn clean_multiline_with_star_prefix() {
        let input = "* First line.\n* Second line.";
        assert_eq!(clean_comment_text(input), "First line. Second line.");
    }

    #[test]
    fn clean_strips_javadoc_delimiters() {
        let input = "/**\n * Body.\n */";
        assert_eq!(clean_comment_text(input), "Body.");
    }

    #[test]
    fn clean_blank_star_lines_dropped() {
        let input = "First.\n*\nSecond.";
        assert_eq!(clean_comment_text(input), "First. Second.");
    }

    #[test]
    fn clean_empty_string() {
        assert_eq!(clean_comment_text(""), "");
    }

    #[test]
    fn clean_only_whitespace() {
        assert_eq!(clean_comment_text("   \n  \n  "), "");
    }

    #[test]
    fn clean_inline_tags_preserved() {
        let input = "* Use {@code System.nanoTime()} for timing.";
        let result = clean_comment_text(input);
        assert!(
            result.contains("{@code System.nanoTime()}"),
            "got: {result}"
        );
    }

    #[test]
    fn clean_strips_license_header() {
        let license = "Licensed under the Apache License, Version 2.0 (the \"License\")";
        assert_eq!(clean_comment_text(license), "");
    }

    #[test]
    fn clean_strips_copyright() {
        let copyright = "Copyright (C) 2013 the original author or authors.";
        assert_eq!(clean_comment_text(copyright), "");
    }

    // -----------------------------------------------------------------------
    // derive_package
    // -----------------------------------------------------------------------

    #[test]
    fn package_normal() {
        let src = b"package io.github.seanchatmangpt.dtr.rendermachine;\n\npublic interface Foo {}";
        assert_eq!(
            derive_package(src),
            "io.github.seanchatmangpt.dtr.rendermachine"
        );
    }

    #[test]
    fn package_with_leading_whitespace() {
        let src = b"  package com.example.util;\n\npublic class Bar {}";
        assert_eq!(derive_package(src), "com.example.util");
    }

    #[test]
    fn package_default_package() {
        let src = b"public class Bare {}";
        assert_eq!(derive_package(src), "");
    }

    #[test]
    fn package_after_license_comment() {
        let src = b"/* Apache License */\n\npackage org.example;\n\nclass X {}";
        assert_eq!(derive_package(src), "org.example");
    }

    #[test]
    fn package_ignores_partial_match() {
        let src = b"// packages are cool\npublic class X {}";
        assert_eq!(derive_package(src), "");
    }

    // -----------------------------------------------------------------------
    // class_name_from_path
    // -----------------------------------------------------------------------

    #[test]
    fn class_name_simple() {
        assert_eq!(
            class_name_from_path(Path::new("src/RenderMachine.java")),
            "RenderMachine"
        );
    }

    #[test]
    fn class_name_deep_path() {
        assert_eq!(
            class_name_from_path(Path::new(
                "/home/user/dtr/dtr-core/src/main/java/io/github/Foo.java"
            )),
            "Foo"
        );
    }

    // -----------------------------------------------------------------------
    // parse_javadoc_comment
    // -----------------------------------------------------------------------

    #[test]
    fn parse_description_only() {
        let comment = "/**\n * Renders a paragraph of text.\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.description, "Renders a paragraph of text.");
        assert!(entry.params.is_empty());
        assert!(entry.returns.is_none());
    }

    #[test]
    fn parse_multiline_description() {
        let comment = "/**\n * First sentence.\n * Second sentence.\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(
            entry.description.contains("First sentence."),
            "got: {}",
            entry.description
        );
        assert!(
            entry.description.contains("Second sentence."),
            "got: {}",
            entry.description
        );
    }

    #[test]
    fn parse_single_param() {
        let comment = "/**\n * Say something.\n *\n * @param text the text to say\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.params.len(), 1);
        assert_eq!(entry.params[0].name, "text");
        assert_eq!(entry.params[0].description, "the text to say");
    }

    #[test]
    fn parse_multiple_params() {
        let comment = r"/**
     * Benchmark.
     *
     * @param label  label
     * @param task   task
     * @param warmupRounds warmup
     * @param measureRounds measure
     */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.params.len(), 4);
        assert_eq!(entry.params[0].name, "label");
        assert_eq!(entry.params[2].name, "warmupRounds");
    }

    #[test]
    fn parse_return_tag() {
        let comment = "/**\n * Gets the name.\n *\n * @return the name string\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.returns.as_deref(), Some("the name string"));
    }

    #[test]
    fn parse_throws_tag() {
        let comment = r"/**
     * Risky operation.
     *
     * @throws IllegalStateException if not ready
     */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.throws.len(), 1);
        assert_eq!(entry.throws[0].exception, "IllegalStateException");
        assert!(
            entry.throws[0].description.contains("if not ready"),
            "got: {}",
            entry.throws[0].description
        );
    }

    #[test]
    fn parse_since_tag() {
        let comment = "/**\n * Some feature.\n *\n * @since 1.0.0\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert_eq!(entry.since.as_deref(), Some("1.0.0"));
    }

    #[test]
    fn parse_deprecated_tag() {
        let comment = "/**\n * Old API.\n *\n * @deprecated use newMethod() instead\n */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(entry.deprecated.is_some());
        assert!(entry.deprecated.as_deref().unwrap().contains("newMethod"));
    }

    #[test]
    fn parse_all_tags_combined() {
        let comment = r"/**
     * Full-featured method.
     *
     * @param input the input value
     * @return the transformed value
     * @throws NullPointerException if input is null
     * @since 2.0.0
     * @deprecated use betterMethod() instead
     */";
        let entry = parse_javadoc_comment(comment).expect("should parse");
        assert!(!entry.description.is_empty());
        assert_eq!(entry.params.len(), 1);
        assert!(entry.returns.is_some());
        assert_eq!(entry.throws.len(), 1);
        assert!(entry.since.is_some());
        assert!(entry.deprecated.is_some());
    }

    #[test]
    fn parse_empty_comment_returns_none() {
        assert!(parse_javadoc_comment("/** */").is_none());
    }

    #[test]
    fn parse_non_javadoc_comment_returns_none() {
        assert!(parse_javadoc_comment("/* not a javadoc */").is_none());
    }

    #[test]
    fn parse_only_see_tag_returns_none() {
        assert!(parse_javadoc_comment("/**\n * @see SomeOtherClass\n */").is_none());
    }

    // -----------------------------------------------------------------------
    // extract_from_source (method docs)
    // -----------------------------------------------------------------------

    const SIMPLE_JAVA: &[u8] = br#"
package com.example;

public class MyService {

    /**
     * Greets someone.
     *
     * @param name the name of the person
     * @return a greeting
     */
    public String greet(String name) {
        return "Hello, " + name;
    }

    /**
     * Constructs a new service.
     *
     * @param config configuration object
     */
    public MyService(Config config) {
    }

    /* Not a Javadoc comment - should be ignored */
    public void notDocumented() {
    }
}
"#;

    #[test]
    fn extract_finds_method_and_constructor() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        assert_eq!(entries.len(), 2);
        let keys: Vec<&str> = entries.iter().map(|(k, _)| k.as_str()).collect();
        assert!(keys.contains(&"com.example.MyService#greet"));
        assert!(keys.contains(&"com.example.MyService#MyService"));
    }

    #[test]
    fn extract_method_has_correct_data() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        let greet = entries
            .iter()
            .find(|(k, _)| k.ends_with("#greet"))
            .map(|(_, v)| v)
            .expect("greet not found");
        assert!(greet.description.contains("Greets"));
        assert_eq!(greet.params.len(), 1);
        assert_eq!(greet.params[0].name, "name");
        assert_eq!(greet.returns.as_deref(), Some("a greeting"));
    }

    #[test]
    fn extract_ignores_non_javadoc_comment() {
        let entries = extract_from_source(SIMPLE_JAVA, Path::new("MyService.java"));
        let keys: Vec<&str> = entries.iter().map(|(k, _)| k.as_str()).collect();
        assert!(!keys.contains(&"com.example.MyService#notDocumented"));
    }

    #[test]
    fn extract_default_package() {
        let src = b"public class Bare {\n    /** Says hi. */\n    public void hi() {}\n}";
        let entries = extract_from_source(src, Path::new("Bare.java"));
        assert_eq!(entries.len(), 1);
        assert_eq!(entries[0].0, "Bare#hi");
    }

    #[test]
    fn extract_empty_source_returns_empty() {
        let entries = extract_from_source(b"", Path::new("Empty.java"));
        assert!(entries.is_empty());
    }

    #[test]
    fn extract_invalid_utf8_is_skipped() {
        let bad_bytes = b"\xff\xfe public class X {}";
        let _ = extract_from_source(bad_bytes, Path::new("X.java"));
    }

    // -----------------------------------------------------------------------
    // Module doc extraction
    // -----------------------------------------------------------------------

    #[test]
    fn module_doc_extracted_for_interface() {
        let src = br"
package com.example;

/**
 * The core documentation API.
 *
 * @since 1.0
 */
public interface MyApi {
    /** Does something. @param x input */
    void doThing(String x);
}
";
        let result = process_file_source(src, Path::new("MyApi.java"));
        let module = result.module_doc.expect("should have module doc");
        assert_eq!(module.simple_name, "MyApi");
        assert_eq!(module.package, "com.example");
        assert_eq!(module.kind, "interface");
        assert!(
            module.description.contains("core documentation API"),
            "desc: {}",
            module.description
        );
        assert_eq!(module.since.as_deref(), Some("1.0"));
        assert!(
            module.signature.contains("interface MyApi"),
            "sig: {}",
            module.signature
        );
    }

    #[test]
    fn module_doc_missing_returns_none() {
        let src = br"
package com.example;

public interface NoDoc {
    void doThing();
}
";
        let result = process_file_source(src, Path::new("NoDoc.java"));
        assert!(result.module_doc.is_none());
    }

    #[test]
    fn module_doc_signature_strips_body() {
        let src = br"
package com.example;

/**
 * A record type.
 */
public record Point(int x, int y) {
    /** Gets distance. @return distance */
    public double distance() { return Math.sqrt(x*x + y*y); }
}
";
        let result = process_file_source(src, Path::new("Point.java"));
        let module = result.module_doc.expect("should have module doc");
        assert!(
            module.signature.contains("record Point"),
            "sig: {}",
            module.signature
        );
        // Body should NOT be in signature
        assert!(
            !module.signature.contains("distance"),
            "sig should not have body: {}",
            module.signature
        );
    }

    // -----------------------------------------------------------------------
    // Violation detection (TPS Jidoka)
    // -----------------------------------------------------------------------

    #[test]
    fn violation_missing_class_doc() {
        let src = br"
package com.example;

public class NoDocs {
    public void doThing() {}
}
";
        let result = process_file_source(src, Path::new("NoDocs.java"));
        let class_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingClassDoc))
            .collect();
        assert_eq!(class_violations.len(), 1, "expected 1 class violation");
    }

    #[test]
    fn violation_missing_method_doc() {
        let src = br"
package com.example;

/**
 * Well-documented class.
 */
public class WellDocs {
    /** Has docs. */
    public void documented() {}

    public void notDocumented() {}
}
";
        let result = process_file_source(src, Path::new("WellDocs.java"));
        let method_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert_eq!(method_violations.len(), 1, "expected 1 method violation");
        if let ViolationKind::MissingMethodDoc { method } = &method_violations[0].kind {
            assert_eq!(method, "notDocumented");
        }
    }

    #[test]
    fn no_violation_for_override_methods() {
        let src = br"
package com.example;

/**
 * Implementation.
 */
public class Impl implements Runnable {
    @Override
    public void run() {}
}
";
        let result = process_file_source(src, Path::new("Impl.java"));
        let method_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert!(
            method_violations.is_empty(),
            "Override methods should not be violations: {method_violations:?}"
        );
    }

    #[test]
    fn no_violation_for_private_methods() {
        let src = br"
package com.example;

/**
 * Has private stuff.
 */
public class Private {
    private void hidden() {}
    protected void alsoHidden() {}
}
";
        let result = process_file_source(src, Path::new("Private.java"));
        let violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert!(
            violations.is_empty(),
            "private/protected should not be violations"
        );
    }

    #[test]
    fn fully_documented_class_has_no_violations() {
        let result = process_file_source(SIMPLE_JAVA, Path::new("MyService.java"));
        // SIMPLE_JAVA has notDocumented() without docs — should be a violation
        let method_violations: Vec<_> = result
            .violations
            .iter()
            .filter(|v| matches!(v.kind, ViolationKind::MissingMethodDoc { .. }))
            .collect();
        assert_eq!(method_violations.len(), 1);
    }

    // -----------------------------------------------------------------------
    // Markdown rendering
    // -----------------------------------------------------------------------

    #[test]
    fn markdown_contains_class_name() {
        let module = ModuleDoc {
            fqcn: "com.example.MyApi".to_string(),
            simple_name: "MyApi".to_string(),
            package: "com.example".to_string(),
            description: "The core API.".to_string(),
            signature: "public interface MyApi".to_string(),
            kind: "interface".to_string(),
            method_count: 1,
            method_names: vec!["doThing".to_string()],
            ..Default::default()
        };
        let methods = std::collections::HashMap::new();
        let md = render_module_markdown(&module, &methods);
        assert!(md.contains("# `MyApi`"), "md: {md}");
        assert!(md.contains("The core API."), "md: {md}");
        assert!(md.contains("```java"), "md: {md}");
        assert!(md.contains("public interface MyApi {"), "md: {md}");
        assert!(md.contains("doThing"), "md: {md}");
    }

    #[test]
    fn markdown_includes_method_docs() {
        let module = ModuleDoc {
            fqcn: "com.example.MyApi".to_string(),
            simple_name: "MyApi".to_string(),
            package: "com.example".to_string(),
            description: "The core API.".to_string(),
            signature: "public interface MyApi".to_string(),
            kind: "interface".to_string(),
            method_count: 1,
            method_names: vec!["doThing".to_string()],
            ..Default::default()
        };
        let mut methods = std::collections::HashMap::new();
        methods.insert(
            "com.example.MyApi#doThing".to_string(),
            JavadocEntry {
                description: "Does a thing.".to_string(),
                params: vec![ParamDoc {
                    name: "x".to_string(),
                    description: "the input".to_string(),
                }],
                returns: Some("result".to_string()),
                ..Default::default()
            },
        );
        let md = render_module_markdown(&module, &methods);
        assert!(md.contains("### `doThing`"), "md: {md}");
        assert!(md.contains("Does a thing."), "md: {md}");
        assert!(md.contains("| `x` |"), "md: {md}");
        assert!(md.contains("**Returns:**"), "md: {md}");
    }

    // -----------------------------------------------------------------------
    // Integration: full DTR source tree
    // -----------------------------------------------------------------------

    #[test]
    fn integration_dtr_core_zero_violations() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            eprintln!("Skipping integration test: {source_dir:?} not found");
            return;
        }

        let (_, _, violations) = process_all(&source_dir);

        if !violations.is_empty() {
            let msg: String = violations.iter().map(|v| format!("\n{v}")).collect();
            panic!(
                "\n\nTPS VIOLATION: {} missing doc(s) found in dtr-core:\n{}\n",
                violations.len(),
                msg
            );
        }
    }

    #[test]
    fn integration_dtr_core_produces_module_docs() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            eprintln!("Skipping integration test: {source_dir:?} not found");
            return;
        }

        let (method_docs, module_docs, _) = process_all(&source_dir);

        assert!(
            module_docs.len() > 30,
            "expected >30 module docs, got {}",
            module_docs.len()
        );

        let rmc = module_docs
            .iter()
            .find(|m| m.simple_name == "RenderMachineCommands")
            .expect("RenderMachineCommands module doc missing");
        assert!(
            rmc.description.contains("DTR") || rmc.description.contains("documentation"),
            "desc: {}",
            rmc.description
        );
        assert!(
            rmc.signature.contains("interface RenderMachineCommands"),
            "sig: {}",
            rmc.signature
        );
        assert!(rmc.method_count > 0);

        // Spot-check markdown output
        let md = render_module_markdown(rmc, &method_docs);
        assert!(
            md.contains("# `RenderMachineCommands`"),
            "md header missing"
        );
        assert!(md.contains("```java"), "code block missing");
        assert!(md.contains("### `say`"), "method section missing");
    }

    #[test]
    fn integration_extract_all_backward_compat() {
        let source_dir = Path::new(env!("CARGO_MANIFEST_DIR"))
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .parent()
            .unwrap()
            .join("dtr-core/src/main/java");

        if !source_dir.exists() {
            eprintln!("Skipping integration test: {source_dir:?} not found");
            return;
        }

        let extracted = extract_all(&source_dir);
        assert!(
            extracted.len() > 50,
            "expected >50 method docs, got {}",
            extracted.len()
        );
    }
}
