# DTR 2.6.0 — 80/20 Quick Reference

**One-page cheat sheet for DTR 2.6.0.** Bookmark this or print it.

---

## Test class boilerplate

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void myTest(DtrContext ctx) {
        ctx.sayNextSection("Section Title");
        ctx.say("Explain what this documents.");
    }
}
```

**Output:** `target/docs/test-results/MyDocTest.md` (+ `.html`, `.tex`, `.json`)

---

## All 37 say* methods

### Core (16)

| Method | Output |
|--------|--------|
| `ctx.say(String)` | Paragraph |
| `ctx.sayNextSection(String)` | H1 heading + TOC |
| `ctx.sayRaw(String)` | Verbatim text |
| `ctx.sayCode(String, lang)` | Fenced code block |
| `ctx.sayJson(Object)` | Pretty-printed JSON |
| `ctx.sayTable(String[][])` | Markdown table |
| `ctx.sayWarning(String)` | Warning callout |
| `ctx.sayNote(String)` | Note callout |
| `ctx.sayKeyValue(Map)` | 2-column table |
| `ctx.sayUnorderedList(List)` | Bulleted list |
| `ctx.sayOrderedList(List)` | Numbered list |
| `ctx.sayAssertions(Map<String,Boolean>)` | Pass/fail checklist |
| `ctx.sayRef(DocTestRef)` | Cross-reference |
| `ctx.sayCite(String)` | Citation |
| `ctx.sayCite(String, String)` | Citation with URL |
| `ctx.sayFootnote(String)` | Footnote |

### JVM Introspection (5) — since v2.4.0

| Method | Output |
|--------|--------|
| `ctx.sayCallSite()` | Call site location |
| `ctx.sayAnnotationProfile(Class<?>)` | Annotation table |
| `ctx.sayClassHierarchy(Class<?>)` | Class tree |
| `ctx.sayStringProfile(String)` | String metrics |
| `ctx.sayReflectiveDiff(Object, Object)` | Field diff table |

### Code Reflection (5) — since v2.3.0

| Method | Output |
|--------|--------|
| `ctx.sayCodeModel(Class<?>)` | JVM code model |
| `ctx.sayCodeModel(Method)` | JVM code model |
| `ctx.sayControlFlowGraph(Method)` | Mermaid CFG |
| `ctx.sayCallGraph(Class<?>)` | Mermaid call graph |
| `ctx.sayOpProfile(Method)` | Opcode profile |

### Benchmarking (2) — NEW in v2.6.0

| Method | Output |
|--------|--------|
| `ctx.sayBenchmark(String, Runnable)` | Benchmark result table (100 warmup, 1K iter) |
| `ctx.sayBenchmark(String, Runnable, int, int)` | Benchmark result table (explicit counts) |

### Mermaid (2) — NEW in v2.6.0

| Method | Output |
|--------|--------|
| `ctx.sayMermaid(String)` | Any Mermaid diagram |
| `ctx.sayClassDiagram(Class<?>...)` | Generated class diagram |

### Coverage and Quality (3) — NEW in v2.6.0

| Method | Output |
|--------|--------|
| `ctx.sayDocCoverage(Class<?>...)` | Documentation coverage report |
| `ctx.sayContractVerification(Class<?>, Class<?>...)` | Interface compliance table |
| `ctx.sayEvolutionTimeline(Class<?>, int)` | API change timeline |

### Utility (4) — NEW in v2.6.0

| Method | Output |
|--------|--------|
| `ctx.sayEnvProfile()` | Java/OS/memory table |
| `ctx.sayRecordComponents(Class<? extends Record>)` | Record component table |
| `ctx.sayException(Throwable)` | Exception detail block |
| `ctx.sayAsciiChart(String, double[], String[])` | ASCII bar chart |

---

## RenderMachine quick reference

```java
// Default (Markdown + HTML + LaTeX + JSON)
ctx.getRenderMachine();  // RenderMachineImpl

// Replace with multi-format output
ctx.setRenderMachine(new MultiRenderMachine(
    new RenderMachineImpl(),
    new RenderMachineLatex(new IEEETemplate(), new PdflatexStrategy()),
    new BlogRenderMachine(new DevToTemplate()),
    new SlideRenderMachine()
));
```

**RenderMachine implementations:**

| Class | Output |
|-------|--------|
| `RenderMachineImpl` | Markdown + HTML + LaTeX + JSON |
| `RenderMachineLatex` | LaTeX / PDF |
| `BlogRenderMachine` | Social blog post |
| `SlideRenderMachine` | Reveal.js slides |
| `MultiRenderMachine` | All above via virtual threads |

**LaTeX templates:** `ArXivTemplate`, `NatureTemplate`, `IEEETemplate`, `ACMTemplate`, `UsPatentTemplate`

**Blog templates:** `DevToTemplate`, `MediumTemplate`, `SubstackTemplate`, `HashnodeTemplate`, `LinkedInTemplate`

**LaTeX compilers:** `PdflatexStrategy`, `XelatexStrategy`, `LatexmkStrategy`, `PandocStrategy`

---

## Common patterns

### Basic documentation

```java
ctx.sayNextSection("User API");
ctx.say("Returns all registered users.");
ctx.sayCode("List<User> users = userService.findAll();", "java");
ctx.sayTable(new String[][] {
    {"Field", "Type", "Description"},
    {"id",    "long",   "Unique identifier"},
    {"name",  "String", "Display name"},
});
```

### Benchmark with environment

```java
ctx.sayNextSection("Performance");
ctx.sayEnvProfile();  // always first
ctx.sayBenchmark("ArrayList 1K add", () -> {
    var list = new java.util.ArrayList<String>();
    for (int i = 0; i < 1_000; i++) list.add("item" + i);
}, 100, 5_000);
ctx.sayAsciiChart("Results (ns)", new double[]{12.3, 45.6}, new String[]{"avg", "p99"});
```

### Mermaid diagram

```java
ctx.sayMermaid("""
    sequenceDiagram
        Client->>DtrContext: say*()
        DtrContext->>RenderMachine: say*()
        RenderMachine-->>Client: void
    """);
```

### Record introspection

```java
record ApiResponse(int status, String body) {}
ctx.sayRecordComponents(ApiResponse.class);
ctx.sayReflectiveDiff(
    new ApiResponse(200, "OK"),
    new ApiResponse(404, "Not Found")
);
```

### Exception documentation

```java
try {
    riskyOp();
} catch (Exception e) {
    ctx.sayException(e);
}
```

### Coverage audit

```java
ctx.sayDocCoverage(UserService.class);
ctx.sayContractVerification(UserServiceImpl.class, UserService.class);
ctx.sayEvolutionTimeline(DtrContext.class, 5);
```

---

## Run tests

```bash
# Run a specific DocTest
mvnd test -pl dtr-integration-test -Dtest=MyDocTest

# Run all tests
mvnd test

# View output
ls target/docs/test-results/
cat target/docs/test-results/MyDocTest.md
```

---

## REMOVED in 2.6.0

The following classes were removed. Do not use them.

| Removed | Replacement |
|---------|-------------|
| `TestBrowser` / `TestBrowserImpl` | No replacement — HTTP testing is out of scope |
| `Request.GET()` / `.POST()` etc. | No replacement |
| `Response.httpStatus()` / `.payloadAs()` | No replacement |
| `Url` builder | No replacement |
| `sayAndMakeRequest(Request)` | No replacement |
| `sayAndAssertThat(String, T, Matcher)` | Use JUnit assertions + `ctx.sayAssertions(Map)` |
| `WebSocketClient`, `WebSocketSession` | No replacement |
| `ServerSentEventsClient` | No replacement |
| `BearerTokenAuth`, `ApiKeyAuth`, `BasicAuth` | No replacement |
| `HttpConstants` | No replacement |

---

**DTR 2.6.0** — `io.github.seanchatmangpt.dtr:dtr-core:2.6.0` — Java 26+ — `--enable-preview`
