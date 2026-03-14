# `RenderMachineFactory`

> **Package:** `io.github.seanchatmangpt.dtr.render`  

Factory for creating configured render machine instances. Supports selecting output format(s) via Maven system properties: - -Ddtr.output=markdown,blog,slides,latex,all - -Ddtr.latex.format=arxiv|patent|ieee|acm|nature (for LaTeX output) LaTeX Format Selection: - arxiv (default): arXiv pre-print submissions - patent: USPTO patent exhibit format - ieee: IEEE journal articles - acm: ACM conference proceedings - nature: Nature scientific reports Java 26 Enhancement (JEP 526 - Lazy Constants): Template instances are cached and reused across all test invocations. The JIT compiler will inline these constants after first access, eliminating allocation overhead for subsequent factory calls. Examples: - -Ddtr.output=markdown (default, single render machine) - -Ddtr.output=all (all formats simultaneously) - -Ddtr.output=latex -Ddtr.latex.format=patent (USPTO patents) - -Ddtr.output=blog,slides (blog posts + slides only)

```java
public final class RenderMachineFactory {
    // createRenderMachine, createRenderMachine, selectLatexTemplateLazy
}
```

---

## Methods

### `createRenderMachine`

Create a render machine for single format tests (simpler variant).

| Parameter | Description |
| --- | --- |
| `testClassName` | the test class name |

> **Returns:** configured render machine

---

### `selectLatexTemplateLazy`

Select LaTeX template based on system property (uses cached instances for JEP 526). The -Ddtr.latex.format property controls which academic/patent format: - arxiv (default): arXiv pre-print submissions - patent: USPTO patent exhibit format - ieee: IEEE journal articles - acm: ACM conference proceedings - nature: Nature scientific reports

> **Returns:** selected cached LaTeX template

---

