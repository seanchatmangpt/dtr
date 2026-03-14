# DTR 80/20 Essentials: The Minimal Path to Productivity

**Goal:** Master the core DTR 2.6.0 `say*` API in 45 minutes — covering the 20% of methods that handle 80% of documentation scenarios.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## The JUnit 5 Test Pattern

Every DTR test follows this structure:

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyDocTest {

    @Test
    void myTest(DtrContext ctx) {
        ctx.sayNextSection("My Section");
        ctx.say("Content here.");
    }
}
```

`DtrContext` is injected by JUnit 5 via `DtrExtension`. All `say*` calls on `ctx` produce output in `target/docs/test-results/MyDocTest.md`.

---

## The 14 Core say* Methods

### Structure and Text

| Method | Output | Use For |
|--------|--------|---------|
| `sayNextSection(String)` | H1 heading | Chapter/section titles |
| `say(String)` | Paragraph | Body text and explanations |
| `sayNote(String)` | Note block | Tips and context |
| `sayWarning(String)` | Alert block | Critical information |

```java
ctx.sayNextSection("User Management");
ctx.say("The User API supports CRUD operations.");
ctx.sayNote("All endpoints require authentication.");
ctx.sayWarning("Deleting a user is irreversible.");
```

### Data and Code

| Method | Output | Use For |
|--------|--------|---------|
| `sayCode(String, String)` | Fenced code block | Code examples |
| `sayJson(Object)` | Pretty-printed JSON | Payload examples |
| `sayTable(String[][])` | Markdown table | Data comparison |
| `sayKeyValue(Map)` | 2-column table | Metadata display |

```java
ctx.sayCode("var x = 42;", "java");
ctx.sayJson(Map.of("status", "ok", "id", 42));
ctx.sayTable(new String[][] {
    {"Name", "Email"},
    {"Alice", "alice@example.com"}
});
ctx.sayKeyValue(Map.of("Java", "25.0.2", "DTR", "2.6.0"));
```

### Lists

| Method | Output | Use For |
|--------|--------|---------|
| `sayUnorderedList(List)` | Bullet list | Features, options |
| `sayOrderedList(List)` | Numbered list | Steps, sequence |

```java
ctx.sayUnorderedList(List.of("Record schemas", "Mermaid diagrams", "ASCII charts"));
ctx.sayOrderedList(List.of("Add dependency", "Write test", "Run mvnd test"));
```

### New in 2.6.0: Measurement and Introspection

| Method | Output | Use For |
|--------|--------|---------|
| `sayBenchmark(String, Runnable)` | Timing table | Inline microbenchmarks |
| `sayEnvProfile()` | Environment table | Java/OS/heap snapshot |
| `sayRecordComponents(Class)` | Schema table | Record field documentation |
| `sayException(Throwable)` | Exception block | Error documentation |

```java
ctx.sayBenchmark("String concatenation", () -> {
    String s = "";
    for (int i = 0; i < 1000; i++) s += i;
});

ctx.sayEnvProfile();

record User(String name, String email, int age) {}
ctx.sayRecordComponents(User.class);

try {
    throw new IllegalArgumentException("Bad input");
} catch (Exception e) {
    ctx.sayException(e);
}
```

---

## Five Common Workflows

### Workflow 1: Document a Data Structure

```java
@Test
void documentUserRecord(DtrContext ctx) {
    ctx.sayNextSection("User Record Schema");
    ctx.say("The User record represents an authenticated user in the system.");

    record User(String name, String email, boolean active) {}
    ctx.sayRecordComponents(User.class);

    ctx.say("Example payload:");
    ctx.sayJson(new User("alice", "alice@example.com", true));
}
```

### Workflow 2: Inline Benchmark

```java
@Test
void benchmarkStringOps(DtrContext ctx) {
    ctx.sayNextSection("String Operation Performance");
    ctx.say("Comparing concatenation strategies on Java " + System.getProperty("java.version"));

    ctx.sayBenchmark("StringBuilder (1000 appends)", () -> {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append(i);
        sb.toString();
    });

    ctx.sayBenchmark("String.join (1000 elements)", () -> {
        String.join("", java.util.Collections.nCopies(1000, "x"));
    });
}
```

### Workflow 3: Document Exception Handling

```java
@Test
void documentExceptions(DtrContext ctx) {
    ctx.sayNextSection("Error Handling");
    ctx.say("The API throws structured exceptions for invalid input.");

    try {
        if (true) throw new IllegalArgumentException("userId must be positive, got: -1");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    ctx.sayNote("Callers should catch IllegalArgumentException and return HTTP 400.");
}
```

### Workflow 4: Document HTTP Interactions (java.net.http)

```java
@Test
void documentHttpCall(DtrContext ctx) throws Exception {
    ctx.sayNextSection("Fetching Users");
    ctx.say("Use java.net.http.HttpClient to call the user API.");

    ctx.sayCode("""
        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create("https://api.example.com/users"))
            .GET()
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        """, "java");

    ctx.sayJson(Map.of(
        "users", List.of(
            Map.of("id", 1, "name", "Alice"),
            Map.of("id", 2, "name", "Bob")
        )
    ));
}
```

### Workflow 5: Capture Environment

```java
@Test
void captureEnvironment(DtrContext ctx) {
    ctx.sayNextSection("Test Environment");
    ctx.say("This test ran in the following environment:");
    ctx.sayEnvProfile();
}
```

---

## Complete Annotated Example

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.List;
import java.util.Map;

@ExtendWith(DtrExtension.class)
class UserApiDocTest {

    record User(long id, String name, String email) {}

    @Test
    void documentUserApi(DtrContext ctx) {
        // Section heading — appears as H1 in output
        ctx.sayNextSection("User Management API");

        // Explanatory paragraph
        ctx.say("The User API provides CRUD operations for user resources. " +
                "All operations require a valid Bearer token.");

        // Document the schema
        ctx.sayRecordComponents(User.class);

        // Example payload
        ctx.say("Example user object:");
        ctx.sayJson(new User(42L, "alice", "alice@example.com"));

        // Benchmark the serialization
        ctx.sayBenchmark("JSON serialization of User", () -> {
            // simulate serialization
            var u = new User(1L, "test", "test@example.com");
            u.toString();
        });

        // Environment snapshot
        ctx.sayEnvProfile();

        // Warning
        ctx.sayWarning("User IDs are assigned by the server and must not be set by clients.");
    }
}
```

**What this test produces:**
- `target/docs/test-results/UserApiDocTest.md`
- `target/docs/test-results/UserApiDocTest.html`
- `target/docs/test-results/UserApiDocTest.tex`
- `target/docs/test-results/UserApiDocTest.json`

---

## Quick Reference Table

| Need | Method |
|------|--------|
| Section heading | `ctx.sayNextSection("Title")` |
| Body text | `ctx.say("Paragraph text.")` |
| Code example | `ctx.sayCode("code", "java")` |
| JSON payload | `ctx.sayJson(object)` |
| Markdown table | `ctx.sayTable(new String[][] {...})` |
| Key-value pairs | `ctx.sayKeyValue(map)` |
| Bullet list | `ctx.sayUnorderedList(list)` |
| Numbered list | `ctx.sayOrderedList(list)` |
| Inline benchmark | `ctx.sayBenchmark("label", runnable)` |
| Environment info | `ctx.sayEnvProfile()` |
| Record schema | `ctx.sayRecordComponents(MyRecord.class)` |
| Exception details | `ctx.sayException(throwable)` |
| Note | `ctx.sayNote("tip")` |
| Warning | `ctx.sayWarning("critical info")` |

---

## Next Steps

- [Benchmarking](benchmarking.md) — Use `sayBenchmark` for real performance documentation
- [Document Record Schemas](upload-files.md) — Deep-dive into `sayRecordComponents`
- [Mermaid Diagrams](websockets-connection.md) — Visualize type structure with `sayMermaid` and `sayClassDiagram`
- [Advanced Rendering Formats](advanced-rendering-formats.md) — Blog, LaTeX, slides from a single test
