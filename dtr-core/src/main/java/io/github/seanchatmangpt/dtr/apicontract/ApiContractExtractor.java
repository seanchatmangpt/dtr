/*
 * Copyright (C) 2026 the original author or authors.
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
package io.github.seanchatmangpt.dtr.apicontract;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts the public API contract of a class using reflection and renders it
 * as structured tables or markdown.
 *
 * <p>Reflects over all public methods declared directly in the given class
 * (both instance and static), skipping synthetic and bridge methods. Each
 * method is described by its name, parameter types (simple names), return type
 * (simple name), and any annotations present. Results are sorted alphabetically
 * by method name for stable output.</p>
 *
 * <p>This extractor never throws — per-method reflection errors are caught and
 * recorded as an error note in the annotations column so that a single
 * problematic method does not abort the entire extraction.</p>
 *
 * @since 2026.1.0
 */
public final class ApiContractExtractor {

    private ApiContractExtractor() {}

    // -------------------------------------------------------------------------
    // Result records
    // -------------------------------------------------------------------------

    /**
     * A single public method's extracted contract entry.
     *
     * @param name        the method's simple name
     * @param paramTypes  comma-separated simple type names of the parameter types,
     *                    or {@code "(none)"} when the method takes no parameters
     * @param returnType  simple name of the return type (e.g. {@code "String"}, {@code "void"})
     * @param annotations comma-separated {@code @AnnotationName} strings for each annotation
     *                    declared on the method, or {@code ""} when there are none
     */
    public record MethodEntry(String name, String paramTypes, String returnType, String annotations) {}

    /**
     * The full contract result for one class: the class's simple name plus all
     * extracted public method entries.
     *
     * @param className simple name of the inspected class
     * @param methods   extracted method entries sorted by method name ascending
     */
    public record ApiContractResult(String className, List<MethodEntry> methods) {}

    // -------------------------------------------------------------------------
    // Core extraction
    // -------------------------------------------------------------------------

    /**
     * Extracts the public API contract of {@code clazz}.
     *
     * <p>Includes all public methods returned by {@link Class#getDeclaredMethods()}
     * that are neither synthetic nor bridge. Each method is processed
     * independently; reflection errors for a single method are swallowed and
     * represented as an error note rather than propagating an exception.</p>
     *
     * @param clazz the class to inspect (must not be null)
     * @return a non-null {@link ApiContractResult} sorted by method name
     */
    public static ApiContractResult extract(Class<?> clazz) {
        var className = clazz.getSimpleName();
        var entries = new ArrayList<MethodEntry>();

        Method[] methods;
        try {
            methods = clazz.getDeclaredMethods();
        } catch (Exception e) {
            // If we cannot even list the methods, return an empty result with a note.
            entries.add(new MethodEntry(
                    "[error]",
                    "",
                    "",
                    "Could not retrieve methods: " + e.getMessage()));
            return new ApiContractResult(className, List.copyOf(entries));
        }

        for (var method : methods) {
            // Skip non-public, synthetic, and bridge methods
            if (!Modifier.isPublic(method.getModifiers())) continue;
            if (method.isSynthetic()) continue;
            if (method.isBridge()) continue;

            try {
                var name = method.getName();
                var paramTypes = buildParamTypes(method);
                var returnType = simpleName(method.getReturnType());
                var annotations = buildAnnotations(method);
                entries.add(new MethodEntry(name, paramTypes, returnType, annotations));
            } catch (Exception e) {
                // Record the failure as an entry so the table stays informative.
                entries.add(new MethodEntry(
                        safeMethodName(method),
                        "[error]",
                        "[error]",
                        "Extraction failed: " + e.getMessage()));
            }
        }

        entries.sort(Comparator.comparing(MethodEntry::name));
        return new ApiContractResult(className, List.copyOf(entries));
    }

    // -------------------------------------------------------------------------
    // Rendering helpers
    // -------------------------------------------------------------------------

    /**
     * Converts an {@link ApiContractResult} into a list of markdown lines
     * containing a heading, a GFM table, and a one-sentence method-count summary.
     *
     * <p>The returned list is suitable for joining with {@code "\n"} or passing
     * line-by-line to {@code sayRaw()}.</p>
     *
     * @param result a non-null {@link ApiContractResult}
     * @return mutable list of markdown lines (never null, never empty)
     */
    public static List<String> toMarkdown(ApiContractResult result) {
        var lines = new ArrayList<String>();

        lines.add("### API Contract: %s".formatted(result.className()));
        lines.add("");
        lines.add("| Method | Parameters | Returns | Annotations |");
        lines.add("|--------|-----------|---------|-------------|");

        for (var m : result.methods()) {
            var annotations = m.annotations().isBlank() ? "—" : m.annotations();
            var params      = m.paramTypes().isBlank() ? "(none)" : m.paramTypes();
            lines.add("| `%s` | `%s` | `%s` | %s |".formatted(
                    m.name(), params, m.returnType(), annotations));
        }

        lines.add("");
        lines.add("**Total public methods:** %d".formatted(result.methods().size()));

        return lines;
    }

    /**
     * Converts an {@link ApiContractResult} into a {@code String[][]} table
     * suitable for passing directly to {@code sayTable()}.
     *
     * <p>The first row is the header {@code ["Method", "Parameters", "Returns",
     * "Annotations"]}. Each subsequent row corresponds to one
     * {@link MethodEntry} in declaration (sorted) order.</p>
     *
     * @param result a non-null {@link ApiContractResult}
     * @return a 2-D array with one header row followed by one row per method
     */
    public static String[][] toTable(ApiContractResult result) {
        var methods = result.methods();
        var rows = new String[methods.size() + 1][4];
        rows[0] = new String[]{"Method", "Parameters", "Returns", "Annotations"};

        for (int i = 0; i < methods.size(); i++) {
            var m = methods.get(i);
            rows[i + 1] = new String[]{
                m.name(),
                m.paramTypes().isBlank() ? "(none)" : m.paramTypes(),
                m.returnType(),
                m.annotations().isBlank() ? "" : m.annotations()
            };
        }
        return rows;
    }

    // -------------------------------------------------------------------------
    // Private utilities
    // -------------------------------------------------------------------------

    private static String buildParamTypes(Method method) {
        var types = method.getParameterTypes();
        if (types.length == 0) {
            return "";
        }
        return Arrays.stream(types)
                .map(ApiContractExtractor::simpleName)
                .collect(Collectors.joining(", "));
    }

    private static String buildAnnotations(Method method) {
        var annotations = method.getDeclaredAnnotations();
        if (annotations.length == 0) {
            return "";
        }
        return Arrays.stream(annotations)
                .map(a -> "@" + a.annotationType().getSimpleName())
                .collect(Collectors.joining(", "));
    }

    /**
     * Returns the simple name of a type, handling arrays by appending {@code []}.
     * Component types of arrays are recursively simplified so that
     * {@code String[][]} renders as {@code String[][]}.
     */
    private static String simpleName(Class<?> type) {
        if (type.isArray()) {
            return simpleName(type.getComponentType()) + "[]";
        }
        return type.getSimpleName();
    }

    /** Retrieves a method name defensively — returns {@code "[unknown]"} on any error. */
    private static String safeMethodName(Method method) {
        try {
            return method.getName();
        } catch (Exception e) {
            return "[unknown]";
        }
    }
}
