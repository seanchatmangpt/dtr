# `JavadocIndex`

> **Package:** `io.github.seanchatmangpt.dtr.javadoc`  

Static index of Javadoc entries extracted from Java source by the dtr-javadoc Rust binary. <p>Loaded once at class initialization from {@code docs/meta/javadoc.json}. Keys are {@code fully.qualified.ClassName#methodName}.</p> <p>If the JSON file does not exist (e.g. Rust binary not run yet), the index is empty and all lookups return {@link Optional#empty()}.</p>

```java
public final class JavadocIndex {
    // lookup
}
```

---

## Methods

### `lookup`

Looks up the Javadoc entry for the given method.

| Parameter | Description |
| --- | --- |
| `method` | the method to look up |

> **Returns:** the Javadoc entry, or empty if not found

---

