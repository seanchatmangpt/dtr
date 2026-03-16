package io.github.seanchatmangpt.dtr.validation;

/**
 * Immutable record representing the result of a validation operation.
 *
 * <p>This class provides a simple, type-safe way to represent validation outcomes.
 * Each result contains a boolean indicating validity and an optional error message.
 *
 * <h2>Example Usage</h2>
 * <pre>{@code
 * ValidationResult result = DtrValidator.validate("username")
 *     .value(input)
 *     .notBlank()
 *     .orThrow();
 * }</pre>
 *
 * @see DtrValidator
 * @since 2026.4.0
 */
public record ValidationResult(boolean isValid, String errorMessage) {

    /**
     * Creates a valid validation result with no error message.
     *
     * <p>This factory method should be used when all validation rules pass.
     *
     * @return a valid ValidationResult with no error message
     */
    public static ValidationResult valid() {
        return new ValidationResult(true, null);
    }

    /**
     * Creates an invalid validation result with the specified error message.
     *
     * <p>This factory method should be used when a validation rule fails.
     * The error message should clearly describe what validation failed and why.
     *
     * @param message the error message describing the validation failure
     * @return an invalid ValidationResult with the provided error message
     * @throws IllegalArgumentException if message is null or blank
     */
    public static ValidationResult invalid(String message) {
        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("Error message cannot be null or blank");
        }
        return new ValidationResult(false, message);
    }

    /**
     * Throws an exception if this validation result is invalid.
     *
     * <p>This method provides a convenient way to terminate the validation chain
     * and enforce validation rules. If the result is valid, this method does nothing.
     *
     * <h2>Example Usage</h2>
     * <pre>{@code
     * ValidationResult result = DtrValidator.validate("age")
     *     .value(userAge)
     *     .positive();
     *
     * result.orThrow(); // Throws IllegalArgumentException if invalid
     * }</pre>
     *
     * @throws IllegalArgumentException if this validation result is invalid
     */
    public void orThrow() {
        if (!isValid) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /**
     * Returns a string representation of this validation result.
     *
     * <p>The format includes the validation status and, if invalid, the error message.
     *
     * @return a string representation of this validation result
     */
    @Override
    public String toString() {
        if (isValid) {
            return "ValidationResult[VALID]";
        }
        return "ValidationResult[INVALID: " + errorMessage + "]";
    }
}
