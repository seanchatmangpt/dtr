package io.github.seanchatmangpt.dtr.versioning;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Builds a feature x version compatibility matrix.
 *
 * <p>Each cell shows:
 * <ul>
 *   <li>✓ — supported (confirmed by live probe)</li>
 *   <li>✗ — not supported</li>
 *   <li>⚡ — preview / experimental</li>
 *   <li>— — not applicable</li>
 * </ul>
 */
public final class VersionMatrixBuilder {

    public enum CellValue {
        SUPPORTED("✓"),
        NOT_SUPPORTED("✗"),
        PREVIEW("⚡"),
        NA("—");

        private final String symbol;
        CellValue(String symbol) { this.symbol = symbol; }
        public String symbol() { return symbol; }
    }

    public record FeatureRow(
        String featureName,
        String jepRef,        // e.g. "JEP 444" or ""
        Map<String, CellValue> versionValues  // version label -> cell value
    ) {}

    public record MatrixResult(
        List<String> versions,
        List<FeatureRow> rows
    ) {}

    public static final class Builder {
        private final List<String> versions = new ArrayList<>();
        private final List<FeatureRow> rows = new ArrayList<>();

        public Builder versions(String... v) {
            versions.addAll(List.of(v));
            return this;
        }

        public Builder feature(String name, String jepRef, Map<String, CellValue> cells) {
            rows.add(new FeatureRow(name, jepRef, new LinkedHashMap<>(cells)));
            return this;
        }

        public MatrixResult build() {
            return new MatrixResult(new ArrayList<>(versions), new ArrayList<>(rows));
        }
    }

    public static Builder builder() { return new Builder(); }

    // -- Probes for current JVM ---------------------------------------------------

    /** True if virtual threads are available (Java 21+). */
    public static boolean hasVirtualThreads() {
        try {
            Thread.class.getMethod("ofVirtual");
            return true;
        } catch (NoSuchMethodException e) { return false; }
    }

    /** True if records are available (Java 16+). */
    public static boolean hasRecords() {
        try {
            Class.class.getMethod("isRecord");
            return true;
        } catch (NoSuchMethodException e) { return false; }
    }

    /** True if sealed classes are available (Java 17+). */
    public static boolean hasSealedClasses() {
        try {
            Class.class.getMethod("isSealed");
            return true;
        } catch (NoSuchMethodException e) { return false; }
    }

    /** True if pattern matching for switch is available (Java 21+). */
    public static boolean hasPatternMatchingSwitch() {
        try {
            return Runtime.version().feature() >= 21;
        } catch (Exception e) { return false; }
    }

    /** True if structured concurrency is available (Java 21+). */
    public static boolean hasStructuredConcurrency() {
        try {
            Class.forName("java.util.concurrent.StructuredTaskScope");
            return true;
        } catch (ClassNotFoundException e) { return false; }
    }

    /** Returns CellValue.SUPPORTED if probe returns true, else NOT_SUPPORTED. */
    public static CellValue probe(BooleanSupplier probe) {
        try {
            return probe.getAsBoolean() ? CellValue.SUPPORTED : CellValue.NOT_SUPPORTED;
        } catch (Exception e) {
            return CellValue.NOT_SUPPORTED;
        }
    }
}
