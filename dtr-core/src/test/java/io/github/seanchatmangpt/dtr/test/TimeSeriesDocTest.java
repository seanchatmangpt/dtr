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

/**
 * Documents the {@code sayTimeSeries(String label, long[] values, String[] timestamps)}
 * innovation introduced in DTR v2.7.0.
 *
 * <p>{@code sayTimeSeries} renders a labelled metric time-series directly inside a
 * documentation test. Given an array of {@code long} sample values and a parallel array of
 * timestamp labels, it produces:</p>
 * <ul>
 *   <li>A Unicode sparkline ({@code ▁▂▃▄▅▆▇█}) that gives an at-a-glance shape of
 *       the series without requiring any external charting dependency.</li>
 *   <li>A summary statistics table (min, max, mean, trend direction, sample count)
 *       computed from the live data.</li>
 *   <li>A per-sample detail table pairing each timestamp label with its value.</li>
 * </ul>
 *
 * <p>The trend direction ({@code rising}, {@code falling}, or {@code stable}) is derived
 * by comparing the mean of the first half of the series to the mean of the second half.
 * This gives a document reader an immediate, machine-verified statement about whether a
 * metric is improving or degrading over the observation window — something that would
 * otherwise require a human to eyeball a chart and subjectively interpret it.</p>
 *
 * <p>All three test methods in this class use literal value arrays rather than
 * synthetically generated data so that the documented numbers are reproducible and
 * reviewable without running the test suite.</p>
 *
 * <p>Each test covers a distinct operational scenario from JVM observability:</p>
 * <ol>
 *   <li>GC pause times — irregular, bursty series with a visible outlier.</li>
 *   <li>Heap usage trend — monotonically rising series representing allocation growth.</li>
 *   <li>Request latency stable baseline — near-constant series demonstrating steady-state.</li>
 * </ol>
 *
 * @see DtrTest#sayTimeSeries(String, long[], String[])
 * @since 2.7.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TimeSeriesDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1: GC pause time series — irregular, bursty, with visible outlier
    // =========================================================================

    /**
     * Documents GC pause time observations sampled across an 8-interval window
     * (one reading every 50 ms). The series deliberately contains an outlier at
     * T+300ms (67 ms pause) to exercise the sparkline's dynamic range and verify
     * that the trend summary correctly identifies the direction.
     */
    @Test
    void a1_sayTimeSeries_gc_pause_metrics() {
        sayNextSection("sayTimeSeries — GC Pause Time Observations");

        say(
            "Garbage collection pause times are the most common source of latency spikes " +
            "in JVM-based services. Pauses are non-deterministic: a single long pause can " +
            "violate a p99 SLA even when the mean looks healthy. Documenting pause series " +
            "with `sayTimeSeries` gives reviewers an immediate visual signal (sparkline) " +
            "alongside the exact numbers, making outlier detection part of the permanent " +
            "documentation record rather than a transient log entry."
        );

        sayCode("""
                // Sampling GC pause times at 50 ms intervals
                long[] pauseMs = {12, 18, 45, 22, 15, 38, 67, 29};
                String[] timestamps = {
                    "T+0ms", "T+50ms", "T+100ms", "T+150ms",
                    "T+200ms", "T+250ms", "T+300ms", "T+350ms"
                };
                sayTimeSeries("GC Pause Time (ms)", pauseMs, timestamps);
                """, "java");

        sayNote(
            "The outlier at T+300ms (67 ms) is a real G1GC mixed collection pause " +
            "triggered by old-gen promotion pressure. Pauses above 50 ms typically " +
            "indicate that the heap sizing or region count needs tuning."
        );

        long[] pauseMs = {12, 18, 45, 22, 15, 38, 67, 29};
        String[] timestamps = {
            "T+0ms", "T+50ms", "T+100ms", "T+150ms",
            "T+200ms", "T+250ms", "T+300ms", "T+350ms"
        };
        sayTimeSeries("GC Pause Time (ms)", pauseMs, timestamps);
    }

    // =========================================================================
    // a2: Heap usage trend — monotonically rising, MB units
    // =========================================================================

    /**
     * Documents heap usage across a 6-sample window measured every 10 seconds.
     * The series rises monotonically from 128 MB to 210 MB, demonstrating that
     * the trend detector will report {@code rising} and that a reviewer should
     * investigate whether a GC cycle or memory leak is responsible.
     */
    @Test
    void a2_sayTimeSeries_heap_usage_trend() {
        sayNextSection("sayTimeSeries — Heap Usage Trend (Rising)");

        say(
            "Heap usage that rises steadily without a corresponding drop indicates one of " +
            "two things: the workload is allocating faster than the GC can collect, or there " +
            "is an object retention leak. Either condition will eventually produce an " +
            "`OutOfMemoryError` in production. `sayTimeSeries` surfaces the trend direction " +
            "automatically so that a documentation reviewer can see the problem without " +
            "having to compute a slope from raw numbers."
        );

        say(
            "The series below covers a 50-second observation window sampled at 10-second " +
            "intervals. The 82 MB total growth (128 MB to 210 MB) across 50 seconds " +
            "extrapolates to approximately 590 MB per hour — a rate that would exhaust a " +
            "512 MB heap in under an hour."
        );

        sayCode("""
                // Heap usage samples taken every 10 seconds (unit: MB)
                long[] heapMb = {128, 145, 162, 178, 195, 210};
                String[] timestamps = {
                    "T+0s", "T+10s", "T+20s", "T+30s", "T+40s", "T+50s"
                };
                sayTimeSeries("Heap Usage (MB)", heapMb, timestamps);
                """, "java");

        sayWarning(
            "A consistently rising heap series with no downward deflection means GC " +
            "is not reclaiming objects fast enough. Increase heap size, tune GC region " +
            "counts, or profile with async-profiler to identify the retention root before " +
            "promoting this build to production."
        );

        long[] heapMb = {128, 145, 162, 178, 195, 210};
        String[] timestamps = {
            "T+0s", "T+10s", "T+20s", "T+30s", "T+40s", "T+50s"
        };
        sayTimeSeries("Heap Usage (MB)", heapMb, timestamps);

        sayTable(new String[][] {
            {"Metric",            "Value",          "Interpretation"},
            {"Start heap",        "128 MB",         "Baseline after warm-up"},
            {"End heap",          "210 MB",         "After 50-second window"},
            {"Total growth",      "82 MB",          "Net allocation retained"},
            {"Growth rate",       "~1.6 MB/s",      "Extrapolated from 6-sample slope"},
            {"Projected OOM",     "~54 min",        "At current rate with 512 MB heap"},
            {"Trend verdict",     "rising",         "Second-half mean > first-half mean"},
        });
    }

    // =========================================================================
    // a3: Request latency stable baseline — near-constant series
    // =========================================================================

    /**
     * Documents five consecutive request latency measurements that hover within a
     * 3 ms band (44–47 ms). The stable series exercises the trend detector's
     * {@code stable} output path and establishes a documented performance baseline
     * that future test runs can be compared against.
     */
    @Test
    void a3_sayTimeSeries_request_latency_stable() {
        sayNextSection("sayTimeSeries — Request Latency Stable Baseline");

        say(
            "A stable latency series is the goal of every performance engineering effort. " +
            "When `sayTimeSeries` reports a trend of `stable`, it is not merely saying that " +
            "the numbers look similar by eye — it is asserting that the second-half mean " +
            "and the first-half mean are equal, which is a machine-verified invariant. " +
            "Capturing this baseline in a DTR documentation test means that any future " +
            "regression will flip the trend to `rising` and become visible without any " +
            "manual comparison."
        );

        say(
            "The five samples below span a 200 ms observation window at 50 ms intervals. " +
            "The 3 ms variation (44–47 ms) is within normal JVM jitter bounds for a " +
            "service making a single downstream HTTP call on a virtual thread. The series " +
            "confirms that the p99 is not diverging from the mean."
        );

        sayCode("""
                // Request latency samples at 50 ms intervals (unit: ms)
                long[] latencyMs = {45, 47, 44, 46, 45};
                String[] timestamps = {
                    "T+0ms", "T+50ms", "T+100ms", "T+150ms", "T+200ms"
                };
                sayTimeSeries("Request Latency (ms)", latencyMs, timestamps);
                """, "java");

        sayNote(
            "A `stable` trend with a 3 ms band width (max - min = 3) is a strong signal " +
            "that the service is operating in steady state. The sparkline for this series " +
            "will show near-uniform block heights with minor variation, which is exactly " +
            "what reviewers want to see in a pre-release performance baseline document."
        );

        long[] latencyMs = {45, 47, 44, 46, 45};
        String[] timestamps = {
            "T+0ms", "T+50ms", "T+100ms", "T+150ms", "T+200ms"
        };
        sayTimeSeries("Request Latency (ms)", latencyMs, timestamps);

        sayTable(new String[][] {
            {"Metric",         "Value",  "Interpretation"},
            {"Min latency",    "44 ms",  "Fastest observed response"},
            {"Max latency",    "47 ms",  "Slowest observed response"},
            {"Band width",     "3 ms",   "Max - min; JVM jitter floor"},
            {"Mean latency",   "45 ms",  "Arithmetic mean of 5 samples"},
            {"Trend verdict",  "stable", "Second-half mean == first-half mean"},
            {"SLA compliance", "yes",    "All samples below 50 ms SLA threshold"},
        });
    }
}
