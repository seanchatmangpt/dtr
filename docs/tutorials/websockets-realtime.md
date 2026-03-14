# Tutorial: Visualizing Code with sayMermaid and sayClassDiagram

Learn how DTR 2.6.0 embeds diagrams directly in generated documentation using `sayMermaid`, `sayClassDiagram`, and `sayCallGraph`. By the end of this tutorial you will produce a Markdown file containing architecture flowcharts, automatically generated class diagrams, and a call graph — all derived from real Java code.

**Time:** ~25 minutes
**Prerequisites:** Java 26, DTR 2.6.0, completion of [Your First DocTest](your-first-doctest.md)
**What you'll learn:** How to write raw Mermaid DSL, how DTR generates class diagrams via reflection, and how to embed call graphs in living documentation

---

## What Is sayMermaid?

`sayMermaid(String diagramDsl)` takes raw [Mermaid](https://mermaid.js.org/) DSL and emits a fenced code block with the `mermaid` language tag. Markdown renderers that support Mermaid (GitHub, GitLab, Obsidian, many static site generators) render the DSL as an SVG diagram automatically.

```java
ctx.sayMermaid("""
    flowchart LR
        A[Start] --> B{Decision}
        B -- Yes --> C[Action]
        B -- No  --> D[Skip]
    """);
```

No external tooling is required — DTR just writes the DSL into the output file.

---

## Step 1 — Write a Flowchart with sayMermaid

Create `src/test/java/com/example/DiagramDocTest.java`:

```java
package com.example;

import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class DiagramDocTest {

    @Test
    void documentRequestLifecycle(DtrContext ctx) {

        ctx.sayNextSection("HTTP Request Lifecycle");

        ctx.say("The following diagram shows how an HTTP request flows through "
            + "a typical layered Java application:");

        ctx.sayMermaid("""
            flowchart TD
                Client([HTTP Client])
                Filter[Security Filter]
                Controller[Controller Layer]
                Service[Service Layer]
                Repo[Repository Layer]
                DB[(Database)]

                Client -->|request| Filter
                Filter -->|authenticated| Controller
                Controller -->|call| Service
                Service -->|query| Repo
                Repo -->|SQL| DB
                DB -->|ResultSet| Repo
                Repo -->|domain objects| Service
                Service -->|response dto| Controller
                Controller -->|JSON| Client
            """);

        ctx.say("Each arrow represents a method call or I/O operation. "
            + "The diagram was written as Mermaid DSL directly in the test.");
    }
}
```

Run the test and open the output:

```bash
mvnd test -Dtest=DiagramDocTest
cat target/docs/test-results/DiagramDocTest.md
```

You will see a fenced `mermaid` block in the Markdown. Paste it into a Mermaid-enabled renderer to see the SVG.

---

## Step 2 — Auto-Generate a Class Diagram with sayClassDiagram

`sayClassDiagram(Class<?>... classes)` uses reflection to inspect the given classes and emits a Mermaid `classDiagram` block with fields, methods, and relationships.

Add a second test method:

```java
    // Sample domain classes to diagram
    interface Printable {
        void print();
    }

    record Author(String name, String email) {}

    record Book(String title, Author author, int pages) implements Printable {
        @Override public void print() { System.out.println(title); }
    }

    record Library(String name, java.util.List<Book> books) {
        int totalPages() {
            return books.stream().mapToInt(Book::pages).sum();
        }
    }

    @Test
    void documentClassHierarchy(DtrContext ctx) {

        ctx.sayNextSection("Class Diagram: Library Domain");

        ctx.say("DTR can generate a Mermaid `classDiagram` by inspecting classes "
            + "at runtime via reflection. No manual DSL is needed:");

        ctx.sayCode("""
            ctx.sayClassDiagram(Author.class, Book.class, Library.class);
            """, "java");

        ctx.sayClassDiagram(Author.class, Book.class, Library.class);

        ctx.say("The diagram above was generated automatically. "
            + "It shows record components as fields, "
            + "methods declared in the class body, "
            + "and implemented interfaces.");
    }
```

---

## Step 3 — Diagram a Call Graph with sayCallGraph

`sayCallGraph(Class<?>)` analyzes the invoke operations in a class's code model and emits a Mermaid graph showing which methods call which:

```java
    static class OrderProcessor {
        void process(Order order) {
            validate(order);
            persist(order);
            notify(order);
        }

        void validate(Order order) { /* ... */ }
        void persist(Order order)  { /* ... */ }
        void notify(Order order)   { /* ... */ }
    }

    record Order(long id, String item) {}

    @Test
    void documentCallGraph(DtrContext ctx) {

        ctx.sayNextSection("Call Graph: OrderProcessor");

        ctx.say("The call graph shows which methods invoke which other methods. "
            + "DTR derives this from Code Reflection IR — no source parsing required:");

        ctx.sayCallGraph(OrderProcessor.class);

        ctx.say("Nodes represent methods; edges represent direct invocations. "
            + "This is useful for spotting tangled dependencies at a glance.");
    }
```

---

## Step 4 — Write a Sequence Diagram

`sayMermaid` accepts any Mermaid diagram type. Here is a sequence diagram:

```java
    @Test
    void documentSequenceDiagram(DtrContext ctx) {

        ctx.sayNextSection("Sequence Diagram: Order Creation");

        ctx.say("A sequence diagram shows the message flow between participants "
            + "over time. Write it as Mermaid DSL:");

        ctx.sayMermaid("""
            sequenceDiagram
                actor User
                participant API as REST API
                participant SVC as OrderService
                participant DB  as Database

                User  ->>  API: POST /orders
                API   ->>  SVC: createOrder(dto)
                SVC   ->>  DB:  INSERT INTO orders
                DB    -->> SVC: order_id = 42
                SVC   -->> API: Order{id=42, ...}
                API   -->> User: 201 Created
            """);

        ctx.sayNote("Mermaid sequence diagrams render well on GitHub, "
            + "GitLab, and most modern Markdown viewers. "
            + "The DSL is stored verbatim in the generated .md file.");
    }
```

---

## Step 5 — Combine Diagrams with Benchmarks

A common pattern is to document both the structure and the performance of a component:

```java
    @Test
    void documentAndBenchmarkSorting(DtrContext ctx) {

        ctx.sayNextSection("Sorting Algorithm: Structure and Performance");

        ctx.say("First, visualize the algorithm as a flowchart:");

        ctx.sayMermaid("""
            flowchart TD
                Start([Start]) --> Check{list.size() <= 1?}
                Check -- Yes --> Return([Return list])
                Check -- No  --> Pivot[Choose pivot]
                Pivot --> Partition[Partition around pivot]
                Partition --> Left[Recurse left]
                Partition --> Right[Recurse right]
                Left --> Merge[Merge results]
                Right --> Merge
                Merge --> Return
            """);

        ctx.say("Then benchmark the real implementation:");

        var data = new java.util.ArrayList<Integer>(1000);
        for (int i = 0; i < 1000; i++) data.add(i);
        java.util.Collections.shuffle(data);

        ctx.sayBenchmark(
            "Collections.sort on 1,000 integers",
            () -> {
                var copy = new java.util.ArrayList<>(data);
                java.util.Collections.sort(copy);
            },
            10,  // warmupRounds
            50   // measureRounds
        );

        ctx.say("The diagram documents intent; the benchmark documents reality.");
    }
```

---

## Mermaid Diagram Types Supported

| Diagram | Mermaid keyword | Use case |
|---------|----------------|----------|
| Flowchart | `flowchart` / `graph` | Process flows, decision trees |
| Sequence | `sequenceDiagram` | Message flows between actors |
| Class | `classDiagram` | Object structure and relationships |
| State | `stateDiagram-v2` | State machines |
| ER | `erDiagram` | Database schemas |
| Gantt | `gantt` | Project timelines |
| Pie | `pie` | Proportional data |

Pass any of these to `sayMermaid` and DTR will write the fenced block verbatim.

---

## What You Learned

- `sayMermaid(String)` — embeds raw Mermaid DSL in generated documentation
- `sayClassDiagram(Class<?>...)` — auto-generates a class diagram via reflection
- `sayCallGraph(Class<?>)` — generates a call graph from Code Reflection IR
- Combining `sayMermaid` + `sayBenchmark` to document both structure and performance
- All Mermaid diagram types accepted by `sayMermaid`

---

## Next Steps

- [Tutorial: Contract Verification](server-sent-events.md) — document which classes implement which interface methods
- [Tutorial: Code Evolution with sayEvolutionTimeline](grpc-streaming.md) — visualize git history as a timeline diagram
- [Tutorial: Benchmarking with Virtual Threads](virtual-threads-lightweight-concurrency.md) — measure performance with `sayBenchmark`
