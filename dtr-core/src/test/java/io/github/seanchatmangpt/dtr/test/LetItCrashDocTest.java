package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner;
import io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner.NamedProcess;
import io.github.seanchatmangpt.dtr.erlang.LetItCrashRunner.SupervisionReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.greaterThan;

/**
 * DTR sayLetItCrash — Joe Armstrong's "Let It Crash" supervision documentation tests.
 *
 * <p>Demonstrates {@link LetItCrashRunner} by supervising a mix of stable and crashing
 * processes in isolated virtual threads. Each crashing process triggers recorded
 * restart attempts. The supervision report is rendered via {@code sayLetItCrash()},
 * proving the philosophy in live, measurable code — not estimates.</p>
 *
 * <p>All timing figures are measured with {@link System#nanoTime()} over real
 * virtual-thread execution on Java 26.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class LetItCrashDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — Mixed stable and crashing processes
    // =========================================================================

    @Test
    void t1_mixed_stable_and_crashing_processes() {
        sayNextSection("Let It Crash — Joe Armstrong's Process Isolation Philosophy");

        say("""
            Joe Armstrong's Erlang mantra "Let It Crash" inverts defensive programming: \
            instead of guarding every failure path, each process is isolated so that crashes \
            are contained and automatically restarted by a supervisor. \
            DTR documents this philosophy with real measurements: each process runs in its own \
            virtual thread (JEP 444), crashes are recorded with nanosecond precision, and the \
            supervisor retries up to 3 times before giving up.""");

        sayCode("""
            // Define a mix of stable and unstable processes
            var processes = List.of(
                new NamedProcess("database-writer",   () -> writeToDb()),
                new NamedProcess("flaky-http-client", () -> { throw new RuntimeException("timeout"); }),
                new NamedProcess("metrics-collector", () -> collectMetrics()),
                new NamedProcess("bad-parser",        () -> { throw new IllegalStateException("parse error"); }),
                new NamedProcess("cache-warmer",      () -> warmCache())
            );

            SupervisionReport report = LetItCrashRunner.supervise("my-supervisor", processes);
            sayLetItCrash(report);
            """, "java");

        AtomicInteger dbWrites = new AtomicInteger(0);
        AtomicInteger metricsCollected = new AtomicInteger(0);
        AtomicInteger cacheWarmed = new AtomicInteger(0);

        List<NamedProcess> processes = List.of(
            new NamedProcess("database-writer",
                () -> dbWrites.incrementAndGet()),
            new NamedProcess("flaky-http-client",
                () -> { throw new RuntimeException("connection timeout after 30s"); }),
            new NamedProcess("metrics-collector",
                () -> metricsCollected.incrementAndGet()),
            new NamedProcess("bad-parser",
                () -> { throw new IllegalStateException("unexpected token at position 42"); }),
            new NamedProcess("cache-warmer",
                () -> cacheWarmed.incrementAndGet())
        );

        long start = System.nanoTime();
        SupervisionReport report = LetItCrashRunner.supervise("my-supervisor", processes);
        long elapsedNs = System.nanoTime() - start;

        sayLetItCrash(report);

        sayKeyValue(java.util.Map.of(
            "Supervisor", report.supervisorName(),
            "Total processes", String.valueOf(processes.size()),
            "Total crashes", String.valueOf(report.totalCrashes()),
            "Total restarts", String.valueOf(report.totalRestarts()),
            "Total elapsed", LetItCrashRunner.humanNs(report.totalNs()),
            "Wall-clock", LetItCrashRunner.humanNs(elapsedNs),
            "Java version", System.getProperty("java.version")
        ));

        sayNote("""
            Two processes crash on every attempt (flaky-http-client and bad-parser). \
            Each receives up to 3 restart attempts before the supervisor gives up. \
            The stable processes (database-writer, metrics-collector, cache-warmer) \
            complete on the first attempt.""");

        sayWarning("""
            In a real Erlang/OTP system the supervisor would use a restart strategy \
            (one-for-one, one-for-all, rest-for-one) and a restart intensity limit \
            (max N restarts in M seconds) before escalating to the parent supervisor. \
            DTR's LetItCrashRunner demonstrates the core isolation principle — \
            not a production-grade supervision tree.""");

        sayAndAssertThat("Supervisor name matches",
            report.supervisorName(), is("my-supervisor"));
        sayAndAssertThat("At least one crash recorded",
            report.totalCrashes(), greaterThan(0));
        sayAndAssertThat("Restarts triggered by crashes",
            report.totalRestarts(), greaterThan(0));
        sayAndAssertThat("Results list is non-empty",
            report.results().isEmpty(), is(false));
    }

    // =========================================================================
    // Test 2 — All stable processes (no crashes)
    // =========================================================================

    @Test
    void t2_all_stable_processes_no_crashes() {
        sayNextSection("Let It Crash — All-Stable Supervision (Zero Crashes)");

        say("""
            When all processes complete without error the supervisor reports zero crashes \
            and zero restarts. This is the happy-path baseline that confirms the runner \
            correctly distinguishes stable from unstable processes.""");

        AtomicInteger counter = new AtomicInteger(0);

        List<NamedProcess> processes = List.of(
            new NamedProcess("process-alpha", counter::incrementAndGet),
            new NamedProcess("process-beta",  counter::incrementAndGet),
            new NamedProcess("process-gamma", counter::incrementAndGet)
        );

        SupervisionReport report = LetItCrashRunner.supervise("stable-supervisor", processes);

        sayLetItCrash(report);

        sayAndAssertThat("Zero crashes for stable processes",
            report.totalCrashes(), is(0));
        sayAndAssertThat("Zero restarts needed",
            report.totalRestarts(), is(0));
        sayAndAssertThat("All three processes produced a result row",
            report.results().size(), is(3));
        sayAndAssertThat("All outcomes are ok",
            report.results().stream().allMatch(r -> "ok".equals(r.outcome())), is(true));
    }

    // =========================================================================
    // Test 3 — Single always-crashing process (exhausts all restarts)
    // =========================================================================

    @Test
    void t3_always_crashing_process_exhausts_restarts() {
        sayNextSection("Let It Crash — Restart Exhaustion");

        say("""
            A process that always throws will be restarted up to 3 times. \
            After all restart attempts are exhausted the supervisor records 4 crash results \
            (1 initial start + 3 restarts) for that process and moves on. \
            This validates that the restart limit is enforced.""");

        List<NamedProcess> processes = List.of(
            new NamedProcess("always-boom",
                () -> { throw new RuntimeException("BOOM"); })
        );

        SupervisionReport report = LetItCrashRunner.supervise("retry-supervisor", processes);

        sayLetItCrash(report);

        sayCode("""
            // Expected: 4 crash results — start + restart-1 + restart-2 + restart-3
            // Each with outcome "crash"
            assert report.totalCrashes()  == 4;
            assert report.totalRestarts() == 3;
            assert report.results().size() == 4;
            """, "java");

        sayAndAssertThat("Four crash results for always-crashing process (1 start + 3 restarts)",
            report.results().size(), is(4));
        sayAndAssertThat("Total crashes equals 4",
            report.totalCrashes(), is(4));
        sayAndAssertThat("Total restarts equals 3",
            report.totalRestarts(), is(3));
        sayAndAssertThat("All results are crash outcomes",
            report.results().stream().allMatch(r -> "crash".equals(r.outcome())), is(true));
    }
}
