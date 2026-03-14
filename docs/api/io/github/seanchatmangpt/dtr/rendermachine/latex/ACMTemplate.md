# `ACMTemplate`

> **Package:** `io.github.seanchatmangpt.dtr.rendermachine.latex`  

ACM-compatible LaTeX template for conference papers and journals. Uses acmart document class (configurable to sigplan, sigsoft, tog). Provides ACM-style formatting with author-year citations, CCS concepts classification, conference header metadata, and tables with captions BELOW (opposite of IEEE). Supports auto-generation of CCS concepts from DocMetadata keywords with pre-populated mappings for common stack domains.

```java
public record ACMTemplate(String template, String conference) implements LatexTemplate {
    // ACMTemplate, mapKeywordToCCS, generateCCSConcepts, escapeXml
}
```

---

## Methods

### `ACMTemplate`

Creates an ACM template with default SIGPLAN template and no conference header.

---

### `escapeXml`

Escape XML special characters for CCS concept XML.

---

### `generateCCSConcepts`

Generate CCS Concept XML from a list of keywords.

---

### `mapKeywordToCCS`

Map common keywords to ACM CCS taxonomy IDs. Pre-populated for stack domains (databases, languages, testing, etc.).

---

