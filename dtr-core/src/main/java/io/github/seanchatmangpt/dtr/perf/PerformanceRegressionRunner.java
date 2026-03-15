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
package io.github.seanchatmangpt.dtr.perf;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs a {@link Runnable} task multiple times, measures average nanoseconds per
 * iteration, compares the result to a provided baseline, and reports whether
 * performance regressed beyond a configurable threshold.
 *
 * <p>All measurements use {@code System.nanoTime()} for monotonic, high-resolution
 * wall-clock timing. Warmup rounds are executed and discarded before the timed
 * measurement rounds begin, giving the JIT compiler time to reach steady state.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // Convenience overload — 10 warmup, 100 measure, 20% max regression
 * var result = PerformanceRegressionRunner.run(
 *     "String concat", 500L, () -> "hello" + " world");
 *
 * // Explicit control
 * var result = PerformanceRegressionRunner.run(
 *     "Array access", 200L, () -> array[42], 20, 200, 15.0);
 *
 * if (!result.passed()) {
 *     System.err.println("Regression detected: " + result.regressionPct() + "%");
 * }
 * }</pre>
 *
 * <h2>Regression formula</h2>
 * <pre>
 *   regressionPct = ((measuredNs - baselineNs) / baselineNs) * 100.0
 *   passed        = regressionPct &lt;= maxRegressionPct
 * </pre>
 *
 * <p>A negative {@code regressionPct} means the task ran <em>faster</em> than the
 * baseline — that always passes.</p>
 *
 * @since 2026.1.0
 */
public final class PerformanceRegressionRunner {

    private PerformanceRegressionRunner() {}

    // =========================================================================
    // Public API — result type
    // =========================================================================

    /**
     * Immutable result produced by {@link #run}.
     *
     * @param label          human-readable name for the task being measured
     * @param baselineNs     the expected/historical average nanoseconds per iteration
     * @param measuredNs     the actual average nanoseconds per iteration measured in this run
     * @param regressionPct  {@code ((measuredNs - baselineNs) / baselineNs) * 100.0};
     *                       negative means faster than baseline
     * @param passed         {@code true} when {@code regressionPct <= maxRegressionPct}
     * @param iterations     number of measurement iterations actually executed
     */
    public record RegressionResult(
            String label,
            long baselineNs,
            long measuredNs,
            double regressionPct,
            boolean passed,
            int iterations) {}

    // =========================================================================
    // Public API — run overloads
    // =========================================================================

    /**
     * Convenience overload with sensible defaults: 10 warmup rounds, 100 measure
     * rounds, and a 20 % maximum regression threshold.
     *
     * @param label      human-readable name for this check
     * @param baselineNs expected average nanoseconds per iteration (historical baseline)
     * @param task       the code to benchmark
     * @return a {@link RegressionResult} describing the outcome
     */
    public static RegressionResult run(String label, long baselineNs, Runnable task) {
        return run(label, baselineNs, task, 10, 100, 20.0);
    }

    /**
     * Full-control overload.
     *
     * <p>Executes {@code task} {@code warmupRounds} times and discards those
     * timings. Then executes {@code task} {@code measureRounds} times, recording
     * the elapsed nanoseconds for each iteration. The average of those samples is
     * compared against {@code baselineNs} to compute the regression percentage.</p>
     *
     * @param label            human-readable name for this check
     * @param baselineNs       expected average nanoseconds per iteration
     * @param task             the code to benchmark; must be non-null
     * @param warmupRounds     number of un-timed warmup iterations (must be &gt;= 0)
     * @param measureRounds    number of timed iterations (must be &gt;= 1)
     * @param maxRegressionPct maximum acceptable regression in percent (e.g. {@code 20.0}
     *                         means up to 20 % slower than baseline is still a pass)
     * @return a {@link RegressionResult} describing the outcome
     * @throws IllegalArgumentException if {@code measureRounds} &lt; 1
     */
    public static RegressionResult run(
            String label,
            long baselineNs,
            Runnable task,
            int warmupRounds,
            int measureRounds,
            double maxRegressionPct) {

        if (measureRounds < 1) {
            throw new IllegalArgumentException(
                    "measureRounds must be >= 1, got: " + measureRounds);
        }

        // --- warmup (results discarded) ---
        for (int i = 0; i < warmupRounds; i++) {
            task.run();
        }

        // --- measurement ---
        long[] samples = new long[measureRounds];
        for (int i = 0; i < measureRounds; i++) {
            long start = System.nanoTime();
            task.run();
            samples[i] = System.nanoTime() - start;
        }

        // --- compute average ---
        long sum = 0L;
        for (long s : samples) {
            sum += s;
        }
        long avgNs = sum / measureRounds;

        // --- regression analysis ---
        double regressionPct = baselineNs > 0
                ? ((double) (avgNs - baselineNs) / baselineNs) * 100.0
                : 0.0;
        boolean passed = regressionPct <= maxRegressionPct;

        return new RegressionResult(label, baselineNs, avgNs, regressionPct, passed, measureRounds);
    }

    // =========================================================================
    // Markdown rendering
    // =========================================================================

    /**
     * Formats a {@link RegressionResult} as a list of Markdown table lines
     * suitable for embedding in DTR documentation.
     *
     * <p>The returned list contains exactly five entries:</p>
     * <ol>
     *   <li>Table header row</li>
     *   <li>Separator row</li>
     *   <li>Data row with label, baseline (ns), measured (ns), delta %, and PASS/FAIL</li>
     * </ol>
     *
     * <p>Example output:</p>
     * <pre>
     * | Task | Baseline (ns) | Measured (ns) | Delta % | Status |
     * | --- | --- | --- | --- | --- |
     * | String concat | 500 | 312 | -37.60% | PASS |
     * </pre>
     *
     * @param result the result to render; must not be null
     * @return an unmodifiable list of Markdown lines (header + separator + data row)
     */
    public static List<String> toMarkdown(RegressionResult result) {
        String status = result.passed() ? "PASS" : "FAIL";
        String deltaFormatted = "%.2f%%".formatted(result.regressionPct());

        var lines = new ArrayList<String>(3);
        lines.add("| Task | Baseline (ns) | Measured (ns) | Delta % | Status |");
        lines.add("| --- | --- | --- | --- | --- |");
        lines.add("| %s | %d | %d | %s | %s |".formatted(
                result.label(),
                result.baselineNs(),
                result.measuredNs(),
                deltaFormatted,
                status));
        return List.copyOf(lines);
    }
}
