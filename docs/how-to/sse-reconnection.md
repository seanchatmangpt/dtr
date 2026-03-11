# How-to: Handle SSE Reconnection

Implement automatic reconnection and handle disconnections gracefully.

---

## Basic Reconnection

```java
boolean reconnect(String sseUrl, int maxRetries) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            Response response = makeRequest(
                Request.GET()
                    .url(sseUrl));

            if (response.httpStatus() == 200) {
                System.out.println("Connected on attempt " + attempt);
                return true;
            }
        } catch (Exception e) {
            System.err.println("Attempt " + attempt + " failed: " + e.getMessage());

            if (attempt < maxRetries) {
                long backoffMs = (long) Math.pow(2, attempt - 1) * 100;
                try {
                    Thread.sleep(backoffMs);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
    return false;
}
```

---

## Use Server-Provided Retry Interval

```java
record RetryConfig(long delayMs) {}

RetryConfig parseRetryConfig(String sseStream) {
    for (String line : sseStream.split("\n")) {
        if (line.startsWith("retry: ")) {
            long delayMs = Long.parseLong(line.substring(7));
            return new RetryConfig(delayMs);
        }
    }
    return new RetryConfig(0); // No retry specified
}

// Reconnect using server's recommended delay
void reconnectWithServerDelay(String sseUrl, String lastEventId) throws Exception {
    String stream = makeRequest(Request.GET().url(sseUrl)).payloadAsString();

    RetryConfig config = parseRetryConfig(stream);
    long delayMs = config.delayMs() > 0 ? config.delayMs() : 1000; // Default 1s

    System.out.println("Server recommends reconnect delay: " + delayMs + "ms");
    Thread.sleep(delayMs);

    // Reconnect with Last-Event-ID to resume
    reconnect(sseUrl, lastEventId);
}
```

---

## Resume from Last Event ID

```java
String lastEventId = "0";

void streamWithCheckpoint(String sseUrl) throws Exception {
    while (true) {
        try {
            Response response = makeRequest(
                Request.GET()
                    .url(sseUrl)
                    .addHeader("Last-Event-ID", lastEventId));

            String stream = response.payloadAsString();
            List<SseEvent> events = parseEvents(stream);

            for (SseEvent event : events) {
                processEvent(event);
                lastEventId = event.id; // Update checkpoint
            }

            break; // Success
        } catch (Exception e) {
            System.err.println("Stream failed: " + e.getMessage());
            System.out.println("Reconnecting from event: " + lastEventId);

            Thread.sleep(1000); // Wait before retry
        }
    }
}
```

---

## Exponential Backoff

```java
boolean connectWithExponentialBackoff(String url) throws Exception {
    long initialDelayMs = 100;
    long maxDelayMs = 30000; // 30 seconds max
    long delayMs = initialDelayMs;
    int maxAttempts = 10;

    for (int attempt = 1; attempt <= maxAttempts; attempt++) {
        try {
            System.out.println("Attempt " + attempt);
            Response response = makeRequest(Request.GET().url(url));

            if (response.httpStatus() == 200) {
                return true;
            }
        } catch (Exception e) {
            System.err.println("Attempt " + attempt + " failed. Retrying in " + delayMs + "ms");

            if (attempt < maxAttempts) {
                Thread.sleep(delayMs);
                delayMs = Math.min(delayMs * 2, maxDelayMs); // Cap at maxDelayMs
            }
        }
    }
    return false;
}
```

---

## Detect Unexpected Closure

```java
class ResilientSseClient {
    private String url;
    private String lastEventId = "0";
    private boolean shouldReconnect = true;

    void listen() {
        while (shouldReconnect) {
            try {
                Response response = makeRequest(
                    Request.GET()
                        .url(url)
                        .addHeader("Last-Event-ID", lastEventId));

                String stream = response.payloadAsString();

                if (stream.isEmpty()) {
                    // Server closed connection
                    System.out.println("Server closed connection, reconnecting...");
                    Thread.sleep(1000);
                    continue;
                }

                // Process events
                List<SseEvent> events = parseEvents(stream);
                for (SseEvent event : events) {
                    processEvent(event);
                    lastEventId = event.id;
                }

            } catch (Exception e) {
                System.err.println("Connection lost: " + e.getMessage());
                System.out.println("Attempting to reconnect...");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    shouldReconnect = false;
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    void stop() {
        shouldReconnect = false;
    }

    private void processEvent(SseEvent event) {
        // Handle event
    }
}
```

---

## Test Reconnection Scenario

```java
@Test
void testReconnectionAfterFailure() throws Exception {
    String sseUrl = "http://localhost:8080/events";

    // Simulate server returning 503 (service unavailable)
    // Then succeeding

    int attempt = 0;
    int maxRetries = 3;

    for (int i = 1; i <= maxRetries; i++) {
        try {
            Response response = makeRequest(Request.GET().url(sseUrl));

            if (response.httpStatus() == 200) {
                System.out.println("✓ Connected on attempt " + i);
                assert true;
                return;
            }
        } catch (Exception e) {
            if (i < maxRetries) {
                long backoffMs = (long) Math.pow(2, i - 1) * 100;
                System.out.println("✗ Attempt " + i + " failed, retrying in " + backoffMs + "ms");
                Thread.sleep(backoffMs);
            }
        }
    }

    fail("Failed to reconnect after " + maxRetries + " attempts");
}
```

---

## Handle Jitter in Backoff

```java
long calculateBackoffWithJitter(int attempt) {
    long baseDelayMs = (long) Math.pow(2, attempt - 1) * 100;
    long jitterMs = (long) (Math.random() * baseDelayMs * 0.1); // ±10% jitter
    return baseDelayMs + jitterMs;
}

// Usage
for (int attempt = 1; attempt <= 5; attempt++) {
    long delayMs = calculateBackoffWithJitter(attempt);
    System.out.println("Attempt " + attempt + ": delay = " + delayMs + "ms");
    Thread.sleep(delayMs);
}
```

---

## Best Practices

✅ **DO:**
- Use exponential backoff (2^n)
- Cap max backoff (e.g., 30 seconds)
- Use server-provided retry hints
- Track Last-Event-ID for resume
- Add jitter to prevent thundering herd

❌ **DON'T:**
- Retry immediately (causes server load)
- Ignore server's retry field
- Lose event IDs on disconnect
- Retry indefinitely without limit

---

## See Also

- [How-to: Subscribe to SSE Streams](sse-subscription.md)
- [How-to: Parse SSE Events](sse-parsing.md)
- [Tutorial: Server-Sent Events](../tutorials/server-sent-events.md)
