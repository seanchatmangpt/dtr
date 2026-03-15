package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.comparison.AlternativesComparer;
import io.github.seanchatmangpt.dtr.comparison.AlternativesComparer.Alternative;
import io.github.seanchatmangpt.dtr.comparison.AlternativesComparer.AlternativeResult;
import io.github.seanchatmangpt.dtr.comparison.AlternativesComparer.ComparisonResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

/**
 * DTR doc-test for {@code AlternativesComparer} — side-by-side implementation comparison.
 *
 * <p>Each test method runs real implementations, measures them with {@code System.nanoTime()},
 * and renders a comparison table. The documentation is the measurement — no hard-coded numbers.</p>
 */
public class AlternativesDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    @Test
    public void testStringConcatenationAlternatives() {
        sayNextSection("sayAlternatives — Side-by-Side Implementation Comparison");
        say("DTR can document performance trade-offs between alternative implementations "
                + "of the same operation. The comparison is live-measured — not claimed.");

        var result = AlternativesComparer.compare(List.of(
            new Alternative("String + (naive concat)", () -> {
                String s = "";
                for (int i = 0; i < 100; i++) s = s + "x";
            }),
            new Alternative("StringBuilder", () -> {
                var sb = new StringBuilder();
                for (int i = 0; i < 100; i++) sb.append("x");
                sb.toString();
            }),
            new Alternative("String.repeat()", () -> {
                "x".repeat(100);
            })
        ));

        renderComparisonTable(result);

        say("Fastest: **" + result.fastestName() + "**");
        sayNote("Results measured on " + Runtime.getRuntime().availableProcessors()
                + " CPU cores, Java " + System.getProperty("java.version"));
    }

    @Test
    public void testMapLookupAlternatives() {
        sayNextSection("sayAlternatives — HashMap vs TreeMap");
        say("Choosing the right Map implementation matters for performance. "
                + "DTR documents the measured difference.");

        int size = 1_000;
        var hashMap = new java.util.HashMap<String, Integer>(size * 2);
        var treeMap = new java.util.TreeMap<String, Integer>();
        for (int i = 0; i < size; i++) {
            hashMap.put("key" + i, i);
            treeMap.put("key" + i, i);
        }
        String lookupKey = "key" + (size / 2);

        var result = AlternativesComparer.compare(List.of(
            new Alternative("HashMap.get()", () -> hashMap.get(lookupKey)),
            new Alternative("TreeMap.get()",  () -> treeMap.get(lookupKey))
        ));

        renderComparisonTable(result);

        say("Fastest: **" + result.fastestName() + "**");
        sayNote("Results measured on " + Runtime.getRuntime().availableProcessors()
                + " CPU cores, Java " + System.getProperty("java.version"));
    }

    /** Renders an AlternativesComparer result as a sayTable comparison table. */
    private void renderComparisonTable(ComparisonResult result) {
        var rows = result.results();
        String[][] table = new String[rows.size() + 1][4];
        table[0] = new String[]{"Implementation", "Avg (ns)", "Relative", "Rank"};
        for (int i = 0; i < rows.size(); i++) {
            AlternativeResult r = rows.get(i);
            table[i + 1] = new String[]{
                r.name(),
                String.valueOf(r.avgNanos()),
                String.format("%.2fx", r.relativeToFastest()),
                r.rank()
            };
        }
        sayTable(table);
    }
}
