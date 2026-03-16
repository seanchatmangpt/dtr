package io.github.seanchatmangpt.dtr.config;

import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItemInArray;
import static org.hamcrest.Matchers.arrayWithSize;

public class DtrConfigTest {

    @Test
    public void testOutputFormatEnumValues() {
        DtrConfig.OutputFormat[] formats = DtrConfig.OutputFormat.values();
        assertThat(formats, arrayWithSize(4));
        assertThat(formats, hasItemInArray(DtrConfig.OutputFormat.MARKDOWN));
        assertThat(formats, hasItemInArray(DtrConfig.OutputFormat.HTML));
        assertThat(formats, hasItemInArray(DtrConfig.OutputFormat.LATEX));
        assertThat(formats, hasItemInArray(DtrConfig.OutputFormat.PDF));
    }

    @Test
    public void testOutputFormatFromSystemPropertyValid() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty("markdown");
        assertThat(format, equalTo(DtrConfig.OutputFormat.MARKDOWN));

        format = DtrConfig.OutputFormat.fromSystemProperty("HTML");
        assertThat(format, equalTo(DtrConfig.OutputFormat.HTML));

        format = DtrConfig.OutputFormat.fromSystemProperty("LaTeX");
        assertThat(format, equalTo(DtrConfig.OutputFormat.LATEX));

        format = DtrConfig.OutputFormat.fromSystemProperty("PDF");
        assertThat(format, equalTo(DtrConfig.OutputFormat.PDF));
    }

    @Test
    public void testOutputFormatFromSystemPropertyInvalid() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty("invalid");
        assertThat(format, equalTo(DtrConfig.OutputFormat.MARKDOWN));
    }

    @Test
    public void testOutputFormatFromSystemPropertyNull() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty((String) null);
        assertThat(format, equalTo(DtrConfig.OutputFormat.MARKDOWN));
    }

    @Test
    public void testOutputFormatFromSystemPropertyEmpty() {
        DtrConfig.OutputFormat format = DtrConfig.OutputFormat.fromSystemProperty("  ");
        assertThat(format, equalTo(DtrConfig.OutputFormat.MARKDOWN));
    }

    @Test
    public void testOutputFormatGetExtension() {
        assertThat(DtrConfig.OutputFormat.MARKDOWN.getExtension(), equalTo(".md"));
        assertThat(DtrConfig.OutputFormat.HTML.getExtension(), equalTo(".html"));
        assertThat(DtrConfig.OutputFormat.LATEX.getExtension(), equalTo(".tex"));
        assertThat(DtrConfig.OutputFormat.PDF.getExtension(), equalTo(".pdf"));
    }

    @Test
    public void testLatexTemplateEnumValues() {
        DtrConfig.LatexTemplate[] templates = DtrConfig.LatexTemplate.values();
        assertThat(templates, arrayWithSize(4));
        assertThat(templates, hasItemInArray(DtrConfig.LatexTemplate.ARTICLE));
        assertThat(templates, hasItemInArray(DtrConfig.LatexTemplate.BOOK));
        assertThat(templates, hasItemInArray(DtrConfig.LatexTemplate.BEAMER));
        assertThat(templates, hasItemInArray(DtrConfig.LatexTemplate.REPORT));
    }

    @Test
    public void testLatexTemplateGetDocumentClass() {
        assertThat(DtrConfig.LatexTemplate.ARTICLE.getDocumentClass(), equalTo("article"));
        assertThat(DtrConfig.LatexTemplate.BOOK.getDocumentClass(), equalTo("book"));
        assertThat(DtrConfig.LatexTemplate.BEAMER.getDocumentClass(), equalTo("beamer"));
        assertThat(DtrConfig.LatexTemplate.REPORT.getDocumentClass(), equalTo("report"));
    }

    @Test
    public void testLatexTemplateGetExtension() {
        assertThat(DtrConfig.LatexTemplate.ARTICLE.getExtension(), equalTo(".tex"));
        assertThat(DtrConfig.LatexTemplate.BOOK.getExtension(), equalTo(".tex"));
        assertThat(DtrConfig.LatexTemplate.BEAMER.getExtension(), equalTo(".tex"));
        assertThat(DtrConfig.LatexTemplate.REPORT.getExtension(), equalTo(".tex"));
    }
}
