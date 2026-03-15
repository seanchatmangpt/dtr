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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.seanchatmangpt.dtr.bibliography.BibliographyManager;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
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

    @Override
    public void sayPokaYoke(String operation,
                            java.util.List<String> mistakeProofs,
                            java.util.List<Boolean> verified) {
        // LaTeX rendering: emit a description-list environment for the poka-yoke analysis
        if (operation == null || mistakeProofs == null || verified == null) return;
        texDocument.add("\\paragraph{Mistake-Proofing: " + operation + "}");
        texDocument.add("\\begin{description}");
        int rows = Math.min(mistakeProofs.size(), verified.size());
        for (int i = 0; i < rows; i++) {
            String label = Boolean.TRUE.equals(verified.get(i)) ? "Verified" : "Unverified";
            texDocument.add("  \\item[" + label + "] " + mistakeProofs.get(i));
        }
        texDocument.add("\\end{description}");
    }
}
