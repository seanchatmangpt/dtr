# How-to: Pattern Matching with Sealed Records

Use Java 25 pattern matching to deconstruct sealed types and handle all cases exhaustively. Pattern matching eliminates explicit casts and makes type-safe handling natural.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Basic Pattern Matching: `instanceof`

Deconstruct a record inline:

```java
record User(String name, int age) {}

User user = new User("alice", 30);

// Old way: explicit cast
if (user instanceof User) {
    User u = (User) user;
    System.out.println(u.name());
}

// New way: pattern matching
if (user instanceof User(String name, int age)) {
    System.out.println(name);
    System.out.println(age);
}
```

The pattern `User(String name, int age)` destructures the record automatically.

---

## Exhaustive Switch on Sealed Types

Sealed interfaces guarantee you handle all cases:

```java
sealed interface ServiceResult<T> {
    record Success<T>(T value) implements ServiceResult<T> {}
    record NotFound(long id) implements ServiceResult<Object> {}
    record ValidationError(String field, String message) implements ServiceResult<Object> {}
}

ServiceResult<String> result = new ServiceResult.Success<>("data");

// Pattern matching switch: exhaustive (no default needed)
String description = switch (result) {
    case ServiceResult.Success<String>(String value) ->
        "Found: " + value;
    case ServiceResult.NotFound(long id) ->
        "Not found: " + id;
    case ServiceResult.ValidationError(String field, String message) ->
        "Invalid " + field + ": " + message;
};
```

If you forget a case, the compiler errors. If you add a new subtype to the sealed interface, all switches require updates.

---

## Document Pattern Matching in DTR Tests

Use `sayCode` and `sayClassDiagram` to document sealed type usage:

```java
@ExtendWith(DtrExtension.class)
class PatternMatchingDocTest {

    sealed interface ApiResult<T> {
        record Ok<T>(T data, int statusCode) implements ApiResult<T> {}
        record Error(int statusCode, String message) implements ApiResult<Object> {}
        record Redirect(String location) implements ApiResult<Object> {}
    }

    @Test
    void documentPatternMatching(DtrContext ctx) {
        ctx.sayNextSection("Pattern Matching with Sealed ApiResult");

        ctx.sayClassDiagram(
            ApiResult.class,
            ApiResult.Ok.class,
            ApiResult.Error.class,
            ApiResult.Redirect.class
        );

        ctx.sayCode("""
            String handle(ApiResult<?> result) {
                return switch (result) {
                    case ApiResult.Ok<?>(Object data, int code) when code == 200 ->
                        "Success: " + data;
                    case ApiResult.Ok<?>(Object data, int code) ->
                        "Unusual success " + code + ": " + data;
                    case ApiResult.Error(int code, String msg) ->
                        "Error " + code + ": " + msg;
                    case ApiResult.Redirect(String location) ->
                        "Redirect to " + location;
                };
            }
            """, "java");
    }
}
```

---

## Nested Record Patterns

Deconstruct nested records:

```java
record Person(String name, Address address) {}
record Address(String city, String country) {}

Person person = new Person("alice", new Address("London", "UK"));

if (person instanceof Person(String name, Address(String city, String country))) {
    System.out.println(name + " lives in " + city);
}
```

---

## Guarded Patterns

Add conditions with `when`:

```java
sealed interface Number {
    record Int(int value) implements Number {}
    record Float(float value) implements Number {}
}

Number num = new Number.Int(42);

String result = switch (num) {
    case Number.Int(int v) when v > 0 ->
        "Positive: " + v;
    case Number.Int(int v) when v < 0 ->
        "Negative: " + v;
    case Number.Int(int v) ->
        "Zero";
    case Number.Float(float f) ->
        "Float: " + f;
};

System.out.println(result); // "Positive: 42"
```

The `when` clause refines the pattern — the case only matches if both the pattern and the guard are true.

---

## Unnamed Patterns

Use `_` for fields you don't care about:

```java
record ApiResult(int status, String body, long timestamp) {}

ApiResult result = new ApiResult(200, "OK", System.currentTimeMillis());

// Pattern with unnamed fields
if (result instanceof ApiResult(int status, String _, _)) {
    System.out.println("Status: " + status);
    // body and timestamp are ignored
}
```

---

## Type Patterns in Switch

Handle different types exhaustively:

```java
sealed interface Payload {
    record JsonPayload(String data) implements Payload {}
    record XmlPayload(String data) implements Payload {}
    record BinaryPayload(byte[] data) implements Payload {}
}

Payload payload = new Payload.JsonPayload("{\"key\": \"value\"}");

String processed = switch (payload) {
    case Payload.JsonPayload(String json) -> "JSON: " + json;
    case Payload.XmlPayload(String xml) -> "XML: " + xml;
    case Payload.BinaryPayload(byte[] bytes) -> "Binary: " + bytes.length + " bytes";
};
```

---

## Real-World Example: HTTP Response Handling

```java
sealed interface HttpResponse<T> {
    record Ok<T>(T body) implements HttpResponse<T> {}
    record NotFound(String path) implements HttpResponse<Object> {}
    record ServerError(Throwable cause) implements HttpResponse<Object> {}
}

void handle(HttpResponse<String> response) {
    switch (response) {
        case HttpResponse.Ok<String>(String body) when !body.isBlank() -> {
            System.out.println("Body: " + body);
        }
        case HttpResponse.Ok<String>(_) -> {
            System.out.println("Empty response");
        }
        case HttpResponse.NotFound(String path) -> {
            System.out.println("Not found: " + path);
        }
        case HttpResponse.ServerError(Throwable cause) -> {
            System.out.println("Error: " + cause.getMessage());
        }
    }
}
```

---

## Pattern Matching in Method Parameters (Preview)

Java 25 allows pattern matching in method parameters (preview feature):

```java
// Requires --enable-preview
void process(HttpResponse.Ok<?>(Object body)) {
    System.out.println("Body: " + body);
}
```

---

## Best Practices

**Define sealed interfaces when you have multiple related types.** The compiler enforces exhaustiveness — adding a new case forces all switches to be updated.

**Use pattern matching instead of explicit casts.** Deconstruct directly in `instanceof` or `switch` patterns.

**Use guards (`when`) to refine patterns.** Guards let you split a single type into multiple cases based on field values.

**Use `_` for unused fields.** Makes it explicit that certain fields are intentionally ignored.

**Document sealed hierarchies with sayClassDiagram.** Readers need to see the type structure before they can understand the pattern matching code.

---

## See Also

- [Switch Expressions](switch-expressions.md) — Pattern matching in switch context
- [Generate Class Diagrams](websockets-broadcast.md) — sayClassDiagram for sealed hierarchies
- [Document Exception Handling](test-xml-endpoints.md) — sayException with sealed result types
