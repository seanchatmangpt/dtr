# Reference: Virtual Threads API

Complete API reference for Java 25 virtual threads. Virtual threads are lightweight, JVM-managed threads that enable scalable concurrent programming.

---

## Core Classes and Methods

### `java.util.concurrent.Executors`

Factory methods for creating virtual thread executors:

#### `newVirtualThreadPerTaskExecutor()`

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

Creates an unbounded executor that spawns a new virtual thread for each task. Automatically scales based on load.

**Returns:** `ExecutorService`

**Example:**
```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> System.out.println("Running on virtual thread"));
}
```

---

### `java.lang.Thread`

#### `Thread.ofVirtual()`

```java
Thread virtualThread = Thread.ofVirtual()
    .name("my-thread")
    .start(() -> System.out.println("Hello"));
```

Creates and starts a single virtual thread with a name.

**Returns:** `Thread`

**Chaining methods:**
- `.name(String)` — set thread name
- `.start(Runnable)` — start immediately with runnable

---

#### `Thread.ofPlatform()`

For comparison: creates a traditional OS-managed platform thread.

```java
Thread platformThread = Thread.ofPlatform()
    .name("platform-thread")
    .start(() -> System.out.println("Platform thread"));
```

---

### `java.util.concurrent.ExecutorService`

Standard executor interface — same for platform and virtual threads.

#### `submit(Callable<T>)`

```java
Future<String> future = executor.submit(() -> {
    // Do work
    return "result";
});

String result = future.get(); // Block until done
```

**Returns:** `Future<T>`

---

#### `submit(Runnable)`

```java
Future<?> future = executor.submit(() -> {
    System.out.println("Work without return value");
});

future.get(); // Wait for completion
```

**Returns:** `Future<?>`

---

#### `invokeAll(Collection<Callable<T>>)`

```java
List<Callable<Integer>> tasks = List.of(
    () -> 1,
    () -> 2,
    () -> 3
);

List<Future<Integer>> futures = executor.invokeAll(tasks);
// All complete before returning
```

Submits all tasks and waits for completion. Blocks until all are done.

**Returns:** `List<Future<T>>`

---

#### `invokeAny(Collection<Callable<T>>)`

```java
List<Callable<String>> tasks = List.of(
    () -> { Thread.sleep(100); return "slow"; },
    () -> { Thread.sleep(50); return "fast"; },
    () -> { Thread.sleep(200); return "slower"; }
);

String firstResult = executor.invokeAny(tasks); // "fast"
```

Returns the first successful result; cancels others.

**Returns:** `T` (not a Future)

**Throws:** `InterruptedException`, `ExecutionException`

---

#### `shutdown()`

```java
executor.shutdown();
// No new tasks accepted; wait for existing to complete
executor.awaitTermination(10, TimeUnit.SECONDS);
```

Gracefully shutdown: no new tasks, wait for existing.

---

#### `shutdownNow()`

```java
List<Runnable> pending = executor.shutdownNow();
// Immediate shutdown; returns unstarted tasks
```

Aggressive shutdown: interrupt running tasks, return pending.

**Returns:** `List<Runnable>`

---

### `java.util.concurrent.Future<T>`

Represents the result of an asynchronous computation.

#### `get()`

```java
T result = future.get(); // Block until result available
```

Blocks indefinitely until the result is ready.

**Throws:** `ExecutionException` (task threw exception), `InterruptedException` (thread was interrupted)

---

#### `get(long timeout, TimeUnit unit)`

```java
T result = future.get(5, TimeUnit.SECONDS);
```

Block with timeout. Throws `TimeoutException` if not done in time.

**Throws:** `TimeoutException`, `ExecutionException`, `InterruptedException`

---

#### `isDone()`

```java
if (future.isDone()) {
    System.out.println("Task completed");
}
```

Returns `true` if task is done (success, exception, or cancelled).

**Returns:** `boolean`

---

#### `cancel(boolean mayInterruptIfRunning)`

```java
boolean wasCancelled = future.cancel(true);
// true: interrupt if currently running
// false: cancel only if not yet started
```

Attempt to cancel the task.

**Returns:** `boolean` — true if cancel succeeded, false if already done

---

#### `isCancelled()`

```java
if (future.isCancelled()) {
    System.out.println("Task was cancelled");
}
```

Returns `true` if task was cancelled.

**Returns:** `boolean`

---

## Common Patterns

### Pattern: Collect Multiple Results

```java
List<Future<String>> futures = new ArrayList<>();
for (int i = 0; i < 100; i++) {
    futures.add(executor.submit(() -> "result"));
}

List<String> results = futures.stream()
    .map(f -> {
        try {
            return f.get();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    })
    .toList();
```

---

### Pattern: Partition Results into Success/Failure

```java
sealed interface TaskResult {
    record Success(String value) implements TaskResult {}
    record Failure(String error) implements TaskResult {}
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
```

---

### Pattern: Timeout Handling

```java
try {
    String result = future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Task took too long");
    future.cancel(true);
} catch (ExecutionException e) {
    System.out.println("Task failed: " + e.getCause());
} catch (InterruptedException e) {
    System.out.println("Waiting was interrupted");
    Thread.currentThread().interrupt();
}
```

---

## Virtual vs. Platform Threads Comparison

| Aspect | Virtual Thread | Platform Thread |
|--------|---|---|
| **Creation cost** | Microseconds, kilobytes | Milliseconds, megabytes |
| **Max per JVM** | Millions | Thousands |
| **Context switch** | JVM-managed, fast | OS-managed, slower |
| **Use case** | I/O-bound work | CPU-bound work (ForkJoinPool better) |
| **Executor** | `newVirtualThreadPerTaskExecutor()` | `newFixedThreadPool()`, `newCachedThreadPool()` |
| **Blocking** | Designed for blocking I/O | OK for blocking |

---

## Best Practices

✅ **DO:**
- Use `Executors.newVirtualThreadPerTaskExecutor()` for I/O workloads
- Call `.get()` with timeout for network operations
- Handle `ExecutionException` — task may have failed
- Use try-with-resources to ensure executor cleanup
- Spawn thousands if needed — virtual threads scale

❌ **DON'T:**
- Use virtual threads for CPU-bound work (use `ForkJoinPool`)
- Forget exception handling in `.get()` calls
- Leave executors running (memory leaks)
- Assume `.submit()` blocks (it returns immediately)
- Create custom thread factories — use `Executors`

---

## See Also

- [Tutorial: Virtual Threads for Concurrency](../tutorials/virtual-threads-lightweight-concurrency.md)
- [How-to: Use Virtual Threads](../how-to/use-virtual-threads.md)
- [Explanation: Why Virtual Threads Matter](../explanation/virtual-threads-philosophy.md)
