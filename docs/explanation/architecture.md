# Explanation: Architecture

> **Note:** This is the original deep-dive into DTR 2026.4.1 internals. For the comprehensive architecture overview, see **[ARCHITECTURE.md](../ARCHITECTURE.md)**. This document focuses on implementation philosophy and new module details.

This document describes DTR's module structure, class design, and the internal event pipeline that transforms test method calls into multi-format documentation output.

---

## What DTR Is (and Is Not)

DTR 2026.4.1 is a **pure documentation-generation library**. It has no HTTP client, no network stack, and no server coupling. Its single job is to receive structured `say*()` calls from JUnit Jupiter 6 tests and render them into Markdown, LaTeX, HTML, JSON, and other formats simultaneously.

This boundary is intentional. HTTP testing — making actual requests, inspecting responses, verifying contracts — is your responsibility using tools designed for it: `java.net.http.HttpClient`, RestAssured, Spring MockMvc. DTR then documents what you observed, with precision derived from your running code.

---

## Module Structure

```
dtr/
├── pom.xml                          # Parent POM
├── dtr-core/                        # The library (published to Maven Central)
│   └── src/main/java/io/github/seanchatmangpt/dtr/
│       ├── DtrExtension.java        # JUnit Jupiter 6 extension entrypoint
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

**Maven Coordinate:** `io.github.seanchatmangpt.dtr:dtr-core:2026.4.1`

---

## Class Hierarchy

### Modern Field Injection Architecture (Primary Flow)

```
JUnit Jupiter 6 test class
    │
    │ @ExtendWith(DtrExtension.class) + @DtrTest
    │
    ▼
DtrExtension                         ← JUnit Jupiter 6 lifecycle hooks + field injection
    │  injects DtrContext via @DtrContextField
    │
    ▼
DtrContext (via field injection)     ← Test-facing API with direct field access
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

### Legacy Inheritance Architecture (Alternative)

```
JUnit Jupiter 6 test class
    │
    │ @ExtendWith(DtrExtension.class) + extends DtrTest
    │
    ▼
DtrTest (base class)                ← Legacy base class providing direct say*() methods
    │
    │ (DtrExtension still runs for lifecycle management)
    ▼
DtrContext (inherited)              ← Test-facing API via inheritance
    │  emits SayEvents to
    ▼
[Same render machine pipeline as above]
```

### Key Design Decisions

**Field Injection** (`@DtrContextField`) is the **primary** modern pattern:
- **✓ Cleaner test code** - No inheritance pollution
- **✓ Flexible composition** - Can mix with other annotations
- **✓ Better IDE support** - Field completion vs inherited methods
- **✓ Explicit dependency** - Clear visual contract in test code

**Inheritance** (`extends DtrTest`) is **legacy** but still supported:
- **✓ Backward compatibility** - Existing code continues to work
- **✓ Quick migration** - Simple change for existing tests
- **✓ Direct method access** - No field dereferencing needed


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

### Modern Field Injection Flow
```
@Test
void myFeature(@DtrContextField DtrContext context) {
    context.say("Testing documentation...");
    │
    │  creates SayEvent records from injected field
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
    ├── MyFeatureTest.md
    ├── MyFeatureTest.tex
    ├── MyFeatureTest.html
    └── MyFeatureTest.json
```

### Legacy Inheritance Flow
```
public class MyLegacyTest extends DtrTest {
    @Test
    void myLegacyTest() {
        say("Testing documentation via inheritance...");
        │
        │  creates SayEvent records from inherited method
        ▼
[Same pipeline as above]
    }
}
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

## Field Injection Architecture

### @DtrTest and @DtrContextField

**@DtrTest** is the new composite annotation that combines JUnit test functionality with DTR capabilities:

```java
@DtrTest
public class ModernFeatureTest {

    @Test
    void authenticationFlow(@DtrContextField DtrContext context) {
        context.say("Testing authentication endpoint...");
        context.sayCode("POST /auth", "http");
    }
}
```

### Key Benefits of Field Injection

1. **Explicit Dependency Declaration**: `@DtrContextField` makes the DTR dependency visible in the method signature
2. **Clean Test Methods**: No inheritance clutter in test classes
3. **Flexible Composition**: Can mix with other `@*Test` annotations and parameters
4. **Better IDE Support**: Auto-completion and type safety for injected fields
5. **Framework Agnostic**: Works with any testing framework, not just JUnit

### Field Injection Implementation

The `DtrExtension` detects `@DtrContextField` annotations and performs field injection:

```java
public class DtrExtension implements ParameterResolver, BeforeEachCallback {

    @Override
    public boolean supportsParameter(ParameterContext parameter,
                                     ExtensionContext extensionContext) {
        return parameter.isAnnotated(DtrContextField.class) &&
               parameter.getParameter().getType() == DtrContext.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameter,
                                  ExtensionContext extensionContext) {
        return getOrCreateDtrContext(extensionContext);
    }
}
```

### @DtrTest vs. @ExtendWith Comparison

| Pattern | @DtrTest | @ExtendWith(DtrExtension.class) |
|---------|----------|----------------------------------|
| **Simplicity** | Single annotation | Two annotations needed |
| **Visibility** | Clear in test declaration | Extension is implicit |
| **Flexibility** | Limited to DTR-aware tests | Works with any test class |
| **Legacy** | Modern approach | Original approach |

**Recommendation**: Use `@DtrTest` for new tests. Use `@ExtendWith(DtrExtension.class)` for existing codebases or when mixing DTR with other extensions.

---

## Lifecycle Details

### Per Test Class

`DtrExtension` implements `BeforeAllCallback` and `AfterAllCallback`:

- `beforeAll`: A `MultiRenderMachine` is created for the class, wired to all configured output targets.
- `afterAll`: `finishAndWriteOut()` is called on the `MultiRenderMachine`, which flushes accumulated events to all render targets in parallel and writes output files.

### Per Test Method

`DtrExtension` implements `BeforeEachCallback` and `AfterEachCallback`:

- `beforeEach`: A fresh `DtrContext` is injected into test methods using one of three patterns:
  1. **Field injection** (primary): `@DtrContextField private DtrContext context;`
  2. **Method parameter injection**: `void test(DtrContext context) { ... }`
  3. **Inheritance** (legacy): Extend `DtrTest` base class
- `afterEach`: Any pending section markers or open blocks are closed.

### @DtrTest Composite Annotation

**@DtrTest** combines JUnit 5 test functionality with DTR capabilities. When used, it automatically applies both `@Test` and `@ExtendWith(DtrExtension.class)`:

```java
// Modern approach (recommended)
@DtrTest
public class ModernTest {
    @Test
    void featureTest(@DtrContextField DtrContext context) {
        context.say("Testing with field injection...");
    }
}

// Legacy approach (still supported)
@ExtendWith(DtrExtension.class)
public class LegacyTest {
    @Test
    void legacyTest(DtrContext context) {
        context.say("Testing with method injection...");
    }
}

// Inheritance approach (legacy)
public class InheritanceTest extends DtrTest {
    @Test
    void inheritanceTest() {
        say("Testing via inheritance...");
    }
}
```

### Field Injection Implementation Details

The field injection system uses JUnit 5's `ParameterResolver` mechanism:

1. **Annotation Detection**: `@DtrContextField` identifies injection targets
2. **Context Creation**: Creates a thread-safe `DtrContext` instance per test
3. **Field Population**: Injects the context into annotated fields before test execution
4. **Lifecycle Management**: Each test gets a fresh context to ensure isolation

### @DtrTest vs. Inheritance Relationship

**@DtrTest** represents the **evolution** of DTR's API design:

- **Primary Pattern**: Field injection (`@DtrContextField`) - Modern, explicit, composable
- **Legacy Pattern**: Inheritance (`extends DtrTest`) - Backward compatible, convenient but restrictive
- **Bridge Pattern**: Method parameter injection - Transitional, flexible but verbose

**Design Philosophy**: Field injection is superior because it:
- Makes dependencies explicit and visible
- Allows test classes to be composition-based rather than inheritance-based
- Enables better IDE support and refactoring
- Reduces coupling between tests and the DTR framework

### Java 26 Requirement

DTR 2026.4.1 requires `--enable-preview` because it uses the Code Reflection API (Project Babylon, JEP 516). This API is used by `sayCallSite()` to capture the exact source location where documentation was generated — file name, line number, method name — without requiring a stack walk at runtime. The flag is not optional.

---

## Dependencies in dtr-core

DTR 2026.4.1 adds field injection capability with **zero new external dependencies**. The dependency list remains minimal:

| Dependency | Purpose |
|---|---|
| `junit:junit-jupiter:5.x` | JUnit Jupiter 6 extension API |
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
