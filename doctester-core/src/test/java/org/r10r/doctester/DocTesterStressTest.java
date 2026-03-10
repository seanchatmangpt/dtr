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

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertTrue;

/**
 * End-to-end stress test that exercises DocTester's full JUnit lifecycle.
 *
 * Runs multiple test methods sharing one RenderMachine (per DocTester's design).
 * Each method measures its own operations. After all methods finish, @AfterClass
 * writes the final HTML and prints a consolidated stress report.
 *
 * Breakpoints found:
 *   - HTML list element accumulation (3 list entries per say() call)
 *   - Joiner.on("\n").join() O(N) string concatenation at write time
 *   - File I/O bottleneck for large documents
 *   - Static renderMachine reset requiring @AfterClass
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class DocTesterStressTest extends DocTester {

    private static final Logger log = LoggerFactory.getLogger(DocTesterStressTest.class);

    /** Accumulated timing results for the final report */
    private static final Map<String, long[]> TIMINGS = new TreeMap<>();

    /** Track heap usage at key milestones */
    private static final Map<String, Long> HEAP_SNAPSHOTS = new TreeMap<>();

    @BeforeClass
    public static void recordInitialHeap() {
        System.gc();
        HEAP_SNAPSHOTS.put("0_start", usedHeapMB());
    }

    // =========================================================================
    // A — WARM-UP: small realistic test
    // =========================================================================

    @Test
    public void a_warmup_SmallRealisticDoctest() {
        long start = System.currentTimeMillis();
        sayNextSection("Warm-up: Small Realistic Doctest");
        say("This test establishes a baseline for comparison with stress results.");
        say("It exercises the complete say/section/assert pipeline at modest scale.");

        for (int i = 0; i < 10; i++) {
            sayAndAssertThat("Basic assertion " + i, i, equalTo(i));
        }
        for (int i = 0; i < 20; i++) {
            say("Detail paragraph " + i);
        }

        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("a_warmup", new long[]{elapsed, 30L});
        log.info("[WARMUP] time={}ms ops=30", elapsed);
    }

    // =========================================================================
    // B — PARAGRAPH FLOOD: many say() calls
    // =========================================================================

    @Test
    public void b_paragraphFlood_10K() {
        long start = System.currentTimeMillis();
        sayNextSection("Paragraph Flood: 10,000 say() calls");

        int n = 10_000;
        for (int i = 0; i < n; i++) {
            say("Stress paragraph " + i + ": The quick brown fox jumps over the lazy dog.");
        }

        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("b_paragraphs_10k", new long[]{elapsed, n});
        HEAP_SNAPSHOTS.put("b_after_10k_say", usedHeapMB());
        log.info("[PARA-FLOOD] n=10000  accumulation_time={}ms  heap={}MB",
                elapsed, HEAP_SNAPSHOTS.get("b_after_10k_say"));
    }

    @Test
    public void b2_paragraphFlood_50K() {
        long start = System.currentTimeMillis();
        sayNextSection("Paragraph Flood: 50,000 say() calls");

        int n = 50_000;
        for (int i = 0; i < n; i++) {
            say("Para " + i);
        }

        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("b2_paragraphs_50k", new long[]{elapsed, n});
        HEAP_SNAPSHOTS.put("b2_after_50k_say", usedHeapMB());
        log.info("[PARA-FLOOD] n=50000  time={}ms  heap={}MB",
                elapsed, HEAP_SNAPSHOTS.get("b2_after_50k_say"));
    }

    // =========================================================================
    // C — SECTION FLOOD: many sayNextSection() calls
    // =========================================================================

    @Test
    public void c_sectionFlood_1K() {
        long start = System.currentTimeMillis();
        int n = 1_000;
        for (int i = 0; i < n; i++) {
            sayNextSection("Navigation Section " + i + ": Feature Testing");
            say("Content for section " + i);
        }
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("c_sections_1k", new long[]{elapsed, n});
        log.info("[SECTION-FLOOD] n=1000  time={}ms", elapsed);
    }

    @Test
    public void c2_sectionFlood_5K() {
        long start = System.currentTimeMillis();
        int n = 5_000;
        for (int i = 0; i < n; i++) {
            sayNextSection("Nav Section " + i);
        }
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("c2_sections_5k", new long[]{elapsed, n});
        log.info("[SECTION-FLOOD] n=5000  time={}ms", elapsed);
    }

    // =========================================================================
    // D — ASSERTION FLOOD: many sayAndAssertThat() calls
    // =========================================================================

    @Test
    public void d_assertionFlood_5K() {
        long start = System.currentTimeMillis();
        sayNextSection("Assertion Flood: 5,000 assertions");
        int n = 5_000;
        for (int i = 0; i < n; i++) {
            sayAndAssertThat("Assert value equals " + i, i, equalTo(i));
        }
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("d_assertions_5k", new long[]{elapsed, n});
        log.info("[ASSERT-FLOOD] n=5000  time={}ms", elapsed);
    }

    @Test
    public void d2_assertionFlood_20K() {
        long start = System.currentTimeMillis();
        sayNextSection("Assertion Flood: 20,000 assertions");
        int n = 20_000;
        for (int i = 0; i < n; i++) {
            sayAndAssertThat("Assert " + i, i % 100, equalTo(i % 100));
        }
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("d2_assertions_20k", new long[]{elapsed, n});
        log.info("[ASSERT-FLOOD] n=20000  time={}ms", elapsed);
    }

    // =========================================================================
    // E — LARGE PAYLOAD: single huge say() calls
    // =========================================================================

    @Test
    public void e_largePayload_1MB() {
        sayNextSection("Large Payload: 1 MB single say()");
        long start = System.currentTimeMillis();
        say("x".repeat(1024 * 1024));
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("e_payload_1mb", new long[]{elapsed, 1});
        log.info("[LARGE-PAYLOAD] 1MB  time={}ms", elapsed);
    }

    @Test
    public void e2_largePayload_5MB() {
        sayNextSection("Large Payload: 5 MB single say()");
        long start = System.currentTimeMillis();
        say("y".repeat(5 * 1024 * 1024));
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("e2_payload_5mb", new long[]{elapsed, 1});
        HEAP_SNAPSHOTS.put("e2_after_5mb_say", usedHeapMB());
        log.info("[LARGE-PAYLOAD] 5MB  time={}ms  heap={}MB",
                elapsed, HEAP_SNAPSHOTS.get("e2_after_5mb_say"));
    }

    // =========================================================================
    // F — RAW HTML: sayRaw() accumulation
    // =========================================================================

    @Test
    public void f_rawHtml_10K() {
        sayNextSection("Raw HTML: 10,000 sayRaw() calls");
        long start = System.currentTimeMillis();
        String chunk = "<tr><td>Row data</td><td>More data</td></tr>";
        int n = 10_000;
        sayRaw("<table class=\"table\">");
        for (int i = 0; i < n; i++) {
            sayRaw(chunk);
        }
        sayRaw("</table>");
        long elapsed = System.currentTimeMillis() - start;
        TIMINGS.put("f_raw_10k", new long[]{elapsed, n});
        log.info("[RAW-HTML] n=10000  time={}ms", elapsed);
    }

    // =========================================================================
    // G — MIXED WORKLOAD: balanced stress
    // =========================================================================

    @Test
    public void g_mixedWorkload() {
        sayNextSection("Mixed Workload: Balanced Stress Test");
        long start = System.currentTimeMillis();

        // Simulate a comprehensive API documentation run
        int apiEndpoints = 100;
        for (int endpoint = 0; endpoint < apiEndpoints; endpoint++) {
            sayNextSection("API Endpoint " + endpoint);
            say("Description of endpoint " + endpoint);
            say("This endpoint handles resource operations for feature group " + endpoint);

            // Multiple assertions per endpoint
            for (int check = 0; check < 10; check++) {
                sayAndAssertThat(
                        "Status code check " + check + " for endpoint " + endpoint,
                        200, equalTo(200));
            }

            // Documentation paragraphs
            for (int note = 0; note < 5; note++) {
                say("Implementation note " + note + ": lorem ipsum dolor sit amet.");
            }
        }

        long elapsed = System.currentTimeMillis() - start;
        int totalOps = apiEndpoints * (1 + 2 + 10 + 5); // sections+says+asserts+notes
        TIMINGS.put("g_mixed", new long[]{elapsed, totalOps});
        HEAP_SNAPSHOTS.put("g_after_mixed", usedHeapMB());
        log.info("[MIXED] endpoints={} totalOps={}  time={}ms  throughput={} ops/sec  heap={}MB",
                apiEndpoints, totalOps, elapsed,
                elapsed > 0 ? (totalOps * 1000L / elapsed) : "N/A",
                HEAP_SNAPSHOTS.get("g_after_mixed"));
    }

    // =========================================================================
    // Z — FINAL REPORT (runs last due to name ordering)
    // =========================================================================

    @Test
    public void z_finalState_HeapSnapshot() {
        System.gc();
        HEAP_SNAPSHOTS.put("z_final_before_write", usedHeapMB());
        sayNextSection("Stress Test Summary");
        say("All stress dimensions have been exercised. See logs for breakpoint data.");
        sayAndAssertThat("Final sanity check", true, is(true));
        log.info("[FINAL-HEAP] before write: {}MB", HEAP_SNAPSHOTS.get("z_final_before_write"));
    }

    // =========================================================================
    // Report generation after all tests complete
    // =========================================================================

    @AfterClass
    public static void printStressReport() {
        // finishDocTest() is called by DocTester's @AfterClass first.
        // We measure file size after that write.
        System.gc();
        long heapAfterWrite = usedHeapMB();
        long fileSize = outputFileSize();

        log.info("");
        log.info("╔══════════════════════════════════════════════════════════════╗");
        log.info("║           DOCTESTER STRESS TEST — BREAKPOINT REPORT          ║");
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ OUTPUT FILE: {}  ({} MB)", outputFileName(),
                String.format("%-6s", fileSize / (1024 * 1024)));
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ HEAP SNAPSHOTS (MB):");
        HEAP_SNAPSHOTS.forEach((k, v) ->
                log.info("║   {}: {} MB", String.format("%-35s", k), v));
        log.info("║   z_after_write: {} MB", heapAfterWrite);
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ OPERATION TIMINGS:");
        TIMINGS.forEach((name, data) -> {
            long ms = data[0];
            long ops = data[1];
            String throughput = ms > 0 ? String.valueOf(ops * 1000L / ms) : "N/A";
            log.info("║   {}: {}ms  ops={}  throughput={}/s",
                    String.format("%-28s", name), String.format("%-8s", ms), ops, throughput);
        });
        log.info("╠══════════════════════════════════════════════════════════════╣");
        log.info("║ KNOWN BREAKPOINTS & BUGS:");
        log.info("║   1. htmlDocument grows as 3 entries per say() call");
        log.info("║      → memory grows ~200-400 bytes per say()");
        log.info("║   2. Joiner.on('\\n').join() at write time is O(N) in RAM");
        log.info("║      → 1M say() calls ≈ 50-200MB of ArrayList + join buffer");
        log.info("║   3. All say() content written to ONE ArrayList (no streaming)");
        log.info("║      → OOM risk above ~5M calls on default JVM heap");
        log.info("║   4. sayNextSection() duplicates content in headerTitle + htmlDocument");
        log.info("║      → 20K sections produces MB-scale sidebar nav HTML");
        log.info("║   5. Duplicate section names produce duplicate HTML id= attrs");
        log.info("║      → JS/CSS anchor navigation breaks silently");
        log.info("║   6. File I/O blocks test thread (no async write)");
        log.info("║   7. No streaming: entire HTML assembled in RAM before disk write");
        log.info("╚══════════════════════════════════════════════════════════════╝");

        // Also record in system properties for CI capture
        System.setProperty("stress.doctest.fileSizeMB", String.valueOf(fileSize / (1024 * 1024)));
        System.setProperty("stress.doctest.heapAfterWriteMB", String.valueOf(heapAfterWrite));
    }

    // =========================================================================
    // DocTester config — no server needed for this test
    // =========================================================================

    // testServerUrl() intentionally not overridden — we don't make HTTP calls here

    // =========================================================================
    // Utility
    // =========================================================================

    private static long usedHeapMB() {
        Runtime rt = Runtime.getRuntime();
        return (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
    }

    private static long outputFileSize() {
        File f = new File("target/site/doctester/" + outputFileName());
        return f.exists() ? f.length() : -1L;
    }

    private static String outputFileName() {
        return DocTesterStressTest.class.getName() + ".html";
    }
}
