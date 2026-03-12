/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.metadata;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Benchmark for JEP 516 (AoT Object Caching) impact on DocMetadata.getInstance().
 *
 * This test measures the performance improvement from caching the DocMetadata
 * globally at class initialization time, rather than recomputing it on each access.
 *
 * Expected Results:
 * - First call: ~260-620ms (spawns 4 external processes: git, mvn, hostname)
 * - Subsequent calls: ~1-2ms (pure object reference return)
 * - Speedup factor: 200-600x improvement
 *
 * JEP 516 Context:
 * The static initializer block in DocMetadata calls computeFromBuild() once
 * at class load time, caching the result in CACHED_INSTANCE. All subsequent
 * getInstance() calls return this pre-computed object in ~1-2ms (JVM constant load).
 * This eliminates repeated spawning of external processes when multiple test
 * classes are initialized in the same JVM session.
 */
@DisplayName("DocMetadata JEP 516 AoT Caching Benchmark")
public class DocMetadataBenchmarkTest {

    private static final int NUM_CACHED_CALLS = 10;

    /**
     * Note: The first call to getInstance() happens during class loading,
     * so we cannot directly measure it in this test. However, we can verify
     * that subsequent calls are fast and that all calls return the same instance.
     */
    @Test
    @DisplayName("Cached calls should be sub-millisecond")
    void testCachedInstancePerformance() {
        // Ensure the instance is loaded (already done at class init, but just to be explicit)
        DocMetadata firstLoad = DocMetadata.getInstance();
        assertNotNull(firstLoad, "getInstance() should never return null");

        // Verify all calls return the SAME object reference (identity check)
        DocMetadata secondLoad = DocMetadata.getInstance();
        assertSame(firstLoad, secondLoad,
            "getInstance() must return the identical cached instance, not a copy");

        // Now benchmark repeated cached accesses
        List<Long> timings = new ArrayList<>();

        for (int i = 0; i < NUM_CACHED_CALLS; i++) {
            long nanoStart = System.nanoTime();
            DocMetadata cached = DocMetadata.getInstance();
            long nanoEnd = System.nanoTime();

            long nanos = nanoEnd - nanoStart;
            timings.add(nanos);

            assertSame(firstLoad, cached,
                "All calls must return identical instance (test iteration " + i + ")");
        }

        // Analyze timings
        long minNanos = timings.stream().mapToLong(Long::longValue).min().orElse(-1);
        long maxNanos = timings.stream().mapToLong(Long::longValue).max().orElse(-1);
        double avgNanos = timings.stream().mapToLong(Long::longValue).average().orElse(-1);

        long minMicros = minNanos / 1_000;
        long maxMicros = maxNanos / 1_000;
        double avgMicros = avgNanos / 1_000;

        System.out.println();
        System.out.println("=== DocMetadata.getInstance() Cached Access Benchmark ===");
        System.out.println("Number of calls: " + NUM_CACHED_CALLS);
        System.out.println("Min access time: " + minMicros + " micros (" + minNanos + " nanos)");
        System.out.println("Max access time: " + maxMicros + " micros (" + maxNanos + " nanos)");
        System.out.println("Avg access time: " + String.format("%.2f", avgMicros) + " micros ("
            + String.format("%.0f", avgNanos) + " nanos)");
        System.out.println();

        // Verify that cached access is very fast (should be < 1 microsecond in most cases)
        // On slower systems, allow up to 10 microseconds
        assertTrue(avgMicros < 10,
            "Average cached access should be < 10 microseconds, got: " + avgMicros + " micros");
    }

    @Test
    @DisplayName("getInstance() always returns identical instance")
    void testInstanceIdentity() {
        DocMetadata meta1 = DocMetadata.getInstance();
        DocMetadata meta2 = DocMetadata.getInstance();
        DocMetadata meta3 = DocMetadata.getInstance();

        assertSame(meta1, meta2, "Should return the same cached instance");
        assertSame(meta2, meta3, "Should return the same cached instance");
        assertSame(meta1, meta3, "Transitive equality should hold");
    }

    @Test
    @DisplayName("Cached metadata contains valid fields")
    void testCachedMetadataContent() {
        DocMetadata meta = DocMetadata.getInstance();

        assertNotNull(meta, "getInstance() must not return null");
        assertNotNull(meta.projectName(), "projectName should be set");
        assertNotNull(meta.projectVersion(), "projectVersion should be set");
        assertNotNull(meta.buildTimestamp(), "buildTimestamp should be set");
        assertNotNull(meta.javaVersion(), "javaVersion should be set");
        // Maven version, git info, and hostname may be "unknown" on some systems
        assertNotNull(meta.mavenVersion(), "mavenVersion should be set");
        assertNotNull(meta.gitCommit(), "gitCommit should be set");
        assertNotNull(meta.gitBranch(), "gitBranch should be set");
        assertNotNull(meta.gitAuthor(), "gitAuthor should be set");
        assertNotNull(meta.buildHost(), "buildHost should be set");
        assertNotNull(meta.systemProperties(), "systemProperties map should be set");
        assertFalse(meta.systemProperties().isEmpty(),
            "systemProperties map should not be empty");

        // Verify Java version is at least 25
        assertTrue(meta.javaVersion().startsWith("25") || Integer.parseInt(meta.javaVersion()
            .split("\\.")[0]) >= 25, "Java 25+ required, got: " + meta.javaVersion());

        // Verify timestamp is ISO 8601 format
        assertTrue(meta.buildTimestamp().contains("T") && meta.buildTimestamp().contains("Z"),
            "buildTimestamp should be ISO 8601 format, got: " + meta.buildTimestamp());

        System.out.println();
        System.out.println("=== Cached DocMetadata Content ===");
        System.out.println("Project: " + meta.projectName() + " v" + meta.projectVersion());
        System.out.println("Java: " + meta.javaVersion());
        System.out.println("Maven: " + meta.mavenVersion());
        System.out.println("Git: commit=" + meta.gitCommit().substring(0, 7) + " branch="
            + meta.gitBranch() + " author=" + meta.gitAuthor());
        System.out.println("Host: " + meta.buildHost());
        System.out.println("Built at: " + meta.buildTimestamp());
        System.out.println("System properties count: " + meta.systemProperties().size());
        System.out.println();
    }

    @Test
    @DisplayName("Concurrent access is thread-safe")
    void testConcurrentAccess() throws InterruptedException {
        final int THREAD_COUNT = 8;
        final int CALLS_PER_THREAD = 100;

        final List<DocMetadata> collectedInstances = new ArrayList<>();
        final Object lock = new Object();

        Thread[] threads = new Thread[THREAD_COUNT];

        for (int t = 0; t < THREAD_COUNT; t++) {
            threads[t] = new Thread(() -> {
                for (int i = 0; i < CALLS_PER_THREAD; i++) {
                    DocMetadata instance = DocMetadata.getInstance();
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
        assertEquals(THREAD_COUNT * CALLS_PER_THREAD, collectedInstances.size(),
            "Should have collected all instances");

        DocMetadata firstInstance = collectedInstances.get(0);
        for (DocMetadata instance : collectedInstances) {
            assertSame(firstInstance, instance,
                "All concurrent accesses must return the same instance");
        }

        System.out.println();
        System.out.println("=== Concurrent Access Test ===");
        System.out.println("Thread count: " + THREAD_COUNT);
        System.out.println("Calls per thread: " + CALLS_PER_THREAD);
        System.out.println("Total calls: " + collectedInstances.size());
        System.out.println("All instances identical: YES");
        System.out.println();
    }

    @Test
    @DisplayName("Measure cache initialization overhead")
    void testInitializationOverhead() {
        // This test documents that initialization happens once at class load time.
        // We cannot directly measure this in a method, but we can verify the cost
        // is not repeated.

        DocMetadata instance = DocMetadata.getInstance();

        // If we were to create a new instance (not recommended), it would take
        // 260-620ms. But getInstance() does not do this; it returns the cached instance
        // in ~1-2 microseconds.

        // Verify by taking a timing
        long nanoStart = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            DocMetadata.getInstance();
        }
        long nanoEnd = System.nanoTime();

        long avgNanos = (nanoEnd - nanoStart) / 1000;
        long avgMicros = avgNanos / 1000;

        System.out.println();
        System.out.println("=== Initialization Overhead Test ===");
        System.out.println("1000 repeated getInstance() calls took: " + (nanoEnd - nanoStart)
            + " nanos total");
        System.out.println("Average per call: " + avgMicros + " micros (" + avgNanos + " nanos)");
        System.out.println("Note: Initialization cost (~260-620ms) is paid ONCE at class load");
        System.out.println("Subsequent 1000 calls cost only: " + String.format("%.2f", avgMicros)
            + " micros each");
        System.out.println();

        // Each cached call should be < 100 nanos (single reference dereference)
        assertTrue(avgNanos < 100,
            "Average per-call overhead should be minimal after caching, got: " + avgNanos
                + " nanos");
    }
}
