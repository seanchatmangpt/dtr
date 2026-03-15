/*
 * Copyright (C) 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.vthread.VirtualThreadComparator;
import io.github.seanchatmangpt.dtr.vthread.VirtualThreadComparator.ComparisonResult;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;

import java.util.List;
import java.util.Map;

/**
 * Documentation test for {@link VirtualThreadComparator}.
 *
 * <p>Demonstrates and documents Java 21+ virtual threads by running real
 * measurements against platform threads. Timing assertions are deliberately
 * absent — wall-clock results are non-deterministic across environments.
 * The test asserts only structural invariants (non-null results, valid fields).</p>
 *
 * <p>All tasks are designed to complete well within 5 seconds total.</p>
 *
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class VirtualThreadComparisonDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: Overview and API walkthrough
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Virtual Thread Comparison — Overview");

        say("""
                Java 21 introduced virtual threads (JEP 444) as a lightweight alternative \
                to platform threads. Unlike platform threads, which map 1:1 to OS threads, \
                virtual threads are scheduled by the JVM and can number in the millions. \
                This makes them especially effective for I/O-bound or blocking workloads \
                where platform threads would otherwise sit idle waiting for the OS.""");

        say("""
                `VirtualThreadComparator` submits the same workload to both a \
                `FixedThreadPool` (bounded by available processors) and a \
                `VirtualThreadPerTaskExecutor`, measures wall-clock time for each, \
                and surfaces the results as a `ComparisonResult` record.""");

        sayCode("""
                // Platform threads — bounded by CPU count
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

                // Virtual threads — one per task, JVM-scheduled
                Executors.newVirtualThreadPerTaskExecutor()

                // Both executors run the same taskCount tasks,
                // then both are shut down and awaited.
                var result = VirtualThreadComparator.compare("sleep-1ms", 50, () -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });""", "java");

        sayNote("""
                Virtual threads shine on blocking workloads. \
                CPU-bound tasks (pure computation with no blocking) \
                show smaller differences because neither thread type \
                can exceed the physical core count.""");
    }

    // =========================================================================
    // Test 2: Blocking workload — Thread.sleep(1)
    // =========================================================================

    @Test
    void t02_blockingWorkload() {
        sayNextSection("Blocking Workload: Thread.sleep(1 ms) × 50 Tasks");

        say("""
                A 1 ms sleep models a minimal I/O wait — a database round-trip, \
                a cache lookup, or a network packet. With a platform-thread pool \
                limited to available processors, tasks queue behind each other. \
                Virtual threads park individually and resume when the sleep expires, \
                so the JVM can run many more tasks concurrently.""");

        Runnable sleepTask = () -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };

        var result = VirtualThreadComparator.compare("sleep-1ms", 50, sleepTask);

        sayCode("""
                Runnable sleepTask = () -> {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                };
                var result = VirtualThreadComparator.compare("sleep-1ms", 50, sleepTask);""",
                "java");

        renderComparisonTable(result);

        sayNote("""
                Results vary by machine. On a system with few cores the virtual-thread \
                advantage is most visible because the fixed pool queues many tasks. \
                On a heavily multi-core machine the gap narrows.""");

        // Structural assertions only — no timing assertions
        sayAndAssertThat("Result is not null", result, Matchers.notNullValue());
        sayAndAssertThat("Task count is 50", result.taskCount(), Matchers.equalTo(50));
        sayAndAssertThat("platformMs >= 0", result.platformMs() >= 0, Matchers.equalTo(true));
        sayAndAssertThat("virtualMs >= 0", result.virtualMs() >= 0, Matchers.equalTo(true));
        sayAndAssertThat("winner is 'virtual' or 'platform'",
                result.winner(),
                Matchers.anyOf(Matchers.equalTo("virtual"), Matchers.equalTo("platform")));
        sayAndAssertThat("speedupFactor >= 1.0", result.speedupFactor() >= 1.0, Matchers.equalTo(true));
    }

    // =========================================================================
    // Test 3: CPU-bound workload — integer summation
    // =========================================================================

    @Test
    void t03_cpuBoundWorkload() {
        sayNextSection("CPU-Bound Workload: Integer Summation × 50 Tasks");

        say("""
                For contrast, a CPU-bound task with no blocking shows how virtual threads \
                behave when the work never parks. Each task sums 10 000 integers — \
                pure arithmetic with no I/O. The expectation is that both executors \
                perform similarly because the bottleneck is CPU capacity, not thread \
                scheduling overhead.""");

        Runnable cpuTask = () -> {
            long sum = 0;
            for (int i = 0; i < 10_000; i++) {
                sum += i;
            }
            // prevent dead-code elimination via a cheap sink
            if (sum < 0) throw new AssertionError("unreachable");
        };

        var result = VirtualThreadComparator.compare("sum-10k", 50, cpuTask);

        sayCode("""
                Runnable cpuTask = () -> {
                    long sum = 0;
                    for (int i = 0; i < 10_000; i++) {
                        sum += i;
                    }
                };
                var result = VirtualThreadComparator.compare("sum-10k", 50, cpuTask);""",
                "java");

        renderComparisonTable(result);

        sayNote("""
                CPU-bound tasks produce results close to 1.0x speedup because \
                both thread models are bounded by the same number of physical cores. \
                Virtual threads add negligible overhead but cannot exceed hardware parallelism.""");

        sayAndAssertThat("Result is not null", result, Matchers.notNullValue());
        sayAndAssertThat("label is 'sum-10k'", result.label(), Matchers.equalTo("sum-10k"));
        sayAndAssertThat("speedupFactor >= 1.0", result.speedupFactor() >= 1.0, Matchers.equalTo(true));
    }

    // =========================================================================
    // Test 4: toMarkdown() rendering
    // =========================================================================

    @Test
    void t04_markdownRendering() {
        sayNextSection("ComparisonResult Markdown Rendering");

        say("""
                `VirtualThreadComparator.toMarkdown(result)` converts a \
                `ComparisonResult` into a list of markdown lines. This is the \
                bridge between the measurement layer and the documentation layer: \
                any DTR render machine can consume the output via `sayRaw()`.""");

        var result = VirtualThreadComparator.compare("markdown-demo", 20, () -> {
            // minimal work — this test exists to exercise toMarkdown(), not measure perf
            long x = System.nanoTime() % 1000;
            if (x < 0) throw new AssertionError("unreachable");
        });

        List<String> lines = VirtualThreadComparator.toMarkdown(result);

        sayCode("""
                var result  = VirtualThreadComparator.compare("markdown-demo", 20, task);
                var lines   = VirtualThreadComparator.toMarkdown(result);
                lines.forEach(line -> sayRaw(line));""", "java");

        say("Rendered output from `toMarkdown()`:");
        for (var line : lines) {
            sayRaw(line);
        }

        sayAndAssertThat("toMarkdown returns non-empty list", lines, Matchers.not(Matchers.empty()));
        sayAndAssertThat("first line is table header",
                lines.get(0),
                Matchers.containsString("Thread Type"));
    }

    // =========================================================================
    // Test 5: ComparisonResult record structure
    // =========================================================================

    @Test
    void t05_recordStructure() {
        sayNextSection("ComparisonResult Record Structure");

        say("""
                `ComparisonResult` is a Java record — an immutable, transparent carrier \
                for the six measurement fields. Records eliminate boilerplate (no getters, \
                no equals/hashCode/toString to write) and express intent clearly: \
                this type exists purely to hold data.""");

        sayRecordComponents(ComparisonResult.class);

        sayCode("""
                public record ComparisonResult(
                        String label,         // workload description
                        int    taskCount,     // tasks submitted per executor
                        long   platformMs,    // wall-clock ms for platform threads
                        long   virtualMs,     // wall-clock ms for virtual threads
                        double speedupFactor, // faster/slower ratio (>= 1.0)
                        String winner) {}     // "virtual" or "platform"
                """, "java");

        say("""
                The `speedupFactor` is always >= 1.0 and always describes the \
                winner-relative improvement: if virtual threads take 40 ms and \
                platform threads take 200 ms, speedupFactor = 5.0 and winner = "virtual".""");

        sayTable(new String[][] {
            {"Field", "Type", "Invariant"},
            {"label",         "String", "Non-null, human-readable workload name"},
            {"taskCount",     "int",    ">= 1"},
            {"platformMs",    "long",   ">= 0"},
            {"virtualMs",     "long",   ">= 0"},
            {"speedupFactor", "double", ">= 1.0"},
            {"winner",        "String", "Exactly \"virtual\" or \"platform\""},
        });

        var sample = new ComparisonResult("demo", 10, 200L, 40L, 5.0, "virtual");
        sayAndAssertThat("sample label", sample.label(), Matchers.equalTo("demo"));
        sayAndAssertThat("sample winner", sample.winner(), Matchers.equalTo("virtual"));
        sayAndAssertThat("sample speedupFactor", sample.speedupFactor(), Matchers.equalTo(5.0));
    }

    // =========================================================================
    // Private helper — shared table rendering
    // =========================================================================

    /**
     * Renders a standardised three-column comparison table plus a summary line
     * for the given {@code result}.
     */
    private void renderComparisonTable(ComparisonResult result) {
        sayTable(new String[][] {
            {"Thread Type", "Tasks", "Wall-clock (ms)", "Result"},
            {"Platform",
                    String.valueOf(result.taskCount()),
                    String.valueOf(result.platformMs()),
                    result.winner().equals("platform") ? "winner" : ""},
            {"Virtual",
                    String.valueOf(result.taskCount()),
                    String.valueOf(result.virtualMs()),
                    result.winner().equals("virtual") ? "winner" : ""},
        });

        say("**%s threads** finished first for `%s` — **%.2fx** speedup over %s threads.".formatted(
                capitalize(result.winner()),
                result.label(),
                result.speedupFactor(),
                result.winner().equals("virtual") ? "platform" : "virtual"));
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
