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
package io.github.seanchatmangpt.dtr.websocket;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of {@link WebSocketSession} using the Java-WebSocket library.
 *
 * <p>This implementation provides WebSocket session functionality for sending
 * and receiving messages from a WebSocket server.
 */
public final class WebSocketSessionImpl implements WebSocketSession {

    private final InternalWebSocketClient client;
    private final List<WebSocketMessage> receivedMessages = new CopyOnWriteArrayList<>();
    private final CountDownLatch connectionLatch = new CountDownLatch(1);
    private volatile boolean connected = false;
    private volatile Throwable connectionError;

    /**
     * Creates a new WebSocket session.
     *
     * @param uri the WebSocket URI to connect to
     * @param headers HTTP headers to include in the handshake
     */
    WebSocketSessionImpl(URI uri, Map<String, String> headers) {
        this.client = new InternalWebSocketClient(uri, headers);
    }

    /**
     * Connects to the WebSocket server and blocks until connected.
     *
     * @throws IllegalStateException if connection fails
     */
    void connectBlocking() {
        try {
            client.connect();
            // Wait for connection with timeout
            if (!connectionLatch.await(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Connection timeout");
            }
            if (connectionError != null) {
                throw new IllegalStateException("Connection failed: " + connectionError.getMessage(), connectionError);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Connection interrupted", e);
        }
    }

    @Override
    public void sendText(String message) {
        if (!connected) {
            throw new IllegalStateException("Session is not connected");
        }
        client.send(message);
    }

    @Override
    public void sendBinary(byte[] data) {
        if (!connected) {
            throw new IllegalStateException("Session is not connected");
        }
        client.send(ByteBuffer.wrap(data));
    }

    @Override
    public List<WebSocketMessage> getReceivedMessages() {
        return Collections.unmodifiableList(List.copyOf(receivedMessages));
    }

    @Override
    public void awaitMessages(int count, Duration timeout) throws InterruptedException {
        var latch = new CountDownLatch(count);
        var startTime = System.currentTimeMillis();

        // Check if we already have enough messages
        while (receivedMessages.size() < count) {
            long elapsed = System.currentTimeMillis() - startTime;
            long remaining = timeout.toMillis() - elapsed;

            if (remaining <= 0) {
                throw new RuntimeException(
                    new TimeoutException("Timeout waiting for " + count + " messages. Received: " + receivedMessages.size())
                );
            }

            Thread.sleep(Math.min(50, remaining));
        }
    }

    @Override
    public boolean isOpen() {
        return connected && client.isOpen();
    }

    @Override
    public void close() {
        connected = false;
        try {
            client.close();
        } catch (Exception e) {
            // Ignore errors during close
        }
    }

    /**
     * Internal WebSocket client that handles callbacks.
     */
    private class InternalWebSocketClient extends WebSocketClient {

        InternalWebSocketClient(URI serverUri, Map<String, String> headers) {
            super(serverUri);
            if (headers != null) {
                for (var entry : headers.entrySet()) {
                    addHeader(entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public void onOpen(ServerHandshake handshake) {
            connected = true;
            connectionLatch.countDown();
        }

        @Override
        public void onMessage(String message) {
            receivedMessages.add(new WebSocketMessage.Text(message, Instant.now()));
        }

        @Override
        public void onMessage(ByteBuffer bytes) {
            byte[] data = new byte[bytes.remaining()];
            bytes.get(data);
            receivedMessages.add(new WebSocketMessage.Binary(data, Instant.now()));
        }

        @Override
        public void onClose(int code, String reason, boolean remote) {
            connected = false;
            connectionLatch.countDown();
        }

        @Override
        public void onError(Exception ex) {
            connectionError = ex;
            receivedMessages.add(new WebSocketMessage.Error(ex, Instant.now()));
            connectionLatch.countDown();
        }
    }
}
