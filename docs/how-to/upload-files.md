# How-To: Document Record Schemas with sayRecordComponents

Automatically generate schema documentation for Java records using DTR 2.6.0's `sayRecordComponents` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayRecordComponents Does

`sayRecordComponents(Class<? extends Record>)` uses reflection to enumerate all components (fields) of a record and renders them as a schema table. The table shows each component's name and type, plus any annotations present on the component.

---

## Basic Record Schema

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class RecordSchemaDocTest {

    record User(long id, String name, String email, boolean active, java.time.Instant createdAt) {}

    @Test
    void documentUserRecord(DtrContext ctx) {
        ctx.sayNextSection("User Record Schema");
        ctx.say("The User record represents an authenticated user in the system.");

        ctx.sayRecordComponents(User.class);

        ctx.say("Example user:");
        ctx.sayJson(new User(
            42L, "alice", "alice@example.com", true,
            java.time.Instant.parse("2026-01-15T10:30:00Z")));
    }
}
```

---

## Document Multiple Records Together

```java
record Address(String street, String city, String postalCode, String country) {}
record ContactInfo(String phone, String email, Address address) {}
record Person(long id, String firstName, String lastName, ContactInfo contact) {}

@Test
void documentPersonModel(DtrContext ctx) {
    ctx.sayNextSection("Person Data Model");
    ctx.say("The Person entity consists of three nested records:");

    ctx.say("**Address:**");
    ctx.sayRecordComponents(Address.class);

    ctx.say("**ContactInfo:**");
    ctx.sayRecordComponents(ContactInfo.class);

    ctx.say("**Person:**");
    ctx.sayRecordComponents(Person.class);

    ctx.say("Complete example:");
    ctx.sayJson(new Person(
        1L, "Alice", "Smith",
        new ContactInfo(
            "+1-555-0100",
            "alice@example.com",
            new Address("123 Main St", "Springfield", "12345", "US")
        )
    ));
}
```

---

## Document a Sealed Type Hierarchy with Schemas

```java
sealed interface PaymentMethod {
    record CreditCard(String last4, String brand, String expiryMonth, String expiryYear)
        implements PaymentMethod {}
    record BankTransfer(String iban, String bic, String bankName)
        implements PaymentMethod {}
    record DigitalWallet(String provider, String walletId)
        implements PaymentMethod {}
}

@Test
void documentPaymentMethods(DtrContext ctx) {
    ctx.sayNextSection("Payment Method Schemas");
    ctx.say("The PaymentMethod sealed interface has three implementations:");

    ctx.sayClassDiagram(
        PaymentMethod.class,
        PaymentMethod.CreditCard.class,
        PaymentMethod.BankTransfer.class,
        PaymentMethod.DigitalWallet.class
    );

    ctx.say("**CreditCard:**");
    ctx.sayRecordComponents(PaymentMethod.CreditCard.class);
    ctx.sayJson(new PaymentMethod.CreditCard("4242", "Visa", "03", "2028"));

    ctx.say("**BankTransfer:**");
    ctx.sayRecordComponents(PaymentMethod.BankTransfer.class);
    ctx.sayJson(new PaymentMethod.BankTransfer("GB29NWBK60161331926819", "NWBKGB2L", "NatWest"));

    ctx.say("**DigitalWallet:**");
    ctx.sayRecordComponents(PaymentMethod.DigitalWallet.class);
    ctx.sayJson(new PaymentMethod.DigitalWallet("PayPal", "user@example.com"));
}
```

---

## Document API Request/Response Pairs

```java
record CreateProductRequest(
    String name,
    String description,
    double price,
    int initialStock,
    String categoryId
) {}

record ProductResponse(
    String id,
    String name,
    String description,
    double price,
    int stock,
    String categoryId,
    java.time.Instant createdAt,
    java.time.Instant updatedAt
) {}

@Test
void documentProductApiSchemas(DtrContext ctx) {
    ctx.sayNextSection("Product API: Request and Response Schemas");

    ctx.say("**POST /api/products — Request body:**");
    ctx.sayRecordComponents(CreateProductRequest.class);
    ctx.sayJson(new CreateProductRequest(
        "Java 25 in Action",
        "A comprehensive guide to Java 25 language features.",
        49.99, 100, "books-programming"
    ));

    ctx.say("**POST /api/products — 201 Created response:**");
    ctx.sayRecordComponents(ProductResponse.class);
    ctx.sayJson(new ProductResponse(
        "prod_abc123",
        "Java 25 in Action",
        "A comprehensive guide to Java 25 language features.",
        49.99, 100, "books-programming",
        java.time.Instant.now(),
        java.time.Instant.now()
    ));
}
```

---

## Combine Schema with Evolution Timeline

Show how a record has evolved alongside its current schema:

```java
record Order(long id, long userId, java.util.List<Long> productIds, double total, String status) {}

@Test
void documentOrderRecordEvolution(DtrContext ctx) {
    ctx.sayNextSection("Order Record: Schema and History");

    ctx.say("**Current schema:**");
    ctx.sayRecordComponents(Order.class);

    ctx.say("**Evolution history** (last 5 commits):");
    ctx.sayEvolutionTimeline(Order.class, 5);
}
```

---

## Best Practices

**Always pair with sayJson.** `sayRecordComponents` shows types; `sayJson` shows actual values. Readers need both.

**Document parent and child records together.** If a record contains other records as fields, document all of them in the same test so readers see the complete structure.

**Use class diagrams for hierarchy.** When documenting sealed hierarchies, start with `sayClassDiagram` to show the relationships, then drill into each variant with `sayRecordComponents`.

**Add constraint documentation.** If a record has a compact constructor that validates fields, document the validation rules with a `sayTable` call after `sayRecordComponents`.

---

## See Also

- [Use sayContractVerification](grpc-unary.md) — Verify interface contracts
- [Document Exception Handling](test-xml-endpoints.md) — sayException for constructor validation errors
- [Generate Class Diagrams](websockets-broadcast.md) — sayClassDiagram for record hierarchies
