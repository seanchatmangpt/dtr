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
     * rules to a single value. Validation methods return this builder for chaining,
     * and the final result can be obtained via {@link #getResult()} or {@link #orThrow()}.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * ValidationResult result = DtrValidator.validate("email")
     *     .value(userEmail)
     *     .notNull()
     *     .notBlank()
     *     .getResult();
     *
     * if (result.isValid()) {
     *     // Proceed with validated value
     * }
     * }</pre>
     */
    public static class ValidationBuilder {
        private final String fieldName;
        private Object value;
        private ValidationResult currentResult = ValidationResult.valid();

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
         * <p>If a previous validation already failed, this method does nothing
         * (short-circuit behavior). Otherwise, checks if the value is non-null.
         *
         * <h2>Example</h2>
         * <pre>{@code
         * ValidationResult result = DtrValidator.validate("config")
         *     .value(configObject)
         *     .notNull()
         *     .getResult();
         * }</pre>
         *
         * @return this builder instance for method chaining
         */
        public ValidationBuilder notNull() {
            if (currentResult.isValid() && value == null) {
                currentResult = ValidationResult.invalid(fieldName + " cannot be null");
            }
            return this;
        }

        /**
         * Validates that the value is not a blank string.
         *
         * <p>If a previous validation already failed, this method does nothing
         * (short-circuit behavior). Otherwise, checks if the value is a non-blank string.
         *
         * <h2>Example</h2>
         * <pre>{@code
         * ValidationResult result = DtrValidator.validate("username")
         *     .value(input)
         *     .notBlank()
         *     .getResult();
         * }</pre>
         *
         * @return this builder instance for method chaining
         */
        public ValidationBuilder notBlank() {
            if (!currentResult.isValid()) {
                return this; // Short-circuit on previous failure
            }
            if (value instanceof String s) {
                if (s.isBlank()) {
                    currentResult = ValidationResult.invalid(fieldName + " cannot be blank");
                }
            } else {
                // Non-string values are considered invalid for notBlank()
                if (value == null) {
                    currentResult = ValidationResult.invalid(fieldName + " cannot be null");
                } else {
                    currentResult = ValidationResult.invalid(fieldName + " must be a String");
                }
            }
            return this;
        }

        /**
         * Validates that the value is a positive number.
         *
         * <p>If a previous validation already failed, this method does nothing
         * (short-circuit behavior). Otherwise, checks if the value is a positive number.
         *
         * <h2>Example</h2>
         * <pre>{@code
         * ValidationResult result = DtrValidator.validate("age")
         *     .value(userAge)
         *     .positive()
         *     .getResult();
         * }</pre>
         *
         * @return this builder instance for method chaining
         */
        public ValidationBuilder positive() {
            if (!currentResult.isValid()) {
                return this; // Short-circuit on previous failure
            }
            if (value instanceof Number n) {
                if (n.longValue() <= 0) {
                    currentResult = ValidationResult.invalid(fieldName + " must be positive");
                }
            } else {
                if (value == null) {
                    currentResult = ValidationResult.invalid(fieldName + " cannot be null");
                } else {
                    currentResult = ValidationResult.invalid(fieldName + " must be a Number");
                }
            }
            return this;
        }

        /**
         * Returns the validation result after all chained validations.
         *
         * <p>This method should be called at the end of the validation chain
         * to get the final result.
         *
         * @return the ValidationResult from all chained validations
         */
        public ValidationResult getResult() {
            return currentResult;
        }

        /**
         * Throws an exception if the validation failed.
         *
         * <p>This is a convenience method that delegates to the result's orThrow().
         *
         * @throws IllegalArgumentException if validation failed
         */
        public void orThrow() {
            currentResult.orThrow();
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
