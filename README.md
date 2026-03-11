# DocTester — Java 25 Living Documentation

DocTester is a **Markdown documentation generator** that writes living docs as your Java 25 tests execute. Every test run regenerates documentation from live behavior, keeping docs in sync with reality.

**Current version:** `2.0.0`
**License:** Apache 2.0
**Maven:** `org.r10r:doctester-core`
**Java:** 25 LTS with `--enable-preview`

---

## Tutorials — Get Started Here

### Generate Your First Documentation Test

This tutorial walks you through creating a simple DocTest that documents a Java data structure.

**1. Create a test class:**

```java
package example;

import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Test;
import java.util.List;

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

        sayJson(products.getFirst());
    }
}
```

**2. Run the test:**

```bash
mvnd test -Dtest=FirstDocTest
```

**3. Find generated docs:**

```
docs/test/FirstDocTest.md
```

---

### Document Sealed Classes with Exhaustive Pattern Matching

Learn how sealed classes force exhaustive pattern matching in Java 25.

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
            "The compiler verifies all subtypes are handled—no default case needed.");
}
```

---

### Spawn Virtual Threads for Concurrent Documentation

Lightweight virtual threads in Java 25 enable effortless concurrent testing.

```java
@Test
void documentConcurrentWork() throws InterruptedException {
    sayNextSection("Concurrent Task Execution");

    int threadCount = 50;
    var completed = new java.util.concurrent.atomic.AtomicInteger(0);
    var latch = new java.util.concurrent.CountDownLatch(threadCount);

    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
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

    latch.await(10, java.util.concurrent.TimeUnit.SECONDS);

    sayJson(Map.of(
        "total_threads", threadCount,
        "completed", completed.get(),
        "success_rate", "%.0f%%".formatted(100.0 * completed.get() / threadCount)
    ));

    sayNote("Virtual threads are managed by the JVM, not the OS. " +
            "Spawn thousands without exhausting system resources.");
}
```

---

### Use Text Blocks for Readable Multi-Line Code

Java 25 text blocks make complex strings readable without escape sequences.

```java
@Test
void documentTextBlocks() {
    sayNextSection("SQL Queries with Text Blocks");

    String query = """
        SELECT u.id, u.name, COUNT(a.id) as article_count
        FROM users u
        LEFT JOIN articles a ON u.id = a.user_id
        WHERE u.active = true
        GROUP BY u.id
        ORDER BY article_count DESC;
        """;

    sayCode(query, "sql");

    say("Text blocks preserve indentation and eliminate escape sequences:");

    String json = """
        {
          "status": "success",
          "timestamp": "2026-03-11T00:00:00Z",
          "data": null
        }
        """;

    sayCode(json, "json");
}
```

---

### Pattern Matching with Records and Guards

Java 25 pattern matching destructures records and enables guards (when clauses).

```java
@Test
void documentPatternMatching() {
    record User(String name, int age) {}
    record Order(User customer, double amount) {}

    sayNextSection("Pattern Matching with Guards");

    var orders = List.of(
        new Order(new User("Alice", 25), 150.00),
        new Order(new User("Bob", 17), 50.00),
        new Order(new User("Charlie", 30), 500.00)
    );

    sayUnorderedList(orders.stream()
        .map(order -> switch (order) {
            case Order(var user, var amount) when user.age() >= 21 && amount > 200 ->
                "Premium order: %s ($%.2f)".formatted(user.name(), amount);
            case Order(var user, var amount) when user.age() < 21 ->
                "Underage customer: %s ($%.2f)".formatted(user.name(), amount);
            case Order(var user, var amount) ->
                "Standard order: %s ($%.2f)".formatted(user.name(), amount);
        })
        .toList()
    );

    sayNote("Pattern matching with guards (when clauses) replaces nested if-else chains. " +
            "Java 25 supports record deconstruction, guards, and exhaustiveness checking.");
}
```

---

### SequencedCollections for Ordered Access

Java 25 SequencedCollections provide clear, safe first/last element access.

```java
@Test
void documentSequencedCollections() {
    sayNextSection("SequencedCollections for First/Last Access");

    var items = new java.util.LinkedList<>(List.of("first", "second", "third", "fourth", "fifth"));

    sayKeyValue(Map.of(
        "First item", items.getFirst(),
        "Last item", items.getLast(),
        "Total count", String.valueOf(items.size())
    ));

    say("Reversed iteration:");
    sayUnorderedList(
        items.reversed().stream()
            .map(s -> "→ " + s)
            .toList()
    );

    sayNote("SequencedCollections make intent explicit. " +
            "getFirst() is safer and clearer than get(0) or get(size()-1).");
}
```

---

## How-To Guides — Solve Specific Tasks

### How to Compare Java 25 Features

**Goal:** Show a comparison table for different Java language features.

```java
@Test
void compareJava25Features() {
    sayTable(new String[][] {
        {"Feature", "Stable?", "Use Case"},
        {"Records", "Yes", "Immutable value objects"},
        {"Sealed classes", "Yes", "Type-safe hierarchies"},
        {"Pattern matching", "Yes", "Safe type extraction"},
        {"Virtual threads", "Yes", "Lightweight concurrency"},
        {"Text blocks", "Yes", "Readable multi-line strings"},
        {"Switch expressions", "Yes", "Exhaustive logic"},
        {"Primitive patterns", "Preview", "Pattern matching on primitives"}
    });
}
```

**Output:** A Markdown table in your generated documentation.

---

### How to Serialize Objects to JSON

**Goal:** Show how a Java 25 record serializes to JSON format.

```java
@Test
void documentJsonSerialization() {
    record Author(String name, String email) {}
    record Article(long id, String title, Author author) {}

    sayNextSection("Article JSON Format");

    var article = new Article(
        1L,
        "Mastering Java 25",
        new Author("Alice Chen", "alice@example.com")
    );

    sayJson(article);
}
```

**Output:** Pretty-printed JSON code block in your documentation.

---

### How to Summarize Test Assertions

**Goal:** Document which assertions passed or failed.

```java
@Test
void summarizeTestResults() {
    sayAssertions(Map.of(
        "Data loaded successfully", "✓ PASS",
        "All records have IDs", "✓ PASS",
        "No duplicate entries", "✗ FAIL — 3 duplicates found",
        "Timestamp format valid", "✓ PASS"
    ));
}
```

**Output:** A Check/Result table showing test outcomes.

---

### How to Highlight Critical Warnings

**Goal:** Alert readers to important constraints or gotchas.

```java
@Test
void addWarningsAndNotes() {
    sayWarning("Virtual threads are for I/O-bound workloads only. " +
               "Do not use for CPU-intensive tasks—they will block carrier threads.");

    sayNote("Records are immutable by design. All fields are final and cannot be reassigned after construction.");

    sayNote("Pattern matching with primitives is a preview feature. " +
            "Compile with --enable-preview to use primitive type patterns.");
}
```

---

## Reference — API & Technical Details

### say* Methods

| Method | Purpose | Output |
|--------|---------|--------|
| `say(String)` | Paragraph text | Markdown paragraph with formatting |
| `sayNextSection(String)` | Section heading | # Heading (H1 + TOC entry) |
| `sayRaw(String)` | Raw Markdown/HTML | Custom unescaped content |
| `sayTable(String[][])` | Data table | Markdown table with headers |
| `sayCode(String, String)` | Syntax-highlighted code | ``` language code ``` |
| `sayWarning(String)` | Warning callout | > [!WARNING] message |
| `sayNote(String)` | Info callout | > [!NOTE] message |
| `sayKeyValue(Map)` | Key-value pairs | 2-column metadata table |
| `sayUnorderedList(List)` | Bullet list | - item 1<br>- item 2 |
| `sayOrderedList(List)` | Numbered list | 1. item<br>2. item |
| `sayJson(Object)` | JSON serialization | Pretty-printed JSON code block |
| `sayAssertions(Map)` | Test results | Check/Result table |

### Java 25 Compiler Setup

**Required flags:**
```bash
-source 25
-target 25
--enable-preview
```

**Maven compiler plugin:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>25</release>
        <compilerArgs>--enable-preview</compilerArgs>
    </configuration>
</plugin>
```

**Surefire for test execution:**
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.2</version>
    <configuration>
        <argLine>--enable-preview</argLine>
    </configuration>
</plugin>
```

### Maven & Java Toolchain

**Requirements:**
- Java 25 LTS (openjdk or Oracle JDK)
- Maven 4.0.0-rc-5+
- `mvnd` daemon (optional but recommended)
- `--enable-preview` flag enabled

**Verify your setup:**
```bash
java -version                                        # openjdk version "25.0.2"
mvnd --version                                       # Maven 4.0.0+
echo $JAVA_HOME                                      # /usr/lib/jvm/java-25-openjdk-amd64
```

**Build commands:**
```bash
mvnd clean verify                                    # Full build and test
mvnd test -Dtest=FirstDocTest                        # Run specific test
mvnd --stop                                          # Stop Maven daemon
```

### Generated Documentation

All `say*` methods output **pure Markdown**:
- **Portable:** Works on GitHub, GitLab, Gitea, and all Markdown renderers
- **Version-control friendly:** Clean text diffs, easy to review
- **Tool-agnostic:** No custom CSS, JavaScript, or runtime dependencies

Generated docs appear in:
```
docs/test/
├── README.md
├── YourDocTest.md
└── OtherTest.md
```

---

## Explanation — Java 25 Concepts

### Why Records?

**Traditional classes** require boilerplate for value objects:

```java
// Verbose: 25+ lines
public class Product {
    private final String name;
    private final double price;

    public Product(String name, double price) {
        this.name = name;
        this.price = price;
    }

    public String name() { return name; }
    public double price() { return price; }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Product p)) return false;
        return name.equals(p.name) && price == p.price;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, price);
    }

    @Override
    public String toString() {
        return "Product[name=%s, price=%s]".formatted(name, price);
    }
}
```

**Java 25 records** eliminate all boilerplate:

```java
// Concise: 1 line
record Product(String name, double price) {}
```

Records provide:
- **Immutability:** All fields are final
- **Canonical constructor:** Auto-generated with parameter validation via compact constructor
- **Accessor methods:** `name()`, `price()` (not getters)
- **Equality:** Auto-generated `equals()`, `hashCode()`
- **String representation:** Auto-generated `toString()`

**In DocTests:** Records make test data clearer and more concise.

---

### Why Sealed Classes?

**Unrestricted inheritance** allows anyone to extend your types:

```java
sealed interface Payment {
    // Anyone can add CreditCard, PayPal, Crypto, etc.
}
```

**Sealed classes** restrict implementations to known subtypes:

```java
sealed interface Payment permits CreditCard, PayPal, BankTransfer {
    record CreditCard(String cardNumber) implements Payment {}
    record PayPal(String email) implements Payment {}
    record BankTransfer(String iban) implements Payment {}
}
```

**Benefit:** Pattern matching is exhaustive—the compiler forces you to handle all cases:

```java
String method = switch (payment) {
    case CreditCard(var card) -> "Credit card ending in " + card.substring(card.length() - 4);
    case PayPal(var email) -> "PayPal: " + email;
    case BankTransfer(var iban) -> "Bank transfer: " + iban;
    // No 'default' needed; compiler verifies completeness
};
```

**In DocTests:** Sealed classes document all possible result types without surprises or missing cases.

---

### Why Virtual Threads?

**OS threads** are heavyweight and limited:

```java
// Old way: Only ~1000 threads per JVM process
ExecutorService pool = Executors.newFixedThreadPool(100);
for (int i = 0; i < 100; i++) {
    pool.submit(() -> blockingWork());  // Each tied to an OS thread
}
```

**Virtual threads** are lightweight and abundant:

```java
// New way: Spawn 100,000 without resource exhaustion
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 100_000; i++) {
        executor.submit(() -> blockingWork());  // Managed by JVM, not OS
    }
}
```

Virtual threads:
- Run on a small pool of carrier threads (OS threads)
- Automatically park when blocking (transparent, no callbacks)
- Resume on a carrier thread when ready (no await/async needed)
- Ideal for I/O-bound workloads (database queries, files, network calls)
- No special APIs—use `Thread.sleep()`, blocking I/O, locks normally

**Java 25 guarantee:** Virtual threads are stable and production-ready.

**In DocTests:** Document concurrent behavior without spawning hundreds of OS threads.

---

### Why Pattern Matching?

**Type checks with explicit casting** are verbose:

```java
Object value = getResult();
if (value instanceof String) {
    String str = (String) value;  // Manual cast after instanceof check
    if (!str.isBlank()) {
        // use str
    }
}
```

**Pattern matching** combines the check and extraction:

```java
Object value = getResult();
if (value instanceof String str && !str.isBlank()) {
    // str is extracted; no separate cast needed
}
```

**Record pattern deconstruction** in Java 25:

```java
record User(String name, int age) {}

if (user instanceof User(var name, var age) && age >= 21) {
    // name and age are extracted from the record pattern
}
```

**In switch expressions:**

```java
String label = switch (user) {
    case User(var name, var age) when age >= 18 -> "Adult: " + name;
    case User(var name, var _) -> "Minor: " + name;  // _ = unnamed pattern
};
```

Java 25 pattern matching:
- Eliminates redundant variable declarations
- Guards (when clauses) enable conditional logic
- Exhaustive checking by the compiler
- Works with records, sealed classes, and primitives (preview)

---

### Why Text Blocks?

**String concatenation** obscures readability:

```java
String sql = "SELECT u.id, u.name, COUNT(a.id) as cnt "
    + "FROM users u "
    + "LEFT JOIN articles a ON u.id = a.user_id "
    + "WHERE u.active = true "
    + "GROUP BY u.id "
    + "ORDER BY cnt DESC;";
```

**Text blocks** preserve formatting exactly:

```java
String sql = """
    SELECT u.id, u.name, COUNT(a.id) as cnt
    FROM users u
    LEFT JOIN articles a ON u.id = a.user_id
    WHERE u.active = true
    GROUP BY u.id
    ORDER BY cnt DESC;
    """;
```

Text blocks:
- Preserve indentation and line breaks
- Eliminate `\"` escape sequences
- Make multi-line strings readable as-is
- Perfect for SQL, JSON, HTML, configuration

**In DocTests:** Show examples exactly as they appear in production.

---

## Quick Reference

### Minimal Example

```java
import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Test;
import java.util.List;

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

**Output:** `docs/test/MyDocTest.md`

---

### Java 25 Core Features

| Feature | Purpose | Java 25 Status |
|---------|---------|---|
| Records | Immutable value objects | Stable |
| Sealed classes | Restricted inheritance hierarchies | Stable |
| Pattern matching | Safe type extraction with guards | Stable |
| Virtual threads | Lightweight concurrency | Stable |
| Text blocks | Readable multi-line strings | Stable |
| Switch expressions | Functional switch logic | Stable |
| SequencedCollections | First/last element access | Stable |
| Primitive type patterns | Pattern matching on int, long, etc. | Preview |
| Unnamed patterns | Wildcard patterns with `_` | Preview |
| var type inference | Local variable type inference | Stable |

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
    <version>2.0.0</version>
    <scope>test</scope>
</dependency>
```

---

## Contributing

1. Write tests using Java 25 idioms (records, sealed classes, pattern matching, virtual threads)
2. Document with `say*` methods
3. Ensure all tests pass: `mvnd clean verify`
4. Update this README

---

## License

Apache License 2.0. See `LICENSE.md`.

---

## Support

- **Issues:** https://github.com/r10r-org/doctester/issues
- **Discussions:** https://github.com/r10r-org/doctester/discussions
