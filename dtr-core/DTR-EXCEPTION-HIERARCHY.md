# DTR Exception Hierarchy - Implementation Guide

## Overview

DTR now has a base `DtrException` class that provides:
- **Builder pattern** for fluent exception construction
- **Structured error context** with key-value pairs
- **Error codes** for categorization (e.g., "DTR-REF-001")
- **Actionable error messages** following "what + why + how to fix" pattern

## Base Class: DtrException

Location: `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/DtrException.java`

### Key Features

1. **Builder Pattern**
   ```java
   throw DtrException.builder()
       .errorCode("DTR-001")
       .message("Something went wrong")
       .context("key", "value")
       .cause(cause)
       .build();
   ```

2. **Structured Context**
   - All context key-value pairs are preserved
   - Immutable after construction
   - Available via `getContext()`

3. **Error Codes**
   - Standardized format: `DTR-{CATEGORY}-{NUMBER}`
   - Examples: `DTR-REF-001`, `DTR-CITE-001`, `DTR-ANCHOR-001`

4. **Enhanced toString()**
   - Includes error code
   - Shows context map
   - Clear error message

## Updated Exceptions

### 1. InvalidDocTestRefException
**Location:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/crossref/InvalidDocTestRefException.java`

**Error Code:** `DTR-REF-001`

**Before:**
```java
public class InvalidDocTestRefException extends RuntimeException {
    public InvalidDocTestRefException(String message) {
        super(message);
    }
}
```

**After:**
```java
public class InvalidDocTestRefException extends DtrException {
    public InvalidDocTestRefException(String message) {
        super(builder().message(message).errorCode("DTR-REF-001"));
    }
}
```

### 2. InvalidAnchorException
**Location:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/crossref/InvalidAnchorException.java`

**Error Code:** `DTR-ANCHOR-001`

**Before:**
```java
public class InvalidAnchorException extends RuntimeException {
    public InvalidAnchorException(String message) {
        super(message);
    }
}
```

**After:**
```java
public class InvalidAnchorException extends DtrException {
    public InvalidAnchorException(String message) {
        super(builder().message(message).errorCode("DTR-ANCHOR-001"));
    }
}
```

### 3. UnknownCitationException
**Location:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/bibliography/UnknownCitationException.java`

**Error Code:** `DTR-CITE-001`

**Before:**
```java
public class UnknownCitationException extends RuntimeException {
    public UnknownCitationException(String key) {
        super("Unknown citation key: '%s'".formatted(key));
    }
}
```

**After:**
```java
public class UnknownCitationException extends DtrException {
    public UnknownCitationException(String key) {
        super(builder()
            .message("Unknown citation key: '%s'".formatted(key))
            .errorCode("DTR-CITE-001")
            .context("citationKey", key));
    }
}
```

**Enhancement:** Now includes structured context with the citation key for debugging.

### 4. MultiRenderException
**Location:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/MultiRenderMachine.java` (nested class)

**Status:** Unchanged
**Reason:** This is a special-purpose exception that aggregates multiple failures from parallel rendering. It's not a typical DTR error and serves a different architectural purpose.

## How to Create New DTR Exceptions

### Pattern 1: Simple Exception with Error Code
```java
package io.github.seanchatmangpt.dtr.myfeature;

import io.github.seanchatmangpt.dtr.DtrException;

public class MyFeatureException extends DtrException {

    public MyFeatureException(String message) {
        super(builder()
            .message(message)
            .errorCode("DTR-MYFEATURE-001"));
    }
}
```

### Pattern 2: Exception with Context
```java
package io.github.seanchatmangpt.dtr.myfeature;

import io.github.seanchatmangpt.dtr.DtrException;

public class ValidationException extends DtrException {

    public ValidationException(String field, String value) {
        super(builder()
            .message("Validation failed for field: %s".formatted(field))
            .errorCode("DTR-VALIDATE-001")
            .context("field", field)
            .context("value", value)
            .context("severity", "ERROR"));
    }
}
```

### Pattern 3: Exception with Cause
```java
package io.github.seanchatmangpt.dtr.myfeature;

import io.github.seanchatmangpt.dtr.DtrException;

public class ParseException extends DtrException {

    public ParseException(String input, Throwable cause) {
        super(builder()
            .message("Failed to parse input: %s".formatted(input))
            .errorCode("DTR-PARSE-001")
            .context("input", input)
            .cause(cause));
    }
}
```

### Pattern 4: Using Builder Directly
```java
// In your code
throw DtrException.builder()
    .errorCode("DTR-001")
    .message("Failed to process template")
    .context("templateName", "my-template.md")
    .context("lineNumber", 42)
    .cause(new IOException("File not found"))
    .build();
```

## Error Code Convention

**Format:** `DTR-{CATEGORY}-{NUMBER}`

**Categories:**
- `REF` - Cross-references (DocTestRef, anchors)
- `CITE` - Bibliography citations
- `ANCHOR` - Section anchors
- `PARSE` - Parsing errors
- `VALIDATE` - Validation errors
- `RENDER` - Rendering errors
- `IO` - File I/O errors

**Examples:**
- `DTR-REF-001` - Invalid DocTest reference
- `DTR-CITE-001` - Unknown citation key
- `DTR-ANCHOR-001` - Invalid anchor
- `DTR-PARSE-001` - Parse error
- `DTR-RENDER-001` - Render failure

## Benefits

### 1. **Actionable Error Messages**
```java
// Before
throw new RuntimeException("Invalid anchor");

// After
throw DtrException.builder()
    .message("Invalid anchor 'nonexistent-section' in class com.example.MyTest")
    .errorCode("DTR-ANCHOR-001")
    .context("className", "com.example.MyTest")
    .context("anchor", "nonexistent-section")
    .context("availableAnchors", List.of("intro", "conclusion"))
    .build();
```

### 2. **Structured Debugging Context**
```java
try {
    // ... code that throws DtrException
} catch (DtrException e) {
    // Access structured context
    Map<String, Object> ctx = e.getContext();
    System.out.println("Error code: " + e.getErrorCode());
    System.out.println("Context: " + ctx);
}
```

### 3. **Consistent Error Handling**
```java
// Catch all DTR exceptions
catch (DtrException e) {
    logger.error("DTR error [{}]: {}", e.getErrorCode(), e.getMessage());
    logger.error("Context: {}", e.getContext());
}

// Catch specific exception
catch (InvalidDocTestRefException e) {
    // Handle invalid references
}
```

### 4. **Better Error Reporting**
```java
@Override
public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(getClass().getSimpleName());
    if (errorCode != null) {
        sb.append("[").append(errorCode).append("]");
    }
    sb.append(": ").append(getMessage());
    if (!context.isEmpty()) {
        sb.append("\nContext: ").append(context);
    }
    return sb.toString();
}
```

## Migration Guide

### For Existing Exceptions

1. **Change extends clause**
   ```java
   // Before
   public class MyException extends RuntimeException

   // After
   public class MyException extends DtrException
   ```

2. **Add import**
   ```java
   import io.github.seanchatmangpt.dtr.DtrException;
   ```

3. **Update constructors**
   ```java
   // Before
   public MyException(String message) {
       super(message);
   }

   // After
   public MyException(String message) {
       super(builder().message(message).errorCode("DTR-MYCODE-001"));
   }
   ```

4. **Add context where useful**
   ```java
   public MyException(String key) {
       super(builder()
           .message("Invalid key: " + key)
           .errorCode("DTR-MYCODE-001")
           .context("key", key));
   }
   ```

## Testing

```java
@Test
public void testDtrExceptionWithBuilder() {
    DtrException ex = DtrException.builder()
        .message("Test error")
        .errorCode("DTR-TEST-001")
        .context("key1", "value1")
        .build();

    assertEquals("DTR-TEST-001", ex.getErrorCode());
    assertEquals("Test error", ex.getMessage());
    assertEquals(Map.of("key1", "value1"), ex.getContext());
}

@Test
public void testInvalidDocTestRefExceptionExtendsDtrException() {
    InvalidDocTestRefException ex = new InvalidDocTestRefException("Test message");

    assertTrue(ex instanceof DtrException);
    assertEquals("DTR-REF-001", ex.getErrorCode());
    assertEquals("Test message", ex.getMessage());
}
```

## Compilation Verification

All exception files compile successfully:
```bash
cd /Users/sac/dtr/dtr-core
javac --enable-preview -source 26 -cp target/classes \
  src/main/java/io/github/seanchatmangpt/dtr/DtrException.java \
  src/main/java/io/github/seanchatmangpt/dtr/crossref/InvalidDocTestRefException.java \
  src/main/java/io/github/seanchatmangpt/dtr/crossref/InvalidAnchorException.java \
  src/main/java/io/github/seanchatmangpt/dtr/bibliography/UnknownCitationException.java
```

## Next Steps

1. ✅ Created `DtrException` base class
2. ✅ Updated 3 existing exceptions to extend `DtrException`
3. ✅ Added error codes to all exceptions
4. ✅ Enhanced `UnknownCitationException` with structured context
5. ⏭️ Add tests for exception hierarchy
6. ⏭️ Update error handling code to use new context
7. ⏭️ Document error code conventions in project docs

## Files Modified

- **Created:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/DtrException.java`
- **Modified:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/crossref/InvalidDocTestRefException.java`
- **Modified:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/crossref/InvalidAnchorException.java`
- **Modified:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/bibliography/UnknownCitationException.java`
- **Unchanged:** `/Users/sac/dtr/dtr-core/src/main/java/io/github/seanchatmangpt/dtr/rendermachine/MultiRenderMachine.java` (nested MultiRenderException)
