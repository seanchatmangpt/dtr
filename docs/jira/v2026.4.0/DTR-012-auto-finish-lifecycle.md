# DTR-012: Auto-Finish Lifecycle with @AutoFinishDocTest Annotation

**Priority**: P4
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx, qol, test-authoring, annotations, lifecycle

## Description

Eliminate the need for manual `@AfterAll` lifecycle methods to call `finishAndWriteOut()` by introducing an `@AutoFinishDocTest` annotation that automatically handles documentation finalization.

Currently, users must write boilerplate code in every test class:

```java
@ExtendWith(DtrExtension.class)
class MyTest {
    @AfterAll
    static void cleanup(DtrContext ctx) {
        ctx.finishAndWriteOut();
    }
}
```

This ticket introduces an annotation that automatically invokes `finishAndWriteOut()` after all tests in a class complete, reducing boilerplate and eliminating a common source of errors (forgetting to call cleanup).

## Acceptance Criteria

- [ ] Create `@AutoFinishDocTest` annotation in `io.github.seanchatmangpt.dtr.junit5` package
- [ ] Annotation should be meta-annotated with `@Retention(RetentionPolicy.RUNTIME)` and `@Target(ElementType.TYPE)`
- [ ] Modify `DtrExtension` to detect `@AutoFinishDocTest` on test classes
- [ ] When detected, automatically register an `AfterAllCallback` that calls `finishAndWriteOut()`
- [ ] Ensure automatic cleanup runs even if tests fail (use `@AfterAll` semantics)
- [ ] Add comprehensive unit tests verifying auto-finish behavior
- [ ] Add integration tests with various test scenarios (passing, failing, mixed)
- [ ] Update javadoc for `DtrExtension` to document `@AutoFinishDocTest` usage
- [ ] Add example to tutorials showing simplified test setup
- [ ] Ensure backward compatibility - tests without annotation work as before

## Technical Notes

### File Paths

- **New annotation**: `src/main/java/io/github/seanchatmangpt/dtr/junit5/AutoFinishDocTest.java`
- **Modified extension**: `src/main/java/io/github/seanchatmangpt/dtr/junit5/DtrExtension.java`
- **Unit tests**: `src/test/java/io/github/seanchatmangpt/dtr/junit5/AutoFinishDocTestTest.java`

### Implementation Details

```java
package io.github.seanchatmangpt.dtr.junit5;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Automatically finishes documentation output after all tests complete.
 * Eliminates need for manual @AfterAll cleanup methods.
 *
 * <p>Usage:
 * <pre>
 * &#64;ExtendWith(DtrExtension.class)
 * &#64;AutoFinishDocTest
 * class MyTest {
 *     // No @AfterAll needed!
 *     &#64;Test
 *     void testSomething(DtrContext ctx) {
 *         ctx.say("Documentation here");
 *     }
 * }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AutoFinishDocTest {
}
```

### DtrExtension Modifications

```java
@Override
public void beforeAll(ExtensionContext context) throws Exception {
    // Check for @AutoFinishDocTest
    Class<?> testClass = context.getRequiredTestClass();
    if (testClass.isAnnotationPresent(AutoFinishDocTest.class)) {
        // Register automatic AfterAll callback
        context.getRoot().getStore(NAMESPACE)
            .get("autoFinish-" + context.getUniqueId(), AutoFinishCallback.class);
    }
}
```

### Test Scenarios

1. **Normal completion**: Tests pass → auto-finish runs → docs generated
2. **Test failure**: Test fails → auto-finish still runs → docs include failure info
3. **Multiple test classes**: Each class with annotation gets separate cleanup
4. **No annotation**: Tests work as before (backward compatibility)
5. **Manual cleanup conflict**: If both annotation and `@AfterAll` present → annotation wins (document behavior)

## Dependencies

- **DTR-011** (Test Runner Callbacks) - Must complete first as this builds on the callback infrastructure

## References

- Related to: `@DtrTest` unified annotation (DTR-009)
- Similar to: JUnit 5's built-in lifecycle callbacks
- Documentation: `docs/tutorials/basics.md` (add simplified example)
- API Reference: Update `docs/reference/dtr-test-api.md` with new annotation

## Success Metrics

- Reduces test class boilerplate by 3-5 lines per class
- Eliminates "missing cleanup" as a source of bugs
- Maintains 100% backward compatibility
- Zero performance overhead (same lifecycle timing as manual cleanup)
