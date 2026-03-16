# Quick Start Guide

Get up and running with DTR in 5 minutes. This guide takes you from zero to your first documentation test.

## Prerequisites

Before you begin, verify you have the required tools installed:

```bash
# Check Java version (must be 26+)
java -version
# Expected output: openjdk version "26.ea.13" or higher

# Check Maven version (must be 4.0.0-rc-3+)
mvn -version
# Expected output: Apache Maven 4.0.0-rc-3 or higher

# Verify Git is installed
git --version
```

**If any of these checks fail:**

- **Java 26**: Install via SDKMAN: `sdk install java 26.ea.13-tem` or download from [openjdk.org](https://openjdk.org/)
- **Maven 4**: Download from [maven.apache.org](https://maven.apache.org/download.cgi) or use `brew install maven` (macOS)
- **mvnd (recommended)**: Install the Maven daemon for faster builds: `brew install mvnd` (macOS) or from [GitHub releases](https://github.com/apache/maven-mvnd/releases)

## Installation

Add DTR to your Maven project. Create a new Maven project or add to an existing one.

### 1. Add Dependency

Add this to your `pom.xml`:

```xml
<dependencies>
    <!-- DTR Core -->
    <dependency>
        <groupId>io.github.seanchatmangpt.dtr</groupId>
        <artifactId>dtr-core</artifactId>
        <version>2026.4.1</version>
        <scope>test</scope>
    </dependency>

    <!-- JUnit Jupiter 6 (required) -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>6.0.3</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Configure Maven Plugins

Add this to the `<build><plugins>` section of your `pom.xml`:

```xml
<build>
    <plugins>
        <!-- Compiler: Enable Java 26 preview features -->
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

        <!-- Surefire: Run tests with preview features enabled -->
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
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 3. Verify Installation

Run this command to verify everything is configured:

```bash
mvn dependency:resolve
```

If you see no errors, you're ready to write your first test.

## Your First Test

### The Modern Way with Field Injection (DTR 2026.4.1+)

The recommended approach uses field injection for cleaner code and better test isolation:

```java
package com.example;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.Doc;
import io.github.seanchatmangpt.dtr.DtrContext;
import org.junit.jupiter.api.Test;

// Single annotation - no @ExtendWith needed!
@DtrTest
class MyFirstDocTest {

    // Field injection: DTR automatically provides a context instance
    private DtrContext dtr;

    @Test
    @Doc(
        section = "Hello DTR",
        description = "This is my first documentation test with field injection!"
    )
    void helloDtr() {
        // Use the injected context to generate documentation
        dtr.sayNextSection("Code Example");
        dtr.sayCode("System.out.println(\"Hello, DTR!\");", "java");
        dtr.sayNote("Documentation is generated from this test.");
    }
}
```

### Using Static Imports (Cleaner Syntax)

For the cleanest syntax, use static imports:

```java
package com.example;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.Doc;
import io.github.seanchatmangpt.dtr.DtrContext;
import org.junit.jupiter.api.Test;

import static io.github.seanchatmangpt.dtr.Dtr.*; // Static imports

@DtrTest
class MyFirstDocTest {

    // Field injection - automatic context provision
    private DtrContext dtr;

    @Test
    @Doc(section = "Hello DTR", description = "This is my first documentation test!")
    void helloDtr() {
        // Clean syntax without qualification
        sayNextSection("Code Example");
        sayCode("System.out.println(\"Hello, DTR!\");", "java");
        sayNote("Documentation is generated from this test.");
    }
}
```
```

### Legacy Approach (Still Supported)

The traditional "extends DtrTest" approach is still supported but not recommended for new projects:

```java
package com.example;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.DocSection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DtrExtension.class)
class MyFirstDocTest extends DtrTest {

    @Test
    @DocSection("Hello DTR")
    void helloDtr() {
        say("This is my first documentation test!");
        sayNextSection("Code Example");
        sayCode("System.out.println(\"Hello, DTR!\");", "java");
        sayNote("Documentation is generated from this test.");
    }
}
```

**Recommended Approach:** Use field injection with @DtrTest annotation for better test isolation and cleaner code. The "extends DtrTest" approach is considered legacy for new development.

## Running the Test

Execute your test with Maven:

```bash
# Using standard Maven
mvn test

# OR using mvnd (faster, recommended)
mvnd test

# OR run specific test class
mvn test -Dtest=MyFirstDocTest
```

**Expected output:**

```
[INFO] -------------------------------------------------------
[INFO]  T E S T S
[INFO] -------------------------------------------------------
[INFO] Running com.example.MyFirstDocTest
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

## Viewing Output

DTR generates documentation in the `docs/test/` directory:

```bash
# View the generated documentation
cat docs/test/com.example.MyFirstDocTest.md

# Or open in your default editor
open docs/test/com.example.MyFirstDocTest.md  # macOS
xdg-open docs/test/com.example.MyFirstDocTest.md  # Linux
```

**You should see:**

```markdown
# Hello DTR

This is my first documentation test!

## Code Example

```java
System.out.println("Hello, DTR!");
```

> [!NOTE]
> Documentation is generated from this test.
```

## Next Steps

Congratulations! You've just created your first documentation test. Here's what to explore next:

### Learn the Basics

- **[say* API Reference](/Users/sac/dtr/docs/reference/say-api.md)** - Complete guide to all 50+ documentation methods
- **[Tutorial: Structured Documentation](/Users/sac/dtr/docs/tutorials/structured-documentation.md)** - Learn to organize documentation with sections, tables, and lists
- **[Tutorial: Code Examples](/Users/sac/dtr/docs/tutorials/code-examples.md)** - Document code with syntax highlighting and annotations

### Advanced Features

- **[Java 26 Code Reflection](/Users/sac/dtr/docs/explanation/java26-code-reflection.md)** - Auto-generate documentation from bytecode using JEP 516
- **[Cross-References](/Users/sac/dtr/docs/tutorials/cross-references.md)** - Link between documentation sections
- **[Assertion Documentation](/Users/sac/dtr/docs/tutorials/assertion-documentation.md)** - Document test results as tables

### Real-World Examples

Explore the DTR codebase for examples:

```bash
# Find all documentation tests in the DTR project
find . -name "*DocTest.java" -type f

# View example documentation output
ls -la docs/test/
```

## Troubleshooting

### Build Fails with "Java 26 required"

**Problem:** Maven enforcer plugin rejects Java version

**Solution:** Install Java 26 and verify:
```bash
java -version  # Must show 26.ea.13 or higher
export JAVA_HOME=$(/usr/libexec/java_home -v 26)  # macOS
```

### Tests Fail with "ClassNotFoundException: DtrExtension"

**Problem:** DTR dependency not resolved

**Solution:** Verify `pom.xml` has the dtr-core dependency:
```bash
mvn dependency:tree | grep dtr-core
```

### "enable-preview" Flag Errors

**Problem:** Tests fail with preview feature errors

**Solution:** Ensure both compiler and surefire plugins have `--enable-preview`:
```bash
mvn clean test -X | grep "enable-preview"
```

### No Documentation Generated

**Problem:** Tests pass but `docs/test/` is empty

**Solutions:**

1. Check test uses `@DtrTest` annotation:
   ```bash
   grep "@DtrTest" src/test/java/**/*.java
   ```

2. For legacy tests, verify `@ExtendWith(DtrExtension.class)` is present:
   ```bash
   grep "@ExtendWith(DtrExtension.class)" src/test/java/**/*.java
   ```

3. Check system property configuration:
   ```bash
   mvn test -Ddtr.output.dir=docs/test -Ddtr.format=markdown
   ```

4. For field injection tests, ensure `DtrContext` field is properly injected:
   ```bash
   grep "private DtrContext dtr" src/test/java/**/*.java
   ```

### mvnd is Faster than mvn

**Recommendation:** Use `mvnd` instead of `mvn` for faster builds:

```bash
# Install mvnd (Maven Daemon)
brew install mvnd  # macOS
# Or download from: https://github.com/apache/maven-mvnd/releases

# Use mvnd exactly like mvn
mvnd test  # 2-3x faster than mvn test
```

### Can't Run Tests in IDE

**Problem:** Tests fail when run from IntelliJ IDEA or Eclipse

**Solution:** Configure IDE to use `--enable-preview`:

- **IntelliJ IDEA**: File → Settings → Build, Execution, Deployment → Build Tools → Maven → Runner → VM Options: `--enable-preview`
- **Eclipse**: Run → Run Configurations → JUnit → Arguments → VM Arguments: `--enable-preview`

## Getting Help

Still stuck? Here's how to get help:

- **GitHub Issues**: [Report bugs or request features](https://github.com/seanchatmangpt/dtr/issues)
- **Documentation**: [Full documentation index](/Users/sac/dtr/docs/index.md)
- **Examples**: [Browse example tests](https://github.com/seanchatmangpt/dtr/tree/main/dtr-core/src/test/java)

---

**Time to First Documentation Test:** 5 minutes

**What You've Learned:**
- Set up DTR in a Maven project
- Write a basic documentation test
- Run tests and view generated documentation
- Troubleshoot common issues

**Next Recommendation:** Work through the [Structured Documentation Tutorial](/Users/sac/dtr/docs/tutorials/structured-documentation.md) to learn sections, tables, and advanced formatting.
