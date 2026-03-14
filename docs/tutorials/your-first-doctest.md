# Tutorial: Your First DocTest

In this tutorial you will write a DocTest that exercises real Java code, asserts on results, and produces a Markdown documentation page — all from a single JUnit 5 test class.

By the end you will have:

- A Maven project with DTR 2.6.0 configured
- A working test that documents a Java data model
- A Markdown file in `target/docs/test-results/` you can read directly

**Time:** ~20 minutes
**Prerequisites:** Java 26, Maven 4 (`mvnd`)

---

## Step 1 — Add DTR to your project

Open your `pom.xml` and add DTR to the test dependencies:

```xml
<dependencies>
    <!-- JUnit 5 -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.11.0</version>
        <scope>test</scope>
    </dependency>

    <!-- DTR 2.6.0 -->
    <dependency>
        <groupId>io.github.seanchatmangpt.dtr</groupId>
        <artifactId>dtr-core</artifactId>
        <version>2.6.0</version>
        <scope>test</scope>
    </dependency>

    <!-- AssertJ -->
    <dependency>
        <groupId>org.assertj</groupId>
        <artifactId>assertj-core</artifactId>
        <version>3.26.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Configure the compiler plugin for Java 26 with preview features enabled:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
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
            <version>3.5.0</version>
            <configuration>
                <argLine>--enable-preview</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

Create `.mvn/maven.config` to apply preview flags globally:

```
--enable-preview
```

---

## Step 2 — Create your first test class

Create the file `src/test/java/com/example/ProductDocTest.java`:

```java
package com.example;

import io.github.seanchatmangpt.dtr.core.DtrContext;
import io.github.seanchatmangpt.dtr.core.DtrExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DtrExtension.class)
class ProductDocTest {

    // A simple domain record
    record Product(long id, String name, double price, String category) {}

    @Test
    void documentProductModel(DtrContext ctx) {

        ctx.sayNextSection("Product Data Model");

        ctx.say("The `Product` record is the core data model. It is immutable, "
            + "serializable to JSON, and has compiler-generated equals, hashCode, and toString.");

        // Show the record schema using DTR's built-in reflection
        ctx.sayRecordComponents(Product.class);

        ctx.sayNextSection("Creating Products");

        ctx.say("Construct a Product with the canonical record constructor:");

        ctx.sayCode("""
            var widget = new Product(1L, "Widget Pro", 49.99, "tools");
            var gadget = new Product(2L, "Gadget Mini", 19.99, "electronics");
            """, "java");

        var widget = new Product(1L, "Widget Pro", 49.99, "tools");
        var gadget = new Product(2L, "Gadget Mini", 19.99, "electronics");

        ctx.sayNextSection("Assertions");

        ctx.say("DTR documents the shape of your data alongside your assertions. "
            + "Use AssertJ for rich, readable checks:");

        assertThat(widget.name()).isEqualTo("Widget Pro");
        assertThat(widget.price()).isPositive();
        assertThat(gadget.category()).isEqualTo("electronics");

        ctx.sayAssertions(Map.of(
            "widget.name()", "Widget Pro",
            "widget.price() > 0", "true",
            "gadget.category()", "electronics"
        ));

        ctx.sayNextSection("Collection Operations");

        ctx.say("Java streams work naturally with records. Here we filter by category "
            + "and collect to a list:");

        var products = List.of(widget, gadget,
            new Product(3L, "Wrench Set", 34.50, "tools"));

        var tools = products.stream()
            .filter(p -> p.category().equals("tools"))
            .toList();

        ctx.say("Tools category contains " + tools.size() + " products.");

        assertThat(tools).hasSize(2);
        assertThat(tools).allMatch(p -> p.category().equals("tools"));

        ctx.sayTable(new String[][] {
            {"ID", "Name", "Price", "Category"},
            {"1", "Widget Pro", "$49.99", "tools"},
            {"3", "Wrench Set", "$34.50", "tools"}
        });
    }
}
```

**What's happening here?**

- `@ExtendWith(DtrExtension.class)` — registers DTR with JUnit 5
- `DtrContext ctx` — injected parameter that gives access to all `say*` methods
- `ctx.sayNextSection(...)` — creates a section heading in the output
- `ctx.say(...)` — adds a paragraph of explanatory text
- `ctx.sayRecordComponents(...)` — auto-generates a schema table from the record class
- `ctx.sayAssertions(...)` — documents assertion results in a readable table
- `ctx.sayTable(...)` — renders a Markdown table

---

## Step 3 — Run the test

```bash
mvnd test -Dtest=ProductDocTest
```

You will see standard JUnit 5 output. After it completes:

```bash
ls target/docs/test-results/
```

You should see:

```
ProductDocTest.md
ProductDocTest.html
ProductDocTest.tex
ProductDocTest.json
```

---

## Step 4 — Read the documentation

Open `target/docs/test-results/ProductDocTest.md` in any Markdown viewer:

```bash
cat target/docs/test-results/ProductDocTest.md
```

You will see structured sections with:

- A **record schema table** generated by `sayRecordComponents`
- **Code blocks** showing usage examples
- An **assertion table** showing what was validated
- A **data table** of the filtered results

This is your living documentation. It was generated by running the test against real code.

---

## Step 5 — Add an environment snapshot

Append a final test method to capture build-time environment facts:

```java
@Test
void documentEnvironment(DtrContext ctx) {

    ctx.sayNextSection("Build Environment");

    ctx.say("DTR can capture a snapshot of the environment in which tests run. "
        + "This is useful for reproducibility and audit trails.");

    ctx.sayEnvProfile();
}
```

`sayEnvProfile()` takes no arguments. It reads Java version, OS, heap size, and DTR version at runtime and emits a formatted key-value table.

Run again and observe the new section in the output:

```bash
mvnd test -Dtest=ProductDocTest
cat target/docs/test-results/ProductDocTest.md
```

---

## What you learned

- How to add DTR 2.6.0 to a Maven project with JUnit 5
- The JUnit 5 extension pattern: `@ExtendWith(DtrExtension.class)` + `DtrContext ctx` parameter
- Core `say*` methods: `sayNextSection`, `say`, `sayCode`, `sayTable`, `sayAssertions`
- New 2.6.0 methods: `sayRecordComponents`, `sayEnvProfile`
- Where DTR writes its output (`target/docs/test-results/`)

---

## Next steps

- [Tutorial: Testing a REST API](testing-a-rest-api.md) — call real HTTP endpoints and document the results
- [Tutorial: Records and Sealed Classes](records-sealed-classes.md) — advanced record patterns with `sayRecordComponents` and `sayCodeModel`
- [Tutorial: Benchmarking with sayBenchmark](virtual-threads-lightweight-concurrency.md) — measure and document performance
