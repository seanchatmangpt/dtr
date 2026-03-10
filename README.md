# DocTester — Living API Documentation

DocTester is a Java testing framework that generates **Bootstrap-styled HTML documentation while running JUnit tests**. It bridges the gap between testing and documentation by making them inseparable — your docs stay in sync because they *are* your tests.

[![Build Status](https://api.travis-ci.org/r10r-org/doctester.svg)](https://travis-ci.org/r10r-org/doctester)

**Current version:** `1.1.12-SNAPSHOT`
**License:** Apache 2.0
**Maven coordinates:** `org.r10r:doctester-core`

---

## What is DocTester?

DocTester solves a fundamental problem: **documentation gets outdated**. You change your API, update your tests, but documentation lags behind—becoming misleading or wrong.

DocTester's solution: write tests **and** documentation **simultaneously**. Every time your tests run, your documentation is regenerated from live test execution. If your tests pass, your docs are correct.

### Key Principles
- Tests **are** documentation — not separate artifacts
- HTML docs generated **during** test execution
- Bootstrap 3 styling out-of-the-box
- Fluent Java API following Hamcrest conventions
- Works with any JUnit 4 test framework (Ninja, Arquillian, servlet containers, embedded servers, etc.)

---

## Comprehensive DocTest Types

DocTester can document and test **all of the following scenarios**. Each type is shown with live examples from the integration test suite:

### 1. **REST/HTTP API Tests** (JSON & XML)

Document REST endpoints with automatic request/response capture:

```java
@Test
public void testJsonApi() {
    sayNextSection("Listing Articles (JSON GET)");
    say("GET /api/users returns all users as JSON.");

    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/users")));

    sayAndAssertThat("Status is 200",
        response.httpStatus(), equalTo(200));
}
```

**Generates:**
- Request panel (method, URL, headers, cookies)
- Response panel (status, headers, formatted JSON payload)
- Assertion result (green/red box)

**See:** `doctester-integration-test/src/test/java/controllers/docs/JsonApiDocTest.java`

---

### 2. **XML API Tests**

Full support for XML serialization/deserialization with pretty-printed output:

```java
@Test
public void testXmlEndpoint() {
    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/articles.xml")));

    ArticlesDto dto = response.payloadAs(ArticlesDto.class);
    sayAndAssertThat("3 articles returned",
        dto.articles.size(), equalTo(3));
}
```

**Features:**
- Automatic XML detection via Content-Type header
- Jackson XmlMapper deserialization
- Pretty-printed XML in response panel
- Mixed JSON/XML format comparison

**See:** `doctester-integration-test/src/test/java/controllers/docs/XmlApiDocTest.java`

---

### 3. **HTTP Method Tests** (GET, POST, PUT, PATCH, DELETE, HEAD)

All standard HTTP methods fully supported:

```java
// GET
Request.GET().url(...)

// POST with payload
Request.POST()
    .url(...)
    .contentTypeApplicationJson()
    .payload(myDto)

// PUT
Request.PUT().url(...)

// PATCH
Request.PATCH().url(...)

// DELETE
Request.DELETE().url(...)

// HEAD
Request.HEAD().url(...)
```

**Documented:** Each method generates its own request panel showing method, URL, headers.

**See:** `doctester-integration-test/src/test/java/controllers/docs/HttpMethodsDocTest.java`

---

### 4. **Query Parameters**

Document endpoints with dynamic query strings:

```java
@Test
public void testSearchWithQueryParams() {
    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl()
                .path("/api/users")
                .addQueryParameter("search", "John")
                .addQueryParameter("role", "admin")));

    sayAndAssertThat("Results include admins", ...);
}
```

**Generates:** URL with query string in request panel.

**See:** `doctester-integration-test/src/test/java/controllers/docs/QueryParametersDocTest.java`

---

### 5. **Form Submissions**

Document form-based endpoints with `application/x-www-form-urlencoded`:

```java
@Test
public void testLoginForm() {
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/login"))
            .addFormParameter("username", "alice@example.com")
            .addFormParameter("password", "secret"));

    sayAndAssertThat("Login succeeds",
        response.httpStatus(), equalTo(200));
}
```

**Features:**
- Multiple form parameters
- Automatic Content-Type header: `application/x-www-form-urlencoded`
- Form parameters visible in request panel

**See:** `doctester-integration-test/src/test/java/controllers/docs/AuthenticationDocTest.java`

---

### 6. **File Uploads** (Multipart/Form-Data)

Document multipart file upload endpoints:

```java
@Test
public void testFileUpload() throws IOException {
    File document = new File("src/test/resources/report.pdf");

    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/documents"))
            .addFormParameter("title", "Q4 Report")
            .addFileToUpload("file", document));

    sayAndAssertThat("Uploaded successfully",
        response.httpStatus(), equalTo(201));
}
```

**Features:**
- Single or multiple file uploads
- Mix files with form parameters
- Automatic `multipart/form-data` boundary detection
- Request panel shows Content-Type with boundary

**See:** `doctester-integration-test/src/test/java/controllers/docs/FileUploadDocTest.java`

---

### 7. **Authentication & Session Cookies**

Document session-based authentication flows:

```java
@Test
public void testAuthenticatedFlow() {
    // Login
    makeRequest(  // silent login, don't document
        Request.POST()
            .url(testServerUrl().path("/login"))
            .addFormParameter("username", "bob@gmail.com")
            .addFormParameter("password", "secret"));

    // Subsequent requests auto-include session cookie
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(newArticle));

    sayAndAssertThat("Authenticated write succeeds",
        response.httpStatus(), equalTo(200));
}
```

**Features:**
- Automatic cookie jar management
- Cookies persist across requests within same @Test method
- `sayAndGetCookies()` to document all cookies
- `sayAndGetCookieWithName(String)` to extract specific cookies
- Per-test-method isolation (no cookie leakage between tests)

**See:** `doctester-integration-test/src/test/java/controllers/docs/AuthenticationDocTest.java`

---

### 8. **Error & Exception Handling**

Document error responses and HTTP status codes:

```java
@Test
public void testErrorResponse() {
    // Test 403 Forbidden
    Response forbidden = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));

    sayAndAssertThat("Unauthenticated write returns 403",
        forbidden.httpStatus(), equalTo(403));

    // Test 404 Not Found
    Response notFound = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/nonexistent")));

    sayAndAssertThat("Unknown endpoint returns 404",
        notFound.httpStatus(), equalTo(404));

    // Test 204 No Content
    Response deleted = sayAndMakeRequest(
        Request.DELETE()
            .url(testServerUrl().path("/api/article/123")));

    sayAndAssertThat("Successful delete returns 204",
        deleted.httpStatus(), equalTo(204));
}
```

**Features:**
- Error responses documented same as successes
- Request and response panels appear regardless of status
- Clear visual documentation of error codes
- Response body (if any) included in panel

**See:** `doctester-integration-test/src/test/java/controllers/docs/ErrorHandlingDocTest.java`

---

### 9. **Payload Serialization (JSON & XML)**

Document DTOs and payloads with automatic serialization:

```java
@Test
public void testJsonPayload() {
    ArticleDto article = new ArticleDto();
    article.title = "Introduction to DocTester";
    article.content = "Framework for living documentation...";

    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));  // Auto-serialized to JSON
}
```

**Deserialization:**
```java
// Auto-detect JSON/XML from Content-Type
ArticleDto single = response.payloadAs(ArticleDto.class);

// Explicit JSON with TypeReference
List<ArticleDto> list = response.payloadJsonAs(
    new TypeReference<List<ArticleDto>>() {});

// Explicit XML
ArticleDto fromXml = response.payloadXmlAs(ArticleDto.class);
```

**Features:**
- Jackson integration (JSON + XML)
- Automatic Content-Type detection
- Pretty-printed payload in response panel
- Full deserialization support

**See:** `doctester-integration-test/src/test/java/controllers/docs/JsonApiDocTest.java`

---

### 10. **Narrative Documentation with Paragraphs**

Describe context and reasoning in natural language:

```java
@Test
public void testArticleWorkflow() {
    sayNextSection("Complete Article Workflow");

    say("Articles can only be modified by their author or by administrators. "
        + "This test demonstrates the typical workflow: "
        + "login, create, verify, and then update.");

    say("First, we authenticate by sending credentials to /login:");

    Response loginResponse = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/login"))
            .addFormParameter("username", "alice@example.com")
            .addFormParameter("password", "secret"));

    say("After login, the server sets an HTTP-only session cookie. "
        + "This cookie is automatically stored and included in subsequent requests.");

    // ... more requests
}
```

**Features:**
- Prose explaining API behavior
- Readable documentation for non-technical users
- Mixes with request/response panels for context

**See:** `doctester-integration-test/src/test/java/controllers/docs/DocumentationNarrativeDocTest.java`

---

### 11. **HTML Content & Tables**

Embed raw HTML for custom documentation:

```java
@Test
public void documentFieldStructure() {
    sayRaw("""
        <table class="table table-bordered table-condensed">
          <thead><tr><th>Field</th><th>Type</th><th>Description</th></tr></thead>
          <tbody>
            <tr><td>id</td>      <td>number</td><td>Auto-assigned identifier</td></tr>
            <tr><td>title</td>   <td>string</td><td>Article headline</td></tr>
            <tr><td>content</td> <td>string</td><td>Body text</td></tr>
            <tr><td>postedAt</td><td>number</td><td>Unix timestamp (ms)</td></tr>
          </tbody>
        </table>
        """);
}
```

**Features:**
- Raw HTML pass-through (`sayRaw()`)
- Bootstrap CSS classes available
- Useful for schemas, field descriptions, matrices
- Code blocks with syntax highlighting

**See:** `doctester-integration-test/src/test/java/controllers/docs/JsonApiDocTest.java`

---

### 12. **Code Examples (with Syntax Highlighting)**

Display code snippets:

```java
sayRaw("""
    <pre><code class="language-java">
    ArticleDto article = new ArticleDto();
    article.title = "Title";

    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));
    </code></pre>
    """);
```

**Features:**
- HTML-escaped code blocks
- Language hints for syntax highlighting
- Embedded in generated documentation

**See:** `doctester-integration-test/src/test/java/controllers/docs/FileUploadDocTest.java`

---

### 13. **Assertion Visualization (Green/Red Boxes)**

Visual assertion results in HTML:

```java
@Test
public void testAssertions() {
    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users")));

    // Green box if assertion passes
    sayAndAssertThat("Status is 200 OK",
        response.httpStatus(), equalTo(200));

    // Message with reason (shows both)
    sayAndAssertThat("User count correct",
        "Expected at least 1 user",
        userList.size(), greaterThan(0));
}
```

**Generates:**
- **Green box** for passing assertions
- **Red box** for failing assertions (test fails)
- Message and reason both displayed
- Hamcrest matchers fully supported

**See:** All DocTest examples use this

---

### 14. **Annotation-Based Documentation**

Declare documentation purely via annotations (no method calls):

```java
@DocSection("Creating Users")
@DocDescription({
    "POST /api/users creates a new user.",
    "The request body must contain 'username' and 'email' fields.",
    "Returns 201 Created with the new user in the response body."
})
@DocNote({
    "Usernames must be unique.",
    "Email validation happens server-side."
})
@DocWarning({
    "Do NOT send passwords in the request body.",
    "Use OAuth 2.0 token exchange instead."
})
@DocCode(value = {
    "UserDto user = new UserDto(\"alice\", \"alice@example.com\");",
    "Response response = sayAndMakeRequest(",
    "    Request.POST()",
    "        .url(testServerUrl().path(\"/api/users\"))",
    "        .payload(user));"
}, language = "java")
@Test
public void testCreateUser() {
    // Test code here - annotations provide structure
}
```

**Features:**
- `@DocSection` — section heading
- `@DocDescription` — narrative paragraphs
- `@DocNote` — info callout boxes (blue)
- `@DocWarning` — warning callout boxes (yellow)
- `@DocCode` — syntax-highlighted code blocks
- All optional and independent
- Annotations processed automatically

**See:** `doctester-integration-test/src/test/java/controllers/docs/Java25DocTest.java`

---

### 15. **Access Control & Role-Based Tests**

Document authorization and permission scenarios:

```java
@Test
public void testRoleBasedAccess() {
    // Login as regular user
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/login"))
            .addFormParameter("username", "alice@example.com")
            .addFormParameter("password", "secret"));

    // Attempt admin-only operation
    Response response = sayAndMakeRequest(
        Request.DELETE()
            .url(testServerUrl().path("/api/users/123")));

    sayAndAssertThat("Non-admin user denied access",
        response.httpStatus(), equalTo(403));

    say("Admin users can perform the same operation:");
    // ... demonstrate with admin credentials
}
```

**Features:**
- Test multiple user roles
- Document permission errors
- Show access control boundaries
- Cross-request authentication state

**See:** `doctester-integration-test/src/test/java/controllers/docs/AccessControlDocTest.java`

---

### 16. **Integration with Web Frameworks**

Out-of-the-box support for framework-specific server setup:

**Ninja Framework:**
```java
public class MyApiDocTest extends NinjaApiDoctester {
    // Server starts automatically
    // testServerUrl() returns embedded Jetty URL
}
```

**Arquillian/JBoss:**
```java
@RunWith(Arquillian.class)
@RunAsClient
public class MyApiDocTest extends DocTester {
    @ArquillianResource
    private URL baseUrl;

    @Override
    public Url testServerUrl() {
        return Url.host(baseUrl.toURI().toString());
    }
}
```

**Custom Servers:**
```java
public class MyServerDocTest extends DocTester {
    @Override
    public Url testServerUrl() {
        return Url.host("http://my-custom-server:8080");
    }
}
```

**See:** `doctester-integration-test/src/test/java/controllers/utils/NinjaApiDoctester.java`

---

### 17. **Request/Response Inspection**

Inspect and validate response components:

```java
@Test
public void testResponseInspection() {
    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/articles")));

    // Status code
    int status = response.httpStatus();

    // Raw body
    byte[] rawBytes = response.payload();

    // As string
    String body = response.payloadAsString();

    // Deserialized
    Article article = response.payloadAs(Article.class);

    // Pretty-printed
    String formatted = response.payloadAsPrettyString();

    // Headers
    Map<String, String> headers = response.headers();
    String contentType = headers.get("Content-Type");
}
```

---

### 18. **Multiple Scenarios & Workflows**

Chain multiple requests to document complete workflows:

```java
@Test
public void testCompleteUserJourney() {
    // Step 1: Create account
    sayNextSection("Step 1: User Registration");
    Response signup = sayAndMakeRequest(...);

    // Step 2: Login
    sayNextSection("Step 2: Authentication");
    Response login = sayAndMakeRequest(...);

    // Step 3: Update profile
    sayNextSection("Step 3: Profile Updates");
    Response update = sayAndMakeRequest(...);

    // Step 4: Query data
    sayNextSection("Step 4: Data Retrieval");
    Response query = sayAndMakeRequest(...);
}
```

**Features:**
- Multiple sections per test
- Logical flow of operations
- Shows before/after states
- Demonstrates actual use cases

**See:** `doctester-integration-test/src/test/java/controllers/docs/AuthenticationDocTest.java`

---

## Generated Output

DocTester generates **beautiful HTML documentation** in `target/site/doctester/`:

```
target/site/doctester/
├── index.html                                    # Landing page with links to all docs
├── com.example.JsonApiDocTest.html              # Per-test-class documentation
├── com.example.XmlApiDocTest.html
├── bootstrap/
│   ├── css/bootstrap.min.css
│   └── js/bootstrap.min.js
├── jquery/jquery-1.9.0.min.js
└── custom_doctester_stylesheet.css              # Optional custom CSS
```

### Example Panels Generated

Each panel shows:

| Type | Content |
|------|---------|
| **Request** | HTTP method, URL, headers, cookies, body (formatted) |
| **Response** | Status code, headers, body (formatted), Content-Type |
| **Assertion** | Green/red box with message and matcher result |
| **Narrative** | Prose paragraphs explaining behavior |
| **Table** | Custom HTML (field documentation, schemas) |
| **Code** | Syntax-highlighted example code |

---

## Quick Start

### 1. Extend DocTester

```java
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
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

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/articles")));

        sayAndAssertThat("Status is 200 OK",
            response.httpStatus(), equalTo(200));
    }

    @AfterClass
    public static void finish() {
        finishDocTest();  // Generate index.html
    }
}
```

### 2. Build & Run Tests

```bash
mvnd clean verify
```

### 3. View Documentation

Open `target/site/doctester/index.html` in a browser.

---

## Core API Reference

### DocTester Methods

| Method | Purpose |
|--------|---------|
| `say(String)` | Render a paragraph |
| `sayNextSection(String)` | Render a section heading (H1) |
| `sayRaw(String)` | Embed raw HTML |
| `sayAndMakeRequest(Request)` | Execute HTTP request and document |
| `makeRequest(Request)` | Execute HTTP request (silent, no doc) |
| `sayAndAssertThat(String, T, Matcher)` | Assert with documentation |
| `sayAndGetCookies()` | Retrieve all cookies and document |
| `sayAndGetCookieWithName(String)` | Get named cookie and document |
| `getCookies()` | Get all cookies (silent) |
| `getCookieWithName(String)` | Get named cookie (silent) |
| `clearCookies()` | Clear cookie jar |
| `testServerUrl()` | Override to set server URL |
| `finishDocTest()` | Generate index.html (call in @AfterClass) |

### Request Builder

```java
Request.GET()           // HEAD, GET, DELETE, POST, PUT, PATCH
    .url(Url)
    .addHeader(String, String)
    .addFormParameter(String, String)
    .contentTypeApplicationJson()
    .contentTypeApplicationXml()
    .payload(Object)
    .addFileToUpload(String, File)
    .followRedirects(boolean)
```

### Response Object

```java
response.httpStatus()           // int
response.payload()              // byte[]
response.payloadAsString()      // String
response.payloadAsPrettyString()// String (formatted JSON/XML)
response.payloadAs(Class)       // T (auto-detect JSON/XML)
response.payloadJsonAs(Class)   // T (force JSON)
response.payloadXmlAs(Class)    // T (force XML)
response.headers()              // Map<String, String>
```

### Annotations

```java
@DocSection("Title")                        // Section heading
@DocDescription({"Line 1", "Line 2"})      // Narrative paragraphs
@DocNote({"Info 1", "Info 2"})             // Info callout boxes
@DocWarning({"Warning 1", "Warning 2"})    // Warning callout boxes
@DocCode(value = {"Code"}, language="java") // Code blocks
```

---

## Advanced Features

### Custom Test Browser

```java
public class MyDocTest extends DocTester {
    @Override
    public TestBrowser getTestBrowser() {
        return new MyCustomTestBrowser();
    }
}
```

### Custom Render Machine

```java
public class MyDocTest extends DocTester {
    @Override
    public RenderMachine getRenderMachine() {
        return new MyCustomRenderMachine();
    }
}
```

### Custom CSS Styling

Place a file in `src/test/resources/custom_doctester_stylesheet.css`:

```css
body {
    background-color: #f5f5f5;
}

a.navbar-brand {
    color: #F39300 !important;
}

.panel-heading {
    background-color: #004e89;
}
```

---

## Architecture

DocTester's architecture consists of three layers:

**Request Layer** (`testbrowser/`)
- `Request` — fluent HTTP request builder
- `Url` — fluent URL builder
- `HttpConstants` — canonical headers and content-types

**Execution Layer** (`TestBrowserImpl`)
- Wraps Apache HttpComponents 4.5
- Manages cookie jar
- Handles multipart uploads
- Serializes/deserializes payloads (Jackson)

**Documentation Layer** (`rendermachine/`)
- `RenderMachine` — captures all `say*` calls
- `RenderMachineImpl` — generates Bootstrap HTML
- Generates index page linking all tests

---

## Configuration

### System Requirements
- Java 25 (or 21+)
- Maven 4+
- JUnit 4.12+

### Dependencies

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

<!-- HTTP Client (included) -->
<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5</version>
</dependency>

<!-- JSON/XML (included) -->
<dependency>
    <groupId>com.fasterxml.jackson.core</groupId>
    <artifactId>jackson-core</artifactId>
    <version>2.5.4</version>
</dependency>
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| No HTML generated | Call `finishDocTest()` in `@AfterClass` |
| Wrong URL | Override `testServerUrl()` in your test class |
| Cookies not persisting | Ensure subsequent requests are in same @Test method |
| Content-Type detection fails | Explicitly use `payloadJsonAs()` or `payloadXmlAs()` |
| File upload fails | File must exist; don't call `contentTypeApplicationJson()` |
| Assertions show red | Test assertion failed; fix assertion or code |

---

## Examples

**Complete examples** available in the integration test module:

- `JsonApiDocTest` — JSON endpoints, deserialization, assertions
- `XmlApiDocTest` — XML support, format comparison
- `FileUploadDocTest` — multipart file uploads
- `AuthenticationDocTest` — session cookies, login flows
- `ErrorHandlingDocTest` — HTTP error codes, status validation
- `AccessControlDocTest` — role-based access, permissions
- `Java25DocTest` — Java 25 features, annotations, records

Run integration tests:
```bash
mvnd clean verify -pl doctester-integration-test
```

View generated docs:
```bash
open doctester-integration-test/target/site/doctester/index.html
```

---

## Contributing

Contributions are welcome! Please:

1. Write tests for your feature
2. Document code with Javadoc
3. Follow Sun Java code style (4 spaces, UTF-8)
4. Update this README
5. Add your name to `TEAM.md`

Then submit a pull request.

---

## License

DocTester is licensed under the **Apache License 2.0**. See `LICENSE.md` for details.

---

## Support

For issues, questions, or feature requests, visit:
- **GitHub Issues:** https://github.com/r10r-org/doctester/issues
- **GitHub Discussions:** https://github.com/r10r-org/doctester/discussions
