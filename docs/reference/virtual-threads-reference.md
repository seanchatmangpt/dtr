# Reference: Virtual Threads API

Complete API reference for Java 26 virtual threads. Includes v2.6.0 `MultiRenderMachine` virtual dispatch and `sayBenchmark` warmup patterns.

---

## Core Classes and Methods

### `java.util.concurrent.Executors`

#### `newVirtualThreadPerTaskExecutor()`

```java
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

Creates an unbounded executor that spawns a new virtual thread for each task.

**Returns:** `ExecutorService`

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 10_000; i++) {
        executor.submit(() -> processRequest());
    }
} // blocks until all tasks complete
```

---

### `java.lang.Thread`

#### `Thread.ofVirtual()`

```java
Thread vt = Thread.ofVirtual()
    .name("worker-1")
    .start(() -> System.out.println("Running on virtual thread"));
```

**Chaining methods:**
- `.name(String)` — set thread name
- `.start(Runnable)` — start and return the thread

#### `Thread.ofPlatform()`

Traditional OS-managed thread for CPU-bound work.

```java
Thread pt = Thread.ofPlatform().name("cpu-worker").start(() -> compute());
```

---

### `java.util.concurrent.ExecutorService`

#### `submit(Callable<T>)` → `Future<T>`

```java
Future<String> future = executor.submit(() -> {
    return fetchFromDatabase();  // blocking I/O — ideal for virtual threads
});
String result = future.get();
```

#### `submit(Runnable)` → `Future<?>`

```java
Future<?> future = executor.submit(() -> writeLog());
future.get(); // wait for completion
```

#### `invokeAll(Collection<Callable<T>>)` → `List<Future<T>>`

Submits all tasks and waits for all to complete before returning.

```java
List<Callable<String>> tasks = List.of(
    () -> callServiceA(),
    () -> callServiceB(),
    () -> callServiceC()
);

List<Future<String>> futures = executor.invokeAll(tasks);
// all three complete before this line
```

#### `invokeAny(Collection<Callable<T>>)` → `T`

Returns the first successful result; cancels remaining tasks.

```java
String fastest = executor.invokeAny(List.of(
    () -> callRegion("us-east"),
    () -> callRegion("eu-west"),
    () -> callRegion("ap-south")
));
```

#### `shutdown()` / `awaitTermination(long, TimeUnit)`

```java
executor.shutdown();
executor.awaitTermination(10, TimeUnit.SECONDS);
```

#### `shutdownNow()` → `List<Runnable>`

Interrupt running tasks, return unstarted tasks.

---

### `java.util.concurrent.Future<T>`

#### `get()` → `T`

Block until result is ready. Throws `ExecutionException` if the task threw an exception.

#### `get(long timeout, TimeUnit unit)` → `T`

Block with timeout. Throws `TimeoutException` if not done in time.

#### `isDone()` → `boolean`

True if completed (success, exception, or cancelled).

#### `cancel(boolean mayInterruptIfRunning)` → `boolean`

Attempt to cancel. Returns `true` if cancelled, `false` if already done.

---

## DTR 2.6.0: MultiRenderMachine Virtual Dispatch

`MultiRenderMachine` uses virtual threads to dispatch every `say*` call to all registered `RenderMachine` implementations concurrently. Total wall time equals the slowest machine, not the sum.

```java
@Test
void setup(DtrContext ctx) {
    ctx.setRenderMachine(new MultiRenderMachine(
        new RenderMachineImpl(),                                              // Markdown + HTML
        new RenderMachineLatex(new ACMTemplate(), new LatexmkStrategy()),    // LaTeX
        new BlogRenderMachine(new DevToTemplate()),                           // Blog
        new SlideRenderMachine()                                              // Slides
    ));
}
```

### Dispatch model

```
ctx.say("text")
    ├── virtual thread 1 → RenderMachineImpl.say("text")
    ├── virtual thread 2 → RenderMachineLatex.say("text")
    ├── virtual thread 3 → BlogRenderMachine.say("text")
    └── virtual thread 4 → SlideRenderMachine.say("text")
(all four run in parallel; ctx.say returns after all complete)
```

---

## DTR 2.6.0: sayBenchmark with Virtual Thread Warmup

`sayBenchmark` runs warmup iterations before measurement to allow the JIT compiler to optimize the task. For virtual thread benchmarks, increase warmup to account for JVM thread scheduler warm-up:

```java
@Test
void benchmarkVirtualThread(DtrContext ctx) {
    ctx.sayNextSection("Virtual Thread Performance");
    ctx.sayEnvProfile();

    // Platform thread baseline
    ctx.sayBenchmark(
        "Platform thread spawn + join",
        () -> {
            try {
                Thread.ofPlatform().start(() -> {}).join();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        },
        500, 5_000
    );

    // Virtual thread measurement
    ctx.sayBenchmark(
        "Virtual thread spawn + join",
        () -> {
            try {
                Thread.ofVirtual().start(() -> {}).join();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        },
        500, 5_000
    );
}
```

---

## Common Patterns

### Collect Multiple Results

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<String>> futures = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
        futures.add(executor.submit(() -> fetchItem()));
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
}
```

---

### Partition Results into Success/Failure

```java
sealed interface TaskResult permits TaskResult.Success, TaskResult.Failure {
    record Success(String value) implements TaskResult {}
    record Failure(String error) implements TaskResult {}
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
    .filter(r -> r instanceof TaskResult.Success)
    .count();
```

---

### Timeout Handling

```java
try {
    String result = future.get(5, TimeUnit.SECONDS);
} catch (TimeoutException e) {
    System.out.println("Timed out");
    future.cancel(true);
} catch (ExecutionException e) {
    System.out.println("Task failed: " + e.getCause());
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
}
```

---

## Virtual vs. Platform Thread Comparison

| Aspect | Virtual Thread | Platform Thread |
|--------|----------------|-----------------|
| Creation cost | Microseconds, ~KB | Milliseconds, ~MB |
| Max per JVM | Millions | Thousands |
| Context switch | JVM-managed, fast | OS-managed, slower |
| Use case | I/O-bound | CPU-bound (use ForkJoinPool) |
| Executor | `newVirtualThreadPerTaskExecutor()` | `newFixedThreadPool()` |
| Blocking I/O | Designed for it | Works but wastes OS thread |

---

## Best Practices

**DO:**
- Use `Executors.newVirtualThreadPerTaskExecutor()` for I/O workloads
- Use try-with-resources to ensure executor shutdown
- Call `.get()` with a timeout for network-bound operations
- Use `MultiRenderMachine` when generating multiple output formats — it handles parallelism automatically
- Use `sayBenchmark` with ≥ 500 warmup iterations for virtual thread benchmarks

**DON'T:**
- Use virtual threads for CPU-bound computation (use `ForkJoinPool` or `newFixedThreadPool` instead)
- Forget exception handling in `.get()` calls
- Assume `.submit()` blocks — it returns a `Future` immediately
- Create custom thread factories for virtual threads — use `Thread.ofVirtual()` or `Executors`

---

## See also

- [Benchmarking API Reference](url-builder.md) — `sayBenchmark` overloads
- [RenderMachine API](rendermachine-api.md) — `MultiRenderMachine` virtual thread dispatch
- [Java 26 Features Reference](java25-features-reference.md) — virtual threads in broader context
