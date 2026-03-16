# `DtrValidator`

> **Package:** `io.github.seanchatmangpt.dtr.validation`  
> **Since:** `2026.4.0`  

Fluent validation API for DTR input validation. <p>This class provides a type-safe, fluent API for validating input parameters and configuration values. The API follows a builder pattern that allows chaining multiple validation rules together. <h2>Basic Usage</h2> <pre>{@code // Single validation DtrValidator.validate("username")     .value(input)     .notBlank()     .orThrow(); // Multiple validations ValidationResult nameResult = DtrValidator.validate("username")     .value(input)     .notBlank()     .notNull(); if (!nameResult.isValid()) {     System.err.println("Validation failed: " + nameResult.errorMessage()); } }</pre> <h2>Validation Rules</h2> <ul>   <li>{@code notNull()} - Ensures the value is not null</li>   <li>{@code notBlank()} - Ensures strings are not null or blank</li>   <li>{@code positive()} - Ensures numbers are greater than zero</li> </ul> <h2>Error Messages</h2> <p>All error messages automatically include the field name for clarity: <pre> "username cannot be blank" "age must be positive" </pre>

```java
public class DtrValidator {
    // DtrValidator, validate, ValidationBuilder, value, notNull, notBlank, positive, getResult, ... (11 total)
}
```

---

## Methods

### `DtrValidator`

Private constructor to prevent instantiation. <p>This class is designed to be used via its static factory method only.

---

### `ValidationBuilder`

Creates a new validation builder for the specified field.

| Parameter | Description |
| --- | --- |
| `fieldName` | the name of the field being validated |

---

### `getFieldName`

Returns the field name being validated.

> **Returns:** the field name

---

### `getResult`

Returns the validation result after all chained validations. <p>This method should be called at the end of the validation chain to get the final result.

> **Returns:** the ValidationResult from all chained validations

---

### `getValue`

Returns the value being validated.

> **Returns:** the value, or null if not yet set

---

### `notBlank`

Validates that the value is not a blank string. <p>If a previous validation already failed, this method does nothing (short-circuit behavior). Otherwise, checks if the value is a non-blank string. <h2>Example</h2> <pre>{@code ValidationResult result = DtrValidator.validate("username")     .value(input)     .notBlank()     .getResult(); }</pre>

> **Returns:** this builder instance for method chaining

---

### `notNull`

Validates that the value is not null. <p>If a previous validation already failed, this method does nothing (short-circuit behavior). Otherwise, checks if the value is non-null. <h2>Example</h2> <pre>{@code ValidationResult result = DtrValidator.validate("config")     .value(configObject)     .notNull()     .getResult(); }</pre>

> **Returns:** this builder instance for method chaining

---

### `orThrow`

Throws an exception if the validation failed. <p>This is a convenience method that delegates to the result's orThrow().

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if validation failed |

---

### `positive`

Validates that the value is a positive number. <p>If a previous validation already failed, this method does nothing (short-circuit behavior). Otherwise, checks if the value is a positive number. <h2>Example</h2> <pre>{@code ValidationResult result = DtrValidator.validate("age")     .value(userAge)     .positive()     .getResult(); }</pre>

> **Returns:** this builder instance for method chaining

---

### `validate`

Creates a new validation builder for the specified field name. <p>The field name is used in error messages to provide context about which validation failed.

| Parameter | Description |
| --- | --- |
| `fieldName` | the name of the field being validated |

> **Returns:** a new ValidationBuilder instance

| Exception | Description |
| --- | --- |
| `IllegalArgumentException` | if fieldName is null or blank |

---

### `value`

Sets the value to be validated. <p>This method must be called before any validation rules.

| Parameter | Description |
| --- | --- |
| `value` | the value to validate |

> **Returns:** this builder instance for method chaining

---

