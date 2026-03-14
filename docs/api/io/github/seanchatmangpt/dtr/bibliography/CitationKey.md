# `CitationKey`

> **Package:** `io.github.seanchatmangpt.dtr.bibliography`  

Immutable record representing a citation key with optional page reference. <p>CitationKey is a record that encapsulates a bibliography entry identifier and an optional page reference for pinpointing specific content within a source. <p>Example: <pre>{@code var citation = new CitationKey("Knuth1997", "pp. 42-47"); var simpleCitation = new CitationKey("Smith2020", null); }</pre>

```java
public record CitationKey(String key, String pageRef) {
}
```

