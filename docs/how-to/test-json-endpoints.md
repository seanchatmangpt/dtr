# How-To: Document JSON Payloads

Use `sayJson` and `java.net.http.HttpClient` to document HTTP interactions that involve JSON in DTR 2.6.0.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Document a JSON Request and Response

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class UserApiJsonDocTest {

    @Test
    void documentCreateUser(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Create User");
        ctx.say("POST /api/users creates a new user and returns 201 with the assigned ID.");

        // Document the request payload
        var requestPayload = Map.of("name", "alice", "email", "alice@example.com");
        ctx.say("Request body:");
        ctx.sayJson(requestPayload);

        // Make the actual HTTP call
        var client = HttpClient.newHttpClient();
        var body = new com.fasterxml.jackson.databind.ObjectMapper()
            .writeValueAsString(requestPayload);
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/users"))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .header("Content-Type", "application/json")
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Assert
        assertThat(response.statusCode()).isEqualTo(201);

        // Document the response
        ctx.say("Response body (201 Created):");
        ctx.sayJson(response.body());
    }
}
```

---

## Document a GET Response

```java
@Test
void documentListUsers(DtrContext ctx) throws Exception {
    ctx.sayNextSection("List Users");
    ctx.say("GET /api/users returns a paginated list of active users.");

    ctx.sayCode("GET /api/users?page=1&size=20", "http");

    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/api/users?page=1&size=20"))
        .GET()
        .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(200);

    ctx.say("Example response (status " + response.statusCode() + "):");
    ctx.sayJson(response.body());
}
```

---

## Document a Record Schema and Example

Use `sayRecordComponents` alongside `sayJson` to fully document a data type:

```java
record CreateUserRequest(String name, String email) {}
record UserResponse(long id, String name, String email, String createdAt) {}

@Test
void documentUserSchema(DtrContext ctx) {
    ctx.sayNextSection("User API Schemas");

    ctx.say("**Request schema** — POST /api/users body:");
    ctx.sayRecordComponents(CreateUserRequest.class);
    ctx.sayJson(new CreateUserRequest("alice", "alice@example.com"));

    ctx.say("**Response schema** — on 201 Created:");
    ctx.sayRecordComponents(UserResponse.class);
    ctx.sayJson(new UserResponse(42L, "alice", "alice@example.com", "2026-03-14T10:00:00Z"));
}
```

---

## Document Error Responses

```java
@Test
void documentUserNotFound(DtrContext ctx) throws Exception {
    ctx.sayNextSection("Error: User Not Found");
    ctx.say("GET /api/users/{id} returns 404 when the user does not exist.");

    var client = HttpClient.newHttpClient();
    var request = HttpRequest.newBuilder()
        .uri(URI.create("http://localhost:8080/api/users/99999"))
        .GET()
        .build();
    var response = client.send(request, HttpResponse.BodyHandlers.ofString());

    assertThat(response.statusCode()).isEqualTo(404);

    ctx.say("Error response body (404 Not Found):");
    ctx.sayJson(response.body());

    ctx.sayNote("All error responses follow the same structure: " +
                "{\"statusCode\": N, \"errorCode\": \"...\", \"message\": \"...\"}");
}
```

---

## Document a Complex Nested JSON Structure

```java
@Test
void documentOrderPayload(DtrContext ctx) {
    ctx.sayNextSection("Order Creation Payload");
    ctx.say("The order creation endpoint accepts a nested JSON structure " +
            "with line items, shipping address, and payment method.");

    var orderPayload = Map.of(
        "customerId", 42,
        "items", java.util.List.of(
            Map.of("productId", 101, "quantity", 2, "unitPrice", 29.99),
            Map.of("productId", 205, "quantity", 1, "unitPrice", 49.99)
        ),
        "shippingAddress", Map.of(
            "street", "123 Main St",
            "city", "Springfield",
            "country", "US"
        ),
        "paymentMethod", Map.of(
            "type", "credit_card",
            "last4", "4242"
        )
    );

    ctx.sayJson(orderPayload);

    ctx.say("A successful response returns 201 Created with the order ID and estimated delivery date.");
}
```

---

## Best Practices

**Document both request and response.** A complete API example shows what goes in and what comes out. Always include both.

**Use sayRecordComponents for schema documentation.** Before showing an example payload, show the field-level schema so readers understand each field's type and purpose.

**Use real HTTP calls when integration test infrastructure is available.** Prefer live server calls over hard-coded examples — they stay in sync with the API automatically.

**Use sayJson for static examples when no server is available.** In unit tests or documentation-only tests, pass `Map` or `record` instances directly to `sayJson`.

---

## See Also

- [Document Record Schemas](upload-files.md) — sayRecordComponents
- [Document Exception Handling](test-xml-endpoints.md) — sayException for error documentation
- [Use sayDocCoverage](use-custom-headers.md) — Coverage matrix for API documentation
