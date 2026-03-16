# DTR-007: Standardize Parameter Naming in Javadoc

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,api,consistency,documentation

## Description
The DtrContext API has inconsistent parameter naming in Javadoc across similar methods. This makes the API harder to learn and use because developers must remember different parameter names for conceptually similar operations.

This ticket focuses on **Javadoc-only changes** to standardize parameter names without modifying any method signatures (source/binary compatibility maintained).

## Acceptance Criteria
- [ ] Audit all `say*` methods in `DtrContext` for parameter naming consistency
- [ ] Standardize collection/array parameters: prefer `data/items/pairs`
- [ ] Standardize text content parameters: prefer `text/code/message`
- [ ] Standardize identifier parameters: prefer `label/name/key`
- [ ] Update Javadoc `@param` tags only (no method signature changes)
- [ ] Verify all Javadoc compiles cleanly with `mvnd javadoc:javadoc`
- [ ] Update CLAUDE.md method reference table if parameter names changed

## Technical Notes

### Standardized Parameter Naming Convention

| Parameter Type | Preferred Names | Examples |
|----------------|-----------------|----------|
| **Collection/Array** | `data`, `items`, `pairs` | `String[][] data`, `List<String> items`, `Map<String,String> pairs` |
| **Text Content** | `text`, `code`, `message` | `String text`, `String code`, `String message` |
| **Identifiers** | `label`, `name`, `key` | `String label`, `String name`, `String key` |
| **Objects** | `object`, `ref`, `clazz` | `Object object`, `DocTestRef ref`, `Class<?> clazz` |
| **Numbers** | `warmupRounds`, `measureRounds` | `int warmupRounds`, `int measureRounds` |

### Target Methods for Review

**Core Methods** (30+ methods to audit):
```java
void say(String text)                              // ✓ already consistent
void sayTable(String[][] data)                     // ✓ already consistent
void sayCode(String code, String language)         // ✓ already consistent
void sayKeyValue(Map<String, String> pairs)        // ✓ already consistent
void sayUnorderedList(List<String> items)          // ✓ already consistent
void sayOrderedList(List<String> items)            // ✓ already consistent
void sayWarning(String message)                    // ✓ already consistent
void sayNote(String message)                       // ✓ already consistent
```

**Methods to Review** (potential inconsistencies):
```java
void sayBenchmark(String label, ...)               // 'label' is consistent
void sayJson(Object object)                        // 'object' is consistent
void sayAssertions(Map<String, String> assertions) // 'assertions' → 'data'?
void sayRef(Class<?> docTestClass, String anchor)  // 'docTestClass' → 'clazz'?
void sayCite(String citationKey)                   // 'citationKey' → 'key'?
```

### Audit Process
```bash
# Find all say* methods
grep -n "void say" /Users/sac/dtr/src/main/java/com/sac/DtrContext.java

# Extract method signatures with @param tags
grep -A 10 "@param" /Users/sac/dtr/src/main/java/com/sac/DtrContext.java
```

### Javadoc Update Example

**BEFORE**:
```java
/**
 * Renders a key-value table.
 *
 * @param keyValuePairs the key-value pairs to render
 */
void sayKeyValue(Map<String, String> keyValuePairs);
```

**AFTER**:
```java
/**
 * Renders a key-value table.
 *
 * @param pairs the key-value pairs to render
 */
void sayKeyValue(Map<String, String> pairs);
```

**IMPORTANT**: Only update `@param` documentation. Do NOT change method signatures.

### File Location
- **Primary**: `/Users/sac/dtr/src/main/java/com/sac/DtrContext.java`
- **Secondary**: `/Users/sac/dtr/src/main/java/com/sac/DtrTest.java` (base test class)

## Dependencies
- None (documentation-only change)
- Complements DTR-006 (method naming consistency)

## Migration Impact
- **Breaking Change**: No (Javadoc only, no signature changes)
- **User Action Required**: None (documentation improvement only)
- **Source Compatibility**: Maintained (no API changes)
- **Binary Compatibility**: Maintained (no signature changes)

## Validation
```bash
# Verify Javadoc compiles
mvnd javadoc:javadoc

# Check for Javadoc warnings
mvnd javadoc:javadoc | grep -i "warning"

# Run full test suite
mvnd verify
```

## References
- Related to DTR-006 (method naming)
- Part of 2026.4.0 DX/QoL release
- API consistency improvement
- Javadoc standardization

## Definition of Done
- [ ] All acceptance criteria met
- [ ] Javadoc compiles without warnings
- [ ] All tests pass (`mvnd verify`)
- [ ] Parameter naming follows standardized convention
- [ ] CLAUDE.md updated if parameter documentation changed
- [ ] No source/binary compatibility issues introduced
