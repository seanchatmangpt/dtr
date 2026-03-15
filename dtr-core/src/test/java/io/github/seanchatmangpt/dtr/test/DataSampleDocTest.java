/*
 * Copyright 2026 the original author or authors.
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
import io.github.seanchatmangpt.dtr.datasample.DataSampleAnalyzer;
import io.github.seanchatmangpt.dtr.datasample.DataSampleAnalyzer.SampleResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * Documentation test for {@link DataSampleAnalyzer}.
 *
 * <p>Demonstrates the {@code sayDataSample} innovation: given a {@code List} of
 * Java records, {@code DataSampleAnalyzer} uses reflection to infer the field
 * schema, extract a configurable-size sample, and compute per-field statistics
 * (null count, unique count, total count) — all without requiring any user-supplied
 * schema descriptor.</p>
 *
 * <p>Output tables are forwarded directly to {@code sayTable()}, making the
 * analysis immediately visible in generated documentation.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DataSampleDocTest extends DtrTest {

    // -------------------------------------------------------------------------
    // Domain model used throughout this test class
    // -------------------------------------------------------------------------

    /**
     * A minimal e-commerce product record used to demonstrate schema inference.
     * The {@code stock} component is intentionally {@code Integer} (nullable) so
     * that null-count statistics show real non-zero values in the output.
     */
    record Product(String name, String category, double price, Integer stock) {}

    // -------------------------------------------------------------------------
    // Shared fixture — 12 products, 3 with null stock
    // -------------------------------------------------------------------------

    private static final List<Product> PRODUCTS = List.of(
        new Product("Espresso Maker",    "Kitchen",     89.99,  42),
        new Product("Yoga Mat",          "Sports",      34.50,  120),
        new Product("Bluetooth Speaker", "Electronics", 59.95,  null),
        new Product("Cast Iron Pan",     "Kitchen",     45.00,  67),
        new Product("Running Shoes",     "Sports",      110.00, 30),
        new Product("USB-C Hub",         "Electronics", 29.99,  null),
        new Product("French Press",      "Kitchen",     24.75,  88),
        new Product("Resistance Bands",  "Sports",      18.00,  200),
        new Product("Noise Headphones",  "Electronics", 149.00, 15),
        new Product("Stand Mixer",       "Kitchen",     299.00, null),
        new Product("Hiking Boots",      "Sports",      175.00, 22),
        new Product("Smart Plug",        "Electronics", 14.99,  310)
    );

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Section 1 — Overview
    // =========================================================================

    @Test
    void s1_overview() {
        sayNextSection("DataSampleAnalyzer — Reflection-Based Data Profiling");

        say("""
            `DataSampleAnalyzer` accepts any `List<?>` and produces a `SampleResult` \
            containing three things:

            1. A **sample table** — the first N rows as strings, ready for `sayTable()`.
            2. A **stats table** — per-field null count, unique count, and total count.
            3. The raw `SampleResult` record — usable programmatically for assertions.

            Schema inference requires no annotation, no descriptor, and no code \
            generation. The field list is derived from `Class.getDeclaredFields()` on \
            the first non-null element; `setAccessible(true)` is called so that private \
            and package-private fields are included.""");

        sayNote("""
            The analysis always covers the full list for statistics, even when \
            `maxSampleRows` limits the displayed sample. Null counts therefore reflect \
            the entire data set, not just the visible rows.""");
    }

    // =========================================================================
    // Section 2 — Sample table
    // =========================================================================

    @Test
    void s2_sample_table() {
        sayNextSection("Sample Rows (first 5 of 12)");

        say("""
            Calling `DataSampleAnalyzer.analyze(products, 5)` with a list of 12 \
            `Product` records returns a `SampleResult` where `sampledRows` is 5 \
            and `totalRows` is 12. The sample table below shows the header row \
            followed by those 5 data rows.""");

        sayCode("""
                record Product(String name, String category, double price, Integer stock) {}

                var result = DataSampleAnalyzer.analyze(products, 5);
                sayTable(DataSampleAnalyzer.toSampleTable(result));
                """, "java");

        var result = DataSampleAnalyzer.analyze(PRODUCTS, 5);
        sayTable(DataSampleAnalyzer.toSampleTable(result));

        say("Each cell is the result of `Object.toString()` on the field value, " +
            "or the literal string `null` when the field value is `null`. " +
            "If field access fails for any reason (e.g., a security manager restriction), " +
            "the cell shows `N/A` instead.");
    }

    // =========================================================================
    // Section 3 — Stats table
    // =========================================================================

    @Test
    void s3_stats_table() {
        sayNextSection("Field Statistics (full data set, 12 rows)");

        say("""
            `toStatsTable(result)` converts the `List<FieldStats>` inside the \
            `SampleResult` into a `String[][]` with five columns: \
            **Field**, **Type**, **Nulls**, **Uniques**, **Total**. \
            Statistics are computed over all 12 rows, not just the 5-row sample.""");

        sayCode("""
                var result = DataSampleAnalyzer.analyze(products, 5);
                sayTable(DataSampleAnalyzer.toStatsTable(result));
                """, "java");

        var result = DataSampleAnalyzer.analyze(PRODUCTS, 5);
        sayTable(DataSampleAnalyzer.toStatsTable(result));

        sayNote("""
            The `stock` field has `nullCount = 3` because three `Product` entries \
            were constructed with `null` stock. The `price` field has \
            `uniqueCount = 12` because every price in the fixture is distinct. \
            The `category` field has `uniqueCount = 3` (Kitchen, Sports, Electronics).""");
    }

    // =========================================================================
    // Section 4 — SampleResult record schema
    // =========================================================================

    @Test
    void s4_sample_result_schema() {
        sayNextSection("SampleResult Record Schema");

        say("""
            Both result types are plain Java 26 records, making them transparent \
            and directly usable in assertions without casting or helper methods.""");

        sayRecordComponents(SampleResult.class);

        say("The `sampleRows` list always starts with the header row at index 0, " +
            "so `sampleRows.size() == sampledRows + 1`. " +
            "Field ordering in `fieldNames` and `stats` matches the JVM's " +
            "declaration order for the analyzed class.");
    }

    // =========================================================================
    // Section 5 — Assertions
    // =========================================================================

    @Test
    void s5_assertions() {
        sayNextSection("Programmatic Assertions on SampleResult");

        say("""
            Because `SampleResult` and `FieldStats` are records, their components \
            are directly accessible in Hamcrest assertions. The following assertions \
            validate the fixture-level expectations for the 12-product list.""");

        var result = DataSampleAnalyzer.analyze(PRODUCTS, 5);

        sayAndAssertThat("totalRows",   result.totalRows(),   org.hamcrest.Matchers.equalTo(12));
        sayAndAssertThat("sampledRows", result.sampledRows(), org.hamcrest.Matchers.equalTo(5));
        sayAndAssertThat("field count", result.fieldNames().size(), org.hamcrest.Matchers.equalTo(4));

        // Verify null count for the 'stock' field (index 3, declaration order)
        var stockStats = result.stats().stream()
            .filter(s -> s.fieldName().equals("stock"))
            .findFirst()
            .orElseThrow();

        sayAndAssertThat("stock nullCount",  stockStats.nullCount(),   org.hamcrest.Matchers.equalTo(3L));
        sayAndAssertThat("stock totalCount", stockStats.totalCount(),  org.hamcrest.Matchers.equalTo(12L));

        // Verify category unique count
        var categoryStats = result.stats().stream()
            .filter(s -> s.fieldName().equals("category"))
            .findFirst()
            .orElseThrow();

        sayAndAssertThat("category uniqueCount", categoryStats.uniqueCount(), org.hamcrest.Matchers.equalTo(3L));

        sayNote("All assertions above are live — they run as part of the test " +
                "and will fail the build if the analyzer's output changes unexpectedly.");
    }

    // =========================================================================
    // Section 6 — Edge cases
    // =========================================================================

    @Test
    void s6_edge_cases() {
        sayNextSection("Edge Cases and Graceful Degradation");

        say("""
            Two edge cases worth documenting explicitly:

            - **maxSampleRows > list size** — when the requested sample cap exceeds \
              the number of rows, `sampledRows` is clamped to `totalRows`. \
              No `IndexOutOfBoundsException` is thrown.
            - **All-null list** — passing a list where every element is `null` \
              triggers `IllegalArgumentException` with a clear message, because \
              schema inference requires at least one non-null element.""");

        // Edge case 1: cap larger than list
        var small = List.of(
            new Product("Widget A", "Misc", 9.99, 1),
            new Product("Widget B", "Misc", 4.99, 2)
        );
        var clampedResult = DataSampleAnalyzer.analyze(small, 100);
        sayAndAssertThat("clamped sampledRows", clampedResult.sampledRows(), org.hamcrest.Matchers.equalTo(2));

        sayTable(DataSampleAnalyzer.toSampleTable(clampedResult));

        // Edge case 2: all-null list raises a clear exception
        sayCode("""
                // Attempting to analyze a list of only nulls:
                DataSampleAnalyzer.analyze(List.of(null, null), 5);
                // → throws IllegalArgumentException:
                //     "data list contains no non-null elements — cannot infer schema"
                """, "java");

        boolean caughtExpected = false;
        try {
            //noinspection unchecked,rawtypes
            DataSampleAnalyzer.analyze((List) java.util.Arrays.asList(null, null), 5);
        } catch (IllegalArgumentException e) {
            caughtExpected = true;
        }
        sayAndAssertThat("all-null list throws IllegalArgumentException", caughtExpected,
            org.hamcrest.Matchers.is(true));

        sayNote("The `maxSampleRows = 0` case is also valid: it produces an empty " +
                "sample table (header row only) while still computing full statistics.");
    }
}
