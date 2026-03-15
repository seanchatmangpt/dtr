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
package io.github.seanchatmangpt.dtr.datasample;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Analyzes a {@code List} of arbitrary objects using reflection to infer field schema,
 * extract a data sample, and compute basic column-level statistics.
 *
 * <p>All field access uses {@code setAccessible(true)}; if a field is inaccessible
 * for any reason, the cell value is reported as {@code "N/A"} and does not propagate
 * an exception to the caller.</p>
 *
 * <p>Typical usage inside a DTR doc-test:</p>
 * <pre>{@code
 * var result = DataSampleAnalyzer.analyze(products, 5);
 * sayTable(DataSampleAnalyzer.toSampleTable(result));
 * sayTable(DataSampleAnalyzer.toStatsTable(result));
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class DataSampleAnalyzer {

    private DataSampleAnalyzer() {}

    // -------------------------------------------------------------------------
    // Public result types
    // -------------------------------------------------------------------------

    /**
     * Per-field statistics computed across the full data set (not just the sample).
     *
     * @param fieldName   the declared field name
     * @param fieldType   the simple type name (e.g. {@code "String"}, {@code "double"})
     * @param nullCount   number of rows where the field value was {@code null}
     * @param uniqueCount number of distinct non-null string representations
     * @param totalCount  total number of rows examined
     */
    public record FieldStats(
            String fieldName,
            String fieldType,
            long nullCount,
            long uniqueCount,
            long totalCount) {}

    /**
     * The combined result of a {@link #analyze} call.
     *
     * @param totalRows   total rows in the source list
     * @param sampledRows number of rows included in {@code sampleRows} (excluding the header)
     * @param fieldNames  ordered list of field names discovered via reflection
     * @param sampleRows  rows of stringified values; the first entry is the header row
     * @param stats       one {@link FieldStats} entry per field, in declaration order
     */
    public record SampleResult(
            int totalRows,
            int sampledRows,
            List<String> fieldNames,
            List<String[]> sampleRows,
            List<FieldStats> stats) {}

    // -------------------------------------------------------------------------
    // Core analysis
    // -------------------------------------------------------------------------

    /**
     * Analyzes {@code data} using reflection and returns a {@link SampleResult}.
     *
     * <p>Schema inference is performed from the first non-null element's class.
     * All declared fields (including private ones) are included. Fields are ordered
     * in JVM declaration order as returned by {@link Class#getDeclaredFields()}.</p>
     *
     * @param data          the list to analyze; must not be {@code null}
     * @param maxSampleRows maximum number of data rows to include in the sample
     *                      (header row is not counted)
     * @return a {@link SampleResult} containing schema, sample, and statistics
     * @throws IllegalArgumentException if {@code data} is empty or contains only nulls
     */
    public static SampleResult analyze(List<?> data, int maxSampleRows) {
        Objects.requireNonNull(data, "data must not be null");

        // Find the first non-null element to infer the schema
        Object prototype = data.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "data list contains no non-null elements — cannot infer schema"));

        Field[] declaredFields = prototype.getClass().getDeclaredFields();

        // Make all fields accessible up-front; ignore fields that resist
        List<Field> accessibleFields = new ArrayList<>(declaredFields.length);
        for (var f : declaredFields) {
            try {
                f.setAccessible(true);
                accessibleFields.add(f);
            } catch (Exception ignored) {
                // Inaccessible fields are omitted from the schema
            }
        }

        var fieldNames = accessibleFields.stream()
                .map(Field::getName)
                .toList();

        int totalRows  = data.size();
        int sampleCap  = Math.min(maxSampleRows, totalRows);

        // Build sample rows (header + data rows)
        List<String[]> sampleRows = new ArrayList<>(sampleCap + 1);
        sampleRows.add(fieldNames.toArray(String[]::new));   // header

        for (int rowIdx = 0; rowIdx < sampleCap; rowIdx++) {
            Object obj = data.get(rowIdx);
            String[] row = new String[accessibleFields.size()];
            for (int col = 0; col < accessibleFields.size(); col++) {
                row[col] = extractValue(accessibleFields.get(col), obj);
            }
            sampleRows.add(row);
        }

        // Compute statistics across the full data set
        List<FieldStats> stats = new ArrayList<>(accessibleFields.size());
        for (var field : accessibleFields) {
            long nullCount   = 0L;
            Set<String> seen = new HashSet<>();
            for (var obj : data) {
                if (obj == null) {
                    nullCount++;
                    continue;
                }
                String val = extractValue(field, obj);
                if ("null".equals(val) || "N/A".equals(val)) {
                    // treat explicit null values as null for statistics purposes
                    try {
                        Object raw = field.get(obj);
                        if (raw == null) nullCount++;
                        else seen.add(val);
                    } catch (Exception ignored) {
                        nullCount++;
                    }
                } else {
                    seen.add(val);
                }
            }
            stats.add(new FieldStats(
                    field.getName(),
                    field.getType().getSimpleName(),
                    nullCount,
                    (long) seen.size(),
                    (long) totalRows));
        }

        return new SampleResult(totalRows, sampleCap, fieldNames, sampleRows, stats);
    }

    // -------------------------------------------------------------------------
    // Table helpers for sayTable()
    // -------------------------------------------------------------------------

    /**
     * Converts the sample portion of a {@link SampleResult} into a {@code String[][]}
     * suitable for {@code sayTable()}. The first row of the returned array is the
     * header row derived from field names.
     *
     * @param result the result produced by {@link #analyze}
     * @return a 2-D array with header and up to {@code sampledRows} data rows
     */
    public static String[][] toSampleTable(SampleResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<String[]> rows = result.sampleRows();
        return rows.toArray(String[][]::new);
    }

    /**
     * Converts the statistics portion of a {@link SampleResult} into a {@code String[][]}
     * suitable for {@code sayTable()}. Columns are: Field, Type, Nulls, Uniques, Total.
     *
     * @param result the result produced by {@link #analyze}
     * @return a 2-D array with a header row followed by one row per field
     */
    public static String[][] toStatsTable(SampleResult result) {
        Objects.requireNonNull(result, "result must not be null");
        List<FieldStats> stats = result.stats();
        String[][] table = new String[stats.size() + 1][5];
        table[0] = new String[]{"Field", "Type", "Nulls", "Uniques", "Total"};
        for (int i = 0; i < stats.size(); i++) {
            var s = stats.get(i);
            table[i + 1] = new String[]{
                s.fieldName(),
                s.fieldType(),
                String.valueOf(s.nullCount()),
                String.valueOf(s.uniqueCount()),
                String.valueOf(s.totalCount())
            };
        }
        return table;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Reads a single field value from an object, returning {@code "N/A"} if the
     * field cannot be accessed for any reason.
     */
    private static String extractValue(Field field, Object obj) {
        if (obj == null) return "N/A";
        try {
            Object val = field.get(obj);
            return val == null ? "null" : val.toString();
        } catch (Exception ignored) {
            return "N/A";
        }
    }
}
