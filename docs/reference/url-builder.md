# Reference: Url Builder

**Package:** `org.r10r.doctester.testbrowser`
**File:** `dtr-core/src/main/java/org/r10r/doctester/testbrowser/Url.java`

`Url` is a fluent URL builder. It is used to compose the target URL for `Request` objects, typically starting from `testServerUrl()`.

---

## Factory method

#### `Url.host(String host)` → `Url`

Creates a new `Url` from a host string. The host must include the scheme and may include a port.

```java
Url.host("http://localhost:8080")
Url.host("https://api.example.com")
Url.host("https://staging.example.com:8443")
```

---

## Path

#### `path(String path)` → `Url`

Appends a path segment to the URL. The path should start with `/`.

```java
testServerUrl().path("/api/users")
// → http://localhost:8080/api/users

testServerUrl().path("/api/articles/" + articleId)
// → http://localhost:8080/api/articles/42
```

Multiple `path()` calls are supported but uncommon:

```java
testServerUrl().path("/api").path("/users")
// → http://localhost:8080/api/users
```

---

## Query parameters

#### `addQueryParameter(String key, String value)` → `Url`

Appends a query parameter. Can be called multiple times. Values are URL-encoded automatically.

```java
testServerUrl()
    .path("/api/articles")
    .addQueryParameter("page", "1")
    .addQueryParameter("pageSize", "20")
    .addQueryParameter("q", "hello world")
// → http://localhost:8080/api/articles?page=1&pageSize=20&q=hello+world
```

---

## Conversion

#### `uri()` → `java.net.URI`

Converts the `Url` to a `java.net.URI`. Called internally when passing to a `Request`.

```java
URI uri = testServerUrl().path("/api/users").uri();
```

#### `toString()` → `String`

Returns the full URL as a string.

```java
String url = testServerUrl().path("/api/users").toString();
// → "http://localhost:8080/api/users"
```

---

## Usage patterns

**Basic path:**
```java
Request.GET().url(testServerUrl().path("/api/health"))
```

**Path with ID:**
```java
Request.GET().url(testServerUrl().path("/api/users/" + userId))
```

**Path with query parameters:**
```java
Request.GET().url(
    testServerUrl()
        .path("/api/search")
        .addQueryParameter("q", "Alice")
        .addQueryParameter("limit", "10"))
```

**Reuse a base URL:**
```java
Url articlesUrl = testServerUrl().path("/api/articles");

Response list = sayAndMakeRequest(Request.GET().url(articlesUrl));
Response create = sayAndMakeRequest(
    Request.POST()
        .url(articlesUrl)
        .contentTypeApplicationJson()
        .payload(newArticle));
```

**Dynamic path with encoding:**
```java
String searchTerm = "café & bakery";  // special characters
Request.GET().url(
    testServerUrl()
        .path("/api/places")
        .addQueryParameter("name", searchTerm))
// → /api/places?name=caf%C3%A9+%26+bakery
```

---

## Notes

- `Url` is immutable — each method returns a new instance
- Path segments are concatenated as strings; no normalization of double slashes
- `addQueryParameter` encodes values but not keys; use simple keys without special characters
