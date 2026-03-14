# `BibliographyManager`

> **Package:** `io.github.seanchatmangpt.dtr.bibliography`  

Singleton manager for registering and retrieving bibliography entries. <p>BibliographyManager maintains a simple key-to-citation mapping for storing bibliography references used in documentation tests. It provides methods to register citations with optional page references and retrieve formatted citations. <p>Thread-safe via synchronized map access. Typical usage: <pre>{@code BibliographyManager bib = BibliographyManager.getInstance(); bib.register("Knuth1997", "The Art of Computer Programming, Vol. 1"); String cite = bib.getCitation("Knuth1997"); }</pre>

```java
public class BibliographyManager {
    // getInstance, register, register, getCitation, clear, size
}
```

---

## Methods

### `clear`

Clears all registered citations. Useful for resetting state between test suites.

---

### `getCitation`

Retrieves the formatted citation for the given key.

| Parameter | Description |
| --- | --- |
| `key` | the citation identifier |

> **Returns:** the stored citation string, or null if key not found

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if key is null or empty |

---

### `getInstance`

Returns the singleton instance of BibliographyManager.

> **Returns:** the shared BibliographyManager instance

---

### `register`

Registers a citation with optional page reference. <p>Stores the citation and tracks the page reference. The page reference is combined with the citation for retrieval via {@link #getCitation(String)}.

| Parameter | Description |
| --- | --- |
| `key` | the unique citation identifier |
| `citation` | the full citation string |
| `pageRef` | optional page reference (e.g., "pp. 42-47" or null) |

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if key is null or empty |
| `NullPointerException` | if citation is null |

---

### `size`

Returns the number of registered citations.

> **Returns:** the count of entries in the bibliography

---

