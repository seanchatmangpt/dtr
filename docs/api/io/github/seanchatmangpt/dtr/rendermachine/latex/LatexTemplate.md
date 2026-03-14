# `LatexTemplate`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

Sealed interface for LaTeX document templates. Each implementation provides document class, preamble, and formatting conventions for a specific target format (ArXiv, USPTO, IEEE, ACM, Nature). Phase 2.1.0 ships with ArXiv and USPTO templates only.

```java
public sealed interface LatexTemplate permits ArXivTemplate, UsPatentTemplate, IEEETemplate, ACMTemplate, NatureTemplate {
    // getDocumentClass, getPreamble, getBeginDocument, getEndDocument, formatSection, formatSubsection, escapeLatex, formatCodeBlock, ... (17 total)
}
```

---

## Methods

### `escapeLatex`

Escape special LaTeX characters to prevent syntax errors. Must handle: _ $ % &amp; # ^ ~ \\ { }

---

### `formatAssertions`

Format assertion results as a table (Check | Result columns).

---

### `formatCodeBlock`

Format code block with optional language syntax highlighting. Language may be null; implementations should handle gracefully.

---

### `formatFootnote`

Format a footnote in LaTeX. Text is escaped to prevent syntax errors.

---

### `formatJson`

Format JSON/pretty-printed content in a code block.

---

### `formatKeyValue`

Format key-value pairs as a 2-column LaTeX table.

---

### `formatNote`

Format an info/note callout box (colored background, icon, etc.).

---

### `formatOrderedList`

Format an ordered (numbered) list in LaTeX.

---

### `formatSection`

LaTeX command for section-level heading (e.g., "\\\\section{%s}"). Placeholder %s will be replaced with heading text.

---

### `formatSubsection`

LaTeX command for subsection-level heading (e.g., "\\\\subsection{%s}").

---

### `formatTable`

Format a table from 2D string array. First row is headers.

---

### `formatUnorderedList`

Format an unordered (bullet) list in LaTeX.

---

### `formatWarning`

Format a warning callout box (colored background, icon, etc.).

---

### `getBeginDocument`

Text to insert after \\begin{document}.

---

### `getDocumentClass`

LaTeX document class (e.g., "article", "report", custom).

---

### `getEndDocument`

Text to insert before \\end{document}.

---

### `getPreamble`

Full preamble including \\documentclass, \\usepackage, \\title, etc.

---

