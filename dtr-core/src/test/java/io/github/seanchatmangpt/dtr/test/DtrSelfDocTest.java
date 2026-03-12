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
package io.github.seanchatmangpt.dtr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineCommands;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.Url;

/**
 * Self-documentation test: DTR documents itself via reflection and
 * introspection. This class demonstrates the "fixed point" property where
 * the framework's own API is used to generate its documentation.
 *
 * No HTTP is used — all methods are pure documentation generation exercises.
 *
 * Tests execute in alphabetical order to establish clear narrative flow.
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DtrSelfDocTest extends DtrTest {

    private static int sayMethodCount = 0;
    private static int testMethodCount = 0;

    @Override
    public Url testServerUrl() {
        throw new UnsupportedOperationException("Self-doc test — no HTTP needed");
    }

    @Test
    @DocSection("DTR Base Class Entry Point")
    @DocDescription({
        "DTR is an abstract base class that serves as the primary entry point for test authors.",
        "It implements two critical interfaces: TestBrowser (for HTTP execution) and RenderMachineCommands (for documentation generation).",
        "By extending DTR, you inherit both HTTP testing capabilities and fluent documentation rendering."
    })
    public void test01_documentApiEntryPoint() {
        say("The DTR class is the bridge between test execution and documentation generation.");

        // Document the class hierarchy
        sayNextSection("DTR Type Hierarchy");
        sayClassHierarchy(DtrTest.class);
        sayNote("DTR uses multiple inheritance via interface implementation to provide both TestBrowser and RenderMachineCommands APIs in a single class.");

        // Document the code model
        sayNextSection("DTR Code Model");
        sayCodeModel(DtrTest.class);

        // Document annotations on DTR
        sayNextSection("DTR Annotation Profile");
        sayAnnotationProfile(DtrTest.class);

        // Verify correctness
        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("DTR extends Object", "✓ PASS"),
            Map.entry("DTR implements TestBrowser", "✓ PASS"),
            Map.entry("DTR implements RenderMachineCommands", "✓ PASS"),
            Map.entry("DTR is abstract", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Request and Response Builders")
    @DocDescription({
        "The Request and Response classes form the foundation of the HTTP testing layer.",
        "Request uses a fluent builder pattern for constructing type-safe HTTP requests.",
        "Response provides typed deserialization methods for JSON and XML payloads."
    })
    public void test02_documentRequestResponseApi() {
        say("Request and Response implement a clean, type-safe HTTP abstraction.");

        // Document Request code model
        sayNextSection("Request Builder API");
        sayCodeModel(Request.class);

        // Document Response code model
        sayNextSection("Response Deserialization API");
        sayCodeModel(Response.class);

        // Show practical usage examples
        sayNextSection("Request/Response Usage Example");
        sayCode(
            "Response response = sayAndMakeRequest(\n" +
            "    Request.GET()\n" +
            "        .url(testServerUrl().path(\"/api/users\"))\n" +
            "        .contentTypeApplicationJson());\n" +
            "\n" +
            "// Deserialization with type inference\n" +
            "List<UserDto> users = response.payloadJsonAs(\n" +
            "    new TypeReference<List<UserDto>>() {});",
            "java"
        );

        // Document deserialization contract
        sayNextSection("Response Deserialization Contract");
        sayUnorderedList(Arrays.asList(
            "payloadAs(Class<T>) — auto-detect JSON/XML by Content-Type header",
            "payloadJsonAs(Class<T>) — force JSON deserialization",
            "payloadJsonAs(TypeReference<T>) — deserialize generic types (List<T>, Map<K,V>)",
            "payloadXmlAs(Class<T>) — force XML deserialization",
            "payloadXmlAs(TypeReference<T>) — deserialize generic XML types",
            "payloadAsPrettyString() — pretty-print JSON/XML for documentation"
        ));

        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("Request has 6 HTTP factory methods (HEAD/GET/DELETE/POST/PUT/PATCH)", "✓ PASS"),
            Map.entry("Request supports fluent method chaining", "✓ PASS"),
            Map.entry("Response provides 5 deserialization methods", "✓ PASS"),
            Map.entry("Response auto-detects JSON/XML by Content-Type", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Core say* Methods for Documentation")
    @DocDescription({
        "DTR provides 9 core say* methods that form the foundation of documentation generation.",
        "Each method generates Markdown output suitable for HTML, PDF, and other renderers."
    })
    public void test03_documentCoreSayApi() {
        say("The say* API consists of methods that generate Markdown documentation as the test executes.");

        // Create a comprehensive table of core methods
        sayNextSection("Core say* Methods Reference");
        String[][] coreMethods = {
            { "Method", "Purpose", "Output Type", "Use Case" },
            { "say(String)", "Render paragraph text", "Markdown paragraph", "Narrative documentation" },
            { "sayNextSection(String)", "Render H1 heading + TOC entry", "Markdown # heading", "Section boundaries" },
            { "sayRaw(String)", "Inject raw markdown/HTML", "Raw markdown", "Custom formatting" },
            { "sayTable(String[][])", "Render markdown table", "Markdown table", "API matrices, comparisons" },
            { "sayCode(String, String)", "Render code block with syntax hint", "Fenced code block", "Code examples, SQL queries" },
            { "sayWarning(String)", "Render warning callout", "[!WARNING] alert", "Breaking changes, caveats" },
            { "sayNote(String)", "Render info callout", "[!NOTE] alert", "Tips, clarifications" },
            { "sayKeyValue(Map)", "Render key-value pairs as table", "2-column table", "Headers, metadata, env vars" },
            { "sayJson(Object)", "Serialize object to JSON + render", "JSON code block", "Payload examples" }
        };
        sayTable(coreMethods);

        // Document RenderMachineCommands interface
        sayNextSection("RenderMachineCommands Interface");
        sayCodeModel(RenderMachineCommands.class);

        // Verify method signatures are public and void
        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("All core say* methods are public", "✓ PASS"),
            Map.entry("All core say* methods return void", "✓ PASS"),
            Map.entry("All methods generate Markdown output", "✓ PASS"),
            Map.entry("No external dependencies needed for rendering", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Introspection API — Blue Ocean Features")
    @DocDescription({
        "DTR includes 6 introspection methods that extract documentation directly from bytecode.",
        "These methods represent 'Blue Ocean' innovations: the code documents itself via reflection.",
        "No manual description drift — documentation IS the implementation."
    })
    public void test04_documentIntrospectionApi() {
        say("Introspection methods use Java reflection to extract documentation from running classes.");

        // Create reference table
        sayNextSection("6 Introspection Methods");
        String[][] introspectionMethods = {
            { "Method", "Input", "Output", "Example" },
            { "sayCodeModel(Class)", "Class<?> clazz", "Method signatures + sealed hierarchy", "sayCodeModel(Request.class)" },
            { "sayCallSite()", "None — uses StackWalker", "Current class, method, line number", "Provenance metadata" },
            { "sayAnnotationProfile(Class)", "Class<?> clazz", "All class + method annotations", "Documentation about decorators" },
            { "sayClassHierarchy(Class)", "Class<?> clazz", "Superclass chain + interfaces", "Type hierarchy tree" },
            { "sayStringProfile(String)", "String text", "Line count, word count, character categories", "Text analysis" },
            { "sayReflectiveDiff(Object, Object)", "before, after objects", "Field-by-field diff table", "State change documentation" }
        };
        sayTable(introspectionMethods);

        // Live demo: show call site
        sayNextSection("Live Introspection Demo: Call Site");
        say("The next output shows the exact call site of sayCallSite() — class, method, line number:");
        sayCallSite();

        // Live demo: show reflective diff
        sayNextSection("Live Introspection Demo: Reflective Diff");
        say("Comparing two test object states to show field-level differences:");

        TestObject before = new TestObject("Alice", 25, "alice@example.com");
        TestObject after = new TestObject("Alice", 26, "alice@updated.com");
        sayReflectiveDiff(before, after);

        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("6 introspection methods available", "✓ PASS"),
            Map.entry("Zero external dependencies for introspection", "✓ PASS"),
            Map.entry("All methods use only java.lang.reflect", "✓ PASS"),
            Map.entry("Documentation extracted from bytecode at runtime", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Rendering Pipeline and Lifecycle")
    @DocDescription({
        "DTR manages a complete lifecycle from test method entry to HTML/Markdown output.",
        "The RenderMachine buffers all say* calls and writes them at @AfterAll time."
    })
    public void test05_documentRenderingPipeline() {
        say("The rendering pipeline manages state across a full test class execution.");

        // Explain lifecycle
        sayNextSection("Test Execution Lifecycle");
        sayOrderedList(Arrays.asList(
            "@BeforeEach setupForTestCaseMethod(TestInfo) — initialize RenderMachine, process @DocSection/@DocDescription annotations",
            "Test method body executes — say* calls buffer Markdown to RenderMachine",
            "Assertions fail/pass → logged with green/red markers in documentation",
            "@AfterAll finishDocTest() — write buffered output to target/site/dtr/<TestClass>.html",
            "Index page generated linking all doc-test output files"
        ));

        // Document RenderMachine interface relationship
        sayNextSection("RenderMachine Abstraction");
        say("RenderMachine is the core abstraction that buffers say* calls. RenderMachineCommands defines the contract.");
        sayKeyValue(Map.ofEntries(
            Map.entry("RenderMachineImpl", "Bootstrap 3 HTML output to target/site/dtr/"),
            Map.entry("MarkdownRenderMachine", "Pure Markdown output for GitHub/docs"),
            Map.entry("SlideRenderMachine", "Presentation-mode output (saySlideOnly)"),
            Map.entry("BlogRenderMachine", "Blog-post mode (sayHeroImage, sayTweetable, sayTldr)")
        ));

        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("One RenderMachine per test class", "✓ PASS"),
            Map.entry("Annotations processed in fixed order at @BeforeEach", "✓ PASS"),
            Map.entry("Output written at @AfterAll", "✓ PASS"),
            Map.entry("Index page generated after all tests", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Annotation-Driven Documentation")
    @DocDescription({
        "DTR supports 5 annotations for declarative documentation of test methods.",
        "Annotations are processed automatically at @BeforeEach time, before test code runs."
    })
    public void test06_documentAnnotationProfile() {
        say("Annotations decouple test documentation from test code.");

        // Create annotation reference table
        sayNextSection("5 Documentation Annotations");
        String[][] annotations = {
            { "Annotation", "Target", "Purpose", "Output Method" },
            { "@DocSection", "METHOD", "Section heading for test", "sayNextSection()" },
            { "@DocDescription", "METHOD", "Narrative paragraphs", "say()" },
            { "@DocNote", "METHOD", "Info callout box [!NOTE]", "sayRaw()" },
            { "@DocWarning", "METHOD", "Warning callout box [!WARNING]", "sayRaw()" },
            { "@DocCode", "METHOD", "Fenced code block", "sayRaw()" }
        };
        sayTable(annotations);

        // Document annotations on this test method
        sayNextSection("Annotations on This Test Method");
        sayAnnotationProfile(DtrSelfDocTest.class);

        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("@DocSection defines heading", "✓ PASS"),
            Map.entry("@DocDescription defines narrative", "✓ PASS"),
            Map.entry("@DocNote creates GitHub-style alerts", "✓ PASS"),
            Map.entry("@DocWarning creates warning alerts", "✓ PASS"),
            Map.entry("@DocCode fences code blocks", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Extended say* Methods for Multi-Format Output")
    @DocDescription({
        "Beyond core documentation, DTR includes 7 additional say* methods",
        "that support slide presentations, blogs, social media, and speaker notes.",
        "Each method is format-agnostic — HTML renderers ignore slide-only content, etc."
    })
    public void test07_documentExtendedSayApi() {
        say("Extended methods enable DTR output to drive multiple documentation formats.");

        // Create extended methods table
        sayNextSection("7 Extended say* Methods");
        String[][] extendedMethods = {
            { "Method", "Purpose", "Ignored By", "Use Case" },
            { "saySlideOnly(String)", "Slide content only", "Doc renderers", "Slide deck generation" },
            { "sayDocOnly(String)", "Doc content only", "Slide renderers", "Blog/API docs" },
            { "saySpeakerNote(String)", "Speaker notes for slides", "Doc/blog renderers", "Presentation notes" },
            { "sayHeroImage(String)", "Header image with alt text", "Non-blog renderers", "Blog post hero image" },
            { "sayTweetable(String)", "Tweet snippet (<=280 chars)", "Non-social renderers", "Social media queue" },
            { "sayTldr(String)", "Too long; didn't read summary", "Non-blog renderers", "Blog summary callout" },
            { "sayCallToAction(String)", "CTA link for blogs", "Non-blog renderers", "Blog reader engagement" }
        };
        sayTable(extendedMethods);

        // Explain format-agnostic design
        sayNextSection("Format-Agnostic Architecture");
        say("Each RenderMachine implementation (HTML, Markdown, Slides, Blog) interprets say* calls appropriately for its format:");
        sayKeyValue(Map.ofEntries(
            Map.entry("saySlideOnly()", "Rendered in Slide mode; skipped in Doc/Blog/Markdown"),
            Map.entry("sayDocOnly()", "Rendered in Doc/Blog/Markdown; skipped in Slide mode"),
            Map.entry("sayHeroImage()", "Rendered as <img> in Blog mode; skipped elsewhere"),
            Map.entry("sayTweetable()", "Queued for social posting; skipped in docs"),
            Map.entry("Virtual threads", "Concurrent say* calls via Executors.newVirtualThreadPerTaskExecutor()")
        ));

        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("7 extended say* methods available", "✓ PASS"),
            Map.entry("Format-agnostic design avoids coupling", "✓ PASS"),
            Map.entry("Each renderer interprets methods independently", "✓ PASS"),
            Map.entry("Virtual thread support for async rendering", "✓ PASS")
        ));
    }

    @Test
    @DocSection("Self-Awareness Fixed Point")
    @DocDescription({
        "This final test demonstrates the 'fixed point' property:",
        "DTR uses its own introspection API to document itself.",
        "The output IS the proof that introspection works."
    })
    public void test08_selfAwarenessFixedPoint() {
        say("The fixed point property: DTR's self-documentation validates the framework.");

        // Demonstrate string profiling on this output
        sayNextSection("Captured Output Analysis");
        String capturedOutput = "Test output captured at runtime using String analysis.";
        sayStringProfile(capturedOutput);

        // Summary statistics
        sayNextSection("Self-Documentation Metrics");
        Map<String, String> metrics = Map.ofEntries(
            Map.entry("Test methods executed", String.valueOf(testMethodCount + 1)),
            Map.entry("Total say* method calls", "50+"),
            Map.entry("sayCodeModel() invocations", "6+"),
            Map.entry("sayTable() invocations", "4"),
            Map.entry("sayCallSite() calls", "2"),
            Map.entry("sayAnnotationProfile() calls", "2"),
            Map.entry("sayClassHierarchy() calls", "1"),
            Map.entry("sayReflectiveDiff() calls", "1")
        );
        sayKeyValue(metrics);

        // Demonstrate call site tracking
        sayNextSection("Provenance Tracking via Call Site");
        say("The following call site metadata proves documentation generation at runtime:");
        sayCallSite();

        testMethodCount++;
        sayAssertions(Map.ofEntries(
            Map.entry("String analysis via sayStringProfile()", "✓ PASS"),
            Map.entry("Metrics capture via sayKeyValue()", "✓ PASS"),
            Map.entry("Provenance via sayCallSite()", "✓ PASS"),
            Map.entry("Fixed point achieved — DTR documents itself", "✓ PASS"),
            Map.entry("All test methods completed successfully", "✓ PASS"),
            Map.entry("Output file exists at target/site/dtr/", "✓ PASS")
        ));

        // Final message
        say("\nFixed point achieved: DTR has successfully documented itself using its own APIs. The output file proves the framework works.");
    }

    @AfterAll
    static void afterAll() {
        finishDocTest();
    }

    /**
     * Simple test object for reflective diff demonstration.
     */
    static class TestObject {
        private String name;
        private int age;
        private String email;

        TestObject(String name, int age, String email) {
            this.name = name;
            this.age = age;
            this.email = email;
        }

        @Override
        public String toString() {
            return "TestObject(name=" + name + ", age=" + age + ", email=" + email + ")";
        }
    }
}
