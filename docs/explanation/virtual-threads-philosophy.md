# Explanation: Why Virtual Threads Matter

Virtual threads represent a fundamental shift in how Java approaches concurrency. This document explains the design philosophy behind them and how DTR uses them — not just as a performance optimization, but as a structural enabler of two distinct capabilities.

---

## The Problem: Thread Scalability

For decades, Java's concurrency model has been platform threads — one-to-one mappings to OS threads:

```
Java Thread ↔ OS Thread (~2MB RAM, OS scheduler manages it)
```

This worked for programs with dozens of concurrent tasks. Modern software demands more. A documentation generator writing to four output formats simultaneously has four concurrent I/O operations. If each blocks a platform thread, four threads sit idle during disk writes.

The cost is not measured in throughput — the bottleneck is disk, not CPU. The cost is measured in design constraints: without virtual threads, parallel output requires careful thread pool management, explicit synchronization, or an async programming model that makes the code harder to read.

---

## The Solution: Virtual Threads

Virtual threads flip the model:

```
Java Virtual Thread (~1KB RAM, JVM-managed)
    │
    └── mounted onto Carrier Thread (OS platform thread)
```

When a virtual thread blocks on I/O, the JVM unmounts it from its carrier thread. Another virtual thread mounts on that carrier. The OS sees only a few platform threads; the JVM runs thousands of virtual threads on top of them.

The crucial property: code written for virtual threads is sequential and blocking. There are no callbacks, no `CompletableFuture` chains, no reactive operators. The code reads as if each operation were synchronous. The JVM provides the concurrency transparently.

---

## How DTR Uses Virtual Threads: Two Distinct Purposes

Virtual threads appear in DTR in two places, for two different reasons. Understanding both clarifies why the feature is not optional for DTR's design.

### Purpose 1: MultiRenderMachine Parallel Output

`MultiRenderMachine` holds a list of `RenderMachine` implementations. When `finishAndWriteOut()` is called after all test methods complete, it must produce output in every format — Markdown, LaTeX, HTML, JSON — simultaneously.

Without virtual threads, parallel output requires a managed thread pool, careful lifecycle handling, and explicit synchronization. The output code becomes complex enough to be a bug surface.

With virtual threads, the implementation is direct:

```java
// Conceptual structure of MultiRenderMachine.finishAndWriteOut()
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (RenderMachine target : targets) {
        executor.submit(() -> target.finishAndWriteOut());
    }
}
// All render machines complete before this line
```

Each render machine runs on its own virtual thread. Disk I/O in the Markdown writer does not delay the LaTeX writer. The total rendering time approaches the cost of the slowest format, not the sum of all formats. And the code looks like sequential iteration — because, from the caller's perspective, it is.

The try-with-resources pattern is structured concurrency: the block does not exit until all submitted tasks complete. There are no thread leaks, no forgotten shutdowns, no race between cleanup and ongoing work.

Because `SayEvent` records are immutable, sharing them across virtual threads requires no synchronization. The Markdown render machine and the JSON render machine can read the same `SayEvent.Benchmark` record simultaneously without a lock.

### Purpose 2: `sayBenchmark` Warmup Batches

Benchmarking JVM code has a persistent problem: the JIT compiler optimizes code progressively as it runs. Early iterations execute interpreted code; later iterations execute heavily optimized native code. A benchmark that measures only the first few iterations measures something that does not represent steady-state performance.

`sayBenchmark(Runnable, int iterations)` addresses this using virtual thread batches:

1. A warmup phase runs the lambda in batches on virtual threads, allowing the JIT to observe and optimize the code path
2. A measurement phase runs the lambda again, with the JIT-compiled path active
3. Mean, min, max, and standard deviation are computed from the measurement phase
4. The statistics are documented as a `SayEvent.Benchmark`

The batching structure uses virtual threads because it allows the warmup batches to overlap with JIT compilation decisions made by the JVM. The JIT compiler works on carrier threads; virtual threads run the lambda; when a virtual thread yields or blocks, the carrier thread is available for JIT work. This is an emergent benefit of the virtual thread model, not an explicit design of the JIT.

The result: `sayBenchmark` produces measurements that reflect actual optimized performance, not cold-start behavior. The benchmark results embedded in documentation are the numbers that matter — the numbers your users will observe after the JVM has warmed up.

---

## Why Blocking Is Natural for Documentation Generation

Documentation generation is I/O-bound. The CPU work — pattern matching on events, string formatting, JSON serialization — is fast. The bottleneck is writing files, and file writes block.

Platform thread models require you to design around blocking: use non-blocking I/O, callbacks, or async APIs. Virtual thread models let you ignore blocking: write synchronous code, let the JVM unmount the thread when it waits.

For DTR, this means each render machine can be written as a straightforward accumulate-then-flush implementation:

1. Accumulate formatted strings as events are dispatched
2. Call `Files.writeString(path, accumulated)` at the end

`Files.writeString` blocks. With virtual threads, this is fine — the carrier thread is released to other work while the disk write proceeds. The render machine code is simple because it does not need to manage the blocking behavior.

---

## Structured Concurrency in DTR

DTR uses try-with-resources for its virtual thread executor in every location where parallel work is needed. This is structured concurrency — a discipline that ensures:

- All submitted tasks complete before the calling code continues
- If any task throws, the exception is propagated to the caller
- No threads run in the background after the try block exits

Structured concurrency is why `MultiRenderMachine.finishAndWriteOut()` can be called synchronously from the JUnit `afterAll` hook. The hook returns only after all render machines have written their output. There is no race between the JUnit lifecycle and the output files being written.

---

## Immutability and Concurrency

`SayEvent` records being immutable is not coincidentally compatible with virtual thread dispatch — it is required by it.

`MultiRenderMachine` dispatches the same `SayEvent` instance to multiple render machines running concurrently. If events were mutable, concurrent access would require synchronization. Synchronization in virtual thread code can cause "pinning" — a virtual thread holding a monitor while blocked cannot be unmounted from its carrier thread, defeating the scalability benefit.

Records eliminate this problem entirely. An immutable object has no mutation to synchronize. Every render machine reads the same event simultaneously with no coordination required. This is why all 13 `SayEvent` types are records: immutability is not a stylistic preference, it is the property that makes concurrent dispatch correct without synchronization.

---

## The Broader Concurrency Philosophy

Virtual threads reflect a design decision about where complexity should live: in the JVM, not in application code.

Before virtual threads, Java developers who wanted both scalable I/O and readable code had to choose: write blocking code (simple, but limited by thread count) or write async code (scalable, but complex). Libraries like Netty, RxJava, and Project Reactor were built to bridge this gap — adding substantial complexity to achieve async scalability with readable-enough code.

Virtual threads eliminate the choice. Blocking code is scalable code. The JVM manages the threading model; application code reads sequentially and behaves concurrently.

For DTR, this means the render pipeline — one of the architecturally most complex parts of the library — is implemented with simple, sequential code in each render machine, parallel dispatch in `MultiRenderMachine`, and structured cleanup via try-with-resources. The concurrent behavior emerges from the virtual thread model, not from custom synchronization logic.

---

## Limitations in the DTR Context

Virtual threads have known limitations that are worth understanding:

**Pinning.** A virtual thread holding a `synchronized` block while blocking on I/O cannot be unmounted. DTR avoids `synchronized` in all code that runs on virtual threads, using `ConcurrentHashMap` for the reflection cache and relying on record immutability elsewhere.

**CPU-bound work.** Virtual threads do not help with CPU-bound computation. `sayBenchmark` warmup batches run lambda code that may be CPU-bound. The benefit in that case is not concurrency — it is the JIT interaction described earlier, not reduced resource consumption.

**Debugger support.** Virtual thread stack traces in some debuggers and profilers may be less clear than platform thread traces. This affects development experience, not correctness.

None of these limitations affect DTR's correctness. They are design-time considerations that shaped how the virtual thread usage was structured.
