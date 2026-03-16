# HttpDocTestHelper — Fluent HTTP Testing for DTR

**Feature ID:** DTR-014
**Status:** ✅ Implemented
**Version:** 2026.4.0

## Overview

`HttpDocTestHelper` is a fluent API helper for HTTP API documentation testing in DTR. It reduces boilerplate when documenting HTTP interactions by providing a chainable builder pattern for making requests, asserting on responses, and generating documentation.

## Problem Statement

Before `HttpDocTestHelper`, testing and documenting HTTP APIs in DTR required verbose code:

```java
// Old way: verbose and repetitive
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.example.com/users"))
    .GET()
    .build();

HttpResponse<String> response = client.send(request,
    HttpResponse.BodyHandlers.ofString());

ctx.sayAndAssertThat("HTTP Status", response.statusCode(), is(200));
ctx.sayKeyValue(Map.of(
    "HTTP Status", String.valueOf(response.statusCode()),
    "Content-Type", response.headers().firstValue("Content-Type").orElse("unknown")
));
ctx.sayNextSection("User List");
ctx.sayCode(response.body(), "json");
```

## Solution

`HttpDocTestHelper` provides a fluent, chainable API:

```java
// New way: concise and readable
var helper = new HttpDocTestHelper(ctx);

helper.get("https://api.example.com/users")
      .send()
      .expectStatus(200)
      .documentResponse()
      .documentJson("User List");
```

## API Design

### Core Classes

1. **`HttpDocTestHelper`** — Entry point for HTTP requests
2. **`RequestBuilder`** — Fluent builder for configuring requests
3. **`ResponseBuilder`** — Fluent builder for assertions and documentation

### HTTP Methods

All standard HTTP methods are supported:

```java
helper.get(url)      // GET request
helper.post(url)     // POST request
helper.put(url)      // PUT request
helper.delete(url)   // DELETE request
helper.patch(url)    // PATCH request
```

### Request Configuration

```java
helper.post("https://api.example.com/users")
      .header("Content-Type", "application/json")     // Single header
      .headers(Map.of(                                // Multiple headers
          "Content-Type", "application/json",
          "Authorization", "Bearer token123"
      ))
      .body("{\"name\":\"Alice\"}")                   // Request body
      .send();                                        // Execute request
```

### Response Assertions

```java
helper.get(url).send()
      .expectStatus(200)                              // Assert status code
      .expectHeader("Content-Type", "application/json") // Assert header value
      .expectHeaderPresent("Location")                // Assert header exists
      .expectBodyContains("users")                    // Assert substring in body
      .expectBody("{\"status\":\"ok\"}");             // Assert exact body
```

### Documentation Methods

```java
helper.get(url).send()
      .documentResponse()                             // Document status + Content-Type
      .documentJson("Response Label")                 // Document body as JSON
      .documentBody("Label", "xml");                  // Document body with custom language
```

## Usage Examples

### Example 1: Simple GET Request

```java
@ExtendWith(DtrExtension.class)
class UserApiDocTest {

    @Test
    void testGetUsers(DtrContext ctx) throws Exception {
        var helper = new HttpDocTestHelper(ctx);

        helper.get("https://api.example.com/users")
              .send()
              .expectStatus(200)
              .documentResponse()
              .documentJson("User List");
    }
}
```

**Output:**
```markdown
### User List

| HTTP Status | Content-Type |
|-------------|--------------|
| 200         | application/json |

```json
{
  "users": [...]
}
```
```

### Example 2: POST with JSON Body

```java
@Test
void testCreateUser(DtrContext ctx) throws Exception {
    var helper = new HttpDocTestHelper(ctx);

    String requestBody = """
        {
          "name": "Alice",
          "email": "alice@example.com"
        }
        """;

    helper.post("https://api.example.com/users")
          .header("Content-Type", "application/json")
          .body(requestBody)
          .send()
          .expectStatus(201)
          .expectHeader("Location", "https://api.example.com/users/123")
          .documentResponse()
          .documentJson("Created User");
}
```

### Example 3: Custom HTTP Client

```java
@Test
void testWithCustomClient(DtrContext ctx) throws Exception {
    // Custom client with extended timeout
    HttpClient customClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(30))
        .build();

    var helper = new HttpDocTestHelper(ctx, customClient);

    helper.get("https://slow-api.example.com/data")
          .send()
          .expectStatus(200)
          .documentResponse();
}
```

### Example 4: Advanced Response Validation

```java
@Test
void testAdvancedValidation(DtrContext ctx) throws Exception {
    var helper = new HttpDocTestHelper(ctx);

    var response = helper.get("https://api.example.com/users/123")
                         .send()
                         .expectStatus(200)
                         .expectBodyContains("alice@example.com");

    // Access raw response for custom assertions
    String body = response.getBody();
    JsonObject json = JsonParser.parseString(body).getAsJsonObject();

    ctx.sayAndAssertThat("User ID", json.get("id").getAsInt(), equalTo(123));
    ctx.sayAndAssertThat("Email", json.get("email").getAsString(), containsString("@"));
}
```

### Example 5: Full CRUD Documentation

```java
@Test
void testCrudLifecycle(DtrContext ctx) throws Exception {
    var helper = new HttpDocTestHelper(ctx);

    // CREATE
    ctx.sayNextSection("Create User");
    String newUser = helper.post("https://api.example.com/users")
                          .header("Content-Type", "application/json")
                          .body("{\"name\":\"Bob\"}")
                          .send()
                          .expectStatus(201)
                          .getBody();

    String userId = JsonParser.parseString(newUser)
                             .getAsJsonObject()
                             .get("id").getAsString();

    // READ
    ctx.sayNextSection("Read User");
    helper.get("https://api.example.com/users/" + userId)
          .send()
          .expectStatus(200)
          .documentJson("User Details");

    // UPDATE
    ctx.sayNextSection("Update User");
    helper.put("https://api.example.com/users/" + userId)
          .header("Content-Type", "application/json")
          .body("{\"name\":\"Robert\"}")
          .send()
          .expectStatus(200)
          .documentJson("Updated User");

    // DELETE
    ctx.sayNextSection("Delete User");
    helper.delete("https://api.example.com/users/" + userId)
          .send()
          .expectStatus(204);
}
```

## Method Reference

### HttpDocTestHelper

| Method | Description |
|--------|-------------|
| `HttpDocTestHelper(DtrContext ctx)` | Create helper with default HTTP client (10s timeout) |
| `HttpDocTestHelper(DtrContext ctx, HttpClient client)` | Create helper with custom HTTP client |
| `get(String url)` | Start building a GET request |
| `post(String url)` | Start building a POST request |
| `put(String url)` | Start building a PUT request |
| `delete(String url)` | Start building a DELETE request |
| `patch(String url)` | Start building a PATCH request |
| `getClient()` | Get the underlying HttpClient |

### RequestBuilder

| Method | Description |
|--------|-------------|
| `header(String name, String value)` | Add a single header |
| `headers(Map<String, String> headers)` | Add multiple headers |
| `body(String body)` | Set request body |
| `send()` | Execute request and return ResponseBuilder |

### ResponseBuilder

| Method | Description |
|--------|-------------|
| `expectStatus(int expected)` | Assert status code |
| `expectHeader(String name, String expectedValue)` | Assert header value |
| `expectHeaderPresent(String name)` | Assert header exists |
| `expectBodyContains(String substring)` | Assert body contains substring |
| `expectBody(String expectedBody)` | Assert exact body match |
| `documentResponse()` | Document status + Content-Type |
| `documentJson(String label)` | Document body as JSON code block |
| `documentBody(String label, String language)` | Document body with custom language |
| `getBody()` | Get raw response body |
| `getResponse()` | Get underlying HttpResponse |
| `getStatusCode()` | Get status code (convenience) |
| `getHeader(String name)` | Get header value (convenience) |

## Design Decisions

### 1. Immutability in RequestBuilder

`RequestBuilder` methods return new instances rather than mutating:

```java
// Good: each call creates a new builder
helper.get(url)
      .header("A", "1")   // Returns new builder
      .header("B", "2");  // Returns new builder

// Avoid: don't reuse the same builder variable
RequestBuilder builder = helper.get(url);
builder.header("A", "1");  // Don't do this - returns new instance!
builder.header("B", "2");  // This header is lost!
```

**Rationale:** Prevents accidental mutation and enables safe parallel test execution.

### 2. Checked Exceptions

`send()` throws `Exception` rather than wrapping in unchecked exceptions:

```java
@Test
void testHttp(DtrContext ctx) throws Exception {  // Must declare throws
    helper.get(url).send();
}
```

**Rationale:** HTTP failures (network errors, timeouts) are exceptional and should be explicitly handled by the test framework.

### 3. No Automatic JSON Parsing

`HttpDocTestHelper` works with strings, not JSON objects:

```java
// Returns raw string
String body = helper.get(url).send().getBody();

// Parse separately if needed
JsonObject json = JsonParser.parseString(body).getAsJsonObject();
```

**Rationale:** Keeps DTR dependency-free and flexible. Users can choose any JSON library (Jackson, Gson, JSON-P, etc.).

### 4. Fluent API Terminated by send()

Request building and response handling are separate phases:

```java
// Phase 1: Build request
helper.get(url).header("A", "1")

// Phase 2: Execute (terminal operation)
       .send()

// Phase 3: Assert and document
       .expectStatus(200);
```

**Rationale:** Clear separation of concerns. Request configuration happens before execution; response assertions happen after.

## Integration with DTR

`HttpDocTestHelper` integrates seamlessly with existing DTR features:

### With sayAndAssertThat

```java
var response = helper.get(url).send().expectStatus(200);

// Custom assertions with automatic documentation
ctx.sayAndAssertThat("Response time", responseTime, lessThan(1000));
ctx.sayAndAssertThat("User count", userCount, equalTo(10));
```

### With sayKeyValue

```java
helper.get(url).send().documentResponse();

// Add custom metadata
ctx.sayKeyValue(Map.of(
    "Response time", "23ms",
    "Cache hit", "true"
));
```

### With sayTable

```java
// Compare multiple endpoints
helper.get(url1).send().expectStatus(200);
int status1 = helper.getClient().send(...).statusCode();

helper.get(url2).send().expectStatus(200);
int status2 = helper.getClient().send(...).statusCode();

ctx.sayTable(new String[][]{
    {"Endpoint", "Status", "Time"},
    {url1, String.valueOf(status1), "23ms"},
    {url2, String.valueOf(status2), "45ms"}
});
```

## Testing Considerations

### 1. Use Public APIs for Examples

Tests should hit stable, public APIs (e.g., httpbin.org) rather than internal services:

```java
// Good: reliable public API
helper.get("https://httpbin.org/json").send();

// Avoid: internal service that might be down
helper.get("http://localhost:8080/internal").send();
```

### 2. Handle Network Failures Gracefully

```java
@Test
void testApi(DtrContext ctx) throws Exception {
    try {
        helper.get("https://api.example.com/users")
              .send()
              .expectStatus(200);
    } catch (IOException | InterruptedException e) {
        ctx.sayWarning("API unavailable: " + e.getMessage());
        // Skip or fail gracefully
    }
}
```

### 3. Mock External Dependencies in CI

For CI/CD, consider mocking HTTP responses to avoid flaky tests:

```java
// Use Mockito or WireMock to mock HTTP server in unit tests
@Mock HttpClient mockedClient;

// In test, inject mocked client
var helper = new HttpDocTestHelper(ctx, mockedClient);
```

## Future Enhancements

Potential future additions (not yet implemented):

- **Async support:** `sendAsync()` for non-blocking requests
- **Retry logic:** `.retry(3)` for transient failures
- **Authentication helpers:** `.bearerAuth(token)`, `.basicAuth(user, pass)`
- **JSON helpers:** `.expectJsonPath("$.users.length()", equalTo(10))`
- **Schema validation:** `.expectJsonSchema(schema)`

## Related Documentation

- [DtrContext Javadoc](https://javadoc.io/doc/io.github.seanchatmangpt/dtr-core/latest/io/github/seanchatmangpt/dtr/junit5/DtrContext.html)
- [HttpClient (Java 11+)](https://docs.oracle.com/en/java/javase/11/docs/api/java.net.http/java/net/http/HttpClient.html)
- [RFC 9110: HTTP Semantics](https://httpwg.org/specs/rfc9110.html)

## Migration Guide

### From Plain HttpClient

**Before:**
```java
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create(url))
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(body))
    .build();

HttpResponse<String> response = client.send(request,
    HttpResponse.BodyHandlers.ofString());

ctx.sayAndAssertThat("Status", response.statusCode(), is(200));
```

**After:**
```java
var helper = new HttpDocTestHelper(ctx);

helper.post(url)
      .header("Content-Type", "application/json")
      .body(body)
      .send()
      .expectStatus(200);
```

### From TestBrowser (dtr-integration-test)

**Before:**
```java
Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/users"))
);

ctx.sayAndAssertThat("Status", response.httpStatus, is(200));
ctx.sayCode(response.payload, "json");
```

**After:**
```java
var helper = new HttpDocTestHelper(ctx);

helper.get(testServerUrl().path("/api/users"))
      .send()
      .expectStatus(200)
      .documentJson("Users");
```

**Note:** TestBrowser is still preferred for integration tests with Ninja Framework or when you need automatic cookie handling and session management.

---

**Implementation File:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/junit5/HttpDocTestHelper.java`

**Example Tests:** `/Users/sac/dtr/dtr-core/src/test/java/io/github/seanchatmangpt/dtr/junit5/HttpDocTestHelperExampleTest.java`
