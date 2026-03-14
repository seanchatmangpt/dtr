# How-To: Generate Control Flow and Call Graphs

Visualize the internal structure of your Java methods and classes using DTR 2.6.0's `sayControlFlowGraph` and `sayCallGraph` methods.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## sayControlFlowGraph: Method-Level Flow

`sayControlFlowGraph(Method)` uses Java 25 Code Reflection IR (preview) to produce a Mermaid `flowchart` showing the control flow branches inside a single method: conditionals, loops, switches, and exception handlers.

### Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.lang.reflect.Method;

@ExtendWith(DtrExtension.class)
class ControlFlowDocTest {

    static class OrderValidator {
        public String validate(String orderId, int quantity, double price) {
            if (orderId == null || orderId.isBlank()) {
                return "INVALID_ORDER_ID";
            }
            if (quantity <= 0) {
                return "INVALID_QUANTITY";
            }
            if (price < 0) {
                return "INVALID_PRICE";
            }
            if (quantity > 1000) {
                return "QUANTITY_EXCEEDS_LIMIT";
            }
            return "VALID";
        }
    }

    @Test
    void documentValidatorControlFlow(DtrContext ctx) throws NoSuchMethodException {
        ctx.sayNextSection("OrderValidator.validate() Control Flow");
        ctx.say("The following flowchart shows all validation branches in the validate() method:");

        Method validateMethod = OrderValidator.class.getDeclaredMethod(
            "validate", String.class, int.class, double.class);
        ctx.sayControlFlowGraph(validateMethod);

        ctx.sayNote("Each diamond node is a conditional check. " +
                    "The left edge is the false branch, the right edge is the true branch.");
    }
}
```

---

## sayCallGraph: Class-Level Call Structure

`sayCallGraph(Class<?>)` analyzes an entire class and shows which methods call which other methods within the class. This is useful for understanding the structure of pipeline classes, service implementations, and complex business logic.

### Document a Service Class

```java
@Test
void documentServiceCallGraph(DtrContext ctx) {
    ctx.sayNextSection("PaymentService Internal Call Graph");
    ctx.say("This diagram shows how PaymentService.processPayment() " +
            "delegates to internal helper methods:");

    ctx.sayCallGraph(PaymentService.class);
}
```

---

## Combine Both for Full Analysis

```java
@Test
void fullMethodAnalysis(DtrContext ctx) throws NoSuchMethodException {
    ctx.sayNextSection("OrderProcessor: Full Analysis");

    ctx.say("**Class-level call graph** — which methods delegate to which:");
    ctx.sayCallGraph(OrderProcessor.class);

    ctx.say("**Method-level control flow** — branching inside processOrder():");
    Method processOrder = OrderProcessor.class.getDeclaredMethod("processOrder", long.class);
    ctx.sayControlFlowGraph(processOrder);

    ctx.say("**Operation profile** — instruction counts per operation:");
    ctx.sayOpProfile(processOrder);
}
```

---

## Document a Switch Expression

Pattern matching switches produce interesting control flow graphs. Document the branching behavior:

```java
static class NotificationRouter {
    sealed interface Channel {
        record Email(String address) implements Channel {}
        record Sms(String phone) implements Channel {}
        record Push(String token) implements Channel {}
    }

    public String route(Channel channel, String message) {
        return switch (channel) {
            case Channel.Email(String addr) when addr.contains("@") ->
                "email:" + addr + ":" + message;
            case Channel.Email(String addr) ->
                "invalid_email";
            case Channel.Sms(String phone) ->
                "sms:" + phone + ":" + message;
            case Channel.Push(String token) ->
                "push:" + token + ":" + message;
        };
    }
}

@Test
void documentSwitchControlFlow(DtrContext ctx) throws NoSuchMethodException {
    ctx.sayNextSection("NotificationRouter.route() Control Flow");
    ctx.say("The route() method uses an exhaustive switch over the Channel sealed type:");

    Method routeMethod = NotificationRouter.class.getDeclaredMethod(
        "route", NotificationRouter.Channel.class, String.class);
    ctx.sayControlFlowGraph(routeMethod);
}
```

---

## sayOpProfile: Operation Count Table

`sayOpProfile(Method)` produces a table of operation counts (comparisons, memory accesses, allocations, etc.) for a method. Use it alongside `sayBenchmark` to explain why one implementation is faster:

```java
@Test
void profileAndBenchmark(DtrContext ctx) throws NoSuchMethodException {
    ctx.sayNextSection("String Parsing: Profile and Benchmark");

    Method parseMethod = StringParser.class.getDeclaredMethod("parse", String.class);

    ctx.say("Operation profile:");
    ctx.sayOpProfile(parseMethod);

    ctx.say("Actual timing:");
    ctx.sayBenchmark("StringParser.parse (10k calls)", () -> {
        StringParser.parse("hello world 42");
    }, 5, 100);
}
```

---

## Best Practices

**Use `sayControlFlowGraph` before code review.** Generate the CFG for complex methods, then include the diagram in the PR description to help reviewers understand branching.

**Use `sayCallGraph` before refactoring.** Capture the current call structure, refactor, then capture it again. The two diagrams tell the before/after story.

**Combine with `sayOpProfile` and `sayBenchmark`.** The full picture of a method is: what it does (CFG), how it relates to other methods (call graph), how many operations it performs (op profile), and how fast it runs (benchmark).

**Requires `--enable-preview`.** Both `sayControlFlowGraph` and `sayOpProfile` depend on Java 25 Code Reflection IR, which is a preview feature. Ensure `--enable-preview` is in your `.mvn/maven.config`.

---

## See Also

- [Generate Mermaid Diagrams](websockets-connection.md) — sayMermaid for custom diagrams
- [Generate Class Diagrams](websockets-broadcast.md) — sayClassDiagram via reflection
- [Benchmarking](benchmarking.md) — sayBenchmark for timing
