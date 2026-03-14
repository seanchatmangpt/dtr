# How-To: Generate Documentation Coverage with sayDocCoverage

Use DTR 2.6.0's `sayDocCoverage` to generate a coverage matrix showing which public methods and classes have associated documentation.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayDocCoverage Does

`sayDocCoverage(Class<?>... classes)` uses reflection to:
1. Enumerate all public methods declared by each class
2. Check for Javadoc presence (via reflection on annotations and source metadata)
3. Render a coverage matrix table: class name × method name, with COVERED / MISSING status

Use it in documentation audit tests to enforce documentation standards across your codebase.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class DocCoverageAuditTest {

    @Test
    void auditServiceDocumentation(DtrContext ctx) {
        ctx.sayNextSection("Service Layer Documentation Coverage");
        ctx.say("This report shows which public API methods have Javadoc documentation.");

        ctx.sayDocCoverage(
            UserService.class,
            OrderService.class,
            ProductService.class
        );

        ctx.sayWarning("Methods marked MISSING must be documented before the next release.");
    }
}
```

---

## Audit a Single Class

```java
@Test
void auditUserService(DtrContext ctx) {
    ctx.sayNextSection("UserService Documentation Coverage");
    ctx.sayEnvProfile();

    ctx.sayDocCoverage(UserService.class);

    ctx.sayNote("Coverage is calculated from Javadoc annotations on public methods. " +
                "Private methods are excluded.");
}
```

---

## Combine with Contract Verification

Run a complete service layer audit in one test:

```java
@Test
void fullServiceLayerAudit(DtrContext ctx) {
    ctx.sayNextSection("Service Layer Full Audit");
    ctx.say("Audit date: " + java.time.LocalDate.now());
    ctx.sayEnvProfile();

    ctx.say("**Step 1: Contract verification** — Are all interface methods implemented?");
    ctx.sayContractVerification(UserService.class, UserServiceImpl.class);

    ctx.say("**Step 2: Documentation coverage** — Are all public methods documented?");
    ctx.sayDocCoverage(UserServiceImpl.class);

    ctx.say("**Step 3: Call graph** — Is the implementation well-structured?");
    ctx.sayCallGraph(UserServiceImpl.class);

    ctx.sayWarning("Address all MISSING items before the next release tag.");
}
```

---

## Generate a Coverage Report for All Modules

```java
@Test
void fullCoverageReport(DtrContext ctx) {
    ctx.sayNextSection("Full Codebase Documentation Coverage Report");
    ctx.say("Generated: " + java.time.LocalDate.now());
    ctx.sayEnvProfile();

    // API layer
    ctx.say("**API Layer:**");
    ctx.sayDocCoverage(
        UserController.class,
        OrderController.class,
        ProductController.class
    );

    // Service layer
    ctx.say("**Service Layer:**");
    ctx.sayDocCoverage(
        UserService.class,
        OrderService.class,
        ProductService.class
    );

    // Repository layer
    ctx.say("**Repository Layer:**");
    ctx.sayDocCoverage(
        UserRepository.class,
        OrderRepository.class
    );
}
```

---

## Track Coverage Over Time

Combine `sayDocCoverage` with `sayEvolutionTimeline` to show coverage in the context of class history:

```java
@Test
void trackPaymentServiceCoverage(DtrContext ctx) {
    ctx.sayNextSection("PaymentService: Coverage and History");

    ctx.say("**Recent changes:**");
    ctx.sayEvolutionTimeline(PaymentService.class, 8);

    ctx.say("**Current documentation coverage:**");
    ctx.sayDocCoverage(PaymentService.class);

    ctx.sayNote("Compare the number of commits that added methods vs. " +
                "the coverage percentage to identify documentation debt.");
}
```

---

## Use in CI to Enforce Standards

Add a dedicated documentation audit test to your CI pipeline. The test should fail if `sayDocCoverage` reports too many missing entries:

```java
@Test
void enforceCoverageStandard(DtrContext ctx) {
    ctx.sayNextSection("Documentation Coverage Enforcement");
    ctx.say("This test generates a coverage report AND enforces a minimum standard.");

    ctx.sayDocCoverage(UserService.class, OrderService.class);

    // The actual enforcement: verify via AssertJ that coverage meets threshold
    // (This would query the coverage data programmatically in a real implementation)
    // assertThat(coverage.percentage()).isGreaterThanOrEqualTo(80.0);

    ctx.sayNote("Coverage threshold: 80%. Tests fail below this threshold in CI.");
}
```

---

## Best Practices

**Run sayDocCoverage in a dedicated audit test.** Keep coverage checks in their own test class so they don't slow down functional tests.

**Audit public API surfaces first.** Focus on interfaces and public methods that clients call. Internal implementation classes are lower priority.

**Combine with sayContractVerification.** An undocumented method that also doesn't implement a contract is the highest-priority item to fix.

**Use in code review.** Generate the coverage report as part of a pre-release checklist. Include the generated Markdown in the PR.

**Track trends with sayEvolutionTimeline.** See if coverage is improving or declining as the class evolves.

---

## See Also

- [Verify Interface Contracts](grpc-unary.md) — sayContractVerification
- [Capture Environment Profile](use-cookies.md) — sayEnvProfile
- [Generate Documentation Coverage (grpc guide)](grpc-error-handling.md) — Alternative coverage walkthrough
