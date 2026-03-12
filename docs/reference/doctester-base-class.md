# Reference: DTR Base Class

**Package:** `org.r10r.doctester`
**File:** `dtr-core/src/main/java/org/r10r/doctester/DTR.java`

`DocTester` is the abstract JUnit base class your test classes extend. It orchestrates both HTTP execution (via `TestBrowser`) and HTML documentation generation (via `RenderMachine`).

---

## Extending DocTester

```java
public class MyApiDocTest extends DTR {

    @Test
    public void testSomething() {
        // use say*, makeRequest, etc.
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

---

## Methods

### Documentation output

#### `say(String text)`

Renders a paragraph `<p>` element in the HTML output.

```java
say("This endpoint returns a paginated list of all users.");
```

**Parameters:**
- `text` — Plain text content (HTML-escaped before rendering)

---

#### `sayNextSection(String title)`

Renders an `<h1>` heading and adds an entry to the HTML sidebar navigation. Use this to organize the documentation page into logical sections.

```java
sayNextSection("User Management");
```

**Parameters:**
- `title` — Section heading text (also used as anchor ID for navigation)

---

#### `sayRaw(String html)`

Inserts a raw HTML string directly into the documentation without escaping. Use for custom tables, alerts, or embedded markup.

```java
sayRaw("<div class='alert alert-warning'>Rate limit: 100 req/min</div>");
```

**Parameters:**
- `html` — Raw HTML string injected verbatim

---

#### `sayAndAssertThat(String message, T actual, Matcher<T> matcher)`

Runs a Hamcrest assertion and renders the result as a colored box in the HTML output:
- **Green box** (alert-success) on pass
- **Red box** (alert-danger) with stack trace on failure

```java
sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
```

**Parameters:**
- `message` — Description shown in the HTML box
- `actual` — The value being tested
- `matcher` — Any Hamcrest `Matcher<T>`

**Throws:** `AssertionError` on failure (fails the JUnit test)

---

#### `sayAndAssertThat(String message, String reason, T actual, Matcher<T> matcher)`

Overload with an additional `reason` parameter for extra context when the assertion fails.

```java
sayAndAssertThat(
    "Articles list is not empty",
    "Server must return at least the seeded articles",
    articles.size(),
    greaterThan(0));
```

---

### HTTP requests

#### `makeRequest(Request request)` → `Response`

Executes an HTTP request via the `TestBrowser`. Does **not** generate any HTML output. Use for test setup (login, seeding data) that you don't want to document.

```java
// Silent login — not documented
makeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/login"))
        .addFormParameter("username", "admin")
        .addFormParameter("password", "secret"));
```

**Returns:** `Response` with status, headers, and body

---

#### `sayAndMakeRequest(Request request)` → `Response`

Executes an HTTP request and renders the full request/response cycle as a Bootstrap panel in the HTML output. The panel shows:
- HTTP method and URL
- Request headers
- Request payload (pretty-printed JSON/XML)
- Response status code
- Response headers
- Response body (pretty-printed JSON/XML)

```java
Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/users")));
```

**Returns:** `Response` with status, headers, and body

---

### Cookie management

#### `getCookies()` → `List<Cookie>`

Returns all cookies currently in the cookie jar. Does not generate HTML output.

```java
List<Cookie> cookies = getCookies();
```

---

#### `sayAndGetCookies()` → `List<Cookie>`

Returns all cookies and renders them as a table in the HTML output.

```java
List<Cookie> cookies = sayAndGetCookies();
```

---

#### `getCookieWithName(String name)` → `Cookie`

Returns the cookie with the given name, or `null` if not found.

```java
Cookie session = getCookieWithName("SESSION");
```

---

#### `sayAndGetCookieWithName(String name)` → `Cookie`

Returns the named cookie and renders it in the HTML output.

```java
Cookie session = sayAndGetCookieWithName("SESSION");
sayAndAssertThat("Session cookie exists", session, notNullValue());
```

---

#### `clearCookies()`

Clears all cookies from the cookie jar. Useful when switching user contexts within a single test.

```java
clearCookies();
```

---

### Configuration (override in subclasses)

#### `testServerUrl()` → `Url`

Override to provide the base URL for the server under test. This URL is used as the starting point for all `Url` chaining in the test.

```java
@Override
public Url testServerUrl() {
    return Url.host("http://localhost:8080");
}
```

**Default:** Returns `null` — you must override this method.

---

#### `getTestBrowser()` → `TestBrowser`

Override to supply a custom `TestBrowser` implementation.

```java
@Override
public TestBrowser getTestBrowser() {
    return new MyCustomTestBrowser();
}
```

**Default:** Returns `new TestBrowserImpl()`

---

#### `getRenderMachine()` → `RenderMachine`

Override to supply a custom `RenderMachine` implementation.

```java
@Override
public RenderMachine getRenderMachine() {
    return new MyCustomRenderMachine();
}
```

**Default:** Returns `new RenderMachineImpl()`

---

#### `setClassNameForDocTesterOutputFile(String name)`

Sets an alternative filename for the HTML output file (without `.html` extension). Call this in a `@Before` method.

```java
@Before
public void configureOutput() {
    setClassNameForDocTesterOutputFile("user-api-reference");
}
```

Produces: `target/site/doctester/user-api-reference.html`

**Default:** Uses the fully-qualified test class name, e.g., `com.example.UserApiDocTest`

---

## Lifecycle

DTR uses JUnit 4 lifecycle hooks internally:

| Hook | What happens |
|---|---|
| `@Before setupForTestCaseMethod()` | Creates a fresh `TestBrowser` for the test method |
| `@AfterClass finishDocTest()` | Writes HTML output to `target/site/doctester/` |

The `RenderMachine` is shared across all test methods in a class; the `TestBrowser` (and its cookie jar) is reset per test method.

---

## Thread safety

DTR is **not thread-safe**. Do not run tests from the same class in parallel. Maven Surefire's default (one thread per class) is safe.
