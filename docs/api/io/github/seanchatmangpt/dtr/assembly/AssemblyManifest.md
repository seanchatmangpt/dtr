# `AssemblyManifest`

> **Package:** `io.github.seanchatmangpt.dtr.assembly`  

Immutable manifest of a complete document assembly combining multiple DocTests. <p>Contains only fields that are actually populated during assembly. Receipt signing and SHA3 hash computation are not yet implemented and have been removed rather than pretending they work with null/empty placeholders.

```java
public record AssemblyManifest( List<?> includedTests, int totalPages, int totalWords, int totalCodeListings, int totalTables, int totalCitations, int totalCrossReferences, Instant assembledAt ) {
    // toJson, toColophonText
}
```

---

## Methods

### `toColophonText`

Returns human-readable colophon page content for the assembled document.

---

### `toJson`

Returns a machine-readable JSON representation of this manifest.

---

