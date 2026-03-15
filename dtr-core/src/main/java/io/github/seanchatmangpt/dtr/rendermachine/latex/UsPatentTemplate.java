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
 * USPTO-compatible LaTeX template for patent exhibits and technical briefs.
 *
 * Uses report class with formal numbering, large margins, explicit section
 * numbering, and claim-style formatting suitable for patent offices and
 * enterprise technical documentation.
 *
 * Formatted for single-column layout with emphasis on clear, evidence-based
 * documentation of technical implementations.
 */
public record UsPatentTemplate() implements LatexTemplate {

    @Override
    public String getDocumentClass() {
        return "report";
    }

    @Override
    public String getPreamble() {
        return """
            \\documentclass[12pt]{report}
            \\usepackage[utf-8]{inputenc}
            \\usepackage[T1]{fontenc}
            \\usepackage{times}
            \\usepackage{amsmath}
            \\usepackage{amssymb}
            \\usepackage{listings}
            \\usepackage{xcolor}
            \\usepackage{tcolorbox}
            \\usepackage{booktabs}
            \\usepackage{hyperref}
            \\usepackage{graphicx}
            \\usepackage[margin=1.5in]{geometry}
            \\usepackage{fancyhdr}

            % Page numbering
            \\pagestyle{fancy}
            \\fancyhf{}
            \\cfoot{\\thepage}

            % Listings configuration for code/technical content
            \\lstset{
                basicstyle=\\ttfamily\\normalsize,
                breaklines=true,
                breakatwhitespace=true,
                commentstyle=\\color{gray},
                keywordstyle=\\color{blue},
                stringstyle=\\color{red},
                showstringspaces=false,
                frame=lines,
                rulecolor=\\color{black},
                backgroundcolor=\\color{white}
            }

            % Color definitions for alerts
            \\definecolor{warnbg}{RGB}{255, 230, 230}
            \\definecolor{notebg}{RGB}{230, 240, 255}

            """;
    }

    @Override
    public String getBeginDocument() {
        return "\\chapter*{Technical Documentation}\\n";
    }

    @Override
    public String getEndDocument() {
        return "\\appendix\\n\\chapter*{Execution Receipts}\\n";
    }

    @Override
    public String formatSection(String title) {
        return "\\section{" + escapeLatex(title) + "}\\n";
    }

    @Override
    public String formatSubsection(String title) {
        return "\\subsection{" + escapeLatex(title) + "}\\n";
    }

    @Override
    public String escapeLatex(String text) {
        return StringEscapeUtils.escapeLaTeX(text);
    }

    @Override
    public String formatCodeBlock(String code, String language) {
        if (code == null || code.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        var sb = new StringBuilder();
        String lang = language != null && !language.isEmpty() ? language : "text";

        sb.append("\\begin{lstlisting}[language=").append(lang).append(",numbers=left]\n");
        sb.append(code).append("\n");
        sb.append("\\end{lstlisting}\n");

        return sb.toString();
    }

    @Override
    public String formatKeyValue(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        var sb = new StringBuilder();
        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");
        sb.append("\\begin{tabular}{|p{2in}|p{4in}|}\n");
        sb.append("\\hline\n");

        for (var entry : pairs.entrySet()) {
            String key = entry.getKey() != null ? escapeLatex(entry.getKey()) : "";
            String value = entry.getValue() != null ? escapeLatex(entry.getValue()) : "";
            sb.append("\\textbf{").append(key).append("} & \\texttt{").append(value).append("} \\\\\n");
        }

        sb.append("\\hline\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatUnorderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
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
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
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
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        var sb = new StringBuilder();
        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");

        // Determine column count and alignment
        int cols = data[0].length;
        var colAlign = new StringBuilder();
        for (int i = 0; i < cols; i++) {
            colAlign.append("|c");
        }
        colAlign.append("|");

        sb.append("\\begin{tabular}{").append(colAlign).append("}\n");
        sb.append("\\hline\n");

        // Header row
        String[] header = data[0];
        var headerJoiner = new StringJoiner(" & ");
        for (String cell : header) {
            String escaped = cell != null ? escapeLatex(cell) : "";
            headerJoiner.add("\\textbf{" + escaped + "}");
        }
        sb.append(headerJoiner).append(" \\\\\n");
        sb.append("\\hline\n");

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

        sb.append("\\hline\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatWarning(String message) {
        if (message == null || message.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        return "\\begin{tcolorbox}[colback=warnbg,colframe=red!70!black,title={\\textbf{WARNING}}]\n"
            + escapeLatex(message) + "\n"
            + "\\end{tcolorbox}\n";
    }

    @Override
    public String formatNote(String message) {
        if (message == null || message.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        return "\\begin{tcolorbox}[colback=notebg,colframe=blue!70!black,title={\\textbf{TECHNICAL NOTE}}]\n"
            + escapeLatex(message) + "\n"
            + "\\end{tcolorbox}\n";
    }

    @Override
    public String formatAssertions(Map<String, String> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        var sb = new StringBuilder();
        sb.append("\\begin{table}[h]\n");
        sb.append("\\centering\n");
        sb.append("\\begin{tabular}{|p{3in}|p{2in}|}\n");
        sb.append("\\hline\n");
        sb.append("\\textbf{Assertion} & \\textbf{Result} \\\\\n");
        sb.append("\\hline\n");

        for (var entry : assertions.entrySet()) {
            String check = entry.getKey() != null ? escapeLatex(entry.getKey()) : "";
            String result = entry.getValue() != null ? escapeLatex(entry.getValue()) : "";
            sb.append(check).append(" & ").append(result).append(" \\\\\n");
        }

        sb.append("\\hline\n");
        sb.append("\\end{tabular}\n");
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        var sb = new StringBuilder();
        sb.append("\\begin{lstlisting}[language=json,numbers=left]\n");
        sb.append(jsonString).append("\n");
        sb.append("\\end{lstlisting}\n");

        return sb.toString();
    }

    @Override
    public String formatFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return "";  // hguard-ok: null/empty guard — implemented method returns empty for absent input
        }

        return "\\footnote{" + escapeLatex(text) + "}";
    }
}
