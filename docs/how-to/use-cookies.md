# How-to: Use Cookies

DocTester maintains a cookie jar across requests within a single test method. Cookies set by the server are automatically sent with subsequent requests — no manual cookie management needed.

## Authenticate and carry the session cookie

```java
@Test
public void testAuthenticatedRequest() {

    // Login — session cookie is set automatically
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("username", "admin")
            .addFormParameter("password", "secret"));

    // All subsequent requests carry the session cookie
    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/admin/users")));

    sayAndAssertThat("Admin endpoint accessible", 200, equalTo(response.httpStatus()));
}
```

> `makeRequest` (without `say`) runs the login silently so it doesn't clutter the documentation.

## Assert that a cookie was set

```java
import org.apache.http.cookie.Cookie;

Response loginResponse = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/login"))
        .addFormParameter("username", "alice")
        .addFormParameter("password", "pass123"));

Cookie sessionCookie = getCookieWithName("SESSION");
sayAndAssertThat("Session cookie present", sessionCookie, notNullValue());
```

## Document the cookies

Use `sayAndGetCookies()` to list all current cookies in the documentation:

```java
sayAndGetCookies();
```

Use `sayAndGetCookieWithName(String)` to show a specific cookie:

```java
Cookie session = sayAndGetCookieWithName("SESSION");
sayAndAssertThat("Cookie is not expired", session.isExpired(new Date()), equalTo(false));
```

## Get cookies programmatically (without documenting)

```java
List<Cookie> allCookies = getCookies();
Cookie session = getCookieWithName("SESSION");
```

## Clear cookies between scenarios

Use `clearCookies()` to reset the cookie jar — useful when testing with different users in the same test class:

```java
@Test
public void testAsAdmin() {
    makeRequest(Request.POST()
        .url(testServerUrl().path("/api/login"))
        .addFormParameter("username", "admin")
        .addFormParameter("password", "adminpass"));

    // ... admin tests ...
}

@Test
public void testAsRegularUser() {
    clearCookies();  // remove admin session

    makeRequest(Request.POST()
        .url(testServerUrl().path("/api/login"))
        .addFormParameter("username", "user")
        .addFormParameter("password", "userpass"));

    // ... user tests ...
}
```

> Note: DocTester creates a fresh `TestBrowser` (and thus a fresh cookie jar) for each test method by default. `clearCookies()` is mainly useful within a single test method.

## Complete authentication flow example

```java
@Test
public void testSecuredEndpoints() {

    sayNextSection("Authentication and Sessions");

    say("Write operations require an authenticated session. "
        + "Authenticate by POSTing credentials to /api/login.");

    Response loginResponse = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("username", "alice")
            .addFormParameter("password", "pass123"));

    sayAndAssertThat("Login accepted", 200, equalTo(loginResponse.httpStatus()));

    say("On success, the server sets a `SESSION` cookie. "
        + "DocTester stores it automatically for subsequent requests:");

    Cookie session = sayAndGetCookieWithName("SESSION");
    sayAndAssertThat("SESSION cookie is set", session, notNullValue());

    sayNextSection("Accessing Protected Resources");

    say("With the session cookie in place, access authenticated endpoints normally:");

    Response profileResponse = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/profile")));

    sayAndAssertThat("Profile accessible", 200, equalTo(profileResponse.httpStatus()));
}
```
