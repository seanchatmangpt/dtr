# Environment Variables Reference

All environment variables that affect DocTester CLI behavior and configuration.

## DocTester Core Configuration

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `DTR_HOME` | path | `~/.doctester` | Configuration directory for CLI settings and cache |
| `DTR_OUTPUT_DIR` | path | `./` | Default output directory for exports |
| `DTR_FORMAT` | string | `markdown` | Default output format (markdown, html, latex, slides, blog, openapi) |
| `DTR_VERBOSE` | bool | `false` | Enable verbose logging (true, false, 1, 0) |
| `DTR_PARALLEL` | int | `1` | Default parallel jobs (0 = auto-detect CPU cores) |
| `DTR_TIMEOUT` | int | `300` | Operation timeout in seconds |
| `DTR_QUIET` | bool | `false` | Suppress output (except errors) |
| `DTR_CONFIG` | path | `~/.doctester/config` | Path to configuration file |

## Java & Maven Integration

| Variable | Type | Default/Required | Description |
|----------|------|------------------|-------------|
| `JAVA_HOME` | path | Required | Path to Java 25+ installation (openjdk-amd64) |
| `MAVEN_HOME` | path | Optional | Path to Maven 4.0.0-rc-5+ installation |
| `M2_REPO` | path | `~/.m2/repository` | Maven local repository location |
| `MAVEN_OPTS` | string | `` | JVM options for Maven (e.g., `-Xmx2g --enable-preview`) |
| `MVND_HOME` | path | `/opt/mvnd` | Maven Daemon installation directory |

## Output & Rendering Configuration

| Variable | Type | Description |
|----------|------|-------------|
| `DTR_LATEX_COMPILER` | string | LaTeX compiler: pdflatex, xelatex, lualatex, latexmk, pandoc |
| `DTR_LATEX_TEMPLATE` | string | Default LaTeX template: acm-conference, ieee, nature, arxiv, us-patent |
| `DTR_CODE_HIGHLIGHTER` | string | Syntax highlighter: pygments, highlight.js, auto, none |
| `DTR_THEME` | string | Code highlighting theme: monokai, dracula, solarized, github, etc. |
| `DTR_LINE_NUMBERS` | bool | Enable line numbers in code blocks (true/false) |
| `DTR_TOC_DEPTH` | int | Table of contents depth (1-6) |
| `DTR_LINE_LENGTH` | int | Text wrapping line length in characters |

## Blog & Social Media Publishing

| Variable | Type | Description |
|----------|------|-------------|
| `DEVTO_API_KEY` | string | Dev.to API key for publishing |
| `MEDIUM_ACCESS_TOKEN` | string | Medium.com API access token |
| `HASHNODE_API_KEY` | string | Hashnode API key for publishing |
| `SUBSTACK_EMAIL` | string | Substack account email |
| `SUBSTACK_PASSWORD` | string | Substack account password (use with caution) |
| `LINKEDIN_ACCESS_TOKEN` | string | LinkedIn API token for posting |
| `DTR_BLOG_AUTO_PUBLISH` | bool | Automatically publish to configured platforms |
| `DTR_SOCIAL_QUEUE_DIR` | path | Directory for social media posting queue |

## File System & Caching

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `DTR_CACHE_DIR` | path | `~/.doctester/cache` | Cache directory for compiled documents |
| `DTR_TEMP_DIR` | path | System temp | Temporary file directory |
| `DTR_CACHE_TTL` | int | `3600` | Cache time-to-live in seconds |
| `DTR_DISABLE_CACHE` | bool | `false` | Disable caching entirely |
| `DTR_PRESERVE_TEMP` | bool | `false` | Keep temporary files after execution |

## Logging & Debugging

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `DTR_LOG_LEVEL` | string | `INFO` | Logging level: TRACE, DEBUG, INFO, WARN, ERROR |
| `DTR_LOG_FILE` | path | (stderr) | Log file path (if not set, logs go to stderr) |
| `DTR_DEBUG` | bool | `false` | Enable debug mode with stack traces |
| `DTR_PROFILE` | bool | `false` | Enable profiling/timing output |

## Integration Test Configuration

| Variable | Type | Description |
|----------|------|-------------|
| `DOCTESTER_TEST_SERVER_URL` | url | Test server base URL (e.g., http://localhost:8080) |
| `DOCTESTER_DATABASE_URL` | url | H2/Database connection URL (jdbc:h2:mem:test) |
| `DOCTESTER_DATABASE_USER` | string | Database username (default: sa) |
| `DOCTESTER_DATABASE_PASSWORD` | string | Database password |
| `NINJA_SERVLET_PORT` | int | Ninja framework test server port |

## Feature Flags & Experimental Features

| Variable | Type | Default | Description |
|----------|------|---------|-------------|
| `DTR_ENABLE_WEBSOCKET` | bool | `true` | Enable WebSocket support |
| `DTR_ENABLE_SSE` | bool | `true` | Enable Server-Sent Events |
| `DTR_ENABLE_OPENAPI` | bool | `true` | Enable OpenAPI spec generation |
| `DTR_ENABLE_RECEIPTS` | bool | `true` | Enable cryptographic receipts |
| `DTR_ENABLE_CITATIONS` | bool | `true` | Enable bibliography/citation support |

## Examples

### Basic Setup

```bash
# Set Java home for Java 25
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64

# Set default output format and directory
export DTR_FORMAT=latex
export DTR_OUTPUT_DIR=~/Documents/exports

# Enable verbose logging
export DTR_VERBOSE=true

# Export to shell and use in commands
dtr export guide.md  # Will use above settings
```

### Development/Debugging

```bash
# Debug mode with full stack traces
export DTR_DEBUG=true
export DTR_LOG_LEVEL=DEBUG
export DTR_PROFILE=true

# Keep temporary files for inspection
export DTR_PRESERVE_TEMP=true

# Disable caching for fresh exports
export DTR_DISABLE_CACHE=true

dtr export guide.md
```

### Parallel Processing

```bash
# Use 8 parallel jobs (or auto-detect)
export DTR_PARALLEL=8
# or
export DTR_PARALLEL=0  # Auto-detect CPU cores

dtr export docs/*.md --parallel $DTR_PARALLEL
```

### Blog Publishing

```bash
# Set API credentials
export DEVTO_API_KEY="your_key_here"
export MEDIUM_ACCESS_TOKEN="your_token"
export HASHNODE_API_KEY="your_key"

# Enable auto-publishing
export DTR_BLOG_AUTO_PUBLISH=true

dtr export guide.md --format blog
```

### LaTeX/PDF Generation

```bash
# Set LaTeX compiler and template
export DTR_LATEX_COMPILER=xelatex
export DTR_LATEX_TEMPLATE=ieee

dtr export research.md --format latex
```

### Integration Tests

```bash
# Configure test environment
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export MAVEN_OPTS="-Xmx2g --enable-preview"
export DOCTESTER_TEST_SERVER_URL=http://localhost:8080
export DOCTESTER_DATABASE_URL=jdbc:h2:mem:test

mvnd test -pl doctester-integration-test
```

## Precedence & Override Order

Environment variables are applied in this order (later overrides earlier):

1. System defaults (hardcoded in CLI)
2. `~/.doctester/config` (user-level configuration)
3. `doctester.yaml` (project-level configuration)
4. Environment variables
5. Command-line arguments (`--format`, `--output-dir`, etc.)

**Example:**
```bash
# System default: DTR_FORMAT=markdown
# User config (~/.doctester/config): format: latex
# Env var: export DTR_FORMAT=html
# CLI arg: dtr export guide.md --format slides

# Result: --format slides wins (highest precedence)
```

## Security Notes

- **Do NOT commit credentials** (API keys, tokens) to git. Use environment variables or ~/.doctester/config with proper file permissions (chmod 600).
- **Sensitive variables**: `DEVTO_API_KEY`, `MEDIUM_ACCESS_TOKEN`, `HASHNODE_API_KEY`, `LINKEDIN_ACCESS_TOKEN`, `SUBSTACK_PASSWORD`
- Use `.gitignore` to exclude `~/.doctester/config` if it contains secrets.
- Consider using a `.env` file with tools like `direnv` for development workflows.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| "JAVA_HOME not set" | Run `export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64` |
| Slow parallel exports | Lower `DTR_PARALLEL` or set to auto-detect |
| Out of memory errors | Increase heap via `MAVEN_OPTS="-Xmx4g"` |
| Blog publishing fails | Verify API credentials in `DTR_HOME/config` or env vars |
| LaTeX compilation fails | Check `DTR_LATEX_COMPILER` is installed and in PATH |
| Cache issues | Set `DTR_DISABLE_CACHE=true` or delete `~/.doctester/cache` |
