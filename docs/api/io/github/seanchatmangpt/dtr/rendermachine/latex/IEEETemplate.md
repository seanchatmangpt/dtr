# `IEEETemplate`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

IEEE-compatible LaTeX template for journal and conference papers. Uses IEEEtran document class configured for journal submissions (configurable to conference mode). Provides IEEE-style formatting with numeric citations [1], abstract and keywords sections, and table/code formatting adhering to IEEE guidelines. Tables use caption ABOVE, wide tables employ table*.

```java
public record IEEETemplate(String mode) implements LatexTemplate {
    // IEEETemplate
}
```

---

## Methods

### `IEEETemplate`

Creates an IEEE template with default journal mode.

---

