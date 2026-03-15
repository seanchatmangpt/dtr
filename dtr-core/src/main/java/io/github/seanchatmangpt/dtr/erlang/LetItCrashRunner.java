package io.github.seanchatmangpt.dtr.erlang;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Models Joe Armstrong's "Let It Crash" supervision philosophy from Erlang/OTP.
 *
 * <p>Each {@link NamedProcess} is isolated in its own virtual thread (JEP 444).
 * If it throws, the supervisor records the crash and attempts up to 3 restarts.
 * The final {@link SupervisionReport} is directly renderable via
 * {@code DtrTest.sayLetItCrash()}.</p>
 *
 * <p>All records are immutable. No mutable state escapes outside the
 * {@link #supervise} call. Uses only {@code java.util} and
 * {@code java.util.concurrent} — no external dependencies.</p>
 *
 * @since 2026.1.0
 */
public final class LetItCrashRunner {

    /** Maximum restart attempts per process before giving up. */
    private static final int MAX_RESTARTS = 3;

    private LetItCrashRunner() {}

    // =========================================================================
    // Public API types
    // =========================================================================

    /**
     * A named process: a label plus the runnable it wraps.
     *
     * @param name     human-readable process identifier
     * @param runnable the work to execute (may throw)
     */
    public record NamedProcess(String name, Runnable runnable) {}

    /**
     * The result of a single process execution attempt.
     *
     * @param processName the process identifier
     * @param action      what the supervisor did ("start", "restart-1", "restart-2", "restart-3")
     * @param outcome     "ok" if the process completed normally, "crash" if it threw
     * @param elapsedNs   wall-clock nanoseconds for this attempt
     * @param error       the throwable that caused the crash, or {@code null} on success
     */
    public record CrashResult(
            String processName,
            String action,
            String outcome,
            long elapsedNs,
            Throwable error) {}

    /**
     * Aggregate supervision report produced by {@link #supervise}.
     *
     * @param supervisorName the name passed to {@link #supervise}
     * @param results        all {@link CrashResult} entries in execution order
     * @param totalCrashes   total count of "crash" outcomes across all attempts
     * @param totalRestarts  total restart attempts triggered by crashes
     * @param totalNs        total wall-clock nanoseconds across all attempts
     */
    public record SupervisionReport(
            String supervisorName,
            List<CrashResult> results,
            int totalCrashes,
            int totalRestarts,
            long totalNs) {}

    // =========================================================================
    // Supervision logic
    // =========================================================================

    /**
     * Runs each process in an isolated virtual thread with up to
     * {@value MAX_RESTARTS} restart attempts on failure.
     *
     * <p>Each attempt produces one {@link CrashResult}. A successful attempt
     * (no exception) stops retrying that process. A crashing attempt increments
     * the crash counter and schedules a restart until the maximum is reached.</p>
     *
     * @param supervisorName human-readable supervisor label (appears in the report)
     * @param processes      the processes to supervise
     * @return a fully-populated {@link SupervisionReport}
     */
    public static SupervisionReport supervise(String supervisorName,
                                              List<NamedProcess> processes) {
        List<CrashResult> allResults = new ArrayList<>();
        AtomicInteger crashCount = new AtomicInteger(0);
        AtomicInteger restartCount = new AtomicInteger(0);
        long globalStart = System.nanoTime();

        for (NamedProcess process : processes) {
            List<CrashResult> processResults = runWithRestarts(process,
                    crashCount, restartCount);
            allResults.addAll(processResults);
        }

        long totalNs = System.nanoTime() - globalStart;

        return new SupervisionReport(
                supervisorName,
                List.copyOf(allResults),
                crashCount.get(),
                restartCount.get(),
                totalNs);
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Runs a single process with up to {@value MAX_RESTARTS} restart attempts.
     * Each attempt is isolated in its own virtual thread.
     */
    private static List<CrashResult> runWithRestarts(NamedProcess process,
                                                     AtomicInteger crashCount,
                                                     AtomicInteger restartCount) {
        List<CrashResult> results = new ArrayList<>();
        boolean succeeded = false;

        for (int attempt = 0; attempt <= MAX_RESTARTS && !succeeded; attempt++) {
            String action = (attempt == 0) ? "start" : ("restart-" + attempt);
            if (attempt > 0) {
                restartCount.incrementAndGet();
            }

            CrashResult result = runOnce(process, action);
            results.add(result);

            if ("crash".equals(result.outcome())) {
                crashCount.incrementAndGet();
            } else {
                succeeded = true;
            }
        }

        return results;
    }

    /**
     * Executes a single attempt of the process in a dedicated virtual thread,
     * capturing timing and any thrown exception.
     */
    private static CrashResult runOnce(NamedProcess process, String action) {
        Throwable[] caught = new Throwable[1];
        CountDownLatch done = new CountDownLatch(1);
        long start = System.nanoTime();

        try (var exec = Executors.newVirtualThreadPerTaskExecutor()) {
            exec.submit(() -> {
                try {
                    process.runnable().run();
                } catch (Throwable t) {
                    caught[0] = t;
                } finally {
                    done.countDown();
                }
            });
            try {
                done.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                caught[0] = ie;
            }
        }

        long elapsedNs = System.nanoTime() - start;
        String outcome = (caught[0] == null) ? "ok" : "crash";

        return new CrashResult(process.name(), action, outcome, elapsedNs, caught[0]);
    }

    // =========================================================================
    // Formatting
    // =========================================================================

    /**
     * Formats a nanosecond duration as a human-readable string.
     *
     * <ul>
     *   <li>&lt; 1,000 ns  → "Xns"</li>
     *   <li>&lt; 1,000,000 ns → "X.Xµs"</li>
     *   <li>otherwise → "X.Xms"</li>
     * </ul>
     *
     * @param ns the duration in nanoseconds
     * @return a compact human-readable string
     */
    public static String humanNs(long ns) {
        if (ns < 1_000L) {
            return ns + "ns";
        } else if (ns < 1_000_000L) {
            return String.format("%.1fµs", ns / 1_000.0);
        } else {
            return String.format("%.1fms", ns / 1_000_000.0);
        }
    }
}
