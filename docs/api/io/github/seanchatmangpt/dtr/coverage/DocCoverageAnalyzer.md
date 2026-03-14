# `DocCoverageAnalyzer`

> **Package:** `io.github.seanchatmangpt.dtr.coverage`  

Analyzes documentation coverage for a class — which public methods were documented in the current test (tracked via a set of method signatures) vs. which were not.

```java
public final class DocCoverageAnalyzer {
    // analyze
}
```

---

## Methods

### `analyze`

Produces a coverage report for the given class.

| Parameter | Description |
| --- | --- |
| `clazz` | the class whose public API to check |
| `documentedSigs` | set of method signatures that were documented (caller-tracked) |

> **Returns:** list of coverage rows, one per public method

---

