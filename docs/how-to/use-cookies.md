# How-To: Capture Environment Profiles with sayEnvProfile

Document the runtime environment of your tests using DTR 2.6.0's `sayEnvProfile` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayEnvProfile Does

`sayEnvProfile()` captures a snapshot of the current runtime environment and renders it as a key-value table. It includes:

- Java version (`java.version`, `java.vendor`)
- JVM implementation (HotSpot, GraalVM, etc.)
- Operating system name and version
- Available processors (CPU cores)
- Heap configuration (max heap, current usage)
- DTR version
- Build timestamp

Use it at the start of any test that produces timing-sensitive results.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class EnvironmentProfileDocTest {

    @Test
    void captureEnvironment(DtrContext ctx) {
        ctx.sayNextSection("Test Environment");
        ctx.say("This documentation was generated in the following environment:");

        ctx.sayEnvProfile();

        ctx.sayNote("Benchmark results in this document are only valid for the environment shown above.");
    }
}
```

---

## Required Before Benchmarks

Always call `sayEnvProfile()` before reporting any timing measurements:

```java
@Test
void benchmarkWithEnvironmentContext(DtrContext ctx) {
    ctx.sayNextSection("String Concatenation Benchmark");

    // Required: document the environment first
    ctx.sayEnvProfile();

    ctx.sayBenchmark("StringBuilder (1000 appends)", () -> {
        var sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) sb.append(i);
    });

    ctx.sayBenchmark("String.join (1000 elements)", () -> {
        java.util.Collections.nCopies(1000, "x").stream().collect(
            java.util.stream.Collectors.joining());
    });
}
```

---

## Document CI vs Local Environments

Use `sayEnvProfile` to make CI/local environment differences explicit in documentation:

```java
@Test
void documentEnvironmentDifferences(DtrContext ctx) {
    ctx.sayNextSection("Build Environment");

    boolean isCI = System.getenv("CI") != null;
    ctx.say("Build type: " + (isCI ? "CI (automated)" : "Local (developer machine)"));

    ctx.sayEnvProfile();

    if (isCI) {
        ctx.sayNote("CI builds use a fixed cloud instance. " +
                    "Timing results are reproducible across runs.");
    } else {
        ctx.sayWarning("Local builds vary by developer machine. " +
                       "Do not compare timing results between local and CI builds.");
    }
}
```

---

## Combine with sayDocCoverage for a Full Audit Report

```java
@Test
void generateAuditReport(DtrContext ctx) {
    ctx.sayNextSection("Documentation Audit Report");
    ctx.say("Generated on: " + java.time.LocalDate.now());

    ctx.sayEnvProfile();

    ctx.say("**Documentation coverage across service layer:**");
    ctx.sayDocCoverage(
        UserService.class,
        OrderService.class,
        ProductService.class
    );

    ctx.sayNote("This report is generated automatically by the DTR audit test suite. " +
                "Run `mvnd test -Dtest=AuditDocTest` to refresh.");
}
```

---

## Capture Heap Information for Memory-Sensitive Tests

```java
@Test
void documentMemoryUsage(DtrContext ctx) {
    ctx.sayNextSection("Memory Profile");
    ctx.sayEnvProfile();

    var runtime = Runtime.getRuntime();
    long maxMb = runtime.maxMemory() / (1024 * 1024);
    long usedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);

    ctx.sayKeyValue(java.util.Map.of(
        "Max Heap (MB)", String.valueOf(maxMb),
        "Used Heap (MB)", String.valueOf(usedMb),
        "Processors", String.valueOf(runtime.availableProcessors())
    ));

    ctx.sayBenchmark("Large array allocation (10MB)", () -> {
        new byte[10 * 1024 * 1024];
    }, 5, 20);
}
```

---

## Best Practices

**Call sayEnvProfile() at the top of every benchmark test.** Timing results without environment context are not reproducible or comparable.

**Prefer sayEnvProfile() over manual System.getProperty() calls.** It captures more properties consistently without boilerplate.

**Include in CI reports.** When running documentation generation in CI, `sayEnvProfile()` creates a permanent record of what environment produced the documentation — valuable for debugging performance regressions.

**Combine with sayNote for caveat text.** After `sayEnvProfile()`, add a note explaining any environmental limitations: "Results are specific to this hardware. Your results may differ."

---

## See Also

- [Benchmarking](benchmarking.md) — Full benchmarking workflow with sayBenchmark
- [Generate Documentation Coverage](use-custom-headers.md) — sayDocCoverage
- [Performance Tuning](performance-tuning.md) — Build-level optimization strategies
