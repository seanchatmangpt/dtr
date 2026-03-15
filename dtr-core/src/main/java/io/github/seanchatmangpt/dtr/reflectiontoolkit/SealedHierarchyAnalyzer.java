/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.seanchatmangpt.dtr.reflectiontoolkit;

import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Analyses a sealed interface or abstract class and produces a structured
 * representation of its complete permitted-subtype hierarchy, including
 * record components and further sealing.
 *
 * <p>No other documentation tool automatically maps sealed type trees.
 * {@code SealedHierarchyAnalyzer} uses pure reflection — no bytecode
 * manipulation, no agents — and works at runtime with the classes that
 * are actually loaded.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * HierarchyResult result = SealedHierarchyAnalyzer.analyze(SayEvent.class);
 * // result.subtypes() contains every permitted subtype in DFS order
 * }</pre>
 *
 * @since 2026.1.0
 */
public final class SealedHierarchyAnalyzer {

    private SealedHierarchyAnalyzer() {}

    /**
     * Information about a single permitted subtype in the sealed hierarchy.
     *
     * @param name       simple class name
     * @param kind       one of: "record", "sealed interface", "interface",
     *                   "sealed abstract class", "abstract class", "final class", "class"
     * @param components comma-separated record component summary, or empty string for non-records
     * @param isSealed   true when this subtype is itself sealed (and its children follow in the flat list)
     * @param depth      1-based nesting depth (1 = direct child of root)
     */
    public record SubtypeInfo(
            String name,
            String kind,
            String components,
            boolean isSealed,
            int depth
    ) {
        public SubtypeInfo {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(kind, "kind must not be null");
            Objects.requireNonNull(components, "components must not be null");
            if (depth < 1) throw new IllegalArgumentException("depth must be >= 1, got " + depth);
        }
    }

    /**
     * The full hierarchy result for a root sealed type.
     *
     * @param rootName    simple name of the root type
     * @param rootIsSealed true when the root is sealed (always true unless a non-sealed class was passed)
     * @param subtypes    flat DFS list of all permitted subtypes at all depths
     */
    public record HierarchyResult(
            String rootName,
            boolean rootIsSealed,
            List<SubtypeInfo> subtypes
    ) {
        public HierarchyResult {
            Objects.requireNonNull(rootName, "rootName must not be null");
            Objects.requireNonNull(subtypes, "subtypes must not be null");
            subtypes = List.copyOf(subtypes);
        }
    }

    /**
     * Analyses {@code root} and returns its full sealed subtype hierarchy.
     *
     * <p>If {@code root} is not sealed, returns a {@link HierarchyResult} with
     * {@code rootIsSealed == false} and an empty subtypes list rather than
     * throwing an exception.</p>
     *
     * @param root the class or interface to analyse; must not be null
     * @return the complete hierarchy result
     * @throws NullPointerException if {@code root} is null
     */
    public static HierarchyResult analyze(Class<?> root) {
        Objects.requireNonNull(root, "root must not be null");
        if (!root.isSealed()) {
            return new HierarchyResult(root.getSimpleName(), false, List.of());
        }
        var subtypes = new ArrayList<SubtypeInfo>();
        collectSubtypes(root, 1, subtypes);
        return new HierarchyResult(root.getSimpleName(), true, subtypes);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static void collectSubtypes(Class<?> parent, int depth, List<SubtypeInfo> out) {
        for (var sub : parent.getPermittedSubclasses()) {
            var kind = resolveKind(sub);
            var components = resolveComponents(sub);
            out.add(new SubtypeInfo(sub.getSimpleName(), kind, components, sub.isSealed(), depth));
            if (sub.isSealed()) {
                collectSubtypes(sub, depth + 1, out);
            }
        }
    }

    private static String resolveKind(Class<?> sub) {
        if (sub.isRecord()) {
            return "record";
        }
        if (sub.isInterface()) {
            return sub.isSealed() ? "sealed interface" : "interface";
        }
        if (Modifier.isAbstract(sub.getModifiers())) {
            return sub.isSealed() ? "sealed abstract class" : "abstract class";
        }
        if (Modifier.isFinal(sub.getModifiers())) {
            return "final class";
        }
        return "class";
    }

    private static String resolveComponents(Class<?> sub) {
        if (!sub.isRecord()) {
            return "";
        }
        RecordComponent[] rc = sub.getRecordComponents();
        if (rc == null || rc.length == 0) {
            return "";
        }
        var sb = new StringBuilder();
        for (int i = 0; i < rc.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(rc[i].getType().getSimpleName()).append(" ").append(rc[i].getName());
        }
        return sb.toString();
    }
}
