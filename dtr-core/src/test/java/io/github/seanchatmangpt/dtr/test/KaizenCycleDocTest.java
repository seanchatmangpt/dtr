package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.toyota.KaizenCycle;
import io.github.seanchatmangpt.dtr.toyota.KaizenCycle.KaizenReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

/**
 * DTR sayKaizenCycle — Toyota continuous improvement cycle documentation tests.
 *
 * <p>Demonstrates {@link KaizenCycle} by running a real operation across multiple
 * improvement cycles and measuring the performance trend. Each cycle captures
 * actual min/avg/max nanosecond timings via {@link System#nanoTime()} — no estimates,
 * no synthetic data.</p>
 *
 * <p>Kaizen (改善) means "change for the better" — the Toyota Production System
 * principle of small continuous improvements compounding into dramatic gains.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class KaizenCycleDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — String concatenation across 5 Kaizen cycles
    // =========================================================================

    @Test
    void t1_kaizen_string_concatenation() {
        sayNextSection("Kaizen Cycle — Toyota Continuous Improvement Measurement");

        say("""
            Kaizen (改善) is the Toyota Production System principle of continuous \
            improvement: run the same process, measure it, refine it, and repeat. \
            DTR models this by executing an operation across N cycles and measuring \
            the performance trend cycle by cycle. \
            All timings are real — captured via <code>System.nanoTime()</code> \
            over actual invocations on Java 26.""");

        sayCode("""
            KaizenReport report = KaizenCycle.run(
                "String list build",
                () -> {
                    List<String> list = new ArrayList<>(50);
                    for (int i = 0; i < 50; i++) list.add("item-" + i);
                },
                5,   // improvement cycles
                50   // samples per cycle
            );
            sayKaizenCycle(report);
            """, "java");

        // Run the real operation: build a 50-element list with string concatenation
        KaizenReport report = KaizenCycle.run(
            "String list build",
            () -> {
                List<String> list = new ArrayList<>(50);
                for (int i = 0; i < 50; i++) list.add("item-" + i);
            },
            5,
            50
        );

        sayKaizenCycle(report);

        sayAndAssertThat("Baseline nanoseconds > 0",
            report.baselineNs(), greaterThan(0L));

        sayAndAssertThat("Cycle count == 5",
            report.cycles().size(), is(5));

        sayNote("Kaizen: small continuous improvements compound into dramatic gains");
    }

    // =========================================================================
    // Test 2 — List sort across 5 Kaizen cycles
    // =========================================================================

    @Test
    void t2_kaizen_list_sort() {
        sayNextSection("Kaizen Cycle — List Sort Stabilisation");

        say("""
            This test applies the Kaizen model to a list-sort operation. \
            The JVM's JIT compiler typically improves performance across early cycles \
            as it detects hot paths and compiles them to native code. \
            Convergence is achieved when the last 3 cycles all fall within 5% of each other.""");

        // 20-element integer list sort — chosen to trigger measurable JIT warm-up
        int[] data = new int[]{19, 3, 15, 7, 12, 1, 17, 5, 9, 11,
                               14, 0, 8, 16, 4, 2, 18, 6, 13, 10};

        KaizenReport report = KaizenCycle.run(
            "Integer list sort (20 elements)",
            () -> {
                List<Integer> list = new ArrayList<>(data.length);
                for (int v : data) list.add(v);
                list.sort(Integer::compareTo);
            },
            5,
            50
        );

        sayKaizenCycle(report);

        sayAndAssertThat("Baseline nanoseconds > 0",
            report.baselineNs(), greaterThan(0L));

        sayAndAssertThat("Cycle count == 5",
            report.cycles().size(), is(5));

        sayNote("Kaizen: small continuous improvements compound into dramatic gains");
    }
}
