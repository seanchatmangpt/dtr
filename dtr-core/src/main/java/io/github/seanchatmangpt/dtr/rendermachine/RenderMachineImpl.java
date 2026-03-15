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
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import io.github.seanchatmangpt.dtr.benchmark.BenchmarkRunner;
import io.github.seanchatmangpt.dtr.contract.ContractVerifier;
import io.github.seanchatmangpt.dtr.coverage.CoverageRow;
import io.github.seanchatmangpt.dtr.coverage.DocCoverageAnalyzer;
import io.github.seanchatmangpt.dtr.diagram.CallGraphBuilder;
import io.github.seanchatmangpt.dtr.diagram.ClassDiagramGenerator;
import io.github.seanchatmangpt.dtr.diagram.CodeModelAnalyzer;
import io.github.seanchatmangpt.dtr.diagram.ControlFlowGraphBuilder;
import io.github.seanchatmangpt.dtr.evolution.GitHistoryReader;
import io.github.seanchatmangpt.dtr.javadoc.JavadocEntry;
import io.github.seanchatmangpt.dtr.javadoc.JavadocIndex;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seanchatmangpt.dtr.bibliography.BibliographyManager;
import io.github.seanchatmangpt.dtr.bibliography.BibTeXRenderer;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Markdown-based render machine implementation for generating portable API documentation.
 *
 * <p>Converts test execution into clean, portable markdown files suitable for GitHub,
 * documentation platforms, and static site generators. Produces self-contained markdown
 * with no external HTML/CSS/JavaScript dependencies.</p>
 *
 * <p><strong>Output Format:</strong></p>
 * <ul>
 *   <li>Auto-generated table of contents</li>
 *   <li>Markdown tables for data</li>
 *   <li>Syntax-highlighted code blocks</li>
 *   <li>GitHub-style alert boxes ([!NOTE], [!WARNING])</li>
 *   <li>Formatted JSON payloads</li>
 *   <li>HTTP request/response documentation</li>
 *   <li>Cross-references to other DocTests</li>
 * </ul>
 *
 * <p><strong>Output Location:</strong></p>
 * <p>Files are written to {@code docs/test/} directory (relative to project root):
 * <ul>
 *   <li>{@code &lt;TestClassName&gt;.md} - main documentation file</li>
 *   <li>{@code &lt;TestClassName&gt;.json} - structured event data</li>
 *   <li>{@code README.md} - index of all DocTest outputs</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * RenderMachine markdown = new RenderMachineImpl();
 * markdown.setFileName("UserApiDocTest");
 * markdown.sayNextSection("User Registration");
 * markdown.say("Creates a new user account via POST /api/users");
 * markdown.sayTable(new String[][] {
 *     {"Field", "Type", "Required"},
 *     {"email", "String", "Yes"},
 *     {"password", "String", "Yes"}
 * });
 *
 *
 * markdown.finishAndWriteOut();  // Write to disk
 * }</pre>
 *
 * <p><strong>Design Note:</strong></p>
 * <p>This is a {@code final} class to enable JIT devirtualization of all method calls.
 * While RenderMachine is not sealed (due to multi-package implementations), all
 * concrete implementations are {@code final} for performance.</p>
 *
 * @since 1.0.0
 * @see RenderMachine for interface contract
 * @see RenderMachineCommands for documentation API
 */
public final class RenderMachineImpl extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineImpl.class);

    private static final String BASE_DIR = "docs/test";
    private static final String INDEX_FILE = "README";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** Accumulated section headings for table of contents. */
    final List<String> sections = new ArrayList<>();

    /** Table of contents entries (markdown list format). */
    final List<String> toc = new ArrayList<>();

    /** Accumulated markdown document lines. */
    List<String> markdownDocument = new ArrayList<>();

    /** Output filename (typically the test class name). */
    private String fileName;

    /**
     * Creates a new RenderMachineImpl with empty documentation.
     */
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
     * Documents a class's structure using Java reflection — the DTR stand-in for
     * Project Babylon's Code Reflection API (JEP 494).
     *
     * <p>Renders the class's sealed hierarchy, record components, and all public method
     * signatures derived directly from the bytecode. The documentation cannot drift
     * from the implementation because it IS the implementation.</p>
     *
     * <p>Demonstrates Java 26/26 features:</p>
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
        doc.add("*Generated by [DTR](http://www.dtr.org)*");

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
        index.add("Generated by [DTR](http://www.dtr.org)");
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
            throw new RuntimeException(
                "DTR failed to write documentation file: " + outputFile.getAbsolutePath(), e);
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
                .filter(f -> !f.getClassName().startsWith("io.github.seanchatmangpt.dtr.DTR"))
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

    // =========================================================================
    // Java 26 Code Reflection API (JEP 516 / Project Babylon)
    // =========================================================================

    /**
     * Implements the previously-stub sayCodeModel(Method) using the Java 26
     * Code Reflection API. Renders an op-count table + IR excerpt when a code model
     * is available; falls back to method signature rendering on older runtimes.
     */
    @Override
    @SuppressWarnings("preview")
    public void sayCodeModel(java.lang.reflect.Method method) {
        if (method == null) return;

        var analysis = CodeModelAnalyzer.analyze(method, 10);

        markdownDocument.add("");
        markdownDocument.add("### Method Code Model: `" + analysis.methodSig() + "`");
        markdownDocument.add("");

        if (!analysis.hasModel()) {
            markdownDocument.add("*(Code model not available — method not annotated with `@CodeReflection` or runtime < Java 26)*");
            markdownDocument.add("");
            markdownDocument.add("**Signature:** `" + analysis.methodSig() + "`");
            return;
        }

        // Op-count table
        markdownDocument.add("**Operation Profile (Java 26 Code Reflection / JEP 516):**");
        markdownDocument.add("");
        markdownDocument.add("| Op Type | Count |");
        markdownDocument.add("| --- | --- |");
        for (var entry : analysis.opCounts().entrySet()) {
            markdownDocument.add("| `" + entry.getKey() + "` | " + entry.getValue() + " |");
        }
        markdownDocument.add("");
        markdownDocument.add("**Blocks:** " + analysis.blockCount()
                + "  |  **Total Ops:** " + analysis.totalOps()
                + "  |  **Java:** " + System.getProperty("java.version"));

        // IR excerpt
        if (!analysis.irExcerpt().isEmpty()) {
            markdownDocument.add("");
            markdownDocument.add("**IR Excerpt (first " + analysis.irExcerpt().size() + " ops):**");
            markdownDocument.add("");
            markdownDocument.add("```");
            for (String line : analysis.irExcerpt()) {
                markdownDocument.add(line);
            }
            markdownDocument.add("```");
        }
    }

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        if (method == null) return;

        markdownDocument.add("");
        markdownDocument.add("### Control Flow Graph: `" + method.getName() + "`");
        markdownDocument.add("");

        String dsl = ControlFlowGraphBuilder.build(method);
        if (dsl.isEmpty()) {
            markdownDocument.add("*(Control flow graph not available — method requires `@CodeReflection` annotation and Java 26+)*");
            return;
        }

        markdownDocument.add("```mermaid");
        for (String line : dsl.split("\n")) {
            markdownDocument.add(line);
        }
        markdownDocument.add("```");
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        if (clazz == null) return;

        markdownDocument.add("");
        markdownDocument.add("### Call Graph: `" + clazz.getSimpleName() + "`");
        markdownDocument.add("");

        String dsl = CallGraphBuilder.build(clazz);
        if (dsl.isEmpty()) {
            markdownDocument.add("*(Call graph not available — methods require `@CodeReflection` annotation and Java 26+)*");
            return;
        }

        markdownDocument.add("```mermaid");
        for (String line : dsl.split("\n")) {
            markdownDocument.add(line);
        }
        markdownDocument.add("```");
    }

    @Override
    @SuppressWarnings("preview")
    public void sayOpProfile(java.lang.reflect.Method method) {
        if (method == null) return;

        var analysis = CodeModelAnalyzer.analyze(method, 0);

        markdownDocument.add("");
        markdownDocument.add("### Op Profile: `" + analysis.methodSig() + "`");
        markdownDocument.add("");

        if (!analysis.hasModel()) {
            markdownDocument.add("*(Op profile not available — method requires `@CodeReflection` annotation and Java 26+)*");
            return;
        }

        markdownDocument.add("| Op Type | Count |");
        markdownDocument.add("| --- | --- |");
        for (var entry : analysis.opCounts().entrySet()) {
            markdownDocument.add("| `" + entry.getKey() + "` | " + entry.getValue() + " |");
        }
        markdownDocument.add("");
        markdownDocument.add("**Blocks:** " + analysis.blockCount()
                + "  |  **Total Ops:** " + analysis.totalOps());
    }

    // =========================================================================
    // Blue Ocean: Inline Benchmarking
    // =========================================================================

    @Override
    public void sayBenchmark(String label, Runnable task) {
        sayBenchmark(label, task, 50, 500);
    }

    @Override
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        if (label == null || task == null) return;

        var result = BenchmarkRunner.run(task, warmupRounds, measureRounds);

        markdownDocument.add("");
        markdownDocument.add("### Benchmark: " + label);
        markdownDocument.add("");
        markdownDocument.add("| Metric | Result |");
        markdownDocument.add("| --- | --- |");
        markdownDocument.add("| Avg | `" + result.avgNs() + " ns` |");
        markdownDocument.add("| Min | `" + result.minNs() + " ns` |");
        markdownDocument.add("| Max | `" + result.maxNs() + " ns` |");
        markdownDocument.add("| p99 | `" + result.p99Ns() + " ns` |");
        markdownDocument.add("| Ops/sec | `" + "%,d".formatted(result.opsPerSec()) + "` |");
        markdownDocument.add("| Warmup rounds | `" + warmupRounds + "` |");
        markdownDocument.add("| Measure rounds | `" + measureRounds + "` |");
        markdownDocument.add("| Java | `" + System.getProperty("java.version") + "` |");
    }

    // =========================================================================
    // Blue Ocean: Mermaid Diagrams
    // =========================================================================

    @Override
    public void sayMermaid(String diagramDsl) {
        if (diagramDsl == null || diagramDsl.isBlank()) return;

        markdownDocument.add("");
        markdownDocument.add("```mermaid");
        for (String line : diagramDsl.split("\n")) {
            markdownDocument.add(line);
        }
        markdownDocument.add("```");
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;

        markdownDocument.add("");
        String title = Arrays.stream(classes)
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        markdownDocument.add("### Class Diagram: " + title);
        markdownDocument.add("");

        String dsl = ClassDiagramGenerator.generate(classes);
        markdownDocument.add("```mermaid");
        for (String line : dsl.split("\n")) {
            markdownDocument.add(line);
        }
        markdownDocument.add("```");
    }

    // =========================================================================
    // Blue Ocean: Documentation Coverage
    // =========================================================================

    @Override
    public void sayDocCoverage(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;

        for (Class<?> clazz : classes) {
            markdownDocument.add("");
            markdownDocument.add("### Documentation Coverage: `" + clazz.getSimpleName() + "`");
            markdownDocument.add("");

            // Note: coverage tracking lives in DtrContext; RenderMachineImpl receives
            // an already-computed rows list via the overloaded method below.
            markdownDocument.add("*(Coverage data not available — use DtrContext.sayDocCoverage() in tests)*");
        }
    }

    /** Called by DtrContext with pre-computed coverage rows. */
    public void sayDocCoverage(Class<?> clazz, List<CoverageRow> rows) {
        if (clazz == null || rows == null) return;

        markdownDocument.add("");
        markdownDocument.add("### Documentation Coverage: `" + clazz.getSimpleName() + "`");
        markdownDocument.add("");
        markdownDocument.add("| Method | Status | Via |");
        markdownDocument.add("| --- | --- | --- |");

        int covered = 0;
        for (CoverageRow row : rows) {
            if (row.documented()) covered++;
            markdownDocument.add("| `" + row.methodSig() + "` | " + (row.documented() ? "✅" : "❌") + " | " + row.via() + " |");
        }

        markdownDocument.add("");
        int total = rows.size();
        double pct = total > 0 ? (covered * 100.0 / total) : 0.0;
        markdownDocument.add("**Coverage: %.1f%% (%d/%d methods documented)**".formatted(pct, covered, total));
    }

    // =========================================================================
    // 80/20 Low-Hanging Fruit
    // =========================================================================

    @Override
    public void sayEnvProfile() {
        markdownDocument.add("");
        markdownDocument.add("### Environment Profile");
        markdownDocument.add("");
        markdownDocument.add("| Property | Value |");
        markdownDocument.add("| --- | --- |");
        markdownDocument.add("| Java Version | `" + System.getProperty("java.version") + "` |");
        markdownDocument.add("| Java Vendor | `" + System.getProperty("java.vendor") + "` |");
        markdownDocument.add("| OS | `" + System.getProperty("os.name") + " " + System.getProperty("os.arch") + "` |");
        markdownDocument.add("| Processors | `" + Runtime.getRuntime().availableProcessors() + "` |");
        markdownDocument.add("| Max Heap | `" + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + " MB` |");
        markdownDocument.add("| Timezone | `" + System.getProperty("user.timezone", "UTC") + "` |");
        markdownDocument.add("| DTR Version | `2.6.0` |");
        markdownDocument.add("| Timestamp | `" + Instant.now().toString() + "` |");
    }

    @Override
    public void sayOperatingSystem() {
        markdownDocument.add("");
        markdownDocument.add("### Operating System Metrics");
        markdownDocument.add("");
        markdownDocument.add("| Metric | Value |");
        markdownDocument.add("| --- | --- |");

        // Get the standard OperatingSystemMXBean
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        // Basic metrics (always available)
        String osName = osBean.getName();
        String osVersion = osBean.getVersion();
        String osArch = osBean.getArch();
        int availableProcessors = osBean.getAvailableProcessors();
        double systemLoadAverage = osBean.getSystemLoadAverage();

        markdownDocument.add("| OS Name | `" + osName + "` |");
        markdownDocument.add("| OS Version | `" + osVersion + "` |");
        markdownDocument.add("| OS Architecture | `" + osArch + "` |");
        markdownDocument.add("| Available Processors | `" + availableProcessors + "` |");

        // System load average (may be negative if not supported)
        if (systemLoadAverage >= 0) {
            markdownDocument.add("| System Load Average | `" + String.format("%.2f", systemLoadAverage) + "` |");
        } else {
            markdownDocument.add("| System Load Average | `N/A (not supported on this platform)` |");
        }

        // Try to get extended metrics from com.sun.management.OperatingSystemMXBean
        try {
            var sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;

            // CPU metrics
            double processCpuLoad = sunOsBean.getProcessCpuLoad() * 100;
            double systemCpuLoad = sunOsBean.getCpuLoad() * 100;

            if (processCpuLoad >= 0) {
                markdownDocument.add("| Process CPU Load | `" + String.format("%.1f%%", processCpuLoad) + "` |");
            } else {
                markdownDocument.add("| Process CPU Load | `N/A (not available)` |");
            }

            if (systemCpuLoad >= 0) {
                markdownDocument.add("| System CPU Load | `" + String.format("%.1f%%", systemCpuLoad) + "` |");
            } else {
                markdownDocument.add("| System CPU Load | `N/A (not available)` |");
            }

            // Memory metrics (convert bytes to MB)
            long totalPhysicalMemory = sunOsBean.getTotalPhysicalMemorySize() / (1024 * 1024);
            long freePhysicalMemory = sunOsBean.getFreePhysicalMemorySize() / (1024 * 1024);
            long usedPhysicalMemory = totalPhysicalMemory - freePhysicalMemory;

            long totalSwapSpace = sunOsBean.getTotalSwapSpaceSize() / (1024 * 1024);
            long freeSwapSpace = sunOsBean.getFreeSwapSpaceSize() / (1024 * 1024);
            long usedSwapSpace = totalSwapSpace - freeSwapSpace;

            markdownDocument.add("| Total Physical Memory | `" + totalPhysicalMemory + " MB` |");
            markdownDocument.add("| Free Physical Memory | `" + freePhysicalMemory + " MB` |");
            markdownDocument.add("| Used Physical Memory | `" + usedPhysicalMemory + " MB` |");
            markdownDocument.add("| Total Swap Space | `" + totalSwapSpace + " MB` |");
            markdownDocument.add("| Free Swap Space | `" + freeSwapSpace + " MB` |");
            markdownDocument.add("| Used Swap Space | `" + usedSwapSpace + " MB` |");

        } catch (ClassCastException e) {
            // com.sun.management.OperatingSystemMXBean not available
            // Fallback: render basic metrics only
            markdownDocument.add("| Process CPU Load | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| System CPU Load | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| Total Physical Memory | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| Free Physical Memory | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| Used Physical Memory | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| Total Swap Space | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| Free Swap Space | `N/A (extended metrics unavailable)` |");
            markdownDocument.add("| Used Swap Space | `N/A (extended metrics unavailable)` |");
        }
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        if (recordClass == null) return;

        markdownDocument.add("");
        markdownDocument.add("### Record Schema: `" + recordClass.getSimpleName() + "`");
        markdownDocument.add("");

        var components = recordClass.getRecordComponents();
        if (components == null || components.length == 0) {
            markdownDocument.add("*(No record components found)*");
            return;
        }

        markdownDocument.add("| Component | Type | Generic Type | Annotations |");
        markdownDocument.add("| --- | --- | --- | --- |");

        for (var component : components) {
            String type = component.getType().getSimpleName();
            String genericType = component.getGenericType().getTypeName();
            // Simplify generic type if it matches simple type
            String genericDisplay = genericType.equals(component.getType().getName()) ? "—" : genericType;
            String annotations = Arrays.stream(component.getAnnotations())
                    .map(a -> "`@" + a.annotationType().getSimpleName() + "`")
                    .collect(Collectors.joining(", "));
            if (annotations.isEmpty()) annotations = "—";
            markdownDocument.add("| `" + component.getName() + "` | `" + type + "` | " + genericDisplay + " | " + annotations + " |");
        }
    }

    @Override
    public void sayException(Throwable t) {
        if (t == null) return;

        markdownDocument.add("");
        markdownDocument.add("### Exception: `" + t.getClass().getSimpleName() + "`");
        markdownDocument.add("");
        markdownDocument.add("**Message:** " + (t.getMessage() != null ? t.getMessage() : "*(no message)*"));
        markdownDocument.add("");

        // Cause chain
        var causeChain = new ArrayList<String>();
        Throwable cause = t.getCause();
        while (cause != null) {
            causeChain.add("`" + cause.getClass().getSimpleName() + "`"
                    + (cause.getMessage() != null ? ": " + cause.getMessage() : ""));
            cause = cause.getCause();
        }
        if (!causeChain.isEmpty()) {
            markdownDocument.add("**Cause chain:**");
            for (String c : causeChain) {
                markdownDocument.add("- " + c);
            }
            markdownDocument.add("");
        }

        // Top stack frames
        var frames = t.getStackTrace();
        int limit = Math.min(5, frames.length);
        if (limit > 0) {
            markdownDocument.add("**Stack Trace (top " + limit + " frames):**");
            markdownDocument.add("");
            markdownDocument.add("| # | Class | Method | Line |");
            markdownDocument.add("| --- | --- | --- | --- |");
            for (int i = 0; i < limit; i++) {
                var f = frames[i];
                markdownDocument.add("| " + (i + 1) + " | `" + f.getClassName() + "` | `" + f.getMethodName() + "` | " + f.getLineNumber() + " |");
            }
        }
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        if (label == null || values == null || values.length == 0) return;

        markdownDocument.add("");
        markdownDocument.add("### Chart: " + label);
        markdownDocument.add("");

        double max = Arrays.stream(values).max().orElse(1.0);
        if (max == 0) max = 1.0;
        int barWidth = 20;

        markdownDocument.add("```");
        for (int i = 0; i < values.length; i++) {
            String rowLabel = (xLabels != null && i < xLabels.length) ? xLabels[i] : ("" + i);
            int filled = (int) Math.round((values[i] / max) * barWidth);
            int empty = barWidth - filled;
            String bar = "█".repeat(filled) + "░".repeat(empty);
            markdownDocument.add("%-6s %s  %.0f".formatted(rowLabel, bar, values[i]));
        }
        markdownDocument.add("```");
    }

    // =========================================================================
    // Contract Verification + Git Evolution Timeline
    // =========================================================================

    @Override
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        if (contract == null) return;

        // Auto-detect implementations from sealed hierarchy if none provided
        Class<?>[] impls = (implementations == null || implementations.length == 0)
                ? (contract.isSealed() ? contract.getPermittedSubclasses() : new Class<?>[0])
                : implementations;

        markdownDocument.add("");
        markdownDocument.add("### Contract Verification: `" + contract.getSimpleName() + "`");
        markdownDocument.add("");

        var rows = ContractVerifier.verify(contract, impls);
        if (rows.isEmpty()) {
            markdownDocument.add("*(No public abstract methods found in contract)*");
            return;
        }

        // Build header from impl names
        List<String> implNames = new ArrayList<>(rows.getFirst().implStatus().keySet());
        StringBuilder header = new StringBuilder("| Method |");
        StringBuilder sep = new StringBuilder("| --- |");
        for (String name : implNames) {
            header.append(" ").append(name).append(" |");
            sep.append(" --- |");
        }
        markdownDocument.add(header.toString());
        markdownDocument.add(sep.toString());

        int missing = 0;
        for (var row : rows) {
            StringBuilder line = new StringBuilder("| `").append(row.methodSig()).append("` |");
            for (String status : row.implStatus().values()) {
                line.append(" ").append(status).append(" |");
                if (status.contains("MISSING")) missing++;
            }
            markdownDocument.add(line.toString());
        }

        markdownDocument.add("");
        if (missing == 0) {
            markdownDocument.add("**All contract methods covered across all implementations.**");
        } else {
            markdownDocument.add("**" + missing + " missing implementation(s) detected above (❌ MISSING).**");
        }
    }

    @Override
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        if (clazz == null) return;

        markdownDocument.add("");
        markdownDocument.add("### Evolution Timeline: `" + clazz.getSimpleName() + "`");
        markdownDocument.add("");

        var entries = GitHistoryReader.read(clazz, maxEntries > 0 ? maxEntries : 10);

        if (entries.isEmpty()) {
            markdownDocument.add("> [!NOTE]");
            markdownDocument.add("> Git history unavailable in this environment. " +
                    "Run in a git repository with `git log` accessible to see commit history.");
            return;
        }

        markdownDocument.add("| Commit | Date | Author | Summary |");
        markdownDocument.add("| --- | --- | --- | --- |");
        for (var entry : entries) {
            markdownDocument.add("| `" + entry.hash() + "` | " + entry.date()
                    + " | " + entry.author() + " | " + entry.subject() + " |");
        }
        markdownDocument.add("");
        markdownDocument.add("*" + entries.size() + " most recent commits touching `"
                + clazz.getSimpleName() + ".java`*");
    }

    @Override
    public void sayJavadoc(java.lang.reflect.Method method) {
        if (method == null) return;

        JavadocIndex.lookup(method).ifPresent(entry -> {
            if (!entry.description().isBlank()) {
                markdownDocument.add("");
                markdownDocument.add(entry.description());
            }

            if (!entry.params().isEmpty()) {
                markdownDocument.add("");
                markdownDocument.add("| Parameter | Description |");
                markdownDocument.add("| --- | --- |");
                for (JavadocEntry.ParamDoc p : entry.params()) {
                    markdownDocument.add("| `" + p.name() + "` | " + p.description() + " |");
                }
            }

            entry.optReturns().ifPresent(r -> {
                markdownDocument.add("");
                markdownDocument.add("> [!NOTE]");
                markdownDocument.add("> **Returns:** " + r);
            });

            if (!entry.throws_().isEmpty()) {
                markdownDocument.add("");
                markdownDocument.add("| Exception | Description |");
                markdownDocument.add("| --- | --- |");
                for (JavadocEntry.ThrowsDoc t : entry.throws_()) {
                    markdownDocument.add("| `" + t.exception() + "` | " + t.description() + " |");
                }
            }

            entry.optSince().ifPresent(s -> {
                markdownDocument.add("");
                markdownDocument.add("> [!NOTE]");
                markdownDocument.add("> **Since:** " + s);
            });

            entry.optDeprecated().ifPresent(d -> {
                markdownDocument.add("");
                markdownDocument.add("> [!WARNING]");
                markdownDocument.add("> **Deprecated:** " + d);
            });
        });
    }

    @Override
    public void saySecurityManager() {
        markdownDocument.add("");
        markdownDocument.add("### Security Manager");
        markdownDocument.add("");

        // Security Manager Status
        SecurityManager sm = System.getSecurityManager();
        markdownDocument.add("| Property | Status |");
        markdownDocument.add("| --- | --- |");
        if (sm != null) {
            markdownDocument.add("| Security Manager | `PRESENT` |");
            markdownDocument.add("| Class | `" + sm.getClass().getName() + "` |");
        } else {
            markdownDocument.add("| Security Manager | `ABSENT` |");
            markdownDocument.add("| Class | — |");
        }
        markdownDocument.add("");

        // Security Providers
        markdownDocument.add("### Security Providers");
        markdownDocument.add("");
        markdownDocument.add("| Provider | Version | Info |");
        markdownDocument.add("| --- | --- | --- |");

        java.security.Provider[] providers = java.security.Security.getProviders();
        if (providers == null || providers.length == 0) {
            markdownDocument.add("| *(No security providers installed)* | — | — |");
        } else {
            for (java.security.Provider provider : providers) {
                String name = provider.getName();
                double version = provider.getVersion();
                String info = provider.getInfo();
                markdownDocument.add("| `" + name + "` | `" + version + "` | " + info + " |");
            }
        }

        markdownDocument.add("");

        // Cryptographic Algorithms
        markdownDocument.add("### Available Cryptographic Algorithms");
        markdownDocument.add("");

        // KeyPairGenerator algorithms
        markdownDocument.add("**KeyPairGenerator:**");
        var kpgAlgos = java.security.Security.getAlgorithms("KeyPairGenerator");
        if (kpgAlgos.isEmpty()) {
            markdownDocument.add("*(None available)*");
        } else {
            markdownDocument.add("`" + String.join("`, `", kpgAlgos) + "`");
        }

        markdownDocument.add("");

        // Cipher algorithms
        markdownDocument.add("**Cipher:**");
        var cipherAlgos = java.security.Security.getAlgorithms("Cipher");
        if (cipherAlgos.isEmpty()) {
            markdownDocument.add("*(None available)*");
        } else {
            // Show first 20 to avoid overwhelming output
            var limited = cipherAlgos.stream()
                    .limit(20)
                    .collect(Collectors.toList());
            markdownDocument.add("`" + String.join("`, `", limited) + "`" +
                    (cipherAlgos.size() > 20 ? " ... (" + (cipherAlgos.size() - 20) + " more)" : ""));
        }

        markdownDocument.add("");

        // MessageDigest algorithms
        markdownDocument.add("**MessageDigest:**");
        var mdAlgos = java.security.Security.getAlgorithms("MessageDigest");
        if (mdAlgos.isEmpty()) {
            markdownDocument.add("*(None available)*");
        } else {
            markdownDocument.add("`" + String.join("`, `", mdAlgos) + "`");
        }

        markdownDocument.add("");

        // SecureRandom info
        markdownDocument.add("### SecureRandom");
        markdownDocument.add("");
        markdownDocument.add("| Property | Value |");
        markdownDocument.add("| --- | --- |");

        try {
            java.security.SecureRandom sr = java.security.SecureRandom.getInstanceStrong();
            markdownDocument.add("| Strong Algorithm | `" + sr.getAlgorithm() + "` |");
            markdownDocument.add("| Provider | `" + sr.getProvider().getName() + "` |");
        } catch (Exception e) {
            markdownDocument.add("| Strong Algorithm | *(Unavailable: " + e.getMessage() + ")* |");
            markdownDocument.add("| Provider | — |");
        }

        try {
            java.security.SecureRandom sr = new java.security.SecureRandom();
            markdownDocument.add("| Default Algorithm | `" + sr.getAlgorithm() + "` |");
            markdownDocument.add("| Provider | `" + sr.getProvider().getName() + "` |");
        } catch (Exception e) {
            markdownDocument.add("| Default Algorithm | *(Unavailable: " + e.getMessage() + ")* |");
            markdownDocument.add("| Provider | — |");
        }
    }

    @Override
    public void sayThreadDump() {
        markdownDocument.add("");
        markdownDocument.add("### Thread Summary");
        markdownDocument.add("");

        var threadMXBean = java.lang.management.ManagementFactory.getThreadMXBean();

        // Summary metrics table
        markdownDocument.add("| Metric | Value |");
        markdownDocument.add("| --- | --- |");
        markdownDocument.add("| Thread Count | `" + threadMXBean.getThreadCount() + "` |");
        markdownDocument.add("| Daemon Thread Count | `" + threadMXBean.getDaemonThreadCount() + "` |");
        markdownDocument.add("| Peak Thread Count | `" + threadMXBean.getPeakThreadCount() + "` |");
        markdownDocument.add("| Total Started Thread Count | `" + threadMXBean.getTotalStartedThreadCount() + "` |");

        markdownDocument.add("");
        markdownDocument.add("### Thread Details");
        markdownDocument.add("");

        // Thread details table
        markdownDocument.add("| Thread ID | Name | State | Alive | Interrupted |");
        markdownDocument.add("| --- | --- | --- | --- | --- |");

        // Get all thread IDs
        long[] threadIds = threadMXBean.getAllThreadIds();

        if (threadIds == null || threadIds.length == 0) {
            markdownDocument.add("| *(No threads detected)* | — | — | — | — |");
        } else {
            for (long threadId : threadIds) {
                var threadInfo = threadMXBean.getThreadInfo(threadId);
                if (threadInfo != null) {
                    String threadName = threadInfo.getThreadName();
                    Thread.State state = threadInfo.getThreadState();
                    boolean alive = threadInfo.getThreadState() != Thread.State.TERMINATED;

                    // Escape pipes in thread name to avoid breaking markdown table
                    String escapedName = threadName != null ? threadName.replace("|", "\\|") : "null";

                    markdownDocument.add("| `" + threadId + "` | `" + escapedName + "` | `" +
                            state + "` | `" + alive + "` | `N/A` |");
                }
            }
        }

        markdownDocument.add("");
        markdownDocument.add("*" + threadIds.length + " live threads*");
    }

    @Override
    public void saySystemProperties() {
        saySystemProperties(null);
    }

    @Override
    public void saySystemProperties(String regexFilter) {
        var props = System.getProperties();

        // Filter properties if regex is provided
        var entryStream = props.entrySet().stream();
        if (regexFilter != null && !regexFilter.isBlank()) {
            var pattern = java.util.regex.Pattern.compile(regexFilter);
            var predicate = pattern.asPredicate();
            entryStream = entryStream.filter(e -> predicate.test(e.getKey().toString()));
        }

        // Sort by key and collect to list
        var sortedEntries = entryStream
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .toList();

        if (sortedEntries.isEmpty()) {
            markdownDocument.add("");
            markdownDocument.add("> [!NOTE]");
            markdownDocument.add("> No system properties found matching filter: `" + regexFilter + "`");
            return;
        }

        markdownDocument.add("");
        markdownDocument.add("### JVM System Properties");
        if (regexFilter != null && !regexFilter.isBlank()) {
            markdownDocument.add("");
            markdownDocument.add("*Filter: `" + regexFilter + "`*");
        }
        markdownDocument.add("");
        markdownDocument.add("| Property Key | Property Value |");
        markdownDocument.add("| --- | --- |");

        for (var entry : sortedEntries) {
            var key = entry.getKey().toString();
            var value = entry.getValue().toString();
            markdownDocument.add("| `" + key + "` | `" + value + "` |");
        }

        markdownDocument.add("");
        markdownDocument.add("*" + sortedEntries.size() + " properties documented*");
    }

    /**
     * Converts a section heading to a lowercase anchor ID suitable for use
     * in markdown table-of-contents links. Strips all non-alphanumeric characters.
     *
     * @param text the heading text to convert
     * @return a lowercase alphanumeric anchor ID
     */
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

    // =========================================================================
    // Blue Ocean: JPMS Module Dependencies
    // =========================================================================

    @Override
    public void sayModuleDependencies(Class<?>... classes) {
        if (classes == null || classes.length == 0) {
            markdownDocument.add("");
            markdownDocument.add("> [!NOTE]");
            markdownDocument.add("> No classes provided for module dependency analysis.");
            return;
        }

        markdownDocument.add("");
        markdownDocument.add("### Module Dependencies");
        markdownDocument.add("");

        // Group classes by module to avoid duplicates
        Map<Module, List<Class<?>>> moduleMap = Arrays.stream(classes)
                .filter(clazz -> clazz != null)
                .collect(Collectors.groupingBy(Class::getModule, LinkedHashMap::new, Collectors.toList()));

        if (moduleMap.isEmpty()) {
            markdownDocument.add("*(No valid classes provided)*");
            return;
        }

        for (var entry : moduleMap.entrySet()) {
            Module module = entry.getKey();
            List<Class<?>> moduleClasses = entry.getValue();

            renderModuleInfo(module, moduleClasses);
        }
    }

    /**
     * Renders module information for a single module.
     *
     * @param module the module to document
     * @param classes the classes from this module (for context)
     */
    private void renderModuleInfo(Module module, List<Class<?>> classes) {
        String moduleName = module.getName();
        boolean isNamed = module.isNamed();

        markdownDocument.add("#### " + (isNamed ? "`" + moduleName + "`" : "**Unnamed Module** (classpath)"));
        markdownDocument.add("");

        if (!isNamed) {
            markdownDocument.add("> [!NOTE]");
            markdownDocument.add("> This class is loaded from the classpath (unnamed module). " +
                    "JPMS module descriptors are not available for unnamed modules.");
            markdownDocument.add("");

            // Show which classes are from this unnamed module
            markdownDocument.add("**Classes from this module:**");
            for (Class<?> clazz : classes) {
                markdownDocument.add("- `" + clazz.getName() + "`");
            }
            markdownDocument.add("");
            return;
        }

        // Named module - extract descriptor
        var descriptor = module.getDescriptor();
        if (descriptor == null) {
            markdownDocument.add("*(Module descriptor unavailable)*");
            markdownDocument.add("");
            return;
        }

        // Module metadata
        markdownDocument.add("- **Module Name:** `" + descriptor.name() + "`");
        markdownDocument.add("- **Automatic:** " + (descriptor.isAutomatic() ? "Yes" : "No"));
        markdownDocument.add("");

        // Packages
        var packages = descriptor.packages();
        markdownDocument.add("**Packages** (" + packages.size() + "):");
        if (packages.isEmpty()) {
            markdownDocument.add("*(no packages)*");
        } else {
            // Show first 10 packages, then count if more
            int limit = Math.min(10, packages.size());
            var pkgArray = packages.toArray(new String[0]);
            for (int i = 0; i < limit; i++) {
                markdownDocument.add("- `" + pkgArray[i] + "`");
            }
            if (packages.size() > 10) {
                markdownDocument.add("- ... and " + (packages.size() - 10) + " more");
            }
        }
        markdownDocument.add("");

        // Requires (dependencies)
        var requires = descriptor.requires();
        markdownDocument.add("**Requires** (" + requires.size() + " module dependencies):");
        if (requires.isEmpty()) {
            markdownDocument.add("*(none — this is likely the base module)*");
        } else {
            markdownDocument.add("| Module | Modifiers |");
            markdownDocument.add("| --- | --- |");
            for (var req : requires) {
                String modifiers = req.modifiers().stream()
                        .map(m -> "`" + m + "`")
                        .collect(Collectors.joining(", "));
                if (modifiers.isEmpty()) modifiers = "—";
                markdownDocument.add("| `" + req.name() + "` | " + modifiers + " |");
            }
        }
        markdownDocument.add("");

        // Exports
        var exports = descriptor.exports();
        markdownDocument.add("**Exports** (" + exports.size() + "):");
        if (exports.isEmpty()) {
            markdownDocument.add("*(none)*");
        } else {
            markdownDocument.add("| Package | Target Modules |");
            markdownDocument.add("| --- | --- |");
            for (var exp : exports) {
                String targets = exp.targets().isEmpty()
                        ? "*(all modules)*"
                        : exp.targets().stream()
                                .map(t -> "`" + t + "`")
                                .collect(Collectors.joining(", "));
                markdownDocument.add("| `" + exp.source() + "` | " + targets + " |");
            }
        }
        markdownDocument.add("");

        // Opens
        var opens = descriptor.opens();
        markdownDocument.add("**Opens** (" + opens.size() + "):");
        if (opens.isEmpty()) {
            markdownDocument.add("*(none)*");
        } else {
            markdownDocument.add("| Package | Target Modules |");
            markdownDocument.add("| --- | --- |");
            for (var open : opens) {
                String targets = open.targets().isEmpty()
                        ? "*(all modules)*"
                        : open.targets().stream()
                                .map(t -> "`" + t + "`")
                                .collect(Collectors.joining(", "));
                markdownDocument.add("| `" + open.source() + "` | " + targets + " |");
            }
        }
        markdownDocument.add("");

        // Uses (service dependencies)
        var uses = descriptor.uses();
        markdownDocument.add("**Uses** (service dependencies, " + uses.size() + "):");
        if (uses.isEmpty()) {
            markdownDocument.add("*(none)*");
        } else {
            for (var service : uses) {
                markdownDocument.add("- `" + service + "`");
            }
        }
        markdownDocument.add("");

        // Provides (service implementations)
        var provides = descriptor.provides();
        markdownDocument.add("**Provides** (service implementations, " + provides.size() + "):");
        if (provides.isEmpty()) {
            markdownDocument.add("*(none)*");
        } else {
            markdownDocument.add("| Service | Implementations |");
            markdownDocument.add("| --- | --- |");
            for (var prov : provides) {
                String impls = prov.providers().stream()
                        .map(p -> "`" + p + "`")
                        .collect(Collectors.joining(", "));
                markdownDocument.add("| `" + prov.service() + "` | " + impls + " |");
            }
        }
        markdownDocument.add("");
    }

    @Override
    public void sayHttpContract(String url, String[][] expectedFields) {
        var result = io.github.seanchatmangpt.dtr.http.HttpContractAnalyzer.analyze(url, expectedFields);
        io.github.seanchatmangpt.dtr.http.HttpContractAnalyzer.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayPerformanceRegression(String label, long baselineNs, Runnable task) {
        var result = io.github.seanchatmangpt.dtr.perf.PerformanceRegressionRunner.run(label, baselineNs, task);
        io.github.seanchatmangpt.dtr.perf.PerformanceRegressionRunner.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayVirtualThreadComparison(String label, int taskCount, Runnable task) {
        var result = io.github.seanchatmangpt.dtr.vthread.VirtualThreadComparator.compare(label, taskCount, task);
        io.github.seanchatmangpt.dtr.vthread.VirtualThreadComparator.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayNarrativeScenario(String given, String when, String then, Runnable action) {
        var result = io.github.seanchatmangpt.dtr.narrative.NarrativeScenarioRenderer.run(
            "Scenario", given, when, then, action, () -> "completed");
        io.github.seanchatmangpt.dtr.narrative.NarrativeScenarioRenderer.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayDataSample(java.util.List<?> data, int maxSampleRows) {
        var result = io.github.seanchatmangpt.dtr.datasample.DataSampleAnalyzer.analyze(data, maxSampleRows);
        // Sample table
        markdownDocument.add("**Data Sample (" + result.sampledRows() + " of " + result.totalRows() + " rows)**");
        markdownDocument.add("");
        var sampleTable = io.github.seanchatmangpt.dtr.datasample.DataSampleAnalyzer.toSampleTable(result);
        sayTable(sampleTable);
        markdownDocument.add("**Field Statistics**");
        markdownDocument.add("");
        var statsTable = io.github.seanchatmangpt.dtr.datasample.DataSampleAnalyzer.toStatsTable(result);
        sayTable(statsTable);
        markdownDocument.add("");
    }

    @Override
    public void sayDecisionRecord(String id, String title, String context, String decision, String consequences) {
        var adr = io.github.seanchatmangpt.dtr.adr.DecisionRecord.accepted(id, title, context, decision, consequences);
        io.github.seanchatmangpt.dtr.adr.DecisionRecord.toMarkdown(adr).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayLoadProfile(String label, int threads, long durationMs, Runnable task) {
        var result = io.github.seanchatmangpt.dtr.load.LoadProfileRunner.run(label, threads, durationMs, task);
        io.github.seanchatmangpt.dtr.load.LoadProfileRunner.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayTypeCompat(Class<?> v1, Class<?> v2) {
        var result = io.github.seanchatmangpt.dtr.typecompat.TypeCompatAnalyzer.analyze(v1, v2);
        io.github.seanchatmangpt.dtr.typecompat.TypeCompatAnalyzer.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void saySecurityProfile(Class<?> clazz) {
        var result = io.github.seanchatmangpt.dtr.security.SecurityProfileAnalyzer.analyze(clazz);
        io.github.seanchatmangpt.dtr.security.SecurityProfileAnalyzer.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayGitHotspot(Class<?> clazz, String projectRoot) {
        var result = io.github.seanchatmangpt.dtr.githotspot.GitHotspotAnalyzer.analyze(clazz, projectRoot);
        io.github.seanchatmangpt.dtr.githotspot.GitHotspotAnalyzer.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    // ── Wave 3 innovations ───────────────────────────────────────────────────

    @Override
    public void sayBenchmarkComparison(java.util.Map<String, Runnable> tasks) {
        var result = io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison.compare(tasks, 10, 50);
        io.github.seanchatmangpt.dtr.benchmark.BenchmarkComparison.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayJavadocSelf() {
        var result = io.github.seanchatmangpt.dtr.javadoc.JavadocSelfLookup.lookup();
        io.github.seanchatmangpt.dtr.javadoc.JavadocSelfLookup.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayParallelBenchmark(String label, int taskCount, Runnable task) {
        var result = io.github.seanchatmangpt.dtr.parallel.ParallelBenchmarkRunner.run(label, taskCount, task);
        io.github.seanchatmangpt.dtr.parallel.ParallelBenchmarkRunner.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayDependencyGraph(String projectRoot) {
        var result = io.github.seanchatmangpt.dtr.dependencies.MavenDependencyReader.read(projectRoot);
        io.github.seanchatmangpt.dtr.dependencies.MavenDependencyReader.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayApiContract(Class<?> clazz) {
        var result = io.github.seanchatmangpt.dtr.apicontract.ApiContractExtractor.extract(clazz);
        io.github.seanchatmangpt.dtr.apicontract.ApiContractExtractor.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayDocumentSnapshot(String key) {
        var lines = java.util.List.copyOf(markdownDocument);
        var result = io.github.seanchatmangpt.dtr.snapshot.DocumentSnapshot.save(key, lines);
        io.github.seanchatmangpt.dtr.snapshot.DocumentSnapshot.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayDocumentDiff(String key) {
        var lines = java.util.List.copyOf(markdownDocument);
        var result = io.github.seanchatmangpt.dtr.snapshot.DocumentSnapshot.diff(key, lines);
        io.github.seanchatmangpt.dtr.snapshot.DocumentSnapshot.diffToMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void saySchemaEvolution(Class<?> clazz, String projectRoot) {
        var result = io.github.seanchatmangpt.dtr.schemaevolution.SchemaEvolutionReader.read(clazz, projectRoot);
        io.github.seanchatmangpt.dtr.schemaevolution.SchemaEvolutionReader.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayPropertyBasedTest(String label,
                                     java.util.function.Supplier<Object> gen,
                                     java.util.function.Predicate<Object> predicate,
                                     int trials) {
        var result = io.github.seanchatmangpt.dtr.property.PropertyTestRunner.run(label, gen, predicate, trials);
        io.github.seanchatmangpt.dtr.property.PropertyTestRunner.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }

    @Override
    public void sayTestCoverage(Class<?> clazz, java.util.Set<String> calledMethods) {
        var result = io.github.seanchatmangpt.dtr.methodcoverage.MethodCoverageTracker.analyze(clazz, calledMethods);
        io.github.seanchatmangpt.dtr.methodcoverage.MethodCoverageTracker.toMarkdown(result).forEach(markdownDocument::add);
        markdownDocument.add("");
    }
}
