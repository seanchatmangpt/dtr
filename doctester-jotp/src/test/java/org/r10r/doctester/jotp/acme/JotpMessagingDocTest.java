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
package org.r10r.doctester.jotp.acme;

import org.acme.MessageBus;
import org.acme.MessageBus.Subscription;
import org.junit.Test;
import org.r10r.doctester.DocTester;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.is;

/**
 * DocTester documentation for the JOTP reactive messaging patterns — specifically
 * {@link MessageBus}: topic-based routing, wildcard subscriptions, message envelopes,
 * and subscription lifecycle.
 *
 * <p>Sourced from <a href="https://github.com/seanchatmangpt/java-maven-template">
 * seanchatmangpt/java-maven-template</a> (git submodule at {@code jotp/}).
 *
 * <p>Covers: {@link MessageBus#create()}, {@link MessageBus.Envelope},
 * {@link MessageBus.Subscription}, topic routing, wildcard pattern matching,
 * publish/subscribe lifecycle, and header-enriched envelopes.
 */
public class JotpMessagingDocTest extends DocTester {

    // =========================================================================
    // MessageBus — publish / subscribe
    // =========================================================================

    @Test
    public void testPublishSubscribeBasic() throws Exception {
        sayNextSection("MessageBus — Topic-Based Publish/Subscribe (Enterprise Integration Pattern)");
        say("Joe Armstrong: <em>\"In Erlang, all communication is via message passing. "
                + "The message bus is the universal integration layer — every process talks "
                + "to every other process through it.\"</em> "
                + "<code>MessageBus.create()</code> produces a bus with a default in-memory "
                + "message store. <code>subscribe(topic, handler)</code> returns a "
                + "<code>Subscription</code> handle; <code>publish(topic, payload)</code> "
                + "delivers an <code>Envelope</code> to all matching subscribers.");

        MessageBus bus = MessageBus.create();

        var received = new ArrayList<Object>();
        var latch    = new CountDownLatch(1);

        Subscription sub = bus.subscribe("orders.created", envelope -> {
            received.add(envelope.payload());
            latch.countDown();
        });

        bus.publish("orders.created", "order-123");

        boolean delivered = latch.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("message delivered to subscriber", true, is(delivered));
        sayAndAssertThat("received payload is 'order-123'", "order-123", is(received.get(0)));
        sayAndAssertThat("subscription is active",          true, is(sub.isActive()));
        sayAndAssertThat("subscription topic is 'orders.created'",
                "orders.created", is(sub.topic()));
    }

    @Test
    public void testSubscriptionCancel() throws Exception {
        sayNextSection("Subscription.cancel() — Unsubscribe from a Topic");
        say("Calling <code>sub.cancel()</code> removes the handler. Subsequent publishes "
                + "to the same topic are no longer delivered to the cancelled subscriber. "
                + "Other active subscribers on the same topic are unaffected.");

        MessageBus bus = MessageBus.create();
        var count = new int[]{0};

        Subscription sub = bus.subscribe("events.tick", env -> count[0]++);
        bus.publish("events.tick", "first");
        Thread.sleep(50);

        sub.cancel();
        bus.publish("events.tick", "second"); // must NOT reach cancelled sub
        Thread.sleep(50);

        sayAndAssertThat("subscription is inactive after cancel()", false, is(sub.isActive()));
        sayAndAssertThat("only the pre-cancel message was delivered", 1, is(count[0]));
    }

    @Test
    public void testMultipleSubscribersOnSameTopic() throws Exception {
        sayNextSection("MessageBus — Fan-Out: Multiple Subscribers on Same Topic");
        say("The message bus fans out a single publish to all active subscribers "
                + "on the same topic. Each subscriber receives its own copy of the envelope.");

        MessageBus bus   = MessageBus.create();
        var latch         = new CountDownLatch(3);
        var received      = new ArrayList<String>();

        bus.subscribe("metrics.cpu", env -> { received.add("sub-A"); latch.countDown(); });
        bus.subscribe("metrics.cpu", env -> { received.add("sub-B"); latch.countDown(); });
        bus.subscribe("metrics.cpu", env -> { received.add("sub-C"); latch.countDown(); });

        bus.publish("metrics.cpu", 87.5);

        boolean allDelivered = latch.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("all 3 subscribers receive the message", true, is(allDelivered));
        sayAndAssertThat("3 deliveries total", 3, is(received.size()));
    }

    @Test
    public void testTopicIsolation() throws Exception {
        sayNextSection("MessageBus — Topic Isolation: Different Topics Don't Interfere");
        say("A subscriber on topic <code>A</code> does not receive messages published "
                + "to topic <code>B</code>. Topics are independent routing keys.");

        MessageBus bus      = MessageBus.create();
        var latchA           = new CountDownLatch(1);
        var wrongDeliveries  = new int[]{0};

        bus.subscribe("topic.A", env -> latchA.countDown());
        bus.subscribe("topic.B", env -> wrongDeliveries[0]++);

        bus.publish("topic.A", "hello-A");
        boolean aDelivered = latchA.await(2, TimeUnit.SECONDS);

        // give bus time to process any erroneous deliveries
        Thread.sleep(50);

        sayAndAssertThat("topic.A subscriber receives the message",    true, is(aDelivered));
        sayAndAssertThat("topic.B subscriber does NOT receive it",     0,    is(wrongDeliveries[0]));
    }

    @Test
    public void testEnvelopeMetadata() throws Exception {
        sayNextSection("MessageBus.Envelope — Message Metadata");
        say("Every published message is wrapped in an <code>Envelope</code> carrying "
                + "metadata: the topic string, a UTC timestamp, a correlation UUID, "
                + "and a headers map. The static factory <code>Envelope.of(topic, payload)</code> "
                + "populates defaults automatically.");

        MessageBus bus = MessageBus.create();
        var latch       = new CountDownLatch(1);
        var captured    = new MessageBus.Envelope[1];

        bus.subscribe("audit.log", env -> {
            captured[0] = env;
            latch.countDown();
        });

        bus.publish("audit.log", "user-created");
        latch.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("envelope.topic() matches published topic",
                "audit.log", is(captured[0].topic()));
        sayAndAssertThat("envelope.payload() is the published object",
                "user-created", is(captured[0].payload()));
        sayAndAssertThat("envelope.timestamp() is not null",
                true, is(captured[0].timestamp() != null));
        sayAndAssertThat("envelope.correlationId() is a valid UUID",
                true, is(captured[0].correlationId() instanceof UUID));
    }

    @Test
    public void testEnvelopeWithHeaders() throws Exception {
        sayNextSection("MessageBus.Envelope.of(topic, payload, headers) — Custom Headers");
        say("Envelopes can carry custom string headers for routing metadata, tracing, "
                + "or content-type negotiation. Headers are an immutable <code>Map&lt;String,String&gt;</code> "
                + "accessible via <code>envelope.headers()</code>.");

        MessageBus bus  = MessageBus.create();
        var latch        = new CountDownLatch(1);
        var captured     = new MessageBus.Envelope[1];

        bus.subscribe("telemetry.raw", env -> {
            captured[0] = env;
            latch.countDown();
        });

        var headers = java.util.Map.of("content-type", "application/json", "source", "sensor-42");
        bus.publish(MessageBus.Envelope.of("telemetry.raw", "{speed:200}", headers));

        latch.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("header 'content-type' is 'application/json'",
                "application/json", is(captured[0].headers().get("content-type")));
        sayAndAssertThat("header 'source' is 'sensor-42'",
                "sensor-42", is(captured[0].headers().get("source")));
    }

    @Test
    public void testNamedBusCreate() {
        sayNextSection("MessageBus.create(name) — Named Message Bus");
        say("A named bus is useful when multiple independent buses coexist in the same "
                + "JVM. Names are for identification and debugging only — they do not "
                + "affect routing semantics.");

        MessageBus bus = MessageBus.create("telemetry-bus");

        sayAndAssertThat("named bus created successfully", true, is(bus != null));
    }

    @Test
    public void testBuilderAPI() throws Exception {
        sayNextSection("MessageBus.builder() — Fluent Builder API");
        say("The builder API allows configuration of the bus name, message store, "
                + "and dead-letter handler before creation. All settings are optional — "
                + "defaults are applied for any omitted configuration.");

        MessageBus bus = MessageBus.builder()
                .name("order-bus")
                .build();

        var latch = new CountDownLatch(1);
        bus.subscribe("order.placed", env -> latch.countDown());
        bus.publish("order.placed", "order-456");

        boolean ok = latch.await(2, TimeUnit.SECONDS);

        sayAndAssertThat("builder-constructed bus delivers messages", true, is(ok));
    }
}
