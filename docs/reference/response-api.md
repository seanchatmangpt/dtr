# Reference: Response API

**Package:** `io.github.seanchatmangpt.dtr.doctester.testbrowser`
**File:** `dtr-core/src/main/java/org/r10r/doctester/testbrowser/Response.java`

`Response` holds the result of an HTTP request. It provides access to the status code, headers, and body, with built-in JSON/XML deserialization.

---

## Fields

#### `httpStatus` → `int`

The HTTP response status code (200, 201, 404, etc.).

```java
sayAndAssertThat("OK", 200, equalTo(response.httpStatus()));
sayAndAssertThat("Created", 201, equalTo(response.httpStatus()));
sayAndAssertThat("Not found", 404, equalTo(response.httpStatus()));
```

#### `headers` → `Map<String, String>`

The response headers as a case-sensitive map. Header names are returned as the server sent them.

```java
String contentType = response.headers().get("Content-Type");
String location = response.headers().get("Location");
```

#### `payload` → `String`

The raw response body as a string. Use `payloadAsString()` instead for clarity.

---

## Body access

#### `payloadAsString()` → `String`

Returns the raw response body as a string.

```java
String body = response.payloadAsString();
```

#### `payloadAsPrettyString()` → `String`

Returns the response body formatted for readability:
- JSON: indented with 2 spaces
- XML: indented
- Other: returned as-is

This is what DTR renders in the HTML response panel.

```java
String prettyBody = response.payloadAsPrettyString();
```

---

## Deserialization

All deserialization methods use Jackson under the hood.

#### `payloadAs(Class<T> type)` → `T`

Deserializes the response body to the given type. Auto-detects format from the `Content-Type` header:
- `application/json` → uses `ObjectMapper`
- `application/xml` → uses `XmlMapper`

```java
record User(Long id, String name, String email) {}

User user = response.payloadAs(User.class);
```

```java
record UserList(List<User> users) {}

UserList list = response.payloadAs(UserList.class);
```

#### `payloadJsonAs(Class<T> type)` → `T`

Deserializes the body as JSON regardless of the `Content-Type` header.

```java
User user = response.payloadJsonAs(User.class);
```

#### `payloadXmlAs(Class<T> type)` → `T`

Deserializes the body as XML regardless of the `Content-Type` header.

```java
Article article = response.payloadXmlAs(Article.class);
```

#### `payloadJsonAs(TypeReference<T> typeRef)` → `T`

Deserializes JSON to a generic type. Use for `List<T>`, `Map<K,V>`, and other parameterized types.

```java
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

List<User> users = response.payloadJsonAs(new TypeReference<List<User>>() {});

Map<String, Object> map = response.payloadJsonAs(new TypeReference<Map<String, Object>>() {});
```

#### `payloadXmlAs(TypeReference<T> typeRef)` → `T`

Deserializes XML to a generic type.

---

## Common patterns

**Check status code and deserialize:**
```java
Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/users/1")));

sayAndAssertThat("User found", 200, equalTo(response.httpStatus()));

User user = response.payloadAs(User.class);
sayAndAssertThat("Name is Alice", "Alice", equalTo(user.name()));
```

**Check a response header:**
```java
Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/users"))
        .contentTypeApplicationJson()
        .payload(newUser));

sayAndAssertThat("Created", 201, equalTo(response.httpStatus()));

String location = response.headers().get("Location");
sayAndAssertThat("Location header present", location, notNullValue());
say("The new resource URL is: `" + location + "`");
```

**Assert on a list response:**
```java
Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/articles")));

record ArticleList(List<Article> articles) {}
ArticleList result = response.payloadAs(ArticleList.class);

sayAndAssertThat("At least one article", result.articles().size(), greaterThan(0));
```

**Assert on 404:**
```java
Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/users/99999")));

sayAndAssertThat(
    "Non-existent user returns 404",
    404,
    equalTo(response.httpStatus()));
```

---

## Notes on deserialization

- `payloadAs(Class)` reads `Content-Type` — if the header is missing or unexpected, use `payloadJsonAs` or `payloadXmlAs` explicitly
- For XML deserialization, your DTO class needs `@XmlRootElement` (JAXB annotation)
- Jackson's `ObjectMapper` is used with default settings (no special modules)
- For records (Java 16+), Jackson requires the `jackson-module-parameter-names` module or constructor annotations if field names differ
