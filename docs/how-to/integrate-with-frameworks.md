# How-to: Integrate with Frameworks

DTR works with any test framework that can start a server before your tests run. The pattern is the same in all cases: create a base class that overrides `testServerUrl()` to return the actual server address.

---

## Arquillian / JBoss

Use `@RunAsClient` to run DocTests as client-side tests. Inject the server URL with `@ArquillianResource`:

```java
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.runner.RunWith;
import io.github.seanchatmangpt.dtr.dtr.DTR;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Url;

import java.net.URISyntaxException;
import java.net.URL;

@RunWith(Arquillian.class)
@RunAsClient
public abstract class ArquillianDTR extends DTR {

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

Your DocTest classes then extend `ArquillianDTR`:

```java
@RunWith(Arquillian.class)
public class UserApiDocTest extends ArquillianDTR {

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
import io.github.seanchatmangpt.dtr.dtr.DTR;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Url;

public abstract class NinjaDTR extends DTR {

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
public abstract class NinjaApiDTR extends DTR {

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
import io.github.seanchatmangpt.dtr.dtr.DTR;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Url;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class SpringDTR extends DTR {

    @LocalServerPort
    private int port;

    @Override
    public Url testServerUrl() {
        return Url.host("http://localhost:" + port);
    }
}
```

DocTest classes extend `SpringDTR`:

```java
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserApiDocTest extends SpringDTR {

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
import io.github.seanchatmangpt.dtr.dtr.DTR;
import io.github.seanchatmangpt.dtr.dtr.testbrowser.Url;

public abstract class JettyDTR extends DTR {

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
public abstract class MyFrameworkDTR extends DTR {

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
public abstract class StagingDTR extends DTR {

    @Override
    public Url testServerUrl() {
        String url = System.getenv()
            .getOrDefault("STAGING_URL", "https://staging.example.com");
        return Url.host(url);
    }
}
```
