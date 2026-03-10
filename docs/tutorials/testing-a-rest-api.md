# Tutorial: Testing a REST API End-to-End

In this tutorial you will build a complete documented test suite for a CRUD REST API. You will cover authentication, JSON payloads, response deserialization, and assertions — producing a multi-section HTML documentation page that reads like a developer guide.

**Time:** ~45 minutes
**Prerequisites:** Complete [Your First DocTest](your-first-doctest.md) first

---

## The API we'll test

We'll document a hypothetical Articles API with these endpoints:

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/login` | Authenticate, receive session cookie |
| `GET` | `/api/articles` | List all articles |
| `POST` | `/api/articles` | Create an article |
| `GET` | `/api/articles/{id}` | Get a single article |
| `DELETE` | `/api/articles/{id}` | Delete an article |

This is the same structure as the DocTester integration test — adapt the URLs and DTOs to your own API.

---

## Step 1 — Create the DTO classes

Create simple Java records for the request/response bodies.

`src/test/java/com/example/dto/Article.java`:

```java
package com.example.dto;

public record Article(Long id, String title, String body, String author) {}
```

`src/test/java/com/example/dto/ArticleRequest.java`:

```java
package com.example.dto;

public record ArticleRequest(String title, String body, String author) {}
```

`src/test/java/com/example/dto/ArticleList.java`:

```java
package com.example.dto;

import java.util.List;

public record ArticleList(List<Article> articles) {}
```

---

## Step 2 — Create a base test class

When testing a real server, create a base class that sets the server URL once. This is the recommended pattern for projects with multiple DocTest classes.

`src/test/java/com/example/ApiDocTester.java`:

```java
package com.example;

import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;

public abstract class ApiDocTester extends DocTester {

    @Override
    public Url testServerUrl() {
        // In CI, read from a system property; fall back to localhost
        String host = System.getProperty("test.server.url", "http://localhost:8080");
        return Url.host(host);
    }
}
```

All your DocTest classes extend `ApiDocTester` instead of `DocTester` directly.

---

## Step 3 — Write the authentication section

`src/test/java/com/example/ArticlesApiDocTest.java`:

```java
package com.example;

import com.example.dto.Article;
import com.example.dto.ArticleList;
import com.example.dto.ArticleRequest;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)  // ensures test order
public class ArticlesApiDocTest extends ApiDocTester {

    // shared state across tests
    private static Long createdArticleId;

    @Test
    public void test01_authentication() {

        sayNextSection("Authentication");

        say("All write operations require authentication. "
            + "Authenticate via POST /api/login with form credentials. "
            + "The server sets a session cookie used in subsequent requests.");

        // Use makeRequest (not sayAndMakeRequest) — we don't want to document
        // internal test setup that isn't interesting to API consumers.
        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/login"))
                .addFormParameter("username", "admin")
                .addFormParameter("password", "secret"));

        sayAndAssertThat(
            "Login returns 200 OK",
            200,
            equalTo(response.httpStatus()));

        sayAndAssertThat(
            "Session cookie is set",
            getCookieWithName("SESSION"),
            notNullValue());

        say("The session cookie is stored automatically and sent with all "
            + "subsequent requests in this test. You do not need to manage it manually.");
    }
```

**Key point:** `makeRequest` (without `say`) runs the HTTP call silently. `sayAndMakeRequest` documents it. Use whichever fits the narrative you want to tell.

---

## Step 4 — Document listing resources

```java
    @Test
    public void test02_listArticles() {

        sayNextSection("Listing Articles");

        say("GET /api/articles returns all articles as a JSON array. "
            + "No authentication required for read operations.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/articles"))
                .contentTypeApplicationJson());

        sayAndAssertThat(
            "Response is 200 OK",
            200,
            equalTo(response.httpStatus()));

        ArticleList articleList = response.payloadAs(ArticleList.class);

        sayAndAssertThat(
            "Response contains articles",
            articleList.articles(),
            notNullValue());

        say("The response body is a JSON object with an `articles` array. "
            + "Each article contains `id`, `title`, `body`, and `author` fields.");
    }
```

`response.payloadAs(ArticleList.class)` automatically detects JSON or XML from the `Content-Type` header and deserializes the body.

---

## Step 5 — Document creating a resource

```java
    @Test
    public void test03_createArticle() {

        sayNextSection("Creating an Article");

        say("POST /api/articles with a JSON body creates a new article. "
            + "Requires an authenticated session.");

        var newArticle = new ArticleRequest(
            "Getting Started with DocTester",
            "DocTester makes API documentation effortless...",
            "Alice");

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/articles"))
                .contentTypeApplicationJson()
                .payload(newArticle));

        sayAndAssertThat(
            "Article created with 201 Created",
            201,
            equalTo(response.httpStatus()));

        Article created = response.payloadAs(Article.class);
        createdArticleId = created.id();

        sayAndAssertThat("Created article has an ID", created.id(), notNullValue());
        sayAndAssertThat(
            "Title matches what was sent",
            "Getting Started with DocTester",
            equalTo(created.title()));

        say("The `Location` header in the response contains the URL of the "
            + "newly created resource: `" + response.headers().get("Location") + "`");
    }
```

---

## Step 6 — Document retrieving a single resource

```java
    @Test
    public void test04_getArticleById() {

        sayNextSection("Retrieving a Single Article");

        say("GET /api/articles/{id} returns the article with the given ID.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/articles/" + createdArticleId)));

        sayAndAssertThat("Article found", 200, equalTo(response.httpStatus()));

        Article article = response.payloadAs(Article.class);

        sayAndAssertThat(
            "Article ID matches",
            createdArticleId,
            equalTo(article.id()));

        sayAndAssertThat(
            "Title is correct",
            "Getting Started with DocTester",
            equalTo(article.title()));
    }
```

---

## Step 7 — Document deleting a resource

```java
    @Test
    public void test05_deleteArticle() {

        sayNextSection("Deleting an Article");

        say("DELETE /api/articles/{id} permanently removes an article. "
            + "Returns 204 No Content on success. Requires authentication.");

        Response deleteResponse = sayAndMakeRequest(
            Request.DELETE()
                .url(testServerUrl().path("/api/articles/" + createdArticleId)));

        sayAndAssertThat(
            "Article deleted — 204 No Content",
            204,
            equalTo(deleteResponse.httpStatus()));

        say("After deletion, attempting to GET the same article returns 404:");

        Response getResponse = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path("/api/articles/" + createdArticleId)));

        sayAndAssertThat(
            "Deleted article is gone — 404 Not Found",
            404,
            equalTo(getResponse.httpStatus()));
    }
}
```

---

## Step 8 — Run and review

```bash
mvnd test -Dtest=ArticlesApiDocTest
open target/site/doctester/com.example.ArticlesApiDocTest.html
```

The generated page now has five sections with full request/response documentation for each, plus your narrative text woven throughout.

---

## Writing good narrative

The `say()` calls are prose aimed at API consumers. Write them like you're explaining to a colleague:

**Too terse:**
```java
say("POST /api/articles");
```

**Too technical:**
```java
say("This method sends an HTTP POST request with Content-Type: application/json " +
    "to the /api/articles endpoint and parses the 201 response body.");
```

**Just right:**
```java
say("To create an article, POST a JSON object with `title`, `body`, and `author` " +
    "fields. The server assigns an ID and returns the full article in the response body.");
```

Think of DocTester output as the narrative section of a developer guide, with the request/response panels as embedded examples.

---

## What you learned

- How to create a shared base class with `testServerUrl()`
- Using `@FixMethodOrder` to control test sequence for scenario-based tests
- When to use `sayAndMakeRequest` vs `makeRequest` (documented vs undocumented)
- Deserializing JSON responses with `response.payloadAs(MyClass.class)`
- Sharing state between tests with static fields
- Writing good narrative text with `say()`

---

## Next steps

- [How-to: Integrate with Frameworks](../how-to/integrate-with-frameworks.md) — Ninja, Arquillian, Spring Boot, Jetty
- [How-to: Test XML Endpoints](../how-to/test-xml-endpoints.md) — XML payloads and deserialization
- [How-to: Upload Files](../how-to/upload-files.md) — multipart form data
- [Explanation: How DocTester Works](../explanation/how-doctester-works.md) — the lifecycle in detail
