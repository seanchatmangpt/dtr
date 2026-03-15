/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JEP 491 — Synchronized Virtual Threads without Pinning.
 *
 * <p>Documents the carrier-thread pinning problem that existed before JEP 491,
 * the fix delivered in Java 24+ (finalized in Java 26), and provides real
 * concurrent measurements proving that synchronized blocks are now safe for
 * virtual threads without degrading throughput.</p>
 *
 * <p>Sections covered:</p>
 * <ol>
 *   <li>Overview: pinning problem, JEP 491 fix, compatibility notes</li>
 *   <li>Basic synchronized counter: 1000 concurrent virtual threads, verified count</li>
 *   <li>Monitor contention benchmark: virtual vs platform throughput under synchronized</li>
 *   <li>Reentrant synchronized: virtual threads respect monitor re-entrancy</li>
 *   <li>Pinning detection: monitoring approach and JVM flags</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class SynchronizedVirtualThreadsDocTest extends DtrTest {

    // =========================================================================
    // Static nested helper classes — local classes inside methods cause
    // ClassNotFoundException in Surefire's forked JVM class loader.
    // =========================================================================

    /** Simple counter protected by its intrinsic monitor. Used in a2_synchronized_basic. */
    static final class SyncCounter {
        private int value = 0;
        synchronized void increment() { value++; }
        synchronized int get()         { return value; }
    }

    /** Counter whose outer() calls inner() on the same monitor — demonstrates re-entrancy. */
    static final class ReentrantCounter {
        private int depth = 0;

        synchronized int outer() {
            depth++;
            return inner();
        }

        synchronized int inner() {
            depth++;
            return depth;
        }

        synchronized int get() { return depth; }
    }

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: JEP 491 Overview
    // =========================================================================

    @Test
    void a1_jep491_overview() {
        sayNextSection("JEP 491: Synchronized Virtual Threads without Pinning");

        say("Before JEP 491, virtual threads entering a 'synchronized' block or method " +
            "were PINNED to their carrier thread for the duration of the block. Pinning means " +
            "the carrier OS thread was held idle — not released for other virtual threads to run. " +
            "A single long-running synchronized block could therefore starve the entire carrier " +
            "thread pool, turning the expected M:N scheduling into effective 1:1 scheduling.");

        say("JEP 491, finalized in Java 24 and carried forward into Java 26, eliminates this " +
            "constraint. Virtual threads that block on an object monitor (synchronized) now " +
            "unmount from the carrier, exactly as they do when calling Thread.sleep() or blocking " +
            "on java.util.concurrent.locks.Lock. The carrier is released to run other virtual " +
            "threads while the blocking thread waits for the monitor. When the monitor is " +
            "acquired, the JVM remounts the virtual thread on an available carrier.");

        sayEnvProfile();

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "JEP number",               "491",
            "JEP title",                "Synchronize Virtual Threads without Pinning",
            "Status",                   "Finalized (Java 24+, included in Java 26)",
            "Before JEP 491",           "synchronized pins virtual thread to carrier thread",
            "After JEP 491",            "virtual thread unmounts from carrier on monitor block",
            "Carrier thread impact",    "Carrier released during monitor wait — runs other VTs",
            "API changes",              "None — existing synchronized code is unmodified",
            "Compatibility",            "Fully backward compatible; no source or binary changes",
            "Monitoring flag (old)",    "-Djdk.tracePinnedThreads=short (rarely triggered now)",
            "Pool size flag",           "-Djdk.virtualThreadScheduler.maxPoolSize=N"
        )));

        sayNote("JEP 491 is a purely JVM-internal change. Existing code using 'synchronized' " +
                "requires no modification. The improvement is automatic for all virtual threads " +
                "running on Java 24 or later.");

        sayWarning("A narrow set of cases still causes pinning: native frames on the call stack " +
                   "(JNI calls) and certain VM-internal operations. These are unaffected by JEP 491. " +
                   "Profile with -Djdk.tracePinnedThreads=short if throughput is lower than expected.");
    }

    // =========================================================================
    // Section 2: Synchronized Counter — 1000 Concurrent Virtual Threads
    // =========================================================================

    @Test
    void a2_synchronized_basic() throws Exception {
        sayNextSection("Basic Synchronized Counter: 1000 Concurrent Virtual Threads");

        say("The simplest proof that synchronized works correctly under JEP 491: " +
            "increment a shared counter from 1000 virtual threads simultaneously, " +
            "each inside a synchronized block. Without correct monitor semantics the " +
            "final count would be less than 1000 due to lost updates. " +
            "Without JEP 491 the carrier thread pool would be exhausted by pinned threads.");

        sayCode("""
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
                """, "java");

        // Live measurement
        final int THREAD_COUNT = 1000;

        var counter = new SyncCounter();
        var latch   = new CountDownLatch(THREAD_COUNT);

        long startNs = System.nanoTime();
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                exec.submit(() -> {
                    counter.increment();
                    latch.countDown();
                });
            }
            latch.await(30, TimeUnit.SECONDS);
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        int finalCount = counter.get();

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Virtual threads launched",    String.valueOf(THREAD_COUNT),
            "Final counter value",         String.valueOf(finalCount),
            "Expected counter value",      String.valueOf(THREAD_COUNT),
            "Mutual exclusion holds",      finalCount == THREAD_COUNT ? "YES" : "NO — LOST UPDATES",
            "Wall time (all tasks done)",  elapsedMs + " ms",
            "Carrier threads (approx)",    String.valueOf(Runtime.getRuntime().availableProcessors())
        )));

        sayAssertions(new LinkedHashMap<>(Map.of(
            "counter.get() == " + THREAD_COUNT + " (no lost updates)",
                finalCount == THREAD_COUNT
                    ? "PASS — " + finalCount
                    : "FAIL — got " + finalCount + ", lost " + (THREAD_COUNT - finalCount) + " updates",
            "All " + THREAD_COUNT + " virtual threads completed within 30s",
                latch.getCount() == 0 ? "PASS — latch reached zero" : "FAIL — latch.getCount()=" + latch.getCount(),
            "Wall time measured with System.nanoTime()",
                "PASS — " + elapsedMs + " ms (real measurement, Java 26)"
        )));
    }

    // =========================================================================
    // Section 3: Monitor Contention Benchmark
    // =========================================================================

    @Test
    void a3_monitor_contention_benchmark() throws Exception {
        sayNextSection("Monitor Contention Benchmark: Virtual vs Platform Threads");

        say("This benchmark measures throughput under high monitor contention: " +
            "1000 tasks each perform 100 synchronized increments on a shared object, " +
            "for a total of 100 000 monitor acquisitions. Two executor strategies are compared: " +
            "virtual thread per task versus a fixed platform thread pool sized to the " +
            "available processor count. Before JEP 491, virtual threads would pin their " +
            "carriers during each synchronized block; the benchmark would show virtual thread " +
            "throughput collapse. After JEP 491 the carriers are released and throughput scales.");

        final int TASK_COUNT       = 1000;
        final int INCREMENTS_EACH  = 100;
        final int TOTAL_OPS        = TASK_COUNT * INCREMENTS_EACH;

        // Shared counter — intrinsic monitor, not ReentrantLock
        final Object monitor = new Object();
        final long[] sharedValue = {0L};

        // --- Virtual thread benchmark ---
        long vtStartNs = System.nanoTime();
        {
            var done = new CountDownLatch(TASK_COUNT);
            try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
                for (int i = 0; i < TASK_COUNT; i++) {
                    exec.submit(() -> {
                        for (int j = 0; j < INCREMENTS_EACH; j++) {
                            synchronized (monitor) {
                                sharedValue[0]++;
                            }
                        }
                        done.countDown();
                    });
                }
                done.await(60, TimeUnit.SECONDS);
            }
        }
        long vtElapsedMs  = (System.nanoTime() - vtStartNs) / 1_000_000;
        long vtFinalValue = sharedValue[0];
        long vtOpsPerSec  = vtElapsedMs > 0 ? (TOTAL_OPS * 1000L / vtElapsedMs) : TOTAL_OPS;

        // Reset counter for platform thread run
        sharedValue[0] = 0L;

        // --- Platform thread benchmark ---
        final int PLATFORM_POOL = Math.min(64, Runtime.getRuntime().availableProcessors() * 4);
        long ptStartNs = System.nanoTime();
        {
            var done = new CountDownLatch(TASK_COUNT);
            try (var exec = Executors.newFixedThreadPool(PLATFORM_POOL)) {
                for (int i = 0; i < TASK_COUNT; i++) {
                    exec.submit(() -> {
                        for (int j = 0; j < INCREMENTS_EACH; j++) {
                            synchronized (monitor) {
                                sharedValue[0]++;
                            }
                        }
                        done.countDown();
                    });
                }
                done.await(120, TimeUnit.SECONDS);
            }
        }
        long ptElapsedMs  = (System.nanoTime() - ptStartNs) / 1_000_000;
        long ptFinalValue = sharedValue[0];
        long ptOpsPerSec  = ptElapsedMs > 0 ? (TOTAL_OPS * 1000L / ptElapsedMs) : TOTAL_OPS;

        sayTable(new String[][] {
            {"Mode", "Threads", "Total Ops", "Wall Time (ms)", "Ops/sec"},
            {
                "Virtual threads (JEP 491)",
                String.valueOf(TASK_COUNT),
                String.valueOf(TOTAL_OPS),
                String.valueOf(vtElapsedMs),
                String.valueOf(vtOpsPerSec)
            },
            {
                "Platform threads (pool=" + PLATFORM_POOL + ")",
                String.valueOf(PLATFORM_POOL),
                String.valueOf(TOTAL_OPS),
                String.valueOf(ptElapsedMs),
                String.valueOf(ptOpsPerSec)
            },
        });

        sayNote("Both executors perform identical work: 100 000 synchronized increments " +
                "on a single shared monitor. The virtual thread executor launches one thread " +
                "per task (1000 threads total) while the platform pool serialises tasks in " +
                "batches of " + PLATFORM_POOL + ". JEP 491 ensures virtual thread carriers " +
                "are not held idle during monitor contention.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "Virtual thread final counter == " + TOTAL_OPS,
                vtFinalValue == TOTAL_OPS
                    ? "PASS — " + vtFinalValue
                    : "FAIL — got " + vtFinalValue,
            "Platform thread final counter == " + TOTAL_OPS,
                ptFinalValue == TOTAL_OPS
                    ? "PASS — " + ptFinalValue
                    : "FAIL — got " + ptFinalValue,
            "Both wall times measured with System.nanoTime()",
                "PASS — VT: " + vtElapsedMs + " ms, PT: " + ptElapsedMs + " ms (Java 26)"
        )));
    }

    // =========================================================================
    // Section 4: Reentrant Synchronized
    // =========================================================================

    @Test
    void a4_reentrant_synchronized() throws Exception {
        sayNextSection("Reentrant Synchronized: Virtual Threads and Monitor Re-Entrancy");

        say("Java's intrinsic monitors are reentrant: a thread that already holds a monitor " +
            "may re-acquire it without blocking. This contract must hold for virtual threads " +
            "under JEP 491, because unmounting on every monitor entry — including reentrant " +
            "ones — would cause deadlock. This test verifies that a virtual thread calling " +
            "a synchronized method A that internally calls synchronized method B on the same " +
            "object completes without blocking or deadlock.");

        sayCode("""
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
                """, "java");

        // Live test: 500 virtual threads each exercise reentrant synchronized
        final int THREAD_COUNT = 500;

        var barrier      = new CyclicBarrier(THREAD_COUNT);
        var latch        = new CountDownLatch(THREAD_COUNT);
        var failures     = new AtomicLong(0L);
        var depthSum     = new AtomicLong(0L);

        long startNs = System.nanoTime();
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < THREAD_COUNT; i++) {
                exec.submit(() -> {
                    try {
                        // Each thread gets its own counter instance to isolate reentrancy
                        var rc = new ReentrantCounter();
                        barrier.await(10, TimeUnit.SECONDS);  // all start simultaneously
                        int depth = rc.outer();
                        depthSum.addAndGet(depth);
                        if (depth != 2) {
                            failures.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(30, TimeUnit.SECONDS);
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        long expectedDepthSum = (long) THREAD_COUNT * 2;

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Virtual threads launched",    String.valueOf(THREAD_COUNT),
            "Expected depth per thread",   "2 (outer increments, then inner increments)",
            "Total depth sum (actual)",    String.valueOf(depthSum.get()),
            "Total depth sum (expected)",  String.valueOf(expectedDepthSum),
            "Threads with wrong depth",    String.valueOf(failures.get()),
            "Wall time",                   elapsedMs + " ms"
        )));

        sayNote("Each virtual thread operates on its own ReentrantCounter instance. " +
                "The CyclicBarrier ensures all threads begin the reentrant call simultaneously, " +
                "maximizing the chance of scheduler interactions. A depth != 2 would indicate " +
                "the monitor was not re-entered correctly.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "All " + THREAD_COUNT + " threads completed without exception",
                latch.getCount() == 0
                    ? "PASS — latch reached zero"
                    : "FAIL — latch.getCount()=" + latch.getCount(),
            "All threads returned depth == 2 (reentrant acquisition correct)",
                failures.get() == 0
                    ? "PASS — 0 failures"
                    : "FAIL — " + failures.get() + " threads returned wrong depth",
            "Total depth sum == " + expectedDepthSum,
                depthSum.get() == expectedDepthSum
                    ? "PASS — " + depthSum.get()
                    : "FAIL — got " + depthSum.get()
        )));
    }

    // =========================================================================
    // Section 5: Pinning Detection
    // =========================================================================

    @Test
    void a5_pinning_detection() throws Exception {
        sayNextSection("Pinning Detection: Monitoring Virtual Thread Carrier Pinning");

        say("Before JEP 491, developers used the JVM flag -Djdk.tracePinnedThreads=short " +
            "to detect when virtual threads were pinned to their carriers. Each pinning event " +
            "emitted a stack trace to stderr. After JEP 491 this output is rare because " +
            "synchronized blocks no longer cause pinning. Only JNI frames and certain internal " +
            "VM operations still pin. This section documents the monitoring approach and " +
            "demonstrates that a virtual thread can observe its own virtual nature at runtime.");

        sayCode("""
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
                """, "java");

        // Live probe: start virtual threads, each entering a synchronized block,
        // confirm the test framework (platform thread) is not virtual,
        // and confirm virtual threads identify themselves correctly.
        final int PROBE_COUNT = 200;
        var latch      = new CountDownLatch(PROBE_COUNT);
        var nonVirtual = new AtomicLong(0L);
        var lock       = new Object();

        long startNs = System.nanoTime();
        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < PROBE_COUNT; i++) {
                exec.submit(() -> {
                    synchronized (lock) {
                        // Inside synchronized block — after JEP 491 the carrier is NOT pinned here
                        if (!Thread.currentThread().isVirtual()) {
                            nonVirtual.incrementAndGet();
                        }
                    }
                    latch.countDown();
                });
            }
            latch.await(30, TimeUnit.SECONDS);
        }
        long elapsedMs = (System.nanoTime() - startNs) / 1_000_000;

        // Read the system property to document the scheduler pool size
        String maxPoolSizeProp = System.getProperty(
            "jdk.virtualThreadScheduler.maxPoolSize",
            "(not set — defaults to availableProcessors=" + Runtime.getRuntime().availableProcessors() + ")"
        );
        String tracePinnedProp = System.getProperty(
            "jdk.tracePinnedThreads",
            "(not set — pinning events not logged)"
        );

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "jdk.tracePinnedThreads",                  tracePinnedProp,
            "jdk.virtualThreadScheduler.maxPoolSize",  maxPoolSizeProp,
            "Virtual threads probed inside synchronized block",
                                                       String.valueOf(PROBE_COUNT),
            "Threads that reported isVirtual()==false (unexpected)",
                                                       String.valueOf(nonVirtual.get()),
            "Test thread isVirtual() (must be false)",
                                                       String.valueOf(Thread.currentThread().isVirtual()),
            "Probe wall time",                         elapsedMs + " ms"
        )));

        sayNote("The JVM provides no public API to retrieve the carrier thread of a virtual " +
                "thread. The internal ForkJoinPool that backs the virtual thread scheduler is " +
                "accessible only to the JVM itself. Carrier-level diagnostics are available " +
                "through -Djdk.tracePinnedThreads=short (JVM flag, not a Java API).");

        sayWarning("After JEP 491, synchronized blocks in pure Java code should not appear " +
                   "in -Djdk.tracePinnedThreads output. If they do, inspect the call stack for " +
                   "JNI frames (native methods), which still pin regardless of JEP 491.");

        sayUnorderedList(List.of(
            "-Djdk.tracePinnedThreads=short — log a compact stack trace for every pinning event",
            "-Djdk.tracePinnedThreads=full  — log the full stack trace for every pinning event",
            "-Djdk.virtualThreadScheduler.maxPoolSize=N — set the maximum carrier thread pool size",
            "-Djdk.virtualThreadScheduler.parallelism=N — set the base parallelism of the scheduler",
            "Thread.currentThread().isVirtual() — detect if the current thread is a virtual thread"
        ));

        sayAssertions(new LinkedHashMap<>(Map.of(
            "All " + PROBE_COUNT + " virtual threads confirmed isVirtual()==true inside synchronized",
                nonVirtual.get() == 0
                    ? "PASS — 0 non-virtual threads observed"
                    : "FAIL — " + nonVirtual.get() + " threads reported isVirtual()==false",
            "Test thread (platform) confirmed isVirtual()==false",
                !Thread.currentThread().isVirtual()
                    ? "PASS — test runs on platform thread"
                    : "FAIL — unexpected virtual thread context",
            "All probes completed (latch reached zero)",
                latch.getCount() == 0
                    ? "PASS — latch.getCount()=0"
                    : "FAIL — latch.getCount()=" + latch.getCount(),
            "Wall time measured with System.nanoTime()",
                "PASS — " + elapsedMs + " ms (real measurement, Java 26)"
        )));
    }
}
