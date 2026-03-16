# `DtrConfig`

> **Package:** `io.github.seanchatmangpt.dtr.config`  

Supported output formats for DTR documentation. <p>Each format has specific rendering capabilities and output file extensions:</p> <ul>   <li>{@link #MARKDOWN} — GitHub Flavored Markdown (.md)</li>   <li>{@link #HTML} — Standalone HTML with embedded CSS (.html)</li>   <li>{@link #LATEX} — LaTeX source files (.tex)</li>   <li>{@link #PDF} — Compiled PDF documents via LaTeX (.pdf)</li> </ul>

```java
enum OutputFormat {
    // fromSystemProperty, fromSystemProperty, getExtension, getDocumentClass, getExtension
}
```

---

## Methods

### `fromSystemProperty`

Parses an output format from system property {@code dtr.format}.

> **Returns:** the parsed output format, or {@link #MARKDOWN} if not set

---

### `getDocumentClass`

Returns the LaTeX document class name for this template.

> **Returns:** the document class (e.g., "article", "book", "beamer", "report")

---

### `getExtension`

Returns the default file extension for this template.

> **Returns:** the file extension (always ".tex" for LaTeX templates)

---

