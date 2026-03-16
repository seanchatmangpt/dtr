# `DtrException`

> **Package:** `io.github.seanchatmangpt.dtr`  

Base exception for all DTR-specific errors. <p>Provides actionable error messages with "what + why + how to fix" pattern. Uses builder pattern for fluent construction and supports structured error context.</p> <p><strong>Error message pattern:</strong></p> <ul>   <li><strong>What:</strong> Clear description of what failed</li>   <li><strong>Why:</strong> Root cause or precondition that failed</li>   <li><strong>How to fix:</strong> Actionable steps to resolve</li> </ul> <p>Example usage:</p> <pre>{@code throw DtrException.builder()     .errorCode("DTR-001")     .message("Failed to resolve cross-reference")     .context("targetClass", "com.example.MissingClass")     .context("anchor", "nonexistent-section")     .cause(new ClassNotFoundException())     .build(); }</pre> <p>Subclasses should use the builder to construct their instances:</p> <pre>{@code public class InvalidDocTestRefException extends DtrException {     public InvalidDocTestRefException(String className, String anchor) {         super(DtrException.builder()             .errorCode("DTR-REF-001")             .message("Invalid DocTest reference: %s#%s".formatted(className, anchor))             .context("className", className)             .context("anchor", anchor));     } } }</pre>

```java
public class DtrException extends RuntimeException {
    // DtrException, DtrException, builder, getErrorCode, getContext, message, errorCode, context, ... (11 total)
}
```

---

## Methods

### `DtrException`

Creates a new DTR exception using a builder.

| Parameter | Description |
| --- | --- |
| `builder` | the builder instance |

---

### `build`

Builds and returns the DTR exception.

> **Returns:** a new DTR exception instance

| Exception | Description |
| --- | --- |
| `IllegalStateException` | if message is not set |

---

### `builder`

Creates a new builder for constructing DTR exceptions.

> **Returns:** a new builder instance

---

### `cause`

Sets the underlying cause.

| Parameter | Description |
| --- | --- |
| `cause` | the underlying throwable |

> **Returns:** this builder for chaining

---

### `context`

Sets all context entries at once.

| Parameter | Description |
| --- | --- |
| `context` | the context map to copy |

> **Returns:** this builder for chaining

---

### `errorCode`

Sets the error code.

| Parameter | Description |
| --- | --- |
| `code` | the error code (e.g., "DTR-001") |

> **Returns:** this builder for chaining

---

### `getContext`

Returns the structured context map for debugging.

> **Returns:** an unmodifiable map of context key-value pairs

---

### `getErrorCode`

Returns the error code for this exception.

> **Returns:** the error code (e.g., "DTR-001")

---

### `message`

Sets the error message.

| Parameter | Description |
| --- | --- |
| `message` | the error message |

> **Returns:** this builder for chaining

---

