# FAQ & Troubleshooting Guide

Common issues when using DTR, solutions, and diagnostic commands.

## Installation & Setup

### Q: Maven says "could not find artifact"
**Error:** `The requested required projects <module> do not exist`

**Solution:**
1. Ensure you've added the correct dependency:
   ```xml
   <dependency>
       <groupId>io.github.seanchatmangpt.dtr</groupId>
       <artifactId>dtr-core</artifactId>
       <version>2.5.0</version>
       <scope>test</scope>
   </dependency>
   ```

2. Clear Maven cache and rebuild:
   ```bash
   mvnd clean install -U
   ```

3. If Maven Central download fails, start the proxy and retry:
   ```bash
   python3 maven-proxy-auth.py &
   mvnd clean install -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
   ```

**See also:** [Add DTR to Maven](../how-to/add-to-maven.md)

---

### Q: "too many authentication attempts" from Maven Central
**Error:** `Failed to authenticate with the remote repository at central`

**Solution:**
Maven Central rate-limits authentication attempts. Start the proxy server:

```bash
# 1. Start proxy
python3 maven-proxy-auth.py &

# 2. Add to .mvn/jvm.config
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1

# 3. Rebuild
mvnd clean install
```

The proxy handles HTTPS CONNECT tunneling and manages authentication automatically.

**Troubleshooting the proxy:**
```bash
# Restart proxy if still failing
pkill -f maven-proxy
python3 maven-proxy-auth.py &

# Verify proxy is running
ps aux | grep maven-proxy

# Check environment variable
echo $https_proxy
```

---

### Q: Java version error
**Error:** `Unsupported class version 63.0` or `Java 25 is required`

**Solution:**
DTR requires Java 25+. Check your Java version:
```bash
java -version
```

If you have multiple Java versions, set JAVA_HOME:
```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
java -version  # Verify
mvnd clean install
```

---

### Q: "Preview features disabled" error
**Error:** `error: java.lang.IllegalAccessError: class X (in unnamed module) cannot access class Y` or similar preview errors

**Solution:**
DTR uses Java 25 preview features. Enable them:

1. Add to `.mvn/maven.config`:
   ```
   --enable-preview
   ```

2. Also add to `pom.xml` compiler and surefire plugins:
   ```xml
   <build>
       <plugins>
           <plugin>
               <groupId>org.apache.maven.plugins</groupId>
               <artifactId>maven-compiler-plugin</artifactId>
               <configuration>
                   <release>25</release>
                   <compilerArgs>
                       <arg>--enable-preview</arg>
                   </compilerArgs>
               </configuration>
           </plugin>
           <plugin>
               <groupId>org.apache.maven.plugins</groupId>
               <artifactId>maven-surefire-plugin</artifactId>
               <configuration>
                   <argLine>--enable-preview</argLine>
               </configuration>
           </plugin>
       </plugins>
   </build>
   ```

3. Rebuild:
   ```bash
   mvnd clean install
   ```

---

## Test Execution

### Q: Tests compile but don't run
**Error:** Test class not found, or `@Test` annotation not recognized

**Solution:**
Ensure you're using the correct test framework. DTR supports both JUnit 4 and JUnit 5:

**JUnit 4:**
```java
import org.junit.Test;

public class MyDocTest {
    @Test
    public void testExample() {
        // test code
    }
}
```

**JUnit 5 (recommended):**
```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DTRExtension.class)
public class MyDocTest {
    @Test
    void testExample(DTRContext ctx) {
        // test code
    }
}
```

See [Your First DocTest](../tutorials/your-first-doctest.md) for full setup.

---

### Q: Test runs but finds no documentation output
**Error:** `target/docs/` directory is empty or missing

**Solution:**
Check that:
1. Tests are calling `say*()` methods (they generate documentation)
2. Tests are running successfully (failed tests may not generate docs)
3. Output directory is correct:
   ```bash
   ls -la target/docs/test-results/
   ```

If still empty, run a test explicitly:
```bash
mvnd test -Dtest=YourTestClass -v
cat target/docs/test-results/YourTestClass.md  # Should exist now
```

---

### Q: HTTP request times out
**Error:** `java.net.SocketTimeoutException` or `Connection refused`

**Solution:**
1. Verify the test server is running:
   ```bash
   curl http://localhost:8080/api/health  # Adjust URL/port
   ```

2. If server is running, increase timeout:
   ```java
   Response response = sayAndMakeRequest(
       Request.GET()
           .url(testServerUrl().path("/api/users"))
           .socketTimeout(30000)  // 30 seconds
   );
   ```

3. Check firewall/network:
   ```bash
   nc -zv localhost 8080  # Test connectivity
   ```

---

### Q: JSON parsing fails in tests
**Error:** `JsonProcessingException` or `Cannot deserialize`

**Solution:**
1. Verify response is actually JSON:
   ```java
   Response response = sayAndMakeRequest(Request.GET().url(testServerUrl().path("/api/users")));
   say("Response body: " + response.getBody());  // Debug output
   say("Content-Type: " + response.getContentType());  // Check header
   ```

2. Ensure class matches response structure:
   ```java
   // If response is {"id": 1, "name": "Alice"}
   record User(int id, String name) {}
   User user = response.payloadAs(User.class);
   ```

3. If response contains nested objects, create matching records:
   ```java
   record Address(String street, String city) {}
   record User(int id, String name, Address address) {}
   ```

---

## Output & Rendering

### Q: HTML output doesn't look right
**Error:** Missing CSS, broken layout, or poor formatting

**Solution:**
1. Check the HTML file exists:
   ```bash
   ls -la target/site/dtr/*.html
   ```

2. Open directly in browser:
   ```bash
   open target/site/dtr/YourTest.html  # macOS
   xdg-open target/site/dtr/YourTest.html  # Linux
   ```

3. If assets are missing, rebuild with full output:
   ```bash
   mvnd clean site
   ```

See [Customize HTML Output](../how-to/customize-html-output.md) for styling options.

---

### Q: LaTeX/PDF generation fails
**Error:** `pdflatex not found` or LaTeX compilation error

**Solution:**
1. Ensure LaTeX is installed:
   ```bash
   pdflatex --version
   ```

2. If not installed:
   ```bash
   # Ubuntu/Debian
   sudo apt-get install texlive-latex-base texlive-fonts-recommended

   # macOS
   brew cask install mactex
   ```

3. Rebuild with LaTeX output:
   ```bash
   mvnd clean test
   ls -la target/docs/test-results/*.tex
   ls -la target/docs/test-results/*.pdf
   ```

See [Advanced Rendering Formats](../how-to/advanced-rendering-formats.md) for detailed LaTeX configuration.

---

### Q: Blog export doesn't work
**Error:** Blog files not generated or incorrect format

**Solution:**
1. Check that `say*()` methods are being called (blog needs content)
2. Verify output location:
   ```bash
   ls -la target/blog/
   ```

3. Ensure Maven plugin is configured for blog rendering

See [Advanced Rendering Formats](../how-to/advanced-rendering-formats.md) for blog export configuration.

---

## Real-Time Protocols

### Q: WebSocket connection fails
**Error:** `WebSocket handshake failed` or connection timeout

**Solution:**
1. Verify WebSocket server is running:
   ```bash
   curl http://localhost:8080/health  # Check HTTP health first
   ```

2. Check WebSocket URL is correct:
   ```java
   // Should be ws://, not http://
   sayAndConnectWebSocket(
       WebSocket.connect()
           .url("ws://localhost:8080/api/stream")
   );
   ```

3. Increase connection timeout:
   ```java
   sayAndConnectWebSocket(
       WebSocket.connect()
           .url("ws://localhost:8080/api/stream")
           .connectTimeout(10000)  // 10 seconds
   );
   ```

See [Testing WebSockets](../how-to/websockets-connection.md) for full WebSocket testing guide.

---

### Q: gRPC tests fail with "UNAVAILABLE" status
**Error:** `io.grpc.StatusRuntimeException: UNAVAILABLE`

**Solution:**
1. Verify gRPC server is running on correct port:
   ```bash
   netstat -tlnp | grep 50051  # Default gRPC port
   ```

2. Check endpoint is correct:
   ```java
   Channel channel = ManagedChannelBuilder
       .forAddress("localhost", 50051)
       .usePlaintext()
       .build();
   ```

3. Ensure proto definitions match service:
   ```bash
   protoc --version  # Check protobuf compiler
   mvnd clean compile  # Regenerate stubs
   ```

See [Testing gRPC APIs](../how-to/grpc-unary.md) for complete gRPC setup.

---

### Q: SSE connection closes unexpectedly
**Error:** Connection drops before expected messages received

**Solution:**
1. Check server-side logs for errors
2. Increase reconnection timeout:
   ```java
   ServerSentEvent.subscribe()
       .url(testServerUrl().path("/api/events"))
       .timeout(30000)  // 30 seconds
   ```

3. Verify server sends keep-alive comments:
   ```
   : keep-alive
   ```

See [Testing Server-Sent Events](../how-to/sse-subscription.md) for SSE troubleshooting.

---

## Build & Performance

### Q: Build is slow
**Symptoms:** `mvnd clean install` takes a long time

**Solution:**
1. Use mvnd daemon (much faster than plain mvn):
   ```bash
   mvnd clean install  # Reuses daemon
   ```

2. Skip documentation generation during development:
   ```bash
   mvnd -DskipTests=false -DskipDocs=true clean test
   ```

3. Build only changed modules:
   ```bash
   mvnd clean install -pl dtr-core  # Only dtr-core
   ```

4. Use parallel builds:
   ```bash
   mvnd -T 1C clean install  # 1 thread per core
   ```

See [Benchmarking](../how-to/benchmarking.md) for performance measurement guidance.

---

### Q: "out of memory" during build
**Error:** `java.lang.OutOfMemoryError: Java heap space`

**Solution:**
Increase Maven's memory:

```bash
export MAVEN_OPTS="-Xmx2g"  # 2GB
mvnd clean install
```

Or add to `~/.m2/jvm.config`:
```
-Xmx2g
-XX:+UseG1GC
```

---

## Daemon Issues

### Q: Maven daemon becomes unresponsive
**Error:** Builds hang or timeout, despite no actual errors

**Solution:**
Stop and restart the daemon:

```bash
# Stop all daemon processes
mvnd --stop

# Verify they're gone
ps aux | grep mvnd

# Rebuild
mvnd clean install
```

Or restart more aggressively:
```bash
pkill -9 mvnd
mvnd clean install
```

---

### Q: Daemon uses too much memory
**Solution:**
Monitor and limit daemon memory:

```bash
# See running daemons
mvnd --list

# Stop all daemons
mvnd --stop

# Rebuild with lower memory if needed
export MAVEN_OPTS="-Xmx1g"
mvnd clean install
```

---

## Debugging

### Q: How do I debug a failing test?
**Solution:**
1. Run with verbose output:
   ```bash
   mvnd test -pl dtr-integration-test -Dtest=YourTest -v
   ```

2. Run with extra debug output:
   ```bash
   mvnd test -pl dtr-integration-test -Dtest=YourTest -X
   ```

3. Check test reports:
   ```bash
   cat target/surefire-reports/YourTest.txt
   cat target/surefire-reports/YourTest-output.txt
   ```

4. Add debug output in your test:
   ```java
   say("DEBUG: Response status = " + response.httpStatus());
   say("DEBUG: Response body = " + response.getBody());
   ```

---

### Q: How do I verify Java/Maven/mvnd versions?
**Solution:**
```bash
java -version          # Should be 25+
mvnd --version         # Should be 2.0.0+
mvn --version          # Should be 4.0.0-rc-5+
echo $JAVA_HOME        # Verify path
```

---

## Advanced Topics

### Q: How do I generate OpenAPI documentation?
See [Advanced Rendering Formats](../how-to/advanced-rendering-formats.md) for OpenAPI configuration.

---

### Q: How do I benchmark DTR performance?
See [Benchmarking](../how-to/benchmarking.md) for performance measurement with JMH.

---

### Q: How do I integrate DTR with Spring Boot / Quarkus?
See [Integrate with Frameworks](../how-to/integrate-with-frameworks.md) for framework-specific setup.

---

## Getting Help

- Check the relevant tutorial: [Tutorials](../tutorials/index.md)
- Search the how-to guides: [How-To Index](../how-to/index.md)
- Read the API reference: [Reference Docs](../reference/index.md)
- Review the architecture: [How DTR Works](../explanation/how-dtr-works.md)
- Report issues: GitHub issues (if available in your version)
