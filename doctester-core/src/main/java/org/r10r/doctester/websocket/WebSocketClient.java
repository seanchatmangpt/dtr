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

import java.net.URI;
import java.util.Map;

/**
 * Client for establishing WebSocket connections.
 *
 * <p>Implementations of this interface provide WebSocket client functionality
 * for connecting to WebSocket servers and exchanging messages.
 *
 * <p>Usage:
 * <pre>{@code
 * try (WebSocketClient client = WebSocketClient.create()) {
 *     WebSocketSession session = client.connect(URI.create("ws://localhost:8080/ws"));
 *
 *     session.sendText("Hello!");
 *     session.awaitMessages(1, Duration.ofSeconds(5));
 *
 *     var messages = session.getReceivedMessages();
 *     // process messages...
 *
 *     session.close();
 * }
 * }</pre>
 *
 * @see WebSocketSession
 * @see WebSocketMessage
 */
public interface WebSocketClient extends AutoCloseable {

    /**
     * Connects to a WebSocket server at the specified URI.
     *
     * @param uri the WebSocket URI (ws:// or wss://)
     * @return an active WebSocket session
     * @throws java.net.ConnectException if the connection cannot be established
     */
    WebSocketSession connect(URI uri);

    /**
     * Connects to a WebSocket server with custom headers.
     *
     * @param uri the WebSocket URI (ws:// or wss://)
     * @param headers HTTP headers to include in the handshake
     * @return an active WebSocket session
     * @throws java.net.ConnectException if the connection cannot be established
     */
    WebSocketSession connect(URI uri, Map<String, String> headers);

    /**
     * Closes the client and releases all resources.
     *
     * <p>Any active sessions will be closed.
     */
    @Override
    void close();
}
