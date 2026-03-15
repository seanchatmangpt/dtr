# `StringEscapeUtils`

> **Package:** `io.github.seanchatmangpt.dtr.util`  

Utility class for escaping strings to various formats (LaTeX, JSON, YAML, HTML, BibTeX). Consolidates duplicate escape logic from multiple template and writer classes.

```java
public final class StringEscapeUtils {
    // escapeLaTeX, escapeJson, escapeYaml, escapeHtml, escapeBibValue
}
```

---

## Methods

### `escapeBibValue`

Escapes special BibTeX characters in the given string.

| Parameter | Description |
| --- | --- |
| `value` | the raw value to escape |

> **Returns:** escaped value safe for BibTeX

---

### `escapeHtml`

Escapes special HTML characters in the given string.

| Parameter | Description |
| --- | --- |
| `text` | the text to escape |

> **Returns:** escaped text safe for HTML

---

### `escapeJson`

Escapes special JSON characters in the given string.

| Parameter | Description |
| --- | --- |
| `text` | the text to escape |

> **Returns:** escaped text safe for JSON

---

### `escapeLaTeX`

Escapes special LaTeX characters in the given string.

| Parameter | Description |
| --- | --- |
| `text` | the text to escape |

> **Returns:** escaped text safe for LaTeX

---

### `escapeYaml`

Escapes special YAML characters in the given string.

| Parameter | Description |
| --- | --- |
| `text` | the text to escape |

> **Returns:** escaped text safe for YAML

---

