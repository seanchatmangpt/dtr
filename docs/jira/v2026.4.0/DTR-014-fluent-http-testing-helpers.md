# DTR-014: Fluent HTTP Testing Helpers (HttpDocTestHelper)

**Priority**: P4
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx, qol, test-authoring, http, fluent-api, testing

## Description

Create a fluent builder API for HTTP testing that reduces boilerplate and provides semantic, chainable methods for documenting HTTP interactions. This helper will wrap common HTTP client patterns while automatically generating documentation.

Currently, HTTP tests require verbose setup:

```java
@Test
void testGetUser(DtrContext ctx) throws Exception {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create("https://api.example.com/users/123"))
        .header("Authorization", "Bearer token")
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    ctx.sayNextSection("GET /users/{id}");
    ctx.sayCode("GET https://api.example.com/users/123", "http");
    ctx.sayKeyValue(Map.of(
        "Status", response.statusCode(),
        "Content-Type", response.headers().firstValue("Content-Type").orElse("unknown")
    ));
    ctx.sayCode(response.body(), "json");
    assertThat(response.statusCode()).isEqualTo(200);
}
```

With fluent helpers:

```java
@Test
void testGetUser() throws Exception {
    http("https://api.example.com")
        .path("/users/123")
        .header("Authorization", "Bearer token")
        .get()
        .expectStatus(200)
        .document();  // Auto-generates all documentation
}
```

## Acceptance Criteria

### Core API

- [ ] Create `HttpDocTestHelper` class with fluent builder API
- [ ] Support HTTP methods: GET, POST, PUT, DELETE, PATCH
- [ ] Support common headers: Authorization, Content-Type, Accept
- [ ] Support request body for POST/PUT/PATCH
- [ ] Support query parameters
- [ ] Automatic documentation generation on `document()` call

### Documentation Methods

- [ ] `document()` - Auto-generates complete HTTP interaction docs
- [ ] `documentRequest()` - Document only request
- [ ] `documentResponse()` - Document only response
- [ ] `documentHeaders()` - Document headers in table format
- [ ] `documentBody()` - Document response body with syntax highlighting

### Assertion Methods

- [ ] `expectStatus(int)` - Assert HTTP status code
- [ ] `expectHeader(String, String)` - Assert header value
- [ ] `expectBodyContains(String)` - Assert response body contains text
- [ ] `expectJsonPath(String, String)` - Assert JSON path value (if response is JSON)
- [ ] `expectContentType(String)` - Assert Content-Type header

### Advanced Features

- [ ] Support for template URIs (e.g., `/users/{id}`)
- [ ] Automatic request ID generation for tracking
- [ ] Support for multipart/form-data
- [ ] Support for file upload/download
- [ ] Integration with `@AuthenticatedTest` for automatic auth headers
- [ ] Configurable timeout and retry logic

### Testing & Documentation

- [ ] Comprehensive unit tests for all HTTP methods
- [ ] Integration tests against real HTTP endpoints (using test servers)
- [ ] Tutorial section: "HTTP Testing with Fluent Helpers"
- [ ] Javadoc with examples for each method
- [ ] Migration guide from verbose to fluent style

## Technical Notes

### File Paths

- **Main class**: `src/main/java/io/github/seanchatmangpt/dtr/http/HttpDocTestHelper.java`
- **Supporting classes**:
  - `src/main/java/io/github/seanchatmangpt/dtr/http/HttpRequestBuilder.java`
  - `src/main/java/io/github/seanchatmangpt/dtr/http/HttpResponseWrapper.java`
  - `src/main/java/io/github/seanchatmangpt/dtr/http/HttpDocTestException.java`
- **Tests**:
  - `src/test/java/io/github/seanchatmangpt/dtr/http/HttpDocTestHelperTest.java`
  - `src/test/java/io/github/seanchatmangpt/dtr/http/HttpDocTestHelperIntegrationTest.java`

### Implementation Details

#### HttpDocTestHelper Core API

```java
package io.github.seanchatmangpt.dtr.http;

import io.github.seanchatmangpt.dtr.DtrContext;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * Fluent API for HTTP testing with automatic documentation.
 *
 * <p>Usage:
 * <pre>
 * http("https://api.example.com")
 *     .path("/users/123")
 *     .header("Authorization", "Bearer token")
 *     .get()
 *     .expectStatus(200)
 *     .document();
 * </pre>
 */
public class HttpDocTestHelper {

    private final HttpClient client;
    private final String baseUrl;
    private final DtrContext ctx;
    private final HttpRequestBuilder requestBuilder;
    private HttpResponse<String> response;

    private HttpDocTestHelper(String baseUrl, DtrContext ctx) {
        this.client = HttpClient.newHttpClient();
        this.baseUrl = baseUrl;
        this.ctx = ctx;
        this.requestBuilder = new HttpRequestBuilder();
    }

    public static HttpDocTestHelper http(String baseUrl) {
        return new HttpDocTestHelper(baseUrl, DtrContext.get());
    }

    public HttpDocTestHelper path(String path) {
        requestBuilder.path(path);
        return this;
    }

    public HttpDocTestHelper queryParam(String key, String value) {
        requestBuilder.queryParam(key, value);
        return this;
    }

    public HttpDocTestHelper header(String key, String value) {
        requestBuilder.header(key, value);
        return this;
    }

    public HttpDocTestHelper body(String body) {
        requestBuilder.body(body);
        return this;
    }

    public HttpDocTestHelper timeout(Duration timeout) {
        requestBuilder.timeout(timeout);
        return this;
    }

    public HttpDocTestHelper get() throws Exception {
        execute("GET");
        return this;
    }

    public HttpDocTestHelper post(String body) throws Exception {
        requestBuilder.body(body);
        execute("POST");
        return this;
    }

    public HttpDocTestHelper put(String body) throws Exception {
        requestBuilder.body(body);
        execute("PUT");
        return this;
    }

    public HttpDocTestHelper delete() throws Exception {
        execute("DELETE");
        return this;
    }

    public HttpDocTestHelper patch(String body) throws Exception {
        requestBuilder.body(body);
        execute("PATCH");
        return this;
    }

    private void execute(String method) throws Exception {
        HttpRequest request = requestBuilder.build(baseUrl, method);
        response = client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // Assertions
    public HttpDocTestHelper expectStatus(int expectedStatus) {
        int actualStatus = response.statusCode();
        if (actualStatus != expectedStatus) {
            throw new HttpDocTestException(
                "Expected status " + expectedStatus + " but got " + actualStatus
            );
        }
        return this;
    }

    public HttpDocTestHelper expectHeader(String name, String value) {
        String actualValue = response.headers().firstValue(name).orElse(null);
        if (!value.equals(actualValue)) {
            throw new HttpDocTestException(
                "Expected header " + name + " = " + value + " but got " + actualValue
            );
        }
        return this;
    }

    public HttpDocTestHelper expectBodyContains(String text) {
        if (!response.body().contains(text)) {
            throw new HttpDocTestException(
                "Response body does not contain: " + text
            );
        }
        return this;
    }

    // Documentation
    public void document() {
        documentRequest();
        documentResponse();
        documentHeaders();
        documentBody();
    }

    public void documentRequest() {
        ctx.sayCode(requestBuilder.getMethod() + " " + requestBuilder.getFullUrl(), "http");
    }

    public void documentResponse() {
        ctx.sayKeyValue(Map.of(
            "Status", String.valueOf(response.statusCode()),
            "Request ID", requestBuilder.getRequestId()
        ));
    }

    public void documentHeaders() {
        ctx.sayTable(response.headers().map().entrySet().stream()
            .map(e -> new String[]{e.getKey(), String.join(", ", e.getValue())})
            .toArray(String[][]::new));
    }

    public void documentBody() {
        String contentType = response.headers().firstValue("Content-Type")
            .orElse("text/plain");
        String language = contentType.contains("json") ? "json" :
                         contentType.contains("xml") ? "xml" : "text";
        ctx.sayCode(response.body(), language);
    }
}
```

#### Static Import Support

```java
// In Dtr.java or DtrContext.java
public static HttpDocTestHelper http(String baseUrl) {
    return HttpDocTestHelper.http(baseUrl);
}
```

#### Usage with Static Imports

```java
import static io.github.seanchatmangpt.dtr.Dtr.*;

@Test
void testCreateUser() throws Exception {
    http("https://api.example.com")
        .path("/users")
        .header("Content-Type", "application/json")
        .post("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}")
        .expectStatus(201)
        .expectHeader("Location", "/users/123")
        .document();
}
```

### Test Scenarios

1. **Simple GET**: Basic request with auto-documentation
2. **POST with JSON**: Create resource with body
3. **Query parameters**: Search/filter operations
4. **Authentication**: Bearer token, Basic Auth
5. **Error responses**: 404, 500, etc.
6. **File upload**: multipart/form-data
7. **Retry logic**: Automatic retry on failure
8. **Multiple requests**: Chained API calls
9. **Template URIs**: `/users/{id}` with variable substitution
10. **Static import**: Clean syntax with `http()` method

### Integration with @AuthenticatedTest

```java
@Test
@AuthenticatedTest(token = "test-token", type = AuthType.BEARER)
void testSecureEndpoint() throws Exception {
    // Token automatically added to Authorization header
    http("https://api.example.com")
        .path("/secure")
        .get()
        .expectStatus(200)
        .document();
}
```

## Dependencies

- **DTR-013** (@AuthenticatedTest) - Can integrate for automatic auth headers
- **DTR-009** (@DtrTest) - Works seamlessly with static imports
- **Java 11+ HttpClient** - Uses built-in `java.net.http` (no external dependencies)

## References

- Similar to: RestAssured's fluent API
- Similar to: Spring's `TestRestTemplate`
- Documentation: Update `docs/tutorials/http-testing.md` with fluent examples
- API Reference: Add `docs/reference/http-testing-helpers.md`

## Success Metrics

- Reduces HTTP test boilerplate by 60-80%
- Improves test readability (intent is clear)
- Supports 90% of common HTTP testing patterns
- Zero external dependencies (uses Java 11+ HttpClient)
- Maintains compatibility with existing HTTP client code
- Performance overhead < 5% compared to manual HttpClient usage

## Future Enhancements (Out of Scope)

- WebSocket testing support
- gRPC testing support
- GraphQL testing support
- Response schema validation
- Mock server integration (WireMock, MockServer)
