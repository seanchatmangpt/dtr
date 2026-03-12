# Tutorial: Virtual Threads for Lightweight Concurrency

Learn how Java 25 virtual threads enable you to write naturally concurrent code without the resource overhead of traditional threads. This tutorial uses HTTP testing as an example, but virtual threads apply to any I/O-bound workload.

**Time:** ~25 minutes
**Prerequisites:** Java 25, understanding of concurrency concepts
**What you'll learn:** How to spawn thousands of virtual threads cheaply and write straightforward sequential code that runs in parallel

---

## What Are Virtual Threads?

Traditional Java threads are **platform threads** — each one is an OS thread with its own memory (stack), consuming ~2MB of RAM. Creating 1000 of them exhausts resources.

**Virtual threads** (Java 19+, matured in Java 25) are lightweight, JVM-managed threads:
- Cost pennies of RAM (kilobytes, not megabytes)
- Automatically scheduled on a small pool of carrier threads
- Scale to millions in a single JVM
- Make your code read sequentially while running in parallel

This changes what's practical. You can now:
- Spawn one virtual thread per incoming request
- Never block on "thread pool exhaustion"
- Write straightforward blocking code instead of async/await chains

---

## Step 1 — Basic Virtual Thread Usage

Create `src/test/java/com/example/VirtualThreadsExampleTest.java`:

```java
package com.example;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

public class VirtualThreadsExampleTest {

    public static void main(String[] args) throws Exception {
        System.out.println("Creating virtual thread executor...");

        // Create an executor backed by virtual threads
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            // Submit 1000 tasks concurrently
            System.out.println("Submitting 1000 tasks");
            long start = System.nanoTime();

            for (int i = 1; i <= 1000; i++) {
                final int id = i;
                executor.submit(() -> {
                    try {
                        // Simulate I/O work (e.g., HTTP request, database query)
                        Thread.sleep(100);
                        System.out.println("Task " + id + " completed");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // Wait for all to complete
            executor.shutdown();
            executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

            long elapsedMs = (System.nanoTime() - start) / 1_000_000;
            System.out.println("1000 tasks completed in " + elapsedMs + "ms");
            System.out.println("If using platform threads, this would take ~100 seconds");
            System.out.println("With virtual threads, all tasks run concurrently");
        }
    }
}
```

**Run it:**
```bash
mvnd compile exec:java -Dexec.mainClass="com.example.VirtualThreadsExampleTest"
```

**Output (actual time ~100ms, not 100+ seconds):**
```
Creating virtual thread executor...
Submitting 1000 tasks
Task 1 completed
Task 2 completed
...
1000 tasks completed in 103ms
```

This demonstrates the core principle: **thousands of tasks, negligible overhead, natural blocking code**.

---

## Step 2 — Virtual Threads in Tests (with DTR)

Extend `DTR` and use virtual threads to make concurrent assertions:

```java
package com.example;

import org.junit.Test;
import io.github.seanchatmangpt.dtr.dtr.DTR;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Url;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class VirtualThreadsDocTest extends DTR {

    @Test
    public void demonstrateVirtualThreads() throws Exception {

        sayNextSection("Virtual Threads in Java 25");

        say("Virtual threads allow us to spawn thousands of concurrent tasks "
            + "without creating expensive OS threads. "
            + "Each virtual thread is lightweight — the JVM schedules them automatically.");

        int concurrency = 1000;
        say("Submitting " + concurrency + " concurrent HTTP requests...");

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

            long startNanos = System.nanoTime();
            List<Future<Response>> futures = new ArrayList<>();

            // Submit 1000 requests
            for (int i = 1; i <= concurrency; i++) {
                final int id = i;
                futures.add(executor.submit(() ->
                    makeRequest(Request.GET()
                        .url(testServerUrl().path("/api/users/" + id)))));
            }

            // Collect results
            List<Response> responses = new ArrayList<>();
            for (Future<Response> future : futures) {
                responses.add(future.get());
            }

            long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;

            long successCount = responses.stream()
                .filter(r -> r.httpStatus() == 200)
                .count();

            say("All " + concurrency + " requests completed in " + elapsedMs + "ms");
            say("Success rate: " + (successCount * 100 / concurrency) + "%");

            sayAndAssertThat(
                "All virtual threads completed without error",
                concurrency,
                org.hamcrest.CoreMatchers.equalTo(successCount));
        }

        say("The virtual thread executor automatically sized itself based on workload. "
            + "There was no explicit thread pool configuration — the JVM handled it.");
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**Key points:**
- `Executors.newVirtualThreadPerTaskExecutor()` — creates an executor that spawns one virtual thread per task
- `executor.submit(Callable)` — returns a `Future` you can await
- No explicit sizing — the JVM creates as many virtual threads as needed
- Code reads sequentially; execution is parallel

---

## Step 3 — Compare Virtual Threads to Platform Threads

Write a benchmark showing the difference:

```java
@Test
public void virtualVsPlatformThreadComparison() throws Exception {

    sayNextSection("Virtual Threads vs Platform Threads");

    say("Here we compare the resource usage and execution time of "
        + "the same 100 concurrent tasks using platform threads vs virtual threads.");

    int taskCount = 100;

    // Platform thread executor
    say("Platform threads (cached thread pool):");
    long platformStart = System.nanoTime();
    try (ExecutorService platformExec = Executors.newCachedThreadPool()) {
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            futures.add(platformExec.submit(() -> {
                Thread.sleep(100); // Simulate I/O
                return 1;
            }));
        }
        int platformCount = 0;
        for (Future<Integer> f : futures) {
            platformCount += f.get();
        }
    }
    long platformMs = (System.nanoTime() - platformStart) / 1_000_000;

    // Virtual thread executor
    say("Virtual threads:");
    long virtualStart = System.nanoTime();
    try (ExecutorService virtualExec = Executors.newVirtualThreadPerTaskExecutor()) {
        List<Future<Integer>> futures = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            futures.add(virtualExec.submit(() -> {
                Thread.sleep(100); // Same I/O
                return 1;
            }));
        }
        int virtualCount = 0;
        for (Future<Integer> f : futures) {
            virtualCount += f.get();
        }
    }
    long virtualMs = (System.nanoTime() - virtualStart) / 1_000_000;

    say("Platform threads time: " + platformMs + "ms");
    say("Virtual threads time: " + virtualMs + "ms");
    say("Both complete in roughly the same time (~100ms) because all tasks block together. "
        + "The difference appears at scale: virtual threads consume 1/100th the memory "
        + "and don't exhaust system resources.");
}
```

---

## Step 4 — Structured Concurrency (try-with-resources)

Always close the executor to ensure all virtual threads complete:

```java
@Test
public void structuredConcurrency() throws Exception {

    sayNextSection("Structured Concurrency with Virtual Threads");

    say("Use try-with-resources to guarantee all virtual threads complete before moving on.");

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
        // All tasks run here
        for (int i = 0; i < 100; i++) {
            executor.submit(() -> makeRequest(
                Request.GET().url(testServerUrl().path("/api/data"))));
        }
        // Executor automatically shuts down and waits for completion
    }

    say("By the time we exit the try block, all 100 virtual threads have completed. "
        + "This is called structured concurrency — scoped execution with guaranteed cleanup.");
}
```

**Why this matters:** You never "forget" threads running in the background. The JVM enforces cleanup.

---

## Step 5 — Exception Handling with Virtual Threads

Handle errors from concurrent tasks elegantly:

```java
@Test
public void errorHandlingInVirtualThreads() throws Exception {

    sayNextSection("Exception Handling");

    say("When a virtual thread throws an exception, it's captured in the Future.");

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

        List<Future<Response>> futures = new ArrayList<>();

        for (int i = 1; i <= 50; i++) {
            final int id = i;
            futures.add(executor.submit(() -> {
                if (id % 7 == 0) {
                    throw new IllegalStateException("Simulated error for ID " + id);
                }
                return makeRequest(Request.GET()
                    .url(testServerUrl().path("/api/users/" + id)));
            }));
        }

        sealed interface TaskResult {
            record Success(Response response) implements TaskResult {}
            record Failure(String error) implements TaskResult {}
        }

        List<TaskResult> results = new ArrayList<>();
        for (Future<Response> future : futures) {
            try {
                results.add(new TaskResult.Success(future.get()));
            } catch (Exception e) {
                results.add(new TaskResult.Failure(e.getMessage()));
            }
        }

        long successCount = results.stream()
            .filter(r -> r instanceof TaskResult.Success)
            .count();

        long failureCount = results.stream()
            .filter(r -> r instanceof TaskResult.Failure)
            .count();

        say("Results: " + successCount + " succeeded, " + failureCount + " failed");
        say("We used sealed records and pattern matching (Java 25 features) "
            + "to type-safely classify results.");
    }
}
```

---

## Key Takeaways

| Concept | Explanation |
|---------|-------------|
| **Virtual Thread** | JVM-managed, lightweight thread (kilobytes, not megabytes) |
| **Executor** | `Executors.newVirtualThreadPerTaskExecutor()` creates a virtual thread per task |
| **Future** | Result of an async computation; call `.get()` to wait and retrieve |
| **Structured Concurrency** | Try-with-resources guarantees all threads complete and clean up |
| **Blocking is OK** | Virtual threads are designed for blocking I/O; the JVM handles scheduling |

---

## Best Practices

✅ **DO:**
- Use `newVirtualThreadPerTaskExecutor()` for I/O-bound workloads
- Close executors with try-with-resources
- Call `.get()` on futures to await results
- Catch `ExecutionException` and `InterruptedException`
- Spawn thousands if needed — virtual threads scale

❌ **DON'T:**
- Create unbounded thread pools of platform threads
- Forget to close executors (memory leaks)
- Use virtual threads for CPU-bound work (use `ForkJoinPool` instead)
- Assume `.submit()` starts execution immediately (submit is async)

---

## Next Steps

- [Tutorial: Records and Sealed Classes](records-sealed-classes.md) — type-safe data models with Java 25
- [How-to: Use Virtual Threads with Executors](../how-to/use-virtual-threads.md) — patterns and examples
- [How-to: Pattern Matching with Sealed Records](../how-to/pattern-matching.md) — exhaustive type handling
- [Reference: Virtual Threads API](../reference/virtual-threads-reference.md) — complete documentation
- [Explanation: Why Virtual Threads Matter](../explanation/virtual-threads-philosophy.md) — design rationale
