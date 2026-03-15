package io.github.seanchatmangpt.dtr.toyota;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Toyota Production System Andon stop-the-line quality control model for DTR documentation.
 *
 * <p>Models the Andon cord: any worker (station health check) can pull the cord
 * to stop the production line when a defect is detected. GREEN = running normally,
 * YELLOW = degraded/monitor closely, RED = defect detected / line stopped.</p>
 *
 * <p>All computation uses pure Java — no external dependencies.</p>
 *
 * @since 2026.1.0
 */
public final class AndonBoard {

    private AndonBoard() {}

    // =========================================================================
    // Domain model
    // =========================================================================

    /** Andon signal states following Toyota's visual management system. */
    public enum Signal {
        /** Station is healthy and operating within expected parameters. */
        GREEN,
        /** Station is degraded — monitor closely, intervention may be needed. */
        YELLOW,
        /** Station has a defect — line should stop until resolved. */
        RED
    }

    /**
     * A single production station with its current health signal.
     *
     * @param name          station identifier (e.g. "Database", "HTTP API")
     * @param signal        current health signal (GREEN/YELLOW/RED)
     * @param status        human-readable status description
     * @param lastCheckNs   timestamp of last health check (from {@link System#nanoTime()})
     * @param defect        the exception that caused a RED signal, or null if GREEN/YELLOW
     */
    public record Station(
            String name,
            Signal signal,
            String status,
            long lastCheckNs,
            Throwable defect) {}

    /**
     * Full board state aggregating all station signals.
     *
     * @param lineName       display name of the production line
     * @param stations       list of all station health results
     * @param overallSignal  RED if any station is RED; YELLOW if any YELLOW; else GREEN
     * @param greenCount     number of GREEN stations
     * @param yellowCount    number of YELLOW stations
     * @param redCount       number of RED stations
     * @param lineRunning    true only when redCount == 0 (no defects blocking the line)
     */
    public record BoardState(
            String lineName,
            List<Station> stations,
            Signal overallSignal,
            int greenCount,
            int yellowCount,
            int redCount,
            boolean lineRunning) {}

    // =========================================================================
    // Health check contract
    // =========================================================================

    /**
     * Evaluates the health of a named station.
     *
     * <p>Implementations should return a {@link Station} with a non-null {@link Signal}.
     * A probe that throws an exception should map to {@link Signal#RED}.</p>
     */
    @FunctionalInterface
    public interface HealthCheck {
        /**
         * Runs the health check for the named station.
         *
         * @param stationName the name of the station being checked
         * @return the station's current health state (never null)
         */
        Station check(String stationName);
    }

    // =========================================================================
    // Factory helpers
    // =========================================================================

    /**
     * Convenience factory: creates a {@link HealthCheck} that runs {@code probe}.
     *
     * <ul>
     *   <li>GREEN — probe completes within 100 ms</li>
     *   <li>YELLOW — probe completes but takes longer than 100 ms</li>
     *   <li>RED   — probe throws any {@link Throwable}</li>
     * </ul>
     *
     * @param status the status description to use when the probe succeeds
     * @param probe  the runnable probe to execute
     * @return a HealthCheck that classifies the result
     */
    public static HealthCheck of(String status, Runnable probe) {
        return stationName -> {
            long start = System.nanoTime();
            try {
                probe.run();
                long elapsedNs = System.nanoTime() - start;
                boolean slow = elapsedNs > 100_000_000L; // > 100 ms
                Signal signal = slow ? Signal.YELLOW : Signal.GREEN;
                String desc = slow
                        ? status + " (slow: " + (elapsedNs / 1_000_000L) + "ms)"
                        : status;
                return new Station(stationName, signal, desc, start, null);
            } catch (Throwable t) {
                return new Station(stationName, Signal.RED,
                        "DEFECT: " + t.getMessage(), start, t);
            }
        };
    }

    // =========================================================================
    // Board evaluation
    // =========================================================================

    /**
     * Runs all health checks and computes the aggregate {@link BoardState}.
     *
     * <p>Overall signal rules:</p>
     * <ol>
     *   <li>RED if any station signals RED</li>
     *   <li>YELLOW if any station signals YELLOW (and none are RED)</li>
     *   <li>GREEN if all stations are GREEN</li>
     * </ol>
     *
     * <p>The line is considered running ({@code lineRunning == true}) only when
     * no stations are RED.</p>
     *
     * @param lineName the display name of the production line
     * @param stations a map from station name to its {@link HealthCheck}
     * @return the evaluated board state
     */
    public static BoardState evaluate(String lineName, Map<String, HealthCheck> stations) {
        List<Station> results = new ArrayList<>(stations.size());

        for (Map.Entry<String, HealthCheck> entry : stations.entrySet()) {
            Station result = entry.getValue().check(entry.getKey());
            results.add(result);
        }

        int green = 0, yellow = 0, red = 0;
        for (Station s : results) {
            switch (s.signal()) {
                case GREEN  -> green++;
                case YELLOW -> yellow++;
                case RED    -> red++;
            }
        }

        Signal overall = red > 0 ? Signal.RED
                       : yellow > 0 ? Signal.YELLOW
                       : Signal.GREEN;

        boolean lineRunning = red == 0;

        return new BoardState(lineName, List.copyOf(results), overall,
                green, yellow, red, lineRunning);
    }
}
