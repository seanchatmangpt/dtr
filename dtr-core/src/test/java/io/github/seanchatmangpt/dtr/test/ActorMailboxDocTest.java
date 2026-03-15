package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.actor.ActorMailbox;
import io.github.seanchatmangpt.dtr.actor.ActorMailbox.Actor;
import io.github.seanchatmangpt.dtr.actor.ActorMailbox.ActorReport;
import io.github.seanchatmangpt.dtr.actor.ActorMailbox.Message;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * DTR sayActorMailbox — Joe Armstrong Actor Model message-passing documentation tests.
 *
 * <p>Demonstrates {@link ActorMailbox} by simulating a small actor system with
 * virtual threads (JEP 444). Each actor receives messages, processes them, and
 * optionally produces reply messages. All timings are measured with
 * {@code System.nanoTime()} over real virtual-thread concurrency on Java 26.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ActorMailboxDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — counter / logger / aggregator system
    // =========================================================================

    @Test
    void t1_actor_mailbox_message_passing() {
        sayNextSection("Actor Mailbox \u2014 Joe Armstrong's Message-Passing Concurrency");

        say("""
            Joe Armstrong's actor model treats concurrency as isolated processes that \
            communicate exclusively via message passing \u2014 no shared mutable state, \
            no locks. DTR documents actor systems by simulating real message routing \
            on Java 26 virtual threads (JEP 444), capturing per-actor mailbox traces \
            and processing times with {@code System.nanoTime()}.""");

        sayCode("""
            // Define actors as lambdas implementing the Actor interface
            Actor counter = new Actor() {
                public String name() { return "counter"; }
                public List<Message> receive(Message msg) {
                    long now = System.nanoTime();
                    // Forward a "counted" notification to aggregator
                    return List.of(new Message("counter", "aggregator",
                        "counted:" + msg.payload(), now));
                }
            };
            """, "java");

        // ----- Define actors -----
        Actor counter = new Actor() {
            @Override
            public String name() { return "counter"; }

            @Override
            public List<Message> receive(Message msg) {
                long now = System.nanoTime();
                return List.of(new Message("counter", "aggregator",
                        "counted:" + msg.payload(), now));
            }
        };

        Actor logger = new Actor() {
            @Override
            public String name() { return "logger"; }

            @Override
            public List<Message> receive(Message msg) {
                long now = System.nanoTime();
                // Forward a "logged" event to aggregator
                return List.of(new Message("logger", "aggregator",
                        "logged:" + msg.payload(), now));
            }
        };

        Actor aggregator = new Actor() {
            @Override
            public String name() { return "aggregator"; }

            @Override
            public List<Message> receive(Message msg) {
                // Aggregator is a sink — no replies
                return List.of();
            }
        };

        List<Actor> actors = List.of(counter, logger, aggregator);

        // ----- Seed messages -----
        long t0 = System.nanoTime();
        List<Message> initial = List.of(
                new Message("world", "counter", "event-A", t0),
                new Message("world", "counter", "event-B", t0),
                new Message("world", "logger",  "request-1", t0)
        );

        // ----- Run simulation -----
        long simStart = System.nanoTime();
        ActorReport report = ActorMailbox.simulate("Counter-Logger-Aggregator System", actors, initial);
        long simNs = System.nanoTime() - simStart;

        sayTable(new String[][]{
            {"Metric",          "Value"},
            {"System name",     report.systemName()},
            {"Total messages",  String.valueOf(report.totalMessages())},
            {"Active actors",   String.valueOf(report.actors().size())},
            {"Simulation time", ActorMailbox.humanNs(simNs) + " (Java 26 virtual threads)"},
        });

        sayActorMailbox(report);

        sayNote("""
            Each actor runs on a Java 26 virtual thread (JEP 444). \
            Actors never share state \u2014 all coordination happens through \
            immutable {@code Message} records passed via the mailbox.""");

        sayWarning("Simulation runs at most 3 rounds of message routing. " +
            "Actors that produce replies to actors that produce replies " +
            "will be cut off after round 3 to prevent runaway loops.");

        // ----- Assertions -----
        sayAndAssertThat("totalMessages >= initial message count (3)",
                report.totalMessages(), greaterThanOrEqualTo(3));

        sayAndAssertThat("active actor count is 3",
                report.actors().size(), is(3));
    }

    // =========================================================================
    // Test 2 — humanNs unit documentation
    // =========================================================================

    @Test
    void t2_humanNs_formatting() {
        sayNextSection("Actor Mailbox \u2014 humanNs Time Formatting");

        say("""
            {@code ActorMailbox.humanNs(long ns)} formats raw nanosecond durations \
            into compact, human-readable strings. Three tiers cover the range from \
            nanosecond-precision micro-benchmarks to millisecond-range simulations.""");

        long nsValue  = 42L;
        long usValue  = 8_500L;
        long msValue  = 3_200_000L;

        String nsStr = ActorMailbox.humanNs(nsValue);
        String usStr = ActorMailbox.humanNs(usValue);
        String msStr = ActorMailbox.humanNs(msValue);

        sayTable(new String[][]{
            {"Input (ns)", "Output", "Tier"},
            {String.valueOf(nsValue),  nsStr, "nanoseconds"},
            {String.valueOf(usValue),  usStr, "microseconds"},
            {String.valueOf(msValue),  msStr, "milliseconds"},
        });

        sayAndAssertThat("42 ns formats as '42ns'",
                nsStr, is("42ns"));
        sayAndAssertThat("8500 ns formats as '8µs'",
                usStr, is("8\u00b5s"));
        sayAndAssertThat("3_200_000 ns formats as '3ms'",
                msStr, is("3ms"));
    }
}
