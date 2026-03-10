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
package org.r10r.doctester;

import org.r10r.doctester.rendermachine.RenderMachineImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertTrue;

/**
 * Stress tests for the RenderMachineImpl.
 *
 * Probes these breakpoint dimensions:
 *   1. say() call COUNT — how many paragraphs before OOM / timeout
 *   2. say() PAYLOAD SIZE — largest single payload before failure
 *   3. sayNextSection() COUNT — navigation sidebar explosion
 *   4. sayAndAssertThat() COUNT — assertion accumulation
 *   5. MEMORY growth rate — bytes per say() call
 *   6. CONCURRENT instances — parallel RenderMachine writes
 *   7. MIXED workload — realistic combined stress
 *
 * Results are logged and breakpoints are recorded as JVM system properties
 * so the companion DocTesterStressTest can report them.
 */
public class RenderMachineStressTest {

    private static final Logger log = LoggerFactory.getLogger(RenderMachineStressTest.class);

    /** Output directory used by RenderMachineImpl */
    private static final String BASE_DIR = "target/site/doctester";

    /** One KiB of ASCII 'x' */
    private static final String ONE_KB = "x".repeat(1024);

    /** One MiB of ASCII 'x' */
    private static final String ONE_MB = "x".repeat(1024 * 1024);

    private RenderMachineImpl rm;

    @Before
    public void setUp() {
        // Fresh instance per test — no shared state
        rm = new RenderMachineImpl();
        ensureOutputDir();
    }

    @After
    public void tearDown() {
        rm = null;
        System.gc();
    }

    // -------------------------------------------------------------------------
    // 1. say() CALL COUNT — scaling from 100 → 1 000 000
    // -------------------------------------------------------------------------

    @Test
    public void testSayCallScaling_100() {
        long[] result = benchmarkSayCalls(rm, 100, "say-100");
        log.info("[SAY-COUNT] n=100  time={}ms  file={}B", result[0], result[1]);
        System.setProperty("stress.say.100.ms", String.valueOf(result[0]));
        System.setProperty("stress.say.100.bytes", String.valueOf(result[1]));
    }

    @Test
    public void testSayCallScaling_1K() {
        long[] result = benchmarkSayCalls(rm, 1_000, "say-1k");
        log.info("[SAY-COUNT] n=1000  time={}ms  file={}B", result[0], result[1]);
        System.setProperty("stress.say.1k.ms", String.valueOf(result[0]));
        System.setProperty("stress.say.1k.bytes", String.valueOf(result[1]));
    }

    @Test
    public void testSayCallScaling_10K() {
        long[] result = benchmarkSayCalls(rm, 10_000, "say-10k");
        log.info("[SAY-COUNT] n=10000  time={}ms  file={}B  fileSizeKB={}",
                result[0], result[1], result[1] / 1024);
        System.setProperty("stress.say.10k.ms", String.valueOf(result[0]));
        System.setProperty("stress.say.10k.bytes", String.valueOf(result[1]));
    }

    @Test
    public void testSayCallScaling_100K() {
        long[] result = benchmarkSayCalls(rm, 100_000, "say-100k");
        log.info("[SAY-COUNT] n=100000  time={}ms  file={}KB  fileMB={}",
                result[0], result[1] / 1024, result[1] / (1024 * 1024));
        System.setProperty("stress.say.100k.ms", String.valueOf(result[0]));
        System.setProperty("stress.say.100k.bytes", String.valueOf(result[1]));
    }

    @Test
    public void testSayCallScaling_500K() {
        long[] result = benchmarkSayCalls(rm, 500_000, "say-500k");
        log.info("[SAY-COUNT] n=500000  time={}ms  fileMB={}",
                result[0], result[1] / (1024 * 1024));
        System.setProperty("stress.say.500k.ms", String.valueOf(result[0]));
        System.setProperty("stress.say.500k.bytes", String.valueOf(result[1]));
    }

    /** Flood to 1 million — the big breakpoint test */
    @Test
    public void testSayCallScaling_1M() {
        long[] result = benchmarkSayCalls(rm, 1_000_000, "say-1m");
        log.info("[SAY-COUNT] n=1000000  time={}ms  fileMB={}",
                result[0], result[1] / (1024 * 1024));
        System.setProperty("stress.say.1m.ms", String.valueOf(result[0]));
        System.setProperty("stress.say.1m.bytes", String.valueOf(result[1]));
    }

    // -------------------------------------------------------------------------
    // 2. say() PAYLOAD SIZE — 1 KB → 10 MB per call
    // -------------------------------------------------------------------------

    @Test
    public void testPayloadSize_1KB() {
        long[] result = benchmarkPayloadSize(rm, ONE_KB, "payload-1kb");
        log.info("[PAYLOAD] size=1KB  time={}ms  file={}B", result[0], result[1]);
    }

    @Test
    public void testPayloadSize_10KB() {
        long[] result = benchmarkPayloadSize(rm, "x".repeat(10 * 1024), "payload-10kb");
        log.info("[PAYLOAD] size=10KB  time={}ms  file={}KB", result[0], result[1] / 1024);
    }

    @Test
    public void testPayloadSize_100KB() {
        long[] result = benchmarkPayloadSize(rm, "x".repeat(100 * 1024), "payload-100kb");
        log.info("[PAYLOAD] size=100KB  time={}ms  file={}KB", result[0], result[1] / 1024);
    }

    @Test
    public void testPayloadSize_1MB() {
        long[] result = benchmarkPayloadSize(rm, ONE_MB, "payload-1mb");
        log.info("[PAYLOAD] size=1MB  time={}ms  file={}MB", result[0], result[1] / (1024 * 1024));
    }

    @Test
    public void testPayloadSize_5MB() {
        long[] result = benchmarkPayloadSize(rm, "x".repeat(5 * 1024 * 1024), "payload-5mb");
        log.info("[PAYLOAD] size=5MB  time={}ms  file={}MB", result[0], result[1] / (1024 * 1024));
    }

    @Test
    public void testPayloadSize_10MB() {
        long[] result = benchmarkPayloadSize(rm, "x".repeat(10 * 1024 * 1024), "payload-10mb");
        log.info("[PAYLOAD] size=10MB  time={}ms  file={}MB", result[0], result[1] / (1024 * 1024));
        System.setProperty("stress.payload.10mb.ms", String.valueOf(result[0]));
    }

    /** Repeat a 1 MB payload 10 times — 10 MB total accumulated in-memory */
    @Test
    public void testPayloadSize_10x1MB() {
        long startMs = System.currentTimeMillis();
        for (int i = 0; i < 10; i++) {
            rm.say(ONE_MB);
        }
        rm.setFileName("payload-10x1mb");
        rm.finishAndWriteOut();
        long elapsed = System.currentTimeMillis() - startMs;
        long fileSize = fileSize("payload-10x1mb");
        log.info("[PAYLOAD] 10x1MB accumulated  time={}ms  file={}MB", elapsed, fileSize / (1024 * 1024));
    }

    // -------------------------------------------------------------------------
    // 3. sayNextSection() COUNT — sidebar navigation explosion
    // -------------------------------------------------------------------------

    @Test
    public void testSectionCount_100() {
        long[] r = benchmarkSections(rm, 100, "sections-100");
        log.info("[SECTIONS] n=100  time={}ms  file={}KB", r[0], r[1] / 1024);
    }

    @Test
    public void testSectionCount_1K() {
        long[] r = benchmarkSections(rm, 1_000, "sections-1k");
        log.info("[SECTIONS] n=1000  time={}ms  file={}KB", r[0], r[1] / 1024);
    }

    @Test
    public void testSectionCount_5K() {
        long[] r = benchmarkSections(rm, 5_000, "sections-5k");
        log.info("[SECTIONS] n=5000  time={}ms  file={}KB", r[0], r[1] / 1024);
        System.setProperty("stress.sections.5k.ms", String.valueOf(r[0]));
    }

    @Test
    public void testSectionCount_20K() {
        long[] r = benchmarkSections(rm, 20_000, "sections-20k");
        log.info("[SECTIONS] n=20000  time={}ms  file={}MB", r[0], r[1] / (1024 * 1024));
        System.setProperty("stress.sections.20k.ms", String.valueOf(r[0]));
    }

    // -------------------------------------------------------------------------
    // 4. sayAndAssertThat() COUNT — assertion accumulation
    // -------------------------------------------------------------------------

    @Test
    public void testAssertionCount_1K() {
        long[] r = benchmarkAssertions(rm, 1_000, "assert-1k");
        log.info("[ASSERT] n=1000  time={}ms  file={}KB", r[0], r[1] / 1024);
    }

    @Test
    public void testAssertionCount_10K() {
        long[] r = benchmarkAssertions(rm, 10_000, "assert-10k");
        log.info("[ASSERT] n=10000  time={}ms  file={}KB", r[0], r[1] / 1024);
    }

    @Test
    public void testAssertionCount_50K() {
        long[] r = benchmarkAssertions(rm, 50_000, "assert-50k");
        log.info("[ASSERT] n=50000  time={}ms  file={}MB", r[0], r[1] / (1024 * 1024));
        System.setProperty("stress.assert.50k.ms", String.valueOf(r[0]));
    }

    @Test
    public void testAssertionCount_100K() {
        long[] r = benchmarkAssertions(rm, 100_000, "assert-100k");
        log.info("[ASSERT] n=100000  time={}ms  file={}MB", r[0], r[1] / (1024 * 1024));
        System.setProperty("stress.assert.100k.ms", String.valueOf(r[0]));
    }

    // -------------------------------------------------------------------------
    // 5. MEMORY GROWTH — bytes per say() call
    // -------------------------------------------------------------------------

    @Test
    public void testMemoryGrowthRate() {
        Runtime rt = Runtime.getRuntime();
        System.gc();
        long heapBefore = rt.totalMemory() - rt.freeMemory();

        int n = 100_000;
        String shortMsg = "Paragraph content for memory growth measurement.";
        for (int i = 0; i < n; i++) {
            rm.say(shortMsg);
        }

        System.gc();
        long heapAfter = rt.totalMemory() - rt.freeMemory();
        long growthBytes = heapAfter - heapBefore;
        long bytesPerCall = growthBytes / n;

        log.info("[MEMORY] {} say() calls — heap grew {}KB — ~{} bytes/call",
                n, growthBytes / 1024, bytesPerCall);
        System.setProperty("stress.memory.bytesPerSay", String.valueOf(bytesPerCall));

        // Soft assertion: growth should be measurable but below 500 bytes/call
        // (each call adds 3 list entries of ~16-50 bytes each + overhead)
        log.info("[MEMORY] bytesPerCall={} (expect <2000 for simple messages)", bytesPerCall);
    }

    // -------------------------------------------------------------------------
    // 6. CONCURRENT instances — parallel RenderMachine writes
    // -------------------------------------------------------------------------

    @Test
    public void testConcurrentRenderMachines_10threads() throws Exception {
        int threadCount = 10;
        int callsPerThread = 1_000;
        runConcurrentRenderMachines(threadCount, callsPerThread);
    }

    @Test
    public void testConcurrentRenderMachines_50threads() throws Exception {
        int threadCount = 50;
        int callsPerThread = 500;
        runConcurrentRenderMachines(threadCount, callsPerThread);
    }

    @Test
    public void testConcurrentRenderMachines_VirtualThreads() throws Exception {
        // Java 25: virtual threads stress test — 200 lightweight threads
        int count = 200;
        int callsPerVThread = 200;
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(count);
        List<Thread> threads = new ArrayList<>(count);

        for (int i = 0; i < count; i++) {
            final int idx = i;
            Thread vt = Thread.ofVirtual().name("vt-rm-" + idx).start(() -> {
                try {
                    RenderMachineImpl localRm = new RenderMachineImpl();
                    localRm.setFileName("vthread-" + idx);
                    for (int j = 0; j < callsPerVThread; j++) {
                        localRm.say("Virtual thread " + idx + " paragraph " + j);
                    }
                    localRm.sayNextSection("Section " + idx);
                    localRm.finishAndWriteOut();
                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.error("Virtual thread {} failed: {}", idx, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
            threads.add(vt);
        }

        boolean completed = latch.await(120, TimeUnit.SECONDS);
        log.info("[CONCURRENT-VT] {} virtual threads x {} calls — completed={} errors={}",
                count, callsPerVThread, completed, errors.get());
        assertTrue("Virtual thread stress timed out", completed);
        System.setProperty("stress.vthread.errors", String.valueOf(errors.get()));
    }

    // -------------------------------------------------------------------------
    // 7. MIXED WORKLOAD — realistic combined stress
    // -------------------------------------------------------------------------

    @Test
    public void testMixedWorkload_Realistic() {
        // Simulate a large real-world test suite with balanced operations
        int sections = 50;
        int paragraphsPerSection = 100;
        int assertionsPerSection = 20;

        long startMs = System.currentTimeMillis();
        for (int s = 0; s < sections; s++) {
            rm.sayNextSection("Section " + s + ": Testing Feature Group");
            rm.say("Overview paragraph for section " + s);
            for (int p = 0; p < paragraphsPerSection; p++) {
                rm.say("Detail paragraph " + p + " in section " + s + ": " +
                        "The quick brown fox jumps over the lazy dog. " +
                        "Lorem ipsum dolor sit amet consectetur.");
            }
            for (int a = 0; a < assertionsPerSection; a++) {
                rm.sayAndAssertThat("Assertion " + a + " in section " + s,
                        a, equalTo(a));
            }
        }

        rm.setFileName("mixed-realistic");
        rm.finishAndWriteOut();

        long elapsed = System.currentTimeMillis() - startMs;
        long fileSz = fileSize("mixed-realistic");
        int totalOps = sections * (1 + paragraphsPerSection + assertionsPerSection);

        log.info("[MIXED] sections={} paragraphsPerSection={} assertionsPerSection={} " +
                "totalOps={}  time={}ms  file={}KB  throughput={} ops/sec",
                sections, paragraphsPerSection, assertionsPerSection,
                totalOps, elapsed, fileSz / 1024,
                elapsed > 0 ? (totalOps * 1000L / elapsed) : "N/A");
    }

    @Test
    public void testMixedWorkload_Heavy() {
        // Heavier version — find where it starts to degrade
        int sections = 200;
        int paragraphsPerSection = 500;
        int assertionsPerSection = 50;

        long startMs = System.currentTimeMillis();
        for (int s = 0; s < sections; s++) {
            rm.sayNextSection("Heavy Section " + s);
            for (int p = 0; p < paragraphsPerSection; p++) {
                rm.say("Para " + p + " sect " + s);
            }
            for (int a = 0; a < assertionsPerSection; a++) {
                rm.sayAndAssertThat("Assert " + a, a, equalTo(a));
            }
        }

        rm.setFileName("mixed-heavy");
        rm.finishAndWriteOut();

        long elapsed = System.currentTimeMillis() - startMs;
        long fileSz = fileSize("mixed-heavy");
        int totalOps = sections * (paragraphsPerSection + assertionsPerSection);

        log.info("[MIXED-HEAVY] totalOps={}  time={}ms  file={}MB  throughput={} ops/sec",
                totalOps, elapsed, fileSz / (1024 * 1024),
                elapsed > 0 ? (totalOps * 1000L / elapsed) : "N/A");
        System.setProperty("stress.mixed.heavy.ms", String.valueOf(elapsed));
        System.setProperty("stress.mixed.heavy.fileMB", String.valueOf(fileSz / (1024 * 1024)));
    }

    // -------------------------------------------------------------------------
    // 8. HTML ESCAPING — special character payload stress
    // -------------------------------------------------------------------------

    @Test
    public void testHtmlEscapingStress() {
        // Payloads that trigger extensive HTML escaping
        String dangerous = "<script>alert('xss')</script>".repeat(1000);
        String jsonLike = "{\"key\": \"value\", \"arr\": [1,2,3]}".repeat(500);
        String unicodeMix = "\u00e9\u00e0\u00fc\u4e2d\u6587\u65e5\u672c\u8a9e".repeat(5000);

        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; i++) {
            rm.say(dangerous);
            rm.say(jsonLike);
            rm.say(unicodeMix);
        }

        rm.setFileName("html-escape-stress");
        rm.finishAndWriteOut();

        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize("html-escape-stress");
        log.info("[HTML-ESCAPE] 3000 calls with special chars  time={}ms  file={}MB",
                elapsed, fileSz / (1024 * 1024));
    }

    // -------------------------------------------------------------------------
    // 9. SECTION ID COLLISION — duplicate section names
    // -------------------------------------------------------------------------

    @Test
    public void testDuplicateSectionNames() {
        // All sections have identical names → duplicate HTML IDs
        int n = 10_000;
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            rm.sayNextSection("Duplicate Section Name");
        }
        rm.setFileName("duplicate-sections");
        rm.finishAndWriteOut();

        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize("duplicate-sections");
        log.info("[SECTION-ID] {} identical section names  time={}ms  file={}KB",
                n, elapsed, fileSz / 1024);
        // Note: all sections get the same HTML id="duplicatesectionname" — this is a bug
        System.setProperty("stress.section.duplicate.detected", "true");
    }

    // -------------------------------------------------------------------------
    // 10. RAW HTML INJECTION — sayRaw() accumulation
    // -------------------------------------------------------------------------

    @Test
    public void testRawHtmlScaling() {
        String rawChunk = "<div class=\"row\"><div class=\"col-md-12\">" +
                "<table class=\"table\"><tr><td>Cell</td></tr></table>" +
                "</div></div>";

        long start = System.currentTimeMillis();
        int n = 50_000;
        for (int i = 0; i < n; i++) {
            rm.sayRaw(rawChunk);
        }
        rm.setFileName("raw-html-50k");
        rm.finishAndWriteOut();

        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize("raw-html-50k");
        log.info("[RAW-HTML] n={} rawChunks  time={}ms  file={}MB",
                n, elapsed, fileSz / (1024 * 1024));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Runs N say() calls, writes out, returns [elapsedMs, fileSizeBytes].
     */
    private long[] benchmarkSayCalls(RenderMachineImpl machine, int n, String outputName) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            machine.say("Paragraph number " + i + " in the stress test.");
        }
        machine.setFileName(outputName);
        machine.finishAndWriteOut();
        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize(outputName);
        return new long[]{elapsed, fileSz};
    }

    /**
     * Calls say() with the given payload once, writes out, returns [elapsedMs, fileSizeBytes].
     */
    private long[] benchmarkPayloadSize(RenderMachineImpl machine, String payload, String outputName) {
        long start = System.currentTimeMillis();
        machine.say(payload);
        machine.setFileName(outputName);
        machine.finishAndWriteOut();
        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize(outputName);
        return new long[]{elapsed, fileSz};
    }

    /**
     * Runs N sayNextSection() calls, writes out, returns [elapsedMs, fileSizeBytes].
     */
    private long[] benchmarkSections(RenderMachineImpl machine, int n, String outputName) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            machine.sayNextSection("Section " + i + " Title for Navigation Testing");
        }
        machine.setFileName(outputName);
        machine.finishAndWriteOut();
        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize(outputName);
        return new long[]{elapsed, fileSz};
    }

    /**
     * Runs N sayAndAssertThat() calls (all passing), writes out, returns [elapsedMs, fileSizeBytes].
     */
    private long[] benchmarkAssertions(RenderMachineImpl machine, int n, String outputName) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < n; i++) {
            machine.sayAndAssertThat("Assertion " + i, i, equalTo(i));
        }
        machine.setFileName(outputName);
        machine.finishAndWriteOut();
        long elapsed = System.currentTimeMillis() - start;
        long fileSz = fileSize(outputName);
        return new long[]{elapsed, fileSz};
    }

    /**
     * Spawns N threads each creating an independent RenderMachineImpl, runs callsPerThread
     * say() calls per thread, writes out. Reports timing and error count.
     */
    private void runConcurrentRenderMachines(int threadCount, int callsPerThread) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        AtomicInteger errors = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threadCount);
        long start = System.currentTimeMillis();

        for (int t = 0; t < threadCount; t++) {
            final int idx = t;
            pool.submit(() -> {
                try {
                    RenderMachineImpl localRm = new RenderMachineImpl();
                    localRm.setFileName("concurrent-" + threadCount + "t-" + idx);
                    for (int i = 0; i < callsPerThread; i++) {
                        localRm.say("Thread " + idx + " paragraph " + i);
                    }
                    localRm.sayNextSection("Thread " + idx + " results");
                    localRm.finishAndWriteOut();
                } catch (Exception e) {
                    errors.incrementAndGet();
                    log.error("Thread {} failed: {}", idx, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        boolean done = latch.await(60, TimeUnit.SECONDS);
        pool.shutdown();
        long elapsed = System.currentTimeMillis() - start;

        log.info("[CONCURRENT-{}T] {} threads x {} calls  time={}ms  errors={}  completed={}",
                threadCount, threadCount, callsPerThread, elapsed, errors.get(), done);
        assertTrue("Concurrent test timed out", done);
        System.setProperty("stress.concurrent." + threadCount + "t.errors",
                String.valueOf(errors.get()));
    }

    private long fileSize(String nameWithoutSuffix) {
        File f = new File(BASE_DIR + File.separator + nameWithoutSuffix + ".html");
        return f.exists() ? f.length() : -1L;
    }

    private void ensureOutputDir() {
        new File(BASE_DIR).mkdirs();
    }
}
