# DTR-002: Add Missing sayAndAssertThat to DtrContext

**Priority**: P1
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,api

## Description

The `sayAndAssertThat` methods are currently only available in the `DtrTest` base class, but not in `DtrContext`. This limits their usability for users who prefer dependency injection or custom base classes. This ticket adds the 4 overloaded `sayAndAssertThat` methods directly to `DtrContext` and writes comprehensive tests.

## Acceptance Criteria

- [ ] Add `sayAndAssertThat(String label, T actual, Matcher<? super T> matcher)` generic method to DtrContext
- [ ] Add `sayAndAssertThat(String label, long actual, Matcher<Long> matcher)` primitive long method to DtrContext
- [ ] Add `sayAndAssertThat(String label, int actual, Matcher<Integer> matcher)` primitive int method to DtrContext
- [ ] Add `sayAndAssertThat(String label, boolean actual, Matcher<Boolean> matcher)` primitive boolean method to DtrContext
- [ ] All methods delegate to existing `DtrTest` implementation to avoid duplication
- [ ] Write unit tests for all 4 method overloads
- [ ] Update API documentation (javadoc)
- [ ] Verify `mvnd verify` passes with new tests

## Technical Notes

### Implementation Location
- **File**: `/Users/sac/dtr/src/main/java/org/sparsevoid/dtr/DtrContext.java`
- **Pattern**: Delegate to `DtrTest.sayAndAssertThat` to maintain single source of truth

### Method Signatures
```java
// Generic version
public <T> void sayAndAssertThat(String label, T actual, Matcher<? super T> matcher)

// Primitive versions (avoid boxing)
public void sayAndAssertThat(String label, long actual, Matcher<Long> matcher)
public void sayAndAssertThat(String label, int actual, Matcher<Integer> matcher)
public void sayAndAssertThat(String label, boolean actual, Matcher<Boolean> matcher)
```

### Test Location
- **File**: `/Users/sac/dtr/src/test/java/org/sparsevoid/dtr/DtrContextTest.java`
- **Coverage**: Test each overload with different matcher types (equalTo, greaterThan, instanceOf, etc.)

### Dependencies
- `org.hamcrest:hamcrest` - Already in project dependencies
- `org.junit.jupiter:junit-jupiter` - Test framework

### Example Usage
```java
// Before (only in DtrTest)
public class MyTest extends DtrTest {
    @Test
    void test() {
        sayAndAssertThat("Status", response.getCode(), equalTo(200));
    }
}

// After (available in DtrContext via DI or composition)
public class MyTest {
    private final DtrContext ctx = new DtrContext();

    @Test
    void test() {
        ctx.sayAndAssertThat("Status", response.getCode(), equalTo(200));
    }
}
```

## Dependencies

- None (can be implemented independently)

## References

- DtrTest base class: `/Users/sac/dtr/src/test/java/org/sparsevoid/dtr/DtrTest.java`
- API Reference: See `CLAUDE.md` section "Assertion Combos (DtrTest Base Class)"
- Hamcrest matchers: https://hamcrest.org/JavaHamcrest/
