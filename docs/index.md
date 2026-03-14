# DTR Documentation

DTR (Documentation Testing Runtime) is a Java 25 library that generates rich Markdown, LaTeX, HTML, and OpenAPI documentation directly from JUnit 5 tests via a declarative `say*` API.

**Version:** 2.6.0 | **Maven:** `io.github.seanchatmangpt.dtr:dtr-core:2.6.0` | **Java:** 25 + `--enable-preview`

---

## What's New in 2.6.0

DTR 2.6.0 is a significant release focused on pure documentation generation.

**14 new `say*` method signatures:**

| Method | Purpose |
|---|---|
| `sayBenchmark(BenchmarkResult)` | Render JMH benchmark results |
| `sayBenchmark(String, Runnable, int)` | Inline microbenchmark with iteration count |
| `sayMermaid(String)` | Embed Mermaid diagram markup |
| `sayClassDiagram(Class<?>...)` | Auto-generate UML class diagram |
| `sayControlFlowGraph(Method)` | Render control flow graph for a method |
| `sayCallGraph(Class<?>)` | Render call graph for a class |
| `sayOpProfile(String, LongSupplier)` | Capture and document operation profile |
| `sayDocCoverage(Class<?>)` | Report doc coverage for a class |
| `sayEnvProfile()` | Document JVM and OS environment |
| `sayRecordComponents(Record)` | Table of a record's components and values |
| `sayException(Throwable)` | Document an exception with stack trace |
| `sayAsciiChart(String, double[])` | Render an ASCII bar chart |
| `sayContractVerification(Object)` | Document contract verification result |
| `sayEvolutionTimeline(Class<?>)` | Render git-history evolution timeline |

**HTTP stack removed:** `sayAndMakeRequest`, `sayAndAssertThat`, `Request`, `Response`, `TestBrowser`, WebSocket, and SSE classes have been removed. DTR is now a pure documentation-generation library. If you depend on these, stay on 2.5.x or migrate to a dedicated HTTP testing library.

---

## Why DTR?

- **Docs that can't drift** — documentation is produced by the same code that tests it
- **No template engine** — write Java, get Markdown, LaTeX, HTML, and OpenAPI
- **Virtual-thread native** — `MultiRenderMachine` dispatches to output engines concurrently
- **Zero new dependencies** — 2.6.0 added 14 methods with no new runtime dependencies
- **Java 25 idiomatic** — records, sealed classes, pattern matching throughout

---

## Quick Start

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.6.0</version>
    <scope>test</scope>
</dependency>
```

Write a test:

```java
@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void documentMySystem(DtrContext ctx) {
        ctx.sayNextSection("System Overview");
        ctx.say("This section documents the core data model.");
        ctx.sayRecordComponents(new User("alice", 42));
        ctx.sayEnvProfile();
    }
}
```

Run and view output:

```bash
mvnd test -pl dtr-integration-test -Dtest=MyDocTest
cat target/docs/test-results/MyDocTest.md
```

---

## Documentation Map

DTR's documentation follows the [Diataxis](https://diataxis.fr/) framework:

| Section | Purpose | Start here if... |
|---|---|---|
| [Tutorials](tutorials/index.md) | Step-by-step learning | You are new to DTR |
| [How-to Guides](how-to/index.md) | Task-focused recipes | You know what you want to do |
| [Reference](reference/index.md) | Complete API and configuration docs | You need to look something up |
| [Explanation](explanation/index.md) | Concepts and design decisions | You want to understand why |
| [Contributing](contributing/index.md) | Developer guide | You want to contribute to DTR |

---

## Further Reading

- [CHANGELOG.md](../CHANGELOG.md) — full version history
- [README.md](../README.md) — project overview and license
