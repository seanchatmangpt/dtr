# Reference: DtrContext and DtrTest API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.6.0

`DtrContext` is the parameter-injection API for JUnit 5 tests. `DtrExtension` is the JUnit 5 extension that manages the documentation lifecycle. Together they replace the v2.4.x `DTR` abstract base class.

---

## Quick start

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyDocTest {

    @Test
    void example(DtrContext ctx) {
        ctx.sayNextSection("My Feature");
        ctx.say("This section documents the feature.");
        ctx.sayCode("record User(String name) {}", "java");
    }
}
```

**Output:** `target/docs/test-results/MyDocTest.md` (and `.html`, `.tex`, `.json`)

---

## DtrExtension

**Class:** `io.github.seanchatmangpt.dtr.core.DtrExtension`

Register on the test class with `@ExtendWith(DtrExtension.class)`.

### Extension lifecycle

| JUnit event | DtrExtension action |
|-------------|---------------------|
| Before all tests in class | Creates shared `RenderMachine` (default: `RenderMachineImpl`) |
| Before each test method | Creates `DtrContext` bound to the shared `RenderMachine` |
| Parameter resolution | Injects `DtrContext` into `@Test` method parameters |
| After all tests in class | Calls `RenderMachine.finishAndWriteOut()` → writes output files |

The `RenderMachine` is shared across the entire test class (to produce a single coherent document per class). A fresh `DtrContext` wrapper is provided per test method, but all methods write to the same machine.

---

## DtrContext

**Class:** `io.github.seanchatmangpt.dtr.core.DtrContext`

Provides access to all 37 `say*` documentation methods and the `RenderMachine` instance.

### say* methods

All 37 documentation output methods are documented in [say* Core API Reference](request-api.md). The method groups are:

| Group | Methods | Since |
|-------|---------|-------|
| Core | `say`, `sayNextSection`, `sayRaw`, `sayCode`, `sayJson`, `sayTable`, `sayWarning`, `sayNote`, `sayKeyValue`, `sayUnorderedList`, `sayOrderedList`, `sayAssertions`, `sayRef`, `sayCite` (×2), `sayFootnote` | v2.0 |
| JVM Introspection | `sayCallSite`, `sayAnnotationProfile`, `sayClassHierarchy`, `sayStringProfile`, `sayReflectiveDiff` | v2.4.0 |
| Code Reflection | `sayCodeModel` (×2), `sayControlFlowGraph`, `sayCallGraph`, `sayOpProfile` | v2.3.0 |
| Benchmarking | `sayBenchmark` (×2) | v2.6.0 |
| Mermaid | `sayMermaid`, `sayClassDiagram` | v2.6.0 |
| Coverage and Quality | `sayDocCoverage`, `sayContractVerification`, `sayEvolutionTimeline` | v2.6.0 |
| Utility | `sayEnvProfile`, `sayRecordComponents`, `sayException`, `sayAsciiChart` | v2.6.0 |

### RenderMachine methods

#### `getRenderMachine()` → `RenderMachine`

Returns the current render machine for the test class.

```java
RenderMachine rm = ctx.getRenderMachine();
```

#### `setRenderMachine(RenderMachine machine)` → `void`

Replaces the render machine. All subsequent `say*` calls in all test methods of the class use the new machine.

```java
@Test
void configureOutput(DtrContext ctx) {
    // Switch to multi-format output for this entire test class
    ctx.setRenderMachine(new MultiRenderMachine(
        new RenderMachineImpl(),
        new RenderMachineLatex(new ArXivTemplate(), new LatexmkStrategy())
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
import io.github.seanchatmangpt.dtr.rendermachine.latex.ACMTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.PdflatexStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

@ExtendWith(DtrExtension.class)
class FeatureDocTest {

    @Test
    void overview(DtrContext ctx) {
        ctx.sayNextSection("Feature Overview");
        ctx.say("This document describes the v2.6.0 release.");
        ctx.sayNote("Requires Java 25+ with --enable-preview.");
        ctx.sayWarning("API incompatible with DTR 2.5.x — see changelog.");
    }

    @Test
    void apiSurface(DtrContext ctx) {
        ctx.sayNextSection("API Surface");
        ctx.sayTable(new String[][] {
            {"Group",        "Method count"},
            {"Core",         "16"},
            {"Benchmarking", "2"},
            {"Mermaid",      "2"},
            {"Coverage",     "3"},
            {"Utility",      "4"},
        });
        ctx.sayUnorderedList(List.of(
            "37 total say* methods",
            "RenderMachine: abstract, not sealed",
            "MultiRenderMachine virtual thread dispatch"
        ));
    }

    @Test
    void benchmarkExample(DtrContext ctx) {
        ctx.sayNextSection("Benchmark");
        ctx.sayEnvProfile();
        ctx.sayBenchmark("ArrayList 1K add", () -> {
            var list = new java.util.ArrayList<String>();
            for (int i = 0; i < 1_000; i++) list.add("item" + i);
        }, 100, 1_000);
    }

    @Test
    void diagramExample(DtrContext ctx) {
        ctx.sayNextSection("Architecture");
        ctx.sayMermaid("""
            flowchart LR
                Test --> DtrContext
                DtrContext --> RenderMachine
                RenderMachine --> Markdown
                RenderMachine --> LaTeX
                RenderMachine --> Blog
            """);
    }
}
```

---

## Output location

All output files are written to `target/docs/test-results/`:

```
target/docs/test-results/
├── FeatureDocTest.md
├── FeatureDocTest.html
├── FeatureDocTest.tex
└── FeatureDocTest.json
```

---

## Thread safety

`DtrContext` is not thread-safe. Do not share a single instance across concurrent threads. Maven Surefire's default (one thread per test class) is safe. The `MultiRenderMachine` handles internal fan-out parallelism transparently.

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

## Migration from v2.4.x DTR base class

In v2.5.0 the `DTR` abstract base class and JUnit 4 lifecycle were replaced by `DtrContext` + `DtrExtension`.

| v2.4.x (DTR base class) | v2.6.0 (DtrContext) |
|-------------------------|---------------------|
| `extends DTR` | `@ExtendWith(DtrExtension.class)` |
| `sayNextSection("x")` | `ctx.sayNextSection("x")` |
| `say("x")` | `ctx.say("x")` |
| `getRenderMachine()` | `ctx.getRenderMachine()` |
| `setRenderMachine(m)` | `ctx.setRenderMachine(m)` |
| `@Before setupForTestCaseMethod()` | Handled automatically by `DtrExtension` |
| `@AfterClass finishDocTest()` | Handled automatically by `DtrExtension` |

---

## See also

- [say* Core API Reference](request-api.md) — all 37 method signatures
- [RenderMachine API](rendermachine-api.md) — rendering implementations
- [Configuration](configuration.md) — output directory, Maven settings
