# Configuration File Reference

Complete guide to configuring DocTester CLI via configuration files.

## User Configuration: ~/.doctester/config

The user-level configuration file is stored in YAML format at `~/.doctester/config`. This file controls default behavior for all DocTester CLI commands across your entire system.

### Location

```
~/.doctester/config
```

### Permissions

Set secure file permissions to protect sensitive credentials:

```bash
chmod 600 ~/.doctester/config
```

### Complete Configuration Example

```yaml
# Default output format for all exports
format: markdown

# Default output directory
output_dir: ~/Documents/exports

# Enable verbose logging by default
verbose: false

# Enable quiet mode (suppress output)
quiet: false

# Default parallel jobs (0 = auto-detect CPU cores)
parallel: 0

# Operation timeout in seconds
timeout: 300

# Text formatting options
text:
  line_length: 80
  tab_width: 2
  preserve_formatting: false

# Code highlighting configuration
code:
  highlighter: pygments
  line_numbers: true
  theme: monokai

# LaTeX/PDF rendering
latex:
  default_template: acm-conference
  compiler: pdflatex  # pdflatex, xelatex, lualatex, latexmk, pandoc

# Blog publishing configuration
blog:
  auto_publish: false
  platforms:
    - devto
    - medium
    - hashnode

  # Platform-specific settings
  devto:
    api_key: "${DEVTO_API_KEY}"
    auto_publish: false

  medium:
    access_token: "${MEDIUM_ACCESS_TOKEN}"
    publication_id: "your-publication-id"

  hashnode:
    api_key: "${HASHNODE_API_KEY}"
    publication_id: "your-publication-id"

  substack:
    email: "${SUBSTACK_EMAIL}"
    password: "${SUBSTACK_PASSWORD}"

  linkedin:
    access_token: "${LINKEDIN_ACCESS_TOKEN}"

# Table of contents configuration
toc:
  enabled: true
  depth: 3  # Heading levels 1-3
  include_page_numbers: false

# Cache configuration
cache:
  enabled: true
  ttl: 3600  # seconds
  directory: ~/.doctester/cache

# Logging configuration
logging:
  level: INFO  # TRACE, DEBUG, INFO, WARN, ERROR
  file: ~/.doctester/cli.log
  format: json  # json, text

# OpenAPI/Swagger generation
openapi:
  enabled: true
  output_format: yaml  # yaml, json
  include_examples: true
  version: "3.1.0"

# Bibliography/citations
bibliography:
  enabled: true
  style: IEEE  # IEEE, APA, Chicago, Nature

# Optional: Per-command defaults
commands:
  export:
    format: markdown
    template: default

  fmt:
    minify: false
    expand: true

  report:
    format: text
    detailed: false
```

### Configuration Keys

#### Core Settings

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `format` | string | markdown | Default output format |
| `output_dir` | path | ./ | Default output directory |
| `verbose` | bool | false | Enable verbose logging |
| `quiet` | bool | false | Suppress non-error output |
| `parallel` | int | 1 | Parallel jobs (0 = auto) |
| `timeout` | int | 300 | Operation timeout (seconds) |

#### Text Formatting

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `text.line_length` | int | 80 | Max line length for wrapping |
| `text.tab_width` | int | 2 | Tab indentation width |
| `text.preserve_formatting` | bool | false | Keep original spacing |

#### Code Highlighting

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `code.highlighter` | string | pygments | Highlighter: pygments, highlight.js, none |
| `code.line_numbers` | bool | true | Show line numbers |
| `code.theme` | string | monokai | Color theme |

---

## Project Configuration: doctester.yaml

Project-level configuration file stored in the project root. Overrides user-level settings for a specific project.

### Location

```
./doctester.yaml  # Project root
```

### Complete Project Configuration Example

```yaml
# Project metadata
name: MyProject
version: 1.0.0
author: Your Name
description: API documentation and integration tests

# Override user settings for this project
format: latex
template: ieee
output_dir: build/docs

# Multiple export targets with different configurations
exports:
  - name: api-documentation
    description: Complete API reference
    input: src/docs/api.md
    format: latex
    template: ieee
    output: build/docs/api.pdf
    options:
      code_highlight: pygments
      include_toc: true

  - name: user-guide
    description: End-user guide and tutorials
    input: src/docs/guide.md
    format: html
    output: build/docs/guide.html
    options:
      code_highlight: highlight.js

  - name: slides
    description: Presentation slides
    input: src/docs/presentation.md
    format: slides
    output: build/slides/presentation.html

  - name: blog-posts
    description: Blog export queue
    input: src/docs/blog/*.md
    format: blog
    options:
      auto_publish: false
      platforms:
        - devto
        - medium

# Maven integration
maven:
  generate_docs: true
  output_dir: target/docs
  skip_tests: false
  test_module: doctester-integration-test

# Java/compilation settings
java:
  version: 25
  enable_preview: true
  release: 25

# Blog publishing defaults for this project
blog:
  auto_publish: false
  platforms:
    - devto
    - medium

  devto:
    auto_publish: true
    tags: [api, documentation, java]

  medium:
    auto_publish: false
    tags: [api, documentation]

# OpenAPI/Swagger generation
openapi:
  enabled: true
  title: "MyProject API"
  version: "1.0.0"
  description: "Complete API reference documentation"
  output: build/openapi.yaml

# Database configuration (for integration tests)
database:
  type: h2  # h2, postgresql, mysql, oracle
  url: "jdbc:h2:mem:test"
  username: sa
  password: ""
  schema: public

# LaTeX/PDF settings
latex:
  default_template: ieee
  compiler: pdflatex
  paper_size: letter  # letter, a4
  font_size: 11  # 10, 11, 12
  margin: 1in
  include_cover_page: true
  include_toc: true
  toc_depth: 3

# Custom output patterns
output_patterns:
  markdown: "target/docs/test-results.md"
  html: "target/docs/index.html"
  pdf: "target/pdf/documentation.pdf"
  openapi: "target/openapi.yaml"
```

### Project Configuration Keys

#### Project Metadata

| Key | Type | Description |
|-----|------|-------------|
| `name` | string | Project name |
| `version` | string | Project version |
| `author` | string | Author/maintainer name |
| `description` | string | Project description |

#### Exports

The `exports` section defines multiple output targets:

```yaml
exports:
  - name: unique-identifier
    description: Human-readable description
    input: input-file-or-directory
    format: markdown|html|latex|slides|blog|openapi
    template: template-name
    output: output-file-path
    options:
      # Format-specific options
```

#### Maven Integration

| Key | Type | Description |
|-----|------|-------------|
| `maven.generate_docs` | bool | Auto-generate docs during build |
| `maven.output_dir` | path | Maven output directory |
| `maven.skip_tests` | bool | Skip test execution |
| `maven.test_module` | string | Module containing doc tests |

#### Java/Compilation

| Key | Type | Default | Description |
|-----|------|---------|-------------|
| `java.version` | int | 25 | Java version |
| `java.enable_preview` | bool | true | Enable preview features |
| `java.release` | int | 25 | Release version |

---

## Configuration Precedence

DocTester applies configuration in this order (later overrides earlier):

1. **Built-in defaults** (hardcoded in CLI)
2. **User config** (`~/.doctester/config`)
3. **Project config** (`./doctester.yaml`)
4. **Environment variables** (`DTR_FORMAT`, `DTR_VERBOSE`, etc.)
5. **Command-line arguments** (`--format`, `--verbose`, etc.)

### Example: Precedence in Action

```yaml
# ~/.doctester/config
format: markdown
parallel: 4
```

```yaml
# ./doctester.yaml (project-level)
format: latex
template: acm-conference
```

```bash
# Command-line
dtr export guide.md --format html

# Result:
# format = html        (CLI argument wins)
# parallel = 4         (from user config, not overridden)
# template = acm-conference (from project config)
```

---

## Templates

### LaTeX Templates

Available values for `latex.default_template`:

| Template | Purpose |
|----------|---------|
| `acm-conference` | ACM conference proceedings format |
| `ieee` | IEEE Transactions journal format |
| `nature` | Nature magazine format |
| `arxiv` | arXiv preprint format (cs.CL, physics, etc.) |
| `us-patent` | US patent document format |

### Blog Templates

Available values for `blog.platforms`:

| Platform | Requires | Description |
|----------|----------|-------------|
| `devto` | `DEVTO_API_KEY` | Dev.to community blog |
| `medium` | `MEDIUM_ACCESS_TOKEN` | Medium.com long-form publishing |
| `hashnode` | `HASHNODE_API_KEY` | Hashnode developer blog network |
| `substack` | `SUBSTACK_EMAIL`, `SUBSTACK_PASSWORD` | Substack newsletter platform |
| `linkedin` | `LINKEDIN_ACCESS_TOKEN` | LinkedIn professional network |

---

## Secret Management

### Using Environment Variables

Protect sensitive data by referencing environment variables in config:

```yaml
blog:
  devto:
    api_key: "${DEVTO_API_KEY}"  # Reads from $DEVTO_API_KEY

  medium:
    access_token: "${MEDIUM_ACCESS_TOKEN}"
```

Then set the variable before running:

```bash
export DEVTO_API_KEY="your_key_here"
dtr export guide.md --format blog
```

### File Permissions

Protect sensitive configuration files:

```bash
chmod 600 ~/.doctester/config
chmod 600 ./doctester.yaml  # If it contains secrets
```

### .gitignore

Prevent accidental commits of sensitive files:

```
# .gitignore
~/.doctester/config
.env
.env.local
doctester.yaml  # If it contains API keys
```

---

## Validation

Validate configuration files before using:

```bash
dtr config show      # Display all configuration
dtr config validate  # Check for errors
```

---

## Common Patterns

### Minimal Configuration (User)

```yaml
# ~/.doctester/config
format: markdown
output_dir: ~/Documents/exports
verbose: false
```

### Team/Project Configuration

```yaml
# ./doctester.yaml
name: TeamProject
version: 1.0.0

exports:
  - name: api-docs
    input: docs/api.md
    format: latex
    template: ieee
    output: build/api.pdf

  - name: user-guide
    input: docs/guide.md
    format: html
    output: build/guide.html

maven:
  generate_docs: true
  output_dir: target/docs
```

### CI/CD Configuration

```yaml
# ./doctester.yaml (for CI pipelines)
name: ProjectCI
java:
  version: 25
  enable_preview: true

exports:
  - name: generated-docs
    input: "*.md"
    format: html
    output: build/docs/index.html

maven:
  generate_docs: true
  skip_tests: false
```
