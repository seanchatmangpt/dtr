use clap::Parser;
use std::path::PathBuf;

use dtr_javadoc::{process_all, write_api_docs};

#[derive(Parser)]
#[command(about = "Extract Javadoc from Java source files into JSON (TPS: fails on missing docs)")]
struct Args {
    /// Source directory to scan (e.g. src/main/java or dtr-core/src/main/java)
    #[arg(short, long, default_value = "src/main/java")]
    source: PathBuf,

    /// Output JSON file path
    #[arg(short, long, default_value = "docs/meta/javadoc.json")]
    output: PathBuf,

    /// Output directory for per-module API docs (markdown)
    #[arg(long, default_value = "docs/api")]
    docs: PathBuf,

    /// Skip TPS violation check (allow missing Javadoc without failing the build)
    #[arg(long, default_value_t = false)]
    allow_missing_docs: bool,
}

fn main() -> anyhow::Result<()> {
    let args = Args::parse();

    eprintln!("dtr-javadoc: scanning {}", args.source.display());

    let (method_docs, module_docs, violations) = process_all(&args.source);

    // ── TPS / Jidoka: stop the line on missing documentation ─────────────────
    if !violations.is_empty() {
        eprintln!();
        eprintln!("╔══════════════════════════════════════════════════════════════════╗");
        eprintln!("║  TPS VIOLATION — MISSING JAVADOC (stop the line / Jidoka)       ║");
        eprintln!("╠══════════════════════════════════════════════════════════════════╣");
        for v in &violations {
            eprintln!("{v}");
        }
        eprintln!("╠══════════════════════════════════════════════════════════════════╣");
        eprintln!(
            "║  {} violation(s) found. Add /** ... */ Javadoc to fix.{}║",
            violations.len(),
            " ".repeat(19usize.saturating_sub(violations.len().to_string().len()))
        );
        eprintln!("╚══════════════════════════════════════════════════════════════════╝");
        eprintln!();

        if !args.allow_missing_docs {
            std::process::exit(1);
        }

        eprintln!("dtr-javadoc: --allow-missing-docs set, continuing despite violations");
    }

    // ── Write method-level JSON index ─────────────────────────────────────────
    if let Some(parent) = args.output.parent() {
        std::fs::create_dir_all(parent)?;
    }
    let json = serde_json::to_string_pretty(&method_docs)?;
    std::fs::write(&args.output, &json)?;

    eprintln!(
        "dtr-javadoc: extracted {} method entries → {}",
        method_docs.len(),
        args.output.display()
    );

    // ── Write per-module API docs ──────────────────────────────────────────────
    write_api_docs(&module_docs, &method_docs, &args.docs)?;

    eprintln!(
        "dtr-javadoc: wrote {} module doc(s) → {}/",
        module_docs.len(),
        args.docs.display()
    );

    Ok(())
}
