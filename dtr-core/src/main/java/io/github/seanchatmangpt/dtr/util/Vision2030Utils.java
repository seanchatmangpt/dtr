package io.github.seanchatmangpt.dtr.util;

import io.github.seanchatmangpt.dtr.benchmark.BenchmarkRunner;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;

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
    public static SequencedMap<String, String> systemFingerprint() {
        var rt = Runtime.getRuntime();
        var map = new LinkedHashMap<String, String>();
        map.put("Java Version", System.getProperty("java.version", "unknown"));
        map.put("Java Vendor", System.getProperty("java.vendor", "unknown"));
        map.put("OS", "%s %s %s".formatted(
                System.getProperty("os.name", "unknown"),
                System.getProperty("os.version", ""),
                System.getProperty("os.arch", "")));
        map.put("Processors", String.valueOf(rt.availableProcessors()));
        map.put("Max Heap (MB)", String.valueOf(rt.maxMemory() / (1024 * 1024)));
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
    public static SequencedMap<String, String> classMetadata(Class<?> clazz) {
        var moduleName = clazz.getModule().getName();
        var map = new LinkedHashMap<String, String>();
        map.put("Class", clazz.getName());
        map.put("Package", clazz.getPackageName());
        map.put("Module", moduleName instanceof String name ? name : "unnamed");
        map.put("Sealed", String.valueOf(clazz.isSealed()));
        map.put("Record", String.valueOf(clazz.isRecord()));
        map.put("Interface", String.valueOf(clazz.isInterface()));
        map.put("Public Methods", String.valueOf(clazz.getMethods().length));
        map.put("Declared Fields", String.valueOf(clazz.getDeclaredFields().length));
        return map;
    }

    // =========================================================================
    // Benchmark Suite
    // =========================================================================

    /**
     * Runs N benchmarks and returns a complete table (header + N data rows)
     * with avg/min/max/p99/ops columns. Designed for {@code sayTable()} consumption.
     *
     * @param labels human-readable labels for each benchmark
     * @param tasks  the runnables to benchmark (same length as labels)
     * @return a {@code String[][]} where row 0 is the header and rows 1..N are results
     */
    public static String[][] benchmarkSuite(String[] labels, Runnable[] tasks) {
        Objects.requireNonNull(labels, "labels must not be null");
        Objects.requireNonNull(tasks, "tasks must not be null");
        if (labels.length != tasks.length) {
            throw new IllegalArgumentException(
                    "labels.length (%d) != tasks.length (%d)".formatted(labels.length, tasks.length));
        }

        var table = new String[labels.length + 1][6];
        table[0] = new String[]{"Label", "Avg (ns)", "Min (ns)", "Max (ns)", "P99 (ns)", "Ops/sec"};

        for (int i = 0; i < labels.length; i++) {
            var result = BenchmarkRunner.run(tasks[i]);
            table[i + 1] = new String[]{
                    labels[i],
                    String.valueOf(result.avgNs()),
                    String.valueOf(result.minNs()),
                    String.valueOf(result.maxNs()),
                    String.valueOf(result.p99Ns()),
                    String.valueOf(result.opsPerSec())
            };
        }

        return table;
    }

    // =========================================================================
    // Interface Compliance Matrix
    // =========================================================================

    /**
     * Returns a {@code String[][]} showing which methods each implementation class
     * overrides, inherits, or is missing from the contract interface.
     * Pure reflection, no say* calls. Designed for {@code sayTable()} consumption.
     *
     * @param contract        the interface whose public methods define the contract
     * @param implementations the classes to check against the contract
     * @return a {@code String[][]} with header row and one row per contract method
     */
    public static String[][] interfaceComplianceMatrix(Class<?> contract, Class<?>... implementations) {
        Objects.requireNonNull(contract, "contract must not be null");
        Objects.requireNonNull(implementations, "implementations must not be null");

        var contractMethods = Arrays.stream(contract.getMethods())
                .filter(m -> m.getDeclaringClass() == contract)
                .sorted((a, b) -> a.getName().compareTo(b.getName()))
                .toArray(Method[]::new);

        var table = new String[contractMethods.length + 1][implementations.length + 1];

        table[0][0] = "Method";
        for (int c = 0; c < implementations.length; c++) {
            table[0][c + 1] = implementations[c].getSimpleName();
        }

        for (int r = 0; r < contractMethods.length; r++) {
            var method = contractMethods[r];
            var signature = method.getName() + "(" + parameterTypeNames(method) + ")";
            table[r + 1][0] = signature;

            for (int c = 0; c < implementations.length; c++) {
                table[r + 1][c + 1] = checkCompliance(implementations[c], method);
            }
        }

        return table;
    }

    private static String parameterTypeNames(Method method) {
        return Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
    }

    private static String checkCompliance(Class<?> impl, Method contractMethod) {
        try {
            var found = impl.getMethod(contractMethod.getName(), contractMethod.getParameterTypes());
            if (found.getDeclaringClass() == impl) {
                return "\u2713 direct";
            }
            if (found.getDeclaringClass() != Object.class) {
                return "\u2197 inherited";
            }
            return "\u2717 MISSING";
        } catch (NoSuchMethodException e) {
            return "\u2717 MISSING";
        }
    }

    // =========================================================================
    // Record to Map
    // =========================================================================

    /**
     * Converts any Java record instance to a {@link LinkedHashMap} of
     * component name to {@code value.toString()}. Preserves declaration order.
     * Designed for {@code sayKeyValue()} consumption.
     *
     * @param record the record instance to convert
     * @return a linked map preserving component declaration order
     */
    public static Map<String, String> recordToMap(Record record) {
        Objects.requireNonNull(record, "record must not be null");

        RecordComponent[] components = record.getClass().getRecordComponents();
        var map = new LinkedHashMap<String, String>(components.length);

        for (var component : components) {
            try {
                var accessor = component.getAccessor();
                var value = accessor.invoke(record);
                map.put(component.getName(), value == null ? "null" : value.toString());
            } catch (ReflectiveOperationException e) {
                map.put(component.getName(), "<error: " + e.getMessage() + ">");
            }
        }

        return map;
    }

    // =========================================================================
    // Thread Snapshot
    // =========================================================================

    /**
     * Returns a {@link Map} with current JVM thread metrics: thread count,
     * daemon count, peak count, total started count, and virtual thread count.
     * Designed for {@code sayKeyValue()} consumption.
     *
     * @return a map of thread metric names to their current values
     */
    public static Map<String, String> threadSnapshot() {
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        var map = new LinkedHashMap<String, String>();
        map.put("Thread Count", String.valueOf(threadBean.getThreadCount()));
        map.put("Daemon Thread Count", String.valueOf(threadBean.getDaemonThreadCount()));
        map.put("Peak Thread Count", String.valueOf(threadBean.getPeakThreadCount()));
        map.put("Total Started Thread Count", String.valueOf(threadBean.getTotalStartedThreadCount()));

        long virtualCount = Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isVirtual)
                .count();
        map.put("Virtual Thread Count", String.valueOf(virtualCount));

        return map;
    }

    // =========================================================================
    // Heap Snapshot
    // =========================================================================

    /**
     * Returns a {@link Map} with current JVM heap memory metrics in megabytes.
     * Designed for {@code sayKeyValue()} consumption.
     *
     * @return a map of heap metric names to their current values in MB
     */
    public static Map<String, String> heapSnapshot() {
        var runtime = Runtime.getRuntime();
        long totalMb = runtime.totalMemory() / (1024 * 1024);
        long freeMb = runtime.freeMemory() / (1024 * 1024);
        long maxMb = runtime.maxMemory() / (1024 * 1024);
        long usedMb = totalMb - freeMb;

        var map = new LinkedHashMap<String, String>();
        map.put("Used Heap", usedMb + " MB");
        map.put("Free Heap", freeMb + " MB");
        map.put("Total Heap", totalMb + " MB");
        map.put("Max Heap", maxMb + " MB");

        return map;
    }
}
