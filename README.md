# DocTester — Java 21-25 Living Documentation

DocTester is a **Markdown documentation generator** that writes living docs as your Java tests execute. Every test run regenerates documentation from live behavior, keeping docs in sync with reality.

**Current version:** `1.1.12-SNAPSHOT`
**License:** Apache 2.0
**Maven:** `org.r10r:doctester-core`
**Java:** 21 LTS+ (25 recommended with `--enable-preview`)

---

## Tutorials — Get Started Here

### Generate Your First Documentation Test

This tutorial walks you through creating a simple DocTest that documents a Java data structure.

**1. Create a test class:**

```java
package example;

import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Test;

public class FirstDocTest extends DocTester {

    record Product(String name, double price, boolean inStock) {}

    @Test
    void documentProducts() {
        sayNextSection("Product Catalog");
        say("Our catalog contains three products:");

        var products = List.of(
            new Product("Laptop", 999.99, true),
            new Product("Mouse", 29.99, true),
            new Product("Keyboard", 79.99, false)
        );

        sayTable(new String[][] {
            {"Name", "Price", "In Stock"},
            {"Laptop", "$999.99", "Yes"},
            {"Mouse", "$29.99", "Yes"},
            {"Keyboard", "$79.99", "No"}
        });

        sayJson(products.get(0));
    }
}
```

**2. Run the test:**

```bash
mvnd test -Dtest=FirstDocTest
```

**3. Find generated docs:**

```
target/site/doctester/FirstDocTest.md
```

---

### Document Sealed Classes with Pattern Matching

Learn how sealed classes and exhaustive pattern matching create type-safe documentation.

```java
@Test
void documentSealedResults() {
    sealed interface Result permits Result.Success, Result.Failure {
        record Success(String message) implements Result {}
        record Failure(int code, String error) implements Result {}
    }

    sayNextSection("Operation Results");

    var results = List.of(
        (Result) new Result.Success("User created"),
        new Result.Failure(404, "Not found"),
        new Result.Success("Data processed")
    );

    sayUnorderedList(results.stream()
        .map(r -> switch (r) {
            case Result.Success(var msg) -> "✓ " + msg;
            case Result.Failure(var code, var err) -> "✗ [%d] %s".formatted(code, err);
        })
        .toList()
    );

    sayNote("Sealed classes force exhaustive pattern matching. " +
            "The compiler verifies all subtypes are handled.");
}
```

---

### Use Virtual Threads for Concurrent Testing

Spawn lightweight virtual threads to document concurrent behavior.

```java
@Test
void documentConcurrentWork() throws InterruptedException {
    sayNextSection("Concurrent Task Execution");

    int threadCount = 50;
    AtomicInteger completed = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(threadCount);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Thread.sleep(10);
                    completed.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
    }

    latch.await(10, TimeUnit.SECONDS);

    sayJson(Map.of(
        "total_threads", threadCount,
        "completed", completed.get(),
        "success_rate", "%.0f%%".formatted(100.0 * completed.get() / threadCount)
    ));
}
```

---

## How-To Guides — Solve Specific Tasks

### How to Document a Multi-Line SQL Query

**Goal:** Show a SQL query in your documentation with syntax highlighting.

```java
@Test
void documentDatabaseQueries() {
    sayNextSection("User Statistics Query");

    String query = """
        SELECT u.id, u.name, COUNT(a.id) as article_count
        FROM users u
        LEFT JOIN articles a ON u.id = a.user_id
        WHERE u.active = true
        GROUP BY u.id
        ORDER BY article_count DESC;
        """;

    sayCode(query, "sql");
}
```

**Output:** A fenced code block with SQL syntax highlighting in your Markdown.

---

### How to Document a Data Structure as JSON

**Goal:** Show how a record/object serializes to JSON.

```java
@Test
void documentJsonFormat() {
    record Author(String name, String email) {}
    record Article(long id, String title, Author author) {}

    sayNextSection("Article JSON Format");

    var article = new Article(1L, "Java 25 Guide",
        new Author("Alice", "alice@example.com"));

    sayJson(article);
}
```

**Output:** Pretty-printed JSON in a code block.

---

### How to Compare Features Across Versions

**Goal:** Create a comparison table for API versions or Java features.

```java
@Test
void compareVersions() {
    sayTable(new String[][] {
        {"Feature", "Java 21", "Java 25"},
        {"Virtual Threads", "✅", "✅"},
        {"Records", "✅", "✅"},
        {"Pattern Matching", "🔄 Preview", "✅"},
        {"Sealed Classes", "✅", "✅"}
    });
}
```

**Output:** A Markdown table with clear formatting.

---

### How to Summarize Test Results

**Goal:** Document which assertions passed and which failed.

```java
@Test
void summarizeResults() {
    sayAssertions(Map.of(
        "Data loaded", "✓ PASS",
        "Validation passed", "✓ PASS",
        "Duplicate check", "✗ FAIL — found 2 duplicates"
    ));
}
```

---

### How to Highlight Critical Information

**Goal:** Add warnings and notes to your documentation.

```java
@Test
void provideWarningsAndNotes() {
    sayWarning("Virtual threads are not suitable for CPU-bound workloads. " +
               "Use them for I/O-bound operations.");

    sayNote("Text blocks (triple quotes) eliminate escape sequences " +
            "and preserve formatting automatically.");
}
```

---

## Reference — API & Technical Details

### say* Methods

| Method | Purpose | Example Output |
|--------|---------|-----------------|
| `say(String)` | Paragraph | Text with **bold**, *italic*, `code` |
| `sayNextSection(String)` | Section heading | # Heading (H1) |
| `sayRaw(String)` | Raw Markdown/HTML | Custom content (unescaped) |
| `sayTable(String[][])` | Data table | Markdown table |
| `sayCode(String, String)` | Syntax-highlighted code | ``` java code ```|
| `sayWarning(String)` | Warning callout | > [!WARNING] message |
| `sayNote(String)` | Info callout | > [!NOTE] message |
| `sayKeyValue(Map)` | Key-value pairs | 2-column table |
| `sayUnorderedList(List)` | Bullet list | - item 1<br>- item 2 |
| `sayOrderedList(List)` | Numbered list | 1. item 1<br>2. item 2 |
| `sayJson(Object)` | JSON serialization | Pretty-printed JSON code block |
| `sayAssertions(Map)` | Test results | Check/Result table |

### Output Format

All `say*` methods generate **pure Markdown**:
- **Portable:** Works on GitHub, GitLab, Gitea, and all Markdown renderers
- **Version-control friendly:** Clean text diffs
- **Tool-agnostic:** No custom CSS or JavaScript

Generated docs appear in:
```
target/site/doctester/
├── index.html
├── YourDocTest.md
└── assets/
```

### Maven & Java Setup

**Toolchain:**
- Java 21+ (25 recommended)
- Maven 4.0.0-rc-5+
- `--enable-preview` flag (for Java 25 preview features)

**Verify:**
```bash
java -version              # openjdk version "25.0.2" or higher
mvnd --version             # Maven 4.0.0+
echo $JAVA_HOME            # /usr/lib/jvm/java-25-openjdk-amd64
```

**Build commands:**
```bash
mvnd clean verify
mvnd test -Dtest=YourDocTest
mvnd --stop                # Stop Maven daemon
```

---

## Explanation — Concepts & Philosophy

### Why Records?

**Traditional classes** require constructors, getters, equals, hashCode, toString:

```java
// Old way: 20+ lines
public class User {
    private final String name;
    private final String email;

    public User(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof User u)) return false;
        return name.equals(u.name) && email.equals(u.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email);
    }

    @Override
    public String toString() {
        return "User[name=%s, email=%s]".formatted(name, email);
    }
}
```

**Records** are one-liners:

```java
// New way: 1 line
record User(String name, String email) {}
```

Records automatically provide:
- Final fields (immutable)
- Canonical constructor
- `equals()`, `hashCode()`, `toString()`
- Compact record syntax for validation

**In documentation:** Records make test data concise and readable.

---

### Why Sealed Classes?

**Inheritance without sealing** is open-ended:

```java
interface Animal { }
class Dog implements Animal { }
class Cat implements Animal { }
// Anyone can add Horse, Fish, etc. later
```

**Sealed classes** are explicit about permitted subtypes:

```java
sealed interface Animal permits Dog, Cat {
    final class Dog implements Animal { }
    final class Cat implements Animal { }
    // No other types allowed
}
```

**Benefit:** Pattern matching is exhaustive—the compiler verifies all subtypes are handled:

```java
String sound = switch (animal) {
    case Dog d -> "Woof";
    case Cat c -> "Meow";
    // No 'default' needed; compiler ensures completeness
};
```

**In documentation:** Sealed hierarchies document all possible result types without surprises.

---

### Why Virtual Threads?

**OS threads** are expensive:

```java
// Old way: Limited to ~1000 threads per process
ExecutorService pool = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10; i++) {
    pool.submit(() -> blockingWork());  // Tied up OS thread
}
```

**Virtual threads** are lightweight:

```java
// New way: Spawn thousands trivially
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(() -> blockingWork());  // Minimal overhead
    }
}
```

Virtual threads:
- Run on a small pool of OS threads (carrier threads)
- Automatically park when blocking (transparent async)
- No callback hell or reactive frameworks required
- Ideal for I/O-bound workloads (database queries, files, network)

**In documentation:** Document concurrent behavior without exhausting resources.

---

### Why Pattern Matching?

**Explicit casting** is verbose:

```java
Object payload = response;
if (payload instanceof String) {
    String str = (String) payload;  // Manual cast
    if (!str.isBlank()) {
        // use str
    }
}
```

**Pattern matching** combines checking and extraction:

```java
Object payload = response;
if (payload instanceof String str && !str.isBlank()) {
    // str is already extracted and available
}
```

Pattern matching applies to records too:

```java
record User(String name, String email) {}

if (user instanceof User(var name, var email)) {
    // name and email are extracted from the record
}
```

**In documentation:** Cleaner, more readable code examples.

---

### Why Text Blocks?

**String concatenation** obscures readability:

```java
String query = "SELECT u.id, u.name "
    + "FROM users u "
    + "WHERE u.active = true "
    + "ORDER BY u.name;";
```

**Text blocks** preserve formatting:

```java
String query = """
    SELECT u.id, u.name
    FROM users u
    WHERE u.active = true
    ORDER BY u.name;
    """;
```

Text blocks:
- Preserve indentation and line breaks
- Eliminate `\"` escape sequences
- Make multi-line strings readable as-is
- Perfect for SQL, JSON, HTML, configuration

**In documentation:** Show examples exactly as they appear.

---

## Quick Reference

### Minimal Example

```java
import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Test;

public class MyDocTest extends DocTester {

    @Test
    void myFirstDoc() {
        sayNextSection("My Section");
        say("Hello, world!");

        var items = List.of("Alice", "Bob", "Charlie");
        sayUnorderedList(items);
    }
}
```

**Run:**
```bash
mvnd test -Dtest=MyDocTest
```

**Output:** `target/site/doctester/MyDocTest.md`

---

### Java 21-25 Features at a Glance

| Feature | Java | Purpose |
|---------|------|---------|
| Records | 16+ | Immutable value objects |
| Sealed classes | 17+ | Restricted inheritance hierarchies |
| Pattern matching | 16+ (preview), 21+ (stable) | Safe type extraction |
| Virtual threads | 19 (preview), 21+ (stable) | Lightweight concurrency |
| Text blocks | 15+ | Readable multi-line strings |
| Switch expressions | 14+ | Functional switch logic |
| SequencedCollections | 21+ | First/last element access |
| var type inference | 10+ | Local variable type inference |
| Optional | 8+ | Explicit null handling |
| Streams & lambdas | 8+ | Functional data processing |

---

## Module Structure

```
doctester/
├── doctester-core/
│   ├── pom.xml
│   └── src/main/java/org/r10r/doctester/
│       ├── DocTester.java          # Base class with say* methods
│       └── rendermachine/          # Markdown generation
│           ├── RenderMachine.java
│           └── RenderMachineImpl.java
└── doctester-integration-test/
    └── src/test/java/
        └── example/
            └── Java25DocTest.java  # Full example
```

---

## Maven Dependency

```xml
<dependency>
    <groupId>org.r10r</groupId>
    <artifactId>doctester-core</artifactId>
    <version>1.1.12-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

---

## Contributing

1. Write tests with `say*` documentation methods
2. Use Java 21+ idioms (records, sealed classes, pattern matching)
3. Ensure all tests pass: `mvnd clean verify`
4. Update this README

---

## License

Apache License 2.0. See `LICENSE.md`.

---

## Support

- **Issues:** https://github.com/r10r-org/doctester/issues
- **Discussions:** https://github.com/r10r-org/doctester/discussions
