# `DtrValidator`

> **Package:** `io.github.seanchatmangpt.dtr.validation`  
> **Since:** `2026.4.0`  

Fluent validation API for DTR input validation. <p>This class provides a type-safe, fluent API for validating input parameters and configuration values. The API follows a builder pattern that allows chaining multiple validation rules together. <h2>Basic Usage</h2> <pre>{@code // Single validation DtrValidator.validate("username")     .value(input)     .notBlank()     .orThrow(); // Multiple validations ValidationResult nameResult = DtrValidator.validate("username")     .value(input)     .notBlank()     .notNull(); if (!nameResult.isValid()) {     System.err.println("Validation failed: " + nameResult.errorMessage()); } }</pre> <h2>Validation Rules</h2> <ul>   <li>{@code notNull()} - Ensures the value is not null</li>   <li>{@code notBlank()} - Ensures strings are not null or blank</li>   <li>{@code positive()} - Ensures numbers are greater than zero</li> </ul> <h2>Error Messages</h2> <p>All error messages automatically include the field name for clarity: <pre> "username cannot be blank" "age must be positive" </pre>

```java
public class DtrValidator {
    // DtrValidator, validate, ValidationBuilder, value, notNull, notBlank, positive, getFieldName, ... (9 total)
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

### `getValue`

Returns the value being validated.

> **Returns:** the value, or null if not yet set

---

### `notBlank`

Validates that the value is not a blank string. <p>Returns a valid result if the value is a non-null, non-blank string. A blank string is one that is empty, contains only whitespace, or is null. <h2>Example</h2> <pre>{@code ValidationResult result = DtrValidator.validate("username")     .value(input)     .notBlank(); }</pre>

> **Returns:** a ValidationResult indicating whether the value is non-blank

---

### `notNull`

Validates that the value is not null. <p>Returns a valid result if the value is non-null, otherwise returns an invalid result with an appropriate error message. <h2>Example</h2> <pre>{@code ValidationResult result = DtrValidator.validate("config")     .value(configObject)     .notNull(); }</pre>

> **Returns:** a ValidationResult indicating whether the value is non-null

---

### `positive`

Validates that the value is a positive number. <p>Returns a valid result if the value is a Number greater than zero. This works with any numeric type (Integer, Long, Double, etc.). <h2>Example</h2> <pre>{@code ValidationResult result = DtrValidator.validate("age")     .value(userAge)     .positive(); }</pre>

> **Returns:** a ValidationResult indicating whether the value is positive

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

