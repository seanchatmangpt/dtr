# DocTester — Java 21-25 Living Documentation Framework

DocTester is a **Java 21+ documentation generation framework** that generates clean, portable **Markdown documentation** while your Java tests execute. It's built to showcase modern Java idioms—records, sealed classes, virtual threads, pattern matching, and more—while automatically rendering your code behavior as living documentation.

**Core insight:** Your tests ARE your documentation. Every test execution regenerates your docs from live behavior, keeping documentation in sync with reality.

[![Build Status](https://api.travis-ci.org/r10r-org/doctester.svg)](https://travis-ci.org/r10r-org/doctester)

**Current version:** `1.1.12-SNAPSHOT`
**License:** Apache 2.0
**Maven coordinates:** `org.r10r:doctester-core`
**Java requirement:** Java 21 LTS or higher (Java 25 recommended)

---

## Why Java 21+?

DocTester targets **Java 21+** developers, using modern idioms that make code clearer and safer:

- **Records** eliminate boilerplate for immutable value objects
- **Sealed classes** + **pattern matching** express type hierarchies precisely
- **Virtual threads** enable lightweight concurrent testing scenarios
- **Text blocks** make multi-line strings readable (SQL, JSON, configuration)
- **Switch expressions** are exhaustive—the compiler ensures all cases are covered
- **SequencedCollections** reduce off-by-one errors in ordered collections
- **Stream API** with method references for functional data processing
- **Optional** for explicit null handling
- **var** type inference for cleaner local variable declarations

This README demonstrates how to **leverage these features in your documentation tests**.

---

## The say* API — 12 Documentation Methods

DocTester provides a fluent `say*` API for rich documentation generation. All output is **pure Markdown**, making docs portable, version-control friendly, and tool-agnostic.

### Core Methods

#### 1. `say(String text)` — Paragraphs

Render narrative text with optional Markdown formatting.

```java
@Test
void demonstrateBasicDocumentation() {
    say("This is a simple paragraph. You can use **bold**, *italic*, and `code`.");
    say("Multiple paragraphs add narrative context to your tests.");
}
```

**Output:**
```markdown
This is a simple paragraph. You can use **bold**, *italic*, and `code`.

Multiple paragraphs add narrative context to your tests.
```

---

#### 2. `sayNextSection(String headline)` — Section Headers

Create top-level section headings that appear in the table of contents.

```java
@Test
void documentBySection() {
    sayNextSection("Java Records for Immutable Data");
    say("Records eliminate constructor boilerplate...");

    sayNextSection("Pattern Matching Capabilities");
    say("Pattern matching reduces casting verbosity...");
}
```

**Output:**
```markdown
# Java Records for Immutable Data

Records eliminate constructor boilerplate...

# Pattern Matching Capabilities

Pattern matching reduces casting verbosity...
```

---

#### 3. `sayRaw(String rawMarkdown)` — Raw Content Injection

Inject raw Markdown or HTML directly (no escaping).

```java
@Test
void injectCustomMarkdown() {
    sayRaw("""
        > **Important:** This is a blockquote with raw HTML.
        > You have full control over the output.
        """);
}
```

---

#### 4. `sayTable(String[][] data)` — Markdown Tables

Generate clean, readable tables from 2D arrays. First row is treated as headers.

```java
@Test
void compareJavaVersions() {
    sayTable(new String[][] {
        {"Feature", "Java 21", "Java 25"},
        {"Virtual Threads", "✅ Stable", "✅ Stable"},
        {"Records", "✅ Stable", "✅ Stable"},
        {"Sealed Classes", "✅ Stable", "✅ Stable"},
        {"Pattern Matching", "🔄 Preview", "✅ Stable"}
    });
}
```

**Output:**
```markdown
| Feature | Java 21 | Java 25 |
| --- | --- | --- |
| Virtual Threads | ✅ Stable | ✅ Stable |
| Records | ✅ Stable | ✅ Stable |
| Sealed Classes | ✅ Stable | ✅ Stable |
| Pattern Matching | 🔄 Preview | ✅ Stable |
```

---

#### 5. `sayCode(String code, String language)` — Syntax-Highlighted Code Blocks

Render code with language hints for proper syntax highlighting.

```java
@Test
void documentCodeExamples() {
    sayCode("SELECT u.id, u.name, COUNT(a.id) as article_count " +
            "FROM users u LEFT JOIN articles a ON u.id = a.user_id " +
            "WHERE u.active = true GROUP BY u.id;", "sql");

    sayCode("""
        public record User(String name, String email) {}
        """, "java");
}
```

**Output:**
````markdown
```sql
SELECT u.id, u.name, COUNT(a.id) as article_count
FROM users u LEFT JOIN articles a ON u.id = a.user_id
WHERE u.active = true GROUP BY u.id;
```

```java
public record User(String name, String email) {}
```
````

---

#### 6. `sayWarning(String message)` — Warning Alerts

Render GitHub-style warning callouts for critical information.

```java
@Test
void highlightCriticalInfo() {
    sayWarning("Virtual threads are not a replacement for blocking operations. " +
               "Use them for I/O-bound workloads, not CPU-bound tasks.");
}
```

**Output:**
```markdown
> [!WARNING]
> Virtual threads are not a replacement for blocking operations. Use them for I/O-bound workloads, not CPU-bound tasks.
```

---

#### 7. `sayNote(String message)` — Info Alerts

Render GitHub-style info callouts for helpful context.

```java
@Test
void provideContext() {
    sayNote("Pattern matching with guards (when clauses) enables conditional extraction. " +
            "This reduces defensive code and improves readability.");
}
```

**Output:**
```markdown
> [!NOTE]
> Pattern matching with guards (when clauses) enables conditional extraction. This reduces defensive code and improves readability.
```

---

#### 8. `sayKeyValue(Map<String, String> pairs)` — Key-Value Pairs

Display metadata, configuration, or headers in a clean 2-column table.

```java
@Test
void documentConfiguration() {
    sayKeyValue(Map.of(
        "Java Version", "openjdk version \"25.0.2\"",
        "Maven", "Apache Maven 4.0.0-rc-5",
        "Encoding", "UTF-8",
        "Preview Flag", "--enable-preview"
    ));
}
```

**Output:**
```markdown
| Key | Value |
| --- | --- |
| Java Version | openjdk version "25.0.2" |
| Maven | Apache Maven 4.0.0-rc-5 |
| Encoding | UTF-8 |
| Preview Flag | --enable-preview |
```

---

#### 9. `sayUnorderedList(List<String> items)` — Bullet Lists

Render a bulleted list for unordered items.

```java
@Test
void listJavaFeatures() {
    sayUnorderedList(List.of(
        "Records for immutable data structures",
        "Sealed classes for type-safe hierarchies",
        "Virtual threads for lightweight concurrency",
        "Pattern matching for safe type extraction",
        "Text blocks for readable multi-line strings"
    ));
}
```

**Output:**
```markdown
- Records for immutable data structures
- Sealed classes for type-safe hierarchies
- Virtual threads for lightweight concurrency
- Pattern matching for safe type extraction
- Text blocks for readable multi-line strings
```

---

#### 10. `sayOrderedList(List<String> items)` — Numbered Lists

Render a numbered list for ordered sequences.

```java
@Test
void documentWorkflow() {
    sayOrderedList(List.of(
        "Parse configuration file as YAML",
        "Validate configuration against schema",
        "Build DataSource connection pool",
        "Execute schema migrations",
        "Run application startup hooks"
    ));
}
```

**Output:**
```markdown
1. Parse configuration file as YAML
2. Validate configuration against schema
3. Build DataSource connection pool
4. Execute schema migrations
5. Run application startup hooks
```

---

#### 11. `sayJson(Object object)` — JSON Serialization

Serialize any object to pretty-printed JSON and render in a code block.

```java
@Test
void documentDataStructures() {
    record User(String name, String email, int age) {}

    sayJson(Map.of(
        "id", 1,
        "user", new User("Alice", "alice@example.com", 30),
        "active", true
    ));
}
```

**Output:**
````markdown
```json
{
  "id" : 1,
  "user" : {
    "name" : "Alice",
    "email" : "alice@example.com",
    "age" : 30
  },
  "active" : true
}
```
````

---

#### 12. `sayAssertions(Map<String, String> assertions)` — Test Results Table

Document assertion results in a Check/Result table format.

```java
@Test
void summarizeTestResults() {
    sayAssertions(Map.of(
        "List is not empty", "✓ PASS",
        "First element is 'Alice'", "✓ PASS",
        "Count equals 3", "✓ PASS",
        "Email domain is valid", "✗ FAIL — missing @domain.com"
    ));
}
```

**Output:**
```markdown
| Check | Result |
| --- | --- |
| List is not empty | ✓ PASS |
| First element is 'Alice' | ✓ PASS |
| Count equals 3 | ✓ PASS |
| Email domain is valid | ✗ FAIL — missing @domain.com |
```

---

## Java 21-25 Features with DocTester

### 1. Records — Immutable Value Objects

**Problem:** Traditional classes require constructors, getters, equals, hashCode, toString.

**Solution:** Records are one-liners.

```java
record User(String name, String email) {}
record Article(long id, String title, String content, User author) {}
```

**In a DocTest:**

```java
@Test
void demonstrateRecords() {
    sayNextSection("Java Records for Immutable Data");
    say("Records eliminate boilerplate for value objects:");

    var user = new User("Alice", "alice@example.com");
    var article = new Article(1L, "Java 25 Features", "...", user);

    sayJson(Map.of(
        "article", article,
        "author", user
    ));

    sayAssertions(Map.of(
        "Record created successfully", "✓ PASS",
        "Immutable fields prevent mutation", "✓ PASS"
    ));
}
```

**Benefits:**
- Final fields prevent mutation
- Compact code (100 lines → 1 line)
- Automatic equals, hashCode, toString
- Canonical constructor with validation
- Compact constructors for derived fields

---

### 2. Sealed Classes & Exhaustive Pattern Matching

**Problem:** Forget to handle a case in a switch, or add a new type and miss updates.

**Solution:** Sealed hierarchies force compiler exhaustiveness checking.

```java
sealed interface ProcessResult permits ProcessResult.Success, ProcessResult.Error {
    record Success(String message) implements ProcessResult {}
    record Error(int code, String reason) implements ProcessResult {}
}
```

**In a DocTest:**

```java
@Test
void demonstrateSealedClasses() {
    sayNextSection("Sealed Classes & Exhaustive Pattern Matching");

    var results = List.of(
        new ProcessResult.Success("Operation completed"),
        new ProcessResult.Error(500, "Internal error"),
        new ProcessResult.Success("Data processed")
    );

    sayUnorderedList(results.stream()
        .map(r -> switch (r) {
            case ProcessResult.Success(var msg) -> "✓ " + msg;
            case ProcessResult.Error(var code, var reason) -> "✗ [%d] %s".formatted(code, reason);
            // No 'default' needed — compiler verifies completeness
        })
        .toList()
    );

    sayNote("Sealed classes ensure all subtypes are known at compile time. " +
            "This enables exhaustive switch expressions without a default case.");
}
```

**Benefits:**
- Compiler catches missing cases
- New subtypes force updates to all switches
- Type-safe hierarchies
- Better performance (sealed type hierarchies allow optimizations)

---

### 3. Virtual Threads — Lightweight Concurrency

**Problem:** Testing concurrent scenarios typically exhausts OS thread pools.

**Solution:** Virtual threads are lightweight; create thousands without resource exhaustion.

```java
@Test
void demonstrateVirtualThreads() throws InterruptedException {
    sayNextSection("Virtual Threads for Lightweight Concurrency");

    int threadCount = 100;
    AtomicInteger completedCount = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(threadCount);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Simulate work
                    Thread.sleep(10);
                    completedCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
    }

    latch.await(10, TimeUnit.SECONDS);

    sayJson(Map.of(
        "virtual_threads_created", threadCount,
        "threads_completed", completedCount.get(),
        "success_rate", "%.1f%%".formatted(100.0 * completedCount.get() / threadCount)
    ));

    sayNote("Virtual threads are ideal for I/O-bound workloads. " +
            "Each virtual thread has minimal overhead (no OS context switch cost).");
}
```

**Benefits:**
- No OS thread limits
- Lightweight stack management
- Natural async code (no callbacks)
- Trivial to spawn thousands
- Works with existing synchronous APIs

---

### 4. Text Blocks — Readable Multi-Line Strings

**Problem:** String concatenation obscures SQL, JSON, and configuration.

**Solution:** Text blocks preserve formatting and eliminate escape sequences.

```java
@Test
void demonstrateTextBlocks() {
    sayNextSection("Text Blocks for Readable Multi-Line Strings");

    say("Text blocks (triple quotes) preserve formatting without escape sequences:");

    String sql = """
        SELECT u.id, u.name, COUNT(a.id) as article_count
        FROM users u
        LEFT JOIN articles a ON u.id = a.user_id
        WHERE u.active = true
        GROUP BY u.id
        ORDER BY article_count DESC;
        """;

    sayCode(sql, "sql");

    String json = """
        {
          "feature": "Text Blocks",
          "since": "Java 15",
          "status": "Stable"
        }
        """;

    sayCode(json, "json");
}
```

**Benefits:**
- No `\"` escape sequences
- Formatting is preserved
- Multi-line strings are readable as-is
- Excellent for SQL, JSON, configuration, HTML

---

### 5. Pattern Matching — Safe Type Extraction

**Problem:** Extract data from objects with explicit type checks and casts.

**Solution:** Pattern matching combines checking and extraction in one expression.

```java
@Test
void demonstratePatternMatching() {
    sayNextSection("Pattern Matching for Safe Type Extraction");

    sealed interface Value permits Value.IntValue, Value.StringValue, Value.BoolValue {
        record IntValue(int val) implements Value {}
        record StringValue(String val) implements Value {}
        record BoolValue(boolean val) implements Value {}
    }

    var values = List.of(
        new Value.IntValue(42),
        new Value.StringValue("hello"),
        new Value.BoolValue(true),
        new Value.IntValue(100)
    );

    sayUnorderedList(values.stream()
        .map(v -> switch (v) {
            case Value.IntValue(var i) when i > 50 -> "Large integer: " + i;
            case Value.IntValue(var i) -> "Small integer: " + i;
            case Value.StringValue(var s) -> "String: \"" + s + "\"";
            case Value.BoolValue(var b) -> "Boolean: " + b;
        })
        .toList()
    );

    sayNote("Pattern matching with guards (when clauses) enables conditional extraction. " +
            "This reduces defensive code and improves readability.");
}
```

**Benefits:**
- No explicit casts
- Guards (when clauses) for conditional logic
- Compiler ensures exhaustiveness
- Pattern destructuring for nested types

---

### 6. SequencedCollections — Intent-Clear Collection Access

**Problem:** Getting first/last elements requires index math or iteration.

**Solution:** `SequencedCollection` adds `getFirst()` / `getLast()` / `reversed()`.

```java
@Test
void demonstrateSequencedCollections() {
    sayNextSection("SequencedCollections for Ordered Access");

    var items = new LinkedList<>(List.of("first", "second", "third"));

    sayKeyValue(Map.of(
        "First item", items.getFirst(),
        "Last item", items.getLast(),
        "Total count", String.valueOf(items.size())
    ));

    say("Reversed order:");
    sayUnorderedList(
        items.reversed().stream().toList()
    );

    sayNote("SequencedCollection makes intent explicit: getFirst() and getLast() " +
            "are clearer than items.get(0) and items.get(items.size()-1).");
}
```

**Benefits:**
- No index bounds errors
- Intent is explicit
- Works with List, Deque, Set, etc.
- `reversed()` returns a reversed view

---

### 7. Switch Expressions — Functional, Exhaustive, No Default

**Problem:** Switch statements are verbose, easy to forget `break`, don't return values cleanly.

**Solution:** Switch expressions are functional, exhaustive, return values.

```java
@Test
void demonstrateSwitchExpressions() {
    sayNextSection("Switch Expressions for Functional Logic");

    var statuses = List.of(200, 201, 400, 404, 500);

    sayTable(new String[][] {
        {"Status Code", "Description"},
        {
            "200",
            switch (200) {
                case 200, 201 -> "Success";
                case 400, 403 -> "Client Error";
                case 500, 502, 503 -> "Server Error";
                default -> "Unknown";
            }
        },
        {
            "404",
            switch (404) {
                case 200, 201 -> "Success";
                case 400, 403 -> "Client Error";
                case 500, 502, 503 -> "Server Error";
                default -> "Unknown";
            }
        }
    });

    sayNote("Switch expressions are exhaustive. The compiler ensures all cases are covered. " +
            "With sealed types, you can omit the default case.");
}
```

**Benefits:**
- No `break` bugs
- Returns values directly
- Compiler ensures exhaustiveness
- Arrow syntax is cleaner

---

### 8. Stream API & Method References

**Problem:** Manual iteration and explicit lambdas are verbose.

**Solution:** Stream API with method references is concise and functional.

```java
@Test
void demonstrateStreams() {
    sayNextSection("Functional Programming with Streams and Method References");

    record Person(String name, int age) {}

    var people = List.of(
        new Person("Alice", 25),
        new Person("Bob", 30),
        new Person("Charlie", 22),
        new Person("Diana", 28)
    );

    say("All people:");
    sayUnorderedList(people.stream()
        .map(Person::name)
        .sorted()
        .toList()
    );

    say("People aged 25 or older:");
    sayUnorderedList(people.stream()
        .filter(p -> p.age >= 25)
        .map(p -> "%s (%d)".formatted(p.name, p.age))
        .toList()
    );

    say("Statistics:");
    sayKeyValue(Map.of(
        "Total count", String.valueOf(people.size()),
        "Average age", "%.1f".formatted(people.stream()
            .mapToInt(Person::age)
            .average()
            .orElse(0.0)),
        "Oldest person", people.stream()
            .max((a, b) -> Integer.compare(a.age, b.age))
            .map(Person::name)
            .orElse("N/A")
    ));
}
```

**Benefits:**
- Declarative (what, not how)
- Method references are concise
- Lazy evaluation
- Composable transformations

---

### 9. Optional — Explicit Null Handling

**Problem:** Nullable fields cause unexpected NullPointerExceptions.

**Solution:** `Optional` forces explicit null checks.

```java
@Test
void demonstrateOptional() {
    sayNextSection("Explicit Null Handling with Optional");

    var users = List.of(
        Map.of("name", "Alice", "email", "alice@example.com"),
        Map.of("name", "Bob"),  // missing email
        Map.of("name", "Charlie", "email", "charlie@example.com")
    );

    say("User emails:");
    sayUnorderedList(users.stream()
        .map(u -> Optional.ofNullable((String) u.get("email"))
            .map(e -> u.get("name") + ": " + e)
            .orElse(u.get("name") + ": (no email)"))
        .toList()
    );

    say("Users with emails:");
    sayUnorderedList(users.stream()
        .filter(u -> Optional.ofNullable(u.get("email")).isPresent())
        .map(u -> (String) u.get("name"))
        .toList()
    );

    sayNote("Optional makes null-handling explicit. " +
            "ifPresent(), orElse(), orElseThrow() prevent surprise NullPointerExceptions.");
}
```

**Benefits:**
- Null checks are explicit and required
- No surprise null pointers
- Code is self-documenting
- Natural with streams

---

### 10. Var Type Inference — Less Boilerplate

**Problem:** Local variable declarations repeat the type name (redundant).

**Solution:** `var` infers the type from the right-hand side.

```java
@Test
void demonstrateVar() {
    sayNextSection("Type Inference with var");

    var names = List.of("Alice", "Bob", "Charlie");
    var count = names.size();
    var doubled = names.stream()
        .map(n -> n + " & " + n)
        .toList();

    sayTable(new String[][] {
        {"Variable", "Type", "Value"},
        {"names", "List<String>", names.toString()},
        {"count", "int", String.valueOf(count)},
        {"doubled", "List<String>", doubled.toString()}
    });

    sayNote("var reduces boilerplate when the RHS type is obvious. " +
            "Use it for local variables; explicit types are clearer for fields and parameters.");
}
```

**Benefits:**
- Shorter, cleaner code
- Type is obvious from context
- IDE refactoring tools handle type changes
- Reduces visual noise

---

## Complete Example

Here's a full DocTest showcasing Java 25 features:

```java
import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

@DisplayName("Java 25 Features Live Documentation")
public class Java25FeaturesDocTest extends DocTester {

    record Article(long id, String title, String author) {}

    sealed interface QueryResult permits QueryResult.Found, QueryResult.NotFound {
        record Found(Article article) implements QueryResult {}
        record NotFound(String reason) implements QueryResult {}
    }

    @Test
    @DisplayName("Records, sealed classes, and pattern matching")
    void demonstrateModernJava() {
        sayNextSection("Java 25: Records, Sealed Classes, Pattern Matching");

        say("This test demonstrates core Java 25 features for data-driven documentation:");

        var articles = List.of(
            new Article(1L, "Getting Started with Records", "Alice"),
            new Article(2L, "Sealed Classes in Practice", "Bob"),
            new Article(3L, "Pattern Matching Deep Dive", "Charlie")
        );

        say("**Articles in collection:**");
        sayUnorderedList(articles.stream()
            .map(a -> "#%d: %s (by %s)".formatted(a.id, a.title, a.author))
            .toList()
        );

        say("**Searching with sealed results:**");
        var results = articles.stream()
            .map(a -> a.id == 2L
                ? (QueryResult) new QueryResult.Found(a)
                : new QueryResult.NotFound("Not found")
            )
            .map(r -> switch (r) {
                case QueryResult.Found(var a) -> "✓ " + a.title();
                case QueryResult.NotFound(var reason) -> "✗ " + reason;
            })
            .toList();

        sayUnorderedList(results);

        sayWarning("Records are immutable. All fields are final and cannot be reassigned.");
        sayNote("Pattern matching with sealed types enables compiler-checked exhaustiveness.");
    }
}
```

---

## Generated Documentation Output

DocTester generates clean **Markdown** files in `target/site/doctester/`:

```
target/site/doctester/
├── index.html
├── Java25FeaturesDocTest.md
└── assets/
```

**Markdown is:**
- **Portable:** Works on GitHub, GitLab, Gitea, standard Markdown renderers
- **Version-control friendly:** Clean text diffs in git
- **Tool-agnostic:** No dependency on custom CSS or JavaScript
- **Readable:** Works in raw text editors

---

## Quick Start

### 1. Add Dependency

```xml
<dependency>
    <groupId>org.r10r</groupId>
    <artifactId>doctester-core</artifactId>
    <version>1.1.12-SNAPSHOT</version>
    <scope>test</scope>
</dependency>
```

### 2. Extend DocTester

```java
import org.r10r.doctester.DocTester;
import org.junit.jupiter.api.Test;

public class MyDocTest extends DocTester {

    @Test
    void documentMyFeature() {
        sayNextSection("My Feature");
        say("This is my feature documentation.");

        var data = List.of("item1", "item2", "item3");
        sayUnorderedList(data);
    }
}
```

### 3. Run Tests

```bash
mvnd clean verify
```

### 4. View Generated Docs

```bash
open target/site/doctester/index.html
```

---

## Build Commands

```bash
# Fast build with Maven Daemon (preferred)
mvnd clean install -DskipTests

# Build and test (all modules)
mvnd clean verify

# Run specific DocTest
mvnd test -pl doctester-core -Dtest=Java25FeaturesDocTest

# Stop the mvnd daemon
mvnd --stop
```

---

## Java Version Requirements

- **Java 21 LTS minimum** (OpenJDK or Oracle JDK)
- **Java 25 recommended** for all latest features
- **Maven 4.0.0-rc-5+** or `mvnd` daemon
- **`--enable-preview`** flag (for Java 25 preview features)

### Verification

```bash
java -version         # openjdk version "25.0.2" or higher
mvnd --version        # Maven 4.0.0+
echo $JAVA_HOME       # /usr/lib/jvm/java-25-openjdk-amd64 (or equivalent)
```

---

## say* API Reference

| Method | Purpose | Output |
|--------|---------|--------|
| `say(String)` | Paragraph text | Markdown paragraph |
| `sayNextSection(String)` | Section heading | Markdown H1 + TOC entry |
| `sayRaw(String)` | Raw content | Unescaped Markdown/HTML |
| `sayTable(String[][])` | Data table | Markdown table |
| `sayCode(String, String)` | Syntax-highlighted code | Fenced code block |
| `sayWarning(String)` | Warning alert | GitHub `[!WARNING]` callout |
| `sayNote(String)` | Info alert | GitHub `[!NOTE]` callout |
| `sayKeyValue(Map)` | Key-value pairs | 2-column table |
| `sayUnorderedList(List)` | Bullet list | Markdown bullets |
| `sayOrderedList(List)` | Numbered list | Markdown numbering |
| `sayJson(Object)` | Serialize to JSON | Pretty-printed JSON code block |
| `sayAssertions(Map)` | Test results | Check/Result table |

---

## Module Structure

```
doctester/
├── doctester-core/                  # Core library (JAR artifact)
│   ├── pom.xml
│   └── src/main/java/org/r10r/doctester/
│       ├── DocTester.java           # Abstract base class
│       └── rendermachine/           # Markdown generation
│           ├── RenderMachine.java
│           └── RenderMachineImpl.java
└── doctester-integration-test/      # Full example tests
    └── src/test/java/
        └── Java25FeaturesDocTest.java
```

---

## Examples

Complete working examples in `doctester-integration-test/src/test/java/`:

- `Java25FeaturesDocTest.java` — Records, sealed classes, pattern matching, streams
- `VirtualThreadsDocTest.java` — Concurrent testing with virtual threads
- `TextBlocksDocTest.java` — Readable multi-line strings
- `StreamsAndFunctionalDocTest.java` — Functional programming patterns
- `OptionalDocTest.java` — Null handling with Optional

**Run all examples:**

```bash
mvnd verify -pl doctester-integration-test
```

---

## Architecture

**Three-layer design:**

1. **API Layer** (`DocTester`) — Fluent `say*` methods
2. **Rendering Engine** (`RenderMachine`) — Markdown generation
3. **Output** — Markdown files in `target/site/doctester/`

---

## Contributing

Contributions welcome! Please:

1. Write tests for your feature
2. Document with Javadoc on public methods
3. Use Sun Java code style (4 spaces, UTF-8)
4. Use Java 21+ idioms (records, sealed classes, pattern matching)
5. Update this README
6. Submit a pull request

---

## License

Apache License 2.0. See `LICENSE.md`.

---

## Support

- **Issues:** https://github.com/r10r-org/doctester/issues
- **Discussions:** https://github.com/r10r-org/doctester/discussions
