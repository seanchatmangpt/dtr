# `NatureTemplate`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

Nature-compatible LaTeX template for journal submissions. Uses plain article class with Nature style formatting, author-year citations via natbib (\citep, \citet), and strict constraints aligned with Nature's publication guidelines: - No colored boxes; warnings/notes use italicized text - Tables use booktabs only (no vertical rules) - Abstract: 200-word limit enforced at assembly time - Methods section: mandatory for empirical claims - Word limit: 3000 words for Letters (enforced at assembly time) - Data availability statement: included in preamble - Code availability statement: auto-populated from git remote URL This template is publication-grade for Nature, Nature Methods, and related venues.

```java
public record NatureTemplate(String codeRepositoryUrl) implements LatexTemplate {
    // NatureTemplate, isAbstractValid, countWords, isWordCountValid
}
```

---

## Methods

### `NatureTemplate`

Creates a Nature template with optional code repository URL.

---

### `countWords`

Counts words in document body (excluding preamble and metadata). Nature Letters have a 3000-word limit.

---

### `isAbstractValid`

Validates that abstract is within 200-word limit. Nature requires abstracts to be self-contained and ≤200 words.

---

### `isWordCountValid`

Enforces Nature word limits based on article type. - Letter: 3000 words max - Article: 5000 words max

---

