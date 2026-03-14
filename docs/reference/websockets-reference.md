# Reference: Coverage and Contract API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.6.0 (new in this release)

DTR 2.6.0 introduces three methods for documenting code quality: documentation coverage, contract verification, and API evolution timelines.

---

## sayDocCoverage

```java
ctx.sayDocCoverage(Class<?>... classes)
```

Analyzes one or more classes and renders a documentation coverage report. The report shows which public methods have been exercised by at least one `say*` call in any DocTest in the project.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `classes` | `Class<?>...` | One or more classes to check |

**Output:** A table with columns: Class, Method, Signature, Documented.

```java
@Test
void checkCoverage(DtrContext ctx) {
    ctx.sayNextSection("Documentation Coverage Report");
    ctx.sayDocCoverage(
        UserService.class,
        OrderService.class,
        PaymentService.class
    );
}
```

**Example output table:**

| Class | Method | Documented |
|-------|--------|-----------|
| `UserService` | `findById(long)` | Yes |
| `UserService` | `deleteById(long)` | No |
| `OrderService` | `processOrder(Order)` | Yes |

Undocumented methods are highlighted. Use this report to identify API surface that lacks documentation tests.

---

## sayContractVerification

```java
ctx.sayContractVerification(Class<?> impl, Class<?>... contracts)
```

Verifies that an implementation class fulfills each contract interface and renders a compliance table.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `impl` | `Class<?>` | The implementation class to verify |
| `contracts` | `Class<?>...` | One or more interface or abstract class contracts |

**Output:** A table with columns: Contract, Method, Present in Impl, Documented.

```java
@Test
void verifyRenderMachineContract(DtrContext ctx) {
    ctx.sayNextSection("RenderMachine Contract Verification");
    ctx.sayContractVerification(
        RenderMachineImpl.class,
        RenderMachine.class
    );
}
```

**Example output:**

| Contract | Method | Present | Documented |
|----------|--------|---------|-----------|
| `RenderMachine` | `say(String)` | Yes | Yes |
| `RenderMachine` | `sayNextSection(String)` | Yes | Yes |
| `RenderMachine` | `sayBenchmark(String,Runnable)` | Yes | No |

---

## sayEvolutionTimeline

```java
ctx.sayEvolutionTimeline(Class<?> clazz, int versionCount)
```

Reads git history and renders a timeline table showing when public methods were added, removed, or changed across the most recent `versionCount` tagged releases.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `clazz` | `Class<?>` | The class to track |
| `versionCount` | `int` | Number of past versions to include |

**Output:** A Mermaid `timeline` diagram plus a table with columns: Version, Method, Change.

```java
@Test
void showApiEvolution(DtrContext ctx) {
    ctx.sayNextSection("DtrContext API Evolution");
    ctx.sayEvolutionTimeline(DtrContext.class, 5);
}
```

**Example output (timeline):**

```
timeline
    title DtrContext API Changes
    v2.2.0 : sayCode added
    v2.3.0 : sayCodeModel added
             sayControlFlowGraph added
    v2.4.0 : sayCallSite added
             sayAnnotationProfile added
    v2.6.0 : sayBenchmark added
             sayMermaid added
             sayDocCoverage added
```

**Requirements:** The project must be a git repository with version tags following semver (e.g., `v2.5.0`). If git history is unavailable, the method renders a warning and skips the timeline.

---

## Combining coverage methods

Best practice: run all three coverage methods together in a dedicated quality DocTest:

```java
@ExtendWith(DtrExtension.class)
class QualityDocTest {

    @Test
    void documentationCoverage(DtrContext ctx) {
        ctx.sayNextSection("Documentation Coverage");
        ctx.sayDocCoverage(
            UserService.class,
            OrderService.class
        );
    }

    @Test
    void contractCompliance(DtrContext ctx) {
        ctx.sayNextSection("Contract Compliance");
        ctx.sayContractVerification(RenderMachineImpl.class, RenderMachine.class);
        ctx.sayContractVerification(BlogRenderMachine.class, RenderMachine.class);
    }

    @Test
    void apiHistory(DtrContext ctx) {
        ctx.sayNextSection("API Evolution (last 5 releases)");
        ctx.sayEvolutionTimeline(DtrContext.class, 5);
    }
}
```

---

## See also

- [say* Core API Reference](request-api.md) — all 37 method signatures
- [RenderMachine API Reference](response-api.md) — output implementations
- [Mermaid Diagram API Reference](http-constants.md) — diagram rendering
