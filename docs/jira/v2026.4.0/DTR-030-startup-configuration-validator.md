# DTR-030: Startup Configuration Validator

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,automation,validation

## Description
Create `DtrConfigurationValidator` to validate DTR configuration settings at startup before any documentation tests execute. This prevents cryptic failures mid-execution due to misconfiguration.

The validator should check:
- Required environment variables are set
- File system paths exist and are accessible
- Java version meets minimum requirements
- Maven configuration is valid
- Output directories are writable

## Acceptance Criteria
- [ ] Create `src/main/java/com/sac/dtr/validation/DtrConfigurationValidator.java`
- [ ] Validator runs automatically during `DtrContext` initialization
- [ ] Validates all required environment variables with clear error messages
- [ ] Checks file system permissions for input/output directories
- [ ] Verifies Java version compatibility (throws if < required)
- [ ] Provides structured validation report with pass/fail per check
- [ ] Includes integration tests for all validation scenarios
- [ ] Fails fast with actionable error messages

## Technical Notes

### Implementation Location
```
src/main/java/com/sac/dtr/validation/
├── DtrConfigurationValidator.java   # Main validator class
├── ValidationCheck.java              # Interface for individual checks
├── checks/
│   ├── EnvironmentVariableCheck.java
│   ├── FileSystemCheck.java
│   ├── JavaVersionCheck.java
│   └── MavenConfigurationCheck.java
```

### Validation Checks
```java
public class DtrConfigurationValidator {
    private final List<ValidationCheck> checks;

    public ValidationResult validate() {
        // Run all checks, collect results
        // Fail fast on critical errors
        // Return detailed report
    }
}
```

### Integration Point
```java
// In DtrContext constructor or factory
public static DtrContext create() {
    DtrConfigurationValidator validator = new DtrConfigurationValidator();
    ValidationResult result = validator.validate();
    if (!result.isValid()) {
        throw new DtrConfigurationException(result.getErrors());
    }
    return new DtrContext(...);
}
```

### Example Validation Output
```
DTR Configuration Validation:
✓ DTR_OUTPUT_DIR: /tmp/dtr/output (writable)
✓ JAVA_HOME: /usr/lib/jvm/java-26-openjdk (version 26.ea.13)
✓ MAVEN_HOME: /opt/maven (version 4.0.0-rc-3)
✗ DTR_DOC_PATH: /docs/nonexistent (directory not found)
✗ DTR_JAVA_VERSION_MIN: unset (required)

Configuration validation failed. Fix errors above before running tests.
```

## Dependencies
- None (foundational automation component)

## References
- DTR Improvement Plan: Priority 9 - Automation Enhancements
- Related: DTR-028 (DX Diagnostic Framework)
- Related: DTR-029 (Interactive Setup Wizard)
