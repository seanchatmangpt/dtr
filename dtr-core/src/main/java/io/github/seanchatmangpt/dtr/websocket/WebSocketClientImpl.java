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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Implementation of {@link WebSocketClient} using the Java-WebSocket library.
 *
 * <p>This implementation provides WebSocket client functionality for connecting
 * to WebSocket servers and exchanging messages during tests.
 *
 * <p>Usage:
 * <pre>{@code
 * try (WebSocketClient client = WebSocketClientImpl.create()) {
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
public final class WebSocketClientImpl implements WebSocketClient {

    private final List<WebSocketSessionImpl> sessions = new CopyOnWriteArrayList<>();
    private volatile boolean closed = false;

    private WebSocketClientImpl() {
        // Private constructor - use create() factory method
    }

    /**
     * Creates a new WebSocket client instance.
     *
     * @return a new WebSocketClient
     */
    public static WebSocketClientImpl create() {
        return new WebSocketClientImpl();
    }

    @Override
    public WebSocketSession connect(URI uri) {
        return connect(uri, Collections.emptyMap());
    }

    @Override
    public WebSocketSession connect(URI uri, Map<String, String> headers) {
        if (closed) {
            throw new IllegalStateException("WebSocket client has been closed");
        }

        var session = new WebSocketSessionImpl(uri, headers != null ? headers : Collections.emptyMap());
        sessions.add(session);
        session.connectBlocking();
        return session;
    }

    @Override
    public void close() {
        closed = true;
        for (var session : sessions) {
            try {
                session.close();
            } catch (Exception e) {
                // Ignore errors during close
            }
        }
        sessions.clear();
    }
}
