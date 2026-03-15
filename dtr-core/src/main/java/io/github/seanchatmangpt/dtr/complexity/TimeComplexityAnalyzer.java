package io.github.seanchatmangpt.dtr.complexity;

import java.util.function.IntFunction;

/**
 * Empirically measures runtime at increasing input sizes and infers
 * the algorithmic complexity class (O(1), O(log n), O(n), O(n log n), O(n²), O(n³)).
 *
 * <p>Uses log-log regression on timing ratios between the first and last measured
 * input size to derive the growth exponent and map it to a standard Big-O class.</p>
 */
public final class TimeComplexityAnalyzer {

    public record SizeResult(int n, long nanosAvg) {}

    public record ComplexityResult(
        String label,
        SizeResult[] measurements,
        String inferredClass   // "O(1)", "O(log n)", "O(n)", "O(n log n)", "O(n²)", "O(n³)"
    ) {}

    private static final int[] DEFAULT_SIZES = {10, 100, 1_000, 10_000, 100_000};
    private static final int WARMUP = 5;
    private static final int MEASURE = 20;

    private TimeComplexityAnalyzer() {}

    public static ComplexityResult analyze(
            String label,
            IntFunction<Runnable> factory) {
        return analyze(label, factory, DEFAULT_SIZES);
    }

    public static ComplexityResult analyze(
            String label,
            IntFunction<Runnable> factory,
            int[] sizes) {
        var results = new SizeResult[sizes.length];
        for (int i = 0; i < sizes.length; i++) {
            int n = sizes[i];
            var work = factory.apply(n);
            // warmup
            for (int w = 0; w < WARMUP; w++) work.run();
            // measure
            long start = System.nanoTime();
            for (int m = 0; m < MEASURE; m++) work.run();
            long avg = (System.nanoTime() - start) / MEASURE;
            results[i] = new SizeResult(n, Math.max(avg, 1L));
        }
        return new ComplexityResult(label, results, infer(results));
    }

    static String infer(SizeResult[] r) {
        if (r.length < 2) return "unknown";
        // compute ratio of time increase vs size increase between first and last
        double timeRatio = (double) r[r.length - 1].nanosAvg() / r[0].nanosAvg();
        double sizeRatio = (double) r[r.length - 1].n()        / r[0].n();

        double exponent = Math.log(timeRatio) / Math.log(sizeRatio);

        if (exponent < 0.05) return "O(1)";
        if (exponent < 0.45) return "O(log n)";
        if (exponent < 1.35) return "O(n)";
        if (exponent < 1.75) return "O(n log n)";
        if (exponent < 2.35) return "O(n²)";
        return "O(n³)";
    }
}
