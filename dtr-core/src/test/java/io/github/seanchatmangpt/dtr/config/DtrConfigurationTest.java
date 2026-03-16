package io.github.seanchatmangpt.dtr.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DtrConfigurationTest {

    @BeforeEach
    public void setUp() {
        System.clearProperty("dtr.format");
        System.clearProperty("dtr.output.dir");
        System.clearProperty("dtr.latex.template");
        System.clearProperty("dtr.include.env.profile");
        System.clearProperty("dtr.generate.toc");
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty("dtr.format");
        System.clearProperty("dtr.output.dir");
        System.clearProperty("dtr.latex.template");
        System.clearProperty("dtr.include.env.profile");
        System.clearProperty("dtr.generate.toc");
    }

    @Test
    public void testBuilderWithDefaults() {
        DtrConfiguration config = DtrConfiguration.builder().build();

        assertEquals(DtrConfig.OutputFormat.MARKDOWN, config.getFormat());
        assertEquals(DtrConfig.LatexTemplate.ARTICLE, config.getLatexTemplate());
        assertEquals("docs/test", config.getOutputDir());
        assertEquals(false, config.isIncludeEnvProfile());
        assertEquals(true, config.isGenerateToc());
    }

    @Test
    public void testBuilderWithCustomValues() {
        DtrConfiguration config = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.PDF)
            .latexTemplate(DtrConfig.LatexTemplate.BEAMER)
            .outputDir("custom/path")
            .includeEnvProfile(true)
            .generateToc(false)
            .build();

        assertEquals(DtrConfig.OutputFormat.PDF, config.getFormat());
        assertEquals(DtrConfig.LatexTemplate.BEAMER, config.getLatexTemplate());
        assertEquals("custom/path", config.getOutputDir());
        assertEquals(true, config.isIncludeEnvProfile());
        assertEquals(false, config.isGenerateToc());
    }

    @Test
    public void testApplySystemPropertiesFormat() {
        System.setProperty("dtr.format", "pdf");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertEquals(DtrConfig.OutputFormat.PDF, config.getFormat());
    }

    @Test
    public void testApplySystemPropertiesOutputDir() {
        System.setProperty("dtr.output.dir", "build/custom-docs");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertEquals("build/custom-docs", config.getOutputDir());
    }

    @Test
    public void testApplySystemPropertiesLatexTemplate() {
        System.setProperty("dtr.latex.template", "beamer");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertEquals(DtrConfig.LatexTemplate.BEAMER, config.getLatexTemplate());
    }

    @Test
    public void testApplySystemPropertiesIncludeEnvProfile() {
        System.setProperty("dtr.include.env.profile", "true");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertEquals(true, config.isIncludeEnvProfile());
    }

    @Test
    public void testApplySystemPropertiesGenerateToc() {
        System.setProperty("dtr.generate.toc", "false");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertEquals(false, config.isGenerateToc());
    }

    @Test
    public void testSystemPropertiesOverrideBuilderDefaults() {
        DtrConfiguration.Builder builder = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.HTML)
            .outputDir("builder/path")
            .includeEnvProfile(false);

        System.setProperty("dtr.format", "latex");
        System.setProperty("dtr.output.dir", "system/path");
        System.setProperty("dtr.include.env.profile", "true");

        DtrConfiguration config = builder.applySystemProperties().build();

        assertEquals(DtrConfig.OutputFormat.LATEX, config.getFormat());
        assertEquals("system/path", config.getOutputDir());
        assertEquals(true, config.isIncludeEnvProfile());
    }

    @Test
    public void testFromSystemProperties() {
        System.setProperty("dtr.format", "pdf");
        System.setProperty("dtr.output.dir", "system/docs");

        DtrConfiguration config = DtrConfiguration.fromSystemProperties();

        assertEquals(DtrConfig.OutputFormat.PDF, config.getFormat());
        assertEquals("system/docs", config.getOutputDir());
    }

    @Test
    public void testToString() {
        DtrConfiguration config = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.HTML)
            .outputDir("docs/test")
            .build();

        String str = config.toString();
        assertTrue(str.contains("format=HTML"));
        assertTrue(str.contains("outputDir='docs/test'"));
    }

    @Test
    public void testEqualsAndHashCode() {
        DtrConfiguration config1 = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.PDF)
            .latexTemplate(DtrConfig.LatexTemplate.ARTICLE)
            .outputDir("docs/test")
            .includeEnvProfile(true)
            .generateToc(true)
            .build();

        DtrConfiguration config2 = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.PDF)
            .latexTemplate(DtrConfig.LatexTemplate.ARTICLE)
            .outputDir("docs/test")
            .includeEnvProfile(true)
            .generateToc(true)
            .build();

        DtrConfiguration config3 = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.HTML)
            .build();

        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        assertTrue(!config1.equals(config3));
    }
}
