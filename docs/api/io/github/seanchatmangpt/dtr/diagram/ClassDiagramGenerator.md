# `ClassDiagramGenerator`

> **Package:** `io.github.seanchatmangpt.dtr.diagram`  

Generates Mermaid {@code classDiagram} DSL from Java class structures using standard reflection ({@link Class#getSuperclass()}, {@link Class#getInterfaces()}, {@link Class#getDeclaredMethods()}). <p>The resulting diagram renders natively on GitHub, GitLab, and Obsidian.</p>

```java
public final class ClassDiagramGenerator {
    // generate
}
```

---

## Methods

### `generate`

Generates a Mermaid {@code classDiagram} for the given classes.

| Parameter | Description |
| --- | --- |
| `classes` | the classes to include in the diagram |

> **Returns:** Mermaid classDiagram DSL string

---

