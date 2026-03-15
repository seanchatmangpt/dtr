package io.github.seanchatmangpt.dtr.toyota;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Toyota Production System kanban pull-system model for DTR documentation.
 *
 * <p>Models a kanban board as a snapshot: cards assigned to statuses,
 * WIP limits per column, flow efficiency, and blocked card counts.
 * All computation is pure Java — no external dependencies.</p>
 *
 * @since 2026.1.0
 */
public final class KanbanBoard {

    private KanbanBoard() {}

    // =========================================================================
    // Domain model
    // =========================================================================

    /** Kanban column statuses following Toyota's pull system flow. */
    public enum Status {
        BACKLOG,
        IN_PROGRESS,
        BLOCKED,
        DONE
    }

    /**
     * A single work item on the board.
     *
     * @param id        unique identifier (e.g. "TASK-001")
     * @param title     human-readable title
     * @param status    current column/status
     * @param priority  1 = highest, higher number = lower priority
     * @param cycleNs   time spent in IN_PROGRESS state, in nanoseconds
     */
    public record KanbanCard(
            String id,
            String title,
            Status status,
            int priority,
            long cycleNs) {}

    /**
     * Aggregate statistics for one board column.
     *
     * @param name        column name (derived from {@link Status#name()})
     * @param count       number of cards currently in this column
     * @param wipLimit    maximum allowed cards (0 = no limit)
     * @param overWip     true when count exceeds wipLimit and wipLimit > 0
     * @param avgCycleMs  average cycle time in milliseconds for cards in this column
     */
    public record ColumnStats(
            String name,
            int count,
            int wipLimit,
            boolean overWip,
            double avgCycleMs) {}

    /**
     * Full point-in-time snapshot of the board.
     *
     * @param boardName      display name of the board
     * @param columns        per-column statistics (one per {@link Status})
     * @param totalCards     total number of cards on the board
     * @param blockedCount   number of cards in {@link Status#BLOCKED}
     * @param flowEfficiency percentage of cards that are {@link Status#DONE} (0–100)
     */
    public record BoardSnapshot(
            String boardName,
            List<ColumnStats> columns,
            int totalCards,
            int blockedCount,
            double flowEfficiency) {}

    // =========================================================================
    // Factory method
    // =========================================================================

    /**
     * Computes a {@link BoardSnapshot} from a list of cards and WIP limits.
     *
     * <p>Flow efficiency = (DONE cards / total cards) * 100.  A WIP violation
     * occurs when the card count in a column exceeds the configured limit.</p>
     *
     * @param name      board display name
     * @param cards     all cards on the board
     * @param wipLimits map of {@link Status} to maximum allowed WIP (missing key = no limit)
     * @return computed snapshot
     */
    public static BoardSnapshot snapshot(
            String name,
            List<KanbanCard> cards,
            Map<Status, Integer> wipLimits) {

        int total = cards.size();
        int blocked = (int) cards.stream()
                .filter(c -> c.status() == Status.BLOCKED)
                .count();
        int done = (int) cards.stream()
                .filter(c -> c.status() == Status.DONE)
                .count();
        double flowEfficiency = total == 0 ? 0.0 : (done * 100.0) / total;

        // Group cards by status
        Map<Status, List<KanbanCard>> byStatus = cards.stream()
                .collect(Collectors.groupingBy(KanbanCard::status));

        List<ColumnStats> columns = List.of(Status.values()).stream()
                .map(status -> {
                    List<KanbanCard> colCards = byStatus.getOrDefault(status, List.of());
                    int count = colCards.size();
                    int wipLimit = wipLimits.getOrDefault(status, 0);
                    boolean overWip = wipLimit > 0 && count > wipLimit;
                    double avgCycleMs = colCards.isEmpty() ? 0.0
                            : colCards.stream()
                                    .mapToLong(KanbanCard::cycleNs)
                                    .average()
                                    .orElse(0.0) / 1_000_000.0;
                    return new ColumnStats(status.name(), count, wipLimit, overWip, avgCycleMs);
                })
                .collect(Collectors.toList());

        return new BoardSnapshot(name, columns, total, blocked, flowEfficiency);
    }
}
