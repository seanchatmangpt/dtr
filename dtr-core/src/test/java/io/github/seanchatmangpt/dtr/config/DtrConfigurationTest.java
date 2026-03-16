package io.github.seanchatmangpt.dtr.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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

        assertThat(config.getFormat(), equalTo(DtrConfig.OutputFormat.MARKDOWN));
        assertThat(config.getLatexTemplate(), equalTo(DtrConfig.LatexTemplate.ARTICLE));
        assertThat(config.getOutputDir(), equalTo("docs/test"));
        assertThat(config.isIncludeEnvProfile(), equalTo(false));
        assertThat(config.isGenerateToc(), equalTo(true));
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

        assertThat(config.getFormat(), equalTo(DtrConfig.OutputFormat.PDF));
        assertThat(config.getLatexTemplate(), equalTo(DtrConfig.LatexTemplate.BEAMER));
        assertThat(config.getOutputDir(), equalTo("custom/path"));
        assertThat(config.isIncludeEnvProfile(), equalTo(true));
        assertThat(config.isGenerateToc(), equalTo(false));
    }

    @Test
    public void testApplySystemPropertiesFormat() {
        System.setProperty("dtr.format", "pdf");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertThat(config.getFormat(), equalTo(DtrConfig.OutputFormat.PDF));
    }

    @Test
    public void testApplySystemPropertiesOutputDir() {
        System.setProperty("dtr.output.dir", "build/custom-docs");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertThat(config.getOutputDir(), equalTo("build/custom-docs"));
    }

    @Test
    public void testApplySystemPropertiesLatexTemplate() {
        System.setProperty("dtr.latex.template", "beamer");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertThat(config.getLatexTemplate(), equalTo(DtrConfig.LatexTemplate.BEAMER));
    }

    @Test
    public void testApplySystemPropertiesIncludeEnvProfile() {
        System.setProperty("dtr.include.env.profile", "true");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertThat(config.isIncludeEnvProfile(), equalTo(true));
    }

    @Test
    public void testApplySystemPropertiesGenerateToc() {
        System.setProperty("dtr.generate.toc", "false");

        DtrConfiguration config = DtrConfiguration.builder()
            .applySystemProperties()
            .build();

        assertThat(config.isGenerateToc(), equalTo(false));
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

        assertThat(config.getFormat(), equalTo(DtrConfig.OutputFormat.LATEX));
        assertThat(config.getOutputDir(), equalTo("system/path"));
        assertThat(config.isIncludeEnvProfile(), equalTo(true));
    }

    @Test
    public void testFromSystemProperties() {
        System.setProperty("dtr.format", "pdf");
        System.setProperty("dtr.output.dir", "system/docs");

        DtrConfiguration config = DtrConfiguration.fromSystemProperties();

        assertThat(config.getFormat(), equalTo(DtrConfig.OutputFormat.PDF));
        assertThat(config.getOutputDir(), equalTo("system/docs"));
    }

    @Test
    public void testToString() {
        DtrConfiguration config = DtrConfiguration.builder()
            .format(DtrConfig.OutputFormat.HTML)
            .outputDir("docs/test")
            .build();

        String str = config.toString();
        assertThat(str, containsString("format=HTML"));
        assertThat(str, containsString("outputDir='docs/test'"));
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

        assertThat(config1, equalTo(config2));
        assertThat(config1.hashCode(), equalTo(config2.hashCode()));
        assertThat(config1, not(equalTo(config3)));
    }
}
