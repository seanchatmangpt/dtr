# Explanation: Why Records and Sealed Classes

Records and sealed classes address fundamental problems in Java's type system and data modeling. This document explains the design philosophy and how they enable safer, more concise code.

---

## The Problem: Boilerplate and Mutability

### Before Records

Modeling a simple data carrier required **tons of boilerplate**:

```java
public class User {
    private final String name;
    private final String email;
    private final int age;

    public User(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public int getAge() { return age; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return age == user.age &&
               Objects.equals(name, user.name) &&
               Objects.equals(email, user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, age);
    }

    @Override
    public String toString() {
        return "User{" +
               "name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", age=" + age +
               '}';
    }
}
```

**200+ lines for a simple data carrier.** The **intent** (hold 3 immutable fields) is buried in boilerplate.

### After Records

```java
record User(String name, String email, int age) {}
```

**One line.** All the boilerplate is generated. The **intent** is clear.

---

## The Solution: Records

Records encode an assumption: **This is a data carrier — immutable, with transparent components, focused on value semantics.**

When you write `record User(...)`, you're saying:
- ✅ These fields define my data
- ✅ Constructor takes all fields in order
- ✅ Accessors are field-named (no `get` prefix)
- ✅ Equality is based on all fields
- ✅ Data is immutable
- ✅ I'm not adding complex behavior

This removes the boilerplate that hides the actual intent.

---

## Problem: Type Safety and Exhaustiveness

### Before Sealed Classes

Without sealed hierarchies, you lose type safety in pattern matching:

```java
sealed interface ApiResult {
    record Success(String data) implements ApiResult {}
    record Failure(String error) implements ApiResult {}
}

// ❌ BAD: If you add a new case, this silently ignores it
String describe(ApiResult result) {
    if (result instanceof ApiResult.Success s) {
        return "Success: " + s.data();
    }
    return "Unknown";  // Oops: forgot Failure!
}

// ❌ UNSAFE: switch with default hides missing cases
String describe2(ApiResult result) {
    return switch (result) {
        case ApiResult.Success s -> "Success: " + s.data();
        default -> "Unknown";  // Failure is silently handled here
    };
}
```

If you add a third case (`Timeout`), the code still compiles but silently handles it wrong.

### With Sealed Classes

```java
sealed interface ApiResult {
    record Success(String data) implements ApiResult {}
    record Failure(String error) implements ApiResult {}
    record Timeout() implements ApiResult {}
}

// ✅ GOOD: Compiler forces you to handle all cases
String describe(ApiResult result) {
    return switch (result) {
        case ApiResult.Success s -> "Success: " + s.data();
        case ApiResult.Failure f -> "Failure: " + f.error();
        case ApiResult.Timeout t -> "Timeout";
        // No default. Add a new case? Compiler error until you handle it.
    };
}
```

If you add `Timeout`, every switch that uses `ApiResult` **must be updated**. The compiler enforces it.

---

## Design Principles

### 1. **Immutability as Default**

Records are immutable by design:

```java
record User(String name, int age) {}

User user = new User("alice", 30);
user.name = "bob";  // ❌ Compile error: fields are final
```

**Why?** Immutable data is:
- **Thread-safe** — no synchronization needed
- **Cacheable** — same value = same object (hashCode consistency)
- **Easier to reason about** — no hidden modifications

Mutations are explicitly `new User(...)`, not side effects.

### 2. **Exhaustiveness Checking**

Sealed classes let the compiler verify you've handled all cases:

```java
sealed interface Status permits Active, Inactive, Suspended {
    // ...
}

// If you add Suspended later, all existing switches error
String getLabel(Status status) {
    return switch (status) {
        case Active -> "Active";
        case Inactive -> "Inactive";
        // ❌ Error: case Suspended not handled!
    };
}
```

This catches refactoring bugs **at compile time**, not runtime.

### 3. **Transparency**

Records expose their structure — no hidden fields or methods:

```java
record Point(int x, int y) {}

// Direct field access (via accessor method)
Point p = new Point(3, 4);
int x = p.x();  // Not p.getX()
```

This is a **semantic signal**: "This object is entirely defined by these fields."

---

## Sealed Hierarchies: Bounded Polymorphism

Before sealed classes, inheritance was **open-ended:**

```java
class Animal { }
class Dog extends Animal { }
class Cat extends Animal { }
// Anyone can add: class Bird extends Animal { }
```

This flexibility has a cost: you can't assume you know all subtypes.

**Sealed classes bound the hierarchy:**

```java
sealed class Animal permits Dog, Cat {
    // Only Dog and Cat can extend this
}

final class Dog extends Animal { }
final class Cat extends Animal { }
```

Now the compiler knows: "Animal has exactly 2 subtypes." Pattern matching can be exhaustive.

---

## Comparison: OOP vs. Data-Oriented

### OOP Mindset (Classes)

Focus: **What can this object do?**

```java
public class User {
    private String name;

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }
    public boolean isValid() { return name != null && !name.isEmpty(); }
}
```

Encapsulation, mutability, behavior.

### Data-Oriented Mindset (Records)

Focus: **What data does this object carry?**

```java
record User(String name) {
    public User {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Invalid name");
        }
    }
}
```

Transparency, immutability, structure.

**Both are valid** — use the right tool for the job:
- **Records** for data carriers, DTOs, request/response bodies, configuration
- **Classes** for complex objects with mutable state and rich behavior

---

## Pattern Matching: Beyond Equality

Records + sealed classes enable **exhaustive pattern matching**, which is more powerful than traditional if/else:

```java
sealed interface HttpResponse {
    record Ok(String body) implements HttpResponse {}
    record Redirect(String location) implements HttpResponse {}
    record NotFound(String path) implements HttpResponse {}
    record ServerError(String message) implements HttpResponse {}
}

// Traditional if/else: Forget a case? Silently wrong.
HttpResponse response = ...;
if (response instanceof HttpResponse.Ok ok) {
    System.out.println(ok.body());
} else if (response instanceof HttpResponse.NotFound nf) {
    System.out.println("Not found: " + nf.path());
}
// Forgot Redirect and ServerError!

// Sealed pattern matching: Compiler enforces all cases
String result = switch (response) {
    case HttpResponse.Ok ok -> "Success: " + ok.body();
    case HttpResponse.Redirect r -> "Redirect to " + r.location();
    case HttpResponse.NotFound nf -> "Not found: " + nf.path();
    case HttpResponse.ServerError se -> "Error: " + se.message();
    // Compiler error if any case is missing
};
```

This is a form of **compile-time verification** that your code handles all scenarios.

---

## Real-World Benefits

### Before (Verbose, Error-Prone)

```java
public class OrderResponse {
    private int status;
    private String message;
    private Order order;
    private String errorCode;

    // Many constructors to support different cases
    public OrderResponse(int status, String message) { ... }
    public OrderResponse(Order order) { ... }
    public OrderResponse(int status, String errorCode) { ... }

    // Handling is unsafe
    public void handle(OrderResponse response) {
        if (response.getOrder() != null) {
            System.out.println("Order: " + response.getOrder());
        } else if (response.getErrorCode() != null) {
            System.out.println("Error: " + response.getErrorCode());
        } else {
            System.out.println("Unknown: " + response.getMessage());
        }
        // What if both order and errorCode are set? Undefined behavior.
    }
}
```

### After (Concise, Type-Safe)

```java
sealed interface OrderResponse {
    record Success(Order order) implements OrderResponse {}
    record Failure(String errorCode, String message) implements OrderResponse {}
}

public String handle(OrderResponse response) {
    return switch (response) {
        case OrderResponse.Success(Order order) ->
            "Order: " + order;
        case OrderResponse.Failure(String code, String msg) ->
            "Error " + code + ": " + msg;
        // Compiler guarantees all cases handled
    };
}
```

**Benefits:**
- ✅ One sealed interface, not multiple ad-hoc fields
- ✅ Compiler ensures exhaustiveness
- ✅ Clear intent: these are the only two outcomes
- ✅ No null-checking games
- ✅ Type-safe destructuring in pattern match

---

## Integration with Jackson and JSON

Records work seamlessly with JSON serialization:

```java
record User(String name, String email) {}

ObjectMapper mapper = new ObjectMapper();

// Serialize
User user = new User("alice", "alice@example.com");
String json = mapper.writeValueAsString(user);

// Deserialize
User restored = mapper.readValue(json, User.class);
```

No custom serialization code needed. This is **critical for APIs** where DTOs and domain models are defined with records.

---

## The Bigger Picture

Records and sealed classes are part of Java's shift toward:

1. **Immutability** — less mutable state = fewer bugs
2. **Type safety** — let the compiler catch errors
3. **Clarity** — code expresses intent, not boilerplate
4. **Composition** — build type systems from sealed hierarchies

Together with **pattern matching**, they enable a more **functional programming style** while keeping Java's familiar syntax.

---

## See Also

- [Tutorial: Records and Sealed Classes](../tutorials/records-sealed-classes.md)
- [How-to: Pattern Matching](../how-to/pattern-matching.md)
- [How-to: Switch Expressions](../how-to/switch-expressions.md)
- [Reference: Records and Sealed Classes](../reference/records-sealed-reference.md)
