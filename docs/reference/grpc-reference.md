# Reference: gRPC Testing API

Complete reference for gRPC testing with Java 25.

---

## Channel Management

```java
// Create channel
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build();

// Graceful shutdown
channel.shutdown();
boolean terminated = channel.awaitTermination(5, TimeUnit.SECONDS);

// Force shutdown
channel.shutdownNow();
```

---

## Stub Types

| Type | Use | Blocking |
|------|-----|----------|
| `BlockingStub` | Unary, easy testing | Yes |
| `Stub` | Streaming, async | No |
| `FutureStub` | Async with futures | No |

```java
// Blocking (simplest for tests)
MyServiceGrpc.MyServiceBlockingStub stub =
    MyServiceGrpc.newBlockingStub(channel);

// Async
MyServiceGrpc.MyServiceStub asyncStub =
    MyServiceGrpc.newStub(channel);

// Futures
MyServiceGrpc.MyServiceFutureStub futureStub =
    MyServiceGrpc.newFutureStub(channel);
```

---

## RPC Method Types

| Method | Request | Response | Blocking? |
|--------|---------|----------|-----------|
| Unary | 1 | 1 | Yes (blocking stub) |
| Server streaming | 1 | N | Yes (returns Iterator) |
| Client streaming | N | 1 | No (StreamObserver) |
| Bidirectional | N | N | No (StreamObserver) |

---

## Unary RPC

```java
// Call
MyResponse response = stub.getUser(request);

// With timeout
MyResponse response = stub
    .withDeadlineAfter(5, TimeUnit.SECONDS)
    .getUser(request);

// With metadata (headers)
io.grpc.Metadata metadata = new io.grpc.Metadata();
metadata.put(io.grpc.Metadata.Key.of("auth", io.grpc.Metadata.ASCII_STRING_MARSHALLER), "token");

MyResponse response = MetadataUtils.attachHeaders(stub, metadata)
    .getUser(request);
```

---

## Server Streaming

```java
// Returns Iterator
Iterator<Item> items = stub.listItems(request);

while (items.hasNext()) {
    Item item = items.next();
    System.out.println(item);
}

// Collect to list
List<Item> all = new ArrayList<>();
stub.listItems(request).forEachRemaining(all::add);
```

---

## Client Streaming

```java
// Callback for response
StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
    @Override
    public void onNext(Response value) {
        System.out.println("Response: " + value);
    }

    @Override
    public void onError(Throwable t) {
        System.err.println("Error: " + t);
    }

    @Override
    public void onCompleted() {
        System.out.println("Done");
    }
};

// Send requests
StreamObserver<Request> requestObserver = asyncStub.createItems(responseObserver);

requestObserver.onNext(request1);
requestObserver.onNext(request2);
requestObserver.onCompleted(); // Signal end
```

---

## Bidirectional Streaming

```java
// Same pattern as client streaming, but server can send anytime
StreamObserver<Response> responses = new StreamObserver<Response>() {
    @Override
    public void onNext(Response resp) {
        System.out.println("From server: " + resp);
    }

    @Override
    public void onError(Throwable t) {}

    @Override
    public void onCompleted() {}
};

StreamObserver<Request> requests = asyncStub.chat(responses);

requests.onNext(msg1);
requests.onNext(msg2);
requests.onCompleted();
```

---

## Error Codes

| Code | Meaning |
|------|---------|
| `OK` (0) | Success |
| `INVALID_ARGUMENT` (3) | Bad input |
| `DEADLINE_EXCEEDED` (4) | Timeout |
| `NOT_FOUND` (5) | Resource not found |
| `PERMISSION_DENIED` (7) | No access |
| `UNAVAILABLE` (14) | Service down |
| `UNAUTHENTICATED` (16) | No auth |

---

## Handling Errors

```java
try {
    response = stub.method(request);
} catch (io.grpc.StatusRuntimeException e) {
    io.grpc.Status status = e.getStatus();
    System.out.println("Code: " + status.getCode());
    System.out.println("Message: " + status.getDescription());
}
```

---

## Configuration

| Setting | Method | Default |
|---------|--------|---------|
| Deadline | `withDeadlineAfter()` | None |
| Compression | `withCompression()` | None |
| Metadata | `MetadataUtils.attachHeaders()` | None |
| Max retry attempts | Policy config | 4 |

---

## Virtual Threads Integration

```java
// Concurrent RPC with virtual threads
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<Response>> futures = new ArrayList<>();

    for (int i = 0; i < 1000; i++) {
        futures.add(executor.submit(() -> stub.method(request)));
    }

    for (Future<Response> f : futures) {
        Response resp = f.get();
    }
}
```

---

## See Also

- [How-to: Make Unary RPC Calls](../how-to/grpc-unary.md)
- [How-to: Handle gRPC Streaming](../how-to/grpc-streaming.md)
- [How-to: Test gRPC Error Codes](../how-to/grpc-error-handling.md)
- [Tutorial: gRPC Streaming](../tutorials/grpc-streaming.md)
- [Reference: Real-Time Protocols](realtime-protocols-reference.md)
