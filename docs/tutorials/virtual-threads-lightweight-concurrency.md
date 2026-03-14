# Tutorial: Benchmarking with Virtual Threads and sayBenchmark

Learn how to measure and document performance using DTR 2.6.0's `sayBenchmark` method. This tutorial uses Java 25 virtual threads as a real-world subject: you will benchmark virtual thread creation overhead, compare warmup strategies, and document results directly in generated Markdown.

**Time:** ~30 minutes
**Prerequisites:** Java 25, DTR 2.6.0, completion of [Your First DocTest](your-first-doctest.md)
**What you'll learn:** How `sayBenchmark` works, when to use explicit warmup rounds, and how to embed benchmark results in living documentation

---

## What Is sayBenchmark?

`sayBenchmark` is a DTR 2.6.0 method that runs a `Runnable` task, measures its execution time using `System.nanoTime()`, performs virtual-thread-based warmup, and emits a formatted result table in the documentation output.

Two signatures are available:

```java
// Simple: DTR chooses warmup and measure rounds
ctx.sayBenchmark(String label, Runnable task);

// Explicit: you control warmup and measure rounds
ctx.sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds);
```

The output includes the label, average nanoseconds per iteration, total elapsed time, and the Java version.

---

## Step 1 — Set Up the Test Class

Create `src/test/java/com/example/BenchmarkDocTest.java`:

```java
package com.example;

import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class BenchmarkDocTest {

    @Test
    void benchmarkVirtualThreadCreation(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Virtual Thread Creation Overhead");

        ctx.say("Virtual threads are lightweight JVM-managed threads. "
            + "Unlike platform threads (~2 MB stack each), virtual threads cost "
            + "kilobytes and are created in nanoseconds. "
            + "This benchmark measures the overhead of spawning and joining one virtual thread.");

        ctx.sayBenchmark("spawn and join one virtual thread", () -> {
            try {
                Thread vt = Thread.ofVirtual().start(() -> {});
                vt.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        ctx.say("The result above shows the average cost in nanoseconds per iteration "
            + "after JIT warmup.");
    }
}
```

Run it:

```bash
mvnd test -Dtest=BenchmarkDocTest
cat target/docs/test-results/BenchmarkDocTest.md
```

The Markdown output will contain a benchmark result table under the section heading.

---

## Step 2 — Control Warmup and Measure Rounds

The default `sayBenchmark` chooses warmup and measurement iterations automatically. When you need precise control — for example to match a published benchmark or reduce test time — use the four-argument form:

```java
    @Test
    void benchmarkWithExplicitRounds(DtrContext ctx) {

        ctx.sayNextSection("Explicit Warmup and Measure Rounds");

        ctx.say("For reproducible comparisons, fix the warmup and measure counts. "
            + "Here we use 5 warmup rounds and 20 measure rounds.");

        // Benchmark: creating an ArrayList and adding 1000 elements
        ctx.sayBenchmark(
            "ArrayList construction (1000 elements)",
            () -> {
                var list = new ArrayList<Integer>(1000);
                for (int i = 0; i < 1000; i++) list.add(i);
            },
            5,   // warmupRounds
            20   // measureRounds
        );

        ctx.say("Explicit rounds are useful when comparing multiple workloads side by side: "
            + "keep the rounds identical so the numbers are comparable.");
    }
```

---

## Step 3 — Compare Two Implementations

Use consecutive `sayBenchmark` calls to document a side-by-side comparison:

```java
    @Test
    void comparePlatformVsVirtualThreadScheduling(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Platform Threads vs Virtual Threads: Scheduling 100 Tasks");

        ctx.say("Both executors submit 100 tasks that each sleep for 10 ms. "
            + "Virtual threads are scheduled cooperatively by the JVM, "
            + "so all 100 tasks overlap. Platform threads from a fixed pool "
            + "of 8 are serialized in batches of 8.");

        int taskCount = 100;
        int sleepMs   = 10;

        // Platform thread executor (fixed pool of 8)
        ctx.sayBenchmark(
            "platform thread pool (size 8), 100 tasks × 10 ms",
            () -> {
                try (ExecutorService exec = Executors.newFixedThreadPool(8)) {
                    List<Future<?>> futures = new ArrayList<>();
                    for (int i = 0; i < taskCount; i++) {
                        futures.add(exec.submit(() -> {
                            try { Thread.sleep(sleepMs); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }));
                    }
                    for (Future<?> f : futures) {
                        try { f.get(); }
                        catch (Exception e) { /* ignore */ }
                    }
                }
            },
            2,  // warmupRounds
            5   // measureRounds
        );

        // Virtual thread executor (unbounded)
        ctx.sayBenchmark(
            "virtual thread executor, 100 tasks × 10 ms",
            () -> {
                try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
                    List<Future<?>> futures = new ArrayList<>();
                    for (int i = 0; i < taskCount; i++) {
                        futures.add(exec.submit(() -> {
                            try { Thread.sleep(sleepMs); }
                            catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                        }));
                    }
                    for (Future<?> f : futures) {
                        try { f.get(); }
                        catch (Exception e) { /* ignore */ }
                    }
                }
            },
            2,  // warmupRounds
            5   // measureRounds
        );

        ctx.say("With a fixed pool of 8 platform threads, 100 × 10 ms tasks need "
            + "roughly 13 batches, so wall-clock time is ~130 ms. "
            + "With virtual threads all 100 tasks overlap, so wall-clock time is ~10 ms. "
            + "The benchmark numbers above reflect real measurements on this machine.");
    }
```

---

## Step 4 — Benchmark a Pure CPU Computation

Virtual threads excel at I/O-bound work. For CPU-bound tasks, benchmark with caution — the JVM's carrier thread pool is bounded by `Runtime.availableProcessors()`. This benchmark shows a CPU task for contrast:

```java
    @Test
    void benchmarkCpuBoundComputation(DtrContext ctx) {

        ctx.sayNextSection("CPU-Bound Computation Baseline");

        ctx.say("Sorting a shuffled list of 10,000 integers is a pure CPU task. "
            + "Virtual threads offer no throughput advantage here, "
            + "but the benchmark establishes a baseline for comparison.");

        var data = new ArrayList<Integer>(10_000);
        for (int i = 0; i < 10_000; i++) data.add(i);
        java.util.Collections.shuffle(data);

        ctx.sayBenchmark(
            "Collections.sort on 10,000 integers",
            () -> {
                var copy = new ArrayList<>(data);
                java.util.Collections.sort(copy);
            },
            10,   // warmupRounds
            50    // measureRounds
        );

        ctx.sayNote("For CPU-bound parallelism, use `ForkJoinPool` or parallel streams, "
            + "not virtual threads. Virtual threads park on I/O and wake efficiently; "
            + "CPU-bound tasks never park.");
    }
```

---

## Step 5 — Capture Environment Context

Close the documentation with an environment snapshot so readers know which machine the numbers came from:

```java
    @Test
    void captureEnvironment(DtrContext ctx) {

        ctx.sayNextSection("Benchmark Environment");

        ctx.say("Benchmark results are only meaningful in context. "
            + "DTR captures the environment automatically:");

        ctx.sayEnvProfile();
    }
```

`sayEnvProfile()` emits a key-value table with Java version, OS, available processors, heap size, and DTR version — no arguments required.

---

## What Virtual Threads Are (and Are Not)

| Concept | Detail |
|---------|--------|
| **Virtual thread** | JVM-managed, lightweight — kilobytes of stack, not megabytes |
| **Carrier thread** | OS thread that actually runs virtual threads; pool size = CPU count |
| **Parking** | When a virtual thread blocks on I/O, it parks and frees its carrier thread |
| **Best for** | High-concurrency I/O workloads: HTTP, JDBC, file I/O |
| **Not for** | CPU-bound loops (use `ForkJoinPool` or parallel streams instead) |
| **`Executors.newVirtualThreadPerTaskExecutor()`** | Creates one virtual thread per submitted task |

---

## What You Learned

- How `sayBenchmark(label, task)` measures and documents performance in one call
- How `sayBenchmark(label, task, warmupRounds, measureRounds)` gives explicit control
- The pattern of side-by-side `sayBenchmark` calls for comparisons
- Why virtual threads outperform platform thread pools for I/O-bound concurrent tasks
- How `sayEnvProfile()` anchors benchmark numbers to the execution environment

---

## Next Steps

- [Tutorial: Records and Sealed Classes](records-sealed-classes.md) — benchmark record construction vs class construction
- [Tutorial: Testing a REST API](testing-a-rest-api.md) — measure HTTP endpoint latency with `sayBenchmark`
- [Tutorial: Visualizing Code with sayMermaid](websockets-realtime.md) — diagram the virtual thread scheduler
