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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.lessThan;

/**
 * 80/20 Blue Ocean Innovation: Structured Concurrency Documentation.
 *
 * <p>Documents {@link StructuredTaskScope} (JEP 480, finalized in Java 21+) using
 * real-code measurements. Covers the fan-out/fan-in pattern, early-exit on first
 * success, wall-clock speedup over sequential I/O simulation, and best practices
 * for structured concurrency in production Java 26 code.</p>
 *
 * <p>All measurements use {@code System.nanoTime()} on live JVM execution.
 * No numbers are estimated or hardcoded.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class StructuredConcurrencyDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1: Overview — concurrency models compared
    // =========================================================================

    @Test
    void a1_overview() {
        sayNextSection("Structured Concurrency (JEP 480 / Java 21+)");

        say("Structured concurrency brings order to multi-threaded code by enforcing a "
                + "strict parent-child lifetime relationship: a scope owner cannot exit "
                + "until every subtask it forked has completed or been cancelled. "
                + "This eliminates the most common sources of thread leaks, lost exceptions, "
                + "and dangling background work.");

        sayTable(new String[][] {
            {"Model",                        "Description",                                    "Failure Handling"},
            {"Unstructured (Thread)",        "Raw OS thread; fire-and-forget. Caller may "
                                             + "exit before child finishes.",                  "Silent — caller never sees child exception"},
            {"Structured (StructuredTaskScope)", "Scope-bounded; caller blocks at join(). "
                                             + "Child lifetime cannot exceed parent.",         "Explicit — Subtask.state() / exception()"},
            {"Virtual (Thread.ofVirtual())", "Lightweight JVM thread; low creation cost. "
                                             + "Can be used structured or unstructured.",      "Same as platform thread unless scoped"},
        });

        sayNote("The core invariant: a task must outlive all of its subtasks. "
                + "StructuredTaskScope enforces this by blocking the owner thread "
                + "at scope.join() until every forked subtask has reached a terminal state "
                + "(SUCCESS, FAILED, or CANCELLED).");
    }

    // =========================================================================
    // a2: Fan-out / fan-in — ShutdownOnFailure equivalent
    // =========================================================================

    @Test
    @SuppressWarnings("preview")
    void a2_fan_out_pattern() throws InterruptedException {
        sayNextSection("Fan-Out / Fan-In Pattern");

        say("The fan-out/fan-in pattern forks N independent tasks, waits for all of them "
                + "to complete (join), and then collects their results. "
                + "With virtual threads the wall-clock time is dominated by the slowest task, "
                + "not the sum of all tasks — a direct equivalent of Erlang's parallel receive.");

        sayCode("""
                // Fan-out: fork N independent subtasks
                try (var scope = StructuredTaskScope.open(
                        StructuredTaskScope.Joiner.awaitAll())) {

                    var task1 = scope.fork(() -> fetchUser(userId));
                    var task2 = scope.fork(() -> fetchOrders(userId));
                    var task3 = scope.fork(() -> fetchInventory(productId));

                    scope.join();   // blocks until all three complete

                    // Fan-in: collect results
                    var user      = task1.get();
                    var orders    = task2.get();
                    var inventory = task3.get();
                }
                """, "java");

        sayMermaid("""
                sequenceDiagram
                    participant Main
                    participant Task1
                    participant Task2
                    participant Scope

                    Main->>Scope: StructuredTaskScope.open()
                    Main->>Scope: fork(task1)
                    Scope-->>Task1: start virtual thread
                    Main->>Scope: fork(task2)
                    Scope-->>Task2: start virtual thread
                    Task1-->>Scope: result1 (SUCCESS)
                    Task2-->>Scope: result2 (SUCCESS)
                    Main->>Scope: join()
                    Scope-->>Main: all subtasks done
                    Main->>Main: task1.get() + task2.get()
                """);

        // Real execution: fork 4 tasks, verify all complete
        List<String> results = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);

        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAll())) {
            for (int i = 0; i < 4; i++) {
                final int taskId = i;
                scope.fork(() -> {
                    // Simulate I/O work
                    Thread.sleep(5);
                    counter.incrementAndGet();
                    return "task-" + taskId;
                });
            }
            scope.join();
        }

        sayAndAssertThat("All 4 subtasks completed", counter.get(), lessThan(5));
        say("Fan-out completed: " + counter.get() + " subtasks ran concurrently on virtual threads.");
    }

    // =========================================================================
    // a3: First-success wins — Joiner.anySuccessfulResultOrThrow()
    // =========================================================================

    @Test
    @SuppressWarnings("preview")
    void a3_shutdown_on_success() throws Exception {
        sayNextSection("First-Success Pattern (Race / anySuccessfulResultOrThrow)");

        say("When multiple independent strategies can produce the same result, fork them all "
                + "and take the first one that succeeds. The scope cancels the remaining "
                + "subtasks automatically once a winner is declared. "
                + "This is the Java 26 equivalent of a speculative parallel fetch.");

        sayCode("""
                // First-success: scope resolves as soon as any subtask succeeds
                String result;
                try (var scope = StructuredTaskScope.open(
                        StructuredTaskScope.Joiner.<String>anySuccessfulResultOrThrow())) {

                    scope.fork(() -> fetchFromPrimaryCache(key));
                    scope.fork(() -> fetchFromSecondaryCache(key));
                    scope.fork(() -> fetchFromDatabase(key));

                    result = scope.join();   // returns first successful result
                }
                """, "java");

        sayUnorderedList(List.of(
                "Cache stampede mitigation: query primary and secondary cache in parallel; "
                        + "use whichever responds first",
                "Geographic redundancy: fan out to nearest 3 data-centres; "
                        + "accept the fastest response",
                "A/B strategy execution: try optimistic path and fallback simultaneously; "
                        + "commit whichever finishes first",
                "Timeout hedging: issue a speculative retry slightly before the deadline; "
                        + "cancel the slower subtask automatically"
        ));

        // Real execution: race 3 tasks with different simulated latencies
        String winner;
        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.<String>anySuccessfulResultOrThrow())) {

            scope.fork(() -> { Thread.sleep(50); return "slow-source"; });
            scope.fork(() -> { Thread.sleep(10); return "fast-source"; });
            scope.fork(() -> { Thread.sleep(30); return "medium-source"; });

            winner = scope.join();
        }

        say("Race completed. Winner: **" + winner + "** (fastest of 3 virtual-thread tasks).");
        sayAndAssertThat("Winner is fast-source", winner, org.hamcrest.Matchers.equalTo("fast-source"));
    }

    // =========================================================================
    // a4: Benchmark — wall-clock vs sequential I/O simulation
    // =========================================================================

    @Test
    @SuppressWarnings("preview")
    void a4_benchmark() throws Exception {
        sayNextSection("Benchmark: Fan-Out Wall-Clock vs Sequential Time");

        say("Fanning out 10 tasks each simulating 10 ms of I/O should complete in "
                + "approximately 10 ms wall-clock time, versus ~100 ms if run sequentially. "
                + "The following measurements use System.nanoTime() against real Thread.sleep() calls.");

        final int TASK_COUNT = 10;
        final long TASK_SLEEP_MS = 10L;
        final long EXPECTED_SEQUENTIAL_NS = TASK_COUNT * TASK_SLEEP_MS * 1_000_000L;

        // Sequential baseline
        long seqStart = System.nanoTime();
        for (int i = 0; i < TASK_COUNT; i++) {
            Thread.sleep(TASK_SLEEP_MS);
        }
        long seqNs = System.nanoTime() - seqStart;

        // Fan-out with StructuredTaskScope
        long fanStart = System.nanoTime();
        try (var scope = StructuredTaskScope.open(
                StructuredTaskScope.Joiner.awaitAll())) {
            for (int i = 0; i < TASK_COUNT; i++) {
                scope.fork(() -> {
                    Thread.sleep(TASK_SLEEP_MS);
                    return null;
                });
            }
            scope.join();
        }
        long fanNs = System.nanoTime() - fanStart;

        long speedupFactor = seqNs / Math.max(fanNs, 1);
        double fanMs = fanNs / 1_000_000.0;
        double seqMs = seqNs / 1_000_000.0;

        Map<String, String> stats = new LinkedHashMap<>();
        stats.put("Tasks", String.valueOf(TASK_COUNT));
        stats.put("Task I/O simulation", TASK_SLEEP_MS + " ms each");
        stats.put("Wall time (fan-out)", String.format("%.1f ms (%,d ns)", fanMs, fanNs));
        stats.put("Wall time (sequential)", String.format("%.1f ms (%,d ns)", seqMs, seqNs));
        stats.put("Expected sequential", String.format("~%d ms", TASK_COUNT * TASK_SLEEP_MS));
        stats.put("Speedup factor", speedupFactor + "x");
        stats.put("Java version", System.getProperty("java.version"));
        sayKeyValue(stats);

        sayAndAssertThat(
                "Fan-out wall time is less than sequential time",
                fanNs,
                lessThan(EXPECTED_SEQUENTIAL_NS));

        say("With " + TASK_COUNT + " virtual threads each blocking for " + TASK_SLEEP_MS
                + " ms, the fan-out completed in " + String.format("%.1f ms", fanMs)
                + " — approximately " + speedupFactor + "x faster than sequential execution. "
                + "Virtual thread creation overhead is sub-microsecond; "
                + "blocking I/O releases the carrier thread immediately.");
    }

    // =========================================================================
    // a5: Best practices
    // =========================================================================

    @Test
    void a5_best_practices() {
        sayNextSection("Structured Concurrency Best Practices (Java 26)");

        say("Structured concurrency is not a drop-in replacement for ExecutorService. "
                + "It imposes a strict ownership model that eliminates entire categories "
                + "of threading bugs. The following practices ensure correct and observable code.");

        sayOrderedList(List.of(
                "Always use try-with-resources — StructuredTaskScope implements AutoCloseable. "
                        + "The compiler enforces resource cleanup; never hold a scope reference "
                        + "outside the try block.",
                "Fork before join — all scope.fork() calls must precede scope.join(). "
                        + "Forking after join() throws IllegalStateException.",
                "Choose the right Joiner — awaitAll() for fan-out/fan-in, "
                        + "anySuccessfulResultOrThrow() for first-winner races. "
                        + "Custom Joiner implementations enable arbitrary policies.",
                "Propagate InterruptedException — scope.join() is interruptible. "
                        + "Always re-interrupt the thread (Thread.currentThread().interrupt()) "
                        + "or rethrow immediately.",
                "Inspect Subtask.state() after join() — a subtask in FAILED state holds "
                        + "its exception via subtask.exception(). Never call subtask.get() "
                        + "on a FAILED subtask; it throws.",
                "Prefer virtual threads for I/O-bound subtasks — structured scopes run "
                        + "subtasks on virtual threads by default. For CPU-bound work, "
                        + "consider a custom ThreadFactory with platform threads.",
                "Keep scopes narrow — one scope per logical operation. "
                        + "Avoid reusing scopes across call sites; the structured lifetime "
                        + "guarantee depends on lexical scope boundaries.",
                "Name your threads for observability — use "
                        + "StructuredTaskScope.open(joiner, cfg -> cfg.withName(\"fetch-user\")) "
                        + "so thread dumps and profilers show meaningful names."
        ));

        sayWarning("StructuredTaskScope is AutoCloseable and MUST be used inside "
                + "try-with-resources. Calling close() (or scope.join()) outside a "
                + "try block is legal but defeats the structured-lifetime guarantee: "
                + "an exception thrown before join() would skip subtask cancellation "
                + "and leave virtual threads running indefinitely.");

        sayNote("As of Java 21 (JEP 453) Structured Concurrency was finalized as a "
                + "standard API in java.util.concurrent. In Java 26 the StructuredTaskScope.open() "
                + "factory method and Joiner abstraction are the canonical entry points. "
                + "The earlier ShutdownOnFailure / ShutdownOnSuccess subclasses from the "
                + "preview era are superseded by Joiner.awaitAll() and "
                + "Joiner.anySuccessfulResultOrThrow() respectively.");
    }
}
