# Tutorial: Code Evolution with sayEvolutionTimeline and sayClassDiagram

Learn how DTR 2.6.0 turns git history and reflection into living documentation. This tutorial shows how `sayEvolutionTimeline` generates a timeline from `git log`, how `sayClassDiagram` auto-generates Mermaid class diagrams, and how `sayOpProfile` documents the operation profile of a class.

**Time:** ~30 minutes
**Prerequisites:** Java 25, DTR 2.6.0, a git repository (the project's own repo is fine)
**What you'll learn:** How to embed git history timelines, auto-generated class diagrams, and operation-count tables in your living documentation

---

## What Is sayEvolutionTimeline?

`sayEvolutionTimeline(Class<?> clazz, int maxEntries)` runs `git log` against the source file corresponding to the given class, parses the output, and emits a Mermaid `timeline` block showing commit dates, hashes, and messages — limited to the most recent `maxEntries` commits.

This is valuable for documenting *why* a class is the way it is. Reviewers can see the evolution at a glance without leaving the documentation page.

---

## Step 1 — Set Up the Test Class

Create `src/test/java/com/example/EvolutionDocTest.java`:

```java
package com.example;

import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class EvolutionDocTest {

    @Test
    void documentClassEvolution(DtrContext ctx) {

        ctx.sayNextSection("Evolution of the Order Domain Model");

        ctx.say("The `Order` class has evolved over the life of this project. "
            + "`sayEvolutionTimeline` reads `git log` and documents the history "
            + "directly in the output Markdown:");

        ctx.sayEvolutionTimeline(Order.class, 10);

        ctx.say("Each entry in the timeline above corresponds to a real git commit "
            + "that touched the source file for `Order`. "
            + "The timeline is generated at test-run time — it is always current.");
    }

    // A sample domain class to document
    record Order(long id, String customer, List<String> items, double total) {
        Order {
            if (total < 0) throw new IllegalArgumentException("total must be non-negative");
        }
        boolean isEmpty() { return items.isEmpty(); }
        double averageItemCost() { return items.isEmpty() ? 0.0 : total / items.size(); }
    }
}
```

Run the test:

```bash
mvnd test -Dtest=EvolutionDocTest
cat target/docs/test-results/EvolutionDocTest.md
```

The output will contain a Mermaid `timeline` block with up to 10 entries from `git log`.

---

## Step 2 — Auto-Generate a Class Diagram with sayClassDiagram

`sayClassDiagram(Class<?>...)` inspects the given classes via reflection and emits a Mermaid `classDiagram` block. Use it to document the structure of your domain model alongside its history:

```java
    record Customer(String name, String email, String tier) {}
    record Product(long sku, String name, double price) {}
    record LineItem(Product product, int quantity) {
        double subtotal() { return product.price() * quantity; }
    }

    @Test
    void documentDomainClassDiagram(DtrContext ctx) {

        ctx.sayNextSection("Domain Class Diagram");

        ctx.say("The following class diagram is generated automatically from the "
            + "record components and method signatures of the domain classes. "
            + "No manual DSL is required:");

        ctx.sayClassDiagram(Customer.class, Product.class, LineItem.class, Order.class);

        ctx.say("DTR uses reflection to discover fields (record components), "
            + "declared methods, implemented interfaces, and inheritance. "
            + "The diagram updates automatically when the classes change.");
    }
```

---

## Step 3 — Document Operation Profiles with sayOpProfile

`sayOpProfile(Method)` counts the types of bytecode operations in a method and emits a table. This is useful for documenting computational complexity or spotting unexpected allocations:

```java
    @Test
    void documentOpProfile(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Operation Profile: Order.averageItemCost");

        ctx.say("An operation profile counts the kinds of operations in a method's "
            + "code model. This helps document algorithmic characteristics:");

        var method = Order.class.getDeclaredMethod("averageItemCost");
        ctx.sayOpProfile(method);

        ctx.say("The table above shows operation categories (field accesses, "
            + "invocations, arithmetic, returns) and their counts. "
            + "A high invocation count may indicate a method that delegates heavily; "
            + "many field accesses may indicate a data-intensive method.");
    }
```

---

## Step 4 — Combine Timeline and Diagram for a Full Audit

Combine all three methods to produce a complete audit page for a class:

```java
    sealed interface PaymentResult permits PaymentResult.Approved, PaymentResult.Declined {
        record Approved(String authCode, double amount) implements PaymentResult {}
        record Declined(String reason)                 implements PaymentResult {}
    }

    static class PaymentService {
        PaymentResult charge(Customer customer, double amount) {
            if (amount <= 0) return new PaymentResult.Declined("amount must be positive");
            if (customer.tier().equals("blocked")) return new PaymentResult.Declined("account blocked");
            return new PaymentResult.Approved("AUTH-" + System.nanoTime(), amount);
        }

        String describeResult(PaymentResult result) {
            return switch (result) {
                case PaymentResult.Approved(var code, var amt) -> "Approved %s for $%.2f".formatted(code, amt);
                case PaymentResult.Declined(var reason)        -> "Declined: " + reason;
            };
        }
    }

    @Test
    void fullClassAudit(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Full Audit: PaymentService");

        ctx.say("A complete class audit includes the git evolution history, "
            + "the class diagram, and the operation profile of its key methods.");

        // 1. Evolution timeline
        ctx.sayNextSection("Git History");
        ctx.sayEvolutionTimeline(PaymentService.class, 5);

        // 2. Class diagram
        ctx.sayNextSection("Class Structure");
        ctx.sayClassDiagram(PaymentService.class, PaymentResult.class,
            PaymentResult.Approved.class, PaymentResult.Declined.class);

        // 3. Op profile of the main method
        ctx.sayNextSection("Operation Profile: charge()");
        var chargeMethod = PaymentService.class.getDeclaredMethod("charge", Customer.class, double.class);
        ctx.sayOpProfile(chargeMethod);

        // 4. Demonstrate the logic works
        ctx.sayNextSection("Behavioral Assertions");

        var svc      = new PaymentService();
        var regular  = new Customer("alice", "alice@example.com", "regular");
        var blocked  = new Customer("bob",   "bob@example.com",   "blocked");

        var approved = svc.charge(regular, 49.99);
        var declined = svc.charge(blocked, 49.99);

        assertThat(approved).isInstanceOf(PaymentResult.Approved.class);
        assertThat(declined).isInstanceOf(PaymentResult.Declined.class);

        ctx.sayAssertions(Map.of(
            "regular customer charge result", svc.describeResult(approved),
            "blocked customer charge result", svc.describeResult(declined)
        ));

        ctx.sayEnvProfile();
    }
```

---

## Step 5 — Control Timeline Depth

Pass a small `maxEntries` value to keep the documentation concise. A value of 5 shows the most recent five commits; a value of 0 is treated as unlimited:

```java
    @Test
    void timelineDepthExamples(DtrContext ctx) {

        ctx.sayNextSection("Timeline Depth Options");

        ctx.say("Limit the timeline to the five most recent commits for a quick summary:");
        ctx.sayEvolutionTimeline(Order.class, 5);

        ctx.say("Or extend it to twenty entries for a complete audit trail:");
        ctx.sayEvolutionTimeline(Order.class, 20);

        ctx.sayNote("If the class has fewer commits than `maxEntries`, "
            + "all available commits are shown. "
            + "DTR does not pad the timeline with empty entries.");
    }
```

---

## Key Methods in This Tutorial

| Method | What it does |
|--------|-------------|
| `sayEvolutionTimeline(Class<?>, int)` | Runs `git log` on the class's source file and emits a Mermaid timeline |
| `sayClassDiagram(Class<?>...)` | Reflects on classes and emits a Mermaid `classDiagram` block |
| `sayOpProfile(Method)` | Counts operation types in a method's code model and emits a table |
| `sayEnvProfile()` | Captures Java version, OS, heap, DTR version — no arguments needed |

---

## What You Learned

- `sayEvolutionTimeline(Class<?>, int)` reads real git history and renders it as a Mermaid timeline — always current at test-run time
- `sayClassDiagram(Class<?>...)` auto-generates class diagrams via reflection — no manual DSL
- `sayOpProfile(Method)` documents the computational characteristics of a method
- Combining timeline + diagram + op profile + behavioral assertions produces a complete class audit

---

## Next Steps

- [Tutorial: Visualizing Code with sayMermaid](websockets-realtime.md) — write custom Mermaid diagrams by hand
- [Tutorial: Contract Verification](server-sent-events.md) — document interface coverage across implementations
- [Tutorial: Benchmarking with Virtual Threads](virtual-threads-lightweight-concurrency.md) — measure the methods you are documenting
