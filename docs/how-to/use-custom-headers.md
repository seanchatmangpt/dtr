# How-to: Use Custom Headers

## Add a single custom header

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/data"))
        .addHeader("Authorization", "Bearer my-token-here"));
```

## Add an Accept header

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/articles/1"))
        .addHeader("Accept", "application/json"));
```

## Add multiple headers

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/resource"))
        .addHeader("Authorization", "Bearer token123")
        .addHeader("X-Request-ID", "abc-456")
        .addHeader("X-Client-Version", "2.0.0"));
```

## Set headers from a map

```java
Map<String, String> headers = new LinkedHashMap<>();
headers.put("Authorization", "Bearer token123");
headers.put("X-Correlation-ID", "req-789");

Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/data"))
        .headers(headers));
```

> `headers(Map)` **replaces** all headers. Use `addHeader` to add incrementally.

## API key authentication

```java
Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/data"))
        .addHeader("X-API-Key", "your-api-key"));
```

## Bearer token authentication

```java
String token = obtainBearerToken();  // from login or test setup

Response response = sayAndMakeRequest(
    Request.GET()
        .url(testServerUrl().path("/api/protected"))
        .addHeader("Authorization", "Bearer " + token));
```

## Complete example

```java
@Test
public void testApiKeyAuth() {

    sayNextSection("API Key Authentication");

    say("Some endpoints require an API key passed in the `X-API-Key` header. "
        + "Contact your administrator to obtain a key.");

    Response response = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/data"))
            .addHeader("X-API-Key", "demo-key-12345")
            .addHeader("Accept", "application/json"));

    sayAndAssertThat("API key accepted", 200, equalTo(response.httpStatus()));

    say("Without the header, the server returns 401 Unauthorized:");

    Response unauth = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/data")));

    sayAndAssertThat("Missing API key rejected", 401, equalTo(unauth.httpStatus()));
}
```
