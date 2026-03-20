package io.github.seanchatmangpt.dtr.util;

import io.github.seanchatmangpt.dtr.benchmark.BenchmarkRunner;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static utilities for Vision 2030 documentation patterns.
 *
 * <p>Provides reusable helpers for system fingerprinting, benchmark comparisons,
 * and environment metadata — the 80/20 operations that appear in most DTR tests.</p>
 *
 * @since 2026.5.0
 */
public final class Vision2030Utils {

    private Vision2030Utils() {}

    /**
     * Returns a reproducibility fingerprint of the current runtime environment.
     * Useful as metadata for any benchmark or test documentation.
     *
     * @return ordered map of environment key-value pairs
     */
    public static Map<String, String> systemFingerprint() {
        var map = new LinkedHashMap<String, String>();
        map.put("Java Version", System.getProperty("java.version", "unknown"));
        map.put("Java Vendor", System.getProperty("java.vendor", "unknown"));
        map.put("OS", System.getProperty("os.name", "unknown") + " " +
                System.getProperty("os.version", "") + " " +
                System.getProperty("os.arch", ""));
        map.put("Processors", String.valueOf(Runtime.getRuntime().availableProcessors()));
        map.put("Max Heap (MB)", String.valueOf(Runtime.getRuntime().maxMemory() / (1024 * 1024)));
        map.put("Timezone", System.getProperty("user.timezone",
                java.util.TimeZone.getDefault().getID()));
        return map;
    }

    /**
     * Runs two benchmarks and returns a side-by-side comparison as a table-ready 2D array.
     * First row is headers; subsequent rows are metrics.
     *
     * @param labelA name of the first task
     * @param taskA  the first runnable to benchmark
     * @param labelB name of the second task
     * @param taskB  the second runnable to benchmark
     * @return 2D string array suitable for {@code sayTable()}
     */
    public static String[][] benchmarkComparison(String labelA, Runnable taskA,
                                                  String labelB, Runnable taskB) {
        return benchmarkComparison(labelA, taskA, labelB, taskB, 50, 500);
    }

    /**
     * Runs two benchmarks with explicit round counts and returns a comparison table.
     *
     * @param labelA        name of the first task
     * @param taskA         the first runnable to benchmark
     * @param labelB        name of the second task
     * @param taskB         the second runnable to benchmark
     * @param warmupRounds  warmup iterations for each benchmark
     * @param measureRounds measurement iterations for each benchmark
     * @return 2D string array suitable for {@code sayTable()}
     */
    public static String[][] benchmarkComparison(String labelA, Runnable taskA,
                                                  String labelB, Runnable taskB,
                                                  int warmupRounds, int measureRounds) {
        var resultA = BenchmarkRunner.run(taskA, warmupRounds, measureRounds);
        var resultB = BenchmarkRunner.run(taskB, warmupRounds, measureRounds);
        return new String[][]{
                {"Metric", labelA, labelB},
                {"Avg (ns)", String.valueOf(resultA.avgNs()), String.valueOf(resultB.avgNs())},
                {"Min (ns)", String.valueOf(resultA.minNs()), String.valueOf(resultB.minNs())},
                {"Max (ns)", String.valueOf(resultA.maxNs()), String.valueOf(resultB.maxNs())},
                {"P99 (ns)", String.valueOf(resultA.p99Ns()), String.valueOf(resultB.p99Ns())},
                {"Ops/sec", String.valueOf(resultA.opsPerSec()), String.valueOf(resultB.opsPerSec())}
        };
    }

    /**
     * Returns a map of class metadata useful for documentation headers.
     *
     * @param clazz the class to describe
     * @return ordered map of class metadata
     */
    public static Map<String, String> classMetadata(Class<?> clazz) {
        var map = new LinkedHashMap<String, String>();
        map.put("Class", clazz.getName());
        map.put("Package", clazz.getPackageName());
        map.put("Module", clazz.getModule().getName() != null
                ? clazz.getModule().getName() : "unnamed");
        map.put("Sealed", String.valueOf(clazz.isSealed()));
        map.put("Record", String.valueOf(clazz.isRecord()));
        map.put("Interface", String.valueOf(clazz.isInterface()));
        map.put("Public Methods", String.valueOf(clazz.getMethods().length));
        map.put("Declared Fields", String.valueOf(clazz.getDeclaredFields().length));
        return map;
    }
}
