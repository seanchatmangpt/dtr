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
package io.github.seanchatmangpt.dtr.javadoc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Walks the JVM call stack to identify the method that invoked
 * {@code sayJavadocSelf()} and optionally enriches the result with
 * Javadoc text sourced from {@code docs/meta/javadoc.json}.
 *
 * <p>Frame layout assumed by {@link #lookup()}:
 * <ol>
 *   <li>frame 0 — {@code JavadocSelfLookup.lookup()} itself</li>
 *   <li>frame 1 — {@code RenderMachineImpl.sayJavadocSelf()}</li>
 *   <li>frame 2 — the caller of {@code sayJavadocSelf()} (the target method)</li>
 * </ol>
 *
 * <p>This class never throws: all exceptions are caught and a default
 * (empty) {@link LookupResult} is returned instead.</p>
 *
 * @since 2026.1.0
 */
public final class JavadocSelfLookup {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private JavadocSelfLookup() {}

    // -------------------------------------------------------------------------
    // Public record — the result of a self-lookup
    // -------------------------------------------------------------------------

    /**
     * Immutable result of a Javadoc self-lookup.
     *
     * @param callerClass   fully-qualified name of the calling class
     * @param callerMethod  simple name of the calling method
     * @param lineNumber    source line at which {@code sayJavadocSelf()} was invoked
     * @param javadocText   Javadoc description text, or {@code ""} when unavailable
     */
    public record LookupResult(
            String callerClass,
            String callerMethod,
            int lineNumber,
            String javadocText) {}

    // -------------------------------------------------------------------------
    // Core lookup
    // -------------------------------------------------------------------------

    /**
     * Performs the self-lookup by walking the JVM stack and reading the Javadoc
     * index file when present.
     *
     * <p>Never throws — a default {@link LookupResult} with empty strings and
     * line number {@code -1} is returned on any failure.</p>
     *
     * @return the lookup result for the method that called {@code sayJavadocSelf()}
     */
    public static LookupResult lookup() {
        try {
            var walker = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

            // Frame index 2: skip lookup() itself (0) and sayJavadocSelf() (1).
            var frame = walker.walk(stream ->
                    stream.skip(2).findFirst()
            );

            if (frame.isEmpty()) {
                return new LookupResult("", "", -1, "");
            }

            var f = frame.get();
            var callerClass  = f.getClassName();
            var callerMethod = f.getMethodName();
            var lineNumber   = f.getLineNumber();
            var javadocText  = resolveJavadocText(callerClass, callerMethod);

            return new LookupResult(callerClass, callerMethod, lineNumber, javadocText);

        } catch (Exception e) {
            return new LookupResult("", "", -1, "");
        }
    }

    // -------------------------------------------------------------------------
    // Markdown rendering
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link LookupResult} into Markdown lines suitable for direct
     * injection into the DTR render machine's document buffer.
     *
     * <p>The output always includes a heading and a caller-info table. When
     * {@code javadocText} is non-empty a fenced Javadoc block is appended.</p>
     *
     * @param result the lookup result to render (must not be null)
     * @return an immutable list of Markdown lines (never null, never empty)
     */
    public static List<String> toMarkdown(LookupResult result) {
        var lines = new ArrayList<String>();

        lines.add("");
        lines.add("### Javadoc Self-Reference");
        lines.add("");
        lines.add("| Field | Value |");
        lines.add("| --- | --- |");
        lines.add("| Caller class  | `%s` |".formatted(result.callerClass()));
        lines.add("| Caller method | `%s` |".formatted(result.callerMethod()));
        lines.add("| Line number   | `%d` |".formatted(result.lineNumber()));

        if (!result.javadocText().isBlank()) {
            lines.add("");
            lines.add("**Javadoc:**");
            lines.add("");
            lines.add("> " + result.javadocText().replace("\n", "\n> "));
        }

        return List.copyOf(lines);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Attempts to read {@code docs/meta/javadoc.json} and return the description
     * for the given class and method. Returns {@code ""} on any failure or when
     * the file does not exist.
     */
    private static String resolveJavadocText(String className, String methodName) {
        try {
            var file = new File("docs/meta/javadoc.json");
            if (!file.exists()) {
                return "";
            }

            TypeReference<Map<String, Map<String, String>>> typeRef = new TypeReference<>() {};
            Map<String, Map<String, String>> index = MAPPER.readValue(file, typeRef);

            var classEntry = index.get(className);
            if (classEntry == null) {
                return "";
            }

            var text = classEntry.get(methodName);
            return text != null ? text : "";

        } catch (Exception e) {
            return "";
        }
    }
}
