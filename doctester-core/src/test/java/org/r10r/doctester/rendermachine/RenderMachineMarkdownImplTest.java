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
package org.r10r.doctester.rendermachine;

import com.google.common.collect.ImmutableMap;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.testbrowser.Url;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class RenderMachineMarkdownImplTest {

    @Mock
    TestBrowser testBrowser;

    RenderMachineMarkdownImpl renderMachine;

    @Before
    public void setupTest() {
        renderMachine = new RenderMachineMarkdownImpl();
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
        String id1 = renderMachine.convertHeadingToId("Get All Users");
        String id2 = renderMachine.convertHeadingToId("Create User (POST)");
        String id3 = renderMachine.convertHeadingToId("Delete User by ID");

        assert id1.equals("getallusers");
        assert id2.equals("createuserpost");
        assert id3.equals("deleteuserbyid");
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
}
