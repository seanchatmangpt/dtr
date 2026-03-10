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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hc.client5.http.cookie.Cookie;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.hamcrest.Matcher;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Markdown-based render machine for generating portable API documentation.
 *
 * Converts test execution into clean markdown files suitable for GitHub,
 * documentation platforms, and static site generators. No HTML/CSS/JS
 * dependencies—just clean, portable markdown.
 */
public class RenderMachineImpl implements RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineImpl.class);

    private static final String BASE_DIR = "target/docs";
    private static final String INDEX_FILE = "README";

    final List<String> sections = new ArrayList<>();
    final List<String> toc = new ArrayList<>();
    List<String> markdownDocument = new ArrayList<>();
    private TestBrowser testBrowser;
    private String fileName;

    public RenderMachineImpl() {
    }

    @Override
    public void say(String text) {
        markdownDocument.add("");
        markdownDocument.add(text);
    }

    @Override
    public void sayNextSection(String heading) {
        sections.add(heading);
        String anchorId = convertTextToId(heading);
        toc.add(String.format("- [%s](#%s)", heading, anchorId));

        markdownDocument.add("");
        markdownDocument.add("## " + heading);
    }

    @Override
    public List<Cookie> sayAndGetCookies() {
        List<Cookie> cookies = testBrowser.getCookies();
        markdownDocument.add("");
        markdownDocument.add("### Cookies");
        for (Cookie cookie : cookies) {
            markdownDocument.add(String.format("- **%s**: `%s` (path: %s, domain: %s)",
                    cookie.getName(), cookie.getValue(), cookie.getPath(), cookie.getDomain()));
        }
        return cookies;
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        Cookie cookie = testBrowser.getCookieWithName(name);
        markdownDocument.add("");
        markdownDocument.add("### Cookie: " + name);
        if (cookie != null) {
            markdownDocument.add(String.format("- **Value**: `%s`", cookie.getValue()));
            markdownDocument.add(String.format("- **Path**: `%s`", cookie.getPath()));
            markdownDocument.add(String.format("- **Domain**: `%s`", cookie.getDomain()));
        }
        return cookie;
    }

    @Override
    public Response sayAndMakeRequest(Request request) {
        Response response = testBrowser.makeRequest(request);
        formatHttpExchange(request, response);
        return response;
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        sayAndAssertThat(message, "", actual, matcher);
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        try {
            Assert.assertThat(reason, actual, matcher);
            markdownDocument.add("");
            markdownDocument.add("✓ " + message);
        } catch (AssertionError e) {
            markdownDocument.add("");
            markdownDocument.add("✗ **FAILED**: " + message);
            markdownDocument.add("");
            markdownDocument.add("```");
            markdownDocument.add(convertStackTraceToString(e));
            markdownDocument.add("```");
            throw e;
        }
    }

    @Override
    public void sayRaw(String markdown) {
        markdownDocument.add(markdown);
    }

    /**
     * Add a Java code example to the documentation.
     *
     * Useful for showing the test code that generates the HTTP requests/responses.
     *
     * @param javaCode the Java code to include (as a string)
     * @param description optional description of what the code does
     */
    public void sayJavaCode(String javaCode, String description) {
        markdownDocument.add("");
        if (description != null && !description.isEmpty()) {
            markdownDocument.add("**" + description + "**");
        }
        markdownDocument.add("");
        markdownDocument.add("```java");
        for (String line : javaCode.split("\n")) {
            markdownDocument.add(line);
        }
        markdownDocument.add("```");
    }

    @Override
    public void setTestBrowser(TestBrowser testBrowser) {
        this.testBrowser = testBrowser;
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void finishAndWriteOut() {
        createTestDocumentationFile();
        createIndexFile();
    }

    private void createTestDocumentationFile() {
        List<String> doc = new ArrayList<>();

        doc.add("# " + fileName);
        doc.add("");

        if (!toc.isEmpty()) {
            doc.add("## Table of Contents");
            doc.add("");
            doc.addAll(toc);
            doc.add("");
        }

        doc.addAll(markdownDocument);

        doc.add("");
        doc.add("---");
        doc.add("*Generated by [DocTester](http://www.doctester.org)*");

        writeMarkdownFile(doc, fileName);
    }

    private void createIndexFile() {
        File dir = new File(BASE_DIR);
        File[] files = dir.listFiles((d, name) -> name.endsWith(".md") && !name.equals(INDEX_FILE + ".md"));

        if (files == null || files.length == 0) {
            return;
        }

        Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));

        List<String> index = new ArrayList<>();
        index.add("# API Documentation");
        index.add("");
        index.add("Generated by [DocTester](http://www.doctester.org)");
        index.add("");
        index.add("## Tests");
        index.add("");

        for (File file : files) {
            String name = file.getName();
            String baseName = name.substring(0, name.length() - 3); // remove .md
            index.add(String.format("- [%s](%s)", baseName, name));
        }

        writeMarkdownFile(index, INDEX_FILE);
    }

    private void writeMarkdownFile(List<String> lines, String fileNameWithoutExtension) {
        File outputDir = new File(BASE_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(BASE_DIR + File.separator + fileNameWithoutExtension + ".md");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.write('\n');
            }
        } catch (IOException e) {
            logger.error("Error writing markdown file: {}", outputFile, e);
        }
    }

    private void formatHttpExchange(Request request, Response response) {
        markdownDocument.add("");
        markdownDocument.add("### Request");
        markdownDocument.add("");

        String httpMethod = request.httpRequestType;
        String url = request.uri.toString();
        markdownDocument.add("```");
        markdownDocument.add(httpMethod + " " + url);

        for (Entry<String, String> header : request.headers.entrySet()) {
            markdownDocument.add(header.getKey() + ": " + header.getValue());
        }

        if (request.payload != null) {
            markdownDocument.add("");
            markdownDocument.add(request.payloadAsPrettyString());
        }

        markdownDocument.add("```");

        markdownDocument.add("");
        markdownDocument.add("### Response");
        markdownDocument.add("");
        markdownDocument.add("**Status**: `" + response.httpStatus + "`");
        markdownDocument.add("");

        if (!response.headers.isEmpty()) {
            markdownDocument.add("**Headers**:");
            for (Entry<String, String> header : response.headers.entrySet()) {
                markdownDocument.add("- `" + header.getKey() + ": " + header.getValue() + "`");
            }
            markdownDocument.add("");
        }

        if (response.payload != null) {
            markdownDocument.add("**Body**:");
            markdownDocument.add("");
            markdownDocument.add("```json");
            markdownDocument.add(response.payloadAsPrettyString());
            markdownDocument.add("```");
        } else {
            markdownDocument.add("*(No response body)*");
        }
    }

    public String convertTextToId(String text) {
        return text.toLowerCase()
                .replaceAll("[^a-z0-9]", "");
    }

    private String convertStackTraceToString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName());
        if (throwable.getMessage() != null) {
            sb.append(": ").append(throwable.getMessage());
        }
        sb.append('\n');

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element).append('\n');
        }

        return sb.toString();
    }
}
