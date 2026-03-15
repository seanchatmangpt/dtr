package io.github.seanchatmangpt.dtr.toyota;

/**
 * Toyota Takt Time measurement utility for DTR documentation.
 *
 * <p>Takt Time is the maximum allowable time to produce one unit in order to
 * meet customer demand: {@code takt = availableTimeNs / customerDemand}.
 * A production cycle that exceeds takt time cannot keep up with demand.</p>
 *
 * <p>All computation uses {@link System#nanoTime()} — real measurements,
 * no estimates.</p>
 *
 * @since 2026.1.0
 */
public final class TaktTimer {

    private TaktTimer() {}

    // =========================================================================
    // Domain model
    // =========================================================================

    /**
     * Complete takt analysis for a named process.
     *
     * @param processName    human-readable name for the process under measurement
     * @param taktTimeNs     allowable time per unit = availableTimeNs / customerDemand
     * @param avgCycleNs     measured average cycle time = availableTimeNs / unitsProduced
     * @param utilizationPct avgCycleNs / taktTimeNs * 100 (how much of takt is consumed)
     * @param meetsTarget    true when avgCycleNs &lt;= taktTimeNs
     * @param unitsProduced  number of repetitions actually measured
     * @param availableTimeNs total elapsed time for all repetitions in nanoseconds
     * @param customerDemand  units the customer requires within the available window
     */
    public record TaktAnalysis(
            String processName,
            long taktTimeNs,
            long avgCycleNs,
            double utilizationPct,
            boolean meetsTarget,
            int unitsProduced,
            long availableTimeNs,
            long customerDemand) {}

    // =========================================================================
    // Measurement
    // =========================================================================

    /**
     * Measures a process by running {@code unit} for {@code repetitions} iterations,
     * then computes takt analysis against {@code customerDemand}.
     *
     * <p>Formula:</p>
     * <ul>
     *   <li>{@code availableTimeNs} = total elapsed nanoseconds across all repetitions</li>
     *   <li>{@code taktTimeNs}      = availableTimeNs / customerDemand</li>
     *   <li>{@code avgCycleNs}      = availableTimeNs / repetitions</li>
     *   <li>{@code meetsTarget}     = avgCycleNs &lt;= taktTimeNs</li>
     *   <li>{@code utilizationPct}  = avgCycleNs / taktTimeNs * 100</li>
     * </ul>
     *
     * @param processName    label for the process being measured
     * @param unit           the work unit to execute per repetition
     * @param repetitions    number of times to execute {@code unit} (must be &gt; 0)
     * @param customerDemand units required by the customer in the same window (must be &gt; 0)
     * @return fully computed {@link TaktAnalysis}
     * @throws IllegalArgumentException if repetitions or customerDemand are &lt;= 0
     */
    public static TaktAnalysis measure(
            String processName,
            Runnable unit,
            int repetitions,
            long customerDemand) {

        if (repetitions <= 0) {
            throw new IllegalArgumentException("repetitions must be > 0, got: " + repetitions);
        }
        if (customerDemand <= 0) {
            throw new IllegalArgumentException("customerDemand must be > 0, got: " + customerDemand);
        }

        long start = System.nanoTime();
        for (int i = 0; i < repetitions; i++) {
            unit.run();
        }
        long availableTimeNs = System.nanoTime() - start;

        long taktTimeNs  = availableTimeNs / customerDemand;
        long avgCycleNs  = availableTimeNs / repetitions;
        boolean meets    = avgCycleNs <= taktTimeNs;
        double util      = taktTimeNs == 0 ? Double.MAX_VALUE
                         : (avgCycleNs * 100.0) / taktTimeNs;

        return new TaktAnalysis(
                processName,
                taktTimeNs,
                avgCycleNs,
                util,
                meets,
                repetitions,
                availableTimeNs,
                customerDemand);
    }

    // =========================================================================
    // Formatting helper
    // =========================================================================

    /**
     * Formats a nanosecond duration as a human-readable string:
     * {@code Xns}, {@code Xµs}, or {@code Xms}.
     *
     * @param ns duration in nanoseconds
     * @return formatted string with appropriate unit
     */
    public static String humanNs(long ns) {
        if (ns < 1_000L) {
            return ns + "ns";
        } else if (ns < 1_000_000L) {
            return (ns / 1_000L) + "µs";
        } else {
            return (ns / 1_000_000L) + "ms";
        }
    }
}
