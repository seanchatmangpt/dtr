# CLI Commands Reference

Complete reference for all `dtr` (DocTester) commands with all options, arguments, and examples.

## dtr export

Convert markdown/HTML to other formats (HTML, PDF, slides, blog, OpenAPI, and more).

### Syntax

```
dtr export <INPUT_FILE> [OUTPUT_FILE] [OPTIONS]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| INPUT_FILE | Yes | Path to markdown or HTML file (or directory for batch mode) |
| OUTPUT_FILE | No | Path for output file; auto-generated from INPUT_FILE if omitted |

### Options

| Option | Short | Type | Default | Description |
|--------|-------|------|---------|-------------|
| --format | -f | string | markdown | Output format: markdown, html, latex, slides, blog, openapi |
| --template | -t | string | default | Template name: bootstrap, acm-conference, ieee, nature, arxiv, us-patent |
| --output-dir | -o | path | ./ | Directory for output files |
| --parallel | -p | int | 1 | Number of concurrent operations (0 = auto-detect CPU cores) |
| --dry-run | | flag | false | Show what would happen without executing |
| --verbose | -v | flag | false | Detailed output and logging |
| --force | | flag | false | Overwrite existing output files |
| --code-highlight | | string | auto | Syntax highlighter: pygments, highlight.js, none |
| --preserve-formatting | | flag | false | Keep original whitespace and line breaks |
| --minify | | flag | false | Remove unnecessary whitespace |
| --include-toc | | flag | true | Generate table of contents (markdown, html) |
| --line-length | | int | 80 | Maximum line length for text wrapping |
| --help | -h | flag | false | Show help for this command |

### Examples

```bash
# Export single markdown file to HTML
dtr export guide.md output.html

# Export to PDF with ACM conference template
dtr export guide.md output.pdf --format latex --template acm-conference

# Export directory of files to HTML (parallel)
dtr export docs/ --format html --output-dir build/ --parallel 8

# Dry-run to see what would happen
dtr export guide.md --format latex --dry-run

# Export with custom formatting options
dtr export api-docs.md --format html --code-highlight pygments --line-length 100

# Force overwrite and include verbose logging
dtr export guide.md output.html -f -v

# Export to slides with no line numbering
dtr export tutorial.md slides.html --format slides
```

---

## dtr fmt

Format/beautify HTML or markdown files for consistency and readability.

### Syntax

```
dtr fmt <INPUT_FILE> [--output OUTPUT_FILE] [OPTIONS]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| INPUT_FILE | Yes | Path to markdown or HTML file |

### Options

| Option | Short | Type | Default | Description |
|--------|-------|------|---------|-------------|
| --output | -o | path | (stdout) | Output file path; prints to console if omitted |
| --minify | | flag | false | Minify output (remove all whitespace) |
| --expand | | flag | false | Expand/prettify output with indentation |
| --line-length | -l | int | 80 | Maximum line length for wrapping |
| --code-highlight | | string | auto | Syntax highlighter: pygments, highlight.js, none |
| --normalize-headers | | flag | true | Normalize heading levels (h1, h2, etc.) |
| --sort-tables | | flag | false | Sort table rows alphabetically |
| --tab-width | | int | 2 | Tab/indent width in spaces |
| --preserve-comments | | flag | true | Keep HTML comments intact |
| --help | -h | flag | false | Show help for this command |

### Examples

```bash
# Format markdown file and save
dtr fmt messy.md --output formatted.md

# Beautify HTML with indentation
dtr fmt ugly.html --output pretty.html --expand

# Minify HTML for distribution
dtr fmt large.html --output small.html --minify

# Format with custom line length
dtr fmt code.md --output out.md --line-length 100 --code-highlight pygments

# Print formatted output to console
dtr fmt guide.md

# Normalize all headers and expand formatting
dtr fmt doc.md --output formatted.md --normalize-headers --expand
```

---

## dtr report

Generate summary reports (word count, readability metrics, structure analysis).

### Syntax

```
dtr report <INPUT_FILE> [OPTIONS]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| INPUT_FILE | Yes | Path to markdown or HTML file |

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| --format | string | text | Output format: text, json, csv |
| --detailed | flag | false | Include detailed breakdown per section |
| --readability | flag | false | Calculate readability scores (Flesch-Kincaid, etc.) |
| --output | path | (stdout) | Output file path |
| --include-images | flag | true | Count and report on images |
| --include-links | flag | true | Count and report on links |
| --help | -h | flag | false | Show help for this command |

### Examples

```bash
# Generate basic report
dtr report guide.md

# Generate detailed JSON report
dtr report guide.md --format json --detailed --output report.json

# Readability analysis (Flesch-Kincaid, etc.)
dtr report api-docs.md --readability

# CSV format for spreadsheet import
dtr report docs/*.md --format csv --output report.csv

# Full detailed analysis
dtr report guide.md --detailed --readability --include-images --include-links
```

---

## dtr list

List exported files and their metadata.

### Syntax

```
dtr list [DIRECTORY] [OPTIONS]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| DIRECTORY | No | Directory to scan; defaults to current directory |

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| --sort | string | name | Sort by: name, size, date, format, type |
| --filter | string | * | Filter by format or pattern (*.html, *.pdf) |
| --detailed | flag | false | Show full metadata (size, date, format) |
| --recursive | -r | flag | false | Scan subdirectories |
| --output | path | (stdout) | Output file path (json, csv, text) |
| --format | string | text | Output format: text, json, csv, table |
| --help | -h | flag | false | Show help for this command |

### Examples

```bash
# List files in current directory
dtr list

# List all PDF files, sorted by date
dtr list --filter "*.pdf" --sort date

# Detailed listing with all metadata
dtr list docs/ --detailed --recursive

# Export file list to JSON
dtr list . --recursive --format json --output inventory.json

# List only markdown files, sorted by size
dtr list --filter "*.md" --sort size --detailed
```

---

## dtr validate

Validate markdown/HTML syntax and structure.

### Syntax

```
dtr validate <INPUT_FILE> [OPTIONS]
```

### Arguments

| Argument | Required | Description |
|----------|----------|-------------|
| INPUT_FILE | Yes | Path to markdown or HTML file |

### Options

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| --strict | flag | false | Enable strict validation (all warnings treated as errors) |
| --format | string | auto | Expected format: markdown, html (auto-detect if omitted) |
| --output | path | (stdout) | Output file path for report |
| --help | -h | flag | false | Show help for this command |

### Examples

```bash
# Validate markdown file
dtr validate guide.md

# Strict validation (warnings = errors)
dtr validate guide.md --strict

# Validate HTML and save report
dtr validate index.html --output validation-report.txt
```

---

## dtr config

Manage CLI configuration and settings.

### Syntax

```
dtr config [COMMAND] [OPTIONS]
```

### Subcommands

| Subcommand | Description |
|------------|-------------|
| show | Display current configuration |
| set | Set a configuration value |
| get | Get a single configuration value |
| reset | Reset to default configuration |
| edit | Open configuration file in editor |

### Options

| Option | Type | Description |
|--------|------|-------------|
| --user | flag | Show/set user-level config (~/.doctester/config) |
| --project | flag | Show/set project-level config (doctester.yaml) |
| --help | -h | flag | Show help for this command |

### Examples

```bash
# Show all configuration
dtr config show

# Set default format
dtr config set format latex

# Get specific value
dtr config get output_dir

# Reset to defaults
dtr config reset

# Edit config file in $EDITOR
dtr config edit
```

---

## dtr version

Display CLI version and environment information.

### Syntax

```
dtr version [OPTIONS]
```

### Options

| Option | Type | Description |
|--------|------|-------------|
| --verbose | -v | flag | Show Java/Maven/system details |
| --help | -h | flag | Show help for this command |

### Examples

```bash
# Show version
dtr version

# Show detailed environment info
dtr version --verbose
```

---

## dtr help

Display help information for commands.

### Syntax

```
dtr help [COMMAND] [OPTIONS]
```

### Examples

```bash
# Show main help
dtr help

# Show help for export command
dtr help export

# Show help for fmt
dtr help fmt
```

---

## Global Options

All commands support these global flags:

| Option | Short | Type | Default | Description |
|--------|-------|------|---------|-------------|
| --verbose | -v | flag | false | Enable verbose logging |
| --quiet | -q | flag | false | Suppress output (except errors) |
| --config | -c | path | ~/.doctester/config | Path to configuration file |
| --help | -h | flag | false | Show help |
| --version | | flag | false | Show CLI version |
| --parallel | -p | int | 1 | Default parallel jobs (0 = auto) |

### Examples

```bash
# Use custom config file
dtr export guide.md --config /etc/doctester.yaml

# Verbose output for all commands
dtr export guide.md -v

# Quiet mode (no output except errors)
dtr export guide.md --quiet
```
