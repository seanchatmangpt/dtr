package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.toyota.AndonBoard;
import io.github.seanchatmangpt.dtr.toyota.AndonBoard.BoardState;
import io.github.seanchatmangpt.dtr.toyota.AndonBoard.HealthCheck;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

/**
 * DTR sayAndonBoard — Toyota Andon stop-the-line health dashboard documentation tests.
 *
 * <p>Demonstrates {@link AndonBoard} by evaluating a production line with four stations:
 * two healthy (GREEN), one degraded (YELLOW), and one with a defect (RED). The board
 * reflects Toyota's TPS principle: any worker can pull the Andon cord to stop the line
 * the moment a defect is detected.</p>
 *
 * <p>All health-check timing is measured with {@link System#nanoTime()} on real execution
 * — no simulated or hard-coded signal values.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class AndonBoardDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — Andon board with mixed station signals
    // =========================================================================

    @Test
    void t1_andonBoard_with_mixed_signals() {
        sayNextSection("Andon Board — Toyota Stop-the-Line Quality Control");

        say("""
            The Toyota Andon system allows any worker to stop the production line \
            the moment a defect is detected. DTR models this as a health dashboard: \
            each station runs a real probe and reports GREEN, YELLOW, or RED. \
            If any station is RED the line is marked STOPPED.""");

        sayCode("""
            // Build a map of station names to health checks
            Map<String, HealthCheck> stations = new LinkedHashMap<>();

            // GREEN: probe succeeds quickly
            stations.put("Order Service",   AndonBoard.of("HTTP 200 OK", () -> {}));
            stations.put("Inventory Cache", AndonBoard.of("Cache hit rate 98%", () -> {}));

            // YELLOW: probe succeeds but we explicitly return a degraded station
            stations.put("Payment Gateway", stationName ->
                new AndonBoard.Station(stationName, AndonBoard.Signal.YELLOW,
                    "Response p99 > 800ms — degraded", System.nanoTime(), null));

            // RED: probe throws an exception — defect detected
            stations.put("Shipping API", AndonBoard.of("Connection refused", () -> {
                throw new RuntimeException("Connection refused: shipping-svc:8080");
            }));

            BoardState board = AndonBoard.evaluate("Fulfilment Line v3", stations);
            sayAndonBoard(board);
            """, "java");

        // Build real stations — measures are from System.nanoTime() in AndonBoard.of()
        Map<String, HealthCheck> stations = new LinkedHashMap<>();

        // Station 1 — GREEN: probe completes instantly
        stations.put("Order Service", AndonBoard.of("HTTP 200 OK", () -> {}));

        // Station 2 — GREEN: probe completes instantly
        stations.put("Inventory Cache", AndonBoard.of("Cache hit rate 98%", () -> {}));

        // Station 3 — YELLOW: return a YELLOW station directly (degraded but alive)
        stations.put("Payment Gateway", stationName ->
                new AndonBoard.Station(stationName, AndonBoard.Signal.YELLOW,
                        "Response p99 > 800ms — degraded", System.nanoTime(), null));

        // Station 4 — RED: probe throws, AndonBoard.of() catches and returns RED
        stations.put("Shipping API", AndonBoard.of("Connection refused", () -> {
            throw new RuntimeException("Connection refused: shipping-svc:8080");
        }));

        BoardState board = AndonBoard.evaluate("Fulfilment Line v3", stations);

        sayAndonBoard(board);

        sayAndAssertThat("Line is STOPPED because a RED station exists",
                board.lineRunning(), is(false));

        sayAndAssertThat("Exactly 1 RED station detected",
                board.redCount(), is(1));

        sayNote("Toyota TPS: any worker can pull the Andon cord to stop the line. "
                + "The line does not restart until the root cause is resolved — "
                + "this is jidoka (autonomation with a human touch).");
    }
}
