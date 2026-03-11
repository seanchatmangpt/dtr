# How-to: Switch Expressions for Concise Type Handling

Use Java 25 switch expressions to replace `if/else` chains with exhaustive, type-safe pattern matching. Switch expressions return values, enable guarded patterns, and force completeness at compile time.

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

Drawbacks:
- Requires `break` statements (easy to forget)
- No type safety
- `default` can silently catch new cases
- Verbose

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

**Benefits:**
- Returns a value (can assign to variable)
- No `break` needed
- More concise
- Type-safe

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
        System.out.println("Request failed");
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

The `when` guard refines the pattern — the case only matches if both the pattern and condition are true.

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

## Pattern Matching in Switch vs. If

**Old if/else:**
```java
Object obj = "hello";

if (obj instanceof String s) {
    System.out.println("String: " + s);
} else if (obj instanceof Integer i) {
    System.out.println("Integer: " + i);
} else {
    System.out.println("Unknown");
}
```

**Switch expression:**
```java
Object obj = "hello";

String result = switch (obj) {
    case String s -> "String: " + s;
    case Integer i -> "Integer: " + i;
    default -> "Unknown";
};
```

The switch is more readable and exhaustive checking applies automatically.

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

Use `_` for destructured fields you don't need.

---

## Comparison: If vs. Switch

| Feature | If/Else | Switch |
|---------|---------|--------|
| Return value | Assign to variable | Direct return |
| Type safety | Manual cast needed | Pattern matching |
| Completeness | No compiler check | Enforced on sealed types |
| Readability | Multiple conditions | Single expression |
| Exhaustiveness | Runtime bugs possible | Compile-time guarantee |

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
        // Success case
        case ApiResponse.Ok(String data) when !data.isEmpty() -> {
            System.out.println("Response: " + data);
            yield 200;
        }
        case ApiResponse.Ok(_) -> {
            System.out.println("Empty response");
            yield 204; // No content
        }

        // Redirect
        case ApiResponse.Redirect(String location) -> {
            System.out.println("Redirecting to " + location);
            yield 301;
        }

        // Client errors
        case ApiResponse.BadRequest(String field, String reason) -> {
            System.out.println("Validation failed: " + field + " — " + reason);
            yield 400;
        }
        case ApiResponse.Unauthorized() -> {
            System.out.println("Authentication required");
            yield 401;
        }

        // Server error
        case ApiResponse.ServerError(String message) -> {
            System.out.println("Server error: " + message);
            yield 500;
        }
    };
}
```

---

## Best Practices

✅ **DO:**
- Use switch expressions instead of `if/else` chains
- Define sealed interfaces for type-safe case handling
- Use guards (`when`) to refine patterns
- Let the compiler enforce exhaustiveness
- Use `_` for ignored fields

❌ **DON'T:**
- Use `default` in exhaustive switches on sealed types
- Forget guards when conditions matter
- Mix sealed and non-sealed in the same hierarchy
- Create deep nesting with multi-line cases

---

## See Also

- [Tutorial: Records and Sealed Classes](../tutorials/records-sealed-classes.md)
- [How-to: Pattern Matching with Sealed Records](pattern-matching.md)
- [Reference: Java 25 Language Features](../reference/java25-features-reference.md)
