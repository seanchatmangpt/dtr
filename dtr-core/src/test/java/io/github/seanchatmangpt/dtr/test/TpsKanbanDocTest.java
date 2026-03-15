package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TPS Station 3 — Kanban: Virtual Thread Pull System
 *
 * <p>Kanban (看板, "signboard") is Toyota's pull-based scheduling system.
 * Work is never pushed to a station; a station pulls work when it has capacity.
 * The Kanban card is the signal: "I am ready for the next unit." Inventory
 * between stations is bounded by the number of Kanban cards in circulation.
 * When cards run out, upstream stations stop — automatically, without a manager.</p>
 *
 * <p>Joe Armstrong designed Erlang's mailbox as a Kanban queue: messages pile
 * up in the mailbox; the process receives exactly one message at a time when
 * it is ready. No message is ever forced into a process faster than it can
 * consume them. Back-pressure is built in, not bolted on.</p>
 *
 * <p>In Java 26, virtual threads + BlockingQueue implement the Kanban pull
 * system at near-zero cost: one virtual thread per station, each blocking on
 * {@code queue.take()} when idle. No busy-waiting, no explicit synchronization,
 * no thread pool tuning. The JVM parks the virtual thread and unparks it when
 * work arrives — exactly the Kanban signal made mechanical.</p>
 */
class TpsKanbanDocTest extends DtrTest {

    @Test
    void kanban_virtualThreadPullSystem() {
        sayNextSection("TPS Station 3 — Kanban: Virtual Thread Pull System");

        say(
            "The Kanban insight is counterintuitive to engineers raised on batch processing: " +
            "the most efficient production system is one where each station pulls work at its " +
            "own pace, bounded by a small in-process queue. There is no scheduler, no task " +
            "allocator, no work dispatcher. The queue size is the only tuning knob. " +
            "If a downstream station is slow, its Kanban queue fills up, and upstream stations " +
            "stop — automatically — without any explicit coordination."
        );

        sayTable(new String[][] {
            {"Kanban Concept", "Toyota Factory", "Java 26 Implementation", "Erlang Analogue"},
            {"Kanban card",    "Physical card attached to container", "Object in BlockingQueue", "Message in process mailbox"},
            {"WIP limit",      "Number of Kanban cards in circulation", "Queue capacity (bounded)", "Mailbox message limit"},
            {"Pull signal",    "Downstream station empties a container", "queue.take() unblocks virtual thread", "receive clause matches message"},
            {"Stop signal",    "No more Kanban cards — upstream halts", "put() blocks when queue full", "process parks when mailbox empty"},
            {"Replenishment",  "Upstream refills container, returns card", "upstream calls queue.put(item)", "upstream sends new message"},
        });

        sayNextSection("The Three-Queue Pipeline: Real WIP Bounds");

        say(
            "A Kanban pipeline has exactly as many queues as there are handoffs between stations. " +
            "Each queue has a fixed capacity — the WIP (Work In Progress) limit. " +
            "The total WIP in the system is bounded by the sum of all queue capacities. " +
            "This is the mathematical basis for Little's Law: throughput = WIP / cycle time. " +
            "To increase throughput, reduce cycle time — not increase WIP."
        );

        sayCode(
            """
            // Three-station Kanban pipeline, each queue WIP-bounded
            // Virtual threads block on empty queues — zero CPU waste when idle

            var parseQueue    = new ArrayBlockingQueue<RawDocument>(32);   // WIP=32
            var validateQueue = new ArrayBlockingQueue<ParsedDoc>(16);      // WIP=16
            var outputQueue   = new ArrayBlockingQueue<ValidatedDoc>(8);    // WIP=8

            // Station 1: Parser — pulls from input source, pushes to parseQueue
            Thread.ofVirtual().start(() -> {
                while (true) {
                    var raw = source.next();             // pull from upstream
                    var parsed = parser.parse(raw);
                    parseQueue.put(parsed);              // blocks if downstream is full
                }
            });

            // Station 2: Validator — pulls from parseQueue, pushes to validateQueue
            Thread.ofVirtual().start(() -> {
                while (true) {
                    var parsed = parseQueue.take();      // blocks until work arrives
                    var validated = validator.validate(parsed);
                    validateQueue.put(validated);        // blocks if downstream is full
                }
            });

            // Station 3: Output Writer — pulls from validateQueue, writes to sink
            Thread.ofVirtual().start(() -> {
                while (true) {
                    var validated = validateQueue.take(); // blocks until work arrives
                    sink.write(validated);
                }
            });
            """,
            "java"
        );

        sayNote(
            "The virtual thread is the key insight. On platform (OS) threads, blocking on an " +
            "empty queue wastes a thread. On virtual threads (JEP 444, finalized Java 21), " +
            "the JVM unmounts the virtual thread from its carrier thread when it blocks — " +
            "the carrier thread is free to run other virtual threads. " +
            "One million virtual threads blocking on Kanban queues consumes less memory than " +
            "100 platform threads busy-waiting. " +
            "Armstrong's mailbox-receive has the same property: an Erlang process costs ~300 bytes " +
            "when blocked waiting for a message."
        );

        sayNextSection("WIP Limit Sizing: The Little's Law Calculation");

        say(
            "The WIP limit for each queue is not arbitrary. " +
            "Little's Law gives the formula: WIP = Throughput × Cycle Time. " +
            "To keep a station 90% utilised with headroom for bursts, " +
            "set WIP = 2 × (max_burst_rate × station_cycle_time). " +
            "Too large: latency increases (work piles up). " +
            "Too small: upstream stations stall (throughput drops)."
        );

        sayTable(new String[][] {
            {"Station", "Throughput (items/s)", "Cycle Time (ms)", "Min WIP", "Recommended WIP (2×)"},
            {"InputReader",      "1000", "1",   "1",  "4"},
            {"SchemaValidator",  "500",  "2",   "1",  "4"},
            {"TokenizerAgent",   "250",  "4",   "1",  "4"},
            {"ContextAssembler", "100",  "10",  "1",  "4"},
            {"InferenceEngine",  "20",   "50",  "1",  "4"},
            {"OutputFormatter",  "20",   "50",  "1",  "4"},
        });

        sayWarning(
            "InferenceEngine is the bottleneck station (20 items/s). " +
            "This is the constraint in Theory of Constraints terms. " +
            "Increasing WIP upstream of InferenceEngine only increases latency — " +
            "it does not increase throughput. " +
            "The correct response to a bottleneck is to: " +
            "(1) Exploit it (make it as efficient as possible), " +
            "(2) Subordinate all other stations to its pace (takt time = 50ms), " +
            "(3) Elevate it (parallelize with multiple InferenceEngine instances). " +
            "Station 9 (Heijunka) covers step 3."
        );

        sayNextSection("Poison Pill Shutdown Protocol");

        say(
            "Every Kanban pipeline needs a clean shutdown mechanism. " +
            "The Poison Pill pattern: a special sentinel value is placed on the queue " +
            "that tells the consuming station to stop processing. " +
            "Every station passes the pill downstream before exiting — " +
            "the shutdown propagates through the pipeline automatically."
        );

        sayCode(
            """
            // Sentinel — typed null via Optional is cleaner than a null check
            // But a dedicated singleton record is safest with sealed types
            sealed interface WorkItem permits DataItem, PoisonPill {}
            record DataItem(byte[] payload)    implements WorkItem {}
            record PoisonPill()                implements WorkItem {}

            static final PoisonPill STOP = new PoisonPill();

            // Station loop with clean shutdown
            while (true) {
                WorkItem item = queue.take();
                switch (item) {
                    case PoisonPill p -> {
                        downstream.put(STOP); // propagate shutdown
                        return;               // station exits cleanly
                    }
                    case DataItem d -> {
                        process(d);
                        downstream.put(new DataItem(transform(d.payload())));
                    }
                }
            }
            """,
            "java"
        );

        sayNextSection("Back-Pressure Propagation: The Kanban Invariant");

        var invariants = new LinkedHashMap<String, String>();
        invariants.put("Bounded queues",         "Every inter-station queue has a fixed capacity — no unbounded growth");
        invariants.put("Virtual thread per station", "One virtual thread per station — blocked when idle, unparked when work arrives");
        invariants.put("Pull only",              "No station is ever pushed work — all work is pulled via take()");
        invariants.put("WIP = Little's Law",     "Queue capacity = 2 × (throughput × cycle_time) for each station");
        invariants.put("Bottleneck is takt",     "System throughput = bottleneck throughput — all other stations yield to it");
        invariants.put("Poison pill propagates", "Shutdown signal flows downstream through every queue — no orphaned threads");
        sayKeyValue(invariants);

        sayOrderedList(List.of(
            "Virtual threads make Kanban queues nearly free to implement",
            "Back-pressure is automatic — blocked put() is the signal, no explicit flow control",
            "WIP limits prevent memory blowup on burst traffic",
            "Little's Law gives a principled formula for queue capacity",
            "The bottleneck station sets the pace for the entire pipeline (takt time)",
            "Poison pill propagates shutdown cleanly through all stations",
            "The same pattern works for 10 or 10,000 stations — queue API is unchanged"
        ));
    }
}
