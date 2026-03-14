# `BibTeXRenderer`

> **Package:** `io.github.seanchatmangpt.dtr.bibliography`  

Renders BibTeXEntry objects in multiple citation styles. Supports: - IEEE numeric style: [1], [2], [3] - ACM/Author-year style: (Author 2026) - Nature author-year with natbib

```java
public final class BibTeXRenderer {
    // renderIEEENumeric, renderACMAuthorYear, renderMarkdownInline, renderBibliography, renderLatexBibliography, renderEntryMarkdown, renderEntryLatex, extractFirstAuthor
}
```

---

## Methods

### `extractFirstAuthor`

Extracts the first author from an author field.

| Parameter | Description |
| --- | --- |
| `authorField` | the author field (may contain "and" separators) |

> **Returns:** the first author surname

---

### `renderACMAuthorYear`

Renders a single citation in ACM author-year style.

| Parameter | Description |
| --- | --- |
| `entry` | the BibTeX entry to render |

> **Returns:** formatted citation like "(Smith 2026)"

---

### `renderBibliography`

Renders a bibliography section in markdown format.

| Parameter | Description |
| --- | --- |
| `entries` | the bibliography entries to render |
| `style` | the citation style to use |

> **Returns:** formatted bibliography markdown

---

### `renderEntryLatex`

Renders a single entry as LaTeX.

| Parameter | Description |
| --- | --- |
| `entry` | the BibTeX entry |
| `index` | the numeric index |
| `style` | the citation style |

> **Returns:** formatted entry as LaTeX

---

### `renderEntryMarkdown`

Renders a single entry as markdown.

| Parameter | Description |
| --- | --- |
| `entry` | the BibTeX entry |

> **Returns:** formatted entry as markdown

---

### `renderIEEENumeric`

Renders a single citation in IEEE numeric style.

| Parameter | Description |
| --- | --- |
| `index` | the numeric index (1, 2, 3, ...) |

> **Returns:** formatted citation like "[1]"

---

### `renderLatexBibliography`

Generates a complete LaTeX bibliography block with entries.

| Parameter | Description |
| --- | --- |
| `entries` | the bibliography entries |
| `style` | the citation style |

> **Returns:** formatted LaTeX thebibliography environment

---

### `renderMarkdownInline`

Renders a single citation in markdown inline format.

| Parameter | Description |
| --- | --- |
| `key` | the citation key |

> **Returns:** formatted citation like "[key]"

---

