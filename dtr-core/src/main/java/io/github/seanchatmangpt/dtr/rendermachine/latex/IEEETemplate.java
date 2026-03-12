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
 * IEEE-compatible LaTeX template for journal and conference papers.
 *
 * Uses IEEEtran document class configured for journal submissions (configurable
 * to conference mode). Provides IEEE-style formatting with numeric citations [1],
 * abstract and keywords sections, and table/code formatting adhering to IEEE
 * guidelines. Tables use caption ABOVE, wide tables employ table*.
 */
public record IEEETemplate(String mode) implements LatexTemplate {

    /**
     * Creates an IEEE template with default journal mode.
     */
    public IEEETemplate() {
        this("journal");
    }

    @Override
    public String getDocumentClass() {
        return "IEEEtran";
    }

    @Override
    public String getPreamble() {
        return """
            \\documentclass[%s]{IEEEtran}
            \\usepackage[utf-8]{inputenc}
            \\usepackage[T1]{fontenc}
            \\usepackage{amsmath}
            \\usepackage{amssymb}
            \\usepackage{listings}
            \\usepackage{xcolor}
            \\usepackage{booktabs}
            \\usepackage{hyperref}
            \\usepackage{graphicx}
            \\usepackage{natbib}

            %% Code listing configuration with 8pt font as per IEEE guidelines
            \\lstset{
                basicstyle=\\footnotesize\\ttfamily,
                breaklines=true,
                breakatwhitespace=true,
                commentstyle=\\color{gray},
                keywordstyle=\\color{blue},
                stringstyle=\\color{red},
                showstringspaces=false,
                frame=single,
                rulecolor=\\color{lightgray},
                backgroundcolor=\\color{white!95!gray},
                numbers=left,
                numberstyle=\\tiny,
                stepnumber=1
            }

            %% Define color scheme (IEEE prefers minimal colors)
            \\definecolor{notebg}{RGB}{240, 245, 255}

            """.formatted(mode);
    }

    @Override
    public String getBeginDocument() {
        return """
            %% Title block (must be customized by document author)
            \\title{Research Title}
            \\author{Author Name}
            \\maketitle

            """;
    }

    @Override
    public String getEndDocument() {
        return """
            \\begin{thebibliography}{1}
            \\bibitem{key} Reference information
            \\end{thebibliography}

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

        sb.append("\\begin{lstlisting}[language=").append(lang).append(",basicstyle=\\footnotesize\\ttfamily]\n");
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
        sb.append("\\caption{Parameters}\n");
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
        boolean wideTable = cols > 5;

        var sb = new StringBuilder();

        if (wideTable) {
            sb.append("\\begin{table*}[t]\n");
        } else {
            sb.append("\\begin{table}[h]\n");
        }

        sb.append("\\caption{Table Data}\n");
        sb.append("\\centering\n");

        // IEEE style: use booktabs without vertical rules
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

        if (wideTable) {
            sb.append("\\end{table*}\n");
        } else {
            sb.append("\\end{table}\n");
        }

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
        sb.append("\\caption{Validation Results}\n");
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
        sb.append("\\end{table}\n");

        return sb.toString();
    }

    @Override
    public String formatJson(String jsonString) {
        if (jsonString == null || jsonString.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        sb.append("\\begin{lstlisting}[language=json,basicstyle=\\footnotesize\\ttfamily]\n");
        sb.append(jsonString).append("\n");
        sb.append("\\end{lstlisting}\n");

        return sb.toString();
    }

    @Override
    public String formatFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return "\\footnote{" + escapeLatex(text) + "}";
    }
}
