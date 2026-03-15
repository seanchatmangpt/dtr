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
package io.github.seanchatmangpt.dtr.adr;

import java.util.ArrayList;
import java.util.List;

/**
 * Architecture Decision Record (ADR) — lightweight living documentation for design choices.
 *
 * <p>An ADR captures the context, decision, and consequences of a significant architectural
 * choice. By modelling ADRs as Java records and rendering them through the DTR pipeline,
 * the records become executable documentation: they exist in the codebase alongside the
 * code they govern and are rendered by the same test suite that verifies that code.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * var adr = DecisionRecord.accepted(
 *     "001", "Use Java 26 with Preview Features",
 *     "The DTR project targets Java 26 to use sealed classes, records, and pattern matching.",
 *     "Enable --enable-preview in .mvn/maven.config and in all surefire plugin configurations.",
 *     "All contributors must use Java 26+. Preview APIs may change between releases."
 * );
 * sayRaw(String.join("\n", DecisionRecord.toMarkdown(adr)));
 * }</pre>
 *
 * <p>This class is intentionally stateless and final — all factory methods return
 * immutable {@link Adr} records. There is no mutable builder pattern and no
 * hidden configuration.</p>
 */
public final class DecisionRecord {

    private DecisionRecord() {
        // utility class — no instances
    }

    // -------------------------------------------------------------------------
    // Status enum
    // -------------------------------------------------------------------------

    /**
     * Lifecycle status of an Architecture Decision Record.
     *
     * <ul>
     *   <li>{@link #PROPOSED} — under discussion, not yet binding</li>
     *   <li>{@link #ACCEPTED} — in force, governing current design</li>
     *   <li>{@link #DEPRECATED} — superseded by a newer decision or rendered irrelevant</li>
     *   <li>{@link #SUPERSEDED} — explicitly replaced by another ADR</li>
     * </ul>
     */
    public enum Status {
        PROPOSED,
        ACCEPTED,
        DEPRECATED,
        SUPERSEDED;

        /**
         * Returns the GitHub-style badge text for this status, including a leading emoji
         * that provides at-a-glance signal in rendered markdown.
         *
         * @return badge string, e.g. {@code "⚠️ PROPOSED"} or {@code "✅ ACCEPTED"}
         */
        public String badge() {
            return switch (this) {
                case PROPOSED   -> "⚠️ PROPOSED";
                case ACCEPTED   -> "✅ ACCEPTED";
                case DEPRECATED -> "❌ DEPRECATED";
                case SUPERSEDED -> "🔄 SUPERSEDED";
            };
        }
    }

    // -------------------------------------------------------------------------
    // ADR record
    // -------------------------------------------------------------------------

    /**
     * Immutable value type representing a single Architecture Decision Record.
     *
     * @param id           short identifier, e.g. {@code "001"}
     * @param title        human-readable title, e.g. {@code "Use Java 26 with Preview Features"}
     * @param status       current lifecycle status
     * @param context      forces and constraints that drove the decision
     * @param decision     the choice that was made and how it was made
     * @param consequences positive and negative outcomes of the decision
     * @param alternatives other options that were considered but not chosen (may be empty)
     */
    public record Adr(
        String id,
        String title,
        Status status,
        String context,
        String decision,
        String consequences,
        List<String> alternatives
    ) {}

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates an {@link Adr} with all fields specified explicitly.
     *
     * @param id           short identifier
     * @param title        human-readable title
     * @param status       lifecycle status
     * @param context      forces that drove the decision
     * @param decision     the choice that was made
     * @param consequences outcomes of the decision
     * @param alternatives other options considered (may be an empty list)
     * @return a new, immutable {@link Adr}
     */
    public static Adr of(
            String id,
            String title,
            Status status,
            String context,
            String decision,
            String consequences,
            List<String> alternatives) {
        return new Adr(id, title, status, context, decision, consequences,
                List.copyOf(alternatives));
    }

    /**
     * Convenience factory for the common case of an already-accepted decision
     * with no explicit alternatives to record.
     *
     * <p>Equivalent to calling {@link #of} with {@link Status#ACCEPTED} and an
     * empty alternatives list.</p>
     *
     * @param id           short identifier
     * @param title        human-readable title
     * @param context      forces that drove the decision
     * @param decision     the choice that was made
     * @param consequences outcomes of the decision
     * @return a new, immutable {@link Adr} with status {@link Status#ACCEPTED}
     */
    public static Adr accepted(
            String id,
            String title,
            String context,
            String decision,
            String consequences) {
        return new Adr(id, title, Status.ACCEPTED, context, decision, consequences,
                List.of());
    }

    // -------------------------------------------------------------------------
    // Markdown rendering
    // -------------------------------------------------------------------------

    /**
     * Converts an {@link Adr} to a list of markdown lines ready for injection
     * via {@code sayRaw(String.join("\n", lines))}.
     *
     * <p>Output structure:</p>
     * <pre>
     * ## ADR-{id}: {title}
     *
     * **Status:** {badge}
     *
     * ### Context
     * {context}
     *
     * ### Decision
     * {decision}
     *
     * ### Consequences
     * {consequences}
     *
     * ### Alternatives Considered          ← omitted when alternatives is empty
     * - {alternative 1}
     * - {alternative 2}
     * </pre>
     *
     * @param adr the ADR to render (must not be null)
     * @return an ordered, non-empty list of markdown lines
     */
    public static List<String> toMarkdown(Adr adr) {
        var lines = new ArrayList<String>();

        // Heading
        lines.add("## ADR-%s: %s".formatted(adr.id(), adr.title()));
        lines.add("");

        // Status badge
        lines.add("**Status:** " + adr.status().badge());
        lines.add("");

        // Context subsection
        lines.add("### Context");
        lines.add("");
        lines.add(adr.context());
        lines.add("");

        // Decision subsection
        lines.add("### Decision");
        lines.add("");
        lines.add(adr.decision());
        lines.add("");

        // Consequences subsection
        lines.add("### Consequences");
        lines.add("");
        lines.add(adr.consequences());
        lines.add("");

        // Alternatives subsection — omitted when the list is empty
        if (!adr.alternatives().isEmpty()) {
            lines.add("### Alternatives Considered");
            lines.add("");
            for (var alternative : adr.alternatives()) {
                lines.add("- " + alternative);
            }
            lines.add("");
        }

        return List.copyOf(lines);
    }
}
