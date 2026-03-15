# `ControlFlowGraphBuilder`

> **Package:** `io.github.seanchatmangpt.dtr.diagram`  

Builds a Mermaid {@code flowchart TD} diagram from the Java 26 Code Reflection IR (JEP 516 / Project Babylon) of a method. <p>Each basic block becomes a node. Conditional branch successors become directed edges. Op descriptions (first 2 per block) appear as node labels.</p>

```java
@SuppressWarnings("preview") public final class ControlFlowGraphBuilder {
    // build
}
```

---

## Methods

### `build`

Builds a Mermaid flowchart DSL string from the code model of the given method. Returns an empty string if no code model is available.

| Parameter | Description |
| --- | --- |
| `method` | the method (should be annotated with {@code @CodeReflection}) |

> **Returns:** Mermaid {@code flowchart TD} DSL, or empty string on failure

---

