package io.github.seanchatmangpt.dtr.benchmark;

import java.util.Arrays;

/**
 * Runs a microbenchmark using {@code System.nanoTime()} with configurable warmup and
 * measurement rounds. Uses Java 26 virtual threads for parallel warmup batches to
 * reduce JIT cold-start bias.
 *
 * <p>All measurements are per-invocation nanoseconds. Throughput is computed as
 * {@code 1_000_000_000L / avgNs} (ops/sec).</p>
 */
public final class BenchmarkRunner {

    private BenchmarkRunner() {}

    /**
     * Result of a benchmark run.
     *
     * @param avgNs      average nanoseconds per invocation
     * @param minNs      minimum nanoseconds per invocation
     * @param maxNs      maximum nanoseconds per invocation
     * @param p99Ns      99th-percentile nanoseconds per invocation
     * @param opsPerSec  operations per second (1_000_000_000 / avgNs)
     */
    public record Result(long avgNs, long minNs, long maxNs, long p99Ns, long opsPerSec) {}

    /**
     * Runs the benchmark with default settings: 50 warmup rounds, 500 measure rounds.
     */
    public static Result run(Runnable task) {
        return run(task, 50, 500);
    }

    /**
     * Runs the benchmark with explicit warmup and measure counts.
     *
     * @param task         the code to benchmark
     * @param warmupRounds number of warmup iterations (discarded)
     * @param measureRounds number of measured iterations
     */
    @SuppressWarnings("preview")
    public static Result run(Runnable task, int warmupRounds, int measureRounds) {
        // Warmup: use virtual threads for parallel batch warmup to reduce cold-start bias
        try (var scope = java.util.concurrent.StructuredTaskScope.open()) {
            for (int i = 0; i < Math.min(warmupRounds, 4); i++) {
                final int batchSize = warmupRounds / 4;
                scope.fork(() -> {
                    for (int j = 0; j < batchSize; j++) task.run();
                    return null;  // hguard-ok: Callable<Void> contract requires null return — not a stub
                });
            }
            scope.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // Fall through to measurement even if warmup partially fails
        }

        // Measurement
        long[] samples = new long[measureRounds];
        for (int i = 0; i < measureRounds; i++) {
            long start = System.nanoTime();
            task.run();
            samples[i] = System.nanoTime() - start;
        }

        Arrays.sort(samples);
        long sum = 0;
        for (long s : samples) sum += s;
        long avg = sum / measureRounds;
        long min = samples[0];
        long max = samples[measureRounds - 1];
        long p99 = samples[(int) (measureRounds * 0.99)];
        long opsPerSec = avg > 0 ? 1_000_000_000L / avg : Long.MAX_VALUE;

        return new Result(avg, min, max, p99, opsPerSec);
    }
}
