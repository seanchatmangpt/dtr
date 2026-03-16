# DTR — Documentation Testing Runtime

> **Transform Java documentation into executable tests that prove code works as documented.**

[![CI Gate](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml)
[![Quality Gates](https://github.com/seanchatmangpt/dtr/actions/workflows/quality-gates.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/quality-gates.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.seanchatmangpt.dtr/dtr-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core/versions)
[![Java 26](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/projects/jdk/26/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Version:** 2026.4.1 | **License:** Apache 2.0 | **Java:** 26+ (`--enable-preview`)

---

## What is DTR?

DTR (Documentation Testing Runtime) is a Java 26 library that turns JUnit Jupiter 6 tests into living documentation generators. Instead of writing docs separately from code — and watching them drift as code changes — you write `say*` method calls inside your tests that simultaneously:

1. **Execute** your assertions and validations
2. **Capture** structured documentation events
3. **Render** to Markdown, LaTeX, blog posts, and presentations — all in one test run

**The core guarantee:** If a test passes, its documentation is accurate. Documentation can only exist for behavior the test actually exercises.

### Why DTR?

Documentation stays in sync with code because it's generated from live test execution. When code changes, tests fail, and outdated docs are never produced.

---

## Quick Start (5 minutes)

### Add the dependency

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.4.1</version>
    <scope>test</scope>
</dependency>
```

Configure for Java 26 with preview features:

```xml
<properties>
    <maven.compiler.release>26</maven.compiler.release>
</properties>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>26</release>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.3</version>
            <configuration>
                <argLine>--enable-preview</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Add to `.mvn/maven.config`:
```
--enable-preview
```

### Write your first documentation test

```java
import io.github.seanchatmangpt.dtr.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrContextField;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

@ExtendWith(DtrExtension.class)
class HelloDtrTest {

    @DtrContextField
    private DtrContext ctx;

    @Test
    @AutoSection  // Auto-generates "Get User" from method name
    void testGetUser() {
        ctx.say("Returns user details by ID.");
        ctx.sayCode("GET /api/users/{id}", "http");
        ctx.sayKeyValue(Map.of(
            "Authentication", "Bearer token",
            "Rate Limit", "100/hour"
        ));
    }
}
```

### Run the test

```bash
mvnd test -Dtest=HelloDtrTest
```

### View the generated documentation

```bash
cat target/docs/test-results/HelloDtrTest.md
```

Output is pure, portable Markdown — committed to your repository, diffable, and rendered natively by GitHub, GitLab, and every Markdown-aware editor.

---

## Context Injection Patterns

DTR supports multiple patterns for accessing `DtrContext` in your tests. The primary modern approach is field injection, while parameter injection is recommended for specific use cases, and inheritance is considered a legacy pattern.

### Field Injection (Primary Pattern)

Declare the context once at the class level - this is the **recommended approach for most use cases**:

```java
@ExtendWith(DtrExtension.class)
class MyApiTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }

    @Test
    void createUser() {
        ctx.say("Creates a new user");
    }
}
```

**Benefits:**
- Clean method signatures
- One declaration for entire test class
- Less repetition for tests with many methods

### Parameter Injection (For Specific Use Cases)

Inject the context as a method parameter - this is recommended when you need different setup for each test:

```java
@ExtendWith(DtrExtension.class)
class MyApiTest {
    @Test
    void listUsers(DtrContext ctx) {
        ctx.say("Returns all users");
    }

    @Test
    void createUser(DtrContext ctx) {
        ctx.say("Creates a new user");
    }
}
```

**Benefits:**
- Explicit dependencies per test method
- Different setup possible for each method
- More familiar to JUnit users

### Which Pattern to Choose?

| Use Case | Recommended Pattern |
|----------|-------------------|
| Test class with 3+ methods | **Field Injection** |
| Each test needs different setup | Parameter Injection |
| New to DTR | **Field Injection** (simpler) |
| Migrating from JUnit assertions | Parameter Injection (familiar) |

**Default recommendation:** Start with **field injection** for cleaner, more maintainable test classes.

### Inheritance (Legacy Pattern)

**Legacy pattern:** Extend the `DtrTest` base class - this is an older approach that is now considered legacy:

```java
class MyLegacyTest extends DtrTest {  // Legacy pattern
    @Test
    void listUsers() {
        ctx.say("Returns all users");
    }
}
```

**Legacy pattern drawbacks:**
- Tightly couples test classes to DTR
- Cannot combine with other base classes
- Less flexible configuration
- Considered superseded by field injection

**Migration path:** Switch to field injection for new tests when possible.

See **[Field Injection Guide](docs/tutorials/field-injection-guide.md)** for complete details including advanced patterns, thread safety, and migration guides.

---

## What's New in 2026.4.1

### Critical Bug Fix: DtrContext Parameter Injection

The `DtrContext` parameter injection in test methods now works correctly. Previously, you may have seen:

```
No ParameterResolver registered for parameter [io.github.seanchatmangpt.dtr.junit5.DtrContext arg0]
```

This is now fixed. The standard pattern works as documented:

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @Test
    void testGetUser(DtrContext ctx) {  // Parameter injection now works!
        ctx.say("Returns user details by ID.");
    }
}
```

### New Features

#### Field Injection for DtrContext

New `@DtrContextField` annotation provides cleaner test classes:

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @DtrContextField
    private DtrContext ctx;

    @Test
    void test() {
        ctx.say("Clean method signatures");
    }
}
```

Field injection is the **primary pattern** for modern DTR testing. See **[Field Injection Guide](docs/tutorials/field-injection-guide.md)** for complete details.

#### Assertion + Documentation Combined

Four new `sayAndAssertThat` methods combine assertions with documentation:

```java
ctx.sayAndAssertThat("HTTP Status", response.getStatusCode(), equalTo(200));
```

#### Presentation-Specific Methods

New methods for slides, blogs, and social media:

```java
ctx.sayTldr("Quick summary for busy readers");
ctx.saySlideOnly("Simplified bullet points for slides");
ctx.sayTweetable("DTR 2026.4.1 adds field injection as the primary pattern!");
```

#### Configuration Annotations

- `@DtrConfig` — Hierarchical configuration (system properties → annotation → default)
- `@LivePreview` — IDE integration markers for live preview support

### Breaking Changes

**Most users: No action required!**

1. **RenderConfig removed** (< 5 users affected)
   - Migration: Use `@DtrConfig` or `DtrConfiguration.builder()`

2. **sayCodeModel(Method) deprecated** (low impact)
   - Migration: Use `sayMethodSignature(Method)` instead
   - Removal planned for 2027.1.0

See [MIGRATING.md](docs/MIGRATING.md) for detailed migration guides.

---

## Features

### 44+ `say*` Methods for Documentation

DTR provides a comprehensive API for generating documentation:

- **Core:** `say()`, `sayNextSection()`, `sayCode()`, `sayTable()`, `sayKeyValue()`
- **Formatting:** `sayNote()`, `sayWarning()`, `sayUnorderedList()`, `sayOrderedList()`, `sayJson()`
- **Cross-references:** `sayRef()`, `sayCite()`, `sayFootnote()`
- **Code analysis:** `sayCodeModel()`, `sayCallSite()`, `sayClassHierarchy()`, `sayAnnotationProfile()`
- **Java 26 Code Reflection:** `sayControlFlowGraph()`, `sayCallGraph()`, `sayOpProfile()` (JEP 516)
- **Benchmarking:** `sayBenchmark()` with configurable warmup/measure rounds
- **Diagrams:** `sayMermaid()`, `sayClassDiagram()`
- **Quality:** `sayDocCoverage()`, `sayContractVerification()`
- **Presentation:** `saySlideOnly()`, `saySpeakerNote()`, `sayHeroImage()`, `sayTweetable()`, `sayTldr()`
- **Testing:** `sayAndAssertThat()` — assert + document in one call

### Multi-Format Output

Every test run generates documentation in multiple formats from a single source:

- **Markdown** — GitHub-ready, diffable, portable
- **LaTeX** — Academic papers, technical reports
- **Blog posts** — Jekyll, Hugo, static site generators
- **Reveal.js slides** — Presentations from tests
- **HTML** — Standalone web pages
- **JSON** — Machine-readable API docs

### Java 26 Code Reflection (JEP 516)

Leverage Java 26's Code Reflection API to generate control flow graphs, call graphs, and operation profiles directly from bytecode:

```java
@Test
void documentAlgorithm(DtrContext ctx) {
    ctx.sayControlFlowGraph(MyClass.class.getMethod("sort"));
}
```

### Inline Benchmarking

Document performance characteristics with real measurements:

```java
ctx.sayBenchmark("String concatenation (1000 ops)", () -> {
    String s = "";
    for (int i = 0; i < 1000; i++) s += i;
});
```

Output includes: metric + units + Java version + iterations + environment.

---

## 80/20 API: The 8 Essential Methods

Most documentation uses just 8 methods. Master these first:

| Method | Use Case |
|--------|----------|
| `say(text)` | Simple paragraphs, most common |
| `sayCode(code, lang)` | Code blocks with syntax highlighting |
| `sayTable(data)` | Structured data tables |
| `sayNextSection(headline)` | H1 heading with TOC entry |
| `sayRef(class, anchor)` | Link to other documentation sections |
| `sayNote(message)` | Additional context |
| `sayWarning(message)` | Critical warnings |
| `sayKeyValue(pairs)` | Metadata, configuration, key facts |

Example using the 80/20 set:

```java
@DtrContextField
private DtrContext ctx;

@Test
void documentApi() {
    ctx.sayNextSection("Authentication API");
    ctx.say("All endpoints require Bearer token authentication.");
    ctx.sayCode("Authorization: Bearer <token>", "http");
    ctx.sayKeyValue(Map.of(
        "Token Source", "OAuth 2.0",
        "Expiration", "3600 seconds"
    ));
    ctx.sayNote("Tokens refresh automatically.");
    ctx.sayWarning("Never expose tokens in client-side code.");
}
```

---

## Tutorials

Learn by doing with step-by-step guides:

- **[Field Injection Guide](docs/tutorials/field-injection-guide.md)** — Recommended pattern for DtrContext access
- **[Your First DocTest](docs/tutorials/your-first-doctest.md)** — 20-minute introduction to DTR basics
- **[Testing a REST API](docs/tutorials/testing-a-rest-api.md)** — Document real HTTP endpoints
- **[Records and Sealed Classes](docs/tutorials/records-sealed-classes.md)** — Advanced pattern matching
- **[Virtual Threads](docs/tutorials/virtual-threads-lightweight-concurrency.md)** — Concurrency testing
- **[Server-Sent Events](docs/tutorials/server-sent-events.md)** — Real-time protocols
- **[WebSocket Testing](docs/tutorials/websockets-realtime.md)** — Bidirectional communication

---

## API Reference

Complete documentation for all 44+ `say*` methods:

- **[80/20 Essentials](docs/how-to/80-20-essentials.md)** — The minimal path to productivity
- **[Complete API Reference](docs/reference/)** — All methods, signatures, and examples
- **[Configuration](docs/reference/configuration.md)** — System properties and environment variables
- **[FAQ & Troubleshooting](docs/reference/FAQ_AND_TROUBLESHOOTING.md)** — Common issues and solutions

### Key Reference Sections

- **[DtrTest Base Class](docs/reference/doctester-base-class.md)** — Assertions combined with documentation
- **[RenderMachine API](docs/reference/rendermachine-api.md)** — Custom output formats
- **[Benchmarking](docs/how-to/benchmarking.md)** — Performance measurement
- **[Advanced Rendering](docs/how-to/advanced-rendering-formats.md)** — LaTeX, slides, blogs

---

## Contributing

We welcome contributions! See **[Contributing Guide](docs/contributing/index.md)** for:

- **[Setup](docs/contributing/setup.md)** — Development environment configuration
- **[Codebase Tour](docs/contributing/codebase-tour.md)** — Architecture overview
- **[Making Changes](docs/contributing/making-changes.md)** — Pull request workflow
- **[Releasing](docs/contributing/releasing.md)** — Version management and deployment

### Quick Setup for Contributors

```bash
# Clone the repository
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# Install Java 26 with preview features
sdk install java 26.ea.13-open
sdk use java 26.ea.13-open

# Install Maven Daemon (mvnd)
brew install mvnd  # macOS
# or: SDKMAN, apt, etc.

# Run tests
mvnd verify

# Build documentation
mvnd verify -DskipTests
```

---

## Versioning

DTR uses **CalVer** versioning: `YYYY.MINOR.PATCH`

- **`2026.3.0`** — First release of 2026, minor version 1
- **`2026.1.1`** — Patch release (bug fix, dependency update)
- **`2026.3.0`** — Minor release (new features, new `say*` methods)
- **`2027.1.0`** — Year boundary (January 1st, new calendar year)

**Release types:**
- `make release-minor` — New capability or new `say*` method → `YYYY.(N+1).0`
- `make release-patch` — Bug fix or dependency update → `YYYY.MINOR.(N+1)`
- `make release-year` — Explicit year boundary → `YYYY.1.0`

See **[CHANGELOG.md](CHANGELOG.md)** for complete version history.

---

## Architecture

DTR is organized as a multi-module Maven project:

- **`dtr-core`** — Main library with `DtrContext` and all `say*` methods
- **`dtr-benchmark`** — Performance benchmarks and validation
- **`dtr-integration-test`** — End-to-end testing across formats

Key design principles:

1. **Single source of truth** — Tests are the specification
2. **Progressive disclosure** — Simple first, complex later
3. **Real measurements** — No estimates, always metric + units + environment
4. **CI-first** — Design for GitHub Actions, not localhost

See **[Architecture Documentation](docs/explanation/architecture.md)** for details.

---

## License

Apache License 2.0 — See [LICENSE](LICENSE) for details.

---

## Links

- **Maven Central:** [https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)
- **GitHub:** [https://github.com/seanchatmangpt/dtr](https://github.com/seanchatmangpt/dtr)
- **Issues:** [https://github.com/seanchatmangpt/dtr/issues](https://github.com/seanchatmangpt/dtr/issues)
- **Discussions:** [https://github.com/seanchatmangpt/dtr/discussions](https://github.com/seanchatmangpt/dtr/discussions)

---

**Invariant:** The pipeline is the specification of done. Everything else is execution detail.
