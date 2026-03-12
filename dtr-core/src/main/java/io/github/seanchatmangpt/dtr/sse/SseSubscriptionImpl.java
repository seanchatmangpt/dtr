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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Implementation of {@link SseSubscription} using the standard Java HTTP client.
 *
 * <p>This implementation parses SSE events according to the
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">W3C specification</a>.
 */
public final class SseSubscriptionImpl implements SseSubscription {

    private static final String ACCEPT_HEADER = "text/event-stream";

    private final URI uri;
    private final Map<String, String> headers;
    private final HttpClient httpClient;
    private final List<SseEvent> receivedEvents = new CopyOnWriteArrayList<>();
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);

    private volatile Thread readerThread;
    private volatile HttpResponse<java.io.InputStream> response;

    /**
     * Creates a new SSE subscription.
     *
     * @param uri the SSE endpoint URI
     * @param headers HTTP headers to include in the request
     * @param httpClient the HTTP client to use
     */
    SseSubscriptionImpl(URI uri, Map<String, String> headers, HttpClient httpClient) {
        this.uri = uri;
        this.headers = headers;
        this.httpClient = httpClient;
    }

    /**
     * Starts the SSE subscription.
     *
     * @throws IllegalStateException if the subscription cannot be established
     */
    void start() {
        if (closed.get()) {
            throw new IllegalStateException("Subscription has been closed");
        }

        try {
            var requestBuilder = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", ACCEPT_HEADER)
                .header("Cache-Control", "no-cache")
                .GET();

            // Add custom headers
            for (var entry : headers.entrySet()) {
                requestBuilder.header(entry.getKey(), entry.getValue());
            }

            var request = requestBuilder.build();
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IllegalStateException("SSE subscription failed with status: " + response.statusCode());
            }

            active.set(true);

            // Start reader thread
            readerThread = Thread.ofVirtual().start(this::readEvents);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to establish SSE subscription: " + e.getMessage(), e);
        }
    }

    private void readEvents() {
        try (var reader = new BufferedReader(new InputStreamReader(response.body()))) {
            String line;
            StringBuilder dataBuilder = new StringBuilder();
            String eventId = null;
            String eventType = null;

            while (active.get() && (line = reader.readLine()) != null) {
                if (line.isEmpty()) {
                    // Empty line dispatches the event
                    if (!dataBuilder.isEmpty()) {
                        var event = new SseEvent(
                            Optional.ofNullable(eventId),
                            Optional.ofNullable(eventType),
                            dataBuilder.toString(),
                            Instant.now()
                        );
                        receivedEvents.add(event);

                        // Reset for next event
                        dataBuilder = new StringBuilder();
                        eventId = null;
                        eventType = null;
                    }
                } else if (line.startsWith(":")) {
                    // Comment line, ignore
                } else if (line.contains(":")) {
                    var colonIndex = line.indexOf(':');
                    var field = line.substring(0, colonIndex);
                    var value = line.substring(colonIndex + 1);
                    if (value.startsWith(" ")) {
                        value = value.substring(1);
                    }

                    switch (field) {
                        case "id" -> eventId = value;
                        case "event" -> eventType = value;
                        case "data" -> {
                            if (!dataBuilder.isEmpty()) {
                                dataBuilder.append('\n');
                            }
                            dataBuilder.append(value);
                        }
                        case "retry" -> {
                            // Retry directive - not implemented in this version
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (active.get()) {
                // Log error but don't throw - subscription may have been closed
                System.err.println("SSE read error: " + e.getMessage());
            }
        } finally {
            active.set(false);
        }
    }

    @Override
    public List<SseEvent> getReceivedEvents() {
        return Collections.unmodifiableList(List.copyOf(receivedEvents));
    }

    @Override
    public void awaitEvents(int count, Duration timeout) throws InterruptedException {
        var startTime = System.currentTimeMillis();

        while (receivedEvents.size() < count) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = timeout.toMillis() - elapsed;

            if (remaining <= 0) {
                throw new RuntimeException(
                    new TimeoutException("Timeout waiting for " + count + " events. Received: " + receivedEvents.size())
                );
            }

            Thread.sleep(Math.min(50, remaining));
        }
    }

    @Override
    public boolean isActive() {
        return active.get() && !closed.get();
    }

    @Override
    public void close() {
        closed.set(true);
        active.set(false);

        // Close the response stream
        if (response != null) {
            try {
                response.body().close();
            } catch (IOException e) {
                // Ignore
            }
        }

        // Interrupt reader thread
        if (readerThread != null) {
            readerThread.interrupt();
        }
    }
}
