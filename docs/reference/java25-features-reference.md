# Reference: Java 25 Features

Complete reference for Java 25 language features, idioms, and APIs. Includes v2.6.0 usage examples.

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

### Records with DTR 2.6.0

```java
@Test
void documentRecord(DtrContext ctx) {
    ctx.sayNextSection("ApiResponse Record");
    record ApiResponse(int status, String body) {}
    ctx.sayRecordComponents(ApiResponse.class);
    ctx.sayReflectiveDiff(new ApiResponse(200, "OK"), new ApiResponse(404, "Not Found"));
}
```

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

**Note (v2.5.0+):** `RenderMachine` was changed from `sealed` to plain `abstract`. You can now extend it freely without `permits` constraints. See [RenderMachine API](rendermachine-api.md).

---

## Pattern Matching

```java
// Instanceof pattern (JDK 16+)
if (obj instanceof String str && str.length() > 0) {
    System.out.println("Non-empty: " + str);
}

// Record deconstruction (JDK 21+)
record Pair(int x, int y) {}
if (new Pair(3, 4) instanceof Pair(int x, int y)) {
    System.out.println("x=" + x + ", y=" + y);
}

// Unnamed pattern variable (JDK 21+)
if (user instanceof User(String name, _, _)) {
    System.out.println("Name: " + name);
}
```

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

### Text blocks with DTR 2.6.0

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

### Virtual threads with DTR 2.6.0 — MultiRenderMachine

`MultiRenderMachine` dispatches every `say*` call to all registered render machines concurrently using virtual threads:

```java
ctx.setRenderMachine(new MultiRenderMachine(
    new RenderMachineImpl(),
    new RenderMachineLatex(new IEEETemplate(), new PdflatexStrategy()),
    new BlogRenderMachine(new DevToTemplate())
));
// subsequent say* calls fan out to all three machines in parallel
```

### Virtual threads with DTR 2.6.0 — benchmarking

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

---

## Code Reflection — JEP 516 (Java 25 preview)

JEP 516 provides structured access to the code model of a method: operations, control flow, and data flow, without bytecode parsing.

```java
// Requires --enable-preview
import java.lang.reflect.Method;

Method m = MyService.class.getMethod("process", Input.class);

// DTR 2.6.0 wraps JEP 516 output
ctx.sayCodeModel(m);
ctx.sayControlFlowGraph(m);
ctx.sayCallGraph(MyService.class);
ctx.sayOpProfile(m);
```

See [Code Reflection API Reference](grpc-reference.md) for complete method documentation.

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

// JEP 516: Code Reflection
import java.lang.reflect.Method;
// sayCodeModel, sayControlFlowGraph, etc. — DTR 2.6.0 wraps this API
```

---

## Best Practices

**DO:**
- Use records for data carriers — immutable, boilerplate-free
- Use sealed interfaces for bounded type hierarchies
- Use pattern matching in `switch` for sealed types — compiler enforces exhaustiveness
- Use text blocks for multi-line strings passed to `ctx.sayCode(...)` or `ctx.sayMermaid(...)`
- Use virtual threads for I/O-bound work; benchmark with `ctx.sayBenchmark(...)`
- Use `var` where the right-hand side type is obvious from context

**DON'T:**
- Use records for entities with mutable state
- Forget compact constructor validation in records
- Use virtual threads for CPU-bound computation (use `ForkJoinPool` instead)
- Use `var` where the type is non-obvious (`var x = factory.create()`)

---

## See also

- [Virtual Threads Reference](virtual-threads-reference.md) — `ExecutorService`, `Future`, `Thread.ofVirtual()`
- [Records and Sealed Reference](records-sealed-reference.md) — detailed pattern matching syntax
- [Code Reflection API Reference](grpc-reference.md) — JEP 516 integration
- [Benchmarking API Reference](url-builder.md) — `sayBenchmark` usage
