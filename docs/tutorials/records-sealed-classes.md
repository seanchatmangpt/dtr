# Tutorial: Records and Sealed Classes for Type-Safe Data Models

Learn how Java 25 records and sealed classes eliminate boilerplate and enable exhaustive pattern matching. This tutorial shows how to model data types safely and concisely — key for writing maintainable test code.

**Time:** ~30 minutes
**Prerequisites:** Java 25, familiarity with classes and inheritance
**What you'll learn:** How records replace getters/setters/equals, and how sealed hierarchies enforce completeness

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

    public String name() { return name; }
    public String email() { return email; }
    public int age() { return age; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        User user = (User) o;
        return age == user.age &&
               name.equals(user.name) &&
               email.equals(user.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, email, age);
    }

    @Override
    public String toString() {
        return "User{" + "name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", age=" + age + '}';
    }
}
```

Dozens of lines for a simple data carrier!

### The Java 25 Way: Records

```java
// After records: one line
record User(String name, String email, int age) {}
```

That's it. The record compiler automatically generates:
- ✅ Constructor with all fields
- ✅ Accessor methods (`name()`, `email()`, `age()`)
- ✅ `equals()` and `hashCode()`
- ✅ `toString()`
- ✅ Immutability (fields are final)

---

## Step 1 — Use Records in Test Code

Create `src/test/java/com/example/RecordsExampleTest.java`:

```java
package com.example;

import org.junit.Test;
import io.github.seanchatmangpt.dtr.doctester.DocTester;
import io.github.seanchatmangpt.dtr.doctester.testbrowser.Request;
import io.github.seanchatmangpt.dtr.doctester.testbrowser.Response;
import io.github.seanchatmangpt.dtr.doctester.testbrowser.Url;

import java.util.List;

public class RecordsExampleTest extends DTR {

    @Test
    public void useRecordsForApiPayloads() throws Exception {

        sayNextSection("Records for API Payloads");

        say("Records eliminate boilerplate when modeling request and response bodies. "
            + "Here we define a User record in one line:");

        say("```java\nrecord User(String name, String email, int age) {}\n```");

        // Create a user using the record constructor
        var newUser = new User("alice", "alice@example.com", 30);

        say("Create a user: " + newUser);

        // Send it as JSON
        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser));

        sayAndAssertThat(
            "User created with 201 status",
            201,
            org.hamcrest.CoreMatchers.equalTo(response.httpStatus()));

        say("The record was automatically serialized to JSON by Jackson. "
            + "No custom serialization code needed.");
    }

    @Test
    public void deserializeToRecords() throws Exception {

        sayNextSection("Deserialize to Records");

        say("Use response.payloadAs(User.class) to deserialize JSON back into a record:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path("/api/users/42")));

        User user = response.payloadAs(User.class);

        sayAndAssertThat(
            "Deserialized user has correct name",
            "Alice",
            org.hamcrest.CoreMatchers.equalTo(user.name()));

        say("The record constructor is called automatically. "
            + "Immutability is enforced — you can't modify user.name after creation.");
    }

    // Record definition
    record User(String name, String email, int age) {}

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

---

## Step 2 — Sealed Hierarchies for Type Safety

Define a sealed hierarchy when you have multiple record types that should be exhaustive:

```java
public class ApiResponseExampleTest extends DTR {

    // Sealed interface: only Success and Failure can implement it
    sealed interface ApiResponse {
        record Success(int status, String body) implements ApiResponse {}
        record Failure(int status, String error) implements ApiResponse {}
    }

    @Test
    public void sealedResponses() throws Exception {

        sayNextSection("Sealed Hierarchies");

        say("A sealed interface ensures we handle all possible response types. "
            + "The compiler enforces this with pattern matching.");

        // Make some requests and classify them
        Response httpResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path("/api/status")));

        ApiResponse result = httpResponse.httpStatus() == 200
            ? new ApiResponse.Success(
                httpResponse.httpStatus(),
                httpResponse.payloadAsString())
            : new ApiResponse.Failure(
                httpResponse.httpStatus(),
                "Request failed");

        say("Classifying response: " + result);

        // Pattern match on the sealed type
        String message = switch (result) {
            case ApiResponse.Success(int status, String body) ->
                "✓ Success: " + status + " with " + body.length() + " bytes";
            case ApiResponse.Failure(int status, String error) ->
                "✗ Failure: " + status + " — " + error;
        };

        say(message);

        sayAndAssertThat(
            "Response was successful",
            true,
            org.hamcrest.CoreMatchers.is(result instanceof ApiResponse.Success));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**Why this matters:**
- The `switch` statement is **exhaustive** — the compiler requires you to handle every subtype
- No `default` clause needed or allowed
- If you add a new type to the sealed interface, the compiler forces you to update all `switch` expressions
- This prevents silent bugs where you forget to handle a case

---

## Step 3 — Records with Methods

Records can contain methods for validation or computation:

```java
sealed interface HttpRequest permits CreateUserRequest, UpdateUserRequest {
    int validate();
}

record CreateUserRequest(String name, String email) implements HttpRequest {
    // Constructor with validation
    public CreateUserRequest {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be empty");
        }
        if (email == null || !email.contains("@")) {
            throw new IllegalArgumentException("email must be valid");
        }
    }

    @Override
    public int validate() {
        return 0; // 0 = valid
    }
}

record UpdateUserRequest(int id, String email) implements HttpRequest {
    @Override
    public int validate() {
        if (id <= 0) return 400;
        if (email == null || !email.contains("@")) return 400;
        return 0;
    }
}

@Test
public void recordsWithValidation() throws Exception {

    sayNextSection("Records with Methods and Validation");

    say("Records can implement interfaces and contain custom methods.");

    // Validation in record constructor
    try {
        var invalid = new CreateUserRequest("", "alice@example.com");
        say("ERROR: Should have thrown");
    } catch (IllegalArgumentException e) {
        say("✓ Constructor validation caught empty name: " + e.getMessage());
    }

    // Valid record
    var valid = new CreateUserRequest("alice", "alice@example.com");

    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/users"))
            .contentTypeApplicationJson()
            .payload(valid));

    sayAndAssertThat(
        "Valid request accepted",
        201,
        org.hamcrest.CoreMatchers.equalTo(response.httpStatus()));
}
```

---

## Step 4 — Complex Records with Collections

Records can contain collections and nested structures:

```java
@Test
public void recordsWithCollections() throws Exception {

    sayNextSection("Records with Collections");

    // Records containing lists
    record UserListResponse(List<User> users, int total) {}
    record User(int id, String name, String email) {}

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users")));

    UserListResponse listing = response.payloadAs(UserListResponse.class);

    say("Retrieved " + listing.total + " users");

    long emailsValid = listing.users.stream()
        .filter(u -> u.email().contains("@"))
        .count();

    say("Valid emails: " + emailsValid + "/" + listing.total);

    sayAndAssertThat(
        "All users have valid emails",
        listing.total,
        org.hamcrest.CoreMatchers.equalTo(emailsValid));
}
```

---

## Step 5 — Pattern Matching with Records

Use pattern matching to deconstruct records inline:

```java
sealed interface Result permits Success, Failure {
    record Success(int code, User data) implements Result {}
    record Failure(int code, String message) implements Result {}
}

record User(String name, String email) {}

@Test
public void patternMatchingOnRecords() throws Exception {

    sayNextSection("Pattern Matching");

    say("Pattern matching lets you destructure records in if/switch statements.");

    Result outcome = new Result.Success(200, new User("bob", "bob@example.com"));

    // Pattern match with deconstruction
    if (outcome instanceof Result.Success(int code, User user)) {
        say("Success code: " + code);
        say("User: " + user.name());
    }

    // Exhaustive switch on sealed type
    String description = switch (outcome) {
        case Result.Success(int code, User user) ->
            "✓ " + code + ": " + user.name();
        case Result.Failure(int code, String msg) ->
            "✗ " + code + ": " + msg;
    };

    say(description);
}
```

---

## Comparison Table

| Feature | Class | Record |
|---------|-------|--------|
| Boilerplate | Lots (constructor, getters, equals, hashCode, toString) | None |
| Mutability | Mutable by default | Immutable (fields are final) |
| Inheritance | Any class | Implements interfaces only (no inheritance) |
| Use case | Complex objects with behavior | Data carriers |
| Jackson support | Yes (with annotations) | Yes (automatic) |

---

## Best Practices

✅ **DO:**
- Use records for **data carriers** — DTOs, request/response bodies, configuration
- Use sealed interfaces with records for **type-safe hierarchies**
- Use pattern matching on sealed types for **exhaustive case handling**
- Nest records in test classes to keep code local
- Add validation to record constructors using compact syntax

❌ **DON'T:**
- Use records for entities with complex behavior (use regular classes)
- Mix mutable and immutable data in the same model
- Forget to seal hierarchies when you want exhaustiveness
- Create deep inheritance chains (records support interfaces, not extends)

---

## Next Steps

- [Tutorial: Virtual Threads for Concurrency](virtual-threads-lightweight-concurrency.md) — lightweight parallelism
- [How-to: Pattern Matching with Sealed Records](../how-to/pattern-matching.md) — advanced patterns
- [How-to: Text Blocks for Documentation](../how-to/text-blocks.md) — multiline strings in Java 25
- [Reference: Records API](../reference/records-sealed-reference.md) — complete documentation
- [Explanation: Why Records and Sealed Classes](../explanation/records-sealed-philosophy.md) — design philosophy
