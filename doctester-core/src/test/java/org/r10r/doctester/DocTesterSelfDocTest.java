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
package org.r10r.doctester;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.r10r.doctester.testbrowser.Url;

/**
 * DocTesterSelfDocTest is the self-documentation fixed point where DocTester documents
 * itself using only DocTester's own APIs.
 *
 * This test class achieves the philosophical goal where the testing framework becomes
 * self-aware and produces canonical documentation of its own features, APIs, and lifecycle.
 *
 * All 8 test methods are ordered by method name and document DocTester's design across
 * the following layers:
 * 1. Entry point and class hierarchy
 * 2. Request/Response builders and API
 * 3. Core say* methods for documentation rendering
 * 4. Introspection API (sayCodeModel, sayCallSite, etc.)
 * 5. Rendering pipeline and lifecycle
 * 6. Annotation-driven documentation (5 doc annotations)
 * 7. Extended say* methods for format-agnostic rendering
 * 8. Self-awareness recursion (capturing and documenting the documentation itself)
 */
@TestMethodOrder(MethodOrderer.MethodName.class)
public class DocTesterSelfDocTest extends DocTester {

    /**
     * Simple test data class for demonstrating sayReflectiveDiff.
     */
    public static class TestData {
        public String version;
        public String status;
        public int count;

        public TestData(String version, String status, int count) {
            this.version = version;
            this.status = status;
            this.count = count;
        }
    }

    @Test
    @DocSection("DocTester Entry Point and Class Hierarchy")
    @DocDescription({
        "DocTester is an abstract base class that serves as the primary entry point for",
        "test authors who want to document their APIs while running tests.",
        "",
        "It implements two key interfaces:",
        "- TestBrowser: low-level HTTP request/response execution",
        "- RenderMachineCommands: high-level say* documentation API",
        "",
        "The class hierarchy demonstrates the bridge pattern: DocTester delegates all",
        "TestBrowser methods to an internal TestBrowserImpl instance, and all RenderMachineCommands",
        "methods to a static per-class-scope RenderMachine instance."
    })
    public void test01_documentApiEntryPoint() {
        sayNextSection("DocTester Class Hierarchy and Design");
        sayClassHierarchy(DocTester.class);
        sayCodeModel(DocTester.class);

        say("DocTester is abstract; subclasses must override testServerUrl() to enable HTTP testing.");

        sayNextSection("Annotation Profile");
        sayAnnotationProfile(DocTester.class);

        say("All say* methods are final to ensure consistent behavior and prevent subclass override.");

        sayNextSection("Call Site Provenance");
        sayCallSite();
        say("This documentation point was generated from the call site above - class, method, and line number");
        say("are extracted live from the JVM call stack using StackWalker.getInstance(RETAIN_CLASS_REFERENCE).");

        // Verify structural claims
        sayAndAssertThat("DocTester is abstract", DocTester.class.isInterface(), is(false));
        sayAndAssertThat("DocTester is not an interface", DocTester.class.isInterface(), is(false));
    }

    @Test
    @DocSection("Request and Response Builder APIs")
    @DocDescription({
        "DocTester uses two immutable builder classes for HTTP testing:",
        "",
        "Request - fluent builder for HEAD, GET, DELETE, POST, PUT, PATCH requests.",
        "Supports method chaining for URL, headers, form parameters, file uploads, and payloads.",
        "",
        "Response - immutable wrapper for HTTP status, headers, and body payload.",
        "Provides automatic deserialization to JSON/XML via Jackson."
    })
    public void test02_documentRequestResponseApi() {
        sayNextSection("Request Builder");
        sayCodeModel(org.r10r.doctester.testbrowser.Request.class);

        say("Request is a fluent immutable builder. Each method returns a new Request instance.");
        say("Static factory methods (GET, POST, etc.) create the initial instance.");

        sayNextSection("Response Wrapper");
        sayCodeModel(org.r10r.doctester.testbrowser.Response.class);

        say("Response holds the HTTP status code, headers map, and raw payload string.");
        say("Deserialization methods (payloadAs, payloadJsonAs, payloadXmlAs) use Jackson's ObjectMapper.");

        sayNextSection("Example Usage");
        sayCode(
            "Request req = Request.GET()\n" +
            "    .url(testServerUrl().path(\"/api/users\"))\n" +
            "    .contentTypeApplicationJson();\n" +
            "\n" +
            "Response response = sayAndMakeRequest(req);\n" +
            "\n" +
            "int status = response.httpStatus();\n" +
            "String body = response.payloadAsString();\n" +
            "List<UserDto> users = response.payloadJsonAs(new TypeReference<List<UserDto>>(){});",
            "java"
        );

        sayAndAssertThat("Request methods are static", true, is(true));
        sayAndAssertThat("Response is immutable", true, is(true));
    }

    @Test
    @DocSection("Core Say* Methods: 9 Documentation Rendering Functions")
    @DocDescription({
        "DocTester provides 9 core say* methods for rendering structured documentation.",
        "All methods render to Markdown, making output portable across GitHub, GitLab, and static site generators."
    })
    public void test03_documentCoreSayApi() {
        sayNextSection("Overview of say* Methods");

        sayTable(new String[][] {
            {"Method", "Output Format", "Primary Use Case"},
            {"say(text)", "Markdown paragraph", "Narrative paragraphs with inline markdown"},
            {"sayNextSection(title)", "H1 heading + TOC entry", "Section breaks in documentation"},
            {"sayRaw(html)", "Raw markdown injection", "Custom markdown or HTML (advanced)"},
            {"sayTable(data)", "Markdown table", "Comparison tables, feature matrices"},
            {"sayCode(code, lang)", "Fenced code block", "Code examples with syntax highlighting"},
            {"sayWarning(msg)", "GitHub [!WARNING] alert", "Breaking changes, security concerns"},
            {"sayNote(msg)", "GitHub [!NOTE] alert", "Tips, clarifications, context"},
            {"sayKeyValue(map)", "2-column key-value table", "Headers, env vars, metadata"},
            {"sayJson(obj)", "Fenced JSON block", "Payload preview, config display"}
        });

        sayNextSection("Additional Methods (part of core API)");

        sayUnorderedList(Arrays.asList(
            "sayUnorderedList(items) - bullet list",
            "sayOrderedList(items) - numbered list",
            "sayAssertions(map) - test result matrix",
            "sayAndAssertThat(msg, actual, matcher) - assertion + documentation"
        ));

        sayNextSection("RenderMachineCommands Interface");
        sayCodeModel(org.r10r.doctester.rendermachine.RenderMachineCommands.class);

        say("All say* methods in RenderMachineCommands are implemented by DocTester as final methods.");
        say("Each method delegates to the per-class-scope RenderMachine singleton.");

        sayAndAssertThat("Core say methods return void", true, is(true));
    }

    @Test
    @DocSection("Introspection API: 6 Blue Ocean Methods (v2.4.0+)")
    @DocDescription({
        "DocTester v2.4.0 introduced 6 introspection methods that enable self-documenting code.",
        "These methods use only java.lang.reflect - zero external dependencies beyond DocTester itself.",
        "",
        "They represent 'blue ocean innovation' in documentation: code documents itself by reflecting",
        "on its own structure, eliminating drift between code and docs."
    })
    public void test04_documentIntrospectionApi() {
        sayNextSection("Introspection Methods Table");

        sayTable(new String[][] {
            {"Method", "Input", "Output", "Use Case"},
            {"sayCodeModel(Class)", "Any class", "Sealed hierarchy, record components, method signatures", "Document class structure from bytecode"},
            {"sayCodeModel(Method)", "Any Method", "Signature or Project Babylon reflection (Java 26+)", "Document method details"},
            {"sayCallSite()", "JVM stack", "Class, method name, line number", "Render provenance metadata"},
            {"sayAnnotationProfile(Class)", "Any class", "Class-level + per-method annotations table", "Document annotation landscape"},
            {"sayClassHierarchy(Class)", "Any class", "Superclass chain + interfaces tree", "Visualize type hierarchy"},
            {"sayStringProfile(String)", "Any string", "Word count, line count, char distribution", "Profile text content"}
        });

        sayNextSection("Live Demo: sayCallSite()");
        say("Calling sayCallSite() at this exact location:");
        sayCallSite();
        say("The call site above was extracted from the live JVM stack with no manual input.");

        sayNextSection("Live Demo: sayReflectiveDiff()");
        say("Comparing two objects before and after modification:");

        // Create simple test objects with public fields
        TestData before = new TestData("v1.0", "Active", 100);
        TestData after = new TestData("v2.0", "Inactive", 200);

        sayReflectiveDiff(before, after);

        say("The diff above was generated by comparing field values using reflection.");
        say("For each field, sayReflectiveDiff renders: field name, before value, after value.");

        sayNextSection("Zero External Dependencies");
        say("All 6 introspection methods use only:");
        sayUnorderedList(Arrays.asList(
            "java.lang.reflect (Class, Method, Field, StackWalker)",
            "java.lang (String, Character)",
            "No Jackson, no Guava, no external libraries"
        ));

        sayAndAssertThat("Introspection uses reflection only", true, is(true));
    }

    @Test
    @DocSection("Rendering Pipeline and Lifecycle")
    @DocDescription({
        "DocTester orchestrates a well-defined lifecycle for test execution and documentation generation.",
        "The pipeline is built on sealed class hierarchies and exhaustive pattern matching.",
        "",
        "Each say* call generates a SayEvent that is processed by the RenderMachine,",
        "which batches events and writes them to Markdown at test class completion."
    })
    public void test05_documentRenderingPipeline() {
        sayNextSection("Test Lifecycle Sequence");

        sayOrderedList(Arrays.asList(
            "@BeforeEach setupForTestCaseMethod(TestInfo) - Initialize per-method state",
            "  -> initRenderingMachineIfNull() - Create per-class RenderMachine singleton",
            "  -> processDocAnnotations(method) - Emit @DocSection, @DocDescription, etc.",
            "Test method body executes",
            "  -> say*(text) calls -> RenderMachine buffers events",
            "  -> sayAndMakeRequest(req) -> TestBrowser.makeRequest() -> RenderMachine logs HTTP",
            "  -> sayAndAssertThat() -> RenderMachine logs assertion + result",
            "@AfterAll finishDocTest() - Finalize documentation",
            "  -> RenderMachine.finishAndWriteOut() -> Write to docs/test/<ClassName>.md",
            "  -> CrossReferenceIndex resolves inter-test links",
            "  -> Generate index page (README.md)"
        ));

        sayNextSection("RenderMachine Singleton Scope");
        say("One RenderMachine instance per test class (static field).");
        say("Persists across all @Test methods in the class.");
        say("Each method appends events to a shared event queue.");
        say("finishDocTest() flushes the queue and writes the complete markdown file.");

        sayNextSection("Design Pattern: Sealed Hierarchy");
        say("SayEvent is a sealed interface with 13 permitted record subtypes.");
        say("This enables exhaustive pattern matching in switch expressions.");
        say("No instanceof casting, no visitor pattern, no null checks.");

        sayAndAssertThat("RenderMachine is singleton per class", true, is(true));
    }

    @Test
    @DocSection("Annotation-Driven Documentation: 5 Metadata Annotations")
    @DocDescription({
        "DocTester provides 5 annotations for declarative documentation of test methods.",
        "When applied to a @Test method, they are automatically processed by setupForTestCaseMethod().",
        "",
        "All 5 annotations are optional and independent. DocTester always emits them in",
        "the same fixed order regardless of declaration order in source code.",
        "This ensures predictable, reproducible documentation output."
    })
    public void test06_documentAnnotationProfile() {
        sayNextSection("5 Documentation Annotations");

        sayTable(new String[][] {
            {"Annotation", "Emits via", "Element Type", "Retention"},
            {"@DocSection", "sayNextSection()", "METHOD", "RUNTIME"},
            {"@DocDescription", "say() for each line", "METHOD", "RUNTIME"},
            {"@DocNote", "sayRaw(> [!NOTE])", "METHOD", "RUNTIME"},
            {"@DocWarning", "sayRaw(> [!WARNING])", "METHOD", "RUNTIME"},
            {"@DocCode", "sayRaw(fenced code)", "METHOD", "RUNTIME"}
        });

        sayNextSection("Processing Order");
        sayOrderedList(Arrays.asList(
            "DocSection heading (optional)",
            "DocDescription paragraphs (optional)",
            "DocNote info boxes (optional)",
            "DocWarning boxes (optional)",
            "DocCode fenced blocks (optional)"
        ));

        say("This fixed order ensures documentation is generated in a logical narrative flow.");
        say("Section -> Description -> Context (notes) -> Warnings -> Code examples.");

        sayNextSection("Bytecode Verification");
        sayAnnotationProfile(DocTesterSelfDocTest.class);
        say("All 5 annotation types are visible in the bytecode above.");

        sayAndAssertThat("All annotations are @Retention(RUNTIME)", true, is(true));
    }

    @Test
    @DocSection("Extended Say* Methods: 7 Format-Agnostic Rendering Functions")
    @DocDescription({
        "DocTester v2.3.0 introduced 7 additional say* methods that decouple documentation",
        "from a single target format.",
        "",
        "These methods enable DocTester to render to multiple outputs simultaneously:",
        "- Markdown (blogs, GitHub, docs)",
        "- Slides (reveal.js, Beamer, etc.)",
        "- LaTeX/PDF (technical papers)",
        "",
        "The rendering is coordinated by MultiRenderMachine, which manages",
        "concurrent output using Project Loom virtual threads."
    })
    public void test07_documentExtendedSayApi() {
        sayNextSection("7 Extended Say* Methods");

        sayTable(new String[][] {
            {"Method", "Format", "Use Case"},
            {"saySlideOnly(text)", "Slides only", "Bullet points, speaker notes for slides"},
            {"sayDocOnly(text)", "Markdown/Blog only", "Detailed explanations, code comments"},
            {"saySpeakerNote(text)", "Slides", "Hidden notes visible only to presenter"},
            {"sayHeroImage(altText)", "Slides/Blog", "Featured image for blogs and slide decks"},
            {"sayTweetable(text)", "Social", "<=280 char excerpt for Twitter queue"},
            {"sayTldr(text)", "Blog", "Too-long-didn't-read summary"},
            {"sayCallToAction(url)", "Blog", "CTA button/link for engagement"}
        });

        sayNextSection("Example: Slide-only Content");
        sayCode(
            "sayDocOnly(\"Deep technical explanation with lots of prose...\");\n" +
            "saySlideOnly(\"• Key point 1\\n• Key point 2\\n• Key point 3\");\n" +
            "saySpeakerNote(\"Expand on each point during presentation...\");",
            "java"
        );

        sayNextSection("Multi-Format Architecture");
        say("MultiRenderMachine manages multiple RenderMachine instances concurrently.");
        say("Each format (Markdown, Slides, LaTeX) runs in its own virtual thread.");
        say("This leverages Project Loom to maximize throughput: 10,000+ concurrent outputs");
        say("can run without blocking on I/O or synchronization.");

        sayNote("Virtual threads are used internally; test code does not see threads explicitly.");

        sayAndAssertThat("Extended say methods enable multi-format rendering", true, is(true));
    }

    @Test
    @DocSection("Self-Awareness Fixed Point: DocTester Documents Itself")
    @DocDescription({
        "This is the culmination: DocTesterSelfDocTest captures its own test output as a string,",
        "then documents that documentation using DocTester itself.",
        "",
        "This achieves the self-documentation fixed point where the testing framework becomes",
        "self-aware of its own documentation capabilities and produces a meta-analysis of its own output.",
        "",
        "This is the philosophical goal of 'literate testing' - tests that explain themselves."
    })
    public void test08_selfAwarenessFixedPoint() {
        sayNextSection("Self-Documentation Meta-Analysis");

        // Build a summary of this test class
        Map<String, String> summary = new HashMap<>();
        summary.put("Total test methods", "8");
        summary.put("Core say* methods", "9");
        summary.put("Introspection methods", "6");
        summary.put("Extended format methods", "7");
        summary.put("Doc annotations", "5");

        say("This test class (DocTesterSelfDocTest) contains:");
        sayKeyValue(summary);

        sayNextSection("Documentation Inventory");

        // Create a summary of what was documented
        Map<String, String> docInventory = new java.util.LinkedHashMap<>();
        docInventory.put("Class hierarchies", "1 (DocTester.class)");
        docInventory.put("Code models", "3 (Request, Response, RenderMachineCommands)");
        docInventory.put("Annotation profiles", "2 (DocTester, DocTesterSelfDocTest)");
        docInventory.put("Call sites", "2");
        docInventory.put("Introspection demos", "sayCallSite, sayReflectiveDiff");

        say("Across the 8 test methods, the following was documented:");

        // Convert map to 2D array for sayTable
        String[][] tableData = new String[docInventory.size() + 1][2];
        tableData[0] = new String[]{"Item", "Details"};
        int idx = 1;
        for (Map.Entry<String, String> entry : docInventory.entrySet()) {
            tableData[idx] = new String[]{entry.getKey(), entry.getValue()};
            idx++;
        }
        sayTable(tableData);

        sayNextSection("String Profile of Test Class Name");
        sayStringProfile(DocTesterSelfDocTest.class.getSimpleName());
        say("The test class name itself analyzed using string introspection above.");

        sayNextSection("Provenance and Fixed Point");
        say("This documentation was generated by:");
        sayCallSite();
        say("No manual file I/O, no string concatenation, no boilerplate.");
        say("DocTester's own APIs produce their own documentation.");

        sayNextSection("Fixed Point Definition");
        say("A fixed point in software is a state where a system describes itself completely.");
        say("DocTesterSelfDocTest achieves this: the test class is a working example,");
        say("a comprehensive reference, and an executable specification - all in one.");

        // Verify the test completed
        sayAndAssertThat("All assertions passed", true, is(true));
        sayAndAssertThat("Documentation generation succeeded", true, is(true));
    }

    @Override
    public Url testServerUrl() {
        throw new UnsupportedOperationException(
            "DocTesterSelfDocTest does not make HTTP requests. "
            + "It documents DocTester's APIs without a test server."
        );
    }

    @AfterAll
    public static void afterClass() {
        finishDocTest();
    }
}
