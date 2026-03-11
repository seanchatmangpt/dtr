# How-to: Pattern Matching with Sealed Records

Use Java 25 pattern matching to deconstruct sealed types and handle all cases exhaustively. Pattern matching eliminates explicit casts and makes type-safe handling natural.

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
sealed interface HttpResponse {
    record Success(int status, String body) implements HttpResponse {}
    record Redirect(int status, String location) implements HttpResponse {}
    record Error(int status, String message) implements HttpResponse {}
}

HttpResponse response = new HttpResponse.Success(200, "OK");

// Pattern matching switch: exhaustive (no default needed)
String description = switch (response) {
    case HttpResponse.Success(int status, String body) ->
        "Success: " + status;
    case HttpResponse.Redirect(int status, String location) ->
        "Redirect to " + location;
    case HttpResponse.Error(int status, String message) ->
        "Error: " + message;
};

System.out.println(description); // "Success: 200"
```

If you forget a case, the compiler errors. If you add a new subtype to the sealed interface, all switches require updates.

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

Add conditions to pattern matching:

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

Or in a switch:

```java
String message = switch (result) {
    case ApiResult(200, String body, _) -> "Success: " + body;
    case ApiResult(int status, _, _) -> "Error: " + status;
};
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

System.out.println(processed);
```

---

## Combining Pattern Matching and Sealed Records in Real Code

```java
sealed interface RequestResult {
    record Ok(int code, String body) implements RequestResult {}
    record Failed(int code, Exception cause) implements RequestResult {}
}

void handleResult(RequestResult result) {
    switch (result) {
        case RequestResult.Ok(int code, String body) when code == 200 -> {
            System.out.println("Success: " + body);
        }
        case RequestResult.Ok(int code, String body) -> {
            System.out.println("Unusual success code: " + code);
        }
        case RequestResult.Failed(int code, Exception cause) -> {
            System.out.println("Failed with " + code + ": " + cause.getMessage());
        }
    }
}
```

---

## Pattern Matching in Method Parameters (Preview)

Java 25 allows pattern matching in method parameters (preview feature with `--enable-preview`):

```java
// Requires --enable-preview
void process(HttpResponse(int status, String body)) {
    System.out.println("Status: " + status);
}

// Called like:
process(new HttpResponse.Success(200, "OK"));
```

---

## Best Practices

✅ **DO:**
- Define sealed interfaces when you have multiple related types
- Use pattern matching instead of explicit casts
- Leverage exhaustive switches to prevent missing cases
- Use named patterns for fields you need; `_` for ones you don't
- Add guards (`when`) to refine patterns

❌ **DON'T:**
- Use non-sealed classes when exhaustiveness would help
- Mix sealed and unsealed in the same hierarchy
- Use default in exhaustive switches (forces defaults on new types)
- Over-use unnamed patterns if the field names matter

---

## See Also

- [Tutorial: Records and Sealed Classes](../tutorials/records-sealed-classes.md)
- [Reference: Records and Sealed Classes](../reference/records-sealed-reference.md)
