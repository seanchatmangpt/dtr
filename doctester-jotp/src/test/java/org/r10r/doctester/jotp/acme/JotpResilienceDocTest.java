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

import org.acme.CircuitBreaker;
import org.acme.Parallel;
import org.acme.RateLimiter;
import org.acme.Result;
import org.junit.Test;
import org.r10r.doctester.DocTester;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.is;

/**
 * DocTester documentation for JOTP resilience patterns:
 * {@link CircuitBreaker}, {@link Parallel}, and {@link RateLimiter}.
 *
 * <p>Sourced from <a href="https://github.com/seanchatmangpt/java-maven-template">
 * seanchatmangpt/java-maven-template</a> (git submodule at {@code jotp/}).
 *
 * <p>Covers: Circuit Breaker state machine (CLOSED → OPEN → HALF_OPEN → CLOSED),
 * all three {@link CircuitBreaker.CircuitError} variants, parallel fan-out with
 * fail-fast semantics, and Token Bucket / Sliding Window rate limiting.
 */
public class JotpResilienceDocTest extends DocTester {

    // =========================================================================
    // CircuitBreaker
    // =========================================================================

    @Test
    public void testCircuitBreakerClosedStateSucceeds() {
        sayNextSection("CircuitBreaker — CLOSED State: Normal Operation");
        say("Martin Fowler: <em>\"The basic idea behind the circuit breaker is very simple. "
                + "You wrap a protected function call in a circuit breaker object, "
                + "which monitors for failures.\"</em> "
                + "In the <code>CLOSED</code> state the operation is executed normally. "
                + "A successful call resets the failure counter to zero.");

        CircuitBreaker breaker = CircuitBreaker.builder("payment-svc")
                .failureThreshold(5)
                .timeout(Duration.ofSeconds(5))
                .resetTimeout(Duration.ofSeconds(30))
                .build();

        Result<String, CircuitBreaker.CircuitError> result = breaker.execute(() -> "payment-ok");

        sayAndAssertThat("initial state is CLOSED",
                CircuitBreaker.State.CLOSED, is(breaker.state()));
        sayAndAssertThat("execute() in CLOSED state returns Success",
                true, is(result.isSuccess()));
        sayAndAssertThat("success value is 'payment-ok'",
                "payment-ok", is(result.orElse(null)));
    }

    @Test
    public void testCircuitBreakerTripsToOpen() {
        sayNextSection("CircuitBreaker — CLOSED → OPEN: Failure Threshold Exceeded");
        say("When the number of consecutive failures reaches <code>failureThreshold</code>, "
                + "the circuit breaker trips to <code>OPEN</code>. All subsequent calls "
                + "immediately return <code>CircuitError.CircuitOpen</code> without invoking "
                + "the operation — protecting the downstream service from overload.");

        CircuitBreaker breaker = CircuitBreaker.builder("inventory-svc")
                .failureThreshold(3)
                .timeout(Duration.ofSeconds(1))
                .resetTimeout(Duration.ofSeconds(60))
                .build();

        // Exhaust the failure threshold (timeout failures)
        for (int i = 0; i < 3; i++) {
            breaker.execute(() -> { throw new RuntimeException("downstream down"); });
        }

        sayAndAssertThat("state is OPEN after 3 failures", CircuitBreaker.State.OPEN, is(breaker.state()));

        Result<String, CircuitBreaker.CircuitError> blocked = breaker.execute(() -> "should-not-run");

        sayAndAssertThat("blocked call returns Failure", Boolean.TRUE, is(blocked.isFailure()));
        boolean isCircuitOpen = blocked.fold(v -> false, e -> e instanceof CircuitBreaker.CircuitError.CircuitOpen);
        sayAndAssertThat("error is CircuitOpen variant", Boolean.TRUE, is(isCircuitOpen));
    }

    @Test
    public void testCircuitBreakerManualReset() {
        sayNextSection("CircuitBreaker.reset() — Force CLOSED State");
        say("<code>reset()</code> clears the failure counter and forces the circuit "
                + "back to <code>CLOSED</code>. Useful in tests and for administrative "
                + "recovery after a manual incident resolution.");

        CircuitBreaker breaker = CircuitBreaker.builder("auth-svc")
                .failureThreshold(1)
                .timeout(Duration.ofSeconds(1))
                .resetTimeout(Duration.ofSeconds(60))
                .build();

        breaker.execute(() -> { throw new RuntimeException("fail"); });
        sayAndAssertThat("state is OPEN after 1 failure (threshold=1)",
                CircuitBreaker.State.OPEN, is(breaker.state()));

        breaker.reset();
        sayAndAssertThat("state is CLOSED after reset()",
                CircuitBreaker.State.CLOSED, is(breaker.state()));

        Result<String, CircuitBreaker.CircuitError> ok = breaker.execute(() -> "recovered");
        sayAndAssertThat("execute() works again after reset",
                true, is(ok.isSuccess()));
    }

    @Test
    public void testCircuitBreakerManualTrip() {
        sayNextSection("CircuitBreaker.trip() — Force OPEN State");
        say("<code>trip()</code> immediately transitions to <code>OPEN</code> without "
                + "waiting for failures. Useful for proactive circuit breaking during "
                + "planned maintenance or canary deployment.");

        CircuitBreaker breaker = CircuitBreaker.builder("shipping-svc")
                .failureThreshold(10)
                .timeout(Duration.ofSeconds(5))
                .resetTimeout(Duration.ofSeconds(30))
                .build();

        breaker.trip();

        sayAndAssertThat("state is OPEN immediately after trip()",
                CircuitBreaker.State.OPEN, is(breaker.state()));

        Result<String, CircuitBreaker.CircuitError> result = breaker.execute(() -> "blocked");
        sayAndAssertThat("all calls blocked while OPEN", true, is(result.isFailure()));
    }

    @Test
    public void testCircuitBreakerConfig() {
        sayNextSection("CircuitBreaker.Config — Immutable Configuration Record");
        say("The builder produces an immutable <code>Config</code> record accessible via "
                + "<code>config()</code>. Configurations are value types: two breakers with "
                + "the same parameters have equal configs.");

        CircuitBreaker b = CircuitBreaker.builder("cfg-test")
                .failureThreshold(7)
                .timeout(Duration.ofSeconds(3))
                .resetTimeout(Duration.ofSeconds(45))
                .build();

        CircuitBreaker.Config cfg = b.config();

        sayAndAssertThat("config.name() is 'cfg-test'",         "cfg-test", is(cfg.name()));
        sayAndAssertThat("config.failureThreshold() is 7",      7,          is(cfg.failureThreshold()));
        sayAndAssertThat("config.timeout() is PT3S",            Duration.ofSeconds(3),  is(cfg.timeout()));
        sayAndAssertThat("config.resetTimeout() is PT45S",      Duration.ofSeconds(45), is(cfg.resetTimeout()));
    }

    // =========================================================================
    // Parallel — structured fan-out with fail-fast semantics
    // =========================================================================

    @Test
    public void testParallelAllSuccess() throws Exception {
        sayNextSection("Parallel.all() — Structured Fan-Out, All Tasks Succeed");
        say("Joe Armstrong: <em>\"Processes share nothing, communicate only by message passing.\"</em> "
                + "<code>Parallel.all(tasks)</code> runs all tasks concurrently on virtual threads "
                + "using Java's <code>StructuredTaskScope</code>. If <em>every</em> task succeeds, "
                + "it returns <code>Result.Success(results)</code> with results in fork order. "
                + "Mirrors Erlang's <code>pmap/2</code>.");

        List<String> results = Parallel.<String>all(List.of(
                () -> "alpha",
                () -> "beta",
                () -> "gamma"
        )).orElseThrow();

        sayAndAssertThat("all 3 results collected in fork order", 3, is(results.size()));
        sayAndAssertThat("result[0] is 'alpha'", "alpha", is(results.get(0)));
        sayAndAssertThat("result[1] is 'beta'",  "beta",  is(results.get(1)));
        sayAndAssertThat("result[2] is 'gamma'", "gamma", is(results.get(2)));
    }

    @Test
    public void testParallelFailFast() {
        sayNextSection("Parallel.all() — Fail-Fast on First Task Failure");
        say("Armstrong: <em>\"If any process fails, fail fast. Don't mask errors.\"</em> "
                + "When any task throws, <code>Parallel.all()</code> cancels remaining tasks "
                + "immediately and returns <code>Result.Failure(firstException)</code>. "
                + "This is all-or-nothing semantics: partial results are never returned.");

        var counter = new AtomicInteger(0);
        Result<List<String>, Exception> result = Parallel.all(List.of(
                () -> { counter.incrementAndGet(); return "ok"; },
                () -> { throw new RuntimeException("boom"); },
                () -> { counter.incrementAndGet(); return "ok2"; }
        ));

        sayAndAssertThat("result is Failure when any task throws", true, is(result.isFailure()));
    }

    @Test
    public void testParallelEmptyList() {
        sayNextSection("Parallel.all() — Empty Task List Returns Empty Success");
        say("An empty task list is trivially successful: no tasks can fail, "
                + "so the result is <code>Success(emptyList)</code>.");

        Result<List<Object>, Exception> result = Parallel.all(List.of());

        sayAndAssertThat("empty list returns Success",       true, is(result.isSuccess()));
        sayAndAssertThat("result list is empty",             0,    is(result.orElseThrow().size()));
    }

    // =========================================================================
    // RateLimiter — token bucket and sliding window
    // =========================================================================

    @Test
    public void testRateLimiterTokenBucketAcquire() {
        sayNextSection("RateLimiter.tokenBucket() — Smooth Traffic with Burst Tolerance");
        say("The token bucket algorithm fills at a constant rate (refill rate) up to "
                + "a maximum capacity. Each <code>tryAcquire()</code> removes one token; "
                + "requests above capacity are rejected. Burst capacity allows short "
                + "spikes above the steady-state rate.");

        RateLimiter limiter = RateLimiter.tokenBucket(10, 10.0); // capacity=10, refill=10/s

        // Acquire all 10 tokens immediately (burst)
        boolean allAcquired = true;
        for (int i = 0; i < 10; i++) {
            allAcquired &= limiter.tryAcquire();
        }

        sayAndAssertThat("all 10 burst tokens acquired",          true,  is(allAcquired));
        sayAndAssertThat("11th acquire fails — bucket empty",     false, is(limiter.tryAcquire()));
        sayAndAssertThat("currentRate() returns configured rate", 10.0,  is(limiter.currentRate()));
    }

    @Test
    public void testRateLimiterPerSecondFactory() {
        sayNextSection("RateLimiter.perSecond() — Convenience Factory");
        say("<code>RateLimiter.perSecond(rate)</code> creates a token bucket with "
                + "capacity and refill rate both set to <code>rate</code>, meaning up to "
                + "<code>rate</code> requests per second with full burst capacity.");

        RateLimiter limiter = RateLimiter.perSecond(5);

        sayAndAssertThat("perSecond(5) allows 5 immediate acquires", true,
                is(limiter.tryAcquire() && limiter.tryAcquire() && limiter.tryAcquire()
                        && limiter.tryAcquire() && limiter.tryAcquire()));
        sayAndAssertThat("6th acquire fails for perSecond(5)", false, is(limiter.tryAcquire()));
    }

    @Test
    public void testRateLimiterSlidingWindow() {
        sayNextSection("RateLimiter.slidingWindow() — Precise Request Counting");
        say("The sliding window algorithm counts requests within a rolling time window. "
                + "Unlike token bucket it does not allow bursting above the per-window limit. "
                + "Useful when strict fairness matters more than throughput.");

        RateLimiter limiter = RateLimiter.slidingWindow(3, Duration.ofSeconds(1));

        boolean t1 = limiter.tryAcquire();
        boolean t2 = limiter.tryAcquire();
        boolean t3 = limiter.tryAcquire();
        boolean t4 = limiter.tryAcquire(); // 4th within 1s window — must be rejected

        sayAndAssertThat("first 3 acquires within window succeed", true,  is(t1 && t2 && t3));
        sayAndAssertThat("4th acquire within same window fails",   false, is(t4));
    }

    @Test
    public void testRateLimiterReset() {
        sayNextSection("RateLimiter.reset() — Restore Full Capacity");
        say("<code>reset()</code> refills the bucket to full capacity immediately, "
                + "clearing any previously consumed tokens. Useful for test isolation "
                + "or administrative recovery.");

        RateLimiter limiter = RateLimiter.tokenBucket(3, 3.0);
        limiter.tryAcquire();
        limiter.tryAcquire();
        limiter.tryAcquire(); // bucket empty

        sayAndAssertThat("bucket empty after 3 acquires", false, is(limiter.tryAcquire()));

        limiter.reset();
        sayAndAssertThat("tryAcquire succeeds again after reset()", true, is(limiter.tryAcquire()));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    /**
     * Alias for sayAndAssertThat — used when the assertion expresses a "tired" pattern
     * (i.e., a negative check after deliberate failures) to avoid IDE warnings.
     */
    private <T> void sayAndAssertTired(String msg, T actual,
            org.hamcrest.Matcher<T> matcher) {
        sayAndAssertThat(msg, actual, matcher);
    }
}
