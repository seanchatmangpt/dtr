# How-to: Text Blocks for Multiline Strings

Use Java 26 text blocks to write multiline strings naturally — perfect for JSON, SQL, HTML, Mermaid DSL, and documentation content. No escape sequences needed; indentation is automatically managed.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Basic Text Block

Text blocks are delimited by triple quotes:

```java
// Old way: concatenation and escape sequences
String json = "{\"name\": \"alice\", \"email\": \"alice@example.com\"}";

// New way: text block
String json = """
    {
        "name": "alice",
        "email": "alice@example.com"
    }
    """;
```

Indentation at the start of the block is stripped automatically. The result is exactly as you see it.

---

## Text Blocks in DTR Tests

Text blocks are essential for writing `sayCode`, `sayMermaid`, and `sayJson` content cleanly:

```java
@ExtendWith(DtrExtension.class)
class TextBlockDocTest {

    @Test
    void documentWithTextBlocks(DtrContext ctx) {
        ctx.sayNextSection("Text Block Examples");

        // Use text blocks for code examples
        ctx.sayCode("""
            record User(long id, String name, String email) {}

            // Pattern matching on the record
            if (obj instanceof User(long id, String name, String email)) {
                System.out.println(name + " <" + email + ">");
            }
            """, "java");

        // Use text blocks for Mermaid diagrams
        ctx.sayMermaid("""
            sequenceDiagram
                Client->>Server: GET /api/users
                Server-->>Client: 200 OK [{...}]
            """);
    }
}
```

---

## JSON with Text Blocks

Natural JSON syntax without escape sequences:

```java
String requestBody = """
    {
        "name": "alice",
        "email": "alice@example.com",
        "roles": ["admin", "user"],
        "active": true
    }
    """;

String responseBody = """
    {
        "id": 42,
        "name": "alice",
        "email": "alice@example.com",
        "createdAt": "2026-03-14T10:00:00Z"
    }
    """;
```

---

## SQL Queries

Write readable SQL without concatenation:

```java
String query = """
    SELECT u.id, u.name, u.email, COUNT(o.id) AS order_count
    FROM users AS u
    LEFT JOIN orders AS o ON u.id = o.user_id
    WHERE u.active = true
    GROUP BY u.id
    ORDER BY order_count DESC
    LIMIT 20
    """;
```

---

## HTTP Request Documentation

Use text blocks for HTTP request/response examples in `sayCode`:

```java
ctx.sayCode("""
    POST /api/users HTTP/1.1
    Host: api.example.com
    Content-Type: application/json
    Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

    {
        "name": "charlie",
        "email": "charlie@example.com"
    }
    """, "http");

ctx.sayCode("""
    HTTP/1.1 201 Created
    Location: /api/users/43
    Content-Type: application/json

    {"id": 43, "name": "charlie", "email": "charlie@example.com"}
    """, "http");
```

---

## String Interpolation with `formatted()`

Combine text blocks with `String.formatted()`:

```java
String name = "alice";
int age = 30;

String message = """
    Welcome, %s!
    Your age is %d years.
    """.formatted(name, age);
```

---

## Escape Sequences in Text Blocks

Use `\` at the end of a line to continue without a newline:

```java
String oneLine = """
    This is a very long line that I want to split \
    across multiple lines in source, \
    but keep as a single line in the result.
    """;
```

---

## Indentation Management

Leading whitespace is preserved relative to the closing `"""`:

```java
// Standard: closing """ on its own line determines indentation
String standard = """
    Line 1
    Line 2
    """;

// Extra indentation preserved relative to closing """
String indented = """
        Indented line
        Another indented line
    Less indented line
    """;
```

---

## Mermaid DSL with Text Blocks

Text blocks make Mermaid diagrams readable:

```java
ctx.sayMermaid("""
    flowchart LR
        A[Request] --> B{Auth check}
        B -->|Authenticated| C[Process]
        B -->|Unauthenticated| D[401 Unauthorized]
        C --> E[Response]
    """);
```

Without text blocks:
```java
ctx.sayMermaid("flowchart LR\n    A[Request] --> B{Auth check}\n    B -->|Authenticated| C[Process]\n    B -->|Unauthenticated| D[401 Unauthorized]\n    C --> E[Response]");
```

The text block version is clearly superior for readability and maintenance.

---

## Comparison Table

| Scenario | Old Way | Java 26 Way |
|----------|---------|------------|
| Multiline string | `"line1\nline2\n..."` | `"""\nline1\nline2\n"""` |
| Embedded quotes | `\"quoted\"` | `"quoted"` (no escape) |
| JSON blocks | Concatenation + escapes | Text block (native syntax) |
| SQL | Concatenation | Text block |
| Mermaid DSL | Concatenation + `\n` | Text block |
| String formatting | `String.format(...)` | `.formatted(...)` |

---

## Best Practices

**Use text blocks for HTML, SQL, JSON, and Mermaid DSL.** These formats benefit most from natural indentation and no escaping.

**Use `.formatted()` instead of `String.format()`.** It chains naturally off text blocks: `""" ... """.formatted(name)`.

**Let closing `"""` determine indentation.** Place the closing `"""` at the dedented level you want as the baseline.

**Combine with DTR `say*` methods.** `sayCode`, `sayMermaid`, and `sayJson` all accept strings — text blocks make those strings readable.

---

## See Also

- [Generate Mermaid Diagrams](websockets-connection.md) — sayMermaid with text block DSL
- [Document JSON Payloads](test-json-endpoints.md) — sayJson with structured content
- [Control What Gets Documented](control-documentation.md) — Using say* with text block content
