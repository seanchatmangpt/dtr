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

class MyTest {
    @DtrContextField private DtrContext ctx;

    @DtrTest void test() {
        ctx.say("Documentation is generated from passing tests");
    }
}
```

Run: `mvnd test` → Output: `target/docs/test-results/MyTest.md`

---

## say* API Reference

### Core Methods

```java
ctx.say("text")                    // Paragraph
ctx.sayNextSection("Heading")      // H1 with TOC entry
ctx.sayRaw("raw *markdown*")       // Bypass formatting
```

### Code & Data

```java
ctx.sayCode("int x = 42;", "java")           // Fenced code block
ctx.sayTable(new String[][]{{"A","B"},{"1","2"}})  // Table
ctx.sayKeyValue(Map.of("key","value"))       // 2-column table
ctx.sayJson(object)                         // Pretty JSON
```

### Formatting

```java
ctx.sayNote("context")           // [!NOTE] block
ctx.sayWarning("caution")        // [!WARNING] block
ctx.sayUnorderedList(List.of("a","b"))  // Bullet list
ctx.sayOrderedList(List.of("1","2"))    // Numbered list
```

### Cross-References

```java
ctx.sayRef(MyClass.class, "anchor")      // Link to section
ctx.sayCite("bibtex-key")                // Bibliography
ctx.sayFootnote("text")                  // Footnote
```

### Code Model (Reflection)

```java
ctx.sayCodeModel(MyClass.class)          // Class structure
ctx.sayCodeModel(method)                 // Method signature
ctx.sayCallSite()                        // Caller location
ctx.sayAnnotationProfile(MyClass.class)  // All annotations
ctx.sayClassHierarchy(MyClass.class)     // Inheritance tree
ctx.sayStringProfile("text")             // Word count, Unicode metrics
ctx.sayReflectiveDiff(before, after)     // Field comparison table
```

### Java 26 Code Reflection (JEP 516)

```java
ctx.sayControlFlowGraph(method)          // Mermaid flowchart
ctx.sayCallGraph(MyClass.class)          // Mermaid call graph
ctx.sayOpProfile(method)                 // Operation counts
```

### Benchmarking

```java
ctx.sayBenchmark("label", () -> { code; })                          // 50/500 rounds
ctx.sayBenchmark("label", () -> code, 100, 1000)                    // Custom rounds
```

### Diagrams

```java
ctx.sayMermaid("flowchart LR-->A-->B")      // Raw Mermaid
ctx.sayClassDiagram(A.class, B.class)       // Auto class diagram
```

### Quality & Coverage

```java
ctx.sayDocCoverage(A.class, B.class)        // Coverage report
ctx.sayContractVerification(Interface.class, Impl.class)  // Impl matrix
```

### Presentation-Specific

```java
ctx.saySlideOnly("slides only")             // Slides-only content
ctx.sayDocOnly("docs only")                 // Docs-only content
ctx.saySpeakerNote("presenter notes")       // Speaker notes
ctx.sayHeroImage("alt text")                // Hero image
ctx.sayTweetable("social quote")            // Tweet box
ctx.sayTldr("summary")                      // TL;DR box
ctx.sayCallToAction("https://example.com")  // CTA button/link
```

### Testing

```java
ctx.sayAndAssertThat("label", actual, matcher)           // Assert + doc
ctx.sayAndAssertThat("label", 42L, greaterThan(40L))     // Primitive long
ctx.sayAndAssertThat("label", true, is(true))            // Primitive boolean
```

### Environment & Debugging

```java
ctx.sayEnvProfile()                 // Java version, OS, heap
ctx.sayException(throwable)         // Exception type, cause, stack trace
ctx.sayAssertions(Map.of("check","result"))  // Assertion table
```

### Specialized

```java
ctx.sayRecordComponents(MyRecord.class)      // Record schema
ctx.sayAsciiChart("label", values, labels)   // Horizontal bar chart
ctx.sayEvolutionTimeline(MyClass.class, 10)  // Git log for file
ctx.sayJavadoc(method)                       // Extract Javadoc
```

---

## 80/20 Essential Methods

Most documentation uses just 8 methods:

| Method | Use For |
|--------|---------|
| `say()` | Paragraphs |
| `sayCode()` | Code blocks |
| `sayTable()` | Data tables |
| `sayNextSection()` | Headings |
| `sayKeyValue()` | Metadata |
| `sayNote()` | Context notes |
| `sayWarning()` | Warnings |
| `sayRef()` | Cross-references |

---

## Annotations

```java
@DtrTest                              // @Test + @ExtendWith(DtrExtension.class)
@DtrTest(autoFinish=true)             // Auto-call ctx.finish()
@DtrTest(fileName="custom.md")        // Custom output file
@DtrContextField                      // Field injection for DtrContext
@DtrConfig(outputDir="path")          // Configuration
```

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
class Test extends DtrTest {  // Deprecated
    @Test void test() { ctx.say("text"); }
}
```

---

## Output Formats

- **Markdown** — Primary output
- **LaTeX** — Academic papers
- **Blog** — Jekyll, Hugo
- **Slides** — Reveal.js
- **HTML** — Standalone pages
- **JSON** — Machine-readable

---

## Links

- **[Maven Central](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)**
- **[Documentation](docs/)**
- **[GitHub](https://github.com/seanchatmangpt/dtr)**

---

## License

Apache 2.0
