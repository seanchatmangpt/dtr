# DocTester — Java 25 Testing & Living Documentation

DocTester is a **Java 25+ testing framework** that generates Bootstrap-styled HTML documentation while running JUnit tests. It's designed from the ground up to showcase modern Java language features—records, sealed classes, virtual threads, pattern matching, and more—while automatically documenting your APIs and behavior.

**Key insight:** Your tests are your documentation. Every time your tests run, your docs regenerate from live execution.

[![Build Status](https://api.travis-ci.org/r10r-org/doctester.svg)](https://travis-ci.org/r10r-org/doctester)

**Current version:** `1.1.12-SNAPSHOT`
**License:** Apache 2.0
**Maven coordinates:** `org.r10r:doctester-core`
**Java requirement:** Java 25 LTS with `--enable-preview`

---

## Why Java 25 First?

DocTester is written for **Java 25 developers**. Modern Java idioms make tests clearer:

- **Records** eliminate boilerplate for request/response DTOs
- **Sealed classes** + **pattern matching** express type hierarchies precisely
- **Virtual threads** enable chaos testing and concurrent scenarios without OS threads
- **Text blocks** make multi-line strings (HTML, JSON) readable
- **Switch expressions** are exhaustive—the compiler ensures all cases are covered
- **SequencedCollections** and **Optional** reduce null-pointer bugs

This README shows you how to **leverage these features in your DocTests**.

---

## Java 25 Features in DocTester

### 1. Virtual Threads — Chaos Testing Without Thread Pools

**Problem:** Testing a service under concurrent load typically requires OS thread pools, which exhaust quickly.

**Solution:** Java 25 virtual threads are lightweight; spawn thousands without resource exhaustion.

```java
@Test
@DisplayName("Concurrent requests: 20 virtual threads, chaos injection")
void concurrentChaosWithVirtualThreads() throws InterruptedException {
    int numThreads = 20;
    AtomicInteger successes = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(numThreads);

    // Java 25: single Executors call, no OS thread limits
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < numThreads; i++) {
            final int idx = i;
            executor.submit(() -> {
                try {
                    // Each virtual thread has its own TestBrowserImpl
                    var browser = new TestBrowserImpl();
                    if (idx % 2 == 0) {
                        Response r = browser.makeRequest(
                            Request.GET().url(testServerUrl().path("/ok/" + idx)));
                        if (r.httpStatus == 200) successes.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
    }

    latch.await(10, TimeUnit.SECONDS);

    sayAndAssertThat("All 10 concurrent requests succeeded",
        successes.get(), equalTo(10));
}
```

**Documentation generated:**
- Test name and display name
- Assertion result (green/red box)
- Total execution time
- Virtual thread behavior details

**See:** `DocTesterChaosTest.java` (concurrentChaosWithVirtualThreads test)

---

### 2. Records — Immutable DTOs, Zero Boilerplate

**Problem:** Request/response payloads require getters, setters, constructors, equals, hashCode, toString.

**Solution:** Records are one-liners.

```java
// Before (verbose, mutable, error-prone)
public class ArticleDto {
    private String title;
    private String content;
    public ArticleDto() {}
    public ArticleDto(String title, String content) { ... }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    // ... more boilerplate
}

// After (Java 25 record — immutable, canonical)
record ArticleRecord(String title, String content) {}
```

**In a DocTest:**

```java
@Test
public void testRecordPayloads() {
    sayNextSection("Records as Request Payload Types");

    say("Java 25 records work directly with Jackson 2.12+ serialization:");

    // Create a record payload
    var payload = new ArticleRecord("My Title", "My Content");

    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(payload));  // Jackson serializes the record

    sayAndAssertThat("Article created with record payload",
        response.httpStatus(), equalTo(201));
}
```

**Benefits:**
- No null-pointer bugs (immutable, final fields)
- Compact code (100 lines → 1 line)
- Documentation is in the record signature

**See:** `Java25DocTest.java` (testRecordPayloads test)

---

### 3. Sealed Interfaces & Exhaustive Pattern Matching

**Problem:** API results can succeed or fail, but you might forget to handle a case, or add a new case and forget all the switch statements.

**Solution:** Sealed hierarchies + pattern matching force compiler exhaustiveness checking.

```java
// Define a sealed result type — only Ok and Err are allowed
sealed interface ApiResult<T> permits ApiResult.Ok, ApiResult.Err {
    record Ok<T>(int status, T body) implements ApiResult<T> {}
    record Err<T>(int status, String reason) implements ApiResult<T> {}
}
```

**In a DocTest:**

```java
@Test
public void testSealedInterfacesAndSwitchExpressions() {
    sayNextSection("Sealed Interfaces and Exhaustive Switch Expressions");

    Response raw = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    // Map to sealed type
    ApiResult<ArticlesDto> result = raw.httpStatus == 200
        ? new ApiResult.Ok<>(raw.httpStatus, raw.payloadAs(ArticlesDto.class))
        : new ApiResult.Err<>(raw.httpStatus, raw.payloadAsString());

    // Exhaustive switch: compiler ensures all cases are covered
    String summary = switch (result) {
        case ApiResult.Ok<ArticlesDto> ok ->
            "Fetched %d articles (HTTP %d)".formatted(ok.status(), ok.body().articles.size());
        case ApiResult.Err<ArticlesDto> err ->
            "Failed: HTTP %d: %s".formatted(err.status(), err.reason());
        // No 'default' needed — compiler verifies completeness
    };

    say("Result: " + summary);

    sayAndAssertThat("Result matches expected",
        summary, equalTo("Fetched 3 articles (HTTP 200)"));
}
```

**Benefits:**
- Compiler catches missing cases at compile time
- New result types force updates to all switch statements
- Clear, readable, exhaustive logic

**See:** `Java25DocTest.java` (testSealedInterfacesAndSwitchExpressions test)

---

### 4. Text Blocks — Readable Multi-Line Strings

**Problem:** Building HTML, JSON, or SQL with string concatenation is hard to read and maintain.

**Solution:** Text blocks (""") preserve formatting and eliminate escape sequences.

```java
// Before: concatenation noise
say("The following endpoints are available: "
    + "GET /api/users (list), "
    + "POST /api/users (create), "
    + "DELETE /api/users/{id} (delete)");

// After: readable text block
sayRaw("""
    <div class="alert alert-info">
      <strong>API Endpoints</strong>
      <ul>
        <li><code>GET /api/users</code> — list (public)</li>
        <li><code>POST /api/users</code> — create (authenticated)</li>
        <li><code>DELETE /api/users/{id}</code> — delete (admin)</li>
      </ul>
    </div>
    """);
```

**In a DocTest:**

```java
@Test
public void testTextBlocksForNarrative() {
    sayNextSection("Text Blocks for Cleaner HTML Narrative");

    say("Java text blocks (\"\"\"...\"\"\") eliminate string concatenation noise:");

    sayRaw("""
        <table class="table table-striped">
          <thead>
            <tr><th>Method</th><th>Endpoint</th><th>Auth</th></tr>
          </thead>
          <tbody>
            <tr><td>GET</td><td>/api/articles</td><td>No</td></tr>
            <tr><td>POST</td><td>/api/articles</td><td>Yes</td></tr>
            <tr><td>DELETE</td><td>/api/articles/{id}</td><td>Admin</td></tr>
          </tbody>
        </table>
        """);

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    sayAndAssertThat("Endpoint live", response.httpStatus(), equalTo(200));
}
```

**Benefits:**
- HTML, JSON, SQL are readable as-is
- No escape sequences (`\"` becomes just `"`)
- Formatting is preserved

**See:** `Java25DocTest.java` (testTextBlocksForNarrative test)

---

### 5. Pattern Matching — No More Explicit Casts

**Problem:** Extract data from a response, check types, and cast — verbose and error-prone.

**Solution:** Pattern matching combines type check and extraction in one expression.

```java
// Before: explicit cast
if (response instanceof HttpResponse) {
    HttpResponse http = (HttpResponse) response;
    if (http.getStatus() == 200) {
        String body = http.getBody();
        // use body
    }
}

// After: pattern matching (single expression, no redundant variable)
if (response instanceof HttpResponse http && http.getStatus() == 200) {
    String body = http.getBody();
    // use body
}
```

**Record pattern deconstruction:**

```java
record ArticleView(long id, String title) {}

// Deconstruct a record in a switch
ArticleView view = new ArticleView(1L, "Long Article Title Here");

String label = switch (view) {
    case ArticleView(var id, var title) when title.length() > 20 ->
        "long-title #" + id;
    case ArticleView(var id, var _) ->  // _ = unnamed pattern, ignore title
        "short-title #" + id;
};
```

**In a DocTest:**

```java
@Test
public void testPatternMatchingAndRecordPatterns() {
    sayNextSection("Pattern Matching and Record Patterns");

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    sayAndAssertThat("200 OK", response.httpStatus(), equalTo(200));

    // Pattern matching: no explicit cast
    Object rawPayload = response.payloadAsString();
    if (rawPayload instanceof String body && !body.isBlank()) {
        sayAndAssertThat("Payload is non-blank", true, equalTo(true));
    }

    ArticlesDto dto = response.payloadAs(ArticlesDto.class);

    // Map to ArticleView records
    List<ArticleView> views = dto.articles.stream()
        .map(a -> new ArticleView(a.id, a.title))
        .toList();

    // Record pattern deconstruction with guards
    var summary = new StringBuilder();
    for (ArticleView view : views) {
        String label = switch (view) {
            case ArticleView(var id, var title) when title.length() > 20 ->
                "long-title #" + id;
            case ArticleView(var id, var _) ->
                "short-title #" + id;
        };
        summary.append(label).append("; ");
    }

    sayAndAssertThat("All articles processed",
        views.size(), equalTo(3));
}
```

**Benefits:**
- No redundant variable declarations
- Guards (when clauses) enable conditional logic
- Compiler ensures all patterns are covered

**See:** `Java25DocTest.java` (testPatternMatchingAndRecordPatterns test)

---

### 6. SequencedCollections — Intent-Clear API Access

**Problem:** Getting the first/last element requires index math or iteration.

**Solution:** `SequencedCollection` adds `getFirst()` / `getLast()` / `reversed()`.

```java
// Before: unclear intent
List<Article> articles = dto.articles;
Article first = articles.get(0);           // Magic number
Article last = articles.get(articles.size() - 1);  // Verbose

// After: clear intent
SequencedCollection<Article> articles = dto.articles;
Article first = articles.getFirst();       // Clear, safe
Article last = articles.getLast();         // Clear, safe
```

**In a DocTest:**

```java
@Test
public void testSequencedCollections() {
    sayNextSection("SequencedCollection for Ordered Article Access");

    say("SequencedCollection.getFirst() / getLast() express intent clearly:");

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    ArticlesDto dto = response.payloadAs(ArticlesDto.class);
    SequencedCollection<Article> articles = dto.articles;

    Article first = articles.getFirst();
    Article last = articles.getLast();

    sayAndAssertThat("First article has ID", first.id, notNullValue());
    sayAndAssertThat("Last article has ID", last.id, notNullValue());
    sayAndAssertThat("3 articles total", articles.size(), equalTo(3));
}
```

**Benefits:**
- No index bounds errors
- Intent is explicit
- Works with any ordered collection (List, Deque, etc.)

**See:** `Java25DocTest.java` (testSequencedCollections test)

---

### 7. Switch Expressions — Functional, Exhaustive, No Default

**Problem:** Switch statements are verbose, require careful `break` statements, don't return values cleanly.

**Solution:** Switch expressions are functional, exhaustive, return values.

```java
// Before: switch statement (verbose, easy to forget break)
String status;
switch (code) {
    case 200:
    case 201:
        status = "Success"; break;
    case 400:
        status = "Bad Request"; break;
    case 500:
        status = "Server Error"; break;
    default:
        status = "Unknown"; break;
}

// After: switch expression (functional, no default for sealed types)
String status = switch (code) {
    case 200, 201 -> "Success";
    case 400 -> "Bad Request";
    case 500 -> "Server Error";
    default -> "Unknown";
};
```

**In a DocTest:**

```java
@Test
public void testSwitchExpressions() {
    sayNextSection("Switch Expressions for HTTP Status Handling");

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    String description = switch (response.httpStatus()) {
        case 200, 201 -> "Success";
        case 400 -> "Invalid request";
        case 403 -> "Forbidden";
        case 404 -> "Not found";
        case 500, 502, 503 -> "Server error";
        default -> "Unknown status";
    };

    sayAndAssertThat("Status description matches",
        description, equalTo("Success"));
}
```

**Benefits:**
- No `break` bugs
- Return values directly
- Compiler ensures exhaustiveness (with sealed types)

---

### 8. Var Type Inference — Less Boilerplate

**Problem:** Local variable declarations repeat the type name (redundant).

**Solution:** `var` infers the type from the right-hand side.

```java
// Before: type name repeated
List<ArticleDto> articles = response.payloadJsonAs(new TypeReference<List<ArticleDto>>(){});
ArticleDto firstArticle = articles.get(0);
String title = firstArticle.title;

// After: var infers types (RHS is obvious)
var articles = response.payloadJsonAs(new TypeReference<List<ArticleDto>>(){});
var firstArticle = articles.get(0);
var title = firstArticle.title;
```

**In a DocTest:**

```java
@Test
public void testModernJavaIdioms() {
    sayNextSection("var, Streams, and Method References");

    say("Combine var type inference, method references, and streams:");

    // var infers types from RHS
    var titles = List.of("First article", "Second article");

    for (var title : titles) {
        var article = new ArticleDto();
        article.title = title;
        article.content = "Content for: " + title;

        var resp = makeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/articles"))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Created: " + title,
            resp.httpStatus(), equalTo(200));
    }

    var listResp = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    var dto = listResp.payloadAs(ArticlesDto.class);
    var returnedTitles = dto.articles.stream()
        .map(a -> a.title)
        .toList();

    for (var expected : titles) {
        sayAndAssertThat("Title present: " + expected,
            returnedTitles.contains(expected), equalTo(true));
    }
}
```

**Benefits:**
- Shorter, cleaner code
- Type is obvious from context
- IDE refactoring tools handle type changes

**See:** `Java25DocTest.java` (testModernJavaIdioms test)

---

### 9. Optional — Explicit Null Handling

**Problem:** Nullable fields in API responses cause NullPointerExceptions.

**Solution:** `Optional` forces explicit null checks.

```java
// Before: implicit null risk
String title = article.getTitle();
if (title != null) {
    // use title
}

// After: explicit, forced handling
Optional<String> title = Optional.ofNullable(article.getTitle());
title.ifPresent(t -> { /* use t */ });
String value = title.orElseThrow();  // Fail loudly if missing
```

**In a DocTest:**

```java
@Test
public void testOptionalForNullableFields() {
    sayNextSection("Optional for Nullable Response Fields");

    say("Wrap nullable response fields in Optional to avoid NullPointerExceptions:");

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    ArticlesDto dto = response.payloadAs(ArticlesDto.class);

    Optional<Long> firstId = dto.articles.stream()
        .findFirst()
        .map(a -> a.id);

    sayAndAssertThat("First article has ID",
        firstId.isPresent(), equalTo(true));

    sayAndAssertThat("ID is positive",
        firstId.orElseThrow() > 0, equalTo(true));
}
```

**Benefits:**
- Null checks are explicit and required
- No surprise null pointers in production
- Code is self-documenting

**See:** `Java25DocTest.java` (testOptionalForNullableFields test)

---

## API Testing Features

Beyond Java 25 features, DocTester excels at testing and documenting all aspects of APIs:

### REST/HTTP APIs (JSON & XML)

```java
Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/articles"))
        .contentTypeApplicationJson()
        .payload(articleDto));

sayAndAssertThat("Created with 201",
    response.httpStatus(), equalTo(201));
```

### File Uploads

```java
Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/documents"))
        .addFormParameter("title", "Q4 Report")
        .addFileToUpload("file", new File("report.pdf")));
```

### Authentication & Cookies

```java
makeRequest(  // Silent login
    Request.POST()
        .url(testServerUrl().path("/login"))
        .addFormParameter("username", "alice@example.com")
        .addFormParameter("password", "secret"));

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/articles"))
        .payload(articleDto));  // Cookies auto-included
```

### Error Handling

```java
Response forbidden = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/articles"))
        .payload(articleDto));

sayAndAssertThat("Unauthenticated write returns 403",
    forbidden.httpStatus(), equalTo(403));
```

### Query Parameters

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl()
            .path("/api/articles")
            .addQueryParameter("page", "2")
            .addQueryParameter("limit", "10")));
```

---

## Core API Reference

### DocTester Methods

| Method | Purpose |
|--------|---------|
| `say(String)` | Render paragraph text |
| `sayNextSection(String)` | Render section heading |
| `sayRaw(String)` | Inject raw HTML |
| `sayAndMakeRequest(Request)` | Execute HTTP and document |
| `makeRequest(Request)` | Execute HTTP silently |
| `sayAndAssertThat(String, T, Matcher)` | Assert and document result |

### Annotations for Declarative Documentation

```java
@DocSection("Creating Users")
@DocDescription({"POST /api/users creates a new user."})
@DocNote({"Usernames must be unique."})
@DocWarning({"Don't send passwords in plain text."})
@DocCode(value = {
    "var user = new User(\"alice\", \"alice@example.com\");",
    "Response r = sayAndMakeRequest(Request.POST()...)"
}, language = "java")
@Test
public void testCreateUser() {
    // Test code
}
```

### Request Builder

```java
Request.GET()      // or POST, PUT, PATCH, DELETE, HEAD
    .url(Url)
    .contentTypeApplicationJson()
    .addHeader(String, String)
    .addFormParameter(String, String)
    .addFileToUpload(String, File)
    .payload(Object)
    .followRedirects(boolean)
```

### Response Inspector

```java
response.httpStatus()           // int
response.payload()              // byte[]
response.payloadAsString()      // String
response.payloadAs(Class)       // T (auto-detect JSON/XML)
response.payloadJsonAs(Class)   // T (force JSON)
response.payloadXmlAs(Class)    // T (force XML)
response.headers()              // Map<String, String>
```

---

## Generated Documentation

DocTester generates per-test-class HTML in `target/site/doctester/`:

```
target/site/doctester/
├── index.html                          # Landing page
├── controllers.docs.Java25DocTest.html # Per-test documentation
├── assets/bootstrap/3.0.0/
└── jquery/1.9.0/
```

Each test produces:
- **Request panel** — HTTP method, URL, headers, cookies, body (formatted)
- **Response panel** — Status, headers, body (formatted), Content-Type
- **Assertion panel** — Green (pass) or red (fail) with message
- **Narrative text** — Paragraphs explaining behavior
- **Code blocks** — Syntax-highlighted examples
- **Tables** — Field documentation, schemas

**Example:** Open `java25doctests/controllers.docs.Java25DocTest.html` in a browser to see all of the above.

---

## Quick Start

### 1. Extend DocTester

```java
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Url;
import org.junit.Test;
import org.junit.AfterClass;

import static org.hamcrest.CoreMatchers.*;

public class MyApiDocTest extends DocTester {

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }

    @Test
    public void testGetArticles() {
        sayNextSection("Fetching Articles");
        say("GET /api/articles returns a list of articles.");

        var response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/articles")));

        sayAndAssertThat("Status is 200",
            response.httpStatus(), equalTo(200));
    }

    @AfterClass
    public static void finish() {
        finishDocTest();
    }
}
```

### 2. Run Tests

```bash
mvnd clean verify
```

### 3. View Docs

```bash
open target/site/doctester/index.html
```

---

## Architecture

**Three-layer design:**

1. **Request Layer** (`testbrowser/`) — Fluent HTTP builders
2. **Execution Layer** (`TestBrowserImpl`) — Apache HttpClient, cookie management
3. **Documentation Layer** (`rendermachine/`) — Bootstrap HTML generation

---

## Java 25 Requirements

- **Java 25 LTS** (openjdk)
- **Maven 4.0.0-rc-5+** (or `mvnd` daemon)
- **Compile flag:** `--enable-preview` (baked into maven.config)

### Verification

```bash
java -version         # openjdk version "25.0.2"
mvnd --version        # Maven 4.0.0-rc-5
echo $JAVA_HOME       # /usr/lib/jvm/java-25-openjdk-amd64
```

---

## Running All DocTests

```bash
# Build and test everything
mvnd clean verify

# Run specific DocTest
mvnd test -pl doctester-integration-test -Dtest=Java25DocTest

# View generated docs
open doctester-integration-test/target/site/doctester/index.html
```

---

## Dependencies

```xml
<dependency>
    <groupId>org.r10r</groupId>
    <artifactId>doctester-core</artifactId>
    <version>1.1.12-SNAPSHOT</version>
    <scope>test</scope>
</dependency>

<dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
    <scope>test</scope>
</dependency>
```

---

## Chaos Testing with Virtual Threads

DocTester includes chaos testing framework using WireMock and virtual threads:

```java
@Test
void concurrentChaosWithVirtualThreads() throws InterruptedException {
    int threads = 20;
    AtomicInteger successes = new AtomicInteger(0);
    CountDownLatch latch = new CountDownLatch(threads);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
        for (int i = 0; i < threads; i++) {
            final int idx = i;
            executor.submit(() -> {
                var browser = new TestBrowserImpl();
                Response r = browser.makeRequest(...);
                if (r.httpStatus == 200) successes.incrementAndGet();
                latch.countDown();
            });
        }
    }

    latch.await(10, TimeUnit.SECONDS);
    assertEquals(threads, successes.get());
}
```

**See:** `DocTesterChaosTest.java`

---

## Examples

Complete examples in `doctester-integration-test/src/test/java/controllers/docs/`:

- `Java25DocTest.java` — Records, sealed classes, pattern matching, text blocks, SequencedCollections
- `FileUploadDocTest.java` — Multipart file uploads
- `AuthenticationDocTest.java` — Session cookies, login flows
- `ErrorHandlingDocTest.java` — HTTP error codes
- `AccessControlDocTest.java` — Role-based access control
- `JsonApiDocTest.java` — JSON serialization/deserialization
- `XmlApiDocTest.java` — XML support

**Run all examples:**

```bash
mvnd verify -pl doctester-integration-test
```

---

## Contributing

Contributions welcome! Please:

1. Write tests for your feature
2. Document with Javadoc
3. Use Sun Java code style (4 spaces, UTF-8)
4. Update this README
5. Submit a pull request

---

## License

Apache License 2.0. See `LICENSE.md`.

---

## Support

- **Issues:** https://github.com/r10r-org/doctester/issues
- **Discussions:** https://github.com/r10r-org/doctester/discussions
