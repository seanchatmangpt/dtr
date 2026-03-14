# How-To: Render ASCII Charts with sayAsciiChart

Generate Unicode bar charts from numerical data using DTR 2.6.0's `sayAsciiChart` method.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## What sayAsciiChart Does

`sayAsciiChart(String label, double[] values, String[] xLabels)` renders a Unicode horizontal bar chart directly in the documentation. Each bar represents one value with its label. The chart is proportionally scaled to the maximum value.

Use it to visualize benchmark results, comparison data, test coverage percentages, and any other numerical series.

---

## Basic Usage

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class CollectionBenchmarkDocTest {

    @Test
    void chartCollectionPerformance(DtrContext ctx) {
        ctx.sayNextSection("Collection Add Performance");
        ctx.sayEnvProfile();

        String[] labels = {"ArrayList", "LinkedList", "ArrayDeque", "HashSet", "TreeSet"};
        double[] timesNs = new double[labels.length];

        // Warmup
        for (int w = 0; w < 100; w++) {
            new java.util.ArrayList<>().add(0);
        }

        // Measure
        for (int c = 0; c < labels.length; c++) {
            final int idx = c;
            long start = System.nanoTime();
            for (int i = 0; i < 100_000; i++) {
                switch (idx) {
                    case 0 -> new java.util.ArrayList<>().add(i);
                    case 1 -> new java.util.LinkedList<>().add(i);
                    case 2 -> new java.util.ArrayDeque<>().add(i);
                    case 3 -> new java.util.HashSet<>().add(i);
                    case 4 -> new java.util.TreeSet<>().add(i);
                }
            }
            timesNs[c] = (double)(System.nanoTime() - start) / 100_000;
        }

        ctx.sayAsciiChart("Add operation: ns/op (lower is better)", timesNs, labels);
        ctx.say("Measured on Java " + System.getProperty("java.version") +
                ", " + Runtime.getRuntime().availableProcessors() + " cores.");
    }
}
```

---

## Visualize Benchmark Results

Combine `sayBenchmark` with `sayAsciiChart` — run the detailed benchmark first, then visualize a summary:

```java
@Test
void benchmarkAndChart(DtrContext ctx) {
    ctx.sayNextSection("String Formatting Performance");
    ctx.sayEnvProfile();

    String[] operations = {
        "String.format",
        "String.formatted()",
        "StringBuilder",
        "String.valueOf + +"
    };

    double[] avgNs = new double[operations.length];
    int iterations = 100_000;

    // Measure each
    avgNs[0] = measureNs(iterations, () -> String.format("Hello %s, you are %d", "Alice", 30));
    avgNs[1] = measureNs(iterations, () -> "Hello %s, you are %d".formatted("Alice", 30));
    avgNs[2] = measureNs(iterations, () -> {
        new StringBuilder().append("Hello ").append("Alice").append(", you are ").append(30).toString();
    });
    avgNs[3] = measureNs(iterations, () -> "Hello " + "Alice" + ", you are " + 30);

    // Detailed benchmarks
    for (int i = 0; i < operations.length; i++) {
        final int idx = i;
        ctx.sayBenchmark(operations[idx] + " (" + iterations + " iterations)", () -> {
            switch (idx) {
                case 0 -> String.format("Hello %s, you are %d", "Alice", 30);
                case 1 -> "Hello %s, you are %d".formatted("Alice", 30);
                case 2 -> new StringBuilder().append("Hello ").append("Alice").append(30).toString();
                case 3 -> "Hello " + "Alice" + ", you are " + 30;
            }
        });
    }

    // Summary chart
    ctx.sayAsciiChart("String formatting avg ns/op (lower is better)", avgNs, operations);
}

private double measureNs(int iterations, Runnable r) {
    // Warmup
    for (int i = 0; i < 100; i++) r.run();
    long start = System.nanoTime();
    for (int i = 0; i < iterations; i++) r.run();
    return (double)(System.nanoTime() - start) / iterations;
}
```

---

## Chart Documentation Coverage Percentages

Visualize coverage data across multiple classes:

```java
@Test
void chartDocumentationCoverage(DtrContext ctx) {
    ctx.sayNextSection("Documentation Coverage by Module");

    // Hypothetical coverage percentages derived from sayDocCoverage analysis
    String[] modules = {"UserService", "OrderService", "PaymentService", "ProductService", "ReportService"};
    double[] coverage = {87.5, 72.3, 95.1, 60.0, 44.8};

    ctx.sayAsciiChart("Documentation coverage % (higher is better)", coverage, modules);

    ctx.sayWarning("Modules below 70% require documentation before the next release. " +
                   "ProductService and ReportService are blocked.");
}
```

---

## Chart Trend Data

Use `sayAsciiChart` to show trends over time (data gathered from CI builds):

```java
@Test
void chartBuildTrend(DtrContext ctx) {
    ctx.sayNextSection("Test Suite Build Time Trend");

    String[] builds = {"2025-01", "2025-04", "2025-07", "2025-10", "2026-01", "2026-03"};
    double[] buildTimeSec = {145.2, 138.7, 127.4, 118.9, 103.2, 94.6};

    ctx.sayAsciiChart("Build time in seconds (lower is better)", buildTimeSec, builds);
    ctx.say("Build time has decreased by " +
            String.format("%.0f%%", (1 - buildTimeSec[5] / buildTimeSec[0]) * 100) +
            " since January 2025, driven by test parallelization and mvnd caching.");
}
```

---

## Best Practices

**Always measure before charting.** Never hard-code values into `sayAsciiChart`. All values must come from real measurements via `System.nanoTime()`, CI metrics, or reflection.

**Add context with `say()`.** The chart shows relative differences; use prose to explain the absolute values and their significance.

**Use `sayEnvProfile()` before benchmark charts.** Timing data is meaningless without knowing the hardware and Java version.

**Pair with `sayBenchmark` for detail.** `sayAsciiChart` gives the visual summary; `sayBenchmark` provides the detailed statistics. Use both in the same test.

---

## See Also

- [Benchmark Inline](sse-reconnection.md) — sayBenchmark for detailed statistics
- [Document Evolution Timeline](sse-subscription.md) — sayEvolutionTimeline for git history
- [Benchmarking (full guide)](benchmarking.md) — Complete benchmarking workflow
