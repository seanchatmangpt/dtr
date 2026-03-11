# Reference: Records and Sealed Classes

Complete API reference for Java 25 records and sealed class hierarchies. Records eliminate boilerplate for immutable data carriers; sealed classes enforce exhaustiveness in type hierarchies.

---

## Records

### Syntax

```java
record Name(Type1 field1, Type2 field2, ...) {}
```

The record compiler generates:
- Constructor accepting all fields in order
- Accessor methods named after fields (no `get` prefix)
- `equals()` and `hashCode()` based on all fields
- `toString()` showing all fields
- All fields are `final` (immutable)

---

### Basic Record

```java
record User(String name, String email, int age) {}

// Usage
User user = new User("alice", "alice@example.com", 30);
System.out.println(user.name());     // "alice"
System.out.println(user.equals(new User("alice", "alice@example.com", 30))); // true
System.out.println(user);            // User[name=alice, email=alice@example.com, age=30]
```

---

### Record with Methods

Add custom methods and validation:

```java
record Email(String value) {
    // Compact constructor: implicit parameters are the record fields
    public Email {
        if (!value.contains("@")) {
            throw new IllegalArgumentException("Invalid email");
        }
    }

    // Custom method
    public String domain() {
        return value.substring(value.indexOf("@") + 1);
    }

    // Static method
    public static Email system() {
        return new Email("system@example.com");
    }
}
```

**Compact constructor syntax:** Parameters are implicit; you receive the fields directly and can validate them.

---

### Record Implementing Interface

```java
interface Serializable {
    byte[] serialize();
}

record User(String name, String email) implements Serializable {
    @Override
    public byte[] serialize() {
        return (name + "," + email).getBytes();
    }
}
```

Records can implement interfaces but cannot extend other classes.

---

### Record with Generic Type

```java
record Pair<T, U>(T first, U second) {}

// Usage
Pair<String, Integer> pair = new Pair<>("Alice", 30);
System.out.println(pair.first());  // "Alice"
System.out.println(pair.second()); // 30
```

---

### Jackson Serialization

Records work seamlessly with Jackson (JSON serialization):

```java
import com.fasterxml.jackson.databind.ObjectMapper;

record User(String name, String email) {}

ObjectMapper mapper = new ObjectMapper();

// Serialize to JSON
User user = new User("alice", "alice@example.com");
String json = mapper.writeValueAsString(user);
// Result: {"name":"alice","email":"alice@example.com"}

// Deserialize from JSON
User restored = mapper.readValue(json, User.class);
```

---

## Sealed Classes and Interfaces

### Sealed Interface Syntax

```java
sealed interface Result permits Success, Failure {
    // Only Success and Failure can implement this
}

record Success(String data) implements Result {}
record Failure(String error) implements Result {}
```

The `permits` clause lists all subtypes. Only those can implement/extend the sealed type.

---

### Sealed Class Hierarchy

```java
sealed class Animal permits Dog, Cat {
    public abstract void speak();
}

final class Dog extends Animal {
    @Override
    public void speak() { System.out.println("Woof"); }
}

final class Cat extends Animal {
    @Override
    public void speak() { System.out.println("Meow"); }
}
```

Each subtype must be either:
- `final` (cannot be extended further)
- `sealed` (further restricted)
- `non-sealed` (open for extension)

---

### Pattern Matching on Sealed Types

```java
sealed interface Response {
    record Ok(String body) implements Response {}
    record Error(int code) implements Response {}
}

String describe(Response response) {
    return switch (response) {
        case Response.Ok(String body) -> "Success: " + body;
        case Response.Error(int code) -> "Error: " + code;
        // No default needed; compiler knows all cases
    };
}
```

The compiler **forces exhaustiveness**: you must handle all permitted types.

---

### Sealed with Generics

```java
sealed interface Container<T> permits Box {
    T contents();
}

record Box<T>(T value) implements Container<T> {
    @Override
    public T contents() {
        return value;
    }
}

// Usage
Container<String> box = new Box<>("hello");
String contents = box.contents();
```

---

## Pattern Matching

### Basic Pattern

```java
record Point(int x, int y) {}

Point p = new Point(3, 4);

if (p instanceof Point(int x, int y) && x > 0 && y > 0) {
    System.out.println("First quadrant: " + x + ", " + y);
}
```

The pattern `Point(int x, int y)` destructures the record.

---

### Nested Pattern

```java
record Person(String name, Address address) {}
record Address(String city, String country) {}

Person person = new Person("alice", new Address("London", "UK"));

if (person instanceof Person(String name, Address(String city, String country))) {
    System.out.println(name + " lives in " + city + ", " + country);
}
```

Patterns can nest arbitrarily deep.

---

### Type Pattern

```java
Object obj = "hello";

if (obj instanceof String s) {
    System.out.println("String: " + s.toUpperCase());
}
```

Simple type pattern for non-record types.

---

### Guarded Pattern

```java
sealed interface Number {
    record Int(int value) implements Number {}
    record Double(double value) implements Number {}
}

Number num = new Number.Int(42);

String category = switch (num) {
    case Number.Int(int v) when v > 0 -> "Positive";
    case Number.Int(int v) when v < 0 -> "Negative";
    case Number.Int(0) -> "Zero";
    case Number.Double(double d) -> "Decimal";
};
```

The `when` guard narrows the case condition.

---

### Unnamed Pattern

```java
record User(String name, int age, String email) {}

// Only care about name and age
if (user instanceof User(String name, int age, _)) {
    System.out.println(name + " is " + age);
    // email is ignored
}
```

Use `_` to ignore fields.

---

## Comparison Table

### Records vs. Regular Classes

| Feature | Record | Class |
|---------|--------|-------|
| Boilerplate | None (auto-generated) | Lots (constructor, getters, equals, etc.) |
| Constructor | Positional (all fields) | Custom, any parameters |
| Mutability | Immutable (fields are final) | Mutable by default |
| Inheritance | Interfaces only | Any class |
| `equals()` | Field-based | Custom or identity |
| Use case | Data carriers, DTOs | Complex objects with behavior |

### Sealed vs. Open Hierarchies

| Feature | Sealed | Open |
|---------|--------|------|
| Permitted subtypes | Explicit list | Unlimited |
| Compiler exhaustiveness | Enforced | Not enforced |
| Maintenance | Safer (can't forget cases) | More flexible |
| Switch patterns | Can omit default | Need default |
| Runtime polymorphism | Bounded | Unlimited |

---

## Best Practices

✅ **DO:**
- Use records for data carriers — immutable, boilerplate-free
- Use sealed hierarchies when you have fixed subtypes
- Use pattern matching in `switch` for sealed types
- Leverage compact constructors for validation
- Combine sealed records for type-safe designs

❌ **DON'T:**
- Use records for entities with mutable state
- Forget to add validation in compact constructors
- Create deep, complex hierarchies (keep sealed interfaces focused)
- Mix sealed and non-sealed in the same hierarchy
- Use `default` in exhaustive switches on sealed types

---

## See Also

- [Tutorial: Records and Sealed Classes](../tutorials/records-sealed-classes.md)
- [How-to: Pattern Matching](../how-to/pattern-matching.md)
- [How-to: Switch Expressions](../how-to/switch-expressions.md)
- [Explanation: Records and Sealed Philosophy](../explanation/records-sealed-philosophy.md)
