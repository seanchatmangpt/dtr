# `BibTeXEntry`

> **Package:** `io.github.seanchatmangpt.dtr.bibliography`  

Record representing a BibTeX entry. Supports formats: @article, @book, @inproceedings, @techreport. Parses and extracts structured field data from .bib files.

```java
public record BibTeXEntry( String type, String key, Map<String, String> fields ) {
    // getField, toString
}
```

---

## Methods

### `getField`

Retrieves a field value by name (case-insensitive).

| Parameter | Description |
| --- | --- |
| `fieldName` | the field to retrieve |

> **Returns:** the field value, or empty string if not present

---

### `toString`

Renders the entry in standard BibTeX format.

> **Returns:** a formatted BibTeX entry string

---

