# DTR 80/20 Quick Reference

**One-page cheat sheet** for the most common DTR patterns. Print this or bookmark it.

---

## Core Methods

| Method | Purpose | Example |
|--------|---------|---------|
| `sayNextSection(String)` | Create H1 heading + TOC entry | `sayNextSection("User API")` |
| `say(String)` | Add explanatory paragraph | `say("This creates a new user.")` |
| `sayAndMakeRequest(Request)` | Execute HTTP + document | `Response r = sayAndMakeRequest(Request.GET()...)` |
| `makeRequest(Request)` | Execute HTTP silently | `makeRequest(Request.POST()...)` |
| `sayAndAssertThat(String, T, Matcher)` | Assert + document result | `sayAndAssertThat("Status is 200", status, equalTo(200))` |

---

## HTTP Request Building

### GET
```java
Request.GET()
    .url(testServerUrl().path("/api/users"))
```

### POST with JSON
```java
Request.POST()
    .url(testServerUrl().path("/api/users"))
    .contentTypeApplicationJson()
    .payload(userDto)
```

### POST with Form Data
```java
Request.POST()
    .url(testServerUrl().path("/api/login"))
    .addFormParameter("email", "user@example.com")
    .addFormParameter("password", "secret")
```

### PUT / PATCH / DELETE
```java
Request.PUT().url(...).payload(...)
Request.PATCH().url(...).contentTypeApplicationJson().payload(...)
Request.DELETE().url(...)
```

### With Headers
```java
Request.GET()
    .url(...)
    .addHeader("Authorization", "Bearer TOKEN")
    .addHeader("Accept", "application/json")
```

### With Query Parameters
```java
Request.GET()
    .url(testServerUrl().path("/api/users")
        .addQueryParameter("page", "1")
        .addQueryParameter("sort", "name"))
```

### File Upload
```java
Request.POST()
    .url(testServerUrl().path("/api/upload"))
    .addFileToUpload("file", new File("document.pdf"))
```

---

## Response Parsing

| Method | Returns | Use when |
|--------|---------|----------|
| `.httpStatus()` | `int` | Need HTTP status code |
| `.payloadAsString()` | `String` | Need raw text |
| `.payloadAsPrettyString()` | `String` | Need pretty-printed JSON/XML |
| `.payloadAs(Class)` | `T` | Deserialize single object (auto-detect JSON/XML) |
| `.payloadJsonAs(Class)` | `T` | Deserialize as JSON |
| `.payloadJsonAs(TypeReference)` | `T` | Deserialize generic types (List, Map, etc.) |
| `.payloadXmlAs(Class)` | `T` | Deserialize as XML |
| `.headers()` | `Map<String,String>` | Need response headers |

### Examples
```java
// Single object
UserDto user = response.payloadAs(UserDto.class);

// List
List<UserDto> users = response.payloadJsonAs(
    new TypeReference<List<UserDto>>() {});

// Map
Map<String, Object> data = response.payloadJsonAs(
    new TypeReference<Map<String, Object>>() {});

// Raw text
String rawResponse = response.payloadAsString();

// Status
int status = response.httpStatus();
```

---

## URL Building

```java
Url.host("http://localhost:8080")
    .path("/api/users")
    .addQueryParameter("page", "1")
    .uri()  // Convert to java.net.URI
```

Or use `testServerUrl()` (override in your test class):
```java
@Override
public Url testServerUrl() {
    return Url.host("http://localhost:8080");
}

// Then in tests:
testServerUrl().path("/api/users")
testServerUrl().path("/api/article/" + articleId)
```

---

## Content-Type Shortcuts

| Method | Header Value |
|--------|--------------|
| `.contentTypeApplicationJson()` | `application/json` |
| `.contentTypeApplicationXml()` | `application/xml` |
| `.contentTypeTextPlain()` | `text/plain` |
| `.contentTypeTextHtml()` | `text/html` |

---

## Assertion Matchers

Use with `sayAndAssertThat(String, T, Matcher<T>)` for readable documentation.

### Equality
```java
import static org.hamcrest.CoreMatchers.*;

sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
sayAndAssertThat("Strings match", "hello", equalToIgnoringCase("HELLO"));
```

### Comparison
```java
sayAndAssertThat("Count > 0", count, greaterThan(0));
sayAndAssertThat("Value >= 100", value, greaterThanOrEqualTo(100));
sayAndAssertThat("In range", num, allOf(greaterThan(0), lessThan(100)));
```

### Null/Presence
```java
sayAndAssertThat("ID exists", created.id, notNullValue());
sayAndAssertThat("Error is null", error, nullValue());
sayAndAssertThat("List has items", users, not(empty()));
```

### String Matching
```java
sayAndAssertThat("Contains domain", email, containsString("@"));
sayAndAssertThat("Starts with", path, startsWith("/api"));
sayAndAssertThat("Matches regex", version, matchesRegex("\\d+\\.\\d+"));
```

### Collection Matching
```java
sayAndAssertThat("List size", users, hasSize(3));
sayAndAssertThat("Has item", users, hasItem(alice));
sayAndAssertThat("Any match", numbers, hasItem(greaterThan(10)));
```

### Type Checking
```java
sayAndAssertThat("Is a String", obj, instanceOf(String.class));
```

---

## Session Cookies (Authentication)

Cookies **auto-persist** across requests in the same test method:

```java
@Test
public void testWithCookies() {
    // Login silently (no documentation)
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("email", "user@example.com")
            .addFormParameter("password", "secret"));

    // All subsequent requests include session cookie automatically
    Response authenticated = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(article));

    // Cookies reset between test methods
}
```

---

## Common HTTP Status Codes

| Code | Meaning | Use case |
|------|---------|----------|
| 200 | OK | Successful GET, PUT, PATCH |
| 201 | Created | Successful POST creating a resource |
| 204 | No Content | Successful DELETE |
| 400 | Bad Request | Invalid request data |
| 401 | Unauthorized | Missing/invalid auth |
| 403 | Forbidden | Authenticated but no permission |
| 404 | Not Found | Resource doesn't exist |
| 500 | Server Error | Server-side error |

---

## Test Class Template

```java
import io.github.seanchatmangpt.dtr.doctester.DocTester;
import io.github.seanchatmangpt.dtr.doctester.testbrowser.Request;
import io.github.seanchatmangpt.dtr.doctester.testbrowser.Response;
import io.github.seanchatmangpt.dtr.doctester.testbrowser.Url;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.*;

public class MyApiDocTest extends DTR {

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }

    @Test
    public void testSomething() {
        sayNextSection("Section Title");
        say("Explain what this test does.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/endpoint")));

        sayAndAssertThat("Status is 200", 200, equalTo(response.httpStatus()));
    }
}
```

---

## Run Tests

```bash
# Build and run tests
mvnd clean test

# Run only one test class
mvnd test -Dtest=MyApiDocTest

# View output (generated HTML docs)
open target/site/doctester/index.html
```

---

## Common Patterns

### Silent Setup (No Documentation)
```java
// Login without documenting it
makeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/login"))
        .addFormParameter("email", "test@example.com")
        .addFormParameter("password", "secret"));
```

### Parse and Validate
```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/users")));

List<UserDto> users = response.payloadJsonAs(
    new TypeReference<List<UserDto>>() {});

sayAndAssertThat("Got 3 users", users.size(), equalTo(3));
```

### Handle Errors
```java
Response forbidden = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/protected")));

sayAndAssertThat("Returns 403", forbidden.httpStatus(), equalTo(403));
```

### Verify Created Object
```java
UserDto newUser = new UserDto("alice", "alice@example.com");
Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/users"))
        .contentTypeApplicationJson()
        .payload(newUser));

UserDto created = response.payloadAs(UserDto.class);
sayAndAssertThat("ID assigned", created.id, notNullValue());
sayAndAssertThat("Email matches", created.email, equalTo("alice@example.com"));
```

---

**Print this page or bookmark it as your daily reference!**
