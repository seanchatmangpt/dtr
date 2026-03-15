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
import io.github.seanchatmangpt.dtr.parallel.ParallelBenchmarkRunner;
import io.github.seanchatmangpt.dtr.parallel.ParallelBenchmarkRunner.BenchmarkLevel;
import io.github.seanchatmangpt.dtr.parallel.ParallelBenchmarkRunner.ParallelBenchmarkResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Documentation test for {@link ParallelBenchmarkRunner}.
 *
 * <p>Each test method exercises the runner with a lightweight, CPU-bound workload
 * and documents the results using the DTR {@code say*} API. Timing assertions are
 * deliberately absent — wall-clock results are non-deterministic across environments.
 * Only structural invariants (non-null results, correct record counts) are asserted.</p>
 *
 * <p>All tasks are designed to complete quickly: fast math and string operations
 * on small task counts (max 100) keep the full suite well under 10 seconds even on
 * constrained CI runners.</p>
 *
 * @since 2026.1.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ParallelBenchmarkDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: Overview and API walkthrough
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Parallel Benchmark — Virtual Thread Scalability");

        say("""
                `ParallelBenchmarkRunner` measures how a task scales with increasing \
                concurrency by submitting it at four thread levels: 1, 2, 4, and 8. \
                Each level uses Java 26 virtual threads via \
                `Executors.newVirtualThreadPerTaskExecutor()`, giving every submitted \
                task its own lightweight carrier thread. Throughput (tasks per second) \
                is recorded at each level so that scalability trends — linear, sub-linear, \
                or super-linear — are visible at a glance.""");

        say("""
                Because virtual threads are cheap to create and park, the cost of spinning \
                up 8 threads instead of 1 is negligible. The dominant factor is whether \
                the task itself can exploit parallelism — CPU-bound tasks are limited by \
                core count, while I/O-bound tasks benefit strongly from higher concurrency.""");

        sayCode("""
                // Run a workload at thread levels [1, 2, 4, 8]
                var result = ParallelBenchmarkRunner.run(
                        "sqrt-random",          // workload label
                        50,                     // tasks per level
                        () -> Math.sqrt(Math.random()));

                // Render as a Markdown table
                ParallelBenchmarkRunner.toMarkdown(result).forEach(line -> sayRaw(line));
                """, "java");

        sayNote("""
                Virtual threads do not increase CPU parallelism beyond the number of \
                physical cores. For CPU-bound tasks, throughput at 8 threads will not \
                exceed 8× single-thread throughput, and may plateau at the core count. \
                The benchmark documents whatever the JVM measures — no numbers are \
                hard-coded.""");
    }

    // =========================================================================
    // Test 2: Math workload — Math.sqrt(Math.random())
    // =========================================================================

    @Test
    void t02_mathWorkload() {
        sayNextSection("Math Workload: Math.sqrt(Math.random()) x 50 Tasks");

        say("""
                A `Math.sqrt(Math.random())` call exercises both the pseudo-random \
                number generator and a native floating-point square-root instruction. \
                It is fast enough to complete 50 tasks per level in milliseconds while \
                being non-trivial enough to avoid complete dead-code elimination.""");

        // Document via the DtrTest sayParallelBenchmark method (routes through renderMachine)
        sayParallelBenchmark("sqrt-random", 50, () -> Math.sqrt(Math.random()));

        // Also run directly to assert structural invariants
        var result = ParallelBenchmarkRunner.run("sqrt-random-assert", 50,
                () -> Math.sqrt(Math.random()));

        assertNotNull(result, "ParallelBenchmarkResult must not be null");
        assertNotNull(result.label(), "label must not be null");
        assertNotNull(result.levels(), "levels list must not be null");

        say("The `sayParallelBenchmark()` call above routes to `ParallelBenchmarkRunner.run()` " +
                "internally and renders the result through the DTR render pipeline. " +
                "The direct `run()` call below asserts structural correctness.");

        sayAssertions(java.util.Map.of(
                "result is not null", "PASS",
                "label is not null", "PASS",
                "levels list is not null", "PASS"));
    }

    // =========================================================================
    // Test 3: String-ops workload — toMarkdown rendering
    // =========================================================================

    @Test
    void t03_levels() {
        sayNextSection("String-Ops Workload: toMarkdown() Rendering");

        say("""
                `\"test\".toUpperCase()` is a minimal string operation: it allocates a new \
                `String` object and copies characters with an uppercase transform. \
                Repeated across 100 tasks at four concurrency levels, it documents how \
                string allocation behaves under concurrent virtual-thread pressure.""");

        var result = ParallelBenchmarkRunner.run("string-ops", 100,
                () -> "test".toUpperCase());

        sayCode("""
                var result = ParallelBenchmarkRunner.run("string-ops", 100,
                        () -> "test".toUpperCase());
                var markdownLines = ParallelBenchmarkRunner.toMarkdown(result);
                markdownLines.forEach(line -> sayRaw(line));
                """, "java");

        List<String> markdownLines = ParallelBenchmarkRunner.toMarkdown(result);

        say("Raw Markdown output from `toMarkdown()`:");
        for (var line : markdownLines) {
            sayRaw(line);
        }

        sayNote("""
                `toMarkdown()` returns one heading line, a blank line, a table header, \
                a separator row, four data rows (one per thread level), a blank line, \
                and a summary sentence identifying the best-throughput level.""");

        assertEquals(4, result.levels().size(),
                "ParallelBenchmarkResult must contain exactly 4 levels (1, 2, 4, 8 threads)");

        sayAssertions(java.util.Map.of(
                "levels().size() == 4", "PASS"));
    }

    // =========================================================================
    // Test 4: Throughput scaling — best level documented with sayKeyValue
    // =========================================================================

    @Test
    void t04_throughputScaling() {
        sayNextSection("Throughput Scaling: Best Concurrency Level");

        say("""
                This test identifies the thread level that achieved the highest throughput \
                and documents it using `sayKeyValue()`. Because the workload (integer \
                increment) is extremely fast and CPU-bound, the best level is typically \
                the one that most closely matches the number of available cores.""");

        var result = ParallelBenchmarkRunner.run("counter-ops", 50, () -> {
            // Simple integer arithmetic to prevent dead-code elimination
            int x = 0;
            for (int i = 0; i < 1_000; i++) {
                x += i;
            }
            if (x < 0) throw new AssertionError("unreachable");
        });

        // Find the best-throughput level
        BenchmarkLevel best = result.levels().stream()
                .max(java.util.Comparator.comparingLong(BenchmarkLevel::throughput))
                .orElseThrow();

        // Use a LinkedHashMap to preserve insertion order for readable output
        var kv = new LinkedHashMap<String, String>();
        kv.put("Workload", result.label());
        kv.put("Best thread level", String.valueOf(best.threads()));
        kv.put("Best throughput (tasks/s)", String.valueOf(best.throughput()));
        kv.put("Duration at best level (ms)", String.valueOf(best.durationMs()));
        kv.put("Tasks completed at best level", String.valueOf(best.tasksCompleted()));

        sayKeyValue(kv);

        sayNote("""
                Throughput values are proportional to the machine's core count and \
                JVM warm-up state. The documented numbers are real measurements — not \
                estimates — captured at test runtime.""");

        assertTrue(best.throughput() > 0,
                "Best throughput must be greater than zero (at least one task completed)");

        sayAssertions(java.util.Map.of(
                "best.throughput() > 0", "PASS"));
    }

    // =========================================================================
    // Test 5: Record structure — ParallelBenchmarkResult schema
    // =========================================================================

    @Test
    void t05_recordStructure() {
        sayNextSection("ParallelBenchmarkResult Record Structure");

        say("""
                `ParallelBenchmarkResult` is a Java record — an immutable, transparent \
                data carrier for the workload label and list of per-level measurements. \
                `BenchmarkLevel` is also a record, carrying the four fields that describe \
                each concurrency level: `threads`, `durationMs`, `tasksCompleted`, and \
                `throughput`. Using records eliminates boilerplate and prevents accidental \
                mutation of benchmark results after collection.""");

        sayRecordComponents(ParallelBenchmarkResult.class);

        say("The `BenchmarkLevel` record that makes up each entry in `levels()`:");

        sayRecordComponents(BenchmarkLevel.class);

        sayCode("""
                public record ParallelBenchmarkResult(
                        String           label,   // workload description
                        List<BenchmarkLevel> levels) {} // one entry per thread level

                public record BenchmarkLevel(
                        int  threads,          // concurrency level used
                        long durationMs,       // wall-clock time for all tasks at this level
                        long tasksCompleted,   // tasks that finished without error
                        long throughput) {}    // tasksCompleted * 1000 / durationMs
                """, "java");

        sayTable(new String[][] {
            {"Field",            "Type",                "Invariant"},
            {"label",            "String",              "Non-null, human-readable"},
            {"levels",           "List<BenchmarkLevel>","Exactly 4 entries: [1, 2, 4, 8]"},
            {"threads",          "int",                 "One of 1, 2, 4, 8"},
            {"durationMs",       "long",                ">= 1"},
            {"tasksCompleted",   "long",                ">= 0"},
            {"throughput",       "long",                ">= 0 (tasks/s)"},
        });

        // Construct a sample to assert record accessor behaviour
        var sampleLevel = new BenchmarkLevel(4, 25L, 50L, 2000L);
        var sampleResult = new ParallelBenchmarkResult("demo",
                List.of(sampleLevel));

        assertNotNull(sampleResult, "constructed ParallelBenchmarkResult must not be null");
        assertEquals("demo", sampleResult.label());
        assertEquals(1, sampleResult.levels().size());
        assertEquals(4, sampleResult.levels().get(0).threads());
        assertEquals(2000L, sampleResult.levels().get(0).throughput());

        sayAssertions(java.util.Map.of(
                "sampleResult is not null", "PASS",
                "label == \"demo\"", "PASS",
                "levels().size() == 1", "PASS",
                "level threads == 4", "PASS",
                "level throughput == 2000", "PASS"));
    }
}
