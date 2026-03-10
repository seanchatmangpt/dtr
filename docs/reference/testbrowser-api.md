# Reference: TestBrowser Interface

**Package:** `org.r10r.doctester.testbrowser`
**File:** `doctester-core/src/main/java/org/r10r/doctester/testbrowser/TestBrowser.java`

`TestBrowser` is the HTTP client interface used by `DocTester`. The default implementation is `TestBrowserImpl` (Apache HttpClient 4). Override `getTestBrowser()` in your `DocTester` subclass to supply a custom implementation.

---

## Interface methods

#### `makeRequest(Request request)` → `Response`

Executes the HTTP request and returns the response.

```java
Response response = testBrowser.makeRequest(
    Request.GET().url(Url.host("http://localhost:8080").path("/api/data")));
```

#### `getCookies()` → `List<Cookie>`

Returns all cookies currently stored in the cookie jar.

#### `getCookieWithName(String name)` → `Cookie`

Returns the cookie with the given name, or `null` if not present.

#### `clearCookies()`

Clears all cookies from the cookie jar.

---

## Default implementation: TestBrowserImpl

**File:** `doctester-core/src/main/java/org/r10r/doctester/testbrowser/TestBrowserImpl.java`

`TestBrowserImpl` wraps Apache HttpClient 4.5 with:

- Persistent cookie store across requests
- Automatic redirect following (configurable per-request via `Request.followRedirects(boolean)`)
- Support for all HTTP methods: GET, HEAD, POST, PUT, PATCH, DELETE
- JSON/XML payload serialization (Jackson)
- Form parameters (`application/x-www-form-urlencoded`)
- Multipart file uploads (`multipart/form-data`)

---

## Custom TestBrowser

Implement `TestBrowser` to replace the HTTP client:

```java
public class OkHttpTestBrowser implements TestBrowser {

    private final OkHttpClient client = new OkHttpClient.Builder()
        .cookieJar(new InMemoryCookieJar())
        .build();

    @Override
    public Response makeRequest(Request request) {
        okhttp3.Request okRequest = buildOkRequest(request);
        try (okhttp3.Response okResponse = client.newCall(okRequest).execute()) {
            return new Response(
                collectHeaders(okResponse),
                okResponse.code(),
                okResponse.body().string());
        } catch (IOException e) {
            throw new RuntimeException("Request failed", e);
        }
    }

    // ... implement getCookies(), getCookieWithName(), clearCookies()
}
```

Inject it by overriding `getTestBrowser()` in your DocTest base class:

```java
public abstract class MyDocTester extends DocTester {

    @Override
    public TestBrowser getTestBrowser() {
        return new OkHttpTestBrowser();
    }
}
```

---

## Cookie type

The `Cookie` type is `org.apache.http.cookie.Cookie` from Apache HttpClient. Key methods:

| Method | Returns | Description |
|---|---|---|
| `getName()` | `String` | Cookie name |
| `getValue()` | `String` | Cookie value |
| `getDomain()` | `String` | Domain scope |
| `getPath()` | `String` | Path scope |
| `getExpiryDate()` | `Date` | Expiry (null = session) |
| `isExpired(Date)` | `boolean` | Check if expired at given time |
| `isSecure()` | `boolean` | HTTPS only flag |

> If you implement a custom `TestBrowser` without Apache HttpClient, you'll need to return `org.apache.http.cookie.Cookie` instances or change the interface contract.
