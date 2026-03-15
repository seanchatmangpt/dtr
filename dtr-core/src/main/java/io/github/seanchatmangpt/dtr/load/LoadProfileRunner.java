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
package io.github.seanchatmangpt.dtr.load;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs a {@link Runnable} task under concurrent load for a fixed duration,
 * collects per-invocation latency samples, and computes throughput and
 * percentile statistics.
 *
 * <p>Platform threads are used intentionally — virtual threads would mask
 * contention, whereas platform threads expose real scheduling pressure and
 * produce more representative latency distributions for stress testing.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * var result = LoadProfileRunner.run("math-add", 4, 500L,
 *     () -> { int x = 3 + 5; });
 *
 * // result.totalOps()          → total invocations across all threads
 * // result.opsPerSec()         → throughput
 * // result.latency().p99Ms()   → 99th-percentile latency in ms
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class LoadProfileRunner {

    private LoadProfileRunner() {}

    // =========================================================================
    // Public result types
    // =========================================================================

    /**
     * Percentile and min/max latency statistics derived from raw samples.
     *
     * @param p50Ms  50th-percentile latency in milliseconds
     * @param p95Ms  95th-percentile latency in milliseconds
     * @param p99Ms  99th-percentile latency in milliseconds
     * @param minMs  minimum observed latency in milliseconds
     * @param maxMs  maximum observed latency in milliseconds
     * @param avgMs  arithmetic mean latency in milliseconds
     */
    public record LatencyStats(
            long p50Ms,
            long p95Ms,
            long p99Ms,
            long minMs,
            long maxMs,
            double avgMs) {}

    /**
     * Immutable result produced by {@link #run}.
     *
     * @param label       human-readable name for the load scenario
     * @param threads     number of concurrent platform threads used
     * @param durationMs  wall-clock duration of the test window in milliseconds
     * @param totalOps    total task invocations completed across all threads
     * @param opsPerSec   throughput: {@code totalOps / (durationMs / 1000.0)}
     * @param latency     percentile latency statistics
     */
    public record LoadResult(
            String label,
            int threads,
            long durationMs,
            long totalOps,
            double opsPerSec,
            LatencyStats latency) {}

    // =========================================================================
    // Public API — run
    // =========================================================================

    /**
     * Runs {@code task} concurrently on {@code threads} platform threads for
     * {@code durationMs} milliseconds, then returns aggregated statistics.
     *
     * <p>Each worker thread loops continuously, invoking the task and recording
     * the wall-clock latency (in nanoseconds, converted to milliseconds for the
     * stats) of every invocation. A {@code volatile boolean} flag signals all
     * threads to stop after the duration elapses. Threads are joined with a
     * per-thread timeout of {@code durationMs + 2000} ms to avoid blocking
     * indefinitely on a misbehaving task.</p>
     *
     * @param label      human-readable identifier for this load scenario
     * @param threads    number of concurrent platform threads (must be >= 1)
     * @param durationMs how long to run the load test in milliseconds (must be >= 1)
     * @param task       the operation to stress-test; must be thread-safe
     * @return a {@link LoadResult} with throughput and latency statistics
     * @throws IllegalArgumentException if {@code threads} or {@code durationMs} is less than 1
     */
    public static LoadResult run(
            String label,
            int threads,
            long durationMs,
            Runnable task) {

        if (threads < 1) {
            throw new IllegalArgumentException("threads must be >= 1, got: " + threads);
        }
        if (durationMs < 1) {
            throw new IllegalArgumentException("durationMs must be >= 1, got: " + durationMs);
        }

        // Shared latency collector — ConcurrentLinkedQueue is lock-free for producers
        var latenciesNs = new ConcurrentLinkedQueue<Long>();

        // AtomicBoolean flag: workers read this on every iteration to know when to stop
        var stop = new AtomicBoolean(false);

        // Build and start platform threads
        var workerThreads = new ArrayList<Thread>(threads);
        for (int t = 0; t < threads; t++) {
            var worker = new Thread(() -> {
                while (!stop.get()) {
                    long start = System.nanoTime();
                    task.run();
                    long elapsed = System.nanoTime() - start;
                    latenciesNs.offer(elapsed);
                }
            });
            worker.setDaemon(true);
            workerThreads.add(worker);
        }

        long wallStart = System.nanoTime();
        for (var worker : workerThreads) {
            worker.start();
        }

        // Let workers run for the requested duration
        try {
            Thread.sleep(durationMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Signal all workers to stop
        stop.set(true);
        long wallElapsedMs = (System.nanoTime() - wallStart) / 1_000_000L;

        // Join all workers with a generous timeout
        long joinTimeoutMs = durationMs + 2_000L;
        for (var worker : workerThreads) {
            try {
                worker.join(joinTimeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Convert nanosecond samples to milliseconds for the stats API
        var latenciesMs = new ArrayList<Long>(latenciesNs.size());
        for (long ns : latenciesNs) {
            latenciesMs.add(ns / 1_000_000L);
        }

        long totalOps = latenciesMs.size();
        double actualDurationSec = Math.max(wallElapsedMs, 1L) / 1_000.0;
        double opsPerSec = totalOps / actualDurationSec;

        LatencyStats stats = latenciesMs.isEmpty()
                ? new LatencyStats(0, 0, 0, 0, 0, 0.0)
                : computeStats(latenciesMs);

        return new LoadResult(label, threads, durationMs, totalOps, opsPerSec, stats);
    }

    // =========================================================================
    // Public API — statistics
    // =========================================================================

    /**
     * Sorts {@code latenciesMs} and computes percentile and aggregate statistics.
     *
     * <p>Percentiles are computed using the nearest-rank method on the sorted
     * array. The returned {@link LatencyStats} contains p50, p95, p99, min,
     * max, and arithmetic mean — all in milliseconds.</p>
     *
     * @param latenciesMs list of per-invocation latency samples in milliseconds;
     *                    must not be null or empty
     * @return computed {@link LatencyStats}
     * @throws IllegalArgumentException if the list is empty
     */
    public static LatencyStats computeStats(List<Long> latenciesMs) {
        if (latenciesMs.isEmpty()) {
            throw new IllegalArgumentException("latenciesMs must not be empty");
        }

        var sorted = new ArrayList<>(latenciesMs);
        Collections.sort(sorted);

        int n = sorted.size();
        long minMs  = sorted.get(0);
        long maxMs  = sorted.get(n - 1);
        long p50Ms  = sorted.get(percentileIndex(n, 50));
        long p95Ms  = sorted.get(percentileIndex(n, 95));
        long p99Ms  = sorted.get(percentileIndex(n, 99));

        double sumMs = 0.0;
        for (long v : sorted) {
            sumMs += v;
        }
        double avgMs = sumMs / n;

        return new LatencyStats(p50Ms, p95Ms, p99Ms, minMs, maxMs, avgMs);
    }

    // =========================================================================
    // Public API — Markdown rendering
    // =========================================================================

    /**
     * Formats a {@link LoadResult} as an unmodifiable list of Markdown table lines
     * suitable for embedding in DTR documentation.
     *
     * <p>The returned list contains a header row, a separator row, and one data
     * row per statistic: throughput, p50, p95, p99, min, max, and avg latency.</p>
     *
     * <p>Example output:</p>
     * <pre>
     * | Metric | Value |
     * | --- | --- |
     * | Label | math-add |
     * | Threads | 4 |
     * ...
     * </pre>
     *
     * @param result the result to render; must not be null
     * @return an unmodifiable list of Markdown lines
     */
    public static List<String> toMarkdown(LoadResult result) {
        var lines = new ArrayList<String>();
        lines.add("| Metric | Value |");
        lines.add("| --- | --- |");
        lines.add("| Label | %s |".formatted(result.label()));
        lines.add("| Threads | %d |".formatted(result.threads()));
        lines.add("| Duration (ms) | %d |".formatted(result.durationMs()));
        lines.add("| Total ops | %d |".formatted(result.totalOps()));
        lines.add("| Throughput (ops/sec) | %.1f |".formatted(result.opsPerSec()));
        lines.add("| Latency p50 (ms) | %d |".formatted(result.latency().p50Ms()));
        lines.add("| Latency p95 (ms) | %d |".formatted(result.latency().p95Ms()));
        lines.add("| Latency p99 (ms) | %d |".formatted(result.latency().p99Ms()));
        lines.add("| Latency min (ms) | %d |".formatted(result.latency().minMs()));
        lines.add("| Latency max (ms) | %d |".formatted(result.latency().maxMs()));
        lines.add("| Latency avg (ms) | %.3f |".formatted(result.latency().avgMs()));
        return List.copyOf(lines);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns the zero-based index for the given percentile using the nearest-rank
     * method: {@code index = ceil(pct / 100.0 * n) - 1}, clamped to [0, n-1].
     */
    private static int percentileIndex(int n, int pct) {
        int idx = (int) Math.ceil(pct / 100.0 * n) - 1;
        return Math.max(0, Math.min(idx, n - 1));
    }
}
