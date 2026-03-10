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
package org.r10r.doctester.jotp.acme;

import org.acme.CrashRecovery;
import org.acme.ExitSignal;
import org.acme.Proc;
import org.acme.ProcessRegistry;
import org.acme.ProcRef;
import org.acme.ProcTimer;
import org.acme.Result;
import org.acme.Supervisor;
import org.junit.After;
import org.junit.Test;
import org.r10r.doctester.DocTester;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * DocTester documentation for JOTP (Java Open Telecom Protocol) OTP Primitives.
 *
 * <p>Sourced from <a href="https://github.com/seanchatmangpt/java-maven-template">
 * seanchatmangpt/java-maven-template</a> (git submodule at {@code jotp/}).
 *
 * <p>Covers: {@link Proc}, {@link ProcRef}, {@link ProcTimer}, {@link ProcessRegistry},
 * {@link Supervisor}, {@link CrashRecovery}, {@link ExitSignal}.
 */
public class JotpOtpPrimitivesDocTest extends DocTester {

    @After
    public void cleanRegistry() {
        ProcessRegistry.reset();
    }

    // =========================================================================
    // Proc — lightweight virtual-thread process
    // =========================================================================

    @Test
    public void testProcTellAndAsk() throws Exception {
        sayNextSection("Proc — Lightweight Virtual-Thread Process (OTP spawn/3)");
        say("A <code>Proc&lt;S,M&gt;</code> is the Java equivalent of an Erlang process: "
                + "a virtual thread with a private mailbox and a pure state-handler function. "
                + "State is never shared — every message produces a new state value.");
        say("<code>tell(msg)</code> is fire-and-forget (Erlang's <code>!</code> send). "
                + "<code>ask(msg)</code> is request-reply, returning a "
                + "<code>CompletableFuture&lt;S&gt;</code> that resolves to the state "
                + "<em>after</em> the message is processed.");

        // Integer-accumulator process: state = running sum, message = int to add
        var proc = new Proc<>(0, (state, msg) -> state + (Integer) msg);

        proc.tell(10);
        proc.tell(15);
        // ask returns a future completing with the state after the message is processed
        int result = proc.ask(17).get(2, TimeUnit.SECONDS);

        sayAndAssertThat("ask(17) → state after 10+15+17 = 42", 42, is(result));

        proc.stop();
    }

    @Test
    public void testProcAskWithTimeout() throws Exception {
        sayNextSection("Proc.ask(msg, timeout) — Timed Request-Reply (gen_server:call with Timeout)");
        say("Armstrong: <em>\"An unbounded call is a latent deadlock. Every call must have a timeout.\"</em> "
                + "<code>ask(msg, timeout)</code> mirrors OTP's <code>gen_server:call(Pid, Msg, Timeout)</code>: "
                + "the returned future completes exceptionally with <code>TimeoutException</code> if the "
                + "process does not respond within the given duration.");

        var proc = new Proc<>("", (state, msg) -> state + msg);

        String state = proc.ask("hello", Duration.ofSeconds(2)).get();

        sayAndAssertThat("ask with 2-second timeout succeeds for fast handler",
                "hello", is(state));

        proc.stop();
    }

    @Test
    public void testProcTrapExits() throws Exception {
        sayNextSection("Proc.trapExits(true) — EXIT Signals as Mailbox Messages");
        say("OTP: <code>process_flag(trap_exit, true)</code> converts incoming EXIT signals "
                + "from linked processes into <code>ExitSignal</code> messages delivered to "
                + "the mailbox, rather than killing the process. The process can then "
                + "pattern-match and decide whether to propagate the crash.");

        var proc = new Proc<>(false, (state, msg) -> switch (msg) {
            case ExitSignal sig -> true;   // received exit signal → set flag
            default             -> state;
        });
        proc.trapExits(true);

        sayAndAssertThat("isTrappingExits() is true after trapExits(true)",
                true, is(proc.isTrappingExits()));

        // Deliver an exit signal directly
        proc.deliverExitSignal(new RuntimeException("simulated crash"));
        boolean sawExit = proc.ask("noop", Duration.ofSeconds(2)).get();

        sayAndAssertThat("ExitSignal is delivered as a mailbox message (not a process kill)",
                true, is(sawExit));

        proc.stop();
    }

    @Test
    public void testProcCrashCallback() throws Exception {
        sayNextSection("Proc.addCrashCallback — Abnormal Termination Hook");
        say("Crash callbacks are invoked when the handler throws an unhandled exception "
                + "(abnormal exit). They are <em>not</em> called on graceful <code>stop()</code> — "
                + "mirroring OTP's distinction between <code>normal</code> and non-normal exit reasons. "
                + "Used internally by <code>Supervisor</code> and <code>ProcessLink</code>.");

        var crashed = new CountDownLatch(1);
        var proc = new Proc<>(0, (state, msg) -> {
            throw new RuntimeException("intentional crash");
        });
        proc.addCrashCallback(crashed::countDown);
        proc.tell("trigger");

        boolean callbackFired = crashed.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("crash callback fires on unhandled exception", true, is(callbackFired));
    }

    // =========================================================================
    // ProcTimer — OTP timer:send_after / timer:send_interval
    // =========================================================================

    @Test
    public void testProcTimerSendAfter() throws Exception {
        sayNextSection("ProcTimer.sendAfter — One-Shot Timed Message (timer:send_after/3)");
        say("OTP processes model timeouts by <em>receiving</em> timed messages rather than "
                + "using blocking sleep or callback APIs. <code>ProcTimer.sendAfter(delayMs, proc, msg)</code> "
                + "schedules a single delivery after the given delay, returning a "
                + "<code>TimerRef</code> that can cancel delivery before it fires.");

        var received = new CountDownLatch(1);
        var proc = new Proc<>(false, (state, msg) -> {
            received.countDown();
            return true;
        });

        var ref = ProcTimer.sendAfter(50, proc, "tick");

        boolean delivered = received.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("message is delivered after 50 ms", true, is(delivered));
        sayAndAssertThat("TimerRef.cancel() returns false once fired", false, is(ref.cancel()));

        proc.stop();
    }

    @Test
    public void testProcTimerCancel() throws Exception {
        sayNextSection("ProcTimer cancel — Prevent Scheduled Delivery (timer:cancel/1)");
        say("Cancelling a <code>TimerRef</code> before it fires prevents the message from "
                + "being delivered. Returns <code>true</code> if cancellation succeeded. "
                + "Mirrors Erlang's <code>timer:cancel(TRef)</code>.");

        var proc = new Proc<>(0, (state, msg) -> state + 1);

        // Schedule far in the future and cancel immediately
        var ref = ProcTimer.sendAfter(10_000, proc, "should-never-arrive");
        boolean cancelled = ProcTimer.cancel(ref);

        sayAndAssertThat("cancel before firing returns true", true, is(cancelled));

        // give a moment to ensure no message arrives
        Thread.sleep(50);
        int state = proc.ask("probe", Duration.ofSeconds(1)).get();
        // The ask("probe") itself increments state by 1; if the cancelled timer message had also
        // been delivered, state would be 2 instead of 1.
        sayAndAssertThat("no timer message delivered after cancellation (only probe incremented state to 1)", 1, is(state));

        proc.stop();
    }

    @Test
    public void testProcTimerSendInterval() throws Exception {
        sayNextSection("ProcTimer.sendInterval — Repeating Timed Message (timer:send_interval/3)");
        say("<code>sendInterval(periodMs, proc, msg)</code> delivers the message repeatedly "
                + "every <code>periodMs</code> milliseconds. The timer continues until "
                + "explicitly cancelled via <code>ProcTimer.cancel(ref)</code>. "
                + "Mirrors Erlang's <code>timer:send_interval(Ms, Pid, Msg)</code>.");

        var count = new AtomicInteger(0);
        var proc = new Proc<>(0, (state, msg) -> {
            count.incrementAndGet();
            return state + 1;
        });

        var ref = ProcTimer.sendInterval(30, proc, "tick");
        Thread.sleep(120); // expect ~4 ticks in 120 ms
        ProcTimer.cancel(ref);

        sayAndAssertThat("at least 3 ticks delivered in 120 ms", true, is(count.get() >= 3));

        proc.stop();
    }

    // =========================================================================
    // ProcessRegistry — global name table (register/2, whereis/1)
    // =========================================================================

    @Test
    public void testProcessRegistryRegisterAndWhereis() throws Exception {
        sayNextSection("ProcessRegistry — Global Name Table (OTP register/2, whereis/1)");
        say("OTP: every process can be registered under a unique atom name. Other processes "
                + "look up the name via <code>whereis/1</code> to obtain a Pid without "
                + "threading it through the call stack. Registration is JVM-scoped and "
                + "automatically removed when the process terminates.");

        var proc = new Proc<String, String>("idle", (state, msg) -> msg);
        ProcessRegistry.register("my-worker", proc);

        var found = ProcessRegistry.<String, String>whereis("my-worker");

        sayAndAssertThat("whereis('my-worker') returns the registered process",
                true, is(found.isPresent()));

        // Auto-deregistration on termination
        proc.stop();
        Thread.sleep(50); // let termination callback fire
        var gone = ProcessRegistry.<String, String>whereis("my-worker");

        sayAndAssertThat("whereis returns empty after process terminates",
                false, is(gone.isPresent()));
    }

    @Test
    public void testProcessRegistryDuplicateFails() {
        sayNextSection("ProcessRegistry — Duplicate Registration Throws");
        say("Registering a name that is already taken throws "
                + "<code>IllegalStateException</code> — matching OTP's behaviour where "
                + "an attempt to register an already-registered name is an error.");

        var p1 = new Proc<>(0, (s, m) -> s);
        var p2 = new Proc<>(0, (s, m) -> s);
        ProcessRegistry.register("unique-name", p1);

        boolean threw = false;
        try {
            ProcessRegistry.register("unique-name", p2);
        } catch (IllegalStateException e) {
            threw = true;
        }

        sayAndAssertThat("second register for same name throws IllegalStateException",
                true, is(threw));

        try { p1.stop(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        try { p2.stop(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Test
    public void testProcessRegistryUnregister() throws Exception {
        sayNextSection("ProcessRegistry.unregister — Explicit Name Removal (OTP unregister/1)");
        say("<code>unregister(name)</code> removes the name without stopping the process. "
                + "After unregistering, the process can be registered under a new name or "
                + "the same name can be reused. Safe to call even when the name is not present.");

        var proc = new Proc<>(0, (s, m) -> s);
        ProcessRegistry.register("temp-name", proc);
        ProcessRegistry.unregister("temp-name");

        var result = ProcessRegistry.<Integer, Object>whereis("temp-name");

        sayAndAssertThat("whereis returns empty after explicit unregister",
                false, is(result.isPresent()));
        sayAndAssertThat("registered() does not contain the unregistered name",
                false, is(ProcessRegistry.registered().contains("temp-name")));

        proc.stop();
    }

    // =========================================================================
    // Supervisor — hierarchical supervision tree
    // =========================================================================

    @Test
    public void testSupervisorOneForOne() throws Exception {
        sayNextSection("Supervisor — ONE_FOR_ONE Restart Strategy");
        say("OTP <em>supervision trees</em> are the backbone of fault tolerance. "
                + "When a child crashes under <code>ONE_FOR_ONE</code>, only that child is "
                + "restarted — siblings are unaffected. <code>supervise(id, state, handler)</code> "
                + "returns a <code>ProcRef</code>: a stable handle that transparently redirects "
                + "to the restarted process so callers need not change.");

        var supervisor = new Supervisor(Supervisor.Strategy.ONE_FOR_ONE, 5, Duration.ofSeconds(60));

        var counter = new AtomicInteger(0);
        ProcRef<Integer, Integer> worker =
                supervisor.supervise("adder", 0, (state, msg) -> state + msg);

        worker.tell(10);
        worker.tell(20);
        int state = worker.ask(12).get(2, TimeUnit.SECONDS);

        sayAndAssertThat("supervised worker accumulates messages: 10+20+12=42", 42, is(state));

        supervisor.shutdown();
        sayAndAssertThat("supervisor shuts down cleanly", false, is(supervisor.isRunning()));
    }

    @Test
    public void testSupervisorRestartsOnCrash() throws Exception {
        sayNextSection("Supervisor — Automatic Restart After Child Crash");
        say("When a supervised process throws an unhandled exception, the supervisor "
                + "automatically spawns a fresh process from the same initial state. "
                + "The <code>ProcRef</code> transparently redirects all subsequent calls "
                + "to the new process — callers observe no change in the reference.");

        var restartCount = new AtomicInteger(0);
        var supervisor = new Supervisor("test", Supervisor.Strategy.ONE_FOR_ONE, 5, Duration.ofSeconds(60));

        ProcRef<String, String> worker = supervisor.supervise(
                "crasher",
                "initial",
                (state, msg) -> switch (msg) {
                    case "crash" -> throw new RuntimeException("intentional crash");
                    default      -> state + "+" + msg;
                });

        // trigger crash then observe fresh restart
        worker.tell("crash");
        Thread.sleep(100); // let supervisor restart the process

        String freshState = worker.ask("hello").get(2, TimeUnit.SECONDS);

        sayAndAssertThat("after restart, state is reset to initial + new message",
                "initial+hello", is(freshState));

        supervisor.shutdown();
    }

    // =========================================================================
    // CrashRecovery — "let it crash" + supervised retry
    // =========================================================================

    @Test
    public void testCrashRecoveryRetrySuccess() {
        sayNextSection("CrashRecovery.retry — \"Let It Crash\" + Supervised Retry");
        say("Joe Armstrong: <em>\"The key to building reliable systems is to design for failure, "
                + "not to try to prevent it.\"</em> "
                + "<code>CrashRecovery.retry(maxAttempts, supplier)</code> executes the supplier "
                + "in an isolated virtual thread on each attempt. On success it returns "
                + "<code>Result.Success(value)</code>; only after exhausting all attempts does "
                + "it return <code>Result.Failure(lastException)</code>.");

        var attempt = new AtomicInteger(0);
        Result<String, Exception> result = CrashRecovery.retry(3, () -> {
            if (attempt.incrementAndGet() < 3) {
                throw new RuntimeException("not yet");
            }
            return "success-on-attempt-3";
        });

        sayAndAssertThat("result is Success after 3rd attempt", true, is(result.isSuccess()));
        sayAndAssertThat("value is 'success-on-attempt-3'",
                "success-on-attempt-3", is(result.orElse("failed")));
    }

    @Test
    public void testCrashRecoveryExhausted() {
        sayNextSection("CrashRecovery.retry — All Attempts Exhausted → Failure");
        say("If every attempt throws, <code>retry</code> returns "
                + "<code>Result.Failure(lastException)</code>. Each attempt runs in its "
                + "own virtual thread with no shared state — mirroring OTP's supervisor "
                + "restart model where each restart is a fresh process.");

        Result<String, Exception> result = CrashRecovery.retry(3, () -> {
            throw new IllegalStateException("always fails");
        });

        sayAndAssertThat("result is a Failure when all attempts throw", true, is(result.isFailure()));
        sayAndAssertThat("retry(0) throws IllegalArgumentException for invalid maxAttempts",
                true, is(assertThrows(() -> CrashRecovery.retry(0, () -> "x"))));
    }

    // =========================================================================
    // ExitSignal — exit signal record
    // =========================================================================

    @Test
    public void testExitSignalRecord() {
        sayNextSection("ExitSignal — EXIT Signal as a Mailbox Message");
        say("OTP: when a process has enabled exit trapping (<code>trap_exit, true</code>), "
                + "EXIT signals from linked processes arrive as <code>{'EXIT', FromPid, Reason}</code> "
                + "tuples in the mailbox. <code>ExitSignal</code> is the Java 25 record equivalent: "
                + "a simple value carrying the exit reason, pattern-matchable in a switch expression.");

        var normal  = new ExitSignal(null);
        var crashed = new ExitSignal(new RuntimeException("boom"));

        sayAndAssertThat("ExitSignal with null reason represents a normal exit",
                null, is(normal.reason()));
        sayAndAssertThat("ExitSignal with non-null reason carries the exception",
                "boom", is(crashed.reason().getMessage()));
        sayAndAssertThat("ExitSignal is a record — value equality",
                new ExitSignal(null), is(normal));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private static boolean assertThrows(Runnable action) {
        try {
            action.run();
            return false;
        } catch (Exception e) {
            return true;
        }
    }
}
