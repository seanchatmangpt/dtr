package io.github.seanchatmangpt.dtr.validation;

/**
 * Fluent validation API for DTR input validation.
 *
 * <p>This class provides a type-safe, fluent API for validating input parameters
 * and configuration values. The API follows a builder pattern that allows chaining
 * multiple validation rules together.
 *
 * <h2>Basic Usage</h2>
 * <pre>{@code
 * // Single validation
 * DtrValidator.validate("username")
 *     .value(input)
 *     .notBlank()
 *     .orThrow();
 *
 * // Multiple validations
 * ValidationResult nameResult = DtrValidator.validate("username")
 *     .value(input)
 *     .notBlank()
 *     .notNull();
 *
 * if (!nameResult.isValid()) {
 *     System.err.println("Validation failed: " + nameResult.errorMessage());
 * }
 * }</pre>
 *
 * <h2>Validation Rules</h2>
 * <ul>
 *   <li>{@code notNull()} - Ensures the value is not null</li>
 *   <li>{@code notBlank()} - Ensures strings are not null or blank</li>
 *   <li>{@code positive()} - Ensures numbers are greater than zero</li>
 * </ul>
 *
 * <h2>Error Messages</h2>
 * <p>All error messages automatically include the field name for clarity:
 * <pre>
 * "username cannot be blank"
 * "age must be positive"
 * </pre>
 *
 * @since 2026.4.0
 */
public class DtrValidator {

    /**
     * Private constructor to prevent instantiation.
     *
     * <p>This class is designed to be used via its static factory method only.
     */
    private DtrValidator() {
        throw new AssertionError("DtrValidator cannot be instantiated");
    }

    /**
     * Creates a new validation builder for the specified field name.
     *
     * <p>The field name is used in error messages to provide context about
     * which validation failed.
     *
     * @param fieldName the name of the field being validated
     * @return a new ValidationBuilder instance
     * @throws IllegalArgumentException if fieldName is null or blank
     */
    public static ValidationBuilder validate(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            throw new IllegalArgumentException("Field name cannot be null or blank");
        }
        return new ValidationBuilder(fieldName);
    }

    /**
     * Fluent builder for validation rules.
     *
     * <p>This class provides a chainable API for applying multiple validation
     * rules to a single value. Each validation rule returns a ValidationResult
     * that can be checked or used to throw an exception.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * ValidationResult result = DtrValidator.validate("email")
     *     .value(userEmail)
     *     .notNull()
     *     .notBlank();
     *
     * if (result.isValid()) {
     *     // Proceed with validated value
     * }
     * }</pre>
     */
    public static class ValidationBuilder {
        private final String fieldName;
        private Object value;

        /**
         * Creates a new validation builder for the specified field.
         *
         * @param fieldName the name of the field being validated
         */
        private ValidationBuilder(String fieldName) {
            this.fieldName = fieldName;
        }

        /**
         * Sets the value to be validated.
         *
         * <p>This method must be called before any validation rules.
         *
         * @param value the value to validate
         * @return this builder instance for method chaining
         */
        public ValidationBuilder value(Object value) {
            this.value = value;
            return this;
        }

        /**
         * Validates that the value is not null.
         *
         * <p>Returns a valid result if the value is non-null, otherwise returns
         * an invalid result with an appropriate error message.
         *
         * <h2>Example</h2>
         * <pre>{@code
         * ValidationResult result = DtrValidator.validate("config")
         *     .value(configObject)
         *     .notNull();
         * }</pre>
         *
         * @return a ValidationResult indicating whether the value is non-null
         */
        public ValidationResult notNull() {
            if (value == null) {
                return ValidationResult.invalid(fieldName + " cannot be null");
            }
            return ValidationResult.valid();
        }

        /**
         * Validates that the value is not a blank string.
         *
         * <p>Returns a valid result if the value is a non-null, non-blank string.
         * A blank string is one that is empty, contains only whitespace, or is null.
         *
         * <h2>Example</h2>
         * <pre>{@code
         * ValidationResult result = DtrValidator.validate("username")
         *     .value(input)
         *     .notBlank();
         * }</pre>
         *
         * @return a ValidationResult indicating whether the value is non-blank
         */
        public ValidationResult notBlank() {
            if (value instanceof String s) {
                if (s.isBlank()) {
                    return ValidationResult.invalid(fieldName + " cannot be blank");
                }
            } else {
                // Non-string values are considered invalid for notBlank()
                if (value == null) {
                    return ValidationResult.invalid(fieldName + " cannot be null");
                }
                return ValidationResult.invalid(fieldName + " must be a String");
            }
            return ValidationResult.valid();
        }

        /**
         * Validates that the value is a positive number.
         *
         * <p>Returns a valid result if the value is a Number greater than zero.
         * This works with any numeric type (Integer, Long, Double, etc.).
         *
         * <h2>Example</h2>
         * <pre>{@code
         * ValidationResult result = DtrValidator.validate("age")
         *     .value(userAge)
         *     .positive();
         * }</pre>
         *
         * @return a ValidationResult indicating whether the value is positive
         */
        public ValidationResult positive() {
            if (value instanceof Number n) {
                if (n.longValue() <= 0) {
                    return ValidationResult.invalid(fieldName + " must be positive");
                }
            } else {
                if (value == null) {
                    return ValidationResult.invalid(fieldName + " cannot be null");
                }
                return ValidationResult.invalid(fieldName + " must be a Number");
            }
            return ValidationResult.valid();
        }

        /**
         * Returns the field name being validated.
         *
         * @return the field name
         */
        public String getFieldName() {
            return fieldName;
        }

        /**
         * Returns the value being validated.
         *
         * @return the value, or null if not yet set
         */
        public Object getValue() {
            return value;
        }
    }
}
