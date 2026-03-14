use clap::Parser;
use std::path::PathBuf;

use dtr_javadoc::extract_all;

#[derive(Parser)]
#[command(about = "Extract Javadoc from Java source files into JSON")]
struct Args {
    /// Source directory to scan (e.g. src/main/java or dtr-core/src/main/java)
    #[arg(short, long, default_value = "src/main/java")]
    source: PathBuf,

    /// Output JSON file path
    #[arg(short, long, default_value = "docs/meta/javadoc.json")]
    output: PathBuf,
}

fn main() -> anyhow::Result<()> {
    let args = Args::parse();

    eprintln!(
        "dtr-javadoc: scanning {}",
        args.source.display()
    );

    let results = extract_all(&args.source);

    if let Some(parent) = args.output.parent() {
        std::fs::create_dir_all(parent)?;
    }

    let json = serde_json::to_string_pretty(&results)?;
    std::fs::write(&args.output, &json)?;

    eprintln!(
        "dtr-javadoc: extracted {} entries → {}",
        results.len(),
        args.output.display()
    );

    Ok(())
}
