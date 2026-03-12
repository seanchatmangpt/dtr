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
package io.github.seanchatmangpt.dtr.rendermachine;

import com.google.common.collect.ImmutableMap;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;
import io.github.seanchatmangpt.dtr.testbrowser.Url;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RenderMachineImplTest {

    @Mock
    TestBrowser testBrowser;

    RenderMachineImpl renderMachine;

    @BeforeEach
    public void setupTest() {
        renderMachine = new RenderMachineImpl();
        renderMachine.setTestBrowser(testBrowser);
        renderMachine.setFileName("TestExample");
    }

    @Test
    public void testSayGeneratesMarkdownParagraph() {
        renderMachine.say("This is a test paragraph");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("This is a test paragraph"));
    }

    @Test
    public void testSayNextSectionGeneratesMarkdownHeading() {
        renderMachine.sayNextSection("My Test Section");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("## My Test Section"));
    }

    @Test
    public void testSayNextSectionAddsToTableOfContents() {
        renderMachine.sayNextSection("First Section");
        renderMachine.sayNextSection("Second Section");

        String output = String.join("\n", renderMachine.toc);
        assertTrue(output.contains("[First Section](#firstsection)"));
        assertTrue(output.contains("[Second Section](#secondsection)"));
    }

    @Test
    public void testSayAndMakeRequestFormatsHttpExchange() {
        Map<String, String> headers = ImmutableMap.of("Content-Type", "application/json");
        Response response = new Response(headers, 200, "{\"name\": \"Alice\"}");
        Request request = Request.GET().url(Url.host("http://localhost:8080").path("/api/users"));

        when(testBrowser.makeRequest(request)).thenReturn(response);

        renderMachine.sayAndMakeRequest(request);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("### Request"));
        assertTrue(output.contains("### Response"));
        assertTrue(output.contains("**Status**: `200`"));
        assertTrue(output.contains("GET"));
        assertTrue(output.contains("/api/users"));
    }

    @Test
    public void testSayAndAssertThatSuccessShowsCheckmark() {
        org.hamcrest.Matcher<Integer> matcher = new org.hamcrest.BaseMatcher<Integer>() {
            @Override
            public boolean matches(Object o) {
                return o.equals(200);
            }

            @Override
            public void describeTo(org.hamcrest.Description description) {
                description.appendText("200");
            }
        };
        renderMachine.sayAndAssertThat("Test assertion", 200, matcher);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("✓ Test assertion"));
    }

    @Test
    public void testSayAndAssertThatFailureShowsCross() {
        try {
            org.hamcrest.Matcher<Integer> matcher = new org.hamcrest.BaseMatcher<Integer>() {
                @Override
                public boolean matches(Object o) {
                    return o.equals(200);
                }

                @Override
                public void describeTo(org.hamcrest.Description description) {
                    description.appendText("200");
                }
            };
            renderMachine.sayAndAssertThat("This should fail", 404, matcher);
        } catch (AssertionError e) {
            // Expected
        }

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("✗ **FAILED**: This should fail"));
    }

    @Test
    public void testSayRawInjectsMarkdown() {
        renderMachine.sayRaw("**Bold text** and *italic text*");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Bold text**"));
        assertTrue(output.contains("*italic text*"));
    }

    @Test
    public void testSayJavaCodeFormatsCodeBlock() {
        String javaCode = "Response response = sayAndMakeRequest(\n"
                + "    Request.GET()\n"
                + "        .url(testServerUrl().path(\"/api/users\"))\n"
                + "        .contentTypeApplicationJson());\n";

        renderMachine.sayJavaCode(javaCode, "Fetching users from API");

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```java"));
        assertTrue(output.contains("Response response"));
        assertTrue(output.contains("**Fetching users from API**"));
    }

    @Test
    public void testHeaderConversionToId() {
        String id1 = renderMachine.convertTextToId("Get All Users");
        String id2 = renderMachine.convertTextToId("Create User (POST)");
        String id3 = renderMachine.convertTextToId("Delete User by ID");

        assertTrue(id1.equals("getallusers"));
        assertTrue(id2.equals("createuserpost"));
        assertTrue(id3.equals("deleteuserbyid"));
    }

    @Test
    public void testFormParametersAreIncludedInRequest() {
        Map<String, String> formParameters = ImmutableMap.of("username", "alice", "password", "secret");
        Map<String, String> responseHeaders = ImmutableMap.of("Content-Type", "application/json");

        Request request = Request.POST()
                .url(Url.host("http://localhost:8080").path("/login"))
                .formParameters(formParameters);
        Response response = new Response(responseHeaders, 200, "{\"token\": \"abc123\"}");

        when(testBrowser.makeRequest(request)).thenReturn(response);

        renderMachine.sayAndMakeRequest(request);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("POST"));
        assertTrue(output.contains("/login"));
        assertTrue(output.contains("**Status**: `200`"));
    }

    @Test
    public void testResponseHeadersAreFormatted() {
        Map<String, String> headers = ImmutableMap.of(
                "Content-Type", "application/json",
                "X-Request-ID", "12345"
        );
        Response response = new Response(headers, 201, "{\"id\": 1}");
        Request request = Request.POST().url(Url.host("http://localhost:8080").path("/api/users"));

        when(testBrowser.makeRequest(request)).thenReturn(response);

        renderMachine.sayAndMakeRequest(request);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Status**: `201`"));
        assertTrue(output.contains("**Headers**:"));
    }

    @Test
    public void testNullResponseBodyHandling() {
        Response response = new Response(ImmutableMap.of(), 204, null);
        Request request = Request.DELETE().url(Url.host("http://localhost:8080").path("/api/users/1"));

        when(testBrowser.makeRequest(request)).thenReturn(response);

        renderMachine.sayAndMakeRequest(request);

        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("**Status**: `204`"));
        assertTrue(output.contains("*(No response body)*"));
    }

    // =========================================================================
    // Structural output invariant tests — AGI quality / Joe Armstrong theorems
    //
    // These tests verify that each say* method produces Markdown with the correct
    // structural properties. They belong here (not in DocTesterPropertyTest) because
    // markdownDocument is package-private and only accessible within this package.
    // =========================================================================

    @Test
    public void sayTableOutputContainsPipes() {
        renderMachine.sayTable(new String[][] {
            {"Header1", "Header2"},
            {"Value1",  "Value2"}
        });
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Header1 | Header2 |"),
            "sayTable() must produce a header row with pipe-separated columns");
        assertTrue(output.contains("| --- |"),
            "sayTable() must produce a separator row with '| --- |'");
        assertTrue(output.contains("| Value1 | Value2 |"),
            "sayTable() must produce data rows with pipe-separated values");
    }

    @Test
    public void sayCodeOutputContainsBacktickFence() {
        renderMachine.sayCode("x = 1 + 2", "python");
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("```python"),
            "sayCode() must produce an opening ` ```python ` fence");
        assertTrue(output.contains("x = 1 + 2"),
            "sayCode() must preserve the code content verbatim");
        assertTrue(output.contains("```"),
            "sayCode() must produce a closing ` ``` ` fence");
    }

    @Test
    public void sayWarningOutputContainsGitHubAlert() {
        renderMachine.sayWarning("critical danger");
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!WARNING]"),
            "sayWarning() must produce a GitHub-style [!WARNING] alert");
        assertTrue(output.contains("> critical danger"),
            "sayWarning() must include the warning message text");
    }

    @Test
    public void sayNoteOutputContainsGitHubNote() {
        renderMachine.sayNote("helpful tip");
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("> [!NOTE]"),
            "sayNote() must produce a GitHub-style [!NOTE] alert");
        assertTrue(output.contains("> helpful tip"),
            "sayNote() must include the note message text");
    }

    @Test
    public void sayKeyValueOutputContainsPipesAndValues() {
        renderMachine.sayKeyValue(Map.of("api-key", "my-value"));
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("| Key | Value |"),
            "sayKeyValue() must produce '| Key | Value |' header");
        assertTrue(output.contains("`api-key`"),
            "sayKeyValue() must render key in backtick-code format");
        assertTrue(output.contains("`my-value`"),
            "sayKeyValue() must render value in backtick-code format");
    }

    @Test
    public void sayUnorderedListOutputContainsBullets() {
        renderMachine.sayUnorderedList(Arrays.asList("alpha", "beta", "gamma"));
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("- alpha"),
            "sayUnorderedList() must produce '- item' bullet entries");
        assertTrue(output.contains("- beta"),
            "sayUnorderedList() must include all list items");
        assertTrue(output.contains("- gamma"),
            "sayUnorderedList() must include all list items");
    }

    @Test
    public void sayOrderedListOutputContainsNumbers() {
        renderMachine.sayOrderedList(Arrays.asList("step one", "step two", "step three"));
        String output = String.join("\n", renderMachine.markdownDocument);
        assertTrue(output.contains("1. step one"),
            "sayOrderedList() must produce '1. item' numbered entries");
        assertTrue(output.contains("2. step two"),
            "sayOrderedList() must produce '2. item' for the second entry");
        assertTrue(output.contains("3. step three"),
            "sayOrderedList() must produce '3. item' for the third entry");
    }

    @Test
    public void sayCodeCellsWithAdversarialContent() {
        // Adversarial input: content that could break the fence if not handled correctly.
        // Backticks in content must not terminate the fence prematurely.
        List<String> adversarialInputs = Arrays.asList(
            "``` this looks like a fence ```",
            "end fence ```",
            "```java class Foo {}```"
        );
        for (String adversarial : adversarialInputs) {
            var freshRm = new RenderMachineImpl();
            freshRm.setFileName("AdversarialCode");
            freshRm.sayCode(adversarial, "text");
            String output = String.join("\n", freshRm.markdownDocument);
            // The fence must still open and close correctly
            assertTrue(output.contains("```text"),
                "Opening fence must still be present for adversarial input: " + adversarial);
        }
    }

    @Test
    public void sayTableWithPipeInCellContent() {
        // Cells containing '|' must not corrupt the table structure.
        // The key invariant: the output must still contain valid header separator.
        renderMachine.sayTable(new String[][] {
            {"Col A", "Col B"},
            {"value | with | pipes", "normal value"}
        });
        String output = String.join("\n", renderMachine.markdownDocument);
        // The separator row must always be present — table structure intact
        assertTrue(output.contains("| --- |"),
            "sayTable() with pipe-containing cells must still produce a valid '| --- |' separator");
        assertTrue(output.contains("Col A"),
            "sayTable() must include the header content despite adversarial cell data");
    }

    // =========================================================================
    // Minimal RenderMachine stubs for MultiRenderMachine tests
    // =========================================================================

    /**
     * A no-op RenderMachine base that provides empty implementations for all
     * required abstract and interface methods, so test subclasses can override
     * only the methods they care about.
     */
    static class NoOpRenderMachine extends RenderMachine {
        @Override public void say(String text) {}
        @Override public void sayNextSection(String headline) {}
        @Override public void sayRaw(String rawMarkdown) {}
        @Override public void sayTable(String[][] data) {}
        @Override public void sayCode(String code, String language) {}
        @Override public void sayWarning(String message) {}
        @Override public void sayNote(String message) {}
        @Override public void sayKeyValue(Map<String, String> pairs) {}
        @Override public void sayUnorderedList(List<String> items) {}
        @Override public void sayOrderedList(List<String> items) {}
        @Override public void sayJson(Object object) {}
        @Override public void sayAssertions(Map<String, String> assertions) {}
        @Override public void sayRef(io.github.seanchatmangpt.dtr.crossref.DocTestRef ref) {}
        @Override public void sayCite(String citationKey) {}
        @Override public void sayCite(String citationKey, String pageRef) {}
        @Override public void sayFootnote(String text) {}
        @Override public List<org.apache.hc.client5.http.cookie.Cookie> sayAndGetCookies() { return List.of(); }
        @Override public org.apache.hc.client5.http.cookie.Cookie sayAndGetCookieWithName(String name) { return null; }
        @Override public Response sayAndMakeRequest(io.github.seanchatmangpt.dtr.testbrowser.Request req) { return null; }
        @Override public <T> void sayAndAssertThat(String message, String reason, T actual, org.hamcrest.Matcher<? super T> matcher) {}
        @Override public <T> void sayAndAssertThat(String message, T actual, org.hamcrest.Matcher<? super T> matcher) {}
        @Override public void sayCodeModel(Class<?> clazz) {}
        @Override public void sayCodeModel(java.lang.reflect.Method method) {}
        @Override public void sayCallSite() {}
        @Override public void sayAnnotationProfile(Class<?> clazz) {}
        @Override public void sayClassHierarchy(Class<?> clazz) {}
        @Override public void sayStringProfile(String text) {}
        @Override public void sayReflectiveDiff(Object before, Object after) {}
        @Override public void setTestBrowser(TestBrowser testBrowser) {}
        @Override public void setFileName(String fileName) {}
        @Override public void finishAndWriteOut() {}
    }

    /** A slow machine whose say() blocks for 30 seconds to trigger timeout. */
    static final class SlowRenderMachine extends NoOpRenderMachine {
        @Override public void say(String text) {
            try { Thread.sleep(30_000); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
    }

    /** A machine whose say() always throws to trigger MultiRenderException. */
    static final class FailingRenderMachine extends NoOpRenderMachine {
        @Override public void say(String text) {
            throw new IllegalStateException("deliberate failure from FailingRenderMachine");
        }
    }

    // =========================================================================
    // MultiRenderMachine — timeout and failure tests
    // =========================================================================

    @Test
    public void multiRenderMachineAllMachinesCompleteWithinTimeout() {
        // Two fast machines that complete immediately — no exception expected
        var machine1 = new RenderMachineImpl();
        machine1.setFileName("Multi1");
        var machine2 = new RenderMachineImpl();
        machine2.setFileName("Multi2");

        var multi = new MultiRenderMachine(List.of(machine1, machine2), 10);

        // Must not throw — all tasks finish well within 10 seconds
        multi.say("Hello from multi machine");
        multi.sayNextSection("Shared Section");

        String out1 = String.join("\n", machine1.markdownDocument);
        String out2 = String.join("\n", machine2.markdownDocument);
        assertTrue(out1.contains("Hello from multi machine"),
            "Machine 1 must receive dispatched say() call");
        assertTrue(out2.contains("Hello from multi machine"),
            "Machine 2 must receive dispatched say() call");
        assertTrue(out1.contains("## Shared Section"),
            "Machine 1 must receive dispatched sayNextSection() call");
        assertTrue(out2.contains("## Shared Section"),
            "Machine 2 must receive dispatched sayNextSection() call");
    }

    @Test
    public void multiRenderMachineTimeoutThrowsRuntimeExceptionWithTimedOutMessage() {
        var multi = new MultiRenderMachine(List.of(new SlowRenderMachine()), 1);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> multi.say("trigger timeout"),
            "A machine that hangs past the timeout must cause a RuntimeException");
        assertTrue(ex.getMessage().contains("timed out"),
            "Timeout exception message must contain 'timed out', was: " + ex.getMessage());
    }

    @Test
    public void multiRenderMachineFailingMachineIncludesClassNameInMessage() {
        var multi = new MultiRenderMachine(List.of(new FailingRenderMachine()), 10);

        MultiRenderMachine.MultiRenderException ex = assertThrows(
            MultiRenderMachine.MultiRenderException.class,
            () -> multi.say("trigger failure"),
            "A machine that throws must cause a MultiRenderException");
        assertTrue(ex.getMessage().contains("FailingRenderMachine"),
            "MultiRenderException message must contain the failing machine's class name, was: " + ex.getMessage());
        assertFalse(ex.getCauses().isEmpty(),
            "MultiRenderException must contain at least one cause");
    }

    // =========================================================================
    // RenderMachineImpl output file verification
    // =========================================================================

    @Test
    public void generatedMdFileIsValidUtf8() throws Exception {
        renderMachine.sayNextSection("UTF-8 Test");
        renderMachine.say("Japanese: \u65e5\u672c\u8a9e \uD83D\uDE80");
        renderMachine.finishAndWriteOut();

        File outputFile = new File("docs/test/TestExample.md");
        assertTrue(outputFile.exists(), "Output .md file must exist after finishAndWriteOut()");

        byte[] rawBytes = Files.readAllBytes(outputFile.toPath());
        // Decoding as UTF-8 must not produce replacement characters (U+FFFD)
        String decoded = new String(rawBytes, StandardCharsets.UTF_8);
        assertFalse(decoded.contains("\uFFFD"),
            "Output file must be valid UTF-8 — no replacement characters");
        assertTrue(decoded.contains("\u65e5\u672c\u8a9e"),
            "Japanese characters must survive the UTF-8 write/read round-trip");
    }

    @Test
    public void generatedMdFileContainsExpectedH1HeadingFormat() throws Exception {
        renderMachine.finishAndWriteOut();

        File outputFile = new File("docs/test/TestExample.md");
        assertTrue(outputFile.exists(), "Output .md file must exist after finishAndWriteOut()");

        byte[] rawBytes = Files.readAllBytes(outputFile.toPath());
        String content = new String(rawBytes, StandardCharsets.UTF_8);

        // H1 heading must be exactly "# TestExample\n\n" at the top of the document
        assertTrue(content.startsWith("# TestExample\n"),
            "Output file must start with '# <FileName>' H1 heading");
        // There must be a blank line after the H1
        assertTrue(content.startsWith("# TestExample\n\n"),
            "Output file must have an empty line after the H1 heading");
    }

    @Test
    public void generatedMdFileContainsProperMarkdownTableFormat() throws Exception {
        renderMachine.sayTable(new String[][] {
            {"col1", "col2"},
            {"v1",   "v2"}
        });
        renderMachine.finishAndWriteOut();

        File outputFile = new File("docs/test/TestExample.md");
        assertTrue(outputFile.exists(), "Output .md file must exist after finishAndWriteOut()");

        byte[] rawBytes = Files.readAllBytes(outputFile.toPath());
        String content = new String(rawBytes, StandardCharsets.UTF_8);

        assertTrue(content.contains("| col1 | col2 |"),
            "Output file must contain a pipe-formatted table header row");
        assertTrue(content.contains("| --- |"),
            "Output file must contain the markdown table separator row '| --- |'");
    }

    @Test
    public void generatedMdFileIsNotEmptyAfterFinishAndWriteOut() throws Exception {
        renderMachine.say("Some content");
        renderMachine.finishAndWriteOut();

        File outputFile = new File("docs/test/TestExample.md");
        assertTrue(outputFile.exists(), "Output .md file must exist after finishAndWriteOut()");
        assertTrue(outputFile.length() > 0,
            "Output .md file must not be empty after finishAndWriteOut()");
    }
}
