# Reference: Server-Sent Events Testing API

Complete reference for SSE testing with Java 25.

---

## Event Stream Format

```
event: eventType
data: payload
id: 1
retry: 1000

```

| Field | Required | Purpose |
|-------|----------|---------|
| `event:` | No | Event type (default: "message") |
| `data:` | Yes | Payload (single or multiple lines) |
| `id:` | No | Event ID (for reconnection) |
| `retry:` | No | Reconnection delay in ms |

---

## Multiline Data

```
event: longMessage
data: Line 1
data: Line 2
data: Line 3
id: 1

```

Combine with newlines: `"Line 1\nLine 2\nLine 3"`

---

## Parsing Events

```java
sealed interface SseEvent {
    record Event(String type, String data, String id) implements SseEvent {}
}

List<SseEvent> parseStream(String stream) {
    List<SseEvent> events = new ArrayList<>();
    String[] lines = stream.split("\n");

    String type = null, data = null, id = null;

    for (String line : lines) {
        if (line.isEmpty()) {
            if (type != null) {
                events.add(new SseEvent.Event(type, data, id));
                type = null;
                data = null;
                id = null;
            }
        } else if (line.startsWith("event: ")) {
            type = line.substring(7);
        } else if (line.startsWith("data: ")) {
            data = line.substring(6);
        } else if (line.startsWith("id: ")) {
            id = line.substring(4);
        }
    }

    if (type != null) {
        events.add(new SseEvent.Event(type, data, id));
    }

    return events;
}
```

---

## Connection

```java
// HTTP GET request that expects streaming response
Response response = makeRequest(
    Request.GET()
        .url(sseUrl)
        .addHeader("Accept", "text/event-stream"));

// Check for 200 OK
assert response.httpStatus() == 200;

// Stream data is in response body
String eventStream = response.payloadAsString();
```

---

## Event Types

Standard event types by convention:

| Type | Purpose |
|------|---------|
| `message` | Generic message (default) |
| `notification` | User notification |
| `alert` | Alert/warning |
| `update` | Data update |
| `status` | Status change |

Custom types are allowed.

---

## Reconnection

```java
// Server-provided retry hint
retry: 5000

// Client implementation
long retryMs = 5000;
Thread.sleep(retryMs);
// Reconnect with Last-Event-ID header

headers:
Last-Event-ID: 42
```

---

## HTTP Headers

**Request:**
```
GET /events HTTP/1.1
Host: example.com
Accept: text/event-stream
Last-Event-ID: 42
```

**Response:**
```
HTTP/1.1 200 OK
Content-Type: text/event-stream
Cache-Control: no-cache
Connection: keep-alive
```

---

## Filtering Events

```java
// By type
List<SseEvent> notifications = events.stream()
    .filter(e -> "notification".equals(e.type))
    .toList();

// By ID range
List<SseEvent> recent = events.stream()
    .filter(e -> Long.parseLong(e.id) > 100)
    .toList();
```

---

## JSON Events

```java
record Notification(String title, String message) {}

List<Notification> parseJsonEvents(List<SseEvent> events) {
    ObjectMapper mapper = new ObjectMapper();
    return events.stream()
        .map(e -> {
            try {
                return mapper.readValue(e.data, Notification.class);
            } catch (Exception ex) {
                return null;
            }
        })
        .filter(Objects::nonNull)
        .toList();
}
```

---

## Best Practices

✅ **DO:**
- Use `text/event-stream` content type
- Include event IDs for reliable delivery
- Respect retry hints from server
- Handle multiline data correctly
- Resume from Last-Event-ID

❌ **DON'T:**
- Confuse with WebSockets (one-way only)
- Assume immediate delivery
- Poll instead of streaming
- Ignore reconnection hints

---

## Virtual Threads

```java
// Process events concurrently
try (ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor()) {
    List<SseEvent> events = parseStream(response.payloadAsString());

    for (SseEvent event : events) {
        exec.submit(() -> processEvent(event));
    }
}
```

---

## Error Handling

```java
// Check HTTP status first
if (response.httpStatus() != 200) {
    System.err.println("Failed: " + response.httpStatus());
    return;
}

// Handle parse errors
try {
    List<SseEvent> events = parseStream(stream);
} catch (Exception e) {
    System.err.println("Parse error: " + e.getMessage());
}
```

---

## See Also

- [How-to: Subscribe to SSE Streams](../how-to/sse-subscription.md)
- [How-to: Parse SSE Events](../how-to/sse-parsing.md)
- [How-to: Handle SSE Reconnection](../how-to/sse-reconnection.md)
- [Tutorial: Server-Sent Events](../tutorials/server-sent-events.md)
- [Reference: Real-Time Protocols](realtime-protocols-reference.md)
