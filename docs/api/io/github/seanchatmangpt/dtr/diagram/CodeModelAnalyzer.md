# `CodeModelAnalyzer`

> **Package:** `io.github.seanchatmangpt.dtr.diagram`  

Shared utility for Java 26 Code Reflection API (JEP 516 / Project Babylon) traversal. <p>Uses {@code method.codeModel()} (preview API) to walk the IR tree of a {@code @CodeReflection}-annotated method and extract operation counts, block counts, and IR excerpts for documentation.</p> <p>Falls back gracefully to method signature introspection when no code model is available (method not annotated with {@code @CodeReflection}, or runtime &lt; Java 25).</p>

```java
@SuppressWarnings("preview") public final class CodeModelAnalyzer {
    // analyze, extractCallees, extractBlocks
}
```

---

## Methods

### `analyze`

Analyzes a method using the Java 26 Code Reflection API, with fallback to standard reflection on older runtimes or un-annotated methods.

| Parameter | Description |
| --- | --- |
| `method` | the method to analyze (should be annotated with {@code @CodeReflection}) |
| `excerptLimit` | maximum number of op descriptions to include in the IR excerpt |

> **Returns:** analysis result; never null

---

### `extractBlocks`

Extracts blocks with their ops for CFG generation. Returns a list of block descriptions, each containing op names.

---

### `extractCallees`

Extracts all unique callee method names from InvokeOp nodes in a method's code model.

| Parameter | Description |
| --- | --- |
| `method` | the method to analyze |

> **Returns:** list of callee method simple names, empty if no code model available

---

