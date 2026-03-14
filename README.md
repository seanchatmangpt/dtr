# DTR — Documentation Testing Runtime

> **Generate living documentation as your tests execute.**
> Every test run regenerates docs in multiple formats (Markdown, LaTeX, Blog posts, PDF,
> Reveal.js slides) from live behavior — keeping docs forever in sync with reality.

**Latest:** `2026.1.0` | **License:** Apache 2.0 | **Java:** 25+ (`--enable-preview`) | **Build:** Maven 4 / mvnd 2.x | **Versioning:** [CalVer](https://calver.org) YYYY.MINOR.PATCH

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

DTR (Documentation Testing Runtime) is a Java 25 library that turns JUnit 5 tests into
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
    ctx.sayFootnote("Measurements taken on Java 25.0.2, Intel i9-13900K, 64GB RAM.");

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

DTR is documentation-only; you bring your own HTTP client. Combine standard
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
        DTR is a documentation testing runtime for Java 25 that generates living documentation
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

    // Operation count table from Code Reflection IR (Java 26+) or signature fallback (Java 25)
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

#### Code Reflection Methods

| Method | Output | Use For |
|--------|--------|---------|
| `sayCodeModel(Class<?>)` | Class structure table | API surface docs |
| `sayCodeModel(Method)` | Method IR breakdown | Operation profiling |
| `sayControlFlowGraph(Method)` | Mermaid flowchart | Control flow visualization |
| `sayCallGraph(Class<?>)` | Mermaid graph LR | Call relationship visualization |
| `sayOpProfile(Method)` | Operation count table | Quick performance characterization |

#### Inline Benchmarking

| Method | Output | Use For |
|--------|--------|---------|
| `sayBenchmark(String label, Runnable task)` | Performance table | Quick throughput/latency docs |
| `sayBenchmark(String, Runnable, int warmup, int measure)` | Performance table | Precise benchmark control |

#### Mermaid Diagram Generation

| Method | Output | Use For |
|--------|--------|---------|
| `sayMermaid(String diagramDsl)` | Fenced `mermaid` block | Custom diagrams |
| `sayClassDiagram(Class<?>... classes)` | Mermaid classDiagram | Architecture docs |

#### Documentation Coverage & Quality

| Method | Output | Use For |
|--------|--------|---------|
| `sayDocCoverage(Class<?>... classes)` | Coverage matrix | CI documentation gates |
| `sayContractVerification(Class<?>, Class<?>...)` | Contract matrix | Implementation compliance |
| `sayEvolutionTimeline(Class<?>, int maxEntries)` | Git timeline table | Change history docs |

#### Utility & Profiling

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

### Why Java 25?

| Java 25 Feature | How DTR Uses It |
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

### Why No HTTP Client?

DTR removed the built-in HTTP client (`TestBrowserImpl`, `sayAndMakeRequest`, etc.)
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

## Module Structure

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
        ├── BlueOceanInnovationsTest.java          # 2026.1.0 new methods showcase
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
// Correct 2026.1.0 imports
import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import io.github.seanchatmangpt.dtr.junit5.DtrContext;
```

### "sayAndMakeRequest cannot be found" / "sayAndAssertThat cannot be found"

These methods were removed when the HTTP stack was dropped. DTR no longer includes an HTTP client.
Use `java.net.http.HttpClient` (JDK 11+) or RestAssured directly, and document
the results manually using `say*` methods. See [How-To: Test an API Endpoint](#how-to-test-an-api-endpoint-and-document-it).

### "RenderMachine sealed class violation"

DTR uses an abstract base class, not a sealed class. If you see this error, you are
still importing an older version of `dtr-core`. Update to the current version:

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

See [`CHANGELOG.md`](CHANGELOG.md) for the complete version history, including all
breaking changes, migration guides, dependency tables, and architecture notes.

> Starting with `2026.1.0`, DTR uses **CalVer** (`YYYY.MINOR.PATCH`). Earlier versions
> used semver and are listed below for historical context.

**Version summary:**

| Version | Date | Theme |
|---------|------|-------|
| **2026.1.0** | 2026-03-14 | CalVer launch: first release under YYYY.MINOR.PATCH scheme |
| 2.6.0 *(pre-CalVer)* | 2026-03-13 | Blue Ocean: 14 new say* signatures, HTTP stack removed |
| 2.5.0 *(pre-CalVer)* | 2026-03-12 | Maven Central ready, RenderMachine unsealed, caching |
| 2.4.0 *(pre-CalVer)* | 2026-03-11 | JVM introspection: sayCallSite, sayAnnotationProfile, sayClassHierarchy |
| 2.3.0 *(pre-CalVer)* | 2026-03-11 | Multi-format pipeline (LaTeX, blog, slides), 9 new say* methods |
| 2.2.0 *(pre-CalVer)* | 2026-03-10 | Publication-grade pipeline, bibliography, cross-references |
| 2.1.0 *(pre-CalVer)* | 2026-03-10 | Stable JUnit 5 baseline: DtrExtension, DtrContext, DtrCommands |
| 2.0.0 *(pre-CalVer)* | 2026-03-10 | MAJOR: Markdown-first, Java 25, JUnit 5, package rename |

---

## Contributors & History

**DTR 2026.1.0** is maintained by:

- **[Sean Chatman](https://github.com/seanchatmangpt)** (@seanchatmangpt) — architect and lead maintainer

### Original Project

DTR is a modern reimplementation and major evolution of the original
**[doctester](https://github.com/r10r-org/doctester)** project by René Aba and the r10r
organization (2013–2018). The original project pioneered the concept of test-driven
documentation generation on the JVM. DTR modernizes this concept for:

- **Java 25+** — records, sealed classes, pattern matching, virtual threads, text blocks
- **Maven Central distribution** — `io.github.seanchatmangpt.dtr:dtr-core`
- **Multi-format output** — Markdown, LaTeX, blog posts, Reveal.js slides
- **JVM introspection** — 5 bytecode-derived documentation methods
- **Blue Ocean innovations** — inline benchmarking, Mermaid diagrams, coverage analysis

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
