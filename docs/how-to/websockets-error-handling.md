# How-to: Handle WebSocket Errors

Manage connection failures, message errors, and graceful shutdown.

---

## Catch Connection Errors

```java
try {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    Session session = container.connectToServer(
        new MyEndpoint(),
        URI.create("ws://localhost:8080/chat"));
} catch (DeploymentException e) {
    System.err.println("Deployment error: " + e.getMessage());
} catch (IOException e) {
    System.err.println("Connection error: " + e.getMessage());
}
```

---

## Handle Message Errors in Endpoint

```java
@ClientEndpoint
public class ErrorHandlingEndpoint {
    @OnError
    public void onError(Session session, Throwable error) {
        if (error instanceof IOException) {
            System.err.println("IO error: " + error.getMessage());
        } else if (error instanceof ProtocolException) {
            System.err.println("Protocol error: " + error.getMessage());
        } else {
            System.err.println("Unexpected error: " + error.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message) {
        try {
            // Process message
        } catch (Exception e) {
            System.err.println("Failed to process message: " + e.getMessage());
        }
    }
}
```

---

## Timeout Error Handling

```java
try {
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    container.setDefaultMaxSessionIdleTimeout(5000); // 5 seconds

    Session session = container.connectToServer(
        new TimeoutEndpoint(),
        URI.create("ws://localhost:8080/chat"));

    // Send message
    session.getBasicRemote().sendText("test");

} catch (java.util.concurrent.TimeoutException e) {
    System.err.println("Timeout waiting for response");
} catch (Exception e) {
    System.err.println("Error: " + e.getMessage());
}
```

---

## Send Message with Error Callback

```java
session.getAsyncRemote().sendText("message", result -> {
    if (result.isOK()) {
        System.out.println("Message sent successfully");
    } else {
        Throwable error = result.getException();
        System.err.println("Failed to send: " + error.getMessage());
    }
});
```

---

## Handle Connection Closure

```java
@ClientEndpoint
public class GracefulShutdownEndpoint {
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Connection closed");
        System.out.println("Close code: " + reason.getCloseCode());
        System.out.println("Reason: " + reason.getReasonPhrase());

        // Attempt reconnection if not normal closure
        if (reason.getCloseCode() != CloseReason.CloseCodes.NORMAL_CLOSURE) {
            System.out.println("Unexpected closure, attempting reconnect...");
            // Implement reconnection logic
        }
    }
}
```

---

## Network Error Recovery

```java
Session connectWithRetryAndTimeout(URI uri, int maxRetries, long timeoutMs)
        throws Exception {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            container.setDefaultMaxSessionIdleTimeout(timeoutMs);

            return container.connectToServer(
                new MyEndpoint(),
                uri);

        } catch (Exception e) {
            if (attempt == maxRetries) {
                throw e;
            }
            long backoffMs = (long) Math.pow(2, attempt - 1) * 100;
            System.out.println("Attempt " + attempt + " failed. Retrying in " + backoffMs + "ms...");
            Thread.sleep(backoffMs);
        }
    }
    throw new IllegalStateException("Should not reach here");
}
```

---

## Close Session Safely

```java
// Graceful close
session.close();

// Close with code and reason
session.close(new CloseReason(
    CloseReason.CloseCodes.NORMAL_CLOSURE,
    "Client shutting down"));

// Force close if stuck
if (!session.isOpen()) {
    // Already closed
} else {
    session.close();
}
```

---

## Best Practices

✅ **DO:**
- Handle all error callbacks (@OnError)
- Implement timeout protection
- Use async send with callbacks
- Log all errors
- Gracefully close on shutdown

❌ **DON'T:**
- Ignore @OnError callback
- Assume connection never fails
- Leave sessions open on error
- Retry indefinitely without backoff

---

## See Also

- [How-to: Connect to WebSocket Servers](websockets-connection.md)
- [How-to: Broadcast Messages](websockets-broadcast.md)
- [Tutorial: WebSockets](../tutorials/websockets-realtime.md)
