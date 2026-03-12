# Reference: Request API

**Package:** `org.r10r.doctester.testbrowser`
**File:** `dtr-core/src/main/java/org/r10r/doctester/testbrowser/Request.java`

`Request` is a fluent builder for HTTP requests. Create instances via the static factory methods and chain configuration methods.

---

## Factory methods

All factory methods return a new `Request` instance with the given HTTP method set.

#### `Request.GET()` → `Request`
#### `Request.POST()` → `Request`
#### `Request.PUT()` → `Request`
#### `Request.PATCH()` → `Request`
#### `Request.DELETE()` → `Request`
#### `Request.HEAD()` → `Request`

```java
Request.GET().url(testServerUrl().path("/api/users"))
Request.POST().url(testServerUrl().path("/api/users")).contentTypeApplicationJson().payload(user)
Request.DELETE().url(testServerUrl().path("/api/users/42"))
```

---

## URL

#### `url(Url url)` → `Request`

Sets the target URL using a `Url` builder instance.

```java
Request.GET().url(testServerUrl().path("/api/articles"))
```

#### `url(URI uri)` → `Request`

Sets the target URL using a `java.net.URI`.

```java
Request.GET().url(URI.create("http://localhost:8080/api/data"))
```

---

## Content type

#### `contentTypeApplicationJson()` → `Request`

Sets the `Content-Type` header to `application/json; charset=utf-8`.

```java
Request.POST()
    .url(testServerUrl().path("/api/users"))
    .contentTypeApplicationJson()
    .payload(newUser)
```

#### `contentTypeApplicationXml()` → `Request`

Sets the `Content-Type` header to `application/xml; charset=utf-8`.

```java
Request.POST()
    .url(testServerUrl().path("/api/articles.xml"))
    .contentTypeApplicationXml()
    .payload(article)
```

---

## Headers

#### `addHeader(String key, String value)` → `Request`

Adds a single header. Can be called multiple times to add multiple headers.

```java
Request.GET()
    .url(testServerUrl().path("/api/data"))
    .addHeader("Authorization", "Bearer token123")
    .addHeader("X-Request-ID", "abc-456")
```

#### `headers(Map<String, String> headers)` → `Request`

Replaces all headers with the provided map.

```java
Map<String, String> headers = Map.of(
    "Authorization", "Bearer token123",
    "Accept", "application/json");

Request.GET().url(url).headers(headers)
```

> Warning: `headers(Map)` replaces all existing headers including any set by `contentTypeApplicationJson()`. Call `headers()` before content type methods, or use `addHeader()` instead.

---

## Request body (payload)

#### `payload(Object payload)` → `Request`

Sets the request body. The object is serialized based on the `Content-Type` header:
- `application/json` → Jackson `ObjectMapper.writeValueAsString()`
- `application/xml` → Jackson `XmlMapper.writeValueAsString()`

```java
record CreateUser(String name, String email) {}

Request.POST()
    .url(testServerUrl().path("/api/users"))
    .contentTypeApplicationJson()
    .payload(new CreateUser("alice", "alice@example.com"))
```

**Parameters:**
- `payload` — Any Java object. Must be Jackson-serializable.

---

## Form parameters

#### `addFormParameter(String key, String value)` → `Request`

Adds a URL-encoded form field. Used for `application/x-www-form-urlencoded` requests (login forms, etc.).

```java
Request.POST()
    .url(testServerUrl().path("/api/login"))
    .addFormParameter("username", "alice")
    .addFormParameter("password", "secret")
```

> Do not combine `addFormParameter` with `contentTypeApplicationJson()`. Form parameters and JSON payloads are mutually exclusive.

#### `formParameters(Map<String, String> params)` → `Request`

Replaces all form parameters with the provided map.

---

## File uploads

#### `addFileToUpload(String paramName, File file)` → `Request`

Adds a file to a `multipart/form-data` request. Can be combined with `addFormParameter`.

```java
Request.POST()
    .url(testServerUrl().path("/api/avatars"))
    .addFormParameter("userId", "42")
    .addFileToUpload("avatar", new File("src/test/resources/test.png"))
```

> Setting a file upload automatically uses `multipart/form-data`. Do not set `contentTypeApplicationJson()`.

---

## Redirect handling

#### `followRedirects(boolean follow)` → `Request`

Controls whether the HTTP client follows redirects automatically.

```java
// Do not follow redirects — capture the 302 response
Request.POST()
    .url(testServerUrl().path("/api/login"))
    .addFormParameter("username", "alice")
    .addFormParameter("password", "secret")
    .followRedirects(false)
```

**Default:** `true` — redirects are followed automatically

---

## Utility

#### `payloadAsPrettyString()` → `String`

Returns the request payload as a pretty-printed string. Used internally by the HTML renderer but also available for assertions.

---

## Complete examples

**Authenticated GET with custom header:**
```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl()
            .path("/api/reports")
            .addQueryParameter("year", "2025"))
        .addHeader("Authorization", "Bearer " + token)
        .addHeader("Accept", "application/json"));
```

**POST with JSON:**
```java
record OrderRequest(String item, int quantity, String address) {}

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/orders"))
        .contentTypeApplicationJson()
        .payload(new OrderRequest("widget-42", 3, "123 Main St")));
```

**Login via form POST, no redirect:**
```java
Response loginResponse = makeRequest(
    Request.POST()
        .url(testServerUrl().path("/login"))
        .addFormParameter("username", "admin")
        .addFormParameter("password", "pass")
        .followRedirects(false));

// Expect redirect to /dashboard
sayAndAssertThat("Login redirects", 302, equalTo(loginResponse.httpStatus()));
```
