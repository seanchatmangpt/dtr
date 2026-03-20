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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.util.BlueOceanLayer;
import io.github.seanchatmangpt.dtr.util.Vision2030Utils;

/**
 * Integration test for {@link Vision2030Utils} and {@link BlueOceanLayer}.
 *
 * <p>Validates that the Vision 2030 utility layer produces correct, non-empty
 * results for all core operations — system fingerprinting, benchmark comparison,
 * class metadata, and BlueOceanLayer composite documentation methods.</p>
 *
 * <p>This is a standard JUnit 5 test (not extending DtrTest) that exercises
 * the utilities in isolation and then verifies end-to-end pipeline integration
 * through the RenderMachine.</p>
 *
 * <p><strong>Running this test:</strong></p>
 * <pre>{@code
 * mvnd test -pl dtr-integration-test -Dtest=Vision2030UtilsIntegrationTest --enable-preview
 * }</pre>
 *
 * @since 2026.5.0
 */
class Vision2030UtilsIntegrationTest {

    // =========================================================================
    // 1. Vision2030Utils.systemFingerprint()
    // =========================================================================

    @Test
    void systemFingerprint_returnsNonEmptyValuesForAllKeys() {
        Map<String, String> fingerprint = Vision2030Utils.systemFingerprint();

        assertNotNull(fingerprint, "Fingerprint map must not be null");
        assertFalse(fingerprint.isEmpty(), "Fingerprint map must not be empty");

        // Verify all expected keys are present
        List<String> expectedKeys = List.of(
                "Java Version", "Java Vendor", "OS", "Processors", "Max Heap (MB)", "Timezone");

        for (String key : expectedKeys) {
            assertTrue(fingerprint.containsKey(key),
                    "Fingerprint must contain key: " + key);
            String value = fingerprint.get(key);
            assertNotNull(value, "Value for key '" + key + "' must not be null");
            assertFalse(value.isBlank(), "Value for key '" + key + "' must not be blank");
        }

        // Verify specific values are plausible
        String javaVersion = fingerprint.get("Java Version");
        assertFalse("unknown".equals(javaVersion),
                "Java Version should not be 'unknown' in a real JVM");

        int processors = Integer.parseInt(fingerprint.get("Processors"));
        assertTrue(processors >= 1, "Must have at least 1 processor, got: " + processors);

        long maxHeapMb = Long.parseLong(fingerprint.get("Max Heap (MB)"));
        assertTrue(maxHeapMb > 0, "Max heap must be positive, got: " + maxHeapMb);
    }

    @Test
    void systemFingerprint_returnsExactlySixKeys() {
        Map<String, String> fingerprint = Vision2030Utils.systemFingerprint();
        assertEquals(6, fingerprint.size(),
                "Fingerprint should contain exactly 6 entries, got: " + fingerprint.keySet());
    }

    // =========================================================================
    // 2. Vision2030Utils.benchmarkComparison()
    // =========================================================================

    @Test
    void benchmarkComparison_producesValidTableDimensions() {
        String[][] table = Vision2030Utils.benchmarkComparison(
                "StringBuilder", () -> new StringBuilder("test").append("value").toString(),
                "String concat", () -> "test" + "value",
                5, 20 // small counts for test speed
        );

        assertNotNull(table, "Benchmark comparison table must not be null");

        // Table structure: 1 header row + 5 metric rows = 6 rows total
        assertEquals(6, table.length,
                "Table must have 6 rows (1 header + 5 metrics), got: " + table.length);

        // Each row must have exactly 3 columns: Metric, LabelA, LabelB
        for (int i = 0; i < table.length; i++) {
            assertEquals(3, table[i].length,
                    "Row " + i + " must have 3 columns, got: " + table[i].length);
        }

        // Verify header row
        assertEquals("Metric", table[0][0]);
        assertEquals("StringBuilder", table[0][1]);
        assertEquals("String concat", table[0][2]);

        // Verify metric labels in first column
        assertEquals("Avg (ns)", table[1][0]);
        assertEquals("Min (ns)", table[2][0]);
        assertEquals("Max (ns)", table[3][0]);
        assertEquals("P99 (ns)", table[4][0]);
        assertEquals("Ops/sec", table[5][0]);
    }

    @Test
    void benchmarkComparison_producesNumericValues() {
        String[][] table = Vision2030Utils.benchmarkComparison(
                "Noop A", () -> {},
                "Noop B", () -> {},
                5, 20
        );

        // All metric values (rows 1-5, columns 1-2) must parse as longs
        for (int row = 1; row < table.length; row++) {
            for (int col = 1; col <= 2; col++) {
                String cellValue = table[row][col];
                assertNotNull(cellValue, "Cell [" + row + "][" + col + "] must not be null");
                assertDoesNotThrow(() -> Long.parseLong(cellValue),
                        "Cell [" + row + "][" + col + "] must be a valid long, got: '" + cellValue + "'");
            }
        }
    }

    @Test
    void benchmarkComparison_defaultOverloadUsesDefaultRounds() {
        // The 4-arg overload delegates to 6-arg with warmup=50, measure=500
        String[][] table = assertDoesNotThrow(() ->
                Vision2030Utils.benchmarkComparison(
                        "A", () -> {},
                        "B", () -> {}
                ), "Default benchmarkComparison must not throw");

        assertNotNull(table);
        assertEquals(6, table.length);
    }

    // =========================================================================
    // 3. Vision2030Utils.classMetadata()
    // =========================================================================

    @Test
    void classMetadata_returnsAccurateDataForStringClass() {
        Map<String, String> metadata = Vision2030Utils.classMetadata(String.class);

        assertNotNull(metadata);
        assertEquals("java.lang.String", metadata.get("Class"));
        assertEquals("java.lang", metadata.get("Package"));
        assertEquals("java.base", metadata.get("Module"));
        assertEquals("false", metadata.get("Sealed"));
        assertEquals("false", metadata.get("Record"));
        assertEquals("false", metadata.get("Interface"));

        int publicMethods = Integer.parseInt(metadata.get("Public Methods"));
        assertTrue(publicMethods > 0, "String must have public methods, got: " + publicMethods);

        int declaredFields = Integer.parseInt(metadata.get("Declared Fields"));
        assertTrue(declaredFields >= 0, "Declared fields must be non-negative");
    }

    @Test
    void classMetadata_returnsAccurateDataForRecord() {
        record SampleRecord(String name, int value) {}

        Map<String, String> metadata = Vision2030Utils.classMetadata(SampleRecord.class);

        assertNotNull(metadata);
        assertTrue(metadata.get("Class").endsWith("SampleRecord"),
                "Class name should end with SampleRecord, got: " + metadata.get("Class"));
        assertEquals("true", metadata.get("Record"));
        assertEquals("false", metadata.get("Interface"));
        assertEquals("false", metadata.get("Sealed"));
    }

    @Test
    void classMetadata_returnsAccurateDataForInterface() {
        Map<String, String> metadata = Vision2030Utils.classMetadata(List.class);

        assertNotNull(metadata);
        assertEquals("java.util.List", metadata.get("Class"));
        assertEquals("true", metadata.get("Interface"));
        assertEquals("false", metadata.get("Record"));
    }

    @Test
    void classMetadata_containsExactlyEightKeys() {
        Map<String, String> metadata = Vision2030Utils.classMetadata(Object.class);

        assertEquals(8, metadata.size(),
                "Metadata should have 8 entries, got: " + metadata.keySet());

        List<String> expectedKeys = List.of(
                "Class", "Package", "Module", "Sealed", "Record",
                "Interface", "Public Methods", "Declared Fields");

        for (String key : expectedKeys) {
            assertTrue(metadata.containsKey(key), "Missing key: " + key);
        }
    }

    // =========================================================================
    // 4. BlueOceanLayer composite methods don't throw
    // =========================================================================

    @Test
    void documentClassProfile_doesNotThrowForRealClass() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_ClassProfile");

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentClassProfile(renderMachine, String.class),
                "documentClassProfile must not throw for String.class");
    }

    @Test
    void documentClassProfile_doesNotThrowForInterface() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_InterfaceProfile");

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentClassProfile(renderMachine, Runnable.class),
                "documentClassProfile must not throw for Runnable.class");
    }

    @Test
    void documentPerformanceProfile_doesNotThrowForRealTasks() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_PerfProfile");

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentPerformanceProfile(
                                renderMachine,
                                "String Operations",
                                new String[]{"concat", "builder"},
                                new Runnable[]{
                                        () -> "hello" + " world",
                                        () -> new StringBuilder("hello").append(" world").toString()
                                }),
                "documentPerformanceProfile must not throw");
    }

    @Test
    void documentSystemLandscape_doesNotThrow() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_SystemLandscape");

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentSystemLandscape(renderMachine),
                "documentSystemLandscape must not throw");
    }

    @Test
    void documentContractCompliance_doesNotThrow() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_Contract");

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentContractCompliance(
                                renderMachine, CharSequence.class, String.class),
                "documentContractCompliance must not throw");
    }

    @Test
    void documentRecordProfile_doesNotThrow() {
        record TestConfig(String host, int port, boolean ssl) {}

        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_RecordProfile");

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentRecordProfile(
                                renderMachine,
                                TestConfig.class,
                                new TestConfig("localhost", 8080, true)),
                "documentRecordProfile must not throw");
    }

    @Test
    void documentErrorPattern_doesNotThrow() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("BlueOceanTest_ErrorPattern");

        var rootCause = new NullPointerException("null value");
        var wrappedCause = new IllegalArgumentException("bad input", rootCause);
        var topLevel = new IllegalStateException("operation failed", wrappedCause);

        assertDoesNotThrow(() ->
                        BlueOceanLayer.documentErrorPattern(renderMachine, topLevel),
                "documentErrorPattern must not throw");
    }

    // =========================================================================
    // 5. End-to-end pipeline: Utils -> BlueOceanLayer -> RenderMachine -> output
    // =========================================================================

    @Test
    void endToEnd_fullPipelineProducesNonEmptyMarkdown() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("Vision2030_E2E_Test");

        // Step 1: Use Vision2030Utils to gather data
        Map<String, String> fingerprint = Vision2030Utils.systemFingerprint();
        Map<String, String> metadata = Vision2030Utils.classMetadata(String.class);

        // Verify utils produced valid data
        assertFalse(fingerprint.isEmpty());
        assertFalse(metadata.isEmpty());

        // Step 2: Feed data through RenderMachine directly
        renderMachine.sayNextSection("System Fingerprint");
        renderMachine.sayKeyValue(fingerprint);
        renderMachine.sayNextSection("Class Metadata: String");
        renderMachine.sayKeyValue(metadata);

        // Step 3: Use BlueOceanLayer composite methods
        BlueOceanLayer.documentClassProfile(renderMachine, String.class);

        record PipelineRecord(String stage, String status) {}
        BlueOceanLayer.documentRecordProfile(
                renderMachine,
                PipelineRecord.class,
                new PipelineRecord("integration", "passing"));

        // Step 4: Verify the render machine accumulated content
        assertFalse(renderMachine.markdownDocument.isEmpty(),
                "RenderMachine must have accumulated markdown content");

        // Verify sections were created
        assertFalse(renderMachine.sections.isEmpty(),
                "RenderMachine must have tracked sections");

        // Verify TOC entries were generated
        assertFalse(renderMachine.toc.isEmpty(),
                "RenderMachine must have generated TOC entries");

        // Step 5: Verify finishAndWriteOut produces files without error
        assertDoesNotThrow(() -> renderMachine.finishAndWriteOut(),
                "finishAndWriteOut must not throw");
    }

    @Test
    void endToEnd_benchmarkComparisonIntegratesWithRenderMachine() {
        var renderMachine = new RenderMachineImpl();
        renderMachine.setFileName("Vision2030_Benchmark_E2E");

        // Generate benchmark table via utils
        String[][] table = Vision2030Utils.benchmarkComparison(
                "ArrayList.add", () -> new java.util.ArrayList<>().add("item"),
                "LinkedList.add", () -> new java.util.LinkedList<>().add("item"),
                5, 20
        );

        // Feed into render machine
        renderMachine.sayNextSection("Collection Benchmark");
        renderMachine.say("Comparing ArrayList vs LinkedList add performance.");
        renderMachine.sayTable(table);
        renderMachine.sayKeyValue(Vision2030Utils.systemFingerprint());

        // Verify content was accumulated
        assertTrue(renderMachine.markdownDocument.size() > 5,
                "Should have generated multiple markdown lines");

        // Verify the table data made it into the document
        String fullDoc = String.join("\n", renderMachine.markdownDocument);
        assertTrue(fullDoc.contains("ArrayList.add"),
                "Document should contain benchmark label 'ArrayList.add'");
        assertTrue(fullDoc.contains("LinkedList.add"),
                "Document should contain benchmark label 'LinkedList.add'");
        assertTrue(fullDoc.contains("Avg (ns)"),
                "Document should contain metric label 'Avg (ns)'");
    }
}
