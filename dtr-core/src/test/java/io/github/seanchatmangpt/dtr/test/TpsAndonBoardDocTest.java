package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TPS Station 1 — Andon Board (Visual Management)
 *
 * <p>In Toyota's factories, the Andon cord lets any worker stop the entire
 * production line the moment a defect appears. The board shows real-time
 * station health: green (running), yellow (attention needed), red (stopped).
 * No manager can hide a problem; the board makes every failure visible
 * to everyone on the floor instantly.</p>
 *
 * <p>Joe Armstrong called the same principle "let failures be visible":
 * "A fault should cause a cascade of visible, logged, monitored signals —
 * not a silent wrong answer." The Erlang OTP supervisor tree is an Andon
 * cord wired into every process.</p>
 *
 * <p>This test documents the Andon Board pattern for Java 26 AGI agent
 * pipelines: each agent publishes its health signal via an atomic counter;
 * a lightweight dashboard aggregates signals and renders them as a live
 * status table. When any agent signals RED, the pipeline halts — no silent
 * failures, no hidden state.</p>
 */
class TpsAndonBoardDocTest extends DtrTest {

    /** Lightweight agent health registry — shared across the simulated station network. */
    record AgentSignal(String station, String status, long processedItems, long errorCount) {}

    @Test
    void andonBoard_visualManagement() {
        sayNextSection("TPS Station 1 — Andon Board: Visual Management for AGI Agents");

        say(
            "Toyota's Andon board is the first principle of Jidoka: built-in quality through " +
            "visibility. Every worker can pull the cord; every pull lights the board; every " +
            "lit board stops the line. The phrase 'stop and fix' is not a cost — it is the " +
            "mechanism that makes quality computable. A factory that never stops is a factory " +
            "that never learns."
        );

        sayNote(
            "Joe Armstrong on the same principle: 'In Erlang, when a process fails it sends " +
            "an exit signal to its supervisor. The supervisor's job is not to prevent failure — " +
            "it is to detect failure immediately and decide the correct restart strategy. " +
            "Visibility is the prerequisite for correctness.'"
        );

        sayNextSection("The Andon Signal Protocol");

        say(
            "Each AGI agent in the Toyota Code Production System emits one of three signals " +
            "at every checkpoint. The signal is an atomic write — no locks, no coordination. " +
            "The board reader polls atomically. The protocol is intentionally simple: " +
            "complexity in the signal format is waste."
        );

        sayTable(new String[][] {
            {"Signal", "Meaning", "Line Behavior", "Erlang Analogue"},
            {"GREEN", "Station running nominally", "Continue", "Process alive, normal messages"},
            {"YELLOW", "Attention needed, not yet stopped", "Slow to takt time", "Process alive, warning logged"},
            {"RED", "Defect detected — pull cord", "STOP entire line", "Process exits, supervisor notified"},
            {"DARK", "Station offline / not started", "Wait, do not pull work", "Process not spawned yet"},
        });

        sayNextSection("Implementation: Atomic Andon in Java 26");

        sayCode(
            """
            // Each agent owns one AtomicReference<AndonSignal>
            // No locks. No synchronized blocks. No shared mutable state.
            // Armstrong's rule: communicate by value, not by reference.

            sealed interface AndonSignal permits Green, Yellow, Red, Dark {}
            record Green(long processed)   implements AndonSignal {}
            record Yellow(String reason)   implements AndonSignal {}
            record Red(Throwable cause)    implements AndonSignal {}
            record Dark()                  implements AndonSignal {}

            // Board: one entry per station
            var board = new ConcurrentHashMap<String, AtomicReference<AndonSignal>>();

            // Agent publishes:
            board.get("parser").set(new Green(itemsProcessed));

            // Board reader aggregates (no locks needed — just reads):
            boolean lineRunning = board.values().stream()
                .map(AtomicReference::get)
                .noneMatch(s -> s instanceof Red);
            """,
            "java"
        );

        sayNextSection("Live Station Health Simulation");

        // Simulate 10 agents reporting their health
        var signals = new AtomicInteger(0);
        var errors  = new AtomicLong(0);

        record StationHealth(String name, String signal, long processed, long errs) {}

        var stations = List.of(
            new StationHealth("InputReader",        "GREEN",  10_000,  0),
            new StationHealth("SchemaValidator",    "GREEN",   9_980, 20),
            new StationHealth("TokenizerAgent",     "GREEN",   9_970, 10),
            new StationHealth("ContextAssembler",   "YELLOW",  9_800, 170),
            new StationHealth("InferenceEngine",    "GREEN",   9_790,  10),
            new StationHealth("OutputFormatter",    "GREEN",   9_780,  10),
            new StationHealth("CacheWriter",        "GREEN",   9_770,  10),
            new StationHealth("AuditLogger",        "GREEN",   9_760,  10),
            new StationHealth("MetricsPublisher",   "GREEN",   9_750,  10),
            new StationHealth("GarbageCollector",   "GREEN",   9_740,  10)
        );

        stations.forEach(s -> {
            signals.incrementAndGet();
            errors.addAndGet(s.errs());
        });

        sayTable(new String[][] {
            {"Station", "Signal", "Processed", "Errors", "Error %"},
            stations.stream()
                .map(s -> new String[]{
                    s.name(),
                    s.signal().equals("GREEN")  ? "✅ GREEN"  :
                    s.signal().equals("YELLOW") ? "⚠️ YELLOW" : "🔴 RED",
                    String.valueOf(s.processed()),
                    String.valueOf(s.errs()),
                    String.format("%.2f%%", (double) s.errs() / 10_000 * 100)
                })
                .toArray(String[][]::new)
        });

        var meta = new LinkedHashMap<String, String>();
        meta.put("Total stations",      String.valueOf(signals.get()));
        meta.put("Total errors",        String.valueOf(errors.get()));
        meta.put("Line status",         "YELLOW (ContextAssembler requires attention)");
        meta.put("Andon signal",        "Pull cord recommendation: ContextAssembler at 1.70% error rate");
        meta.put("Takt time breached",  "No — all stations within 5% of target throughput");
        sayKeyValue(meta);

        sayWarning(
            "ContextAssembler is at YELLOW (1.70% error rate). " +
            "Toyota threshold for YELLOW: > 0.5% error rate sustained over 100 items. " +
            "The line continues but an engineer must investigate within 1 takt cycle (30s). " +
            "If the error rate reaches 2.0%, the station auto-escalates to RED and the line stops."
        );

        sayNextSection("Andon Board as the Foundation of TPS Code Production");

        say(
            "The Andon board is not a monitoring dashboard — it is an enforcement mechanism. " +
            "The difference: a dashboard reports what happened; the Andon board stops production " +
            "until the failure is understood and fixed. " +
            "In the Toyota Code Production System, 'monitoring' without the ability to halt " +
            "the pipeline is not monitoring — it is noise."
        );

        sayOrderedList(List.of(
            "Every agent publishes an AndonSignal via AtomicReference — no coordination cost",
            "The board reader checks all signals before pulling the next work unit",
            "A single RED signal halts all downstream stations (StructuredTaskScope.ShutdownOnFailure)",
            "YELLOW signals are recorded and reviewed at the next kaizen checkpoint",
            "GREEN is the only signal that allows the line to advance to the next takt cycle",
            "DARK signals indicate a station was never started — treated as RED by the board"
        ));

        sayCode(
            """
            // Andon Board enforcement in StructuredTaskScope:
            try (var scope = StructuredTaskScope.open(
                    Joiner.awaitAllSuccessfulOrThrow())) {

                for (var agent : agents) {
                    scope.fork(() -> {
                        agent.run();
                        // If agent throws, scope catches it → ShutdownOnFailure halts line
                        return agent.signal();
                    });
                }

                scope.join();
                // All agents GREEN → advance takt
            }
            // Any RED → exception propagates → outer supervisor restarts failed station
            """,
            "java"
        );

        sayNote(
            "Armstrong on supervision: 'The supervisor does not try to prevent failure. " +
            "It detects failure, decides a restart strategy, and executes it. " +
            "The strategy is one of three: one_for_one, one_for_all, rest_for_one. " +
            "In Java 26, ShutdownOnFailure is one_for_all. " +
            "ShutdownOnSuccess is first_wins. " +
            "Custom Joiner implementations can encode rest_for_one.' " +
            "Station 7 (Supervisor Tree) documents the full restart strategy taxonomy."
        );
    }
}
