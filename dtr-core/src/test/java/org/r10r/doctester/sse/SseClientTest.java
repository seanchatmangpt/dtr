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

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for SSE client implementation.
 */
class SseClientTest {

    private HttpServer server;
    private int port;
    private SseClientImpl client;

    @BeforeEach
    void setUp() throws IOException {
        // Start test server on a random port
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        server.start();
        client = SseClientImpl.create();
    }

    @AfterEach
    void tearDown() {
        if (client != null) {
            // Client doesn't need explicit close since it uses the shared HttpClient
        }
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void create_shouldReturnNewInstance() {
        var newClient = SseClientImpl.create();
        assertThat(newClient, is(notNullValue()));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_shouldReceiveEvents() throws Exception {
        // Set up SSE endpoint
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                // Send 3 events
                for (int i = 1; i <= 3; i++) {
                    String event = "data: Event " + i + "\n\n";
                    os.write(event.getBytes(StandardCharsets.UTF_8));
                    os.flush();
                    sleep(50);
                }
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        subscription.awaitEvents(3, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThat(events.size(), is(3));
        assertThat(events.get(0).data(), containsString("Event 1"));
        assertThat(events.get(1).data(), containsString("Event 2"));
        assertThat(events.get(2).data(), containsString("Event 3"));

        subscription.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withEventAndId_shouldParseCorrectly() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                String event = "id: 123\nevent: custom\ndata: Test data\n\n";
                os.write(event.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        subscription.awaitEvents(1, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThat(events.size(), is(1));
        assertThat(events.get(0).id().isPresent(), is(true));
        assertThat(events.get(0).id().get(), equalTo("123"));
        assertThat(events.get(0).event().isPresent(), is(true));
        assertThat(events.get(0).event().get(), equalTo("custom"));
        assertThat(events.get(0).data(), equalTo("Test data"));

        subscription.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withMultilineData_shouldCombineData() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                String event = "data: Line 1\ndata: Line 2\ndata: Line 3\n\n";
                os.write(event.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        subscription.awaitEvents(1, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThat(events.size(), is(1));
        assertThat(events.get(0).data(), equalTo("Line 1\nLine 2\nLine 3"));

        subscription.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withHeaders_shouldIncludeHeaders() throws Exception {
        server.createContext("/events", exchange -> {
            // Check for custom header
            var authHeader = exchange.getRequestHeaders().getFirst("Authorization");
            if (!"Bearer test-token".equals(authHeader)) {
                exchange.sendResponseHeaders(401, -1);
                return;
            }

            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write("data: authorized\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });

        var headers = new java.util.HashMap<String, String>();
        headers.put("Authorization", "Bearer test-token");

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"), headers);

        subscription.awaitEvents(1, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThat(events.size(), is(1));
        assertThat(events.get(0).data(), equalTo("authorized"));

        subscription.close();
    }

    @Test
    void subscribe_toNonexistentEndpoint_shouldThrow() {
        assertThrows(IllegalStateException.class, () -> {
            client.subscribe(URI.create("http://localhost:" + port + "/nonexistent"));
        });
    }

    @Test
    void isActive_shouldReturnFalseAfterClose() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            // Keep connection open
            try (OutputStream os = exchange.getResponseBody()) {
                sleep(1000);
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        assertThat(subscription.isActive(), is(true));

        subscription.close();

        assertThat(subscription.isActive(), is(false));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void getReceivedEvents_shouldReturnUnmodifiableList() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write("data: test\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        subscription.awaitEvents(1, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThrows(UnsupportedOperationException.class, () -> {
            events.add(SseEvent.of("test"));
        });

        subscription.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void sseEvent_staticFactories_shouldWork() {
        var event1 = SseEvent.of("data only");
        assertThat(event1.data(), equalTo("data only"));
        assertThat(event1.id().isPresent(), is(false));
        assertThat(event1.event().isPresent(), is(false));

        var event2 = SseEvent.of("custom-event", "data with type");
        assertThat(event2.data(), equalTo("data with type"));
        assertThat(event2.event().isPresent(), is(true));
        assertThat(event2.event().get(), equalTo("custom-event"));

        var event3 = SseEvent.of("123", "event-type", "full data");
        assertThat(event3.id().isPresent(), is(true));
        assertThat(event3.id().get(), equalTo("123"));
        assertThat(event3.event().isPresent(), is(true));
        assertThat(event3.event().get(), equalTo("event-type"));
        assertThat(event3.data(), equalTo("full data"));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withCommentLines_shouldIgnoreComments() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                // Comment line should be ignored
                os.write(": this is a comment\n".getBytes(StandardCharsets.UTF_8));
                os.write("data: actual data\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        subscription.awaitEvents(1, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThat(events.size(), is(1));
        assertThat(events.get(0).data(), equalTo("actual data"));

        subscription.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void awaitEvents_shouldThrowOnTimeout() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            // Don't send any events - just keep connection open briefly
            try (OutputStream os = exchange.getResponseBody()) {
                sleep(500);
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        // Request 5 events but only 0 will arrive
        assertThrows(RuntimeException.class, () -> {
            subscription.awaitEvents(5, Duration.ofMillis(100));
        });

        subscription.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withErrorStatus_shouldThrow() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.sendResponseHeaders(500, -1);
        });

        assertThrows(IllegalStateException.class, () -> {
            client.subscribe(URI.create("http://localhost:" + port + "/events"));
        });
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withUnauthorizedStatus_shouldThrow() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.sendResponseHeaders(401, -1);
        });

        assertThrows(IllegalStateException.class, () -> {
            client.subscribe(URI.create("http://localhost:" + port + "/events"));
        });
    }

    @Test
    void sseEvent_timestamp_shouldBeSet() {
        var before = java.time.Instant.now();
        var event = SseEvent.of("test data");
        var after = java.time.Instant.now();

        assertThat(event.timestamp().isAfter(before.minusMillis(1)), is(true));
        assertThat(event.timestamp().isBefore(after.plusMillis(1)), is(true));
    }

    @Test
    void close_shouldBeIdempotent() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                sleep(500);
            }
        });

        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"));

        // Close multiple times should not throw
        subscription.close();
        subscription.close();
        subscription.close();

        assertThat(subscription.isActive(), is(false));
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void subscribe_withNullHeaders_shouldUseEmptyMap() throws Exception {
        server.createContext("/events", exchange -> {
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, 0);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write("data: test\n\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        });

        // Pass null headers - should not throw NPE
        var subscription = client.subscribe(URI.create("http://localhost:" + port + "/events"), null);

        subscription.awaitEvents(1, Duration.ofSeconds(3));

        var events = subscription.getReceivedEvents();
        assertThat(events.size(), is(1));

        subscription.close();
    }

    @Test
    void create_withCustomHttpClient_shouldUseProvidedClient() {
        var customClient = java.net.http.HttpClient.newHttpClient();
        var sseClient = SseClientImpl.create(customClient);
        assertThat(sseClient, is(notNullValue()));
    }

    // Helper method to avoid checked exception in lambdas
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
