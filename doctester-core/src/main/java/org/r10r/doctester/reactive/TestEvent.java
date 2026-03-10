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
package org.r10r.doctester.reactive;

import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.time.Duration;
import java.time.Instant;

/**
 * Sealed interface hierarchy representing all observable events in a DocTester
 * test session. This is the core message type for the reactive messaging system.
 *
 * <p>Uses Java 25 sealed interfaces combined with records to create an exhaustive,
 * type-safe event algebra. Each event variant carries only the data relevant to
 * that event type, making pattern matching precise and allocation-efficient.
 *
 * <h2>Reactive Messaging Pattern: Event-Driven Message Types</h2>
 *
 * <p>This sealed hierarchy implements the <em>Message Type</em> pattern from
 * Enterprise Integration Patterns (Hohpe &amp; Woolf, 2003). Each record is an
 * immutable message that flows through the {@link TestEventBus}. Sealed permits
 * guarantee compile-time exhaustiveness in switch expressions — every consumer
 * must handle every event type, preventing silent failures when new event types
 * are added.
 *
 * <p>Example switch expression over all event types:
 * <pre>{@code
 * String summary = switch (event) {
 *     case TestEvent.TestStarted(var cls, var method, _) ->
 *         "Started: " + cls + "#" + method;
 *     case TestEvent.RequestDispatched(var req, _) ->
 *         "→ " + req.httpRequestType + " " + req.uri;
 *     case TestEvent.ResponseReceived(_, var resp, var elapsed, _) ->
 *         "← " + resp.httpStatus + " in " + elapsed.toMillis() + "ms";
 *     case TestEvent.AssertionPassed(var msg, _, _) ->
 *         "✓ " + msg;
 *     case TestEvent.AssertionFailed(var msg, _, var reason, _) ->
 *         "✗ " + msg + ": " + reason;
 *     case TestEvent.SectionAdded(var title, _) ->
 *         "§ " + title;
 *     case TestEvent.TextAdded(var text, _) ->
 *         "  " + text;
 *     case TestEvent.TestCompleted(var cls, var method, var elapsed, _) ->
 *         "Completed: " + cls + "#" + method + " in " + elapsed.toMillis() + "ms";
 *     case TestEvent.TestFailed(var cls, var method, var cause, _) ->
 *         "FAILED: " + cls + "#" + method + " — " + cause.getMessage();
 * };
 * }</pre>
 *
 * @author DocTester Reactive Extension
 * @see TestEventBus
 */
public sealed interface TestEvent
        permits TestEvent.TestStarted,
                TestEvent.SectionAdded,
                TestEvent.TextAdded,
                TestEvent.RequestDispatched,
                TestEvent.ResponseReceived,
                TestEvent.AssertionPassed,
                TestEvent.AssertionFailed,
                TestEvent.TestCompleted,
                TestEvent.TestFailed {

    /**
     * The instant this event was created. Present on all event types, enabling
     * temporal ordering and latency measurement across the event stream.
     *
     * @return the wall-clock time when this event occurred
     */
    Instant occurredAt();

    // ─────────────────────────────────────────────────────────────────────────
    // Lifecycle events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Emitted when a test method begins execution.
     *
     * @param testClass  fully-qualified name of the test class
     * @param testMethod simple name of the test method
     * @param occurredAt wall-clock time of test start
     */
    record TestStarted(
            String testClass,
            String testMethod,
            Instant occurredAt) implements TestEvent {}

    /**
     * Emitted when a documentation section heading ({@code sayNextSection}) is added.
     *
     * @param title      the section heading text
     * @param occurredAt wall-clock time the section was added
     */
    record SectionAdded(
            String title,
            Instant occurredAt) implements TestEvent {}

    /**
     * Emitted when descriptive prose ({@code say}) is added to documentation.
     *
     * @param text       the paragraph text
     * @param occurredAt wall-clock time the text was added
     */
    record TextAdded(
            String text,
            Instant occurredAt) implements TestEvent {}

    // ─────────────────────────────────────────────────────────────────────────
    // HTTP request/response events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Emitted immediately before an HTTP request is sent to the server.
     * Consumers can use this for logging, metrics, or request interception.
     *
     * @param request    the request about to be dispatched
     * @param occurredAt wall-clock time the request was dispatched
     */
    record RequestDispatched(
            Request request,
            Instant occurredAt) implements TestEvent {}

    /**
     * Emitted after an HTTP response has been received and parsed.
     * Includes the elapsed round-trip time for latency tracking.
     *
     * @param request    the originating request
     * @param response   the parsed HTTP response
     * @param elapsed    round-trip duration from dispatch to receipt
     * @param occurredAt wall-clock time the response arrived
     */
    record ResponseReceived(
            Request request,
            Response response,
            Duration elapsed,
            Instant occurredAt) implements TestEvent {}

    // ─────────────────────────────────────────────────────────────────────────
    // Assertion events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Emitted when an assertion passes ({@code sayAndAssertThat} succeeds).
     *
     * @param message    the human-readable assertion label
     * @param actual     the value that was asserted
     * @param occurredAt wall-clock time the assertion was evaluated
     */
    record AssertionPassed(
            String message,
            Object actual,
            Instant occurredAt) implements TestEvent {}

    /**
     * Emitted when an assertion fails ({@code sayAndAssertThat} throws).
     *
     * @param message    the human-readable assertion label
     * @param actual     the value that failed the assertion
     * @param reason     the matcher failure description
     * @param occurredAt wall-clock time the assertion was evaluated
     */
    record AssertionFailed(
            String message,
            Object actual,
            String reason,
            Instant occurredAt) implements TestEvent {}

    // ─────────────────────────────────────────────────────────────────────────
    // Terminal events
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Emitted when a test method completes without throwing.
     *
     * @param testClass  fully-qualified name of the test class
     * @param testMethod simple name of the test method
     * @param elapsed    total wall-clock duration of the test
     * @param occurredAt wall-clock time of test completion
     */
    record TestCompleted(
            String testClass,
            String testMethod,
            Duration elapsed,
            Instant occurredAt) implements TestEvent {}

    /**
     * Emitted when a test method fails with an uncaught exception.
     *
     * @param testClass  fully-qualified name of the test class
     * @param testMethod simple name of the test method
     * @param cause      the exception that caused the failure
     * @param occurredAt wall-clock time of the failure
     */
    record TestFailed(
            String testClass,
            String testMethod,
            Throwable cause,
            Instant occurredAt) implements TestEvent {}
}
