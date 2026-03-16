# DTR — Documentation Testing Runtime

> **Transform Java documentation into executable tests that prove code works as documented.**

[![CI Gate](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.seanchatmangpt.dtr/dtr-core.svg)](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core/versions)
[![Java 26](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/projects/jdk/26/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Version:** 2026.4.1 | **Java:** 26+ (`--enable-preview`)

---

## 80/20: The Most Efficient Pattern

```java
import io.github.seanchatmangpt.dtr.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.*;
import static org.hamcrest.Matchers.*;

class ApiTest {
    @DtrContextField private DtrContext ctx;

    @DtrTest void endpoint() {
        ctx.say("Returns user by ID");
        ctx.sayCode("GET /users/{id}", "http");
        ctx.sayKeyValue(Map.of(
            "Auth", "Bearer token",
            "Rate Limit", "100/hour"
        ));
        ctx.sayAndAssertThat("status", response.getStatus(), equalTo(200));
    }
}
```

**Setup:**

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.4.1</version>
    <scope>test</scope>
</dependency>
```

Run: `mvnd test` → Output: `target/docs/test-results/ApiTest.md`

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
| `sayCode(String, String)` | Fenced code block | `ctx.sayCode("x=1", "java")` |
| `sayTable(String[][])` | 2D array → table | `ctx.sayTable(new String[][]{{"A","B"},{"1","2"}})` |
| `sayKeyValue(Map)` | 2-column table | `ctx.sayKeyValue(Map.of("k","v"))` |
| `sayJson(Object)` | Pretty JSON | `ctx.sayJson(obj)` |
| `sayAssertions(Map)` | Assertion table | `ctx.sayAssertions(Map.of("check","✓ Pass"))` |

### Formatting

| Method | Description | Example |
|--------|-------------|---------|
| `sayNote(String)` | [!NOTE] block | `ctx.sayNote("info")` |
| `sayWarning(String)` | [!WARNING] block | `ctx.sayWarning("caution")` |
| `sayUnorderedList(List)` | Bullet list | `ctx.sayUnorderedList(List.of("a","b"))` |
| `sayOrderedList(List)` | Numbered list | `ctx.sayOrderedList(List.of("1","2"))` |

### Cross-References

| Method | Description | Example |
|--------|-------------|---------|
| `sayRef(DocTestRef)` | Link to DocTest | `ctx.sayRef(ref)` |
| `sayRef(Class, String)` | Link to class+anchor | `ctx.sayRef(MyClass.class, "api")` |
| `sayCite(String)` | BibTeX citation | `ctx.sayCite("smith2023")` |
| `sayCite(String, String)` | Citation + page | `ctx.sayCite("key", "p42")` |
| `sayFootnote(String)` | Footnote | `ctx.sayFootnote("text")` |

### Code Model (Reflection)

| Method | Description | Example |
|--------|-------------|---------|
| `sayCodeModel(Class)` | Class structure | `ctx.sayCodeModel(MyClass.class)` |
| `sayCodeModel(Method)` | Method signature | `ctx.sayCodeModel(method)` |
| `sayCallSite()` | Caller location | `ctx.sayCallSite()` |
| `sayAnnotationProfile(Class)` | All annotations | `ctx.sayAnnotationProfile(MyClass.class)` |
| `sayClassHierarchy(Class)` | Inheritance tree | `ctx.sayClassHierarchy(MyClass.class)` |
| `sayStringProfile(String)` | Text metrics | `ctx.sayStringProfile("text")` |
| `sayReflectiveDiff(Object, Object)` | Field comparison | `ctx.sayReflectiveDiff(before, after)` |

### Java 26 Code Reflection (JEP 516)

| Method | Description | Example |
|--------|-------------|---------|
| `sayControlFlowGraph(Method)` | Mermaid CFG | `ctx.sayControlFlowGraph(method)` |
| `sayCallGraph(Class)` | Mermaid call graph | `ctx.sayCallGraph(MyClass.class)` |
| `sayOpProfile(Method)` | Operation counts | `ctx.sayOpProfile(method)` |

### Benchmarking

| Method | Description | Example |
|--------|-------------|---------|
| `sayBenchmark(String, Runnable)` | 50 warmup / 500 measure | `ctx.sayBenchmark("label", () -> code)` |
| `sayBenchmark(String, Runnable, int, int)` | Custom rounds | `ctx.sayBenchmark("l", () -> c, 100, 1000)` |

### Diagrams

| Method | Description | Example |
|--------|-------------|---------|
| `sayMermaid(String)` | Raw Mermaid | `ctx.sayMermaid("flowchart LR-->A-->B")` |
| `sayClassDiagram(Class...)` | Auto class diagram | `ctx.sayClassDiagram(A.class, B.class)` |

### Quality & Coverage

| Method | Description | Example |
|--------|-------------|---------|
| `sayDocCoverage(Class...)` | Coverage report | `ctx.sayDocCoverage(A.class, B.class)` |
| `sayContractVerification(Class, Class...)` | Interface impl matrix | `ctx.sayContractVerification(IFace.class, Impl.class)` |

### Presentation-Specific

| Method | Description | Example |
|--------|-------------|---------|
| `saySlideOnly(String)` | Slides-only content | `ctx.saySlideOnly("slides only")` |
| `sayDocOnly(String)` | Docs-only content | `ctx.sayDocOnly("docs only")` |
| `saySpeakerNote(String)` | Presenter notes | `ctx.saySpeakerNote("note")` |
| `sayHeroImage(String)` | Hero image section | `ctx.sayHeroImage("alt text")` |
| `sayTweetable(String)` | Social quote box | `ctx.sayTweetable("quote")` |
| `sayTldr(String)` | TL;DR summary box | `ctx.sayTldr("summary")` |
| `sayCallToAction(String)` | CTA button/link | `ctx.sayCallToAction("https://...")` |

### Testing (Assertions + Documentation)

| Method | Description | Example |
|--------|-------------|---------|
| `sayAndAssertThat(String, T, Matcher)` | Assert + doc (generic) | `ctx.sayAndAssertThat("x", val, equalTo(1))` |
| `sayAndAssertThat(String, long, Matcher)` | Assert + doc (long) | `ctx.sayAndAssertThat("x", 42L, greaterThan(40L))` |
| `sayAndAssertThat(String, int, Matcher)` | Assert + doc (int) | `ctx.sayAndAssertThat("x", 42, equalTo(42))` |
| `sayAndAssertThat(String, boolean, Matcher)` | Assert + doc (boolean) | `ctx.sayAndAssertThat("x", true, is(true))` |

### Environment & Debugging

| Method | Description | Example |
|--------|-------------|---------|
| `sayEnvProfile()` | System snapshot | `ctx.sayEnvProfile()` |
| `sayException(Throwable)` | Exception details | `ctx.sayException(error)` |

### Specialized

| Method | Description | Example |
|--------|-------------|---------|
| `sayRecordComponents(Class)` | Record schema | `ctx.sayRecordComponents(MyRecord.class)` |
| `sayAsciiChart(String, double[], String[])` | Horizontal bar chart | `ctx.sayAsciiChart("x", vals, labels)` |
| `sayEvolutionTimeline(Class, int)` | Git log for file | `ctx.sayEvolutionTimeline(MyClass.class, 10)` |
| `sayJavadoc(Method)` | Extract Javadoc | `ctx.sayJavadoc(method)` |

---

## Annotations

| Annotation | Purpose |
|------------|---------|
| `@DtrTest` | `@Test` + `@ExtendWith(DtrExtension.class)` + auto-finish |
| `@DtrTest(autoFinish=true)` | Auto-call `ctx.finish()` after test |
| `@DtrTest(fileName="custom.md")` | Custom output filename |
| `@DtrContextField` | Field injection for `DtrContext` |
| `@DtrConfig(outputDir="path")` | Configuration override |

---

## Patterns

### Field Injection (Primary)

```java
class Test {
    @DtrContextField private DtrContext ctx;

    @DtrTest void test() { ctx.say("text"); }
}
```

### Parameter Injection

```java
class Test {
    @Test void test(DtrContext ctx) { ctx.say("text"); }
}
```

### Inheritance (Legacy)

```java
class Test extends DtrTest {
    @Test void test() { ctx.say("text"); }
}
```

---

## Full Documentation (Diátaxis)

### 📚 Tutorials
- **[Basics](docs/tutorials/basics.md)** — 20-minute introduction
- **[HTTP Testing](docs/tutorials/http-testing.md)** — REST API documentation
- **[Field Injection Guide](docs/tutorials/field-injection-guide.md)** — Modern patterns

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

## Output Formats

- **Markdown** — Primary output (GitHub-ready)
- **LaTeX** — Academic papers
- **Blog** — Jekyll, Hugo
- **Slides** — Reveal.js
- **HTML** — Standalone pages
- **JSON** — Machine-readable

---

## Links

- **[Maven Central](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)**
- **[GitHub](https://github.com/seanchatmangpt/dtr)**

---

**License:** Apache 2.0
