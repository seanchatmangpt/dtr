# DTR — Documentation Testing Runtime

> **Generate living documentation as your tests execute.**
> Every test run regenerates docs in multiple formats (Markdown, LaTeX, Blog posts, PDF,
> Reveal.js slides) from live behavior — keeping docs forever in sync with reality.

[![CI Gate](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/ci-gate.yml)
[![Quality Gates](https://github.com/seanchatmangpt/dtr/actions/workflows/quality-gates.yml/badge.svg)](https://github.com/seanchatmangpt/dtr/actions/workflows/quality-gates.yml)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.seanchatmangpt.dtr/dtr-core.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core/versions)
[![Java 26](https://img.shields.io/badge/Java-26-orange.svg)](https://openjdk.org/projects/jdk/26/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

**Latest:** `2026.1.0` | **License:** Apache 2.0 | **Java:** 26+ (`--enable-preview`) | **Build:** Maven 4 / mvnd 2.x

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.1.0</version>
    <scope>test</scope>
</dependency>
```

---

## Table of Contents

1. [What Is DTR?](#what-is-dtr)
2. [5-Minute Quick Start](#5-minute-quick-start)
3. [Versioning & Releasing](#versioning--releasing)
4. [Tutorials — Learn by Doing](#tutorials--learn-by-doing)
5. [How-To Guides — Solve Real Problems](#how-to-guides--solve-real-problems)
6. [Reference — Complete API](#reference--complete-api)
7. [Explanation — Why DTR?](#explanation--why-dtr)
8. [Architecture](#architecture)
9. [Module Structure](#module-structure)
10. [Troubleshooting](#troubleshooting)
11. [Changelog](#changelog)
12. [Contributors & History](#contributors--history)

---

## What Is DTR?

DTR (Documentation Testing Runtime) is a Java 26 library that turns JUnit 5 tests into
living documentation generators. Instead of writing docs separately from code — and
watching them drift as code changes — you write `say*` method calls inside your tests
that simultaneously:

1. **Execute** your assertions and validations
2. **Capture** structured documentation events
3. **Render** those events to Markdown, LaTeX, blog posts, and presentations — all in one test run

**The core guarantee:** If a test passes, its documentation is accurate. Documentation can
only exist for behavior the test actually exercises.

### What's New in 2026.1.0

- **14 new `say*` method signatures** across 13 capabilities: inline benchmarking,
  Mermaid diagrams, documentation coverage, ASCII charts, contract verification, git timelines,
  exception documentation, record schemas, environment snapshots, and more.
- **HTTP/WebSocket stack removed** — DTR is now a pure documentation-generation library.
  Bring your own HTTP client (`java.net.http.HttpClient`, RestAssured, etc.).
- Zero new external dependencies.
- **CalVer versioning adopted** — versions now follow `YYYY.MINOR.PATCH`. This release
  (`2026.1.0`) is the first under the new scheme.

See [`CHANGELOG.md`](CHANGELOG.md) for the complete version history.

---

## 5-Minute Quick Start

### Step 1: Configure your `pom.xml`

```xml
<properties>
    <maven.compiler.release>26</maven.compiler.release>
</properties>

<dependencies>
    <dependency>
        <groupId>io.github.seanchatmangpt.dtr</groupId>
        <artifactId>dtr-core</artifactId>
        <version>2026.1.0</version>
        <scope>test</scope>
    </dependency>
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>6.0.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>25</release>
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

### Step 2: Write a self-documenting test

```java
package example;

import io.github.seanchatmangpt.dtr.junit5.DtrContext;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

@ExtendWith(DtrExtension.class)
public class GettingStartedDocTest {

    @Test
    void documentUserApi(DtrContext ctx) {
        ctx.sayNextSection("User Management API");
        ctx.say("The user API provides CRUD operations for managing application users.");

        ctx.sayCode("""
            GET /api/users/{id}
            Authorization: Bearer <token>
            Accept: application/json
            """, "http");

        var exampleUser = Map.of(
            "id",    1,
            "name",  "Alice Nguyen",
            "email", "alice@example.com",
            "role",  "admin"
        );
        ctx.sayJson(exampleUser);

        ctx.sayTable(new String[][] {
            {"Field",   "Type",   "Required", "Description"},
            {"id",      "Long",   "auto",     "Auto-generated surrogate key"},
            {"name",    "String", "yes",      "Display name, max 100 chars"},
            {"email",   "String", "yes",      "Unique email, validated on save"},
            {"role",    "String", "yes",      "Enum: admin | editor | viewer"}
        });

        ctx.sayAssertions(Map.of(
            "Schema validates correctly",    "✅ PASS",
            "Email uniqueness enforced",     "✅ PASS",
            "Role enum is exhaustive",       "✅ PASS"
        ));

        ctx.sayWarning("Admin users can modify system settings. All changes are audit-logged.");
        ctx.sayNote("Pagination uses cursor-based navigation. Do not use offset-based queries on large datasets.");

        ctx.sayEnvProfile();  // Generated-with footer: Java version, OS, heap, DTR version
    }
}
```

### Step 3: Run the test

```bash
mvnd test -Dtest=GettingStartedDocTest
```

### Step 4: View the generated documentation

```bash
cat target/docs/test-results/GettingStartedDocTest.md
```

The output is pure, portable Markdown — committed to your repository, diffable, and
rendered natively by GitHub, GitLab, and every Markdown-aware editor.

---

## Versioning & Releasing

### CalVer: YYYY.MINOR.PATCH

DTR uses [Calendar Versioning](https://calver.org) starting from `2026.1.0`.

| Component | Meaning | Behaviour |
|-----------|---------|-----------|
| `YYYY` | Release year | Reads as a timestamp — `2026` tells you the dependency is ~2 years old in 2028 |
| `MINOR` | Feature iteration within the year | Starts at 1; resets to 1 on year boundary |
| `PATCH` | Fix within a MINOR | Starts at 0; resets to 0 on every MINOR bump |

Year boundaries are automatic: `scripts/bump.sh minor` reads `date +%Y`. If the year changed,
MINOR resets to 1. No human decides when 2027 starts.

**Never type a version number** — the release scripts own the arithmetic:

```bash
make release-minor      # new say* methods, additive features  → YYYY.(N+1).0
make release-patch      # bug fix, no API change               → YYYY.MINOR.(N+1)
make release-year       # explicit year boundary (January)     → YYYY.1.0

make release-rc-minor   # RC for minor                         → YYYY.(N+1).0-rc.N
make release-rc-patch   # RC for patch                         → YYYY.MINOR.(N+1)-rc.N

make snapshot           # deploy SNAPSHOT (no tag, no release)
make version            # print current version
```

### One-Command Release Invariant

```
make release-minor
       │
       ▼
scripts/bump.sh   → computes NEXT (CalVer + year-aware), updates pom.xml
scripts/release.sh → generates CHANGELOG.md, commits, tags v<VERSION>, pushes
       │
       ▼ (GitHub Actions fires on tag)
mvnd verify → mvnd deploy -Prelease → gh release create → artifact on Maven Central
```

There are **no manual steps between `make release-minor` and a published artifact**.
If `mvnd verify` fails, nothing publishes.

---

## Tutorials — Learn by Doing

### Tutorial 1: Benchmark Your Code and Document the Results

Inline microbenchmarking eliminates the gap between performance claims and measurements.
`sayBenchmark` uses `System.nanoTime()` with virtual-thread warmup to produce real numbers.

```java
@Test
void benchmarkStringOperations(DtrContext ctx) {
    ctx.sayNextSection("String Processing Performance");
    ctx.say("Comparing StringBuilder vs String concatenation across 100k iterations.");

    // Default: 50 warmup rounds + 500 measurement rounds
    ctx.sayBenchmark("StringBuilder append (1k chars)", () -> {
        var sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append('x');
        return sb.toString();
    });

    // Custom round counts for expensive operations
    ctx.sayBenchmark("String.format 1000x", () -> {
        for (int i = 0; i < 1000; i++) String.format("user-%d", i);
    }, 10, 100);  // 10 warmup, 100 measurement

    ctx.sayNote("All measurements are real JVM executions. No simulated numbers.");
}
```

**Output columns:** `Benchmark | Avg (ns) | Min (ns) | Max (ns) | p99 (ns) | ops/sec`

---

### Tutorial 2: Visualize Architecture with Mermaid Diagrams

Mermaid diagrams render natively on GitHub, GitLab, and Obsidian. Generate them from
real class structures via reflection — they update automatically when code changes.

```java
@Test
void documentArchitecture(DtrContext ctx) {
    ctx.sayNextSection("DTR Rendering Pipeline");
    ctx.say("The following class diagram is auto-generated from the live codebase.");

    // Auto-generated from Class.getSuperclass() + Class.getInterfaces() + getDeclaredMethods()
    ctx.sayClassDiagram(
        RenderMachine.class,
        RenderMachineImpl.class,
        MultiRenderMachine.class,
        RenderMachineCommands.class
    );

    ctx.say("You can also emit raw Mermaid DSL for custom diagrams:");

    ctx.sayMermaid("""
        sequenceDiagram
            participant Test
            participant DtrContext
            participant RenderMachine
            participant Markdown
            Test->>DtrContext: say("Hello")
            DtrContext->>RenderMachine: dispatch(SayEvent.Text)
            RenderMachine->>Markdown: write("Hello\\n\\n")
        """);
}
```

---

### Tutorial 3: Document JVM Type Structure

Automatically document class hierarchies, annotation landscapes, and record schemas —
derived from bytecode, never from developer prose.

```java
@Test
void documentTypeSystem(DtrContext ctx) {
    ctx.sayNextSection("SayEvent Type Hierarchy");

    // Visual tree: superclass chain + all interfaces at each level
    ctx.sayClassHierarchy(RenderMachineImpl.class);

    ctx.sayNextSection("DtrExtension Annotation Profile");
    // Complete annotation landscape: class-level + per-method annotations
    ctx.sayAnnotationProfile(DtrExtension.class);

    ctx.sayNextSection("CallSiteRecord Schema");
    // Record component names, types, and annotations
    ctx.sayRecordComponents(CallSiteRecord.class);

    ctx.sayNextSection("Where Was This Documentation Generated?");
    // Stack-derived provenance: class, method, file, line number
    ctx.sayCallSite();
}
```

---

### Tutorial 4: Compare Object States with Reflective Diff

`sayReflectiveDiff` compares any two objects field-by-field and renders a diff table.
Ideal for documenting state transitions, before/after mutations, or version migrations.

```java
record UserConfig(String role, int rateLimit, boolean mfaEnabled, String tier) {}

@Test
void documentConfigMigration(DtrContext ctx) {
    ctx.sayNextSection("User Config Migration: Free → Pro");
    ctx.say("Upgrading a user from Free to Pro tier changes these fields:");

    var before = new UserConfig("viewer", 100,  false, "free");
    var after  = new UserConfig("editor", 5000, true,  "pro");

    ctx.sayReflectiveDiff(before, after);
    // Renders: field | before value | after value | changed (✅/—)

    ctx.sayNote("mfaEnabled defaults to false on Free tier. Pro tier enforces MFA.");
}
```

---

### Tutorial 5: Verify Contract Coverage

Document which implementations provide concrete overrides for every method in a contract
interface. Useful for compliance documentation and architectural reviews.

```java
@Test
void documentRenderMachineContracts(DtrContext ctx) {
    ctx.sayNextSection("RenderMachine Contract Coverage");
    ctx.say("Verifying that all render machines implement the full say* contract:");

    ctx.sayContractVerification(
        RenderMachineCommands.class,  // The contract interface
        RenderMachineImpl.class,      // Markdown implementation
        RenderMachineLatex.class,     // LaTeX implementation
        BlogRenderMachine.class,      // Blog implementation
        SlideRenderMachine.class      // Slides implementation
    );
    // Output: ✅ direct | ↗ inherited | ❌ MISSING — per method per implementation
}
```

---

### Tutorial 6: Track Class Evolution from Git History

```java
@Test
void documentRenderMachineHistory(DtrContext ctx) {
    ctx.sayNextSection("RenderMachine Evolution Timeline");
    ctx.say("Git commit history for the core rendering class:");

    ctx.sayEvolutionTimeline(RenderMachine.class, 20);
    // Renders: commit hash | date | author | subject (from git log --follow)
    // Falls back gracefully if git is unavailable in the build environment
}
```

---

### Tutorial 7: Document Exceptions and Error Conditions

```java
@Test
void documentErrorHandling(DtrContext ctx) {
    ctx.sayNextSection("Error Handling: Invalid Citation Key");
    ctx.say("When a BibTeX citation key is not registered, DTR throws UnknownCitationException:");

    try {
        bibliographyManager.cite("nonexistent_key_2026");
    } catch (UnknownCitationException e) {
        ctx.sayException(e);
        // Renders: type | message | cause chain | top 5 stack frames
    }

    ctx.sayWarning("Always register citation keys before calling sayCite().");
}
```

---

### Tutorial 8: Generate LaTeX Academic Papers

```java
@Test
void documentResearchFindings(DtrContext ctx) {
    ctx.sayNextSection("Experimental Results");

    ctx.sayTable(new String[][] {
        {"Metric",             "v2.4.0",  "v2.5.0",  "Improvement"},
        {"sayCallSite (100×)", "120ms",   "6ms",     "20×"},
        {"sayAnnotationProfile (100×)", "210ms", "5ms", "42×"},
        {"sayClassHierarchy (100×)", "180ms", "4ms",  "45×"}
    });

    ctx.sayCite("openjdk-jep516", "pp. 3-7");  // BibTeX citation with page ref
    ctx.sayFootnote("Measurements taken on Java 26.0.2, Intel i9-13900K, 64GB RAM.");

    ctx.sayWarning("These results apply to repeated calls only. First-call latency is unchanged.");
}
```

---

### Tutorial 9: ASCII Charts for Quick Visual Summaries

```java
@Test
void documentThroughput(DtrContext ctx) {
    ctx.sayNextSection("API Endpoint Throughput");

    ctx.sayAsciiChart(
        "Requests per Second by Endpoint",
        new double[]  {12400, 8900, 5600, 3200, 1100},
        new String[]  {"GET /users", "GET /items", "POST /orders", "PUT /user", "DELETE /item"}
    );
    // Renders: Unicode ████ bars normalized to max value
}
```

---

### Tutorial 10: Documentation Coverage Enforcement

```java
@Test
void enforceApiDocumentation(DtrContext ctx) {
    ctx.sayNextSection("DtrContext API Coverage Report");
    ctx.say("The following report shows which public methods were exercised in this test run:");

    // ... your test assertions that call various ctx.say*() methods ...

    ctx.sayDocCoverage(DtrContext.class, RenderMachineCommands.class);
    // Renders: ✅ documented | ❌ undocumented — per public method
    // Use in CI to enforce minimum documentation coverage
}
```

---

## How-To Guides — Solve Real Problems

### How-To: Test an API Endpoint and Document It

<<<<<<< HEAD
DTR 2026.1.0 is documentation-only; you bring your own HTTP client. Combine standard
=======
DTR is documentation-only; you bring your own HTTP client. Combine standard
>>>>>>> origin/master
`java.net.http.HttpClient` (or RestAssured, OkHttp, etc.) with DTR's `say*` methods:

```java
@Test
void documentGetUsersEndpoint(DtrContext ctx) throws Exception {
    ctx.sayNextSection("GET /api/users — List All Users");
    ctx.say("Returns a paginated list of all users. Requires `admin` role.");

    // Make the real HTTP call yourself
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create(baseUrl + "/api/users?page=1&limit=10"))
        .header("Authorization", "Bearer " + bearerToken)
        .GET()
        .build();

    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    // Document the request
    ctx.sayCode("""
        GET /api/users?page=1&limit=10
        Authorization: Bearer <token>
        """, "http");

    // Document the response
    ctx.sayCode(response.body(), "json");

    // Document the assertions
    assertThat(response.statusCode()).isEqualTo(200);
    ctx.sayAssertions(Map.of(
        "HTTP status 200",          "✅ PASS",
        "Content-Type is JSON",     "✅ PASS",
        "Response contains users",  "✅ PASS"
    ));

    ctx.sayNote("Pagination is cursor-based. Use `?cursor=<nextCursor>` for subsequent pages.");
}
```

---

### How-To: Export Documentation to Blog Platforms

A single test run can publish documentation to Medium, Dev.to, Substack, Hashnode, and LinkedIn:

```java
// In your DtrExtension / test setup:
MultiRenderMachine multi = new MultiRenderMachine(
    new RenderMachineImpl("MyApiDocTest"),           // → Markdown
    new RenderMachineLatex(new ArXivTemplate(), "MyApiDocTest"),  // → LaTeX/PDF
    new BlogRenderMachine(new DevToTemplate()),      // → Dev.to JSON
    new BlogRenderMachine(new MediumTemplate()),     // → Medium JSON
    new SlideRenderMachine(new RevealJsTemplate())   // → Reveal.js HTML
);

// In your CI/CD, publish the queue:
// curl -X POST https://dev.to/api/articles -H "api-key: $KEY" -d @target/blog/devto.json
```

---

### How-To: Generate PDF Academic Papers

```java
// ACM Conference (two-column)
new RenderMachineLatex(new ACMTemplate(), "MyDocTest")

// arXiv Preprint
new RenderMachineLatex(new ArXivTemplate(), "MyDocTest")

// IEEE Transaction
new RenderMachineLatex(new IEEETemplate(), "MyDocTest")

// Nature Journal (enforces 200-word abstract limit via sayStringProfile)
new RenderMachineLatex(new NatureTemplate(), "MyDocTest")

// US Patent Application
new RenderMachineLatex(new UsPatentTemplate(), "MyDocTest")
```

Choose a LaTeX compiler strategy:

```java
LatexCompiler compiler = new LatexCompiler(new PdflatexStrategy());
// Or: XelatexStrategy (Unicode fonts), LatexmkStrategy (auto multi-pass), PandocStrategy
```

---

### How-To: Enforce Word Count Constraints

Use `sayStringProfile` to validate that text meets venue-specific limits at test time:

```java
@Test
void validateNatureAbstract(DtrContext ctx) {
    String abstract_ = """
        DTR is a documentation testing runtime for Java 26 that generates living documentation
        from test execution. This paper presents the Blue Ocean innovation methodology applied
        to developer tooling, demonstrating 13 new capabilities with zero new dependencies.
        """;

    ctx.sayStringProfile(abstract_);
    // Renders: word count, line count, char categories, Unicode composition

    // Enforce the Nature 200-word abstract limit
    long wordCount = abstract_.trim().split("\\s+").length;
    assertThat(wordCount)
        .as("Nature abstract must be ≤200 words")
        .isLessThanOrEqualTo(200);
}
```

---

### How-To: Profile Control Flow and Call Graphs

```java
@Test
void documentRenderMachineImpl(DtrContext ctx) throws Exception {
    ctx.sayNextSection("RenderMachineImpl Control Flow Analysis");

    var sayMethod = RenderMachineImpl.class.getDeclaredMethod("say", String.class);

    // Operation count table from Code Reflection IR (Java 26+) or signature fallback (Java 26)
    ctx.sayOpProfile(sayMethod);

    // Mermaid flowchart of basic blocks and branches (Code Reflection IR)
    ctx.sayControlFlowGraph(sayMethod);

    // Mermaid graph of all method-to-method calls in the class
    ctx.sayCallGraph(RenderMachineImpl.class);
}
```

---

### How-To: Set Up Multi-Format Output in a Single Test Run

```java
@ExtendWith(DtrExtension.class)
class MultiFormatDocTest {

    private static MultiRenderMachine multi;

    @BeforeAll
    static void setupPipeline() {
        multi = new MultiRenderMachine(
            new RenderMachineImpl("MultiFormatDocTest"),
            new RenderMachineLatex(new ACMTemplate(), "MultiFormatDocTest"),
            new BlogRenderMachine(new DevToTemplate()),
            new SlideRenderMachine(new RevealJsTemplate())
        );
    }

    @Test
    void myTest(DtrContext ctx) {
        ctx.setRenderMachine(multi);   // Swap the default machine for this test
        ctx.sayNextSection("Section A");
        ctx.say("Content rendered to Markdown, LaTeX, Blog, and Slides simultaneously.");
    }
}
```

---

## Reference — Complete API

### All `say*` Methods

DTR 2026.1.0 provides 37 method signatures across the `RenderMachineCommands` interface.

#### Core Documentation Primitives

| Method | Output | Use For |
|--------|--------|---------|
| `say(String text)` | Paragraph | Body text, explanations |
| `sayNextSection(String headline)` | H1 heading + TOC entry | Chapter/section titles |
| `sayRaw(String rawMarkdown)` | Verbatim injection | Custom Markdown, HTML spans |
| `sayCode(String code, String language)` | Fenced code block | Code examples, HTTP snippets |
| `sayJson(Object object)` | Pretty-printed JSON block | Payload examples, DTOs |
| `sayTable(String[][] data)` | Markdown table | Comparison tables, data grids |
| `sayWarning(String message)` | `[!WARNING]` callout | Critical security notes |
| `sayNote(String message)` | `[!NOTE]` callout | Tips, usage context |
| `sayKeyValue(Map<String, String> pairs)` | Two-column table | Metadata, configuration |
| `sayUnorderedList(List<String> items)` | Bullet list | Feature lists, checklists |
| `sayOrderedList(List<String> items)` | Numbered list | Steps, sequences |
| `sayAssertions(Map<String, String> assertions)` | Check/Result table | Test result matrices |
| `sayRef(DocTestRef ref)` | Cross-reference link | Inter-document navigation |
| `sayCite(String citationKey)` | BibTeX citation | Academic references |
| `sayCite(String citationKey, String pageRef)` | BibTeX citation + page | Precise citations |
| `sayFootnote(String text)` | Inline footnote | Supplementary details |

#### JVM Introspection Methods

| Method | Output | Use For |
|--------|--------|---------|
| `sayCallSite()` | Call site table | Documentation provenance |
| `sayAnnotationProfile(Class<?>)` | Annotation landscape table | Framework behavior docs |
| `sayClassHierarchy(Class<?>)` | Inheritance tree | Type structure docs |
| `sayStringProfile(String)` | Text metrics table | Constraint validation |
| `sayReflectiveDiff(Object, Object)` | Field diff table | State transition docs |

<<<<<<< HEAD
#### Code Reflection Methods (added in v2.3.0 / v2026.1.0)
=======
#### Code Reflection Methods
>>>>>>> origin/master

| Method | Output | Use For |
|--------|--------|---------|
| `sayCodeModel(Class<?>)` | Class structure table | API surface docs |
| `sayCodeModel(Method)` | Method IR breakdown | Operation profiling |
| `sayControlFlowGraph(Method)` | Mermaid flowchart | Control flow visualization |
| `sayCallGraph(Class<?>)` | Mermaid graph LR | Call relationship visualization |
| `sayOpProfile(Method)` | Operation count table | Quick performance characterization |

<<<<<<< HEAD
#### Inline Benchmarking (added in v2026.1.0)
=======
#### Inline Benchmarking
>>>>>>> origin/master

| Method | Output | Use For |
|--------|--------|---------|
| `sayBenchmark(String label, Runnable task)` | Performance table | Quick throughput/latency docs |
| `sayBenchmark(String, Runnable, int warmup, int measure)` | Performance table | Precise benchmark control |

<<<<<<< HEAD
#### Mermaid Diagram Generation (added in v2026.1.0)
=======
#### Mermaid Diagram Generation
>>>>>>> origin/master

| Method | Output | Use For |
|--------|--------|---------|
| `sayMermaid(String diagramDsl)` | Fenced `mermaid` block | Custom diagrams |
| `sayClassDiagram(Class<?>... classes)` | Mermaid classDiagram | Architecture docs |

<<<<<<< HEAD
#### Documentation Coverage & Quality (added in v2026.1.0)
=======
#### Documentation Coverage & Quality
>>>>>>> origin/master

| Method | Output | Use For |
|--------|--------|---------|
| `sayDocCoverage(Class<?>... classes)` | Coverage matrix | CI documentation gates |
| `sayContractVerification(Class<?>, Class<?>...)` | Contract matrix | Implementation compliance |
| `sayEvolutionTimeline(Class<?>, int maxEntries)` | Git timeline table | Change history docs |

<<<<<<< HEAD
#### Utility & Profiling (added in v2026.1.0)
=======
#### Utility & Profiling
>>>>>>> origin/master

| Method | Output | Use For |
|--------|--------|---------|
| `sayEnvProfile()` | Key-value table | Generated-with footer |
| `sayRecordComponents(Class<? extends Record>)` | Schema table | Record API docs |
| `sayException(Throwable)` | Exception table | Error handling docs |
| `sayAsciiChart(String, double[], String[])` | Unicode bar chart | Visual data summaries |

---

### DtrContext

`DtrContext` is injected as a JUnit 5 parameter by `DtrExtension`. It implements all
`say*` methods and exposes render pipeline control:

```java
@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void myTest(DtrContext ctx) {
        // All say* methods available on ctx
        ctx.say("Hello, DTR!");

        // Advanced: swap the render machine for this test
        ctx.setRenderMachine(new RenderMachineLatex(new ArXivTemplate(), "MyDocTest"));

        // Inspect current machine
        RenderMachine machine = ctx.getRenderMachine();
    }
}
```

---

### DtrTest (Base Class Alternative)

If you prefer inheritance over parameter injection, extend `DtrTest`:

```java
public class MyDocTest extends DtrTest {
    @Test
    void myTest() {
        // say* methods available directly (no ctx parameter needed)
        sayNextSection("Section Title");
        say("Content.");
        sayJson(Map.of("key", "value"));
    }
}
```

---

### Output Locations

By default, documentation is written to `target/docs/test-results/`:

```
target/
├── docs/
│   ├── index.md                        # Auto-generated TOC
│   └── test-results/
│       ├── GettingStartedDocTest.md    # Per-test Markdown
│       ├── GettingStartedDocTest.tex   # LaTeX (if LaTeX machine configured)
│       └── GettingStartedDocTest.html  # HTML (if HTML machine configured)
├── pdf/
│   └── GettingStartedDocTest.pdf       # Compiled PDF (if LatexCompiler runs)
├── blog/
│   ├── devto.json                      # Dev.to publish queue
│   ├── medium.json                     # Medium publish queue
│   └── queue.json                      # SocialQueueWriter manifest
└── slides/
    └── presentation.html               # Reveal.js output
```

Override the output directory via system property:
```bash
mvnd test -Ddtr.output.dir=/custom/path
```

---

### LaTeX Templates

| Template Class | Venue |
|----------------|-------|
| `ArXivTemplate` | arXiv preprints (CS, math, physics) |
| `NatureTemplate` | Nature journal (enforces 200-word abstract) |
| `IEEETemplate` | IEEE conferences and transactions |
| `ACMTemplate` | ACM SIGPLAN / SIGSOFT proceedings |
| `UsPatentTemplate` | USPTO utility patent applications |
| `LatexTemplate` | Generic LaTeX (base class) |

LaTeX compiler strategies: `PdflatexStrategy`, `XelatexStrategy`, `LatexmkStrategy`, `PandocStrategy`.

---

### Blog Platform Templates

| Template Class | Platform |
|----------------|----------|
| `MediumTemplate` | Medium |
| `SubstackTemplate` | Substack |
| `DevToTemplate` | DEV Community (Dev.to) |
| `HashnodeTemplate` | Hashnode |
| `LinkedInTemplate` | LinkedIn Articles |

---

### Annotations (Optional)

Five annotations reduce boilerplate for common documentation patterns:

```java
@DocSection("Chapter: Authentication")   // Replaces sayNextSection()
@DocDescription("Verifies OAuth 2.0")    // Replaces leading say()
@DocNote("Requires valid bearer token")  // Renders [!NOTE] callout
@DocWarning("Rate limited to 100 rps")  // Renders [!WARNING] callout
@DocCode("Authorization: Bearer <token>")// Renders code block
```

All annotations are optional and can be freely mixed with `say*` method calls.

---

### Cross-References and Citations

```java
// Register a cross-reference anchor (happens automatically on sayNextSection)
// Reference it in another test:
ctx.sayRef(new DocTestRef(AnotherDocTest.class, "Section Name", "anchor-id"));

// BibTeX citation (key must be registered in BibliographyManager)
ctx.sayCite("openjdk25");
ctx.sayCite("openjdk25", "pp. 12-15");
```

---

## Explanation — Why DTR?

### The Problem: Documentation Drift

Traditional documentation has a fundamental flaw: it is written by humans, separately from
the code it describes, and falls out of sync the moment code changes.

| Problem | Traditional Docs | DTR Living Docs |
|---------|-----------------|-----------------|
| Accuracy | Decays over time | Guaranteed by test pass |
| Effort | Write once, update forever | Write once (in test), free updates |
| Examples | May not compile | Must compile and execute |
| Formats | One output, manually | Many outputs, automatically |
| Versioning | Manual | Git-tracked with test changes |
| Drift | Common | Physically impossible |

### The Solution: Documentation as Test Execution

DTR makes documentation a side effect of test execution. When a test passes, its
documentation is accurate. When behavior changes, re-running tests regenerates documentation
to match.

```
Test passes → Documentation is accurate
Code changes → Tests break or output changes → Docs regenerate
```

### Why Java 26?

| Java 26 Feature | How DTR Uses It |
|----------------|-----------------|
| **Records** | Immutable value objects: `CallSiteRecord`, `DocTestRef`, `CitationKey`, `StringMetrics` |
| **Sealed classes** | Type-safe event routing: `sealed interface SayEvent` with exhaustive switch |
| **Pattern matching** | Compile-time exhaustiveness in render switch expressions |
| **Virtual threads** | `MultiRenderMachine` parallelism, benchmark warmup batches |
| **Text blocks** | LaTeX templates, SQL fixtures, HTML templates in test code |
| **`--enable-preview`** | Project Babylon Code Reflection API for `sayControlFlowGraph` / `sayCallGraph` |

### Why Multiple Output Formats?

Different audiences consume documentation differently:

| Format | Primary Audience |
|--------|-----------------|
| **Markdown** | Developers — version control, GitHub, documentation sites |
| **PDF (LaTeX)** | Researchers — conferences, journals, formal publications |
| **Blog JSON** | Community — Dev.to, Medium, Hashnode, Substack |
| **Reveal.js HTML** | Presenters — talks, demos, team documentation |

A single DTR test run produces all of them. The test is the single source of truth.

<<<<<<< HEAD
### Why No HTTP Client in v2026.1.0?

DTR 2026.1.0 removes the built-in HTTP client (`TestBrowserImpl`, `sayAndMakeRequest`, etc.)
=======
### Why No HTTP Client?

DTR removed the built-in HTTP client (`TestBrowserImpl`, `sayAndMakeRequest`, etc.)
>>>>>>> origin/master
because:

1. **Separation of concerns:** HTTP testing and documentation generation are distinct
   responsibilities. Mixing them caused `DtrContext` to have 40+ methods.
2. **Better alternatives exist:** `java.net.http.HttpClient` (JDK 11+), RestAssured,
   OkHttp, and Spring's `MockMvc` are purpose-built for HTTP testing and more capable.
3. **Simpler API:** `DtrContext` now has a focused, learnable API surface.

DTR remains the best tool for documenting what your HTTP tests prove — use `say*` methods
to record the request, response, and assertions explicitly.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Your JUnit 5 Test                       │
│                                                             │
│   @ExtendWith(DtrExtension.class)                           │
│   class MyTest {                                            │
│       @Test void myTest(DtrContext ctx) {                   │
│           ctx.sayNextSection("Title");      ─────────────┐  │
│           ctx.sayTable(data);              ─────────────┐│  │
│           ctx.sayBenchmark("op", task);   ─────────────┐││  │
│       }                                                 │││  │
│   }                                                     │││  │
└─────────────────────────────────────────────────────────┼┼┼──┘
                                                          │││
                                  SayEvent stream ────────┘││
                                  (sealed hierarchy)       ││
                                                           ││
┌──────────────────────────────────────────────────────────▼▼──┐
│                    DtrExtension lifecycle                     │
│  @BeforeEach: initialize DtrContext + RenderMachine           │
│  @AfterEach:  call finishAndWriteOut() → flush all output     │
└──────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌──────────────────────────────────────────────────────────────┐
│                      RenderMachine                           │
│   abstract class — template method pattern                   │
│                                                              │
│   RenderMachineImpl ────────────────────────► Markdown .md   │
│   RenderMachineLatex ───────────────────────► LaTeX .tex     │
│   BlogRenderMachine ────────────────────────► Blog .json     │
│   SlideRenderMachine ───────────────────────► Reveal.js .html│
│                                                              │
│   MultiRenderMachine ─────┬─► Markdown .md                  │
│    (virtual threads)      ├─► LaTeX .tex                    │
│                           ├─► Blog .json                    │
│                           └─► Slides .html                  │
└──────────────────────────────────────────────────────────────┘
```

**Core classes:**

| Class | Package | Role |
|-------|---------|------|
| `DtrExtension` | `junit5` | JUnit 5 lifecycle wiring |
| `DtrContext` | `junit5` | Public API surface for tests |
| `DtrCommands` | `junit5` | Interface defining all say* methods |
| `RenderMachineCommands` | `rendermachine` | Interface: all say* signatures |
| `RenderMachine` | `rendermachine` | Abstract base with no-op defaults |
| `RenderMachineImpl` | `rendermachine` | Markdown renderer |
| `RenderMachineLatex` | `rendermachine.latex` | LaTeX renderer |
| `MultiRenderMachine` | `rendermachine` | Virtual thread dispatcher |
| `BlogRenderMachine` | `render.blog` | Social platform exporter |
| `SlideRenderMachine` | `render.slides` | Reveal.js generator |
| `SayEvent` | `rendermachine` | Sealed event hierarchy |
| `DocumentAssembler` | `assembly` | Multi-section document composer |
| `CrossReferenceIndex` | `crossref` | Thread-safe anchor registry |
| `BibliographyManager` | `bibliography` | BibTeX citation registry |

---

<<<<<<< HEAD
## 📚 Tutorials — Learn by Doing

### Tutorial: Document REST API Responses

Learn how to test HTTP endpoints and auto-generate API documentation.

**1. Create a simple controller test:**

```java
@Test
void documentFetchUser() {
    sayNextSection("GET /api/users/:id");
    say("Retrieve a single user by their ID.");

    // Simulate an HTTP request (in real tests, this hits your server)
    var response = Map.of(
        "status", 200,
        "data", Map.of(
            "id", 1,
            "name", "Alice",
            "createdAt", "2026-03-11T00:00:00Z"
        )
    );

    sayJson(response.get("data"));

    sayNote("Timestamps are returned in ISO 8601 UTC format.");
}
```

**2. Run and generate:**

```bash
mvnd test -Dtest=YourApiDocTest
```

**3. Check output:**

```bash
cat target/docs/test-results/YourApiDocTest.md
```

**Result:** Self-documenting API tests that stay in sync with code.

---

### Tutorial: Compare Multiple Scenarios with Tables

Show different API responses or behavior patterns side-by-side.

```java
@Test
void documentPaymentMethods() {
    sayNextSection("Supported Payment Methods");

    sayTable(new String[][] {
        {"Method", "Processing Time", "Supported Regions", "Fees"},
        {"Credit Card", "Instant", "Global", "2.9% + $0.30"},
        {"PayPal", "1-3 hours", "Global", "3.5% + $0.50"},
        {"Bank Transfer", "3-5 days", "Europe/US", "Free"},
        {"Crypto", "10 minutes", "Global", "Network dependent"}
    });

    sayNote("Credit cards are instant but have higher fees. Choose based on your region and timeline.");
}
```

**Output:** Professional comparison table in Markdown.

---

### Tutorial: Generate PDF Academic Papers

Auto-publish test documentation as academic papers with LaTeX templates.

```java
@Test
void documentResearchFindings() {
    sayNextSection("Experimental Results");

    var results = Map.of(
        "trials", 1000,
        "success_rate", "99.2%",
        "avg_latency_ms", 45.3,
        "p_value", 0.0001
    );

    sayJson(results);

    sayWarning("This is a preview version. Results are confidential until peer review is complete.");
}
```

**Render with LaTeX:**

```java
// In your DocumentAssembler:
RenderMachineLatex latex = new RenderMachineLatex(LatexTemplate.ARXIV);
documentAssembler.addRenderMachine(latex);
// Generates: target/pdf/YourTest.pdf
```

**Output:** Publication-ready PDF with proper citations and formatting.

---

## 🎯 How-To Guides — Solve Real Problems

### How-To: Test Authentication and Document It

**Goal:** Show that your OAuth2 implementation works and document the flow.

```java
@Test
void documentOAuth2Flow() {
    sayNextSection("OAuth2 Token Exchange");
    say("Exchange authorization code for access token.");

    var request = Request.POST()
        .url(testServerUrl().path("/oauth/token"))
        .contentTypeApplicationJson()
        .payload(Map.of(
            "grant_type", "authorization_code",
            "code", "auth_code_xyz",
            "client_id", "client_123"
        ))
        .withAuth(BasicAuth.of("client_123", "secret"));

    Response response = sayAndMakeRequest(request);

    sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));

    var token = response.payloadJsonAs(new TypeReference<Map<String, String>>() {});
    sayJson(token);

    sayNote("Token expires in 1 hour. Refresh tokens are valid for 30 days.");
}
```

**Output:** Documented HTTP flow with request, response, and assertions.

---

### How-To: Export Documentation to Your Blog

**Goal:** Auto-publish test results to Dev.to or Medium.

```java
// In your test setup:
BlogRenderMachine blog = new BlogRenderMachine(new DevToTemplate());
documentAssembler.addRenderMachine(blog);

// Generated at: target/blog/devto.json
// Ready to push to Dev.to API
```

Then use the social queue:

```bash
curl -X POST https://dev.to/api/articles \
  -H "api-key: YOUR_KEY" \
  -d @target/blog/devto.json
```

**Result:** Tests automatically published as blog posts. 📝

---

### How-To: Generate OpenAPI Specs from Tests

**Goal:** Auto-generate Swagger/OpenAPI documentation.

```java
@Test
void documentApiWithOpenApi() {
    OpenApiCollector collector = new OpenApiCollector();

    Response resp = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/users"))
            .addQueryParameter("page", "1")
            .addQueryParameter("limit", "10"));

    collector.recordExchange(resp.request(), resp);

    OpenApiSpec spec = collector.buildSpec("User API", "2.5.0");
    OpenApiWriter.writeJson(spec, new File("target/openapi.json"));
    OpenApiWriter.writeYaml(spec, new File("target/openapi.yaml"));
}
```

**Result:** Swagger UI automatically generated from test execution. 📖

---

### How-To: Test WebSocket Real-Time Features

**Goal:** Document bidirectional WebSocket communication.

```java
@Test
void documentWebSocketEvents() {
    sayNextSection("WebSocket Real-Time Events");

    WebSocketClient ws = new WebSocketClientImpl("ws://localhost:8080/api/events");
    WebSocketSession session = ws.connect();

    session.send("""
        {
            "action": "subscribe",
            "channels": ["user.created", "user.updated"]
        }
        """);

    sayCode("""
        {
            "action": "subscribe",
            "channels": ["user.created", "user.updated"]
        }
        """, "json");

    WebSocketMessage event = session.receive(Duration.ofSeconds(5));
    sayJson(Map.of("event_received", event.getPayload()));

    session.close();
    sayNote("Connections persist until explicitly closed.");
}
```

**Result:** Documented real-time API behavior. 🔌

---

### How-To: Create Multi-Format Output from Single Test

**Goal:** Generate Markdown, PDF, Blog post, OpenAPI, and Slides from one test run.

```java
// Create a MultiRenderMachine that chains formats:
MultiRenderMachine multi = new MultiRenderMachine(
    new RenderMachineImpl(),              // → Markdown
    new RenderMachineLatex(...),         // → PDF
    new BlogRenderMachine(...),          // → Blog posts
    new SlideRenderMachine(...),         // → Reveal.js slides
    // new OpenApiCollector(...)          // → OpenAPI spec
);

documentAssembler.addRenderMachine(multi);
```

**Result:** A single test generates docs in 4+ formats. 🎨

---

## 📖 Reference — API & Configuration

### All `say*` Methods

| Method | Purpose | Markdown Output |
|--------|---------|---|
| `say(String)` | Paragraph | Standard paragraph |
| `sayNextSection(String)` | H1 heading + TOC | `# Title` |
| `sayCode(String, lang)` | Syntax block | `` ``` lang ... ``` `` |
| `sayTable(String[][])` | Data table | Markdown table with borders |
| `sayJson(Object)` | Pretty JSON | `` ```json ... ``` `` |
| `sayWarning(String)` | Alert box | `> [!WARNING] ...` |
| `sayNote(String)` | Info box | `> [!NOTE] ...` |
| `sayKeyValue(Map)` | Key-value table | 2-column metadata table |
| `sayUnorderedList(List)` | Bullets | `- item 1` / `- item 2` |
| `sayOrderedList(List)` | Numbered | `1. item` / `2. item` |
| `sayAssertions(Map)` | Test matrix | Check/Result table |
| `sayAndMakeRequest(Request)` | HTTP + doc | Full request/response capture |

### Request Builder (HttpClient5)

```java
Request.GET()                              // HEAD, POST, PUT, PATCH, DELETE
    .url(Url.host("http://localhost:8080").path("/api/users").uri())
    .contentTypeApplicationJson()
    .addHeader("Accept", "application/json")
    .addQueryParameter("page", "1")
    .payload(userDto)                      // Auto-serialized by Jackson
    .withAuth(oauth2Manager)               // OAuth2 token
    .withAuth(BearerTokenAuth.of("jwt")) // JWT
    .withAuth(ApiKeyAuth.of("X-API-Key", "key123"))  // Custom header
    .followRedirects(true)
    .connectTimeout(Duration.ofSeconds(5))
    .readTimeout(Duration.ofSeconds(10));
```

### Response Handler

```java
response.httpStatus()                      // int: 200, 404, 500, etc.
response.payloadAsString()                 // Raw UTF-8 string
response.payloadAsPrettyString()           // Pretty-printed JSON/XML
response.payloadAs(UserDto.class)          // Auto-detect JSON/XML
response.payloadJsonAs(new TypeReference<List<User>>(){})  // Generic types
response.headers()                         // Map<String, String>
```

### Output Locations

```
target/
├── docs/                                  # Markdown output
│   ├── index.md
│   └── test-results/YourTest.md
├── pdf/                                   # LaTeX/PDF (if enabled)
│   └── YourTest.pdf
├── slides/                                # Reveal.js presentations
│   └── presentation.html
├── blog/                                  # Blog export queue
│   ├── devto.json
│   └── queue.json
└── openapi.json / openapi.yaml            # Swagger specs
```

### LaTeX/PDF Templates

Choose your academic format:

```java
// ACM Conference Paper
new RenderMachineLatex(LatexTemplate.ACM_CONFERENCE)

// arXiv Preprint (Computer Science)
new RenderMachineLatex(LatexTemplate.ARXIV)

// IEEE Journal
new RenderMachineLatex(LatexTemplate.IEEE)

// Nature Magazine
new RenderMachineLatex(LatexTemplate.NATURE)

// US Patent
new RenderMachineLatex(LatexTemplate.US_PATENT)
```

### Blog Platform Templates

Export directly to blogging platforms:

```java
new BlogRenderMachine(new DevToTemplate())        // Dev.to
new BlogRenderMachine(new MediumTemplate())       // Medium
new BlogRenderMachine(new HashnodeTemplate())     // Hashnode
new BlogRenderMachine(new LinkedInTemplate())     // LinkedIn
new BlogRenderMachine(new SubstackTemplate())     // Substack
```

### Required Dependencies

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.5.0</version>
    <scope>test</scope>
</dependency>

<!-- Choose ONE: JUnit 4 or JUnit 5 -->
<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
    <scope>test</scope>
</dependency>
<!-- OR -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.3</version>
    <scope>test</scope>
</dependency>
```

### Compiler Configuration (Java 26 + Preview)

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>26</release>
        <compilerArgs>--enable-preview</compilerArgs>
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
```

---

## 💡 Explanation — Why DTR?

### The Problem: Stale Documentation

Traditional documentation:
- ❌ Manually written by humans
- ❌ Falls out of sync when code changes
- ❌ Requires separate tools (Swagger, Javadoc, blogs)
- ❌ No guarantee examples actually work

### The Solution: Living Documentation

DTR (Documentation Testing Runtime) generates docs from **actual test execution**:
- ✅ Always reflects current code behavior
- ✅ Examples are guaranteed to work (they're your tests)
- ✅ Single source of truth: your test suite
- ✅ Outputs multiple formats from one test run
- ✅ Version-controlled, diffable, portable Markdown

### Example: Auto-Documenting API Changes

```java
// When you change an API response
@Test
void documentUserResponse() {
    var user = new User("Alice", "alice@example.com", "admin");
    sayJson(user);  // Auto-generates updated JSON in docs
}
```

**Next test run:**
```bash
mvnd test  # Docs automatically regenerate with new response format
```

**Result:** Your API docs stay forever in sync with code. No manual updates needed.

---

### Why Java 26?

DTR targets Java 26 idioms for concise, expressive tests:

| Java 26 Feature | Use in DTR |
|---|---|
| **Records** | Immutable test data (Product, User, etc.) |
| **Sealed classes** | Type-safe test result hierarchies |
| **Pattern matching** | Clean result extraction without casts |
| **Virtual threads** | Concurrent test execution without overhead |
| **Text blocks** | Readable SQL/JSON examples in tests |
| **Switch expressions** | Exhaustive conditional logic in assertions |
| **String Templates** (Preview) | Dynamic SQL/JSON generation |
| **Scoped Values** (Preview) | Thread-safe context passing |

---

### Why Multiple Output Formats?

Different audiences need different formats:

| Format | Best For |
|---|---|
| **Markdown** | Version control, GitHub, Git diffs, documentation sites |
| **PDF (LaTeX)** | Academic papers, conferences, formal publications |
| **Blog posts** | Developer community outreach (Dev.to, Medium) |
| **OpenAPI** | Automated API client generation, Swagger UI |
| **HTML slides** | Technical presentations, live demos |

A single test generates all of them. 🎯

---

## 🚀 Getting Started

### 1. Install Java 26+ and Maven 4

```bash
# Install Java 26+ (EA) with --enable-preview
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64

# Alternatively, use SDKMAN for latest Java 26 EA
curl -s "https://get.sdkman.io" | bash
sdk install java 26.ea.13-graal

# Verify
java -version          # Shows: openjdk version "26-ea"
mvnd --version         # Shows: Maven 4.0.0-rc-5+
```

### 2. Add DTR to your `pom.xml`

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.5.0</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>6.0.3</version>
    <scope>test</scope>
</dependency>
```

### 3. Create your first test

```java
import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

public class MyFirstDocTest extends DtrTest {
    @Test
    void myFirstDoc() {
        sayNextSection("Hello World");
        say("This is my first living documentation test.");
        sayJson(Map.of("status", "success", "message", "It works!"));
    }
}
```

### 4. Run it

```bash
mvnd test
```

### 5. View output

```bash
cat target/docs/test-results/MyFirstDocTest.md
```

**Done!** You've created your first living documentation. 📚

---

## 📊 Module Structure
=======
## Module Structure
>>>>>>> origin/master

```
dtr/
├── pom.xml                                        # Parent POM, version 2026.1.0
│
├── dtr-core/                                      # Published to Maven Central
│   ├── pom.xml
│   └── src/main/java/io/github/seanchatmangpt/dtr/
│       ├── DtrTest.java                           # Base class (extends for simpler syntax)
│       ├── DocSection.java                        # @DocSection annotation
│       ├── DocDescription.java                    # @DocDescription annotation
│       ├── DocNote.java                           # @DocNote annotation
│       ├── DocWarning.java                        # @DocWarning annotation
│       ├── DocCode.java                           # @DocCode annotation
│       │
│       ├── junit5/
│       │   ├── DtrExtension.java                  # JUnit 5 @ExtendWith integration
│       │   ├── DtrContext.java                    # Primary API — all say* methods
│       │   └── DtrCommands.java                   # say* interface (implemented by DtrContext)
│       │
│       ├── rendermachine/
│       │   ├── RenderMachineCommands.java         # Interface: all 37 say* signatures
│       │   ├── RenderMachine.java                 # Abstract base with no-op defaults
│       │   ├── RenderMachineImpl.java             # Markdown rendering implementation
│       │   ├── MultiRenderMachine.java            # Virtual thread dispatcher
│       │   ├── SayEvent.java                      # Sealed event hierarchy (13 types)
│       │   └── latex/
│       │       ├── RenderMachineLatex.java        # LaTeX rendering
│       │       ├── LatexTemplate.java             # Base template class
│       │       ├── ArXivTemplate.java             # arXiv format
│       │       ├── NatureTemplate.java            # Nature journal format
│       │       ├── IEEETemplate.java              # IEEE format
│       │       ├── ACMTemplate.java               # ACM format
│       │       ├── UsPatentTemplate.java          # USPTO patent format
│       │       ├── LatexCompiler.java             # Compilation orchestration
│       │       ├── PdflatexStrategy.java          # pdflatex compiler
│       │       ├── XelatexStrategy.java           # xelatex compiler
│       │       ├── LatexmkStrategy.java           # latexmk compiler
│       │       └── PandocStrategy.java            # pandoc converter
│       │
│       ├── render/
│       │   ├── RenderMachineFactory.java          # Factory for pipeline construction
│       │   ├── LazyValue.java                     # Deferred computation wrapper
│       │   ├── blog/
│       │   │   ├── BlogRenderMachine.java         # Social platform dispatcher
│       │   │   ├── BlogTemplate.java              # Base blog template
│       │   │   ├── DevToTemplate.java             # Dev.to JSON format
│       │   │   ├── MediumTemplate.java            # Medium JSON format
│       │   │   ├── SubstackTemplate.java          # Substack newsletter format
│       │   │   ├── HashnodeTemplate.java          # Hashnode JSON format
│       │   │   ├── LinkedInTemplate.java          # LinkedIn article format
│       │   │   ├── SocialQueueEntry.java          # Platform queue entry record
│       │   │   └── SocialQueueWriter.java         # Writes queue manifest
│       │   └── slides/
│       │       ├── SlideRenderMachine.java        # Reveal.js HTML generator
│       │       ├── SlideTemplate.java             # Base slide template
│       │       └── RevealJsTemplate.java          # Reveal.js HTML format
│       │
│       ├── assembly/
│       │   ├── DocumentAssembler.java             # Multi-section composer
│       │   ├── AssemblyManifest.java              # Section ordering
│       │   ├── TableOfContents.java               # TOC builder
│       │   ├── IndexBuilder.java                  # Alphabetical index
│       │   └── WordCounter.java                   # Word count for constraint checks
│       │
│       ├── bibliography/
│       │   ├── BibliographyManager.java           # BibTeX citation registry
│       │   ├── BibTeXEntry.java                   # Citation data record
│       │   ├── BibTeXRenderer.java                # Markdown + LaTeX citation rendering
│       │   ├── CitationKey.java                   # Validated key record
│       │   └── UnknownCitationException.java      # Unknown key error
│       │
│       ├── crossref/
│       │   ├── CrossReferenceIndex.java           # Thread-safe anchor registry
│       │   ├── ReferenceResolver.java             # Lazy reference resolution
│       │   ├── DocTestRef.java                    # Cross-reference record
│       │   ├── InvalidAnchorException.java        # Unknown anchor error
│       │   └── InvalidDocTestRefException.java    # Invalid reference error
│       │
│       ├── reflectiontoolkit/
│       │   ├── CallSiteRecord.java                # StackWalker result record
│       │   ├── AnnotationProfile.java             # Annotation landscape record
│       │   ├── ClassHierarchy.java                # Superclass+interface chain record
│       │   ├── StringMetrics.java                 # Text structural metrics record
│       │   └── ReflectiveDiff.java                # Field-by-field diff record
│       │
│       ├── contract/
│       │   └── ContractVerifier.java              # Interface contract coverage
│       │
│       ├── coverage/
│       │   ├── DocCoverageAnalyzer.java           # Documentation coverage analysis
│       │   └── CoverageRow.java                   # Per-method coverage record
│       │
│       ├── evolution/
│       │   └── GitHistoryReader.java              # git log --follow wrapper
│       │
│       ├── diagram/
│       │   ├── CallGraphBuilder.java              # Mermaid call graph from Code Reflection
│       │   ├── ClassDiagramGenerator.java         # Mermaid classDiagram via reflection
│       │   ├── ControlFlowGraphBuilder.java       # Mermaid CFG from Code Reflection
│       │   └── CodeModelAnalyzer.java             # Code Reflection IR analysis
│       │
│       ├── benchmark/
│       │   └── BenchmarkRunner.java               # System.nanoTime microbenchmark engine
│       │
│       ├── config/
│       │   └── RenderConfig.java                  # Output path + pipeline configuration
│       │
│       ├── metadata/
│       │   └── DocMetadata.java                   # Test class metadata with caching
│       │
│       └── util/
│           └── StringEscapeUtils.java             # Markdown/LaTeX escaping
│
├── dtr-benchmarks/                                # JMH benchmarks (not published)
│   ├── pom.xml
│   └── src/main/java/
│       └── io/github/seanchatmangpt/dtr/bench/
│           └── RenderMachineBenchmark.java        # JMH benchmark suite
│
└── dtr-integration-test/                          # Full integration examples
    └── src/test/java/
        ├── PhDThesisDocTest.java                  # Academic paper example
<<<<<<< HEAD
        ├── BlueOceanInnovationsTest.java          # v2026.1.0 new methods showcase
=======
        ├── BlueOceanInnovationsTest.java          # 2026.1.0 new methods showcase
>>>>>>> origin/master
        ├── Java26InnovationsTest.java             # JVM introspection showcase
        ├── ExtendedSayApiDocTest.java             # Full say* API documentation
        ├── StressTest.java                        # Concurrent stress tests
        └── DtrFuzzTest.java                       # jqwik property-based tests
```

---

## Troubleshooting

### "0 tests run" in Maven output

Add `junit-jupiter-engine` explicitly — Surefire 3.x requires it:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter-engine</artifactId>
    <scope>test</scope>
</dependency>
```

Also verify the surefire version is 3.5.3+:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
</plugin>
```

### "cannot find symbol: class DtrTest" / Import errors

Use `io.github.seanchatmangpt.dtr.*` — the original `org.r10r.doctester` package was
renamed in the 2.0.0 release (now `2026.1.0` under CalVer):

```java
<<<<<<< HEAD
// Correct v2026.1.0 imports
=======
// Correct 2026.1.0 imports
>>>>>>> origin/master
import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
```

### "sayAndMakeRequest cannot be found" / "sayAndAssertThat cannot be found"

<<<<<<< HEAD
These methods were removed in v2026.1.0. DTR no longer includes an HTTP client.
=======
These methods were removed when the HTTP stack was dropped. DTR no longer includes an HTTP client.
>>>>>>> origin/master
Use `java.net.http.HttpClient` (JDK 11+) or RestAssured directly, and document
the results manually using `say*` methods. See [How-To: Test an API Endpoint](#how-to-test-an-api-endpoint-and-document-it).

### "RenderMachine sealed class violation"

<<<<<<< HEAD
DTR 2.5.0+ uses an abstract base class, not a sealed class. If you see this error,
you are still importing an older version of `dtr-core`. Update to 2026.1.0:
=======
DTR uses an abstract base class, not a sealed class. If you see this error, you are
still importing an older version of `dtr-core`. Update to the current version:
>>>>>>> origin/master

```xml
<version>2026.1.0</version>
```

### Compilation error: "preview feature requires --enable-preview"

Add `--enable-preview` to both the compiler and surefire argLine (see pom.xml
configuration in [Quick Start](#step-1-configure-your-pomxml)). Also add to
`.mvn/maven.config`:

```
--enable-preview
```

### "too many authentication attempts" from Maven Central

Start the proxy before your Maven command:

```bash
python3 maven-proxy-auth.py &
mvnd clean test
```

Add to `.mvn/jvm.config`:
```
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1
```

### "Documentation file not written"

Check that:
1. `target/docs/test-results/` exists and is writable
2. `DtrExtension` is registered: `@ExtendWith(DtrExtension.class)`
3. The test method takes `DtrContext ctx` as a parameter (if using extension pattern)
4. `DtrExtension#afterEach` is not being suppressed by a framework conflict

DTR calls `finishAndWriteOut()` in the JUnit 5 `afterEach` callback. If your test
framework swallows exceptions or skips lifecycle callbacks, the file write may be skipped.

### Maven daemon auth errors

```bash
mvnd --stop         # Stop the daemon — auth state can persist
mvnd clean test     # Fresh daemon start
```

### Verbose build output for diagnosis

```bash
mvnd -X clean test -Dtest=MyDocTest 2>&1 | tee build.log
cat target/surefire-reports/*.txt
```

---

## Changelog

<<<<<<< HEAD
<<<<<<< HEAD
- **[CLAUDE.md](./CLAUDE.md)** — Comprehensive project guide for contributors
- **[CONTRIBUTING.md](./CONTRIBUTING.md)** — Contribution guidelines and development setup
- **[Documentation](./docs/)** — Full API documentation and guides
- **[Examples](./dtr-integration-test/src/test/java/)** — Working examples

---

## 🧪 Local Testing with act

Test GitHub Actions workflows locally using [act](https://github.com/nektos/act) before pushing:

```bash
# Install act (macOS)
brew install act

# Install act (Linux)
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Run CI gate workflow locally
act -j quality-check
act -j test-coverage
act -j build-verification

# Run all jobs with Java 26
act -j build-verification --matrix java-version:26

# Dry run to see what would execute
act -n
```

**Important:** The CI workflow uses SDKMAN to install Java 26.ea.13-graal, which is compatible with `act`. Ensure you have Docker installed and running.

---

## 🚀 Deploying to Maven Central

### Prerequisites

1. **Sonatype OSSRH Account** - Sign up at [central.sonatype.com](https://central.sonatype.com)
2. **GPG Key** - Generate and upload your public key to a keyserver:
   ```bash
   gpg --gen-key
   gpg --list-keys
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   ```

### GitHub Secrets Configuration

Set these secrets in your GitHub repository settings (Settings → Secrets and variables → Actions):

| Secret | Description |
|--------|-------------|
| `CENTRAL_USERNAME` | Sonatype OSSRH username |
| `CENTRAL_TOKEN` | Sonatype OSSRH token |
| `GPG_PRIVATE_KEY` | Your GPG private key (base64 encoded) |
| `GPG_PASSPHRASE` | GPG key passphrase |
| `GPG_KEY_ID` | Last 8 characters of your GPG key ID |

### Deployment Process

1. **Create a release tag:**
   ```bash
   git tag -a v2.5.0 -m "Release v2.5.0"
   git push origin v2.5.0
   ```

2. **CI Gate automatically:**
   - Runs all quality checks
   - Verifies build on Java 21, 22, and 26
   - Checks required secrets are present
   - Triggers deployment workflow on tag push

3. **Manual deployment (if needed):**
   ```bash
   # Set env vars for Maven Central
   export CENTRAL_USERNAME="your-username"
   export CENTRAL_TOKEN="your-token"
   export GPG_PASSPHRASE="your-passphrase"

   # Deploy to Maven Central
   mvnd clean deploy -Drelease=26
   ```

4. **Verify deployment:**
   - Check [Maven Central](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)
   - Allow 10-15 minutes for synchronization

### Automatic Deployment Workflow

The `.github/workflows/ci-gate.yml` workflow automatically handles deployments when you push a version tag (`v*`):

- ✅ Runs all quality gates
- ✅ Verifies Java 21, 22, and 26 compatibility
- ✅ Checks deployment prerequisites (secrets, tag format)
- ✅ Triggers deployment to Maven Central
- ✅ Generates quality gate report

---

## 💬 Questions?
=======
See [`CHANGELOG.md`](CHANGELOG.md) for the complete version history from v2.0.0 through v2026.1.0,
including all breaking changes, migration guides, dependency tables, and architecture notes.
=======
See [`CHANGELOG.md`](CHANGELOG.md) for the complete version history, including all
breaking changes, migration guides, dependency tables, and architecture notes.

> Starting with `2026.1.0`, DTR uses **CalVer** (`YYYY.MINOR.PATCH`). Earlier versions
> used semver and are listed below for historical context.
>>>>>>> origin/master

**Version summary:**

| Version | Date | Theme |
|---------|------|-------|
<<<<<<< HEAD
| **2026.1.0** | 2026-03-13 | Blue Ocean: 14 new say* signatures, HTTP stack removed |
| **2.5.0** | 2026-03-12 | Maven Central ready, RenderMachine unsealed, caching |
| **2.4.0** | 2026-03-11 | JVM introspection: sayCallSite, sayAnnotationProfile, sayClassHierarchy, sayStringProfile, sayReflectiveDiff |
| **2.3.0** | 2026-03-11 | Multi-format pipeline (LaTeX, blog, slides), 9 new say* methods |
| **2.2.0** | 2026-03-10 | Publication-grade pipeline, bibliography, cross-references |
| **2.1.0** | 2026-03-10 | Stable JUnit 5 baseline: DtrExtension, DtrContext, DtrCommands |
| **2.0.0** | 2026-03-10 | MAJOR: Markdown-first, Java 26, JUnit 5, package rename |
=======
| **2026.1.0** | 2026-03-14 | CalVer launch: first release under YYYY.MINOR.PATCH scheme |
| 2.6.0 *(pre-CalVer)* | 2026-03-13 | Blue Ocean: 14 new say* signatures, HTTP stack removed |
| 2.5.0 *(pre-CalVer)* | 2026-03-12 | Maven Central ready, RenderMachine unsealed, caching |
| 2.4.0 *(pre-CalVer)* | 2026-03-11 | JVM introspection: sayCallSite, sayAnnotationProfile, sayClassHierarchy |
| 2.3.0 *(pre-CalVer)* | 2026-03-11 | Multi-format pipeline (LaTeX, blog, slides), 9 new say* methods |
| 2.2.0 *(pre-CalVer)* | 2026-03-10 | Publication-grade pipeline, bibliography, cross-references |
| 2.1.0 *(pre-CalVer)* | 2026-03-10 | Stable JUnit 5 baseline: DtrExtension, DtrContext, DtrCommands |
| 2.0.0 *(pre-CalVer)* | 2026-03-10 | MAJOR: Markdown-first, Java 25, JUnit 5, package rename |
>>>>>>> origin/master

---

## Contributors & History
>>>>>>> origin/master

**DTR 2026.1.0** is maintained by:

- **[Sean Chatman](https://github.com/seanchatmangpt)** (@seanchatmangpt) — architect and lead maintainer

### Original Project

DTR is a modern reimplementation and major evolution of the original
**[doctester](https://github.com/r10r-org/doctester)** project by René Aba and the r10r
organization (2013–2018). The original project pioneered the concept of test-driven
documentation generation on the JVM. DTR modernizes this concept for:

- **Java 26+** — records, sealed classes, pattern matching, virtual threads, text blocks
- **Maven Central distribution** — `io.github.seanchatmangpt.dtr:dtr-core`
- **Multi-format output** — Markdown, LaTeX, blog posts, Reveal.js slides
- **JVM introspection** — 5 bytecode-derived documentation methods
- **Blue Ocean innovations** — inline benchmarking, Mermaid diagrams, coverage analysis

<<<<<<< HEAD
### Original Project Foundation

DTR is a modern reimplementation and evolution of the original **[doctester](https://github.com/r10r-org/doctester)** project by the r10r organization. The original project provided the foundational concept of test-driven documentation generation. DTR modernizes this approach for:

- **Java 26 & Beyond** — Leveraging latest JDK features (records, sealed classes, pattern matching, virtual threads, text blocks, string templates)
- **Maven Central Distribution** — Professional package management and easy adoption
- **Enhanced Architecture** — Multi-format output (Markdown, LaTeX, HTML, OpenAPI, Blog exports)
- **CI/CD Integration** — GitHub Actions workflows for quality gates and automated deployment
- **Current Maintenance** — Active development with modern tooling and best practices

**Thank you** to the r10r-org team for the original vision that inspired this project.

### Acknowledgments

DTR builds on innovative technology including:
- **Java 26 Preview Features** — Records, sealed classes, pattern matching, virtual threads, text blocks, string templates
- **Apache HttpClient 5** — Reliable HTTP testing foundation
- **Jackson 2.x** — Flexible JSON/XML serialization
- **Guava 33.x** — Essential utilities for the JVM
- **JUnit 5 & JUnit Platform** — Industry-standard Java testing framework
- **GitHub Actions & act** — CI/CD infrastructure and local testing
- **Original doctester Project** — Foundational concept and inspiration
- **The Java Community** — For feedback, testing, and adoption

### Getting Involved

Contributions, bug reports, and feature requests are welcome! See [CONTRIBUTING.md](./CONTRIBUTING.md) for guidelines.
=======
Thank you to the r10r-org team for the foundational vision.

### Acknowledgments

DTR is built on:
- **OpenJDK 25** — records, sealed classes, virtual threads, pattern matching, `--enable-preview`
- **Apache Maven 4** + **mvnd 2.x** — build toolchain
- **JUnit 5 / Jupiter 6.0.3** — testing framework
- **Jackson 2.21.1** — JSON/XML serialization
- **jqwik 1.9.0** — property-based testing
- **BouncyCastle 1.77** — cryptography for LaTeX receipt embedding
- **Guava 33.5.0-jre** — JVM utilities
- **SLF4J 2.0.17** — logging facade
>>>>>>> origin/master

---

## License

Apache License 2.0. See [`LICENSE`](./LICENSE).

---

## See Also

- **[CHANGELOG.md](./CHANGELOG.md)** — Complete v2+ version history with migration guides
- **[CLAUDE.md](./CLAUDE.md)** — Project guide for AI-assisted development sessions
- **[docs/](./docs/)** — Architecture guides, API reference, Diataxis documentation
- **[dtr-integration-test/](./dtr-integration-test/)** — Working end-to-end examples
- **[GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)** — Bug reports
- **[GitHub Discussions](https://github.com/seanchatmangpt/dtr/discussions)** — Questions
