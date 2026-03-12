# Tutorial: Server-Sent Events for One-Way Real-Time Streams

Learn how to test Server-Sent Events (SSE) with DTR. SSE is a simple HTTP-based protocol for pushing real-time data from server to client — perfect for notifications, live updates, and dashboards.

**Time:** ~35 minutes
**Prerequisites:** Java 25, understanding of HTTP and event streams
**What you'll learn:** How to subscribe to SSE, parse events, handle reconnection, and test push notifications

---

## What Are Server-Sent Events?

Server-Sent Events use regular HTTP to maintain a persistent connection, with the server pushing messages to the client:

```
Client                          Server
  |                               |
  |---- GET /events (upgrade) --->|
  |<----- 200 OK (keep-alive) ----|
  |                               |
  |<------ event: message1 -------|
  |<------ event: message2 -------|
  |<------ event: message3 -------|
  |                               |
  |--------- (persistent) --------|
```

**Key characteristics:**
- ✅ One-way communication (server → client)
- ✅ Standard HTTP (works through proxies, firewalls)
- ✅ Automatic reconnection
- ✅ Event types and data
- ✅ Lower overhead than polling

**When to use SSE vs. WebSocket:**
- **SSE:** Notifications, live updates, dashboards (one-way)
- **WebSocket:** Chat, real-time collaboration (bidirectional)

---

## Step 1 — Subscribe to an Event Stream

Connect to an SSE endpoint:

```java
package com.example;

import org.junit.Test;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ServerSentEventsDocTest extends DTR {

    @Test
    public void subscribeToEventStream() throws Exception {

        sayNextSection("Subscribe to Event Stream");

        say("Server-Sent Events (SSE) allow a server to push real-time data over HTTP. "
            + "The connection stays open, and events stream as they occur.");

        say("Connecting to SSE endpoint: GET /events");

        // Make HTTP request that expects streaming response
        Response response = makeRequest(
            Request.GET()
                .url(testServerUrl().path("/events")));

        say("Connected. Server sent: " + response.httpStatus());

        sayAndAssertThat(
            "SSE stream opened",
            200,
            org.hamcrest.CoreMatchers.equalTo(response.httpStatus()));

        say("Connection established. Listening for events...");
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

---

## Step 2 — Parse Event Stream Data

Read and parse SSE events:

```java
@Test
public void parseEventStreamData() throws Exception {

    sayNextSection("Parse Event Data");

    say("SSE events have a simple text format: field: value, separated by blank lines.");

    // Mock SSE stream for demonstration
    String sseStream = """
        event: userJoined
        data: alice joined at 2025-03-11T10:00:00Z
        id: 1

        event: message
        data: {"from":"alice","text":"Hello everyone!"}
        id: 2

        event: userLeft
        data: alice left at 2025-03-11T10:05:00Z
        id: 3
        """;

    say("Example SSE stream:");
    say("```\n" + sseStream + "\n```");

    // Parse events (in real code, read from HTTP stream)
    List<SseEvent> events = parseSseStream(sseStream);

    say("Parsed " + events.size() + " events from stream");

    for (SseEvent event : events) {
        say("Event [" + event.type + "]: " + event.data);
    }

    sayAndAssertThat(
        "Correct number of events parsed",
        3,
        org.hamcrest.CoreMatchers.equalTo(events.size()));

    sayAndAssertThat(
        "First event is userJoined",
        "userJoined",
        org.hamcrest.CoreMatchers.equalTo(events.get(0).type));
}

static record SseEvent(String type, String data, String id) {}

static List<SseEvent> parseSseStream(String stream) {
    List<SseEvent> events = new ArrayList<>();
    String[] lines = stream.split("\n");

    String currentType = null;
    String currentData = null;
    String currentId = null;

    for (String line : lines) {
        if (line.isEmpty()) {
            if (currentType != null) {
                events.add(new SseEvent(currentType, currentData, currentId));
            }
            currentType = null;
            currentData = null;
            currentId = null;
        } else if (line.startsWith("event: ")) {
            currentType = line.substring(7);
        } else if (line.startsWith("data: ")) {
            currentData = line.substring(6);
        } else if (line.startsWith("id: ")) {
            currentId = line.substring(4);
        }
    }

    if (currentType != null) {
        events.add(new SseEvent(currentType, currentData, currentId));
    }

    return events;
}
```

---

## Step 3 — Listen for Real-Time Events

Stream events and validate them in real-time:

```java
@Test
public void listenForRealtimeEvents() throws Exception {

    sayNextSection("Real-Time Event Listening");

    say("Open an SSE connection and listen for events as they arrive. "
        + "This simulates live notifications or updates.");

    // In real code, this would be an HTTP stream from your server
    // For testing, we'll use a background thread to simulate the server
    List<SseEvent> receivedEvents = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch eventLatch = new CountDownLatch(3);

    // Simulate server sending events
    Thread serverSimulator = new Thread(() -> {
        try {
            Thread.sleep(100);
            receivedEvents.add(new SseEvent("notification", "Order #123 shipped", "1"));
            eventLatch.countDown();

            Thread.sleep(100);
            receivedEvents.add(new SseEvent("notification", "Delivery expected tomorrow", "2"));
            eventLatch.countDown();

            Thread.sleep(100);
            receivedEvents.add(new SseEvent("notification", "Package arrived", "3"));
            eventLatch.countDown();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    });

    serverSimulator.start();

    say("Waiting for events from server...");

    boolean allReceived = eventLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

    for (SseEvent event : receivedEvents) {
        say("Received: " + event.data);
    }

    sayAndAssertThat(
        "All real-time events received",
        true,
        org.hamcrest.CoreMatchers.is(allReceived));

    say("Received " + receivedEvents.size() + " events in real-time");
}
```

---

## Step 4 — Test Different Event Types

Validate specific event types:

```java
@Test
public void testDifferentEventTypes() throws Exception {

    sayNextSection("Event Type Handling");

    say("SSE can carry different event types. The client processes them differently.");

    String multiTypeStream = """
        event: login
        data: alice logged in
        id: 1

        event: alert
        data: {"severity":"high","message":"Unusual activity"}
        id: 2

        event: data
        data: 42
        id: 3

        event: logout
        data: alice logged out
        id: 4
        """;

    List<SseEvent> events = parseSseStream(multiTypeStream);

    sealed interface EventHandler {
        record Login(String user) implements EventHandler {}
        record Alert(String message) implements EventHandler {}
        record Data(String value) implements EventHandler {}
        record Logout(String user) implements EventHandler {}
    }

    // Handle different event types
    List<EventHandler> handled = new ArrayList<>();
    for (SseEvent event : events) {
        switch (event.type) {
            case "login" -> {
                handled.add(new EventHandler.Login(event.data));
                say("User logged in: " + event.data);
            }
            case "alert" -> {
                handled.add(new EventHandler.Alert(event.data));
                say("Alert: " + event.data);
            }
            case "data" -> {
                handled.add(new EventHandler.Data(event.data));
                say("Data: " + event.data);
            }
            case "logout" -> {
                handled.add(new EventHandler.Logout(event.data));
                say("User logged out: " + event.data);
            }
        }
    }

    sayAndAssertThat(
        "All event types handled",
        4,
        org.hamcrest.CoreMatchers.equalTo(handled.size()));
}
```

---

## Step 5 — Handle Connection Loss and Reconnection

Test automatic reconnection:

```java
@Test
public void handleReconnection() throws Exception {

    sayNextSection("Automatic Reconnection");

    say("SSE clients should automatically reconnect if the connection drops. "
        + "The server can send a 'retry' field to control reconnection delay.");

    String sseWithRetry = """
        retry: 1000

        event: status
        data: Server is online
        id: 1

        event: status
        data: Server going down for maintenance
        id: 2
        """;

    say("SSE stream with reconnection parameters:");
    say("```\n" + sseWithRetry + "\n```");

    say("The 'retry: 1000' tells the client to wait 1000ms before reconnecting");

    // Extract retry value (in production, the HTTP client handles this)
    String[] lines = sseWithRetry.split("\n");
    long retryMs = 0;

    for (String line : lines) {
        if (line.startsWith("retry: ")) {
            retryMs = Long.parseLong(line.substring(7));
        }
    }

    say("Reconnection delay: " + retryMs + "ms");

    sayAndAssertThat(
        "Reconnection delay parsed correctly",
        1000L,
        org.hamcrest.CoreMatchers.equalTo(retryMs));

    say("Client will reconnect automatically if server closes the connection");
}
```

---

## Step 6 — Test a Live Dashboard with SSE

Combine patterns to test a real dashboard that updates in real-time:

```java
@Test
public void testLiveDashboard() throws Exception {

    sayNextSection("Live Dashboard Updates");

    say("A dashboard receives real-time metrics via SSE: active users, "
        + "server status, alerts, and performance data.");

    List<DashboardMetric> metrics = Collections.synchronizedList(new ArrayList<>());

    // Simulate SSE stream with metrics
    String dashboardStream = """
        event: users
        data: {"active":42,"peak":100}
        id: 1

        event: cpu
        data: 65
        id: 2

        event: memory
        data: 4096
        id: 3

        event: alert
        data: {"level":"warning","message":"CPU > 80%"}
        id: 4
        """;

    List<SseEvent> events = parseSseStream(dashboardStream);

    say("Incoming dashboard metrics:");

    for (SseEvent event : events) {
        switch (event.type) {
            case "users" -> {
                say("👥 Active users: " + event.data);
                metrics.add(new DashboardMetric("users", event.data));
            }
            case "cpu" -> {
                say("💻 CPU: " + event.data + "%");
                metrics.add(new DashboardMetric("cpu", event.data));
            }
            case "memory" -> {
                say("🧠 Memory: " + event.data + " MB");
                metrics.add(new DashboardMetric("memory", event.data));
            }
            case "alert" -> {
                say("⚠️  Alert: " + event.data);
                metrics.add(new DashboardMetric("alert", event.data));
            }
        }
    }

    sayAndAssertThat(
        "Dashboard received all metrics",
        4,
        org.hamcrest.CoreMatchers.equalTo(metrics.size()));

    say("Dashboard updated with " + metrics.size() + " real-time metrics");
}

static record DashboardMetric(String type, String value) {}
```

---

## Key Takeaways

| Concept | Explanation |
|---------|-------------|
| **SSE** | Server-Sent Events; one-way HTTP stream |
| **event: type** | Named event type |
| **data:** | Event payload (single line or multi-line) |
| **id:** | Event ID (for reconnection) |
| **retry:** | Reconnection delay in milliseconds |
| **Blank line** | Marks end of event |
| **Auto-reconnect** | Built-in to SSE protocol |
| **One-way** | Server → client only (WebSockets are bidirectional) |

---

## Best Practices

✅ **DO:**
- Use SSE for one-way server-to-client updates
- Include event IDs for reliable delivery
- Handle reconnection automatically
- Test with actual server (streams are stateful)
- Use `event` type to distinguish message kinds
- Validate event data before processing

❌ **DON'T:**
- Confuse SSE with WebSockets (different purposes)
- Assume events arrive in order (they do, but test it)
- Leave connections open indefinitely (implement idle timeout)
- Poll repeatedly when SSE is available
- Ignore the `retry` field (affects reconnection)

---

## SSE vs. WebSocket vs. gRPC

| Feature | SSE | WebSocket | gRPC |
|---------|-----|-----------|------|
| Direction | One-way (↓) | Bidirectional | Bidirectional |
| Transport | HTTP/1.1 | HTTP/1.1 upgrade | HTTP/2 |
| Auto-reconnect | Yes | No (app must handle) | No (app must handle) |
| Data format | Text/JSON | Binary or text | Protobuf (binary) |
| Firewall friendly | Yes | Requires upgrade | Yes (HTTP/2) |
| Use case | Notifications, updates | Chat, collaboration | Microservices, RPC |

---

## Next Steps

- [How-to: Subscribe to SSE Streams](../how-to/sse-subscription.md)
- [How-to: Parse SSE Events](../how-to/sse-parsing.md)
- [How-to: Handle SSE Reconnection](../how-to/sse-reconnection.md)
- [Tutorial: WebSockets](websockets-realtime.md)
- [Tutorial: gRPC Streaming](grpc-streaming.md)
- [Reference: SSE Testing API](../reference/sse-reference.md)
