# DTR — Documentation Testing Runtime

> **Transform Java documentation into executable tests that prove code works as documented.**

[![CI Gate](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.seanchatmangpt.dtr/dtr-core.svg)](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core/versions)
[![Java 26](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/projects/jdk/26/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Version:** 2026.4.1 | **Java:** 26+ (`--enable-preview`)

---

## Quick Start

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.4.1</version>
    <scope>test</scope>
</dependency>
```

```java
import io.github.seanchatmangpt.dtr.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.*;

class MyDocTest {
    @DtrContextField private DtrContext ctx;

    @DtrTest void page() {
        ctx.say("Documentation is generated from passing tests");
    }
}
```

Run: `mvnd test` → Output: `target/docs/test-results/MyDocTest.md`

---

## 1. Make a Doc Page

Generate Markdown documentation from tests:

```java
class ApiDocTest {
    @DtrContextField private DtrContext ctx;

    @DtrTest void userEndpoint() {
        ctx.sayNextSection("GET /users/{id}");
        ctx.say("Returns user details by ID.");

        ctx.sayCode("""
            GET /users/123
            Authorization: Bearer token
            """, "http");

        ctx.sayKeyValue(Map.of(
            "Status", "200 OK",
            "Content-Type", "application/json"
        ));

        ctx.sayCode("""
            {
              "id": 123,
              "name": "Alice",
              "email": "alice@example.com"
            }
            """, "json");
    }
}
```

**Output:** `target/docs/test-results/ApiDocTest.md` — GitHub-ready Markdown

**Key methods:** `say()`, `sayNextSection()`, `sayCode()`, `sayKeyValue()`, `sayTable()`

---

## 2. Make an arXiv Paper

Generate LaTeX for academic papers:

```java
class PaperTest {
    @DtrContextField private DtrContext ctx;

    @DtrTest void abstractSection() {
        ctx.sayNextSection("Abstract");
        ctx.say("We present a novel approach to documentation testing...");
    }

    @DtrTest void methodology() {
        ctx.sayNextSection("Methodology");
        ctx.say("Our approach combines JUnit 5 with...");

        ctx.sayCite("knuth1984");
        ctx.sayCite("hatta2006", "p42");

        ctx.sayCode("""
            public void testFeature() {
                assertEquals(expected, actual);
            }
            """, "java");
    }

    @DtrTest void results() {
        ctx.sayNextSection("Results");
        ctx.sayTable(new String[][]{
            {"Method", "Accuracy", "Time"},
            {"Ours", "98.5%", "12ms"},
            {"Baseline", "92.1%", "45ms"}
        });
    }
}
```

**Output:** `target/docs/test-results/PaperTest.tex` — arXiv-ready LaTeX

**Key methods:** `sayCite()`, `sayTable()`, `sayCode()`, `sayRef()`

---

## 3. Make a Presentation

Generate Reveal.js slides:

```java
class TalkTest {
    @DtrContextField private DtrContext ctx;

    @DtrTest void titleSlide() {
        ctx.sayNextSection("Documentation Testing Runtime");
        ctx.sayTldr("Transform tests into living documentation");

        ctx.saySpeakerNote("Welcome everyone, today I'll present...");
        ctx.sayCallToAction("https://github.com/seanchatmangpt/dtr");
    }

    @DtrTest void problemSlide() {
        ctx.sayNextSection("The Problem");
        ctx.say("Documentation drifts from code");

        ctx.sayUnorderedList(List.of(
            "Docs written separately",
            "Code changes, docs don't",
            "Users encounter outdated info"
        ));

        ctx.sayWarning("This causes production bugs");
    }

    @DtrTest void solutionSlide() {
        ctx.sayNextSection("Our Solution");
        ctx.sayTweetable("Tests are the single source of truth");

        ctx.sayDocOnly("This text only appears in docs, not slides");

        ctx.saySlideOnly("""
            **DTR: Documentation Testing Runtime**

            - Write tests → Generate docs
            - Test passes → Docs accurate
            - Java 26 + JUnit 5
            """);
    }

    @DtrTest void demoSlide() {
        ctx.sayNextSection("Live Demo");
        ctx.sayCode("""
            @DtrTest void example() {
                ctx.say("Generated from test");
            }
            """, "java");
    }
}
```

**Output:** `target/docs/test-results/TalkTest.html` — Reveal.js presentation

**Key methods:** `saySlideOnly()`, `saySpeakerNote()`, `sayTldr()`, `sayTweetable()`, `sayCallToAction()`, `sayUnorderedList()`

---

## 4. All Output Formats

| Format | Output File | Use Case |
|--------|-------------|----------|
| **Markdown** | `*.md` | GitHub, GitLab, docs sites |
| **LaTeX** | `*.tex` | arXiv, academic papers |
| **Reveal.js** | `*.html` | Presentations, talks |
| **Blog** | `*.md` | Jekyll, Hugo, SSG |
| **HTML** | `*.html` | Standalone web pages |
| **JSON** | `*.json` | API docs, tooling |

---

## Complete say* API Reference

### Core

| Method | Description | Example |
|--------|-------------|---------|
| `say(String)` | Paragraph | `ctx.say("Text")` |
| `sayNextSection(String)` | H1 heading + TOC | `ctx.sayNextSection("API")` |
| `sayRaw(String)` | Raw markdown | `ctx.sayRaw("**raw**")` |

### Code & Data

| Method | Description | Example |
|--------|-------------|---------|
| `sayCode(String, String)` | Fenced code | `ctx.sayCode("x=1", "java")` |
| `sayTable(String[][])` | 2D array → table | `ctx.sayTable(new String[][]{{"A","B"},{"1","2"}})` |
| `sayKeyValue(Map)` | 2-column table | `ctx.sayKeyValue(Map.of("k","v"))` |
| `sayJson(Object)` | Pretty JSON | `ctx.sayJson(obj)` |
| `sayAssertions(Map)` | Assertion table | `ctx.sayAssertions(Map.of("check","✓"))` |

### Formatting

| Method | Description |
|--------|-------------|
| `sayNote(String)` | [!NOTE] block |
| `sayWarning(String)` | [!WARNING] block |
| `sayUnorderedList(List)` | Bullet list |
| `sayOrderedList(List)` | Numbered list |

### Cross-References

| Method | Description |
|--------|-------------|
| `sayRef(Class, String)` | Link to section |
| `sayCite(String)` | BibTeX citation |
| `sayFootnote(String)` | Footnote |

### Code Analysis

| Method | Description |
|--------|-------------|
| `sayCodeModel(Class)` | Class structure |
| `sayCallSite()` | Caller location |
| `sayClassHierarchy(Class)` | Inheritance tree |
| `sayControlFlowGraph(Method)` | Mermaid CFG |
| `sayCallGraph(Class)` | Mermaid call graph |

### Benchmarking

| Method | Description |
|--------|-------------|
| `sayBenchmark(String, Runnable)` | 50/500 rounds |
| `sayBenchmark(String, Runnable, int, int)` | Custom rounds |

### Diagrams

| Method | Description |
|--------|-------------|
| `sayMermaid(String)` | Raw Mermaid |
| `sayClassDiagram(Class...)` | Auto class diagram |

### Quality

| Method | Description |
|--------|-------------|
| `sayDocCoverage(Class...)` | Coverage report |
| `sayContractVerification(Class, Class...)` | Impl matrix |

### Presentation

| Method | Description |
|--------|-------------|
| `saySlideOnly(String)` | Slides-only |
| `sayDocOnly(String)` | Docs-only |
| `saySpeakerNote(String)` | Notes |
| `sayHeroImage(String)` | Hero |
| `sayTweetable(String)` | Quote |
| `sayTldr(String)` | Summary |
| `sayCallToAction(String)` | CTA |

### Testing

| Method | Description |
|--------|-------------|
| `sayAndAssertThat(String, T, Matcher)` | Assert + doc |
| `sayEnvProfile()` | System info |
| `sayException(Throwable)` | Error details |

### Specialized

| Method | Description |
|--------|-------------|
| `sayRecordComponents(Class)` | Record schema |
| `sayAsciiChart(String, double[], String[])` | Bar chart |
| `sayEvolutionTimeline(Class, int)` | Git log |
| `sayJavadoc(Method)` | Extract Javadoc |

---

## Annotations

| Annotation | Purpose |
|------------|---------|
| `@DtrTest` | `@Test` + `@ExtendWith(DtrExtension.class)` |
| `@DtrContextField` | Field injection for `DtrContext` |
| `@DtrConfig(outputDir="path")` | Configuration |

---

## Full Documentation (Diátaxis)

### 📚 Tutorials
- **[Basics](docs/tutorials/basics.md)** — 20-minute introduction
- **[HTTP Testing](docs/tutorials/http-testing.md)** — REST API docs
- **[Field Injection](docs/tutorials/field-injection-guide.md)** — Modern patterns

### 📖 How-To Guides
- **[80/20 Essentials](docs/how-to/80-20-essentials.md)** — Minimal path to productivity
- **[Benchmarking](docs/how-to/benchmarking.md)** — Performance measurement
- **[Advanced Rendering](docs/how-to/advanced-rendering-formats.md)** — LaTeX, slides, blogs

### 💡 Explanation
- **[Design Patterns](docs/explanation/80-20-design-patterns.md)** — Architecture decisions
- **[Architecture](docs/explanation/architecture.md)** — System design

### 📋 Reference
- **[Annotation Reference](docs/reference/annotation-reference.md)** — All annotations
- **[FAQ & Troubleshooting](docs/reference/FAQ_AND_TROUBLESHOOTING.md)** — Common issues
- **[Migration Guide](docs/MIGRATING.md)** — Version upgrades

---

## Links

- **[Maven Central](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)**
- **[GitHub](https://github.com/seanchatmangpt/dtr)**

---

**License:** Apache 2.0
