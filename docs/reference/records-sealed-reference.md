# Reference: Records and Sealed Classes

Complete API reference for Java 26 records and sealed class hierarchies.

**v2.6.0 note:** `RenderMachine` was changed from `sealed` to plain `abstract` in v2.5.0. The new `sayRecordComponents` method (v2.6.0) introspects records and renders their components.

---

## Records

### Syntax

```java
record Name(Type1 field1, Type2 field2) {}
```

The compiler generates: positional constructor, field accessor methods (no `get` prefix), `equals()`, `hashCode()`, `toString()`. All fields are `final`.

---

### Basic Record

```java
record User(String name, String email, int age) {}

User user = new User("alice", "alice@example.com", 30);
System.out.println(user.name());     // "alice"
System.out.println(user.email());    // "alice@example.com"
System.out.println(user.age());      // 30
System.out.println(user);            // User[name=alice, email=alice@example.com, age=30]
System.out.println(user.equals(new User("alice", "alice@example.com", 30))); // true
```

---

### Record with Validation

```java
record Email(String value) {
    // Compact constructor — parameters are the record components
    public Email {
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email: " + value);
        }
        value = value.toLowerCase();  // normalize
    }

    public String domain() {
        return value.substring(value.indexOf("@") + 1);
    }

    public static Email system() {
        return new Email("system@example.com");
    }
}
```

---

### Record Implementing Interface

```java
interface Describable {
    String describe();
}

record Config(String host, int port, boolean tls) implements Describable {
    @Override
    public String describe() {
        return (tls ? "https" : "http") + "://" + host + ":" + port;
    }
}
```

Records can implement interfaces but cannot extend other classes.

---

### Generic Record

```java
record Pair<A, B>(A first, B second) {}

Pair<String, Integer> pair = new Pair<>("Alice", 30);
System.out.println(pair.first());   // "Alice"
System.out.println(pair.second());  // 30
```

---

### Jackson Serialization

```java
import com.fasterxml.jackson.databind.ObjectMapper;

record User(String name, String email) {}

ObjectMapper mapper = new ObjectMapper();
User user   = new User("alice", "alice@example.com");
String json = mapper.writeValueAsString(user);   // {"name":"alice","email":"alice@example.com"}
User restored = mapper.readValue(json, User.class);
```

---

### sayRecordComponents (v2.6.0)

`DtrContext.sayRecordComponents(Class<? extends Record>)` renders a table of all record components with their names, types, and generic signatures:

```java
record ApiResponse(int status, String body, java.util.Map<String, String> headers) {}

@Test
void documentApiResponse(DtrContext ctx) {
    ctx.sayNextSection("ApiResponse Record");
    ctx.sayRecordComponents(ApiResponse.class);
}
```

**Output:**

| Component | Type | Generic Signature |
|-----------|------|-------------------|
| `status` | `int` | `int` |
| `body` | `String` | `java.lang.String` |
| `headers` | `Map` | `java.util.Map<java.lang.String, java.lang.String>` |

---

## Sealed Classes and Interfaces

### Sealed Interface

```java
sealed interface Result permits Success, Failure {}

record Success(Object value) implements Result {}
record Failure(String error)  implements Result {}
```

Only the types listed in `permits` may implement the sealed interface.

---

### Exhaustive Pattern Matching

```java
String describe(Result r) {
    return switch (r) {
        case Success s -> "OK: " + s.value();
        case Failure f -> "ERR: " + f.error();
        // No default needed — compiler verifies exhaustiveness
    };
}
```

---

### Sealed Class Hierarchy

```java
sealed class Shape permits Circle, Rectangle {}

final class Circle    extends Shape { double radius() { return r; } private final double r; Circle(double r) { this.r = r; } }
final class Rectangle extends Shape { double width() { return w; } double height() { return h; }
    private final double w, h; Rectangle(double w, double h) { this.w = w; this.h = h; } }
```

Each permitted subtype must be `final`, `sealed`, or `non-sealed`.

---

### RenderMachine is NOT sealed (since v2.5.0)

In DTR 2.4.x `RenderMachine` was declared `sealed`. As of v2.5.0 it is a plain `abstract` class. Any class can extend it:

```java
// This compiles in v2.6.0 — no permits restriction
public class MyCustomRenderer extends RenderMachine {
    @Override
    public void say(String text) { /* custom rendering */ }
    // ... implement other abstract methods
}
```

The current hierarchy is:
```
RenderMachine (abstract)
├── RenderMachineImpl
├── RenderMachineLatex
├── BlogRenderMachine
├── SlideRenderMachine
└── MultiRenderMachine
```

See [RenderMachine API](rendermachine-api.md) for full hierarchy documentation.

---

## Pattern Matching

### Basic Deconstruction Pattern

```java
record Point(int x, int y) {}

if (new Point(3, 4) instanceof Point(int x, int y) && x > 0) {
    System.out.println("First quadrant: " + x + ", " + y);
}
```

---

### Nested Pattern

```java
record Person(String name, Address address) {}
record Address(String city, String country) {}

if (person instanceof Person(String name, Address(String city, _))) {
    System.out.println(name + " lives in " + city);
}
```

---

### Switch with Sealed Types

```java
sealed interface Number permits Whole, Decimal {}
record Whole(int value) implements Number {}
record Decimal(double value) implements Number {}

String category = switch (number) {
    case Whole(int v) when v > 0    -> "Positive integer";
    case Whole(int v) when v < 0    -> "Negative integer";
    case Whole(0)                   -> "Zero";
    case Decimal(double d)          -> "Decimal: " + d;
};
```

---

### Type Pattern

```java
Object obj = "hello";
if (obj instanceof String s) {
    System.out.println(s.toUpperCase());
}
```

---

### Unnamed Pattern Variable

```java
record User(String name, int age, String email) {}

// Only destructure what you need
if (user instanceof User(String name, _, _)) {
    System.out.println("Name: " + name);
}
```

---

## Comparison Tables

### Records vs. Regular Classes

| Feature | Record | Class |
|---------|--------|-------|
| Boilerplate | None (auto-generated) | Constructor, getters, equals, etc. |
| Constructor | Positional (all fields) | Custom |
| Mutability | Immutable (fields are `final`) | Mutable by default |
| Inheritance | Interfaces only | Any class |
| `equals()` | Field-based | Custom or identity |
| Use case | Data carriers, DTOs | Complex objects with behavior |

### Sealed vs. Open Hierarchies

| Feature | Sealed | Open |
|---------|--------|------|
| Permitted subtypes | Explicit `permits` list | Unlimited |
| Compiler exhaustiveness | Enforced in `switch` | Not enforced |
| Safety | Can't forget cases | More flexible |
| Switch patterns | Can omit `default` | Requires `default` |

---

## Best Practices

**DO:**
- Use records for data carriers — immutable, boilerplate-free
- Use sealed interfaces for bounded type hierarchies where all subtypes are known
- Use compact constructors for input validation in records
- Use pattern matching in `switch` over sealed types — let the compiler check exhaustiveness
- Use `sayRecordComponents` to auto-document record API surface

**DON'T:**
- Use records for entities with mutable state
- Create deep, complex sealed hierarchies — keep them focused
- Add a `default` arm to a `switch` over a sealed type (defeats exhaustiveness checking)
- Mix `sealed` and `non-sealed` subtypes unless you intentionally need an escape hatch

---

## See also

- [say* Core API Reference](request-api.md) — `sayRecordComponents`, `sayReflectiveDiff`
- [Java 26 Features Reference](java25-features-reference.md) — records and sealed in broader context
- [JVM Introspection API Reference](realtime-protocols-reference.md) — `sayReflectiveDiff` usage
- [RenderMachine API](rendermachine-api.md) — RenderMachine hierarchy (abstract, not sealed)
