# DocTester CLI

A comprehensive Python CLI tool for managing, converting, and publishing DocTester documentation exports.

## Features

- **Format Conversion** — Convert between HTML, Markdown, and JSON formats
- **Report Generation** — Generate summaries, coverage reports, and changelogs
- **Directory Management** — List, archive, cleanup, and validate exports
- **Publishing** — Publish to GitHub Pages, AWS S3, Google Cloud Storage, or local directories

## Installation

```bash
cd doctester-cli
pip install -e .
```

## Quick Start

### Convert HTML to Markdown

```bash
doctester convert html-to-markdown target/site/doctester -o ./markdown_docs -r
```

### Generate a Summary Report

```bash
doctester report summary target/site/doctester
```

### List Exports

```bash
doctester manage list target/site/doctester -d
```

### Publish to GitHub Pages

```bash
doctester publish github-pages target/site/doctester --repo owner/repo
```

## Commands

### Convert Commands

```bash
doctester convert html-to-markdown <file>    # HTML → Markdown
doctester convert html-to-json <file>       # HTML → JSON
doctester convert markdown-to-html <file>   # Markdown → HTML
doctester convert list-formats              # Show supported formats
```

### Report Commands

```bash
doctester report summary <export_dir>       # Generate summary report
doctester report coverage <export_dir>      # Generate coverage report
doctester report changelog <export_dir>     # Generate changelog
```

### Manage Commands

```bash
doctester manage list <export_dir>          # List all exports
doctester manage archive <export_dir>       # Create archive (tar.gz/zip)
doctester manage cleanup <export_dir>       # Remove old exports
doctester manage validate <export_dir>      # Validate export integrity
```

### Publish Commands

```bash
doctester publish github-pages <dir> --repo owner/repo
doctester publish s3 <dir> --bucket my-bucket
doctester publish gcs <dir> --bucket my-bucket
doctester publish local <dir> --target /path/to/docs
doctester publish list-platforms            # Show supported platforms
```

## Options

### Global Options

- `--version` — Show version
- `--help` — Show help

### Common Options

- `--output/-o` — Output file or directory
- `--recursive/-r` — Process directories recursively
- `--force/-f` — Overwrite existing files
- `--dry-run` — Preview changes without applying them

## Environment Variables

- `GITHUB_TOKEN` — GitHub personal access token for publishing
- `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY` — AWS credentials for S3
- `GOOGLE_APPLICATION_CREDENTIALS` — GCP service account file path

## Examples

### Convert and Publish Workflow

```bash
# 1. Generate documentation (from Java project)
mvnd clean verify

# 2. Convert HTML to Markdown
doctester convert html-to-markdown target/site/doctester -o docs/api -r

# 3. Generate coverage report
doctester report coverage target/site/doctester -o coverage.html

# 4. Archive exports
doctester manage archive target/site/doctester

# 5. Publish to GitHub Pages
export GITHUB_TOKEN=your_token_here
doctester publish github-pages target/site/doctester --repo myorg/myrepo
```

### CI/CD Integration

```bash
# In your CI/CD pipeline
doctester publish local target/site/doctester --target ./build/docs
doctester publish s3 ./build/docs --bucket my-docs --region us-west-2
```

## Architecture

The CLI is organized into modules:

- **converters/** — Format conversion (HTML, JSON, Markdown)
- **reporters/** — Report generation (summary, coverage, changelog)
- **managers/** — Directory management (list, archive, cleanup, validate)
- **publishers/** — Upload/publish to platforms (GitHub, S3, GCS, local)

Each module has a base abstract class and specific implementations.

## Development

Install development dependencies:

```bash
pip install -e ".[dev]"
```

Run tests:

```bash
pytest
```

## License

Apache 2.0 — Same as DocTester core library
