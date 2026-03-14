# How-to: Switch Expressions for Concise Type Handling

Use Java 25 switch expressions to replace `if/else` chains with exhaustive, type-safe pattern matching. Switch expressions return values, enable guarded patterns, and force completeness at compile time.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Traditional Switch Statements (Old)

```java
String status = httpCode;
String message;

switch (status) {
    case "200":
        message = "OK";
        break;
    case "404":
        message = "Not Found";
        break;
    case "500":
        message = "Server Error";
        break;
    default:
        message = "Unknown";
}
```

Drawbacks: requires `break`, no type safety, `default` silently catches new cases, verbose.

---

## Switch Expressions (Java 14+)

Return a value directly; arrow syntax (`->`) eliminates `break`:

```java
String message = switch (httpCode) {
    case "200" -> "OK";
    case "404" -> "Not Found";
    case "500" -> "Server Error";
    default -> "Unknown";
};
```

---

## Exhaustive Switches on Sealed Types

When the switch input is a sealed type, the compiler requires all cases:

```java
sealed interface HttpResult {
    record Success(int code) implements HttpResult {}
    record NotFound(String path) implements HttpResult {}
    record Error(Exception e) implements HttpResult {}
}

HttpResult result = new HttpResult.Success(200);

// Exhaustive: all three cases required, no default
String message = switch (result) {
    case HttpResult.Success(int code) -> "Success: " + code;
    case HttpResult.NotFound(String path) -> "Not found: " + path;
    case HttpResult.Error(Exception e) -> "Error: " + e.getMessage();
};
```

If you add a fourth case to the sealed interface, the compiler forces all switches to be updated.

---

## Document Switch Logic in DTR Tests

```java
@ExtendWith(DtrExtension.class)
class SwitchExpressionDocTest {

    sealed interface ServiceResult {
        record Created(long id) implements ServiceResult {}
        record Updated(long id, int version) implements ServiceResult {}
        record Deleted(long id) implements ServiceResult {}
        record Conflict(String reason) implements ServiceResult {}
    }

    @Test
    void documentResultHandling(DtrContext ctx) {
        ctx.sayNextSection("ServiceResult Switch Expression");
        ctx.say("The ServiceResult sealed type is handled with an exhaustive switch expression:");

        ctx.sayClassDiagram(
            ServiceResult.class,
            ServiceResult.Created.class,
            ServiceResult.Updated.class,
            ServiceResult.Deleted.class,
            ServiceResult.Conflict.class
        );

        ctx.sayCode("""
            int statusCode(ServiceResult result) {
                return switch (result) {
                    case ServiceResult.Created(long id) -> 201;
                    case ServiceResult.Updated(long id, int v) -> 200;
                    case ServiceResult.Deleted(long id) -> 204;
                    case ServiceResult.Conflict(String reason) -> 409;
                };
            }
            """, "java");

        ctx.sayNote("Adding a new ServiceResult variant requires updating every switch — " +
                    "the compiler enforces this.");
    }
}
```

---

## Multi-line Expressions

Use curly braces `{}` for multiple statements:

```java
String description = switch (result) {
    case HttpResult.Success(int code) -> {
        System.out.println("Request succeeded");
        yield "Success: " + code;  // 'yield' returns the value
    }
    case HttpResult.NotFound(String path) -> {
        System.out.println("Resource missing: " + path);
        yield "Not found";
    }
    case HttpResult.Error(Exception e) -> {
        e.printStackTrace();
        yield "Error: " + e.getMessage();
    }
};
```

Use `yield` to return a value from a multi-statement case.

---

## Guarded Patterns

Add conditions with `when`:

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
    case Number.Double(double d) -> "Decimal: " + d;
};
```

---

## Combined Pattern Matching and Guards

```java
record Request(String method, String path, int code) {}

Request req = new Request("GET", "/api/users", 200);

String outcome = switch (req) {
    case Request("GET", String path, int code) when code == 200 ->
        "Fetched from " + path;

    case Request("POST", String path, int code) when code == 201 ->
        "Created at " + path;

    case Request(String method, String path, int code) when code >= 400 ->
        method + " " + path + " failed with " + code;

    case Request(String method, String path, _) ->
        "Unknown: " + method + " " + path;
};
```

---

## Unnamed Patterns in Switch

Ignore fields you don't care about:

```java
record ApiResponse(int status, String body, long timestamp) {}

ApiResponse resp = new ApiResponse(200, "OK", System.currentTimeMillis());

String message = switch (resp) {
    case ApiResponse(200, String body, _) -> "Success: " + body;
    case ApiResponse(int status, _, _) -> "Status: " + status;
};
```

---

## Real-World Example: API Response Handling

```java
sealed interface ApiResponse {
    record Ok(String data) implements ApiResponse {}
    record Redirect(String location) implements ApiResponse {}
    record BadRequest(String field, String reason) implements ApiResponse {}
    record Unauthorized() implements ApiResponse {}
    record ServerError(String message) implements ApiResponse {}
}

int handleResponse(ApiResponse response) {
    return switch (response) {
        case ApiResponse.Ok(String data) when !data.isEmpty() -> {
            System.out.println("Response: " + data);
            yield 200;
        }
        case ApiResponse.Ok(_) -> {
            System.out.println("Empty response");
            yield 204;
        }
        case ApiResponse.Redirect(String location) -> {
            System.out.println("Redirecting to " + location);
            yield 301;
        }
        case ApiResponse.BadRequest(String field, String reason) -> {
            System.out.println("Validation failed: " + field + " — " + reason);
            yield 400;
        }
        case ApiResponse.Unauthorized() -> {
            System.out.println("Authentication required");
            yield 401;
        }
        case ApiResponse.ServerError(String message) -> {
            System.out.println("Server error: " + message);
            yield 500;
        }
    };
}
```

---

## Best Practices

**Use switch expressions instead of `if/else` chains.** They are more readable and the compiler enforces exhaustiveness on sealed types.

**Define sealed interfaces for type-safe case handling.** The compiler catches missing cases and forces updates when new variants are added.

**Use guards (`when`) to refine patterns.** They let you split a single type into multiple behavioral cases.

**Never use `default` in exhaustive switches on sealed types.** A `default` case defeats the compiler's exhaustiveness check — new sealed variants go silently unhandled.

**Document with sayClassDiagram.** Readers need to understand the type hierarchy before they can understand the switch logic.

---

## See Also

- [Pattern Matching with Sealed Records](pattern-matching.md) — instanceof pattern matching
- [Generate Class Diagrams](websockets-broadcast.md) — sayClassDiagram for sealed hierarchies
- [Document Exception Handling](test-xml-endpoints.md) — Sealed result types vs. exceptions
