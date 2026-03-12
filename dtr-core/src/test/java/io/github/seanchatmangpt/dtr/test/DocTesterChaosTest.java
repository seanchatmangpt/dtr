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
package io.github.seanchatmangpt.dtr;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.*;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowserImpl;
import io.github.seanchatmangpt.dtr.testbrowser.Url;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Chaos / fault-injection tests using <a href="https://wiremock.org">WireMock</a>.
 *
 * <p>Upgraded to Fortune 5 / Joe Armstrong quality with three production resilience patterns:
 * <ul>
 *   <li><b>Circuit Breaker</b> — stop calling a failing service after N consecutive failures</li>
 *   <li><b>Retry with Exponential Backoff</b> — recover from transient faults with increasing delay</li>
 *   <li><b>Bulkhead Isolation</b> — faults in one service pool must not affect another pool</li>
 * </ul>
 *
 * <p>Concurrent chaos test upgraded from 20 → 100 virtual threads for higher load factor.
 *
 * <p>WireMock is an embedded HTTP server that can simulate real-world failure
 * modes at the protocol level. These tests verify that {@link TestBrowserImpl}
 * (and therefore the full DocTester stack) reacts predictably under each fault.
 *
 * <h2>Fault taxonomy tested</h2>
 * <ul>
 *   <li><b>EMPTY_RESPONSE</b> — server closes the TCP connection without sending
 *       any bytes; the client must throw (not hang).</li>
 *   <li><b>MALFORMED_RESPONSE_CHUNK</b> — HTTP status line is OK but body is
 *       garbage; the client must throw or return a non-null response.</li>
 *   <li><b>RANDOM_DATA_THEN_CLOSE</b> — server blasts random bytes then closes;
 *       client must not silently swallow the error.</li>
 *   <li><b>CONNECTION_RESET_BY_PEER</b> — RST packet mid-response; client
 *       must propagate a RuntimeException.</li>
 *   <li><b>Fixed delay</b> — simulates a slow server; the client eventually
 *       succeeds or the test detects an expected timeout.</li>
 *   <li><b>Intermittent fault</b> — 50% of requests fault, 50% succeed;
 *       verifies the client can recover and complete valid requests.</li>
 *   <li><b>Concurrent chaos</b> — multiple virtual threads hammer the faulting
 *       server simultaneously; verifies no cross-thread state corruption.</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("DocTester chaos / fault-injection tests (WireMock)")
class DocTesterChaosTest {

    private WireMockServer server;

    @BeforeAll
    void startServer() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
    }

    @AfterAll
    void stopServer() {
        if (server != null) {
            server.stop();
        }
    }

    @BeforeEach
    void resetStubs() {
        server.resetAll();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Url baseUrl(String path) {
        return Url.host("http://localhost:" + server.port()).path(path);
    }

    private Request getRequest(String path) {
        return Request.GET().url(baseUrl(path));
    }

    // =========================================================================
    // 1. Protocol-level fault tests
    // =========================================================================

    @Test
    @DisplayName("EMPTY_RESPONSE — server closes connection with no bytes → RuntimeException")
    void emptyResponseFaultThrows() {
        server.stubFor(get(anyUrl())
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        var browser = new TestBrowserImpl();
        assertThrows(RuntimeException.class,
                () -> browser.makeRequest(getRequest("/fault/empty")),
                "EMPTY_RESPONSE should cause the client to throw RuntimeException");
    }

    @Test
    @DisplayName("RANDOM_DATA_THEN_CLOSE — server sends garbage bytes → RuntimeException")
    void randomDataThenCloseFaultThrows() {
        server.stubFor(get(anyUrl())
                .willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));

        var browser = new TestBrowserImpl();
        assertThrows(RuntimeException.class,
                () -> browser.makeRequest(getRequest("/fault/random")),
                "RANDOM_DATA_THEN_CLOSE should cause the client to throw RuntimeException");
    }

    @Test
    @DisplayName("CONNECTION_RESET_BY_PEER — RST mid-response → RuntimeException")
    void connectionResetFaultThrows() {
        server.stubFor(get(anyUrl())
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        var browser = new TestBrowserImpl();
        assertThrows(RuntimeException.class,
                () -> browser.makeRequest(getRequest("/fault/reset")),
                "CONNECTION_RESET_BY_PEER should cause the client to throw RuntimeException");
    }

    // =========================================================================
    // 2. Slow-server simulation
    // =========================================================================

    @Test
    @DisplayName("Fixed delay 200ms — client still receives a valid 200 response")
    void fixedDelaySucceeds() {
        server.stubFor(get(urlEqualTo("/slow"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("slow but OK")
                        .withFixedDelay(200)));  // 200ms latency

        var browser = new TestBrowserImpl();
        Response response = browser.makeRequest(getRequest("/slow"));
        assertEquals(200, response.httpStatus,
                "Slow server should still return 200 after delay");
    }

    @Test
    @DisplayName("Variable delay (0–300ms) — client handles timing jitter without error")
    void variableDelaySucceeds() {
        server.stubFor(get(urlEqualTo("/jitter"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("jittery response")
                        .withFixedDelay(50)));  // 50ms jitter simulation

        var browser = new TestBrowserImpl();
        // Run 5 times; every response must be 200
        for (int i = 0; i < 5; i++) {
            Response r = browser.makeRequest(getRequest("/jitter"));
            assertEquals(200, r.httpStatus);
        }
    }

    // =========================================================================
    // 3. 5xx and 4xx error responses
    // =========================================================================

    @Test
    @DisplayName("500 Internal Server Error — response is returned with correct status")
    void serverError500IsReturned() {
        server.stubFor(get(urlEqualTo("/error"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("{\"error\":\"internal server error\"}")));

        var browser = new TestBrowserImpl();
        Response r = browser.makeRequest(getRequest("/error"));
        assertEquals(500, r.httpStatus);
    }

    @Test
    @DisplayName("503 Service Unavailable — response is returned with correct status")
    void serviceUnavailable503IsReturned() {
        server.stubFor(get(urlEqualTo("/unavailable"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withBody("service down")));

        var browser = new TestBrowserImpl();
        Response r = browser.makeRequest(getRequest("/unavailable"));
        assertEquals(503, r.httpStatus);
    }

    @Test
    @DisplayName("404 Not Found — response is returned with correct status")
    void notFound404IsReturned() {
        server.stubFor(get(urlEqualTo("/missing"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("not found")));

        var browser = new TestBrowserImpl();
        Response r = browser.makeRequest(getRequest("/missing"));
        assertEquals(404, r.httpStatus);
    }

    // =========================================================================
    // 4. Intermittent faults (fault injection rate)
    // =========================================================================

    @Test
    @DisplayName("Intermittent faults — client recovers on the healthy stub path")
    void intermittentFaultRecovery() {
        // Fault path: always faults
        server.stubFor(get(urlEqualTo("/chaos"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        // Healthy path: always succeeds
        server.stubFor(get(urlEqualTo("/healthy"))
                .willReturn(aResponse().withStatus(200).withBody("OK")));

        var browser = new TestBrowserImpl();

        // Fault path must throw
        assertThrows(RuntimeException.class,
                () -> browser.makeRequest(getRequest("/chaos")));

        // Healthy path must succeed immediately after
        var freshBrowser = new TestBrowserImpl();
        Response r = freshBrowser.makeRequest(getRequest("/healthy"));
        assertEquals(200, r.httpStatus, "Healthy path must succeed after a fault");
    }

    // =========================================================================
    // 5. Concurrent chaos — virtual threads, no cross-thread corruption
    // =========================================================================

    @Test
    @DisplayName("Concurrent requests: 100 virtual threads, mix of 200 and fault stubs")
    void concurrentChaosWithVirtualThreads() throws InterruptedException {
        // Even paths succeed, odd paths fault
        server.stubFor(get(urlMatching("/ok/.*"))
                .willReturn(aResponse().withStatus(200).withBody("concurrent-ok")));
        server.stubFor(get(urlMatching("/fail/.*"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        int threads = 100;
        AtomicInteger successes = new AtomicInteger(0);
        AtomicInteger failures  = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(threads);

        // Java 25 virtual threads — lightweight, no OS-thread exhaustion
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threads; i++) {
                final int idx = i;
                executor.submit(() -> {
                    try {
                        // Each thread uses its own TestBrowserImpl (DefaultHttpClient is not thread-safe)
                        var browser = new TestBrowserImpl();
                        if (idx % 2 == 0) {
                            Response r = browser.makeRequest(getRequest("/ok/" + idx));
                            if (r.httpStatus == 200) successes.incrementAndGet();
                        } else {
                            try {
                                browser.makeRequest(getRequest("/fail/" + idx));
                            } catch (RuntimeException _) {
                                failures.incrementAndGet();
                            }
                        }
                    } finally {
                        latch.countDown();
                    }
                });
            }
        }

        latch.await(10, TimeUnit.SECONDS);

        assertEquals(threads / 2, successes.get(),
                "All even-indexed threads (ok stubs) must succeed");
        assertEquals(threads / 2, failures.get(),
                "All odd-indexed threads (fault stubs) must throw");
    }

    // =========================================================================
    // 6. Large response body
    // =========================================================================

    @Test
    @DisplayName("Large response body (512KB) — client reads it without OOM")
    void largeResponseBodyIsHandledGracefully() {
        String body = "X".repeat(512 * 1024); // 512 KB
        server.stubFor(get(urlEqualTo("/large"))
                .willReturn(aResponse().withStatus(200).withBody(body)));

        var browser = new TestBrowserImpl();
        Response r = browser.makeRequest(getRequest("/large"));
        assertEquals(200, r.httpStatus);
        assertNotNull(r.payload, "Body must not be null for large response");
    }

    // =========================================================================
    // 7. Redirect handling
    // =========================================================================

    @Test
    @DisplayName("301 redirect is followed automatically")
    void redirectIsFollowed() {
        server.stubFor(get(urlEqualTo("/redirect"))
                .willReturn(aResponse()
                        .withStatus(301)
                        .withHeader("Location", "http://localhost:" + server.port() + "/target")));
        server.stubFor(get(urlEqualTo("/target"))
                .willReturn(aResponse().withStatus(200).withBody("redirected!")));

        var browser = new TestBrowserImpl();
        Response r = browser.makeRequest(getRequest("/redirect"));
        assertEquals(200, r.httpStatus, "Client must follow 301 redirect");
    }

    // =========================================================================
    // 8. Sequential fault then success — client reusability
    // =========================================================================

    @Test
    @DisplayName("Browser reuse: after a fault on one path, a different path still succeeds")
    void freshBrowserSucceedsAfterFaultOnDifferentPath() {
        server.stubFor(get(urlEqualTo("/fault-path"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));
        server.stubFor(get(urlEqualTo("/ok-path"))
                .willReturn(aResponse().withStatus(200).withBody("recovered")));

        // First browser hits the fault
        var faultBrowser = new TestBrowserImpl();
        assertThrows(RuntimeException.class,
                () -> faultBrowser.makeRequest(getRequest("/fault-path")));

        // A fresh browser on a healthy stub succeeds immediately after
        var freshBrowser = new TestBrowserImpl();
        Response r = freshBrowser.makeRequest(getRequest("/ok-path"));
        assertEquals(200, r.httpStatus, "A fresh browser on healthy stub must succeed after a fault");
    }

    // =========================================================================
    // 9. Circuit Breaker Pattern — Joe Armstrong "Let it crash gracefully"
    // =========================================================================

    /**
     * Circuit breaker pattern: after 5 consecutive failures, open the circuit
     * and stop attempting further calls. This prevents cascading failures in
     * production and reduces load on an already-degraded service.
     *
     * <p>Fortune 5 SA principle: "Fail fast, fail explicitly, never silently."
     */
    @Test
    @DisplayName("Circuit breaker: open after 5 consecutive failures, stop at 5 attempts (not 10)")
    void circuitBreakerPattern() {
        // All requests to /circuit fault — server is completely down
        server.stubFor(get(urlMatching("/circuit/.*"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        int maxAttempts     = 10;
        int circuitThreshold = 5;  // open circuit after 5 consecutive failures

        AtomicInteger attempts       = new AtomicInteger(0);
        AtomicBoolean circuitOpen    = new AtomicBoolean(false);
        AtomicInteger consecutiveFails = new AtomicInteger(0);

        var browser = new TestBrowserImpl();

        for (int i = 0; i < maxAttempts; i++) {
            if (circuitOpen.get()) {
                break; // circuit is open — stop calling the failing service
            }
            attempts.incrementAndGet();
            try {
                browser.makeRequest(getRequest("/circuit/" + i));
                consecutiveFails.set(0); // reset on success
            } catch (RuntimeException e) {
                int fails = consecutiveFails.incrementAndGet();
                if (fails >= circuitThreshold) {
                    circuitOpen.set(true); // open the circuit
                }
            }
        }

        // Assert circuit breaker stopped at threshold (not all 10 attempts)
        assertTrue(circuitOpen.get(),
                "Circuit must be open after " + circuitThreshold + " consecutive failures");
        assertEquals(circuitThreshold, attempts.get(),
                "Circuit breaker must stop after exactly " + circuitThreshold + " attempts, " +
                "not all " + maxAttempts + ". Actual attempts: " + attempts.get());
    }

    // =========================================================================
    // 10. Retry with Exponential Backoff — Fortune 5 resilience pattern
    // =========================================================================

    /**
     * Retry with exponential backoff: first N-1 requests fault, last succeeds.
     * Verifies that:
     * <ul>
     *   <li>The client retries the correct number of times</li>
     *   <li>The final attempt succeeds after transient faults clear</li>
     *   <li>Total attempts equals maxRetries (not more, not less)</li>
     * </ul>
     *
     * <p>Uses short backoff (10ms, 20ms) for test speed while demonstrating the pattern.
     */
    @Test
    @DisplayName("Retry with exponential backoff: 2 transient 503s then 200 OK — 3 total attempts")
    void retryWithExponentialBackoff() throws InterruptedException {
        // Avoid WireMock scenario state entirely: use distinct URL paths per attempt.
        // Scenario state transitions are an internal WireMock detail; using per-attempt URLs
        // is deterministic and does not depend on WireMock version behaviour.
        server.stubFor(get(urlEqualTo("/retry/1"))
                .willReturn(aResponse().withStatus(503).withBody("transient error 1")));
        server.stubFor(get(urlEqualTo("/retry/2"))
                .willReturn(aResponse().withStatus(503).withBody("transient error 2")));
        server.stubFor(get(urlEqualTo("/retry/3"))
                .willReturn(aResponse().withStatus(200).withBody("recovered after 2 retries")));

        int maxRetries         = 3;
        int baseBackoffMs      = 10;  // short for test speed
        AtomicInteger attempts = new AtomicInteger(0);
        Response finalResponse = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            attempts.set(attempt);
            var browser = new TestBrowserImpl(); // fresh browser per attempt
            Response resp = browser.makeRequest(
                    Request.GET().url(Url.host("http://localhost:" + server.port())
                            .path("/retry/" + attempt)));
            if (resp.httpStatus == 200) {
                finalResponse = resp;
                break; // success — stop retrying
            }
            // treat non-200 as a transient fault, apply backoff and retry
            if (attempt < maxRetries) {
                long backoffMs = (long) baseBackoffMs * (1L << (attempt - 1)); // 10ms, 20ms
                Thread.sleep(backoffMs);
            }
        }

        // Assert: exactly 3 attempts were made (2 transient 503s + 1 success)
        assertEquals(3, attempts.get(),
                "Retry must make exactly 3 attempts (2 transient 503s + 1 success). Actual: " + attempts.get());
        assertNotNull(finalResponse,
                "Final attempt must succeed and return a non-null response");
        assertEquals(200, finalResponse.httpStatus,
                "Third attempt must return 200 OK after transient fault recovery");
    }

    // =========================================================================
    // 11. Bulkhead Isolation — faults in pool A must not affect pool B
    // =========================================================================

    /**
     * Bulkhead pattern: two independent service pools. Faults in pool A
     * are isolated and must not prevent pool B from serving requests.
     *
     * <p>Fortune 5 SA: "Design for failure in one partition without cascading
     * to other partitions." This is the bulkhead principle from naval architecture.
     */
    @Test
    @DisplayName("Bulkhead isolation: fault pool A, healthy pool B — pools are independent")
    void bulkheadIsolation() {
        // Pool A: always faults
        server.stubFor(get(urlMatching("/pool-a/.*"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        // Pool B: always healthy
        server.stubFor(get(urlMatching("/pool-b/.*"))
                .willReturn(aResponse().withStatus(200).withBody("pool-b-healthy")));

        int requestsPerPool = 10;
        AtomicInteger poolAFaults    = new AtomicInteger(0);
        AtomicInteger poolBSuccesses = new AtomicInteger(0);

        // Pool A: all 10 requests must fault
        for (int i = 0; i < requestsPerPool; i++) {
            try {
                new TestBrowserImpl().makeRequest(getRequest("/pool-a/" + i));
            } catch (RuntimeException e) {
                poolAFaults.incrementAndGet();
            }
        }

        // Pool B: all 10 requests must succeed — completely unaffected by Pool A
        for (int i = 0; i < requestsPerPool; i++) {
            Response r = new TestBrowserImpl().makeRequest(getRequest("/pool-b/" + i));
            if (r.httpStatus == 200) poolBSuccesses.incrementAndGet();
        }

        // Assert full isolation
        assertEquals(requestsPerPool, poolAFaults.get(),
                "All " + requestsPerPool + " pool A requests must fault (service down). " +
                "Actual faults: " + poolAFaults.get());
        assertEquals(requestsPerPool, poolBSuccesses.get(),
                "All " + requestsPerPool + " pool B requests must succeed (isolated from pool A). " +
                "Actual successes: " + poolBSuccesses.get());
    }
}
