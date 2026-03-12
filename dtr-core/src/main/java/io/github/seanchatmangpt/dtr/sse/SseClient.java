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
package io.github.seanchatmangpt.dtr.sse;

import java.net.URI;
import java.util.Map;

/**
 * Client for subscribing to Server-Sent Events (SSE) streams.
 *
 * <p>SSE is a server push technology enabling a client to receive automatic
 * updates from a server via an HTTP connection. This client provides a simple
 * way to subscribe to SSE endpoints and receive events.
 *
 * <p>Usage:
 * <pre>{@code
 * try (SseClient client = SseClient.create()) {
 *     SseSubscription subscription = client.subscribe(
 *         URI.create("http://localhost:8080/events"));
 *
 *     subscription.awaitEvents(5, Duration.ofSeconds(10));
 *
 *     for (SseEvent event : subscription.getReceivedEvents()) {
 *         System.out.println("Received: " + event.data());
 *     }
 *
 *     subscription.close();
 * }
 * }</pre>
 *
 * @see SseSubscription
 * @see SseEvent
 */
public interface SseClient {

    /**
     * Subscribes to an SSE endpoint.
     *
     * @param uri the SSE endpoint URI
     * @return an active subscription
     * @throws java.net.ConnectException if the connection cannot be established
     */
    SseSubscription subscribe(URI uri);

    /**
     * Subscribes to an SSE endpoint with custom headers.
     *
     * @param uri the SSE endpoint URI
     * @param headers HTTP headers to include in the request
     * @return an active subscription
     * @throws java.net.ConnectException if the connection cannot be established
     */
    SseSubscription subscribe(URI uri, Map<String, String> headers);
}
