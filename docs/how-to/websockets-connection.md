# How-To: Generate Mermaid Diagrams

Embed Mermaid diagrams in your documentation using DTR 2.6.0's `sayMermaid` and `sayClassDiagram` methods.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## sayMermaid: Raw Mermaid DSL

`sayMermaid(String diagramDsl)` embeds any Mermaid diagram as a fenced code block. Use it for sequence diagrams, flowcharts, ER diagrams, Gantt charts — any Mermaid-supported type.

### Sequence Diagram

```java
@ExtendWith(DtrExtension.class)
class ApiFlowDocTest {

    @Test
    void documentAuthFlow(DtrContext ctx) {
        ctx.sayNextSection("Authentication Flow");
        ctx.say("The following sequence diagram shows the OAuth 2.0 flow:");

        ctx.sayMermaid("""
            sequenceDiagram
                participant C as Client
                participant A as Auth Server
                participant R as Resource Server

                C->>A: POST /oauth/token (credentials)
                A-->>C: access_token + refresh_token
                C->>R: GET /api/users (Bearer token)
                R->>A: Validate token
                A-->>R: Token valid, userId=42
                R-->>C: 200 OK [{id:42, name:"alice"}]
            """);

        ctx.sayNote("Access tokens expire after 15 minutes. " +
                    "Use the refresh token to obtain a new one.");
    }
}
```

### Flowchart

```java
@Test
void documentOrderProcessing(DtrContext ctx) {
    ctx.sayNextSection("Order Processing Flow");

    ctx.sayMermaid("""
        flowchart TD
            A[Customer submits order] --> B{Inventory check}
            B -->|In stock| C[Reserve items]
            B -->|Out of stock| D[Send back-order notification]
            C --> E[Process payment]
            E -->|Success| F[Ship order]
            E -->|Failure| G[Release inventory]
            F --> H[Send confirmation email]
            D --> I[Notify when available]
        """);
}
```

### State Diagram

```java
@Test
void documentOrderStates(DtrContext ctx) {
    ctx.sayNextSection("Order State Machine");

    ctx.sayMermaid("""
        stateDiagram-v2
            [*] --> Pending
            Pending --> Confirmed : payment_success
            Pending --> Cancelled : payment_failed
            Confirmed --> Shipped : warehouse_dispatch
            Shipped --> Delivered : courier_delivered
            Shipped --> Returned : customer_return
            Delivered --> [*]
            Returned --> [*]
            Cancelled --> [*]
        """);
}
```

---

## sayClassDiagram: Auto-Generated Class Diagrams

`sayClassDiagram(Class<?>... classes)` uses reflection to generate a Mermaid `classDiagram` automatically. It discovers fields, methods, and relationships between the provided classes.

### Simple Class Diagram

```java
record Address(String street, String city, String country) {}
record User(long id, String name, String email, Address address) {}
record Order(long id, User customer, java.util.List<String> items, double total) {}

@Test
void documentDomainModel(DtrContext ctx) {
    ctx.sayNextSection("Domain Model");
    ctx.say("The core domain records and their relationships:");

    ctx.sayClassDiagram(User.class, Address.class, Order.class);
}
```

### Interface and Implementation Hierarchy

```java
interface Repository<T, ID> {
    T findById(ID id);
    java.util.List<T> findAll();
    T save(T entity);
    void delete(ID id);
}

@Test
void documentRepositoryHierarchy(DtrContext ctx) {
    ctx.sayNextSection("Repository Layer Class Diagram");
    ctx.sayClassDiagram(Repository.class, UserRepository.class, OrderRepository.class);
}
```

### Sealed Type Hierarchy

```java
sealed interface PaymentMethod {
    record CreditCard(String last4, String brand) implements PaymentMethod {}
    record BankTransfer(String iban) implements PaymentMethod {}
    record Wallet(String walletId) implements PaymentMethod {}
}

@Test
void documentPaymentTypes(DtrContext ctx) {
    ctx.sayNextSection("Payment Method Types");
    ctx.say("All payment methods are sealed — exhaustive pattern matching is required:");

    ctx.sayClassDiagram(
        PaymentMethod.class,
        PaymentMethod.CreditCard.class,
        PaymentMethod.BankTransfer.class,
        PaymentMethod.Wallet.class
    );

    ctx.sayCode("""
        String describe(PaymentMethod pm) {
            return switch (pm) {
                case PaymentMethod.CreditCard(String last4, String brand) ->
                    brand + " ending in " + last4;
                case PaymentMethod.BankTransfer(String iban) ->
                    "Bank transfer: " + iban;
                case PaymentMethod.Wallet(String id) ->
                    "Wallet: " + id;
            };
        }
        """, "java");
}
```

---

## Combine Mermaid with Record Components

Document both the structure and the schema:

```java
record Product(long id, String name, double price, int stock) {}

@Test
void documentProductModel(DtrContext ctx) {
    ctx.sayNextSection("Product Model");

    ctx.sayRecordComponents(Product.class);

    ctx.say("Entity relationship diagram:");
    ctx.sayMermaid("""
        erDiagram
            PRODUCT {
                long id PK
                string name
                double price
                int stock
            }
            CATEGORY {
                long id PK
                string name
            }
            ORDER_ITEM {
                long orderId FK
                long productId FK
                int quantity
                double unitPrice
            }
            PRODUCT ||--o{ ORDER_ITEM : "included in"
            CATEGORY ||--o{ PRODUCT : "contains"
        """);
}
```

---

## Best Practices

**Use sayMermaid for complex flows.** When the diagram involves time ordering, state transitions, or non-class relationships, write the DSL directly.

**Use sayClassDiagram for type structure.** When you want to document record fields, interface hierarchies, or domain models, let reflection generate the diagram automatically.

**Keep diagrams focused.** A diagram with more than 10 nodes becomes hard to read. Split into multiple diagrams if needed.

**Pair with sayNote for context.** Diagrams alone are not self-explanatory. Always add a `ctx.sayNote()` or `ctx.say()` to explain what the reader should focus on.

---

## See Also

- [Generate Class Diagrams (deep-dive)](websockets-broadcast.md) — sayClassDiagram with reflection details
- [Control Flow and Call Graphs](websockets-error-handling.md) — sayControlFlowGraph, sayCallGraph
- [Document Record Schemas](upload-files.md) — sayRecordComponents
