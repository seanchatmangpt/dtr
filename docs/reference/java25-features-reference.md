# Reference: Java 26 Features

Complete reference for Java 26 language features, idioms, and APIs. Includes v2.6.0+ usage examples with JEP 516 Code Reflection integration.

> **Note:** This file was previously named `java25-features-reference.md` and has been updated to cover Java 26 features, including JEP 516 (Code Reflection), enhanced pattern matching, and structured concurrency previews.

---

## Records

```java
// Immutable data carrier
record User(String name, String email, int age) {}

// With validation in compact constructor
record Email(String value) {
    public Email {
        if (!value.contains("@")) throw new IllegalArgumentException("Invalid email: " + value);
    }
    public String domain() { return value.substring(value.indexOf("@") + 1); }
}

// Usage
User user = new User("Alice", "alice@example.com", 30);
System.out.println(user.name());   // accessor method — no get prefix
System.out.println(user);          // auto toString: User[name=Alice, ...]
```

**Generated automatically:** constructor, `equals()`, `hashCode()`, `toString()`, and accessor methods named after fields.

### Records with DTR 2.6.0+

```java
@Test
void documentRecord(DtrContext ctx) {
    ctx.sayNextSection("ApiResponse Record");
    record ApiResponse(int status, String body) {}
    ctx.sayRecordComponents(ApiResponse.class);
    ctx.sayReflectiveDiff(new ApiResponse(200, "OK"), new ApiResponse(404, "Not Found"));
}
```

**See also:** [Tutorial 3: Records in Practice](../tutorials/java26-features.md#records) for comprehensive examples.

---

## Sealed Classes

```java
// Restrict which classes can implement/extend
sealed interface Result permits Success, Failure {}

record Success(Object value) implements Result {}
record Failure(String error) implements Result {}

// Exhaustive switch — no default needed
String describe(Result r) {
    return switch (r) {
        case Success s -> "OK: " + s.value();
        case Failure f -> "ERR: " + f.error();
    };
}
```

**Note (v2.6.0+):** `RenderMachine` was changed from `sealed` to plain `abstract`. You can now extend it freely without `permits` constraints. See [RenderMachine API](rendermachine-api.md).

---

## Pattern Matching

### Instanceof Patterns (JDK 16+)

```java
if (obj instanceof String str && str.length() > 0) {
    System.out.println("Non-empty: " + str);
}
```

### Record Patterns (JDK 21+, enhanced in Java 26)

```java
// Basic record deconstruction
record Pair(int x, int y) {}
if (new Pair(3, 4) instanceof Pair(int x, int y)) {
    System.out.println("x=" + x + ", y=" + y);
}

// Nested record patterns (Java 26 enhancement)
record Point(int x, int y) {}
record Rectangle(Point topLeft, Point bottomRight) {}

Rectangle rect = new Rectangle(new Point(0, 0), new Point(10, 10));
if (rect instanceof Rectangle(Point(int x1, int y1), Point(int x2, int y2))) {
    System.out.println("Bounds: (" + x1 + "," + y1 + ") to (" + x2 + "," + y2 + ")");
}

// Unnamed pattern variable (JDK 21+)
if (user instanceof User(String name, _, _)) {
    System.out.println("Name: " + name);
}
```

### Guarded Patterns (JDK 21+)

```java
String formatted = switch (obj) {
    case Integer i when i > 0 -> "Positive: " + i;
    case Integer i           -> "Non-positive: " + i;
    case String s when !s.isEmpty() -> "Non-empty string";
    case String s                -> "Empty string";
    default                      -> "Unknown";
};
```

**See also:** [Records and Sealed Reference](records-sealed-reference.md) for advanced pattern matching techniques.

---

## Switch Expressions

```java
// Value-returning switch (JDK 14+)
String day = switch (dayOfWeek) {
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
    case SATURDAY, SUNDAY -> "Weekend";
};

// Pattern matching in switch (JDK 21+)
String describe(Object obj) {
    return switch (obj) {
        case null        -> "null";
        case String s    -> "String: " + s;
        case Integer i   -> "Integer: " + i;
        default          -> "Unknown";
    };
}

// Guard conditions (JDK 21+)
String category = switch (age) {
    case int a when a < 13 -> "Child";
    case int a when a < 18 -> "Teen";
    case int a when a < 65 -> "Adult";
    default -> "Senior";
};
```

---

## Text Blocks

```java
// Multiline strings (JDK 15+)
String json = """
    {
        "name": "Alice",
        "email": "alice@example.com"
    }
    """;

// With formatting
String html = """
    <div class="container">
        <h1>%s</h1>
        <p>%s</p>
    </div>
    """.formatted(title, body);
```

### Text blocks with DTR 2.6.0+

```java
ctx.sayMermaid("""
    flowchart LR
        Client --> DtrContext
        DtrContext --> RenderMachine
    """);

ctx.sayCode("""
    @ExtendWith(DtrExtension.class)
    class MyTest {
        @Test void test(DtrContext ctx) {
            ctx.say("Hello");
        }
    }
    """, "java");
```

**See also:** [Tutorial 5: Diagrams](../tutorials/diagrams.md) for Mermaid visualization patterns.

---

## var Type Inference

```java
var name  = "Alice";                          // String
var count = 42;                               // int
var items = new ArrayList<String>();          // ArrayList<String>

for (var entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}
```

---

## Virtual Threads

```java
// Executor pattern (recommended)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
        executor.submit(() -> performWork());
    }
} // auto-closed; waits for all tasks

// Single thread
Thread.ofVirtual().name("worker-1").start(() -> System.out.println("virtual"));
```

### Virtual threads with DTR 2.6.0+ — MultiRenderMachine

`MultiRenderMachine` dispatches every `say*` call to all registered render machines concurrently using virtual threads:

```java
ctx.setRenderMachine(new MultiRenderMachine(
    new RenderMachineImpl(),
    new RenderMachineLatex(new IEEETemplate(), new PdflatexStrategy()),
    new BlogRenderMachine(new DevToTemplate())
));
// subsequent say* calls fan out to all three machines in parallel
```

**See also:** [Virtual Threads Reference](virtual-threads-reference.md) for complete concurrency patterns.

### Virtual threads with DTR 2.6.0+ — benchmarking

```java
@Test
void benchmarkVirtualThread(DtrContext ctx) {
    ctx.sayNextSection("Virtual Thread Spawn Cost");
    ctx.sayEnvProfile();
    ctx.sayBenchmark(
        "Thread.ofVirtual().start().join()",
        () -> {
            try {
                Thread.ofVirtual().start(() -> {}).join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        },
        500,    // warmup
        10_000  // iterations
    );
}
```

**See also:** [Benchmarking API Reference](url-builder.md) for complete measurement patterns.

---

## Structured Concurrency — JEP 494 (Java 26 Preview)

**Note:** JEP 494 is in preview as of Java 26. While not directly used in DTR's core, it's relevant for concurrent documentation generation patterns.

### Basic Structured Concurrency

```java
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.Future;

// Requires --enable-preview in Java 26
String fetchData() throws InterruptedException {
    try (var scope = new StructuredTaskScope.ShutdownOnFailure()) {
        Future<String> user = scope.fork(() -> fetchUser());
        Future<String> orders = scope.fork(() -> fetchOrders());

        scope.join()           // Join both tasks
             .throwIfFailed(); // Propagate errors

        return new Response(user.resultNow(), orders.resultNow());
    }
}
```

### Virtual Threads + Structured Concurrency

**Relation to DTR:** `MultiRenderMachine` uses virtual threads for parallel rendering. Structured concurrency provides an alternative pattern for coordinated concurrent tasks:

```java
// MultiRenderMachine pattern (current implementation)
ctx.setRenderMachine(new MultiRenderMachine(
    new RenderMachineImpl(),
    new RenderMachineLatex(new IEEETemplate(), new PdflatexStrategy()),
    new BlogRenderMachine(new DevToTemplate())
));

// Structured concurrency pattern (alternative for coordinated tasks)
try (var scope = new StructuredTaskScope<String>()) {
    Future<String> markdown = scope.fork(() -> renderMarkdown(ctx));
    Future<String> latex = scope.fork(() -> renderLatex(ctx));
    Future<String> blog = scope.fork(() -> renderBlog(ctx));

    scope.join();
    // All renders complete together or fail together
}
```

### Version Requirements

**Java 26.ea+:** Structured concurrency API in preview with `--enable-preview`

**Earlier Java versions:** Use `CompletableFuture` or virtual thread executors manually

---

## Code Reflection — JEP 516 (Java 26)

JEP 516 provides structured access to the code model of a method: operations, control flow, and data flow, without bytecode parsing.

### Key Capabilities

**Control Flow Graphs (CFG):**
- Visual representation of method execution paths
- Conditional branches, loops, and return statements
- Generated as Mermaid flowcharts via `sayControlFlowGraph()`

**Call Graphs:**
- Method-to-method call relationships within a class
- Identifies caller/callee relationships
- Generated as Mermaid diagrams via `sayCallGraph()`

**Operation Profiles:**
- Count of bytecode operations (aload, invoke, etc.)
- Performance analysis and optimization insights
- Generated as tables via `sayOpProfile()`

### Usage with DTR 2.6.0+

```java
import java.lang.reflect.Method;

// Requires Java 26.ea+ with --enable-preview flag
@Test
void documentCodeReflection(DtrContext ctx) {
    Method m = MyService.class.getMethod("process", Input.class);

    ctx.sayNextSection("Code Reflection Analysis");

    // Class structure with Code Reflection
    ctx.sayCodeModel(m);

    // Mermaid flowchart of control flow
    ctx.sayControlFlowGraph(m);

    // Mermaid diagram of call graph
    ctx.sayCallGraph(MyService.class);

    // Operation count statistics
    ctx.sayOpProfile(m);
}
```

### Version Requirements

**Java 26.ea+:** Full JEP 516 support with `--enable-preview`

**Earlier Java versions:** DTR gracefully degrades:
- `sayCodeModel(Method)` falls back to standard reflection
- `sayControlFlowGraph()` outputs placeholder message
- `sayCallGraph()` uses standard reflection for method discovery
- `sayOpProfile()` outputs placeholder message

### Example Output

```java
@CodeReflection
void process(Input input) {
    if (input.isValid()) {
        handle(input);
    } else {
        logError();
    }
}
```

Generates:
- **CFG:** Mermaid flowchart showing conditional branch
- **Call Graph:** Edges from `process()` to `handle()` and `logError()`
- **Op Profile:** Table with operation counts (ifnonnull, invoke, return, etc.)

See [Code Reflection API Reference](../explanation/java25-design-philosophy.md#jep-516-integration) for complete method documentation and [Tutorial 5: Diagrams](../tutorials/diagrams.md) for visualization examples.

---

## String Methods

```java
// String.formatted() — like printf
String msg = "Hello %s, age %d".formatted(name, age);

// String.indent()
String indented = "line\n".indent(4);  // adds 4 spaces

// String.translateEscapes()
String raw = "line1\\nline2".translateEscapes(); // "line1\nline2"

// String.stripLeading() / stripTrailing()
String trimmed = "  hello  ".stripLeading();  // "hello  "
```

---

## Sequenced Collections (JDK 21+)

```java
SequencedCollection<String> items = new ArrayList<>();
items.addFirst("first");
items.addLast("last");

String first   = items.getFirst();
String last    = items.getLast();
var   reversed = items.reversed();
```

---

## Unnamed Patterns and Variables (JDK 21+)

```java
// Ignore unused catch exception variable
try {
    risky();
} catch (IOException _) {
    return null;
}

// Ignore unused lambda parameter
items.forEach(_ -> count++);

// Ignore record component in pattern
if (user instanceof User(String name, _)) {
    System.out.println(name);
}
```

---

## Module System

```java
module myapp {
    requires java.base;
    requires java.logging;
    requires transitive java.datatransfer;

    exports com.example.api;
    exports com.example.spi to com.example.plugin;

    opens com.example.internal to java.base;
}
```

---

## API Enhancements

### Files API

```java
Files.writeString(path, content, StandardCharsets.UTF_8);
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
```

### Streams API

```java
List<Integer> items = list.stream()
    .takeWhile(x -> x < 10)
    .toList();

Stream.iterate(0, x -> x < 100, x -> x + 2)
    .forEach(System.out::println);
```

### Collections factory methods

```java
var map  = Map.of("a", 1, "b", 2);
var list = List.of("x", "y", "z");
var set  = Set.of(1, 2, 3);
```

---

## Preview Features (require --enable-preview)

**Note:** These features require Java 26 with `--enable-preview` flag. DTR detects Java version at runtime and degrades gracefully on earlier versions.

```java
// Flexible constructor bodies (JDK 23+)
record Point(int x, int y) {
    public Point {
        if (x < 0 || y < 0) throw new IllegalArgumentException("Must be non-negative");
    }
}

// Primitive types in patterns (JDK 23+)
String describe(int value) {
    return switch (value) {
        case 0            -> "Zero";
        case int n when n > 0 -> "Positive: " + n;
        case int n when n < 0 -> "Negative: " + n;
    };
}

// JEP 516: Code Reflection (Java 26)
import java.lang.reflect.Method;
// Requires @CodeReflection annotation on methods
// DTR 2.6.0 wraps: sayCodeModel, sayControlFlowGraph, sayCallGraph, sayOpProfile
```

### Graceful Degradation

When running on Java < 26:
- `sayControlFlowGraph()` outputs: "Control flow graphs require Java 26+"
- `sayOpProfile()` outputs: "Operation profiles require Java 26+"
- `sayCodeModel(Method)` falls back to standard reflection (no bytecode analysis)
- Other `say*` methods work normally

---

## Best Practices

**DO:**
- Use records for data carriers — immutable, boilerplate-free
- Use sealed interfaces for bounded type hierarchies
- Use pattern matching in `switch` for sealed types — compiler enforces exhaustiveness
- Use text blocks for multi-line strings passed to `ctx.sayCode(...)` or `ctx.sayMermaid(...)`
- Use virtual threads for I/O-bound work; benchmark with `ctx.sayBenchmark(...)`
- Use `var` where the right-hand side type is obvious from context
- Use `@CodeReflection` annotation on methods you want to analyze with `sayControlFlowGraph()`
- Design tests for both Java 26+ (full features) and earlier versions (graceful degradation)

**DON'T:**
- Use records for entities with mutable state
- Forget compact constructor validation in records
- Use virtual threads for CPU-bound computation (use `ForkJoinPool` instead)
- Use `var` where the type is non-obvious (`var x = factory.create()`)
- Call JEP 516 methods without checking Java version first (DTR handles this, but be aware)
- Assume `--enable-preview` is available in production environments

### Java 26 Specific Considerations

**Version Detection:**
```java
// DTR automatically detects Java version
// No manual checks needed for say* methods
ctx.sayControlFlowGraph(method); // Works or degrades gracefully
```

**Testing Strategy:**
```java
@Test
void testJava26Features(DtrContext ctx) {
    // Test passes on all Java versions
    // Output varies based on available features
    ctx.sayNextSection("Java 26 Feature Test");
    ctx.sayControlFlowGraph(MyClass.class.getMethod("process"));
    // On Java 26: Full CFG diagram
    // On Java 21: "Control flow graphs require Java 26+"
}
```

**CI Configuration:**
- GitHub Actions: Use Java 26.ea with `--enable-preview` for full feature testing
- Maven: Configure in `.mvn/maven.config`: `--enable-preview`
- Local development: Any Java 21+ works; JEP 516 features degrade gracefully

---

## See also

- [Tutorial 3: Java 26 Features](../tutorials/java26-features.md) — hands-on Java 26 feature demonstrations
- [Tutorial 5: Diagrams](../tutorials/diagrams.md) — Mermaid flowcharts, call graphs, and visualizations
- [Virtual Threads Reference](virtual-threads-reference.md) — `ExecutorService`, `Future`, `Thread.ofVirtual()`
- [Records and Sealed Reference](records-sealed-reference.md) — detailed pattern matching syntax
- [Code Reflection Design](../explanation/java25-design-philosophy.md#jep-516-integration) — JEP 516 architecture and implementation
- [Benchmarking API Reference](url-builder.md) — `sayBenchmark` usage
