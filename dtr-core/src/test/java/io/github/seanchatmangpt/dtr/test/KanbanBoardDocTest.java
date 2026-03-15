package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.toyota.KanbanBoard;
import io.github.seanchatmangpt.dtr.toyota.KanbanBoard.KanbanCard;
import io.github.seanchatmangpt.dtr.toyota.KanbanBoard.Status;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;

/**
 * DTR sayKanbanBoard — Toyota Production System kanban pull-system visualization.
 *
 * <p>Demonstrates {@link KanbanBoard} by building a realistic board snapshot with
 * cards spread across all statuses, intentional WIP limit violations, and real
 * cycle-time measurements. Documents the TPS pull-system principle: limit WIP,
 * visualize flow, pull don't push.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class KanbanBoardDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — Full board snapshot with WIP violation
    // =========================================================================

    @Test
    void t1_kanban_board_with_wip_violation() {
        sayNextSection("Kanban Board — Toyota Pull System Visualization");

        say("""
            Toyota's kanban system makes work visible and limits work-in-progress (WIP). \
            Each card moves through columns: BACKLOG \u2192 IN_PROGRESS \u2192 DONE, \
            or becomes BLOCKED when an impediment stops flow. \
            WIP limits prevent overloading any column — pull, don't push.""");

        sayCode("""
            List<KanbanCard> cards = List.of(
                new KanbanCard("TASK-001", "Design API schema",    Status.DONE,        1, cycleNs),
                new KanbanCard("TASK-002", "Implement endpoints",  Status.IN_PROGRESS, 1, cycleNs),
                new KanbanCard("TASK-003", "Write DTR doc tests",  Status.IN_PROGRESS, 2, cycleNs),
                new KanbanCard("TASK-004", "Code review",          Status.BLOCKED,     2, 0),
                new KanbanCard("TASK-005", "Deploy to staging",    Status.BACKLOG,     3, 0)
            );
            Map<Status, Integer> wipLimits = Map.of(
                Status.IN_PROGRESS, 2,   // 3 cards will exceed this
                Status.BLOCKED,     1
            );
            BoardSnapshot board = KanbanBoard.snapshot("Sprint-42", cards, wipLimits);
            sayKanbanBoard(board);
            """, "java");

        // Real cycle time measurements for IN_PROGRESS cards
        long cycleNs1 = measureCycleTime(50);
        long cycleNs2 = measureCycleTime(75);
        long cycleNs3 = measureCycleTime(30);

        List<KanbanCard> cards = List.of(
            // DONE cards
            new KanbanCard("TASK-001", "Design API schema",             Status.DONE,        1, cycleNs1),
            new KanbanCard("TASK-002", "Write OpenAPI spec",            Status.DONE,        1, cycleNs2),
            new KanbanCard("TASK-003", "Set up CI pipeline",            Status.DONE,        2, cycleNs3),
            // IN_PROGRESS cards — 3 cards exceeds WIP limit of 2
            new KanbanCard("TASK-004", "Implement REST endpoints",      Status.IN_PROGRESS, 1, cycleNs1),
            new KanbanCard("TASK-005", "Write DTR kanban doc tests",    Status.IN_PROGRESS, 2, cycleNs2),
            new KanbanCard("TASK-006", "Integrate value stream mapper", Status.IN_PROGRESS, 2, cycleNs3),
            // BLOCKED card
            new KanbanCard("TASK-007", "Code review (awaiting member)", Status.BLOCKED,     1, 0L),
            // BACKLOG cards
            new KanbanCard("TASK-008", "Performance benchmarks",        Status.BACKLOG,     3, 0L),
            new KanbanCard("TASK-009", "Deploy to staging",             Status.BACKLOG,     4, 0L)
        );

        Map<Status, Integer> wipLimits = Map.of(
            Status.IN_PROGRESS, 2,   // 3 cards present — intentional violation for demo
            Status.BLOCKED,     1
        );

        KanbanBoard.BoardSnapshot board = KanbanBoard.snapshot("Sprint-42", cards, wipLimits);

        sayKanbanBoard(board);

        // Assertions
        sayAndAssertThat("blockedCount is non-negative",
            board.blockedCount() >= 0, is(true));
        sayAndAssertThat("flowEfficiency is >= 0.0",
            board.flowEfficiency() >= 0.0, is(true));
        sayAndAssertThat("flowEfficiency is <= 100.0",
            board.flowEfficiency() <= 100.0, is(true));
        sayAndAssertThat("totalCards matches list size",
            board.totalCards(), is(cards.size()));

        sayNote("Toyota TPS: limit WIP, visualize flow, pull don't push.");
    }

    // =========================================================================
    // Test 2 — Healthy board with no WIP violations
    // =========================================================================

    @Test
    void t2_kanban_board_healthy_no_wip_violations() {
        sayNextSection("Kanban Board — Healthy Flow (No WIP Violations)");

        say("""
            A well-managed kanban board stays within WIP limits at all times. \
            When every column respects its limit, work flows smoothly from \
            BACKLOG through IN_PROGRESS to DONE — the Toyota ideal state.""");

        long cycleNs = measureCycleTime(100);

        List<KanbanCard> cards = List.of(
            new KanbanCard("STORY-001", "User authentication",     Status.DONE,        1, cycleNs),
            new KanbanCard("STORY-002", "Password reset flow",     Status.DONE,        1, cycleNs),
            new KanbanCard("STORY-003", "OAuth integration",       Status.IN_PROGRESS, 2, cycleNs),
            new KanbanCard("STORY-004", "Session management",      Status.BACKLOG,     3, 0L),
            new KanbanCard("STORY-005", "Audit logging",           Status.BACKLOG,     4, 0L)
        );

        Map<Status, Integer> wipLimits = Map.of(
            Status.IN_PROGRESS, 3,  // 1 card — well within limit
            Status.BLOCKED,     2
        );

        KanbanBoard.BoardSnapshot board = KanbanBoard.snapshot("Auth-Sprint", cards, wipLimits);

        sayKanbanBoard(board);

        // Verify no columns are over WIP
        long overWipColumns = board.columns().stream()
            .filter(KanbanBoard.ColumnStats::overWip)
            .count();

        sayAndAssertThat("No columns over WIP limit", overWipColumns, is(0L));
        sayAndAssertThat("blockedCount is zero", board.blockedCount(), is(0));
        sayAndAssertThat("flowEfficiency >= 0.0", board.flowEfficiency() >= 0.0, is(true));
        sayAndAssertThat("flowEfficiency <= 100.0", board.flowEfficiency() <= 100.0, is(true));

        sayNote("Toyota TPS: limit WIP, visualize flow, pull don't push.");
    }

    // =========================================================================
    // Helper: measure real cycle time using System.nanoTime()
    // =========================================================================

    /**
     * Simulates measuring the real cycle time spent on a work item by
     * sleeping for {@code millis} milliseconds and measuring elapsed nanos.
     *
     * @param millis simulated work duration
     * @return elapsed nanoseconds
     */
    private long measureCycleTime(long millis) {
        long start = System.nanoTime();
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return System.nanoTime() - start;
    }
}
