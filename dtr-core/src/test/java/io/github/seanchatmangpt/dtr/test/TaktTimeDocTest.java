package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.toyota.TaktTimer;
import io.github.seanchatmangpt.dtr.toyota.TaktTimer.TaktAnalysis;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.greaterThan;

/**
 * DTR sayTaktTime — Toyota Takt Time production rate documentation tests.
 *
 * <p>Demonstrates {@link TaktTimer} by measuring real process execution against
 * customer demand. Takt time is the maximum allowable cycle time to satisfy demand:
 * {@code takt = availableTime / customerDemand}. All timings are captured via
 * {@link System#nanoTime()} — no estimates, no synthetic data.</p>
 *
 * <p>Takt (German: "beat", "bar") is the heartbeat of the Toyota Production System —
 * the rhythm at which value must be delivered to meet customer pull.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TaktTimeDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — Fast process that easily meets takt (high customer demand relative
    //           to a near-instantaneous unit of work)
    // =========================================================================

    @Test
    void t1_fast_process_meets_takt() {
        sayNextSection("Takt Time — Toyota Production Rate vs Customer Demand");

        say("""
            Takt time is the rhythm of the Toyota Production System: it answers \
            "how fast must we produce one unit to satisfy customer demand?" \
            Formula: <code>takt = availableProductionTime / customerDemand</code>. \
            When cycle time &le; takt time, production keeps up with demand. \
            DTR measures real execution — all timings via <code>System.nanoTime()</code> \
            on Java 26.""");

        sayCode("""
            // Fast process: no-op unit, measured 10 000 times, demand = 100 units
            TaktAnalysis analysis = TaktTimer.measure(
                "No-op unit process",
                () -> {},        // the work unit
                10_000,          // repetitions (production run)
                100              // customer demand in same window
            );
            sayTaktTime(analysis);
            """, "java");

        // Real measurement: trivial no-op — avgCycleNs will be tiny, taktTimeNs = total/100
        // takt >> cycle, so meetsTarget = true and utilization will be very low (<60%)
        TaktAnalysis analysis = TaktTimer.measure(
            "No-op unit process",
            () -> {},
            10_000,
            100
        );

        sayTaktTime(analysis);

        sayAndAssertThat("avgCycleNs > 0 (real measurement)",
            analysis.avgCycleNs(), greaterThan(0L));

        sayAndAssertThat("availableTimeNs > 0 (real elapsed time)",
            analysis.availableTimeNs(), greaterThan(0L));

        sayNote("Takt Time = Available Production Time / Customer Demand");
    }

    // =========================================================================
    // Test 2 — Slower process under tighter demand window
    // =========================================================================

    @Test
    void t2_slower_process_under_tight_demand() {
        sayNextSection("Takt Time — Slower Process Under Tight Customer Demand");

        say("""
            When customer demand is high relative to available production time, \
            a slower process may fail to meet takt. \
            This test measures a process that performs real array sorting work, \
            against a customer demand set close to (or exceeding) the number of units \
            produced. The outcome — meets or behind demand — is determined entirely \
            by real measured execution on Java 26.""");

        sayCode("""
            // Slower process: sort a 100-element array, 200 repetitions, demand = 180
            int[] data = new int[100];
            for (int i = 0; i < 100; i++) data[i] = 100 - i;

            TaktAnalysis analysis = TaktTimer.measure(
                "Array sort (100 elements)",
                () -> {
                    int[] copy = data.clone();
                    java.util.Arrays.sort(copy);
                },
                200,   // repetitions
                180    // customer demand — tight relative to production run
            );
            sayTaktTime(analysis);
            """, "java");

        int[] data = new int[100];
        for (int i = 0; i < 100; i++) data[i] = 100 - i;

        TaktAnalysis analysis = TaktTimer.measure(
            "Array sort (100 elements)",
            () -> {
                int[] copy = data.clone();
                java.util.Arrays.sort(copy);
            },
            200,
            180
        );

        sayTaktTime(analysis);

        sayAndAssertThat("avgCycleNs > 0 (real measurement)",
            analysis.avgCycleNs(), greaterThan(0L));

        sayAndAssertThat("availableTimeNs > 0 (real elapsed time)",
            analysis.availableTimeNs(), greaterThan(0L));

        sayNote("Takt Time = Available Production Time / Customer Demand");
    }
}
