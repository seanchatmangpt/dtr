# Explanation: Architecture

> **Note:** This is the original deep-dive into DTR 2.6.0 internals. For the comprehensive architecture overview, see **[ARCHITECTURE.md](../ARCHITECTURE.md)**. This document focuses on implementation philosophy and new module details.

This document describes DTR's module structure, class design, and the internal event pipeline that transforms test method calls into multi-format documentation output.

---

## What DTR Is (and Is Not)

DTR 2.6.0 is a **pure documentation-generation library**. It has no HTTP client, no network stack, and no server coupling. Its single job is to receive structured `say*()` calls from JUnit 5 tests and render them into Markdown, LaTeX, HTML, JSON, and other formats simultaneously.

This boundary is intentional. HTTP testing — making actual requests, inspecting responses, verifying contracts — is your responsibility using tools designed for it: `java.net.http.HttpClient`, RestAssured, Spring MockMvc. DTR then documents what you observed, with precision derived from your running code.

---

## Module Structure

```
dtr/
├── pom.xml                          # Parent POM
├── dtr-core/                        # The library (published to Maven Central)
│   └── src/main/java/io/github/seanchatmangpt/dtr/
│       ├── DtrExtension.java        # JUnit 5 extension entrypoint
│       ├── DtrContext.java          # Test-facing API (all say* methods)
│       ├── SayEvent.java            # Sealed event hierarchy (13 record types)
│       ├── rendermachine/
│       │   ├── RenderMachineCommands.java  # Output method interface
│       │   ├── RenderMachine.java          # Abstract base (template method)
│       │   └── MultiRenderMachine.java     # Parallel virtual thread dispatch
│       ├── reflectiontoolkit/       # Introspection-based say* implementations
│       ├── contract/                # Contract verification methods
│       ├── coverage/                # Documentation coverage reporting
│       ├── evolution/               # Git evolution timeline methods
│       ├── diagram/                 # Mermaid diagram generation
│       └── benchmark/               # Inline benchmarking methods
└── dtr-integration-test/            # Integration test suite (not published)
    └── src/test/java/
        └── PhDThesisDocTest.java    # Canonical usage example
```

**Maven Coordinate:** `io.github.seanchatmangpt.dtr:dtr-core:2.6.0`

---

## Class Hierarchy

```
JUnit 5 test class
    │
    │ @ExtendWith(DtrExtension.class)
    ▼
DtrExtension                         ← JUnit 5 lifecycle hooks
    │  injects
    ▼
DtrContext                           ← Test-facing API
    │  emits SayEvents to
    ▼
RenderMachineCommands (interface)    ← Minimal output contract
    │  implemented by
    ▼
RenderMachine (abstract)             ← Template method base
    ├── MarkdownRenderMachine
    ├── LatexRenderMachine
    ├── HtmlRenderMachine
    ├── JsonRenderMachine
    └── MultiRenderMachine           ← Dispatches to all above in parallel
```

`RenderMachine` has been abstract (not sealed) since v2.5.0. This is deliberate: the set of render targets is open. Users may implement `RenderMachine` to emit Confluence pages, OpenAPI fragments, blog posts, or custom formats, without modifying the library. Sealed would close that door unnecessarily.

---

## The SayEvent Sealed Hierarchy

When `DtrContext.say("text")` is called, DTR does not immediately write output. Instead, it emits a `SayEvent` — a value object representing the intent — into the render pipeline.

`SayEvent` is a sealed interface with 13 record implementations:

```
sealed interface SayEvent permits
    SayEvent.Text,
    SayEvent.Section,
    SayEvent.Code,
    SayEvent.Table,
    SayEvent.Json,
    SayEvent.Warning,
    SayEvent.Note,
    SayEvent.Diagram,
    SayEvent.Benchmark,
    SayEvent.RecordSchema,
    SayEvent.CallSite,
    SayEvent.AnnotationProfile,
    SayEvent.Coverage
```

Why sealed here? Because `RenderMachine` implementations must handle every event type — if a new event is added, every render target must be updated to produce output for it. The sealed constraint makes omissions a compile-time error, not a silent blank in the rendered output. This is the canonical use case for sealed: a closed, exhaustive set where pattern matching must be complete.

---

## The Event Pipeline

```
DtrContext.say*(args)
    │
    │  creates a SayEvent record
    ▼
EventQueue (thread-safe, per-test)
    │
    │  on finishAndWriteOut()
    ▼
MultiRenderMachine
    │
    │  dispatches each event to all render machines
    │  using Executors.newVirtualThreadPerTaskExecutor()
    ▼
┌─────────────────────────────────────────────┐
│  MarkdownRenderMachine   │  pattern match    │
│  LatexRenderMachine      │  on SayEvent      │
│  HtmlRenderMachine       │  sealed hierarchy │
│  JsonRenderMachine       │  (all in parallel)│
└─────────────────────────────────────────────┘
    │
    ▼
target/docs/test-results/
    ├── PhDThesisDocTest.md
    ├── PhDThesisDocTest.tex
    ├── PhDThesisDocTest.html
    └── PhDThesisDocTest.json
```

Virtual threads make parallel rendering practical: each render machine runs on its own virtual thread, so disk I/O in one format does not delay output in another.

---

## MultiRenderMachine

`MultiRenderMachine` receives a list of `RenderMachine` instances at construction time and dispatches every incoming `SayEvent` to all of them concurrently using a virtual thread executor:

```java
// Conceptual structure
class MultiRenderMachine extends RenderMachine {

    private final List<RenderMachine> targets;

    @Override
    public void dispatch(SayEvent event) {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (RenderMachine target : targets) {
                executor.submit(() -> target.dispatch(event));
            }
        }
    }
}
```

Because `SayEvent` records are immutable, sharing them across virtual threads requires no synchronization.

---

## New Modules in v2.6.0

### `reflectiontoolkit`

Provides introspection-based `say*` methods:
- `sayRecordComponents(Class<?>)` — documents a record's components via `getRecordComponents()`
- `sayClassHierarchy(Class<?>)` — documents sealed/abstract hierarchies via reflection
- `sayAnnotationProfile(Class<?>)` — documents what annotations a class carries
- `sayStringProfile(Class<?>)` — documents string constants via field reflection
- `sayReflectiveDiff(Object, Object)` — compares two objects field-by-field, documents differences

All five methods cache reflection results in a `ConcurrentHashMap<Class<?>, Object>`. The first call costs ~150µs; subsequent calls cost ~50ns.

### `contract`

Provides `sayContractVerification(Class<?>, Class<?>)` — verifies and documents that a class conforms to an expected interface contract, emitting a `SayEvent.Coverage` event.

### `coverage`

Provides `sayCoverageReport(DtrContext)` — introspects which `say*` method families were used in the current test and documents coverage gaps.

### `evolution`

Provides `sayGitEvolution(String path)` — executes `git log` on the specified path and documents the commit history as a timeline table.

### `diagram`

Provides `sayMermaid(String diagram)` — emits a Mermaid diagram block that renders in GitHub Markdown, GitLab, and Docusaurus.

### `benchmark`

Provides `sayBenchmark(Runnable, int iterations)` — runs the given lambda `iterations` times using virtual thread batches to reduce JIT cold-start bias, then documents the mean, min, max, and standard deviation as a table.

---

## Lifecycle Details

### Per Test Class

`DtrExtension` implements `BeforeAllCallback` and `AfterAllCallback`:

- `beforeAll`: A `MultiRenderMachine` is created for the class, wired to all configured output targets.
- `afterAll`: `finishAndWriteOut()` is called on the `MultiRenderMachine`, which flushes accumulated events to all render targets in parallel and writes output files.

### Per Test Method

`DtrExtension` implements `BeforeEachCallback` and `AfterEachCallback`:

- `beforeEach`: A fresh `DtrContext` is injected into the test method via the `DtrContext` parameter.
- `afterEach`: Any pending section markers or open blocks are closed.

### Java 26 Requirement

DTR requires `--enable-preview` because it uses the Code Reflection API (Project Babylon, JEP 516). This API is used by `sayCallSite()` to capture the exact source location where documentation was generated — file name, line number, method name — without requiring a stack walk at runtime. The flag is not optional.

---

## Dependencies in dtr-core

DTR 2.6.0 adds 14 new capabilities with **zero new external dependencies**. The dependency list is identical to v2.5.0:

| Dependency | Purpose |
|---|---|
| `junit:junit-jupiter:5.x` | JUnit 5 extension API |
| `jackson-databind` | JSON rendering |
| `slf4j-api` | Logging |

Reflection, benchmarking, git log parsing, Mermaid diagram emission, and introspection caching all use the Java standard library.

---

## Extension Points

DTR has two primary extension points:

### 1. Implement `RenderMachine`

Subclass `RenderMachine` and override the template methods for each `SayEvent` type. Register your implementation by passing it to `MultiRenderMachine`. Common uses:
- Emit to Confluence pages
- Generate OpenAPI fragments
- Produce custom Markdown flavors
- Write to a documentation database

### 2. Contribute `SayEvent` Producers

Because `DtrContext` is a concrete class injected by the extension, you can wrap or extend it to emit custom events. Advanced integrations post new `SayEvent` subtypes — though adding a new permitted subtype requires updating all `RenderMachine` implementations that pattern-match on the sealed hierarchy.
