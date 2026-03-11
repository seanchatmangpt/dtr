# How-to: Subscribe to SSE Streams

Receive and process Server-Sent Events.

---

## Basic Subscription

```java
// Make GET request to SSE endpoint
Response response = makeRequest(
    Request.GET()
        .url(testServerUrl().path("/events"))
        .contentTypeApplicationJson());

// Stream is now open
String streamData = response.payloadAsString();
```

---

## Parse Events from Stream

```java
sealed interface SseEvent {
    record TextEvent(String type, String data, String id) implements SseEvent {}
}

List<SseEvent> parseSseStream(String stream) {
    List<SseEvent> events = new ArrayList<>();
    String[] lines = stream.split("\n");

    String currentType = null;
    String currentData = null;
    String currentId = null;

    for (String line : lines) {
        if (line.isEmpty()) {
            if (currentType != null) {
                events.add(new SseEvent.TextEvent(currentType, currentData, currentId));
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
        events.add(new SseEvent.TextEvent(currentType, currentData, currentId));
    }

    return events;
}

// Usage
List<SseEvent> events = parseSseStream(streamData);
events.forEach(e -> System.out.println(e.type));
```

---

## Stream Multiple Events

```java
// Simulate server sending events over time
List<String> receivedEvents = Collections.synchronizedList(new ArrayList<>());
CountDownLatch eventLatch = new CountDownLatch(3);

// In real code, this would be an HTTP stream reader
Thread eventStream = new Thread(() -> {
    try {
        // Event 1
        Thread.sleep(100);
        receivedEvents.add("event: notification\ndata: First update\nid: 1");
        eventLatch.countDown();

        // Event 2
        Thread.sleep(200);
        receivedEvents.add("event: notification\ndata: Second update\nid: 2");
        eventLatch.countDown();

        // Event 3
        Thread.sleep(150);
        receivedEvents.add("event: alert\ndata: System alert\nid: 3");
        eventLatch.countDown();
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
});

eventStream.start();

// Wait for events
boolean allReceived = eventLatch.await(5, TimeUnit.SECONDS);
System.out.println("Received " + receivedEvents.size() + " events");
```

---

## Filter by Event Type

```java
List<String> notifications = events.stream()
    .filter(e -> "notification".equals(e.type))
    .map(e -> e.data)
    .toList();

List<String> alerts = events.stream()
    .filter(e -> "alert".equals(e.type))
    .map(e -> e.data)
    .toList();

System.out.println("Notifications: " + notifications.size());
System.out.println("Alerts: " + alerts.size());
```

---

## Parse JSON Data in Events

```java
record NotificationEvent(String id, String title, String message) {}

List<NotificationEvent> parseNotifications(List<SseEvent> events) {
    ObjectMapper mapper = new ObjectMapper();
    return events.stream()
        .filter(e -> "notification".equals(e.type))
        .map(e -> {
            try {
                JsonNode json = mapper.readTree(e.data);
                return new NotificationEvent(
                    e.id,
                    json.get("title").asText(),
                    json.get("message").asText());
            } catch (Exception ex) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .toList();
}

// Usage
List<NotificationEvent> notifications = parseNotifications(events);
notifications.forEach(n -> System.out.println(n.title + ": " + n.message));
```

---

## Validate Event Order

```java
// Check that events arrived in order
List<SseEvent> events = parseSseStream(stream);

long lastId = -1;
for (SseEvent event : events) {
    long currentId = Long.parseLong(event.id);
    assert currentId > lastId : "Events out of order";
    lastId = currentId;
}
```

---

## Timeout on Stream

```java
ExecutorService executor = Executors.newSingleThreadExecutor();

try {
    Future<List<SseEvent>> future = executor.submit(() -> {
        // Read and parse stream
        return parseSseStream(readStream());
    });

    List<SseEvent> events = future.get(10, TimeUnit.SECONDS);
    System.out.println("Got " + events.size() + " events");
} catch (TimeoutException e) {
    System.out.println("Stream read timed out");
} finally {
    executor.shutdown();
}
```

---

## Best Practices

✅ **DO:**
- Use `CountDownLatch` to wait for events
- Parse event type and data separately
- Validate event order if critical
- Handle JSON parsing errors gracefully

❌ **DON'T:**
- Assume events arrive immediately
- Leave streams open indefinitely
- Ignore parse errors

---

## See Also

- [How-to: Parse SSE Events](sse-parsing.md)
- [How-to: Handle SSE Reconnection](sse-reconnection.md)
- [Tutorial: Server-Sent Events](../tutorials/server-sent-events.md)
