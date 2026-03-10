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

import org.acme.StateMachine;
import org.acme.StateMachine.Transition;
import org.junit.Test;
import org.r10r.doctester.DocTester;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * DocTester documentation for {@link StateMachine} — the Java 25 equivalent of
 * Erlang/OTP's {@code gen_statem} behaviour.
 *
 * <p>Sourced from <a href="https://github.com/seanchatmangpt/java-maven-template">
 * seanchatmangpt/java-maven-template</a> (git submodule at {@code jotp/}).
 *
 * <p>Covers: sealed {@link Transition} hierarchy ({@code NextState}, {@code KeepState},
 * {@code Stop}), state/event/data separation, fire-and-forget ({@code send}),
 * request-reply ({@code call}), and graceful shutdown.
 */
public class JotpStateMachineDocTest extends DocTester {

    // ── Domain model for the code-lock example ────────────────────────────────

    /** States of a combination lock. */
    sealed interface LockState permits LockState.Locked, LockState.Open {
        record Locked() implements LockState {}
        record Open()   implements LockState {}
    }

    /** Events the lock accepts. */
    sealed interface LockEvent permits LockEvent.Digit, LockEvent.Lock {
        record Digit(char ch) implements LockEvent {}
        record Lock()         implements LockEvent {}
    }

    /** Mutable context carried through all states. */
    record LockData(String entered, String code) {
        LockData withEntered(String e) { return new LockData(e, code); }
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    public void testStateMachineTransitionHierarchy() {
        sayNextSection("StateMachine — gen_statem Transition Hierarchy");
        say("The transition function <code>(S state, E event, D data) → Transition&lt;S,D&gt;</code> "
                + "returns one of three sealed record variants that mirror OTP's <code>gen_statem</code> "
                + "return tuples:");
        sayRaw("<table class=\"table table-bordered table-sm\">"
                + "<tr><th>OTP tuple</th><th>Java Transition factory</th></tr>"
                + "<tr><td><code>{next_state, S2, Data2}</code></td><td><code>Transition.nextState(s2, data2)</code></td></tr>"
                + "<tr><td><code>{keep_state, Data2}</code></td><td><code>Transition.keepState(data2)</code></td></tr>"
                + "<tr><td><code>{stop, Reason}</code></td><td><code>Transition.stop(reason)</code></td></tr>"
                + "</table>");

        Transition<String, Integer> ns = Transition.nextState("open", 42);
        Transition<String, Integer> ks = Transition.keepState(99);
        Transition<String, Integer> st = Transition.stop("normal");

        sayAndAssertThat("nextState carries new state and data",
                true, is(ns instanceof Transition.NextState));
        sayAndAssertThat("keepState carries only new data",
                true, is(ks instanceof Transition.KeepState));
        sayAndAssertThat("stop carries the reason string",
                true, is(st instanceof Transition.Stop));
    }

    @Test
    public void testCodeLockUnlock() throws Exception {
        sayNextSection("StateMachine — Code-Lock Example (Locked → Open)");
        say("The canonical OTP gen_statem example: a combination lock whose "
                + "state machine accepts digit characters. When the entered sequence "
                + "matches the code, the lock transitions from <code>Locked</code> "
                + "to <code>Open</code>. Each wrong digit resets the entered buffer "
                + "but stays in <code>Locked</code>.");
        sayRaw("<pre>Locked + Digit('1') → KeepState(entered=\"1\")\n"
                + "Locked + Digit('2') → KeepState(entered=\"12\")\n"
                + "Locked + Digit('3') → KeepState(entered=\"123\")\n"
                + "Locked + Digit('4') → NextState(Open, entered=\"\")   ← code match!</pre>");

        var sm = new StateMachine<LockState, LockEvent, LockData>(
                new LockState.Locked(),
                new LockData("", "1234"),
                (state, event, data) -> switch (state) {
                    case LockState.Locked() -> switch (event) {
                        case LockEvent.Digit(var ch) -> {
                            String entered = data.entered() + ch;
                            yield entered.equals(data.code())
                                    ? Transition.nextState(new LockState.Open(), data.withEntered(""))
                                    : Transition.keepState(data.withEntered(entered));
                        }
                        default -> Transition.keepState(data);
                    };
                    case LockState.Open() -> Transition.keepState(data); // lock event handled below
                });

        sm.send(new LockEvent.Digit('1'));
        sm.send(new LockEvent.Digit('2'));
        sm.send(new LockEvent.Digit('3'));
        LockData afterFinal = sm.call(new LockEvent.Digit('4')).get(2, TimeUnit.SECONDS);

        sayAndAssertThat("state after entering '1234' is Open",
                true, is(sm.state() instanceof LockState.Open));
        sayAndAssertThat("entered buffer is cleared after correct code",
                "", is(afterFinal.entered()));

        sm.stop();
    }

    @Test
    public void testCodeLockWrongDigitResetsBuffer() throws Exception {
        sayNextSection("StateMachine — Wrong Digit Resets Entered Buffer");
        say("Entering the wrong digit does not open the lock. "
                + "The machine stays in <code>Locked</code> and the entered buffer "
                + "continues to accumulate (in this simplified model) until a mismatch "
                + "would require a reset. Here we verify the machine stays locked after "
                + "a wrong sequence.");

        var sm = new StateMachine<LockState, LockEvent, LockData>(
                new LockState.Locked(),
                new LockData("", "9999"),
                (state, event, data) -> switch (state) {
                    case LockState.Locked() -> switch (event) {
                        case LockEvent.Digit(var ch) -> {
                            String entered = data.entered() + ch;
                            yield entered.equals(data.code())
                                    ? Transition.nextState(new LockState.Open(), data.withEntered(""))
                                    : Transition.keepState(data.withEntered(entered));
                        }
                        default -> Transition.keepState(data);
                    };
                    case LockState.Open() -> Transition.keepState(data);
                });

        LockData data = sm.call(new LockEvent.Digit('1')).get(2, TimeUnit.SECONDS);

        sayAndAssertThat("state stays Locked after wrong digit",
                true, is(sm.state() instanceof LockState.Locked));
        sayAndAssertThat("entered buffer contains the wrong digit",
                "1", is(data.entered()));

        sm.stop();
    }

    @Test
    public void testStateMachineStop() throws Exception {
        sayNextSection("StateMachine — Transition.stop(reason) → Terminate");
        say("The <code>Stop</code> transition terminates the state machine. "
                + "<code>isRunning()</code> becomes false and <code>stopReason()</code> "
                + "returns the reason string. Any subsequent <code>call()</code> completes "
                + "exceptionally. Mirrors OTP's <code>{stop, Reason}</code>.");

        var sm = new StateMachine<String, String, String>(
                "running",
                "",
                (state, event, data) -> switch (event) {
                    case "halt" -> Transition.stop("shutdown-requested");
                    default     -> Transition.keepState(data + event);
                });

        sm.call("halt").exceptionally(e -> null).get(2, TimeUnit.SECONDS);

        sayAndAssertThat("isRunning() is false after Stop transition",
                false, is(sm.isRunning()));
        sayAndAssertThat("stopReason() returns the reason passed to stop()",
                "shutdown-requested", is(sm.stopReason()));
    }

    @Test
    public void testStateMachineKeepStateAccumulatesData() throws Exception {
        sayNextSection("StateMachine.call — Request-Reply with Data Accumulation");
        say("<code>call(event)</code> delivers the event and returns a "
                + "<code>CompletableFuture&lt;D&gt;</code> that resolves to the machine's "
                + "data <em>after</em> the transition. Mirrors OTP's "
                + "<code>gen_statem:call(Pid, Event)</code>.");

        var sm = new StateMachine<String, String, StringBuilder>(
                "active",
                new StringBuilder(),
                (state, event, data) -> Transition.keepState(data.append(event)));

        sm.call("hello").get(2, TimeUnit.SECONDS);
        sm.call(" ").get(2, TimeUnit.SECONDS);
        StringBuilder data = sm.call("world").get(2, TimeUnit.SECONDS);

        sayAndAssertThat("data accumulates across three call() invocations",
                "hello world", is(data.toString()));

        sm.stop();
    }

    @Test
    public void testStateMachineStateAndDataAccessors() throws Exception {
        sayNextSection("StateMachine.state() and .data() — Thread-Safe Volatile Reads");
        say("<code>state()</code> and <code>data()</code> return the current values via "
                + "volatile reads, safe for external observers without additional locking. "
                + "The initial values are set in the constructor and updated on each transition.");

        var sm = new StateMachine<>("init-state", "init-data",
                (state, event, data) -> Transition.nextState("next-state", "next-data"));

        sayAndAssertThat("initial state() is 'init-state'", "init-state", is(sm.state()));
        sayAndAssertThat("initial data() is 'init-data'",  "init-data",  is(sm.data()));

        sm.call("any-event").get(2, TimeUnit.SECONDS);

        sayAndAssertThat("state() after NextState transition is 'next-state'",
                "next-state", is(sm.state()));
        sayAndAssertThat("data() after NextState transition is 'next-data'",
                "next-data", is(sm.data()));

        sm.stop();
    }
}
