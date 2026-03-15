package io.github.seanchatmangpt.dtr.comparison;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs multiple alternative implementations and produces a side-by-side
 * performance comparison. Each alternative is named, timed, and ranked.
 *
 * <p>Uses {@code System.nanoTime()} for all measurements — no estimates,
 * no synthetic data. Results are reproducible given the same JVM state.</p>
 *
 * <p>Typical usage from a DTR doc-test:</p>
 * <pre>{@code
 * var result = AlternativesComparer.compare(List.of(
 *     new Alternative("StringBuilder", () -> { ... }),
 *     new Alternative("String.repeat()", () -> { ... })
 * ));
 * // result.results() is ordered by input order, each with rank label
 * }</pre>
 */
public final class AlternativesComparer {

    public record Alternative(String name, Runnable impl) {}

    public record AlternativeResult(
        String name,
        long avgNanos,
        double relativeToFastest,   // 1.0 = fastest, > 1.0 = slower
        String rank                  // "fastest", "2nd", "3rd", "4th", ...
    ) {}

    public record ComparisonResult(
        List<AlternativeResult> results,
        String fastestName
    ) {}

    private static final int WARMUP  = 10;
    private static final int MEASURE = 50;

    public static ComparisonResult compare(List<Alternative> alternatives) {
        return compare(alternatives, WARMUP, MEASURE);
    }

    public static ComparisonResult compare(
            List<Alternative> alternatives,
            int warmupRounds,
            int measureRounds) {

        long[] timings = new long[alternatives.size()];

        for (int i = 0; i < alternatives.size(); i++) {
            Runnable impl = alternatives.get(i).impl();
            // warmup
            for (int w = 0; w < warmupRounds; w++) impl.run();
            // measure
            long start = System.nanoTime();
            for (int m = 0; m < measureRounds; m++) impl.run();
            timings[i] = Math.max(1L, (System.nanoTime() - start) / measureRounds);
        }

        long fastest = Long.MAX_VALUE;
        int fastestIdx = 0;
        for (int i = 0; i < timings.length; i++) {
            if (timings[i] < fastest) {
                fastest = timings[i];
                fastestIdx = i;
            }
        }

        String[] medals = {"fastest", "2nd", "3rd"};

        int[] ranks = rankIndices(timings);
        List<AlternativeResult> results = new ArrayList<>(alternatives.size());
        for (int i = 0; i < alternatives.size(); i++) {
            int rank = ranks[i]; // 0-based rank
            String rankLabel = rank < medals.length ? medals[rank] : (rank + 1) + "th";
            results.add(new AlternativeResult(
                alternatives.get(i).name(),
                timings[i],
                (double) timings[i] / fastest,
                rankLabel
            ));
        }

        return new ComparisonResult(results, alternatives.get(fastestIdx).name());
    }

    /** Returns 0-based rank for each index (0 = fastest). */
    private static int[] rankIndices(long[] timings) {
        int n = timings.length;
        int[] ranks = new int[n];
        for (int i = 0; i < n; i++) {
            int rank = 0;
            for (int j = 0; j < n; j++) {
                if (timings[j] < timings[i]) rank++;
            }
            ranks[i] = rank;
        }
        return ranks;
    }
}
