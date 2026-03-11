/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.r10r.doctester.assembly;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builder for document-wide table of contents extracted from all DocTests.
 */
public final class TableOfContents {

    /**
     * Immutable record representing a single TOC entry.
     */
    public record TocEntry(String docTest, String title, int level, int pageNumber) {

        /**
         * Validates entry on construction.
         */
        public TocEntry {
            if (docTest == null || docTest.trim().isEmpty()) {
                throw new IllegalArgumentException("docTest cannot be null or empty");
            }
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalArgumentException("title cannot be null or empty");
            }
            if (level < 1 || level > 3) {
                throw new IllegalArgumentException("level must be 1, 2, or 3");
            }
        }

        /**
         * LaTeX section command for this entry.
         */
        public String latexCommand() {
            return switch (level) {
                case 1 -> "section";
                case 2 -> "subsection";
                case 3 -> "subsubsection";
                default -> throw new IllegalStateException("Invalid level: " + level);
            };
        }

        /**
         * Human-readable string representation.
         */
        @Override
        public String toString() {
            return "  ".repeat(Math.max(0, level - 1)) + title + " [page " + pageNumber + "]";
        }
    }

    private final List<TocEntry> entries = new ArrayList<>();

    /**
     * Creates an empty table of contents builder.
     */
    public TableOfContents() {
    }

    /**
     * Adds a section entry to the table of contents.
     */
    public void addEntry(String docTest, String sectionTitle, int level) {
        if (docTest == null || docTest.trim().isEmpty()) {
            throw new IllegalArgumentException("docTest cannot be null or empty");
        }
        if (sectionTitle == null || sectionTitle.trim().isEmpty()) {
            throw new IllegalArgumentException("sectionTitle cannot be null or empty");
        }
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("level must be 1, 2, or 3");
        }

        entries.add(new TocEntry(docTest, sectionTitle, level, 0));
    }

    /**
     * Returns all table of contents entries in hierarchical order.
     */
    public List<TocEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * Generates the LaTeX command for table of contents.
     */
    public String generateLatex() {
        return "\\tableofcontents\n\\newpage\n";
    }

    /**
     * Returns a human-readable text representation of the table of contents.
     */
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Table of Contents\n");
        sb.append("==================\n\n");

        for (var entry : entries) {
            sb.append(entry).append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns the number of entries in the table of contents.
     */
    public int size() {
        return entries.size();
    }

    /**
     * Clears all entries from this table of contents.
     */
    public void clear() {
        entries.clear();
    }
}
