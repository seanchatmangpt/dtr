package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.complexity.TimeComplexityAnalyzer;
import io.github.seanchatmangpt.dtr.complexity.TimeComplexityAnalyzer.ComplexityResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;

/**
 * DTR doc test demonstrating {@code sayTimeComplexity} — empirical Big-O analysis.
 *
 * <p>Each test method runs an algorithm at increasing input sizes (n = 10 … 100,000),
 * measures average nanoseconds per iteration, and infers the complexity class from
 * the log-log growth ratio. Results are rendered as a measurement table plus the
 * inferred Big-O label.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TimeComplexityDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    @Test
    void testLinearSearch() {
        sayNextSection("sayTimeComplexity — Empirical Algorithm Complexity Analysis");
        say("DTR can <em>prove</em> algorithmic complexity by measuring real runtime "
                + "at increasing input sizes and inferring the complexity class. "
                + "No other documentation tool runs the algorithm — DTR does.");

        ComplexityResult result = TimeComplexityAnalyzer.analyze(
            "Linear search in array",
            n -> () -> {
                int[] arr = new int[n];
                for (int i = 0; i < n; i++) arr[i] = i;
                int target = n - 1;
                for (int x : arr) {
                    if (x == target) break;
                }
            }
        );

        // Build table: n → avg nanoseconds → relative
        var measurements = result.measurements();
        String[][] table = new String[measurements.length + 1][3];
        table[0] = new String[]{"Input size (n)", "Avg ns", "Relative"};
        long base = measurements[0].nanosAvg();
        for (int i = 0; i < measurements.length; i++) {
            var m = measurements[i];
            table[i + 1] = new String[]{
                String.valueOf(m.n()),
                String.valueOf(m.nanosAvg()),
                "%.1fx".formatted((double) m.nanosAvg() / base)
            };
        }
        sayTable(table);

        say("Inferred complexity: <strong>" + result.inferredClass() + "</strong> "
                + "(based on runtime growth from n=" + measurements[0].n()
                + " to n=" + measurements[measurements.length - 1].n() + ")");

        sayNote("Complexity inference uses log-log regression on timing ratios. "
                + "Results vary by JVM warmup state and hardware.");
    }

    @Test
    void testConstantTimeAccess() {
        sayNextSection("O(1) — HashMap Lookup");
        say("HashMap.get() is O(1) — runtime should be flat as n grows.");

        ComplexityResult result = TimeComplexityAnalyzer.analyze(
            "HashMap.get()",
            n -> {
                HashMap<Integer, Integer> map = new HashMap<>(n * 2);
                for (int i = 0; i < n; i++) map.put(i, i);
                return () -> map.get(n / 2);
            }
        );

        sayAndAssertThat(
            "HashMap.get() infers O(1)",
            result.inferredClass(),
            is("O(1)")
        );
    }
}
