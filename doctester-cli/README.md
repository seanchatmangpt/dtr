# DocTester CLI

A comprehensive, modern Python CLI tool for managing, converting, and publishing DocTester documentation exports.

**Built with:** Python 3.12+ • Typer • uv • Pydantic • Rich

## Features

- **Format Conversion** — Convert between HTML, Markdown, and JSON formats
- **Report Generation** — Generate summaries, coverage reports, and changelogs
- **Directory Management** — List, archive, cleanup, and validate exports
- **Publishing** — Publish to GitHub Pages, AWS S3, Google Cloud Storage, or local directories
- **Modern Python** — Type hints, dataclasses, fast async support with virtual threads
- **Fast Dependency Management** — Managed with `uv` for rapid installation

## Installation

### Using `uv` (Recommended)

```bash
cd doctester-cli
uv sync
```

Install with all optional dependencies:

```bash
uv sync --all-extras
```

### Using pip

```bash
cd doctester-cli
pip install -e .
```

## Quick Start

The CLI is available as both `dtr` (shorthand) and `doctester` (full name):

### Convert HTML to Markdown

```bash
dtr fmt md target/site/doctester -o ./markdown_docs -r
```

### Generate a Summary Report

```bash
dtr report sum target/site/doctester
```

### List Exports

```bash
dtr export list target/site/doctester -d
```

### Publish to GitHub Pages

```bash
dtr push gh target/site/doctester --repo owner/repo
```

## Commands

### Format Commands (`fmt`)

```bash
dtr fmt md <file>                  # Convert HTML → Markdown
dtr fmt json <file>                # Convert HTML → JSON
dtr fmt html <file>                # Convert Markdown → HTML
dtr fmt --help                     # Show available formats
```

### Report Commands (`report`)

```bash
dtr report sum <export_dir>        # Generate summary report
dtr report cov <export_dir>        # Generate coverage report
dtr report log <export_dir>        # Generate changelog
```

### Export Commands (`export`)

```bash
dtr export list <export_dir>       # List all exports
dtr export save <export_dir>       # Create archive (tar.gz/zip)
dtr export clean <export_dir>      # Remove old exports
dtr export check <export_dir>      # Validate export integrity
```

### Push Commands (`push`)

```bash
dtr push gh <dir> --repo owner/repo           # Push to GitHub Pages
dtr push s3 <dir> --bucket my-bucket          # Push to AWS S3
dtr push gcs <dir> --bucket my-bucket         # Push to Google Cloud Storage
dtr push local <dir> --target /path/to/docs   # Push to local directory
dtr push --help                               # Show available platforms
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
dtr fmt md target/site/doctester -o docs/api -r

# 3. Generate coverage report
dtr report cov target/site/doctester -o coverage.html

# 4. Archive exports
dtr export save target/site/doctester

# 5. Publish to GitHub Pages
export GITHUB_TOKEN=your_token_here
dtr push gh target/site/doctester --repo myorg/myrepo
```

### CI/CD Integration

```bash
# In your CI/CD pipeline
dtr export check target/site/doctester          # Validate first
dtr push local target/site/doctester --target ./build/docs
dtr push s3 ./build/docs --bucket my-docs --region us-west-2
```

## Architecture

The CLI is organized into modules:

- **converters/** — Format conversion (HTML, JSON, Markdown)
- **reporters/** — Report generation (summary, coverage, changelog)
- **managers/** — Directory management (list, archive, cleanup, validate)
- **publishers/** — Upload/publish to platforms (GitHub, S3, GCS, local)

Each module has a base abstract class and specific implementations.

## Development

### Setup

Install with development dependencies using `uv`:

```bash
uv sync --all-extras
```

### Available Commands

View all development commands:

```bash
make help
```

Common commands:

```bash
make install-dev    # Install with dev dependencies
make lint           # Run Ruff linter
make format         # Format code
make type-check     # Run mypy type checking
make check          # Run all checks
make test           # Run pytest
make coverage       # Run tests with coverage report
make clean          # Remove build artifacts
```

### Code Quality

This project uses modern Python tooling:

- **Ruff** — Fast Python linter and formatter
- **mypy** — Static type checking (Python 3.12+)
- **pytest** — Unit testing framework
- **uv** — Ultra-fast Python package manager

All code follows PEP 8 with 100-character line length. Import sorting is handled by Ruff's isort integration.

## License

Apache 2.0 — Same as DocTester core library
