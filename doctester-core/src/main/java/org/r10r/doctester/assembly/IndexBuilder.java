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
import java.util.TreeMap;

/**
 * Builder for technical index of terms appearing in the assembled document.
 */
public final class IndexBuilder {

    /**
     * Immutable record representing an indexed term.
     */
    public record IndexedTerm(String term, List<Integer> pageNumbers) {

        /**
         * Validates term on construction.
         */
        public IndexedTerm {
            if (term == null || term.trim().isEmpty()) {
                throw new IllegalArgumentException("term cannot be null or empty");
            }
            if (pageNumbers == null || pageNumbers.isEmpty()) {
                throw new IllegalArgumentException("pageNumbers cannot be null or empty");
            }
            pageNumbers = Collections.unmodifiableList(new ArrayList<>(pageNumbers));
        }

        /**
         * Generates LaTeX index command for this term.
         */
        public String toLatex() {
            var escaped = escapeLaTeX(term);
            return "\\index{" + escaped + "}";
        }

        /**
         * Human-readable string: term (pages 3, 5, 8).
         */
        @Override
        public String toString() {
            var pageStr = pageNumbers.stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + ", " + b)
                .orElse("");
            return term + " (pages " + pageStr + ")";
        }
    }

    private final TreeMap<String, List<Integer>> index = new TreeMap<>();

    /**
     * Creates an empty index builder.
     */
    public IndexBuilder() {
    }

    /**
     * Adds a technical term to the index at the given page number.
     */
    public void addTerm(String term, int pageNumber) {
        if (term == null || term.trim().isEmpty()) {
            throw new IllegalArgumentException("term cannot be null or empty");
        }
        if (pageNumber < 1) {
            throw new IllegalArgumentException("pageNumber must be positive");
        }

        var pages = index.computeIfAbsent(term, _ -> new ArrayList<>());
        if (!pages.contains(pageNumber)) {
            pages.add(pageNumber);
            pages.sort(Integer::compareTo);
        }
    }

    /**
     * Returns all indexed terms in alphabetical order with their page numbers.
     */
    public List<IndexedTerm> getTerms() {
        return index.entrySet().stream()
            .map(entry -> new IndexedTerm(entry.getKey(), new ArrayList<>(entry.getValue())))
            .toList();
    }

    /**
     * Generates LaTeX index entries for all terms.
     */
    public String generateLatex() {
        var sb = new StringBuilder();
        sb.append("% Index generated for ").append(index.size()).append(" terms\n");

        for (var term : getTerms()) {
            sb.append(term.toLatex()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns a human-readable text representation of the index.
     */
    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("Index (").append(index.size()).append(" terms)\n");
        sb.append("==================================\n\n");

        for (var term : getTerms()) {
            sb.append(term).append("\n");
        }

        return sb.toString();
    }

    /**
     * Returns the number of unique terms in the index.
     */
    public int size() {
        return index.size();
    }

    /**
     * Clears all terms from the index.
     */
    public void clear() {
        index.clear();
    }

    /**
     * Escapes LaTeX special characters in a string.
     */
    private static String escapeLaTeX(String s) {
        if (s == null) {
            return "";
        }
        return s
            .replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde{}")
            .replace("^", "\\textasciicircum{}")
            .replace("|", "\\textbar{}");
    }
}
