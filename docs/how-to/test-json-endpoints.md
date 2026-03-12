# How-to: Test JSON Endpoints

## Send a JSON request body

Set `Content-Type: application/json` and pass any Java object as the payload:

```java
record CreateUserRequest(String name, String email) {}

Response response = sayAndMakeRequest(
    Request.POST()
        .url(testServerUrl().path("/api/users"))
        .contentTypeApplicationJson()
        .payload(new CreateUserRequest("alice", "alice@example.com")));
```

DTR serializes the payload to JSON using Jackson. The generated HTML shows the pretty-printed JSON body in the request panel.

## Receive and deserialize a JSON response

Use `response.payloadAs(MyClass.class)` — it auto-detects JSON from the `Content-Type` header:

```java
record User(Long id, String name, String email) {}

Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/users/42")));

User user = response.payloadAs(User.class);
sayAndAssertThat("User name is alice", "alice", equalTo(user.name()));
```

## Deserialize a JSON list or generic type

Use `payloadJsonAs(TypeReference<T>)` for generic types:

```java
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.List;

Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/users")));

List<User> users = response.payloadJsonAs(new TypeReference<List<User>>() {});
sayAndAssertThat("Three users returned", 3, equalTo(users.size()));
```

## Force JSON deserialization

If the server returns JSON but doesn't set the right `Content-Type`, force JSON parsing:

```java
User user = response.payloadJsonAs(User.class);
```

## Assert on a JSON field without creating a DTO

You can deserialize to a `Map` for quick checks without defining a class:

```java
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;

Response response = sayAndMakeRequest(
    Request.GET().url(testServerUrl().path("/api/status")));

Map<String, Object> body = response.payloadJsonAs(new TypeReference<Map<String, Object>>() {});
sayAndAssertThat("Status is UP", "UP", equalTo(body.get("status")));
```

## Get the raw JSON string

```java
String rawJson = response.payloadAsString();
String prettyJson = response.payloadAsPrettyString();
```

`payloadAsPrettyString()` is what appears in the HTML documentation panel.

## Complete example

```java
@Test
public void testUserLifecycle() {

    sayNextSection("User Management API");

    say("Create a user with POST /api/users. The request body is JSON.");

    record CreateUserRequest(String name, String email) {}
    record User(Long id, String name, String email) {}
    record UserList(List<User> users) {}

    Response createResponse = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/users"))
            .contentTypeApplicationJson()
            .payload(new CreateUserRequest("alice", "alice@example.com")));

    sayAndAssertThat("User created", 201, equalTo(createResponse.httpStatus()));

    User created = createResponse.payloadAs(User.class);
    sayAndAssertThat("ID assigned by server", created.id(), notNullValue());

    say("Retrieve the newly created user by ID:");

    Response getResponse = sayAndMakeRequest(
        Request.GET()
            .url(testServerUrl().path("/api/users/" + created.id())));

    User fetched = getResponse.payloadAs(User.class);
    sayAndAssertThat("Email preserved", "alice@example.com", equalTo(fetched.email()));
}
```
