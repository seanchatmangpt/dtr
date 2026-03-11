# Reference: Real-Time Protocols

Complete reference for testing WebSockets, gRPC, and Server-Sent Events.

---

## Quick Comparison

| Feature | WebSocket | gRPC | SSE |
|---------|-----------|------|-----|
| Direction | Bidirectional | Bidirectional | One-way (↓) |
| Protocol | HTTP/1.1 upgrade | HTTP/2 | HTTP/1.1 |
| Transport | Binary frames | Binary (protobuf) | Text (JSON) |
| Auto-reconnect | No | No | Yes |
| Firewall friendly | Good | Good | Excellent |
| Performance | Medium | High | Low |
| Data format | JSON/binary | Protobuf | Text/JSON |
| Use case | Chat, real-time UI | Microservices | Notifications |

---

## WebSockets

### Connection

```java
WebSocketContainer container = ContainerProvider.getWebSocketContainer();

Session session = container.connectToServer(
    new MyEndpoint(),
    URI.create("ws://localhost:8080/path"));
```

### Lifecycle Callbacks

```java
@ClientEndpoint
public class MyEndpoint {
    @OnOpen
    public void onOpen(Session session) { }

    @OnMessage
    public void onMessage(String text) { }

    @OnMessage
    public void onMessage(ByteBuffer binary) { }

    @OnClose
    public void onClose(CloseReason reason) { }

    @OnError
    public void onError(Session session, Throwable error) { }
}
```

### Send Messages

```java
// Text
session.getBasicRemote().sendText("Hello");

// Binary
ByteBuffer data = ByteBuffer.wrap(new byte[]{1, 2, 3});
session.getBasicRemote().sendBinary(data);

// Async (with callback)
session.getAsyncRemote().sendText("Hello", result -> {
    if (result.isOK()) {
        // Success
    } else {
        // Error
    }
});
```

### Configuration

```java
// Timeout
container.setDefaultMaxSessionIdleTimeout(30000); // milliseconds

// Message buffer size
session.setMaxBinaryMessageBufferSize(1024 * 1024); // 1MB
session.setMaxTextMessageBufferSize(1024 * 1024);
```

---

## gRPC

### Channel Management

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build();

// Shutdown (graceful)
channel.shutdown();
channel.awaitTermination(5, TimeUnit.SECONDS);

// Or force
channel.shutdownNow();
```

### Stubs

```java
// Blocking (synchronous)
MyServiceGrpc.MyServiceBlockingStub stub =
    MyServiceGrpc.newBlockingStub(channel);

// Async (callbacks)
MyServiceGrpc.MyServiceStub asyncStub =
    MyServiceGrpc.newStub(channel);

// Futures (CompletableFuture-like)
MyServiceGrpc.MyServiceFutureStub futureStub =
    MyServiceGrpc.newFutureStub(channel);
```

### RPC Methods

```java
// Unary
Response response = stub.unaryMethod(request);

// Server streaming
Iterator<Response> responses = stub.serverStream(request);

// Client streaming
StreamObserver<Response> observer = ...;
StreamObserver<Request> requestObserver = stub.clientStream(observer);
requestObserver.onNext(request);
requestObserver.onCompleted();

// Bidirectional streaming
StreamObserver<Response> observer = ...;
StreamObserver<Request> requestObserver = stub.bidirectional(observer);
```

### Error Handling

```java
try {
    response = stub.method(request);
} catch (io.grpc.StatusRuntimeException e) {
    io.grpc.Status status = e.getStatus();

    // Status codes
    // OK, CANCELLED, UNKNOWN, INVALID_ARGUMENT, DEADLINE_EXCEEDED,
    // NOT_FOUND, ALREADY_EXISTS, PERMISSION_DENIED, RESOURCE_EXHAUSTED,
    // FAILED_PRECONDITION, ABORTED, OUT_OF_RANGE, UNIMPLEMENTED,
    // INTERNAL, UNAVAILABLE, DATA_LOSS, UNAUTHENTICATED

    System.out.println(status.getCode());
    System.out.println(status.getDescription());
}
```

### Timeout and Deadline

```java
stub = stub.withDeadlineAfter(5, TimeUnit.SECONDS);
response = stub.method(request);

// Or explicit deadline
Instant deadline = Instant.now().plusSeconds(5);
stub = stub.withDeadline(io.grpc.Deadline.create(deadline));
```

---

## Server-Sent Events

### Event Format

```
event: eventType
data: {"key":"value"}
id: 1

event: anotherType
data: some data
id: 2

```

Fields:
- `event:` — Event type (optional, default: "message")
- `data:` — Payload (required; multi-line with `data:` prefix)
- `id:` — Event ID (optional, used for reconnection)
- `retry:` — Reconnection delay in milliseconds

### Parsing

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

    return events;
}
```

### Multiline Data

```
event: message
data: Line 1
data: Line 2
data: Line 3
id: 1

```

Combine with newlines: `"Line 1\nLine 2\nLine 3"`

### Reconnection

```
retry: 1000

```

Tells client to wait 1000ms before reconnecting.

---

## Testing Patterns

### Synchronize Async Operations

```java
// Wait for specific event
CountDownLatch latch = new CountDownLatch(1);

observer.onNext(value);
// or
eventHandler.onEvent(() -> latch.countDown());

latch.await(5, TimeUnit.SECONDS);
```

### Collect Results

```java
List<Result> results = Collections.synchronizedList(new ArrayList<>());

observer.onNext(result -> results.add(result));

// Use results...
```

### Handle Streams with Virtual Threads

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    for (int i = 0; i < 1000; i++) {
        executor.submit(() -> {
            // Each virtual thread handles one stream/call
            handleStream();
        });
    }
}
```

---

## Best Practices

✅ **DO:**
- Close connections/channels properly
- Handle all callback methods
- Use timeouts for network operations
- Wait for async completion (`CountDownLatch`, callbacks)
- Validate received data before processing

❌ **DON'T:**
- Assume immediate message delivery
- Leave connections open
- Ignore error callbacks
- Test in isolation from actual server

---

## Performance Comparison

For 1000 concurrent messages:

| Protocol | Memory | Latency | Throughput |
|----------|--------|---------|-----------|
| WebSocket | ~5MB | 10-50ms | 10K msg/s |
| gRPC | ~2MB | 1-10ms | 100K msg/s |
| SSE | ~10MB | 50-200ms | 1K msg/s |

(Approximate; depends on message size and server)

---

## See Also

- [Tutorial: WebSockets](../tutorials/websockets-realtime.md)
- [Tutorial: gRPC Streaming](../tutorials/grpc-streaming.md)
- [Tutorial: Server-Sent Events](../tutorials/server-sent-events.md)
- [How-to: WebSocket Connection](../how-to/websockets-connection.md)
- [How-to: gRPC Unary Calls](../how-to/grpc-unary.md)
- [How-to: SSE Subscription](../how-to/sse-subscription.md)
