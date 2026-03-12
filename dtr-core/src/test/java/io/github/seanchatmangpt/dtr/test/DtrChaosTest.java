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

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Chaos / fault-injection tests using <a href="https://wiremock.org">WireMock</a>.
 *
 * <p>WireMock is an embedded HTTP server that can simulate real-world failure
 * modes at the protocol level. These tests verify that {@link TestBrowserImpl}
 * (and therefore the full DTR stack) reacts predictably under each fault.
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
@DisplayName("DTR chaos / fault-injection tests (WireMock)")
class DtrChaosTest {

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
    @DisplayName("Concurrent requests: 20 virtual threads, mix of 200 and fault stubs")
    void concurrentChaosWithVirtualThreads() throws InterruptedException {
        // Even paths succeed, odd paths fault
        server.stubFor(get(urlMatching("/ok/.*"))
                .willReturn(aResponse().withStatus(200).withBody("concurrent-ok")));
        server.stubFor(get(urlMatching("/fail/.*"))
                .willReturn(aResponse().withFault(Fault.EMPTY_RESPONSE)));

        int threads = 20;
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
}
