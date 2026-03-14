# How-to: Control What Gets Documented

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

## The core choice: say* vs plain code

DTR documents everything you tell it to via `say*` methods. Everything else — setup, silent assertions, helper calls — stays invisible in the output.

Use `say*` methods for content you want readers to see. Use plain Java for internal test mechanics.

## Document only interesting parts

```java
@ExtendWith(DtrExtension.class)
class ArticleDocTest {

    @Test
    void createArticle(DtrContext ctx) throws Exception {
        // Setup is done silently — no documentation output
        var client = java.net.http.HttpClient.newHttpClient();
        var loginRequest = java.net.http.HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:8080/api/login"))
            .POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                "{\"username\":\"admin\",\"password\":\"secret\"}"))
            .header("Content-Type", "application/json")
            .build();
        client.send(loginRequest, java.net.http.HttpResponse.BodyHandlers.discarding());

        // This is what we're documenting
        ctx.sayNextSection("Create Article");
        ctx.say("Create a new article with POST /api/articles:");

        ctx.sayCode("""
            POST /api/articles
            Content-Type: application/json

            {"title": "My Article", "body": "Content here."}
            """, "http");

        ctx.say("Successful creation returns 201 with the new article's ID.");
    }
}
```

## Suppress assertions from output

Use plain `assertThat` (AssertJ) or `assert` when the check is internal bookkeeping:

```java
import static org.assertj.core.api.Assertions.assertThat;

// Not documented — just validates test state
assertThat(loginResponse.statusCode()).isEqualTo(200);

// Documented — visible to readers
ctx.say("The creation response includes the assigned ID.");
ctx.sayJson(Map.of("id", 42, "title", "My Article"));
```

## Add explanatory text anywhere

`say()` and `sayNextSection()` can appear before, between, or after any logic:

```java
ctx.sayNextSection("Order Workflow");
ctx.say("Placing an order requires three steps.");

ctx.sayOrderedList(List.of(
    "Add items to the cart",
    "Proceed to checkout",
    "Confirm payment"
));

ctx.say("The cart endpoint returns the current total after each addition:");
ctx.sayCode("""
    POST /api/cart/items
    {"sku": "sku-123", "qty": 2}
    """, "http");

ctx.say("Checkout calculates the final price including tax:");
ctx.sayCode("""
    POST /api/checkout
    {"shippingAddress": "123 Main St"}
    """, "http");
```

## Inject raw Mermaid diagrams

Use `sayMermaid(String)` to embed Mermaid DSL diagrams:

```java
ctx.sayMermaid("""
    sequenceDiagram
        Client->>API: POST /api/orders
        API->>DB: Insert order
        DB-->>API: Order ID
        API-->>Client: 201 Created {id: 42}
    """);
```

## Conditional documentation

Only generate detailed docs in CI or release builds:

```java
boolean isCI = "true".equals(System.getenv("CI"));

ctx.sayNextSection("User API");

if (isCI) {
    ctx.say("Detailed explanation for release documentation...");
    ctx.sayDocCoverage(UserService.class);
    ctx.sayEnvProfile();
}

// Core test logic always runs
var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
assertThat(response.statusCode()).isEqualTo(200);
```

## Organize with multiple sections

Each `sayNextSection()` call creates an H1 heading in the output:

```java
ctx.sayNextSection("Authentication");
// ... auth documentation ...

ctx.sayNextSection("Reading Data");
// ... GET endpoint documentation ...

ctx.sayNextSection("Writing Data");
// ... POST/PUT/DELETE documentation ...

ctx.sayNextSection("Error Handling");
// ... error scenario documentation ...
```

## Organize with multiple test methods

Each `@Test` method in a class contributes to the same output file. Use JUnit 5's `@TestMethodOrder` to control order:

```java
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

@ExtendWith(DtrExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class UserApiDocTest {

    @Test
    @Order(1)
    void listUsers(DtrContext ctx) { /* ... */ }

    @Test
    @Order(2)
    void createUser(DtrContext ctx) { /* ... */ }

    @Test
    @Order(3)
    void updateUser(DtrContext ctx) { /* ... */ }

    @Test
    @Order(4)
    void deleteUser(DtrContext ctx) { /* ... */ }
}
```

## Use sayNote and sayWarning for callouts

```java
ctx.sayNote("This endpoint supports pagination via `?page=N&size=20` query parameters.");
ctx.sayWarning("Deleting a resource is permanent and cannot be undone.");
```

## Document exceptions explicitly

Use `sayException` to document error conditions in a structured way:

```java
try {
    if (userId <= 0) {
        throw new IllegalArgumentException("userId must be positive, got: " + userId);
    }
} catch (IllegalArgumentException e) {
    ctx.sayException(e);
    ctx.say("Callers should validate the userId before calling this endpoint.");
}
```
