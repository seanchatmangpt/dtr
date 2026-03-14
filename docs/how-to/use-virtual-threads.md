# How-to: Use Virtual Threads with Executors

Spawn thousands of lightweight concurrent tasks using Java 25 virtual threads. Benchmark the results with `sayBenchmark` and document dispatch performance with `MultiRenderMachine`.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## Create a Virtual Thread Executor

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // Submit tasks here
}
```

This creates an executor that spawns one virtual thread per task. The JVM schedules all threads on a shared pool of carrier threads.

---

## Submit One Task

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> future = executor.submit(() -> {
        Thread.sleep(1000);
        return "Result";
    });

    String result = future.get(); // Block and wait for result
}
```

---

## Submit Multiple Tasks and Wait

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
        futures.add(executor.submit(() -> {
            Thread.sleep(50);
            return 42;
        }));
    }

    List<Integer> results = futures.stream()
        .map(f -> {
            try { return f.get(); }
            catch (Exception e) { throw new RuntimeException(e); }
        })
        .toList();

    System.out.println("Got " + results.size() + " results");
}
```

---

## Benchmark Virtual Threads vs Platform Threads

Use `sayBenchmark` to document the performance difference:

```java
@ExtendWith(DtrExtension.class)
class VirtualThreadBenchmarkDocTest {

    @Test
    void benchmarkConcurrency(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Virtual Thread vs Platform Thread Performance");
        ctx.sayEnvProfile();

        final int taskCount = 500;

        ctx.sayBenchmark("Platform threads (" + taskCount + " tasks, pool=8)", () -> {
            try (var exec = Executors.newFixedThreadPool(8)) {
                var futures = new ArrayList<Future<?>>();
                for (int i = 0; i < taskCount; i++) {
                    futures.add(exec.submit(() -> { Thread.sleep(1); return null; }));
                }
                for (var f : futures) f.get();
            } catch (Exception e) { throw new RuntimeException(e); }
        }, 3, 10);

        ctx.sayBenchmark("Virtual threads (" + taskCount + " tasks)", () -> {
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                var futures = new ArrayList<Future<?>>();
                for (int i = 0; i < taskCount; i++) {
                    futures.add(exec.submit(() -> { Thread.sleep(1); return null; }));
                }
                for (var f : futures) f.get();
            } catch (Exception e) { throw new RuntimeException(e); }
        }, 3, 10);

        ctx.sayNote("Virtual threads excel at I/O-bound workloads where platform threads " +
                    "would block. For CPU-bound work, use ForkJoinPool instead.");
    }
}
```

---

## Document MultiRenderMachine Virtual Thread Dispatch

DTR's `MultiRenderMachine` uses virtual threads to dispatch to output engines. Document this:

```java
@Test
void documentDispatchOverhead(DtrContext ctx) {
    ctx.sayNextSection("MultiRenderMachine Dispatch Performance");
    ctx.sayEnvProfile();

    ctx.say("Each say* call is dispatched to all configured output engines " +
            "(Markdown, LaTeX, HTML, JSON) using virtual threads.");

    ctx.sayBenchmark("say() dispatched to all engines", () -> {
        ctx.say("benchmark payload");
    }, 10, 100);

    ctx.sayBenchmark("sayJson() with object serialization", () -> {
        ctx.sayJson(java.util.Map.of("key", "value", "count", 42));
    }, 10, 100);
}
```

---

## invokeAll: Submit All, Wait for All

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tasks = List.of(
        () -> { Thread.sleep(100); return "Task 1"; },
        () -> { Thread.sleep(50);  return "Task 2"; },
        () -> { Thread.sleep(200); return "Task 3"; }
    );

    List<Future<String>> futures = executor.invokeAll(tasks);

    for (Future<String> f : futures) {
        System.out.println(f.get());
    }
}
```

---

## invokeAny: Get First Result

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tasks = List.of(
        () -> { Thread.sleep(500); return "Slow"; },
        () -> { Thread.sleep(100); return "Fast"; },
        () -> { Thread.sleep(300); return "Medium"; }
    );

    String result = executor.invokeAny(tasks);
    System.out.println(result); // "Fast"
}
```

---

## Handle Exceptions

Exceptions in virtual threads are wrapped in `ExecutionException`:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<Integer> future = executor.submit(() -> {
        throw new IllegalStateException("Task failed");
    });

    try {
        future.get();
    } catch (ExecutionException e) {
        System.out.println("Task threw: " + e.getCause().getMessage());
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

---

## Collect Errors Without Stopping

Partition results into successes and failures:

```java
sealed interface TaskResult {
    record Success(Integer value) implements TaskResult {}
    record Failure(String error) implements TaskResult {}
}

try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < 10; i++) {
        final int id = i;
        futures.add(executor.submit(() -> {
            if (id % 3 == 0) throw new IllegalStateException("Bad ID");
            return id * 10;
        }));
    }

    List<TaskResult> results = futures.stream()
        .map(f -> {
            try {
                return (TaskResult) new TaskResult.Success(f.get());
            } catch (Exception e) {
                return new TaskResult.Failure(e.getMessage());
            }
        })
        .toList();

    long successCount = results.stream()
        .filter(r -> r instanceof TaskResult.Success).count();
    System.out.println(successCount + " succeeded");
}
```

---

## Timeout on Waiting

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    Future<String> future = executor.submit(() -> {
        Thread.sleep(5000);
        return "Done";
    });

    try {
        String result = future.get(1, java.util.concurrent.TimeUnit.SECONDS);
        System.out.println(result);
    } catch (java.util.concurrent.TimeoutException e) {
        System.out.println("Task took too long, cancelling");
        future.cancel(true);
    }
}
```

---

## Best Practices

**Use try-with-resources.** The executor is closed automatically, which awaits all running tasks.

**Use virtual threads for I/O-bound work.** Virtual threads shine when tasks block on network, disk, or database I/O. For CPU-bound work, use `ForkJoinPool`.

**Spawn thousands if needed.** Unlike platform threads (costly to create), virtual threads have very low overhead. Creating 10,000 virtual threads is normal.

**Always handle `ExecutionException` and `InterruptedException`.** Both are checked exceptions from `Future.get()`.

**Benchmark with `sayBenchmark`.** When comparing virtual vs. platform threads, let `sayBenchmark` handle the timing and produce documented results.

---

## See Also

- [Benchmarking](benchmarking.md) — sayBenchmark for performance documentation
- [Performance Tuning](performance-tuning.md) — Build-level optimization
- [Pattern Matching with Sealed Records](pattern-matching.md) — Sealed TaskResult type handling
