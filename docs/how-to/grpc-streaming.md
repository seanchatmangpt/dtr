# How-To: Document Call Graphs with sayCallGraph

Visualize the internal call structure of a class using DTR 2.6.0's `sayCallGraph` method, which generates a Mermaid flowchart from reflection.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayCallGraph Does

`sayCallGraph(Class<?>)` analyzes the given class via reflection and Code Reflection IR (Java 26 preview) to produce a Mermaid `flowchart` diagram showing which methods call which other methods within the class. The diagram is embedded as a fenced Mermaid block in the output.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class OrderServiceCallGraphDocTest {

    @Test
    void documentOrderServiceCallGraph(DtrContext ctx) {
        ctx.sayNextSection("OrderService Internal Call Graph");
        ctx.say("The following diagram shows which methods call which others " +
                "inside OrderService. Use this to understand the execution flow " +
                "before refactoring.");

        ctx.sayCallGraph(OrderService.class);

        ctx.sayNote("Edges point from caller → callee. " +
                    "Isolated nodes are leaf methods (no internal calls).");
    }
}
```

---

## Combine Call Graph with Contract Verification

Document both what the service does (call graph) and how completely it implements its contract (contract verification):

```java
@Test
void documentPaymentService(DtrContext ctx) {
    ctx.sayNextSection("PaymentService: Call Graph and Contract Coverage");

    ctx.say("**Call Graph** — internal method dependencies:");
    ctx.sayCallGraph(PaymentServiceImpl.class);

    ctx.say("**Contract Coverage** — interface implementation status:");
    ctx.sayContractVerification(PaymentService.class, PaymentServiceImpl.class);
}
```

---

## Document a Pipeline Class

Call graphs are especially useful for pipeline-style classes where one public method chains through many private helpers:

```java
class DocumentationPipeline {
    public String render(String input) {
        String parsed = parse(input);
        String validated = validate(parsed);
        String formatted = format(validated);
        return output(formatted);
    }

    private String parse(String raw) { return raw.strip(); }
    private String validate(String parsed) { return parsed.isEmpty() ? "EMPTY" : parsed; }
    private String format(String valid) { return valid.toUpperCase(); }
    private String output(String formatted) { return "[" + formatted + "]"; }
}

@Test
void documentPipelineCallGraph(DtrContext ctx) {
    ctx.sayNextSection("DocumentationPipeline Call Structure");
    ctx.say("The render() method delegates through a chain of private helpers:");
    ctx.sayCallGraph(DocumentationPipeline.class);
}
```

---

## Combine with Control Flow Graph

For detailed analysis of a specific method's branching, pair `sayCallGraph` with `sayControlFlowGraph`:

```java
@Test
void documentMethodDetail(DtrContext ctx) throws NoSuchMethodException {
    ctx.sayNextSection("Order Processing: Call Graph and Control Flow");

    ctx.say("Class-level call graph:");
    ctx.sayCallGraph(OrderProcessor.class);

    ctx.say("Control flow inside processOrder():");
    var method = OrderProcessor.class.getDeclaredMethod("processOrder", long.class);
    ctx.sayControlFlowGraph(method);
}
```

---

## Best Practices

**Focus on one class at a time.** Call graphs become unreadable for very large classes. If a class has more than 20 methods, consider splitting it first.

**Combine with sayDocCoverage.** A call graph shows structure; `sayDocCoverage` shows which public methods are documented. Use both together for maintainability reviews.

**Generate before refactoring.** Capture the current call graph, then capture it again after refactoring, to demonstrate the structural simplification.

**Use with sealed result types.** Classes that return sealed types often have complex branching. The call graph and control flow graph together tell the full story.

---

## See Also

- [Verify Interface Contracts](grpc-unary.md) — sayContractVerification
- [Document Coverage Matrix](grpc-error-handling.md) — sayDocCoverage
- [Generate Mermaid Diagrams](websockets-connection.md) — sayMermaid for custom diagrams
