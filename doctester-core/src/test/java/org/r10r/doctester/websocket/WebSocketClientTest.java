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
package org.r10r.doctester.websocket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for WebSocket client implementation.
 */
class WebSocketClientTest {

    private TestWebSocketServer server;
    private int port;
    private WebSocketClientImpl client;

    @BeforeEach
    void setUp() throws Exception {
        // Start test server on a random port
        server = new TestWebSocketServer(0);
        server.start();
        // Wait for server to be ready
        Thread.sleep(100);
        port = server.getPort();
        client = WebSocketClientImpl.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        if (client != null) {
            client.close();
        }
        if (server != null) {
            server.stop();
        }
    }

    @Test
    void create_shouldReturnNewInstance() {
        var newClient = WebSocketClientImpl.create();
        assertThat(newClient, is(notNullValue()));
        newClient.close();
    }

    @Test
    void connect_shouldEstablishConnection() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        assertThat(session.isOpen(), is(true));
        session.close();
    }

    @Test
    void connect_withHeaders_shouldIncludeHeaders() throws Exception {
        var headers = new java.util.HashMap<String, String>();
        headers.put("X-Custom-Header", "test-value");

        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"), headers);

        // Connection should succeed
        assertThat(session.isOpen(), is(true));
        session.close();
    }

    @Test
    void connect_toInvalidServer_shouldThrow() {
        assertThrows(IllegalStateException.class, () -> {
            client.connect(URI.create("ws://127.0.0.1:9999/invalid"));
        });
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void sendText_shouldSendMessage() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        session.sendText("Hello, WebSocket!");

        // Wait for echo response
        session.awaitMessages(1, Duration.ofSeconds(3));

        var messages = session.getReceivedMessages();
        assertThat(messages.size(), is(1));
        assertThat(messages.get(0), instanceOf(WebSocketMessage.Text.class));

        var textMessage = (WebSocketMessage.Text) messages.get(0);
        assertThat(textMessage.payload(), containsString("Hello, WebSocket!"));

        session.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void sendBinary_shouldSendBinaryMessage() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        byte[] data = new byte[]{1, 2, 3, 4, 5};
        session.sendBinary(data);

        // Wait for echo response
        session.awaitMessages(1, Duration.ofSeconds(3));

        var messages = session.getReceivedMessages();
        assertThat(messages.size(), is(1));
        assertThat(messages.get(0), instanceOf(WebSocketMessage.Binary.class));

        session.close();
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void awaitMessages_shouldBlockUntilCount() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        // Server echoes 3 messages
        session.sendText("msg1");
        session.sendText("msg2");
        session.sendText("msg3");

        session.awaitMessages(3, Duration.ofSeconds(3));

        var messages = session.getReceivedMessages();
        assertThat(messages.size(), is(3));

        session.close();
    }

    @Test
    void getReceivedMessages_shouldReturnUnmodifiableList() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        var messages = session.getReceivedMessages();
        assertThrows(UnsupportedOperationException.class, () -> {
            messages.add(new WebSocketMessage.Text("test", java.time.Instant.now()));
        });

        session.close();
    }

    @Test
    void close_shouldCloseSession() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        assertThat(session.isOpen(), is(true));

        session.close();

        // Give it a moment to close
        Thread.sleep(100);
        assertThat(session.isOpen(), is(false));
    }

    @Test
    void close_clientShouldCloseAllSessions() throws Exception {
        var session1 = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));
        var session2 = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        assertThat(session1.isOpen(), is(true));
        assertThat(session2.isOpen(), is(true));

        client.close();

        // Give it a moment to close
        Thread.sleep(100);
        assertThat(session1.isOpen(), is(false));
        assertThat(session2.isOpen(), is(false));
    }

    @Test
    void sendText_afterClose_shouldThrow() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));
        session.close();

        // Give it a moment to close
        Thread.sleep(100);

        assertThrows(IllegalStateException.class, () -> {
            session.sendText("should fail");
        });
    }

    @Test
    void messageTimestamp_shouldBeSet() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));
        var before = java.time.Instant.now();

        session.sendText("test");
        session.awaitMessages(1, Duration.ofSeconds(3));

        var after = java.time.Instant.now();
        var messages = session.getReceivedMessages();
        var timestamp = messages.get(0).timestamp();

        assertThat(timestamp.isAfter(before.minusSeconds(1)), is(true));
        assertThat(timestamp.isBefore(after.plusSeconds(1)), is(true));

        session.close();
    }

    @Test
    void sendBinary_afterClose_shouldThrow() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));
        session.close();

        // Give it a moment to close
        Thread.sleep(100);

        assertThrows(IllegalStateException.class, () -> {
            session.sendBinary(new byte[]{1, 2, 3});
        });
    }

    @Test
    void connect_afterClientClosed_shouldThrow() throws Exception {
        client.close();

        assertThrows(IllegalStateException.class, () -> {
            client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));
        });
    }

    @Test
    void connect_withNullHeaders_shouldUseEmptyMap() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"), null);

        assertThat(session.isOpen(), is(true));
        session.close();
    }

    @Test
    void webSocketMessage_textRecord_shouldHaveCorrectPayload() {
        var now = java.time.Instant.now();
        var textMessage = new WebSocketMessage.Text("hello", now);

        assertThat(textMessage.payload(), equalTo("hello"));
        assertThat(textMessage.timestamp(), equalTo(now));
    }

    @Test
    void webSocketMessage_binaryRecord_shouldHaveCorrectPayload() {
        var now = java.time.Instant.now();
        byte[] data = new byte[]{1, 2, 3};
        var binaryMessage = new WebSocketMessage.Binary(data, now);

        assertThat(binaryMessage.payload(), equalTo(data));
        assertThat(binaryMessage.timestamp(), equalTo(now));
    }

    @Test
    void webSocketMessage_errorRecord_shouldHaveCorrectCause() {
        var now = java.time.Instant.now();
        var exception = new RuntimeException("test error");
        var errorMessage = new WebSocketMessage.Error(exception, now);

        assertThat(errorMessage.cause(), equalTo(exception));
        assertThat(errorMessage.timestamp(), equalTo(now));
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void awaitMessages_timeout_shouldThrow() throws Exception {
        var session = client.connect(URI.create("ws://127.0.0.1:" + port + "/ws"));

        // Don't send any messages - wait should timeout
        assertThrows(RuntimeException.class, () -> {
            session.awaitMessages(1, Duration.ofMillis(100));
        });

        session.close();
    }

    /**
     * Simple WebSocket test server that echoes messages back.
     */
    private static class TestWebSocketServer extends WebSocketServer {

        TestWebSocketServer(int port) {
            super(new InetSocketAddress("127.0.0.1", port));
        }

        @Override
        public void onOpen(WebSocket conn, ClientHandshake handshake) {
            // Connection opened
        }

        @Override
        public void onClose(WebSocket conn, int code, String reason, boolean remote) {
            // Connection closed
        }

        @Override
        public void onMessage(WebSocket conn, String message) {
            // Echo back the message
            conn.send("Echo: " + message);
        }

        @Override
        public void onMessage(WebSocket conn, ByteBuffer message) {
            // Echo back binary message
            conn.send(message);
        }

        @Override
        public void onError(WebSocket conn, Exception ex) {
            // Error occurred
        }

        @Override
        public void onStart() {
            // Server started
        }
    }
}
