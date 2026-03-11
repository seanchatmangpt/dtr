# How-to: Use Virtual Threads with Executors

Spawn thousands of lightweight concurrent tasks using Java 25 virtual threads. This guide covers practical patterns for concurrent execution without resource overhead.

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
        // This runs on a virtual thread
        Thread.sleep(1000);
        return "Result";
    });

    String result = future.get(); // Block and wait for result
}
```

`executor.submit(Callable)` returns a `Future` immediately (non-blocking). Call `.get()` to wait for the result.

---

## Submit Multiple Tasks and Wait for All

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Integer>> futures = new ArrayList<>();

    for (int i = 0; i < 100; i++) {
        futures.add(executor.submit(() -> {
            // Simulate work
            Thread.sleep(50);
            return 42;
        }));
    }

    // Collect all results
    List<Integer> results = new ArrayList<>();
    for (Future<Integer> future : futures) {
        results.add(future.get());
    }

    System.out.println("Got " + results.size() + " results");
}
```

Or use streams:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Integer>> futures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        futures.add(executor.submit(() -> 42));
    }

    List<Integer> results = futures.stream()
        .map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        })
        .toList();
}
```

---

## Invoke Multiple Callables and Wait

Use `invokeAll()` to submit multiple tasks and await all:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tasks = List.of(
        () -> { Thread.sleep(100); return "Task 1"; },
        () -> { Thread.sleep(50);  return "Task 2"; },
        () -> { Thread.sleep(200); return "Task 3"; }
    );

    // Submit all and wait for completion
    List<Future<String>> futures = executor.invokeAll(tasks);

    // All are complete; collect results
    for (Future<String> f : futures) {
        System.out.println(f.get());
    }
}
```

---

## Invoke and Get First Result

Use `invokeAny()` to get the first successful result:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Callable<String>> tasks = List.of(
        () -> { Thread.sleep(500); return "Slow"; },
        () -> { Thread.sleep(100); return "Fast"; },
        () -> { Thread.sleep(300); return "Medium"; }
    );

    // Returns the first to complete (usually "Fast")
    String result = executor.invokeAny(tasks);
    System.out.println(result);
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

## Timeout on Waiting

Use `.get(timeout, unit)` to avoid waiting forever:

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
                return new TaskResult.Success(f.get());
            } catch (Exception e) {
                return new TaskResult.Failure(e.getMessage());
            }
        })
        .toList();

    long successCount = results.stream()
        .filter(r -> r instanceof TaskResult.Success)
        .count();

    System.out.println(successCount + " succeeded");
}
```

---

## Scale Dynamically

Virtual threads automatically scale — no pool sizing needed:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    // 10 tasks
    for (int i = 0; i < 10; i++) {
        executor.submit(() -> simulateWork());
    }
    // executor automatically sizes itself

    // Later, 1000 tasks
    for (int i = 0; i < 1000; i++) {
        executor.submit(() -> simulateWork());
    }
    // executor scales up without configuration
}

void simulateWork() {
    try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
```

---

## Measure Execution Time

Track how long concurrent execution takes:

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    long startNanos = System.nanoTime();

    List<Future<Void>> futures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        futures.add(executor.submit(() -> {
            Thread.sleep(100);
            return null;
        }));
    }

    // Wait for all
    for (Future<?> f : futures) {
        f.get();
    }

    long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
    System.out.println("100 tasks took " + elapsedMs + "ms");
    // Output: ~100ms (all run in parallel), not ~10000ms (sequential)
}
```

---

## Best Practices

✅ **DO:**
- Use try-with-resources to ensure executor cleanup
- Call `.get()` to wait for results
- Handle `ExecutionException` and `InterruptedException`
- Use virtual threads for I/O-bound work
- Spawn thousands if needed

❌ **DON'T:**
- Forget to close the executor (creates thread leaks)
- Use virtual threads for CPU-bound work (use `ForkJoinPool` instead)
- Call `.get()` without exception handling
- Assume `.submit()` blocks (it returns immediately)

---

## See Also

- [Tutorial: Virtual Threads for Concurrency](../tutorials/virtual-threads-lightweight-concurrency.md)
- [Reference: Virtual Threads API](../reference/virtual-threads-reference.md)
