# How-to: Handle gRPC Streaming

Test server streaming, client streaming, and bidirectional streaming.

---

## Server Streaming

```java
// Blocking iterator
Iterator<UserResponse> responses = stub.listUsers(request);

List<UserResponse> all = new ArrayList<>();
while (responses.hasNext()) {
    all.add(responses.next());
}

System.out.println("Received " + all.size() + " users");
```

---

## Server Streaming with Timeout

```java
Iterator<UserResponse> responses = stub
    .withDeadlineAfter(10, TimeUnit.SECONDS)
    .listUsers(request);

while (responses.hasNext()) {
    UserResponse user = responses.next();
    System.out.println(user.getName());
}
```

---

## Client Streaming

```java
StreamObserver<CreateResponse> responseObserver =
    new StreamObserver<CreateResponse>() {
        @Override
        public void onNext(CreateResponse response) {
            System.out.println("Response: " + response.getCreatedCount());
        }

        @Override
        public void onError(Throwable t) {
            System.err.println("Error: " + t.getMessage());
        }

        @Override
        public void onCompleted() {
            System.out.println("Server finished processing stream");
        }
    };

StreamObserver<UserRequest> requestObserver = stub.createUsers(responseObserver);

// Send multiple requests
for (int i = 1; i <= 100; i++) {
    UserRequest req = UserRequest.newBuilder()
        .setName("user" + i)
        .setEmail("user" + i + "@example.com")
        .build();

    requestObserver.onNext(req);
}

// Signal completion
requestObserver.onCompleted();
```

---

## Bidirectional Streaming

```java
List<String> serverMessages = Collections.synchronizedList(new ArrayList<>());
CountDownLatch completeLatch = new CountDownLatch(1);

StreamObserver<MessageResponse> responseObserver =
    new StreamObserver<MessageResponse>() {
        @Override
        public void onNext(MessageResponse message) {
            serverMessages.add(message.getContent());
        }

        @Override
        public void onError(Throwable t) {
            System.err.println("Error: " + t.getMessage());
            completeLatch.countDown();
        }

        @Override
        public void onCompleted() {
            System.out.println("Server closed stream");
            completeLatch.countDown();
        }
    };

StreamObserver<MessageRequest> requestObserver = stub.chat(responseObserver);

// Send messages
for (String msg : messages) {
    requestObserver.onNext(
        MessageRequest.newBuilder().setContent(msg).build());
    Thread.sleep(100); // Simulate delay
}

requestObserver.onCompleted();

// Wait for server
boolean done = completeLatch.await(10, TimeUnit.SECONDS);
System.out.println("Received " + serverMessages.size() + " messages");
```

---

## Handle Streaming Errors

```java
StreamObserver<Response> observer = new StreamObserver<Response>() {
    @Override
    public void onNext(Response response) {
        // Process message
    }

    @Override
    public void onError(Throwable t) {
        if (t instanceof StatusRuntimeException) {
            StatusRuntimeException e = (StatusRuntimeException) t;
            System.out.println("gRPC error: " + e.getStatus().getCode());
        } else {
            System.out.println("Other error: " + t.getMessage());
        }
    }

    @Override
    public void onCompleted() {
        // Stream done
    }
};

// Use observer...
```

---

## Collect All Streamed Results

```java
// Collect server stream into list
Iterator<Response> stream = stub.listItems(request);
List<Response> allItems = new ArrayList<>();

while (stream.hasNext()) {
    allItems.add(stream.next());
}

System.out.println("Got " + allItems.size() + " items");
```

---

## Concurrent Bidirectional Streams

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Future<List<String>>> futures = new ArrayList<>();

    for (int clientId = 1; clientId <= 10; clientId++) {
        final int id = clientId;
        futures.add(executor.submit(() -> {
            List<String> messages = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(1);

            StreamObserver<MessageResponse> responses =
                new StreamObserver<MessageResponse>() {
                    @Override
                    public void onNext(MessageResponse msg) {
                        messages.add(msg.getContent());
                    }

                    @Override
                    public void onError(Throwable t) {
                        latch.countDown();
                    }

                    @Override
                    public void onCompleted() {
                        latch.countDown();
                    }
                };

            StreamObserver<MessageRequest> requests = stub.chat(responses);

            // Send messages
            for (int i = 0; i < 5; i++) {
                requests.onNext(
                    MessageRequest.newBuilder()
                        .setContent("Client " + id + " msg " + i)
                        .build());
            }

            requests.onCompleted();
            latch.await(5, TimeUnit.SECONDS);
            return messages;
        }));
    }

    // Collect results from all streams
    for (Future<List<String>> future : futures) {
        List<String> messages = future.get();
        System.out.println("Stream got " + messages.size() + " responses");
    }
}
```

---

## Best Practices

✅ **DO:**
- Wait for `onCompleted()` before assuming stream is done
- Handle all three callbacks (@OnNext, @OnError, @OnCompleted)
- Use `CountDownLatch` to synchronize async streams
- Test with actual server (streams are stateful)

❌ **DON'T:**
- Assume immediate message arrival
- Forget to call `onCompleted()` (signals end to server)
- Leave streams open

---

## See Also

- [How-to: Make Unary RPC Calls](grpc-unary.md)
- [How-to: Test gRPC Error Codes](grpc-error-handling.md)
- [Tutorial: gRPC Streaming](../tutorials/grpc-streaming.md)
