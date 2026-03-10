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

import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Reactive extension of {@link DocTester} that unifies all five reactive
 * messaging patterns implemented in this package into a single, convenient
 * test base class.
 *
 * <h2>Integrated Reactive Messaging Patterns</h2>
 *
 * <table border="1" cellpadding="4">
 *   <caption>Pattern summary</caption>
 *   <tr><th>Pattern</th><th>Class</th><th>Exposed via</th></tr>
 *   <tr>
 *     <td>Sealed Event Algebra</td>
 *     <td>{@link TestEvent}</td>
 *     <td>{@link #publishEvent}/{@link #onEvent}</td>
 *   </tr>
 *   <tr>
 *     <td>Publisher-Subscriber</td>
 *     <td>{@link TestEventBus}</td>
 *     <td>{@link #onEvent(Consumer)}</td>
 *   </tr>
 *   <tr>
 *     <td>Pipes and Filters</td>
 *     <td>{@link RequestPipeline}</td>
 *     <td>{@link #sayAndMakeRequestAsync(Request)}</td>
 *   </tr>
 *   <tr>
 *     <td>Async Request-Reply + Virtual Threads</td>
 *     <td>{@link ReactiveTestBrowser}</td>
 *     <td>{@link #sayAndMakeRequestAsync(Request)}</td>
 *   </tr>
 *   <tr>
 *     <td>Scatter-Gather</td>
 *     <td>{@link ReactiveTestBrowser#makeRequestsConcurrently}</td>
 *     <td>{@link #sayAndMakeRequestsConcurrently(List)}</td>
 *   </tr>
 * </table>
 *
 * <h3>Default event logging</h3>
 *
 * <p>The constructor registers a default subscriber on the {@link TestEventBus}
 * that prints a concise one-line summary for every event to {@code stdout}.
 * Sub-classes may suppress this by calling
 * {@link TestEventBus#complete()} before the constructor registers the
 * listener, or they may add additional subscribers with {@link #onEvent}.
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * public class MyApiDocTest extends ReactiveDocTester {
 *
 *     @Override
 *     public Url testServerUrl() { return Url.host("http://localhost:8080"); }
 *
 *     @Test
 *     public void testListUsers() throws Exception {
 *         sayNextSection("User API — list");
 *         say("GET /users returns 200 with a JSON array");
 *
 *         // Async pipeline request
 *         CompletableFuture<Response> future =
 *             sayAndMakeRequestAsync(Request.GET().url(testServerUrl().path("/users")));
 *
 *         Response resp = future.get();
 *         sayAndAssertThat("HTTP 200", 200, equalTo(resp.httpStatus));
 *     }
 *
 *     @Test
 *     public void testConcurrentUsers() {
 *         var requests = List.of(
 *             Request.GET().url(testServerUrl().path("/users/1")),
 *             Request.GET().url(testServerUrl().path("/users/2")),
 *             Request.GET().url(testServerUrl().path("/users/3")));
 *
 *         List<Response> responses = sayAndMakeRequestsConcurrently(requests);
 *         responses.forEach(r -> sayAndAssertThat("OK", 200, equalTo(r.httpStatus)));
 *     }
 * }
 * }</pre>
 *
 * @author DocTester Reactive Extension
 */
public abstract class ReactiveDocTester extends DocTester {

    /** Central event bus shared across all reactive components for this test class. */
    protected final TestEventBus eventBus;

    /** Default pipeline — pass-through with event publishing only. */
    protected final RequestPipeline pipeline;

    /** The reactive browser created by {@link #getTestBrowser()}. */
    protected ReactiveTestBrowser reactiveBrowser;

    /**
     * Initialises the reactive infrastructure and registers a default
     * event-logging subscriber.
     */
    protected ReactiveDocTester() {
        this.eventBus = new TestEventBus();
        this.pipeline = RequestPipeline.builder(eventBus).build();
        registerDefaultLoggingSubscriber();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TestBrowser factory — override to inject reactive browser
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns a {@link ReactiveTestBrowser} wired to the shared
     * {@link TestEventBus}.  Called once per test method by
     * {@link DocTester#setupForTestCaseMethod()}.
     *
     * @return a fresh {@link ReactiveTestBrowser}
     */
    @Override
    public TestBrowser getTestBrowser() {
        reactiveBrowser = new ReactiveTestBrowser(eventBus);
        return reactiveBrowser;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Async Request-Reply (via pipeline)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Documents and executes an HTTP request <em>asynchronously</em> through
     * the reactive pipeline.
     *
     * <p>A brief prose note is added to the documentation immediately.  The
     * actual HTTP call runs on a virtual thread; the returned future resolves
     * when the response (and all response transformers) have completed.
     *
     * @param request the request to execute
     * @return a future that resolves to the HTTP response
     */
    public CompletableFuture<Response> sayAndMakeRequestAsync(Request request) {
        say("Async request: " + request.httpRequestType + " " + request.uri);
        return pipeline.execute(request, reactiveBrowser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Scatter-Gather
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Documents and executes a list of requests <em>concurrently</em> using
     * the Scatter-Gather pattern, then returns all responses in original order.
     *
     * @param requests the requests to fan out; must not be {@code null}
     * @return responses in the same order as {@code requests}
     */
    public List<Response> sayAndMakeRequestsConcurrently(List<Request> requests) {
        say("Concurrent fan-out: " + requests.size() + " requests via virtual threads");
        var responses = reactiveBrowser.makeRequestsConcurrently(requests);
        say("All " + responses.size() + " responses gathered");
        return responses;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Event bus access
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Publishes a custom {@link TestEvent} to the shared event bus.
     * Useful for annotating the event stream with domain-specific events.
     *
     * @param event the event to publish; must not be {@code null}
     */
    public void publishEvent(TestEvent event) {
        eventBus.publish(event);
    }

    /**
     * Registers an additional event handler on the shared event bus.
     *
     * <p>Handlers are invoked on virtual threads; they should not perform
     * long-running blocking work inline.
     *
     * @param handler lambda invoked for each subsequent event
     */
    public void onEvent(Consumer<TestEvent> handler) {
        eventBus.subscribe(handler);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Default logging subscriber
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a compact console logger that prints one line per event.
     * Uses exhaustive record-pattern switch to handle all {@link TestEvent}
     * variants.
     */
    private void registerDefaultLoggingSubscriber() {
        eventBus.subscribe(event -> {
            String line = switch (event) {
                case TestEvent.TestStarted(var cls, var method, _) ->
                    "[START ] " + cls + "#" + method;

                case TestEvent.SectionAdded(var title, _) ->
                    "[SECT  ] § " + title;

                case TestEvent.TextAdded(var text, _) ->
                    "[TEXT  ] " + text;

                case TestEvent.RequestDispatched(var req, _) ->
                    "[SEND  ] → " + req.httpRequestType + " " + req.uri;

                case TestEvent.ResponseReceived(_, var resp, var elapsed, _) ->
                    "[RECV  ] ← " + resp.httpStatus
                        + " (" + elapsed.toMillis() + " ms)";

                case TestEvent.AssertionPassed(var msg, _, _) ->
                    "[PASS  ] ✓ " + msg;

                case TestEvent.AssertionFailed(var msg, _, var reason, _) ->
                    "[FAIL  ] ✗ " + msg + " — " + reason;

                case TestEvent.TestCompleted(var cls, var method, var elapsed, _) ->
                    "[DONE  ] " + cls + "#" + method
                        + " (" + elapsed.toMillis() + " ms)";

                case TestEvent.TestFailed(var cls, var method, var cause, _) ->
                    "[ERROR ] " + cls + "#" + method
                        + " — " + cause.getMessage();
            };
            System.out.println(line);
        });
    }
}
