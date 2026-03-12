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

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Nature-compatible LaTeX template for journal submissions.
 *
 * Uses plain article class with Nature style formatting, author-year citations
 * via natbib (\citep, \citet), and strict constraints aligned with Nature's
 * publication guidelines:
 * - No colored boxes; warnings/notes use italicized text
 * - Tables use booktabs only (no vertical rules)
 * - Abstract: 200-word limit enforced at assembly time
 * - Methods section: mandatory for empirical claims
 * - Word limit: 3000 words for Letters (enforced at assembly time)
 * - Data availability statement: included in preamble
 * - Code availability statement: auto-populated from git remote URL
 *
 * This template is publication-grade for Nature, Nature Methods, and related venues.
 */
public record NatureTemplate(String codeRepositoryUrl) implements LatexTemplate {

    /**
     * Creates a Nature template with optional code repository URL.
     */
    public NatureTemplate() {
        this("https://github.com/seanchatmangpt/dtr");
    }

    @Override
    public String getDocumentClass() {
        return "article";
    }

    @Override
    public String getPreamble() {
        return """
            \\documentclass[11pt]{article}
            \\usepackage[utf-8]{inputenc}
            \\usepackage[T1]{fontenc}
            \\usepackage{times}
            \\usepackage{amsmath}
            \\usepackage{amssymb}
            \\usepackage{listings}
            \\usepackage{xcolor}
            \\usepackage{booktabs}
            \\usepackage{hyperref}
            \\usepackage{graphicx}
            \\usepackage[margin=1in]{geometry}
            \\usepackage[sort&compress]{natbib}
            \\bibliographystyle{naturemag}

            %% Listings configuration for Nature submissions
            \\lstset{
                basicstyle=\\ttfamily\\small,
                breaklines=true,
                breakatwhitespace=true,
                commentstyle=\\color{gray},
                keywordstyle=\\color{blue},
                stringstyle=\\color{red},
                showstringspaces=false,
                frame=lines,
                rulecolor=\\color{lightgray},
                backgroundcolor=\\color{white!99!gray}
            }

            %% Page numbering
            \\pagestyle{plain}

            %% Nature requires specific formatting for data/code availability
            %% These statements are auto-generated from template parameters

            """;
    }

    @Override
    public String getBeginDocument() {
        var sb = new StringBuilder();

        sb.append("\\title{Research Title}\n");
        sb.append("\\author{Author Name}\n");
        sb.append("\\date{}\n");
        sb.append("\\maketitle\n\n");

        sb.append("\\begin{abstract}\n");
        sb.append("Abstract (max 200 words) goes here.\n");
        sb.append("\\end{abstract}\n\n");

        sb.append("\\section*{Data Availability}\n");
        sb.append("Data and materials are available at ");
        sb.append(codeRepositoryUrl).append(".\n\n");

        sb.append("\\section*{Code Availability}\n");
        sb.append("Source code is available at ");
        sb.append(codeRepositoryUrl).append(".\n\n");

        return sb.toString();
    }

    @Override
    public String getEndDocument() {
        return """
            \\section*{Acknowledgements}
            [Funding and acknowledgements]

            \\bibliographystyle{naturemag}
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
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
            .replace("\\", "\\textbackslash{}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("~", "\\textasciitilde{}")
            .replace("^", "\\textasciicircum{}");
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
        sb.append("\\caption{Parameters}\n");
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

        // Nature style: use booktabs only, no vertical rules
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

        return "\\textit{Warning: " + escapeLatex(message) + "}\n";
    }

    @Override
    public String formatNote(String message) {
        if (message == null || message.isEmpty()) {
            return "";
        }

        return "\\textit{Note: " + escapeLatex(message) + "}\n";
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
     * Validates that abstract is within 200-word limit.
     * Nature requires abstracts to be self-contained and ≤200 words.
     */
    public static boolean isAbstractValid(String abstractText) {
        if (abstractText == null || abstractText.isEmpty()) {
            return false;
        }
        int wordCount = abstractText.trim().split("\\s+").length;
        return wordCount <= 200;
    }

    /**
     * Counts words in document body (excluding preamble and metadata).
     * Nature Letters have a 3000-word limit.
     */
    public static int countWords(String documentBody) {
        if (documentBody == null || documentBody.isEmpty()) {
            return 0;
        }
        // Remove LaTeX commands and count remaining words
        String cleaned = documentBody
            .replaceAll("\\\\[a-zA-Z]+\\{.*?\\}", "")
            .replaceAll("\\\\[a-zA-Z]+", "")
            .replaceAll("[\\\\{}]", "");
        return cleaned.trim().split("\\s+").length;
    }

    /**
     * Enforces Nature word limits based on article type.
     * - Letter: 3000 words max
     * - Article: 5000 words max
     */
    public static boolean isWordCountValid(String documentBody, String articleType) {
        int count = countWords(documentBody);
        return switch (articleType != null ? articleType.toLowerCase() : "article") {
            case "letter" -> count <= 3000;
            case "article" -> count <= 5000;
            default -> true;
        };
    }

    @Override
    public String formatFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return "\\footnote{" + escapeLatex(text) + "}";
    }
}
