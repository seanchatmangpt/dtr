# How-To: Generate Documentation Coverage with sayDocCoverage

Document which public methods and classes have associated documentation using DTR 2.6.0's `sayDocCoverage` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayDocCoverage Does

`sayDocCoverage(Class<?>... classes)` uses reflection to:
1. Enumerate all public methods in each class
2. Check for Javadoc or annotation-based documentation presence
3. Generate a coverage matrix showing documented vs. undocumented methods

This is the replacement for gRPC error code documentation guides, which relied on the removed HTTP stack. Documentation coverage is applicable to any Java class.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class ServiceDocCoverageDocTest {

    @Test
    void documentCoverage(DtrContext ctx) {
        ctx.sayNextSection("Service Layer Documentation Coverage");
        ctx.say("The following matrix shows which public methods have " +
                "associated documentation across the service layer.");

        ctx.sayDocCoverage(
            UserService.class,
            OrderService.class,
            ProductService.class
        );

        ctx.sayWarning("Methods marked MISSING require Javadoc before the next release.");
    }
}
```

---

## Coverage Report for a Single Class

```java
@Test
void documentUserServiceCoverage(DtrContext ctx) {
    ctx.sayNextSection("UserService Documentation Coverage");

    ctx.sayDocCoverage(UserService.class);

    ctx.sayNote("This report was generated on Java " +
                System.getProperty("java.version") + ".");
}
```

---

## Combine Coverage with Contract Verification

Get both coverage and contract fulfillment in one test:

```java
@Test
void fullServiceAudit(DtrContext ctx) {
    ctx.sayNextSection("Service Layer Audit");

    ctx.say("**1. Contract Verification** — Are all interface methods implemented?");
    ctx.sayContractVerification(
        UserService.class,
        UserServiceImpl.class
    );

    ctx.say("**2. Documentation Coverage** — Are all public methods documented?");
    ctx.sayDocCoverage(UserServiceImpl.class);

    ctx.say("**3. Call Graph** — What is the internal call structure?");
    ctx.sayCallGraph(UserServiceImpl.class);
}
```

---

## Document Exception Handling Coverage

Combine `sayDocCoverage` with `sayException` to show both coverage and what exceptions are thrown:

```java
@Test
void documentExceptionHandling(DtrContext ctx) {
    ctx.sayNextSection("Error Handling Documentation");
    ctx.say("Service methods throw structured exceptions for known error conditions:");

    // Document the exceptions
    try {
        throw new IllegalArgumentException("userId must be positive");
    } catch (IllegalArgumentException e) {
        ctx.sayException(e);
    }

    try {
        throw new java.util.NoSuchElementException("User 42 not found");
    } catch (java.util.NoSuchElementException e) {
        ctx.sayException(e);
    }

    ctx.say("Documentation coverage for the service layer:");
    ctx.sayDocCoverage(UserService.class);
}
```

---

## Track Coverage Over Time

Use `sayEvolutionTimeline` alongside `sayDocCoverage` to show documentation improvement over commits:

```java
@Test
void trackDocumentationEvolution(DtrContext ctx) {
    ctx.sayNextSection("Documentation Coverage Evolution");

    ctx.say("Git history of UserService documentation improvements:");
    ctx.sayEvolutionTimeline(UserService.class, 10);

    ctx.say("Current coverage snapshot:");
    ctx.sayDocCoverage(UserService.class);
}
```

---

## Best Practices

**Run coverage reports in CI.** Add `sayDocCoverage` to a dedicated documentation audit test that runs on every pull request.

**Set a coverage threshold.** Define a minimum coverage percentage in your team's definition of done. Use the generated report to enforce it in code review.

**Prioritize public API methods.** Focus on documenting methods that appear in interfaces. Internal private methods are lower priority.

**Combine with sayContractVerification.** An undocumented method that doesn't implement a contract is a double problem. Check both together.

---

## See Also

- [Verify Interface Contracts](grpc-unary.md) — sayContractVerification
- [Document Call Graphs](grpc-streaming.md) — sayCallGraph
- [Use sayEnvProfile](use-cookies.md) — Environment snapshot with coverage audit
- [Document Exception Handling](test-xml-endpoints.md) — sayException
