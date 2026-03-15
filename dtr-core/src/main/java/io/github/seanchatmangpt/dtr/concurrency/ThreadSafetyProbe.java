package io.github.seanchatmangpt.dtr.concurrency;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Probes a shared mutable object from multiple virtual threads simultaneously.
 * Documents thread-safety behaviour: exceptions thrown, inconsistencies detected,
 * operations per second.
 *
 * <p>Uses virtual threads (JEP 444 / Java 21+) so thousands of concurrent probes
 * impose minimal OS-thread overhead. The {@link ProbeResult} record captures every
 * observable fact about the concurrent execution and is directly renderable via
 * {@code DtrTest.sayTable()} or {@code sayAndAssertThat()}.</p>
 */
public final class ThreadSafetyProbe {

    private ThreadSafetyProbe() {}

    /**
     * Immutable result of a single concurrent probe run.
     *
     * @param label               human-readable name for the operation under test
     * @param threads             number of virtual threads launched
     * @param operationsEach      operations attempted per thread
     * @param totalOperations     threads * operationsEach
     * @param exceptionsDetected  total exception count across all threads
     * @param exceptionTypes      distinct exception class simple-names (sorted)
     * @param elapsedMs           wall-clock time from first submit to latch release
     * @param operationsPerSecond throughput based on completed operations only
     * @param appearsThreadSafe   true iff zero exceptions AND completedOps == totalOperations
     */
    public record ProbeResult(
        String label,
        int threads,
        int operationsEach,
        int totalOperations,
        int exceptionsDetected,
        List<String> exceptionTypes,
        long elapsedMs,
        double operationsPerSecond,
        boolean appearsThreadSafe
    ) {}

    /**
     * Runs the probe with the supplied operation hammered from {@code threads} virtual
     * threads, each executing {@code operationsEach} iterations.
     *
     * @param label           display name for the operation
     * @param sharedOperation the {@link Runnable} that exercises the shared object
     * @param threads         number of concurrent virtual threads
     * @param operationsEach  iterations per thread
     * @return a fully-populated {@link ProbeResult}
     */
    public static ProbeResult probe(
            String label,
            Runnable sharedOperation,
            int threads,
            int operationsEach) {

        List<String> exceptionTypes = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger completedOps = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);

        long start = System.currentTimeMillis();

        // Virtual threads: JEP 444 (stable since Java 21, available in Java 26)
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < threads; t++) {
                executor.submit(() -> {
                    try {
                        for (int i = 0; i < operationsEach; i++) {
                            try {
                                sharedOperation.run();
                                completedOps.incrementAndGet();
                            } catch (Exception e) {
                                exceptionTypes.add(e.getClass().getSimpleName());
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
            try {
                latch.await(30, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long elapsedMs = System.currentTimeMillis() - start;
        int total = threads * operationsEach;
        int completed = completedOps.get();
        double opsPerSec = elapsedMs > 0
            ? (completed * 1000.0 / elapsedMs)
            : completed * 1000.0;

        List<String> distinct = exceptionTypes.stream().distinct().sorted().toList();

        return new ProbeResult(
            label,
            threads,
            operationsEach,
            total,
            exceptionTypes.size(),
            distinct,
            elapsedMs,
            opsPerSec,
            exceptionTypes.isEmpty() && completed == total
        );
    }
}
