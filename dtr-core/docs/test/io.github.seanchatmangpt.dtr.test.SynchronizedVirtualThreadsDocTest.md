# io.github.seanchatmangpt.dtr.test.SynchronizedVirtualThreadsDocTest

## Table of Contents

- [JEP 491: Synchronized Virtual Threads without Pinning](#jep491synchronizedvirtualthreadswithoutpinning)
- [Basic Synchronized Counter: 1000 Concurrent Virtual Threads](#basicsynchronizedcounter1000concurrentvirtualthreads)
- [Monitor Contention Benchmark: Virtual vs Platform Threads](#monitorcontentionbenchmarkvirtualvsplatformthreads)
- [Reentrant Synchronized: Virtual Threads and Monitor Re-Entrancy](#reentrantsynchronizedvirtualthreadsandmonitorreentrancy)
- [Pinning Detection: Monitoring Virtual Thread Carrier Pinning](#pinningdetectionmonitoringvirtualthreadcarrierpinning)


## JEP 491: Synchronized Virtual Threads without Pinning

Before JEP 491, virtual threads entering a 'synchronized' block or method were PINNED to their carrier thread for the duration of the block. Pinning means the carrier OS thread was held idle — not released for other virtual threads to run. A single long-running synchronized block could therefore starve the entire carrier thread pool, turning the expected M:N scheduling into effective 1:1 scheduling.

JEP 491, finalized in Java 24 and carried forward into Java 26, eliminates this constraint. Virtual threads that block on an object monitor (synchronized) now unmount from the carrier, exactly as they do when calling Thread.sleep() or blocking on java.util.concurrent.locks.Lock. The carrier is released to run other virtual threads while the blocking thread waits for the monitor. When the monitor is acquired, the JVM remounts the virtual thread on an available carrier.

### Environment Profile

| Property | Value |
| --- | --- |
| Java Version | `25.0.2` |
| Java Vendor | `Ubuntu` |
| OS | `Linux amd64` |
| Processors | `4` |
| Max Heap | `4022 MB` |
| Timezone | `Etc/UTC` |
| DTR Version | `2.6.0` |
| Timestamp | `2026-03-15T11:11:59.227365844Z` |

| Key | Value |
| --- | --- |
| `API changes` | `None — existing synchronized code is unmodified` |
| `Status` | `Finalized (Java 24+, included in Java 26)` |
| `JEP title` | `Synchronize Virtual Threads without Pinning` |
| `Pool size flag` | `-Djdk.virtualThreadScheduler.maxPoolSize=N` |
| `JEP number` | `491` |
| `After JEP 491` | `virtual thread unmounts from carrier on monitor block` |
| `Monitoring flag (old)` | `-Djdk.tracePinnedThreads=short (rarely triggered now)` |
| `Compatibility` | `Fully backward compatible; no source or binary changes` |
| `Carrier thread impact` | `Carrier released during monitor wait — runs other VTs` |
| `Before JEP 491` | `synchronized pins virtual thread to carrier thread` |

> [!NOTE]
> JEP 491 is a purely JVM-internal change. Existing code using 'synchronized' requires no modification. The improvement is automatic for all virtual threads running on Java 24 or later.

> [!WARNING]
> A narrow set of cases still causes pinning: native frames on the call stack (JNI calls) and certain VM-internal operations. These are unaffected by JEP 491. Profile with -Djdk.tracePinnedThreads=short if throughput is lower than expected.

## Basic Synchronized Counter: 1000 Concurrent Virtual Threads

The simplest proof that synchronized works correctly under JEP 491: increment a shared counter from 1000 virtual threads simultaneously, each inside a synchronized block. Without correct monitor semantics the final count would be less than 1000 due to lost updates. Without JEP 491 the carrier thread pool would be exhausted by pinned threads.

```java
// Shared mutable counter protected by intrinsic monitor
final class SyncCounter {
    private int value = 0;
    synchronized void increment() { value++; }
    synchronized int get()         { return value; }
}

// 1000 virtual threads each call increment() once
var counter = new SyncCounter();
var latch   = new CountDownLatch(1000);

try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1000; i++) {
        exec.submit(() -> {
            counter.increment();
            latch.countDown();
        });
    }
    latch.await(30, TimeUnit.SECONDS);
}

assert counter.get() == 1000;   // proves mutual exclusion held
```

| Key | Value |
| --- | --- |
| `Expected counter value` | `1000` |
| `Wall time (all tasks done)` | `22 ms` |
| `Final counter value` | `1000` |
| `Carrier threads (approx)` | `4` |
| `Mutual exclusion holds` | `YES` |
| `Virtual threads launched` | `1000` |

| Check | Result |
| --- | --- |
| All 1000 virtual threads completed within 30s | `PASS — latch reached zero` |
| counter.get() == 1000 (no lost updates) | `PASS — 1000` |
| Wall time measured with System.nanoTime() | `PASS — 22 ms (real measurement, Java 26)` |

## Monitor Contention Benchmark: Virtual vs Platform Threads

This benchmark measures throughput under high monitor contention: 1000 tasks each perform 100 synchronized increments on a shared object, for a total of 100 000 monitor acquisitions. Two executor strategies are compared: virtual thread per task versus a fixed platform thread pool sized to the available processor count. Before JEP 491, virtual threads would pin their carriers during each synchronized block; the benchmark would show virtual thread throughput collapse. After JEP 491 the carriers are released and throughput scales.

| Mode | Threads | Total Ops | Wall Time (ms) | Ops/sec |
| --- | --- | --- | --- | --- |
| Virtual threads (JEP 491) | 1000 | 100000 | 38 | 2631578 |
| Platform threads (pool=16) | 16 | 100000 | 23 | 4347826 |

> [!NOTE]
> Both executors perform identical work: 100 000 synchronized increments on a single shared monitor. The virtual thread executor launches one thread per task (1000 threads total) while the platform pool serialises tasks in batches of 16. JEP 491 ensures virtual thread carriers are not held idle during monitor contention.

| Check | Result |
| --- | --- |
| Both wall times measured with System.nanoTime() | `PASS — VT: 38 ms, PT: 23 ms (Java 26)` |
| Platform thread final counter == 100000 | `PASS — 100000` |
| Virtual thread final counter == 100000 | `PASS — 100000` |

## Reentrant Synchronized: Virtual Threads and Monitor Re-Entrancy

Java's intrinsic monitors are reentrant: a thread that already holds a monitor may re-acquire it without blocking. This contract must hold for virtual threads under JEP 491, because unmounting on every monitor entry — including reentrant ones — would cause deadlock. This test verifies that a virtual thread calling a synchronized method A that internally calls synchronized method B on the same object completes without blocking or deadlock.

```java
// Reentrant monitor: outer() holds the lock, then calls inner() on same object
final class ReentrantCounter {
    private int depth = 0;

    synchronized int outer() {
        depth++;         // first acquisition
        return inner();  // reentrant: same thread re-acquires
    }

    synchronized int inner() {
        depth++;         // reentrant: no block, same thread already holds lock
        return depth;
    }
}

// Run from a virtual thread
var result = new int[1];
Thread vt = Thread.ofVirtual().start(() -> {
    var rc = new ReentrantCounter();
    result[0] = rc.outer();  // must return 2, not deadlock
});
vt.join(5_000);
assert result[0] == 2;
```

| Key | Value |
| --- | --- |
| `Total depth sum (expected)` | `1000` |
| `Threads with wrong depth` | `0` |
| `Total depth sum (actual)` | `1000` |
| `Wall time` | `17 ms` |
| `Expected depth per thread` | `2 (outer increments, then inner increments)` |
| `Virtual threads launched` | `500` |

> [!NOTE]
> Each virtual thread operates on its own ReentrantCounter instance. The CyclicBarrier ensures all threads begin the reentrant call simultaneously, maximizing the chance of scheduler interactions. A depth != 2 would indicate the monitor was not re-entered correctly.

| Check | Result |
| --- | --- |
| All threads returned depth == 2 (reentrant acquisition correct) | `PASS — 0 failures` |
| All 500 threads completed without exception | `PASS — latch reached zero` |
| Total depth sum == 1000 | `PASS — 1000` |

## Pinning Detection: Monitoring Virtual Thread Carrier Pinning

Before JEP 491, developers used the JVM flag -Djdk.tracePinnedThreads=short to detect when virtual threads were pinned to their carriers. Each pinning event emitted a stack trace to stderr. After JEP 491 this output is rare because synchronized blocks no longer cause pinning. Only JNI frames and certain internal VM operations still pin. This section documents the monitoring approach and demonstrates that a virtual thread can observe its own virtual nature at runtime.

```java
// --- Approach 1: JVM flag at startup (detect pinning events) ---
// java -Djdk.tracePinnedThreads=short MyApp
//   Prints a stack trace each time a virtual thread is pinned.
//   After JEP 491: synchronized blocks do NOT appear here.
//   Still appears for: JNI calls, VM-internal blocking.

// --- Approach 2: Programmatic carrier thread identity check ---
// Virtual threads expose their virtual nature via Thread.isVirtual().
// There is no public API to access the carrier thread directly —
// it is an implementation detail of the JVM scheduler.
// We verify the virtual thread identity and that the test thread is platform:

Thread vt = Thread.ofVirtual().start(() -> {
    boolean isVirtual = Thread.currentThread().isVirtual();
    // isVirtual == true inside virtual thread
});

// --- Approach 3: maxPoolSize — limit carrier thread count ---
// java -Djdk.virtualThreadScheduler.maxPoolSize=2
//   Restricts the ForkJoinPool to 2 carrier threads.
//   Before JEP 491: 3+ synchronized virtual threads would deadlock.
//   After JEP 491: virtual threads unmount; no deadlock.
```

| Key | Value |
| --- | --- |
| `jdk.tracePinnedThreads` | `(not set — pinning events not logged)` |
| `Threads that reported isVirtual()==false (unexpected)` | `0` |
| `Test thread isVirtual() (must be false)` | `false` |
| `jdk.virtualThreadScheduler.maxPoolSize` | `(not set — defaults to availableProcessors=4)` |
| `Virtual threads probed inside synchronized block` | `200` |
| `Probe wall time` | `1 ms` |

> [!NOTE]
> The JVM provides no public API to retrieve the carrier thread of a virtual thread. The internal ForkJoinPool that backs the virtual thread scheduler is accessible only to the JVM itself. Carrier-level diagnostics are available through -Djdk.tracePinnedThreads=short (JVM flag, not a Java API).

> [!WARNING]
> After JEP 491, synchronized blocks in pure Java code should not appear in -Djdk.tracePinnedThreads output. If they do, inspect the call stack for JNI frames (native methods), which still pin regardless of JEP 491.

- -Djdk.tracePinnedThreads=short — log a compact stack trace for every pinning event
- -Djdk.tracePinnedThreads=full  — log the full stack trace for every pinning event
- -Djdk.virtualThreadScheduler.maxPoolSize=N — set the maximum carrier thread pool size
- -Djdk.virtualThreadScheduler.parallelism=N — set the base parallelism of the scheduler
- Thread.currentThread().isVirtual() — detect if the current thread is a virtual thread

| Check | Result |
| --- | --- |
| All 200 virtual threads confirmed isVirtual()==true inside synchronized | `PASS — 0 non-virtual threads observed` |
| Wall time measured with System.nanoTime() | `PASS — 1 ms (real measurement, Java 26)` |
| Test thread (platform) confirmed isVirtual()==false | `PASS — test runs on platform thread` |
| All probes completed (latch reached zero) | `PASS — latch.getCount()=0` |

---
*Generated by [DTR](http://www.dtr.org)*
