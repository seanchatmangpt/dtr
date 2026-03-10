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
package org.r10r.doctester.reactive;

import org.apache.http.cookie.Cookie;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.testbrowser.TestBrowserImpl;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reactive, virtual-thread-backed HTTP test browser that wraps the classic
 * synchronous {@link TestBrowserImpl} with asynchronous and concurrent
 * execution capabilities.
 *
 * <h2>Reactive Messaging Patterns: Virtual-Thread Messaging &amp; Async Request-Reply</h2>
 *
 * <h3>Pattern 1 — Virtual-Thread Messaging</h3>
 *
 * <p>Java 25 virtual threads ({@link Thread#ofVirtual()}) are
 * <em>lightweight user-mode threads</em> managed by the JVM.  Unlike platform
 * threads, they do not hold an OS thread while blocked on I/O: the JVM parks
 * the virtual thread and recycles the carrier platform thread immediately.
 * This allows millions of concurrent HTTP calls with the same memory footprint
 * as a few hundred platform threads.
 *
 * <p>This browser uses {@code Executors.newVirtualThreadPerTaskExecutor()} so
 * every {@link #makeRequestAsync(Request)} call runs on its own virtual thread
 * without thread-pool sizing or back-pressure configuration.
 *
 * <h3>Pattern 2 — Async Request-Reply</h3>
 *
 * <p>The {@link #makeRequestAsync(Request)} method returns a
 * {@link CompletableFuture}&lt;{@link Response}&gt;, implementing the
 * <em>Async Request-Reply</em> pattern: the caller does not block while the
 * network round-trip completes.  Futures can be chained, combined, and
 * joined:
 *
 * <pre>{@code
 * var f1 = browser.makeRequestAsync(Request.GET().url(url1));
 * var f2 = browser.makeRequestAsync(Request.GET().url(url2));
 *
 * CompletableFuture.allOf(f1, f2).join();
 * assertThat(f1.join().httpStatus, equalTo(200));
 * assertThat(f2.join().httpStatus, equalTo(200));
 * }</pre>
 *
 * <h3>Pattern 3 — Scatter-Gather (concurrent fan-out)</h3>
 *
 * <p>{@link #makeRequestsConcurrently(List)} implements the
 * <em>Scatter-Gather</em> pattern: a list of requests is <em>scattered</em>
 * concurrently across virtual threads and the responses are
 * <em>gathered</em> back in original order once all have completed.
 *
 * <h3>Event integration</h3>
 *
 * <p>Every request/response pair is announced to the {@link TestEventBus},
 * publishing {@link TestEvent.RequestDispatched} before the call and
 * {@link TestEvent.ResponseReceived} (with elapsed time) after.
 *
 * @author DocTester Reactive Extension
 * @see TestEventBus
 * @see RequestPipeline
 */
public final class ReactiveTestBrowser implements TestBrowser {

    /** Synchronous delegate that performs the actual Apache HttpClient calls. */
    private final TestBrowserImpl delegate;

    /** Event bus to which every request/response pair is announced. */
    private final TestEventBus eventBus;

    /** Virtual-thread executor shared across all async requests. */
    private final ExecutorService virtualThreadExecutor =
            Executors.newVirtualThreadPerTaskExecutor();

    /**
     * Creates a new {@code ReactiveTestBrowser} that publishes events to the
     * supplied bus.
     *
     * @param eventBus the event bus to publish request/response events to
     */
    public ReactiveTestBrowser(TestEventBus eventBus) {
        this.delegate = new TestBrowserImpl();
        this.eventBus = eventBus;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TestBrowser contract — synchronous (for backward compatibility)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes an HTTP request synchronously, publishing events around the
     * call.  This method satisfies the {@link TestBrowser} contract so that
     * this browser can be used anywhere a {@link TestBrowser} is expected.
     *
     * @param request the request to execute
     * @return the HTTP response
     */
    @Override
    public Response makeRequest(Request request) {
        var start = Instant.now();
        eventBus.publish(new TestEvent.RequestDispatched(request, start));

        var response = delegate.makeRequest(request);

        var elapsed = Duration.between(start, Instant.now());
        eventBus.publish(
            new TestEvent.ResponseReceived(request, response, elapsed, Instant.now()));

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Async Request-Reply
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes an HTTP request asynchronously on a <em>virtual thread</em>.
     *
     * <p>Returns immediately with a {@link CompletableFuture} that completes
     * when the response has been received and all events published.  The
     * calling thread is never blocked.
     *
     * @param request the request to execute
     * @return a future that resolves to the HTTP response
     */
    public CompletableFuture<Response> makeRequestAsync(Request request) {
        return CompletableFuture.supplyAsync(
            () -> makeRequest(request),
            virtualThreadExecutor);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scatter-Gather
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes a list of HTTP requests <em>concurrently</em> using virtual
     * threads and gathers all responses in original request order.
     *
     * <p>This implements the <em>Scatter-Gather</em> messaging pattern:
     * <ol>
     *   <li><strong>Scatter</strong> — each request is submitted as an
     *       independent {@link CompletableFuture} on a virtual thread.</li>
     *   <li><strong>Gather</strong> — {@link CompletableFuture#allOf} waits
     *       for every future to complete, then the responses are collected
     *       preserving the original ordering.</li>
     * </ol>
     *
     * <p>If any single request throws an exception the whole gather fails fast
     * and the returned future completes exceptionally.
     *
     * @param requests the requests to scatter; order is preserved in the result
     * @return responses in the same order as {@code requests}
     */
    public List<Response> makeRequestsConcurrently(List<Request> requests) {
        var futures = requests.stream()
            .map(this::makeRequestAsync)
            .toList();

        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();

        return futures.stream()
            .map(CompletableFuture::join)
            .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cookie management — delegated unchanged
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<Cookie> getCookies() {
        return delegate.getCookies();
    }

    @Override
    public Cookie getCookieWithName(String name) {
        return delegate.getCookieWithName(name);
    }

    @Override
    public void clearCookies() {
        delegate.clearCookies();
    }
}
