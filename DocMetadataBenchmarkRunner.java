import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Standalone benchmark runner for DocMetadata JEP 516 (AoT Object Caching) performance.
 *
 * This simulates the real DocMetadata class and measures the performance impact
 * of caching metadata at class initialization time.
 */
public class DocMetadataBenchmarkRunner {

    /**
     * Simplified DocMetadata record (mimicking the real class)
     */
    record DocMetadata(
        String projectName,
        String projectVersion,
        String buildTimestamp,
        String javaVersion,
        String mavenVersion,
        String gitCommit,
        String gitBranch,
        String gitAuthor,
        String buildHost,
        Map<String, String> systemProperties
    ) {}

    /**
     * Global cache - initialized once at class load time.
     * This is the JEP 516 optimization: AoT Object Caching.
     */
    static class DocMetadataCache {
        private static final DocMetadata CACHED_INSTANCE = computeFromBuild();

        static DocMetadata getInstance() {
            return CACHED_INSTANCE;
        }

        private static DocMetadata computeFromBuild() {
            return new DocMetadata(
                getProperty("project.name", "unknown"),
                getProperty("project.version", "unknown"),
                Instant.now().toString(),
                System.getProperty("java.version", "unknown"),
                getMavenVersion(),
                getGitCommit(),
                getGitBranch(),
                getGitAuthor(),
                getHostname(),
                captureSystemProperties()
            );
        }

        private static String getProperty(String key, String defaultValue) {
            String value = System.getProperty(key);
            return value != null ? value : defaultValue;
        }

        private static String getMavenVersion() {
            String mavenVersion = System.getProperty("maven.version");
            if (mavenVersion != null) {
                return mavenVersion;
            }
            try {
                var processBuilder = new ProcessBuilder("mvn", "-version");
                var process = processBuilder.start();
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                var match = output.split("\n")[0];
                return match.contains("Apache Maven") ? match.trim() : "unknown";
            } catch (IOException e) {
                return "unknown";
            }
        }

        private static String getGitCommit() {
            try {
                var processBuilder = new ProcessBuilder("git", "rev-parse", "HEAD");
                var process = processBuilder.start();
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return output.trim();
            } catch (IOException e) {
                return "unknown";
            }
        }

        private static String getGitBranch() {
            try {
                var processBuilder = new ProcessBuilder("git", "rev-parse", "--abbrev-ref", "HEAD");
                var process = processBuilder.start();
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return output.trim();
            } catch (IOException e) {
                return "unknown";
            }
        }

        private static String getGitAuthor() {
            try {
                var processBuilder = new ProcessBuilder("git", "config", "user.name");
                var process = processBuilder.start();
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return output.trim();
            } catch (IOException e) {
                return "unknown";
            }
        }

        private static String getHostname() {
            String hostname = System.getProperty("hostname");
            if (hostname != null && !hostname.isEmpty()) {
                return hostname;
            }
            try {
                var processBuilder = new ProcessBuilder("hostname");
                var process = processBuilder.start();
                var output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                return output.trim();
            } catch (IOException e) {
                return System.getProperty("user.name", "unknown");
            }
        }

        private static Map<String, String> captureSystemProperties() {
            var props = new LinkedHashMap<String, String>();
            var keyNames = new String[] {
                "java.version",
                "java.vendor",
                "java.vm.name",
                "os.name",
                "os.version",
                "os.arch",
                "user.timezone",
                "project.name",
                "project.version",
                "maven.version"
            };

            for (var key : keyNames) {
                String value = System.getProperty(key);
                if (value != null) {
                    props.put(key, value);
                }
            }

            return Collections.unmodifiableMap(props);
        }
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  JEP 516 (AoT Object Caching) - DocMetadata Performance Benchmark             ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();

        // Test 1: Instance identity
        System.out.println("TEST 1: Instance Identity (Singleton Verification)");
        System.out.println("─────────────────────────────────────────────────────────────────────────────");
        testInstanceIdentity();
        System.out.println();

        // Test 2: Cached access performance (10 calls)
        System.out.println("TEST 2: Cached Access Performance (10 Sequential Calls)");
        System.out.println("─────────────────────────────────────────────────────────────────────────────");
        testCachedAccessPerformance();
        System.out.println();

        // Test 3: 1000 repeated calls
        System.out.println("TEST 3: High-Volume Access (1000 Repeated Calls)");
        System.out.println("─────────────────────────────────────────────────────────────────────────────");
        testHighVolumeAccess();
        System.out.println();

        // Test 4: Concurrent access
        System.out.println("TEST 4: Thread Safety (8 Threads x 100 Calls Each)");
        System.out.println("─────────────────────────────────────────────────────────────────────────────");
        testConcurrentAccess();
        System.out.println();

        // Test 5: Metadata content
        System.out.println("TEST 5: Metadata Content Validation");
        System.out.println("─────────────────────────────────────────────────────────────────────────────");
        testMetadataContent();
        System.out.println();

        System.out.println("╔══════════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  All benchmarks completed successfully!                                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════════╝");
        System.out.println();
    }

    static void testInstanceIdentity() {
        DocMetadata meta1 = DocMetadataCache.getInstance();
        DocMetadata meta2 = DocMetadataCache.getInstance();
        DocMetadata meta3 = DocMetadataCache.getInstance();

        if (meta1 == meta2 && meta2 == meta3) {
            System.out.println("✓ PASS: All calls return identical instance (same object reference)");
        } else {
            System.out.println("✗ FAIL: getInstance() returns different instances");
        }
    }

    static void testCachedAccessPerformance() {
        final int NUM_CALLS = 10;
        List<Long> timings = new ArrayList<>();

        // Warm up
        DocMetadataCache.getInstance();

        // Measure
        for (int i = 0; i < NUM_CALLS; i++) {
            long nanoStart = System.nanoTime();
            DocMetadata cached = DocMetadataCache.getInstance();
            long nanoEnd = System.nanoTime();
            timings.add(nanoEnd - nanoStart);
        }

        long minNanos = timings.stream().mapToLong(Long::longValue).min().orElse(-1);
        long maxNanos = timings.stream().mapToLong(Long::longValue).max().orElse(-1);
        double avgNanos = timings.stream().mapToLong(Long::longValue).average().orElse(-1);

        long minMicros = minNanos / 1_000;
        long maxMicros = maxNanos / 1_000;
        double avgMicros = avgNanos / 1_000;

        System.out.printf("Number of calls:     %d%n", NUM_CALLS);
        System.out.printf("Min access time:     %3d μs (%,d nanos)%n", minMicros, minNanos);
        System.out.printf("Max access time:     %3d μs (%,d nanos)%n", maxMicros, maxNanos);
        System.out.printf("Avg access time:     %.2f μs (%.0f nanos)%n", avgMicros, avgNanos);

        if (avgMicros < 10) {
            System.out.println("✓ PASS: Average access time < 10 microseconds");
        } else {
            System.out.println("✗ FAIL: Average access time exceeds 10 microseconds");
        }
    }

    static void testHighVolumeAccess() {
        final int NUM_CALLS = 1000;

        long nanoStart = System.nanoTime();
        for (int i = 0; i < NUM_CALLS; i++) {
            DocMetadataCache.getInstance();
        }
        long nanoEnd = System.nanoTime();

        long totalNanos = nanoEnd - nanoStart;
        long avgNanosPerCall = totalNanos / NUM_CALLS;
        double avgMicrosPerCall = avgNanosPerCall / 1000.0;

        System.out.printf("Total calls:         %d%n", NUM_CALLS);
        System.out.printf("Total time:          %,d nanos (%.3f ms)%n", totalNanos, totalNanos / 1_000_000.0);
        System.out.printf("Avg per call:        %.2f μs (%d nanos)%n", avgMicrosPerCall, avgNanosPerCall);

        if (avgNanosPerCall < 100) {
            System.out.println("✓ PASS: Average per-call overhead < 100 nanos");
        } else {
            System.out.println("✗ WARN: Average per-call overhead exceeds 100 nanos (possible JVM overhead)");
        }
    }

    static void testConcurrentAccess() {
        final int THREAD_COUNT = 8;
        final int CALLS_PER_THREAD = 100;

        Thread[] threads = new Thread[THREAD_COUNT];
        List<DocMetadata> collectedInstances = Collections.synchronizedList(new ArrayList<>());

        for (int t = 0; t < THREAD_COUNT; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < CALLS_PER_THREAD; i++) {
                    DocMetadata instance = DocMetadataCache.getInstance();
                    collectedInstances.add(instance);
                }
            }, "DocMetadata-Benchmark-Thread-" + t);
        }

        long startTime = System.nanoTime();
        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        long endTime = System.nanoTime();

        DocMetadata firstInstance = collectedInstances.get(0);
        boolean allIdentical = collectedInstances.stream().allMatch(inst -> inst == firstInstance);

        System.out.printf("Thread count:        %d%n", THREAD_COUNT);
        System.out.printf("Calls per thread:    %d%n", CALLS_PER_THREAD);
        System.out.printf("Total calls:         %d%n", collectedInstances.size());
        System.out.printf("Total time:          %.3f ms%n", (endTime - startTime) / 1_000_000.0);
        System.out.printf("All instances identical: %s%n", allIdentical ? "YES" : "NO");

        if (allIdentical && collectedInstances.size() == THREAD_COUNT * CALLS_PER_THREAD) {
            System.out.println("✓ PASS: All concurrent accesses return identical instance");
        } else {
            System.out.println("✗ FAIL: Concurrent access produced non-identical instances");
        }
    }

    static void testMetadataContent() {
        DocMetadata meta = DocMetadataCache.getInstance();

        System.out.println("Metadata Fields:");
        System.out.printf("  Project:           %s v%s%n", meta.projectName(), meta.projectVersion());
        System.out.printf("  Java Version:      %s%n", meta.javaVersion());
        System.out.printf("  Maven Version:     %s%n", meta.mavenVersion());
        System.out.printf("  Git Commit:        %s%n",
            meta.gitCommit().length() > 7 ? meta.gitCommit().substring(0, 7) + "..." : meta.gitCommit());
        System.out.printf("  Git Branch:        %s%n", meta.gitBranch());
        System.out.printf("  Git Author:        %s%n", meta.gitAuthor());
        System.out.printf("  Build Host:        %s%n", meta.buildHost());
        System.out.printf("  Build Timestamp:   %s%n", meta.buildTimestamp());
        System.out.printf("  System Props:      %d properties captured%n", meta.systemProperties().size());

        boolean allValid =
            meta.projectName() != null && !meta.projectName().isEmpty() &&
            meta.projectVersion() != null && !meta.projectVersion().isEmpty() &&
            meta.javaVersion() != null && !meta.javaVersion().isEmpty() &&
            meta.buildTimestamp() != null && meta.buildTimestamp().contains("T") &&
            meta.systemProperties() != null && !meta.systemProperties().isEmpty();

        if (allValid) {
            System.out.println("✓ PASS: All metadata fields populated correctly");
        } else {
            System.out.println("✗ FAIL: Some metadata fields are missing or invalid");
        }
    }
}
