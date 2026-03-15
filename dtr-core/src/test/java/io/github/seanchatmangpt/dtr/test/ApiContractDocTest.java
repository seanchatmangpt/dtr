/*
 * Copyright (C) 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.apicontract.ApiContractExtractor;
import io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Documentation test for the {@link ApiContractExtractor} feature.
 *
 * <p>Demonstrates reflective API contract extraction: how DTR can automatically
 * discover and document all public methods of any class at test time, producing
 * structured tables that cannot drift from the implementation because they are
 * derived directly from bytecode.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ApiContractDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // t01 — Overview
    // =========================================================================

    @Test
    public void t01_overview() {
        sayNextSection("API Contract Documentation");

        say("""
                DTR's API contract feature uses Java reflection to extract the complete \
                public surface of any class at test time. Rather than writing documentation \
                that describes what methods exist, the documentation *is* the method list — \
                derived directly from the compiled bytecode so it can never drift from the \
                actual implementation.""");

        say("""
                `ApiContractExtractor.extract(Class<?>)` inspects every public method \
                declared in the target class, collects parameter types, return types, and \
                annotations, then returns an immutable `ApiContractResult` record. \
                Two rendering helpers convert the result into either a `String[][]` table \
                (for `sayTable()`) or a list of raw Markdown lines (for `sayRaw()` / \
                `sayApiContract()`).""");

        sayCode("""
                // Extract and render in one step via the DtrTest helper
                sayApiContract(BenchmarkComparison.class);

                // Or extract manually for custom rendering
                var result = ApiContractExtractor.extract(BenchmarkComparison.class);
                sayTable(ApiContractExtractor.toTable(result));
                """, "java");

        sayNote("""
                Synthetic and bridge methods are excluded automatically. \
                Per-method reflection errors are caught and surfaced as error rows \
                rather than aborting the entire extraction.""");
    }

    // =========================================================================
    // t02 — sayApiContract integration
    // =========================================================================

    @Test
    public void t02_extractBenchmarkComparison() {
        sayNextSection("sayApiContract — BenchmarkComparison");

        say("""
                The following table was produced by `sayApiContract(BenchmarkComparison.class)`. \
                It lists every public method declared in `BenchmarkComparison` together with its \
                parameter types, return type, and any annotations.""");

        Exception caught = null;
        try {
            sayApiContract(BenchmarkComparison.class);
        } catch (Exception e) {
            caught = e;
        }

        Assertions.assertNull(caught,
                "sayApiContract(BenchmarkComparison.class) must not throw an exception");
    }

    // =========================================================================
    // t03 — toTable rendering
    // =========================================================================

    @Test
    public void t03_tableRendering() {
        sayNextSection("toTable — BenchmarkComparison");

        say("""
                `ApiContractExtractor.toTable()` converts an `ApiContractResult` into a \
                `String[][]` that can be passed directly to `sayTable()`. The first row \
                is always the header `[Method, Parameters, Returns, Annotations]`; \
                subsequent rows correspond to public methods sorted alphabetically.""");

        var result = ApiContractExtractor.extract(BenchmarkComparison.class);
        var table  = ApiContractExtractor.toTable(result);

        sayTable(table);

        // Header row + at least one method row
        Assertions.assertTrue(table.length >= 2,
                "Table must have at least 2 rows (header + at least one method)");

        sayNote("""
                The header row is always `[\"Method\", \"Parameters\", \"Returns\", \"Annotations\"]`. \
                Rows are ordered by method name so the table is deterministic across JVM runs.""");
    }

    // =========================================================================
    // t04 — Self-extraction (this test class)
    // =========================================================================

    @Test
    public void t04_methodDetails() {
        sayNextSection("Method Details — ApiContractDocTest");

        say("""
                DTR can document any class — including itself. The following table \
                was produced by extracting this test class (`ApiContractDocTest`). \
                Every public test method declared here appears as a row, demonstrating \
                that the extractor works on JUnit test classes as well as production code.""");

        var result = ApiContractExtractor.extract(ApiContractDocTest.class);
        sayTable(ApiContractExtractor.toTable(result));

        Assertions.assertFalse(result.methods().isEmpty(),
                "ApiContractDocTest must expose at least one public method");

        sayNote("""
                The `@Test` and `@AfterAll` annotations are captured in the Annotations \
                column, turning the contract table into a lightweight test-plan summary.""");
    }

    // =========================================================================
    // t05 — Annotation detection on java.util.List
    // =========================================================================

    @Test
    public void t05_annotationDetection() {
        sayNextSection("Annotation Detection — java.util.List");

        say("""
                Interface types are supported as well as concrete classes. \
                `java.util.List` is a canonical example: it declares many abstract \
                methods, and some carry annotations such as `@Override` from \
                default implementations in sub-interfaces.""");

        var result = ApiContractExtractor.extract(java.util.List.class);
        sayTable(ApiContractExtractor.toTable(result));

        sayNote("""
                `java.util.List` is an interface, so all declared methods are implicitly \
                public. Methods inherited from `Collection` or `Iterable` are not included \
                because `getDeclaredMethods()` returns only the methods declared directly \
                in `List` itself — not those inherited from super-types.""");

        Assertions.assertTrue(result.className().contains("List"),
                "Extracted class name must contain 'List'");
    }
}
