# How-to: Connect to WebSocket Servers

Establish and manage WebSocket connections for testing real-time services.

---

## Basic Connection

```java
WebSocketContainer container = ContainerProvider.getWebSocketContainer();
Session session = container.connectToServer(
    new MyEndpoint(),
    URI.create("ws://localhost:8080/chat"));

// Use session...
session.close();
```

---

## Connection with Retry Logic

```java
Session connect(URI uri, int maxRetries) throws Exception {
    for (int i = 1; i <= maxRetries; i++) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            return container.connectToServer(new MyEndpoint(), uri);
        } catch (Exception e) {
            if (i == maxRetries) throw e;
            long delayMs = (long) Math.pow(2, i - 1) * 100; // Exponential backoff
            Thread.sleep(delayMs);
        }
    }
    throw new IllegalStateException("Failed to connect");
}

// Usage
Session session = connect(URI.create("ws://localhost:8080/chat"), 3);
```

---

## Create Endpoint Class

```java
@ClientEndpoint
public class ChatEndpoint {
    private CountDownLatch openLatch = new CountDownLatch(1);
    private List<String> messages = Collections.synchronizedList(new ArrayList<>());

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected");
        openLatch.countDown();
    }

    @OnMessage
    public void onMessage(String message) {
        messages.add(message);
    }

    @OnClose
    public void onClose(CloseReason reason) {
        System.out.println("Closed: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("Error: " + error.getMessage());
    }

    public void waitForConnection(long timeout, TimeUnit unit) throws InterruptedException {
        openLatch.await(timeout, unit);
    }

    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }
}
```

---

## Check Connection Status

```java
// Check if open
if (session.isOpen()) {
    System.out.println("Connected");
}

// Get max frame size
long maxFrameSize = session.getMaxBinaryMessageBufferSize();

// Get connected peer
String peerId = session.getId();
```

---

## Send Text Message

```java
session.getBasicRemote().sendText("Hello, server!");

// Or async
session.getAsyncRemote().sendText("Hello, async!", result -> {
    if (result.isOK()) {
        System.out.println("Sent");
    } else {
        System.err.println("Error: " + result.getException());
    }
});
```

---

## Send Binary Message

```java
ByteBuffer data = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
session.getBasicRemote().sendBinary(data);

// Or async
session.getAsyncRemote().sendBinary(data, result -> {
    if (result.isOK()) {
        System.out.println("Binary sent");
    }
});
```

---

## Set Connection Timeout

```java
WebSocketContainer container = ContainerProvider.getWebSocketContainer();
container.setDefaultMaxSessionIdleTimeout(30000); // 30 seconds

Session session = container.connectToServer(new MyEndpoint(), uri);
```

---

## Handle Multiple Endpoints

```java
// Connect to multiple servers
List<Session> sessions = new ArrayList<>();

sessions.add(container.connectToServer(
    new Endpoint1(),
    URI.create("ws://server1:8080/chat")));

sessions.add(container.connectToServer(
    new Endpoint2(),
    URI.create("ws://server2:8080/events")));

// Clean up all
for (Session s : sessions) {
    s.close();
}
```

---

## Graceful Shutdown

```java
try (WebSocketContainer container = ...) {
    Session session = container.connectToServer(...);

    // Use session...

    session.close();
} // Container auto-closed
```

---

## Best Practices

✅ **DO:**
- Always close sessions
- Use try-with-resources where possible
- Handle all lifecycle callbacks
- Wait for @OnOpen before sending

❌ **DON'T:**
- Send immediately after connect (wait for @OnOpen)
- Ignore connection errors
- Leave connections open

---

## See Also

- [How-to: Handle WebSocket Errors](websockets-error-handling.md)
- [How-to: Broadcast Messages](websockets-broadcast.md)
- [Tutorial: WebSockets](../tutorials/websockets-realtime.md)
