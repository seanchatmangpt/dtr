package io.github.seanchatmangpt.dtr.toyota;

import java.util.ArrayList;
import java.util.List;

/**
 * Toyota Kaizen (continuous improvement) cycle measurement for DTR documentation.
 *
 * <p>Models Kaizen by running an operation across N improvement cycles and measuring
 * the performance trend. Each cycle captures min/avg/max nanosecond timings and
 * computes the percentage improvement from the baseline (cycle 0). Converged means
 * the last 3 cycles differ by less than 5% — the process has stabilised.</p>
 *
 * <p>All timing is real — measured with {@link System#nanoTime()} over actual
 * invocations of the supplied {@link Runnable}. No estimates, no synthetic data.</p>
 *
 * @since 2026.1.0
 */
public final class KaizenCycle {

    private KaizenCycle() {}

    // =========================================================================
    // Domain model
    // =========================================================================

    /**
     * Result for a single improvement cycle.
     *
     * @param cycle          zero-based cycle index
     * @param avgNs          mean nanoseconds over all samples in this cycle
     * @param minNs          fastest sample in this cycle
     * @param maxNs          slowest sample in this cycle
     * @param improvementPct percentage improvement vs. cycle-0 baseline
     *                       (positive = faster than baseline)
     */
    public record CycleResult(
            int cycle,
            long avgNs,
            long minNs,
            long maxNs,
            double improvementPct) {}

    /**
     * Full Kaizen report across all cycles.
     *
     * @param processName          human-readable name for the process under improvement
     * @param cycles               ordered list of per-cycle results (cycle 0 first)
     * @param baselineNs           avg nanoseconds of cycle 0
     * @param finalNs              avg nanoseconds of the last cycle
     * @param totalImprovementPct  (baselineNs - finalNs) / baselineNs * 100
     * @param converged            true when the last 3 cycles differ by less than 5%
     */
    public record KaizenReport(
            String processName,
            List<CycleResult> cycles,
            long baselineNs,
            long finalNs,
            double totalImprovementPct,
            boolean converged) {}

    // =========================================================================
    // Factory
    // =========================================================================

    /**
     * Runs {@code operation} for {@code cycles} improvement cycles, each with
     * {@code samplesPerCycle} invocations, and returns a full {@link KaizenReport}.
     *
     * <p>Cycle 0 is the baseline. Improvement percentages are computed relative to
     * the baseline avg. Convergence is detected when the last 3 cycle averages all
     * fall within 5% of each other.</p>
     *
     * @param name            human-readable process name
     * @param operation       the work to measure (must not be null)
     * @param cycles          number of improvement cycles (must be &gt;= 1)
     * @param samplesPerCycle number of invocations per cycle (must be &gt;= 1)
     * @return a fully populated {@link KaizenReport}
     */
    public static KaizenReport run(String name, Runnable operation,
                                   int cycles, int samplesPerCycle) {
        if (cycles < 1) throw new IllegalArgumentException("cycles must be >= 1");
        if (samplesPerCycle < 1) throw new IllegalArgumentException("samplesPerCycle must be >= 1");

        List<CycleResult> results = new ArrayList<>(cycles);
        long baselineAvg = 0L;

        for (int c = 0; c < cycles; c++) {
            long minNs = Long.MAX_VALUE;
            long maxNs = Long.MIN_VALUE;
            long totalNs = 0L;

            for (int s = 0; s < samplesPerCycle; s++) {
                long t0 = System.nanoTime();
                operation.run();
                long elapsed = System.nanoTime() - t0;
                totalNs += elapsed;
                if (elapsed < minNs) minNs = elapsed;
                if (elapsed > maxNs) maxNs = elapsed;
            }

            long avgNs = totalNs / samplesPerCycle;

            if (c == 0) {
                baselineAvg = avgNs;
            }

            double improvementPct = baselineAvg == 0 ? 0.0
                    : (baselineAvg - avgNs) / (double) baselineAvg * 100.0;

            results.add(new CycleResult(c, avgNs, minNs, maxNs, improvementPct));
        }

        long finalAvg = results.get(results.size() - 1).avgNs();
        double totalImprovementPct = baselineAvg == 0 ? 0.0
                : (baselineAvg - finalAvg) / (double) baselineAvg * 100.0;

        boolean converged = checkConvergence(results);

        return new KaizenReport(name, List.copyOf(results),
                baselineAvg, finalAvg, totalImprovementPct, converged);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /**
     * Returns true when the last 3 cycles (or all cycles if fewer than 3) all have
     * average timings within 5% of each other.
     */
    private static boolean checkConvergence(List<CycleResult> results) {
        int n = results.size();
        int windowStart = Math.max(0, n - 3);
        List<CycleResult> window = results.subList(windowStart, n);

        if (window.size() < 2) return true; // single cycle always "converged"

        long maxAvg = window.stream().mapToLong(CycleResult::avgNs).max().orElse(1L);
        long minAvg = window.stream().mapToLong(CycleResult::avgNs).min().orElse(0L);

        if (maxAvg == 0) return true;
        double spread = (maxAvg - minAvg) / (double) maxAvg;
        return spread < 0.05;
    }
}
