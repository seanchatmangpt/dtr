# DTR 80/20 Design Patterns — Understanding the Why

This document explains **why** DTR is designed the way it is. Understanding the reasoning will help you use the framework more effectively.

---

## The Core Philosophy: Tests Are Documentation

**The Problem:** Traditional API documentation rots.

Developers update the code, forget to update the docs, and users inherit incorrect information. Documentation lives in wikis, README files, and Google Docs — separate from the code that actually implements the API.

**The Solution:** Make tests and documentation inseparable.

DTR is built on the principle that **your test is your documentation**. When you write a test, you simultaneously create a living document. Every request and response in your test becomes part of the documentation. When the API changes, the documentation changes with it (because the test breaks).

### Example: The Problem It Solves

**Without DocTester:**
```
Code:     POST /api/users returns 201
Docs:     POST /api/users returns 200    ← WRONG! Docs are outdated
```

**With DocTester:**
```
Test:     sayAndAssertThat("Status is 201", response.httpStatus(), equalTo(201))
Docs:     Auto-generated from test
Result:   Code and docs stay in sync
```

---

## Pattern 1: `sayAndMakeRequest()` — The Central Idea

### Why This Method Exists

`sayAndMakeRequest()` is the **heart of DocTester**. It does two things:
1. Executes the HTTP request (like `makeRequest()`)
2. Captures and documents the request/response in HTML/Markdown

### The Design Decision

You might wonder: **Why not just use `makeRequest()` for documentation too?**

The answer is intentional separation of concerns:

```java
// This executes silently — useful for setup
makeRequest(Request.POST().url(...).payload(...));

// This executes AND documents — for what matters
sayAndMakeRequest(Request.GET().url(...));
```

### The Benefit

This separation lets you:

1. **Hide setup/teardown** — Login requests don't clutter documentation
2. **Control narrative flow** — You choose what's important enough to document
3. **Test more than you document** — Setup steps, edge cases, etc.

### Real-world Example

```java
@Test
public void testCreateAndFetch() {
    // Silent setup: login without documentation
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("email", "test@example.com")
            .addFormParameter("password", "secret"));

    // Now document the user flow
    sayNextSection("Create Article");
    say("Authenticated users can create articles.");

    ArticleDto article = new ArticleDto("Title", "Content");
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));
    // ↑ This gets documented with request/response

    say("Now we fetch the created article.");

    Response fetchResponse = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/articles/" + article.id)));
    // ↑ This also gets documented
}
```

**Generated documentation shows only the meaningful requests** (create and fetch), not the authentication dance that precedes them.

---

## Pattern 2: `say()` — Context for Humans

### Why Assertions Aren't Enough

You could write:
```java
Response response = sayAndMakeRequest(Request.GET()...);
sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
```

This documents the **what** (status is 200) but not the **why** (why do we care?).

### The Design Decision

`say()` adds human-readable context:

```java
say("GET /api/users retrieves all active users. " +
    "Results are paginated with up to 20 per page.");

Response response = sayAndMakeRequest(Request.GET()...);

say("Note: The response includes metadata about pagination " +
    "even when no additional pages exist.");

sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
```

### The Benefit

API consumers understand:
- What the endpoint does (before the request)
- What the response means (after the request)
- Edge cases and caveats
- When to use this endpoint vs alternatives

### Who Reads This?

**The documentation is for:**
- New developers joining the team
- API consumers integrating your service
- Your future self, 6 months from now
- Stakeholders reviewing what the API does

---

## Pattern 3: Assertions As Documentation

### Why Hamcrest Matchers?

DTR uses **Hamcrest matchers** instead of plain assertions. Why?

**Without:**
```java
if (response.httpStatus() != 200) {
    throw new AssertionError("Bad status");
}
```

**With Hamcrest:**
```java
sayAndAssertThat("Status is OK", 200, equalTo(response.httpStatus()));
```

### The Design Decision

Hamcrest matchers are **self-documenting**:

```java
sayAndAssertThat("User email is valid",
    created.email,
    containsString("@"));

sayAndAssertThat("Response list has 3+ items",
    users.size(),
    greaterThanOrEqualTo(3));

sayAndAssertThat("ID is assigned",
    created.id,
    notNullValue());
```

Each matcher reads like a sentence. When the test fails, the HTML/Markdown shows exactly what was expected vs. what was received — **in the documentation**.

### The Benefit

When an assertion fails:
- The documentation shows the expectation clearly
- Readers understand what the API contract is
- The failure message is readable (not cryptic)

### Visual Rendering

In the generated documentation:

**On Pass:**
```
✓ Status is OK
```

**On Fail:**
```
✗ Status is OK
  Expected: 200
  Received: 500
  Error: Internal Server Error
```

This teaches API consumers about error handling, edge cases, and recovery strategies — all through your test assertions.

---

## Pattern 4: Automatic Session Management

### The Problem It Solves

Without automatic cookie handling:

```java
@Test
public void testWithAuth() {
    // Login
    Response loginResponse = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("email", "user@example.com")
            .addFormParameter("password", "secret"));

    // Extract cookie from response manually
    List<Cookie> cookies = loginResponse.headers()...
    // Add cookie to next request
    Request.POST()
        .url(...)
        .addHeader("Cookie", "SESSIONID=" + extractedCookie)
        ...

    // Nightmare for every authenticated request!
}
```

### The Design Decision

DocTester's `TestBrowser` maintains a **cookie jar** across requests:

```java
@Test
public void testWithAuth() {
    // Login (silently — setup)
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("email", "user@example.com")
            .addFormParameter("password", "secret"));

    // Cookie is stored automatically

    // Subsequent requests use the cookie automatically
    sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));
    // ↑ Session cookie included automatically
}
```

### The Benefit

1. **Tests are simple** — Describe the user flow, not HTTP details
2. **Documentation is accurate** — Shows realistic workflows
3. **Less boilerplate** — No manual header manipulation

### Important: Isolation

Cookies **reset between test methods**:

```java
@Test
public void test1() {
    // Login
    makeRequest(Request.POST().url(...).addFormParameter(...));
    // Cookie jar now has SESSIONID
}

@Test
public void test2() {
    // Fresh test method = fresh cookie jar
    // SESSIONID from test1 is gone
}
```

This ensures test isolation — each test is independent.

---

## Pattern 5: Three-Layer Architecture

DTR separates concerns into three layers:

### Layer 1: Request/Response (testbrowser/)

```java
Request.GET().url(...).addHeader(...)
Response.payloadAs(MyDto.class)
```

**Purpose:** Simple, fluent HTTP abstractions

**Design:** Independent of documentation — you could use this API standalone

### Layer 2: Browser (TestBrowserImpl)

```java
testBrowser.executeRequest(request) → Response
```

**Purpose:** HTTP client logic (cookies, redirects, serialization)

**Design:** Pluggable interface (`TestBrowser`) — you can provide custom implementations

### Layer 3: Documentation (RenderMachine)

```java
renderMachine.sayNextSection("...")
renderMachine.sayAndMakeRequest(request, response)
```

**Purpose:** Captures and renders documentation

**Design:** Pluggable interface (`RenderMachine`) — you can swap Markdown for HTML or custom formats

### The Benefit

Each layer is independent:
- You can use Request/Response without documentation
- You can swap the browser implementation (mock, custom HTTP client)
- You can swap the render engine (Markdown, HTML, PDF, etc.)

---

## Pattern 6: The Test Lifecycle

### Flow

```
@BeforeEach (automatic)
  └─ Fresh TestBrowser + RenderMachine per test method
  └─ Fresh cookie jar

@Test
  └─ say*() calls → buffered in RenderMachine
  └─ sayAndMakeRequest() → HTTP + capture request/response
  └─ sayAndAssertThat() → assertion + documentation

@AfterEach (automatic via TestWatcher)
  └─ finishAndWriteOut()
  └─ Write target/site/doctester/TestClassName.html

@AfterAll (manual)
  └─ finishDocTest()
  └─ Generate target/site/doctester/index.html
```

### The Design Decision

Each test method gets:
- **Fresh isolation** — No state leaks between tests
- **Independent documentation** — Each test produces one HTML file
- **Clear lifecycle** — Setup → test → cleanup is automatic

---

## Pattern 7: Why `testServerUrl()` Must Be Overridden

### The Design Decision

Every test class must override:

```java
@Override
public Url testServerUrl() {
    return Url.host("http://localhost:8080");
}
```

Why not auto-detect?

1. **Explicit is better than implicit** — Tests should be self-documenting
2. **Different per environment** — Dev: localhost:8080, CI: http://jenkins-agent:9000, etc.
3. **Clear in the test** — Reader immediately sees what server is being tested

### The Benefit

You know exactly what server your tests hit, no guessing.

---

## Summary: The Philosophy

| Feature | Why |
|---------|-----|
| `sayAndMakeRequest()` vs `makeRequest()` | Control what gets documented vs. what's setup noise |
| `say()` for narrative | Humans need context, not just assertions |
| Hamcrest matchers | Self-documenting, readable failure messages |
| Auto-managed cookies | Realistic auth flows without boilerplate |
| Three-layer architecture | Each concern is pluggable/replaceable |
| Fresh state per test method | Isolation and independent documentation |
| Override `testServerUrl()` | Explicit, clear, environment-aware |

**Central idea:** Tests + documentation are one thing. When you write a good test in DocTester, you automatically write good documentation. The two stay in sync because they're the same artifact.
