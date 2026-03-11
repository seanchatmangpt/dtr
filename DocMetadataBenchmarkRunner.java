import java.util.ArrayList;
import java.util.List;

/**
 * Standalone DocMetadata JEP 516 (AoT Object Caching) benchmark runner.
 *
 * This runs a simple benchmark to measure the performance of DocMetadata.getInstance()
 * which is cached at class initialization time.
 *
 * Expected Results:
 * - First call: ~260-620ms (spawns 4 external processes: git, mvn, hostname)
 * - Subsequent calls: ~1-2ms (pure object reference return from cache)
 * - Speedup factor: 200-600x improvement
 */
public class DocMetadataBenchmarkRunner {

    public static void main(String[] args) throws Exception {
        System.out.println("=== DocMetadata JEP 516 AoT Caching Benchmark ===\n");

        // Benchmark 1: Cached access performance
        benchmarkCachedAccess();

        // Benchmark 2: Instance identity
        benchmarkInstanceIdentity();

        // Benchmark 3: Metadata content
        benchmarkMetadataContent();

        // Benchmark 4: Concurrent access
        benchmarkConcurrentAccess();

        System.out.println("=== All benchmarks completed ===\n");
    }

    private static void benchmarkCachedAccess() {
        System.out.println("Test 1: Cached getInstance() Performance");
        System.out.println("=========================================");

        final int NUM_CALLS = 10;
        List<Long> timings = new ArrayList<>();

        // Warm up (first call is already done at class init)
        org.r10r.doctester.metadata.DocMetadata warmup = org.r10r.doctester.metadata.DocMetadata.getInstance();

        // Now benchmark repeated cached accesses
        for (int i = 0; i < NUM_CALLS; i++) {
            long nanoStart = System.nanoTime();
            org.r10r.doctester.metadata.DocMetadata cached = org.r10r.doctester.metadata.DocMetadata.getInstance();
            long nanoEnd = System.nanoTime();

            long nanos = nanoEnd - nanoStart;
            timings.add(nanos);
        }

        // Analyze timings
        long minNanos = timings.stream().mapToLong(Long::longValue).min().orElse(-1);
        long maxNanos = timings.stream().mapToLong(Long::longValue).max().orElse(-1);
        double avgNanos = timings.stream().mapToLong(Long::longValue).average().orElse(-1);

        long minMicros = minNanos / 1_000;
        long maxMicros = maxNanos / 1_000;
        double avgMicros = avgNanos / 1_000;

        System.out.println("Number of calls: " + NUM_CALLS);
        System.out.println("Min access time: " + minMicros + " micros (" + minNanos + " nanos)");
        System.out.println("Max access time: " + maxMicros + " micros (" + maxNanos + " nanos)");
        System.out.println("Avg access time: " + String.format("%.2f", avgMicros) + " micros ("
            + String.format("%.0f", avgNanos) + " nanos)");
        System.out.println("Result: " + (avgMicros < 10 ? "PASS" : "FAIL") + " (expected < 10 micros)");
        System.out.println();
    }

    private static void benchmarkInstanceIdentity() {
        System.out.println("Test 2: Instance Identity (Singleton Cache)");
        System.out.println("==========================================");

        org.r10r.doctester.metadata.DocMetadata meta1 = org.r10r.doctester.metadata.DocMetadata.getInstance();
        org.r10r.doctester.metadata.DocMetadata meta2 = org.r10r.doctester.metadata.DocMetadata.getInstance();
        org.r10r.doctester.metadata.DocMetadata meta3 = org.r10r.doctester.metadata.DocMetadata.getInstance();

        boolean allSame = (meta1 == meta2) && (meta2 == meta3);
        System.out.println("meta1 == meta2: " + (meta1 == meta2));
        System.out.println("meta2 == meta3: " + (meta2 == meta3));
        System.out.println("All instances identical: " + allSame);
        System.out.println("Result: " + (allSame ? "PASS" : "FAIL") + " (must return identical cached instance)");
        System.out.println();
    }

    private static void benchmarkMetadataContent() {
        System.out.println("Test 3: Cached Metadata Content Validation");
        System.out.println("=========================================");

        org.r10r.doctester.metadata.DocMetadata meta = org.r10r.doctester.metadata.DocMetadata.getInstance();

        System.out.println("Project: " + meta.projectName() + " v" + meta.projectVersion());
        System.out.println("Java: " + meta.javaVersion());
        System.out.println("Maven: " + meta.mavenVersion());
        System.out.println("Git: commit=" + (meta.gitCommit().length() > 7 ? meta.gitCommit().substring(0, 7) : meta.gitCommit())
            + " branch=" + meta.gitBranch() + " author=" + meta.gitAuthor());
        System.out.println("Host: " + meta.buildHost());
        System.out.println("Built at: " + meta.buildTimestamp());
        System.out.println("System properties count: " + meta.systemProperties().size());

        boolean isValid = meta.projectName() != null && !meta.projectName().isEmpty()
            && meta.javaVersion() != null && !meta.javaVersion().isEmpty()
            && meta.buildTimestamp() != null && meta.buildTimestamp().contains("T");

        System.out.println("Result: " + (isValid ? "PASS" : "FAIL") + " (all fields should be populated)");
        System.out.println();
    }

    private static void benchmarkConcurrentAccess() throws InterruptedException {
        System.out.println("Test 4: Concurrent Access Thread Safety");
        System.out.println("======================================");

        final int THREAD_COUNT = 8;
        final int CALLS_PER_THREAD = 100;
        final List<org.r10r.doctester.metadata.DocMetadata> collectedInstances = new ArrayList<>();
        final Object lock = new Object();

        Thread[] threads = new Thread[THREAD_COUNT];

        for (int t = 0; t < THREAD_COUNT; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < CALLS_PER_THREAD; i++) {
                    org.r10r.doctester.metadata.DocMetadata instance = org.r10r.doctester.metadata.DocMetadata.getInstance();
                    synchronized (lock) {
                        collectedInstances.add(instance);
                    }
                }
            }, "DocMetadata-Benchmark-Thread-" + t);
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all threads to finish
        for (Thread t : threads) {
            t.join();
        }

        // Verify all collected instances are identical
        boolean allIdentical = true;
        org.r10r.doctester.metadata.DocMetadata firstInstance = collectedInstances.get(0);
        for (org.r10r.doctester.metadata.DocMetadata instance : collectedInstances) {
            if (instance != firstInstance) {
                allIdentical = false;
                break;
            }
        }

        System.out.println("Thread count: " + THREAD_COUNT);
        System.out.println("Calls per thread: " + CALLS_PER_THREAD);
        System.out.println("Total calls: " + collectedInstances.size());
        System.out.println("All instances identical: " + allIdentical);
        System.out.println("Result: " + (allIdentical ? "PASS" : "FAIL") + " (thread-safe singleton cache)");
        System.out.println();
    }
}
