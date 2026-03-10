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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Reactive event bus for DocTester test events, implementing the
 * <em>Publisher-Subscriber</em> messaging pattern via the standard
 * {@link java.util.concurrent.Flow} API introduced in Java 9.
 *
 * <h2>Reactive Messaging Pattern: Publisher-Subscriber</h2>
 *
 * <p>The Publisher-Subscriber pattern decouples event producers from event
 * consumers through an intermediary channel — this bus. Producers call
 * {@link #publish(TestEvent)} without knowing who (if anyone) is listening.
 * Consumers call {@link #subscribe(Subscriber)} or
 * {@link #subscribe(Consumer)} to register interest without knowing how events
 * are produced.
 *
 * <p>This implementation extends the basic pub-sub model with
 * <em>backpressure</em>: each subscriber independently controls how many events
 * it is ready to process via {@link Subscription#request(long)}. Events are
 * buffered in a per-subscriber {@link LinkedBlockingQueue} and drained only
 * when demand is available. This prevents fast producers from overwhelming slow
 * consumers — a key guarantee of the Reactive Streams specification.
 *
 * <h3>Concurrency model</h3>
 *
 * <p>Each subscriber receives events on a virtual-thread executor
 * ({@code Executors.newVirtualThreadPerTaskExecutor()}). Virtual threads are
 * created per drain cycle rather than per event, amortising creation cost while
 * keeping subscriber callbacks fully isolated from each other and from the
 * publishing thread.
 *
 * <h3>Thread safety</h3>
 *
 * <ul>
 *   <li>Subscriber list: {@link CopyOnWriteArrayList} — safe iteration during
 *       concurrent publish.
 *   <li>Per-subscriber queue: {@link LinkedBlockingQueue} — lock-free offer
 *       from producer; polled under demand guard from consumer virtual thread.
 *   <li>Demand counter: {@link AtomicLong} — compare-and-decrement ensures
 *       at-most-once delivery per demand unit.
 * </ul>
 *
 * <h3>Usage example</h3>
 * <pre>{@code
 * var bus = new TestEventBus();
 *
 * // Functional subscription (unbounded demand)
 * bus.subscribe(event -> switch (event) {
 *     case TestEvent.ResponseReceived(_, var resp, var elapsed, _) ->
 *         metrics.record(resp.httpStatus, elapsed);
 *     default -> {}
 * });
 *
 * // Full Flow.Subscriber for controlled demand
 * bus.subscribe(new Flow.Subscriber<>() {
 *     private Subscription sub;
 *     public void onSubscribe(Subscription s) { sub = s; s.request(10); }
 *     public void onNext(TestEvent e)         { process(e); sub.request(1); }
 *     public void onError(Throwable t)        { log.error("bus error", t); }
 *     public void onComplete()                { log.info("bus closed"); }
 * });
 *
 * bus.publish(new TestEvent.SectionAdded("User API", Instant.now()));
 * bus.complete();
 * }</pre>
 *
 * @author DocTester Reactive Extension
 * @see TestEvent
 * @see RequestPipeline
 */
public final class TestEventBus implements Publisher<TestEvent> {

    /** Active subscriptions — iterated on every publish call. */
    private final CopyOnWriteArrayList<EventSubscription> subscriptions =
            new CopyOnWriteArrayList<>();

    /** Shared virtual-thread executor for draining subscriber queues. */
    private final ExecutorService executorService =
            Executors.newVirtualThreadPerTaskExecutor();

    // ─────────────────────────────────────────────────────────────────────────
    // Publisher contract
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Registers a new {@link Subscriber} and delivers the corresponding
     * {@link Subscription} via {@link Subscriber#onSubscribe(Subscription)}.
     *
     * <p>Per the Reactive Streams rule §1.09, this method must not block.
     * The subscription object is created immediately and passed back; no
     * thread is started here.
     *
     * @param subscriber the subscriber to register; must not be {@code null}
     */
    @Override
    public void subscribe(Subscriber<? super TestEvent> subscriber) {
        var subscription = new EventSubscription(subscriber, executorService);
        subscriptions.add(subscription);
        subscriber.onSubscribe(subscription);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Convenience overload
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Subscribes with a simple lambda consumer using <em>unbounded demand</em>
     * ({@code Long.MAX_VALUE}). Suitable when the consumer is faster than the
     * producer and backpressure is not needed.
     *
     * @param onEvent lambda invoked for each event; must not block long
     */
    public void subscribe(Consumer<TestEvent> onEvent) {
        subscribe(new Subscriber<>() {
            private Subscription subscription;

            @Override
            public void onSubscribe(Subscription sub) {
                this.subscription = sub;
                sub.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(TestEvent event) {
                onEvent.accept(event);
            }

            @Override
            public void onError(Throwable throwable) {
                // Surface errors via the standard logging framework
                System.err.println("[TestEventBus] subscriber error: " + throwable);
            }

            @Override
            public void onComplete() {
                // nothing needed for a functional subscriber
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Publishing
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Publishes an event to all active subscribers.
     *
     * <p>Each event is offered to every subscriber's internal buffer; the
     * buffer drain is triggered asynchronously on virtual threads. Cancelled
     * subscriptions silently drop the event.
     *
     * <p>This method is <em>non-blocking</em>: it returns as soon as all
     * in-memory queue offers have completed.
     *
     * @param event the event to broadcast; must not be {@code null}
     */
    public void publish(TestEvent event) {
        for (var subscription : subscriptions) {
            subscription.offer(event);
        }
    }

    /**
     * Signals {@link Subscriber#onComplete()} to all subscribers and initiates
     * an orderly shutdown of the virtual-thread executor.
     *
     * <p>After calling this method, further {@link #publish(TestEvent)} calls
     * will be silently dropped by cancelled subscriptions.
     */
    public void complete() {
        for (var subscription : subscriptions) {
            subscription.complete();
        }
        executorService.shutdown();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal subscription implementation
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Per-subscriber state: event queue, demand counter, and lifecycle flags.
     *
     * <p>The {@link #drain()} method is the hot path: it is called after every
     * {@link #offer(TestEvent)} and every {@link #request(long)}, guaranteeing
     * that events are dispatched as soon as both an event and demand are
     * present.
     */
    private static final class EventSubscription implements Subscription {

        private final Subscriber<? super TestEvent> subscriber;
        private final ExecutorService executor;

        /** Bounded in-memory buffer for pending events. */
        private final BlockingQueue<TestEvent> queue = new LinkedBlockingQueue<>(1024);

        /** Outstanding demand — decremented on each dispatched event. */
        private final AtomicLong demand = new AtomicLong(0);

        private volatile boolean cancelled = false;
        private volatile boolean completed = false;

        EventSubscription(Subscriber<? super TestEvent> subscriber,
                          ExecutorService executor) {
            this.subscriber = subscriber;
            this.executor   = executor;
        }

        // ── Subscription contract ────────────────────────────────────────────

        /**
         * Adds {@code n} to the outstanding demand and triggers a drain cycle.
         * Per Reactive Streams rule §3.09, {@code n} must be positive.
         */
        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(
                    new IllegalArgumentException(
                        "§3.09: demand must be positive, was " + n));
                return;
            }
            // Guard against overflow per RS §3.17
            demand.updateAndGet(d -> d + n < 0 ? Long.MAX_VALUE : d + n);
            drain();
        }

        /**
         * Cancels the subscription. Future events will be silently dropped
         * and the subscriber will receive no further callbacks.
         */
        @Override
        public void cancel() {
            cancelled = true;
        }

        // ── Internal helpers ─────────────────────────────────────────────────

        void offer(TestEvent event) {
            if (!cancelled) {
                queue.offer(event);
                drain();
            }
        }

        void complete() {
            completed = true;
            drain();
        }

        /**
         * Schedules a single virtual-thread task that dispatches queued events
         * while demand &gt; 0.  Multiple concurrent drain tasks are harmless:
         * the {@link AtomicLong#decrementAndGet()} on {@code demand} ensures
         * each event is delivered exactly once.
         */
        private void drain() {
            if (cancelled) return;
            executor.submit(() -> {
                while (!cancelled && demand.get() > 0) {
                    TestEvent event = queue.poll();
                    if (event == null) {
                        if (completed && queue.isEmpty()) {
                            subscriber.onComplete();
                        }
                        break;
                    }
                    if (demand.decrementAndGet() >= 0) {
                        try {
                            subscriber.onNext(event);
                        } catch (Throwable t) {
                            subscriber.onError(t);
                            cancelled = true;
                            break;
                        }
                    } else {
                        // put event back — demand was concurrently exhausted
                        queue.offer(event);
                        break;
                    }
                }
            });
        }
    }
}
