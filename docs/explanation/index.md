# Explanation

Explanation is **understanding-oriented**. These documents discuss concepts, design decisions, and background — not how to do things, but why things work the way they do.

## Available Explanations

### DTR Architecture and Design

| Document | What it explains |
|---|---|
| [Architecture](architecture.md) | Module structure, class hierarchy, SayEvent pipeline, and extension points in DTR 2.6.0 |
| [How DTR Works](how-doctester-works.md) | The full lifecycle: from test method → SayEvent → MultiRenderMachine → output files |
| [80/20 Design Patterns](80-20-design-patterns.md) | The Blue Ocean methodology: 14 new capabilities, 0 new dependencies, and why each capability exists |

### Documentation Philosophy

| Document | What it explains |
|---|---|
| [Documentation Philosophy](documentation-philosophy.md) | Living documentation, drift-proof accuracy, Blue Ocean innovation, and why HTTP testing was removed |
| [Drift-Proof Documentation via JVM Introspection](realtime-protocols-philosophy.md) | How reflection-based methods eliminate documentation drift; the provenance problem and its solution |

### Java 26 Design Philosophy

| Document | What it explains |
|---|---|
| [Java 26 Design Philosophy](java25-design-philosophy.md) | How records, sealed classes, virtual threads, pattern matching, and `--enable-preview` work together in DTR |
| [Why Virtual Threads Matter](virtual-threads-philosophy.md) | The design philosophy behind virtual threads; how DTR uses them in MultiRenderMachine and sayBenchmark |
| [Why Records and Sealed Classes](records-sealed-philosophy.md) | Why SayEvent is sealed (exhaustiveness), why RenderMachine is abstract (extensibility), and how sayRecordComponents works |

---

If you are trying to accomplish something specific, the [How-to Guides](../how-to/index.md) are more actionable. If you are new to DTR, start with the [Tutorials](../tutorials/index.md).
