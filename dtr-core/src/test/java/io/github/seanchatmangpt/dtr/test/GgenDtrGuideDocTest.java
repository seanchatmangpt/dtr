/**
 * Copyright (C) 2013 the original author or authors.
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

import io.github.seanchatmangpt.dtr.DocDescription;
import io.github.seanchatmangpt.dtr.DocSection;
import io.github.seanchatmangpt.dtr.DtrTest;
import io.github.seanchatmangpt.dtr.junit5.DtrExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Documents the ggen-cli → DTR integration: how to go from a single RDF ontology
 * to a fully scaffolded JUnit 5 documentation test suite in one command.
 *
 * <p>The examples live in {@code examples/ggen-dtr/}. This test documents the
 * design rationale, file structure, and workflow so future users have a reference
 * that is both readable and executable.</p>
 */
@ExtendWith(DtrExtension.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class GgenDtrGuideDocTest extends DtrTest {

    // ── Section 1 ───────────────────────────────────────────────────────────

    @Test
    @DocSection("What ggen-cli Does for DTR")
    @DocDescription({
        "ggen-cli reads an RDF ontology, runs SPARQL queries to extract structured data, " +
        "renders Tera templates with those results, and writes Java files pre-wired for DTR."
    })
    public void test01_overview() {
        say("The usual workflow for writing DTR tests is manual: create a class, add imports, " +
            "choose annotations, pick say* methods. ggen-cli automates that scaffolding step. " +
            "You declare your documentation intent in RDF; ggen emits the Java skeleton.");

        sayTable(new String[][]{
            {"Input",                  "Format",  "Your responsibility"},
            {"ontology/dtr-spec.ttl",  "Turtle",  "Declare test classes and sections"},
            {"templates/*.tera",       "Tera",    "Provided — do not edit unless customising"},
            {"ggen.toml",              "TOML",    "Provided — maps SPARQL queries to templates"}
        });

        sayNote("ggen-cli is a separate Rust tool. Install it with " +
                "`cargo install ggen-cli` or build from source, then run `ggen sync` " +
                "inside `examples/ggen-dtr/`.");
    }

    // ── Section 2 ───────────────────────────────────────────────────────────

    @Test
    @DocSection("The Three Artifacts ggen Produces")
    @DocDescription({
        "One ggen sync run executes three rules, producing a Java test class, " +
        "a Maven pom fragment, and a README index."
    })
    public void test02_three_artifacts() {
        say("Each rule in `ggen.toml` runs an independent SPARQL query, renders a Tera template " +
            "against the results, and writes one output file.");

        sayTable(new String[][]{
            {"Rule",               "Output file",                "Purpose"},
            {"dtr-test-class",     "lib/tests/<ClassName>.java", "Ready-to-compile DTR test class"},
            {"dtr-pom-fragment",   "lib/pom-fragment.xml",       "Copy-paste Maven dependency block"},
            {"dtr-test-index",     "lib/README.md",              "Human-readable index of all sections"}
        });

        sayCode("""
                # Run from examples/ggen-dtr/
                ggen sync
                # → lib/tests/MyFeatureDocTest.java
                # → lib/pom-fragment.xml
                # → lib/README.md
                """, "bash");
    }

    // ── Section 3 ───────────────────────────────────────────────────────────

    @Test
    @DocSection("The Ontology: Declaring Tests in RDF")
    @DocDescription({
        "ontology/dtr-spec.ttl is the single source of truth. " +
        "Edit it to add classes, sections, or say* methods — then re-run ggen sync."
    })
    public void test03_ontology() {
        say("The ontology defines two types: `dtr:DocClass` (one Java file) and " +
            "`dtr:DocSection` (one @Test method). Each section carries the " +
            "annotations and say* scaffold for that method.");

        sayCode("""
                @prefix dtr:  <https://dtr.io/ontology/test#> .
                @prefix :     <https://example.com/myproject#> .

                # ── declare a test class ─────────────────────────────────────
                :MyFeatureDocTest a dtr:DocClass ;
                    dtr:package "com.example.docs" ;
                    dtr:name    "MyFeatureDocTest" ;
                    dtr:section :section_intro .

                # ── declare one section (= one @Test method) ─────────────────
                :section_intro a dtr:DocSection ;
                    dtr:order       "1" ;
                    dtr:title       "Getting Started" ;
                    dtr:description "Walk through the basic usage." ;
                    dtr:sayMethod   "say" ;
                    dtr:sayMethod   "sayCode" .
                """, "turtle");

        sayTable(new String[][]{
            {"Predicate",         "On",              "Effect in generated Java"},
            {"dtr:package",       "dtr:DocClass",    "package declaration"},
            {"dtr:name",          "dtr:DocClass",    "public class <Name>"},
            {"dtr:section",       "dtr:DocClass",    "links to dtr:DocSection resources"},
            {"dtr:order",         "dtr:DocSection",  "@TestMethodOrder sort key"},
            {"dtr:title",         "dtr:DocSection",  "@DocSection(\"...\") value"},
            {"dtr:description",   "dtr:DocSection",  "@DocDescription({\"...\"}) value"},
            {"dtr:sayMethod",     "dtr:DocSection",  "scaffolded say* call in method body"}
        });

        sayNote("dtr:sayMethod is multi-valued — a section can request any combination " +
                "of say*, sayCode, sayTable, sayWarning, sayNote.");
    }

    // ── Section 4 ───────────────────────────────────────────────────────────

    @Test
    @DocSection("Running ggen sync: End-to-End Workflow")
    @DocDescription({
        "From an empty project to a compiled, documentation-generating test suite " +
        "in five steps."
    })
    public void test04_workflow() {
        say("The complete workflow from first run to published documentation:");

        sayOrderedList(List.of(
            "Install ggen-cli: `cargo install ggen-cli` (or build from source)",
            "Navigate to the example: `cd examples/ggen-dtr`",
            "Generate scaffolding: `ggen sync`",
            "Copy output to your project: `cp lib/tests/MyFeatureDocTest.java " +
                "src/test/java/com/example/docs/`",
            "Add the Maven dependency: merge `lib/pom-fragment.xml` into your `pom.xml`",
            "Fill in the TODO placeholders in the generated test with real content",
            "Run the CI gate: `mvnd verify` → `target/docs/MyFeatureDocTest.md` is produced"
        ));

        sayCode("""
                # The generated test class structure (abbreviated):
                @ExtendWith(DtrExtension.class)
                @TestMethodOrder(MethodOrderer.MethodName.class)
                public class MyFeatureDocTest extends DtrTest {

                    @Test
                    @DocSection("Getting Started")
                    @DocDescription({"Walk through the basic usage."})
                    public void test01_getting_started() {
                        say("Walk through the basic usage...");
                        sayCode("// TODO: add example", "java");
                    }

                    @AfterAll static void afterAll() { finishDocTest(); }
                }
                """, "java");

        sayWarning("ggen sync is idempotent but overwrites `lib/`. Never edit files " +
                   "in `lib/` directly — edit the ontology instead and re-run `ggen sync`.");
    }

    // ── Section 5 ───────────────────────────────────────────────────────────

    @Test
    @DocSection("Extending the Ontology")
    @DocDescription({
        "The examples/ggen-dtr ontology is the starting point, not the ceiling. " +
        "Add entities, sections, or entirely new DocClass resources to grow your " +
        "documentation suite without touching the templates."
    })
    public void test05_extending() {
        say("All supported say* methods that can be requested via `dtr:sayMethod`:");

        sayTable(new String[][]{
            {"dtr:sayMethod value",  "Generated call",            "Best for"},
            {"say",                  "say(\"...\");",              "Narrative prose"},
            {"sayCode",              "sayCode(\"...\", \"java\");", "Code examples"},
            {"sayTable",             "sayTable(new String[][]…);", "Config references, comparisons"},
            {"sayNote",              "sayNote(\"...\");",           "Helpful context"},
            {"sayWarning",           "sayWarning(\"...\");",        "Caveats, breaking changes"},
            {"sayOrderedList",       "sayOrderedList(List.of(…));", "Procedures, steps"},
            {"sayUnorderedList",     "sayUnorderedList(List.of(…));", "Unordered bullet lists"},
            {"sayKeyValue",          "sayKeyValue(Map.of(…));",    "Metadata, key facts"},
            {"sayJson",              "sayJson(object);",           "API response shapes"}
        });

        say("To add a new test class, append to the ontology and re-run `ggen sync`:");

        sayCode("""
                # ontology/dtr-spec.ttl — add a second class
                :PaymentDocTest a dtr:DocClass ;
                    dtr:package "com.example.docs" ;
                    dtr:name    "PaymentDocTest" ;
                    dtr:section :section_payment_intro .

                :section_payment_intro a dtr:DocSection ;
                    dtr:order       "1" ;
                    dtr:title       "Payment Processing Overview" ;
                    dtr:description "How payment flow works end-to-end." ;
                    dtr:sayMethod   "say" ;
                    dtr:sayMethod   "sayTable" .
                """, "turtle");

        assertNotNull(getClass().getSimpleName(), "class name is present");
    }

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }
}
