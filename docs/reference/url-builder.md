# Reference: Benchmarking API Reference

**Package:** `io.github.seanchatmangpt.dtr.core`
**Version:** 2.6.0 (new in this release)

DTR 2.6.0 introduces built-in micro-benchmarking via two `sayBenchmark` overloads on `DtrContext`. These methods run your workload, measure elapsed time with `System.nanoTime()`, and render a benchmark result table directly in the documentation.

---

## sayBenchmark — default settings

```java
ctx.sayBenchmark(String label, Runnable task)
```

Runs `task` with 100 warmup iterations followed by 1 000 measured iterations.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `label` | `String` | Display name shown in the result table |
| `task` | `Runnable` | The workload to measure |

**Output:** A table with columns: Label, Warmup iterations, Measured iterations, Min (ns), Max (ns), Avg (ns), Total (ms).

**Example:**

```java
@Test
void benchmarkStringConcat(DtrContext ctx) {
    ctx.sayNextSection("String Concatenation Benchmark");

    ctx.sayBenchmark("String + operator", () -> {
        String s = "";
        for (int i = 0; i < 100; i++) s = s + "x";
    });

    ctx.sayBenchmark("StringBuilder", () -> {
        var sb = new StringBuilder();
        for (int i = 0; i < 100; i++) sb.append("x");
        sb.toString();
    });
}
```

---

## sayBenchmark — explicit warmup and iterations

```java
ctx.sayBenchmark(String label, Runnable task, int warmup, int iterations)
```

Full control over warmup and measurement counts.

**Parameters:**

| Parameter | Type | Description |
|-----------|------|-------------|
| `label` | `String` | Display name shown in the result table |
| `task` | `Runnable` | The workload to measure |
| `warmup` | `int` | Number of warmup iterations (not measured; allow JIT to stabilize) |
| `iterations` | `int` | Number of measured iterations |

**Example:**

```java
@Test
void benchmarkVirtualThreadSpawn(DtrContext ctx) {
    ctx.sayNextSection("Virtual Thread Spawn Cost");

    ctx.sayBenchmark(
        "Thread.ofVirtual().start().join()",
        () -> {
            try {
                Thread.ofVirtual().start(() -> {}).join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        },
        500,      // warmup
        10_000    // measured iterations
    );
}
```

---

## Output format

Each `sayBenchmark` call appends a result table to the documentation:

```
| Benchmark                          | Warmup | Iterations | Min (ns) | Max (ns) | Avg (ns) | Total (ms) |
|------------------------------------|--------|------------|----------|----------|----------|------------|
| Thread.ofVirtual().start().join()  |    500 |     10 000 |      412 |    8 203 |      631 |       6.31 |
```

The table is preceded by environment metadata from `sayEnvProfile()` if called in the same test.

---

## Measurement methodology

- Time measured with `System.nanoTime()` surrounding each individual `task.run()` call.
- Warmup iterations are run but not timed. Warmup allows the JIT compiler to compile and optimize the task before measurement begins.
- Each iteration is timed independently. Min, max, and average are computed from all measured iterations.
- No forking: measurements run in the same JVM as the test. For production benchmarking, use JMH.

---

## Reporting guidelines (from CLAUDE.md)

Always report: metric + units + Java version + iteration counts + environment.

**Correct:** `"ArrayList add 1K: 42 341 ns avg (100 warmup, 1 000 iter, Java 26.0.2, Linux x86_64)"`

**Incorrect:** `"ArrayList is 6 667x faster"` (no measurement basis)

Use `ctx.sayEnvProfile()` before benchmark tables to capture the execution environment automatically.

```java
@Test
void fullBenchmark(DtrContext ctx) {
    ctx.sayNextSection("Benchmark Results");
    ctx.sayEnvProfile();   // captures Java version, OS, CPU, memory
    ctx.sayBenchmark("Task A", () -> taskA());
    ctx.sayBenchmark("Task B", () -> taskB(), 200, 5_000);
}
```

---

## See also

- [say* Core API Reference](request-api.md) — all 37 methods
- [Virtual Threads Reference](virtual-threads-reference.md) — MultiRenderMachine dispatch model
- [Utility API Reference](sse-reference.md) — `sayEnvProfile`, `sayAsciiChart`
