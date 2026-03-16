package io.github.seanchatmangpt.dtr.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DtrConfigTest {

    @Test
    public void testOutputFormatEnumValues() {
        DtrConfig.OutputFormat[] formats = DtrConfig.OutputFormat.values();
        assertEquals(4, formats.length);
    }

    @Test
    public void testOutputFormatFromSystemPropertyValid() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty("markdown");
        assertEquals(DtrConfig.OutputFormat.MARKDOWN, format);

        format = DtrConfig.OutputFormat.fromSystemProperty("HTML");
        assertEquals(DtrConfig.OutputFormat.HTML, format);

        format = DtrConfig.OutputFormat.fromSystemProperty("LaTeX");
        assertEquals(DtrConfig.OutputFormat.LATEX, format);

        format = DtrConfig.OutputFormat.fromSystemProperty("PDF");
        assertEquals(DtrConfig.OutputFormat.PDF, format);
    }

    @Test
    public void testOutputFormatFromSystemPropertyInvalid() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty("invalid");
        assertEquals(DtrConfig.OutputFormat.MARKDOWN, format);
    }

    @Test
    public void testOutputFormatFromSystemPropertyNull() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty((String) null);
        assertEquals(DtrConfig.OutputFormat.MARKDOWN, format);
    }

    @Test
    public void testOutputFormatFromSystemPropertyEmpty() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty("  ");
        assertEquals(DtrConfig.OutputFormat.MARKDOWN, format);
    }

    @Test
    public void testOutputFormatGetExtension() {
        assertEquals(".md", DtrConfig.OutputFormat.MARKDOWN.getExtension());
        assertEquals(".html", DtrConfig.OutputFormat.HTML.getExtension());
        assertEquals(".tex", DtrConfig.OutputFormat.LATEX.getExtension());
        assertEquals(".pdf", DtrConfig.OutputFormat.PDF.getExtension());
    }

    @Test
    public void testLatexTemplateEnumValues() {
        DtrConfig.LatexTemplate[] templates = DtrConfig.LatexTemplate.values();
        assertEquals(4, templates.length);
    }

    @Test
    public void testLatexTemplateGetDocumentClass() {
        assertEquals("article", DtrConfig.LatexTemplate.ARTICLE.getDocumentClass());
        assertEquals("book", DtrConfig.LatexTemplate.BOOK.getDocumentClass());
        assertEquals("beamer", DtrConfig.LatexTemplate.BEAMER.getDocumentClass());
        assertEquals("report", DtrConfig.LatexTemplate.REPORT.getDocumentClass());
    }

    @Test
    public void testLatexTemplateGetExtension() {
        assertEquals(".tex", DtrConfig.LatexTemplate.ARTICLE.getExtension());
        assertEquals(".tex", DtrConfig.LatexTemplate.BOOK.getExtension());
        assertEquals(".tex", DtrConfig.LatexTemplate.BEAMER.getExtension());
        assertEquals(".tex", DtrConfig.LatexTemplate.REPORT.getExtension());
    }
}
