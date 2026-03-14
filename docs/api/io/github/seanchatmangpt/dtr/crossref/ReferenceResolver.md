# `ReferenceResolver`

> **Package:** `io.github.seanchatmangpt.dtr.crossref`  

Parses .tex files to extract sayNextSection calls and build mappings between anchors and LaTeX labels. Supports two-pass LaTeX compilation: - Pass 1: scan .tex files, assign \label{} to every sayNextSection - Pass 2: resolve all \ref{} commands to corresponding labels Generates \ref{} LaTeX commands for cross-references and validates that referenced sections exist before compilation.

```java
public class ReferenceResolver {
    // parseTexFile, buildIndex, resolveLabel, generateRefCommand, validateReferences, convertTextToAnchor, getLabelsForClass, getAllAnchors, ... (9 total)
}
```

---

## Methods

### `buildIndex`

Build an index by scanning all .tex files in the given paths. Called as first pass of compilation to map all sayNextSection calls to their corresponding \label{} commands.

| Parameter | Description |
| --- | --- |
| `texFiles` | list of .tex file paths to scan |
| `docTestClass` | the DocTest class corresponding to these files |

| Exception | Description |
| --- | --- |
| `Exception` | if files cannot be read |

---

### `clear`

Clear all internal state, removing all parsed section labels and anchor mappings. Called by CrossReferenceIndex.clear() to ensure a full reset.

---

### `convertTextToAnchor`

Convert a section title to an anchor slug for use in markdown/LaTeX. Example: "User Creation" -> "user-creation"

| Parameter | Description |
| --- | --- |
| `text` | the section title |

> **Returns:** the anchor slug

---

### `generateRefCommand`

Generate a LaTeX \ref{} command for a resolved reference. The returned string is a complete LaTeX command that can be embedded in the document. It references the label that will be resolved during two-pass compilation.

| Parameter | Description |
| --- | --- |
| `ref` | the reference to generate a \ref{} for |

> **Returns:** LaTeX command like "\ref{section:user-creation}"

| Exception | Description |
| --- | --- |
| `InvalidDocTestRefException` | if the DocTest class is not found |
| `InvalidAnchorException` | if the anchor is not found |

---

### `getAllAnchors`

Get all anchors registered in the index.

> **Returns:** map of anchor -> label

---

### `getLabelsForClass`

Get all registered section labels for a DocTest class.

| Parameter | Description |
| --- | --- |
| `docTestClass` | the DocTest class |

> **Returns:** map of anchor -> label

---

### `parseTexFile`

Parse a single .tex file and extract section-to-label mappings. Searches for patterns like: \section{User Creation} \label{section:user-creation}

| Parameter | Description |
| --- | --- |
| `texFile` | path to the .tex file |
| `docTestClass` | the DocTest class that generated this file |

| Exception | Description |
| --- | --- |
| `Exception` | if the file cannot be read |

---

### `resolveLabel`

Resolve a DocTestRef to its LaTeX label. Returns the actual \label{} identifier that will be used in the compiled PDF.

| Parameter | Description |
| --- | --- |
| `ref` | the reference to resolve |

> **Returns:** the LaTeX label (e.g., "section:user-creation")

| Exception | Description |
| --- | --- |
| `InvalidDocTestRefException` | if the DocTest class is not found |
| `InvalidAnchorException` | if the anchor is not found in the target DocTest |

---

### `validateReferences`

Validate that all registered references target valid DocTests and anchors. Called before compilation to catch invalid references early.

| Parameter | Description |
| --- | --- |
| `refs` | the references to validate |

| Exception | Description |
| --- | --- |
| `InvalidDocTestRefException` | if a DocTest class is not found |
| `InvalidAnchorException` | if an anchor is not found |

---

