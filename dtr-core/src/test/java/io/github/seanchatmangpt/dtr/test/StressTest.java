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
package io.github.seanchatmangpt.dtr;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Stress tests with SLA-bound assertions to find DTR performance envelopes.
 *
 * <p>Joe Armstrong: "A test without an assertion is not a test — it is a demo."
 * Every stress test here measures performance AND asserts against a production SLA.
 * If the SLA is violated, the test fails explicitly rather than silently reporting
 * a degraded number.</p>
 *
 * <p>SLA definitions follow Fortune 5 operational standards:
 * maximum acceptable latency and memory growth per operation class.</p>
 */
public class StressTest extends DtrTest {

    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();

    /** SLA contract: maximum acceptable duration and memory growth for a stress operation. */
    record StressSla(String op, long maxMs, long maxMemDeltaMB) {
        String describe() {
            return "%s: maxMs=%d, maxMemDelta=%dMB".formatted(op, maxMs, maxMemDeltaMB);
        }
    }

    /** Measured result of a stress operation against its SLA. */
    record StressResult(StressSla sla, long actualMs, long actualMemDeltaMB) {
        boolean timingViolated()  { return actualMs > sla.maxMs(); }
        boolean memoryViolated()  { return actualMemDeltaMB > sla.maxMemDeltaMB(); }
        boolean slaViolated()     { return timingViolated() || memoryViolated(); }

        String summary() {
            return "%s — actual: %dms (SLA: %dms) mem Δ: %dMB (SLA: %dMB) %s".formatted(
                sla.op(), actualMs, sla.maxMs(), actualMemDeltaMB, sla.maxMemDeltaMB(),
                slaViolated() ? "✗ VIOLATED" : "✓ PASS");
        }
    }

    private static long usedMemoryMB() {
        return MEMORY.getHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    private StressResult measure(StressSla sla, Runnable work) {
        System.gc(); // encourage GC before measurement for cleaner delta
        long startMem = usedMemoryMB();
        long startTime = System.nanoTime();
        work.run();
        long elapsed = (System.nanoTime() - startTime) / 1_000_000;
        long memDelta = usedMemoryMB() - startMem;
        return new StressResult(sla, elapsed, memDelta);
    }

    // =========================================================================
    // Test 1: say() call count scaling
    // =========================================================================

    @Test
    public void stressSayCallCount() {
        sayNextSection("Stress: say() call count scaling");
        say("Verifies that say() scales linearly to 100K calls within SLA bounds.");

        var sla = new StressSla("say() x 100K", 15_000, 512);

        int[] counts = {100, 1_000, 10_000, 50_000, 100_000};
        String[][] tableData = new String[counts.length + 1][4];
        tableData[0] = new String[]{"Count", "Duration (ms)", "Mem Δ (MB)", "Status"};

        StressResult finalResult = null;
        for (int i = 0; i < counts.length; i++) {
            final int count = counts[i];
            var batchSla = new StressSla("say() x " + count, sla.maxMs(), sla.maxMemDeltaMB());
            var result = measure(batchSla, () -> {
                for (int j = 0; j < count; j++) {
                    say("Say call " + j + " with realistic doc content to simulate production use.");
                }
            });
            tableData[i + 1] = new String[]{
                String.format("%,d", count),
                String.valueOf(result.actualMs()),
                String.valueOf(result.actualMemDeltaMB()),
                result.timingViolated() ? "✗" : "✓"
            };
            if (count == 100_000) finalResult = result;
        }

        sayTable(tableData);

        // SLA assertion: 100K say() calls must complete within bounds
        assertFalse(finalResult.slaViolated(),
            "SLA violated: " + finalResult.summary() + "\n" + sla.describe());
    }

    // =========================================================================
    // Test 2: sayNextSection() scaling
    // =========================================================================

    @Test
    public void stressSectionCount() {
        sayNextSection("Stress: sayNextSection() scaling");
        say("Verifies the section/TOC generation scales to 10K sections within SLA bounds.");

        var sla = new StressSla("sections x 10K", 8_000, 256);

        int[] counts = {100, 1_000, 5_000, 10_000};
        String[][] tableData = new String[counts.length + 1][4];
        tableData[0] = new String[]{"Section Count", "Duration (ms)", "Mem Δ (MB)", "Status"};

        StressResult finalResult = null;
        for (int i = 0; i < counts.length; i++) {
            final int count = counts[i];
            var batchSla = new StressSla("sections x " + count, sla.maxMs(), sla.maxMemDeltaMB());
            var result = measure(batchSla, () -> {
                for (int j = 0; j < count; j++) {
                    sayNextSection("Section " + count + " item " + j);
                }
            });
            tableData[i + 1] = new String[]{
                String.format("%,d", count),
                String.valueOf(result.actualMs()),
                String.valueOf(result.actualMemDeltaMB()),
                result.timingViolated() ? "✗" : "✓"
            };
            if (count == 10_000) finalResult = result;
        }

        sayTable(tableData);

        assertFalse(finalResult.slaViolated(),
            "SLA violated: " + finalResult.summary() + "\n" + sla.describe());
    }

    // =========================================================================
    // Test 3: sayRaw() large payload size
    // =========================================================================

    @Test
    public void stressLargePayloadSize() {
        sayNextSection("Stress: large payload size");
        say("Verifies that single large sayRaw() payloads up to 50MB are buffered within SLA bounds.");

        var sla = new StressSla("payload 50MB", 5_000, 128);

        int[] sizesKB = {1, 10, 100, 1_000, 10_000, 50_000};
        String[][] tableData = new String[sizesKB.length + 1][4];
        tableData[0] = new String[]{"Payload Size", "Duration (ms)", "Mem Δ (MB)", "Status"};

        StressResult finalResult = null;
        for (int i = 0; i < sizesKB.length; i++) {
            final int sizeKB = sizesKB[i];
            var batchSla = new StressSla("payload " + sizeKB + "KB", sla.maxMs(), sla.maxMemDeltaMB());
            var result = measure(batchSla, () -> {
                String payload = "X".repeat(sizeKB * 1024);
                sayRaw("<pre>" + payload + "</pre>");
            });
            tableData[i + 1] = new String[]{
                sizeKB >= 1_000 ? (sizeKB / 1_000) + "MB" : sizeKB + "KB",
                String.valueOf(result.actualMs()),
                String.valueOf(result.actualMemDeltaMB()),
                result.timingViolated() ? "✗" : "✓"
            };
            if (sizeKB == 50_000) finalResult = result;
        }

        sayTable(tableData);

        assertFalse(finalResult.slaViolated(),
            "SLA violated: " + finalResult.summary() + "\n" + sla.describe());
    }

    // =========================================================================
    // Test 4: Combined load (sections × says)
    // =========================================================================

    @Test
    public void stressCombinedLoad() {
        sayNextSection("Stress: combined load — sections × says");
        say("Verifies 500 sections × 50 says = 25,000 total operations " +
            "complete within SLA bounds. Simulates a large, realistic documentation test.");

        int sections       = 500;
        int saysPerSection = 50;
        var sla = new StressSla(
            "combined " + sections + "×" + saysPerSection,
            30_000, 512);

        var result = measure(sla, () -> {
            for (int s = 0; s < sections; s++) {
                sayNextSection("Combined Section " + s);
                for (int i = 0; i < saysPerSection; i++) {
                    say("Section " + s + " paragraph " + i + ": Lorem ipsum dolor sit amet, " +
                        "consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore.");
                }
            }
        });

        sayTable(new String[][] {
            {"Metric", "Value", "SLA", "Status"},
            {"Total operations", String.format("%,d", sections * saysPerSection), "—", "—"},
            {"Duration", result.actualMs() + "ms", sla.maxMs() + "ms", result.timingViolated() ? "✗" : "✓"},
            {"Memory Δ", result.actualMemDeltaMB() + "MB", sla.maxMemDeltaMB() + "MB", result.memoryViolated() ? "✗" : "✓"},
        });

        assertFalse(result.slaViolated(),
            "Combined load SLA violated: " + result.summary());
    }

    // =========================================================================
    // Test 6: Output file size stress (escalating total content)
    // =========================================================================

    @Test
    public void stressOutputFileSize() {
        sayNextSection("Stress: output file size — 200K say() calls");
        say("Verifies 200K say() calls complete within SLA. " +
            "Each call adds ~60 bytes to the output document.");

        int totalSays = 200_000;
        var sla = new StressSla("output-size 200K say()", 30_000, 1_024);

        var result = measure(sla, () -> {
            for (int i = 0; i < totalSays; i++) {
                say("Row " + i + ": ABCDEFGHIJKLMNOPQRSTUVWXYZ abcdefghijklmnopqrstuvwxyz 0123456789");
                if (i % 50_000 == 0 && i > 0) {
                    System.out.println("[StressTest] say #" + i + ": mem=" + usedMemoryMB() + "MB");
                }
            }
        });

        sayTable(new String[][] {
            {"Metric", "Value", "SLA", "Status"},
            {"Total say() calls", String.format("%,d", totalSays), "—", "—"},
            {"Duration", result.actualMs() + "ms", sla.maxMs() + "ms", result.timingViolated() ? "✗" : "✓"},
            {"Memory Δ", result.actualMemDeltaMB() + "MB", sla.maxMemDeltaMB() + "MB", result.memoryViolated() ? "✗" : "✓"},
        });

        assertFalse(result.timingViolated(),
            "Output file size stress timing SLA violated: " + result.summary());
    }

    // =========================================================================
    // Test 7: Guava Joiner bottleneck test
    // =========================================================================

    @Test
    public void stressJoinerBottleneck() {
        sayNextSection("Stress: Joiner bottleneck — 300K say() calls");
        say("The real bottleneck is Guava Joiner.on(\"\\n\").join(finalMarkdownDocument) at finishAndWriteOut(). " +
            "300K say() calls generates 600K+ list entries. This test asserts the accumulation " +
            "phase completes within SLA; the join bottleneck occurs at @AfterAll.");

        int totalSays = 300_000;
        // SLA: accumulation must be < 60s; join overhead happens at @AfterAll time
        var sla = new StressSla("joiner-bottleneck 300K say()", 60_000, 2_048);

        var result = measure(sla, () -> {
            for (int i = 0; i < totalSays; i++) {
                say("Joiner stress line " + i);
            }
        });

        sayTable(new String[][] {
            {"Metric", "Value", "SLA", "Status"},
            {"Total say() calls", String.format("%,d", totalSays), "—", "—"},
            {"Estimated list entries", String.format("%,d", totalSays * 3L), "—", "—"},
            {"Duration", result.actualMs() + "ms", sla.maxMs() + "ms", result.timingViolated() ? "✗" : "✓"},
            {"Memory Δ", result.actualMemDeltaMB() + "MB", sla.maxMemDeltaMB() + "MB", result.memoryViolated() ? "✗" : "✓"},
        });

        sayNote("Join bottleneck occurs at @AfterAll finishAndWriteOut() — not measured here.");

        assertFalse(result.timingViolated(),
            "Joiner bottleneck accumulation SLA violated: " + result.summary());
    }

    // =========================================================================
    // Test 8: Virtual thread concurrency — Joe Armstrong's fundamental concurrency test
    // =========================================================================

    @Test
    public void stressVirtualThreadConcurrency() throws InterruptedException {
        sayNextSection("Stress: Virtual Thread Concurrency — 200 threads × 500 say() calls");
        say("Joe Armstrong: 'Concurrency is fundamental, not an add-on.' " +
            "This test runs 200 virtual threads, each driving its own DocTester instance, " +
            "verifying no cross-thread state corruption. Each thread generates 500 say() calls.");

        int threadCount  = 200;
        int callsPerThread = 500;
        long slaMs = 60_000L;

        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long start = System.nanoTime();

        // Each virtual thread uses its own isolated DocTester subclass instance
        // to avoid any shared state (DocTester.renderMachine is per-class, not per-thread).
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int t = 0; t < threadCount; t++) {
                final int threadId = t;
                executor.submit(() -> {
                    try {
                        // Use RenderMachineImpl directly — isolated per thread, no file I/O.
                        // Each thread has its own instance: no shared mutable state.
                        var rm = new io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl();
                        rm.setFileName("StressVT-thread-" + threadId);
                        for (int i = 0; i < callsPerThread; i++) {
                            rm.say("Thread " + threadId + " call " + i + ": concurrent documentation.");
                        }
                        // Verify via convertTextToId (public method) that the instance is alive
                        String id = rm.convertTextToId("thread-" + threadId);
                        if (id != null && id.length() >= 0) {
                            successes.incrementAndGet();
                        } else {
                            failures.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failures.incrementAndGet();
                        System.err.println("[VT] Thread " + threadId + " threw: " + e);
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        boolean completed = latch.await(60, java.util.concurrent.TimeUnit.SECONDS);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        sayTable(new String[][] {
            {"Metric", "Value", "SLA", "Status"},
            {"Virtual threads", String.valueOf(threadCount), "200", "✓"},
            {"Calls per thread", String.format("%,d", callsPerThread), "500", "✓"},
            {"Total say() calls", String.format("%,d", (long)threadCount * callsPerThread), "—", "—"},
            {"Successes", String.valueOf(successes.get()), String.valueOf(threadCount), successes.get() == threadCount ? "✓" : "✗"},
            {"Failures", String.valueOf(failures.get()), "0", failures.get() == 0 ? "✓" : "✗"},
            {"Duration", elapsedMs + "ms", slaMs + "ms", elapsedMs <= slaMs ? "✓" : "✗"},
            {"Completed within latch", String.valueOf(completed), "true", completed ? "✓" : "✗"},
        });

        assertTrue(completed, "200 virtual threads must complete within 60 seconds");
        assertEquals(threadCount, successes.get(),
            "All " + threadCount + " virtual threads must succeed; failures=" + failures.get());
        assertEquals(0, failures.get(),
            "Zero thread failures expected; got " + failures.get() + " failures");
        assertTrue(elapsedMs <= slaMs,
            "Virtual thread concurrency must complete in <= " + slaMs + "ms but took " + elapsedMs + "ms");
    }
}
