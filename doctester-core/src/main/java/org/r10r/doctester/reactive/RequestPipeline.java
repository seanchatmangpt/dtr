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

import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

/**
 * Composable, async HTTP request pipeline implementing the <em>Pipes and
 * Filters</em> enterprise integration pattern (Hohpe &amp; Woolf, 2003).
 *
 * <h2>Reactive Messaging Pattern: Pipes and Filters / Message Pipeline</h2>
 *
 * <p>A pipeline is a linear chain of processing stages (filters) connected by
 * channels (pipes).  Each filter receives a message, transforms it, and passes
 * the result to the next filter.  The pipeline defined here has two ordered
 * filter sequences:
 *
 * <ol>
 *   <li><strong>Request transformers</strong> — applied before the HTTP call,
 *       in registration order.  Each transformer receives the {@link Request}
 *       produced by the previous stage and returns a new {@link Request}.
 *       Typical uses: add authentication headers, inject correlation IDs,
 *       canonicalise URLs.</li>
 *   <li><strong>Response transformers</strong> — applied after the HTTP
 *       response arrives, in registration order.  Each transformer receives the
 *       {@link Response} produced by the previous stage and returns a new
 *       {@link Response}.  Typical uses: decompress bodies, validate schemas,
 *       record metrics.</li>
 * </ol>
 *
 * <p>Execution is fully asynchronous: each stage runs as a
 * {@link CompletableFuture} continuation on the configured
 * {@link ExecutorService}.  The default executor uses Java 25 <em>virtual
 * threads</em> ({@code Executors.newVirtualThreadPerTaskExecutor()}), so the
 * pipeline can handle thousands of concurrent in-flight requests without
 * exhausting platform threads.
 *
 * <h3>Event integration</h3>
 *
 * <p>The pipeline publishes {@link TestEvent.RequestDispatched} and
 * {@link TestEvent.ResponseReceived} events to a {@link TestEventBus},
 * wiring the Pipes-and-Filters pattern into the broader
 * Publisher-Subscriber topology.
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * var bus      = new TestEventBus();
 * var pipeline = RequestPipeline.builder(bus)
 *     .transformRequest(req -> req.addHeader("X-Correlation-Id", UUID.randomUUID().toString()))
 *     .transformRequest(req -> req.addHeader("Authorization", "Bearer " + token()))
 *     .transformResponse(resp -> logAndReturn(resp))
 *     .build();
 *
 * CompletableFuture<Response> future = pipeline.execute(Request.GET().url(url), browser);
 * future.thenAccept(resp -> assertThat(resp.httpStatus, equalTo(200)));
 * }</pre>
 *
 * @author DocTester Reactive Extension
 * @see TestEventBus
 * @see ReactiveTestBrowser
 */
public final class RequestPipeline {

    private final ExecutorService executor;
    private final List<Function<Request, Request>> requestTransformers;
    private final List<Function<Response, Response>> responseTransformers;
    private final TestEventBus eventBus;

    private RequestPipeline(Builder builder) {
        this.executor             = builder.executor;
        this.requestTransformers  = List.copyOf(builder.requestTransformers);
        this.responseTransformers = List.copyOf(builder.responseTransformers);
        this.eventBus             = builder.eventBus;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Execution
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Executes the full pipeline for the given request asynchronously.
     *
     * <p>Stages execute in this order:
     * <ol>
     *   <li>Apply all request transformers in sequence.</li>
     *   <li>Publish a {@link TestEvent.RequestDispatched} event.</li>
     *   <li>Invoke {@link TestBrowser#makeRequest(Request)} on a virtual thread.</li>
     *   <li>Publish a {@link TestEvent.ResponseReceived} event with elapsed time.</li>
     *   <li>Apply all response transformers in sequence.</li>
     * </ol>
     *
     * <p>Each stage is an asynchronous continuation so no platform thread is
     * ever blocked waiting for I/O.
     *
     * @param request the initial request (before any transformation)
     * @param browser the HTTP client to use for the actual network call
     * @return a {@link CompletableFuture} that completes with the final,
     *         transformed {@link Response}
     */
    public CompletableFuture<Response> execute(Request request, TestBrowser browser) {
        return CompletableFuture
            .supplyAsync(() -> applyRequestTransformers(request), executor)
            .thenApplyAsync(transformedRequest -> {
                var dispatchTime = Instant.now();
                eventBus.publish(
                    new TestEvent.RequestDispatched(transformedRequest, dispatchTime));
                return new RequestTiming(transformedRequest, dispatchTime);
            }, executor)
            .thenApplyAsync(timing -> {
                var response = browser.makeRequest(timing.request());
                var elapsed  = Duration.between(timing.start(), Instant.now());
                eventBus.publish(
                    new TestEvent.ResponseReceived(
                        timing.request(), response, elapsed, Instant.now()));
                return response;
            }, executor)
            .thenApplyAsync(this::applyResponseTransformers, executor);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Request applyRequestTransformers(Request request) {
        var current = request;
        for (var transformer : requestTransformers) {
            current = transformer.apply(current);
        }
        return current;
    }

    private Response applyResponseTransformers(Response response) {
        var current = response;
        for (var transformer : responseTransformers) {
            current = transformer.apply(current);
        }
        return current;
    }

    /**
     * Captures a request and the instant it was dispatched so that round-trip
     * time can be measured after the response arrives.
     */
    private record RequestTiming(Request request, Instant start) {}

    // ─────────────────────────────────────────────────────────────────────────
    // Builder
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new {@link Builder} for constructing a {@link RequestPipeline}.
     *
     * @param eventBus the bus to which dispatch and response events are published
     * @return a fresh builder
     */
    public static Builder builder(TestEventBus eventBus) {
        return new Builder(eventBus);
    }

    /**
     * Fluent builder for {@link RequestPipeline}.
     *
     * <p>Transformers are applied in the order they are added.  A pipeline
     * with no transformers is a pure pass-through that still publishes events.
     */
    public static final class Builder {

        private ExecutorService executor =
                Executors.newVirtualThreadPerTaskExecutor();

        private final List<Function<Request, Request>> requestTransformers =
                new ArrayList<>();

        private final List<Function<Response, Response>> responseTransformers =
                new ArrayList<>();

        private final TestEventBus eventBus;

        private Builder(TestEventBus eventBus) {
            this.eventBus = eventBus;
        }

        /**
         * Overrides the default virtual-thread executor with a custom one.
         * Useful for testing with a direct/synchronous executor.
         *
         * @param executor the executor to use for pipeline stages
         * @return this builder
         */
        public Builder withExecutor(ExecutorService executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Appends a request-transformer stage to the pipeline.
         *
         * <p>The transformer receives the output of the previous stage (or the
         * original request for the first stage) and returns a new
         * {@link Request}.  Transformers <em>must not</em> mutate their
         * input — return a new instance or the same instance unchanged.
         *
         * @param transformer pure function {@code Request → Request}
         * @return this builder
         */
        public Builder transformRequest(Function<Request, Request> transformer) {
            requestTransformers.add(transformer);
            return this;
        }

        /**
         * Appends a response-transformer stage to the pipeline.
         *
         * <p>The transformer receives the {@link Response} produced by the
         * previous stage and returns a (potentially different) {@link Response}.
         *
         * @param transformer pure function {@code Response → Response}
         * @return this builder
         */
        public Builder transformResponse(Function<Response, Response> transformer) {
            responseTransformers.add(transformer);
            return this;
        }

        /**
         * Builds the {@link RequestPipeline} with all registered transformers
         * and the configured executor.
         *
         * @return a new, immutable {@link RequestPipeline}
         */
        public RequestPipeline build() {
            return new RequestPipeline(this);
        }
    }
}
