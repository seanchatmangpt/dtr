# `ContractVerifier`

> **Package:** `io.github.seanchatmangpt.dtr.contract`  

Verifies and documents interface contract coverage across implementation classes. <p>For each public method in the contract interface, checks whether each implementation class provides a concrete (non-abstract) override or inherits it. Uses only {@link Class#getMethods()} and {@link Class#getDeclaredMethod()} — no external dependencies.</p>

```java
public final class ContractVerifier {
    // verify
}
```

---

## Methods

### `verify`

Verifies coverage of all public interface methods across the given implementations.

| Parameter | Description |
| --- | --- |
| `contract` | the interface to check |
| `implementations` | zero or more implementation classes |

> **Returns:** list of rows, one per public method in the contract

---

