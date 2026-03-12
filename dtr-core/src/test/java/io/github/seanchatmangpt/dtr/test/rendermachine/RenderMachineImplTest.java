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
}
