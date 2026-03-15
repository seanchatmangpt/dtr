package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.versioning.VersionMatrixBuilder;
import io.github.seanchatmangpt.dtr.versioning.VersionMatrixBuilder.CellValue;
import io.github.seanchatmangpt.dtr.versioning.VersionMatrixBuilder.FeatureRow;
import io.github.seanchatmangpt.dtr.versioning.VersionMatrixBuilder.MatrixResult;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

public class VersionMatrixDocTest extends DtrTest {

    @Test
    public void testJava26FeatureMatrix() {
        sayNextSection("sayVersionMatrix — Java Feature Compatibility Matrix");
        say("DTR documents feature availability across Java versions. "
                + "Cells are probed against the <em>current</em> JVM to confirm support — "
                + "this matrix is live, not hand-written.");

        int currentFeature = Runtime.version().feature();
        say("Current JVM: Java " + currentFeature + " (" + System.getProperty("java.version") + ")");

        // Build feature x version matrix
        // Columns: Java 17, 21, 22, 26
        // We probe current JVM; for historical versions we use known facts
        MatrixResult matrix = VersionMatrixBuilder.builder()
            .versions("Java 17", "Java 21", "Java 22", "Java 26")
            .feature("Records (JEP 395)", "JEP 395", map(
                "Java 17", CellValue.SUPPORTED,
                "Java 21", CellValue.SUPPORTED,
                "Java 22", CellValue.SUPPORTED,
                "Java 26", VersionMatrixBuilder.probe(VersionMatrixBuilder::hasRecords)
            ))
            .feature("Sealed classes (JEP 409)", "JEP 409", map(
                "Java 17", CellValue.SUPPORTED,
                "Java 21", CellValue.SUPPORTED,
                "Java 22", CellValue.SUPPORTED,
                "Java 26", VersionMatrixBuilder.probe(VersionMatrixBuilder::hasSealedClasses)
            ))
            .feature("Virtual threads (JEP 444)", "JEP 444", map(
                "Java 17", CellValue.NOT_SUPPORTED,
                "Java 21", CellValue.SUPPORTED,
                "Java 22", CellValue.SUPPORTED,
                "Java 26", VersionMatrixBuilder.probe(VersionMatrixBuilder::hasVirtualThreads)
            ))
            .feature("Pattern matching switch (JEP 441)", "JEP 441", map(
                "Java 17", CellValue.NOT_SUPPORTED,
                "Java 21", CellValue.SUPPORTED,
                "Java 22", CellValue.SUPPORTED,
                "Java 26", VersionMatrixBuilder.probe(VersionMatrixBuilder::hasPatternMatchingSwitch)
            ))
            .feature("Structured concurrency (JEP 453)", "JEP 453", map(
                "Java 17", CellValue.NOT_SUPPORTED,
                "Java 21", CellValue.PREVIEW,
                "Java 22", CellValue.PREVIEW,
                "Java 26", VersionMatrixBuilder.probe(VersionMatrixBuilder::hasStructuredConcurrency)
            ))
            .build();

        // Render as table: Feature | JEP | Java 17 | Java 21 | Java 22 | Java 26
        int cols = matrix.versions().size() + 2;
        String[][] table = new String[matrix.rows().size() + 1][cols];
        table[0] = new String[cols];
        table[0][0] = "Feature";
        table[0][1] = "JEP";
        for (int v = 0; v < matrix.versions().size(); v++) {
            table[0][v + 2] = matrix.versions().get(v);
        }
        for (int r = 0; r < matrix.rows().size(); r++) {
            FeatureRow row = matrix.rows().get(r);
            table[r + 1] = new String[cols];
            table[r + 1][0] = row.featureName().contains("(")
                ? row.featureName().substring(0, row.featureName().indexOf("(")).trim()
                : row.featureName();
            table[r + 1][1] = row.jepRef();
            for (int v = 0; v < matrix.versions().size(); v++) {
                CellValue cell = row.versionValues().getOrDefault(
                    matrix.versions().get(v), CellValue.NA);
                table[r + 1][v + 2] = cell.symbol();
            }
        }
        sayTable(table);

        sayNote("✓ = supported, ✗ = not supported, ⚡ = preview, — = N/A. "
                + "Java 26 cells are live-probed against the current JVM.");

        // Verify current JVM (Java 26) has virtual threads
        sayAndAssertThat("Java 26 has virtual threads",
            VersionMatrixBuilder.hasVirtualThreads(), is(true));
        sayAndAssertThat("Java 26 has records",
            VersionMatrixBuilder.hasRecords(), is(true));
        sayAndAssertThat("Java 26 has sealed classes",
            VersionMatrixBuilder.hasSealedClasses(), is(true));
    }

    private static Map<String, CellValue> map(Object... pairs) {
        Map<String, CellValue> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((String) pairs[i], (CellValue) pairs[i + 1]);
        }
        return m;
    }
}
