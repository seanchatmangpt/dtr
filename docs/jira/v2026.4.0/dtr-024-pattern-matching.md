# DTR-024: Replace instanceof Chains with Pattern Matching

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,java26,pattern-matching,code-quality

## Description
Replace all instanceof-and-cast chains with Java 26 pattern matching for improved readability, type safety, and reduced boilerplate across all render machine files. This modernization aligns with Java 26's enhanced pattern matching capabilities.

## Acceptance Criteria
- [ ] Identify all instanceof chains in render machine files
- [ ] Replace instanceof-and-cast patterns with pattern matching constructs
- [ ] Apply pattern guards where appropriate (`instanceof Type t && t.isValid()`)
- [ ] Verify type safety is maintained through all code paths
- [ ] Code coverage remains at 100%
- [ ] All existing tests pass
- [ ] Code readability improved (fewer lines, clearer intent)

## Technical Notes
**Target Files**:
- `src/main/java/org/sperlogar/dte/render/DocRenderMachine.java`
- `src/main/java/org/sperlogar/dte/render/SlideRenderMachine.java`
- `src/main/java/org/sperlogar/dte/render/BlogRenderMachine.java`
- Other render machine files in the same directory

**Current Pattern**:
```java
// Verbose instanceof-and-cast (current)
if (obj instanceof String) {
    String str = (String) obj;
    // use str
}
```

**Target Pattern**:
```java
// Pattern matching (target)
if (obj instanceof String str) {
    // use str directly
}

// With guards
if (obj instanceof Type t && t.isValid()) {
    // use t
}
```

**Search Pattern**:
```bash
# Find instanceof chains
grep -rn "instanceof" src/main/java/org/sperlogar/dte/render/
```

## Dependencies
None - this ticket can be implemented independently

## References
- [JEP 394: Pattern Matching for instanceof](https://openjdk.org/jeps/394)
- [JEP 405: Record Patterns (Preview)](https://openjdk.org/jeps/405)
- [JEP 420: Pattern Matching for switch (Second Preview)](https://openjdk.org/jeps/420)
- [DTR Code Style Guide](/Users/sac/dtr/docs/coding-standards.md)
- Related: DTR-023 (Native CodeReflection), DTR-025 (Virtual Threads)
