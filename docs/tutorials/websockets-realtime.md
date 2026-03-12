# Tutorial: WebSockets for Real-Time Bidirectional Communication

Learn how to test WebSocket servers with DTR. WebSockets enable persistent, full-duplex communication between client and server — perfect for real-time applications like chat, notifications, and live dashboards.

**Time:** ~40 minutes
**Prerequisites:** Java 25, understanding of HTTP and networking
**What you'll learn:** How to connect to WebSocket servers, send and receive messages, handle disconnections, and document real-time behavior

---

## What Are WebSockets?

WebSockets upgrade an HTTP connection into a persistent, bidirectional socket:

```
Client                          Server
  |                               |
  |-------- HTTP Upgrade -------->|
  |<--- 101 Switching Protocols --|
  |                               |
  |-------- Message 1 ----------->|
  |<------ Response Message ------|
  |                               |
  |-------- Message 2 ----------->|
  |<------ Broadcast Message -----|
  |                               |
  |--------- (persistent) --------|
```

**Key differences from HTTP:**
- ✅ Persistent connection (no request/response cycle)
- ✅ Bidirectional (server can push to client)
- ✅ Lower latency (no HTTP headers on each message)
- ✅ Binary or text frames
- ❌ No request semantics (no GET/POST/PUT)

---

## Step 1 — Connect to a WebSocket Server

Create a WebSocket test:

```java
package com.example;

import org.junit.Test;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebSocketDocTest extends DTR {

    @Test
    public void connectToWebSocketServer() throws Exception {

        sayNextSection("WebSocket Connection");

        say("Connect to a WebSocket server and establish a persistent connection.");

        // Server URL (e.g., ws://localhost:8080/chat)
        String wsUrl = "ws://localhost:8080/chat";

        say("Connecting to: " + wsUrl);

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Session session = container.connectToServer(
            new ChatClientEndpoint(),
            URI.create(wsUrl));

        sayAndAssertThat(
            "WebSocket connection established",
            true,
            org.hamcrest.CoreMatchers.is(session.isOpen()));

        say("The WebSocket connection is now open and ready for messages.");

        session.close();
    }

    @ClientEndpoint
    public static class ChatClientEndpoint {
        // Receives messages from the server
        public void onMessage(String message) {
            System.out.println("Received: " + message);
        }

        public void onError(Session session, Throwable error) {
            System.err.println("WebSocket error: " + error.getMessage());
        }
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

---

## Step 2 — Send and Receive Messages

Exchange messages with the server:

```java
@Test
public void sendAndReceiveMessages() throws Exception {

    sayNextSection("Send and Receive Messages");

    say("WebSockets are bidirectional: both client and server can send messages at any time.");

    WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    // Create endpoint that captures received messages
    ChatEndpoint endpoint = new ChatEndpoint();
    Session session = container.connectToServer(
        endpoint,
        URI.create("ws://localhost:8080/chat"));

    say("Connected to WebSocket server");

    // Send a message
    String outgoingMessage = "Hello, WebSocket!";
    session.getBasicRemote().sendText(outgoingMessage);

    say("Sent message: " + outgoingMessage);

    // Wait for response
    boolean received = endpoint.waitForMessage(5, TimeUnit.SECONDS);

    sayAndAssertThat(
        "Server responded with a message",
        true,
        org.hamcrest.CoreMatchers.is(received));

    say("Received message: " + endpoint.getLastMessage());

    session.close();
}

@ClientEndpoint
public static class ChatEndpoint {
    private String lastMessage;
    private CountDownLatch latch = new CountDownLatch(1);

    @javax.websocket.OnMessage
    public void onMessage(String message) {
        this.lastMessage = message;
        latch.countDown();
    }

    public boolean waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
```

---

## Step 3 — Bidirectional Message Exchange

Send multiple messages and receive server broadcasts:

```java
@Test
public void bidirectionalMessaging() throws Exception {

    sayNextSection("Bidirectional Messaging");

    say("Once connected, both client and server can send messages independently. "
        + "The server might broadcast to all connected clients.");

    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    BidirectionalEndpoint endpoint = new BidirectionalEndpoint();

    Session session = container.connectToServer(
        endpoint,
        URI.create("ws://localhost:8080/chat"));

    say("Connected to chat server");

    // Send multiple messages
    String[] messages = {
        "user joined",
        "Hello everyone!",
        "Anyone there?",
        "user left"
    };

    for (String msg : messages) {
        session.getBasicRemote().sendText(msg);
        say("Sent: " + msg);
        Thread.sleep(100); // Small delay to see sequence
    }

    // Server may echo, broadcast, or acknowledge
    int messagesReceived = endpoint.getReceivedMessages().size();

    say("Received " + messagesReceived + " messages from server");

    sayAndAssertThat(
        "Server acknowledged the messages",
        true,
        org.hamcrest.CoreMatchers.is(messagesReceived > 0));

    session.close();
}

@ClientEndpoint
public static class BidirectionalEndpoint {
    private List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

    @javax.websocket.OnMessage
    public void onMessage(String message) {
        receivedMessages.add(message);
        System.out.println("Received: " + message);
    }

    public List<String> getReceivedMessages() {
        return new ArrayList<>(receivedMessages);
    }
}
```

---

## Step 4 — Handle Connection Lifecycle

Test connection open, error, and close events:

```java
@Test
public void connectionLifecycle() throws Exception {

    sayNextSection("Connection Lifecycle");

    say("WebSocket connections have distinct lifecycle events: open, message, error, close.");

    WebSocketContainer container = ContainerProvider.getWebSocketContainer();
    LifecycleEndpoint endpoint = new LifecycleEndpoint();

    say("Connecting to server...");
    Session session = container.connectToServer(
        endpoint,
        URI.create("ws://localhost:8080/chat"));

    sayAndAssertThat(
        "Connection opened event fired",
        true,
        org.hamcrest.CoreMatchers.is(endpoint.isOpened()));

    // Send and receive
    session.getBasicRemote().sendText("test");
    Thread.sleep(500);

    // Close connection
    say("Closing connection gracefully...");
    session.close();

    // Wait for close event
    boolean closed = endpoint.waitForClose(2, TimeUnit.SECONDS);

    sayAndAssertThat(
        "Connection closed event fired",
        true,
        org.hamcrest.CoreMatchers.is(closed));
}

@ClientEndpoint
public static class LifecycleEndpoint {
    private boolean opened = false;
    private CountDownLatch closeLatch = new CountDownLatch(1);

    @javax.websocket.OnOpen
    public void onOpen(Session session) {
        this.opened = true;
        System.out.println("WebSocket opened");
    }

    @javax.websocket.OnMessage
    public void onMessage(String message) {
        System.out.println("Message: " + message);
    }

    @javax.websocket.OnClose
    public void onClose(CloseReason reason) {
        System.out.println("WebSocket closed: " + reason);
        closeLatch.countDown();
    }

    @javax.websocket.OnError
    public void onError(Session session, Throwable error) {
        System.err.println("WebSocket error: " + error.getMessage());
    }

    public boolean isOpened() {
        return opened;
    }

    public boolean waitForClose(long timeout, TimeUnit unit) throws InterruptedException {
        return closeLatch.await(timeout, unit);
    }
}
```

---

## Step 5 — Error Handling and Reconnection

Handle connection failures and implement reconnect logic:

```java
@Test
public void errorHandlingAndReconnect() throws Exception {

    sayNextSection("Error Handling and Reconnection");

    say("When WebSocket connections fail, implement automatic reconnection with exponential backoff.");

    int maxRetries = 3;
    int retryDelayMs = 100;

    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            say("Connection attempt " + attempt);

            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            Session session = container.connectToServer(
                new ResilientEndpoint(),
                URI.create("ws://localhost:8080/chat"));

            say("✓ Connected on attempt " + attempt);

            session.close();
            break; // Success

        } catch (Exception e) {
            say("✗ Connection attempt " + attempt + " failed: " + e.getMessage());

            if (attempt < maxRetries) {
                say("Retrying in " + retryDelayMs + "ms...");
                Thread.sleep(retryDelayMs);
                retryDelayMs *= 2; // Exponential backoff
            }
        }
    }

    sayAndAssertThat(
        "Reconnection strategy implemented",
        true,
        org.hamcrest.CoreMatchers.is(true));
}

@ClientEndpoint
public static class ResilientEndpoint {
    @javax.websocket.OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected");
    }

    @javax.websocket.OnError
    public void onError(Session session, Throwable error) {
        System.err.println("Error: " + error.getMessage());
    }
}
```

---

## Step 6 — Test a Real Chat Application

Combine all patterns to test a complete chat server:

```java
@Test
public void testChatApplication() throws Exception {

    sayNextSection("Complete Chat Test");

    say("Test a multi-user chat application: users connect, send messages, "
        + "the server broadcasts to all participants.");

    // Connect two clients
    WebSocketContainer container = ContainerProvider.getWebSocketContainer();

    ChatUser user1 = new ChatUser("alice");
    ChatUser user2 = new ChatUser("bob");

    Session session1 = container.connectToServer(
        user1,
        URI.create("ws://localhost:8080/chat"));

    Session session2 = container.connectToServer(
        user2,
        URI.create("ws://localhost:8080/chat"));

    say("Two clients connected: alice and bob");

    // User 1 sends message
    session1.getBasicRemote().sendText("alice: Hello everyone!");
    say("alice sent: Hello everyone!");

    // Both should receive the broadcast
    boolean user2ReceivedMsg = user2.waitForMessage(2, TimeUnit.SECONDS);

    sayAndAssertThat(
        "Message broadcasted to other user",
        true,
        org.hamcrest.CoreMatchers.is(user2ReceivedMsg));

    say("Message received by: " + user2.getLastMessage());

    // User 2 replies
    session2.getBasicRemote().sendText("bob: Hi alice!");
    say("bob sent: Hi alice!");

    boolean user1ReceivedReply = user1.waitForMessage(2, TimeUnit.SECONDS);

    sayAndAssertThat(
        "Reply received by original sender",
        true,
        org.hamcrest.CoreMatchers.is(user1ReceivedReply));

    // Close both
    session1.close();
    session2.close();

    say("Chat session ended gracefully");
}

@ClientEndpoint
public static class ChatUser {
    private String username;
    private String lastMessage;
    private CountDownLatch messageLatch = new CountDownLatch(1);

    public ChatUser(String username) {
        this.username = username;
    }

    @javax.websocket.OnMessage
    public void onMessage(String message) {
        this.lastMessage = message;
        messageLatch.countDown();
        messageLatch = new CountDownLatch(1); // Reset for next message
    }

    public boolean waitForMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return messageLatch.await(timeout, unit);
    }

    public String getLastMessage() {
        return lastMessage;
    }
}
```

---

## Key Takeaways

| Concept | Explanation |
|---------|-------------|
| **WebSocket** | Persistent, bidirectional communication over single TCP connection |
| **Session** | Represents an active WebSocket connection |
| **@OnOpen** | Callback when client connects successfully |
| **@OnMessage** | Callback when message arrives from server |
| **@OnError** | Callback when error occurs |
| **@OnClose** | Callback when connection closes |
| **sendText()** | Send a text message to the server |
| **Container** | Factory for connecting to WebSocket servers |

---

## Best Practices

✅ **DO:**
- Use `@ClientEndpoint` for test clients
- Wait for messages with `CountDownLatch` or similar synchronization
- Handle all lifecycle events (@OnOpen, @OnMessage, @OnError, @OnClose)
- Test with actual server, not mocks (WebSockets are stateful)
- Implement exponential backoff for reconnections
- Always close sessions to free resources

❌ **DON'T:**
- Assume immediate message delivery (use latches to wait)
- Forget to handle disconnection scenarios
- Leave WebSocket sessions open (resource leaks)
- Test in isolation from server (protocol is inherently interactive)

---

## Next Steps

- [How-to: Connect to WebSocket Servers](../how-to/websockets-connection.md)
- [How-to: Handle WebSocket Errors](../how-to/websockets-error-handling.md)
- [How-to: Broadcast Messages](../how-to/websockets-broadcast.md)
- [Tutorial: gRPC Streaming](grpc-streaming.md)
- [Tutorial: Server-Sent Events](server-sent-events.md)
- [Reference: WebSocket API](../reference/websockets-reference.md)
