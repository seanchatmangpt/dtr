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

import java.time.Duration;
import java.util.List;

/**
 * Represents an active WebSocket session with a server.
 *
 * <p>Sessions are created by {@link WebSocketClient#connect(java.net.URI)} and
 * provide methods to send messages and receive messages from the server.
 *
 * <p>Usage:
 * <pre>{@code
 * try (WebSocketSession session = client.connect(uri)) {
 *     session.sendText("Hello, Server!");
 *     session.awaitMessages(1, Duration.ofSeconds(5));
 *
 *     for (WebSocketMessage msg : session.getReceivedMessages()) {
 *         if (msg instanceof WebSocketMessage.Text t) {
 *             System.out.println("Received: " + t.payload());
 *         }
 *     }
 * }
 * }</pre>
 *
 * <p>Sessions implement {@link AutoCloseable} and should be closed when no longer needed.
 *
 * @see WebSocketClient
 * @see WebSocketMessage
 */
public interface WebSocketSession extends AutoCloseable {

    /**
     * Sends a text message to the server.
     *
     * @param message the text message to send
     * @throws IllegalStateException if the session is closed
     */
    void sendText(String message);

    /**
     * Sends a binary message to the server.
     *
     * @param data the binary data to send
     * @throws IllegalStateException if the session is closed
     */
    void sendBinary(byte[] data);

    /**
     * Returns all messages received from the server since the session was created
     * or since the last call to this method (depending on implementation).
     *
     * @return list of received messages
     */
    List<WebSocketMessage> getReceivedMessages();

    /**
     * Blocks until the specified number of messages have been received or the timeout expires.
     *
     * @param count the number of messages to wait for
     * @param timeout maximum time to wait
     * @throws InterruptedException if the thread is interrupted while waiting
     * @throws java.util.concurrent.TimeoutException if the timeout expires before receiving the messages
     */
    void awaitMessages(int count, Duration timeout) throws InterruptedException;

    /**
     * Checks if the session is still open and connected.
     *
     * @return true if the session is open, false otherwise
     */
    boolean isOpen();

    /**
     * Closes the WebSocket session.
     *
     * <p>After closing, no more messages can be sent or received.
     */
    @Override
    void close();
}
