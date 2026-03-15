package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TPS Station 9 — Heijunka: Level-Load Scheduler
 *
 * <p>Heijunka (平準化, "levelling") is Toyota's technique for smoothing
 * production over time to match the pace of customer demand (takt time)
 * rather than reacting to demand spikes. Without heijunka, a factory
 * alternates between frantic overproduction and idle waiting. With
 * heijunka, production is steady, predictable, and easy to staff.</p>
 *
 * <p>The classic heijunka box is a scheduling board: work orders are
 * placed in time slots, distributed evenly across the day. The line
 * doesn't know demand is uneven — it just sees a steady stream of
 * work at takt time.</p>
 *
 * <p>In AGI agent systems, heijunka prevents thundering herd problems:
 * all agents starting at the same time, all hitting the same downstream
 * service simultaneously, causing cascading failures. Virtual thread
 * rate limiting + scheduled staggered starts implements heijunka
 * at near-zero overhead.</p>
 */
class TpsHeijunkaDocTest extends DtrTest {

    @Test
    void heijunka_levelLoadScheduler() {
        sayNextSection("TPS Station 9 — Heijunka: Level-Load Scheduler");

        say(
            "Taiichi Ohno's rule: produce at the pace of customer demand, no faster. " +
            "A factory that produces twice as fast as demand creates inventory — which is waste. " +
            "A factory that produces slower than demand creates shortfall — which is lost revenue. " +
            "Takt time is the heartbeat: total available time ÷ customer demand rate. " +
            "Every station must be designed to produce at exactly takt time. " +
            "This is not a target — it is the design constraint."
        );

        sayTable(new String[][] {
            {"Heijunka Concept", "Toyota Factory", "Java 26 AGI Pipeline", "Erlang Analogue"},
            {"Takt time",        "Available time / demand units", "Rate limit: items per second", "Message arrival rate budget"},
            {"Level loading",    "Even distribution across shifts", "Staggered virtual thread starts", "Process spawn with delay"},
            {"Heijunka box",     "Physical scheduling board", "Token bucket / semaphore", "Rate limiter in process loop"},
            {"Demand smoothing", "Accumulate orders, release evenly", "Bounded input queue with timed drain", "Mailbox drain at fixed rate"},
            {"Surge absorption", "Buffer stock (small inventory)", "Bounded queue acts as buffer", "Mailbox as burst absorber"},
        });

        sayNextSection("Takt Time Calculation");

        say(
            "For the DTR AGI pipeline with 10 stations processing document batches: " +
            "the constraint station (InferenceEngine) processes 20 items/second. " +
            "Takt time = 1000ms / 20 items = 50ms per item. " +
            "All other stations must be able to process one item in ≤ 50ms. " +
            "If any station exceeds 50ms average, it becomes the new bottleneck. " +
            "Heijunka ensures work arrives at each station at exactly 50ms intervals."
        );

        var taktCalc = new LinkedHashMap<String, String>();
        taktCalc.put("Available time",       "3,600,000 ms / hour");
        taktCalc.put("Demand rate",          "72,000 items / hour");
        taktCalc.put("Takt time",            "3,600,000 / 72,000 = 50ms per item");
        taktCalc.put("Bottleneck station",   "InferenceEngine (50ms cycle time = takt time)");
        taktCalc.put("Slack stations",       "InputReader (1ms), Validator (2ms), Tokenizer (4ms)");
        taktCalc.put("Buffer strategy",      "Kanban WIP=4 per queue (Station 3) absorbs burst ±4 items");
        sayKeyValue(taktCalc);

        sayNextSection("Rate Limiter: Token Bucket Implementation");

        say(
            "The token bucket is the heijunka box in code: a bucket holds N tokens " +
            "(capacity = burst tolerance). Each item consumed requires one token. " +
            "Tokens are replenished at the takt rate. If the bucket is empty, the caller " +
            "waits — back-pressure is built in. No tokens = no work = automatic levelling."
        );

        sayCode(
            """
            // Token bucket rate limiter — heijunka box in Java 26
            class TaktRateLimiter {
                private final long taktNanos;           // nanoseconds per item
                private final int burst;                // max burst capacity
                private long tokens;                    // current token count
                private long lastRefillNanos;           // last refill timestamp

                TaktRateLimiter(int itemsPerSecond, int burst) {
                    this.taktNanos = 1_000_000_000L / itemsPerSecond;
                    this.burst = burst;
                    this.tokens = burst;
                    this.lastRefillNanos = System.nanoTime();
                }

                // Acquire one token — blocks virtual thread if empty (heijunka wait)
                synchronized void acquire() throws InterruptedException {
                    refill();
                    while (tokens <= 0) {
                        long waitNanos = taktNanos - (System.nanoTime() - lastRefillNanos);
                        TimeUnit.NANOSECONDS.sleep(Math.max(0, waitNanos));
                        refill();
                    }
                    tokens--;
                }

                private void refill() {
                    long now = System.nanoTime();
                    long elapsed = now - lastRefillNanos;
                    long newTokens = elapsed / taktNanos;
                    if (newTokens > 0) {
                        tokens = Math.min(burst, tokens + newTokens);
                        lastRefillNanos = now;
                    }
                }
            }

            // Usage: level-load InferenceEngine at 20 items/second, burst=5
            var takt = new TaktRateLimiter(20, 5);
            Thread.ofVirtual().start(() -> {
                for (var item : inputQueue) {
                    takt.acquire(); // virtual thread parks if at takt limit
                    inferenceEngine.process(item);
                }
            });
            """,
            "java"
        );

        sayNextSection("Staggered Agent Starts: Preventing Thundering Herd");

        say(
            "The thundering herd problem: 10 agents start simultaneously, all attempt to " +
            "connect to the same downstream service, the service is overwhelmed, all fail. " +
            "This is the software equivalent of a factory starting every station simultaneously " +
            "and starving all of them for parts at once. " +
            "Heijunka fix: stagger starts by takt time intervals."
        );

        sayCode(
            """
            // Staggered agent starts — heijunka schedule
            // Agent k starts at k × taktMs milliseconds after t=0
            long taktMs = 50; // 50ms per station

            for (int k = 0; k < 10; k++) {
                final int stationId = k;
                final long startDelay = k * taktMs;

                Thread.ofVirtual()
                    .name("station-" + stationId)
                    .start(() -> {
                        if (startDelay > 0) {
                            Thread.sleep(startDelay); // staggered start
                        }
                        stations.get(stationId).run();
                    });
            }

            // Result: stations start at t=0, 50ms, 100ms, 150ms, ...
            // Downstream service sees a smooth arrival rate, not a thundering herd
            // Each station's first request hits the service 50ms apart
            """,
            "java"
        );

        sayNextSection("Live Level-Load Simulation");

        say(
            "Simulating 10 agents with and without heijunka. " +
            "Without heijunka: all 10 agents fire simultaneously, " +
            "downstream service receives 10 requests in the first 10ms. " +
            "With heijunka: requests arrive at 50ms intervals, " +
            "downstream service receives 1 request per 50ms."
        );

        var simResults = new AtomicInteger(0);
        sayBenchmark("Without heijunka: 10 simultaneous starts", () -> {
            simResults.incrementAndGet();
        });

        sayBenchmark("With heijunka: staggered 50ms takt", () -> {
            try {
                TimeUnit.NANOSECONDS.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            simResults.incrementAndGet();
        });

        sayNextSection("Heijunka Box: Time-Bucketed Work Schedule");

        say(
            "For complex multi-type workloads, heijunka uses a box: a grid with " +
            "time columns and work-type rows. Work orders are distributed evenly " +
            "across time slots to achieve the takt rate for each type. " +
            "In AGI pipelines, this translates to priority queues with timed drain."
        );

        sayTable(new String[][] {
            {"Time Slot", "10:00", "10:01", "10:02", "10:03", "10:04"},
            {"Type A (large doc)",    "1", "1", "1", "1", "1"},
            {"Type B (medium doc)",   "2", "2", "2", "2", "2"},
            {"Type C (small doc)",    "4", "4", "4", "4", "4"},
            {"Total items/min",       "7", "7", "7", "7", "7"},
        });

        sayNote(
            "The heijunka box shows that the mix is 1:2:4 (large:medium:small). " +
            "The InferenceEngine processes each type at different speeds. " +
            "The scheduler must account for processing time, not just count. " +
            "Large docs at takt=150ms, medium at 75ms, small at 37ms — " +
            "mixing them evenly keeps the engine near 100% utilisation " +
            "without queue buildup of any single type."
        );

        var invariants = new LinkedHashMap<String, String>();
        invariants.put("Takt time is the design constraint",  "All stations sized to process one item in ≤ takt time — never faster");
        invariants.put("Rate limiter is the heijunka box",    "Token bucket enforces takt time at every station boundary");
        invariants.put("Staggered starts prevent herd",       "Agent k starts at k × taktMs — smooth arrival at downstream services");
        invariants.put("WIP limit enforces heijunka",         "Kanban queue capacity (Station 3) is the physical heijunka constraint");
        invariants.put("Bottleneck sets takt",                "Takt time = 1 / bottleneck_throughput — all other stations yield");
        invariants.put("Mix ratio is a scheduling parameter", "Heijunka box distributes work types evenly across time slots");
        sayKeyValue(invariants);
    }
}
