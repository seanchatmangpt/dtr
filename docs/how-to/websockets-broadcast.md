# How-to: Broadcast Messages to Multiple Clients

Test WebSocket servers that broadcast messages to all connected clients.

---

## Basic Broadcast Pattern

```java
// Connect multiple clients
WebSocketContainer container = ContainerProvider.getWebSocketContainer();

ClientEndpoint client1 = new BroadcastEndpoint("client1");
ClientEndpoint client2 = new BroadcastEndpoint("client2");
ClientEndpoint client3 = new BroadcastEndpoint("client3");

Session s1 = container.connectToServer(client1, URI.create("ws://localhost:8080/chat"));
Session s2 = container.connectToServer(client2, URI.create("ws://localhost:8080/chat"));
Session s3 = container.connectToServer(client3, URI.create("ws://localhost:8080/chat"));

// One client sends
s1.getBasicRemote().sendText("Hello from client1");

// All should receive
Thread.sleep(500);

boolean allReceived = client1.receivedMessages.size() > 0 &&
                      client2.receivedMessages.size() > 0 &&
                      client3.receivedMessages.size() > 0;

assert allReceived : "Broadcast message not received by all clients";
```

---

## Collect Broadcast Messages

```java
@ClientEndpoint
public class BroadcastEndpoint {
    private String name;
    List<String> receivedMessages = Collections.synchronizedList(new ArrayList<>());

    public BroadcastEndpoint(String name) {
        this.name = name;
    }

    @OnMessage
    public void onMessage(String message) {
        receivedMessages.add(message);
        System.out.println(name + " received: " + message);
    }

    public int getMessageCount() {
        return receivedMessages.size();
    }

    public List<String> getAllMessages() {
        return new ArrayList<>(receivedMessages);
    }
}
```

---

## Verify Broadcast to Specific Group

```java
// Connect users in different rooms
Session room1User1 = container.connectToServer(
    new BroadcastEndpoint("room1:user1"),
    URI.create("ws://localhost:8080/chat?room=room1"));

Session room1User2 = container.connectToServer(
    new BroadcastEndpoint("room1:user2"),
    URI.create("ws://localhost:8080/chat?room=room1"));

Session room2User1 = container.connectToServer(
    new BroadcastEndpoint("room2:user1"),
    URI.create("ws://localhost:8080/chat?room=room2"));

// User in room1 sends message
room1User1.getBasicRemote().sendText("Message for room 1");

Thread.sleep(500);

// Only room1 users should receive (not room2)
assert room1User1.receivedMessages.contains("Message for room 1");
assert room1User2.receivedMessages.contains("Message for room 1");
assert !room2User1.receivedMessages.contains("Message for room 1");
```

---

## Test Echo + Broadcast

```java
// Some servers echo messages back to sender AND broadcast to others
Session sender = container.connectToServer(
    new EchoEndpoint(),
    URI.create("ws://localhost:8080/chat"));

Session receiver = container.connectToServer(
    new EchoEndpoint(),
    URI.create("ws://localhost:8080/chat"));

// Send message
sender.getBasicRemote().sendText("broadcast test");

Thread.sleep(500);

// Sender should receive their own message (echo)
assert sender.getReceivedMessages().contains("broadcast test");

// Receiver should receive broadcasted message
assert receiver.getReceivedMessages().contains("broadcast test");
```

---

## Broadcast with JSON Events

```java
@ClientEndpoint
public class JsonBroadcastEndpoint {
    List<EventMessage> receivedEvents = Collections.synchronizedList(new ArrayList<>());

    @OnMessage
    public void onMessage(String jsonMessage) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            EventMessage event = mapper.readValue(jsonMessage, EventMessage.class);
            receivedEvents.add(event);
        } catch (Exception e) {
            System.err.println("Failed to parse event: " + e.getMessage());
        }
    }

    public List<EventMessage> getEvents() {
        return new ArrayList<>(receivedEvents);
    }

    public record EventMessage(String type, String content, String sender) {}
}

// Test
String eventJson = """
    {
        "type": "userMessage",
        "content": "Hello everyone!",
        "sender": "alice"
    }
    """;

session.getBasicRemote().sendText(eventJson);

// Verify broadcast
List<EventMessage> events = endpoint.getEvents();
assert events.stream()
    .anyMatch(e -> "userMessage".equals(e.type) && e.sender.equals("alice"));
```

---

## Concurrency: Test Many Broadcasters

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    List<Session> sessions = Collections.synchronizedList(new ArrayList<>());

    // Connect 50 clients
    for (int i = 0; i < 50; i++) {
        final int clientId = i;
        executor.submit(() -> {
            try {
                Session session = container.connectToServer(
                    new BroadcastEndpoint("client" + clientId),
                    URI.create("ws://localhost:8080/chat"));
                sessions.add(session);
            } catch (Exception e) {
                // Handle error
            }
        });
    }

    Thread.sleep(1000); // Let all connect

    // First client broadcasts
    if (!sessions.isEmpty()) {
        sessions.get(0).getBasicRemote().sendText("Message from client 0");
    }

    Thread.sleep(500);

    // Verify all (or most) received the message
    long receiversCount = sessions.stream()
        .filter(s -> s.isOpen())
        .count();

    System.out.println("Message received by " + receiversCount + " active clients");
}
```

---

## Best Practices

✅ **DO:**
- Connect multiple clients to same endpoint
- Use `Collections.synchronizedList()` to collect messages
- Wait for network latency (use Thread.sleep or CountDownLatch)
- Test group broadcasts separately

❌ **DON'T:**
- Assume immediate delivery (use delays)
- Mix broadcast and direct messaging without clarity
- Forget to close all sessions

---

## See Also

- [How-to: Connect to WebSocket Servers](websockets-connection.md)
- [How-to: Handle WebSocket Errors](websockets-error-handling.md)
- [Tutorial: WebSockets](../tutorials/websockets-realtime.md)
