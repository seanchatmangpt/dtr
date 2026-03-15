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
package io.github.seanchatmangpt.dtr.actor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Joe Armstrong–style Actor Model mailbox simulation for DTR documentation.
 *
 * <p>Models lightweight actors that communicate exclusively via message passing —
 * the core concurrency abstraction from Erlang. Each actor has a name, a mailbox
 * of incoming messages, and produces reply messages. Simulation runs on Java 26
 * virtual threads (one per actor per round).</p>
 *
 * <p>The static factory method {@link #simulate} drives up to 3 rounds of
 * message routing. Timing is measured with {@code System.nanoTime()} for
 * reproducible documentation.</p>
 *
 * @since 2026.1.0
 */
public final class ActorMailbox {

    private ActorMailbox() {
        // utility class — no instances
    }

    // =========================================================================
    // Public record types
    // =========================================================================

    /**
     * An immutable message sent between actors.
     *
     * @param from     sender actor name
     * @param to       recipient actor name
     * @param payload  arbitrary message content
     * @param sentNs   wall-clock timestamp from {@code System.nanoTime()} at send time
     */
    public record Message(String from, String to, Object payload, long sentNs) {}

    /**
     * Trace of a single actor's mailbox activity across the simulation.
     *
     * @param actorName     the actor's name
     * @param received      messages delivered to this actor's mailbox
     * @param sent          reply messages produced by this actor
     * @param processingNs  total nanoseconds spent processing all received messages
     */
    public record MessageTrace(
            String actorName,
            List<Message> received,
            List<Message> sent,
            long processingNs) {}

    /**
     * Full report of an actor-system simulation run.
     *
     * @param systemName    label for this actor system
     * @param actors        per-actor traces (one entry per actor that received at least one message)
     * @param totalMessages total messages delivered across all rounds
     * @param totalNs       total wall-clock time for the simulation
     */
    public record ActorReport(
            String systemName,
            List<MessageTrace> actors,
            int totalMessages,
            long totalNs) {}

    // =========================================================================
    // Actor interface
    // =========================================================================

    /**
     * A named, stateless actor that processes one incoming message and returns
     * zero or more reply messages.
     *
     * <p>Implementations must be thread-safe because {@link #simulate} may call
     * {@code receive} from multiple virtual threads concurrently.</p>
     */
    public interface Actor {

        /** The globally unique name of this actor within the system. */
        String name();

        /**
         * Process an incoming message and return reply messages.
         *
         * @param msg the incoming message (guaranteed non-null)
         * @return list of reply messages (may be empty, never null)
         */
        List<Message> receive(Message msg);
    }

    // =========================================================================
    // Simulation engine
    // =========================================================================

    /**
     * Runs a message-passing simulation for {@code systemName}.
     *
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Build a name-to-actor lookup map.</li>
     *   <li>Route each initial message to its named recipient actor.</li>
     *   <li>Each actor runs on a virtual thread; replies become next-round messages.</li>
     *   <li>Repeat for at most 3 rounds of routing.</li>
     *   <li>Accumulate per-actor traces; return the final {@link ActorReport}.</li>
     * </ol>
     *
     * @param systemName      label for the system (appears in documentation)
     * @param actors          the actors participating in the simulation
     * @param initialMessages the seed messages that start the simulation
     * @return a complete {@link ActorReport} with timing and trace data
     */
    public static ActorReport simulate(
            String systemName,
            List<Actor> actors,
            List<Message> initialMessages) {

        Map<String, Actor> actorMap = new ConcurrentHashMap<>();
        for (Actor a : actors) {
            actorMap.put(a.name(), a);
        }

        // Per-actor accumulated traces
        Map<String, CopyOnWriteArrayList<Message>> receivedMap = new ConcurrentHashMap<>();
        Map<String, CopyOnWriteArrayList<Message>> sentMap = new ConcurrentHashMap<>();
        Map<String, Long> processingNsMap = new ConcurrentHashMap<>();

        for (Actor a : actors) {
            receivedMap.put(a.name(), new CopyOnWriteArrayList<>());
            sentMap.put(a.name(), new CopyOnWriteArrayList<>());
            processingNsMap.put(a.name(), 0L);
        }

        long simulationStart = System.nanoTime();

        List<Message> currentRound = new ArrayList<>(initialMessages);
        int totalMessages = 0;
        final int MAX_ROUNDS = 3;

        try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int round = 0; round < MAX_ROUNDS && !currentRound.isEmpty(); round++) {
                List<Message> nextRound = Collections.synchronizedList(new ArrayList<>());
                List<Future<?>> futures = new ArrayList<>();

                for (Message msg : currentRound) {
                    Actor recipient = actorMap.get(msg.to());
                    if (recipient == null) {
                        continue; // dead-letter — no actor with that name
                    }

                    totalMessages++;
                    receivedMap.get(msg.to()).add(msg);

                    futures.add(exec.submit(() -> {
                        long t0 = System.nanoTime();
                        List<Message> replies = recipient.receive(msg);
                        long elapsed = System.nanoTime() - t0;

                        processingNsMap.merge(recipient.name(), elapsed, Long::sum);

                        if (replies != null) {
                            for (Message reply : replies) {
                                sentMap.get(recipient.name()).add(reply);
                                nextRound.add(reply);
                            }
                        }
                    }));
                }

                // Wait for all virtual threads in this round
                for (Future<?> f : futures) {
                    try {
                        f.get(5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }

                currentRound = new ArrayList<>(nextRound);
            }
        }

        long totalNs = System.nanoTime() - simulationStart;

        // Build per-actor MessageTrace list (only actors with mailbox activity)
        List<MessageTrace> traces = new ArrayList<>();
        for (Actor a : actors) {
            List<Message> recv = List.copyOf(receivedMap.get(a.name()));
            List<Message> sent = List.copyOf(sentMap.get(a.name()));
            if (!recv.isEmpty() || !sent.isEmpty()) {
                traces.add(new MessageTrace(
                        a.name(),
                        recv,
                        sent,
                        processingNsMap.getOrDefault(a.name(), 0L)));
            }
        }

        return new ActorReport(systemName, List.copyOf(traces), totalMessages, totalNs);
    }

    // =========================================================================
    // Human-readable timing helper
    // =========================================================================

    /**
     * Formats a nanosecond duration as a compact human-readable string.
     *
     * <ul>
     *   <li>{@code < 1_000} ns  → {@code "Xns"}</li>
     *   <li>{@code < 1_000_000} ns  → {@code "Xµs"}</li>
     *   <li>otherwise  → {@code "Xms"}</li>
     * </ul>
     *
     * @param ns duration in nanoseconds
     * @return compact string representation
     */
    public static String humanNs(long ns) {
        if (ns < 1_000L) {
            return ns + "ns";
        } else if (ns < 1_000_000L) {
            return (ns / 1_000L) + "µs";
        } else {
            return (ns / 1_000_000L) + "ms";
        }
    }
}
