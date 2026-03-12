# DTR Documentation

DTR is a Java testing framework that **generates HTML documentation while running JUnit tests**. Write your REST API tests once — get living documentation for free.

## The Core Idea

Traditional API documentation rots. Developers update code, forget to update docs, and users suffer. DTR solves this by making tests and documentation the same thing: every test you write produces a readable HTML page that stays accurate because it is the test.

```java
@Test
public void testGetUsers() {
    sayNextSection("User API");
    say("GET /api/users returns all active users.");

    Response response = sayAndMakeRequest(
        Request.GET().url(testServerUrl().path("/api/users")));

    Users users = response.payloadAs(Users.class);
    sayAndAssertThat("Response contains 3 users", 3, equalTo(users.size()));
}
```

This test runs normally with JUnit **and** generates a polished Bootstrap HTML page showing the request, response, and assertions.

---

## ⚡ Quick Start: The 80/20 Path (45 minutes)

New to DTR? Start here and get productive fast:

1. **[80/20 Essentials](how-to/80-20-essentials.md)** — Master 3 methods that do 80% of the work
2. **[Quick Reference](reference/80-20-quick-reference.md)** — One-page cheat sheet (bookmark it!)
3. **[Design Patterns](explanation/80-20-design-patterns.md)** — Understand the philosophy

These docs will take you from zero to writing working tests in under an hour. Then dive deeper into the full documentation as needed.

---

## Documentation Map

DTR's documentation follows the [Diataxis](https://diataxis.fr/) framework and is organized by topic:

### By Topic

| Topic | Tutorials | How-to | Reference | Explanation |
|---|---|---|---|---|
| **Java 25 Features** | [Virtual Threads](tutorials/virtual-threads-lightweight-concurrency.md), [Records & Sealed](tutorials/records-sealed-classes.md) | [Use Virtual Threads](how-to/use-virtual-threads.md), [Pattern Matching](how-to/pattern-matching.md), [Text Blocks](how-to/text-blocks.md), [Switch](how-to/switch-expressions.md) | [Virtual Threads API](reference/virtual-threads-reference.md), [Records & Sealed](reference/records-sealed-reference.md) | [Design Philosophy](explanation/java25-design-philosophy.md), [Virtual Threads](explanation/virtual-threads-philosophy.md), [Records & Sealed](explanation/records-sealed-philosophy.md) |
| **Real-Time Protocols** | [WebSockets](tutorials/websockets-realtime.md), [gRPC](tutorials/grpc-streaming.md), [SSE](tutorials/server-sent-events.md) | [WS Connection](how-to/websockets-connection.md), [gRPC Unary](how-to/grpc-unary.md), [gRPC Streaming](how-to/grpc-streaming.md), [SSE Subscribe](how-to/sse-subscription.md) | [Protocols Reference](reference/realtime-protocols-reference.md) | [Protocol Philosophy](explanation/realtime-protocols-philosophy.md) |
| **REST API Testing** | [Your First DocTest](tutorials/your-first-doctest.md), [REST API](tutorials/testing-a-rest-api.md) | [JSON](how-to/test-json-endpoints.md), [XML](how-to/test-xml-endpoints.md), [Cookies](how-to/use-cookies.md), [Files](how-to/upload-files.md), [Query Params](how-to/test-with-query-parameters.md), [Headers](how-to/use-custom-headers.md) | [DTR](reference/dtr-base-class.md), [Request](reference/request-api.md), [Response](reference/response-api.md) | [How it Works](explanation/how-dtr-works.md), [Philosophy](explanation/documentation-philosophy.md), [Architecture](explanation/architecture.md) |

### By Learning Mode

| Section | Purpose | Start here if… |
|---|---|---|
| [Tutorials](tutorials/index.md) | Step-by-step learning | You're new to Java 25 or DTR |
| [How-to Guides](how-to/index.md) | Task-focused recipes | You know what you want to do |
| [Reference](reference/index.md) | Complete API docs | You need to look something up |
| [Explanation](explanation/index.md) | Concepts & design | You want to understand *why* |
| [Contributing](contributing/index.md) | Developer experience | You want to develop DTR itself |

---

## Quick Start

**1. Add to `pom.xml`:**

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.5.0</version>
    <scope>test</scope>
</dependency>
```

**2. Write a test:**

```java
public class ApiDocTest extends DTR {

    @Test
    public void testCreateUser() {
        sayNextSection("Creating a User");
        say("POST /api/users creates a new user and returns 201.");

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/users"))
                .contentTypeApplicationJson()
                .payload(new User("alice")));

        sayAndAssertThat("Status is 201 Created", 201, equalTo(response.httpStatus()));
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:8080");
    }
}
```

**3. Run tests and view output:**

```bash
mvnd test
open target/site/dtr/index.html
```

---

## Requirements

- Java 25 (LTS)
- Maven 4 / Maven Daemon (mvnd 2)
- JUnit 4

See [Tutorials → Your First DocTest](tutorials/your-first-doctest.md) for a complete walkthrough.
