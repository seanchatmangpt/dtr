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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.cookie.Cookie;
import io.github.seanchatmangpt.dtr.bibliography.BibliographyManager;
import io.github.seanchatmangpt.dtr.bibliography.BibTeXRenderer;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;
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
public final class RenderMachineImpl extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineImpl.class);

    private static final String BASE_DIR = "docs/test";
    private static final String INDEX_FILE = "README";
    private static final ObjectMapper objectMapper = new ObjectMapper();

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
        toc.add("- [%s](#%s)".formatted(heading, anchorId));

        markdownDocument.add("");
        markdownDocument.add("## " + heading);
    }

    @Override
    public List<Cookie> sayAndGetCookies() {
        List<Cookie> cookies = testBrowser.getCookies();
        markdownDocument.add("");
        markdownDocument.add("### Cookies");
        for (Cookie cookie : cookies) {
            markdownDocument.add("- **%s**: `%s` (path: %s, domain: %s)".formatted(
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
            markdownDocument.add("- **Value**: `%s`".formatted(cookie.getValue()));
            markdownDocument.add("- **Path**: `%s`".formatted(cookie.getPath()));
            markdownDocument.add("- **Domain**: `%s`".formatted(cookie.getDomain()));
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
    public void sayTable(String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }

        markdownDocument.add("");

        // Generate header row with separator
        String[] header = data[0];
        StringBuilder headerRow = new StringBuilder("|");
        StringBuilder separator = new StringBuilder("|");

        for (String cell : header) {
            headerRow.append(" ").append(cell).append(" |");
            separator.append(" --- |");
        }

        markdownDocument.add(headerRow.toString());
        markdownDocument.add(separator.toString());

        // Add data rows
        for (int i = 1; i < data.length; i++) {
            String[] row = data[i];
            StringBuilder rowStr = new StringBuilder("|");
            for (String cell : row) {
                rowStr.append(" ").append(cell == null ? "" : cell).append(" |");
            }
            markdownDocument.add(rowStr.toString());
        }
    }

    @Override
    public void sayCode(String code, String language) {
        markdownDocument.add("");
        markdownDocument.add("```" + (language != null && !language.isEmpty() ? language : ""));
        if (code != null) {
            for (String line : code.split("\n")) {
                markdownDocument.add(line);
            }
        }
        markdownDocument.add("```");
    }

    @Override
    public void sayWarning(String message) {
        markdownDocument.add("");
        if (message != null && !message.isEmpty()) {
            markdownDocument.add("> [!WARNING]");
            markdownDocument.add("> " + message);
        }
    }

    @Override
    public void sayNote(String message) {
        markdownDocument.add("");
        if (message != null && !message.isEmpty()) {
            markdownDocument.add("> [!NOTE]");
            markdownDocument.add("> " + message);
        }
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return;
        }

        markdownDocument.add("");
        markdownDocument.add("| Key | Value |");
        markdownDocument.add("| --- | --- |");

        for (Entry<String, String> entry : pairs.entrySet()) {
            String key = entry.getKey() != null ? entry.getKey() : "";
            String value = entry.getValue() != null ? entry.getValue() : "";
            markdownDocument.add("| `" + key + "` | `" + value + "` |");
        }
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        markdownDocument.add("");
        for (String item : items) {
            markdownDocument.add("- " + (item != null ? item : ""));
        }
    }

    @Override
    public void sayOrderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        markdownDocument.add("");
        for (int i = 0; i < items.size(); i++) {
            String item = items.get(i);
            markdownDocument.add((i + 1) + ". " + (item != null ? item : ""));
        }
    }

    @Override
    public void sayJson(Object object) {
        if (object == null) {
            return;
        }

        markdownDocument.add("");
        markdownDocument.add("```json");
        try {
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
            for (String line : jsonString.split("\n")) {
                markdownDocument.add(line);
            }
        } catch (Exception e) {
            logger.warn("Failed to serialize object to JSON", e);
            markdownDocument.add(object.toString());
        }
        markdownDocument.add("```");
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return;
        }

        markdownDocument.add("");
        markdownDocument.add("| Check | Result |");
        markdownDocument.add("| --- | --- |");

        for (Entry<String, String> entry : assertions.entrySet()) {
            String check = entry.getKey() != null ? entry.getKey() : "";
            String result = entry.getValue() != null ? entry.getValue() : "";
            markdownDocument.add("| " + check + " | `" + result + "` |");
        }
    }

    @Override
    public void sayCite(String citationKey) {
        markdownDocument.add("[cite: " + citationKey + "]");
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        markdownDocument.add("[cite: " + citationKey + ", p. " + pageRef + "]");
    }

    @Override
    public void sayFootnote(String text) {
        markdownDocument.add("[^1]: " + text);
    }

    @Override
    public void sayRef(DocTestRef ref) {
        if (ref == null) {
            return;
        }

        markdownDocument.add("");
        String linkText = ref.toString();
        String docFileName = ref.docTestClassName();
        String anchor = ref.anchor();
        // Render as markdown link: [See ApiControllerDocTest#user-creation](../OtherTest.md#anchor)
        markdownDocument.add("[%s](../%s.md#%s)".formatted(linkText, docFileName, anchor));
    }

    @Override
    public void sayRaw(String markdown) {
        markdownDocument.add(markdown);
    }

    /**
     * Documents a class's structure using Java reflection — the DocTester stand-in for
     * Project Babylon's Code Reflection API (JEP 494).
     *
     * <p>Renders the class's sealed hierarchy, record components, and all public method
     * signatures derived directly from the bytecode. The documentation cannot drift
     * from the implementation because it IS the implementation.</p>
     *
     * <p>Demonstrates Java 25/26 features:</p>
     * <ul>
     *   <li>Guarded switch expression for class kind detection</li>
     *   <li>{@code Class.getPermittedSubclasses()} for sealed hierarchies</li>
     *   <li>{@code Class.getRecordComponents()} for record inspection</li>
     *   <li>{@code var} + streams for method signature rendering</li>
     *   <li>Text block for the method signature template</li>
     * </ul>
     *
     * @param clazz the class to introspect and document
     */
    @Override
    public void sayCodeModel(Class<?> clazz) {
        markdownDocument.add("");
        markdownDocument.add("### Code Model: `" + clazz.getSimpleName() + "`");
        markdownDocument.add("");

        // Guarded switch expression for class kind detection
        // No instanceof chains. No if/else. The compiler knows these are exhaustive.
        String kind = switch (clazz) {
            case Class<?> c when c.isRecord()    -> "record";
            case Class<?> c when c.isInterface() -> "interface";
            case Class<?> c when c.isEnum()      -> "enum";
            default                              -> "class";
        };
        markdownDocument.add("**Kind**: `" + kind + "`");
        markdownDocument.add("");

        // Sealed hierarchy — getPermittedSubclasses() is the reflection API for sealed types
        // This is the runtime mirror of the compile-time `permits` clause
        if (clazz.isSealed()) {
            markdownDocument.add("**Sealed permits:**");
            for (var permitted : clazz.getPermittedSubclasses()) {
                String permittedKind = switch (permitted) {
                    case Class<?> c when c.isRecord()    -> "record";
                    case Class<?> c when c.isInterface() -> "interface";
                    default                              -> "class";
                };
                markdownDocument.add("- `" + permittedKind + " " + permitted.getSimpleName() + "`");
            }
            markdownDocument.add("");
        }

        // Record components — getRecordComponents() introspects the record's canonical constructor
        // Records are nominally immutable: components are final, accessor methods are generated
        if (clazz.isRecord()) {
            var components = clazz.getRecordComponents();
            if (components != null && components.length > 0) {
                markdownDocument.add("**Record components:**");
                for (var component : components) {
                    markdownDocument.add(
                        "- `%s %s`".formatted(
                            component.getType().getSimpleName(),
                            component.getName()));
                }
                markdownDocument.add("");
            }
        }

        // Public methods — the formal API surface, sorted alphabetically for stability
        // Uses var + streams — local type inference where the right-hand side is obvious
        var publicMethods = Arrays.stream(clazz.getDeclaredMethods())
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .sorted(Comparator.comparing(java.lang.reflect.Method::getName))
            .toList();

        if (!publicMethods.isEmpty()) {
            markdownDocument.add("**Public methods:**");
            markdownDocument.add("");
            markdownDocument.add("```java");
            for (var method : publicMethods) {
                var params = Arrays.stream(method.getParameters())
                    .map(p -> p.getType().getSimpleName() + " " + p.getName())
                    .collect(Collectors.joining(", "));
                markdownDocument.add(
                    "%s %s(%s)".formatted(
                        method.getReturnType().getSimpleName(),
                        method.getName(),
                        params));
            }
            markdownDocument.add("```");
        }
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
            index.add("- [%s](%s)".formatted(baseName, name));
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

    @Override
    public void sayCallSite() {
        markdownDocument.add("");
        markdownDocument.add("### Call Site");
        markdownDocument.add("");

        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
            .walk(frames -> frames
                .filter(f -> !f.getClassName().startsWith("io.github.seanchatmangpt.dtr.rendermachine"))
                .filter(f -> !f.getClassName().startsWith("io.github.seanchatmangpt.dtr.DocTester"))
                .filter(f -> !f.getClassName().startsWith("java.lang.reflect"))
                .filter(f -> !f.getClassName().startsWith("sun.reflect"))
                .findFirst())
            .ifPresent(frame -> {
                markdownDocument.add(
                    "**Generated by:** `%s.%s()` at line %d".formatted(
                        frame.getClassName(), frame.getMethodName(), frame.getLineNumber()));
                if (frame.getFileName() != null) {
                    markdownDocument.add("**Source file:** `%s`".formatted(frame.getFileName()));
                }
            });
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        markdownDocument.add("");
        markdownDocument.add("### Annotation Profile: `" + clazz.getSimpleName() + "`");
        markdownDocument.add("");

        var classAnnotations = clazz.getAnnotations();
        if (classAnnotations.length > 0) {
            markdownDocument.add("**Class-level annotations:**");
            for (var a : classAnnotations) {
                markdownDocument.add("- `@" + a.annotationType().getSimpleName() + "`");
            }
            markdownDocument.add("");
        }

        var annotatedMethods = Arrays.stream(clazz.getDeclaredMethods())
            .filter(m -> m.getAnnotations().length > 0)
            .sorted(Comparator.comparing(java.lang.reflect.Method::getName))
            .toList();

        if (!annotatedMethods.isEmpty()) {
            markdownDocument.add("**Method annotations:**");
            markdownDocument.add("");
            markdownDocument.add("| Method | Annotations |");
            markdownDocument.add("| --- | --- |");
            for (var method : annotatedMethods) {
                var annotations = Arrays.stream(method.getAnnotations())
                    .map(a -> "`@" + a.annotationType().getSimpleName() + "`")
                    .collect(Collectors.joining(", "));
                markdownDocument.add("| `%s()` | %s |".formatted(method.getName(), annotations));
            }
        } else {
            markdownDocument.add("*(No method-level annotations found)*");
        }
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        markdownDocument.add("");
        markdownDocument.add("### Class Hierarchy: `" + clazz.getSimpleName() + "`");
        markdownDocument.add("");

        // Build the superclass chain from Object → clazz
        var chain = new ArrayList<Class<?>>();
        Class<?> current = clazz;
        while (current != null) {
            chain.add(current);
            current = current.getSuperclass();
        }
        Collections.reverse(chain);

        // Render as indented tree
        for (int i = 0; i < chain.size(); i++) {
            String indent = "  ".repeat(i);
            String prefix = i == 0 ? "" : "↳ ";
            markdownDocument.add(indent + prefix + "`" + chain.get(i).getSimpleName() + "`");
        }

        // Implemented interfaces
        var interfaces = clazz.getInterfaces();
        if (interfaces.length > 0) {
            markdownDocument.add("");
            markdownDocument.add("**Implements:**");
            for (var iface : interfaces) {
                markdownDocument.add("- `" + iface.getSimpleName() + "`");
            }
        }
    }

    @Override
    public void sayStringProfile(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        markdownDocument.add("");
        markdownDocument.add("### String Profile");
        markdownDocument.add("");

        long words = Arrays.stream(text.trim().split("\\s+"))
            .filter(w -> !w.isEmpty())
            .count();
        long lines = text.lines().count();
        long uniqueChars = text.chars().distinct().count();
        long letters = text.chars().filter(Character::isLetter).count();
        long digits = text.chars().filter(Character::isDigit).count();
        long spaces = text.chars().filter(Character::isWhitespace).count();
        long nonAscii = text.chars().filter(c -> c > 127).count();

        markdownDocument.add("| Metric | Value |");
        markdownDocument.add("| --- | --- |");
        markdownDocument.add("| Total length | `%d` |".formatted(text.length()));
        markdownDocument.add("| Words | `%d` |".formatted(words));
        markdownDocument.add("| Lines | `%d` |".formatted(lines));
        markdownDocument.add("| Unique characters | `%d` |".formatted(uniqueChars));
        markdownDocument.add("| Letters | `%d` |".formatted(letters));
        markdownDocument.add("| Digits | `%d` |".formatted(digits));
        markdownDocument.add("| Whitespace | `%d` |".formatted(spaces));
        markdownDocument.add("| Non-ASCII (Unicode) | `%d` |".formatted(nonAscii));
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        if (before == null || after == null) {
            return;
        }

        markdownDocument.add("");

        if (!before.getClass().equals(after.getClass())) {
            markdownDocument.add("> [!WARNING]");
            markdownDocument.add("> Cannot diff objects of different types: `%s` vs `%s`".formatted(
                before.getClass().getSimpleName(), after.getClass().getSimpleName()));
            return;
        }

        markdownDocument.add("### Reflective Diff: `" + before.getClass().getSimpleName() + "`");
        markdownDocument.add("");
        markdownDocument.add("| Field | Before | After | Changed |");
        markdownDocument.add("| --- | --- | --- | --- |");

        var fields = before.getClass().getDeclaredFields();
        boolean anyChanged = false;
        for (var field : fields) {
            field.setAccessible(true);
            try {
                Object beforeVal = field.get(before);
                Object afterVal = field.get(after);
                boolean changed = !java.util.Objects.equals(beforeVal, afterVal);
                if (changed) anyChanged = true;
                String status = changed ? "**changed**" : "";
                markdownDocument.add("| `%s` | `%s` | `%s` | %s |".formatted(
                    field.getName(),
                    beforeVal != null ? beforeVal.toString() : "null",
                    afterVal != null ? afterVal.toString() : "null",
                    status));
            } catch (IllegalAccessException e) {
                // skip inaccessible fields
            }
        }

        if (fields.length == 0) {
            markdownDocument.add("*(no declared fields to compare)*");
        }
        markdownDocument.add("");
        markdownDocument.add(anyChanged
            ? "> **Objects differ** — changed fields highlighted above."
            : "> **Objects are equal** — no field differences detected.");
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
