# How-to Guides

How-to guides are **task-oriented**. They assume you know what you want to do and show you how to do it. They are not tutorials — they don't explain concepts or hold your hand through learning.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

## Available Guides

### Getting Started (80/20 Fast Path)

| Guide | Purpose |
|---|---|
| [80/20 Essentials](80-20-essentials.md) | Master the core say* API in 45 minutes — start here if you're new |

### Java 25 Language Features

| Guide | Task |
|---|---|
| [Use Virtual Threads with Executors](use-virtual-threads.md) | Spawn lightweight concurrent tasks, benchmark with sayBenchmark |
| [Pattern Matching with Sealed Records](pattern-matching.md) | Deconstruct types exhaustively using pattern matching |
| [Text Blocks for Multiline Strings](text-blocks.md) | Write HTML, JSON, SQL, and templates naturally without escapes |
| [Switch Expressions](switch-expressions.md) | Replace if/else chains with exhaustive, type-safe matching |

### Visualization and Diagrams

| Guide | Task |
|---|---|
| [Generate Mermaid Diagrams](websockets-connection.md) | sayMermaid, sayClassDiagram — render any Mermaid DSL or auto-generate from classes |
| [Generate Class Diagrams](websockets-broadcast.md) | sayClassDiagram via reflection — document your type hierarchy |
| [Control Flow and Call Graphs](websockets-error-handling.md) | sayControlFlowGraph, sayCallGraph — visualize method structure |

### Contract and Coverage

| Guide | Task |
|---|---|
| [Verify Interface Contracts](grpc-unary.md) | sayContractVerification — verify all implementations against a contract |
| [Document Call Graphs](grpc-streaming.md) | sayCallGraph — visualize class-level call structure |
| [Document Coverage Matrix](grpc-error-handling.md) | sayDocCoverage — generate documentation coverage reports |

### Timeline and Charts

| Guide | Task |
|---|---|
| [Document Evolution Timeline](sse-subscription.md) | sayEvolutionTimeline — git log timeline for a class |
| [Render ASCII Charts](sse-parsing.md) | sayAsciiChart — Unicode bar charts from double arrays |
| [Benchmark Inline](sse-reconnection.md) | sayBenchmark — inline microbenchmarks documented automatically |

### Documentation Methods

| Guide | Task |
|---|---|
| [Document JSON Payloads](test-json-endpoints.md) | sayJson + java.net.http.HttpClient — document HTTP interactions |
| [Document Exception Handling](test-xml-endpoints.md) | sayException — structured exception documentation |
| [Validate Constraints](test-with-query-parameters.md) | Record components as constraint documentation |
| [Document Record Schemas](upload-files.md) | sayRecordComponents — auto-generate record field tables |
| [Capture Environment Profile](use-cookies.md) | sayEnvProfile — document Java version, OS, heap, DTR version |
| [Document Coverage](use-custom-headers.md) | sayDocCoverage — interface contract coverage matrix |

### Output and Configuration

| Guide | Task |
|---|---|
| [Configure Multi-Format Output](customize-html-output.md) | MultiRenderMachine — blog, LaTeX, slides, HTML from a single test |
| [Control What Gets Documented](control-documentation.md) | say vs plain methods, conditional documentation |
| [Advanced Rendering Formats](advanced-rendering-formats.md) | LaTeX/PDF, blog posts, OpenAPI specs, HTML slides |

### Integration

| Guide | Task |
|---|---|
| [Add DTR to a Maven Project](add-to-maven.md) | Dependencies and compiler config for v2.6.0 |
| [Integrate with Frameworks](integrate-with-frameworks.md) | Spring Boot, Arquillian, Ninja, Jetty |

### Performance and Measurement

| Guide | Task |
|---|---|
| [Benchmarking](benchmarking.md) | Measure real performance with sayBenchmark and System.nanoTime() |
| [Performance Tuning](performance-tuning.md) | Reduce build time, use reflection caching, sayBenchmark profiling |

### Migration

| Guide | Task |
|---|---|
| [Migrating to v2.5 / v2.6](MIGRATION_2.4_to_2.5.md) | From 2.4.x → 2.5.x → 2.6.0 including HTTP stack removal |

---

If you're new to DTR, start with the [Tutorials](../tutorials/index.md) instead.
