# How-to: Text Blocks for Multiline Strings

Use Java 25 text blocks to write multiline strings naturally — perfect for HTML templates, SQL, JSON, and documentation. No escape sequences needed; indentation is automatically managed.

---

## Basic Text Block

Text blocks are delimited by triple quotes:

```java
// Old way: concatenation and escape sequences
String html = "<div>\n" +
    "  <h1>Welcome</h1>\n" +
    "  <p>This is hard to read</p>\n" +
    "</div>";

// New way: text block
String html = """
    <div>
      <h1>Welcome</h1>
      <p>Much cleaner</p>
    </div>
    """;
```

Indentation at the start of the block is stripped automatically. The result is exactly as you see it.

---

## Escape Sequences in Text Blocks

Use `\` at the end of a line to continue without a newline:

```java
// Text block with line continuation
String oneLine = """
    This is a very long line that I want to split \
    across multiple lines in the source code, \
    but keep as a single line in the result.
    """;
// Result: "This is a very long line that I want to split across multiple lines..."

// Use \n for explicit newlines
String withNewlines = """
    Line 1
    Line 2\n\
    Line 3 (forced newline before this)
    """;
```

---

## HTML Templates

Write HTML without escape sequences:

```java
String template = """
    <html>
    <head>
        <title>API Documentation</title>
        <link rel="stylesheet" href="style.css">
    </head>
    <body>
        <h1>REST API</h1>
        <div class="endpoint">
            <h2>GET /api/users</h2>
            <p>Retrieve all users</p>
        </div>
    </body>
    </html>
    """;
```

---

## JSON Strings

Natural JSON syntax without escape sequences:

```java
String jsonRequest = """
    {
        "name": "alice",
        "email": "alice@example.com",
        "roles": ["admin", "user"],
        "active": true
    }
    """;

String jsonResponse = """
    {
        "id": 42,
        "name": "alice",
        "email": "alice@example.com",
        "createdAt": "2025-01-15T10:30:00Z"
    }
    """;
```

---

## SQL Queries

Write readable SQL without concatenation:

```java
String selectQuery = """
    SELECT user.id, user.name, user.email, COUNT(orders.id) as order_count
    FROM users AS user
    LEFT JOIN orders ON user.id = orders.user_id
    WHERE user.active = true
    GROUP BY user.id
    ORDER BY order_count DESC
    LIMIT 10
    """;

String insertQuery = """
    INSERT INTO users (name, email, created_at)
    VALUES (?, ?, NOW())
    """;
```

---

## String Interpolation with `formatted()`

Combine text blocks with `String.formatted()` (replaces `String.format()`):

```java
String name = "alice";
int age = 30;

// Old way
String old = String.format("User: %s, Age: %d", name, age);

// Java 25 way: text block + formatted()
String message = """
    Welcome, %s!
    Your age is %d years.
    """.formatted(name, age);
// Result:
// Welcome, alice!
// Your age is 30 years.
```

---

## Template Literals with Variables

Embed expressions directly (Java 21+ with preview):

```java
// Requires --enable-preview
String name = "alice";
String greeting = """
    Hello, \{name}!
    Today is \{java.time.LocalDate.now()}
    """;
```

---

## Indentation Management

Leading whitespace is preserved relative to the closing `"""`:

```java
// Closing """ on the same line (unusual)
String compact = """This is a text block""";

// Closing """ on its own line (standard)
String standard = """
    Line 1
    Line 2
    """; // Closing """ determines indentation

// Extra indentation is preserved
String indented = """
        Indented line
        Another indented line
    Less indented line
    """; // Result preserves relative indentation
```

---

## Multiline Documentation Strings

Perfect for documentation in test methods:

```java
void documentComplexScenario() {
    String testDescription = """
        ## Complex Scenario: User Registration with Email Verification

        This test covers:
        - User registration via POST /api/auth/register
        - Email verification token generation
        - Token expiration after 24 hours
        - Re-sending verification emails

        Expected behavior:
        1. POST with valid email → 201 Created
        2. Email sent to user
        3. Verification link valid for 24 hours
        4. After 24 hours, link expires → 410 Gone
        """;

    System.out.println(testDescription);
}
```

---

## Comparison: Old vs. New

| Scenario | Old Way | Java 25 Way |
|----------|---------|------------|
| Single-line string | `"text"` | `"""text"""` |
| Multiline string | `"line1\nline2\n..."` | `"""\nline1\nline2\n"""` |
| Embedded quotes | `\"quoted\"` | `"quoted"` (no escape) |
| HTML/SQL blocks | Concatenation + escape | Text block (native syntax) |
| String formatting | `String.format(...)` | `.formatted(...)` |
| Indentation | Manual alignment | Automatic stripping |

---

## Best Practices

✅ **DO:**
- Use text blocks for HTML, SQL, JSON, XML
- Use `formatted()` instead of `String.format()`
- Let indentation be automatic; place closing `"""` at the dedented level
- Use `\` for line continuation when needed
- Combine with other Java 25 features (records, sealed classes)

❌ **DON'T:**
- Mix traditional strings and text blocks for the same concept
- Manually escape quotes in text blocks (not needed)
- Indent the closing `"""` unless you want that indentation included
- Use text blocks for single-line strings (less readable than `"..."`)

---

## See Also

- [Tutorial: Virtual Threads](../tutorials/virtual-threads-lightweight-concurrency.md)
- [Tutorial: Records and Sealed Classes](../tutorials/records-sealed-classes.md)
- [How-to: Use Virtual Threads](use-virtual-threads.md)
- [Reference: Java 25 Language Features](../reference/java25-features-reference.md)
