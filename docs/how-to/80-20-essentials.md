# DTR 80/20 Essentials: The Minimal Path to Productivity

**Goal:** Master the core DTR 2026.3.0 `say*` API in 30 minutes — covering the 8 essential methods that handle 80% of documentation scenarios.

**DTR Version:** 2026.3.0 | **Java:** 26+ with `--enable-preview`

---

## The JUnit Jupiter 6 Test Pattern

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

`DtrContext` is injected by JUnit Jupiter 6 via `DtrExtension`. All `say*` calls on `ctx` produce output in `target/docs/test-results/MyDocTest.md`.

---

## The 8 Essential Methods

### 1. say(String) - Simple Paragraphs

**Output:** Formatted paragraph (supports markdown)

**Use For:** Body text, explanations, descriptions

```java
ctx.say("The User API provides CRUD operations for user resources.");
ctx.say("All endpoints require authentication via Bearer token.");
```

---

### 2. sayCode(String, String) - Code Blocks

**Output:** Fenced code block with syntax highlighting

**Use For:** Code examples, configuration snippets

```java
ctx.sayCode("""
    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/users"))
        .GET()
        .build();
    """, "java");
```

---

### 3. sayTable(String[][]) - Data Tables

**Output:** Markdown table from 2D array

**Use For:** Comparisons, feature lists, data summaries

**Note:** First row = headers

```java
ctx.sayTable(new String[][] {
    {"Method", "Endpoint", "Description"},
    {"GET", "/users", "List all users"},
    {"POST", "/users", "Create new user"},
    {"GET", "/users/{id}", "Get user by ID"},
    {"DELETE", "/users/{id}", "Delete user"}
});
```

---

### 4. sayNextSection(String) - Section Headings

**Output:** H1 heading with table of contents entry

**Use For:** Chapter titles, major sections

```java
ctx.sayNextSection("User Management API");
// ... content ...
ctx.sayNextSection("Authentication");
// ... content ...
```

---

### 5. sayRef(Class, String) - Cross-References

**Output:** Hyperlink to another documentation section

**Use For:** Linking to related tests, API references

```java
ctx.sayRef(UserApiDocTest.class, "authentication");
// Produces: [authentication](./UserApiDocTest.md#authentication)
```

---

### 6. sayNote(String) - GitHub-Style Notes

**Output:** [!NOTE] alert block

**Use For:** Tips, context, helpful hints

```java
ctx.sayNote("For production use, consider implementing rate limiting.");
```

---

### 7. sayWarning(String) - GitHub-Style Warnings

**Output:** [!WARNING] alert block

**Use For:** Critical information, breaking changes, security issues

```java
ctx.sayWarning("Deleting a user is irreversible and cannot be undone.");
```

---

### 8. sayKeyValue(Map<String, String>) - Metadata Tables

**Output:** 2-column table with key-value pairs

**Use For:** Configuration, system info, status summaries

```java
ctx.sayKeyValue(Map.of(
    "Java Version", "26.ea.13",
    "DTR Version", "2026.3.0",
    "Build Status", "Passing ✓",
    "Coverage", "87%"
));
```

---

## Common Combinations

Real-world documentation often requires combining multiple methods. Here are proven patterns:

### Pattern 1: Document an API Endpoint

```java
@Test
void documentGetUserEndpoint(DtrContext ctx) {
    ctx.sayNextSection("GET /users/{id}");

    // Description
    ctx.say("Retrieves a single user by their unique ID.");

    // Request details
    ctx.sayKeyValue(Map.of(
        "Method", "GET",
        "Endpoint", "/users/{id}",
        "Auth Required", "Yes"
    ));

    // Code example
    ctx.sayCode("""
        var response = client.send(
            HttpRequest.newBuilder()
                .uri(URI.create("https://api.example.com/users/42"))
                .GET()
                .header("Authorization", "Bearer " + token)
                .build(),
            HttpResponse.BodyHandlers.ofString()
        );
        """, "java");

    // Response example
    ctx.say("Success response (200 OK):");
    ctx.sayJson(Map.of(
        "id", 42,
        "name", "Alice",
        "email", "alice@example.com"
    ));

    // Error cases
    ctx.sayTable(new String[][] {
        {"Status", "Description"},
        {"200", "User found"},
        {"404", "User not found"},
        {"401", "Unauthorized"}
    });

    // Warning
    ctx.sayWarning("User IDs are assigned by the server. Never create your own IDs.");
}
```

**Methods used:** `sayNextSection`, `say`, `sayKeyValue`, `sayCode`, `sayJson`, `sayTable`, `sayWarning`

---

### Pattern 2: Document a Configuration Option

```java
@Test
void documentRateLimiting(DtrContext ctx) {
    ctx.sayNextSection("Rate Limiting");

    ctx.say("The API implements rate limiting to prevent abuse.");

    ctx.sayKeyValue(Map.of(
        "Default Limit", "1000 requests/hour",
        "Per-IP", "Yes",
        "Customizable", "Yes"
    ));

    ctx.sayCode("""
    # Configure rate limit in application.properties
    api.rate.limit=2000
    api.rate.window=1h
    """, "properties");

    ctx.sayNote("Premium accounts have higher limits. Contact sales for details.");

    ctx.say("Response when limit exceeded:");
    ctx.sayJson(Map.of(
        "error", "rate_limit_exceeded",
        "message", "Too many requests. Try again in 5 minutes.",
        "retryAfter", 300
    ));
}
```

**Methods used:** `sayNextSection`, `say`, `sayKeyValue`, `sayCode`, `sayNote`, `sayJson`

---

### Pattern 3: Document a Multi-Step Process

```java
@Test
void documentUserRegistration(DtrContext ctx) {
    ctx.sayNextSection("User Registration Flow");

    ctx.say("New user registration requires three steps:");

    ctx.sayOrderedList(List.of(
        "Submit registration form with email and password",
        "Verify email address via confirmation link",
        "Complete profile setup"
    ));

    ctx.say("Each step is documented below:");
    ctx.sayRef(UserRegistrationDocTest.class, "step-1-submit");
    ctx.sayRef(UserRegistrationDocTest.class, "step-2-verify");
    ctx.sayRef(UserRegistrationDocTest.class, "step-3-profile");

    ctx.sayWarning("Unverified accounts are deleted after 24 hours.");
}
```

**Methods used:** `sayNextSection`, `say`, `sayOrderedList`, `sayRef`, `sayWarning`

---

## Decision Flow: "Need to Document..."

Use this flow to choose the right method(s):

```
Need to document...
│
├─ A heading or chapter title?
│  └─ Use: sayNextSection()
│
├─ A paragraph of text?
│  └─ Use: say()
│
├─ Code or configuration?
│  └─ Use: sayCode(code, "language")
│
├─ Structured data (comparisons, features)?
│  └─ Use: sayTable(2D array)
│
├─ Key-value metadata (config, status)?
│  └─ Use: sayKeyValue(Map)
│
├─ A helpful tip or context?
│  └─ Use: sayNote()
│
├─ Critical information or warnings?
│  └─ Use: sayWarning()
│
└─ Link to another section?
   └─ Use: sayRef(Class, "anchor")
```

---

## Complete Working Example

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
        // 1. Section heading
        ctx.sayNextSection("User Management API");

        // 2. Description
        ctx.say("The User API provides CRUD operations for user resources. " +
                "All operations require a valid Bearer token.");

        // 3. Metadata
        ctx.sayKeyValue(Map.of(
            "Base URL", "https://api.example.com",
            "Version", "v1",
            "Authentication", "Bearer Token"
        ));

        // 4. Available endpoints table
        ctx.sayTable(new String[][] {
            {"Method", "Endpoint", "Description"},
            {"GET", "/users", "List all users"},
            {"GET", "/users/{id}", "Get user by ID"},
            {"POST", "/users", "Create new user"},
            {"PUT", "/users/{id}", "Update user"},
            {"DELETE", "/users/{id}", "Delete user"}
        });

        // 5. Code example
        ctx.say("Example: List all users");
        ctx.sayCode("""
            var response = client.send(
                HttpRequest.newBuilder()
                    .uri(URI.create("https://api.example.com/users"))
                    .GET()
                    .header("Authorization", "Bearer " + token)
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            """, "java");

        // 6. Response example
        ctx.say("Response (200 OK):");
        ctx.sayJson(Map.of(
            "users", List.of(
                Map.of("id", 1, "name", "Alice", "email", "alice@example.com"),
                Map.of("id", 2, "name", "Bob", "email", "bob@example.com")
            ),
            "total", 2
        ));

        // 7. Helpful note
        ctx.sayNote("Use the ?page and ?limit query parameters for pagination.");

        // 8. Security warning
        ctx.sayWarning("Never expose Bearer tokens in client-side code or version control.");

        // 9. Cross-reference to auth docs
        ctx.say("See authentication details:");
        ctx.sayRef(AuthDocTest.class, "bearer-token-setup");
    }
}
```

**What this test produces:**
- `target/docs/test-results/UserApiDocTest.md`
- `target/docs/test-results/UserApiDocTest.html`
- `target/docs/test-results/UserApiDocTest.tex`
- `target/docs/test-results/UserApiDocTest.json`

---

## Quick Reference

| Need | Method | Signature |
|------|--------|-----------|
| Section heading | `ctx.sayNextSection()` | `void sayNextSection(String headline)` |
| Body text | `ctx.say()` | `void say(String text)` |
| Code block | `ctx.sayCode()` | `void sayCode(String code, String language)` |
| Data table | `ctx.sayTable()` | `void sayTable(String[][] data)` |
| Cross-reference | `ctx.sayRef()` | `void sayRef(Class<?> clazz, String anchor)` |
| Note | `ctx.sayNote()` | `void sayNote(String message)` |
| Warning | `ctx.sayWarning()` | `void sayWarning(String message)` |
| Key-value pairs | `ctx.sayKeyValue()` | `void sayKeyValue(Map<String, String> pairs)` |

---

## Next Steps

### Hands-On Tutorial
- **[Tutorial 1: Basics](basics.md)** — Write your first DTR test in 5 minutes

### Advanced Topics
- **[API Reference](../api-reference.md)** — Complete list of all 50+ `say*` methods
- **[Benchmarking](benchmarking.md)** — Performance documentation with `sayBenchmark`
- **[Mermaid Diagrams](websockets-connection.md)** — Visualize structure with `sayClassDiagram`
- **[Advanced Rendering](advanced-rendering-formats.md)** — Blog, LaTeX, slides from one test

### Reference Guides
- **[say* Method Reference](../say-method-reference.md)** — Complete method signatures and examples
- **[Testing Best Practices](../testing-best-practices.md)** — Patterns for maintainable documentation tests
