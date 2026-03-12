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
package io.github.seanchatmangpt.dtr.bibliography;

import java.util.Map;

/**
 * Renders BibTeXEntry objects in multiple citation styles.
 *
 * Supports:
 * - IEEE numeric style: [1], [2], [3]
 * - ACM/Author-year style: (Author 2026)
 * - Nature author-year with natbib
 */
public final class BibTeXRenderer {

    /**
     * Enumeration of supported citation styles.
     */
    public enum CitationStyle {
        IEEE_NUMERIC,
        ACM_AUTHOR_YEAR,
        NATURE_NATBIB,
        MARKDOWN_INLINE
    }

    private BibTeXRenderer() {
        // Utility class, no instantiation
    }

    /**
     * Renders a single citation in IEEE numeric style.
     *
     * @param index the numeric index (1, 2, 3, ...)
     * @return formatted citation like "[1]"
     */
    public static String renderIEEENumeric(int index) {
        return "[%d]".formatted(index);
    }

    /**
     * Renders a single citation in ACM author-year style.
     *
     * @param entry the BibTeX entry to render
     * @return formatted citation like "(Smith 2026)"
     */
    public static String renderACMAuthorYear(BibTeXEntry entry) {
        String author = extractFirstAuthor(entry.getField("author"));
        String year = entry.getField("year");

        if (author.isEmpty() || year.isEmpty()) {
            return "(" + entry.key() + ")";
        }

        return "(%s %s)".formatted(author, year);
    }

    /**
     * Renders a single citation in markdown inline format.
     *
     * @param key the citation key
     * @return formatted citation like "[key]"
     */
    public static String renderMarkdownInline(String key) {
        return "[%s]".formatted(key);
    }

    /**
     * Renders a bibliography section in markdown format.
     *
     * @param entries the bibliography entries to render
     * @param style the citation style to use
     * @return formatted bibliography markdown
     */
    public static String renderBibliography(Map<String, BibTeXEntry> entries, CitationStyle style) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n## Bibliography\n\n");

        int index = 1;
        for (BibTeXEntry entry : entries.values()) {
            switch (style) {
                case IEEE_NUMERIC -> {
                    sb.append(renderIEEENumeric(index)).append(" ");
                    sb.append(renderEntryMarkdown(entry));
                    sb.append("\n\n");
                    index++;
                }
                case ACM_AUTHOR_YEAR -> {
                    sb.append(renderACMAuthorYear(entry)).append(": ");
                    sb.append(renderEntryMarkdown(entry));
                    sb.append("\n\n");
                }
                case NATURE_NATBIB -> {
                    sb.append(renderEntryMarkdown(entry));
                    sb.append("\n\n");
                }
                case MARKDOWN_INLINE -> {
                    sb.append("- ").append(entry.key()).append(": ");
                    sb.append(renderEntryMarkdown(entry));
                    sb.append("\n\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Generates a complete LaTeX bibliography block with entries.
     *
     * @param entries the bibliography entries
     * @param style the citation style
     * @return formatted LaTeX thebibliography environment
     */
    public static String renderLatexBibliography(Map<String, BibTeXEntry> entries, CitationStyle style) {
        if (entries == null || entries.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        // Add bibliography style directive
        switch (style) {
            case IEEE_NUMERIC -> sb.append("\\bibliographystyle{ieeetr}\n");
            case ACM_AUTHOR_YEAR -> sb.append("\\bibliographystyle{acmauthoryear}\n");
            case NATURE_NATBIB -> sb.append("\\usepackage{natbib}\n\\bibliographystyle{apalike}\n");
            case MARKDOWN_INLINE -> sb.append("% Bibliography\n");
        }

        // Begin bibliography environment
        int maxWidth = String.valueOf(entries.size()).length();
        sb.append("\n\\begin{thebibliography}{");
        for (int i = 0; i < maxWidth; i++) {
            sb.append("9");
        }
        sb.append("}\n\n");

        // Add entries
        int index = 1;
        for (BibTeXEntry entry : entries.values()) {
            sb.append("\\bibitem{").append(entry.key()).append("}\n");
            sb.append(renderEntryLatex(entry, index, style));
            sb.append("\n\n");
            index++;
        }

        sb.append("\\end{thebibliography}\n");

        return sb.toString();
    }

    /**
     * Renders a single entry as markdown.
     *
     * @param entry the BibTeX entry
     * @return formatted entry as markdown
     */
    public static String renderEntryMarkdown(BibTeXEntry entry) {
        return switch (entry.type()) {
            case "article" -> renderArticleMarkdown(entry);
            case "book" -> renderBookMarkdown(entry);
            case "inproceedings" -> renderInProceedingsMarkdown(entry);
            case "techreport" -> renderTechReportMarkdown(entry);
            default -> entry.key();
        };
    }

    /**
     * Renders a single entry as LaTeX.
     *
     * @param entry the BibTeX entry
     * @param index the numeric index
     * @param style the citation style
     * @return formatted entry as LaTeX
     */
    public static String renderEntryLatex(BibTeXEntry entry, int index, CitationStyle style) {
        return switch (entry.type()) {
            case "article" -> renderArticleLatex(entry);
            case "book" -> renderBookLatex(entry);
            case "inproceedings" -> renderInProceedingsLatex(entry);
            case "techreport" -> renderTechReportLatex(entry);
            default -> entry.key();
        };
    }

    // Markdown renderers

    private static String renderArticleMarkdown(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String journal = entry.getField("journal");
        String year = entry.getField("year");
        String volume = entry.getField("volume");
        String number = entry.getField("number");
        String pages = entry.getField("pages");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(". ");
        if (!title.isEmpty()) sb.append(title).append(". ");
        if (!journal.isEmpty()) sb.append("*").append(journal).append("*");
        if (!volume.isEmpty()) sb.append(", ").append(volume);
        if (!number.isEmpty()) sb.append("(").append(number).append(")");
        if (!pages.isEmpty()) sb.append(":").append(pages);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    private static String renderBookMarkdown(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String publisher = entry.getField("publisher");
        String year = entry.getField("year");
        String edition = entry.getField("edition");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(". ");
        if (!title.isEmpty()) sb.append("*").append(title).append("*. ");
        if (!edition.isEmpty()) sb.append(edition).append(" edition. ");
        if (!publisher.isEmpty()) sb.append(publisher);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    private static String renderInProceedingsMarkdown(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String booktitle = entry.getField("booktitle");
        String year = entry.getField("year");
        String pages = entry.getField("pages");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(". ");
        if (!title.isEmpty()) sb.append(title).append(". ");
        if (!booktitle.isEmpty()) sb.append("In *").append(booktitle).append("*");
        if (!pages.isEmpty()) sb.append(":").append(pages);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    private static String renderTechReportMarkdown(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String institution = entry.getField("institution");
        String year = entry.getField("year");
        String number = entry.getField("number");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(". ");
        if (!title.isEmpty()) sb.append(title).append(". ");
        if (!institution.isEmpty()) sb.append("*").append(institution).append("*");
        if (!number.isEmpty()) sb.append(", Technical Report ").append(number);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    // LaTeX renderers

    private static String renderArticleLatex(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String journal = entry.getField("journal");
        String year = entry.getField("year");
        String volume = entry.getField("volume");
        String number = entry.getField("number");
        String pages = entry.getField("pages");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(", ");
        if (!title.isEmpty()) sb.append("``").append(title).append(",'', ");
        if (!journal.isEmpty()) sb.append("\\textit{").append(journal).append("}");
        if (!volume.isEmpty()) sb.append(", vol.~").append(volume);
        if (!number.isEmpty()) sb.append(", no.~").append(number);
        if (!pages.isEmpty()) sb.append(", pp.~").append(pages);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    private static String renderBookLatex(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String publisher = entry.getField("publisher");
        String year = entry.getField("year");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(", ");
        if (!title.isEmpty()) sb.append("\\textit{").append(title).append("}, ");
        if (!publisher.isEmpty()) sb.append(publisher);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    private static String renderInProceedingsLatex(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String booktitle = entry.getField("booktitle");
        String year = entry.getField("year");
        String pages = entry.getField("pages");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(", ");
        if (!title.isEmpty()) sb.append("``").append(title).append(",'', ");
        if (!booktitle.isEmpty()) sb.append("\\textit{").append(booktitle).append("}");
        if (!pages.isEmpty()) sb.append(", pp.~").append(pages);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    private static String renderTechReportLatex(BibTeXEntry entry) {
        String author = entry.getField("author");
        String title = entry.getField("title");
        String institution = entry.getField("institution");
        String year = entry.getField("year");
        String number = entry.getField("number");

        StringBuilder sb = new StringBuilder();
        if (!author.isEmpty()) sb.append(author).append(", ");
        if (!title.isEmpty()) sb.append("``").append(title).append(",'', ");
        if (!institution.isEmpty()) sb.append("\\textit{").append(institution).append("}");
        if (!number.isEmpty()) sb.append(", Tech. Rep.~").append(number);
        if (!year.isEmpty()) sb.append(", ").append(year);
        sb.append(".");

        return sb.toString();
    }

    /**
     * Extracts the first author from an author field.
     *
     * @param authorField the author field (may contain "and" separators)
     * @return the first author surname
     */
    private static String extractFirstAuthor(String authorField) {
        if (authorField.isEmpty()) {
            return "";
        }

        // Split by "and"
        String[] authors = authorField.split("\\s+and\\s+", 2);
        String firstAuthor = authors[0].trim();

        // Extract last name (after the comma or last space)
        if (firstAuthor.contains(",")) {
            return firstAuthor.substring(0, firstAuthor.indexOf(',')).trim();
        }

        String[] parts = firstAuthor.split("\\s+");
        return parts.length > 0 ? parts[parts.length - 1] : "";
    }
}
