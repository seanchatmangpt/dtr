# `PandocStrategy`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

Pandoc-based PDF compilation strategy. Fallback compiler when pdflatex, latexmk, and xelatex are unavailable. Pandoc provides reduced-fidelity PDF: BibTeX, cross-references, and two-column layouts will not work, but basic LaTeX compilation succeeds. This strategy logs warnings about lost features but does NOT fail the build if Pandoc is unavailable or compilation fails.

```java
public final class PandocStrategy implements CompilerStrategy {
    // buildPandocCommand, selectPdfEngine, isPdfEngineAvailable, logDegradationWarnings
}
```

---

## Methods

### `buildPandocCommand`

Build the Pandoc command with appropriate engine selection.

---

### `isPdfEngineAvailable`

Check if a PDF engine (xelatex, pdflatex) is available.

---

### `logDegradationWarnings`

Log warnings about Pandoc's reduced fidelity.

---

### `selectPdfEngine`

Select the best available PDF engine.

---

