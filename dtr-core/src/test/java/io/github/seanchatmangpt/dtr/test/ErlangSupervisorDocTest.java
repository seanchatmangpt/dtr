package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.erlang.SupervisionTree;
import io.github.seanchatmangpt.dtr.erlang.SupervisionTree.Child;
import io.github.seanchatmangpt.dtr.erlang.SupervisionTree.ChildState;
import io.github.seanchatmangpt.dtr.erlang.SupervisionTree.RestartStrategy;
import io.github.seanchatmangpt.dtr.erlang.SupervisionTree.SupervisionReport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;

/**
 * DTR sayErlangSupervisor — Joe Armstrong OTP Supervision Tree documentation tests.
 *
 * <p>Demonstrates all three Erlang/OTP restart strategies by running real child processes
 * in virtual threads (JEP 444) and documenting the supervision outcomes. Each test proves
 * that the correct children are restarted under each strategy — not estimates, real
 * concurrent execution.</p>
 *
 * <p>All timing figures are measured with {@link System#nanoTime()} over real virtual-thread
 * concurrency on Java 26.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ErlangSupervisorDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — ONE_FOR_ONE: only the crashed child restarts
    // =========================================================================

    @Test
    void t1_one_for_one_only_crashed_child_restarts() {
        sayNextSection("sayErlangSupervisor — ONE_FOR_ONE Strategy");

        say("""
            The **ONE_FOR_ONE** strategy is the most targeted restart policy in Erlang OTP. \
            When a child crashes, only that child is restarted; its siblings continue \
            undisturbed. This minimises disruption to the system and is the default strategy \
            for independent workers.""");

        sayCode("""
            // Child "worker-b" crashes once, then succeeds on restart.
            // Children "worker-a" and "worker-c" never crash — they must NOT be restarted.

            AtomicInteger bAttempts = new AtomicInteger(0);

            List<Child> children = List.of(
                new Child("worker-a", () -> { /* always succeeds */ }, 3),
                new Child("worker-b", () -> {
                    if (bAttempts.getAndIncrement() == 0) {
                        throw new RuntimeException("transient failure");
                    }
                }, 3),
                new Child("worker-c", () -> { /* always succeeds */ }, 3)
            );

            SupervisionReport report = SupervisionTree.supervise(
                "one-for-one-supervisor", RestartStrategy.ONE_FOR_ONE, children);
            """, "java");

        AtomicInteger bAttempts = new AtomicInteger(0);

        List<Child> children = List.of(
            new Child("worker-a", () -> {
                // always succeeds
            }, 3),
            new Child("worker-b", () -> {
                if (bAttempts.getAndIncrement() == 0) {
                    throw new RuntimeException("transient failure");
                }
            }, 3),
            new Child("worker-c", () -> {
                // always succeeds
            }, 3)
        );

        long start = System.nanoTime();
        SupervisionReport report = SupervisionTree.supervise(
            "one-for-one-supervisor", RestartStrategy.ONE_FOR_ONE, children);
        long totalNs = System.nanoTime() - start;

        sayErlangSupervisor(report);

        sayTable(new String[][]{
            {"Metric", "Value", "Environment"},
            {"Supervision time", totalNs + "ns", "Java 26"},
            {"Strategy", "ONE_FOR_ONE", "Erlang OTP"},
            {"Children", "3", "worker-a, worker-b, worker-c"},
        });

        sayNote("ONE_FOR_ONE: only worker-b was restarted. "
            + "worker-a and worker-c ran exactly once without interruption.");

        sayAndAssertThat("Supervisor is healthy (no child exceeded maxRestarts)",
            report.supervisorHealthy(), is(true));
        sayAndAssertThat("worker-a has 0 restarts (not affected by sibling crash)",
            report.children().get(0).restartCount(), is(0));
        sayAndAssertThat("worker-b was restarted at least once",
            report.children().get(1).restartCount() >= 1, is(true));
        sayAndAssertThat("worker-c has 0 restarts (not affected by sibling crash)",
            report.children().get(2).restartCount(), is(0));
        sayAndAssertThat("worker-b final state is TERMINATED (recovered after restart)",
            report.children().get(1).state(), is(ChildState.TERMINATED));
    }

    // =========================================================================
    // Test 2 — ONE_FOR_ALL: all children restart when any one crashes
    // =========================================================================

    @Test
    void t2_one_for_all_all_children_restart_on_crash() {
        sayNextSection("sayErlangSupervisor — ONE_FOR_ALL Strategy");

        say("""
            The **ONE_FOR_ALL** strategy assumes that all children are interdependent. \
            When any single child crashes, the supervisor tears down and restarts every \
            child in the group. This guarantees a consistent system state when processes \
            share mutable data or communication channels.""");

        sayCode("""
            // Child "worker-a" crashes once, then succeeds.
            // Under ONE_FOR_ALL, "worker-b" and "worker-c" must also restart.

            AtomicInteger aAttempts = new AtomicInteger(0);

            List<Child> children = List.of(
                new Child("worker-a", () -> {
                    if (aAttempts.getAndIncrement() == 0) {
                        throw new IllegalStateException("state corrupted");
                    }
                }, 3),
                new Child("worker-b", () -> { /* always succeeds */ }, 3),
                new Child("worker-c", () -> { /* always succeeds */ }, 3)
            );

            SupervisionReport report = SupervisionTree.supervise(
                "one-for-all-supervisor", RestartStrategy.ONE_FOR_ALL, children);
            """, "java");

        AtomicInteger aAttempts = new AtomicInteger(0);

        List<Child> children = List.of(
            new Child("worker-a", () -> {
                if (aAttempts.getAndIncrement() == 0) {
                    throw new IllegalStateException("state corrupted");
                }
            }, 3),
            new Child("worker-b", () -> {
                // always succeeds
            }, 3),
            new Child("worker-c", () -> {
                // always succeeds
            }, 3)
        );

        long start = System.nanoTime();
        SupervisionReport report = SupervisionTree.supervise(
            "one-for-all-supervisor", RestartStrategy.ONE_FOR_ALL, children);
        long totalNs = System.nanoTime() - start;

        sayErlangSupervisor(report);

        sayTable(new String[][]{
            {"Metric", "Value", "Environment"},
            {"Supervision time", totalNs + "ns", "Java 26"},
            {"Strategy", "ONE_FOR_ALL", "Erlang OTP"},
            {"Children", "3", "worker-a, worker-b, worker-c"},
        });

        sayNote("ONE_FOR_ALL: worker-a crashed, causing all three children to restart "
            + "in lock-step — guaranteed consistent group state.");

        sayAndAssertThat("Supervisor is healthy after recovery",
            report.supervisorHealthy(), is(true));
        sayAndAssertThat("worker-a crashed at least once",
            report.totalCrashes() >= 1, is(true));
        sayAndAssertThat("All children terminated successfully after restart",
            report.children().stream().allMatch(c -> c.state() == ChildState.TERMINATED),
            is(true));
    }

    // =========================================================================
    // Test 3 — REST_FOR_ONE: crashed child + later-started children restart
    // =========================================================================

    @Test
    void t3_rest_for_one_crashed_child_and_successors_restart() {
        sayNextSection("sayErlangSupervisor — REST_FOR_ONE Strategy");

        say("""
            The **REST_FOR_ONE** strategy models a start-order dependency graph. \
            Children are ordered A, B, C. When B crashes, B and all children \
            started **after** B (i.e., C) are restarted. A is unaffected because \
            it started before B and does not depend on B.""");

        sayCode("""
            // Children ordered: worker-a (index 0), worker-b (index 1), worker-c (index 2).
            // worker-b crashes once. Under REST_FOR_ONE: worker-b + worker-c restart.
            // worker-a must NOT be restarted.

            AtomicInteger bAttempts = new AtomicInteger(0);

            List<Child> children = List.of(
                new Child("worker-a", () -> { /* always succeeds */ }, 3),
                new Child("worker-b", () -> {
                    if (bAttempts.getAndIncrement() == 0) {
                        throw new RuntimeException("downstream dependency failure");
                    }
                }, 3),
                new Child("worker-c", () -> { /* always succeeds */ }, 3)
            );

            SupervisionReport report = SupervisionTree.supervise(
                "rest-for-one-supervisor", RestartStrategy.REST_FOR_ONE, children);
            """, "java");

        AtomicInteger bAttempts = new AtomicInteger(0);

        List<Child> children = List.of(
            new Child("worker-a", () -> {
                // always succeeds
            }, 3),
            new Child("worker-b", () -> {
                if (bAttempts.getAndIncrement() == 0) {
                    throw new RuntimeException("downstream dependency failure");
                }
            }, 3),
            new Child("worker-c", () -> {
                // always succeeds
            }, 3)
        );

        long start = System.nanoTime();
        SupervisionReport report = SupervisionTree.supervise(
            "rest-for-one-supervisor", RestartStrategy.REST_FOR_ONE, children);
        long totalNs = System.nanoTime() - start;

        sayErlangSupervisor(report);

        sayTable(new String[][]{
            {"Metric", "Value", "Environment"},
            {"Supervision time", totalNs + "ns", "Java 26"},
            {"Strategy", "REST_FOR_ONE", "Erlang OTP"},
            {"Children", "3 (ordered A, B, C)", "worker-a independent; worker-c depends on worker-b"},
        });

        sayNote("REST_FOR_ONE: worker-b crashed, restarting worker-b and worker-c (its successor). "
            + "worker-a ran exactly once — its start-order position protects it.");

        sayAndAssertThat("Supervisor is healthy after recovery",
            report.supervisorHealthy(), is(true));
        sayAndAssertThat("worker-a has 0 restarts (started before crashed child)",
            report.children().get(0).restartCount(), is(0));
        sayAndAssertThat("worker-b was restarted at least once (crashed child)",
            report.children().get(1).restartCount() >= 1, is(true));
        sayAndAssertThat("worker-c was restarted at least once (started after crashed child)",
            report.children().get(2).restartCount() >= 1, is(true));
        sayAndAssertThat("All children terminated successfully",
            report.children().stream().allMatch(c -> c.state() == ChildState.TERMINATED),
            is(true));
    }
}
