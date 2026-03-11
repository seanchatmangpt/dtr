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

import java.time.Instant;

/**
 * Sealed interface representing WebSocket messages received from a server.
 *
 * <p>This sealed hierarchy allows pattern matching on different message types:
 * <pre>{@code
 * switch (message) {
 *     case WebSocketMessage.Text t -> handleText(t.payload());
 *     case WebSocketMessage.Binary b -> handleBinary(b.payload());
 *     case WebSocketMessage.Error e -> handleError(e.cause());
 * }
 * }</pre>
 *
 * @see WebSocketSession
 */
public sealed interface WebSocketMessage {

    /**
     * Returns the timestamp when this message was received or error occurred.
     *
     * @return the timestamp of this message
     */
    Instant timestamp();

    /**
     * Text message received from the WebSocket server.
     *
     * @param payload the text content of the message
     * @param timestamp when the message was received
     */
    record Text(String payload, Instant timestamp) implements WebSocketMessage {}

    /**
     * Binary message received from the WebSocket server.
     *
     * @param payload the binary content of the message
     * @param timestamp when the message was received
     */
    record Binary(byte[] payload, Instant timestamp) implements WebSocketMessage {}

    /**
     * Error that occurred during WebSocket communication.
     *
     * @param cause the exception that caused the error
     * @param timestamp when the error occurred
     */
    record Error(Throwable cause, Instant timestamp) implements WebSocketMessage {}
}
