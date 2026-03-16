# DTR-023: Migrate to Native CodeReflection Compilation

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,java26,code-reflection,performance

## Description
Migrate from reflective `method.codeModel()` calls to native compilation approach for improved performance and better integration with Java 26's Code Reflection (JEP 516). This change will reduce the overhead of reflective access and provide more direct access to method code models.

## Acceptance Criteria
- [ ] Update CodeModelAnalyzer.java to use native compilation instead of reflective access
- [ ] Replace all reflective codeModel() invocations with direct compilation approach
- [ ] Add performance benchmarks showing improvement (measure with System.nanoTime())
- [ ] Update documentation for new approach in relevant test files
- [ ] All existing tests pass with new implementation
- [ ] Code coverage remains at 100%

## Technical Notes
**Primary File**: `src/main/java/org/sperlogar/dte/CodeModelAnalyzer.java`

**Current Pattern**:
```java
// Reflective approach (current)
method.codeModel()
```

**Target Pattern**:
```java
// Native compilation approach (target)
// Direct compilation and analysis without reflection
```

**Benchmarks Required**:
- Measure: System.nanoTime() before/after comparison
- Report: metric + units + Java version + iterations + environment
- Example: "Native CodeReflection: Xms avg (Y iterations, Java 26.ea.13)"

**Reference**: JEP 516 Code Reflection

## Dependencies
None - this ticket can be implemented independently

## References
- [JEP 516: Code Reflection](https://openjdk.org/jeps/516)
- [DTR Architecture Documentation](/Users/sac/dtr/docs/architecture.md)
- Related: DTR-024 (Pattern Matching), DTR-025 (Virtual Threads)
