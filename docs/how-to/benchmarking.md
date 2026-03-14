# How-To: Benchmarking with DTR

Measure real performance using DTR 2.6.0's built-in `sayBenchmark` method and `System.nanoTime()`.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Core Principle: Real Measurements Only

- No simulation, no fakes, no hard-coded numbers
- Measure with `sayBenchmark` or `System.nanoTime()` on real execution
- Report: metric + units + Java version + iterations + environment
- Example: `"String concat: 78ns avg (10M iterations, Java 26.0.2, 8 cores)"`

---

## Quick Start: sayBenchmark

`sayBenchmark(String label, Runnable task)` is the primary benchmarking method in DTR 2.6.0. It runs the task with configurable warmup and measurement rounds, then documents the results automatically.

```java
@ExtendWith(DtrExtension.class)
class StringBenchmarkDocTest {

    @Test
    void benchmarkStringOps(DtrContext ctx) {
        ctx.sayNextSection("String Performance");
        ctx.sayEnvProfile();

        ctx.sayBenchmark("String concatenation (1000 appends)", () -> {
            String s = "";
            for (int i = 0; i < 1000; i++) s += i;
        });

        ctx.sayBenchmark("StringBuilder (1000 appends)", () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1000; i++) sb.append(i);
            sb.toString();
        });

        ctx.sayBenchmark("String.join (1000 elements)", () -> {
            String.join("", java.util.Collections.nCopies(1000, "x"));
        });
    }
}
```

`sayBenchmark` outputs a timing table to the documentation showing average, min, max, and standard deviation across measurement rounds.

---

## Configurable Warmup and Measurement Rounds

For more precise measurements, use the four-argument overload:

```java
@Test
void precisionBenchmark(DtrContext ctx) {
    ctx.sayNextSection("Precision Benchmark");

    // sayBenchmark(label, task, warmupRounds, measureRounds)
    ctx.sayBenchmark("ArrayList add (1000 elements)", () -> {
        var list = new java.util.ArrayList<Integer>();
        for (int i = 0; i < 1000; i++) list.add(i);
    }, 10, 100);

    ctx.sayBenchmark("LinkedList add (1000 elements)", () -> {
        var list = new java.util.LinkedList<Integer>();
        for (int i = 0; i < 1000; i++) list.add(i);
    }, 10, 100);
}
```

---

## Manual Measurement with System.nanoTime()

For custom statistics or multi-step measurements, use `System.nanoTime()` directly:

```java
@Test
void manualBenchmark(DtrContext ctx) {
    ctx.sayNextSection("Manual Timing");

    final int iterations = 1000;
    long[] measurements = new long[iterations];

    // Warmup
    for (int i = 0; i < 100; i++) {
        String.valueOf(i);
    }

    // Measure
    for (int i = 0; i < iterations; i++) {
        long start = System.nanoTime();
        String.valueOf(i);
        measurements[i] = System.nanoTime() - start;
    }

    // Calculate statistics
    java.util.Arrays.sort(measurements);
    long avg = java.util.Arrays.stream(measurements).sum() / measurements.length;
    long min = measurements[0];
    long max = measurements[measurements.length - 1];
    long p95 = measurements[(int)(measurements.length * 0.95)];
    double mean = avg;
    double variance = java.util.Arrays.stream(measurements)
        .mapToDouble(x -> Math.pow(x - mean, 2)).average().orElse(0);
    double stdDev = Math.sqrt(variance);

    ctx.sayKeyValue(java.util.Map.of(
        "Operation", "String.valueOf(int)",
        "Iterations", String.valueOf(iterations),
        "Average", avg + " ns",
        "Min", min + " ns",
        "Max", max + " ns",
        "P95", p95 + " ns",
        "Std Dev", String.format("%.1f ns", stdDev),
        "Java Version", System.getProperty("java.version")
    ));
}
```

---

## Benchmarking Virtual Thread Throughput

```java
@Test
void benchmarkVirtualThreads(DtrContext ctx) throws Exception {
    ctx.sayNextSection("Virtual Thread Throughput");
    ctx.sayEnvProfile();

    final int taskCount = 1000;

    // Platform threads
    long startPlatform = System.nanoTime();
    try (var executor = java.util.concurrent.Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors())) {
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < taskCount; i++) {
            futures.add(executor.submit(() -> {
                Thread.sleep(1);
                return null;
            }));
        }
        for (var f : futures) f.get();
    }
    long platformNs = System.nanoTime() - startPlatform;

    // Virtual threads
    long startVirtual = System.nanoTime();
    try (var executor = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
        for (int i = 0; i < taskCount; i++) {
            futures.add(executor.submit(() -> {
                Thread.sleep(1);
                return null;
            }));
        }
        for (var f : futures) f.get();
    }
    long virtualNs = System.nanoTime() - startVirtual;

    ctx.sayTable(new String[][] {
        {"Executor", "Total Time", "Per Task", "Tasks"},
        {"Platform threads", platformNs / 1_000_000 + " ms",
            platformNs / taskCount + " ns", String.valueOf(taskCount)},
        {"Virtual threads", virtualNs / 1_000_000 + " ms",
            virtualNs / taskCount + " ns", String.valueOf(taskCount)}
    });

    ctx.say("Java " + System.getProperty("java.version") +
            " | Cores: " + Runtime.getRuntime().availableProcessors());
}
```

---

## Benchmark Documentation Coverage

Combine `sayBenchmark` with `sayDocCoverage` to document both performance and coverage:

```java
@Test
void benchmarkAndCover(DtrContext ctx) {
    ctx.sayNextSection("Record Serialization Performance");

    record Point(double x, double y, double z) {}

    ctx.sayRecordComponents(Point.class);

    ctx.sayBenchmark("Point toString (10000 calls)", () -> {
        var p = new Point(1.0, 2.0, 3.0);
        for (int i = 0; i < 10000; i++) p.toString();
    });

    ctx.sayDocCoverage(Point.class);
}
```

---

## ASCII Chart of Benchmark Results

Visualize benchmark results as a bar chart:

```java
@Test
void chartBenchmarkResults(DtrContext ctx) {
    ctx.sayNextSection("Collection Performance Chart");

    String[] labels = {"ArrayList", "LinkedList", "ArrayDeque", "TreeSet"};
    double[] timesNs = new double[labels.length];

    // Measure each
    for (int c = 0; c < labels.length; c++) {
        long start = System.nanoTime();
        for (int i = 0; i < 10_000; i++) {
            switch (c) {
                case 0 -> new java.util.ArrayList<>().add(i);
                case 1 -> new java.util.LinkedList<>().add(i);
                case 2 -> new java.util.ArrayDeque<>().add(i);
                case 3 -> new java.util.TreeSet<>().add(i);
            }
        }
        timesNs[c] = (double)(System.nanoTime() - start) / 10_000;
    }

    ctx.sayAsciiChart("Add operation latency (ns/op, lower is better)", timesNs, labels);
    ctx.say("Measured on Java " + System.getProperty("java.version") +
            ", " + Runtime.getRuntime().availableProcessors() + " cores");
}
```

---

## Best Practices

### Always warm up the JVM

JIT compilation changes timings significantly. Always run warmup iterations:

```java
// Warmup (not measured)
for (int i = 0; i < 1000; i++) {
    yourOperation();
}

// Then measure
ctx.sayBenchmark("Your operation", () -> yourOperation());
```

### Report environment context

```java
ctx.sayEnvProfile(); // Always call this before benchmarks
```

### Use sayBenchmark for standard cases

`sayBenchmark` handles warmup, measurement, and statistics automatically. Only drop to `System.nanoTime()` when you need custom multi-step control.

### Do not hard-code results

```java
// WRONG — no real measurement
ctx.say("Performance: 500 ns");

// CORRECT — real measurement
ctx.sayBenchmark("Your operation", () -> yourOperation());
```

---

## Interpreting Results

Typical `sayBenchmark` output:

| Operation | Avg | Min | Max | Std Dev | Rounds |
|-----------|-----|-----|-----|---------|--------|
| StringBuilder append | 234 ns | 198 ns | 567 ns | 45 ns | 100 |

- **Avg**: Mean time per invocation across all measurement rounds
- **Min/Max**: Best and worst single measurement
- **Std Dev**: Measurement variability — lower is more stable
- **Rounds**: Number of measurement rounds (after warmup)

---

See Also:
- [Performance Tuning](performance-tuning.md) — Reduce build time and profiling strategies
- [Use Virtual Threads](use-virtual-threads.md) — Virtual thread concurrency patterns
- [sayAsciiChart via Timeline](sse-parsing.md) — ASCII chart documentation
