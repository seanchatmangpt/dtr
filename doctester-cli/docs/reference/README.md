# Reference Documentation

Complete, structured reference for all DocTester CLI commands, configuration, environment variables, exit codes, and terminology.

## Files in This Reference

### [CLI Commands Reference](./cli-commands.md)
Complete listing of all `dtr` commands with detailed syntax, arguments, options, and examples.

**Commands documented:**
- `dtr export` — Convert files to multiple output formats
- `dtr fmt` — Format and beautify HTML/markdown files
- `dtr report` — Generate summary reports with statistics
- `dtr list` — List exported files and metadata
- `dtr validate` — Validate markdown/HTML syntax
- `dtr config` — Manage CLI configuration
- `dtr version` — Display version information
- `dtr help` — Display help for commands

**Features:**
- Complete option tables with types and defaults
- Real-world examples for each command
- Global options reference
- Syntax patterns for all commands

### [Environment Variables Reference](./environment-variables.md)
All environment variables affecting DocTester CLI behavior and platform integration.

**Categories:**
- DocTester core configuration (DTR_* variables)
- Java & Maven integration (JAVA_HOME, MAVEN_OPTS, etc.)
- Output & rendering configuration (LaTeX, syntax highlighting, TOC)
- Blog & social media publishing credentials
- File system & caching configuration
- Logging & debugging options
- Integration test configuration
- Feature flags & experimental features

**Features:**
- Complete variable reference with types and defaults
- Precedence and override order explanation
- Usage examples for common scenarios
- Security notes for credential management
- Troubleshooting guide by variable

### [Configuration File Reference](./configuration.md)
User-level (~/.doctester/config) and project-level (./doctester.yaml) configuration.

**Sections:**
- User configuration structure and keys
- Project configuration with export targets
- Maven, Java, and database settings
- Blog publishing platforms and credentials
- LaTeX/PDF rendering options
- OpenAPI/Swagger generation
- Bibliography and citation styles
- Configuration precedence rules
- Secret management and `.gitignore` patterns

**Features:**
- Complete YAML schema examples
- All configuration keys documented
- Template selection guide
- Security best practices
- Common configuration patterns
- CI/CD integration examples

### [Exit Codes Reference](./exit-codes.md)
Complete reference of all exit codes returned by the DocTester CLI.

**Exit codes documented:**
- 0 = Success
- 1 = Validation error
- 2 = Runtime error
- 3 = System error
- 4 = Configuration error
- 5 = Dependency error
- 6 = Output error
- 7 = Template error
- 8 = Compilation error
- 9 = Timeout error
- 10 = Authentication error
- 11 = Network error
- 130 = Interrupted (Ctrl+C)
- 143 = Terminated (SIGTERM)

**Features:**
- Detailed explanation of each code
- Common causes and solutions
- Examples of each error type
- Shell script integration patterns
- CI/CD pipeline usage
- Retry logic examples
- POSIX convention reference

### [Glossary of Terms](./glossary.md)
Comprehensive glossary of all terminology used throughout DocTester documentation.

**Coverage:**
- 100+ defined terms (A-Z)
- Technical definitions
- Context and usage examples
- Links to related concepts
- Explains jargon and abbreviations

**Categories:**
- Build tools (Maven, mvnd, Gradle)
- Formats (Markdown, LaTeX, HTML, JSON, YAML)
- Platforms (Dev.to, Medium, Hashnode, LinkedIn)
- Java features (records, sealed classes, pattern matching, virtual threads)
- Protocols (HTTP, WebSocket, SSE, OAuth2)
- Concepts (CI/CD, authentication, templating)

## How to Use This Reference

### Quick Lookup
- **Need to know a command option?** → [CLI Commands Reference](./cli-commands.md)
- **Looking for an environment variable?** → [Environment Variables Reference](./environment-variables.md)
- **Understanding an exit code?** → [Exit Codes Reference](./exit-codes.md)
- **Setting up configuration?** → [Configuration File Reference](./configuration.md)
- **What does a term mean?** → [Glossary of Terms](./glossary.md)

### By Use Case

**Configuring DocTester:**
1. Read [Configuration File Reference](./configuration.md) for file structure
2. Check [Environment Variables Reference](./environment-variables.md) for available options
3. See precedence rules to understand override order

**Running Commands:**
1. Consult [CLI Commands Reference](./cli-commands.md) for syntax and options
2. Check [Glossary](./glossary.md) if you encounter unfamiliar terms
3. Refer to [Exit Codes Reference](./exit-codes.md) if something fails

**Troubleshooting:**
1. Check the command's exit code in [Exit Codes Reference](./exit-codes.md)
2. Find solutions and common causes
3. Verify configuration in [Configuration File Reference](./configuration.md)
4. Check environment variables in [Environment Variables Reference](./environment-variables.md)

**Publishing Documentation:**
1. Review blog options in [Configuration File Reference](./configuration.md)
2. Set API credentials from [Environment Variables Reference](./environment-variables.md)
3. Use `dtr export --format blog` from [CLI Commands Reference](./cli-commands.md)

## Reference Organization

This reference follows the **Diataxis Framework** for information-oriented documentation:

- **Complete** — All commands, options, variables documented
- **Structured** — Tables, sections, clear organization
- **Searchable** — Headings, glossary, clear hierarchy
- **Scannable** — Tables, lists, bold keywords for quick lookup
- **Accurate** — Reflects actual CLI behavior and features
- **Up-to-date** — Updated with latest DocTester version

## Related Documentation

For other documentation types, see:

- **Tutorials** — Getting started guides and step-by-step walkthroughs
- **How-To Guides** — Task-focused guides for specific scenarios
- **Explanation** — Deep dives into concepts and architecture
- **Troubleshooting** — Problem-solving guides and FAQs

## Quick Reference Cards

### Common Commands

```bash
# Basic export
dtr export guide.md output.html

# Export to PDF
dtr export guide.md output.pdf --format latex --template ieee

# Format markdown
dtr fmt messy.md --output clean.md

# Generate report
dtr report guide.md --readability

# List files
dtr list --filter "*.pdf" --sort date --detailed
```

### Common Configuration

```yaml
# ~/.doctester/config
format: markdown
output_dir: ~/Documents/exports
verbose: false
parallel: 0
```

### Common Environment Variables

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export DTR_FORMAT=latex
export DTR_PARALLEL=8
export DEVTO_API_KEY="your_key_here"
```

---

**Last updated:** 2026-03-11  
**Version:** 2.5.0  
**Format:** Markdown (Diataxis Reference)
