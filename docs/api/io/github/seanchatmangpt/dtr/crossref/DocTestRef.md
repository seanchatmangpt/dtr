# `DocTestRef`

> **Package:** `io.github.seanchatmangpt.dtr.crossref`  
> **Since:** `1.0.0`  

Immutable record representing a cross-reference to another DocTest's section. <p>Enables formal linking between DocTests with automatic section number resolution and page references for LaTeX/PDF compilation. Cross-references are tracked in the {@link CrossReferenceIndex} and resolved after document generation completes.</p> <p><strong>Usage:</strong></p> <pre>{@code // Create a reference to a specific section in another DocTest DocTestRef ref = DocTestRef.of(UserApiDocTest.class, "user-registration"); // Render in documentation ctx.sayRef(ref); // After LaTeX compilation, toString() returns resolved section number // Before: "See UserApiDocTest#user-registration" // After:  "Section 3.2" }</pre> <p><strong>Rendering:</strong></p> <ul>   <li><strong>Markdown:</strong> renders as a markdown link to the target section</li>   <li><strong>LaTeX:</strong> renders as {@code \ref{label}} command, resolved to section number after compilation</li>   <li><strong>Other formats:</strong> delegates to the render machine implementation</li> </ul> <p><strong>Reference Resolution:</strong></p> <p>The {@code resolvedLabel} is initially empty. During LaTeX compilation (via {@link io.github.seanchatmangpt.dtr.assembly.DocumentAssembler}), the label is populated with the actual section number (e.g., "Section 3.2", "page 42").</p>

```java
public record DocTestRef( Class<?> docTestClass, String anchor, Optional<String> resolvedLabel) {
    // of, docTestClassName, toString
}
```

---

## Methods

### `docTestClassName`

Returns the simple class name of the target DocTest. <p>Useful for constructing the reference in unresolved form.</p>

> **Returns:** the simple class name (e.g., "UserApiDocTest")

---

### `of`

Creates a new DocTestRef with the given DocTest class and anchor. <p>The resolved label starts as empty and is populated after LaTeX compilation.</p>

| Parameter | Description |
| --- | --- |
| `docTestClass` | the target DocTest class (must not be null) |
| `anchor` | the section/anchor name (e.g., "user-registration") |

> **Returns:** a new DocTestRef with empty resolvedLabel

---

### `toString`

Returns a human-readable string representation of this reference. <p><strong>Behavior:</strong></p> <ul>   <li>If {@code resolvedLabel} is present: returns the resolved label       (e.g., "Section 3.2", "page 42")</li>   <li>If {@code resolvedLabel} is empty: returns the unresolved form       (e.g., "See UserApiDocTest#user-registration")</li> </ul>

> **Returns:** human-readable reference string

---

