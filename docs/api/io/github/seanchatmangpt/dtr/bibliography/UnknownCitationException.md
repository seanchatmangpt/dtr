# `UnknownCitationException`

> **Package:** `io.github.seanchatmangpt.dtr.bibliography`  

Thrown when a citation references a non-existent bibliography entry. This exception is thrown at test execution time (not compile time) when sayCite() is called with a key that doesn't exist in the loaded bibliography. Tests fail fast if a citation key is not found.

```java
public class UnknownCitationException extends RuntimeException {
    // UnknownCitationException, UnknownCitationException
}
```

---

## Methods

### `UnknownCitationException`

Creates an exception with a custom message.

| Parameter | Description |
| --- | --- |
| `message` | detailed error message |

---

