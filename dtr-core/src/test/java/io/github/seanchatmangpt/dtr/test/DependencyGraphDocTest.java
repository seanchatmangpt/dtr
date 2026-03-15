/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.test;

import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.dependencies.MavenDependencyReader;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;

/**
 * Documentation test for {@link MavenDependencyReader}.
 *
 * <p>Demonstrates how DTR can parse a Maven {@code pom.xml} at the project root and
 * render the result as a Mermaid dependency graph and a structured dependency table.
 * All five test methods exercise the actual DTR pom.xml present in the working directory
 * during the Maven Surefire test run.</p>
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DependencyGraphDocTest extends DtrTest {

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    // =========================================================================
    // t01 — Overview
    // =========================================================================

    @Test
    void t01_overview() {
        sayNextSection("Maven Dependency Graph");

        say("""
            `MavenDependencyReader` parses the `pom.xml` located in a given project
            root directory and produces a `DependencyGraphResult` record containing:

            - The list of parsed `Dependency` records (groupId, artifactId, version, scope)
            - A Mermaid `graph LR` DSL string with one edge per dependency

            All parsing uses the standard JDK `DocumentBuilderFactory` — no external
            XML libraries are required. Errors are caught internally; the method always
            returns a valid result.""");

        sayCode("""
                // Parse pom.xml in the current Maven working directory
                var result = MavenDependencyReader.read(".");

                // The mermaidDsl field is a ready-to-render Mermaid graph
                sayMermaid(result.mermaidDsl());

                // Full dependency list is available as a typed List<Dependency>
                result.dependencies().forEach(dep ->
                    System.out.println(dep.groupId() + ":" + dep.artifactId()));
                """, "java");

        say("The root node label is always the project's own `<artifactId>` extracted " +
            "from the top-level `<project>` element, keeping it distinct from any " +
            "dependency node labels.");

        sayCode("""
                <!-- Minimal pom.xml structure parsed by MavenDependencyReader -->
                <project>
                  <artifactId>my-app</artifactId>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>6.0.3</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """, "xml");
    }

    // =========================================================================
    // t02 — Parse DTR project
    // =========================================================================

    @Test
    void t02_parseDtrProject() {
        sayNextSection("Parsing the DTR Project pom.xml");

        say("The following call resolves `pom.xml` relative to the Maven Surefire " +
            "working directory (`\".\"` maps to the `dtr-core` module directory at " +
            "test runtime). The graph is then rendered directly via `sayDependencyGraph`.");

        var result = MavenDependencyReader.read(".");

        sayDependencyGraph(".");

        sayAndAssertThat("mermaidDsl is not null",  result.mermaidDsl(), notNullValue());
        sayAndAssertThat("mermaidDsl is not empty", result.mermaidDsl(), not(emptyString()));
    }

    // =========================================================================
    // t03 — Dependency table
    // =========================================================================

    @Test
    void t03_dependencyTable() {
        sayNextSection("Dependency Table");

        say("Calling `MavenDependencyReader.read(\".\")` returns a typed " +
            "`List<Dependency>` record list. The table below is built directly " +
            "from those records using `sayTable()`.");

        var result = MavenDependencyReader.read(".");

        sayAndAssertThat("dependencies list is not null", result.dependencies(), notNullValue());

        if (!result.dependencies().isEmpty()) {
            // Build a String[][] from the dependency list for sayTable()
            var rows = new String[result.dependencies().size() + 1][3];
            rows[0] = new String[]{"groupId", "artifactId", "scope"};
            for (int i = 0; i < result.dependencies().size(); i++) {
                var dep = result.dependencies().get(i);
                rows[i + 1] = new String[]{
                    dep.groupId(),
                    dep.artifactId(),
                    dep.scope().isEmpty() ? "compile" : dep.scope()
                };
            }
            sayTable(rows);
        } else {
            sayNote("No dependencies were found in the parsed pom.xml. " +
                    "This is expected if the test is run from a module directory " +
                    "whose pom.xml inherits all dependencies from a parent POM.");
        }

        say("`Dependency` is a Java 26 record with four components: " +
            "`groupId`, `artifactId`, `version`, and `scope`. " +
            "Both `version` and `scope` may be empty strings when the corresponding " +
            "XML elements are absent from a particular `<dependency>` block.");
    }

    // =========================================================================
    // t04 — Mermaid output
    // =========================================================================

    @Test
    void t04_mermaidOutput() {
        sayNextSection("Mermaid Graph Output");

        say("The `mermaidDsl()` component of `DependencyGraphResult` is a self-contained " +
            "Mermaid `graph LR` string. It can be passed directly to `sayMermaid()` for " +
            "rendering in documentation output.");

        var result = MavenDependencyReader.read(".");

        if (!result.mermaidDsl().isEmpty()) {
            sayMermaid(result.mermaidDsl());
        }

        sayNote("""
            Each node in the graph follows the pattern `artifactId:scope`.
            The root node is labelled with the project's own `<artifactId>`.
            When `pom.xml` is absent or unparseable, the Mermaid DSL contains a
            single error node rather than propagating an exception.""");

        sayCode("""
                // Accessing the raw DSL string
                var result = MavenDependencyReader.read(".");
                String dsl = result.mermaidDsl();

                // Always safe to check:
                if (!dsl.isEmpty()) {
                    sayMermaid(dsl);
                }
                """, "java");
    }

    // =========================================================================
    // t05 — API overview
    // =========================================================================

    @Test
    void t05_apiOverview() {
        sayNextSection("MavenDependencyReader API Reference");

        sayTable(new String[][]{
            {"Method", "Return Type", "Description"},
            {"read(String projectRoot)", "DependencyGraphResult",
                "Parse {projectRoot}/pom.xml; never throws"},
            {"toMarkdown(DependencyGraphResult)", "List<String>",
                "Heading + count + Mermaid block + dependency table as Markdown lines"},
        });

        sayNote("""
            `MavenDependencyReader` uses only `javax.xml.parsers.DocumentBuilderFactory`
            from the standard JDK. No third-party XML libraries (e.g. JDOM, dom4j) are
            needed. External entity expansion is disabled to prevent XXE vulnerabilities.""");

        sayTable(new String[][]{
            {"Record", "Components", "Notes"},
            {"Dependency", "groupId, artifactId, version, scope",
                "version and scope may be empty strings"},
            {"DependencyGraphResult", "projectRoot, dependencies, mermaidDsl",
                "dependencies is always an unmodifiable list"},
        });

        say("The `toMarkdown` helper returns an unmodifiable `List<String>` — suitable " +
            "for streaming directly into a DTR markdown document or for assertions in tests.");
    }
}
