# Reference: DtrContext and DtrExtension API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.6.0

`DtrContext` is the primary API surface for writing DTR documentation tests. `DtrExtension` is the JUnit 5 extension that creates and manages the `DtrContext` lifecycle.

---

## DtrExtension

**Class:** `io.github.seanchatmangpt.dtr.core.DtrExtension`
**Implements:** `org.junit.jupiter.api.extension.Extension`

Register with:

```java
@ExtendWith(DtrExtension.class)
class MyDocTest { ... }
```

### Lifecycle

| Event | Action |
|-------|--------|
| Before test class | Initializes shared `RenderMachine` |
| Before each test | Injects `DtrContext` into test method parameters |
| After each test | Flushes method-level output |
| After test class | Calls `RenderMachine.finishAndWriteOut()` |

The `RenderMachine` is shared across all test methods in a class. A fresh `DtrContext` wrapper is provided per test method.

---

## DtrContext

**Class:** `io.github.seanchatmangpt.dtr.core.DtrContext`

Injected as a parameter by `DtrExtension`:

```java
@Test
void testExample(DtrContext ctx) {
    ctx.sayNextSection("Example");
    ctx.say("This documents the example feature.");
}
```

### say* methods

All 37 documentation output methods are available on `DtrContext`. Full signatures and descriptions are in [say* Core API Reference](request-api.md).

Quick listing:

**Core:** `say`, `sayNextSection`, `sayRaw`, `sayCode`, `sayJson`, `sayTable`, `sayWarning`, `sayNote`, `sayKeyValue`, `sayUnorderedList`, `sayOrderedList`, `sayAssertions`, `sayRef`, `sayCite` (2 overloads), `sayFootnote`

**JVM Introspection (v2.4.0+):** `sayCallSite`, `sayAnnotationProfile`, `sayClassHierarchy`, `sayStringProfile`, `sayReflectiveDiff`

**Code Reflection (v2.3.0+):** `sayCodeModel` (2 overloads), `sayControlFlowGraph`, `sayCallGraph`, `sayOpProfile`

**Benchmarking (v2.6.0):** `sayBenchmark` (2 overloads)

**Mermaid (v2.6.0):** `sayMermaid`, `sayClassDiagram`

**Coverage and Quality (v2.6.0):** `sayDocCoverage`, `sayContractVerification`, `sayEvolutionTimeline`

**Utility (v2.6.0):** `sayEnvProfile`, `sayRecordComponents`, `sayException`, `sayAsciiChart`

### RenderMachine access

#### `getRenderMachine()` → `RenderMachine`

Returns the current `RenderMachine` for this test class.

```java
RenderMachine rm = ctx.getRenderMachine();
```

#### `setRenderMachine(RenderMachine machine)` → `void`

Replaces the render machine for the remainder of the test class. Typically called in the first test method or a `@BeforeEach` helper.

```java
@Test
void setup(DtrContext ctx) {
    ctx.setRenderMachine(new MultiRenderMachine(
        new RenderMachineImpl(),
        new RenderMachineLatex(new ArXivTemplate(), new PdflatexStrategy())
    ));
}
```

---

## Complete test class template

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import io.github.seanchatmangpt.dtr.rendermachine.MultiRenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineLatex;
import io.github.seanchatmangpt.dtr.rendermachine.latex.IEEETemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.PdflatexStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

@ExtendWith(DtrExtension.class)
class MyApiDocTest {

    @Test
    void introduction(DtrContext ctx) {
        ctx.sayNextSection("Introduction");
        ctx.say("This document covers the v2.6.0 API.");
        ctx.sayNote("Requires Java 25+ with --enable-preview.");
    }

    @Test
    void apiSurface(DtrContext ctx) {
        ctx.sayNextSection("API Surface");
        ctx.sayTable(new String[][] {
            {"Method",  "Purpose"},
            {"say",     "Paragraph"},
            {"sayCode", "Code block"},
            {"sayJson", "JSON output"},
        });
    }

    @Test
    void benchmark(DtrContext ctx) {
        ctx.sayNextSection("Performance");
        ctx.sayBenchmark("ArrayList 1K add", () -> {
            var list = new java.util.ArrayList<String>();
            for (int i = 0; i < 1_000; i++) list.add("item" + i);
        }, 100, 1_000);
    }
}
```

---

## Thread safety

`DtrContext` is not thread-safe. Do not share a single `DtrContext` instance between concurrent threads. Maven Surefire's default (one thread per test class) is safe. The `MultiRenderMachine` handles internal parallelism transparently.

---

## Maven dependency

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.6.0</version>
    <scope>test</scope>
</dependency>
```

---

## See also

- [say* Core API Reference](request-api.md) — all 37 method signatures
- [RenderMachine API Reference](response-api.md) — rendering implementations
- [Configuration](configuration.md) — output directory, system properties
