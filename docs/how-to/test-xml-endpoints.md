# How-To: Document Exception Handling with sayException

Use DTR 2.6.0's `sayException` to generate structured exception documentation in your tests.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayException Does

`sayException(Throwable)` renders a structured exception block in the documentation output, showing:

- Exception class name and full hierarchy
- Exception message
- Stack trace (formatted and truncated to relevant frames)
- Cause chain (if present)

This replaces XML endpoint testing guides, which relied on the removed HTTP stack.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(DtrExtension.class)
class ExceptionDocTest {

    @Test
    void documentIllegalArgumentException(DtrContext ctx) {
        ctx.sayNextSection("Validation Errors");
        ctx.say("The UserService throws IllegalArgumentException for invalid input:");

        try {
            throw new IllegalArgumentException("userId must be positive, got: -1");
        } catch (IllegalArgumentException e) {
            ctx.sayException(e);
        }

        ctx.sayNote("Callers should catch IllegalArgumentException " +
                    "and return HTTP 400 Bad Request.");
    }
}
```

---

## Document a Caused-By Chain

```java
@Test
void documentCausedByChain(DtrContext ctx) {
    ctx.sayNextSection("Database Error Chain");
    ctx.say("When the database is unavailable, the repository throws a wrapped exception:");

    var dbException = new java.sql.SQLException("Connection refused: localhost:5432");
    var serviceException = new RuntimeException(
        "Failed to load user with id=42", dbException);

    ctx.sayException(serviceException);

    ctx.sayWarning("Database connectivity errors are not retried automatically. " +
                   "The calling service must implement its own retry policy.");
}
```

---

## Document All Expected Exceptions for a Method

```java
static class UserService {
    public User findById(long id) {
        if (id <= 0) throw new IllegalArgumentException("id must be positive, got: " + id);
        if (id > Long.MAX_VALUE / 2) throw new ArithmeticException("id overflow: " + id);
        throw new java.util.NoSuchElementException("User not found: " + id);
    }

    record User(long id, String name) {}
}

@Test
void documentUserServiceExceptions(DtrContext ctx) {
    ctx.sayNextSection("UserService.findById() — Expected Exceptions");
    ctx.say("The findById method throws different exceptions depending on the error condition:");

    ctx.say("**Case 1:** id is zero or negative:");
    try {
        new UserService().findById(-1);
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    ctx.say("**Case 2:** user does not exist:");
    try {
        new UserService().findById(99999);
    } catch (java.util.NoSuchElementException e) {
        ctx.sayException(e);
    }

    ctx.sayTable(new String[][] {
        {"Exception", "HTTP Status", "Condition"},
        {"IllegalArgumentException", "400 Bad Request", "id <= 0"},
        {"NoSuchElementException", "404 Not Found", "User not found in DB"},
        {"RuntimeException", "500 Internal Server Error", "Unexpected system error"}
    });
}
```

---

## Document Exception Handling with Sealed Result Types

Instead of throwing exceptions, use sealed result types for domain-level errors. Document both approaches:

```java
sealed interface FindResult<T> {
    record Found<T>(T value) implements FindResult<T> {}
    record NotFound(long id) implements FindResult<Object> {}
    record InvalidInput(String field, String reason) implements FindResult<Object> {}
}

@Test
void documentSealedResults(DtrContext ctx) {
    ctx.sayNextSection("UserService: Sealed Result Types");
    ctx.say("In DTR 2.6.0, prefer sealed result types over thrown exceptions " +
            "for expected business-level error conditions:");

    ctx.sayCode("""
        sealed interface FindResult<T> {
            record Found<T>(T value) implements FindResult<T> {}
            record NotFound(long id) implements FindResult<Object> {}
            record InvalidInput(String field, String reason) implements FindResult<Object> {}
        }

        // Usage — exhaustive, compiler-checked
        String response = switch (userService.find(42)) {
            case FindResult.Found<User>(User u) -> "Found: " + u.name();
            case FindResult.NotFound(long id) -> "Not found: " + id;
            case FindResult.InvalidInput(String f, String r) -> f + ": " + r;
        };
        """, "java");

    // Document what the exception looks like when thrown from legacy code
    ctx.say("Legacy code may still throw. Document those exceptions:");
    try {
        throw new java.util.NoSuchElementException("User 42 not found (legacy path)");
    } catch (java.util.NoSuchElementException e) {
        ctx.sayException(e);
    }
}
```

---

## Combine with assertThatThrownBy (AssertJ)

```java
@Test
void documentAndAssertException(DtrContext ctx) {
    ctx.sayNextSection("Order Validation");
    ctx.say("The OrderService rejects orders with negative quantities:");

    // Document the expected exception
    try {
        throw new IllegalArgumentException("quantity must be positive, got: -5");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    // Also assert it using AssertJ
    var service = new OrderService();
    assertThatThrownBy(() -> service.createOrder(-5, 29.99))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("quantity must be positive");
}
```

---

## Best Practices

**Document all checked exceptions.** If a method declares `throws`, document what each exception means and when callers should expect it.

**Prefer sealed result types for expected errors.** Use `sayException` for genuinely exceptional conditions (database down, I/O failure). For business errors (not found, invalid input), prefer sealed result types.

**Show the full chain.** Wrap library exceptions in domain exceptions with context. Use `new MyException("message", cause)` so the caused-by chain is meaningful.

**Pair with a table.** After documenting individual exceptions with `sayException`, add a summary table mapping exceptions to HTTP status codes and recovery actions.

---

## See Also

- [Pattern Matching with Sealed Records](pattern-matching.md) — Exhaustive error handling
- [Document JSON Payloads](test-json-endpoints.md) — HTTP error response documentation
- [Document Record Schemas](upload-files.md) — sayRecordComponents for error types
