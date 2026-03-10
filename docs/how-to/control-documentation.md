# How-to: Control What Gets Documented

## The core choice: `say*` vs plain methods

Every DocTester method has two forms:

| Documented (`say*`) | Silent (plain) |
|---|---|
| `sayAndMakeRequest(req)` | `makeRequest(req)` |
| `sayAndGetCookies()` | `getCookies()` |
| `sayAndGetCookieWithName(name)` | `getCookieWithName(name)` |
| `sayAndAssertThat(...)` | Use regular `assertThat(...)` |

Use the `say*` form for everything you want to appear in the generated HTML. Use the plain form for internal test mechanics (login, setup, teardown).

## Document only interesting requests

```java
@Test
public void testCreateArticle() {

    // Login silently — users don't need to see this
    makeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/login"))
            .addFormParameter("username", "admin")
            .addFormParameter("password", "secret"));

    sayNextSection("Create Article");
    say("Create a new article with POST /api/articles:");

    // This is what we're documenting
    Response response = sayAndMakeRequest(
        Request.POST()
            .url(testServerUrl().path("/api/articles"))
            .contentTypeApplicationJson()
            .payload(new ArticleRequest("My Title", "Body text", "Alice")));

    sayAndAssertThat("Article created", 201, equalTo(response.httpStatus()));
}
```

## Suppress assertion output

Use standard Hamcrest `assertThat` when the assertion is internal bookkeeping:

```java
import static org.hamcrest.MatcherAssert.assertThat;

// Not documented — just validates test state
assertThat("Login must succeed before testing", 200, equalTo(loginStatus));

// Documented — visible to API consumers
sayAndAssertThat("New article has an ID", created.id(), notNullValue());
```

## Add explanatory text between requests

`say()` and `sayNextSection()` can appear anywhere — before, between, or after requests:

```java
sayNextSection("Order Workflow");

say("Placing an order is a multi-step process. First, add items to the cart:");

Response cartResponse = sayAndMakeRequest(
    Request.POST().url(testServerUrl().path("/api/cart/items"))
        .contentTypeApplicationJson()
        .payload(new CartItem("sku-123", 2)));

say("Then proceed to checkout. The total is calculated server-side:");

Response checkoutResponse = sayAndMakeRequest(
    Request.POST().url(testServerUrl().path("/api/checkout"))
        .contentTypeApplicationJson()
        .payload(new CheckoutRequest(shippingAddress)));

say("Finally, confirm payment. The server returns an order ID and confirmation number:");

Response confirmResponse = sayAndMakeRequest(
    Request.POST().url(testServerUrl().path("/api/orders/" + orderId + "/confirm"))
        .contentTypeApplicationJson()
        .payload(new PaymentDetails("card", "tok_test_123")));
```

## Insert raw HTML

`sayRaw(String)` injects HTML directly into the output:

```java
sayRaw("""
    <div class="alert alert-warning">
        <strong>Deprecation notice:</strong> This endpoint will be removed in v3.0.
        Use <code>/api/v2/users</code> instead.
    </div>
    """);
```

## Organize with multiple sections

Each `sayNextSection()` call adds an entry to the sidebar navigation:

```java
sayNextSection("Authentication");
// ... auth tests ...

sayNextSection("Reading Data");
// ... GET tests ...

sayNextSection("Writing Data");
// ... POST/PUT/DELETE tests ...

sayNextSection("Error Handling");
// ... error scenario tests ...
```

## Organize with multiple test methods

Each `@Test` method starts a new block in the output. Use `@FixMethodOrder(MethodSorters.NAME_ASCENDING)` to control order:

```java
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserApiDocTest extends DocTester {

    @Test
    public void test01_listUsers() { /* ... */ }

    @Test
    public void test02_createUser() { /* ... */ }

    @Test
    public void test03_updateUser() { /* ... */ }

    @Test
    public void test04_deleteUser() { /* ... */ }
}
```
