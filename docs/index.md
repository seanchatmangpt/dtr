# DocTester Documentation

DocTester is a Java testing framework that **generates HTML documentation while running JUnit tests**. Write your REST API tests once — get living documentation for free.

## The Core Idea

Traditional API documentation rots. Developers update code, forget to update docs, and users suffer. DocTester solves this by making tests and documentation the same thing: every test you write produces a readable HTML page that stays accurate because it is the test.

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

## Documentation Map

DocTester's documentation follows the [Diataxis](https://diataxis.fr/) framework:

| Section | Purpose | Start here if… |
|---|---|---|
| [Tutorials](tutorials/index.md) | Step-by-step learning | You're new to DocTester |
| [How-to Guides](how-to/index.md) | Task-focused recipes | You know what you want to do |
| [Reference](reference/index.md) | Complete API docs | You need to look something up |
| [Explanation](explanation/index.md) | Concepts & architecture | You want to understand *why* |
| [Contributing](contributing/index.md) | Developer experience | You want to develop DocTester itself |

---

## Quick Start

**1. Add to `pom.xml`:**

```xml
<dependency>
    <groupId>org.doctester</groupId>
    <artifactId>doctester-core</artifactId>
    <version>1.1.12</version>
    <scope>test</scope>
</dependency>
```

**2. Write a test:**

```java
public class ApiDocTest extends DocTester {

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
open target/site/doctester/index.html
```

---

## Requirements

- Java 25 (LTS)
- Maven 4 / Maven Daemon (mvnd 2)
- JUnit 4

See [Tutorials → Your First DocTest](tutorials/your-first-doctest.md) for a complete walkthrough.
