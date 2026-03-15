package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * TPS Station 2 — Jidoka: Stop-the-Line Quality Gate
 *
 * <p>Jidoka (自働化) is autonomation with a human touch — the machine stops
 * itself when it detects a defect, rather than producing bad output at full
 * speed. Sakichi Toyoda invented the principle for looms: if a thread breaks,
 * the loom halts. One hundred defective bolts in five minutes is catastrophically
 * worse than five good bolts with one halt for investigation.</p>
 *
 * <p>Joe Armstrong encoded the same principle as "let it crash": a process
 * that detects an invalid state should exit immediately, loudly, and with full
 * diagnostic information — rather than attempt to handle an error it was not
 * designed to handle. The supervisor's job is recovery; the worker's job is
 * correctness or failure.</p>
 *
 * <p>In Java 26, Jidoka maps directly to sealed exception hierarchies + pattern
 * matching: the type system enforces exhaustive handling at every quality gate.
 * A gate that does not handle all defect classes fails to compile. A compiler
 * error at gate design time is infinitely cheaper than a runtime defect in
 * production.</p>
 */
class TpsJidokaDocTest extends DtrTest {

    @Test
    void jidoka_stopTheLineQualityGate() {
        sayNextSection("TPS Station 2 — Jidoka: Stop-the-Line Quality Gate");

        say(
            "Jidoka is not error handling. Error handling is reactive — it catches a problem " +
            "after it has already occurred and tries to recover. Jidoka is preventive: the " +
            "station inspects its own output before passing it downstream, and if the output " +
            "does not meet spec, the station stops and signals the Andon board. " +
            "Nothing downstream ever receives a defective work unit. " +
            "The cost of stopping is one takt cycle. The cost of not stopping is every " +
            "downstream station processing a bad unit until a customer finds it."
        );

        sayTable(new String[][] {
            {"Principle", "Toyota Factory", "Java 26 Code Production", "Armstrong / Erlang"},
            {"Detection", "Sensors on every jig check dimensions", "Sealed defect hierarchy, exhaustive match", "Pattern match on message type"},
            {"Halt", "Loom stops on thread break", "StructuredTaskScope.ShutdownOnFailure", "Process sends exit signal"},
            {"Signal", "Andon lamp turns RED", "throw new QualityDefect(cause)", "supervisor receives {'EXIT', Pid, Reason}"},
            {"Recovery", "Engineer fixes root cause", "Supervisor restarts with corrected input", "one_for_one restart"},
            {"Prevent recurrence", "Poka-yoke added to jig", "Compile-time sealed constraint added", "New spec clause added"},
        });

        sayNextSection("The Sealed Defect Hierarchy");

        say(
            "In Java 26, a sealed interface with exhaustive pattern matching is a Jidoka gate " +
            "implemented in the type system. Every defect class is declared; every handler " +
            "must cover every class; the compiler enforces completeness. " +
            "A quality gate with an uncovered defect class is a compilation error, not a runtime surprise."
        );

        sayCode(
            """
            // Sealed defect hierarchy — every defect type is enumerated at design time
            sealed interface QualityDefect extends Throwable
                permits SchemaDefect, RangeDefect, EncodingDefect, LatencyDefect {}

            record SchemaDefect  (String field, String expected, String actual) implements QualityDefect {}
            record RangeDefect   (String field, double min, double max, double actual) implements QualityDefect {}
            record EncodingDefect(String field, String encoding, byte[] raw) implements QualityDefect {}
            record LatencyDefect (String station, long budgetNs, long actualNs) implements QualityDefect {}

            // Jidoka gate — exhaustive by construction
            QualityDefect defect = inspectWorkUnit(unit);
            String remedy = switch (defect) {
                case SchemaDefect   d -> "Field '%s': expected %s, got %s".formatted(d.field(), d.expected(), d.actual());
                case RangeDefect    d -> "Field '%s': %.2f not in [%.2f, %.2f]".formatted(d.field(), d.actual(), d.min(), d.max());
                case EncodingDefect d -> "Field '%s': encoding %s invalid (%d bytes)".formatted(d.field(), d.encoding(), d.raw().length);
                case LatencyDefect  d -> "Station '%s': %dns > budget %dns".formatted(d.station(), d.actualNs(), d.budgetNs());
                // No default needed — compiler verifies exhaustion
            };
            """,
            "java"
        );

        sayNote(
            "Armstrong: 'The beauty of pattern matching on message types is that the compiler " +
            "tells you when you have added a new message type and forgotten to handle it. " +
            "This is not a runtime check — it is a design-time invariant enforced by the " +
            "compiler. Erlang's function clause matching does the same thing. " +
            "If you add a new atom to your message protocol and forget a clause, " +
            "the process crashes loudly on the first unhandled message — not silently.'"
        );

        sayNextSection("Quality Tiers and Gate Severity");

        say(
            "Not all defects are equal. Jidoka recognises three severity tiers: " +
            "Critical (halt immediately), Major (flag for immediate review, continue cautiously), " +
            "Minor (log and continue). The tier determines the restart strategy."
        );

        sayTable(new String[][] {
            {"Tier", "Defect Type", "Gate Action", "Restart Strategy", "Example"},
            {"Critical", "Data corruption, security violation", "STOP — ShutdownOnFailure", "one_for_all (all siblings stop)", "EncodingDefect on PII field"},
            {"Major",    "Schema mismatch, out-of-range value", "YELLOW — continue with quarantine", "one_for_one (restart this station)", "RangeDefect on numeric field"},
            {"Minor",    "Latency budget exceeded", "LOG — record and proceed", "No restart needed", "LatencyDefect on cache read"},
        });

        sayNextSection("Jidoka in Practice: The Gate Function");

        say(
            "The gate function is the atomic unit of Jidoka. It takes a work unit, " +
            "inspects it against the station's specification, and either returns the " +
            "unit (GREEN) or throws a typed defect (YELLOW/RED). " +
            "The gate function has no side effects beyond the signal. " +
            "Armstrong: 'A function that both does work and reports errors is harder to " +
            "reason about than two functions: one that does work, one that checks correctness.'"
        );

        sayCode(
            """
            // Gate function: pure inspection, no mutation
            static WorkUnit jidokaGate(WorkUnit unit, StationSpec spec) throws QualityDefect {
                // Check schema first (fastest check)
                if (!spec.schema().validate(unit.fields())) {
                    throw new SchemaDefect(unit.failingField(), spec.expectedType(), unit.actualType());
                }
                // Check ranges (numeric bounds)
                for (var field : spec.rangeCheckedFields()) {
                    double val = unit.getDouble(field.name());
                    if (val < field.min() || val > field.max()) {
                        throw new RangeDefect(field.name(), field.min(), field.max(), val);
                    }
                }
                // Unit passed all gates
                return unit;
            }

            // Station calls gate before passing work downstream:
            try {
                WorkUnit checked = jidokaGate(rawUnit, stationSpec);
                downstream.put(checked); // Kanban card advances
            } catch (QualityDefect defect) {
                andonBoard.signal(stationId, defect); // Lights the board
                throw defect; // StructuredTaskScope halts the line
            }
            """,
            "java"
        );

        sayNextSection("The Five Why Root Cause Protocol");

        say(
            "When a Jidoka gate fires a RED signal, the Toyota protocol is Five Why: " +
            "ask 'why' five times until you reach a root cause that can be fixed by a " +
            "system change, not a personnel change. " +
            "In code: the first 'why' is the exception message. " +
            "The second is the stack trace. The third is the input that caused the defect. " +
            "The fourth is the gate specification that was violated. " +
            "The fifth is whether the gate specification was complete."
        );

        var fiveWhy = new LinkedHashMap<String, String>();
        fiveWhy.put("Why 1: Exception",     "SchemaDefect: field 'price', expected Double, got String");
        fiveWhy.put("Why 2: Stack",         "at TokenizerAgent.tokenize(TokenizerAgent.java:87)");
        fiveWhy.put("Why 3: Input",         "Work unit from CSV row 4,201 — price column contains '$42.00' not '42.00'");
        fiveWhy.put("Why 4: Gate spec",     "Gate only validates Double.parseDouble() — does not strip currency symbols");
        fiveWhy.put("Why 5: Root cause",    "Gate specification did not account for currency-formatted price strings from legacy source");
        fiveWhy.put("Fix (Poka-yoke)",      "Add currency-strip normaliser before gate OR update gate spec to accept formatted strings");
        sayKeyValue(fiveWhy);

        sayWarning(
            "The Five Why protocol must reach a system fix, not 'we need to be more careful'. " +
            "'Be more careful' is not a fix — it is a prayer. " +
            "Every Jidoka stop must produce a Poka-yoke (Station 5) that prevents recurrence. " +
            "If you cannot add a compile-time or gate-time constraint to prevent this class of " +
            "defect, you have not finished the Five Why analysis."
        );

        sayOrderedList(List.of(
            "All defect types are declared in the sealed hierarchy at design time — no ad-hoc exceptions",
            "Every gate is a pure function: inspect only, no mutation, no side effects",
            "Every defect throw reaches the Andon board (Station 1) before propagating",
            "CRITICAL defects halt all downstream stations via ShutdownOnFailure",
            "Every Jidoka stop triggers a Five Why analysis and a Poka-yoke fix (Station 5)",
            "Gate coverage is verified by the compiler — no unhandled defect class can reach production"
        ));
    }
}
