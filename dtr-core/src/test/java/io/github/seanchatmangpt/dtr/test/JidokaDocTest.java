package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.toyota.JidokaProcessor;
import io.github.seanchatmangpt.dtr.toyota.JidokaProcessor.DefectDetector;
import io.github.seanchatmangpt.dtr.toyota.JidokaProcessor.JidokaReport;
import io.github.seanchatmangpt.dtr.toyota.JidokaProcessor.WorkItem;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;

/**
 * DTR sayJidoka — Toyota autonomation defect-detection documentation tests.
 *
 * <p>Demonstrates {@link JidokaProcessor} by running work-item batches through
 * defect detectors that mirror Toyota's Jidoka principle: machines detect their own
 * defects and stop automatically. Each test proves the processor's behaviour with
 * real execution — not estimates.</p>
 *
 * <p>All timing figures are measured with {@link System#nanoTime()} over real
 * processing on Java 26.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class JidokaDocTest extends DtrTest {

    @org.junit.jupiter.api.AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — clean batch (no defects, machine stays running)
    // =========================================================================

    @Test
    void t1_clean_batch_machine_stays_running() {
        sayNextSection("sayJidoka — Toyota Autonomation (Clean Batch)");

        say("""
            Toyota Jidoka empowers machines to detect their own defects and stop \
            automatically rather than passing defective work downstream. \
            DTR documents this principle by running work items through \
            {@code JidokaProcessor.process()} with configurable defect detectors \
            and rendering the full per-item audit trail.""");

        sayCode("""
            List<WorkItem> items = List.of(
                new WorkItem("widget-001", 42),
                new WorkItem("widget-002", 17),
                new WorkItem("widget-003", 99),
                new WorkItem("widget-004", 5),
                new WorkItem("widget-005", 63)
            );

            List<DefectDetector> detectors = List.of(
                data -> data == null ? "null payload" : null,
                data -> (data instanceof Integer i && i < 0)
                        ? "negative value: " + i : null
            );

            JidokaReport report = JidokaProcessor.process(
                "Widget Assembly Line", items, detectors);
            """, "java");

        List<WorkItem> items = List.of(
            new WorkItem("widget-001", 42),
            new WorkItem("widget-002", 17),
            new WorkItem("widget-003", 99),
            new WorkItem("widget-004", 5),
            new WorkItem("widget-005", 63)
        );

        List<DefectDetector> detectors = List.of(
            data -> data == null ? "null payload" : null,
            data -> (data instanceof Integer i && i < 0) ? "negative value: " + i : null
        );

        long start = System.nanoTime();
        JidokaReport report = JidokaProcessor.process("Widget Assembly Line", items, detectors);
        long elapsed = System.nanoTime() - start;

        sayJidoka(report);

        sayTable(new String[][]{
            {"Metric", "Value", "Environment"},
            {"Items processed",  String.valueOf(items.size()),           "Java 26"},
            {"Total elapsed",    JidokaProcessor.humanNs(elapsed),       "System.nanoTime()"},
            {"Defectors ran",    String.valueOf(detectors.size()),        "per item"},
            {"Machine stopped",  String.valueOf(report.machineStopped()), "Jidoka verdict"}
        });

        sayAndAssertThat("All items passed",
            report.passed(), is(items.size()));
        sayAndAssertThat("No defects detected",
            report.defects(), is(0));
        sayAndAssertThat("Machine is running (no auto-stop)",
            report.machineStopped(), is(false));
    }

    // =========================================================================
    // Test 2 — defective batch (machine stops on first defect)
    // =========================================================================

    @Test
    void t2_defective_batch_machine_stops() {
        sayNextSection("sayJidoka — Toyota Autonomation (Defective Batch)");

        say("""
            This test submits a mixed batch: some items are valid, others carry defects \
            (null payload, negative values). The Jidoka machine detects each defect, \
            tags the item {@code AUTO_STOPPED}, and sets {@code machineStopped = true}. \
            Human intervention is required to clear the defect and resume the line.""");

        sayCode("""
            List<WorkItem> items = List.of(
                new WorkItem("part-A", 10),
                new WorkItem("part-B", null),      // null payload — defect
                new WorkItem("part-C", 25),
                new WorkItem("part-D", -7),         // negative value — defect
                new WorkItem("part-E", 50)
            );

            List<DefectDetector> detectors = List.of(
                data -> data == null ? "null payload — cannot process" : null,
                data -> (data instanceof Integer i && i < 0)
                        ? "out-of-range value: " + i + " (must be >= 0)" : null
            );

            JidokaReport report = JidokaProcessor.process(
                "Parts Inspection Station", items, detectors);
            """, "java");

        List<WorkItem> items = List.of(
            new WorkItem("part-A", 10),
            new WorkItem("part-B", null),
            new WorkItem("part-C", 25),
            new WorkItem("part-D", -7),
            new WorkItem("part-E", 50)
        );

        List<DefectDetector> detectors = List.of(
            data -> data == null ? "null payload — cannot process" : null,
            data -> (data instanceof Integer i && i < 0)
                    ? "out-of-range value: " + i + " (must be >= 0)" : null
        );

        long start = System.nanoTime();
        JidokaReport report = JidokaProcessor.process("Parts Inspection Station", items, detectors);
        long elapsed = System.nanoTime() - start;

        sayJidoka(report);

        sayTable(new String[][]{
            {"Metric", "Value", "Environment"},
            {"Items processed",  String.valueOf(items.size()),           "Java 26"},
            {"Total elapsed",    JidokaProcessor.humanNs(elapsed),       "System.nanoTime()"},
            {"Passed",           String.valueOf(report.passed()),         "Jidoka verdict"},
            {"Defects detected", String.valueOf(report.defects()),        "Jidoka verdict"},
            {"Auto-stops",       String.valueOf(report.autoStops()),      "Jidoka verdict"},
            {"Machine stopped",  String.valueOf(report.machineStopped()), "Jidoka verdict"}
        });

        sayAndAssertThat("Two defects detected (null + negative)",
            report.defects(), is(2));
        sayAndAssertThat("Machine stopped (auto-stop triggered)",
            report.machineStopped(), is(true));
        sayAndAssertThat("Three items passed",
            report.passed(), is(3));

        sayNote("""
            Toyota Jidoka: the machine detected defects autonomously and stopped the line. \
            No defective parts were passed downstream. \
            A human operator reviews the flagged items and clears the defect before restarting.""");
    }
}
