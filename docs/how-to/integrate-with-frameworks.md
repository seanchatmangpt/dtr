# How-to: Integrate with Frameworks

DocTester works with any test framework that can start a server before your tests run. The pattern is the same in all cases: create a base class that overrides `testServerUrl()` to return the actual server address.

---

## Arquillian / JBoss

Use `@RunAsClient` to run DocTests as client-side tests. Inject the server URL with `@ArquillianResource`:

```java
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.runner.RunWith;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;

import java.net.URISyntaxException;
import java.net.URL;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class ArquillianDocTester extends DocTester {

    @ArquillianResource
    private URL baseUrl;

    @Override
    public Url testServerUrl() {
        try {
            return Url.host(baseUrl.toURI().toString());
        } catch (URISyntaxException ex) {
            throw new IllegalStateException(
                "Invalid URI from Arquillian: " + baseUrl, ex);
        }
    }
}
```

Your DocTest classes then extend `ArquillianDocTester`:

```java
@RunWith(Arquillian.class)
public class UserApiDocTest extends ArquillianDocTester {

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "test.war")
            .addClasses(UserResource.class)
            .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
    }

    @Test
    public void testListUsers() {
        // ...
    }
}
```

---

## Ninja Framework

Start Ninja with a test rule and pass the port to the URL:

```java
import ninja.NinjaTest;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;

public abstract class NinjaDocTester extends DocTester {

    // Ninja assigns a random port; get it after server start
    protected int ninjaPort;

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:" + ninjaPort);
    }
}
```

Or use a `@Rule` with a `TestServer`:

```java
public abstract class NinjaApiDocTester extends DocTester {

    @Rule
    public NinjaTestServer ninjaTestServer = new NinjaTestServer();

    @Override
    public Url testServerUrl() {
        return Url.host(ninjaTestServer.getBaseUrl());
    }
}
```

---

## Spring Boot (with `@SpringBootTest`)

```java
import org.junit.runner.RunWith;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SpringDocTester extends DocTester {

    @LocalServerPort
    private int port;

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:" + port);
    }
}
```

DocTest classes extend `SpringDocTester`:

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserApiDocTest extends SpringDocTester {

    @Test
    public void testCreateUser() {
        // Uses the random port from @SpringBootTest
    }
}
```

---

## Embedded Jetty

Start Jetty in `@BeforeClass` and stop it in `@AfterClass`:

```java
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.r10r.doctester.DocTester;
import org.r10r.doctester.testbrowser.Url;

public abstract class JettyDocTester extends DocTester {

    private static Server server;
    private static int port;

    @BeforeClass
    public static void startJetty() throws Exception {
        server = new Server(0);  // 0 = random port
        ServletContextHandler context = new ServletContextHandler();
        // ... configure servlets ...
        server.setHandler(context);
        server.start();
        port = ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }

    @AfterClass
    public static void stopJetty() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:" + port);
    }
}
```

---

## Generic pattern

The pattern is always the same:

1. Start the server somewhere (rule, `@BeforeClass`, JUnit extension)
2. Capture the server URL/port
3. Create an abstract base class that overrides `testServerUrl()`
4. All DocTest classes extend this base class

```java
public abstract class MyFrameworkDocTester extends DocTester {

    @Override
    public Url testServerUrl() {
        return Url.host(MyFramework.getTestServerUrl());
    }
}
```

---

## Pointing at an existing server

For smoke tests against a running environment, hard-code or configure the URL:

```java
public abstract class StagingDocTester extends DocTester {

    @Override
    public Url testServerUrl() {
        String url = System.getenv()
            .getOrDefault("STAGING_URL", "https://staging.example.com");
        return Url.host(url);
    }
}
```
