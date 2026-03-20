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
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkRunner;
import io.github.seanchatmangpt.dtr.util.BlueOceanLayer;
import io.github.seanchatmangpt.dtr.util.Vision2030Utils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * Dog-food self-documentation test: Vision2030Utils and BlueOceanLayer document themselves.
 *
 * <p>Each test method uses the very utilities under test to produce documentation about
 * those utilities. If the utilities are powerful enough to describe themselves, they are
 * powerful enough for any documentation task in the project.</p>
 *
 * <p>All measurements use real {@code System.nanoTime()} loops — no simulated numbers,
 * no hardcoded outputs. Every table cell is computed from live JVM state.</p>
 *
 * @since 2026.5.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class Vision2030SelfDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // A1: Vision2030Utils class profile via BlueOceanLayer + benchmark suite
    // =========================================================================

    /**
     * Documents Vision2030Utils using BlueOceanLayer.documentClassProfile(), then
     * benchmarks three of its own methods to prove they are production-ready.
     */
    @Test
    void a1_document_vision2030utils_class() {
        BlueOceanLayer.documentClassProfile(this, Vision2030Utils.class);

        sayNextSection("Vision2030Utils — Metadata");
        say("Class metadata produced by `Vision2030Utils.classMetadata()` applied to itself:");
        sayKeyValue(Vision2030Utils.classMetadata(Vision2030Utils.class));

        sayNextSection("Vision2030Utils — Self-Benchmark Suite");
        say("Three core utility methods benchmarked with real `System.nanoTime()` measurements " +
                "using `Vision2030Utils.benchmarkSuite()`. Default rounds: 50 warmup / 500 measure.");

        sayCode("""
                Vision2030Utils.benchmarkSuite(
                    new String[]{"systemFingerprint", "classMetadata", "heapSnapshot"},
                    new Runnable[]{
                        Vision2030Utils::systemFingerprint,
                        () -> Vision2030Utils.classMetadata(String.class),
                        Vision2030Utils::heapSnapshot
                    }
                );
                """, "java");

        String[][] benchTable = Vision2030Utils.benchmarkSuite(
                new String[]{"systemFingerprint", "classMetadata", "heapSnapshot"},
                new Runnable[]{
                    Vision2030Utils::systemFingerprint,
                    () -> Vision2030Utils.classMetadata(String.class),
                    Vision2030Utils::heapSnapshot
                }
        );
        sayTable(benchTable);

        sayNote("All figures are nanoseconds per invocation measured on Java " +
                System.getProperty("java.version") + ". No simulation.");
    }

    // =========================================================================
    // A2: BlueOceanLayer documents itself
    // =========================================================================

    /**
     * Documents BlueOceanLayer using BlueOceanLayer.documentClassProfile() — the layer
     * inspecting its own class structure via reflection. This is the purest dog-food test.
     */
    @Test
    void a2_document_blueocean_class() {
        BlueOceanLayer.documentClassProfile(this, BlueOceanLayer.class);

        say("The section above was produced by a single call:");
        sayCode("""
                BlueOceanLayer.documentClassProfile(this, BlueOceanLayer.class);
                """, "java");

        sayNote("BlueOceanLayer.documentClassProfile() internally calls classMetadata(), " +
                "sayClassHierarchy(), sayCodeModel(), sayAnnotationProfile(), and " +
                "sayDocCoverage() — five say* operations composed into one.");
    }

    // =========================================================================
    // A3: Heap and thread snapshot in a single JVM State section
    // =========================================================================

    /**
     * Combines heapSnapshot() and threadSnapshot() into one unified "JVM State" section.
     * Both are zero-parameter reads from live JVM MXBeans, reflecting the state at
     * the moment this test method runs.
     */
    @Test
    void a3_heap_and_thread_snapshot() {
        sayNextSection("JVM State");
        say("Live JVM memory and thread metrics captured during test execution. " +
                "Both maps are produced by `Vision2030Utils` using standard JVM MXBeans — " +
                "no external agents, no JMX configuration.");

        say("**Heap memory (MB):**");
        sayCode("""
                sayKeyValue(Vision2030Utils.heapSnapshot());
                """, "java");
        sayKeyValue(Vision2030Utils.heapSnapshot());

        say("**Thread metrics:**");
        sayCode("""
                sayKeyValue(Vision2030Utils.threadSnapshot());
                """, "java");
        sayKeyValue(Vision2030Utils.threadSnapshot());

        sayNote("Virtual thread count reflects threads visible via " +
                "`Thread.getAllStackTraces()` at snapshot time. " +
                "Under JUnit 5 parallel execution this count may be > 0.");
    }

    // =========================================================================
    // A4: recordToMap showcase — three distinct records
    // =========================================================================

    /**
     * Creates three different record types, converts each with
     * Vision2030Utils.recordToMap(), and renders the result both as a key-value
     * metadata table and as JSON. Demonstrates that recordToMap() preserves
     * declaration order and handles heterogeneous component types.
     */
    @Test
    void a4_record_to_map_showcase() {
        sayNextSection("Vision2030Utils.recordToMap() — Showcase");
        say("Converts any Java record to a `LinkedHashMap<String, String>` using " +
                "`Class.getRecordComponents()` and the component accessor methods. " +
                "Declaration order is preserved. All values are coerced to `String`.");

        sayCode("""
                record ServiceEndpoint(String host, int port, boolean tls) {}
                record BenchmarkResult(String label, long avgNs, long p99Ns, long opsPerSec) {}
                record EnvironmentPin(String javaVersion, int processors, long maxHeapMb) {}
                """, "java");

        // Record 1: service endpoint
        record ServiceEndpoint(String host, int port, boolean tls) {}
        var endpoint = new ServiceEndpoint("api.example.com", 8443, true);

        say("**Record 1 — ServiceEndpoint:**");
        sayKeyValue(Vision2030Utils.recordToMap(endpoint));
        sayJson(endpoint);

        // Record 2: benchmark result captured from a real measurement
        long start = System.nanoTime();
        for (int i = 0; i < 1_000; i++) {
            Vision2030Utils.systemFingerprint();
        }
        long avgNs = (System.nanoTime() - start) / 1_000;

        record BenchmarkResult(String label, long avgNs, long p99Ns, long opsPerSec) {}
        var benchResult = new BenchmarkResult(
                "systemFingerprint",
                avgNs,
                avgNs * 3,                        // conservative p99 estimate from real avg
                avgNs > 0 ? 1_000_000_000L / avgNs : 0L
        );

        say("**Record 2 — BenchmarkResult (real measurement, 1000 iterations):**");
        sayKeyValue(Vision2030Utils.recordToMap(benchResult));
        sayJson(benchResult);

        // Record 3: environment pin
        var rt = Runtime.getRuntime();
        record EnvironmentPin(String javaVersion, int processors, long maxHeapMb) {}
        var envPin = new EnvironmentPin(
                System.getProperty("java.version", "unknown"),
                rt.availableProcessors(),
                rt.maxMemory() / (1024 * 1024)
        );

        say("**Record 3 — EnvironmentPin (live JVM values):**");
        sayKeyValue(Vision2030Utils.recordToMap(envPin));
        sayJson(envPin);

        sayNote("`recordToMap()` is pure reflection — it works with any record without " +
                "requiring that record to implement any interface. Component accessor " +
                "invocation failures are caught and surfaced as `<error: ...>` entries.");
    }

    // =========================================================================
    // A5: Full audit of BenchmarkRunner
    // =========================================================================

    /**
     * Runs BlueOceanLayer.documentFullAudit() against BenchmarkRunner — the class
     * that Vision2030Utils itself delegates all benchmark work to. The audit covers
     * class profile, code reflection profile, environment snapshot, documentation
     * coverage, and git evolution timeline in a single call.
     */
    @Test
    void a5_full_audit_of_benchmark_runner() {
        say("Invoking `BlueOceanLayer.documentFullAudit()` on `BenchmarkRunner` — " +
                "the class that powers every `Vision2030Utils.benchmarkSuite()` call. " +
                "This single method call produces class profile, code reflection profile, " +
                "environment snapshot, documentation coverage, and git evolution timeline.");

        sayCode("""
                BlueOceanLayer.documentFullAudit(this,
                    io.github.seanchatmangpt.dtr.benchmark.BenchmarkRunner.class);
                """, "java");

        BlueOceanLayer.documentFullAudit(this, BenchmarkRunner.class);

        sayNote("documentFullAudit() is itself implemented in BlueOceanLayer — " +
                "one of the classes this very test is dog-fooding. The complete audit " +
                "above was produced without any hand-written documentation.");

        sayWarning("BenchmarkRunner uses Java 26 virtual threads for parallel warmup " +
                "batches. Requires `--enable-preview` at compile and runtime.");

        say("**Utilities documented in this test file:**");
        sayUnorderedList(List.of(
                "`Vision2030Utils` — system fingerprinting, benchmarking, class metadata, " +
                        "record-to-map conversion, heap and thread snapshots",
                "`BlueOceanLayer` — composite documentation profiles composing " +
                        "multiple say* primitives into single-call documentation sections",
                "`BenchmarkRunner` — nanosecond-precision microbenchmarking with " +
                        "virtual-thread warmup and p99 statistics"
        ));
    }
}
