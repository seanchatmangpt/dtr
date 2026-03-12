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

import java.time.Instant;
import java.util.Optional;

/**
 * Represents a Server-Sent Event (SSE) received from a server.
 *
 * <p>SSE events follow the format defined in the
 * <a href="https://html.spec.whatwg.org/multipage/server-sent-events.html">HTML specification</a>:
 * <pre>
 * id: event-id
 * event: event-type
 * data: event-data
 * </pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * for (SseEvent event : subscription.getReceivedEvents()) {
 *     System.out.println("Event: " + event.event().orElse("message"));
 *     System.out.println("Data: " + event.data());
 * }
 * }</pre>
 *
 * @param id optional event ID for reconnection purposes
 * @param event optional event type (defaults to "message" if not specified)
 * @param data the event data (required)
 * @param timestamp when the event was received
 */
public record SseEvent(
    Optional<String> id,
    Optional<String> event,
    String data,
    Instant timestamp
) {

    /**
     * Creates a simple SSE event with just data.
     *
     * @param data the event data
     * @return a new SseEvent with no id or event type
     */
    public static SseEvent of(String data) {
        return new SseEvent(Optional.empty(), Optional.empty(), data, Instant.now());
    }

    /**
     * Creates an SSE event with an event type.
     *
     * @param event the event type
     * @param data the event data
     * @return a new SseEvent
     */
    public static SseEvent of(String event, String data) {
        return new SseEvent(Optional.empty(), Optional.of(event), data, Instant.now());
    }

    /**
     * Creates an SSE event with id, event type, and data.
     *
     * @param id the event ID
     * @param event the event type
     * @param data the event data
     * @return a new SseEvent
     */
    public static SseEvent of(String id, String event, String data) {
        return new SseEvent(Optional.of(id), Optional.of(event), data, Instant.now());
    }
}
