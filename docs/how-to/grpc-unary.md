# How-to: Make Unary RPC Calls

Test simple request/response gRPC calls.

---

## Create Channel

```java
ManagedChannel channel = ManagedChannelBuilder
    .forAddress("localhost", 50051)
    .usePlaintext()
    .build();

// Don't forget to shutdown
channel.shutdown();
```

---

## Create Stub

```java
// Synchronous (blocking)
MyServiceGrpc.MyServiceBlockingStub stub =
    MyServiceGrpc.newBlockingStub(channel);

// Asynchronous (non-blocking)
MyServiceGrpc.MyServiceStub asyncStub =
    MyServiceGrpc.newStub(channel);

// Futures (for CompletableFuture-style)
MyServiceGrpc.MyServiceFutureStub futureStub =
    MyServiceGrpc.newFutureStub(channel);
```

---

## Make a Call

```java
// Build request
MyRequest request = MyRequest.newBuilder()
    .setUserId(42)
    .setName("alice")
    .build();

// Blocking call
MyResponse response = stub.getUser(request);

System.out.println(response.getId());
System.out.println(response.getEmail());
```

---

## Handle Errors

```java
try {
    MyResponse response = stub.getUser(request);
} catch (io.grpc.StatusRuntimeException e) {
    io.grpc.Status status = e.getStatus();
    System.out.println("Code: " + status.getCode());
    System.out.println("Message: " + status.getDescription());

    switch (status.getCode()) {
        case NOT_FOUND:
            System.out.println("User not found");
            break;
        case INVALID_ARGUMENT:
            System.out.println("Invalid request");
            break;
        case UNAVAILABLE:
            System.out.println("Service unavailable");
            break;
        default:
            System.out.println("Other error");
    }
}
```

---

## Timeout

```java
// Add timeout
stub = stub.withDeadlineAfter(5, java.util.concurrent.TimeUnit.SECONDS);

MyResponse response = stub.getUser(request);
```

---

## Retry with Exponential Backoff

```java
MyResponse callWithRetry(MyServiceGrpc.MyServiceBlockingStub stub,
                          MyRequest request,
                          int maxRetries) {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            return stub.getUser(request);
        } catch (io.grpc.StatusRuntimeException e) {
            if (attempt == maxRetries) throw e;
            if (!isRetryable(e.getStatus().getCode())) throw e;

            long delayMs = (long) Math.pow(2, attempt - 1) * 100;
            Thread.sleep(delayMs);
        }
    }
    throw new IllegalStateException("Should not reach here");
}

boolean isRetryable(io.grpc.Status.Code code) {
    return code == io.grpc.Status.Code.UNAVAILABLE ||
           code == io.grpc.Status.Code.DEADLINE_EXCEEDED;
}
```

---

## Concurrent Calls with Virtual Threads

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<MyResponse>> futures = new ArrayList<>();

    for (int i = 1; i <= 100; i++) {
        final int id = i;
        futures.add(executor.submit(() -> {
            MyRequest req = MyRequest.newBuilder().setUserId(id).build();
            return stub.getUser(req);
        }));
    }

    // Collect results
    for (Future<MyResponse> future : futures) {
        MyResponse response = future.get();
        // Process response
    }
}
```

---

## Validate Response

```java
MyResponse response = stub.getUser(request);

// Check fields
assert response.getId() == 42;
assert response.getEmail().equals("alice@example.com");
assert !response.getEmail().isEmpty();

// Check nested fields
if (response.hasProfile()) {
    assert response.getProfile().getAge() > 0;
}
```

---

## Best Practices

✅ **DO:**
- Always shutdown channels
- Handle StatusRuntimeException
- Use timeouts
- Implement retry for transient failures
- Use blocking stub for tests

❌ **DON'T:**
- Leave channels open
- Retry on non-idempotent calls without care
- Assume immediate failure (use timeouts)

---

## See Also

- [How-to: Handle gRPC Streaming](grpc-streaming.md)
- [How-to: Test gRPC Error Codes](grpc-error-handling.md)
- [Tutorial: gRPC Streaming](../tutorials/grpc-streaming.md)
- [Reference: gRPC Testing](../reference/grpc-reference.md)
