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

import java.util.List;
import java.util.Map;

import io.github.seanchatmangpt.dtr.crossref.DocTestRef;

/**
 * Sealed event hierarchy for the DTR render pipeline.
 *
 * <p>Every {@code say*} invocation on a {@link RenderMachine} corresponds to a
 * {@code SayEvent} subtype. The sealed hierarchy with exhaustive switch expressions
 * ensures compile-time completeness — adding a new event type forces every renderer
 * to handle it or fail compilation.</p>
 *
 * <p>This is the canonical demonstration of sealed classes + records + pattern matching
 * working together as a type-safe event system. The pattern:</p>
 * <pre>{@code
 * String rendered = switch (event) {
 *     case SayEvent.TextEvent(var text)           -> renderParagraph(text);
 *     case SayEvent.SectionEvent(var heading)     -> renderSection(heading);
 *     case SayEvent.CodeEvent(var code, var lang) -> renderCode(code, lang);
 *     // ... exhaustive — compiler enforces completeness
 * };
 * }</pre>
 *
 * <p>No visitor pattern. No dispatch maps. No instanceof chains. No defaults.
 * The type system proves every case is handled.</p>
 *
 * <p>Inspired by Project Babylon's code model approach: the event structure IS the
 * documentation contract, not a description of it.</p>
 *
 * @since 2.0.0
 */
public sealed interface SayEvent
        permits SayEvent.TextEvent,
                SayEvent.SectionEvent,
                SayEvent.CodeEvent,
                SayEvent.TableEvent,
                SayEvent.JsonEvent,
                SayEvent.NoteEvent,
                SayEvent.WarningEvent,
                SayEvent.KeyValueEvent,
                SayEvent.UnorderedListEvent,
                SayEvent.OrderedListEvent,
                SayEvent.AssertionsEvent,
                SayEvent.CitationEvent,
                SayEvent.FootnoteEvent,
                SayEvent.RefEvent,
                SayEvent.RawEvent,
                SayEvent.CodeModelEvent {

    /**
     * A narrative paragraph — the most fundamental documentation unit.
     *
     * @param text the paragraph text, may contain markdown formatting
     */
    record TextEvent(String text) implements SayEvent {
        public TextEvent {
            java.util.Objects.requireNonNull(text, "text must not be null");
        }
    }

    /**
     * A section heading that appears in the table of contents.
     *
     * @param heading the section title
     */
    record SectionEvent(String heading) implements SayEvent {
        public SectionEvent {
            java.util.Objects.requireNonNull(heading, "heading must not be null");
        }
    }

    /**
     * A fenced code block with optional language for syntax highlighting.
     *
     * @param code     the source code content
     * @param language the language hint (e.g., "java", "sql", "json") — may be empty
     */
    record CodeEvent(String code, String language) implements SayEvent {}

    /**
     * A markdown table rendered from a 2D string array.
     * The first row becomes the table header.
     *
     * @param data 2D array of cell values; {@code data[0]} is the header row
     */
    record TableEvent(String[][] data) implements SayEvent {}

    /**
     * A JSON code block rendered from a serializable object.
     *
     * @param object the object to serialize as pretty-printed JSON
     */
    record JsonEvent(Object object) implements SayEvent {}

    /**
     * An informational callout — GitHub-style {@code > [!NOTE]}.
     *
     * @param message the note content
     */
    record NoteEvent(String message) implements SayEvent {}

    /**
     * A warning callout — GitHub-style {@code > [!WARNING]}.
     *
     * @param message the warning content
     */
    record WarningEvent(String message) implements SayEvent {}

    /**
     * Key-value pairs rendered as a two-column markdown table.
     *
     * @param pairs the key→value mapping to render
     */
    record KeyValueEvent(Map<String, String> pairs) implements SayEvent {}

    /**
     * An unordered bullet list.
     *
     * @param items the list items
     */
    record UnorderedListEvent(List<String> items) implements SayEvent {}

    /**
     * An ordered (numbered) list.
     *
     * @param items the list items in display order
     */
    record OrderedListEvent(List<String> items) implements SayEvent {}

    /**
     * An assertions summary table with Check and Result columns.
     *
     * @param assertions map of check descriptions to result strings
     */
    record AssertionsEvent(Map<String, String> assertions) implements SayEvent {}

    /**
     * A BibTeX citation reference, optionally with a page number.
     *
     * @param citationKey the BibTeX key
     * @param pageRef     optional page reference (e.g., "42", "pp. 10-15")
     */
    record CitationEvent(String citationKey, java.util.Optional<String> pageRef)
            implements SayEvent {}

    /**
     * A footnote to be rendered at the end of the document section.
     *
     * @param text the footnote content
     */
    record FootnoteEvent(String text) implements SayEvent {}

    /**
     * A cross-reference link to another DocTest's section.
     *
     * @param ref the target reference
     */
    record RefEvent(DocTestRef ref) implements SayEvent {}

    /**
     * Raw markdown content injected directly into the output.
     *
     * @param markdown the raw markdown string
     */
    record RawEvent(String markdown) implements SayEvent {}

    /**
     * A code model introspection event — the DTR stand-in for Project Babylon's
     * Code Reflection API (JEP 494).
     *
     * <p>Renders a class's sealed hierarchy, record components, and public method
     * signatures derived from the bytecode. The documentation cannot drift from the
     * implementation because it IS the implementation.</p>
     *
     * @param clazz the class to introspect and document
     */
    record CodeModelEvent(Class<?> clazz) implements SayEvent {
        public CodeModelEvent {
            java.util.Objects.requireNonNull(clazz, "clazz must not be null");
        }
    }
}
