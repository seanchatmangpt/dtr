package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * TPS Station 6 — Armstrong Actor Model with Virtual Threads
 *
 * <p>Joe Armstrong's 1986 thesis introduced the actor model to Erlang: each
 * process is an independent unit of computation with its own mailbox. Processes
 * communicate only by sending messages — never by sharing memory. A process can
 * only be killed by its supervisor or by a crash signal. There are no mutexes,
 * no semaphores, no shared data structures. If two processes need to coordinate,
 * they exchange messages.</p>
 *
 * <p>Armstrong's rule: "Concurrency is about dealing with lots of things at once.
 * Parallelism is about doing lots of things at once. They are different." The
 * actor model addresses concurrency — the coordination problem — by eliminating
 * shared state. Parallelism is a consequence, not the goal.</p>
 *
 * <p>In Java 26, virtual threads (JEP 444) + BlockingQueue implement Armstrong's
 * actor model directly: one virtual thread per actor, one BlockingQueue as the
 * mailbox, no shared mutable state. The cost is ~1KB per actor. One million
 * actors is trivially feasible.</p>
 */
class ArmstrongActorDocTest extends DtrTest {

    @Test
    void armstrongActor_virtualThreadActors() {
        sayNextSection("TPS Station 6 — Armstrong Actor Model with Virtual Threads");

        say(
            "Armstrong on why actors work: 'The problem with shared state is that you have " +
            "to think about every possible interleaving of every possible thread accessing " +
            "every possible variable at every possible moment. " +
            "The number of interleavings is factorial in the number of threads. " +
            "With actors: you only think about one sequence of messages for one actor at a time. " +
            "The problem becomes linear, not factorial.'"
        );

        sayTable(new String[][] {
            {"Erlang Process", "Java 26 Actor (Virtual Thread)", "Property"},
            {"spawn(Fun)",     "Thread.ofVirtual().start(runnable)", "Lightweight, one per actor"},
            {"Pid",            "Thread reference or actor ID",       "Identity of the actor"},
            {"Mailbox",        "LinkedBlockingQueue<Message>",        "Bounded or unbounded inbox"},
            {"receive",        "queue.take() — blocks until message", "Park virtual thread when idle"},
            {"send (Pid ! Msg)","actorRef.send(message)",            "Non-blocking put to target mailbox"},
            {"self()",         "Thread.currentThread()",             "Actor's own identity"},
            {"exit(Pid,Reason)","thread.interrupt() or poison pill", "Signal actor to stop"},
        });

        sayNextSection("The Minimal Actor Implementation");

        say(
            "An actor in Java 26 is four things: a virtual thread, a bounded inbox queue, " +
            "a message handler function, and a supervisor reference. Nothing else. " +
            "Armstrong: 'The simplest correct implementation is the best starting point. " +
            "Optimise later. Correctness is not negotiable at any stage.'"
        );

        sayCode(
            """
            // Sealed message protocol (Station 5 poka-yoke)
            sealed interface Msg permits Data, Ping, Shutdown {}
            record Data(String payload)     implements Msg {}
            record Ping(ActorRef replyTo)   implements Msg {}
            record Shutdown()               implements Msg {}

            // Actor reference — the only thing callers hold
            record ActorRef(LinkedBlockingQueue<Msg> mailbox) {
                void send(Msg msg) { mailbox.offer(msg); }  // non-blocking send
            }

            // Actor factory — one virtual thread per actor
            static ActorRef spawn(String name, Consumer<Msg> handler) {
                var mailbox = new LinkedBlockingQueue<Msg>(1024); // bounded WIP=1024
                Thread.ofVirtual()
                    .name(name)
                    .start(() -> {
                        while (true) {
                            Msg msg = mailbox.take(); // park until message arrives
                            if (msg instanceof Shutdown) break;
                            handler.accept(msg);
                        }
                    });
                return new ActorRef(mailbox);
            }

            // Spawn an actor that counts Data messages and replies to Ping
            var counter = new AtomicLong(0);
            ActorRef counterActor = spawn("counter", msg -> switch (msg) {
                case Data d   -> counter.incrementAndGet();
                case Ping  p  -> p.replyTo().send(new Data("count=" + counter.get()));
                case Shutdown s -> {} // handled by outer loop break
            });
            """,
            "java"
        );

        sayNextSection("No Shared State: The Armstrong Invariant");

        say(
            "The single most important invariant in the actor model is also the most violated: " +
            "actors communicate only through messages; no actor ever reads or writes another " +
            "actor's internal state. In Java, this means: no static mutable fields shared " +
            "between actors, no passing mutable objects in messages, no 'shortcut' references. " +
            "If you break this invariant once, the entire concurrency model collapses."
        );

        sayTable(new String[][] {
            {"Pattern", "Status", "Why"},
            {"Immutable records as messages",         "✅ Required",   "Value semantics — no shared mutable state"},
            {"ActorRef as the only shared reference", "✅ Required",   "Callers can only send messages, not inspect"},
            {"static mutable field between actors",   "❌ Forbidden",  "Shared state — destroys isolation"},
            {"Passing List<T> in message",            "❌ Forbidden",  "Caller and actor share the list reference"},
            {"Passing List.copyOf(t) in message",     "✅ Required",   "Defensive copy — actor owns its data"},
            {"Synchronized block inside actor",       "⚠️ Smell",      "Actors should not need locks — review design"},
        });

        sayNextSection("Actor Lifecycle: Spawn → Run → Stop");

        sayCode(
            """
            // Complete actor lifecycle with supervisor notification
            sealed interface ActorEvent permits Started, Completed, Failed {}
            record Started(String name)            implements ActorEvent {}
            record Completed(String name, long processed) implements ActorEvent {}
            record Failed(String name, Throwable cause)   implements ActorEvent {}

            static ActorRef spawnManaged(String name, Consumer<Msg> handler,
                                         ActorRef supervisor) {
                var mailbox = new LinkedBlockingQueue<Msg>(1024);
                Thread.ofVirtual()
                    .name(name)
                    .start(() -> {
                        supervisor.send(new Data("event=started name=" + name));
                        long count = 0;
                        try {
                            while (true) {
                                Msg msg = mailbox.take();
                                if (msg instanceof Shutdown) break;
                                handler.accept(msg);
                                count++;
                            }
                            supervisor.send(new Data("event=completed name=" + name + " count=" + count));
                        } catch (Throwable t) {
                            // Let it crash — notify supervisor, don't try to recover here
                            supervisor.send(new Data("event=failed name=" + name + " reason=" + t.getMessage()));
                        }
                    });
                return new ActorRef(mailbox);
            }
            """,
            "java"
        );

        sayNote(
            "The 'let it crash' philosophy is not about being cavalier with errors. " +
            "It is about recognising that a process in an unknown state is more dangerous " +
            "than a process that does not exist. " +
            "A crashed process has a clean restart. A running process in an inconsistent " +
            "state will produce wrong answers for every request it handles. " +
            "Armstrong: 'A wrong answer is infinitely more dangerous than no answer. ' " +
            "'No answer is detectable. A wrong answer propagates undetected downstream.'"
        );

        sayNextSection("Actor Mesh: 10-Agent Pipeline");

        say(
            "The Toyota Code Production System runs 10 stations in parallel. " +
            "Each station is an actor. The Kanban queues (Station 3) are the actor mailboxes. " +
            "The Andon board (Station 1) is a supervisor actor that receives status messages. " +
            "The entire pipeline is a mesh of actors communicating by message — no locks."
        );

        sayMermaid(
            """
            graph LR
                Source["Source\\n(Feeder)"] -->|Data msg| A1["Actor 1\\nInputReader"]
                A1 -->|Data msg| A2["Actor 2\\nSchemaValidator"]
                A2 -->|Data msg| A3["Actor 3\\nTokenizer"]
                A3 -->|Data msg| A4["Actor 4\\nContextAssembler"]
                A4 -->|Data msg| A5["Actor 5\\nInferenceEngine"]
                A5 -->|Data msg| A6["Actor 6\\nOutputFormatter"]
                A6 -->|Data msg| Sink["Sink\\n(Writer)"]
                A1 & A2 & A3 & A4 & A5 & A6 -->|ActorEvent| Sup["Supervisor\\n(Andon Board)"]
                Sup -->|Restart msg| A1
                Sup -->|Restart msg| A3
            """
        );

        var invariants = new LinkedHashMap<String, String>();
        invariants.put("One virtual thread per actor",   "~1KB overhead per actor — 1M actors feasible on 1GB heap");
        invariants.put("One mailbox per actor",          "LinkedBlockingQueue<Msg> — bounded to prevent unbounded WIP");
        invariants.put("No shared mutable state",        "All messages are immutable records — defensive copies for collections");
        invariants.put("Let it crash",                   "Actor catches Throwable, notifies supervisor, stops — no partial recovery");
        invariants.put("Supervisor notification",        "Every lifecycle event (started, completed, failed) sent to supervisor actor");
        invariants.put("Sealed message protocol",        "All Msg subtypes declared — exhaustive handler required by compiler");
        sayKeyValue(invariants);
    }
}
