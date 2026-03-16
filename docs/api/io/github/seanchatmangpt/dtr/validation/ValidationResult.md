# `ValidationResult`

> **Package:** `io.github.seanchatmangpt.dtr.validation`  
> **Since:** `2026.4.0`  

Immutable record representing the result of a validation operation. <p>This class provides a simple, type-safe way to represent validation outcomes. Each result contains a boolean indicating validity and an optional error message. <h2>Example Usage</h2> <pre>{@code ValidationResult result = DtrValidator.validate("username")     .value(input)     .notBlank()     .orThrow(); }</pre>

```java
public record ValidationResult(boolean isValid, String errorMessage) {
    // valid, invalid, orThrow, toString
}
```

---

## Methods

### `invalid`

Creates an invalid validation result with the specified error message. <p>This factory method should be used when a validation rule fails. The error message should clearly describe what validation failed and why.

| Parameter | Description |
| --- | --- |
| `message` | the error message describing the validation failure |

> **Returns:** an invalid ValidationResult with the provided error message

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if message is null or blank |

---

### `orThrow`

Throws an exception if this validation result is invalid. <p>This method provides a convenient way to terminate the validation chain and enforce validation rules. If the result is valid, this method does nothing. <h2>Example Usage</h2> <pre>{@code ValidationResult result = DtrValidator.validate("age")     .value(userAge)     .positive(); result.orThrow(); // Throws IllegalArgumentException if invalid }</pre>

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if this validation result is invalid |

---

### `toString`

Returns a string representation of this validation result. <p>The format includes the validation status and, if invalid, the error message.

> **Returns:** a string representation of this validation result

---

### `valid`

Creates a valid validation result with no error message. <p>This factory method should be used when all validation rules pass.

> **Returns:** a valid ValidationResult with no error message

---

