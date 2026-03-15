/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

/**
 * Documents the {@code sayActorMessages} innovation in the DTR {@code say*} API.
 *
 * <p>{@code sayActorMessages(String title, List<String> actors, List<String[]> messages)}
 * converts a roster of actor names and a list of {@code [sender, receiver, messageText]}
 * triples into a rendered Mermaid {@code sequenceDiagram} plus a summary table showing
 * actor count and message count. No external diagramming tool, no UML editor, no DSL
 * beyond the plain Java lists already present in the test code.</p>
 *
 * <p>Joe Armstrong designed Erlang and the actor model around one core insight:
 * concurrent systems are easier to reason about when processes share no state and
 * communicate exclusively by message passing. {@code sayActorMessages} applies that
 * same principle to documentation — each message is a first-class fact, and the
 * sequence diagram that results is a verifiable, executable specification of
 * actor-to-actor communication, not a hand-drawn approximation.</p>
 *
 * <p>Three representative actor systems are documented here, each chosen to
 * illustrate a distinct class of message-passing workflow that arises in production
 * distributed systems:</p>
 *
 * <ol>
 *   <li><strong>E-Commerce Order Processing</strong> — the canonical actor model
 *       example from Armstrong's own talks. Client, OrderActor, InventoryActor,
 *       PaymentActor, and ShipmentActor each own a slice of state; they coordinate
 *       exclusively through messages. No shared database row, no global lock.</li>
 *   <li><strong>Real-Time Chat System</strong> — an Erlang-inspired location-transparent
 *       messaging scenario. The same message-passing contract works whether UserA and
 *       UserB are on the same node or opposite sides of the planet.</li>
 *   <li><strong>DTR Rendering Pipeline</strong> — a self-referential example: DTR's
 *       own internal rendering pipeline documented as actors. The document you are
 *       reading was produced by the very pipeline it describes.</li>
 * </ol>
 *
 * <p>Tests execute in alphabetical method-name order ({@code a1_}, {@code a2_},
 * {@code a3_}) to establish a clear narrative progression in the generated document
 * from "what is sayActorMessages" through production chat systems to DTR's own
 * architecture.</p>
 *
 * @see io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands#sayActorMessages
 * @since 2026.8.0
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ActorMessagesDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // a1 — E-Commerce Order Processing
    // =========================================================================

    /**
     * Documents the e-commerce order processing actor system.
     *
     * <p>Joe Armstrong's insight was that eliminating shared state eliminates an
     * entire class of concurrency bugs. This actor network — Client, OrderActor,
     * InventoryActor, PaymentActor, ShipmentActor — has no shared mutable data.
     * Every coordination step is a message. The sequence diagram below is the
     * complete communication specification for placing one order.</p>
     */
    @Test
    void a1_sayActorMessages_order_processing() {
        sayNextSection("sayActorMessages — E-Commerce Order Processing");

        say(
            "Joe Armstrong observed that most concurrency bugs are not bugs in the " +
            "concurrency machinery itself — they are bugs in the shared mutable state " +
            "that concurrent threads compete over. His remedy was radical: ban sharing " +
            "entirely. Actors own their state exclusively. When one actor needs " +
            "information from another, it sends a message and waits for a reply. " +
            "There are no locks, no monitors, no volatile fields, no race windows."
        );

        say(
            "The e-commerce order pipeline below is the textbook demonstration of this " +
            "principle. Client sends a single {@code place_order} message. OrderActor " +
            "coordinates the reservation, payment, and shipment sub-systems through " +
            "discrete messages and collects their replies before reporting back to " +
            "Client. If PaymentActor crashes mid-charge, its supervisor restarts it " +
            "with no corrupted shared state to clean up — because there was none."
        );

        sayCode("""
                // Declare actors and messages, then render.
                List<String> actors = List.of(
                    "Client", "OrderActor", "InventoryActor", "PaymentActor", "ShipmentActor"
                );

                List<String[]> messages = List.of(
                    new String[]{"Client",        "OrderActor",     "place_order(item=SKU-42)"},
                    new String[]{"OrderActor",    "InventoryActor", "reserve(SKU-42, qty=1)"},
                    new String[]{"InventoryActor","OrderActor",     "{reserved, ok}"},
                    new String[]{"OrderActor",    "PaymentActor",   "charge(amount=99.00)"},
                    new String[]{"PaymentActor",  "OrderActor",     "{charged, ok}"},
                    new String[]{"OrderActor",    "ShipmentActor",  "ship(address=...)"},
                    new String[]{"OrderActor",    "Client",         "{order_confirmed, id=7891}"}
                );

                sayActorMessages("E-Commerce Order Processing", actors, messages);
                """, "java");

        say(
            "Each message in the list is a {@code String[3]}: sender, receiver, and " +
            "message text. The render machine declares every actor as a Mermaid " +
            "{@code participant}, then emits one arrow per message. The result is a " +
            "verifiable specification: the test cannot pass if the actor list or " +
            "message list is structurally incorrect."
        );

        sayNote(
            "The reply from OrderActor to Client — {@code {order_confirmed, id=7891}} " +
            "— uses Erlang tuple syntax deliberately. Armstrong's insight was that " +
            "tagged tuples make protocol errors impossible to ignore at the call site. " +
            "A message that is never pattern-matched causes a function-clause exception " +
            "rather than silent corruption."
        );

        List<String> orderActors = List.of(
            "Client", "OrderActor", "InventoryActor", "PaymentActor", "ShipmentActor"
        );

        List<String[]> orderMessages = List.of(
            new String[]{"Client",         "OrderActor",     "place_order(item=SKU-42)"},
            new String[]{"OrderActor",     "InventoryActor", "reserve(SKU-42, qty=1)"},
            new String[]{"InventoryActor", "OrderActor",     "{reserved, ok}"},
            new String[]{"OrderActor",     "PaymentActor",   "charge(amount=99.00)"},
            new String[]{"PaymentActor",   "OrderActor",     "{charged, ok}"},
            new String[]{"OrderActor",     "ShipmentActor",  "ship(address=...)"},
            new String[]{"OrderActor",     "Client",         "{order_confirmed, id=7891}"}
        );

        long start = System.nanoTime();
        sayActorMessages("E-Commerce Order Processing", orderActors, orderMessages);
        long renderNs = System.nanoTime() - start;

        sayTable(new String[][] {
            {"Actor",          "Role",                          "Messages sent", "Messages received"},
            {"Client",         "External request originator",   "1",             "1"},
            {"OrderActor",     "Saga coordinator",              "3",             "3"},
            {"InventoryActor", "Stock reservation service",     "1",             "1"},
            {"PaymentActor",   "Charge processing service",     "1",             "1"},
            {"ShipmentActor",  "Dispatch and logistics",        "0",             "1"},
        });

        sayKeyValue(new java.util.LinkedHashMap<>(java.util.Map.of(
            "sayActorMessages render time", renderNs + " ns",
            "Java version",                System.getProperty("java.version"),
            "Actor count",                 String.valueOf(orderActors.size()),
            "Message count",               String.valueOf(orderMessages.size()),
            "Pattern",                     "Request-reply saga with synchronous sub-steps"
        )));

        sayWarning(
            "ShipmentActor receives a {@code ship} message but never replies in this " +
            "trace. In a production actor system that omission is intentional: " +
            "shipment is fire-and-forget from the order saga's perspective. Delivery " +
            "confirmation arrives asynchronously, hours later, via a separate " +
            "{@code delivery_confirmed} message routed directly to Client. " +
            "Documenting the asynchronous follow-up requires a second " +
            "{@code sayActorMessages} call covering that interaction."
        );
    }

    // =========================================================================
    // a2 — Real-Time Chat System
    // =========================================================================

    /**
     * Documents an Erlang-inspired real-time chat actor system.
     *
     * <p>Erlang's original killer application was the Ericsson AXD301 telephone
     * switch, which achieved nine-nines availability by treating every call as
     * an isolated actor. The same location-transparent message passing that made
     * telephone calls reliable in 1986 applies equally to chat messages in 2026.
     * This test documents that contract.</p>
     */
    @Test
    void a2_sayActorMessages_chat_system() {
        sayNextSection("sayActorMessages — Real-Time Chat System");

        say(
            "Location transparency is one of the most powerful properties of the " +
            "actor model. From UserA's perspective, sending a chat message to UserB " +
            "is identical whether UserB is running on the same JVM, a different " +
            "server in the same data centre, or a handset on the other side of the " +
            "planet. The message is addressed to a process identifier — the PID or " +
            "actor reference — and the runtime routes it. The sending actor never " +
            "needs to know where the receiving actor lives."
        );

        say(
            "The chat session below models a four-actor system: UserA and UserB are " +
            "client-side actors representing connected sessions; ChatServer is the " +
            "message broker; PresenceActor tracks online/offline status. The sequence " +
            "covers connection establishment, presence notification, message delivery, " +
            "and delivery acknowledgement — the complete happy-path protocol for one " +
            "message in a real-time chat system."
        );

        sayCode("""
                List<String> chatActors = List.of(
                    "UserA", "ChatServer", "UserB", "PresenceActor"
                );

                List<String[]> chatMessages = List.of(
                    new String[]{"UserA",         "ChatServer",    "connect(token)"},
                    new String[]{"ChatServer",     "PresenceActor", "online(UserA)"},
                    new String[]{"PresenceActor",  "ChatServer",    "{status, online}"},
                    new String[]{"ChatServer",     "UserA",         "{connected}"},
                    new String[]{"UserA",          "ChatServer",    "send(to=UserB, Hello)"},
                    new String[]{"ChatServer",     "UserB",         "deliver(from=UserA, Hello)"},
                    new String[]{"UserB",          "ChatServer",    "ack(msg_id=1)"}
                );

                sayActorMessages("Real-Time Chat System", chatActors, chatMessages);
                """, "java");

        say(
            "Two sub-protocols are interleaved in this trace. The connection protocol " +
            "occupies the first four messages: UserA authenticates, ChatServer notifies " +
            "PresenceActor, PresenceActor confirms, ChatServer acknowledges. The " +
            "messaging protocol occupies the last three: send, deliver, acknowledge. " +
            "Both sub-protocols are stateless from the outside — any actor can be " +
            "restarted between messages without corrupting the protocol, because all " +
            "state is encoded in the messages themselves."
        );

        sayNote(
            "The {@code ack(msg_id=1)} from UserB back to ChatServer enables at-least-once " +
            "delivery. If ChatServer crashes before receiving the ack, it restarts with " +
            "the message still in its mailbox and retransmits. If UserB crashes before " +
            "sending the ack, ChatServer retransmits on reconnection. In neither case " +
            "is there shared mutable state to repair — only a retransmission."
        );

        List<String> chatActors = List.of(
            "UserA", "ChatServer", "UserB", "PresenceActor"
        );

        List<String[]> chatMessages = List.of(
            new String[]{"UserA",        "ChatServer",    "connect(token)"},
            new String[]{"ChatServer",   "PresenceActor", "online(UserA)"},
            new String[]{"PresenceActor","ChatServer",    "{status, online}"},
            new String[]{"ChatServer",   "UserA",         "{connected}"},
            new String[]{"UserA",        "ChatServer",    "send(to=UserB, Hello)"},
            new String[]{"ChatServer",   "UserB",         "deliver(from=UserA, Hello)"},
            new String[]{"UserB",        "ChatServer",    "ack(msg_id=1)"}
        );

        long start = System.nanoTime();
        sayActorMessages("Real-Time Chat System", chatActors, chatMessages);
        long renderNs = System.nanoTime() - start;

        sayTable(new String[][] {
            {"Actor",         "Role",                            "Messages sent", "Messages received"},
            {"UserA",         "Connecting client session",       "2",             "1"},
            {"ChatServer",    "Central message broker",          "3",             "3"},
            {"UserB",         "Receiving client session",        "1",             "1"},
            {"PresenceActor", "Online/offline status tracker",   "1",             "1"},
        });

        sayKeyValue(new java.util.LinkedHashMap<>(java.util.Map.of(
            "sayActorMessages render time", renderNs + " ns",
            "Java version",                System.getProperty("java.version"),
            "Actor count",                 String.valueOf(chatActors.size()),
            "Message count",               String.valueOf(chatMessages.size()),
            "Delivery guarantee",          "At-least-once via ack(msg_id)"
        )));

        sayWarning(
            "PresenceActor is a shared service — both UserA and UserB sessions route " +
            "through it. In an Erlang system this would be a named registered process, " +
            "not a unique PID per session. Scaling PresenceActor beyond a single node " +
            "requires a distributed registry (Mnesia, etcd, or equivalent). " +
            "The actor model does not eliminate distribution complexity — it isolates it " +
            "inside PresenceActor rather than scattering it across the codebase."
        );
    }

    // =========================================================================
    // a3 — DTR Rendering Pipeline
    // =========================================================================

    /**
     * Documents DTR's own rendering pipeline as actors — a self-referential example.
     *
     * <p>The document you are reading was produced by the pipeline described in this
     * test. TestRunner invokes DtrTest methods; DtrTest delegates to RenderMachine;
     * RenderMachine instructs FileWriter. Each boundary is a message-passing boundary:
     * no component reaches into another's internal state.</p>
     */
    @Test
    void a3_sayActorMessages_dtr_pipeline() {
        sayNextSection("sayActorMessages — DTR Rendering Pipeline");

        say(
            "Every DTR test run is itself an actor system. TestRunner (JUnit 5) " +
            "dispatches test methods to DtrTest instances. Each {@code say*} call " +
            "is a message from DtrTest to RenderMachine. RenderMachine accumulates " +
            "rendered content and delegates final I/O to FileWriter, which flushes " +
            "each output format to disk. No component reaches into another's state: " +
            "DtrTest does not touch FileWriter's buffer, and RenderMachine does not " +
            "know which test class invoked it."
        );

        say(
            "Documenting DTR's own pipeline with {@code sayActorMessages} is " +
            "self-referential in the best sense: the primitive designed to make " +
            "actor-model communication legible is itself used to make its own " +
            "communication legible. The sequence diagram below is the exact protocol " +
            "that produced this document. Readers can trace every arrow back to " +
            "a method call in the DTR source."
        );

        sayCode("""
                List<String> pipelineActors = List.of(
                    "TestRunner", "DtrTest", "RenderMachine", "FileWriter"
                );

                List<String[]> pipelineMessages = List.of(
                    new String[]{"TestRunner",    "DtrTest",       "execute(a3_...)"},
                    new String[]{"DtrTest",       "RenderMachine", "say(paragraph)"},
                    new String[]{"DtrTest",       "RenderMachine", "sayActorMessages(...)"},
                    new String[]{"RenderMachine", "FileWriter",    "flush(docs/test/...md)"},
                    new String[]{"FileWriter",    "DtrTest",       "{written, ok}"}
                );

                sayActorMessages("DTR Rendering Pipeline", pipelineActors, pipelineMessages);
                """, "java");

        say(
            "The message {@code sayActorMessages(...)} in this trace is the call " +
            "that produced the diagram you are viewing. It is both a message in the " +
            "sequence diagram and the Java statement that rendered that diagram. " +
            "This is not a coincidence — it is the consequence of DTR's design " +
            "principle that documentation is produced by running the code, not by " +
            "describing it separately."
        );

        say(
            "The final message — {@code {written, ok}} from FileWriter back to DtrTest " +
            "— completes the round trip. In the actual DTR implementation this reply " +
            "is implicit: {@code finishAndWriteOut()} in the {@code @AfterAll} hook " +
            "returns normally on success. Making it explicit in the actor diagram " +
            "clarifies the ownership boundary: DtrTest is responsible for triggering " +
            "the flush, but FileWriter is responsible for confirming it succeeded."
        );

        List<String> pipelineActors = List.of(
            "TestRunner", "DtrTest", "RenderMachine", "FileWriter"
        );

        List<String[]> pipelineMessages = List.of(
            new String[]{"TestRunner",    "DtrTest",       "execute(a3_...)"},
            new String[]{"DtrTest",       "RenderMachine", "say(paragraph)"},
            new String[]{"DtrTest",       "RenderMachine", "sayActorMessages(...)"},
            new String[]{"RenderMachine", "FileWriter",    "flush(docs/test/...md)"},
            new String[]{"FileWriter",    "DtrTest",       "{written, ok}"}
        );

        long start = System.nanoTime();
        sayActorMessages("DTR Rendering Pipeline", pipelineActors, pipelineMessages);
        long renderNs = System.nanoTime() - start;

        sayTable(new String[][] {
            {"Actor",         "DTR Class / Component",         "Messages sent", "Messages received"},
            {"TestRunner",    "JUnit 5 engine",                "1",             "0"},
            {"DtrTest",       "DtrTest (abstract base class)", "2",             "1"},
            {"RenderMachine", "RenderMachineImpl",             "1",             "2"},
            {"FileWriter",    "RenderMachine I/O layer",       "1",             "1"},
        });

        sayKeyValue(new java.util.LinkedHashMap<>(java.util.Map.of(
            "sayActorMessages render time", renderNs + " ns",
            "Java version",                System.getProperty("java.version"),
            "Actor count",                 String.valueOf(pipelineActors.size()),
            "Message count",               String.valueOf(pipelineMessages.size()),
            "Output formats",              "Markdown, LaTeX, HTML, JSON"
        )));

        sayNote(
            "The two messages from DtrTest to RenderMachine — {@code say(paragraph)} " +
            "and {@code sayActorMessages(...)} — represent the full narrative of this " +
            "test method compressed to two entries. In reality this method makes " +
            "several more {@code say*} calls that are omitted from the actor trace " +
            "for clarity. The actor diagram documents the protocol structure, not " +
            "every individual invocation."
        );

        sayWarning(
            "RenderMachine is not a pure actor in the strict Erlang sense — it is a " +
            "plain Java object that accumulates rendered content in an in-memory list " +
            "and flushes it synchronously at {@code @AfterAll}. The actor metaphor " +
            "is appropriate for reasoning about ownership and message boundaries, but " +
            "DTR does not use an actual actor runtime (Akka, Vert.x, or similar). " +
            "If concurrent documentation generation becomes a requirement, replacing " +
            "RenderMachine with a virtual-thread-per-test-class actor would require " +
            "no changes to the {@code say*} API contract."
        );
    }
}
