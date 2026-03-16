# DTR-003: Add sayRef Convenience Overload to DtrContext

**Priority**: P1
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,api

## Description

Currently, creating cross-references with `sayRef(DocTestRef ref)` requires manual construction of `DocTestRef` objects. This ticket adds a convenience overload that accepts `(Class<?> docTestClass, String anchor)` and creates the `DocTestRef` internally, reducing boilerplate and improving developer experience.

## Acceptance Criteria

- [ ] Add `sayRef(Class<?> docTestClass, String anchor)` method to DtrContext
- [ ] Method creates `DocTestRef` internally and delegates to existing `sayRef(DocTestRef)`
- [ ] Write unit tests for new overload
- [ ] Update API documentation with usage examples
- [ ] Verify `mvnd verify` passes with new tests

## Technical Notes

### Implementation Location
- **File**: `/Users/sac/dtr/src/main/java/org/sparsevoid/dtr/DtrContext.java`

### Method Signature
```java
/**
 * Convenience overload to create cross-reference from class and anchor.
 * Equivalent to: sayRef(new DocTestRef(docTestClass, anchor))
 *
 * @param docTestClass The test class containing the target section
 * @param anchor The anchor identifier within that class
 */
public void sayRef(Class<?> docTestClass, String anchor)
```

### Implementation Pattern
```java
public void sayRef(Class<?> docTestClass, String anchor) {
    sayRef(new DocTestRef(docTestClass, anchor));
}
```

### Test Location
- **File**: `/Users/sac/dtr/src/test/java/org/sparsevoid/dtr/DtrContextTest.java`
- **Test Cases**: Verify correct DocTestRef construction, null handling, anchor validation

### Example Usage
```java
// Before (verbose)
sayRef(new DocTestRef(UserRegistrationDocTest.class, "email-validation"));

// After (concise)
sayRef(UserRegistrationDocTest.class, "email-validation");
```

## Dependencies

- None (can be implemented independently)

## References

- Current sayRef implementation: `/Users/sac/dtr/src/main/java/org/sparsevoid/dtr/DtrContext.java`
- DocTestRef class: `/Users/sac/dtr/src/main/java/org/sparsevoid/dtr/model/DocTestRef.java`
- API Reference: See `CLAUDE.md` section "Cross-References"
