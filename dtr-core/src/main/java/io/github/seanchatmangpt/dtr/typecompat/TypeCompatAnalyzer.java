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
package io.github.seanchatmangpt.dtr.typecompat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Compares two classes via reflection and identifies added, removed, and changed
 * public API members (fields and methods).
 *
 * <p>Used to document API evolution between versions of an interface or class.
 * Only public members are considered; synthetic and bridge methods are excluded.</p>
 *
 * <p>Breaking changes are defined as any REMOVED or TYPE_CHANGED member. Additions
 * and signature changes (overloads with same name but different return type) are
 * reported but do not by themselves constitute a breaking change in this model.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * var result = TypeCompatAnalyzer.analyze(UserApiV1.class, UserApiV2.class);
 * if (result.breaking()) {
 *     System.out.println("Breaking changes detected!");
 * }
 * result.changes().forEach(c -> System.out.println(c.kind() + " " + c.name()));
 * }</pre>
 */
public final class TypeCompatAnalyzer {

    private TypeCompatAnalyzer() {}

    // =========================================================================
    // Public model types
    // =========================================================================

    /**
     * Classifies the nature of a single API member change between two class versions.
     */
    public enum ChangeKind {
        /** Member present in v2 but absent in v1. */
        ADDED,
        /** Member present in v1 but absent in v2. */
        REMOVED,
        /** Field present in both versions but with a different declared type. */
        TYPE_CHANGED,
        /** Method present in both versions but with a different return type. */
        SIGNATURE_CHANGED,
        /** Member present in both versions with identical signature — included for completeness. */
        COMPATIBLE
    }

    /**
     * Describes a single API change between two class versions.
     *
     * @param kind         the classification of this change
     * @param memberType   either {@code "field"} or {@code "method"}
     * @param name         the simple name of the member
     * @param oldSignature the signature as it appeared in v1, or {@code "-"} if absent
     * @param newSignature the signature as it appears in v2, or {@code "-"} if absent
     */
    public record ApiChange(
            ChangeKind kind,
            String memberType,
            String name,
            String oldSignature,
            String newSignature) {}

    /**
     * The full compatibility analysis result between two class versions.
     *
     * @param v1       the baseline (old) class
     * @param v2       the candidate (new) class
     * @param changes  all detected API changes
     * @param breaking {@code true} if any REMOVED or TYPE_CHANGED change exists
     */
    public record CompatResult(
            Class<?> v1,
            Class<?> v2,
            List<ApiChange> changes,
            boolean breaking) {}

    // =========================================================================
    // Analysis
    // =========================================================================

    /**
     * Analyzes the public API difference between {@code v1} and {@code v2}.
     *
     * <p>The analysis covers:</p>
     * <ul>
     *   <li>Methods: REMOVED (in v1, not v2), ADDED (in v2, not v1),
     *       SIGNATURE_CHANGED (same simple name, different return type)</li>
     *   <li>Fields: REMOVED, ADDED, TYPE_CHANGED (same name, different declared type)</li>
     * </ul>
     *
     * <p>Only public, non-synthetic, non-bridge members are examined.
     * Overloaded methods with different parameter lists but the same name are
     * treated as separate entries keyed by their full descriptor.</p>
     *
     * @param v1 the baseline class or interface (must not be null)
     * @param v2 the candidate class or interface (must not be null)
     * @return a {@link CompatResult} capturing all differences
     */
    public static CompatResult analyze(Class<?> v1, Class<?> v2) {
        var changes = new ArrayList<ApiChange>();

        analyzeFields(v1, v2, changes);
        analyzeMethods(v1, v2, changes);

        boolean breaking = changes.stream()
                .anyMatch(c -> c.kind() == ChangeKind.REMOVED || c.kind() == ChangeKind.TYPE_CHANGED);

        return new CompatResult(v1, v2, List.copyOf(changes), breaking);
    }

    // =========================================================================
    // Table and markdown helpers
    // =========================================================================

    /**
     * Converts a {@link CompatResult} into a 2D string table suitable for
     * {@code sayTable()}.
     *
     * <p>The first row is the header: {@code Change, Kind, Member, Old Signature, New Signature}.
     * Each subsequent row corresponds to one {@link ApiChange}.</p>
     *
     * @param result the analysis result
     * @return a 2D array with the header row followed by data rows
     */
    public static String[][] toTable(CompatResult result) {
        var changes = result.changes();
        var rows = new String[changes.size() + 1][5];
        rows[0] = new String[]{"Change", "Kind", "Member", "Old Signature", "New Signature"};
        for (int i = 0; i < changes.size(); i++) {
            var c = changes.get(i);
            rows[i + 1] = new String[]{
                    c.kind().name(),
                    c.memberType(),
                    c.name(),
                    c.oldSignature(),
                    c.newSignature()
            };
        }
        return rows;
    }

    /**
     * Formats a {@link CompatResult} as a list of Markdown lines.
     *
     * <p>The report includes a summary header, breaking-change status, and a
     * Markdown table of all changes. Suitable for injection via
     * {@code sayRaw(String.join("\n", lines))}.</p>
     *
     * @param result the analysis result
     * @return ordered list of Markdown lines (never null, may be empty for no changes)
     */
    public static List<String> toMarkdown(CompatResult result) {
        var lines = new ArrayList<String>();

        lines.add("## API Compatibility: `%s` → `%s`".formatted(
                result.v1().getSimpleName(), result.v2().getSimpleName()));
        lines.add("");

        if (result.breaking()) {
            lines.add("> [!WARNING]");
            lines.add("> **Breaking changes detected.** Callers of `%s` must be updated."
                    .formatted(result.v1().getSimpleName()));
        } else {
            lines.add("> [!NOTE]");
            lines.add("> No breaking changes. `%s` is backward-compatible with `%s`."
                    .formatted(result.v2().getSimpleName(), result.v1().getSimpleName()));
        }
        lines.add("");

        if (result.changes().isEmpty()) {
            lines.add("_No API differences found._");
            return lines;
        }

        lines.add("| Change | Kind | Member | Old Signature | New Signature |");
        lines.add("| --- | --- | --- | --- | --- |");
        for (var c : result.changes()) {
            lines.add("| %s | %s | `%s` | `%s` | `%s` |".formatted(
                    c.kind().name(), c.memberType(), c.name(), c.oldSignature(), c.newSignature()));
        }

        return lines;
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private static void analyzeFields(Class<?> v1, Class<?> v2, List<ApiChange> out) {
        var v1Fields = publicFields(v1);
        var v2Fields = publicFields(v2);

        // REMOVED: in v1 but not v2
        for (var entry : v1Fields.entrySet()) {
            var name = entry.getKey();
            var f1 = entry.getValue();
            if (!v2Fields.containsKey(name)) {
                out.add(new ApiChange(
                        ChangeKind.REMOVED, "field", name,
                        fieldSig(f1), "-"));
            } else {
                var f2 = v2Fields.get(name);
                if (!f1.getType().equals(f2.getType())) {
                    out.add(new ApiChange(
                            ChangeKind.TYPE_CHANGED, "field", name,
                            fieldSig(f1), fieldSig(f2)));
                }
            }
        }

        // ADDED: in v2 but not v1
        for (var entry : v2Fields.entrySet()) {
            var name = entry.getKey();
            if (!v1Fields.containsKey(name)) {
                out.add(new ApiChange(
                        ChangeKind.ADDED, "field", name,
                        "-", fieldSig(entry.getValue())));
            }
        }
    }

    private static void analyzeMethods(Class<?> v1, Class<?> v2, List<ApiChange> out) {
        // Key: methodName + "(" + paramDescriptor + ")"  — uniquely identifies an overload
        var v1Methods = publicMethods(v1);
        var v2Methods = publicMethods(v2);

        // Also build name-only maps to detect same-name / different-return-type situations
        var v1ByName = byName(v1Methods);
        var v2ByName = byName(v2Methods);

        // REMOVED: in v1 but not v2
        for (var entry : v1Methods.entrySet()) {
            var descriptor = entry.getKey();
            var m1 = entry.getValue();
            if (!v2Methods.containsKey(descriptor)) {
                // Check if a method with the same name exists in v2 with different return type
                String name = m1.getName();
                if (v2ByName.containsKey(name)) {
                    // There's at least one method with the same name in v2 — report as SIGNATURE_CHANGED
                    // only once per name to avoid duplicate rows
                    if (!alreadyReported(out, "method", name, ChangeKind.SIGNATURE_CHANGED)) {
                        var m2 = v2ByName.get(name);
                        out.add(new ApiChange(
                                ChangeKind.SIGNATURE_CHANGED, "method", name,
                                methodSig(m1), methodSig(m2)));
                    }
                } else {
                    out.add(new ApiChange(
                            ChangeKind.REMOVED, "method", name,
                            methodSig(m1), "-"));
                }
            }
        }

        // ADDED: in v2 but not v1
        for (var entry : v2Methods.entrySet()) {
            var descriptor = entry.getKey();
            var m2 = entry.getValue();
            String name = m2.getName();
            if (!v1Methods.containsKey(descriptor) && !v1ByName.containsKey(name)) {
                if (!alreadyReported(out, "method", name, ChangeKind.ADDED)) {
                    out.add(new ApiChange(
                            ChangeKind.ADDED, "method", name,
                            "-", methodSig(m2)));
                }
            }
        }
    }

    private static boolean alreadyReported(List<ApiChange> changes,
                                            String memberType,
                                            String name,
                                            ChangeKind kind) {
        return changes.stream().anyMatch(c ->
                c.memberType().equals(memberType) && c.name().equals(name) && c.kind() == kind);
    }

    /** Returns public fields by simple name. */
    private static Map<String, Field> publicFields(Class<?> clazz) {
        var result = new LinkedHashMap<String, Field>();
        for (var f : clazz.getFields()) {
            if (Modifier.isPublic(f.getModifiers()) && !f.isSynthetic()) {
                result.put(f.getName(), f);
            }
        }
        return result;
    }

    /**
     * Returns public, non-synthetic, non-bridge methods keyed by
     * {@code name(paramDescriptor)} — each overload gets its own entry.
     */
    private static Map<String, Method> publicMethods(Class<?> clazz) {
        var result = new LinkedHashMap<String, Method>();
        for (var m : clazz.getMethods()) {
            if (Modifier.isPublic(m.getModifiers()) && !m.isSynthetic() && !m.isBridge()) {
                result.put(methodDescriptor(m), m);
            }
        }
        return result;
    }

    /** Returns a name-to-first-method map (for same-name lookup). */
    private static Map<String, Method> byName(Map<String, Method> methods) {
        var result = new LinkedHashMap<String, Method>();
        for (var m : methods.values()) {
            result.putIfAbsent(m.getName(), m);
        }
        return result;
    }

    private static String methodDescriptor(Method m) {
        var params = Arrays.stream(m.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(","));
        return m.getName() + "(" + params + ")";
    }

    private static String methodSig(Method m) {
        var params = Arrays.stream(m.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return "%s %s(%s)".formatted(m.getReturnType().getSimpleName(), m.getName(), params);
    }

    private static String fieldSig(Field f) {
        return "%s %s".formatted(f.getType().getSimpleName(), f.getName());
    }
}
