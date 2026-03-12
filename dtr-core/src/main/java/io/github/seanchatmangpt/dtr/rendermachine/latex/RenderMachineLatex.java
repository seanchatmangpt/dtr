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
import java.util.Map.Entry;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.cookie.Cookie;
import io.github.seanchatmangpt.dtr.bibliography.BibliographyManager;
import io.github.seanchatmangpt.dtr.crossref.DocTestRef;
import io.github.seanchatmangpt.dtr.metadata.DocMetadata;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;
import org.hamcrest.Matcher;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * LaTeX-based render machine generating publication-ready PDF documentation.
 *
 * Maps all RenderMachine say* methods to LaTeX commands for document generation
 * via pdflatex, latexmk, or xelatex.
 *
 * Supports sealed template system (ArXiv, USPTO, IEEE, ACM, Nature) for format
 * customization. HTTP exchanges formatted as code listings with syntax highlighting.
 *
 * Output: .tex file written to docs/test/latex/ directory.
 */
public final class RenderMachineLatex extends RenderMachine {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineLatex.class);

    private static final String BASE_DIR = "docs/test/latex";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final LatexTemplate template;
    private final DocMetadata metadata;
    private final List<String> texDocument;

    private TestBrowser testBrowser;
    private String fileName;

    /**
     * Create a LaTeX render machine with specified template and metadata.
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
    public List<Cookie> sayAndGetCookies() {
        List<Cookie> cookies = testBrowser.getCookies();

        texDocument.add("");
        texDocument.add("\\subsubsection*{Cookies}");

        if (cookies.isEmpty()) {
            texDocument.add("\\textit{(No cookies)}");
        } else {
            var cookieList = new ArrayList<String>();
            for (Cookie cookie : cookies) {
                String cookieStr = template.escapeLatex(cookie.getName())
                    + " = \\texttt{"
                    + template.escapeLatex(cookie.getValue()) + "}";
                cookieList.add(cookieStr);
            }
            texDocument.add(template.formatUnorderedList(cookieList));
        }

        return cookies;
    }

    @Override
    public Cookie sayAndGetCookieWithName(String name) {
        Cookie cookie = testBrowser.getCookieWithName(name);

        texDocument.add("");
        texDocument.add("\\subsubsection*{Cookie: " + template.escapeLatex(name) + "}");

        if (cookie != null) {
            var cookieMap = Map.of(
                "Name", cookie.getName(),
                "Value", cookie.getValue(),
                "Path", cookie.getPath() != null ? cookie.getPath() : "(default)",
                "Domain", cookie.getDomain() != null ? cookie.getDomain() : "(default)"
            );
            texDocument.add(template.formatKeyValue(cookieMap));
        } else {
            texDocument.add("\\textit{(Cookie not found)}");
        }

        return cookie;
    }

    @Override
    public Response sayAndMakeRequest(Request request) {
        Response response = testBrowser.makeRequest(request);
        formatHttpExchange(request, response);
        return response;
    }

    @Override
    public <T> void sayAndAssertThat(String message, T actual, Matcher<? super T> matcher) {
        sayAndAssertThat(message, "", actual, matcher);
    }

    @Override
    public <T> void sayAndAssertThat(String message, String reason, T actual, Matcher<? super T> matcher) {
        try {
            Assert.assertThat(reason, actual, matcher);
            texDocument.add("");
            texDocument.add("\\textbf{\\color{green}\\checkmark} " + template.escapeLatex(message));
        } catch (AssertionError e) {
            texDocument.add("");
            texDocument.add("\\textbf{\\color{red}\\times} \\textbf{FAILED:} " + template.escapeLatex(message));
            texDocument.add("");
            texDocument.add("\\begin{verbatim}");
            texDocument.add(convertStackTraceToString(e));
            texDocument.add("\\end{verbatim}");
            throw e;
        }
    }

    @Override
    public void setTestBrowser(TestBrowser testBrowser) {
        this.testBrowser = testBrowser;
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
        doc.add("\\author{DocTester}");
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

    private void formatHttpExchange(Request request, Response response) {
        texDocument.add("");
        texDocument.add("\\subsubsection*{HTTP Exchange}");

        // Request section
        texDocument.add("");
        texDocument.add("\\textbf{Request}");
        texDocument.add("");

        var requestLines = new ArrayList<String>();
        String httpMethod = request.httpRequestType;
        String url = request.uri.toString();
        requestLines.add(httpMethod + " " + url);

        for (Entry<String, String> header : request.headers.entrySet()) {
            requestLines.add(header.getKey() + ": " + header.getValue());
        }

        if (request.payload != null) {
            requestLines.add("");
            requestLines.add(request.payloadAsPrettyString());
        }

        texDocument.add(template.formatCodeBlock(String.join("\n", requestLines), null));

        // Response section
        texDocument.add("");
        texDocument.add("\\textbf{Response}");
        texDocument.add("");

        var responseLines = new ArrayList<String>();
        responseLines.add("HTTP/1.1 " + response.httpStatus);

        if (!response.headers.isEmpty()) {
            for (Entry<String, String> header : response.headers.entrySet()) {
                responseLines.add(header.getKey() + ": " + header.getValue());
            }
        }

        if (response.payload != null) {
            responseLines.add("");
            responseLines.add(response.payloadAsPrettyString());
        }

        texDocument.add(template.formatCodeBlock(String.join("\n", responseLines), null));
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
}
