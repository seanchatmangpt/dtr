# Explanation

Explanation is **understanding-oriented**. These documents discuss concepts, design decisions, and background — not how to do things, but **why** things work the way they do.

**DTR version:** 2.6.0 | **Java:** 26+ with `--enable-preview`

## Available Explanations

### DTR Architecture and Design

| Document | What it explains |
|---|---|
| [Architecture](../ARCHITECTURE.md) | **Comprehensive architecture overview** — module structure, class hierarchy, SayEvent pipeline, data flow, output formats, and extension points. This is the primary architecture document. |
| [Architecture (Legacy)](architecture.md) | Original deep-dive into DTR 2.6.0 internals — class design, event pipeline, MultiRenderMachine, and new modules. Overlaps with ARCHITECTURE.md but focuses on internal implementation details. |
| [How DTR Works](how-doctester-works.md) | The complete lifecycle: from test method execution → SayEvent creation → MultiRenderMachine parallel dispatch → multi-format output generation |
| [80/20 Design Patterns](80-20-design-patterns.md) | The Blue Ocean methodology: how DTR 2.6.0 added 14 new capabilities with 0 new dependencies, and the design principles behind each feature |

### Documentation Philosophy

| Document | What it explains |
|---|---|
| [Documentation Philosophy](documentation-philosophy.md) | Living documentation, drift-proof accuracy, why facts should derive from code not developers, and the rationale for removing HTTP testing in v2.6.0 |
| [Drift-Proof Documentation via JVM Introspection](realtime-protocols-philosophy.md) | How reflection-based methods (`sayRecordComponents`, `sayClassHierarchy`, etc.) eliminate documentation drift; the provenance problem and its solution via Code Reflection API (JEP 516) |

### Java 26 Design Philosophy

| Document | What it explains |
|---|---|
| [Java 26 Design Philosophy](java25-design-philosophy.md) | How records, sealed classes, virtual threads, pattern matching, and `--enable-preview` work together in DTR — why `--enable-preview` is required for Code Reflection API (JEP 516) |
| [Why Virtual Threads Matter](virtual-threads-philosophy.md) | The design philosophy behind virtual threads; how DTR uses them in `MultiRenderMachine` for parallel rendering and `sayBenchmark` for warmup batching |
| [Why Records and Sealed Classes](records-sealed-philosophy.md) | Why `SayEvent` is sealed (exhaustive pattern matching), why `RenderMachine` is abstract (open for extension), and how `sayRecordComponents` leverages record introspection |

---

## Cross-References

### New Documentation Structure (2.6.0+)

The documentation was reorganized to follow the [Diataxis framework](https://diataxis.fr/):

- **[ARCHITECTURE.md](../ARCHITECTURE.md)** — Comprehensive architecture overview (new in 2.6.0)
- **[Tutorials](../tutorials/index.md)** — Step-by-step learning guides (new in 2.6.0)
- **[How-to Guides](../how-to/index.md)** — Task-focused recipes and patterns
- **[Reference](../reference/index.md)** — Complete API documentation
- **[Examples](../EXAMPLES.md)** — Real-world usage examples

### Key Breaking Changes in v2.6.0

**HTTP/WebSocket/gRPC Protocols Removed:**

Version 2.6.0 removed the entire HTTP testing stack, including:
- `sayAndMakeRequest`, `Request`, `Response`, `TestBrowser` classes
- WebSocket and Server-Sent Events (SSE) support
- gRPC streaming protocol support

**Rationale:** DTR is now a **pure documentation-generation library**. HTTP testing should use dedicated tools like `java.net.http.HttpClient`, RestAssured, or Spring MockMvc. DTR documents what you observe.

See [Documentation Philosophy](documentation-philosophy.md) for the full explanation.

**Migration Path:** See [MIGRATING.md](../MIGRATING.md) for upgrade instructions from 2.5.x to 2.6.0.

---

## Related Reading

- **[Tutorials](../tutorials/index.md)** — Learn by doing with hands-on guides
  - [Your First DocTest](../tutorials/your-first-doctest.md) — Get started with DTR
  - [Java 26 Features](../tutorials/java26-features.md) — Records, sealed classes, virtual threads
  - [Performance](../tutorials/performance.md) — Benchmarking and optimization
  - [Diagrams](../tutorials/diagrams.md) — Mermaid diagrams and visualization
  - [HTTP Testing](../tutorials/http-testing.md) — Using `java.net.http.HttpClient` with DTR
- **[Examples](../EXAMPLES.md)** — Real-world code examples and patterns
- **[CHANGELOG.md](../CHANGELOG.md)** — Full version history and changes

---

**Navigation:** [Tutorials](../tutorials/index.md) • [How-to Guides](../how-to/index.md) • [Reference](../reference/index.md) • [Contributing](../contributing/index.md)
