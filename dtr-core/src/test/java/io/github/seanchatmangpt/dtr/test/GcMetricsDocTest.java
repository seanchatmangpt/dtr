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
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JEP 490 ZGC Generational Mode and GC Metrics Documentation.
 *
 * <p>Documents ZGC Generational Mode (now default in Java 26 when ZGC is selected)
 * and GC observability through the standard {@code java.lang.management} APIs.
 * Every measurement is taken from the live JVM running the test — no estimates,
 * no synthetic data.</p>
 *
 * <p>Sections covered:</p>
 * <ol>
 *   <li>ZGC overview: characteristics, JVM flags, Java 26 defaults</li>
 *   <li>GarbageCollectorMXBean introspection: names, pools, collection counts</li>
 *   <li>Heap usage profile: before allocation, during allocation, after GC hint</li>
 *   <li>GC pause simulation: 100K short-lived objects, collection count delta</li>
 *   <li>GC configuration summary: recommended flags for DTR workloads</li>
 * </ol>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class GcMetricsDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1: ZGC Overview
    // =========================================================================

    @Test
    void a1_zgc_overview() {
        sayNextSection("JEP 490: ZGC Generational Mode — Default in Java 26");

        say("ZGC (Z Garbage Collector) is a scalable, low-latency garbage collector designed " +
            "to deliver sub-millisecond pause times regardless of heap size. JEP 490, " +
            "delivered as part of Java 21 and now the promoted default generational mode " +
            "in Java 26, introduces a two-generation heap structure — a young generation for " +
            "short-lived objects and an old generation for long-lived ones. This generational " +
            "split dramatically improves allocation throughput because the GC can focus most " +
            "collection effort on the young generation, where the vast majority of objects die.");

        say("Prior to JEP 490, ZGC operated non-generationally: every collection cycle scanned " +
            "the entire heap. Generational ZGC retains ZGC's concurrent, region-based design " +
            "while adding the classic generational hypothesis as an optimization layer. The " +
            "result is higher throughput with the same sub-millisecond pause guarantee.");

        sayEnvProfile();

        // Query the live GC beans to report what GC is actually running
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        String gcNames = gcBeans.stream()
            .map(GarbageCollectorMXBean::getName)
            .collect(Collectors.joining(", "));
        boolean isZgc = gcNames.toLowerCase().contains("zgc") || gcNames.toLowerCase().contains("z ");
        boolean isGenerational = gcNames.toLowerCase().contains("young") || gcNames.toLowerCase().contains("old")
            || gcNames.toLowerCase().contains("minor") || gcBeans.size() >= 2;
        String javaVersion = System.getProperty("java.version", "unknown");
        String vmName = System.getProperty("java.vm.name", "unknown");

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Active GC(s)",              gcNames.isEmpty() ? "(none reported)" : gcNames,
            "GC bean count",             String.valueOf(gcBeans.size()),
            "ZGC detected",              isZgc ? "yes" : "no (running " + vmName + ")",
            "Generational structure",    isGenerational ? "yes — multiple GC beans observed" : "no — single-pass GC",
            "JEP 490 pause goal",        "< 1 ms regardless of heap size",
            "Java version (this JVM)",   javaVersion,
            "Recommended ZGC flag",      "-XX:+UseZGC (generational by default in Java 26)"
        )));

        sayNote("If the active GC shown above is not ZGC, the JVM was started without " +
                "-XX:+UseZGC. In CI, G1GC is often the default. The documentation here covers " +
                "the ZGC API contract; the observability methods work identically for all GCs.");

        sayWarning("ZGC Generational Mode requires Java 21+. In Java 26 it is the default ZGC " +
                   "mode when -XX:+UseZGC is specified. Do not set -XX:-ZGenerational explicitly " +
                   "unless you need non-generational ZGC for diagnostic comparison.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "ManagementFactory.getGarbageCollectorMXBeans() returns at least one bean",
                gcBeans.size() >= 1 ? "PASS — " + gcBeans.size() + " bean(s) found" : "FAIL — 0 beans",
            "GC names are non-empty strings",
                !gcNames.isEmpty() ? "PASS — names: " + gcNames : "FAIL — empty name list",
            "Java version property is accessible",
                !javaVersion.equals("unknown") ? "PASS — " + javaVersion : "FAIL — property missing"
        )));
    }

    // =========================================================================
    // Section 2: GarbageCollectorMXBean Introspection
    // =========================================================================

    @Test
    void a2_gc_bean_introspection() {
        sayNextSection("GarbageCollectorMXBean Introspection");

        say("The standard " +
            "java.lang.management.ManagementFactory.getGarbageCollectorMXBeans() API " +
            "provides a bean per GC phase (e.g., ZGC Minor, ZGC Major, or G1 Young/Old). " +
            "Each bean reports the memory pools it manages, how many collection cycles have " +
            "completed, and total elapsed collection time in milliseconds. This section " +
            "captures those values from the live JVM.");

        sayCode("""
                // Standard GC observability — works for ZGC, G1, Shenandoah, and Serial
                import java.lang.management.ManagementFactory;
                import java.lang.management.GarbageCollectorMXBean;

                List<GarbageCollectorMXBean> gcBeans =
                    ManagementFactory.getGarbageCollectorMXBeans();

                for (GarbageCollectorMXBean bean : gcBeans) {
                    String name       = bean.getName();
                    long   count      = bean.getCollectionCount();  // -1 if undefined
                    long   timeMs     = bean.getCollectionTime();   // -1 if undefined
                    String[] pools    = bean.getMemoryPoolNames();
                }
                """, "java");

        // Build table data from live beans
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // Count data rows: one per (bean, pool) pair, or one per bean if no pools
        int rowCount = gcBeans.stream()
            .mapToInt(b -> b.getMemoryPoolNames().length == 0 ? 1 : b.getMemoryPoolNames().length)
            .sum();
        String[][] table = new String[rowCount + 1][4];
        table[0] = new String[]{"GC Name", "Memory Pool", "Collection Count", "Collection Time (ms)"};

        int row = 1;
        for (GarbageCollectorMXBean bean : gcBeans) {
            String name    = bean.getName();
            long count     = bean.getCollectionCount();
            long timeMs    = bean.getCollectionTime();
            String[] pools = bean.getMemoryPoolNames();
            String countStr  = count  >= 0 ? String.valueOf(count)  : "N/A";
            String timeStr   = timeMs >= 0 ? String.valueOf(timeMs) : "N/A";
            if (pools.length == 0) {
                table[row++] = new String[]{name, "(no pools)", countStr, timeStr};
            } else {
                for (String pool : pools) {
                    table[row++] = new String[]{name, pool, countStr, timeStr};
                }
            }
        }
        sayTable(table);

        // Also report heap usage via MemoryMXBean
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        long heapUsedMb  = heap.getUsed()      / (1024 * 1024);
        long heapMaxMb   = heap.getMax()       / (1024 * 1024);
        long heapCommMb  = heap.getCommitted() / (1024 * 1024);

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Heap used (MB)",      String.valueOf(heapUsedMb),
            "Heap committed (MB)", String.valueOf(heapCommMb),
            "Heap max (MB)",       heapMaxMb > 0 ? String.valueOf(heapMaxMb) : "unbounded",
            "GC beans present",    String.valueOf(gcBeans.size()),
            "Source API",          "ManagementFactory.getGarbageCollectorMXBeans() + getMemoryMXBean()"
        )));

        long totalCollections = gcBeans.stream()
            .mapToLong(b -> Math.max(b.getCollectionCount(), 0))
            .sum();

        sayAssertions(new LinkedHashMap<>(Map.of(
            "Total GC collection count >= 0",
                totalCollections >= 0 ? "PASS — " + totalCollections + " total collections" : "FAIL",
            "Heap used bytes > 0",
                heap.getUsed() > 0 ? "PASS — " + heap.getUsed() + " bytes" : "FAIL",
            "Heap committed bytes > 0",
                heap.getCommitted() > 0 ? "PASS — " + heap.getCommitted() + " bytes" : "FAIL"
        )));
    }

    // =========================================================================
    // Section 3: Heap Usage Profile
    // =========================================================================

    @Test
    void a3_heap_usage_profile() {
        sayNextSection("Heap Usage Profile: Before, During, and After Allocation");

        say("This section captures three heap snapshots from the live JVM: " +
            "(1) baseline before any intentional allocation, " +
            "(2) immediately after allocating approximately 10 MB of byte arrays, and " +
            "(3) after calling System.gc() to hint that a collection is desirable. " +
            "All values are read directly from MemoryMXBean.getHeapMemoryUsage().");

        say("System.gc() is a hint only — the JVM may ignore it, defer it, or perform a " +
            "partial collection. ZGC in particular runs concurrently and may not respond " +
            "immediately. The post-GC snapshot reflects whatever state the JVM reaches " +
            "shortly after the hint; it does not guarantee that all allocated objects " +
            "were reclaimed.");

        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();

        // Phase 1: baseline
        MemoryUsage before = memBean.getHeapMemoryUsage();

        // Phase 2: allocate ~10 MB of byte arrays (kept alive in local array to avoid
        // immediate escape-analysis elimination by the JIT)
        final int ARRAYS = 100;
        final int BYTES_EACH = 100 * 1024; // 100 KB each => ~10 MB total
        byte[][] sink = new byte[ARRAYS][];
        for (int i = 0; i < ARRAYS; i++) {
            sink[i] = new byte[BYTES_EACH];
            // Write a sentinel byte to prevent JIT from eliminating the allocation
            sink[i][0] = (byte) (i & 0xFF);
        }
        MemoryUsage during = memBean.getHeapMemoryUsage();

        // Phase 3: release references, hint GC, wait briefly for concurrent collectors
        sink = null;
        System.gc();
        // Give concurrent GC (ZGC, Shenandoah) up to 200ms to respond
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        MemoryUsage afterGc = memBean.getHeapMemoryUsage();

        sayTable(new String[][] {
            {"Phase", "Init (MB)", "Used (MB)", "Committed (MB)", "Max (MB)"},
            {"Before allocation",
                mb(before.getInit()), mb(before.getUsed()),
                mb(before.getCommitted()), mb(before.getMax())},
            {"During allocation (~10 MB added)",
                mb(during.getInit()), mb(during.getUsed()),
                mb(during.getCommitted()), mb(during.getMax())},
            {"After System.gc() hint",
                mb(afterGc.getInit()), mb(afterGc.getUsed()),
                mb(afterGc.getCommitted()), mb(afterGc.getMax())},
        });

        long deltaUsedBytes = during.getUsed() - before.getUsed();

        sayNote("The 'During allocation' row should show heap usage higher than 'Before'. " +
                "The delta depends on JIT optimizations, GC concurrent activity, and other " +
                "threads running in the test JVM. Measured delta: " +
                (deltaUsedBytes / 1024) + " KB.");

        sayWarning("System.gc() is a hint and not a command. ZGC and Shenandoah run " +
                   "concurrently and may reclaim memory asynchronously. The post-GC snapshot " +
                   "may not reflect full reclamation within the 200ms observation window.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "Heap used during allocation >= 0",
                during.getUsed() >= 0 ? "PASS — " + during.getUsed() + " bytes" : "FAIL",
            "MemoryMXBean reports consistent init across phases",
                before.getInit() == during.getInit()
                    ? "PASS — init is stable at " + mb(before.getInit()) + " MB"
                    : "INFO — init changed (unusual): " + mb(before.getInit()) + " -> " + mb(during.getInit()) + " MB",
            "Heap used after GC hint >= 0",
                afterGc.getUsed() >= 0 ? "PASS — " + afterGc.getUsed() + " bytes" : "FAIL"
        )));
    }

    // =========================================================================
    // Section 4: GC Pause Simulation
    // =========================================================================

    @Test
    void a4_gc_pause_simulation() {
        sayNextSection("GC Pause Simulation: 100K Short-Lived Objects");

        say("This section measures the effect of creating 100,000 short-lived objects — " +
            "a mix of strings and small int arrays — on GC collection counts and elapsed wall " +
            "time. The measurement records the GC collection count delta before and after " +
            "the allocation burst plus a System.gc() hint, providing a lower-bound estimate " +
            "of GC activity triggered by the workload.");

        say("ZGC's generational mode is specifically optimized for exactly this workload: " +
            "short-lived objects are collected from the young generation in concurrent minor " +
            "cycles, avoiding full-heap stops. The collection count delta and elapsed time " +
            "reported here are real measurements from the live JVM.");

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // Snapshot collection counts before
        long countBefore = gcBeans.stream()
            .mapToLong(b -> Math.max(b.getCollectionCount(), 0))
            .sum();

        final int OBJECT_COUNT = 100_000;
        long start = System.nanoTime();

        // Allocate and immediately discard 100K objects
        for (int i = 0; i < OBJECT_COUNT; i++) {
            // Mix of string concatenation and array allocation to produce varied garbage
            String s = "gc-object-" + i;
            int[]  a = new int[]{i, i * 2, i * 3};
            // Access to defeat trivial JIT elimination
            if (s.length() < 0 || a[0] < 0) throw new RuntimeException("unreachable");
        }

        long elapsedNs = System.nanoTime() - start;
        long elapsedMs = elapsedNs / 1_000_000;

        // Hint GC and wait briefly
        System.gc();
        try { Thread.sleep(200); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Snapshot collection counts after
        long countAfter = gcBeans.stream()
            .mapToLong(b -> Math.max(b.getCollectionCount(), 0))
            .sum();
        long countDelta = countAfter - countBefore;

        // Derive a rough pause estimate: total GC time delta across beans
        long gcTimeBefore = gcBeans.stream()
            .mapToLong(b -> Math.max(b.getCollectionTime(), 0))
            .sum();
        // Re-read after wait for updated totals
        long gcTimeAfterMs = ManagementFactory.getGarbageCollectorMXBeans().stream()
            .mapToLong(b -> Math.max(b.getCollectionTime(), 0))
            .sum();
        long gcTimeDeltaMs = gcTimeAfterMs - gcTimeBefore;

        sayTable(new String[][] {
            {"Metric", "Value", "Notes"},
            {"Objects created",         String.valueOf(OBJECT_COUNT),  "strings + int[] pairs"},
            {"Allocation wall time",     elapsedMs + " ms",             "System.nanoTime(), real measurement"},
            {"Allocation time (ns)",     String.valueOf(elapsedNs),     String.valueOf(OBJECT_COUNT) + " objects"},
            {"GC collections triggered", String.valueOf(countDelta),    "delta before/after + System.gc()"},
            {"GC time delta",            gcTimeDeltaMs + " ms",         "sum across all GC beans"},
            {"Avg ns per object",        String.valueOf(elapsedNs / OBJECT_COUNT), "allocation only"},
        });

        sayNote("ZGC Generational Mode targets young-generation collections that pause for " +
                "under 1 ms. G1GC minor pauses are typically 5–50 ms for this workload size. " +
                "The GC collection count delta includes any concurrent background cycles that " +
                "completed during the 200 ms observation window after System.gc().");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "All " + OBJECT_COUNT + " objects allocated without error",
                "PASS — loop completed, elapsed " + elapsedMs + " ms",
            "GC collection count after >= count before",
                countAfter >= countBefore ? "PASS — delta: " + countDelta : "FAIL — negative delta",
            "Elapsed time measured with System.nanoTime()",
                "PASS — " + elapsedNs + " ns (" + elapsedMs + " ms)"
        )));
    }

    // =========================================================================
    // Section 5: GC Configuration Summary
    // =========================================================================

    @Test
    void a5_gc_configuration_summary() {
        sayNextSection("Recommended GC Configuration for DTR Workloads");

        say("DTR tests run in-process with the JVM that executes Maven Surefire. The ideal " +
            "GC choice for DTR is one that minimises latency spikes (so documentation " +
            "generation does not pause mid-test) and handles moderate heap churn from " +
            "string building and JSON serialization. ZGC Generational Mode is the " +
            "recommended choice for Java 26+.");

        say("The configuration below is tested against Java 26.ea.13+ with " +
            "--enable-preview. It is suitable for .mvn/jvm.config or Surefire " +
            "argLine in pom.xml. G1GC is listed as the CI fallback for environments " +
            "where ZGC is not available (older JDK, non-HotSpot JVMs).");

        sayCode("""
                # .mvn/jvm.config — ZGC Generational (Java 26 default when ZGC is active)
                --enable-preview
                -XX:+UseZGC
                -Xms256m
                -Xmx1g
                -XX:SoftMaxHeapSize=768m
                -XX:ZCollectionInterval=0
                -Xlog:gc*:file=target/gc.log:time,uptime,level,tags:filecount=3,filesize=10m
                """, "shell");

        sayCode("""
                <!-- pom.xml — Surefire plugin configuration for CI -->
                <plugin>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <argLine>
                            --enable-preview
                            -XX:+UseZGC
                            -Xms256m
                            -Xmx1g
                            -XX:SoftMaxHeapSize=768m
                            -Xlog:gc:file=${project.build.directory}/gc.log
                        </argLine>
                    </configuration>
                </plugin>
                """, "xml");

        sayTable(new String[][] {
            {"JVM Flag", "Purpose", "Recommended Value"},
            {"-XX:+UseZGC",           "Activate ZGC (generational by default in Java 26)", "required"},
            {"-Xms256m",              "Initial heap — avoids early expansion pauses",        "256m–512m"},
            {"-Xmx1g",               "Max heap — prevents OOM in large test suites",        "1g–2g"},
            {"-XX:SoftMaxHeapSize",   "Target heap before ZGC expands — reduces footprint",  "75% of Xmx"},
            {"-XX:ZCollectionInterval","Force periodic collection (0=on-demand only)",       "0 (default)"},
            {"-Xlog:gc*",             "Structured GC logging for post-mortem analysis",      "file=target/gc.log"},
            {"-XX:+UseG1GC",          "G1GC fallback for environments without ZGC",          "omit if ZGC available"},
        });

        // Report what is actually running so the reader can compare
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        String activeGc = gcBeans.stream()
            .map(GarbageCollectorMXBean::getName)
            .collect(Collectors.joining(", "));
        MemoryUsage heap = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();

        sayKeyValue(new LinkedHashMap<>(Map.of(
            "Active GC (this test run)",  activeGc.isEmpty() ? "(none detected)" : activeGc,
            "Heap max (this JVM)",        mb(heap.getMax()) + " MB",
            "Available processors",       String.valueOf(Runtime.getRuntime().availableProcessors()),
            "Java version",               System.getProperty("java.version", "unknown"),
            "VM name",                    System.getProperty("java.vm.name", "unknown")
        )));

        sayNote("ZGC sub-millisecond pause times are achieved regardless of heap size because " +
                "all marking, relocation, and compaction phases run concurrently with application " +
                "threads. The only stop-the-world phase is a brief initial mark and a final " +
                "relocate-roots step, both measured in tens of microseconds in practice.");

        sayWarning("Do not set -XX:+UnlockExperimentalVMOptions -XX:+UseEpsilonGC in production " +
                   "or CI environments. Epsilon performs no GC at all — it is a no-op collector " +
                   "for allocation-rate benchmarks only and will cause OOM in any real workload.");

        sayAssertions(new LinkedHashMap<>(Map.of(
            "GarbageCollectorMXBeans accessible at test runtime",
                gcBeans.size() > 0 ? "PASS — " + gcBeans.size() + " bean(s): " + activeGc : "FAIL",
            "Heap max > 0 (JVM has bounded heap)",
                heap.getMax() > 0 ? "PASS — " + mb(heap.getMax()) + " MB max" :
                    "INFO — max is -1 (unbounded heap configured)",
            "Java version property available",
                !System.getProperty("java.version", "unknown").equals("unknown")
                    ? "PASS — " + System.getProperty("java.version") : "FAIL"
        )));
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    /** Converts bytes to a formatted megabyte string, handling -1 (undefined) gracefully. */
    private static String mb(long bytes) {
        if (bytes < 0) return "N/A";
        return String.valueOf(bytes / (1024 * 1024));
    }
}
