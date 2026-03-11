# How to Export Multiple Files at Once

## Problem
You have dozens of Markdown files scattered across your project that need to be converted to HTML, PDF, or other formats. Manual file-by-file exports would take forever. You need a way to batch-convert entire directories with consistent formatting.

## Solution Overview

The `dtr export` command supports directory paths, glob patterns, and parallel processing for bulk conversions. A single command can convert hundreds of files while respecting your directory structure.

## Basic Batch Export

### Export Single Directory
```bash
# Export all .md files in src/docs/ to HTML
dtr export src/docs/ --format html --output-dir output/html/

# Output structure:
# output/html/
# ├── getting-started.html
# ├── api-reference.html
# └── troubleshooting.html
```

### Export with Nested Directory Structure
```bash
# Preserve directory hierarchy during export
dtr export src/docs/ --format html --output-dir output/ --preserve-structure

# Output preserves input structure:
# output/
# ├── guides/
# │   ├── tutorial.html
# │   └── advanced.html
# └── reference/
#     ├── api.html
#     └── cli.html
```

## Glob Pattern Matching

### Match All Markdown Files
```bash
# All .md files in entire directory tree
dtr export "**/*.md" --format html --output-dir output/

# Only top-level files
dtr export "*.md" --format html --output-dir output/

# Specific subdirectories
dtr export "src/docs/**/*.md" --format html --output-dir output/
```

### Exclude Files
```bash
# Export all except drafts
dtr export src/docs/ --format html --output-dir output/ \
  --exclude "**/*draft*.md" \
  --exclude "**/README.md"

# Multiple exclusions
dtr export src/docs/ \
  --exclude "drafts/" \
  --exclude "temp/" \
  --exclude ".git/" \
  --format html \
  --output-dir output/
```

## Parallel Processing

### Concurrent Exports (Speed Up Large Batches)
```bash
# Export 50 files with 10 concurrent workers
dtr export src/docs/ --format html --output-dir output/ --parallel 10

# Use all available CPU cores
dtr export src/docs/ --format html --output-dir output/ --parallel auto

# Single-threaded (for debugging)
dtr export src/docs/ --format html --output-dir output/ --parallel 1
```

Parallel processing is 3-5x faster for large batches. Use `--parallel auto` for optimal performance.

### Progress Tracking
```bash
# Show progress during batch conversion
dtr export src/docs/ --format html --output-dir output/ --progress

# Verbose output (logs each file conversion)
dtr export src/docs/ --format html --output-dir output/ -v

# Quiet mode (only errors shown)
dtr export src/docs/ --format html --output-dir output/ -q
```

## Error Handling

### Skip Failed Files and Continue
```bash
# By default, one error stops the batch
dtr export src/docs/ --format html --output-dir output/

# Skip problematic files and continue
dtr export src/docs/ --format html --output-dir output/ --continue-on-error

# Detailed error report
dtr export src/docs/ --format html --output-dir output/ --continue-on-error --error-report report.txt
```

### Validate Before Converting
```bash
# Check all files before converting
dtr validate src/docs/

# Then export only valid files
dtr export src/docs/ --format html --output-dir output/ --validate-first
```

## Dry Run (Preview What Will Export)

### Test Without Writing Files
```bash
# See what would be exported without actually converting
dtr export src/docs/ --format html --output-dir output/ --dry-run

# Output shows:
# [DRY RUN] Would export: src/docs/guide.md -> output/guide.html
# [DRY RUN] Would export: src/docs/tutorial.md -> output/tutorial.html
# [DRY RUN] Total: 15 files would be converted
```

## Advanced Batch Scenarios

### Convert to Multiple Formats Simultaneously
```bash
# Generate HTML, PDF, and Markdown in one pass
dtr export src/docs/ \
  --format html,latex,markdown \
  --output-dir output/ \
  --preserve-structure

# Output structure:
# output/
# ├── html/
# │   ├── guide.html
# │   └── api.html
# ├── latex/
# │   ├── guide.pdf
# │   └── api.pdf
# └── markdown/
#     ├── guide.md
#     └── api.md
```

### Apply Format-Specific Settings to Batch
```bash
# Export to PDF with academic template
dtr export src/docs/ \
  --format latex \
  --latex-template arxiv \
  --output-dir output/pdf/

# Export to HTML with custom styling
dtr export src/docs/ \
  --format html \
  --html-template bootstrap \
  --highlight-theme atom-dark \
  --output-dir output/html/
```

### Batch with Custom Output Naming
```bash
# Add timestamp to output files
dtr export src/docs/ \
  --format html \
  --output-dir output/ \
  --output-naming "{name}-{timestamp}.html"

# Example output:
# guide-2026-03-11-143022.html
# api-2026-03-11-143022.html

# Add version prefix
dtr export src/docs/ \
  --format html \
  --output-dir output/ \
  --output-naming "v1.0-{name}.html"

# Example output:
# v1.0-guide.html
# v1.0-api.html
```

## Real-World Examples

### Scenario 1: Export All Docs to GitHub Pages
```bash
# Prepare for GitHub Pages
dtr export docs/ \
  --format html \
  --output-dir _site/ \
  --preserve-structure \
  --html-template github

# Commit and push _site/ for auto-deployment
git add _site/
git commit -m "Update generated documentation"
git push
```

### Scenario 2: Nightly PDF Generation
```bash
#!/bin/bash
# Generate daily PDFs with timestamp

DATE=$(date +%Y-%m-%d)
dtr export docs/api/ \
  --format latex \
  --latex-template acm-conference \
  --output-dir "backups/$DATE/" \
  --parallel auto \
  --continue-on-error

# Archive old PDFs
tar czf "backups/archive-$DATE.tar.gz" "backups/$DATE/"
```

### Scenario 3: Create Multi-Format Release Package
```bash
#!/bin/bash
# Export all formats for a release

VERSION=1.5.0
OUTPUT_DIR="release/$VERSION"

echo "Generating documentation for v$VERSION..."

dtr export src/docs/ \
  --format markdown,html,latex,slides,openapi \
  --output-dir "$OUTPUT_DIR" \
  --preserve-structure \
  --parallel auto

echo "Documentation ready at: $OUTPUT_DIR"
ls -la "$OUTPUT_DIR"
```

### Scenario 4: Selective Export (Production Only)
```bash
# Export only production documentation (exclude drafts)
dtr export docs/ \
  --exclude "**/*draft*" \
  --exclude "**/*wip*" \
  --exclude "**/internal*" \
  --format html \
  --output-dir public/ \
  --continue-on-error

# Generate report
dtr export docs/ \
  --dry-run \
  --exclude "**/*draft*" > export-manifest.txt
```

## Performance Tips

| Tip | Benefit |
|-----|---------|
| Use `--parallel 10` or `--parallel auto` | 3-5x faster for >50 files |
| Run `--dry-run` first on large batches | Avoid surprises, validate patterns |
| Use `--quiet` to reduce console output | Faster logging, cleaner output |
| Exclude unnecessary directories | Fewer files to process |
| Split huge batches into smaller ones | Better error isolation |

## Troubleshooting

**"Pattern matches no files"** → Verify pattern with `ls` first: `ls "**/*.md"` (in appropriate shell)

**"Permission denied on output directory"** → Check write permissions: `chmod 755 output/`

**"Out of memory with parallel"** → Reduce threads: use `--parallel 5` instead of `auto`

**"Some files failed"** → Check error report: `cat export-errors.txt`

**"Output files have wrong names"** → Verify naming template syntax with `--dry-run`

## Next Steps
- [Export in Different Formats](formats.md)
- [Add Images and Tables](rich-content.md)
- [Integrate with Maven](maven-integration.md)
