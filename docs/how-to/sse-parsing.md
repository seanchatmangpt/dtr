# How-to: Parse Server-Sent Events

Extract and validate event data from SSE streams.

---

## Parse Basic Events

```java
sealed interface SseEvent {
    record Event(String type, String data, String id) implements SseEvent {}
}

List<SseEvent> parseEvents(String stream) {
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

// Usage
String sseStream = """
    event: message
    data: Hello
    id: 1

    event: notification
    data: Alert
    id: 2
    """;

List<SseEvent> events = parseEvents(sseStream);
assert events.size() == 2;
```

---

## Parse Multiline Data

```java
String multilineStream = """
    event: longMessage
    data: Line 1
    data: Line 2
    data: Line 3
    id: 1
    """;

// Combine multiline data with newlines
List<SseEvent> events = parseEvents(multilineStream);
String combined = String.join("\n", events.get(0).data.split("\n"));
assert combined.contains("Line 1");
assert combined.contains("Line 3");
```

---

## Parse JSON Events

```java
record NotificationEvent(String id, String title, String message) {}

List<NotificationEvent> parseJsonEvents(List<SseEvent> events) {
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
String jsonStream = """
    event: notification
    data: {"title":"Alert","message":"High CPU usage"}
    id: 1
    """;

List<SseEvent> events = parseEvents(jsonStream);
List<NotificationEvent> notifications = parseJsonEvents(events);
assert notifications.size() == 1;
assert "Alert".equals(notifications.get(0).title);
```

---

## Filter Events by Type

```java
List<SseEvent> events = parseEvents(sseStream);

List<SseEvent> userMessages = events.stream()
    .filter(e -> "message".equals(e.type))
    .toList();

List<SseEvent> alerts = events.stream()
    .filter(e -> "alert".equals(e.type))
    .toList();

System.out.println("Messages: " + userMessages.size());
System.out.println("Alerts: " + alerts.size());
```

---

## Validate Event IDs

```java
// Check that event IDs are in order
List<SseEvent> events = parseEvents(sseStream);

long lastId = -1;
for (SseEvent event : events) {
    long currentId = Long.parseLong(event.id);
    assert currentId > lastId : "Event IDs out of order";
    lastId = currentId;
}
```

---

## Parse Retry Field

```java
long parseRetryField(String stream) {
    for (String line : stream.split("\n")) {
        if (line.startsWith("retry: ")) {
            return Long.parseLong(line.substring(7));
        }
    }
    return 0; // Default
}

// Usage
String streamWithRetry = """
    retry: 5000

    event: message
    data: Hello
    id: 1
    """;

long retryMs = parseRetryField(streamWithRetry);
assert retryMs == 5000;
```

---

## Handle Parse Errors

```java
List<SseEvent> safeParseEvents(String stream) {
    List<SseEvent> events = new ArrayList<>();

    try {
        String[] lines = stream.split("\n");
        String type = null, data = null, id = null;

        for (String line : lines) {
            try {
                if (line.isEmpty()) {
                    if (type != null) {
                        events.add(new SseEvent.Event(type, data, id));
                    }
                    type = null;
                    data = null;
                    id = null;
                } else if (line.startsWith("event: ")) {
                    type = line.substring(7);
                } else if (line.startsWith("data: ")) {
                    data = line.substring(6);
                } else if (line.startsWith("id: ")) {
                    id = line.substring(4);
                }
            } catch (Exception e) {
                System.err.println("Error parsing line: " + e.getMessage());
            }
        }
    } catch (Exception e) {
        System.err.println("Failed to parse SSE stream: " + e.getMessage());
    }

    return events;
}
```

---

## Best Practices

✅ **DO:**
- Handle multiline data with `data:` prefix
- Validate event IDs if ordering matters
- Parse JSON defensively with try/catch
- Use sealed types for type safety
- Filter events by type

❌ **DON'T:**
- Assume data is always single-line
- Trust IDs without validation
- Forget to handle parse errors
- Lose data on malformed events

---

## See Also

- [How-to: Subscribe to SSE Streams](sse-subscription.md)
- [How-to: Handle SSE Reconnection](sse-reconnection.md)
- [Tutorial: Server-Sent Events](../tutorials/server-sent-events.md)
