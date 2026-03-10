# Reference: Configuration

## Output directory

DocTester writes all output to:

```
target/site/doctester/
```

This path is hard-coded in `RenderMachineImpl`. To change it, supply a custom `RenderMachine`.

## Output files

| File | Description |
|---|---|
| `{ClassName}.html` | Per-test documentation page |
| `index.html` | Index of all DocTest pages |
| `assets/bootstrap/3.0.0/css/bootstrap.min.css` | Bootstrap CSS |
| `assets/bootstrap/3.0.0/js/bootstrap.min.js` | Bootstrap JS |
| `assets/jquery/1.9.0/jquery.min.js` | jQuery |
| `org/doctester/custom_doctester_stylesheet.css` | Your custom CSS (if present) |

## Custom CSS

Place a file at:

```
src/test/resources/org/doctester/custom_doctester_stylesheet.css
```

DocTester copies it to the output directory and links it after Bootstrap in every HTML page.

## Custom output filename

Call `setClassNameForDocTesterOutputFile(String)` in a `@Before` method:

```java
@Before
public void configureOutput() {
    setClassNameForDocTesterOutputFile("user-api-v2");
}
```

Produces `target/site/doctester/user-api-v2.html` instead of the default class-name-based filename.

## Build system integration

DocTester generates documentation as a side effect of running tests. No additional Maven plugin is required.

```bash
# Run tests and generate documentation
mvnd test

# Or as part of full build
mvnd verify
```

The `target/site/doctester/` directory is excluded from `mvnd clean` by default (since `clean` only deletes `target/` entirely).

## Compiler settings

Required Maven compiler configuration for Java 25:

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

## Logging

DocTester uses SLF4J for logging (via Apache HttpClient). Without an SLF4J binding you'll see:

```
SLF4J: No SLF4J providers were found.
```

Add `slf4j-simple` and `jcl-over-slf4j` as test-scoped dependencies to suppress this:

```xml
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-simple</artifactId>
    <version>1.7.12</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>jcl-over-slf4j</artifactId>
    <version>1.7.12</version>
    <scope>test</scope>
</dependency>
```

## HTTP client defaults

`TestBrowserImpl` is configured with:
- **Redirect following:** On (override per-request with `Request.followRedirects(false)`)
- **Cookie persistence:** Per test method (fresh `TestBrowserImpl` per `@Test`)
- **Timeout:** Default Apache HttpClient timeout (no explicit timeout set)
- **SSL:** Uses the JVM's default trust store

## Maven enforcer

The parent `pom.xml` includes Maven Enforcer rules that require:
- Java 25 or higher
- Maven 4 or higher

Run `mvnd validate` to check these rules without building.
