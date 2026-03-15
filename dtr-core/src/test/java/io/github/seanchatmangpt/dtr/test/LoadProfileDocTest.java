/*
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
import io.github.seanchatmangpt.dtr.load.LoadProfileRunner;
import io.github.seanchatmangpt.dtr.load.LoadProfileRunner.LoadResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Documentation test for {@link LoadProfileRunner}.
 *
 * <p>Each test method drives the runner against a deterministic, CPU-bound task
 * (integer arithmetic) and documents the results using {@code sayNextSection()},
 * {@code say()}, {@code sayTable()}, {@code sayCode()}, and {@code sayNote()}.
 * Tests never fail due to timing variability — they document whatever the JVM
 * measures and assert only that the result is structurally sound and that at
 * least one operation completed.</p>
 *
 * <p>The total test suite completes well under 5 seconds: each scenario uses
 * a 500 ms load window, and there are two scenarios.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class LoadProfileDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: Basic concurrent load profile
    // =========================================================================

    /**
     * Runs a 500 ms load test with 4 platform threads executing simple integer
     * arithmetic and documents the resulting throughput and latency distribution.
     */
    @Test
    void test1_basicConcurrentLoadProfile() {
        sayNextSection("Basic Concurrent Load Profile: Integer Arithmetic");

        say(
            "`LoadProfileRunner` measures how a task behaves under real concurrent " +
            "pressure. Platform threads (not virtual threads) are used deliberately: " +
            "they expose scheduling contention and produce representative latency " +
            "distributions that reflect actual OS-level competition for CPU time.");

        say(
            "The following example runs a simple integer-addition loop on 4 threads " +
            "for 500 milliseconds. Every invocation's wall-clock latency is recorded " +
            "in a lock-free queue, then sorted to derive p50, p95, and p99 percentiles.");

        sayCode("""
                // 4 platform threads, 500 ms window, simple math task
                var result = LoadProfileRunner.run(
                    "math-add",        // label
                    4,                 // concurrent threads
                    500L,              // duration in milliseconds
                    () -> {
                        int x = 0;
                        for (int i = 0; i < 100; i++) {
                            x += i * 3;
                        }
                        // prevent dead-code elimination
                        if (x < 0) throw new AssertionError("impossible");
                    });
                """, "java");

        // Run the real load test — deterministic math, no I/O
        var result = LoadProfileRunner.run(
                "math-add",
                4,
                500L,
                () -> {
                    int x = 0;
                    for (int i = 0; i < 100; i++) {
                        x += i * 3;
                    }
                    if (x < 0) throw new AssertionError("impossible");
                });

        // Document result summary
        sayTable(new String[][] {
            {"Metric",                "Value"},
            {"Label",                 result.label()},
            {"Threads",               String.valueOf(result.threads())},
            {"Duration (ms)",         String.valueOf(result.durationMs())},
            {"Total ops",             String.valueOf(result.totalOps())},
            {"Throughput (ops/sec)",  "%.1f".formatted(result.opsPerSec())},
            {"Latency p50 (ms)",      String.valueOf(result.latency().p50Ms())},
            {"Latency p95 (ms)",      String.valueOf(result.latency().p95Ms())},
            {"Latency p99 (ms)",      String.valueOf(result.latency().p99Ms())},
            {"Latency min (ms)",      String.valueOf(result.latency().minMs())},
            {"Latency max (ms)",      String.valueOf(result.latency().maxMs())},
            {"Latency avg (ms)",      "%.3f".formatted(result.latency().avgMs())}
        });

        sayNote(
            "Latency values near zero (0 ms) are expected for a CPU-bound micro-task: " +
            "each invocation completes in sub-millisecond time, so the millisecond " +
            "resolution of the stats rounds down to zero. The throughput figure " +
            "(ops/sec) is the meaningful signal for this class of workload.");

        // Structural assertions only — never assert on timing values
        sayAndAssertThat("LoadResult is not null", result, notNullValue());
        sayAndAssertThat("At least one operation completed", result.totalOps(), greaterThan(0L));
    }

    // =========================================================================
    // Test 2: Markdown rendering of load results
    // =========================================================================

    /**
     * Documents the Markdown output produced by
     * {@link LoadProfileRunner#toMarkdown(LoadResult)} and shows how it maps
     * to a native DTR table.
     */
    @Test
    void test2_markdownRenderingOfLoadResults() {
        sayNextSection("Load Profile Markdown Rendering");

        say(
            "`LoadProfileRunner.toMarkdown(result)` converts a `LoadResult` into " +
            "a list of Markdown table lines. This is useful for embedding load " +
            "evidence directly into generated documentation outside of DTR's own " +
            "render pipeline — for example, in a GitHub Actions summary or a " +
            "standalone report.");

        // Run a second short load scenario for the rendering demo
        var result = LoadProfileRunner.run(
                "string-build",
                2,
                500L,
                () -> {
                    var sb = new StringBuilder();
                    for (int i = 0; i < 10; i++) {
                        sb.append(i);
                    }
                    // use result to prevent dead-code elimination
                    if (sb.length() < 0) throw new AssertionError("impossible");
                });

        // Show the raw markdown lines
        List<String> markdownLines = LoadProfileRunner.toMarkdown(result);
        var rawMarkdown = String.join("\n", markdownLines);

        say("The `toMarkdown()` method produces the following raw Markdown:");
        sayCode(rawMarkdown, "markdown");

        say("When rendered via DTR's `sayTable()` the same data looks like this:");

        sayTable(new String[][] {
            {"Metric",                "Value"},
            {"Label",                 result.label()},
            {"Threads",               String.valueOf(result.threads())},
            {"Duration (ms)",         String.valueOf(result.durationMs())},
            {"Total ops",             String.valueOf(result.totalOps())},
            {"Throughput (ops/sec)",  "%.1f".formatted(result.opsPerSec())},
            {"Latency p50 (ms)",      String.valueOf(result.latency().p50Ms())},
            {"Latency p95 (ms)",      String.valueOf(result.latency().p95Ms())},
            {"Latency p99 (ms)",      String.valueOf(result.latency().p99Ms())},
            {"Latency avg (ms)",      "%.3f".formatted(result.latency().avgMs())}
        });

        sayNote(
            "The `toMarkdown()` output and the `sayTable()` rendering carry identical " +
            "data. Choose `toMarkdown()` when you need raw Markdown strings for an " +
            "external consumer; use `sayTable()` when documenting inside a DTR test.");

        // Structural assertions — never assert on timing values
        sayAndAssertThat("toMarkdown lines are not null", markdownLines, notNullValue());
        sayAndAssertThat("At least one operation completed", result.totalOps(), greaterThan(0L));
    }
}
