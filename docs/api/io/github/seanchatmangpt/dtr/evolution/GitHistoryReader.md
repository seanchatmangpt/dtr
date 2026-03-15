# `GitHistoryReader`

> **Package:** `io.github.seanchatmangpt.dtr.evolution`  

Reads git commit history for a specific class file using {@link ProcessBuilder}. <p>Follows the exact pattern established by {@code DocMetadata}'s git helper methods — shells out to git, captures output, returns empty list on any failure so tests never fail due to git unavailability.</p>

```java
public final class GitHistoryReader {
    // read
}
```

---

## Methods

### `read`

Returns up to {@code maxEntries} commits that touched the source file for the given class. Returns an empty list if git is unavailable or the file cannot be found in git history.

| Parameter | Description |
| --- | --- |
| `clazz` | the class whose source file history to retrieve |
| `maxEntries` | maximum number of commits to return |

> **Returns:** list of git entries, newest first

---

