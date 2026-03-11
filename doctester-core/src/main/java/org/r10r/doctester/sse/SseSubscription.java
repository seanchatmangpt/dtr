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
package org.r10r.doctester.sse;

import java.time.Duration;
import java.util.List;

/**
 * Represents an active SSE (Server-Sent Events) subscription.
 *
 * <p>Subscriptions are created by {@link SseClient#subscribe(java.net.URI)} and
 * provide methods to receive events from the server.
 *
 * <p>Usage:
 * <pre>{@code
 * try (SseSubscription subscription = client.subscribe(uri)) {
 *     subscription.awaitEvents(3, Duration.ofSeconds(10));
 *
 *     for (SseEvent event : subscription.getReceivedEvents()) {
 *         System.out.println("Event: " + event.event().orElse("message"));
 *         System.out.println("Data: " + event.data());
 *     }
 * }
 * }</pre>
 *
 * <p>Subscriptions implement {@link AutoCloseable} and should be closed when no longer needed.
 *
 * @see SseClient
 * @see SseEvent
 */
public interface SseSubscription extends AutoCloseable {

    /**
     * Returns all events received from the server since the subscription was created.
     *
     * @return list of received events
     */
    List<SseEvent> getReceivedEvents();

    /**
     * Blocks until the specified number of events have been received or the timeout expires.
     *
     * @param count the number of events to wait for
     * @param timeout maximum time to wait
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws java.util.concurrent.TimeoutException if the timeout expires before receiving the events
     */
    void awaitEvents(int count, Duration timeout) throws InterruptedException;

    /**
     * Checks if the subscription is still active.
     *
     * @return true if the subscription is active, false if closed or errored
     */
    boolean isActive();

    /**
     * Closes the SSE subscription.
     *
     * <p>After closing, no more events will be received.
     */
    @Override
    void close();
}
