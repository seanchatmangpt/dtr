package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * TPS Station 5 — Poka-yoke: Mistake-Proofing via Sealed Classes
 *
 * <p>Poka-yoke (ポカヨケ, "mistake-proofing") is a Toyota mechanism that makes it
 * physically impossible to perform a step incorrectly. A bolt with an asymmetric
 * cross-section can only be inserted in the correct orientation. A connector with
 * different pin spacings can only mate with the right socket. You do not need to
 * train workers to avoid mistakes — you design the tooling so the mistake cannot
 * be made.</p>
 *
 * <p>In software, the type system is the tooling. Shigeo Shingo (inventor of
 * poka-yoke) would have loved Java 26 sealed classes: a sealed interface with
 * exhaustive pattern matching makes it impossible to write code that ignores a
 * variant. The compiler enforces completeness. The mistake cannot be made.</p>
 *
 * <p>Joe Armstrong built poka-yoke into Erlang's type system implicitly: function
 * clauses must be exhaustive or the compiler warns; unhandled message types cause
 * loud crashes, not silent wrong answers. The design philosophy is identical —
 * use the language to make the wrong code unwritable.</p>
 */
class TpsPokayokeDocTest extends DtrTest {

    @Test
    void pokayoke_mistakeProofingViaSealedClasses() {
        sayNextSection("TPS Station 5 — Poka-yoke: Mistake-Proofing via Sealed Classes");

        say(
            "Shingo's key insight: error prevention is cheaper than error correction at any " +
            "scale. A worker who installs a component backwards costs 5 seconds to correct — " +
            "unless the component has already been welded, painted, and shipped. " +
            "Then the cost is a recall. The asymmetric bolt is free. The recall is not. " +
            "In software: a type error is free (compiler catches it). A production bug is not."
        );

        sayTable(new String[][] {
            {"Poka-yoke Level", "Toyota Example", "Java 26 Equivalent", "Enforcement Point"},
            {"Level 1: Warning",  "Light flashes on wrong assembly",   "Compiler warning (unchecked)", "Compile time"},
            {"Level 2: Control",  "Machine stops on wrong orientation", "Compiler error (sealed)", "Compile time"},
            {"Level 3: Shutdown", "Line halts, alarm fires",           "Jidoka gate throws", "Runtime (Station 2)"},
        });

        sayNote(
            "Level 2 (Control) is the target for all API design in the Toyota Code Production " +
            "System. If a misuse cannot be expressed in the type system, it will eventually be " +
            "expressed as a production incident. " +
            "Armstrong: 'The goal of a type system is to make the wrong code unwritable. ' " +
            "'If your type system cannot prevent the most common class of bug in your domain, ' " +
            "'you have the wrong type system.'"
        );

        sayNextSection("Case 1: Agent State Machine — Impossible Transitions");

        say(
            "An AGI agent moves through states: IDLE → RUNNING → STOPPED or FAILED. " +
            "Without a sealed hierarchy, code can call agent.process() on a STOPPED agent " +
            "and get a NullPointerException at runtime. With a sealed hierarchy, calling " +
            "process() on a non-RUNNING agent is a compile error."
        );

        sayCode(
            """
            // WITHOUT poka-yoke: any method can be called on any state
            class Agent {
                enum State { IDLE, RUNNING, STOPPED, FAILED }
                State state;
                void process(WorkItem item) { /* accidentally called on STOPPED */ }
            }

            // WITH poka-yoke: method is only available on the correct state
            sealed interface AgentState permits Idle, Running, Stopped, Failed {}
            record Idle()                    implements AgentState {}
            record Running(WorkQueue queue)  implements AgentState {}
            record Stopped(Duration runtime) implements AgentState {}
            record Failed(Throwable cause)   implements AgentState {}

            // process() only exists on Running — cannot be called on other states
            static void process(Running agent, WorkItem item) {
                agent.queue().offer(item);
            }

            // The caller must prove the agent is Running before calling process:
            switch (agentState) {
                case Running r  -> process(r, item);           // compiles
                case Idle i     -> throw new IllegalStateException("not started");
                case Stopped s  -> log("agent stopped after " + s.runtime());
                case Failed f   -> supervisor.restart(f.cause());
                // Compiler verifies exhaustion — no default needed
            }
            """,
            "java"
        );

        sayNextSection("Case 2: Work Unit Lifecycle — No Raw Bytes After Parsing");

        say(
            "A common mistake in pipeline code: pass raw bytes to a stage that expects " +
            "a parsed document. With poka-yoke, the raw bytes type is distinct from the " +
            "parsed type — they are not assignment-compatible. The mistake cannot be made."
        );

        sayCode(
            """
            // Sealed work unit hierarchy — each stage has its own type
            sealed interface WorkUnit permits RawBytes, ParsedDoc, ValidatedDoc, EnrichedDoc {}
            record RawBytes    (byte[] data)                     implements WorkUnit {}
            record ParsedDoc   (String id, Map<String,Object> fields) implements WorkUnit {}
            record ValidatedDoc(String id, Schema schema, Map<String,Object> fields) implements WorkUnit {}
            record EnrichedDoc (String id, Schema schema, Map<String,Object> fields,
                                Map<String,Object> enrichments)  implements WorkUnit {}

            // Station 1: Parser — takes RawBytes, returns ParsedDoc
            ParsedDoc parse(RawBytes raw) { ... }

            // Station 2: Validator — takes ParsedDoc, returns ValidatedDoc
            ValidatedDoc validate(ParsedDoc doc) { ... }

            // Station 3: Enricher — takes ValidatedDoc, returns EnrichedDoc
            EnrichedDoc enrich(ValidatedDoc doc) { ... }

            // Cannot pass RawBytes to validate() — compile error, not runtime NPE:
            // ValidatedDoc v = validate(new RawBytes("...")); // ERROR: incompatible types
            """,
            "java"
        );

        sayNextSection("Case 3: Message Protocol — No Unknown Message Types");

        say(
            "Armstrong's Erlang poka-yoke: function clauses for every known message type. " +
            "In Java 26, the equivalent is a sealed interface for the message protocol. " +
            "A new message type added to the protocol must be handled everywhere the protocol " +
            "is pattern-matched — or the code does not compile."
        );

        sayCode(
            """
            // Sealed message protocol — every message type is enumerated
            sealed interface AgentMessage permits
                Start, Stop, Pause, Resume, InjectWork, QueryStatus, Shutdown {}

            record Start()                         implements AgentMessage {}
            record Stop(String reason)             implements AgentMessage {}
            record Pause(Duration maxPauseTime)    implements AgentMessage {}
            record Resume()                        implements AgentMessage {}
            record InjectWork(WorkUnit unit)       implements AgentMessage {}
            record QueryStatus()                   implements AgentMessage {}
            record Shutdown(boolean graceful)      implements AgentMessage {}

            // Message handler — exhaustive by construction
            String handle(AgentMessage msg) {
                return switch (msg) {
                    case Start     s -> agent.start();
                    case Stop      s -> agent.stop(s.reason());
                    case Pause     p -> agent.pause(p.maxPauseTime());
                    case Resume    r -> agent.resume();
                    case InjectWork w -> agent.inject(w.unit());
                    case QueryStatus q -> agent.status().toString();
                    case Shutdown   s -> agent.shutdown(s.graceful());
                    // No default — compiler enforces exhaustion
                };
            }

            // When a new message type (e.g., Checkpoint) is added to the sealed interface,
            // every switch above fails to compile until the new case is added.
            // This is Level 2 poka-yoke: the mistake cannot be made silently.
            """,
            "java"
        );

        sayNextSection("Poka-yoke Design Checklist");

        sayTable(new String[][] {
            {"Question", "If No", "If Yes (Poka-yoke in Place)"},
            {"Can this method be called on the wrong state?",     "Add sealed state, move method to correct subtype", "✓ sealed AgentState restricts call site"},
            {"Can raw data be passed where parsed data is needed?","Add distinct record types per lifecycle stage",   "✓ WorkUnit sealed hierarchy prevents this"},
            {"Can a new message type be added without handling?",  "Add sealed protocol, switch over it",             "✓ sealed AgentMessage compile-fails on new type"},
            {"Can two valid values be confused (e.g. userId vs orderId)?", "Add NewType wrappers", "✓ record UserId(String val) vs OrderId(String val)"},
            {"Can null be passed where a value is required?",     "Use Optional<T> or require non-null records",      "✓ record components are non-null by default"},
        });

        var patternsApplied = new LinkedHashMap<String, String>();
        patternsApplied.put("Sealed state machine",    "Compiler enforces valid state transitions — no runtime state check needed");
        patternsApplied.put("Typed work units",        "Each pipeline stage has its own record type — no raw Object or byte[]");
        patternsApplied.put("Sealed message protocol", "Exhaustive pattern match — new message type = compile error in all handlers");
        patternsApplied.put("NewType primitives",      "record UserId(String val) prevents passing userId where orderId is needed");
        patternsApplied.put("No null",                 "Record components are non-null — Optional<T> for absent values");
        sayKeyValue(patternsApplied);

        sayWarning(
            "Every use of instanceof, unchecked cast, or Object parameter in a public API " +
            "is a poka-yoke violation. It means the wrong code is expressible. " +
            "The fix is always a sealed type, a generic bound, or a record component. " +
            "These are not style preferences — they are load-bearing constraints that prevent " +
            "classes of defects from ever reaching the Jidoka gate (Station 2)."
        );

        sayOrderedList(List.of(
            "Every public API method has argument types that prevent misuse at compile time",
            "Every state machine uses a sealed interface — no enum with conditional methods",
            "Every pipeline stage has a distinct input and output type — no shared raw types",
            "Every message protocol is sealed — adding a new type fails to compile until handled",
            "No null parameters — Optional<T> for absence, non-null records for presence",
            "No instanceof checks in application code — sealed + pattern match only"
        ));
    }
}
