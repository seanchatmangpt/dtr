# Reference: say* Core API Reference

**Package:** `io.github.seanchatmangpt.dtr.*`
**Version:** 2.6.0

`DtrContext` exposes 37 `say*` method signatures for generating documentation output. All methods are called on the `ctx` parameter injected by `DtrExtension`.

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
        ctx.say("Hello, documentation.");
    }
}
```

---

## Core methods

#### `say(String text)` → `void`

Renders a paragraph in the documentation output.

```java
ctx.say("This section documents the user management endpoints.");
```

**Parameters:**
- `text` — Body text; rendered as a paragraph.

---

#### `sayNextSection(String title)` → `void`

Renders an H1 heading and adds a TOC entry.

```java
ctx.sayNextSection("User Management API");
```

**Parameters:**
- `title` — Section heading text; also used as anchor ID.

---

#### `sayRaw(String raw)` → `void`

Inserts a raw string directly into the output without processing.

```java
ctx.sayRaw("> **Note:** This is raw Markdown.");
```

**Parameters:**
- `raw` — Raw content injected verbatim.

---

#### `sayCode(String code, String language)` → `void`

Renders a fenced code block with syntax highlighting.

```java
ctx.sayCode("record User(String name, String email) {}", "java");
```

**Parameters:**
- `code` — Source code string.
- `language` — Language identifier: `"java"`, `"json"`, `"bash"`, `"yaml"`, etc.

---

#### `sayJson(Object object)` → `void`

Serializes an object to pretty-printed JSON and renders it as a code block.

```java
record Config(String host, int port) {}
ctx.sayJson(new Config("localhost", 8080));
```

**Parameters:**
- `object` — Any Jackson-serializable object.

---

#### `sayTable(String[][] table)` → `void`

Renders a Markdown table. First row is treated as headers.

```java
ctx.sayTable(new String[][] {
    {"Method", "Path", "Description"},
    {"GET",    "/api/users", "List all users"},
    {"POST",   "/api/users", "Create user"}
});
```

**Parameters:**
- `table` — Two-dimensional array; `table[0]` is the header row.

---

#### `sayWarning(String message)` → `void`

Renders a warning callout block.

```java
ctx.sayWarning("Rate limit: 100 requests per minute per API key.");
```

---

#### `sayNote(String message)` → `void`

Renders an informational note block.

```java
ctx.sayNote("This endpoint requires the `users:read` scope.");
```

---

#### `sayKeyValue(Map<String, Object> map)` → `void`

Renders a two-column key/value table.

```java
ctx.sayKeyValue(Map.of(
    "Version", "2.6.0",
    "Java",    "25+",
    "Maven",   "4.0.0-rc-3+"
));
```

**Parameters:**
- `map` — Key/value pairs; order is map iteration order.

---

#### `sayUnorderedList(List<String> items)` → `void`

Renders a bulleted list.

```java
ctx.sayUnorderedList(List.of("Records", "Sealed classes", "Virtual threads"));
```

---

#### `sayOrderedList(List<String> items)` → `void`

Renders a numbered list.

```java
ctx.sayOrderedList(List.of(
    "Add dependency to pom.xml",
    "Create a DtrContext test",
    "Run mvnd test"
));
```

---

#### `sayAssertions(Map<String, Boolean> assertions)` → `void`

Renders a pass/fail checklist table.

```java
ctx.sayAssertions(Map.of(
    "Status is 200",     true,
    "Body is non-empty", true,
    "Schema is valid",   false
));
```

---

#### `sayRef(DocTestRef ref)` → `void`

Renders a cross-reference link to another DocTest.

```java
ctx.sayRef(DocTestRef.of(UserApiDocTest.class));
```

---

#### `sayCite(String citation)` → `void`

Renders a citation entry.

```java
ctx.sayCite("Goetz, B. et al. Java Concurrency in Practice. Addison-Wesley, 2006.");
```

---

#### `sayCite(String citation, String url)` → `void`

Renders a citation with a hyperlink.

```java
ctx.sayCite("JEP 444: Virtual Threads", "https://openjdk.org/jeps/444");
```

---

#### `sayFootnote(String text)` → `void`

Renders a footnote.

```java
ctx.sayFootnote("Measured on Java 25.0.2, 10M iterations, Intel i9-12900K.");
```

---

## JVM Introspection methods (since v2.4.0)

#### `sayCallSite()` → `void`

Captures and renders the call site of the invoking method (class, method name, line number).

```java
ctx.sayCallSite();
```

---

#### `sayAnnotationProfile(Class<?> clazz)` → `void`

Renders all annotations present on the given class and its members.

```java
ctx.sayAnnotationProfile(MyService.class);
```

---

#### `sayClassHierarchy(Class<?> clazz)` → `void`

Renders the full class hierarchy (superclasses and interfaces) of the given class as a tree.

```java
ctx.sayClassHierarchy(RenderMachineImpl.class);
```

---

#### `sayStringProfile(String value)` → `void`

Renders introspection data about the given string: length, encoding, character categories.

```java
ctx.sayStringProfile("Hello, 世界 🌍");
```

---

#### `sayReflectiveDiff(Object a, Object b)` → `void`

Compares two objects via reflection and renders a field-level diff table.

```java
record Config(String host, int port) {}
ctx.sayReflectiveDiff(new Config("localhost", 8080), new Config("prod.example.com", 443));
```

---

## Code Reflection methods (since v2.3.0)

#### `sayCodeModel(Class<?> clazz)` → `void`

Renders the JVM code model for all methods in the class using JEP 516 Code Reflection.

```java
ctx.sayCodeModel(UserService.class);
```

---

#### `sayCodeModel(Method method)` → `void`

Renders the JVM code model for a single method.

```java
ctx.sayCodeModel(UserService.class.getMethod("findById", long.class));
```

---

#### `sayControlFlowGraph(Method method)` → `void`

Renders the control flow graph of the given method as a Mermaid flowchart.

```java
ctx.sayControlFlowGraph(UserService.class.getMethod("processOrder", Order.class));
```

---

#### `sayCallGraph(Class<?> clazz)` → `void`

Renders the call graph of all methods within the class as a Mermaid diagram.

```java
ctx.sayCallGraph(OrderProcessor.class);
```

---

#### `sayOpProfile(Method method)` → `void`

Renders a table of JVM operations (opcodes) in the given method with frequency annotations.

```java
ctx.sayOpProfile(PaymentService.class.getMethod("charge", BigDecimal.class));
```

---

## Benchmarking methods (since v2.6.0)

#### `sayBenchmark(String label, Runnable task)` → `void`

Runs the task with default warmup (100 iterations) and measurement (1 000 iterations) and renders a benchmark result table.

```java
ctx.sayBenchmark("ArrayList add", () -> {
    var list = new ArrayList<String>();
    for (int i = 0; i < 1000; i++) list.add("item" + i);
});
```

---

#### `sayBenchmark(String label, Runnable task, int warmup, int iterations)` → `void`

Runs the task with explicit warmup and measurement counts and renders the result.

```java
ctx.sayBenchmark("Virtual thread spawn", () -> {
    Thread.ofVirtual().start(() -> {}).join();
}, 500, 10_000);
```

**Parameters:**
- `label` — Display name for the benchmark.
- `task` — The workload to measure.
- `warmup` — Number of warmup iterations (not measured).
- `iterations` — Number of measured iterations.

---

## Mermaid Diagram methods (since v2.6.0)

#### `sayMermaid(String diagram)` → `void`

Renders a raw Mermaid diagram block.

```java
ctx.sayMermaid("""
    sequenceDiagram
        Client->>Server: Request
        Server-->>Client: Response
    """);
```

---

#### `sayClassDiagram(Class<?>... classes)` → `void`

Generates and renders a Mermaid class diagram from the given classes, showing fields, methods, and relationships.

```java
ctx.sayClassDiagram(RenderMachine.class, RenderMachineImpl.class, MultiRenderMachine.class);
```

---

## Coverage and Quality methods (since v2.6.0)

#### `sayDocCoverage(Class<?>... classes)` → `void`

Renders a documentation coverage report: which public methods have `say*` coverage in any DocTest.

```java
ctx.sayDocCoverage(UserService.class, OrderService.class);
```

---

#### `sayContractVerification(Class<?> impl, Class<?>... contracts)` → `void`

Verifies and renders a table showing which interface contract methods are implemented and tested.

```java
ctx.sayContractVerification(RenderMachineImpl.class, RenderMachine.class);
```

---

#### `sayEvolutionTimeline(Class<?> clazz, int versionCount)` → `void`

Renders a timeline of API changes for the class across recent versions (read from git history).

```java
ctx.sayEvolutionTimeline(DtrContext.class, 5);
```

---

## Utility methods (since v2.6.0)

#### `sayEnvProfile()` → `void`

Renders a table of the current execution environment: Java version, OS, memory, CPU.

```java
ctx.sayEnvProfile();
```

---

#### `sayRecordComponents(Class<? extends Record> recordClass)` → `void`

Renders a table of all record components with their names, types, and generic signatures.

```java
record ApiResponse(int status, String body, Map<String, String> headers) {}
ctx.sayRecordComponents(ApiResponse.class);
```

---

#### `sayException(Throwable throwable)` → `void`

Renders an exception with message, type, and formatted stack trace.

```java
try {
    riskyOperation();
} catch (Exception e) {
    ctx.sayException(e);
}
```

---

#### `sayAsciiChart(String title, double[] values, String[] labels)` → `void`

Renders an ASCII bar chart inline in the documentation.

```java
ctx.sayAsciiChart(
    "Request latency (ms)",
    new double[]{12.3, 45.6, 8.9, 33.1},
    new String[]{"p50", "p95", "p99", "p99.9"}
);
```

**Parameters:**
- `title` — Chart title.
- `values` — Numeric values for each bar.
- `labels` — Labels for each bar; must have same length as `values`.

---

## Summary table

| Method | Since | Output |
|--------|-------|--------|
| `say(String)` | v2.0 | Paragraph |
| `sayNextSection(String)` | v2.0 | H1 heading + TOC |
| `sayRaw(String)` | v2.0 | Verbatim |
| `sayCode(String,String)` | v2.0 | Fenced code block |
| `sayJson(Object)` | v2.0 | Pretty-printed JSON |
| `sayTable(String[][])` | v2.0 | Markdown table |
| `sayWarning(String)` | v2.0 | Warning callout |
| `sayNote(String)` | v2.0 | Note callout |
| `sayKeyValue(Map)` | v2.0 | 2-column table |
| `sayUnorderedList(List)` | v2.0 | Bulleted list |
| `sayOrderedList(List)` | v2.0 | Numbered list |
| `sayAssertions(Map)` | v2.0 | Pass/fail table |
| `sayRef(DocTestRef)` | v2.0 | Cross-reference |
| `sayCite(String)` | v2.0 | Citation |
| `sayCite(String,String)` | v2.0 | Citation with link |
| `sayFootnote(String)` | v2.0 | Footnote |
| `sayCallSite()` | v2.4 | Call site info |
| `sayAnnotationProfile(Class<?>)` | v2.4 | Annotation table |
| `sayClassHierarchy(Class<?>)` | v2.4 | Class tree |
| `sayStringProfile(String)` | v2.4 | String introspection |
| `sayReflectiveDiff(Object,Object)` | v2.4 | Field diff table |
| `sayCodeModel(Class<?>)` | v2.3 | JVM code model |
| `sayCodeModel(Method)` | v2.3 | JVM code model |
| `sayControlFlowGraph(Method)` | v2.3 | CFG diagram |
| `sayCallGraph(Class<?>)` | v2.3 | Call graph diagram |
| `sayOpProfile(Method)` | v2.3 | Opcode profile |
| `sayBenchmark(String,Runnable)` | v2.6 | Benchmark result |
| `sayBenchmark(String,Runnable,int,int)` | v2.6 | Benchmark result |
| `sayMermaid(String)` | v2.6 | Mermaid diagram |
| `sayClassDiagram(Class<?>...)` | v2.6 | Class diagram |
| `sayDocCoverage(Class<?>...)` | v2.6 | Coverage report |
| `sayContractVerification(Class<?>,Class<?>...)` | v2.6 | Contract table |
| `sayEvolutionTimeline(Class<?>,int)` | v2.6 | API timeline |
| `sayEnvProfile()` | v2.6 | Environment table |
| `sayRecordComponents(Class<? extends Record>)` | v2.6 | Components table |
| `sayException(Throwable)` | v2.6 | Exception detail |
| `sayAsciiChart(String,double[],String[])` | v2.6 | ASCII bar chart |
