# Reference: Mermaid Diagram API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.6.0 (new in this release)

DTR 2.6.0 adds first-class Mermaid diagram support. `sayMermaid` and `sayClassDiagram` render diagrams inline in Markdown and HTML output. The Code Reflection methods `sayControlFlowGraph`, `sayCallGraph`, and `sayOpProfile` also produce Mermaid output.

---

## sayMermaid

```java
ctx.sayMermaid(String diagram)
```

Renders any Mermaid diagram type verbatim inside a fenced ` ```mermaid ` block.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `diagram` | `String` | Complete Mermaid diagram source |

**Supported diagram types:** `flowchart`, `sequenceDiagram`, `classDiagram`, `stateDiagram-v2`, `erDiagram`, `gantt`, `pie`, `gitGraph`, `mindmap`, `timeline`.

**Example — sequence diagram:**

```java
ctx.sayMermaid("""
    sequenceDiagram
        participant Client
        participant DtrContext
        participant RenderMachine
        Client->>DtrContext: say("text")
        DtrContext->>RenderMachine: say("text")
        RenderMachine-->>DtrContext: void
    """);
```

**Example — flowchart:**

```java
ctx.sayMermaid("""
    flowchart LR
        A[JUnit test] --> B[DtrContext.say*]
        B --> C[RenderMachine]
        C --> D[Markdown]
        C --> E[LaTeX]
        C --> F[HTML]
        C --> G[JSON]
    """);
```

**Example — state diagram:**

```java
ctx.sayMermaid("""
    stateDiagram-v2
        [*] --> Idle
        Idle --> Running: test starts
        Running --> Finished: finishAndWriteOut()
        Finished --> [*]
    """);
```

---

## sayClassDiagram

```java
ctx.sayClassDiagram(Class<?>... classes)
```

Generates a Mermaid `classDiagram` by reflecting on the given classes and renders it. Shows fields, methods, inheritance, and interface implementation relationships.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `classes` | `Class<?>...` | One or more classes to include in the diagram |

**Example:**

```java
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.rendermachine.MultiRenderMachine;

ctx.sayClassDiagram(RenderMachine.class, RenderMachineImpl.class, MultiRenderMachine.class);
```

Produces a `classDiagram` block showing:
- All public methods of each class
- Public fields
- `extends` and `implements` arrows

---

## sayControlFlowGraph (Code Reflection — v2.3.0+)

```java
ctx.sayControlFlowGraph(Method method)
```

Generates the control flow graph (CFG) of the given method using JEP 516 Code Reflection and renders it as a Mermaid `flowchart TD`.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `method` | `java.lang.reflect.Method` | The method to analyze |

**Example:**

```java
import java.lang.reflect.Method;

Method m = OrderService.class.getMethod("processOrder", Order.class);
ctx.sayNextSection("processOrder Control Flow");
ctx.sayControlFlowGraph(m);
```

**Output:** A `flowchart TD` with nodes for each basic block (entry, branches, loops, exits) and labeled edges for conditional branches.

---

## sayCallGraph (Code Reflection — v2.3.0+)

```java
ctx.sayCallGraph(Class<?> clazz)
```

Generates the intra-class call graph for all methods in the class and renders it as a Mermaid `graph LR`.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `clazz` | `Class<?>` | The class to analyze |

**Example:**

```java
ctx.sayNextSection("OrderProcessor Call Graph");
ctx.sayCallGraph(OrderProcessor.class);
```

**Output:** A `graph LR` where each node is a method and edges represent direct method calls from within the class.

---

## sayOpProfile (Code Reflection — v2.3.0+)

```java
ctx.sayOpProfile(Method method)
```

Profiles JVM operations (bytecode-level) in the given method and renders a table with opcode categories and counts.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `method` | `java.lang.reflect.Method` | The method to profile |

**Example:**

```java
Method charge = PaymentService.class.getMethod("charge", java.math.BigDecimal.class);
ctx.sayOpProfile(charge);
```

**Output:** A table with columns: Category, Opcodes, Count. Categories include: load/store, arithmetic, invocation, branching, object creation, returns.

---

## Mermaid rendering in output formats

| Output format | Mermaid support |
|---------------|-----------------|
| Markdown (`.md`) | Rendered as ` ```mermaid ` fenced blocks (GitHub, GitLab, Obsidian render natively) |
| HTML (`.html`) | Rendered via Mermaid.js CDN script included by `RenderMachineImpl` |
| LaTeX (`.tex`) | Exported as embedded PDF images via `mermaid-cli` if installed; otherwise as verbatim code |
| JSON (`.json`) | Stored as raw diagram source string |

---

## See also

- [say* Core API Reference](request-api.md) — all 37 method signatures
- [Code Reflection API Reference](grpc-reference.md) — full Code Reflection methods
- [RenderMachine API Reference](response-api.md) — output implementations
