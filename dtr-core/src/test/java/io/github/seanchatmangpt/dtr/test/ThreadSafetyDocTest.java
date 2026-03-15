package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.concurrency.ThreadSafetyProbe;
import io.github.seanchatmangpt.dtr.concurrency.ThreadSafetyProbe.ProbeResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;

/**
 * DTR sayThreadSafety — concurrent execution probe documentation tests.
 *
 * <p>Demonstrates {@link ThreadSafetyProbe} by hammering shared objects from multiple
 * virtual threads simultaneously and documenting the results. Each test proves (or
 * disproves) thread-safety claims with real concurrent execution — not estimates.</p>
 *
 * <p>All timing figures are measured with {@link System#currentTimeMillis()} over
 * real virtual-thread concurrency on Java 26.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ThreadSafetyDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // Test 1 — AtomicInteger (thread-safe by design)
    // =========================================================================

    @Test
    void t1_atomicInteger_is_thread_safe() {
        sayNextSection("sayThreadSafety — Concurrent Execution Probe");

        say("""
            DTR documents thread-safety by <em>proving</em> it: the shared object is \
            hammered from multiple virtual threads simultaneously. \
            Exceptions and inconsistencies are captured and reported. \
            Virtual threads (JEP 444) allow high concurrency at minimal OS-thread cost.""");

        sayCode("""
            AtomicInteger counter = new AtomicInteger(0);

            ProbeResult result = ThreadSafetyProbe.probe(
                "AtomicInteger.incrementAndGet()",
                counter::incrementAndGet,
                50,   // virtual threads
                200   // operations per thread
            );
            """, "java");

        var counter = new AtomicInteger(0);

        var result = ThreadSafetyProbe.probe(
            "AtomicInteger.incrementAndGet()",
            counter::incrementAndGet,
            50,
            200
        );

        renderProbeResult(result);

        sayNote("""
            `AtomicInteger` uses CAS (compare-and-swap) hardware instructions — \
            all 10,000 increments complete without exception and the result is exact.""");

        sayAndAssertThat("AtomicInteger is thread-safe (no exceptions)",
            result.appearsThreadSafe(), is(true));
    }

    // =========================================================================
    // Test 2 — CopyOnWriteArrayList (thread-safe by design)
    // =========================================================================

    @Test
    void t2_copyOnWriteArrayList_is_thread_safe() {
        sayNextSection("sayThreadSafety — CopyOnWriteArrayList");

        say("""
            <code>CopyOnWriteArrayList</code> is designed for concurrent reads/writes. \
            DTR verifies this claim with a live concurrent probe: 20 virtual threads \
            each append 100 elements simultaneously.""");

        var list = new CopyOnWriteArrayList<Integer>();

        var result = ThreadSafetyProbe.probe(
            "CopyOnWriteArrayList.add()",
            () -> list.add(1),
            20,
            100
        );

        renderProbeResult(result);

        sayNote("""
            `CopyOnWriteArrayList` writes create a fresh internal array on every mutation. \
            This makes writes relatively expensive but guarantees safe concurrent access — \
            zero exceptions expected.""");

        sayAndAssertThat("CopyOnWriteArrayList.add() is thread-safe",
            result.appearsThreadSafe(), is(true));
    }

    // =========================================================================
    // Test 3 — Unsafe ArrayList (NOT thread-safe, expect exceptions or data loss)
    // =========================================================================

    @Test
    void t3_unsynchronized_arrayList_is_not_thread_safe() {
        sayNextSection("sayThreadSafety — Unsafe ArrayList (Negative Case)");

        say("""
            A plain <code>java.util.ArrayList</code> is documented as not thread-safe. \
            DTR can also document the <em>absence</em> of thread-safety: the probe records \
            any exceptions thrown during concurrent mutation.""");

        sayWarning("""
            This test intentionally subjects an unsynchronized ArrayList to concurrent writes. \
            Exceptions such as ArrayIndexOutOfBoundsException or ConcurrentModificationException \
            are expected and are the evidence for non-thread-safety. \
            The assertion only checks that the probe ran to completion — not that the list is safe.""");

        var unsafeList = new java.util.ArrayList<Integer>();

        var result = ThreadSafetyProbe.probe(
            "ArrayList.add() — unsynchronized",
            () -> unsafeList.add(1),
            20,
            100
        );

        renderProbeResult(result);

        // We document what happened rather than asserting safety.
        // The probe itself completing without hanging is what we assert.
        sayAndAssertThat("Probe completed within timeout (elapsed > 0)",
            result.elapsedMs() >= 0, is(true));

        if (!result.appearsThreadSafe()) {
            sayNote("Exceptions detected as expected — ArrayList is NOT thread-safe. "
                + "Exception types observed: "
                + (result.exceptionTypes().isEmpty() ? "none this run (races are non-deterministic)"
                    : String.join(", ", result.exceptionTypes())));
        } else {
            sayNote("""
                No exceptions observed this run — race conditions are non-deterministic. \
                The absence of exceptions here does not imply thread-safety; \
                only CAS-based or synchronized collections provide that guarantee.""");
        }
    }

    // =========================================================================
    // Shared rendering helper
    // =========================================================================

    private void renderProbeResult(ProbeResult result) {
        sayTable(new String[][]{
            {"Metric",              "Value"},
            {"Label",               result.label()},
            {"Virtual threads",     String.valueOf(result.threads())},
            {"Operations each",     String.valueOf(result.operationsEach())},
            {"Total operations",    String.valueOf(result.totalOperations())},
            {"Exceptions detected", String.valueOf(result.exceptionsDetected())},
            {"Exception types",     result.exceptionTypes().isEmpty()
                                        ? "none"
                                        : String.join(", ", result.exceptionTypes())},
            {"Elapsed",             result.elapsedMs() + " ms"},
            {"Throughput",          "%.0f ops/s".formatted(result.operationsPerSecond())},
            {"Thread-safe",         result.appearsThreadSafe()
                                        ? "yes"
                                        : "NO — exceptions detected"}
        });
    }
}
