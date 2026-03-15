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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seanchatmangpt.dtr.bibliography.BibliographyManager;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.evolution.GitHistoryReader;
import io.github.seanchatmangpt.dtr.javadoc.JavadocEntry;
import io.github.seanchatmangpt.dtr.javadoc.JavadocIndex;
import io.github.seanchatmangpt.dtr.metadata.DocMetadata;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LaTeX-based render machine generating publication-ready PDF documentation.
 *
 * <p>Maps all RenderMachine {@code say*} methods to LaTeX commands for document generation
 * via pdflatex, latexmk, xelatex, or other LaTeX compilers. Produces high-quality PDF suitable
 * for academic papers, technical reports, patents, and professional documentation.</p>
 *
 * <p><strong>Template System:</strong></p>
 * <p>Supports multiple academic and professional templates via sealed {@link LatexTemplate}:
 * <ul>
 *   <li>{@link ArXivTemplate} - arXiv preprint submissions (default)</li>
 *   <li>{@link UsPatentTemplate} - USPTO patent exhibit format</li>
 *   <li>{@link IEEETemplate} - IEEE journal article format</li>
 *   <li>{@link ACMTemplate} - ACM conference proceedings format</li>
 *   <li>{@link NatureTemplate} - Nature scientific publication format</li>
 * </ul>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>LaTeX escaping of special characters in text, code, and tables</li>
 *   <li>Automatic bibliography integration via BibTeX</li>
 *   <li>Cross-references between DocTests with LaTeX \ref{} commands</li>
 *   <li>Metadata support: title, author, date, keywords for PDF properties</li>
 *   <li>Table formatting with column specifications</li>
 *   <li>JSON payload rendering with inline code formatting</li>
 * </ul>
 *
 * <p><strong>Output:</strong></p>
 * <p>Generates a .tex source file written to {@code docs/test/latex/&lt;FileName&gt;.tex}
 * which can be compiled independently or included in a master document.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * DocMetadata metadata = new DocMetadata(
 *     "My API Specification",
 *     "2.0.0",
 *     "Alice Smith",
 *     "alice@example.com");
 *
 * RenderMachine latex = new RenderMachineLatex(
 *     new ArXivTemplate(),
 *     metadata);
 *
 * latex.setFileName("ApiDocTest");
 * latex.sayNextSection("REST API Specification");
 * latex.say("Documents the complete REST API...");
 * latex.finishAndWriteOut();
 * }</pre>
 *
 * <p><strong>Design Note:</strong></p>
 * <p>This is a {@code final} class to enable JIT devirtualization. Works seamlessly
 * with {@link MultiRenderMachine} for simultaneous Markdown + LaTeX generation.</p>
 *
 * @since 1.0.0
 * @see LatexTemplate for template implementations
 * @see DocMetadata for document metadata
 * @see RenderMachine for interface contract
 */
public final class RenderMachineLatex extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineLatex.class);

    private static final String BASE_DIR = "docs/test/latex";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** LaTeX document template (e.g., ArXiv, IEEE, ACM, Nature). */
    private final LatexTemplate template;

    /** Document metadata (title, author, date, keywords). */
    private final DocMetadata metadata;

    /** Accumulated LaTeX document lines. */
    private final List<String> texDocument;

    /** Output filename (typically the test class name). */
    private String fileName;

    /**
     * Creates a new LaTeX render machine with the specified template and metadata.
     *
     * @param template the LaTeX template to use for formatting (e.g., ArXivTemplate)
     * @param metadata the document metadata (title, author, date, keywords)
     */
    public RenderMachineLatex(LatexTemplate template, DocMetadata metadata) {
        this.template = template;
        this.metadata = metadata;
        this.texDocument = new ArrayList<>();
    }

    @Override
    public void say(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.escapeLatex(text));
    }

    @Override
    public void sayNextSection(String heading) {
        if (heading == null || heading.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatSection(heading));
    }

    @Override
    public void sayTable(String[][] data) {
        if (data == null || data.length == 0) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatTable(data));
    }

    @Override
    public void sayCode(String code, String language) {
        if (code == null || code.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatCodeBlock(code, language));
    }

    @Override
    public void sayWarning(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatWarning(message));
    }

    @Override
    public void sayNote(String message) {
        if (message == null || message.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatNote(message));
    }

    @Override
    public void sayKeyValue(Map<String, String> pairs) {
        if (pairs == null || pairs.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatKeyValue(pairs));
    }

    @Override
    public void sayUnorderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatUnorderedList(items));
    }

    @Override
    public void sayOrderedList(List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatOrderedList(items));
    }

    @Override
    public void sayJson(Object object) {
        if (object == null) {
            return;
        }
        try {
            String jsonString = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
            texDocument.add("");
            texDocument.add(template.formatJson(jsonString));
        } catch (Exception e) {
            logger.warn("Failed to serialize object to JSON", e);
            texDocument.add("");
            texDocument.add(template.escapeLatex(object.toString()));
        }
    }

    @Override
    public void sayAssertions(Map<String, String> assertions) {
        if (assertions == null || assertions.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(template.formatAssertions(assertions));
    }

    @Override
    public void sayCite(String citationKey) {
        if (citationKey == null || citationKey.isEmpty()) {
            return;
        }
        texDocument.add("\\cite{" + citationKey + "}");
    }

    @Override
    public void sayCite(String citationKey, String pageRef) {
        if (citationKey == null || citationKey.isEmpty()) {
            return;
        }
        texDocument.add("\\cite[p. " + pageRef + "]{" + citationKey + "}");
    }

    @Override
    public void sayFootnote(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        texDocument.add("\\footnote{" + template.escapeLatex(text) + "}");
    }

    @Override
    public void sayRef(DocTestRef ref) {
        if (ref == null) {
            return;
        }
        texDocument.add("");
        // Generate LaTeX \ref{} command for two-pass compilation
        // The reference resolver will have mapped the anchor to a LaTeX label
        String anchor = ref.anchor();
        String label = "sec:" + convertTextToLatexLabel(anchor);
        texDocument.add("See Section \\ref{" + label + "}");
    }

    @Override
    public void sayRaw(String latex) {
        if (latex == null || latex.isEmpty()) {
            return;
        }
        texDocument.add("");
        texDocument.add(latex);
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public void finishAndWriteOut() {
        createLaTexFile();
    }

    private void createLaTexFile() {
        List<String> doc = new ArrayList<>();

        // Preamble with document class and packages
        doc.add(template.getPreamble());

        // Begin document
        doc.add("\\begin{document}");
        doc.add("");

        // Title page
        doc.add("\\title{" + template.escapeLatex(fileName) + "}");
        doc.add("\\author{DTR}");
        doc.add("\\date{\\today}");
        doc.add("\\maketitle");
        doc.add("");

        // Template-specific begin content
        String beginDoc = template.getBeginDocument();
        if (beginDoc != null && !beginDoc.isEmpty()) {
            doc.add(beginDoc);
            doc.add("");
        }

        // Main document content
        doc.addAll(texDocument);

        doc.add("");

        // Template-specific end content (for receipts, appendix, etc.)
        String endDoc = template.getEndDocument();
        if (endDoc != null && !endDoc.isEmpty()) {
            doc.add(endDoc);
            doc.add("");
        }

        // End document
        doc.add("\\end{document}");

        writeLatexFile(doc, fileName);
    }

    private void writeLatexFile(List<String> lines, String fileNameWithoutExtension) {
        File outputDir = new File(BASE_DIR);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        File outputFile = new File(BASE_DIR + File.separator + fileNameWithoutExtension + ".tex");

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
            for (String line : lines) {
                writer.write(line);
                writer.write('\n');
            }
            logger.info("LaTeX file written: {}", outputFile.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error writing LaTeX file: {}", outputFile, e);
        }
    }

    private String convertStackTraceToString(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.getClass().getName());
        if (throwable.getMessage() != null) {
            sb.append(": ").append(throwable.getMessage());
        }
        sb.append('\n');

        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("  at ").append(element).append('\n');
        }

        return sb.toString();
    }

    /**
     * Convert an anchor text to a valid LaTeX label format.
     * Converts spaces to hyphens and removes invalid characters.
     *
     * @param text the anchor text
     * @return LaTeX label format
     */
    private String convertTextToLatexLabel(String text) {
        return text.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    // ========================================================================
    // Code Model Methods
    // ========================================================================

    @Override
    public void sayCodeModel(Class<?> clazz) {
        if (clazz == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Class: \\texttt{" + template.escapeLatex(clazz.getSimpleName()) + "}}");
        texDocument.add("");
        texDocument.add("\\textbf{Package:} \\texttt{" + template.escapeLatex(clazz.getPackageName()) + "}");
        texDocument.add("");
    }

    @Override
    public void sayCodeModel(java.lang.reflect.Method method) {
        if (method == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Method: \\texttt{" + template.escapeLatex(method.getName()) + "}}");
        texDocument.add("");
        texDocument.add("\\textbf{Return Type:} \\texttt{" + template.escapeLatex(method.getReturnType().getSimpleName()) + "}");
        texDocument.add("");
    }

    @Override
    public void sayCallSite() {
        var walker = java.lang.StackWalker.getInstance(java.lang.StackWalker.Option.RETAIN_CLASS_REFERENCE);
        walker.walk(frames -> {
            frames.skip(2).findFirst().ifPresent(frame -> {
                texDocument.add("");
                texDocument.add("\\textbf{Call Site:} \\texttt{" +
                    template.escapeLatex(frame.getClassName()) + "\\#" +
                    template.escapeLatex(frame.getMethodName()) + ":" +
                    frame.getLineNumber() + "}");
            });
            return null;
        });
    }

    @Override
    public void sayAnnotationProfile(Class<?> clazz) {
        if (clazz == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Annotation Profile}");
        texDocument.add("");

        var annotations = clazz.getAnnotations();
        if (annotations.length > 0) {
            texDocument.add("\\begin{itemize}");
            for (var a : annotations) {
                texDocument.add("  \\item \\texttt{@" + a.annotationType().getSimpleName() + "}");
            }
            texDocument.add("\\end{itemize}");
        }
    }

    @Override
    public void sayClassHierarchy(Class<?> clazz) {
        if (clazz == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Class Hierarchy}");
        texDocument.add("");

        List<String> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            hierarchy.add("\\texttt{" + current.getSimpleName() + "}");
            current = current.getSuperclass();
        }
        texDocument.add(String.join(" $\\rightarrow$ ", hierarchy));
        texDocument.add("");
    }

    @Override
    public void sayStringProfile(String text) {
        if (text == null || text.isEmpty()) return;
        texDocument.add("");
        texDocument.add("\\subsection{String Profile}");
        texDocument.add("");
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Length & " + text.length() + " \\\\");
        texDocument.add("Words & " + text.split("\\s+").length + " \\\\");
        texDocument.add("Lines & " + text.lines().count() + " \\\\");
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void sayReflectiveDiff(Object before, Object after) {
        if (before == null || after == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Reflective Diff}");
        texDocument.add("");

        try {
            texDocument.add("\\begin{tabular}{lll}");
            texDocument.add("Field & Before & After \\\\");
            texDocument.add("\\hline");
            for (var field : before.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                Object beforeVal = field.get(before);
                Object afterVal = field.get(after);
                if (!java.util.Objects.equals(beforeVal, afterVal)) {
                    texDocument.add("\\texttt{" + field.getName() + "} & " +
                        (beforeVal != null ? beforeVal.toString() : "null") + " & " +
                        (afterVal != null ? afterVal.toString() : "null") + " \\\\");
                }
            }
            texDocument.add("\\end{tabular}");
        } catch (Exception e) {
            texDocument.add("Error: " + template.escapeLatex(e.getMessage()));
        }
        texDocument.add("");
    }

    @Override
    public void sayControlFlowGraph(java.lang.reflect.Method method) {
        if (method == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Control Flow Graph: \\texttt{" + method.getName() + "}}");
        texDocument.add("");
        texDocument.add("\\textit{CFG visualization requires Java 26+ with @CodeReflection annotation.}");
        texDocument.add("");
    }

    @Override
    public void sayCallGraph(Class<?> clazz) {
        if (clazz == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Call Graph: \\texttt{" + clazz.getSimpleName() + "}}");
        texDocument.add("");
        texDocument.add("\\textit{Call graph visualization requires Java 26+ with @CodeReflection annotation.}");
        texDocument.add("");
    }

    @Override
    public void sayOpProfile(java.lang.reflect.Method method) {
        if (method == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Op Profile: \\texttt{" + method.getName() + "}}");
        texDocument.add("");
        texDocument.add("\\textit{Op profile requires Java 26+ with @CodeReflection annotation.}");
        texDocument.add("");
    }

    @Override
    public void sayBenchmark(String label, Runnable task) {
        sayBenchmark(label, task, 50, 500);
    }

    @Override
    public void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds) {
        if (label == null || task == null) return;

        // Warmup
        for (int i = 0; i < warmupRounds; i++) {
            task.run();
        }

        // Measure
        long[] times = new long[measureRounds];
        for (int i = 0; i < measureRounds; i++) {
            long start = System.nanoTime();
            task.run();
            times[i] = System.nanoTime() - start;
        }

        Arrays.sort(times);
        double avg = Arrays.stream(times).average().orElse(0);
        long min = times[0];
        long max = times[measureRounds - 1];
        long p99 = times[(int)(measureRounds * 0.99)];

        texDocument.add("");
        texDocument.add("\\subsection{Benchmark: " + template.escapeLatex(label) + "}");
        texDocument.add("");
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Average & " + String.format("%.0f ns", avg) + " \\\\");
        texDocument.add("Min & " + min + " ns \\\\");
        texDocument.add("Max & " + max + " ns \\\\");
        texDocument.add("P99 & " + p99 + " ns \\\\");
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void sayMermaid(String diagramDsl) {
        if (diagramDsl == null || diagramDsl.isEmpty()) return;
        texDocument.add("");
        texDocument.add("% Mermaid diagram (not supported in LaTeX)");
        texDocument.add("\\begin{verbatim}");
        texDocument.add(diagramDsl);
        texDocument.add("\\end{verbatim}");
        texDocument.add("");
    }

    @Override
    public void sayClassDiagram(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        texDocument.add("");
        texDocument.add("\\subsection{Class Diagram}");
        texDocument.add("");
        texDocument.add("\\begin{itemize}");
        for (Class<?> c : classes) {
            if (c != null) {
                texDocument.add("  \\item \\texttt{" + c.getSimpleName() + "}");
            }
        }
        texDocument.add("\\end{itemize}");
        texDocument.add("");
    }

    @Override
    public void sayDocCoverage(Class<?>... classes) {
        if (classes == null || classes.length == 0) return;
        texDocument.add("");
        texDocument.add("\\subsection{Documentation Coverage}");
        texDocument.add("");

        // Doc coverage requires coverage tracking which is only available in DtrContext
        // List the classes that would be analyzed
        texDocument.add("\\textbf{Classes analyzed:}");
        texDocument.add("\\begin{itemize}");
        for (Class<?> c : classes) {
            if (c != null) {
                texDocument.add("  \\item \\texttt{" + c.getSimpleName() + "}");
            }
        }
        texDocument.add("\\end{itemize}");
        texDocument.add("");
        texDocument.add("\\textit{Full coverage analysis requires DtrContext test execution.}");
        texDocument.add("");
    }

    @Override
    public void sayEnvProfile() {
        texDocument.add("");
        texDocument.add("\\subsection{Environment Profile}");
        texDocument.add("");
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Java Version & \\texttt{" + System.getProperty("java.version") + "} \\\\");
        texDocument.add("OS & \\texttt{" + System.getProperty("os.name") + " " + System.getProperty("os.version") + "} \\\\");
        texDocument.add("Processors & " + Runtime.getRuntime().availableProcessors() + " \\\\");
        texDocument.add("Max Memory & " + (Runtime.getRuntime().maxMemory() / (1024 * 1024)) + " MB \\\\");
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void sayRecordComponents(Class<? extends Record> recordClass) {
        if (recordClass == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Record Schema: \\texttt{" + recordClass.getSimpleName() + "}}");
        texDocument.add("");

        var components = recordClass.getRecordComponents();
        if (components == null || components.length == 0) {
            texDocument.add("\\textit{No record components.}");
            return;
        }

        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Component & Type \\\\");
        texDocument.add("\\hline");
        for (var comp : components) {
            texDocument.add("\\texttt{" + comp.getName() + "} & \\texttt{" + comp.getType().getSimpleName() + "} \\\\");
        }
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void sayException(Throwable t) {
        if (t == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Exception: \\texttt{" + t.getClass().getSimpleName() + "}}");
        texDocument.add("");
        texDocument.add("\\textbf{Message:} " + template.escapeLatex(t.getMessage() != null ? t.getMessage() : "no message"));
        texDocument.add("");

        // Cause chain
        List<String> causeChain = new ArrayList<>();
        Throwable cause = t.getCause();
        while (cause != null) {
            causeChain.add(cause.getClass().getSimpleName() +
                (cause.getMessage() != null ? ": " + cause.getMessage() : ""));
            cause = cause.getCause();
        }
        if (!causeChain.isEmpty()) {
            texDocument.add("\\textbf{Cause chain:}");
            texDocument.add("\\begin{itemize}");
            for (String c : causeChain) {
                texDocument.add("  \\item " + template.escapeLatex(c));
            }
            texDocument.add("\\end{itemize}");
        }
        texDocument.add("");
    }

    @Override
    public void sayAsciiChart(String label, double[] values, String[] xLabels) {
        if (label == null || values == null || values.length == 0) return;
        texDocument.add("");
        texDocument.add("\\subsection{" + template.escapeLatex(label) + "}");
        texDocument.add("");

        double max = Arrays.stream(values).max().orElse(1.0);
        if (max == 0) max = 1.0;
        int barWidth = 15;

        texDocument.add("\\begin{verbatim}");
        for (int i = 0; i < values.length; i++) {
            String rowLabel = (xLabels != null && i < xLabels.length) ? xLabels[i] : ("" + i);
            int filled = (int) Math.round((values[i] / max) * barWidth);
            String bar = "X".repeat(filled) + ".".repeat(barWidth - filled);
            texDocument.add(String.format("%-6s %s  %.0f", rowLabel, bar, values[i]));
        }
        texDocument.add("\\end{verbatim}");
        texDocument.add("");
    }

    @Override
    public void sayContractVerification(Class<?> contract, Class<?>... implementations) {
        if (contract == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Contract: \\texttt{" + contract.getSimpleName() + "}}");
        texDocument.add("");
        if (implementations != null && implementations.length > 0) {
            texDocument.add("\\textbf{Implementations:}");
            texDocument.add("\\begin{itemize}");
            for (Class<?> impl : implementations) {
                if (impl != null) {
                    texDocument.add("  \\item \\texttt{" + impl.getSimpleName() + "}");
                }
            }
            texDocument.add("\\end{itemize}");
        }
        texDocument.add("");
    }

    @Override
    public void sayEvolutionTimeline(Class<?> clazz, int maxEntries) {
        if (clazz == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Evolution Timeline: \\texttt{" + clazz.getSimpleName() + "}}");
        texDocument.add("");

        int limit = maxEntries > 0 ? maxEntries : 10;
        var entries = GitHistoryReader.read(clazz, limit);

        if (entries.isEmpty()) {
            texDocument.add("\\textit{Git history unavailable (not in a git repository or file not tracked).}");
        } else {
            texDocument.add("\\begin{tabular}{llll}");
            texDocument.add("\\textbf{Commit} & \\textbf{Date} & \\textbf{Author} & \\textbf{Subject} \\\\");
            texDocument.add("\\hline");
            for (var entry : entries) {
                texDocument.add("\\texttt{" + entry.hash() + "} & " +
                    entry.date() + " & " +
                    template.escapeLatex(entry.author()) + " & " +
                    template.escapeLatex(entry.subject()) + " \\\\");
            }
            texDocument.add("\\end{tabular}");
            texDocument.add("");
            texDocument.add("\\textit{" + entries.size() + " commits shown.}");
        }
        texDocument.add("");
    }

    @Override
    public void sayJavadoc(java.lang.reflect.Method method) {
        if (method == null) return;
        texDocument.add("");
        texDocument.add("\\subsection{Javadoc: \\texttt{" + method.getName() + "}}");
        texDocument.add("");

        var entry = JavadocIndex.lookup(method);

        if (entry.isEmpty()) {
            texDocument.add("\\textit{Javadoc not available for this method.}");
        } else {
            JavadocEntry je = entry.get();

            // Description
            if (je.description() != null && !je.description().isEmpty()) {
                texDocument.add(template.escapeLatex(je.description()));
                texDocument.add("");
            }

            // Parameters table
            if (je.params() != null && !je.params().isEmpty()) {
                texDocument.add("\\textbf{Parameters:}");
                texDocument.add("\\begin{tabular}{ll}");
                texDocument.add("Parameter & Description \\\\");
                texDocument.add("\\hline");
                for (var param : je.params()) {
                    texDocument.add("\\texttt{" + param.name() + "} & " +
                            template.escapeLatex(param.description()) + " \\\\");
                }
                texDocument.add("\\end{tabular}");
                texDocument.add("");
            }

            // Returns
            if (je.returns() != null && !je.returns().isEmpty()) {
                texDocument.add("\\textbf{Returns:} " + template.escapeLatex(je.returns()));
                texDocument.add("");
            }

            // Since
            if (je.since() != null && !je.since().isEmpty()) {
                texDocument.add("\\textbf{Since:} \\texttt{" + je.since() + "}");
                texDocument.add("");
            }
        }
        texDocument.add("");
    }

    @Override
    public void saySystemProperties() {
        saySystemProperties(null);
    }

    @Override
    public void saySystemProperties(String regexFilter) {
        var props = System.getProperties();

        var entryStream = props.entrySet().stream();
        if (regexFilter != null && !regexFilter.isBlank()) {
            var pattern = java.util.regex.Pattern.compile(regexFilter);
            var predicate = pattern.asPredicate();
            entryStream = entryStream.filter(e -> predicate.test(e.getKey().toString()));
        }

        var sortedEntries = entryStream
                .sorted(Comparator.comparing(e -> e.getKey().toString()))
                .limit(50)
                .toList();

        texDocument.add("");
        texDocument.add("\\subsection{JVM System Properties}");
        if (regexFilter != null && !regexFilter.isBlank()) {
            texDocument.add("\\textit{Filter: \\texttt{" + template.escapeLatex(regexFilter) + "}}");
        }
        texDocument.add("");
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Property & Value \\\\");
        texDocument.add("\\hline");
        for (var entry : sortedEntries) {
            String key = entry.getKey().toString();
            String value = entry.getValue().toString();
            if (value.length() > 50) value = value.substring(0, 47) + "...";
            texDocument.add("\\texttt{" + template.escapeLatex(key) + "} & \\texttt{" + template.escapeLatex(value) + "} \\\\");
        }
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void saySecurityManager() {
        texDocument.add("");
        texDocument.add("\\subsection{Security Environment}");
        texDocument.add("");

        var sm = System.getSecurityManager();
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Security Manager & " + (sm != null ? "PRESENT" : "ABSENT") + " \\\\");
        texDocument.add("\\end{tabular}");
        texDocument.add("");

        // Providers
        texDocument.add("\\textbf{Security Providers:}");
        texDocument.add("\\begin{enumerate}");
        var providers = java.security.Security.getProviders();
        for (var provider : providers) {
            texDocument.add("  \\item \\texttt{" + provider.getName() + "} (v" + provider.getVersion() + ")");
        }
        texDocument.add("\\end{enumerate}");
        texDocument.add("");
    }

    @Override
    public void sayModuleDependencies(Class<?>... classes) {
        if (classes == null || classes.length == 0) {
            texDocument.add("");
            texDocument.add("\\textit{No classes provided for module dependency analysis.}");
            return;
        }

        texDocument.add("");
        texDocument.add("\\subsection{Module Dependencies}");
        texDocument.add("");

        Map<Module, List<Class<?>>> moduleMap = Arrays.stream(classes)
                .filter(clazz -> clazz != null)
                .collect(Collectors.groupingBy(Class::getModule, LinkedHashMap::new, Collectors.toList()));

        for (var entry : moduleMap.entrySet()) {
            Module module = entry.getKey();
            String moduleName = module.isNamed() ? module.getName() : "Unnamed Module";
            texDocument.add("\\textbf{" + moduleName + "}");
            texDocument.add("\\begin{itemize}");
            for (Class<?> c : entry.getValue()) {
                texDocument.add("  \\item \\texttt{" + c.getSimpleName() + "}");
            }
            texDocument.add("\\end{itemize}");
        }
        texDocument.add("");
    }

    @Override
    public void sayThreadDump() {
        var threadMXBean = ManagementFactory.getThreadMXBean();

        texDocument.add("");
        texDocument.add("\\subsection{Thread Summary}");
        texDocument.add("");
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("Thread Count & " + threadMXBean.getThreadCount() + " \\\\");
        texDocument.add("Daemon Threads & " + threadMXBean.getDaemonThreadCount() + " \\\\");
        texDocument.add("Peak Threads & " + threadMXBean.getPeakThreadCount() + " \\\\");
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void sayOperatingSystem() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        texDocument.add("");
        texDocument.add("\\subsection{Operating System Metrics}");
        texDocument.add("");
        texDocument.add("\\begin{tabular}{ll}");
        texDocument.add("OS Name & \\texttt{" + template.escapeLatex(osBean.getName()) + "} \\\\");
        texDocument.add("OS Version & \\texttt{" + template.escapeLatex(osBean.getVersion()) + "} \\\\");
        texDocument.add("Architecture & \\texttt{" + template.escapeLatex(osBean.getArch()) + "} \\\\");
        texDocument.add("Processors & " + osBean.getAvailableProcessors() + " \\\\");

        double loadAvg = osBean.getSystemLoadAverage();
        texDocument.add("Load Average & " + (loadAvg >= 0 ? String.format("%.2f", loadAvg) : "N/A") + " \\\\");

        try {
            var sunOsBean = (com.sun.management.OperatingSystemMXBean) osBean;
            texDocument.add("Process CPU & " + String.format("%.1f%%", sunOsBean.getProcessCpuLoad() * 100) + " \\\\");
            texDocument.add("Total Memory & " + (sunOsBean.getTotalPhysicalMemorySize() / (1024 * 1024)) + " MB \\\\");
        } catch (ClassCastException e) {
            // Extended metrics not available
        }
        texDocument.add("\\end{tabular}");
        texDocument.add("");
    }

    @Override
    public void sayDiff(String before, String after, String language) {
        if (before == null && after == null) {
            return;
        }
        texDocument.add("");
        texDocument.add("\\begin{verbatim}");
        if (language != null && !language.isEmpty()) {
            texDocument.add("--- before (" + language + ")");
            texDocument.add("+++ after (" + language + ")");
        } else {
            texDocument.add("--- before");
            texDocument.add("+++ after");
        }

        if (before != null && after != null) {
            String[] beforeLines = before.split("\n", -1);
            String[] afterLines = after.split("\n", -1);

            int i = 0, j = 0;
            while (i < beforeLines.length && j < afterLines.length) {
                if (beforeLines[i].equals(afterLines[j])) {
                    texDocument.add(" " + template.escapeLatex(beforeLines[i]));
                    i++;
                    j++;
                } else {
                    texDocument.add("-" + template.escapeLatex(beforeLines[i]));
                    i++;
                    if (j < afterLines.length && !afterLines[j].equals(beforeLines[i >= beforeLines.length ? beforeLines.length - 1 : i])) {
                        texDocument.add("+" + template.escapeLatex(afterLines[j]));
                        j++;
                    }
                }
            }

            while (i < beforeLines.length) {
                texDocument.add("-" + template.escapeLatex(beforeLines[i]));
                i++;
            }

            while (j < afterLines.length) {
                texDocument.add("+" + template.escapeLatex(afterLines[j]));
                j++;
            }
        }

        texDocument.add("\\end{verbatim}");
        texDocument.add("");
    }

    @Override
    public void sayBreakingChange(String what, String removedIn, String migrateWith) {
        texDocument.add("");
        texDocument.add("\\begin{center}");
        texDocument.add("\\fbox{\\parbox{0.9\\textwidth}{");
        texDocument.add("\\textbf{\\textcolor{red}{Breaking Change:}} " +
            template.escapeLatex(what != null ? what : "API change") +
            " removed in " + template.escapeLatex(removedIn != null ? removedIn : "next version"));
        if (migrateWith != null && !migrateWith.isEmpty()) {
            texDocument.add("\\\\");
            texDocument.add(template.escapeLatex(migrateWith));
        }
        texDocument.add("}}");
        texDocument.add("\\end{center}");
        texDocument.add("");
    }
}
