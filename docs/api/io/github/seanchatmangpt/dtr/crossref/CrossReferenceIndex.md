# `CrossReferenceIndex`

> **Package:** `io.github.seanchatmangpt.dtr.crossref`  

Central registry for all cross-references between DocTests. Manages: - Recording references during test execution - Resolving references to their labels after compilation - Two-pass compilation with LaTeX \label{} and \ref{} commands - Validation of all references before compilation Implements singleton pattern via getInstance() for global access. Thread-safe for concurrent test execution.

```java
public class CrossReferenceIndex {
    // CrossReferenceIndex, getInstance, reset, register, getReferences, resolve, generateLatexRef, buildIndex, ... (13 total)
}
```

---

## Methods

### `CrossReferenceIndex`

Private constructor for singleton pattern.

---

### `buildIndex`

Build the cross-reference index from .tex files (first pass). Scans all .tex files to extract section-to-label mappings before resolving cross-references. This is typically called during a pre-compilation step.

| Parameter | Description |
| --- | --- |
| `texFiles` | list of .tex file paths to scan |
| `docTestClass` | the DocTest class that generated these files |

| Exception | Description |
| --- | --- |
| `Exception` | if files cannot be read |

---

### `clear`

Clear all registered references and reset the index. Useful for testing or when resetting between test runs.

---

### `generateLatexRef`

Generate a LaTeX \ref{} command for a cross-reference.

| Parameter | Description |
| --- | --- |
| `ref` | the reference to generate a command for |

> **Returns:** LaTeX command like "\ref{section:user-creation}"

| Exception | Description |
| --- | --- |
| `InvalidDocTestRefException` | if the DocTest class is not found |
| `InvalidAnchorException` | if the anchor is not found |

---

### `getAllAnchors`

Get all anchors registered in the index.

> **Returns:** immutable map of anchor -> label

---

### `getInstance`

Get the singleton instance of CrossReferenceIndex.

> **Returns:** the global CrossReferenceIndex instance

---

### `getReferences`

Get all registered references. Returns an unmodifiable copy of the current references list.

> **Returns:** immutable list of all registered references

---

### `getResolver`

Get the underlying ReferenceResolver for advanced operations.

> **Returns:** the ReferenceResolver instance

---

### `isCompiled`

Check if the index has been compiled.

> **Returns:** true if buildIndex has been called successfully

---

### `register`

Register a cross-reference during test execution. Called by DTR.sayRef() to record each cross-reference made during test execution.

| Parameter | Description |
| --- | --- |
| `ref` | the reference to register |

---

### `reset`

Reset the singleton for testing purposes.

---

### `resolve`

Resolve a reference to its label after compilation.

| Parameter | Description |
| --- | --- |
| `ref` | the reference to resolve |

> **Returns:** the resolved label (e.g., "Section 3.2")

| Exception | Description |
| --- | --- |
| `InvalidDocTestRefException` | if the DocTest class is not found |
| `InvalidAnchorException` | if the anchor is not found |

---

### `validateReferences`

Validate all registered references against the index. Called before LaTeX compilation to ensure all referenced sections exist. Throws an exception on the first invalid reference found.

| Exception | Description |
| --- | --- |
| `InvalidDocTestRefException` | if a DocTest class is not found |
| `InvalidAnchorException` | if an anchor is not found |

---

