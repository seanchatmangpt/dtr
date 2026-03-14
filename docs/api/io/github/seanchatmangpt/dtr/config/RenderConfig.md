# `RenderConfig`

> **Package:** `io.github.seanchatmangpt.dtr.config`  

Central configuration for render machine selection and instantiation. Parses system properties to determine which render machines to activate: - {@code -Ddtr.output=markdown}: Markdown only (default, backward compatible) - {@code -Ddtr.output=latex}: LaTeX/PDF only - {@code -Ddtr.output=markdown,latex}: Both formats simultaneously LaTeX template selection: - {@code -Ddtr.latex.template=arxiv}: ArXiv format (default) - {@code -Ddtr.latex.template=patent}: USPTO/patent format

```java
public final class RenderConfig {
    // createRenderMachines, selectTemplate, isFormatEnabled, getLatexTemplate
}
```

---

## Methods

### `createRenderMachines`

Create render machine(s) based on system properties.

> **Returns:** Single RenderMachine if one format selected, or MultiRenderMachine if multiple

---

### `getLatexTemplate`

Get the selected LaTeX template name.

---

### `isFormatEnabled`

Check if a specific output format is enabled.

---

### `selectTemplate`

Select LaTeX template by name.

---

