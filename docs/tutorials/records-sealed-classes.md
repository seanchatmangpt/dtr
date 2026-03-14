# Tutorial: Records and Sealed Classes for Type-Safe Data Models

Learn how Java 25 records and sealed classes eliminate boilerplate and enable exhaustive pattern matching. This tutorial shows how to model data types safely and concisely, and how to use DTR 2.6.0 to auto-document your type hierarchy.

**Time:** ~30 minutes
**Prerequisites:** Java 25, DTR 2.6.0, familiarity with classes and interfaces
**What you'll learn:** How records replace getters/setters/equals, how sealed hierarchies enforce completeness, and how `sayRecordComponents` and `sayCodeModel` document your types automatically

---

## Records: Immutable Data Without Boilerplate

### The Old Way (Pre-Java 14)

```java
// Before records: lots of boilerplate
public class User {
    private final String name;
    private final String email;
    private final int age;

    public User(String name, String email, int age) {
        this.name = name;
        this.email = email;
        this.age = age;
    }

    public String name()  { return name; }
    public String email() { return email; }
    public int age()      { return age; }

    @Override public boolean equals(Object o) { /* ... */ }
    @Override public int hashCode()           { /* ... */ }
    @Override public String toString()        { /* ... */ }
}
```

Dozens of lines for a simple data carrier.

### The Java 25 Way: Records

```java
// After records: one line
record User(String name, String email, int age) {}
```

The compiler automatically generates: constructor, accessors, `equals`, `hashCode`, `toString`, and immutable fields.

---

## Step 1 — Document a Record Schema with sayRecordComponents

Create `src/test/java/com/example/RecordsDocTest.java`:

```java
package com.example;

import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class RecordsDocTest {

    // Domain records
    record User(String name, String email, int age) {}
    record Address(String street, String city, String postalCode) {}
    record Order(long id, User customer, List<String> items, double total) {}

    @Test
    void documentRecordSchemas(DtrContext ctx) {

        ctx.sayNextSection("Record Schemas");

        ctx.say("DTR can reflect on record classes and produce a schema table automatically. "
            + "Each component shows its name and declared type.");

        ctx.sayRecordComponents(User.class);

        ctx.say("The `Address` record models a postal address:");

        ctx.sayRecordComponents(Address.class);

        ctx.say("The `Order` record composes `User` and a list of item names:");

        ctx.sayRecordComponents(Order.class);
    }
}
```

Run the test:

```bash
mvnd test -Dtest=RecordsDocTest
cat target/docs/test-results/RecordsDocTest.md
```

`sayRecordComponents` emits a two-column table with **Component** and **Type** for every component in the record.

---

## Step 2 — Document Record Construction and Assertions

Add a second test method to the same class:

```java
    @Test
    void documentRecordUsage(DtrContext ctx) {

        ctx.sayNextSection("Creating and Asserting Records");

        ctx.say("Construct records with the canonical constructor. "
            + "All fields are `final` — the record is immutable by default.");

        ctx.sayCode("""
            var alice = new User("alice", "alice@example.com", 30);
            var bob   = new User("bob",   "bob@example.com",   25);
            """, "java");

        var alice = new User("alice", "alice@example.com", 30);
        var bob   = new User("bob",   "bob@example.com",   25);

        assertThat(alice.name()).isEqualTo("alice");
        assertThat(alice.age()).isPositive();
        assertThat(bob.email()).contains("@");

        ctx.sayAssertions(Map.of(
            "alice.name()", alice.name(),
            "alice.age() > 0", String.valueOf(alice.age() > 0),
            "bob.email() contains '@'", String.valueOf(bob.email().contains("@"))
        ));

        ctx.say("Records implement `equals` structurally — two records with the same "
            + "field values are equal, regardless of identity:");

        var alice2 = new User("alice", "alice@example.com", 30);
        assertThat(alice).isEqualTo(alice2);

        ctx.sayNote("Records cannot be mutated after construction. "
            + "To 'update' a field, create a new record instance.");
    }
```

---

## Step 3 — Sealed Hierarchies for Type Safety

Define a sealed hierarchy when multiple record types must be handled exhaustively:

```java
    sealed interface Result<T> permits Result.Success, Result.Failure {
        record Success<T>(T value)       implements Result<T> {}
        record Failure<T>(String reason) implements Result<T> {}
    }

    @Test
    void documentSealedHierarchy(DtrContext ctx) {

        ctx.sayNextSection("Sealed Hierarchies and Pattern Matching");

        ctx.say("A sealed interface restricts which classes may implement it. "
            + "The compiler enforces exhaustiveness in `switch` expressions — "
            + "no `default` clause is needed or allowed.");

        ctx.sayCode("""
            sealed interface Result<T> permits Result.Success, Result.Failure {
                record Success<T>(T value)       implements Result<T> {}
                record Failure<T>(String reason) implements Result<T> {}
            }
            """, "java");

        Result<User> ok   = new Result.Success<>(new User("carol", "carol@example.com", 28));
        Result<User> fail = new Result.Failure<>("User not found");

        String okMsg = switch (ok) {
            case Result.Success<User>(var u) -> "Found user: " + u.name();
            case Result.Failure<User>(var r) -> "Error: " + r;
        };

        String failMsg = switch (fail) {
            case Result.Success<User>(var u) -> "Found user: " + u.name();
            case Result.Failure<User>(var r) -> "Error: " + r;
        };

        ctx.say("Pattern matching results:");

        ctx.sayAssertions(Map.of(
            "ok result message",   okMsg,
            "fail result message", failMsg
        ));

        ctx.say("If a new subtype is added to the sealed interface, "
            + "every switch expression that handles it becomes a compile error "
            + "until the new case is added. This prevents silent omissions.");
    }
```

---

## Step 4 — Document Code Models with sayCodeModel

DTR 2.6.0 can emit a structured view of a class's code model using Java 25 Code Reflection:

```java
    @Test
    void documentCodeModel(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Code Model Documentation");

        ctx.say("DTR can inspect the bytecode structure of a class and emit "
            + "a human-readable summary of its operations. "
            + "This is useful for auditing what a class actually does at runtime.");

        ctx.sayCode("""
            // sayCodeModel documents the internal structure of a class
            ctx.sayCodeModel(User.class);
            """, "java");

        ctx.sayCodeModel(User.class);

        ctx.say("For a specific method, pass a `java.lang.reflect.Method` reference:");

        var method = User.class.getDeclaredMethod("name");
        ctx.sayCodeModel(method);
    }
```

---

## Step 5 — Records with Custom Compact Constructors

Records support compact constructor syntax for validation:

```java
    record PositiveAmount(double value) {
        PositiveAmount {
            if (value <= 0) throw new IllegalArgumentException(
                "Amount must be positive, got: " + value);
        }
    }

    @Test
    void documentRecordValidation(DtrContext ctx) {

        ctx.sayNextSection("Records with Constructor Validation");

        ctx.say("The compact constructor runs before the canonical constructor assignments. "
            + "Use it to validate or normalize inputs:");

        ctx.sayCode("""
            record PositiveAmount(double value) {
                PositiveAmount {
                    if (value <= 0) throw new IllegalArgumentException(
                        "Amount must be positive, got: " + value);
                }
            }
            """, "java");

        ctx.sayRecordComponents(PositiveAmount.class);

        // Valid case
        var amount = new PositiveAmount(99.99);
        assertThat(amount.value()).isEqualTo(99.99);

        // Invalid case — document the exception
        try {
            new PositiveAmount(-5.0);
        } catch (IllegalArgumentException e) {
            ctx.sayException(e);
        }

        ctx.sayAssertions(Map.of(
            "PositiveAmount(99.99).value()", String.valueOf(amount.value()),
            "PositiveAmount(-5.0) throws", "IllegalArgumentException"
        ));
    }
```

`sayException(Throwable)` is a new DTR 2.6.0 method that renders a structured exception block including the type, message, and abbreviated stack trace.

---

## Comparison Table

| Feature | Class | Record |
|---------|-------|--------|
| Boilerplate | Constructor, getters, equals, hashCode, toString | None |
| Mutability | Mutable by default | Immutable (all fields are `final`) |
| Inheritance | Supports `extends` | Implements interfaces only |
| Use case | Complex objects with behavior | Data carriers, DTOs |
| Pattern matching | `instanceof` check | Deconstruction patterns |

---

## What You Learned

- How to declare records and use the compact constructor for validation
- How `sayRecordComponents(Class)` auto-generates a schema table
- How `sayCodeModel(Class)` / `sayCodeModel(Method)` documents code structure
- How sealed interfaces enforce exhaustive `switch` expressions
- How `sayException(Throwable)` documents expected error conditions

---

## Next Steps

- [Tutorial: Benchmarking with Virtual Threads](virtual-threads-lightweight-concurrency.md) — measure record construction performance with `sayBenchmark`
- [Tutorial: Testing a REST API](testing-a-rest-api.md) — use records as JSON DTOs with `java.net.http.HttpClient`
- [Tutorial: Visualizing Code with sayMermaid](websockets-realtime.md) — diagram your type hierarchy
