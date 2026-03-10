# Explanation: How DocTester Works

This document describes the lifecycle of a DocTest run — from test startup through HTML output. Understanding this helps you predict what will appear in the documentation and debug unexpected behavior.

---

## The three components

DocTester separates concerns across three collaborating components:

```
DocTester (orchestrator)
├── TestBrowser   (HTTP execution + cookie store)
└── RenderMachine (HTML accumulation + file writing)
```

**`DocTester`** is your test class's superclass. It coordinates between the other two: when you call `sayAndMakeRequest()`, DocTester delegates to `TestBrowser` for HTTP and to `RenderMachine` to generate the panel HTML.

**`TestBrowser`** handles the actual HTTP connection (Apache HttpClient), serializes payloads, and maintains a persistent cookie jar. It is stateful — cookies set by the server in one request are automatically sent in the next.

**`RenderMachine`** accumulates HTML content as the test runs and writes output files at the end. It is also stateful — all the `say*` calls append to an internal buffer.

---

## Per-method vs per-class scope

One important subtlety: `TestBrowser` and `RenderMachine` have different scopes.

| Component | Scope | Consequence |
|---|---|---|
| `TestBrowser` | Per test method | Cookie jar is fresh for each `@Test` |
| `RenderMachine` | Per test class | All `@Test` methods write to the same HTML page |

`DocTester`'s `@Before` hook creates a new `TestBrowserImpl` instance for each test method:

```java
// Simplified from DocTester.java
@Before
public void setupForTestCaseMethod() {
    renderMachine.setTestBrowser(getTestBrowser());  // fresh browser per method
}
```

This means cookies don't leak between test methods. If you authenticate in `test01_login()`, the session cookie is gone by `test02_createArticle()`. You need to authenticate again (silently, via `makeRequest`) in each test method that requires it.

The `RenderMachine` is a class-level static field, so all test methods in a class contribute to a single output page.

---

## The lifecycle step by step

### 1. JUnit starts the test class

JUnit 4 discovers the test class via classpath scanning or explicit configuration. DocTester hooks into JUnit via `@Before` and `@AfterClass` annotations on methods in `DocTester` itself.

### 2. `@Before setupForTestCaseMethod()`

Before each `@Test` method:

1. DocTester calls `getTestBrowser()` to create a fresh browser
2. The browser is injected into the `RenderMachine` so it can execute requests

The `RenderMachine` is initialized lazily on first use (first `say*` call in any test method).

### 3. Test method runs

As your test executes:

- `say(text)` → RenderMachine appends `<p>text</p>` to its buffer
- `sayNextSection(title)` → appends `<h1>` and records the section for the sidebar
- `sayAndMakeRequest(req)` → delegates:
  1. `TestBrowser.makeRequest(req)` → executes HTTP, stores cookies
  2. `RenderMachine` appends a request panel (method, URL, headers, payload) and a response panel (status, headers, body) to its buffer
- `sayAndAssertThat(msg, actual, matcher)` → runs the assertion, then:
  - On pass: appends green alert box to buffer
  - On fail: appends red alert box with stack trace, then throws `AssertionError`

### 4. `@AfterClass finishDocTest()`

After all `@Test` methods in the class complete:

1. `RenderMachine.finishAndWriteOut()` is called
2. RenderMachine assembles the full HTML page:
   - Bootstrap navbar
   - Sidebar with section links
   - Main content from the accumulated buffer
3. HTML is written to `target/site/doctester/{ClassName}.html`
4. Bootstrap/jQuery assets are copied if not already present
5. `index.html` is regenerated with a link to the new page

### 5. Test results

JUnit reports test pass/fail normally. DocTester adds no extra failures — if `sayAndAssertThat` throws `AssertionError`, JUnit catches it and marks the method as failed, same as any other assertion.

---

## What the HTML panel shows

For each `sayAndMakeRequest()` call, the rendered panel shows:

**Request panel (blue):**
- HTTP method (GET, POST, etc.)
- Full URL with query parameters
- All request headers (including `Content-Type`, cookies)
- Request payload (pretty-printed JSON or XML)

**Response panel (grey):**
- HTTP status code with text (e.g., `200 OK`)
- All response headers
- Response body (pretty-printed JSON or XML)

The pretty-printing is done by `PayloadUtils`, which detects JSON or XML from the content type header and re-formats using Jackson.

---

## How cookies flow

Cookie flow matches what a browser would do:

1. Server sets a cookie via `Set-Cookie` header
2. `TestBrowserImpl` stores it in Apache HttpClient's `CookieStore`
3. On the next request to the same domain, the cookie is automatically included in the `Cookie` header
4. The cookie appears in the request panel's headers section in the HTML

When you call `getCookies()` or `getCookieWithName()`, you're querying this `CookieStore`.

---

## Redirect handling

By default `TestBrowserImpl` follows redirects automatically. When a redirect is followed:

- The HTML panel shows the **final** response (after all redirects)
- Intermediate 3xx responses are not shown
- The URL shown is the **original** request URL, not the redirect target

To capture a redirect response, use `Request.followRedirects(false)`.

---

## What `makeRequest` vs `sayAndMakeRequest` actually do

Both call `TestBrowser.makeRequest()`. The only difference:

- `makeRequest` returns the `Response`; nothing is added to the HTML buffer
- `sayAndMakeRequest` calls `makeRequest` internally and then renders the request/response panels to the HTML buffer

Use `makeRequest` for HTTP calls that are test setup (login, data seeding, cleanup) rather than the documented API behavior.

---

## Index.html generation

`RenderMachineImpl` maintains a running list of generated documentation pages. Each time `finishAndWriteOut()` runs:

1. It scans `target/site/doctester/` for existing `*.html` files
2. Regenerates `index.html` with links to all of them

This means `index.html` is updated incrementally — if you run tests from class A and then class B, `index.html` will link to both.
