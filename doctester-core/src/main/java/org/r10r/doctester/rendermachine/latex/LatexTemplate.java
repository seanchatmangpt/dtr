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
package org.r10r.doctester.rendermachine.latex;

/**
 * Sealed interface for LaTeX document templates.
 *
 * Each implementation provides document class, preamble, and formatting
 * conventions for a specific target format (ArXiv, USPTO, IEEE, ACM, Nature).
 *
 * Phase 2.1.0 ships with ArXiv and USPTO templates only.
 */
public sealed interface LatexTemplate permits ArXivTemplate, UsPatentTemplate {

    /**
     * LaTeX document class (e.g., "article", "report", custom).
     */
    String getDocumentClass();

    /**
     * Full preamble including \\documentclass, \\usepackage, \\title, etc.
     */
    String getPreamble();

    /**
     * Text to insert after \\begin{document}.
     */
    String getBeginDocument();

    /**
     * Text to insert before \\end{document}.
     */
    String getEndDocument();

    /**
     * LaTeX command for section-level heading (e.g., "\\\\section{%s}").
     * Placeholder %s will be replaced with heading text.
     */
    String formatSection(String title);

    /**
     * LaTeX command for subsection-level heading (e.g., "\\\\subsection{%s}").
     */
    String formatSubsection(String title);

    /**
     * Escape special LaTeX characters to prevent syntax errors.
     * Must handle: _ $ % &amp; # ^ ~ \\ { }
     */
    String escapeLatex(String text);

    /**
     * Format code block with optional language syntax highlighting.
     * Language may be null; implementations should handle gracefully.
     */
    String formatCodeBlock(String code, String language);

    /**
     * Format key-value pairs as a 2-column LaTeX table.
     */
    String formatKeyValue(java.util.Map<String, String> pairs);

    /**
     * Format an unordered (bullet) list in LaTeX.
     */
    String formatUnorderedList(java.util.List<String> items);

    /**
     * Format an ordered (numbered) list in LaTeX.
     */
    String formatOrderedList(java.util.List<String> items);

    /**
     * Format a table from 2D string array.
     * First row is headers.
     */
    String formatTable(String[][] data);

    /**
     * Format a warning callout box (colored background, icon, etc.).
     */
    String formatWarning(String message);

    /**
     * Format an info/note callout box (colored background, icon, etc.).
     */
    String formatNote(String message);

    /**
     * Format assertion results as a table (Check | Result columns).
     */
    String formatAssertions(java.util.Map<String, String> assertions);

    /**
     * Format JSON/pretty-printed content in a code block.
     */
    String formatJson(String jsonString);
}
