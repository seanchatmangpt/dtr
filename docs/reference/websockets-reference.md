# Reference: WebSocket Testing API

Complete reference for WebSocket testing with Java 25.

---

## @ClientEndpoint

Marks a class as a WebSocket client endpoint.

```java
@ClientEndpoint
public class MyEndpoint {
    @OnOpen
    public void onOpen(Session session) {}

    @OnMessage
    public void onMessage(String message) {}

    @OnMessage
    public void onMessage(ByteBuffer binary) {}

    @OnClose
    public void onClose(CloseReason reason) {}

    @OnError
    public void onError(Session session, Throwable error) {}
}
```

---

## WebSocketContainer

Factory and configuration for WebSocket connections.

```java
WebSocketContainer container = ContainerProvider.getWebSocketContainer();

// Configuration
container.setDefaultMaxSessionIdleTimeout(30000);  // milliseconds
container.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);
container.setDefaultMaxTextMessageBufferSize(1024 * 1024);

// Connect
Session session = container.connectToServer(
    endpoint,
    URI.create("ws://host:port/path"));
```

---

## Session

Represents an active WebSocket connection.

| Method | Description |
|--------|-------------|
| `isOpen()` | Check if connection is open |
| `getId()` | Get unique session ID |
| `getBasicRemote()` | Synchronous message sender |
| `getAsyncRemote()` | Asynchronous message sender |
| `close()` | Close connection gracefully |
| `close(CloseReason)` | Close with code and reason |
| `getMaxBinaryMessageBufferSize()` | Get buffer size |
| `setMaxBinaryMessageBufferSize(long)` | Set buffer size |

---

## Sending Messages

### Synchronous (Blocking)

```java
Session.Basic remote = session.getBasicRemote();

// Text
remote.sendText("Hello");

// Binary
ByteBuffer data = ByteBuffer.wrap(new byte[]{1, 2, 3});
remote.sendBinary(data);

// Whole message
remote.sendText("complete", true);

// Throws IOException on failure
```

### Asynchronous (Non-blocking)

```java
Session.Async remote = session.getAsyncRemote();

// Text with callback
remote.sendText("Hello", result -> {
    if (result.isOK()) {
        System.out.println("Sent");
    } else {
        System.err.println(result.getException());
    }
});

// Binary
remote.sendBinary(data);
```

---

## Lifecycle Callbacks

| Annotation | Parameter | Fired when |
|-----------|-----------|-----------|
| `@OnOpen` | `Session` | Connection established |
| `@OnMessage` | `String` or `ByteBuffer` | Message received |
| `@OnClose` | `Session`, `CloseReason` | Connection closed |
| `@OnError` | `Session`, `Throwable` | Error occurs |

---

## CloseReason

Details about connection closure.

```java
CloseReason reason = ...;

int code = reason.getCloseCode().getCode(); // 1000, 1001, etc.
String phrase = reason.getReasonPhrase(); // "Normal closure"

// Standard codes
CloseReason.CloseCodes.NORMAL_CLOSURE           // 1000
CloseReason.CloseCodes.GOING_AWAY               // 1001
CloseReason.CloseCodes.PROTOCOL_ERROR           // 1002
CloseReason.CloseCodes.UNSUPPORTED_DATA         // 1003
CloseReason.CloseCodes.RESERVED                 // 1004
CloseReason.CloseCodes.NO_STATUS_CODE           // 1005
CloseReason.CloseCodes.CLOSED_ABNORMALLY        // 1006
CloseReason.CloseCodes.NOT_CONSISTENT           // 1007
CloseReason.CloseCodes.VIOLATED_POLICY          // 1008
CloseReason.CloseCodes.TOO_BIG                  // 1009
CloseReason.CloseCodes.MISSING_EXTENSION        // 1010
CloseReason.CloseCodes.UNEXPECTED_CONDITION     // 1011
```

---

## Error Handling

```java
@OnError
public void onError(Session session, Throwable error) {
    if (error instanceof IOException) {
        // Connection I/O error
    } else if (error instanceof ProtocolException) {
        // Protocol violation
    } else {
        // Other error
    }
}
```

---

## Configuration Best Practices

| Setting | Default | Recommended |
|---------|---------|------------|
| Idle timeout | 15 minutes | 30 seconds (testing) |
| Text buffer | 32KB | 1MB (for large messages) |
| Binary buffer | 32KB | 1MB |

---

## See Also

- [How-to: Connect to WebSocket Servers](../how-to/websockets-connection.md)
- [How-to: Handle WebSocket Errors](../how-to/websockets-error-handling.md)
- [How-to: Broadcast Messages](../how-to/websockets-broadcast.md)
- [Tutorial: WebSockets](../tutorials/websockets-realtime.md)
- [Reference: Real-Time Protocols](realtime-protocols-reference.md)
