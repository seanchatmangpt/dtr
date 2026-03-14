# How-To: Inline Benchmarks with sayBenchmark

Use DTR 2.6.0's `sayBenchmark` to measure and document performance inline within any JUnit 5 test, without a separate benchmarking module.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Why sayBenchmark

The old approach required:
1. A separate JMH module
2. Build configuration
3. Manual result interpretation
4. Separate documentation

With `sayBenchmark`, you measure and document in one step — inside your existing DTR test. The results appear directly in the generated Markdown output.

---

## Basic: Single Operation

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class InlineBenchmarkDocTest {

    @Test
    void benchmarkHashMapLookup(DtrContext ctx) {
        ctx.sayNextSection("HashMap Lookup Performance");
        ctx.sayEnvProfile();

        var map = new java.util.HashMap<String, Integer>();
        for (int i = 0; i < 10_000; i++) {
            map.put("key" + i, i);
        }

        ctx.sayBenchmark("HashMap.get (10k entries, warm)", () -> {
            map.get("key5000");
        });
    }
}
```

`sayBenchmark` runs warmup rounds, then measurement rounds, and outputs a table with average, min, max, and standard deviation timing.

---

## Configurable Warmup and Measurement Rounds

```java
@Test
void precisionBenchmark(DtrContext ctx) {
    ctx.sayNextSection("Record Construction Performance");
    ctx.sayEnvProfile();

    record Point(double x, double y, double z) {}

    // 20 warmup rounds, 200 measurement rounds
    ctx.sayBenchmark("Point record construction", () -> {
        new Point(1.0, 2.0, 3.0);
    }, 20, 200);

    ctx.sayBenchmark("Point record access", () -> {
        var p = new Point(1.0, 2.0, 3.0);
        p.x(); p.y(); p.z();
    }, 20, 200);
}
```

---

## Compare Multiple Implementations

```java
@Test
void compareImplementations(DtrContext ctx) {
    ctx.sayNextSection("Sorting Algorithm Comparison");
    ctx.sayEnvProfile();

    int[] data = new java.util.Random(42).ints(1000, 0, 10000).toArray();

    ctx.sayBenchmark("Arrays.sort (1000 ints)", () -> {
        int[] copy = data.clone();
        java.util.Arrays.sort(copy);
    });

    ctx.sayBenchmark("Stream sorted (1000 ints)", () -> {
        java.util.Arrays.stream(data).sorted().toArray();
    });

    ctx.say("Note: Both use TimSort internally. The Stream version has additional boxing overhead.");
}
```

---

## Benchmark with Virtual Threads

```java
@Test
void benchmarkVirtualThreadSpawn(DtrContext ctx) throws Exception {
    ctx.sayNextSection("Virtual Thread Spawn Cost");
    ctx.sayEnvProfile();

    ctx.sayBenchmark("Virtual thread: spawn + complete 100 tasks", () -> {
        try (var exec = java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 100; i++) {
                futures.add(exec.submit(() -> "done"));
            }
            for (var f : futures) f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }, 5, 30);

    ctx.sayBenchmark("Platform thread pool: 100 tasks (8 threads)", () -> {
        try (var exec = java.util.concurrent.Executors.newFixedThreadPool(8)) {
            var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();
            for (int i = 0; i < 100; i++) {
                futures.add(exec.submit(() -> "done"));
            }
            for (var f : futures) f.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }, 5, 30);
}
```

---

## Benchmark Combined with ASCII Chart

```java
@Test
void benchmarkAndChart(DtrContext ctx) {
    ctx.sayNextSection("String Builder Strategies");
    ctx.sayEnvProfile();

    String[] labels = {"concat +", "StringBuilder", "String.join", "formatted()"};
    double[] avgNs = new double[labels.length];
    int n = 50_000;

    // Warmup
    for (int w = 0; w < 100; w++) {
        "a" + "b";
        new StringBuilder().append("a").append("b").toString();
    }

    // Measure
    long t0 = System.nanoTime();
    for (int i = 0; i < n; i++) { "prefix" + i + "suffix"; }
    avgNs[0] = (double)(System.nanoTime() - t0) / n;

    t0 = System.nanoTime();
    for (int i = 0; i < n; i++) { new StringBuilder().append("prefix").append(i).append("suffix").toString(); }
    avgNs[1] = (double)(System.nanoTime() - t0) / n;

    t0 = System.nanoTime();
    for (int i = 0; i < n; i++) { String.join("", "prefix", String.valueOf(i), "suffix"); }
    avgNs[2] = (double)(System.nanoTime() - t0) / n;

    t0 = System.nanoTime();
    for (int i = 0; i < n; i++) { "prefix%ssuffix".formatted(i); }
    avgNs[3] = (double)(System.nanoTime() - t0) / n;

    // Detailed benchmarks
    ctx.sayBenchmark("concat + (" + n + " iter)", () -> "prefix" + 1 + "suffix");
    ctx.sayBenchmark("StringBuilder (" + n + " iter)", () ->
        new StringBuilder().append("prefix").append(1).append("suffix").toString());
    ctx.sayBenchmark("String.join (" + n + " iter)", () ->
        String.join("", "prefix", "1", "suffix"));
    ctx.sayBenchmark("formatted() (" + n + " iter)", () ->
        "prefix%ssuffix".formatted(1));

    // Summary chart
    ctx.sayAsciiChart("Avg ns/op (lower is better)", avgNs, labels);
    ctx.say("Measured on Java " + System.getProperty("java.version"));
}
```

---

## Best Practices

**Always call sayEnvProfile() first.** Timing results are meaningless without knowing the hardware and JVM version.

**Let sayBenchmark handle warmup.** The default warmup is sufficient for most cases. Use the four-argument overload only when you need more control.

**Do not measure I/O.** `sayBenchmark` is designed for CPU and memory operations. Network I/O timings are dominated by network variability, not code quality.

**Report absolute numbers.** Do not describe results as "X times faster" without also reporting the absolute timing values.

---

## See Also

- [Benchmarking (full guide)](benchmarking.md) — Complete benchmarking workflow
- [Render ASCII Charts](sse-parsing.md) — sayAsciiChart for visualization
- [Performance Tuning](performance-tuning.md) — Build-level optimization
