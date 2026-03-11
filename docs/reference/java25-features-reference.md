# Reference: Java 25 Features

Complete reference for Java 25 language features, idioms, and APIs.

---

## Records

```java
// Immutable data carrier
record User(String name, String email, int age) {}

// With methods
record Point(int x, int y) {
    public double distance() {
        return Math.sqrt(x * x + y * y);
    }
}

// Usage
User user = new User("Alice", "alice@example.com", 30);
System.out.println(user.name()); // Accessor method
System.out.println(user); // Auto toString
```

**API:**
- Constructor auto-generated
- `equals()`, `hashCode()`, `toString()` auto-generated
- Getter methods: `fieldName()`
- Compact constructor: `record Point(int x, int y) { if (x < 0) throw new IllegalArgumentException(); }`
- `components()` method

---

## Sealed Classes

```java
// Restrict which classes can extend/implement
sealed interface Result permits Success, Error {}

record Success(Object value) implements Result {}
record Error(String message) implements Result {}

// Exhaustive pattern matching
String describe(Result r) {
    return switch (r) {
        case Success s -> "Success: " + s.value();
        case Error e -> "Error: " + e.message();
    };
}
```

---

## Pattern Matching

```java
// Instanceof patterns
if (obj instanceof String str && str.length() > 0) {
    System.out.println("Non-empty string: " + str);
}

// Deconstruction patterns
record Pair(int x, int y) {}

Pair p = new Pair(1, 2);
if (p instanceof Pair(var a, var b)) {
    System.out.println("x=" + a + ", y=" + b);
}

// Nested patterns
if (obj instanceof Pair(var x, Pair(var y, var z))) {
    System.out.println("Nested pair");
}
```

---

## Switch Expressions

```java
// Value-returning switch
String day = switch (dayOfWeek) {
    case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "Weekday";
    case SATURDAY, SUNDAY -> "Weekend";
};

// Pattern matching in switch
String describe(Object obj) {
    return switch (obj) {
        case null -> "null";
        case String s -> "String: " + s;
        case Integer i -> "Integer: " + i;
        case Double d -> "Double: " + d;
        default -> "Unknown";
    };
}

// Guard conditions
int category = switch (age) {
    case int a when a < 13 -> 0; // Child
    case int a when a < 18 -> 1; // Teen
    case int a when a < 65 -> 2; // Adult
    default -> 3; // Senior
};
```

---

## Text Blocks

```java
// Multiline strings without escapes
String json = """
    {
        "name": "Alice",
        "email": "alice@example.com"
    }
    """;

String html = """
    <div class="container">
        <h1>%s</h1>
        <p>%s</p>
    </div>
    """.formatted(title, body);

// With indentation control
String sql = """
    SELECT *
    FROM users
    WHERE age > ?
    """.stripLeading();
```

---

## var Type Inference

```java
// Type inferred from RHS
var name = "Alice"; // String
var count = 42; // int
var items = new ArrayList<String>(); // ArrayList<String>

// Works in loops
for (var item : items) {
    System.out.println(item);
}

for (var entry : map.entrySet()) {
    System.out.println(entry.getKey() + "=" + entry.getValue());
}
```

---

## Virtual Threads

```java
// Create many lightweight threads
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10000; i++) {
        executor.submit(() -> {
            // Each task runs on a virtual thread
            performWork();
        });
    }
} // Executor auto-closed

// Or manual creation
Thread vt = Thread.ofVirtual()
    .name("worker-1")
    .start(() -> System.out.println("Running on virtual thread"));
```

---

## String Methods

```java
// String.formatted() (like printf)
String msg = "Hello %s, you are %d years old".formatted(name, age);

// String.indent()
String indented = """
    Line 1
    Line 2
    """.indent(4);

// String.translateEscapes()
String escaped = "\\n\\t\\\\".translateEscapes();
```

---

## Sequenced Collections

```java
// SequencedCollection, SequencedSet, SequencedMap
SequencedCollection<String> items = new ArrayList<>();
items.add("first");
items.add("second");

String first = items.getFirst(); // New method
String last = items.getLast();
SequencedCollection<String> reversed = items.reversed();
```

---

## Unnamed Patterns and Variables

```java
// Discard unused values
void process(User user) {
    var (name, _, email) = user; // Ignore age
}

// In catch
try {
    risky();
} catch (IOException _) {
    // Ignore the exception
    return null;
}
```

---

## Preview Features

These require `--enable-preview`:

```java
// Flexible constructor bodies (Java 23+)
record Point(int x, int y) {
    public Point {
        if (x < 0 || y < 0) {
            throw new IllegalArgumentException("Coordinates must be non-negative");
        }
    }
}

// Primitive types in patterns (Java 23+)
String describe(int value) {
    return switch (value) {
        case 0 -> "Zero";
        case int n when n > 0 -> "Positive: " + n;
        case int n when n < 0 -> "Negative: " + n;
    };
}
```

---

## API Enhancements

### Files API

```java
// Write text to file (new charset parameter)
Files.writeString(path, content, StandardCharsets.UTF_8);

// Read lines with charset
List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
```

### Collections API

```java
// Collectors enhancements
Set<String> set = list.stream().collect(Collectors.toUnmodifiableSet());

// MapCollector
Map<String, Integer> map = list.stream()
    .collect(Collectors.teeing(
        Collectors.summingInt(String::length),
        Collectors.counting(),
        (sum, count) -> Map.of("total", sum, "count", count.intValue())));
```

### Streams API

```java
// Stream.takeWhile, dropWhile
List<Integer> items = list.stream()
    .takeWhile(x -> x < 10)
    .toList();

// Stream.iterate with predicate
Stream.iterate(0, x -> x < 100, x -> x + 2)
    .forEach(System.out::println);
```

---

## Best Practices

✅ **DO:**
- Use records for data carriers
- Use sealed classes for type hierarchies
- Use pattern matching for type checks
- Use text blocks for multiline strings
- Use virtual threads for I/O workloads
- Use `var` where RHS is obvious

❌ **DON'T:**
- Mix sealed and unsealed in hierarchy
- Forget to handle all patterns (compiler helps)
- Use `var` for unclear types (e.g., `var x = methodReturningUnknownType()`)
- Avoid records because they're "simple" (they're feature-rich)

---

## Java 25 Module System

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

## See Also

- [How-to: Use Virtual Threads](../how-to/use-virtual-threads.md)
- [How-to: Pattern Matching](../how-to/pattern-matching.md)
- [How-to: Text Blocks](../how-to/text-blocks.md)
- [How-to: Switch Expressions](../how-to/switch-expressions.md)
- [Tutorial: Virtual Threads](../tutorials/virtual-threads-lightweight-concurrency.md)
- [Tutorial: Records & Sealed Classes](../tutorials/records-sealed-classes.md)
- [Explanation: Java 25 Design](../explanation/java25-design-philosophy.md)
