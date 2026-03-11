# Tutorial: gRPC Streaming for Efficient Real-Time RPC

Learn how to test gRPC services with DocTester. gRPC is a high-performance RPC framework built on HTTP/2 that supports streaming, multiplexing, and binary protocols — ideal for microservices and real-time applications.

**Time:** ~45 minutes
**Prerequisites:** Java 25, protobuf basics, understanding of RPC
**What you'll learn:** How to test unary RPC, server streaming, client streaming, and bidirectional streaming

---

## What is gRPC?

gRPC (Google Remote Procedure Call) uses protobuf for efficient serialization and HTTP/2 for transport:

```
Client                          Server
  |                               |
  |---- Call + protobuf data ---->|
  |<----- Response + data --------|
  |                               |
  | (Streaming)                   |
  |---- Stream of messages ------>|
  |<----- Stream of responses -----|
  |                               |
```

**Key advantages:**
- ✅ Binary protocol (smaller messages, faster parsing)
- ✅ HTTP/2 multiplexing (multiple calls on one connection)
- ✅ Four RPC modes: unary, server-streaming, client-streaming, bidirectional
- ✅ Generated code (strongly typed)
- ✅ Language-agnostic (Java, Go, Python, etc.)

---

## Setup: gRPC Dependencies

Add to `pom.xml`:

```xml
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-netty-shaded</artifactId>
    <version>1.63.0</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-protobuf</artifactId>
    <version>1.63.0</version>
</dependency>
<dependency>
    <groupId>io.grpc</groupId>
    <artifactId>grpc-stub</artifactId>
    <version>1.63.0</version>
</dependency>
```

---

## Step 1 — Unary RPC (Simple Request/Response)

Test a simple gRPC call:

```java
package com.example;

import org.junit.Test;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import com.example.UserServiceGrpc;
import com.example.UserProto;

public class GrpcUnaryDocTest extends DocTester {

    @Test
    public void unaryRpcCall() throws Exception {

        sayNextSection("Unary RPC: Request/Response");

        say("A unary RPC is the simplest gRPC pattern: client sends one request, "
            + "server responds with one reply.");

        // Create channel (connection to gRPC server)
        ManagedChannel channel = ManagedChannelBuilder
            .forAddress("localhost", 50051)
            .usePlaintext()
            .build();

        say("Connected to gRPC server at localhost:50051");

        // Create stub (client for UserService)
        UserServiceGrpc.UserServiceBlockingStub stub =
            UserServiceGrpc.newBlockingStub(channel);

        // Create request
        UserProto.GetUserRequest request = UserProto.GetUserRequest.newBuilder()
            .setUserId(42)
            .build();

        say("Sending request: GetUser(id=42)");

        // Call RPC
        UserProto.UserResponse response = stub.getUser(request);

        say("Received response: " + response.getName() + " (" + response.getEmail() + ")");

        sayAndAssertThat(
            "User retrieved successfully",
            42,
            org.hamcrest.CoreMatchers.equalTo(response.getUserId()));

        channel.shutdown();
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**Protobuf definition (.proto):**
```protobuf
syntax = "proto3";

package com.example;

service UserService {
  rpc GetUser(GetUserRequest) returns (UserResponse);
}

message GetUserRequest {
  int32 user_id = 1;
}

message UserResponse {
  int32 user_id = 1;
  string name = 2;
  string email = 3;
}
```

---

## Step 2 — Server-Streaming RPC

Server sends a stream of responses:

```java
@Test
public void serverStreamingRpc() throws Exception {

    sayNextSection("Server Streaming: One Request, Multiple Responses");

    say("Server streaming allows the server to send multiple messages "
        + "in response to a single client request.");

    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 50051)
        .usePlaintext()
        .build();

    UserServiceGrpc.UserServiceBlockingStub stub =
        UserServiceGrpc.newBlockingStub(channel);

    say("Requesting list of all users...");

    // Request all users (returns an Iterator of responses)
    UserProto.Empty request = UserProto.Empty.newBuilder().build();

    Iterator<UserProto.UserResponse> responses = stub.listUsers(request);

    List<UserProto.UserResponse> users = new ArrayList<>();
    while (responses.hasNext()) {
        UserProto.UserResponse user = responses.next();
        users.add(user);
        say("Streamed user: " + user.getName());
    }

    sayAndAssertThat(
        "Multiple users received in stream",
        true,
        org.hamcrest.CoreMatchers.is(users.size() > 0));

    say("Total users streamed: " + users.size());

    channel.shutdown();
}
```

**Protobuf:**
```protobuf
service UserService {
  rpc ListUsers(Empty) returns (stream UserResponse);
}

message Empty {}
```

---

## Step 3 — Client-Streaming RPC

Client sends a stream of requests:

```java
@Test
public void clientStreamingRpc() throws Exception {

    sayNextSection("Client Streaming: Multiple Requests, One Response");

    say("Client streaming allows the client to send multiple messages, "
        + "and the server responds once when done.");

    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 50051)
        .usePlaintext()
        .build();

    UserServiceGrpc.UserServiceStub asyncStub =
        UserServiceGrpc.newStub(channel);

    say("Streaming user data to server...");

    // For client streaming, use StreamObserver
    StreamObserver<UserProto.UserResponse> responseObserver =
        new StreamObserver<UserProto.UserResponse>() {
            @Override
            public void onNext(UserProto.UserResponse response) {
                say("Server response: " + response.getMessage());
            }

            @Override
            public void onError(Throwable t) {
                say("Error: " + t.getMessage());
            }

            @Override
            public void onCompleted() {
                say("Server finished processing stream");
            }
        };

    StreamObserver<UserProto.UserRequest> requestObserver =
        asyncStub.createUsers(responseObserver);

    // Send multiple requests
    for (int i = 1; i <= 3; i++) {
        UserProto.UserRequest req = UserProto.UserRequest.newBuilder()
            .setName("User" + i)
            .setEmail("user" + i + "@example.com")
            .build();

        requestObserver.onNext(req);
        say("Streamed user: User" + i);
    }

    // Close stream (signals end to server)
    requestObserver.onCompleted();

    say("All users streamed, waiting for server response...");

    // Wait a bit for async response
    Thread.sleep(1000);

    channel.shutdown();
}
```

**Protobuf:**
```protobuf
service UserService {
  rpc CreateUsers(stream UserRequest) returns (CreateResponse);
}

message UserRequest {
  string name = 1;
  string email = 2;
}

message CreateResponse {
  string message = 1;
  int32 created_count = 2;
}
```

---

## Step 4 — Bidirectional Streaming RPC

Both client and server send streams simultaneously:

```java
@Test
public void bidirectionalStreamingRpc() throws Exception {

    sayNextSection("Bidirectional Streaming");

    say("Both client and server can send messages concurrently. "
        + "This is ideal for chat, collaborative editing, or real-time synchronization.");

    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 50051)
        .usePlaintext()
        .build();

    UserServiceGrpc.UserServiceStub asyncStub =
        UserServiceGrpc.newStub(channel);

    // Responses from server
    List<String> serverMessages = Collections.synchronizedList(new ArrayList<>());
    CountDownLatch completeLatch = new CountDownLatch(1);

    StreamObserver<UserProto.UserMessage> responseObserver =
        new StreamObserver<UserProto.UserMessage>() {
            @Override
            public void onNext(UserProto.UserMessage message) {
                String msg = message.getContent();
                serverMessages.add(msg);
                say("Received from server: " + msg);
            }

            @Override
            public void onError(Throwable t) {
                say("Error: " + t.getMessage());
                completeLatch.countDown();
            }

            @Override
            public void onCompleted() {
                say("Server stream closed");
                completeLatch.countDown();
            }
        };

    // Request stream (bidirectional chat)
    StreamObserver<UserProto.UserMessage> requestObserver =
        asyncStub.chat(responseObserver);

    // Send messages while receiving
    String[] clientMessages = {
        "alice: Hello!",
        "alice: Anyone there?",
        "alice: See you!"
    };

    for (String clientMsg : clientMessages) {
        UserProto.UserMessage msg = UserProto.UserMessage.newBuilder()
            .setContent(clientMsg)
            .build();

        requestObserver.onNext(msg);
        say("Sent: " + clientMsg);
        Thread.sleep(200); // Simulate typing
    }

    requestObserver.onCompleted();
    say("Stream closed from client side");

    // Wait for server to finish
    boolean finished = completeLatch.await(5, java.util.concurrent.TimeUnit.SECONDS);

    sayAndAssertThat(
        "Server responded to bidirectional stream",
        true,
        org.hamcrest.CoreMatchers.is(serverMessages.size() > 0));

    say("Total messages from server: " + serverMessages.size());

    channel.shutdown();
}
```

**Protobuf:**
```protobuf
service UserService {
  rpc Chat(stream UserMessage) returns (stream UserMessage);
}

message UserMessage {
  string content = 1;
}
```

---

## Step 5 — Error Handling and Retries

Handle gRPC errors:

```java
@Test
public void errorHandlingInGrpc() throws Exception {

    sayNextSection("Error Handling");

    say("gRPC operations can fail. Handle `StatusRuntimeException` for standard errors.");

    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 50051)
        .usePlaintext()
        .build();

    UserServiceGrpc.UserServiceBlockingStub stub =
        UserServiceGrpc.newBlockingStub(channel);

    try {
        // Request non-existent user
        UserProto.GetUserRequest request = UserProto.GetUserRequest.newBuilder()
            .setUserId(99999) // Doesn't exist
            .build();

        say("Requesting non-existent user...");
        UserProto.UserResponse response = stub.getUser(request);

    } catch (io.grpc.StatusRuntimeException e) {
        say("✓ Caught expected error: " + e.getStatus().getCode());
        say("  Message: " + e.getStatus().getDescription());

        sayAndAssertThat(
            "Error is NOT_FOUND",
            io.grpc.Status.Code.NOT_FOUND,
            org.hamcrest.CoreMatchers.equalTo(e.getStatus().getCode()));
    }

    channel.shutdown();
}
```

---

## Step 6 — Performance: Virtual Threads + gRPC

Use Java 25 virtual threads for concurrent gRPC calls:

```java
@Test
public void grpcWithVirtualThreads() throws Exception {

    sayNextSection("gRPC with Virtual Threads");

    say("Combine Java 25 virtual threads with gRPC for lightweight concurrent testing. "
        + "Handle thousands of RPC calls without thread resource exhaustion.");

    ManagedChannel channel = ManagedChannelBuilder
        .forAddress("localhost", 50051)
        .usePlaintext()
        .build();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

        List<Future<UserProto.UserResponse>> futures = new ArrayList<>();

        say("Submitting 1000 concurrent RPC calls...");

        for (int userId = 1; userId <= 1000; userId++) {
            final int id = userId;
            futures.add(executor.submit(() -> {
                UserServiceGrpc.UserServiceBlockingStub stub =
                    UserServiceGrpc.newBlockingStub(channel);

                UserProto.GetUserRequest request =
                    UserProto.GetUserRequest.newBuilder()
                        .setUserId(id)
                        .build();

                return stub.getUser(request);
            }));
        }

        // Collect results
        long successCount = 0;
        for (Future<UserProto.UserResponse> future : futures) {
            try {
                UserProto.UserResponse response = future.get();
                if (response != null) {
                    successCount++;
                }
            } catch (Exception e) {
                // Handle errors
            }
        }

        say("Successfully retrieved " + successCount + " users");

        sayAndAssertThat(
            "All RPC calls completed",
            1000L,
            org.hamcrest.CoreMatchers.equalTo(successCount));
    }

    channel.shutdown();
}
```

---

## Key Takeaways

| Concept | Explanation |
|---------|-------------|
| **Channel** | Connection to gRPC server |
| **Stub** | Client for calling RPC methods |
| **Unary** | Single request, single response |
| **Server Streaming** | Single request, stream of responses |
| **Client Streaming** | Stream of requests, single response |
| **Bidirectional** | Stream of requests and responses |
| **StatusRuntimeException** | Standard error type in gRPC |
| **StreamObserver** | Asynchronous callback for streaming |

---

## Best Practices

✅ **DO:**
- Use `BlockingStub` for synchronous tests
- Use `StreamObserver` for async/streaming tests
- Always shutdown channels
- Handle `StatusRuntimeException` for errors
- Use virtual threads for concurrent RPC loads
- Validate response fields

❌ **DON'T:**
- Leave channels open (resource leaks)
- Ignore streaming completion (may hang)
- Mix sync and async APIs in confusing ways
- Assume message ordering in bidirectional streams (not guaranteed)

---

## Next Steps

- [How-to: Make Unary RPC Calls](../how-to/grpc-unary.md)
- [How-to: Handle gRPC Streaming](../how-to/grpc-streaming.md)
- [How-to: Test gRPC Error Codes](../how-to/grpc-error-handling.md)
- [Tutorial: Server-Sent Events](server-sent-events.md)
- [Tutorial: WebSockets](websockets-realtime.md)
- [Reference: gRPC Testing API](../reference/grpc-reference.md)
