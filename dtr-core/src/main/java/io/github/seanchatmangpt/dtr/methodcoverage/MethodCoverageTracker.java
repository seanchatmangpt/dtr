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
package io.github.seanchatmangpt.dtr.methodcoverage;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compares a set of called method names against all public methods of a class,
 * producing a coverage report that can be rendered as a table or markdown.
 *
 * <p>Coverage is name-based: a method is considered covered if its simple name
 * appears in the {@code calledMethods} set. Overloaded methods sharing the same
 * name are all marked covered when any variant of that name is present.</p>
 *
 * <p>Only methods declared directly on the target class are considered —
 * methods inherited from {@link Object} and synthetic methods are excluded.
 * Methods are sorted alphabetically by name for stable, readable output.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * var called = Set.of("compare", "toTable");
 * var result = MethodCoverageTracker.analyze(BenchmarkComparison.class, called);
 * sayTable(MethodCoverageTracker.toTable(result));
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class MethodCoverageTracker {

    private MethodCoverageTracker() {}

    // -------------------------------------------------------------------------
    // Result records
    // -------------------------------------------------------------------------

    /**
     * Coverage data for a single public method.
     *
     * @param methodName simple method name (e.g., {@code "compare"})
     * @param signature  human-readable signature using simple type names,
     *                   e.g., {@code "compare(Map, int, int)"}
     * @param covered    {@code true} if the method name appeared in the called-methods set
     */
    public record CoverageEntry(String methodName, String signature, boolean covered) {}

    /**
     * Full coverage report for a class.
     *
     * @param className fully qualified name of the analysed class
     * @param entries   one entry per public declared method, sorted by method name
     * @param covered   count of methods whose name appeared in the called-methods set
     * @param total     total public declared method count
     */
    public record CoverageResult(String className, List<CoverageEntry> entries, int covered, int total) {

        /**
         * Returns the coverage percentage, or {@code 0.0} if there are no public methods.
         *
         * @return a value in the range {@code [0.0, 100.0]}
         */
        public double percentage() {
            return total == 0 ? 0.0 : (double) covered / total * 100.0;
        }
    }

    // -------------------------------------------------------------------------
    // Analysis
    // -------------------------------------------------------------------------

    /**
     * Analyses the public declared methods of {@code clazz} against the set of
     * {@code calledMethods} names, returning a {@link CoverageResult}.
     *
     * <p>Rules:</p>
     * <ul>
     *   <li>Only methods returned by {@link Class#getDeclaredMethods()} are considered.</li>
     *   <li>Non-public and synthetic methods are excluded.</li>
     *   <li>Methods whose declaring class is {@link Object} are excluded.</li>
     *   <li>A method is covered when {@code calledMethods} contains its exact simple name.</li>
     *   <li>Results are sorted alphabetically by method name.</li>
     * </ul>
     *
     * <p>This method never throws; a {@code null} {@code clazz} returns an empty result,
     * and a {@code null} {@code calledMethods} is treated as an empty set.</p>
     *
     * @param clazz         the class to analyse (may be {@code null})
     * @param calledMethods set of simple method names that were exercised (may be {@code null})
     * @return a non-null {@link CoverageResult}
     */
    public static CoverageResult analyze(Class<?> clazz, Set<String> calledMethods) {
        if (clazz == null) {
            return new CoverageResult("null", List.of(), 0, 0);
        }

        var called = calledMethods != null ? calledMethods : Set.<String>of();

        var publicMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .filter(m -> !m.isSynthetic())
                .filter(m -> !m.getDeclaringClass().equals(Object.class))
                .sorted(Comparator.comparing(Method::getName))
                .toList();

        var entries = new ArrayList<CoverageEntry>(publicMethods.size());
        int coveredCount = 0;

        for (var m : publicMethods) {
            var sig = buildSignature(m);
            boolean isCovered = called.contains(m.getName());
            entries.add(new CoverageEntry(m.getName(), sig, isCovered));
            if (isCovered) {
                coveredCount++;
            }
        }

        return new CoverageResult(
                clazz.getName(),
                List.copyOf(entries),
                coveredCount,
                publicMethods.size()
        );
    }

    // -------------------------------------------------------------------------
    // Rendering
    // -------------------------------------------------------------------------

    /**
     * Converts a {@link CoverageResult} into a list of markdown lines with a heading,
     * a GitHub-flavoured markdown table, and a one-line summary.
     *
     * <p>The returned list is suitable for joining with {@code "\n"} or passing
     * line-by-line to {@code sayRaw()}.</p>
     *
     * @param result a non-null {@link CoverageResult}
     * @return mutable list of markdown lines (never {@code null}, never empty)
     */
    public static List<String> toMarkdown(CoverageResult result) {
        var lines = new ArrayList<String>();

        lines.add("### Method Coverage: " + result.className());
        lines.add("");
        lines.add("| Method | Signature | Status |");
        lines.add("|--------|-----------|--------|");

        for (var e : result.entries()) {
            var status = e.covered() ? "✓" : "✗";
            lines.add("| %s | `%s` | %s |".formatted(e.methodName(), e.signature(), status));
        }

        lines.add("");
        lines.add("Coverage: %d/%d (%.1f%%)".formatted(
                result.covered(), result.total(), result.percentage()));

        return lines;
    }

    /**
     * Converts a {@link CoverageResult} into a {@code String[][]} table suitable
     * for passing directly to {@code sayTable()}.
     *
     * <p>Headers: {@code ["Method", "Signature", "Covered"]}</p>
     * <p>Status cells use {@code ✓} for covered and {@code ✗} for not covered.</p>
     *
     * @param result a non-null {@link CoverageResult}
     * @return a 2D array with one header row followed by one row per entry
     */
    public static String[][] toTable(CoverageResult result) {
        var entries = result.entries();
        var rows = new String[entries.size() + 1][3];
        rows[0] = new String[]{"Method", "Signature", "Covered"};

        for (int i = 0; i < entries.size(); i++) {
            var e = entries.get(i);
            rows[i + 1] = new String[]{
                e.methodName(),
                e.signature(),
                e.covered() ? "✓" : "✗"
            };
        }

        return rows;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String buildSignature(Method m) {
        var params = Arrays.stream(m.getParameterTypes())
                .map(Class::getSimpleName)
                .collect(Collectors.joining(", "));
        return "%s(%s)".formatted(m.getName(), params);
    }
}
