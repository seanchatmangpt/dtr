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
import java.net.http.HttpClient;
import java.util.Collections;
import java.util.Map;

/**
 * Implementation of {@link SseClient} using the standard Java HTTP client.
 *
 * <p>This implementation provides SSE (Server-Sent Events) client functionality
 * for subscribing to SSE endpoints and receiving events during tests.
 *
 * <p>Usage:
 * <pre>{@code
 * try (SseClient client = SseClientImpl.create()) {
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
public final class SseClientImpl implements SseClient {

    private final HttpClient httpClient;

    private SseClientImpl(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Creates a new SSE client instance with a default HTTP client.
     *
     * @return a new SseClient
     */
    public static SseClientImpl create() {
        return new SseClientImpl(HttpClient.newHttpClient());
    }

    /**
     * Creates a new SSE client instance with a custom HTTP client.
     *
     * @param httpClient the HTTP client to use
     * @return a new SseClient
     */
    public static SseClientImpl create(HttpClient httpClient) {
        return new SseClientImpl(httpClient);
    }

    @Override
    public SseSubscription subscribe(URI uri) {
        return subscribe(uri, Collections.emptyMap());
    }

    @Override
    public SseSubscription subscribe(URI uri, Map<String, String> headers) {
        var subscription = new SseSubscriptionImpl(uri, headers != null ? headers : Collections.emptyMap(), httpClient);
        subscription.start();
        return subscription;
    }
}
