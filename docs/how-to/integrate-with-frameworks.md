# How-to: Integrate DTR with Frameworks

DTR 2.6.0 uses JUnit 5's `@ExtendWith(DtrExtension.class)` and works with any framework that can start a server or provide test infrastructure via JUnit 5 extensions.

**DTR Version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

---

## The Pattern

1. Start your server or test infrastructure (using the framework's JUnit 5 integration)
2. Obtain the server URL or connection details
3. Write a `@Test` method that accepts `DtrContext ctx`
4. Use `ctx.say*()` for documentation, `java.net.http.HttpClient` for HTTP calls, `assertThat(...)` for assertions

```java
@ExtendWith(DtrExtension.class)
class MyFrameworkDocTest {

    @Test
    void testEndpoint(DtrContext ctx) throws Exception {
        ctx.sayNextSection("My Endpoint");

        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(serverUrl()))
            .GET()
            .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.sayJson(response.body());
    }

    private String serverUrl() {
        return "http://localhost:8080";
    }
}
```

---

## Spring Boot (with @SpringBootTest)

Use `@SpringBootTest` with `RANDOM_PORT` and inject the port:

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(DtrExtension.class)
class SpringUserApiDocTest {

    @LocalServerPort
    private int port;

    @Test
    void documentListUsers(DtrContext ctx) throws Exception {
        ctx.sayNextSection("List Users");
        ctx.sayEnvProfile();
        ctx.say("GET /api/users returns all active users.");

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/users"))
            .GET()
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.sayJson(response.body());
    }
}
```

---

## Arquillian / JBoss

Use `@RunAsClient` and `@ArquillianResource` with JUnit 5 (via `arquillian-junit5-container`):

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit5.ArquillianExtension;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import java.net.URL;

@ExtendWith({ArquillianExtension.class, DtrExtension.class})
@RunAsClient
class ArquillianDocTest {

    @ArquillianResource
    private URL baseUrl;

    @Test
    void documentDeployedApi(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Deployed API Test");
        ctx.say("Testing deployed application at: " + baseUrl);

        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
            .uri(baseUrl.toURI().resolve("/api/users"))
            .GET()
            .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.sayJson(response.body());
    }
}
```

---

## Embedded Jetty

Start Jetty in `@BeforeAll` and stop it in `@AfterAll`:

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class JettyDocTest {

    private static Server server;
    private static int port;

    @BeforeAll
    static void startJetty() throws Exception {
        server = new Server(0);
        var context = new ServletContextHandler();
        context.addServlet(MyApiServlet.class, "/api/*");
        server.setHandler(context);
        server.start();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @AfterAll
    static void stopJetty() throws Exception {
        if (server != null) server.stop();
    }

    @Test
    void documentApi(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Embedded Jetty API");
        ctx.say("Server running on port " + port);

        var client = HttpClient.newHttpClient();
        var request = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + "/api/status"))
            .GET()
            .build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.sayJson(response.body());
    }
}
```

---

## Ninja Framework

Start a Ninja test server and inject the port:

```java
import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import ninja.NinjaTest;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;

@ExtendWith(DtrExtension.class)
class NinjaDocTest extends NinjaTest {

    @Test
    void documentNinjaApi(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Ninja Framework API");
        ctx.say("Testing Ninja application at port " + ninjaTestServer.getPort());

        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(ninjaTestServer.getBaseUrl() + "/api/users"))
            .GET()
            .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.sayJson(response.body());
    }
}
```

---

## Point at an Existing Server

For smoke tests against staging or production:

```java
@ExtendWith(DtrExtension.class)
class StagingDocTest {

    private String stagingUrl() {
        return System.getenv().getOrDefault("STAGING_URL", "https://staging.example.com");
    }

    @Test
    void smokTestStagingApi(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Staging API Smoke Test");
        ctx.sayEnvProfile();
        ctx.say("Target: " + stagingUrl());

        var client = java.net.http.HttpClient.newHttpClient();
        var request = java.net.http.HttpRequest.newBuilder()
            .uri(java.net.URI.create(stagingUrl() + "/api/health"))
            .GET()
            .build();
        var response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        ctx.sayJson(response.body());
    }
}
```

---

## See Also

- [Add DTR to Maven](add-to-maven.md) — Dependency and compiler configuration
- [Control What Gets Documented](control-documentation.md) — Conditional documentation patterns
- [Document JSON Payloads](test-json-endpoints.md) — HTTP + sayJson patterns
