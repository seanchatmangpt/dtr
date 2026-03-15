# ARCHIVED TUTORIAL

**This tutorial is from a previous version of DTR and is archived for reference.**

For current tutorials, see [docs/tutorials/](../../../tutorials/).

---

# Tutorial: Testing a REST API and Documenting It with DTR

In this tutorial you will call a real HTTP API using `java.net.http.HttpClient`, assert on the responses with AssertJ, and document each step using DTR `say*` methods. The result is a living API reference generated from your test run.

**Time:** ~45 minutes
**Prerequisites:** Complete [Your First DocTest](your-first-doctest.md) first
**Java:** 25+ with `--enable-preview`

---

## The API we'll document

We'll document the public [JSONPlaceholder](https://jsonplaceholder.typicode.com) API. It requires no authentication and is available in any environment.

| Method | Path | Description |
|---|---|---|
| `GET` | `/posts` | List all posts |
| `GET` | `/posts/{id}` | Get a single post |
| `POST` | `/posts` | Create a post |
| `PUT` | `/posts/{id}` | Replace a post |
| `DELETE` | `/posts/{id}` | Delete a post |

---

## Step 1 — Define your data records

Create `src/test/java/com/example/api/Post.java`:

```java
package com.example.api;

public record Post(Integer id, Integer userId, String title, String body) {}
```

Create `src/test/java/com/example/api/NewPost.java`:

```java
package com.example.api;

public record NewPost(Integer userId, String title, String body) {}
```

Records are ideal for REST DTOs: they are immutable, serialize to JSON cleanly, and require no annotations.

---

## Step 2 — Create a shared HTTP helper

Create `src/test/java/com/example/api/JsonHttp.java`:

```java
package com.example.api;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Thin wrapper around java.net.http.HttpClient for JSON REST calls.
 * DTR has no built-in HTTP client in v2.6.0 — use the JDK client directly.
 */
public class JsonHttp {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final HttpClient CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final String baseUrl;

    public JsonHttp(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public <T> T get(String path, Class<T> responseType) throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Accept", "application/json")
            .GET()
            .build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return MAPPER.readValue(response.body(), responseType);
    }

    public RawResponse getRaw(String path) throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Accept", "application/json")
            .GET()
            .build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new RawResponse(response.statusCode(), response.body());
    }

    public RawResponse postJson(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        var request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(json))
            .build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new RawResponse(response.statusCode(), response.body());
    }

    public RawResponse putJson(String path, Object body) throws Exception {
        String json = MAPPER.writeValueAsString(body);
        var request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .PUT(HttpRequest.BodyPublishers.ofString(json))
            .build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new RawResponse(response.statusCode(), response.body());
    }

    public RawResponse delete(String path) throws Exception {
        var request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + path))
            .DELETE()
            .build();
        var response = CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return new RawResponse(response.statusCode(), response.body());
    }

    public record RawResponse(int status, String body) {}
}
```

---

## Step 3 — Write the DocTest class

Create `src/test/java/com/example/PostsApiDocTest.java`:

```java
package com.example;

import com.example.api.JsonHttp;
import com.example.api.JsonHttp.RawResponse;
import com.example.api.NewPost;
import com.example.api.Post;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PostsApiDocTest {

    static final String BASE_URL = "https://jsonplaceholder.typicode.com";
    static JsonHttp http;
    static ObjectMapper mapper = new ObjectMapper();

    @BeforeAll
    static void setup() {
        http = new JsonHttp(BASE_URL);
    }
```

---

## Step 4 — Document listing resources

```java
    @Test
    @org.junit.jupiter.api.Order(1)
    void listPosts(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Listing Posts");

        ctx.say("GET /posts returns all posts as a JSON array. "
            + "No authentication is required for read operations.");

        ctx.sayCode("""
            GET https://jsonplaceholder.typicode.com/posts
            Accept: application/json
            """, "http");

        Post[] posts = http.get("/posts", Post[].class);

        ctx.say("The response is a JSON array. Each element has `id`, `userId`, "
            + "`title`, and `body` fields.");

        assertThat(posts).isNotEmpty();
        assertThat(posts[0].id()).isNotNull();

        ctx.sayAssertions(Map.of(
            "posts.length > 0", String.valueOf(posts.length > 0),
            "posts[0].id != null", String.valueOf(posts[0].id() != null)
        ));

        ctx.sayTable(new String[][] {
            {"id", "userId", "title (truncated)"},
            {String.valueOf(posts[0].id()), String.valueOf(posts[0].userId()),
             posts[0].title().substring(0, Math.min(40, posts[0].title().length()))},
            {String.valueOf(posts[1].id()), String.valueOf(posts[1].userId()),
             posts[1].title().substring(0, Math.min(40, posts[1].title().length()))}
        });

        ctx.say("Received " + posts.length + " posts total.");
    }
```

---

## Step 5 — Document retrieving a single resource

```java
    @Test
    @org.junit.jupiter.api.Order(2)
    void getPostById(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Retrieving a Single Post");

        ctx.say("GET /posts/{id} returns the post with the given ID. "
            + "The response body is a JSON object, not an array.");

        ctx.sayCode("""
            GET https://jsonplaceholder.typicode.com/posts/1
            Accept: application/json
            """, "http");

        Post post = http.get("/posts/1", Post.class);

        assertThat(post.id()).isEqualTo(1);
        assertThat(post.title()).isNotBlank();

        ctx.sayJson(post);

        ctx.sayAssertions(Map.of(
            "post.id()", String.valueOf(post.id()),
            "post.title() not blank", String.valueOf(!post.title().isBlank())
        ));
    }
```

---

## Step 6 — Document creating a resource

```java
    @Test
    @org.junit.jupiter.api.Order(3)
    void createPost(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Creating a Post");

        ctx.say("POST /posts with a JSON body creates a new post. "
            + "JSONPlaceholder simulates creation and echoes back the object with an assigned ID.");

        var newPost = new NewPost(1, "Getting Started with DTR 2.6.0",
            "DTR generates living documentation from JUnit 5 tests.");

        ctx.sayCode("""
            POST https://jsonplaceholder.typicode.com/posts
            Content-Type: application/json

            {
              "userId": 1,
              "title": "Getting Started with DTR 2.6.0",
              "body": "DTR generates living documentation from JUnit 5 tests."
            }
            """, "http");

        RawResponse response = http.postJson("/posts", newPost);

        assertThat(response.status()).isEqualTo(201);
        assertThat(response.body()).contains("id");

        Post created = mapper.readValue(response.body(), Post.class);

        ctx.say("The server responds with 201 Created and returns the new post, "
            + "including the assigned `id`:");

        ctx.sayJson(created);

        ctx.sayAssertions(Map.of(
            "HTTP status", String.valueOf(response.status()),
            "created.id() != null", String.valueOf(created.id() != null),
            "created.title()", created.title()
        ));
    }
```

---

## Step 7 — Document updating a resource

```java
    @Test
    @org.junit.jupiter.api.Order(4)
    void updatePost(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Updating a Post");

        ctx.say("PUT /posts/{id} replaces the entire post resource. "
            + "All fields must be included in the request body.");

        var updatedPost = new NewPost(1, "Updated Title for Post 1", "Revised body content.");

        ctx.sayCode("""
            PUT https://jsonplaceholder.typicode.com/posts/1
            Content-Type: application/json

            {
              "userId": 1,
              "title": "Updated Title for Post 1",
              "body": "Revised body content."
            }
            """, "http");

        RawResponse response = http.putJson("/posts/1", updatedPost);

        assertThat(response.status()).isEqualTo(200);

        Post updated = mapper.readValue(response.body(), Post.class);

        ctx.say("The server responds with 200 OK and returns the updated post:");

        ctx.sayJson(updated);

        ctx.sayAssertions(Map.of(
            "HTTP status", String.valueOf(response.status()),
            "updated.title()", updated.title()
        ));
    }
```

---

## Step 8 — Document deleting a resource

```java
    @Test
    @org.junit.jupiter.api.Order(5)
    void deletePost(DtrContext ctx) throws Exception {

        ctx.sayNextSection("Deleting a Post");

        ctx.say("DELETE /posts/{id} removes the post. "
            + "The server responds with 200 OK and an empty JSON body `{}`.");

        ctx.sayCode("""
            DELETE https://jsonplaceholder.typicode.com/posts/1
            """, "http");

        RawResponse response = http.delete("/posts/1");

        assertThat(response.status()).isEqualTo(200);

        ctx.sayAssertions(Map.of(
            "HTTP status", String.valueOf(response.status()),
            "body", response.body()
        ));

        ctx.sayNote("JSONPlaceholder simulates deletion but does not actually remove data. "
            + "On a real API, a subsequent GET /posts/1 would return 404.");
    }
}
```

---

## Step 9 — Run and review

```bash
mvnd test -Dtest=PostsApiDocTest
cat target/docs/test-results/PostsApiDocTest.md
```

The generated Markdown file contains five sections with:
- HTTP request examples as code blocks
- Real JSON responses captured at test run time
- Assertion tables showing what was validated
- Narrative prose linking the steps together

---

## Writing good narrative

The `say()` calls are prose aimed at API consumers. Write them like you are explaining to a colleague:

**Too terse:**
```java
ctx.say("POST /posts");
```

**Too mechanical:**
```java
ctx.say("This method sends an HTTP POST request with Content-Type: application/json "
    + "to the /posts endpoint and parses the 201 response body.");
```

**Just right:**
```java
ctx.say("To create a post, POST a JSON object with userId, title, and body fields. "
    + "The server assigns an id and returns the full post in the response body.");
```

Think of DTR output as the narrative section of a developer guide, with JSON payloads as embedded examples.

---

## What you learned

- How to use `java.net.http.HttpClient` (JDK built-in) for REST calls in DTR 2.6.0
- The pattern: make an HTTP call, assert with AssertJ, document with `say*`
- `sayJson(Object)` — pretty-prints any object as JSON in the docs
- `sayAssertions(Map)` — renders a table of assertion results
- `sayCode(String, String)` — inline HTTP request examples
- `sayNote(String)` — callout boxes for caveats

---

## Next steps

- [Tutorial: Records and Sealed Classes](records-sealed-classes.md) — model API responses with records
- [Tutorial: Benchmarking with sayBenchmark](virtual-threads-lightweight-concurrency.md) — measure endpoint latency
- [Tutorial: Visualizing Code with sayMermaid](websockets-realtime.md) — diagram your API architecture
