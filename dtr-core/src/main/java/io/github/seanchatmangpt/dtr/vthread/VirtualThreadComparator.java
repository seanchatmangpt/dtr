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
package io.github.seanchatmangpt.dtr.vthread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Measures and compares the throughput of platform threads vs virtual threads
 * for a given workload, returning a structured {@link ComparisonResult}.
 *
 * <p>Platform threads are bounded by {@code availableProcessors()} via a fixed
 * thread pool. Virtual threads use {@code Executors.newVirtualThreadPerTaskExecutor()},
 * which creates one lightweight carrier thread per submitted task.</p>
 *
 * <p>Wall-clock time is measured with {@code System.nanoTime()} for each executor,
 * and the faster executor is declared the winner. A {@code speedupFactor} expresses
 * how many times faster the winner is relative to the loser.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * var result = VirtualThreadComparator.compare("sleep-1ms", 50, () -> {
 *     try { Thread.sleep(1); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
 * });
 * System.out.println(result.winner() + " wins by " + result.speedupFactor() + "x");
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class VirtualThreadComparator {

    private VirtualThreadComparator() {}

    // -------------------------------------------------------------------------
    // Result record
    // -------------------------------------------------------------------------

    /**
     * Immutable result of a platform-vs-virtual-thread comparison run.
     *
     * @param label         human-readable label for the workload
     * @param taskCount     number of tasks submitted to each executor
     * @param platformMs    wall-clock milliseconds for the platform-thread run
     * @param virtualMs     wall-clock milliseconds for the virtual-thread run
     * @param speedupFactor ratio of the slower time to the faster time (>= 1.0)
     * @param winner        {@code "virtual"} or {@code "platform"}
     */
    public record ComparisonResult(
            String label,
            int taskCount,
            long platformMs,
            long virtualMs,
            double speedupFactor,
            String winner) {}

    // -------------------------------------------------------------------------
    // Core comparison method
    // -------------------------------------------------------------------------

    /**
     * Runs {@code taskCount} copies of {@code task} on both a fixed platform-thread
     * pool and a virtual-thread-per-task executor, measures wall-clock time for each,
     * and returns a {@link ComparisonResult}.
     *
     * <p>Each executor is shut down (via {@link ExecutorService#shutdown()}) and
     * awaited (via {@link ExecutorService#awaitTermination}) after all futures have
     * been collected. {@link InterruptedException} is handled gracefully: the current
     * thread's interrupt flag is restored and the partially-measured time is still
     * returned so the caller receives a non-null result.</p>
     *
     * @param label     description of the workload (e.g. {@code "sleep-1ms"})
     * @param taskCount number of tasks to submit per executor
     * @param task      the workload to measure; must be thread-safe
     * @return a non-null {@link ComparisonResult}
     */
    public static ComparisonResult compare(String label, int taskCount, Runnable task) {
        long platformMs = runWithExecutor(
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()),
                taskCount,
                task);

        long virtualMs = runWithExecutor(
                Executors.newVirtualThreadPerTaskExecutor(),
                taskCount,
                task);

        boolean virtualWins = virtualMs <= platformMs;
        double speedupFactor = virtualWins
                ? (platformMs == 0 ? 1.0 : (double) platformMs / Math.max(virtualMs, 1))
                : (virtualMs == 0  ? 1.0 : (double) virtualMs  / Math.max(platformMs, 1));
        String winner = virtualWins ? "virtual" : "platform";

        return new ComparisonResult(label, taskCount, platformMs, virtualMs,
                Math.round(speedupFactor * 100.0) / 100.0, winner);
    }

    // -------------------------------------------------------------------------
    // Markdown rendering
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link ComparisonResult} into a list of markdown table lines
     * suitable for passing to {@code sayRaw()} or joining into a full document.
     *
     * <p>The returned list always contains exactly five lines:</p>
     * <ol>
     *   <li>Table header row</li>
     *   <li>Separator row</li>
     *   <li>Platform-thread data row</li>
     *   <li>Virtual-thread data row</li>
     *   <li>Summary line (winner + speedup)</li>
     * </ol>
     *
     * @param result the comparison result to render
     * @return mutable list of markdown lines (never null, never empty)
     */
    public static List<String> toMarkdown(ComparisonResult result) {
        var lines = new ArrayList<String>();
        lines.add("| Thread Type | Tasks | Wall-clock (ms) | Winner |");
        lines.add("|-------------|------:|----------------:|--------|");
        lines.add("| Platform    | %d | %d | %s |".formatted(
                result.taskCount(),
                result.platformMs(),
                result.winner().equals("platform") ? "**winner**" : ""));
        lines.add("| Virtual     | %d | %d | %s |".formatted(
                result.taskCount(),
                result.virtualMs(),
                result.winner().equals("virtual") ? "**winner**" : ""));
        lines.add("");
        lines.add("**%s threads** won for `%s` — **%.2fx** faster than %s threads.".formatted(
                capitalize(result.winner()),
                result.label(),
                result.speedupFactor(),
                result.winner().equals("virtual") ? "platform" : "virtual"));
        return lines;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Submits {@code taskCount} copies of {@code task} to {@code executor}, waits
     * for all futures to complete, shuts the executor down, and returns elapsed
     * wall-clock time in milliseconds.
     */
    private static long runWithExecutor(ExecutorService executor, int taskCount, Runnable task) {
        long start = System.nanoTime();
        try {
            var futures = new ArrayList<Future<?>>(taskCount);
            for (int i = 0; i < taskCount; i++) {
                futures.add(executor.submit(task));
            }
            for (var future : futures) {
                try {
                    future.get();
                } catch (java.util.concurrent.ExecutionException ex) {
                    // Task itself threw — record the time spent but don't abort
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
            try {
                //noinspection ResultOfMethodCallIgnored
                executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return (System.nanoTime() - start) / 1_000_000L;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
