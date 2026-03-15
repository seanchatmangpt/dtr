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
import io.github.seanchatmangpt.dtr.perf.PerformanceRegressionRunner;
import io.github.seanchatmangpt.dtr.perf.PerformanceRegressionRunner.RegressionResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.hamcrest.Matchers.notNullValue;

/**
 * Documentation test for {@link PerformanceRegressionRunner}.
 *
 * <p>Each test method runs the runner against a real, deterministic task
 * (String operations, integer arithmetic) and documents the results using
 * {@code sayTable()}, {@code sayCode()}, {@code sayNote()}, and
 * {@code sayWarning()}. Tests never fail due to timing — they document
 * whatever the JVM measures and assert only that the result object is
 * well-formed.</p>
 *
 * <p>This file demonstrates the full workflow for embedding performance
 * regression checks into DTR-based documentation pipelines.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class PerformanceRegressionDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1: Passing regression check — String concatenation
    // =========================================================================

    /**
     * Documents a passing regression check. A very generous baseline of 500 ns
     * is provided for a simple String concatenation — on modern JVMs, this task
     * typically completes in well under 500 ns, so the regression percentage
     * will be negative (faster than baseline) and the check will always pass.
     */
    @Test
    void test1_passingRegressionCheck() {
        sayNextSection("Passing Regression Check: String Concatenation");

        say(
            "`PerformanceRegressionRunner` measures a task against a historical " +
            "baseline nanosecond budget. When the task runs faster than the baseline " +
            "the regression percentage is negative and the check is marked **PASS**. " +
            "The following example benchmarks a simple String concatenation with a " +
            "500 ns baseline — generous enough to guarantee a pass on any modern JVM.");

        sayCode("""
                // Convenience overload: 10 warmup, 100 measure, 20% max regression
                var result = PerformanceRegressionRunner.run(
                    "String concat",   // label
                    500L,              // baseline: 500 ns
                    () -> "hello" + " world");

                // result.passed()       → true  (ran faster than 500 ns)
                // result.regressionPct() → negative value (e.g. -37.6%)
                """, "java");

        // Run the real benchmark
        var result = PerformanceRegressionRunner.run(
                "String concat", 500L, () -> "hello" + " world");

        // Document results in a table
        sayTable(new String[][] {
            {"Metric", "Value"},
            {"Label",            result.label()},
            {"Baseline (ns)",    String.valueOf(result.baselineNs())},
            {"Measured avg (ns)", String.valueOf(result.measuredNs())},
            {"Delta %",          "%.2f%%".formatted(result.regressionPct())},
            {"Iterations",       String.valueOf(result.iterations())},
            {"Status",           result.passed() ? "PASS" : "FAIL"}
        });

        sayNote(
            "A negative delta % means the implementation ran faster than the baseline. " +
            "The check passes as long as the delta does not exceed the configured threshold " +
            "(default: 20%).");

        // Assert result is well-formed — never assert on timing values
        sayAndAssertThat("RegressionResult is not null", result, notNullValue());
        sayAndAssertThat("Label is not null", result.label(), notNullValue());
    }

    // =========================================================================
    // Test 2: Regression report format — array access + markdown rendering
    // =========================================================================

    /**
     * Documents the Markdown output format produced by
     * {@link PerformanceRegressionRunner#toMarkdown(RegressionResult)}.
     * Uses an integer array access task — highly deterministic and JIT-friendly —
     * to generate a real result, then renders both the raw Markdown lines and a
     * formatted table so readers understand exactly what the output looks like in
     * generated documentation.
     */
    @Test
    void test2_regressionReportFormat() {
        sayNextSection("Regression Report Format: Markdown Rendering");

        say(
            "`PerformanceRegressionRunner.toMarkdown(result)` converts a " +
            "`RegressionResult` into a three-line Markdown table: a header row, " +
            "a separator row, and one data row. This is the canonical format for " +
            "embedding regression evidence directly into generated documentation.");

        // Benchmark: integer array access — deterministic, no allocations
        int[] data = new int[1024];
        for (int i = 0; i < data.length; i++) {
            data[i] = i * 3;
        }
        // Baseline of 200 ns is generous for a simple array read on modern hardware
        var result = PerformanceRegressionRunner.run(
                "int[] access", 200L,
                () -> {
                    int sum = 0;
                    for (int i = 0; i < 16; i++) {
                        sum += data[i];
                    }
                    // Use sum to prevent dead-code elimination
                    if (sum < 0) throw new AssertionError("impossible");
                },
                10, 100, 20.0);

        // Show the raw markdown lines as a code block
        List<String> markdownLines = PerformanceRegressionRunner.toMarkdown(result);
        var rawMarkdown = String.join("\n", markdownLines);

        say("The `toMarkdown()` method produces the following raw Markdown:");

        sayCode(rawMarkdown, "markdown");

        say("When rendered that Markdown table looks like this in the generated documentation:");

        // Render the same data via sayTable so it appears as a native DTR table
        sayTable(new String[][] {
            {"Task", "Baseline (ns)", "Measured (ns)", "Delta %", "Status"},
            {
                result.label(),
                String.valueOf(result.baselineNs()),
                String.valueOf(result.measuredNs()),
                "%.2f%%".formatted(result.regressionPct()),
                result.passed() ? "PASS" : "FAIL"
            }
        });

        sayNote(
            "The `toMarkdown()` output is identical in content to the DTR table above. " +
            "The difference is format: `toMarkdown()` returns raw Markdown strings for " +
            "embedding in external pipelines, while `sayTable()` feeds the DTR render machine.");

        sayWarning(
            "Do not assert on measured nanosecond values in CI — wall-clock timing varies " +
            "with JVM warm-up state, CPU throttling, and container resource limits. " +
            "Baselines should be measured on the target hardware and stored as constants.");

        // Full-control overload demonstration with explicit round counts
        sayNextSection("Full-Control Overload: Explicit Warmup and Measure Rounds");

        say(
            "When the default settings (10 warmup / 100 measure / 20% threshold) are not " +
            "appropriate, the full-control overload accepts explicit parameters:");

        sayCode("""
                var result = PerformanceRegressionRunner.run(
                    "int[] access",   // label
                    200L,             // baseline: 200 ns
                    () -> data[42],   // task
                    20,               // warmupRounds
                    200,              // measureRounds
                    15.0              // maxRegressionPct: 15%
                );
                """, "java");

        var detailedResult = PerformanceRegressionRunner.run(
                "int[] single access", 200L,
                () -> {
                    int v = data[42];
                    if (v < 0) throw new AssertionError("impossible");
                },
                20, 200, 15.0);

        sayTable(new String[][] {
            {"Parameter", "Value"},
            {"Warmup rounds",     "20"},
            {"Measure rounds",    String.valueOf(detailedResult.iterations())},
            {"Baseline (ns)",     String.valueOf(detailedResult.baselineNs())},
            {"Measured avg (ns)", String.valueOf(detailedResult.measuredNs())},
            {"Delta %",           "%.2f%%".formatted(detailedResult.regressionPct())},
            {"Max threshold",     "15.0%"},
            {"Status",            detailedResult.passed() ? "PASS" : "FAIL"}
        });

        // Assert structural properties — never timing values
        sayAndAssertThat("toMarkdown result is not null", markdownLines, notNullValue());
        sayAndAssertThat("Detailed result is not null", detailedResult, notNullValue());
        sayAndAssertThat("Detailed result label is not null",
                detailedResult.label(), notNullValue());
    }
}
