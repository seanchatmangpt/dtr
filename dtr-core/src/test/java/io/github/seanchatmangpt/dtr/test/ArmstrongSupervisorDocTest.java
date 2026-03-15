package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * TPS Station 7 — Armstrong Supervisor Tree: Hierarchical Fault Tolerance
 *
 * <p>Armstrong's supervisor tree is the most important architectural pattern in
 * Erlang/OTP. Every process is owned by a supervisor. Every supervisor knows
 * the restart strategy for its children. A failure propagates up the tree until
 * it reaches a supervisor with a restart strategy that can handle it. At the
 * top of the tree is the application supervisor — if it fails, the application
 * restarts from scratch. Nothing is left running in an unknown state.</p>
 *
 * <p>The restart strategies are precise and finite:
 * - one_for_one: restart only the failed child
 * - one_for_all: restart all children when any one fails
 * - rest_for_one: restart the failed child and all children started after it
 * These three strategies cover every coordination dependency between children.</p>
 *
 * <p>In Java 26, StructuredTaskScope is the supervisor. ShutdownOnFailure is
 * one_for_all. ShutdownOnSuccess is first_wins. A custom Joiner is rest_for_one
 * or any domain-specific strategy. The JVM finally has a tree-structured
 * concurrency model that Armstrong would recognise.</p>
 */
class ArmstrongSupervisorDocTest extends DtrTest {

    @Test
    void armstrongSupervisor_hierarchicalFaultTolerance() {
        sayNextSection("TPS Station 7 — Armstrong Supervisor Tree: Hierarchical Fault Tolerance");

        say(
            "Armstrong's supervisor tree solves the fundamental problem of concurrent systems: " +
            "what do you do when a process fails? The answer is not 'try to fix it inline' — " +
            "that leads to code that tries to recover from errors it was not designed to handle, " +
            "producing systems in unknown states. " +
            "The answer is: let the process fail, notify the supervisor, and let the supervisor " +
            "apply the correct restart strategy. The process's job is correctness; the " +
            "supervisor's job is recovery. These are different jobs."
        );

        sayTable(new String[][] {
            {"Erlang Strategy", "Java 26 Equivalent", "When to Use", "Toyota Analogue"},
            {"one_for_one",  "Custom Joiner: restart only failed subtask", "Children are independent", "Replace one broken jig, others continue"},
            {"one_for_all",  "StructuredTaskScope.ShutdownOnFailure",      "Children share state — all must restart", "Full station reset when any part fails"},
            {"rest_for_one", "Custom Joiner: shutdown subtasks started after failure", "Children depend on startup order", "Re-run downstream stations after upstream fix"},
            {"first_wins",   "StructuredTaskScope.ShutdownOnSuccess",      "Racing for first correct result",          "First worker to complete a task takes it"},
        });

        sayNextSection("The Three-Level Supervisor Tree");

        say(
            "A production supervisor tree has three levels: the Application Supervisor at the " +
            "top, Station Supervisors in the middle (one per pipeline station), and Worker " +
            "Actors at the leaves. A leaf failure propagates to its station supervisor. " +
            "The station supervisor decides: restart the worker (transient), restart all " +
            "workers in this station (one_for_all), or escalate to the application supervisor. " +
            "The application supervisor restarts the entire station."
        );

        sayMermaid(
            """
            graph TD
                AppSup["Application Supervisor\\none_for_one"]
                InSup["InputStation Supervisor\\none_for_one"]
                ProcSup["ProcessStation Supervisor\\none_for_all"]
                OutSup["OutputStation Supervisor\\none_for_one"]
                W1["Worker: InputReader"]
                W2["Worker: Validator"]
                W3["Worker: Tokenizer\\n(failed → restart)"]
                W4["Worker: ContextAssembler"]
                W5["Worker: InferenceEngine"]
                W6["Worker: OutputFormatter"]
                W7["Worker: Writer"]

                AppSup --> InSup
                AppSup --> ProcSup
                AppSup --> OutSup
                InSup --> W1
                InSup --> W2
                ProcSup --> W3
                ProcSup --> W4
                ProcSup --> W5
                OutSup --> W6
                OutSup --> W7

                style W3 fill:#ff9999
            """
        );

        sayNextSection("StructuredTaskScope as one_for_all Supervisor");

        sayCode(
            """
            // one_for_all: if any subtask fails, cancel all siblings
            // This is ShutdownOnFailure — the default Erlang one_for_all strategy
            try (var scope = StructuredTaskScope.open(
                    Joiner.<WorkResult>awaitAllSuccessfulOrThrow())) {

                var t1 = scope.fork(() -> inputReader.read(batch));
                var t2 = scope.fork(() -> validator.validate(batch));
                var t3 = scope.fork(() -> tokenizer.tokenize(batch));

                scope.join(); // blocks until all complete or any throws

                // If t2 threw, t1 and t3 were cancelled — consistent state guaranteed
                process(t1.get(), t2.get(), t3.get());

            } catch (ExecutionException e) {
                // Root cause is e.getCause() — the actual failing subtask's exception
                andonBoard.signal(RED, e.getCause());
                // Supervisor decides: restart the scope (retry) or escalate
            }
            """,
            "java"
        );

        sayNextSection("StructuredTaskScope as rest_for_one Supervisor");

        say(
            "rest_for_one is the dependency-aware strategy: when station N fails, " +
            "restart station N and all stations started after N (N+1, N+2, ...). " +
            "Stations started before N (N-1, N-2, ...) are unaffected. " +
            "This models pipeline dependencies: if the parser fails, the validator " +
            "and enricher must restart — but the reader can continue."
        );

        sayCode(
            """
            // rest_for_one via custom Joiner
            // When subtask at index K fails, cancel all subtasks with index > K
            class RestForOneJoiner<T> implements StructuredTaskScope.Joiner<T, List<T>> {
                private final List<Subtask<? extends T>> subtasks = new CopyOnWriteArrayList<>();
                private volatile int failedIndex = Integer.MAX_VALUE;

                @Override
                public boolean onComplete(Subtask<? extends T> subtask) {
                    int idx = subtasks.indexOf(subtask);
                    if (subtask.state() == Subtask.State.FAILED) {
                        failedIndex = idx;
                        // Cancel all subtasks with index > failedIndex
                        for (int i = idx + 1; i < subtasks.size(); i++) {
                            subtasks.get(i).cancel(); // rest_for_one shutdown
                        }
                        return true; // signal scope to stop joining
                    }
                    return false;
                }

                @Override
                public List<T> result() throws ExecutionException {
                    if (failedIndex < Integer.MAX_VALUE) {
                        throw new ExecutionException("station " + failedIndex + " failed",
                            subtasks.get(failedIndex).exception());
                    }
                    return subtasks.stream().map(Subtask::get).toList();
                }
            }
            """,
            "java"
        );

        sayNextSection("Restart Intensity and Frequency Limits");

        say(
            "Armstrong's OTP supervisor has two critical parameters: maxR (maximum restarts) " +
            "and maxT (time window in seconds). If a supervisor restarts a child more than " +
            "maxR times in maxT seconds, the supervisor itself fails — it cannot fix the " +
            "child, so it escalates to its own supervisor. " +
            "This prevents infinite restart loops and ensures that genuine unrecoverable " +
            "failures propagate up the tree quickly."
        );

        sayCode(
            """
            // Restart intensity limiter — prevents infinite restart loops
            record RestartLimits(int maxRestarts, Duration window) {}

            class SupervisorWithLimits {
                private final RestartLimits limits;
                private final Deque<Instant> restartHistory = new ArrayDeque<>();

                boolean canRestart() {
                    var now = Instant.now();
                    var cutoff = now.minus(limits.window());
                    restartHistory.removeIf(t -> t.isBefore(cutoff));
                    if (restartHistory.size() >= limits.maxRestarts()) {
                        // Exceeded intensity — escalate to parent supervisor
                        return false;
                    }
                    restartHistory.addLast(now);
                    return true;
                }

                void supervise(Callable<Void> child) {
                    while (true) {
                        try {
                            child.call();
                            return; // normal exit
                        } catch (Throwable t) {
                            if (!canRestart()) {
                                throw new SupervisorIntensityExceeded(t);
                            }
                            log.warn("restarting child after failure: {}", t.getMessage());
                        }
                    }
                }
            }
            """,
            "java"
        );

        sayNextSection("The Five Restart Strategies: Selection Guide");

        sayTable(new String[][] {
            {"Strategy",        "Use When",                                    "Example in TPS Pipeline"},
            {"one_for_one",     "Children are independent, no shared state",   "OutputFormatter fails — only restart it"},
            {"one_for_all",     "Children share a transaction or batch",       "InferenceEngine fails — restart all processStation workers"},
            {"rest_for_one",    "Children depend on startup order",            "Parser fails — restart Parser, Validator, Enricher (not Reader)"},
            {"first_wins",      "Racing for first correct result",             "Multiple InferenceEngines, take the first to respond"},
            {"escalate",        "Exceeded restart intensity",                  "Station Supervisor restarts 5x/30s → fails up to Application Supervisor"},
        });

        var invariants = new LinkedHashMap<String, String>();
        invariants.put("Every actor has a supervisor",    "No unparented actors — orphaned threads are the Java equivalent of a zombie process");
        invariants.put("Supervisors don't do work",       "A supervisor's only job is to monitor children and apply restart strategies");
        invariants.put("Restart intensity is bounded",    "maxR/maxT limits prevent infinite loops — escalate when exceeded");
        invariants.put("Failure is the fast path",        "let it crash is faster than defensive error handling — measure both");
        invariants.put("State is in the actor, not shared","Restart is clean because the actor owns all its state — no external cleanup needed");
        sayKeyValue(invariants);

        sayWarning(
            "A supervisor tree with a single level (all workers under one root supervisor) " +
            "is not a tree — it is a star topology. When the root supervisor's intensity " +
            "is exceeded, the entire system restarts. " +
            "A real tree has intermediate supervisors that contain failures locally. " +
            "An InferenceEngine failure should never restart the InputReader. " +
            "If it does, your supervisor tree has the wrong shape."
        );
    }
}
