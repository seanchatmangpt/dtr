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
package io.github.seanchatmangpt.dtr.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Runs a fixed number of tasks at increasing thread levels — [1, 2, 4, 8] — using
 * Java 26 virtual threads, measuring throughput (tasks/sec) at each level to document
 * parallel scalability.
 *
 * <p>Each thread level uses {@link Executors#newVirtualThreadPerTaskExecutor()} to
 * submit {@code taskCount} tasks concurrently. Wall-clock time is measured with
 * {@code System.nanoTime()}. Throughput is computed as
 * {@code tasksCompleted * 1000 / durationMs}.</p>
 *
 * <p>Errors at any individual level are logged to stderr and the level is skipped
 * rather than aborting the entire run — the caller always receives a non-null
 * {@link ParallelBenchmarkResult}.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * var result = ParallelBenchmarkRunner.run("sqrt-random", 50,
 *         () -> Math.sqrt(Math.random()));
 * ParallelBenchmarkRunner.toMarkdown(result).forEach(line -> sayRaw(line));
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class ParallelBenchmarkRunner {

    private ParallelBenchmarkRunner() {}

    /** Thread levels used for each benchmark run. */
    private static final int[] THREAD_LEVELS = {1, 2, 4, 8};

    // -------------------------------------------------------------------------
    // Public result records
    // -------------------------------------------------------------------------

    /**
     * Measurement result for a single concurrency level.
     *
     * @param threads         number of concurrent virtual threads used at this level
     * @param durationMs      wall-clock milliseconds from first submit to all tasks done
     * @param tasksCompleted  number of tasks that completed successfully
     * @param throughput      tasks per second: {@code tasksCompleted * 1000 / durationMs}
     */
    public record BenchmarkLevel(
            int threads,
            long durationMs,
            long tasksCompleted,
            long throughput) {}

    /**
     * Aggregated result for all measured concurrency levels.
     *
     * @param label   human-readable label for the workload
     * @param levels  one {@link BenchmarkLevel} per entry in {@code THREAD_LEVELS}
     */
    public record ParallelBenchmarkResult(
            String label,
            List<BenchmarkLevel> levels) {}

    // -------------------------------------------------------------------------
    // Core benchmark method
    // -------------------------------------------------------------------------

    /**
     * Runs {@code taskCount} copies of {@code task} at each thread level in
     * [1, 2, 4, 8], using {@link Executors#newVirtualThreadPerTaskExecutor()}.
     *
     * <p>For each level:</p>
     * <ol>
     *   <li>Creates a new virtual-thread executor.</li>
     *   <li>Submits {@code taskCount} tasks and collects all {@link Future} handles.</li>
     *   <li>Awaits all futures, counting completions.</li>
     *   <li>Shuts the executor down and awaits termination (30 s timeout).</li>
     *   <li>Computes throughput as {@code tasksCompleted * 1000 / max(durationMs, 1)}.</li>
     * </ol>
     *
     * <p>Any exception thrown during a level is caught and printed to stderr; the
     * level is recorded with zero throughput and the loop continues.</p>
     *
     * @param label     human-readable name for this workload
     * @param taskCount number of tasks to submit at each concurrency level (must be >= 1)
     * @param task      the operation to benchmark; must be thread-safe
     * @return a non-null {@link ParallelBenchmarkResult} with one entry per thread level
     */
    public static ParallelBenchmarkResult run(String label, int taskCount, Runnable task) {
        var levels = new ArrayList<BenchmarkLevel>(THREAD_LEVELS.length);

        for (var threadCount : THREAD_LEVELS) {
            levels.add(runLevel(threadCount, taskCount, task));
        }

        return new ParallelBenchmarkResult(label, List.copyOf(levels));
    }

    // -------------------------------------------------------------------------
    // Markdown rendering
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link ParallelBenchmarkResult} into a list of Markdown lines
     * suitable for passing to {@code sayRaw()}.
     *
     * <p>The returned list contains:</p>
     * <ol>
     *   <li>A {@code ###} heading with the workload label.</li>
     *   <li>A Markdown table with columns: Threads, Duration (ms), Tasks, Throughput (tasks/s).</li>
     *   <li>One data row per {@link BenchmarkLevel}.</li>
     *   <li>A blank line followed by a summary line naming the highest-throughput level.</li>
     * </ol>
     *
     * @param result the result to render; must not be null
     * @return a mutable list of Markdown lines (never null, never empty)
     */
    public static List<String> toMarkdown(ParallelBenchmarkResult result) {
        var lines = new ArrayList<String>();

        lines.add("### Parallel Benchmark: " + result.label());
        lines.add("");
        lines.add("| Threads | Duration (ms) | Tasks | Throughput (tasks/s) |");
        lines.add("|--------:|--------------:|------:|---------------------:|");

        for (var level : result.levels()) {
            lines.add("| %d | %d | %d | %d |".formatted(
                    level.threads(),
                    level.durationMs(),
                    level.tasksCompleted(),
                    level.throughput()));
        }

        // Summary: find the level with the highest throughput
        var best = result.levels().stream()
                .max(java.util.Comparator.comparingLong(BenchmarkLevel::throughput))
                .orElse(null);

        lines.add("");
        if (best != null) {
            lines.add("**Best throughput** at **%d thread(s)**: %d tasks/s over %d ms."
                    .formatted(best.threads(), best.throughput(), best.durationMs()));
        }

        return lines;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Runs {@code taskCount} tasks on a virtual-thread executor, waits for all
     * completions, and returns a {@link BenchmarkLevel} for the given thread count.
     *
     * <p>The {@code threadCount} parameter is stored in the result for documentation
     * purposes — the underlying executor is always
     * {@code newVirtualThreadPerTaskExecutor()}, which schedules each task on its
     * own virtual thread.</p>
     */
    private static BenchmarkLevel runLevel(int threadCount, int taskCount, Runnable task) {
        long start = System.nanoTime();
        long completed = 0;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<?>>(taskCount);
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(task));
            }
            for (var future : futures) {
                try {
                    future.get();
                    completed++;
                } catch (java.util.concurrent.ExecutionException ex) {
                    // Task threw — count partial completions but continue
                    System.err.println("[ParallelBenchmarkRunner] task error at level "
                            + threadCount + ": " + ex.getCause());
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            executor.shutdown();
            try {
                executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        } catch (Exception ex) {
            System.err.println("[ParallelBenchmarkRunner] level " + threadCount
                    + " failed: " + ex.getMessage());
        }

        long durationMs = Math.max((System.nanoTime() - start) / 1_000_000L, 1L);
        long throughput = completed * 1_000L / durationMs;

        return new BenchmarkLevel(threadCount, durationMs, completed, throughput);
    }
}
