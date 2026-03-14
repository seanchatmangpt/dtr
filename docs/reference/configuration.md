# Reference: Configuration

**Version:** 2.6.0

---

## Maven dependency

```xml
<dependency>
    <groupId>io.github.seanchatmangpt.dtr</groupId>
    <artifactId>dtr-core</artifactId>
    <version>2.6.0</version>
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

---

## Compiler settings

Required Maven compiler configuration for Java 25 with preview features:

```xml
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
```

Required Surefire configuration to pass `--enable-preview` to the test JVM:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <argLine>--enable-preview</argLine>
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
| `dtr.output.dir` | `target/docs/test-results` | Base output directory |
| `dtr.output.formats` | `md,html,tex,json` | Comma-separated list of output formats to generate |
| `dtr.benchmark.warmup` | `100` | Default warmup iterations for `sayBenchmark` |
| `dtr.benchmark.iterations` | `1000` | Default measured iterations for `sayBenchmark` |
| `dtr.mermaid.cli` | (auto-detected) | Path to `mmdc` (Mermaid CLI) for LaTeX diagram export |

Set system properties in Maven:

```bash
mvnd test -Ddtr.output.dir=docs/api -Ddtr.output.formats=md,html
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
            <dtr.output.formats>md,html</dtr.output.formats>
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
    <version>2.0.13</version>
    <scope>test</scope>
</dependency>
```

---

## Maven enforcer rules

The parent `pom.xml` includes Maven Enforcer rules that require:
- Java 25 or higher
- Maven 4.0.0-rc-3 or higher

Run `mvnd validate` to check these rules without building.

---

## Java and toolchain requirements

| Tool | Minimum version | Recommended |
|------|----------------|-------------|
| Java | 25 | 25.0.2+ |
| Maven | 4.0.0-rc-3 | 4.0.0-rc-3+ |
| mvnd | 2.0.0 | 2.0.0+ |

Verify your environment:

```bash
java -version           # should be 25+
mvnd --version          # should be 2.0.0+
echo $JAVA_HOME         # should point to Java 25
```

Set `JAVA_HOME` if needed:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
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

## See also

- [DtrContext and DtrExtension API Reference](testbrowser-api.md) — API surface
- [FAQ and Troubleshooting](FAQ_AND_TROUBLESHOOTING.md) — Maven proxy, Java version errors
- [Known Issues](KNOWN_ISSUES.md) — version-specific limitations
