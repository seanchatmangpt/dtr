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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.util.BlueOceanLayer;
import io.github.seanchatmangpt.dtr.util.Vision2030Utils;

/**
 * Integration test for Vision 2030 utilities and the BlueOceanLayer composite.
 *
 * <p>Validates that:</p>
 * <ol>
 *   <li>{@link Vision2030Utils#systemFingerprint()} returns non-empty values for all keys</li>
 *   <li>{@link Vision2030Utils#benchmarkComparison(String, Runnable, String, Runnable)} produces valid table dimensions</li>
 *   <li>{@link Vision2030Utils#classMetadata(Class)} returns accurate data for known classes</li>
 *   <li>{@link BlueOceanLayer} composite methods execute without exceptions on real classes</li>
 *   <li>End-to-end pipeline: utils -> BlueOceanLayer -> RenderMachine -> output</li>
 * </ol>
 *
 * <p>This is a standard JUnit 5 test (NOT extending DtrTest) that verifies the
 * utilities work correctly in isolation using assertions.</p>
 *
 * <p><strong>Running this test:</strong></p>
 * <pre>{@code
 * mvnd test -pl dtr-core -Dtest=Vision2030UtilsIntegrationTest --enable-preview
 * }</pre>
 */
class Vision2030UtilsIntegrationTest {

    // =========================================================================
    // 1. systemFingerprint() returns non-empty values for all keys
    // =========================================================================

    @Nested
    class SystemFingerprintTests {

        @Test
        void systemFingerprint_returnsAllExpectedKeys() {
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();

            assertNotNull(fingerprint, "fingerprint must not be null");

            List<String> expectedKeys = List.of(
                    "Java Version", "Java Vendor", "OS",
                    "Processors", "Max Heap (MB)", "Timezone"
            );

            for (String key : expectedKeys) {
                assertTrue(fingerprint.containsKey(key),
                        "fingerprint must contain key: " + key);
                assertNotNull(fingerprint.get(key),
                        "value for key '" + key + "' must not be null");
                assertFalse(fingerprint.get(key).isEmpty(),
                        "value for key '" + key + "' must not be empty");
            }
        }

        @Test
        void systemFingerprint_containsExactlySixKeys() {
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();
            assertEquals(6, fingerprint.size(),
                    "fingerprint must contain exactly 6 keys");
        }

        @Test
        void systemFingerprint_processorsIsPositiveInteger() {
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();
            int processors = Integer.parseInt(fingerprint.get("Processors"));
            assertTrue(processors > 0,
                    "Processors must be positive, got: " + processors);
        }

        @Test
        void systemFingerprint_maxHeapIsPositive() {
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();
            long heapMb = Long.parseLong(fingerprint.get("Max Heap (MB)"));
            assertTrue(heapMb > 0,
                    "Max Heap (MB) must be positive, got: " + heapMb);
        }

        @Test
        void systemFingerprint_javaVersionMatchesRuntime() {
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();
            String javaVersion = fingerprint.get("Java Version");
            assertEquals(System.getProperty("java.version"), javaVersion,
                    "Java Version must match System.getProperty(\"java.version\")");
        }

        @Test
        void systemFingerprint_osContainsOsName() {
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();
            String os = fingerprint.get("OS");
            assertTrue(os.contains(System.getProperty("os.name")),
                    "OS field must contain os.name, got: " + os);
        }
    }

    // =========================================================================
    // 2. benchmarkComparison() produces valid table dimensions
    // =========================================================================

    @Nested
    class BenchmarkComparisonTests {

        @Test
        void benchmarkComparison_producesCorrectDimensions() {
            String[][] table = Vision2030Utils.benchmarkComparison(
                    "Map.get()", () -> Map.of("k", "v").get("k"),
                    "List.get()", () -> List.of("a").getFirst()
            );

            // Header row + 5 metric rows = 6 rows total
            assertEquals(6, table.length,
                    "table must have 6 rows (header + 5 metric rows)");
            // 3 columns: Metric, labelA, labelB
            for (String[] row : table) {
                assertEquals(3, row.length,
                        "each row must have exactly 3 columns");
            }
        }

        @Test
        void benchmarkComparison_headerRowIsCorrect() {
            String[][] table = Vision2030Utils.benchmarkComparison(
                    "TaskA", () -> {},
                    "TaskB", () -> {}
            );

            assertEquals("Metric", table[0][0]);
            assertEquals("TaskA", table[0][1]);
            assertEquals("TaskB", table[0][2]);
        }

        @Test
        void benchmarkComparison_containsAllMetricLabels() {
            String[][] table = Vision2030Utils.benchmarkComparison(
                    "Op1", () -> {},
                    "Op2", () -> {}
            );

            // Metric column values
            assertEquals("Avg (ns)", table[1][0]);
            assertEquals("Min (ns)", table[2][0]);
            assertEquals("Max (ns)", table[3][0]);
            assertEquals("P99 (ns)", table[4][0]);
            assertEquals("Ops/sec", table[5][0]);
        }

        @Test
        void benchmarkComparison_timingsAreNonNegative() {
            String[][] table = Vision2030Utils.benchmarkComparison(
                    "Noop", () -> {},
                    "StringConcat", () -> { var s = "a" + "b"; }
            );

            for (int row = 1; row <= 4; row++) { // rows 1-4 are ns timings
                for (int col = 1; col <= 2; col++) {
                    long value = Long.parseLong(table[row][col]);
                    assertTrue(value >= 0,
                            "timing at [%d][%d] must be >= 0, got: %d".formatted(row, col, value));
                }
            }
        }

        @Test
        void benchmarkComparison_withExplicitRounds_producesValidTable() {
            String[][] table = Vision2030Utils.benchmarkComparison(
                    "Fast", () -> {},
                    "Slow", () -> { for (int i = 0; i < 10; i++) Math.random(); },
                    10, 50
            );

            assertEquals(6, table.length, "explicit rounds table must still have 6 rows");
        }

        @Test
        void benchmarkSuite_producesCorrectDimensions() {
            String[] labels = {"Op1", "Op2", "Op3"};
            Runnable[] tasks = {() -> {}, () -> {}, () -> {}};

            String[][] table = Vision2030Utils.benchmarkSuite(labels, tasks);

            // header + 3 data rows
            assertEquals(4, table.length,
                    "benchmarkSuite must produce labels.length + 1 rows");
            // 6 columns: Label, Avg, Min, Max, P99, Ops/sec
            assertEquals(6, table[0].length,
                    "benchmarkSuite must produce 6 columns");
        }

        @Test
        void benchmarkSuite_rejectsMismatchedLengths() {
            assertThrows(IllegalArgumentException.class,
                    () -> Vision2030Utils.benchmarkSuite(
                            new String[]{"a", "b"},
                            new Runnable[]{() -> {}}));
        }
    }

    // =========================================================================
    // 3. classMetadata() returns accurate data for known classes
    // =========================================================================

    @Nested
    class ClassMetadataTests {

        @Test
        void classMetadata_stringClass_returnsAccurateData() {
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(String.class);

            assertEquals("java.lang.String", meta.get("Class"));
            assertEquals("java.lang", meta.get("Package"));
            assertEquals("false", meta.get("Sealed"));
            assertEquals("false", meta.get("Record"));
            assertEquals("false", meta.get("Interface"));
        }

        @Test
        void classMetadata_listInterface_isInterface() {
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(List.class);

            assertEquals("true", meta.get("Interface"));
            assertEquals("false", meta.get("Record"));
        }

        @Test
        void classMetadata_record_isRecord() {
            record TestRecord(String name, int value) {}

            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(TestRecord.class);

            assertEquals("true", meta.get("Record"));
        }

        @Test
        void classMetadata_returnsAllExpectedKeys() {
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(Object.class);

            List<String> expectedKeys = List.of(
                    "Class", "Package", "Module", "Sealed", "Record",
                    "Interface", "Public Methods", "Declared Fields"
            );

            for (String key : expectedKeys) {
                assertTrue(meta.containsKey(key),
                        "metadata must contain key: " + key);
                assertNotNull(meta.get(key),
                        "value for key '" + key + "' must not be null");
            }
        }

        @Test
        void classMetadata_publicMethodCountIsPositiveForObject() {
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(Object.class);
            int count = Integer.parseInt(meta.get("Public Methods"));
            assertTrue(count > 0,
                    "Object must have public methods, got: " + count);
        }

        @Test
        void classMetadata_moduleIsNamedForJdkClass() {
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(String.class);
            assertEquals("java.base", meta.get("Module"),
                    "String.class must be in java.base module");
        }

        @Test
        void classMetadata_moduleIsUnnamedForProjectClass() {
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(Vision2030Utils.class);
            assertEquals("unnamed", meta.get("Module"),
                    "DTR classes on the classpath should have unnamed module");
        }
    }

    // =========================================================================
    // Additional Vision2030Utils methods
    // =========================================================================

    @Nested
    class AdditionalUtilityTests {

        @Test
        void recordToMap_convertsRecordToMap() {
            record Metric(String name, long value, String unit) {}
            var m = new Metric("latency", 42L, "ns");

            Map<String, String> map = Vision2030Utils.recordToMap(m);

            assertEquals("latency", map.get("name"));
            assertEquals("42", map.get("value"));
            assertEquals("ns", map.get("unit"));
            assertEquals(3, map.size());
        }

        @Test
        void recordToMap_rejectsNull() {
            assertThrows(NullPointerException.class,
                    () -> Vision2030Utils.recordToMap(null));
        }

        @Test
        void threadSnapshot_returnsExpectedKeys() {
            Map<String, String> snapshot = Vision2030Utils.threadSnapshot();

            assertTrue(snapshot.containsKey("Thread Count"));
            assertTrue(snapshot.containsKey("Daemon Thread Count"));
            assertTrue(snapshot.containsKey("Peak Thread Count"));
            assertTrue(snapshot.containsKey("Total Started Thread Count"));
            assertTrue(snapshot.containsKey("Virtual Thread Count"));

            int threadCount = Integer.parseInt(snapshot.get("Thread Count"));
            assertTrue(threadCount > 0, "Thread Count must be > 0");
        }

        @Test
        void heapSnapshot_returnsPositiveValues() {
            Map<String, String> heap = Vision2030Utils.heapSnapshot();

            assertTrue(heap.containsKey("Used Heap"));
            assertTrue(heap.containsKey("Free Heap"));
            assertTrue(heap.containsKey("Total Heap"));
            assertTrue(heap.containsKey("Max Heap"));

            // Parse the numeric portion (e.g., "123 MB" -> 123)
            for (String key : heap.keySet()) {
                String value = heap.get(key);
                assertTrue(value.endsWith(" MB"),
                        "heap value for '%s' must end with ' MB', got: %s".formatted(key, value));
                long mb = Long.parseLong(value.replace(" MB", ""));
                assertTrue(mb >= 0,
                        "heap value for '%s' must be >= 0, got: %d".formatted(key, mb));
            }
        }

        @Test
        void interfaceComplianceMatrix_producesCorrectDimensions() {
            String[][] matrix = Vision2030Utils.interfaceComplianceMatrix(
                    CharSequence.class, String.class, StringBuilder.class);

            assertTrue(matrix.length > 1,
                    "matrix must have header + at least 1 method row");
            assertEquals("Method", matrix[0][0]);
            assertEquals("String", matrix[0][1]);
            assertEquals("StringBuilder", matrix[0][2]);
        }

        @Test
        void interfaceComplianceMatrix_showsDirectForStringLength() {
            String[][] matrix = Vision2030Utils.interfaceComplianceMatrix(
                    CharSequence.class, String.class);

            // Find the row for "length()"
            boolean foundLength = false;
            for (int r = 1; r < matrix.length; r++) {
                if (matrix[r][0].startsWith("length")) {
                    foundLength = true;
                    assertTrue(matrix[r][1].contains("direct") || matrix[r][1].contains("inherited"),
                            "String must implement length(), got: " + matrix[r][1]);
                }
            }
            assertTrue(foundLength, "matrix must contain a row for length()");
        }
    }

    // =========================================================================
    // 4. BlueOceanLayer composite methods don't throw
    // =========================================================================

    @Nested
    class BlueOceanLayerTests {

        private RenderMachineImpl createMachine() {
            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("BlueOceanLayerTest");
            return machine;
        }

        @Test
        void documentClassProfile_withStringClass_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentClassProfile(machine, String.class));
        }

        @Test
        void documentClassProfile_withInterface_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentClassProfile(machine, RenderMachineCommands.class));
        }

        @Test
        void documentClassProfile_withRecord_doesNotThrow() {
            record SampleRecord(String name, int id) {}
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentClassProfile(machine, SampleRecord.class));
        }

        @Test
        void documentPerformanceProfile_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentPerformanceProfile(
                            machine,
                            "Map vs List",
                            new String[]{"Map.get()", "List.get()"},
                            new Runnable[]{
                                    () -> Map.of("k", "v").get("k"),
                                    () -> List.of("a").getFirst()
                            }
                    ));
        }

        @Test
        void documentPerformanceProfile_rejectsMismatchedArrays() {
            RenderMachineImpl machine = createMachine();
            assertThrows(IllegalArgumentException.class,
                    () -> BlueOceanLayer.documentPerformanceProfile(
                            machine, "test",
                            new String[]{"a", "b"},
                            new Runnable[]{() -> {}}
                    ));
        }

        @Test
        void documentContractCompliance_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentContractCompliance(
                            machine, CharSequence.class, String.class));
        }

        @Test
        void documentRecordProfile_doesNotThrow() {
            record Config(String host, int port) {}
            var example = new Config("localhost", 8080);
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentRecordProfile(machine, Config.class, example));
        }

        @Test
        void documentErrorPattern_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            var exception = new RuntimeException("outer",
                    new IllegalArgumentException("inner",
                            new NullPointerException("root")));
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentErrorPattern(machine, exception));
        }

        @Test
        void documentCodeReflectionProfile_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentCodeReflectionProfile(machine, String.class));
        }

        @Test
        void documentArchitectureDiagram_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentArchitectureDiagram(
                            machine, "DTR Core",
                            RenderMachineCommands.class, RenderMachineImpl.class));
        }

        @Test
        void documentArchitectureDiagram_rejectsEmptyClasses() {
            RenderMachineImpl machine = createMachine();
            assertThrows(IllegalArgumentException.class,
                    () -> BlueOceanLayer.documentArchitectureDiagram(
                            machine, "Empty"));
        }

        @Test
        void documentSystemLandscape_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentSystemLandscape(machine));
        }

        @Test
        void documentFullAudit_doesNotThrow() {
            RenderMachineImpl machine = createMachine();
            assertDoesNotThrow(
                    () -> BlueOceanLayer.documentFullAudit(machine, Object.class));
        }
    }

    // =========================================================================
    // 5. End-to-end pipeline: utils -> BlueOceanLayer -> RenderMachine -> output
    // =========================================================================

    @Nested
    class EndToEndPipelineTests {

        @Test
        void fullPipeline_producesMarkdownFile() {
            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("Vision2030PipelineTest");

            // Step 1: Use Vision2030Utils for data
            SequencedMap<String, String> fingerprint = Vision2030Utils.systemFingerprint();
            assertFalse(fingerprint.isEmpty());

            // Step 2: Feed data through BlueOceanLayer
            BlueOceanLayer.documentClassProfile(machine, String.class);

            // Step 3: Write output and verify file was created
            assertDoesNotThrow(machine::finishAndWriteOut,
                    "finishAndWriteOut must not throw after pipeline execution");

            java.io.File outputFile = new java.io.File("docs/test/Vision2030PipelineTest.md");
            assertTrue(outputFile.exists(),
                    "pipeline must produce output file: " + outputFile.getAbsolutePath());
            assertTrue(outputFile.length() > 0,
                    "output file must not be empty");
        }

        @Test
        void fullPipeline_outputContainsExpectedContent() throws Exception {
            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("Vision2030ContentTest");

            BlueOceanLayer.documentClassProfile(machine, String.class);
            machine.finishAndWriteOut();

            java.io.File outputFile = new java.io.File("docs/test/Vision2030ContentTest.md");
            assertTrue(outputFile.exists(), "output file must exist");

            String content = java.nio.file.Files.readString(outputFile.toPath());
            assertTrue(content.contains("Class Profile"),
                    "output must contain 'Class Profile' section heading");
            assertTrue(content.contains("String"),
                    "output must reference the profiled class name");
            assertTrue(content.contains("|"),
                    "output must contain table rows (pipe characters)");
        }

        @Test
        void benchmarkPipeline_measuresRealTimings() throws Exception {
            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("Vision2030BenchmarkPipelineTest");

            // Use BlueOceanLayer to run real benchmarks
            BlueOceanLayer.documentPerformanceProfile(
                    machine,
                    "Map vs String",
                    new String[]{"Map.get()", "String.indexOf()"},
                    new Runnable[]{
                            () -> Map.of("key", "value").get("key"),
                            () -> "hello world".indexOf("world")
                    }
            );

            machine.finishAndWriteOut();

            java.io.File outputFile = new java.io.File("docs/test/Vision2030BenchmarkPipelineTest.md");
            assertTrue(outputFile.exists(), "benchmark output file must exist");

            String content = java.nio.file.Files.readString(outputFile.toPath());
            assertTrue(content.contains("Map.get()"),
                    "output must contain the benchmark label 'Map.get()'");
            assertTrue(content.contains("String.indexOf()"),
                    "output must contain the benchmark label 'String.indexOf()'");
            assertTrue(content.contains("ns"),
                    "output must contain nanosecond timing data");
        }

        @Test
        void pipeline_finishAndWriteOut_doesNotThrow() {
            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("Vision2030WriteTest");

            BlueOceanLayer.documentClassProfile(machine, Object.class);

            assertDoesNotThrow(machine::finishAndWriteOut,
                    "finishAndWriteOut must not throw");
        }

        @Test
        void pipeline_outputContainsToc() throws Exception {
            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("Vision2030TocTest");

            BlueOceanLayer.documentClassProfile(machine, String.class);
            BlueOceanLayer.documentClassProfile(machine, Map.class);

            machine.finishAndWriteOut();

            java.io.File outputFile = new java.io.File("docs/test/Vision2030TocTest.md");
            assertTrue(outputFile.exists(), "output file must exist");

            String content = java.nio.file.Files.readString(outputFile.toPath());
            assertTrue(content.contains("Table of Contents"),
                    "output must contain a Table of Contents");
            assertTrue(content.contains("Class Profile: String"),
                    "TOC must reference String class profile");
            assertTrue(content.contains("Class Profile: Map"),
                    "TOC must reference Map class profile");
        }

        @Test
        void pipeline_endToEnd_utilsThroughLayerThroughRenderMachine() throws Exception {
            // This test verifies the complete chain:
            // Vision2030Utils -> BlueOceanLayer -> RenderMachineImpl -> markdown file

            RenderMachineImpl machine = new RenderMachineImpl();
            machine.setFileName("Vision2030EndToEndTest");

            // 1. Utils: system fingerprint
            SequencedMap<String, String> env = Vision2030Utils.systemFingerprint();
            machine.sayNextSection("Environment");
            machine.sayKeyValue(env);

            // 2. Utils: class metadata
            SequencedMap<String, String> meta = Vision2030Utils.classMetadata(RenderMachineImpl.class);
            machine.sayNextSection("Class Metadata");
            machine.sayKeyValue(meta);

            // 3. BlueOceanLayer: class profile
            BlueOceanLayer.documentClassProfile(machine, RenderMachineImpl.class);

            // 4. BlueOceanLayer: error pattern
            BlueOceanLayer.documentErrorPattern(machine,
                    new RuntimeException("test error", new NullPointerException("cause")));

            // 5. Write output
            machine.finishAndWriteOut();

            // 6. Verify the markdown file contains data from all pipeline stages
            java.io.File outputFile = new java.io.File("docs/test/Vision2030EndToEndTest.md");
            assertTrue(outputFile.exists(), "end-to-end output file must exist");

            String content = java.nio.file.Files.readString(outputFile.toPath());
            assertTrue(content.contains("RenderMachineImpl"),
                    "output must reference the profiled class");
            assertTrue(content.contains("Java Version"),
                    "output must contain environment fingerprint data");
            assertTrue(content.contains("test error"),
                    "output must contain the error pattern");
            assertTrue(content.contains("Table of Contents"),
                    "output must contain a Table of Contents from multiple sections");
        }
    }
}
