# How-To: Generate Class Diagrams with sayClassDiagram

Automatically generate Mermaid class diagrams from your Java types using DTR 2.6.0's `sayClassDiagram` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayClassDiagram Does

`sayClassDiagram(Class<?>... classes)` uses reflection to analyze the provided classes and generates a Mermaid `classDiagram` block. It discovers:

- Record components (fields)
- Public methods with signatures
- Implemented interfaces
- Inheritance relationships between the provided classes

The diagram is embedded as a fenced `mermaid` code block in the documentation output.

---

## Document a Domain Model

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class DomainModelDocTest {

    record Address(String street, String city, String postalCode, String country) {}
    record User(long id, String name, String email, Address address, boolean active) {}
    record Product(long id, String name, double price, int stock) {}
    record OrderItem(long productId, int quantity, double unitPrice) {}
    record Order(long id, long userId, java.util.List<OrderItem> items, double total) {}

    @Test
    void documentDomainModel(DtrContext ctx) {
        ctx.sayNextSection("Domain Model Class Diagram");
        ctx.say("The core domain records in the e-commerce system:");

        ctx.sayClassDiagram(
            User.class,
            Address.class,
            Order.class,
            OrderItem.class,
            Product.class
        );

        ctx.sayNote("Records are immutable. All fields are set via the canonical constructor.");
    }
}
```

---

## Document an Interface Hierarchy

```java
interface Shape {
    double area();
    double perimeter();
    String name();
}

interface Resizable {
    void scale(double factor);
}

static class Circle implements Shape {
    private final double radius;
    Circle(double radius) { this.radius = radius; }
    public double area() { return Math.PI * radius * radius; }
    public double perimeter() { return 2 * Math.PI * radius; }
    public String name() { return "Circle(r=" + radius + ")"; }
}

static class Rectangle implements Shape, Resizable {
    private double width, height;
    Rectangle(double w, double h) { this.width = w; this.height = h; }
    public double area() { return width * height; }
    public double perimeter() { return 2 * (width + height); }
    public String name() { return "Rectangle(" + width + "x" + height + ")"; }
    public void scale(double factor) { width *= factor; height *= factor; }
}

@Test
void documentShapeHierarchy(DtrContext ctx) {
    ctx.sayNextSection("Shape Type Hierarchy");
    ctx.say("The geometry module defines Shape and Resizable interfaces " +
            "with several implementations:");

    ctx.sayClassDiagram(Shape.class, Resizable.class, Circle.class, Rectangle.class);
}
```

---

## Document a Sealed Hierarchy

Sealed types are ideal for class diagrams — the compiler enforces the complete set of subtypes:

```java
sealed interface Notification {
    record EmailNotification(String to, String subject, String body) implements Notification {}
    record SmsNotification(String phone, String message) implements Notification {}
    record PushNotification(String deviceToken, String title, String body) implements Notification {}
}

@Test
void documentNotificationTypes(DtrContext ctx) {
    ctx.sayNextSection("Notification System Class Diagram");
    ctx.say("All notification types are sealed. " +
            "Pattern matching handles dispatch exhaustively:");

    ctx.sayClassDiagram(
        Notification.class,
        Notification.EmailNotification.class,
        Notification.SmsNotification.class,
        Notification.PushNotification.class
    );

    ctx.sayCode("""
        void send(Notification n) {
            switch (n) {
                case Notification.EmailNotification(String to, String sub, String body) ->
                    emailSender.send(to, sub, body);
                case Notification.SmsNotification(String phone, String msg) ->
                    smsSender.send(phone, msg);
                case Notification.PushNotification(String token, String title, String body) ->
                    pushSender.send(token, title, body);
            }
        }
        """, "java");
}
```

---

## Combine with Record Components for Full Schema

```java
record ApiError(
    int statusCode,
    String errorCode,
    String message,
    java.time.Instant timestamp
) {}

@Test
void documentApiError(DtrContext ctx) {
    ctx.sayNextSection("API Error Schema");

    ctx.sayRecordComponents(ApiError.class);
    ctx.sayClassDiagram(ApiError.class);

    ctx.say("Example error response:");
    ctx.sayJson(new ApiError(
        404,
        "USER_NOT_FOUND",
        "User with id 42 does not exist",
        java.time.Instant.now()
    ));
}
```

---

## Best Practices

**Provide related classes together.** `sayClassDiagram` can only show relationships between classes you pass to it. Include all classes that reference each other.

**Keep diagrams to 7-10 classes.** Larger diagrams become unreadable. Split your domain into bounded contexts and document each separately.

**Combine with sayMermaid for custom diagrams.** If the auto-generated diagram doesn't capture a subtle relationship (like an association multiplicity), supplement it with a hand-written `sayMermaid` diagram.

**Document sealed types as complete sets.** Always include all `permits` subtypes when documenting sealed hierarchies — the diagram is only complete when all cases are visible.

---

## See Also

- [Generate Mermaid Diagrams](websockets-connection.md) — sayMermaid for custom diagrams
- [Control Flow and Call Graphs](websockets-error-handling.md) — sayControlFlowGraph, sayCallGraph
- [Document Record Schemas](upload-files.md) — sayRecordComponents for field-level detail
