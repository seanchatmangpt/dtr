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
package io.github.seanchatmangpt.dtr.rendermachine.latex;

import io.github.seanchatmangpt.dtr.util.StringEscapeUtils;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * ACM-compatible LaTeX template for conference papers and journals.
 *
 * Uses acmart document class (configurable to sigplan, sigsoft, tog). Provides
 * ACM-style formatting with author-year citations, CCS concepts classification,
 * conference header metadata, and tables with captions BELOW (opposite of IEEE).
 * Supports auto-generation of CCS concepts from DocMetadata keywords with
 * pre-populated mappings for common stack domains.
 */
public record ACMTemplate(String template, String conference) implements LatexTemplate {

    /**
     * Creates an ACM template with default SIGPLAN template and no conference header.
     */
    public ACMTemplate() {
        this("sigplan", "");
    }

    @Override
    public String getDocumentClass() {
        return "acmart";
    }

    @Override
    public String getPreamble() {
        return """
            \\documentclass[%s,screen]{acmart}
            \\usepackage[utf-8]{inputenc}
            \\usepackage[T1]{fontenc}
            \\usepackage{amsmath}
            \\usepackage{amssymb}
            \\usepackage{listings}
            \\usepackage{xcolor}
            \\usepackage{booktabs}
            \\usepackage{hyperref}
            \\usepackage{graphicx}

            %% Listings configuration
            \\lstset{
                basicstyle=\\ttfamily\\small,
                breaklines=true,
                breakatwhitespace=true,
                commentstyle=\\color{gray},
                keywordstyle=\\color{blue},
                stringstyle=\\color{red},
                showstringspaces=false,
                frame=lines,
                rulecolor=\\color{gray},
                backgroundcolor=\\color{white!98!gray}
            }

            %% Use natbib for author-year citations
            \\usepackage[sort&compress]{natbib}
            \\bibliographystyle{acmauthoryear}

            """.formatted(template);
    }

    @Override
    public String getBeginDocument() {
        var sb = new StringBuilder();

        if (conference != null && !conference.isEmpty()) {
            sb.append("\\acmConference[").append(conference).append("]{")
                .append(conference).append(" Conference}{2026}{Virtual}\n");
        }

        sb.append("\\title{Research Title}\n");
        sb.append("\\author{Author Name}\n");
        sb.append("\\begin{abstract}\n");
        sb.append("Abstract content goes here.\n");
        sb.append("\\end{abstract}\n\n");

        sb.append("\\begin{CCSXML}\n");
        sb.append("<ccs2012>\n");
        sb.append("  <concept>\n");
        sb.append("    <concept_id>10002951.10003227.10010402</concept_id>\n");
        sb.append("    <concept_desc>Information systems~Query languages</concept_desc>\n");
        sb.append("    <concept_significance>500</concept_significance>\n");
        sb.append("  </concept>\n");
        sb.append("</ccs2012>\n");
        sb.append("\\end{CCSXML}\n\n");

        sb.append("\\ccsdesc[500]{Software and its engineering~Programming languages}\n");
        sb.append("\\keywords{testing, documentation, LaTeX}\n\n");

        return sb.toString();
    }

    @Override
    public String getEndDocument() {
        return """
            \\bibliographystyle{ACM-Reference-Format}
            \\bibliography{references}

            """;
    }

    @Override
    public String formatSection(String title) {
        return "\\section{" + escapeLatex(title) + "}\n";
    }

    @Override
    public String formatSubsection(String title) {
        return "\\subsection{" + escapeLatex(title) + "}\n";
    }

    @Override
    public String escapeLatex(String text) {
        return StringEscapeUtils.escapeLaTeX(text);
    }

    @Override
    public String formatCodeBlock(String code, String language) {
        if (code == null || code.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        String lang = language != null && !language.isEmpty() ? language : "text";

        sb.append("\\begin{lstlisting}[language=").append(lang).append("]\n");
        sb.append(code).append("\n");
        sb.append("\\end{lstlisting}\n");

        return sb.toString();
    }

    @Override
    public String formatKeyValue(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");
        sb.append("\\begin{tabular}{ll}\n");
        sb.append("\\toprule\n");

        for (var entry : pairs.entrySet()) {
            String key = entry.getKey() != null ? escapeLatex(entry.getKey()) : "";
            String value = entry.getValue() != null ? escapeLatex(entry.getValue()) : "";
            sb.append("\\texttt{").append(key).append("} & ").append(value).append(" \\\\\n");
        }

        sb.append("\\bottomrule\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\caption{Properties}\n");
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatUnorderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{itemize}\n");

        for (var item : items) {
            String escaped = item != null ? escapeLatex(item) : "";
            sb.append("\\item ").append(escaped).append("\n");
        }

        sb.append("\\end{itemize}\n");

        return sb.toString();
    }

    @Override
    public String formatOrderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{enumerate}\n");

        for (var item : items) {
            String escaped = item != null ? escapeLatex(item) : "";
            sb.append("\\item ").append(escaped).append("\n");
        }

        sb.append("\\end{enumerate}\n");

        return sb.toString();
    }

    @Override
    public String formatTable(String[][] data) {
        if (data == null || data.length == 0) {
            return "";
        }

        int cols = data[0].length;
        var sb = new StringBuilder();

        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");

        // ACM style: use booktabs without vertical rules
        var colAlign = new StringBuilder();
        for (int i = 0; i < cols; i++) {
            colAlign.append("l");
        }

        sb.append("\\begin{tabular}{").append(colAlign).append("}\n");
        sb.append("\\toprule\n");

        // Header row
        String[] header = data[0];
        var headerJoiner = new StringJoiner(" & ");
        for (String cell : header) {
            String escaped = cell != null ? escapeLatex(cell) : "";
            headerJoiner.add("\\textbf{" + escaped + "}");
        }
        sb.append(headerJoiner).append(" \\\\\n");
        sb.append("\\midrule\n");

        // Data rows
        for (int i = 1; i < data.length; i++) {
            String[] row = data[i];
            var rowJoiner = new StringJoiner(" & ");
            for (String cell : row) {
                String escaped = cell != null ? escapeLatex(cell) : "";
                rowJoiner.add(escaped);
            }
            sb.append(rowJoiner).append(" \\\\\n");
        }

        sb.append("\\bottomrule\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\caption{Table Data}\n");
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatWarning(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return "\\noindent\\textit{Warning: " + escapeLatex(message) + "}\n";
    }

    @Override
    public String formatNote(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return "\\noindent\\textit{Note: " + escapeLatex(message) + "}\n";
    }

    @Override
    public String formatAssertions(Map<String, String> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");
        sb.append("\\begin{tabular}{ll}\n");
        sb.append("\\toprule\n");
        sb.append("\\textbf{Check} & \\textbf{Result} \\\\\n");
        sb.append("\\midrule\n");

        for (var entry : assertions.entrySet()) {
            String check = entry.getKey() != null ? escapeLatex(entry.getKey()) : "";
            String result = entry.getValue() != null ? escapeLatex(entry.getValue()) : "";
            sb.append(check).append(" & \\texttt{").append(result).append("} \\\\\n");
        }

        sb.append("\\bottomrule\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\caption{Validation Results}\n");
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{lstlisting}[language=json]\n");
        sb.append(jsonString).append("\n");
        sb.append("\\end{lstlisting}\n");

        return sb.toString();
    }

    /**
     * Map common keywords to ACM CCS taxonomy IDs.
     * Pre-populated for stack domains (databases, languages, testing, etc.).
     */
    public static String mapKeywordToCCS(String keyword) {
        return switch (keyword != null ? keyword.trim().toLowerCase() : "") {
            case "sparql", "rdf", "semantic web" -> "10002951.10003227.10010402";
            case "sql", "databases", "query" -> "10002951.10003214";
            case "java", "programming language", "python" -> "10001418.10003227";
            case "sealed types", "pattern matching" -> "10001418.10003240.10003241";
            case "testing", "unit test", "doctest" -> "10011039.10011046";
            case "documentation", "literate programming" -> "10011039.10003248";
            case "compiler", "language design" -> "10001418.10003217";
            case "virtualization", "virtual machine" -> "10010147.10010166";
            case "concurrency", "thread", "async" -> "10010147.10010178";
            case "machine learning", "ai" -> "10010147.10010257";
            case "graph", "tree", "algorithm" -> "10002950.10003082";
            case "security", "cryptography" -> "10010405.10010444";
            case "type system", "static analysis" -> "10001418.10003241";
            case "performance", "optimization" -> "10011039.10011070";
            case "data structure" -> "10002950.10002951";
            case "functional programming" -> "10001418.10003226";
            case "object-oriented", "oop" -> "10001418.10003241";
            default -> "10001418";
        };
    }

    /**
     * Generate CCS Concept XML from a list of keywords.
     */
    public static String generateCCSConcepts(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{CCSXML}\n");
        sb.append("<ccs2012>\n");

        for (var keyword : keywords) {
            String ccsId = mapKeywordToCCS(keyword);
            sb.append("  <concept>\n");
            sb.append("    <concept_id>").append(ccsId).append("</concept_id>\n");
            sb.append("    <concept_desc>").append(escapeXml(keyword)).append("</concept_desc>\n");
            sb.append("    <concept_significance>300</concept_significance>\n");
            sb.append("  </concept>\n");
        }

        sb.append("</ccs2012>\n");
        sb.append("\\end{CCSXML}\n");

        return sb.toString();
    }

    /**
     * Escape XML special characters for CCS concept XML.
     */
    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }

    @Override
    public String formatFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return "\\footnote{" + escapeLatex(text) + "}";
    }
}
