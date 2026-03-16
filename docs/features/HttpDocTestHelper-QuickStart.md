# HttpDocTestHelper — Quick Start Guide

## TL;DR

Fluent HTTP testing helper for DTR that reduces boilerplate:

```java
var helper = new HttpDocTestHelper(ctx);

helper.get("https://api.example.com/users")
      .send()
      .expectStatus(200)
      .documentResponse()
      .documentJson("Users");
```

## Installation

Included in `dtr-core` version 2026.4.0+ (no additional dependencies).

## Basic Usage

### 1. GET Request

```java
@ExtendWith(DtrExtension.class)
class ApiDocTest {
    @Test
    void testGetUsers(DtrContext ctx) throws Exception {
        var helper = new HttpDocTestHelper(ctx);

        helper.get("https://api.example.com/users")
              .send()
              .expectStatus(200)
              .documentResponse()
              .documentJson("User List");
    }
}
```

### 2. POST Request

```java
@Test
void testCreateUser(DtrContext ctx) throws Exception {
    var helper = new HttpDocTestHelper(ctx);

    helper.post("https://api.example.com/users")
          .header("Content-Type", "application/json")
          .body("{\"name\":\"Alice\"}")
          .send()
          .expectStatus(201)
          .documentResponse();
}
```

### 3. PUT / DELETE / PATCH

```java
helper.put(url).send().expectStatus(200);
helper.delete(url).send().expectStatus(204);
helper.patch(url).send().expectStatus(200);
```

## Common Patterns

### Multiple Headers

```java
helper.post(url)
      .headers(Map.of(
          "Content-Type", "application/json",
          "Authorization", "Bearer token123",
          "X-Custom-Header", "value"
      ))
      .body("{}")
      .send()
      .expectStatus(200);
```

### Response Validation

```java
var response = helper.get(url).send()
      .expectStatus(200)
      .expectHeader("Content-Type", "application/json")
      .expectBodyContains("users");

String body = response.getBody();
// Custom parsing...
```

### Custom HTTP Client

```java
HttpClient customClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(30))
    .build();

var helper = new HttpDocTestHelper(ctx, customClient);
helper.get(url).send().expectStatus(200);
```

## Method Reference

### HttpDocTestHelper
- `get(url)`, `post(url)`, `put(url)`, `delete(url)`, `patch(url)`

### RequestBuilder
- `header(name, value)` — Add single header
- `headers(Map)` — Add multiple headers
- `body(String)` — Set request body
- `send()` — Execute request

### ResponseBuilder
- `expectStatus(int)` — Assert status code
- `expectHeader(name, value)` — Assert header value
- `expectBodyContains(String)` — Assert substring in body
- `documentResponse()` — Document status + Content-Type
- `documentJson(label)` — Document body as JSON
- `getBody()` — Get raw response body

## Full Example

```java
@ExtendWith(DtrExtension.class)
class UserApiDocTest {

    @Test
    void testCrudLifecycle(DtrContext ctx) throws Exception {
        var helper = new HttpDocTestHelper(ctx);

        // CREATE
        ctx.sayNextSection("Create User");
        var createResponse = helper.post("https://api.example.com/users")
            .header("Content-Type", "application/json")
            .body("{\"name\":\"Alice\",\"email\":\"alice@example.com\"}")
            .send()
            .expectStatus(201)
            .expectHeaderPresent("Location")
            .documentJson("Created User");

        String userId = "123"; // Extract from response

        // READ
        ctx.sayNextSection("Read User");
        helper.get("https://api.example.com/users/" + userId)
            .send()
            .expectStatus(200)
            .expectBodyContains("alice@example.com")
            .documentJson("User Details");

        // UPDATE
        ctx.sayNextSection("Update User");
        helper.put("https://api.example.com/users/" + userId)
            .header("Content-Type", "application/json")
            .body("{\"name\":\"Alice Smith\"}")
            .send()
            .expectStatus(200)
            .documentJson("Updated User");

        // DELETE
        ctx.sayNextSection("Delete User");
        helper.delete("https://api.example.com/users/" + userId)
            .send()
            .expectStatus(204);
    }
}
```

## See Also

- [Full Documentation](./HttpDocTestHelper.md)
- [JavaDoc](https://javadoc.io/doc/io.github.seanchatmangpt/dtr-core/latest/)
- [Example Tests](/dtr-core/src/test/java/io/github/seanchatmangpt/dtr/junit5/HttpDocTestHelperExampleTest.java)
