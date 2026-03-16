# `HttpDocTestHelper`

> **Package:** `io.github.seanchatmangpt.dtr.junit5`  

Fluent helper for HTTP API documentation testing. Reduces boilerplate when documenting HTTP interactions. <p>Designed for DTR-based API documentation tests where you need to:</p> <ul>   <li>Make HTTP requests (GET, POST, PUT, DELETE, PATCH)</li>   <li>Assert on response status, headers, and body</li>   <li>Document the request/response cycle in generated markdown</li> </ul> <p>Usage example:</p> <pre>{@code @ExtendWith(DtrExtension.class) class UserApiDocTest {     @Test     void testGetUsers(DtrContext ctx) throws Exception {         var helper = new HttpDocTestHelper(ctx);         helper.get("https://api.example.com/users")               .expectStatus(200)               .documentResponse()               .documentJson("User List Response");     }     @Test     void testCreateUser(DtrContext ctx) throws Exception {         var helper = new HttpDocTestHelper(ctx);         String requestBody = """             {               "name": "Alice",               "email": "alice@example.com"             }             """;         helper.post("https://api.example.com/users")               .header("Content-Type", "application/json")               .body(requestBody)               .send()               .expectStatus(201)               .documentResponse();     } } }</pre> <p>The fluent API supports method chaining for readable test code:</p> <ul>   <li><strong>Request phase:</strong> {@code get/post/put/delete/patch} → {@code header} → {@code body} → {@code send}</li>   <li><strong>Response phase:</strong> {@code expectStatus} → {@code documentResponse} → {@code documentJson}</li> </ul> <p>For advanced scenarios, you can access the underlying {@link HttpClient} via {@link #getClient()} or provide a custom client via {@link #HttpDocTestHelper(DtrContext, HttpClient)}.</p>

```java
public class HttpDocTestHelper {
    // HttpDocTestHelper, HttpDocTestHelper, get, post, put, delete, patch, getClient, ... (24 total)
}
```

---

## Methods

### `HttpDocTestHelper`

Creates a new HttpDocTestHelper with a custom HTTP client. <p>Use this constructor when you need custom client configuration:</p> <ul>   <li>Custom timeouts</li>   <li>Custom executors (e.g., for async testing)</li>   <li>Custom authenticators</li>   <li>Custom proxy settings</li>   <li>Disable redirect following</li> </ul> <p>Example:</p> <pre>{@code HttpClient customClient = HttpClient.newBuilder()     .connectTimeout(Duration.ofSeconds(30))     .followRedirects(HttpClient.Redirect.NEVER)     .build(); var helper = new HttpDocTestHelper(ctx, customClient); }</pre>

| Parameter | Description |
| --- | --- |
| `ctx` | the DTR context for documentation output |
| `client` | a custom HTTP client (must not be null) |

| Exception | Description |
| --- | --- |
| `NullPointerException` | if client is null |

---

### `body`

Sets the request body. <p>For methods that don't typically send a body (GET, HEAD), the body is silently ignored by the HttpClient.</p>

| Parameter | Description |
| --- | --- |
| `body` | the request body as a string (e.g., JSON, XML, plain text) |

> **Returns:** a new RequestBuilder with the body set

---

### `delete`

Starts building a DELETE request.

| Parameter | Description |
| --- | --- |
| `url` | the target URL |

> **Returns:** a RequestBuilder for fluent configuration

---

### `documentBody`

Documents the response body as a code block with a custom language. <p>Use this for non-JSON responses (XML, HTML, plain text, etc.). The language parameter controls syntax highlighting in the generated markdown.</p>

| Parameter | Description |
| --- | --- |
| `label` | the section headline |
| `language` | the language identifier (e.g., "xml", "html", "text") |

> **Returns:** this ResponseBuilder for chaining

---

### `documentJson`

Documents the response body as a code block with the given label. <p>Use this for JSON, XML, or plain text responses. The content is rendered as a fenced code block in the generated markdown.</p> <p>Example output:</p> <pre>{@code ### User List Response ```json {   "users": [...] } ``` }</pre>

| Parameter | Description |
| --- | --- |
| `label` | the section headline (e.g., "User List Response") |

> **Returns:** this ResponseBuilder for chaining

---

### `documentResponse`

Documents the response metadata as a key-value table. <p>Documents the following properties:</p> <ul>   <li>HTTP Status (e.g., "200")</li>   <li>Content-Type (if present)</li> </ul> <p>Example output:</p> <pre>{@code | HTTP Status | Content-Type | |-------------|--------------| | 200         | application/json | }</pre>

> **Returns:** this ResponseBuilder for chaining

---

### `expectBody`

Asserts that the response body matches the expected value exactly. <p>Use this for APIs that return plain text responses. For JSON responses, prefer a JSON comparison library.</p>

| Parameter | Description |
| --- | --- |
| `expectedBody` | the expected response body (exact match) |

> **Returns:** this ResponseBuilder for chaining

---

### `expectBodyContains`

Asserts that the response body contains the expected substring. <p>This is a simple substring check, not a full JSON/XML validation. For structured validation, extract the body with {@link #getBody()} and use a JSON parser.</p>

| Parameter | Description |
| --- | --- |
| `substring` | the expected substring (case-sensitive) |

> **Returns:** this ResponseBuilder for chaining

---

### `expectHeader`

Asserts that a response header has the expected value. <p>If the header is missing, the assertion fails with a clear message. The assertion is documented in the DTR output.</p>

| Parameter | Description |
| --- | --- |
| `name` | the header name (case-insensitive per HTTP spec) |
| `expectedValue` | the expected header value |

> **Returns:** this ResponseBuilder for chaining

---

### `expectHeaderPresent`

Asserts that a response header is present. <p>Use this when you only care that a header exists, not its value.</p>

| Parameter | Description |
| --- | --- |
| `name` | the header name (case-insensitive per HTTP spec) |

> **Returns:** this ResponseBuilder for chaining

---

### `expectStatus`

Asserts that the response status code matches the expected value. <p>The assertion is documented as a passing check in the DTR output.</p>

| Parameter | Description |
| --- | --- |
| `expected` | the expected HTTP status code (e.g., 200, 404, 500) |

> **Returns:** this ResponseBuilder for chaining

---

### `get`

Starts building a GET request.

| Parameter | Description |
| --- | --- |
| `url` | the target URL |

> **Returns:** a RequestBuilder for fluent configuration

---

### `getBody`

Gets the raw response body as a string. <p>Use this for custom assertions or parsing:</p> <pre>{@code String json = helper.get(url).send().getBody(); JsonObject parsed = JsonParser.parseString(json).getAsJsonObject(); assertThat(parsed.get("count").getAsInt(), is(42)); }</pre>

> **Returns:** the response body as a string

---

### `getClient`

Gets the underlying HTTP client. <p>Use this for advanced scenarios not covered by the fluent API, such as making requests outside the RequestBuilder flow.</p>

> **Returns:** the HttpClient used by this helper

---

### `getHeader`

Gets a response header value. <p>Convenience method for header access.</p>

| Parameter | Description |
| --- | --- |
| `name` | the header name |

> **Returns:** the header value, or null if not present

---

### `getResponse`

Gets the underlying HttpResponse object. <p>Use this for advanced scenarios not covered by the fluent API, such as accessing all headers, status code, or the request that generated this response.</p>

> **Returns:** the HttpResponse object

---

### `getStatusCode`

Gets the response status code. <p>Convenience method equivalent to {@code getResponse().statusCode()}.</p>

> **Returns:** the HTTP status code

---

### `header`

Adds a header to the request. <p>Can be called multiple times to add multiple headers. If the same header name is used twice, the last value wins.</p>

| Parameter | Description |
| --- | --- |
| `name` | the header name (case-insensitive per HTTP spec) |
| `value` | the header value |

> **Returns:** a new RequestBuilder with the header added

---

### `headers`

Adds multiple headers to the request. <p>All headers in the map are added to the request. Existing headers with the same names are overwritten.</p>

| Parameter | Description |
| --- | --- |
| `headers` | map of header names to values |

> **Returns:** a new RequestBuilder with the headers added

---

### `patch`

Starts building a PATCH request.

| Parameter | Description |
| --- | --- |
| `url` | the target URL |

> **Returns:** a RequestBuilder for fluent configuration

---

### `post`

Starts building a POST request.

| Parameter | Description |
| --- | --- |
| `url` | the target URL |

> **Returns:** a RequestBuilder for fluent configuration

---

### `put`

Starts building a PUT request.

| Parameter | Description |
| --- | --- |
| `url` | the target URL |

> **Returns:** a RequestBuilder for fluent configuration

---

### `send`

Sends the HTTP request and returns a ResponseBuilder for assertions. <p>This is the terminal method in the RequestBuilder chain. After calling {@code send()}, you can chain ResponseBuilder methods:</p> <pre>{@code helper.get("https://api.example.com/users")       .send()       .expectStatus(200)       .documentResponse(); }</pre>

> **Returns:** a ResponseBuilder for asserting and documenting the response

| Exception | Description |
| --- | --- |
| `Exception` | if the request fails (network error, invalid URL, timeout) |

---

