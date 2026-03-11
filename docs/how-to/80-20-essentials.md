# DocTester 80/20 Essentials: The Minimal Path to Productivity

**Goal:** Master 20% of DocTester to be productive with 80% of real-world API testing scenarios in ~45 minutes.

---

## The Core Trinity: Three Methods That Do Most of the Work

Every DocTester test revolves around **three methods**. Master these and you can write working tests immediately.

### 1. Document a Section: `sayNextSection(String title)`

```java
sayNextSection("User Management API");
```

**What it does:**
- Creates an H1 heading in the documentation
- Adds an entry to the table of contents
- Visually separates test sections

**Use this:** Once per logical API section in your test.

### 2. Add Explanatory Text: `say(String text)`

```java
say("GET /api/users retrieves all active users. " +
    "The response is paginated with 20 users per page.");
```

**What it does:**
- Renders a paragraph in the documentation
- Explains to API consumers what your test is demonstrating
- Lives in your test code — stays in sync with API changes

**Use this:** Before and after requests to explain intent and results.

### 3. Make a Request & Document It: `sayAndMakeRequest(Request)`

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/users")));
```

**What it does:**
- Executes an HTTP request
- Captures request and response
- Renders both in documentation with syntax highlighting
- Returns the `Response` object for assertions

**Use this:** For every HTTP request you want documented.

---

## The Request/Response Patterns: Four Patterns Cover 80% of Use Cases

### Pattern 1: Simple GET Request

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/users")));

sayAndAssertThat("Status is 200 OK",
    response.httpStatus(), equalTo(200));
```

**When:** Fetching data from a GET endpoint.

### Pattern 2: POST with JSON Payload

```java
UserDto newUser = new UserDto("alice", "alice@example.com");

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/users"))
        .contentTypeApplicationJson()
        .payload(newUser));

sayAndAssertThat("User created with 201",
    response.httpStatus(), equalTo(201));

UserDto created = response.payloadAs(UserDto.class);
sayAndAssertThat("Created user has ID",
    created.id, notNullValue());
```

**When:** Creating or updating resources via JSON.

**Key methods:**
- `.contentTypeApplicationJson()` — Sets the Content-Type header
- `.payload(object)` — Serializes your DTO to JSON via Jackson

### Pattern 3: Parse JSON Response

```java
List<UserDto> users = response.payloadJsonAs(new TypeReference<List<UserDto>>() {});

sayAndAssertThat("At least 3 users returned",
    users.size(), greaterThanOrEqualTo(3));
```

**When:** Deserializing complex responses (lists, maps).

**Key methods:**
- `.payloadAs(MyDto.class)` — Auto-detect JSON/XML → deserialize
- `.payloadJsonAs(Class)` — Force JSON deserialization
- `.payloadAsString()` — Get raw text

### Pattern 4: Form-Based Authentication + Session Management

```java
// Silent login — no documentation
makeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/login"))
        .addFormParameter("username", "bob@example.com")
        .addFormParameter("password", "secret123"));

// Subsequent requests automatically include session cookie
Response authenticated = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/articles"))
        .contentTypeApplicationJson()
        .payload(article));

sayAndAssertThat("Authenticated POST succeeds",
    authenticated.httpStatus(), equalTo(201));
```

**Key methods:**
- `.makeRequest(Request)` — Silent execution (no documentation)
- `.addFormParameter(key, value)` — Add form fields
- Cookies auto-persist across requests in the same test method

---

## Five Common Workflows: Copy-Paste-Ready Examples

### Workflow 1: GET and Assert Status

```java
@Test
public void testGetArticles() {
    sayNextSection("List Articles");
    say("GET /articles returns a paginated list.");

    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/articles")));

    sayAndAssertThat("Status is 200 OK",
        response.httpStatus(), equalTo(200));
}
```

### Workflow 2: POST, Parse, and Verify

```java
@Test
public void testCreateArticle() {
    sayNextSection("Create Article");
    say("POST creates a new article and returns the created object.");

    ArticleDto article = new ArticleDto("Java 25 Features", "...");
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));

    sayAndAssertThat("Status is 201 Created",
        response.httpStatus(), equalTo(201));

    ArticleDto created = response.payloadAs(ArticleDto.class);
    sayAndAssertThat("Created article has ID",
        created.id, notNullValue());
    sayAndAssertThat("Title matches",
        created.title, equalTo("Java 25 Features"));
}
```

### Workflow 3: Login + Authenticated Request

```java
@Test
public void testAuthenticatedPost() {
    sayNextSection("Authentication");
    say("First we log in with form credentials.");

    // Silent login
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("email", "alice@example.com")
            .addFormParameter("password", "secret"));

    say("Now we make an authenticated request.");
    ArticleDto article = new ArticleDto("Sealed Classes", "...");
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));

    sayAndAssertThat("Authenticated POST succeeds",
        response.httpStatus(), equalTo(201));
}
```

### Workflow 4: Handle Errors Explicitly

```java
@Test
public void testUnauthorizedRequest() {
    sayNextSection("Error Handling");
    say("Requests without authentication return 403 Forbidden.");

    Response forbidden = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(new ArticleDto("Title", "...")));

    sayAndAssertThat("POST without auth returns 403",
        forbidden.httpStatus(), equalTo(403));
}
```

### Workflow 5: File Upload

```java
@Test
public void testFileUpload() {
    sayNextSection("File Upload");
    say("POST /api/upload accepts multipart file uploads.");

    File file = new File("test-file.pdf");
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/upload"))
            .addFileToUpload("file", file));

    sayAndAssertThat("Upload succeeds",
        response.httpStatus(), equalTo(200));
}
```

---

## Complete Annotated Example

Here's a minimal, working DocTest with explanations:

```java
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.Url;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.*;

public class UserApiDocTest extends DocTester {

    // ✅ Implement this — return your test server's base URL
    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }

    @Test
    public void testUserApi() {
        // ✅ START: Create a section heading
        sayNextSection("User Management API");

        // ✅ Add context text — this appears above the request
        say("The User API supports CRUD operations on user resources.");

        // ✅ Create a user via POST
        UserDto newUser = new UserDto("alice", "alice@example.com");
        Response createResponse = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(newUser));

        // ✅ Assert and document the result
        sayAndAssertThat("User created with 201 status",
            createResponse.httpStatus(), equalTo(201));

        // ✅ Parse the response
        UserDto created = createResponse.payloadAs(UserDto.class);

        // ✅ Verify the created object
        sayAndAssertThat("Created user has an ID",
            created.id, notNullValue());
        sayAndAssertThat("Name matches",
            created.name, equalTo("alice"));

        // ✅ Fetch all users
        say("Now we fetch all users.");
        Response listResponse = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/users")));

        sayAndAssertThat("List endpoint returns 200",
            listResponse.httpStatus(), equalTo(200));

        // ✅ Verify list contents
        List<UserDto> users = listResponse
            .payloadJsonAs(new TypeReference<List<UserDto>>() {});
        sayAndAssertThat("At least 1 user in list",
            users.size(), greaterThanOrEqualTo(1));
    }
}
```

**What this test produces:**
1. An HTML file at `target/site/doctester/UserApiDocTest.html`
2. Section heading: "User Management API"
3. Explanatory paragraphs before each request
4. Formatted HTTP request and response panels
5. Green/red assertion boxes showing pass/fail status
6. An index page linking all generated docs

---

## Quick Reference Table

| Need | Method | Example |
|------|--------|---------|
| Create section heading | `sayNextSection(String)` | `sayNextSection("Users")` |
| Add text | `say(String)` | `say("This endpoint creates a user.")` |
| Make request + document | `sayAndMakeRequest(Request)` | See workflows above |
| Make request silently | `makeRequest(Request)` | `makeRequest(Request.POST()...)` |
| Assert with documentation | `sayAndAssertThat(String, T, Matcher)` | `sayAndAssertThat("Status is 200", 200, equalTo(status))` |
| GET request | `Request.GET()` | `.url(baseUrl).path("/api/users")` |
| POST with JSON | `Request.POST().contentTypeApplicationJson()` | `.payload(dto)` |
| POST with form | `Request.POST().addFormParameter()` | `.addFormParameter("name", "value")` |
| Parse JSON | `.payloadJsonAs(Type)` | `.payloadJsonAs(new TypeReference<List<User>>() {})` |
| Parse auto-detect | `.payloadAs(Class)` | `.payloadAs(UserDto.class)` |
| Get raw text | `.payloadAsString()` | Return string for custom parsing |
| Build URL | `Url.host("...").path(...)` | `Url.host("http://localhost:8080").path("/api/users")` |
| Add query param | `.addQueryParameter(key, value)` | `.addQueryParameter("page", "1")` |
| Add header | `.addHeader(key, value)` | `.addHeader("Authorization", "Bearer token")` |
| File upload | `.addFileToUpload(paramName, file)` | `.addFileToUpload("avatar", new File("avatar.png"))` |
| Session cookies | Auto-managed | Persists across requests in same test method |

---

## Common Assertion Matchers

Use these with `sayAndAssertThat()` for readable documentation:

```java
import static org.hamcrest.CoreMatchers.*;

// Equality and comparison
sayAndAssertThat("Status matches", 200, equalTo(response.httpStatus()));
sayAndAssertThat("Count is positive", count, greaterThan(0));
sayAndAssertThat("Value is in range", value, allOf(greaterThan(0), lessThan(100)));

// Null/present
sayAndAssertThat("ID is present", created.id, notNullValue());
sayAndAssertThat("Error message is null", error, nullValue());

// String matching
sayAndAssertThat("Email contains domain", email, containsString("@example.com"));

// Collections
sayAndAssertThat("List is not empty", users, not(empty()));
sayAndAssertThat("List has 3 items", users, hasSize(3));

// Type checking
sayAndAssertThat("Is String", value, instanceOf(String.class));
```

---

## Next Steps

**Now that you know the Core Trinity:**
- Read [Testing a REST API](../tutorials/testing-a-rest-api.md) for a full working example
- Check [Quick Reference](80-20-quick-reference.md) when you need to look something up
- Explore [Design Patterns](../explanation/80-20-design-patterns.md) to understand the philosophy

**To go deeper:**
- [How-to Guides](index.md) for specific tasks (authentication, WebSockets, etc.)
- [API Reference](../reference/index.md) for complete method documentation
- [Explanation](../explanation/index.md) for architectural understanding
