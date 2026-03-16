# Reference: DtrTest API

**Primary Approach:** Field injection with `@DtrContextField` + `@DtrTest` annotation (recommended)
**Legacy Approach:** Extending `DtrTest` (deprecated since 2026.4.1)

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2026.4.1

`DtrContext` is the parameter-injection API for JUnit Jupiter 6 tests. `DtrExtension` is the JUnit Jupiter 6 extension that manages the documentation lifecycle. **Field injection with `@DtrContextField` is the primary approach, providing cleaner code and better test structure than the legacy `extends DtrTest` pattern.** The `@DtrTest` annotation enables composite test scenarios and metadata management.

---

## Quick start

### Recommended: Field Injection with @DtrTest Annotation

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import org.junit.jupiter.api.Test;

@DtrTest(
    description = "User documentation tests for the 2026.4.1 release",
    tags = {"user-management", "authentication"}
)
class UserDocTest {

    @DtrContextField
    private DtrContext ctx;

    @Test
    void userRegistration() {
        ctx.sayNextSection("User Registration");
        ctx.say("This section documents the user registration feature introduced in 2026.4.1.");
        ctx.sayCode("record User(String name, String email) {}", "java");
    }
}
```

### Alternative: Parameter Injection Approach

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class ParamInjectionDocTest {

    @Test
    void example(DtrContext ctx) {
        ctx.sayNextSection("Parameter Injection");
        ctx.say("This section documents the parameter injection approach.");
        ctx.sayCode("record User(String name) {}", "java");
    }
}
```

**Output:** `target/docs/test-results/ParamInjectionDocTest.md` (and `.html`, `.tex`, `.json`)

**Benefits of Field Injection:**
- Cleaner test method signatures (no parameter noise)
- Better IDE support and autocomplete
- More readable test code
- Supports `@DtrTest` for metadata and composite test scenarios
- Avoids repetitive parameter passing across multiple test methods

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

Provides access to all 50+ `say*` documentation methods and the `RenderMachine` instance.

### say* methods

All 50+ documentation output methods are documented in [say* Core API Reference](say-api-methods.md). The method groups are:

| Group | Methods | Since |
|-------|---------|-------|
| Core | `say`, `sayNextSection`, `sayRaw`, `sayCode`, `sayJson`, `sayTable`, `sayWarning`, `sayNote`, `sayKeyValue`, `sayUnorderedList`, `sayOrderedList`, `sayAssertions`, `sayRef`, `sayCite` (×2), `sayFootnote` | 2026.3.0 |
| JVM Introspection | `sayCallSite`, `sayAnnotationProfile`, `sayClassHierarchy`, `sayStringProfile`, `sayReflectiveDiff` | 2026.3.0 |
| Code Reflection | `sayCodeModel` (×2), `sayControlFlowGraph`, `sayCallGraph`, `sayOpProfile` | 2026.3.0 |
| Benchmarking | `sayBenchmark` (×2) | 2026.3.0 |
| Mermaid | `sayMermaid`, `sayClassDiagram` | 2026.3.0 |
| Coverage and Quality | `sayDocCoverage`, `sayContractVerification`, `sayEvolutionTimeline` | 2026.3.0 |
| Utility | `sayEnvProfile`, `sayRecordComponents`, `sayException`, `sayAsciiChart` | 2026.3.0 |
| Field Injection | `@DtrContextField`, `@DtrTest` | 2026.4.1 |

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
        ctx.say("This document describes the 2026.4.1 release.");
        ctx.sayNote("Requires Java 26+ with --enable-preview.");
        ctx.sayWarning("API incompatible with DTR 2026.3.x — see changelog.");
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
    <version>2026.4.1</version>
    <scope>test</scope>
</dependency>
```

---

## Legacy Pattern Migration

In 2026.4.1, the field injection approach with `@DtrContextField` and `@DtrTest` annotation has become the primary pattern, replacing the legacy `extends DtrTest` approach. The field injection provides cleaner code and better test organization.

| Legacy: Extends DtrTest | Modern: Field Injection |
|------------------------|---------------------|
| `extends DtrTest` | `@ExtendWith(DtrExtension.class)` + `@DtrContextField` |
| `ctx.sayNextSection("x")` | `ctx.sayNextSection("x")` |
| `ctx.say("x")` | `ctx.say("x")` |
| `ctx.getRenderMachine()` | `ctx.getRenderMachine()` |
| `ctx.setRenderMachine(m)` | `ctx.setRenderMachine(m)` |
| Automatic lifecycle management | Automatic lifecycle management |
| No metadata support | `@DtrTest` for composite scenarios and metadata |

### Migration from Legacy `extends DtrTest` Pattern

The legacy pattern extends `DtrTest` directly:

```java
// LEGACY - Deprecated since 2026.4.1
class LegacyDocTest extends DtrTest {
    @Test
    void legacyMethod() {
        say("Legacy approach - discouraged for new tests");
    }
}
```

The modern approach uses field injection and the `@DtrTest` annotation:

```java
// MODERN - Recommended since 2026.4.1
@DtrTest(description = "Modern documentation test")
class ModernDocTest {

    @DtrContextField
    private DtrContext ctx;

    @Test
    void modernMethod() {
        ctx.say("Modern approach - cleaner code, better metadata support");
    }
}
```

---

## @DtrTest Annotation Examples

The `@DtrTest` annotation enables composite test scenarios and metadata management:

### Basic @DtrTest Usage

```java
@DtrTest(
    description = "API documentation tests",
    version = "2026.4.1"
)
class ApiDocTest {

    @DtrContextField
    private DtrContext ctx;

    @Test
    void apiOverview() {
        ctx.sayNextSection("API Overview");
        ctx.say("API documentation for version 2026.4.1.");
    }
}
```

### Composite Test Scenarios

```java
@DtrTest(
    description = "Complete user workflow documentation",
    composite = true,
    dependsOn = {"userAuth", "userProfile", "userSettings"}
)
class UserWorkflowDocTest {

    @DtrContextField
    private DtrContext ctx;

    @Test
    void workflowOverview() {
        ctx.sayNextSection("User Workflow Overview");
        ctx.say("Complete user lifecycle documentation for 2026.4.1.");
    }
}
```

### Multi-class Test Organization

```java
// Core functionality tests
@DtrTest(description = "Core functionality documentation", tags = {"core"})
class CoreFunctionalityDoc {

    @DtrContextField
    private DtrContext ctx;

    @Test
    void coreFeatures() {
        ctx.sayNextSection("Core Features");
        ctx.say("Core documentation for 2026.4.1.");
    }
}

// Advanced features tests
@DtrTest(description = "Advanced features documentation", tags = {"advanced"})
class AdvancedFeaturesDoc {

    @DtrContextField
    private DtrContext ctx;

    @Test
    void advancedFeatures() {
        ctx.sayNextSection("Advanced Features");
        ctx.say("Advanced documentation for 2026.4.1.");
    }
}
```

---

## See also

- [Field Injection Guide](../tutorials/field-injection-guide.md) — **Primary pattern**: `@DtrContextField` + `@DtrTest` for cleaner tests
- [say* Core API Reference](say-api-methods.md) — all 50+ method signatures
- [RenderMachine API](rendermachine-api.md) — rendering implementations
- [Configuration](configuration.md) — output directory, Maven settings
- [Architecture](../architecture.md) — DTR system design and component overview
