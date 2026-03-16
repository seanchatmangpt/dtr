# DTR-006: Rename sayCodeModel(Method) for Clarity

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,api,consistency

## Description
The current `sayCodeModel(Method)` method is confusing because `sayCodeModel(Class<?>)` documents the entire class structure (sealed hierarchy, methods, signatures), while `sayCodeModel(Method)` only shows the method signature. This inconsistency makes the API harder to learn and use.

We need to introduce a new, clearly-named method `sayMethodSignature(Method)` that better describes what it does, and deprecate the old `sayCodeModel(Method)` method.

## Acceptance Criteria
- [ ] Add new `sayMethodSignature(Method)` method to `DtrContext` class
- [ ] Implement new method with identical functionality to current `sayCodeModel(Method)`
- [ ] Deprecate `sayCodeModel(Method method)` with `@Deprecated(forRemoval=true)` and javadoc noting replacement
- [ ] Update all internal call sites from `sayCodeModel(method)` to `sayMethodSignature(method)`
- [ ] Update comprehensive API reference documentation in CLAUDE.md
- [ ] Add migration note in javadoc for deprecated method
- [ ] Plan removal for DTR 3.0.0

## Technical Notes

### Current API Location
- **File**: `/Users/sac/dtr/src/main/java/com/sac/Documentation.java` or `/Users/sac/dtr/src/main/java/com/sac/DtrContext.java`
- **Current Method Signature**:
  ```java
  void sayCodeModel(Method method)
  ```

### New Method Signature
```java
/**
 * Documents a method's signature including parameter types, return type, and modifiers.
 * Provides structured signature documentation with proper formatting.
 *
 * @param method the method to document
 */
void sayMethodSignature(Method method)
```

### Deprecation Pattern
```java
/**
 * @deprecated Use {@link #sayMethodSignature(Method)} instead.
 * This method will be removed in DTR 3.0.0.
 */
@Deprecated(forRemoval=true)
void sayCodeModel(Method method) {
    sayMethodSignature(method);
}
```

### Internal Call Sites to Update
Search for:
```bash
grep -r "sayCodeModel.*Method" /Users/sac/dtr/src/
```

Expected locations:
- Test files demonstrating method documentation
- Examples in `/Users/sac/dtr/src/test/`
- Documentation tests showing method signatures

### CLAUDE.md Update Required
Update the **Code Model (Reflection-Based)** section in `/Users/sac/dtr/CLAUDE.md`:

**BEFORE**:
| Method | Signature | Description |
|--------|-----------|-------------|
| `sayCodeModel` | `void sayCodeModel(Method method)` | Method structure with Java 26 Code Reflection |

**AFTER**:
| Method | Signature | Description |
|--------|-----------|-------------|
| `sayMethodSignature` | `void sayMethodSignature(Method method)` | Method signature with parameter types, return type, modifiers |
| `sayCodeModel` | `void sayCodeModel(Method method)` | **@Deprecated** - Use `sayMethodSignature(Method)` instead |

## Dependencies
- None (standalone API improvement)

## Migration Impact
- **Breaking Change**: No (deprecation period before removal)
- **User Action Required**: Users should update call sites before DTR 3.0.0
- **Automated Migration**: IDE warnings will guide users to new method

## References
- Related to DTR-007 (parameter naming consistency)
- Part of 2026.4.0 DX/QoL release
- API consistency improvement

## Definition of Done
- [ ] All acceptance criteria met
- [ ] All tests pass (`mvnd verify`)
- [ ] No remaining internal uses of deprecated `sayCodeModel(Method)` overload
- [ ] Documentation updated in CLAUDE.md
- [ ] Deprecation warnings appear in IDE for old method calls
