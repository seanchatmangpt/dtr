# Reference: Configuration

**Version:** 2026.3.0

---

## Maven dependency

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2026.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## Output directory

DTR writes all output to:

```
target/docs/test-results/
```

This path is the default in `RenderMachineImpl`. To change it, supply a custom `RenderMachine` (see [RenderMachine API](rendermachine-api.md)).

---

## Output files

| File | Description |
|------|-------------|
| `{ClassName}.md` | Markdown documentation page |
| `{ClassName}.html` | HTML documentation page |
| `{ClassName}.tex` | LaTeX source |
| `{ClassName}.json` | Structured JSON representation |
| `{ClassName}.blog.md` | Blog-formatted Markdown |
| `{ClassName}.slides.md` | Slide deck Markdown |

---

## Compiler settings

Required Maven compiler configuration for Java 26 with preview features:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.14.0</version>
    <configuration>
        <release>26</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
        <enablePreview>true</enablePreview>
    </configuration>
</plugin>
```

Required Surefire configuration to pass `--enable-preview` to the test JVM:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
    <configuration>
        <argLine>
            --enable-preview
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
        </argLine>
        <forkCount>1</forkCount>
        <reuseForks>false</reuseForks>
    </configuration>
</plugin>
```

Or add to `.mvn/maven.config` (applies to all Maven invocations):

```
--enable-preview
```

---

## Build system integration

DTR generates documentation as a side effect of running tests. No additional Maven plugin is required.

```bash
# Run all tests and generate documentation
mvnd test

# Run a specific test class
mvnd test -pl dtr-integration-test -Dtest=MyDocTest

# Run as part of full build
mvnd verify

# View output
ls target/docs/test-results/
cat target/docs/test-results/MyDocTest.md
```

---

## System properties

| Property | Default | Description |
|----------|---------|-------------|
| `dtr.output.dir` | `docs/test` | Base output directory |
| `dtr.format` | `markdown` | Output format: `markdown`, `latex`, `blog`, `slides` |
| `dtr.output.formats` | `md,html,tex,json` | Comma-separated list of output formats to generate |
| `dtr.benchmark.warmup` | `100` | Default warmup iterations for `sayBenchmark` |
| `dtr.benchmark.iterations` | `1000` | Default measured iterations for `sayBenchmark` |
| `dtr.mermaid.cli` | (auto-detected) | Path to `mmdc` (Mermaid CLI) for LaTeX diagram export |
| `dtr.javadoc.skip` | `false` | Skip Rust-based Javadoc extraction step |

Set system properties in Maven:

```bash
mvnd test -Ddtr.output.dir=docs/api -Ddtr.format=markdown
```

Or in `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--enable-preview</argLine>
        <systemPropertyVariables>
            <dtr.output.dir>docs/api</dtr.output.dir>
            <dtr.format>markdown</dtr.format>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

---

## Logging

DTR uses SLF4J for internal logging. Without an SLF4J binding you will see:

```
SLF4J: No SLF4J providers were found.
```

Add `slf4j-simple` as a test-scoped dependency to suppress this:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>2.0.17</version>
    <scope>test</scope>
</dependency>
```

---

## Maven enforcer rules

The parent `pom.xml` includes Maven Enforcer rules that require:
- Java 26 or higher
- Maven 4.0.0-rc-3 or higher

Run `mvnd validate` to check these rules without building.

---

## Java and toolchain requirements

| Tool | Minimum version | Recommended |
|------|----------------|-------------|
| Java | 26 | 26.ea.13+ |
| Maven | 4.0.0-rc-3 | 4.0.0-rc-3+ |
| mvnd | 2.0.0 | 2.0.0+ |

Verify your environment:

```bash
java -version           # should be 26+
mvnd --version          # should be 2.0.0+
echo $JAVA_HOME         # should point to Java 26
```

Set `JAVA_HOME` if needed:

```bash
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
```

---

## Maven proxy (enterprise environments)

If Maven Central download fails with authentication errors:

```bash
# 1. Start the proxy
python3 maven-proxy-auth.py &

# 2. Add to .mvn/jvm.config
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1

# 3. Rebuild
mvnd clean install
```

The proxy handles HTTPS CONNECT tunneling and injects `Proxy-Authorization` headers automatically.

---

## Output format configuration

DTR supports multiple output formats for different use cases:

| Format | System Property Value | Use Case |
|--------|----------------------|----------|
| Markdown | `markdown` or `md` | Default documentation format |
| LaTeX | `latex` or `tex` | Academic papers, PDF generation |
| Blog | `blog` | Blog posts with simplified formatting |
| Slides | `slides` | Presentation decks with speaker notes |

Configure in `pom.xml`:

```xml
<properties>
    <dtr.format>markdown</dtr.format>
    <dtr.output.dir>docs/test</dtr.output.dir>
</properties>
```

Or override via system property:

```bash
mvnd test -Ddtr.format=slides
```

---

## RenderMachine customization

To customize output behavior, provide a custom `RenderMachine` implementation:

```java
public class CustomRenderMachine implements RenderMachine {
    @Override
    public void render(DocTestContext context, String output) {
        // Custom rendering logic
    }
}

// Register via @DtrTest configuration
@DtrTest(renderMachine = CustomRenderMachine.class)
public class MyDocumentationTest {
    // ...
}
```

For advanced customization, see the [RenderMachine API](rendermachine-api.md) reference.

---

## See also

- [DtrContext and DtrExtension API Reference](testbrowser-api.md) — API surface
- [Architecture](ARCHITECTURE.md) — System design and component overview
- [Troubleshooting](TROUBLESHOOTING.md) — Maven proxy, Java version errors, common issues
- [Known Issues](KNOWN_ISSUES.md) — version-specific limitations
