package io.github.seanchatmangpt.dtr.erlang;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Models Joe Armstrong's OTP supervision tree restart strategies.
 *
 * <p>Three restart strategies are supported:</p>
 * <ul>
 *   <li>{@link RestartStrategy#ONE_FOR_ONE} — only the crashed child restarts</li>
 *   <li>{@link RestartStrategy#ONE_FOR_ALL} — all children restart when any one crashes</li>
 *   <li>{@link RestartStrategy#REST_FOR_ONE} — the crashed child and all children started
 *       after it restart</li>
 * </ul>
 *
 * <p>Each child runs in a virtual thread (JEP 444). Crash detection, restart accounting,
 * and {@code maxRestarts} enforcement are all real — no simulated results.
 * The {@link SupervisionReport} is directly renderable via
 * {@code DtrTest.sayErlangSupervisor()}.</p>
 *
 * <p>Uses only {@code java.util}, {@code java.util.concurrent}, and
 * {@code java.util.function} — no external dependencies.</p>
 *
 * @since 2026.1.0
 */
public final class SupervisionTree {

    private SupervisionTree() {}

    // =========================================================================
    // Public API enums
    // =========================================================================

    /**
     * Erlang/OTP supervisor restart strategy.
     *
     * <ul>
     *   <li>{@code ONE_FOR_ONE} — only crashed child restarts; siblings continue undisturbed</li>
     *   <li>{@code ONE_FOR_ALL} — all children are restarted when any one crashes</li>
     *   <li>{@code REST_FOR_ONE} — the crashed child plus every child started after it restart</li>
     * </ul>
     */
    public enum RestartStrategy {
        /** Only the crashed child restarts. */
        ONE_FOR_ONE,
        /** All children restart when any crashes. */
        ONE_FOR_ALL,
        /** Crashed child + all children started after it restart. */
        REST_FOR_ONE
    }

    /**
     * Lifecycle state of a supervised child.
     */
    public enum ChildState {
        /** Child is currently executing. */
        RUNNING,
        /** Child has terminated with an unhandled exception. */
        CRASHED,
        /** Child is being restarted by the supervisor. */
        RESTARTING,
        /** Child has completed successfully. */
        TERMINATED
    }

    // =========================================================================
    // Public API records
    // =========================================================================

    /**
     * A named, supervised child process.
     *
     * @param name        human-readable identifier (e.g., "worker-a")
     * @param process     the work to execute (may throw)
     * @param maxRestarts maximum restart attempts before the supervisor gives up on this child
     */
    public record Child(String name, Runnable process, int maxRestarts) {}

    /**
     * Per-child report produced after supervision completes.
     *
     * @param name          the child identifier
     * @param state         final {@link ChildState} of the child
     * @param restartCount  number of times this child was restarted
     * @param crashReasons  human-readable exception messages for each crash
     * @param totalNs       total wall-clock nanoseconds spent on this child across all attempts
     */
    public record ChildReport(
            String name,
            ChildState state,
            int restartCount,
            List<String> crashReasons,
            long totalNs) {}

    /**
     * Aggregate supervision report produced by {@link #supervise}.
     *
     * @param supervisorName    the name passed to {@link #supervise}
     * @param strategy          the {@link RestartStrategy} used
     * @param children          per-child reports (one per child, in original order)
     * @param totalCrashes      total crash events across all children and all attempts
     * @param totalRestarts     total restart actions triggered by crashes
     * @param supervisorHealthy {@code true} if no child exceeded its {@code maxRestarts} limit
     */
    public record SupervisionReport(
            String supervisorName,
            RestartStrategy strategy,
            List<ChildReport> children,
            int totalCrashes,
            int totalRestarts,
            boolean supervisorHealthy) {}

    // =========================================================================
    // Supervision logic
    // =========================================================================

    /**
     * Runs all children under the given strategy, applying restart semantics on crash.
     *
     * <p>Children are numbered by their position in the list (0-based). The restart
     * strategy determines which peers are affected when one child crashes:</p>
     * <ul>
     *   <li>{@code ONE_FOR_ONE} — only the crashing child is restarted</li>
     *   <li>{@code ONE_FOR_ALL} — all children are shut down and restarted</li>
     *   <li>{@code REST_FOR_ONE} — the crashing child and every child at a higher index restart</li>
     * </ul>
     *
     * @param supervisorName human-readable supervisor label
     * @param strategy       restart strategy to apply
     * @param children       the children to supervise (order is significant for REST_FOR_ONE)
     * @return a fully-populated {@link SupervisionReport}
     */
    public static SupervisionReport supervise(
            String supervisorName,
            RestartStrategy strategy,
            List<Child> children) {

        int n = children.size();

        // Per-child mutable state accumulated during supervision
        int[]               restartCounts  = new int[n];
        long[]              totalNsPerChild = new long[n];
        List<List<String>>  crashReasons   = new ArrayList<>();
        ChildState[]        finalStates    = new ChildState[n];
        AtomicInteger       totalCrashes   = new AtomicInteger(0);
        AtomicInteger       totalRestarts  = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            crashReasons.add(new CopyOnWriteArrayList<>());
            finalStates[i] = ChildState.TERMINATED;
        }

        // Track which children still need to run. Initially all do.
        boolean[] needsRun = new boolean[n];
        for (int i = 0; i < n; i++) needsRun[i] = true;

        // Run until all children have either completed successfully or exhausted maxRestarts
        boolean anyChanged = true;
        while (anyChanged) {
            anyChanged = false;

            // Determine which children to launch this round
            List<Integer> toRun = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                if (needsRun[i]) {
                    toRun.add(i);
                    needsRun[i] = false;
                }
            }

            if (toRun.isEmpty()) break;

            // Run selected children in virtual threads
            boolean[] crashed = runChildren(children, toRun, restartCounts, totalNsPerChild,
                    crashReasons, totalCrashes, finalStates);

            // Apply restart strategy for any crashes
            for (int idx : toRun) {
                if (crashed[idx]) {
                    if (restartCounts[idx] < children.get(idx).maxRestarts()) {
                        // Determine which children to schedule for restart
                        switch (strategy) {
                            case ONE_FOR_ONE -> {
                                restartCounts[idx]++;
                                totalRestarts.incrementAndGet();
                                needsRun[idx] = true;
                                finalStates[idx] = ChildState.RESTARTING;
                                anyChanged = true;
                            }
                            case ONE_FOR_ALL -> {
                                // Restart all children that have not exhausted their maxRestarts
                                for (int j = 0; j < n; j++) {
                                    if (restartCounts[j] < children.get(j).maxRestarts()) {
                                        restartCounts[j]++;
                                        totalRestarts.incrementAndGet();
                                        needsRun[j] = true;
                                        finalStates[j] = ChildState.RESTARTING;
                                    }
                                }
                                anyChanged = true;
                                // Only apply this once per round to avoid infinite expand
                                break;
                            }
                            case REST_FOR_ONE -> {
                                // Restart the crashed child + all children at higher indices
                                for (int j = idx; j < n; j++) {
                                    if (restartCounts[j] < children.get(j).maxRestarts()) {
                                        restartCounts[j]++;
                                        totalRestarts.incrementAndGet();
                                        needsRun[j] = true;
                                        finalStates[j] = ChildState.RESTARTING;
                                    }
                                }
                                anyChanged = true;
                                break;
                            }
                        }
                        break; // handle one crash event per round; re-evaluate in next iteration
                    } else {
                        finalStates[idx] = ChildState.CRASHED;
                    }
                }
            }
        }

        // Resolve final states: any child that ran without crashing last is TERMINATED
        for (int i = 0; i < n; i++) {
            if (finalStates[i] == ChildState.RESTARTING) {
                // Still in RESTARTING means we couldn't finish — treat as CRASHED
                finalStates[i] = ChildState.CRASHED;
            }
        }

        boolean healthy = true;
        List<ChildReport> reports = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (finalStates[i] == ChildState.CRASHED) healthy = false;
            reports.add(new ChildReport(
                    children.get(i).name(),
                    finalStates[i],
                    restartCounts[i],
                    List.copyOf(crashReasons.get(i)),
                    totalNsPerChild[i]));
        }

        return new SupervisionReport(
                supervisorName,
                strategy,
                List.copyOf(reports),
                totalCrashes.get(),
                totalRestarts.get(),
                healthy);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Launches each child index in {@code toRun} as a virtual thread, waits for
     * completion, and records crash state.
     *
     * @return a boolean array indexed by child position; {@code true} = crashed this round
     */
    private static boolean[] runChildren(
            List<Child> children,
            List<Integer> toRun,
            int[] restartCounts,
            long[] totalNsPerChild,
            List<List<String>> crashReasons,
            AtomicInteger totalCrashes,
            ChildState[] finalStates) {

        boolean[] crashed = new boolean[children.size()];
        CountDownLatch latch = new CountDownLatch(toRun.size());

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int idx : toRun) {
                final int childIdx = idx;
                Child child = children.get(idx);
                finalStates[idx] = ChildState.RUNNING;

                exec.submit(() -> {
                    long start = System.nanoTime();
                    try {
                        child.process().run();
                        finalStates[childIdx] = ChildState.TERMINATED;
                    } catch (Throwable t) {
                        crashed[childIdx] = true;
                        totalCrashes.incrementAndGet();
                        crashReasons.get(childIdx).add(
                                t.getClass().getSimpleName() + ": " +
                                (t.getMessage() != null ? t.getMessage() : "(no message)"));
                        finalStates[childIdx] = ChildState.CRASHED;
                    } finally {
                        totalNsPerChild[childIdx] += System.nanoTime() - start;
                        latch.countDown();
                    }
                });
            }

            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }

        return crashed;
    }

    // =========================================================================
    // Formatting utilities
    // =========================================================================

    /**
     * Formats a nanosecond duration as a human-readable string.
     *
     * <ul>
     *   <li>&lt; 1,000 ns       → "Xns"</li>
     *   <li>&lt; 1,000,000 ns   → "X.Xµs"</li>
     *   <li>otherwise           → "X.Xms"</li>
     * </ul>
     *
     * @param ns the duration in nanoseconds
     * @return a compact human-readable string
     */
    public static String humanNs(long ns) {
        if (ns < 1_000L) {
            return ns + "ns";
        } else if (ns < 1_000_000L) {
            return String.format("%.1f\u00b5s", ns / 1_000.0);
        } else {
            return String.format("%.1fms", ns / 1_000_000.0);
        }
    }
}
