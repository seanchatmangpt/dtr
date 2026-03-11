# Architecture Overview

DocTester CLI is structured as a layered system where each layer has a specific responsibility. Understanding these layers helps you predict behavior, extend functionality, and debug issues effectively.

## System Layers

### 1. CLI Layer (Typer)
The user-facing interface built with Typer (a FastAPI sibling for CLIs).

**Responsibilities:**
- Parse command-line arguments and options
- Validate input paths (files/directories exist)
- Validate output paths (writable)
- Format enum values (HTML, LaTeX, etc.)
- Present results with color, tables, and progress bars
- Generate friendly error messages

**Key Insight:** This layer is intentionally thin. Heavy lifting happens in layers below. This keeps the CLI responsive and testable.

### 2. Business Logic Layer
Orchestrates the export workflow without touching rendering details.

**Responsibilities:**
- Load configuration (from CLI args + config file)
- Detect input format automatically (markdown, jupyter, etc.)
- Determine which renderer to use
- Coordinate parallel jobs
- Manage state (file tracking, progress)
- Handle errors gracefully (continue on failure, report summary)

**Example Flow:**
```
User runs: dtr export guide.md output.html
↓
CLI validates paths exist
↓
Orchestrator detects markdown format
↓
Selects HTML renderer
↓
Passes to Rendering Engine
```

### 3. Rendering Engine Layer
Converts markdown/jupyter into target format.

**Responsibilities:**
- Apply format-specific transformations
- Select and instantiate templates
- Apply syntax highlighting
- Generate tables of contents
- Invoke external tools (pdflatex, pandoc)
- Embed resources (fonts, stylesheets)

**Why separate?** Each format (HTML, LaTeX, PDF, Blog) has unique rules. Isolating them prevents format A from breaking format B.

### 4. Maven Integration Layer
Allows DocTester to run as part of the build lifecycle.

**Responsibilities:**
- Listen to Maven lifecycle events (test-compile, verify)
- Interpolate Maven properties into configurations
- Inject artifacts into build output directory
- Report build status

**Key Insight:** Maven integration is optional. CLI works standalone. This keeps the core library focused.

### 5. System Layer
Manages files, processes, and resources.

**Responsibilities:**
- Stream large files (not loading into memory)
- Create and clean temp files
- Execute external processes (pdflatex, etc.)
- Handle signals (graceful shutdown on Ctrl+C)
- Manage thread pools for parallel operations

## Data Flow

A typical export follows this path:

```
┌─────────────────────────────────────────────────────────┐
│ User Input                                              │
│ dtr export guide.md output.html --format html            │
└──────────────────┬──────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────────────┐
│ CLI Layer (Typer)                                       │
│ • Parse arguments: input, output, format                │
│ • Validate: file exists, output writable                │
│ • Build config object                                   │
└──────────────────┬──────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────────────┐
│ Business Logic Layer (Orchestrator)                     │
│ • Load user config + defaults                           │
│ • Detect format: "guide.md" → markdown                 │
│ • Select renderer: html                                 │
│ • Create job queue                                      │
└──────────────────┬──────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────────────┐
│ Rendering Engine Layer                                  │
│ • Parse markdown                                        │
│ • Apply HTML template                                   │
│ • Syntax highlight code blocks                          │
│ • Generate output HTML                                  │
└──────────────────┬──────────────────────────────────────┘
                   ↓
┌─────────────────────────────────────────────────────────┐
│ System Layer (File I/O)                                 │
│ • Write HTML file to output.html                        │
│ • Clean up temp files                                   │
│ • Report success                                        │
└──────────────────┬──────────────────────────────────────┘
                   ↓
            ┌──────────────┐
            │ Success ✓    │
            │ output.html  │
            └──────────────┘
```

## Design Principles

### Principle 1: Streaming First
**Problem:** Large files (1GB+) cannot be loaded into memory.

**Solution:** Stream markdown in chunks. Each chunk is processed independently, keeping memory usage constant (~50MB regardless of input size).

**Where Applied:**
- Markdown parsing uses line-by-line iteration
- JSON/YAML output written incrementally
- Temp files used for intermediate results

### Principle 2: Multi-format by Design
**Problem:** Different platforms want different formats (blogs want JSON, papers want PDF).

**Solution:** Single markdown source, multiple renderers. Each renderer is independent; adding a new format doesn't break existing ones.

**Where Applied:**
- `HtmlRenderer`, `LatexRenderer`, `BlogRenderer` are separate classes
- Shared markdown parser fed to different renderers
- No format-specific code in core modules

### Principle 3: Maven-First Integration
**Problem:** Docs are often generated manually or forgotten.

**Solution:** Maven plugin runs automatically during build. Docs always stay in sync with code.

**Where Applied:**
- Maven plugin hooks into `verify` phase
- Properties interpolated from pom.xml
- Artifacts committed to build output

### Principle 4: Graceful Degradation
**Problem:** One failing export shouldn't block others.

**Solution:** Errors are caught, logged, and processing continues.

**Where Applied:**
- Batch exports: one file fails, others continue
- Format fallback: LaTeX fails → use Markdown output instead
- Missing dependencies: guide user to install, don't crash

### Principle 5: Fail Fast on Input
**Problem:** Discovering invalid paths mid-export wastes time.

**Solution:** Validate paths, permissions, and formats immediately.

**Where Applied:**
- CLI layer validates before passing to orchestrator
- Non-existent input files caught in first 100ms
- Permission errors detected upfront

## Concurrency Model

DocTester uses thread pools for parallel operations to make large batch exports fast.

**How It Works:**
```
ThreadPoolExecutor (8 threads by default)
├── Thread 1: Export file1.md → file1.html
├── Thread 2: Export file2.md → file2.html
├── Thread 3: Export file3.md → file3.html
├── ...
└── Thread 8: Export file8.md → file8.html
```

**Safety Measures:**
- Each thread gets isolated temp directory
- RenderMachine state protected by locks
- Final output only written when all threads complete

**Configuration:**
```bash
# Default: auto-detect CPU count
dtr export docs/ --parallel auto

# Explicit thread count
dtr export docs/ --parallel 4

# Serial (for debugging)
dtr export docs/ --parallel 1
```

## Error Handling Strategy

Errors are categorized by severity:

| Error Type | Example | Handling |
|-----------|---------|----------|
| **Fatal** | Invalid CLI args | Exit immediately (code 1) |
| **Path Error** | Input file doesn't exist | Skip file, report, continue |
| **Permission Error** | Can't write output | Fail with clear message |
| **Tool Error** | pdflatex not found | Guide user to install, skip format |
| **Format Error** | Invalid markdown syntax | Log warning, use best-effort output |

This means:
- ✅ Batch exports are resilient: 1 file fails, others succeed
- ✅ Tool issues are survivable: PDF unavailable, HTML still works
- ✅ Invalid syntax produces usable output instead of crashing

## Key Architectural Decision: Python, Not Java

DocTester CLI is written in Python, while the core library is Java. Why?

| Dimension | Python CLI | Java Alternative |
|-----------|-----------|------------------|
| Startup time | <100ms | 2-5 seconds (JVM warmup) |
| Cross-platform | Mac, Linux, Windows equally | Requires JVM on all platforms |
| Distribution | Single executable (PyInstaller) | JAR + JVM overhead |
| Shell integration | Native: bash completion, exit codes | Wrapped scripts needed |
| User accessibility | Easier for non-Java users | Locked to Java ecosystem |

**Tradeoff:** Maven integration requires bridging Python ↔ Java (solved via subprocess + artifact coordinates).

The architecture accepts this tradeoff because:
1. Most users interact via CLI, not Maven directly
2. Maven plugin is optional (CLI works standalone)
3. User experience (fast, responsive) matters more than implementation purity
