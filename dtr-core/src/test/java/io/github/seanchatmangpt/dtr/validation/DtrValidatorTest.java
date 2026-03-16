package io.github.seanchatmangpt.dtr.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Comprehensive test suite demonstrating the DTR Validation Framework.
 *
 * <p>This test class serves as both validation verification and usage documentation.
 * It demonstrates all validation rules and common patterns for using the framework.
 *
 * @see DtrValidator
 * @see ValidationResult
 * @since 2026.4.0
 */
@DisplayName("DTR Validation Framework Tests")
class DtrValidatorTest {

    @Test
    @DisplayName("Basic notNull validation - valid case")
    void testNotNull_Valid() {
        // Arrange
        Object value = "test-value";

        // Act
        ValidationResult result = DtrValidator.validate("fieldName")
            .value(value)
            .notNull().getResult();

        // Assert
        assertThat("Valid value should pass notNull validation", result.isValid(), is(true));
        assertThat("Valid result should have no error message", result.errorMessage(), nullValue());
    }

    @Test
    @DisplayName("Basic notNull validation - invalid case")
    void testNotNull_Invalid() {
        // Act
        ValidationResult result = DtrValidator.validate("fieldName")
            .value(null)
            .notNull().getResult();

        // Assert
        assertThat("Null value should fail notNull validation", result.isValid(), is(false));
        assertThat("Error message should mention the field", result.errorMessage(), containsString("fieldName"));
        assertThat("Error message should be descriptive", result.errorMessage(), containsString("cannot be null"));
    }

    @Test
    @DisplayName("Basic notBlank validation - valid case")
    void testNotBlank_Valid() {
        // Act & Assert
        ValidationResult result1 = DtrValidator.validate("username")
            .value("john.doe")
            .notBlank().getResult();
        assertThat("Non-blank string should pass", result1.isValid(), is(true));

        ValidationResult result2 = DtrValidator.validate("username")
            .value("  whitespace  ")
            .notBlank().getResult();
        assertThat("String with whitespace should pass", result2.isValid(), is(true));
    }

    @Test
    @DisplayName("Basic notBlank validation - invalid cases")
    void testNotBlank_Invalid() {
        // Test empty string
        ValidationResult emptyResult = DtrValidator.validate("username")
            .value("")
            .notBlank().getResult();
        assertThat("Empty string should fail", emptyResult.isValid(), is(false));
        assertThat("Empty string error should be descriptive",
            emptyResult.errorMessage(), containsString("cannot be blank"));

        // Test whitespace-only string
        ValidationResult whitespaceResult = DtrValidator.validate("username")
            .value("   ")
            .notBlank().getResult();
        assertThat("Whitespace-only string should fail", whitespaceResult.isValid(), is(false));

        // Test null
        ValidationResult nullResult = DtrValidator.validate("username")
            .value(null)
            .notBlank().getResult();
        assertThat("Null string should fail", nullResult.isValid(), is(false));
    }

    @Test
    @DisplayName("Basic positive validation - valid cases")
    void testPositive_Valid() {
        // Test Integer
        ValidationResult intResult = DtrValidator.validate("age")
            .value(42)
            .positive().getResult();
        assertThat("Positive integer should pass", intResult.isValid(), is(true));

        // Test Long
        ValidationResult longResult = DtrValidator.validate("count")
            .value(100L)
            .positive().getResult();
        assertThat("Positive long should pass", longResult.isValid(), is(true));

        // Test Double
        ValidationResult doubleResult = DtrValidator.validate("price")
            .value(19.99)
            .positive().getResult();
        assertThat("Positive double should pass", doubleResult.isValid(), is(true));
    }

    @Test
    @DisplayName("Basic positive validation - invalid cases")
    void testPositive_Invalid() {
        // Test zero
        ValidationResult zeroResult = DtrValidator.validate("age")
            .value(0)
            .positive().getResult();
        assertThat("Zero should fail positive validation", zeroResult.isValid(), is(false));
        assertThat("Zero error should be descriptive",
            zeroResult.errorMessage(), containsString("must be positive"));

        // Test negative
        ValidationResult negativeResult = DtrValidator.validate("count")
            .value(-5)
            .positive().getResult();
        assertThat("Negative number should fail", negativeResult.isValid(), is(false));

        // Test null
        ValidationResult nullResult = DtrValidator.validate("price")
            .value(null)
            .positive().getResult();
        assertThat("Null should fail", nullResult.isValid(), is(false));
    }

    @Test
    @DisplayName("orThrow() throws exception on invalid result")
    void testOrThrow_Invalid() {
        ValidationResult result = ValidationResult.invalid("Test error message");

        // Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            result::orThrow,
            "orThrow() should throw IllegalArgumentException for invalid result"
        );

        assertThat("Exception should contain the error message",
            exception.getMessage(), is("Test error message"));
    }

    @Test
    @DisplayName("orThrow() does nothing on valid result")
    void testOrThrow_Valid() {
        ValidationResult result = ValidationResult.valid();

        // Should not throw
        assertDoesNotThrow(() -> result.orThrow(),
            "orThrow() should not throw for valid result");
    }

    @Test
    @DisplayName("Chained validation rules - all pass")
    void testChainedValidation_AllPass() {
        // Arrange
        String username = "john.doe";

        // Act
        ValidationResult result = DtrValidator.validate("username")
            .value(username)
            .notNull()
            .notBlank()
            .getResult();

        // Assert
        assertThat("All validations should pass", result.isValid(), is(true));
    }

    @Test
    @DisplayName("Chained validation rules - first fails")
    void testChainedValidation_FirstFails() {
        // Act
        ValidationResult result = DtrValidator.validate("username")
            .value(null)
            .notNull()
            .notBlank()
            .getResult();

        // Assert
        assertThat("First failing validation should stop chain", result.isValid(), is(false));
        assertThat("Error should be from first failing rule",
            result.errorMessage(), containsString("cannot be null"));
    }

    @Test
    @DisplayName("Real-world example: User registration validation")
    void testRealWorldExample_UserRegistration() {
        // Scenario: Validate user registration input
        String username = "alice";
        String email = "alice@example.com";
        Integer age = 25;

        // Validate username
        ValidationResult usernameResult = DtrValidator.validate("username")
            .value(username)
            .notNull()
            .notBlank()
            .getResult();

        assertThat("Username should be valid", usernameResult.isValid(), is(true));

        // Validate email
        ValidationResult emailResult = DtrValidator.validate("email")
            .value(email)
            .notBlank()
            .getResult();

        assertThat("Email should be valid", emailResult.isValid(), is(true));

        // Validate age
        ValidationResult ageResult = DtrValidator.validate("age")
            .value(age)
            .notNull()
            .positive()
            .getResult();

        assertThat("Age should be valid", ageResult.isValid(), is(true));
    }

    @Test
    @DisplayName("Real-world example: Configuration validation")
    void testRealWorldExample_Configuration() {
        // Scenario: Validate configuration parameters
        String configKey = "dtr.output.directory";
        String configValue = "/tmp/dtr-output";
        Integer timeoutSeconds = 30;

        // Validate configuration key
        ValidationResult keyResult = DtrValidator.validate("configKey")
            .value(configKey)
            .notBlank()
            .getResult();

        assertThat("Config key should be valid", keyResult.isValid(), is(true));

        // Validate configuration value
        ValidationResult valueResult = DtrValidator.validate("configValue")
            .value(configValue)
            .notBlank()
            .getResult();

        assertThat("Config value should be valid", valueResult.isValid(), is(true));

        // Validate timeout
        ValidationResult timeoutResult = DtrValidator.validate("timeoutSeconds")
            .value(timeoutSeconds)
            .positive()
            .getResult();

        assertThat("Timeout should be valid", timeoutResult.isValid(), is(true));

        // All validations pass - proceed with configuration
        assertDoesNotThrow(() -> {
            keyResult.orThrow();
            valueResult.orThrow();
            timeoutResult.orThrow();
        });
    }

    @Test
    @DisplayName("Error handling: Collecting multiple validation errors")
    void testErrorHandling_MultipleErrors() {
        // Scenario: Collect errors from multiple validations
        String username = "";
        Integer age = -5;

        ValidationResult usernameResult = DtrValidator.validate("username")
            .value(username)
            .notBlank().getResult();

        ValidationResult ageResult = DtrValidator.validate("age")
            .value(age)
            .positive().getResult();

        // Both should fail
        assertThat("Username validation should fail", usernameResult.isValid(), is(false));
        assertThat("Age validation should fail", ageResult.isValid(), is(false));

        // Error messages should be field-specific
        assertThat("Username error should mention field",
            usernameResult.errorMessage(), containsString("username"));
        assertThat("Age error should mention field",
            ageResult.errorMessage(), containsString("age"));
    }

    @Test
    @DisplayName("ValidationResult factory methods")
    void testValidationResult_Factories() {
        // Test valid() factory
        ValidationResult valid = ValidationResult.valid();
        assertThat("valid() should create valid result", valid.isValid(), is(true));
        assertThat("valid() should have null error message", valid.errorMessage(), nullValue());

        // Test invalid() factory
        ValidationResult invalid = ValidationResult.invalid("Custom error");
        assertThat("invalid() should create invalid result", invalid.isValid(), is(false));
        assertThat("invalid() should preserve error message", invalid.errorMessage(), is("Custom error"));

        // Test invalid() with null message
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationResult.invalid(null),
            "invalid() should reject null message"
        );

        // Test invalid() with blank message
        assertThrows(
            IllegalArgumentException.class,
            () -> ValidationResult.invalid("  "),
            "invalid() should reject blank message"
        );
    }

    @Test
    @DisplayName("ValidationResult toString() provides useful output")
    void testValidationResult_ToString() {
        ValidationResult valid = ValidationResult.valid();
        assertThat("Valid result toString", valid.toString(), is("ValidationResult[VALID]"));

        ValidationResult invalid = ValidationResult.invalid("Test error");
        assertThat("Invalid result toString", invalid.toString(),
            containsString("ValidationResult[INVALID:"));
        assertThat("Invalid result toString includes error", invalid.toString(),
            containsString("Test error"));
    }

    @Test
    @DisplayName("DtrValidator.validate() rejects invalid field names")
    void testDtrValidator_FieldNameValidation() {
        // Test null field name
        assertThrows(
            IllegalArgumentException.class,
            () -> DtrValidator.validate(null),
            "validate() should reject null field name"
        );

        // Test blank field name
        assertThrows(
            IllegalArgumentException.class,
            () -> DtrValidator.validate("  "),
            "validate() should reject blank field name"
        );
    }

    @Test
    @DisplayName("Type safety: notBlank() rejects non-String values")
    void testTypeSafety_NotBlank() {
        ValidationResult nonStringResult = DtrValidator.validate("field")
            .value(12345)
            .notBlank().getResult();

        assertThat("notBlank() should reject non-String types", nonStringResult.isValid(), is(false));
        assertThat("Error should mention type requirement",
            nonStringResult.errorMessage(), containsString("must be a String"));
    }

    @Test
    @DisplayName("Type safety: positive() rejects non-Number values")
    void testTypeSafety_Positive() {
        ValidationResult nonNumberResult = DtrValidator.validate("field")
            .value("not-a-number")
            .positive().getResult();

        assertThat("positive() should reject non-Number types", nonNumberResult.isValid(), is(false));
        assertThat("Error should mention type requirement",
            nonNumberResult.errorMessage(), containsString("must be a Number"));
    }

    @Test
    @DisplayName("DTR-009: Validation Framework Integration")
    void testIntegration_CompleteWorkflow() {
        // Scenario: Validate a complete DTR configuration
        String outputDir = "/tmp/dtr";
        Integer maxThreads = 4;
        String javaVersion = "26";
        Boolean enablePreview = true;

        // Validate all configuration parameters
        ValidationResult outputDirResult = DtrValidator.validate("outputDir")
            .value(outputDir)
            .notBlank().getResult();

        ValidationResult maxThreadsResult = DtrValidator.validate("maxThreads")
            .value(maxThreads)
            .positive().getResult();

        ValidationResult javaVersionResult = DtrValidator.validate("javaVersion")
            .value(javaVersion)
            .notBlank().getResult();

        // All validations should pass
        assertThat("Output directory should be valid", outputDirResult.isValid(), is(true));
        assertThat("Max threads should be valid", maxThreadsResult.isValid(), is(true));
        assertThat("Java version should be valid", javaVersionResult.isValid(), is(true));

        // Verify we can proceed with the configuration
        assertDoesNotThrow(() -> {
            outputDirResult.orThrow();
            maxThreadsResult.orThrow();
            javaVersionResult.orThrow();
        }, "All validations should pass without throwing exceptions");
    }
}
