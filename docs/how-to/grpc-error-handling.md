# How-to: Test gRPC Error Codes

Handle and validate gRPC error responses.

---

## Catch Status Runtime Exception

```java
try {
    MyRequest request = MyRequest.newBuilder()
        .setUserId(999) // Non-existent
        .build();

    MyResponse response = stub.getUser(request);

} catch (io.grpc.StatusRuntimeException e) {
    io.grpc.Status status = e.getStatus();
    System.out.println("Code: " + status.getCode());
    System.out.println("Description: " + status.getDescription());
}
```

---

## Test Specific Error Codes

```java
void testNotFoundError() {
    MyRequest request = MyRequest.newBuilder()
        .setUserId(99999)
        .build();

    try {
        stub.getUser(request);
        fail("Should have thrown StatusRuntimeException");
    } catch (io.grpc.StatusRuntimeException e) {
        assert e.getStatus().getCode() == io.grpc.Status.Code.NOT_FOUND;
        assert e.getStatus().getDescription().contains("User not found");
    }
}

void testInvalidArgumentError() {
    MyRequest request = MyRequest.newBuilder()
        .setUserId(-1) // Invalid ID
        .build();

    try {
        stub.getUser(request);
        fail("Should have thrown StatusRuntimeException");
    } catch (io.grpc.StatusRuntimeException e) {
        assert e.getStatus().getCode() == io.grpc.Status.Code.INVALID_ARGUMENT;
    }
}

void testPermissionDeniedError() {
    MyRequest request = MyRequest.newBuilder()
        .setUserId(42)
        .build();

    try {
        stub.getUser(request);
        fail("Should have thrown StatusRuntimeException");
    } catch (io.grpc.StatusRuntimeException e) {
        assert e.getStatus().getCode() == io.grpc.Status.Code.PERMISSION_DENIED;
    }
}
```

---

## Standard gRPC Error Codes

```java
// Common error codes and meanings:
io.grpc.Status.Code.OK                  // Success (0)
io.grpc.Status.Code.CANCELLED           // Cancelled (1)
io.grpc.Status.Code.UNKNOWN             // Unknown error (2)
io.grpc.Status.Code.INVALID_ARGUMENT    // Bad argument (3)
io.grpc.Status.Code.DEADLINE_EXCEEDED   // Timeout (4)
io.grpc.Status.Code.NOT_FOUND           // Not found (5)
io.grpc.Status.Code.ALREADY_EXISTS      // Already exists (6)
io.grpc.Status.Code.PERMISSION_DENIED   // No permission (7)
io.grpc.Status.Code.RESOURCE_EXHAUSTED  // Quota exceeded (8)
io.grpc.Status.Code.FAILED_PRECONDITION // Invalid state (9)
io.grpc.Status.Code.ABORTED             // Aborted (10)
io.grpc.Status.Code.OUT_OF_RANGE        // Out of range (11)
io.grpc.Status.Code.UNIMPLEMENTED       // Not implemented (12)
io.grpc.Status.Code.INTERNAL            // Server error (13)
io.grpc.Status.Code.UNAVAILABLE         // Service unavailable (14)
io.grpc.Status.Code.DATA_LOSS           // Data loss (15)
io.grpc.Status.Code.UNAUTHENTICATED     // No authentication (16)
```

---

## Extract Error Details

```java
void handleGrpcError(StatusRuntimeException e) {
    io.grpc.Status status = e.getStatus();
    String code = status.getCode().name();
    String message = status.getDescription();
    Throwable cause = status.getCause();

    System.err.println("Error Code: " + code);
    System.err.println("Error Message: " + message);
    if (cause != null) {
        System.err.println("Caused by: " + cause.getMessage());
    }

    // Get metadata if available
    Metadata metadata = io.grpc.Status.trailingMetadataFromThrowable(e);
    if (metadata != null) {
        System.err.println("Metadata: " + metadata);
    }
}
```

---

## Retry on Transient Errors

```java
MyResponse callWithRetry(MyServiceGrpc.MyServiceBlockingStub stub,
                        MyRequest request,
                        int maxRetries) throws io.grpc.StatusRuntimeException {
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            return stub.getUser(request);
        } catch (io.grpc.StatusRuntimeException e) {
            io.grpc.Status.Code code = e.getStatus().getCode();

            // Only retry transient errors
            if (code == io.grpc.Status.Code.UNAVAILABLE ||
                code == io.grpc.Status.Code.DEADLINE_EXCEEDED ||
                code == io.grpc.Status.Code.RESOURCE_EXHAUSTED) {

                if (attempt == maxRetries) throw e;

                long delayMs = (long) Math.pow(2, attempt - 1) * 100;
                System.out.println("Retrying in " + delayMs + "ms...");
                Thread.sleep(delayMs);
            } else {
                // Don't retry permanent errors
                throw e;
            }
        }
    }
    throw new IllegalStateException("Should not reach here");
}
```

---

## Handle Streaming Errors

```java
StreamObserver<Response> responseObserver = new StreamObserver<Response>() {
    @Override
    public void onNext(Response response) {
        System.out.println("Got: " + response);
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof io.grpc.StatusRuntimeException) {
            io.grpc.StatusRuntimeException e = (io.grpc.StatusRuntimeException) t;
            System.err.println("Stream error: " + e.getStatus().getCode());
        } else {
            System.err.println("Non-gRPC error: " + t.getMessage());
        }
    }

    @Override
    public void onCompleted() {
        System.out.println("Stream completed");
    }
};

StreamObserver<Request> requests = stub.streamMethod(responseObserver);
// Use requests...
```

---

## Test Deadline Exceeded

```java
void testDeadlineExceeded() {
    try {
        MyResponse response = stub
            .withDeadlineAfter(100, java.util.concurrent.TimeUnit.MILLISECONDS)
            .slowMethod(request);

        fail("Should timeout");
    } catch (io.grpc.StatusRuntimeException e) {
        assert e.getStatus().getCode() == io.grpc.Status.Code.DEADLINE_EXCEEDED;
    }
}
```

---

## Custom Error Details

```java
// Some servers include custom error details in metadata
io.grpc.StatusRuntimeException error = ...;
io.grpc.Metadata metadata = io.grpc.Status.trailingMetadataFromThrowable(error);

if (metadata != null) {
    // Check for custom keys
    String customError = metadata.get(
        io.grpc.Metadata.Key.of("x-custom-error", io.grpc.Metadata.ASCII_STRING_MARSHALLER));

    if (customError != null) {
        System.out.println("Custom error: " + customError);
    }
}
```

---

## Best Practices

✅ **DO:**
- Check error code first (identity)
- Use specific catch patterns
- Retry only transient errors
- Log full error details
- Extract metadata for context

❌ **DON'T:**
- Retry permanent errors (BAD_ARGUMENT, NOT_FOUND, PERMISSION_DENIED)
- Retry indefinitely
- Ignore error descriptions
- Assume all errors are transient

---

## Error Decision Tree

```
Is it UNAVAILABLE or DEADLINE_EXCEEDED?
  YES → Retry with exponential backoff
  NO  → Check if it's transient
    Is it RESOURCE_EXHAUSTED?
      YES → Retry (server is busy)
      NO  → Permanent error, fail immediately
```

---

## See Also

- [How-to: Make Unary RPC Calls](grpc-unary.md)
- [How-to: Handle gRPC Streaming](grpc-streaming.md)
- [Tutorial: gRPC Streaming](../tutorials/grpc-streaming.md)
