# `CallGraphBuilder`

> **Package:** `io.github.seanchatmangpt.dtr.diagram`  

Builds a Mermaid {@code graph LR} call graph for all methods in a class that have a code model available (annotated with {@code @CodeReflection}). <p>Extracts {@code InvokeOp} targets from the Java 26 Code Reflection IR (JEP 516 / Project Babylon) and renders directed caller → callee edges.</p>

```java
@SuppressWarnings("preview") public final class CallGraphBuilder {
    // build
}
```

---

## Methods

### `build`

Builds a Mermaid {@code graph LR} DSL string from the call relationships in the given class. Only methods with code models contribute edges.

| Parameter | Description |
| --- | --- |
| `clazz` | the class to analyze |

> **Returns:** Mermaid graph DSL, or an empty string if no call relationships found

---

