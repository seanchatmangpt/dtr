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
package io.github.seanchatmangpt.dtr.config;

/**
 * Runtime configuration for DTR documentation output.
 *
 * <p>This class holds resolved configuration settings for DTR, combining
 * defaults, annotations, and system properties according to the precedence
 * rules defined in {@link DtrConfig}.</p>
 *
 * <p><strong>Supported System Properties:</strong></p>
 * <ul>
 *   <li>{@code dtr.format} — Output format (markdown, html, latex, pdf)</li>
 *   <li>{@code dtr.output.dir} — Output directory path</li>
 *   <li>{@code dtr.latex.template} — LaTeX template (article, book, beamer, report)</li>
 *   <li>{@code dtr.include.env.profile} — Include environment profile (true/false)</li>
 *   <li>{@code dtr.generate.toc} — Generate table of contents (true/false)</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // Create configuration with defaults
 * DtrConfiguration config = DtrConfiguration.builder().build();
 *
 * // Create custom configuration
 * DtrConfiguration config = DtrConfiguration.builder()
 *     .format(OutputFormat.PDF)
 *     .latexTemplate(LatexTemplate.ARTICLE)
 *     .outputDir("build/docs")
 *     .includeEnvProfile(true)
 *     .build();
 * }</pre>
 *
 * @see DtrConfig for annotation definition
 * @see DtrConfig.OutputFormat
 * @see DtrConfig.LatexTemplate
 */
public class DtrConfiguration {

    private final DtrConfig.OutputFormat format;
    private final DtrConfig.LatexTemplate latexTemplate;
    private final String outputDir;
    private final boolean includeEnvProfile;
    private final boolean generateToc;

    /**
     * Creates a new DtrConfiguration with the specified settings.
     *
     * @param format the output format
     * @param latexTemplate the LaTeX template
     * @param outputDir the output directory
     * @param includeEnvProfile whether to include environment profile
     * @param generateToc whether to generate table of contents
     */
    private DtrConfiguration(
            DtrConfig.OutputFormat format,
            DtrConfig.LatexTemplate latexTemplate,
            String outputDir,
            boolean includeEnvProfile,
            boolean generateToc) {
        this.format = format;
        this.latexTemplate = latexTemplate;
        this.outputDir = outputDir;
        this.includeEnvProfile = includeEnvProfile;
        this.generateToc = generateToc;
    }

    /**
     * Returns the output format for documentation generation.
     *
     * @return the output format
     */
    public DtrConfig.OutputFormat getFormat() {
        return format;
    }

    /**
     * Returns the LaTeX template for PDF/LaTeX generation.
     *
     * @return the LaTeX template
     */
    public DtrConfig.LatexTemplate getLatexTemplate() {
        return latexTemplate;
    }

    /**
     * Returns the output directory path.
     *
     * @return the output directory (relative to project root)
     */
    public String getOutputDir() {
        return outputDir;
    }

    /**
     * Returns whether to include environment profile in documentation.
     *
     * @return {@code true} if environment profile should be included
     */
    public boolean isIncludeEnvProfile() {
        return includeEnvProfile;
    }

    /**
     * Returns whether to generate table of contents.
     *
     * @return {@code true} if table of contents should be generated
     */
    public boolean isGenerateToc() {
        return generateToc;
    }

    /**
     * Creates a new builder for constructing DtrConfiguration instances.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a configuration with system properties applied.
     *
     * <p>System properties override builder defaults but not annotation values
     * (which are applied separately in the resolution algorithm).</p>
     *
     * @return a configuration with system properties applied
     */
    public static DtrConfiguration fromSystemProperties() {
        return builder().applySystemProperties().build();
    }

    /**
     * Builder for constructing DtrConfiguration instances with fluent API.
     *
     * <p>Example usage:</p>
     * <pre>{@code
     * DtrConfiguration config = DtrConfiguration.builder()
     *     .format(OutputFormat.PDF)
     *     .latexTemplate(LatexTemplate.BEAMER)
     *     .outputDir("custom/path")
     *     .includeEnvProfile(true)
     *     .generateToc(false)
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private DtrConfig.OutputFormat format = DtrConfig.OutputFormat.MARKDOWN;
        private DtrConfig.LatexTemplate latexTemplate = DtrConfig.LatexTemplate.ARTICLE;
        private String outputDir = "docs/test";
        private boolean includeEnvProfile = false;
        private boolean generateToc = true;

        /**
         * Creates a new builder with default values.
         */
        private Builder() {
            // Private constructor - use builder() factory method
        }

        /**
         * Applies configuration from system properties.
         *
         * <p>System properties have highest precedence and override all
         * annotation values. Supported properties:</p>
         * <ul>
         *   <li>{@code dtr.format}</li>
         *   <li>{@code dtr.output.dir}</li>
         *   <li>{@code dtr.latex.template}</li>
         *   <li>{@code dtr.include.env.profile}</li>
         *   <li>{@code dtr.generate.toc}</li>
         * </ul>
         *
         * @return this builder for method chaining
         */
        public Builder applySystemProperties() {
            // Format
            String formatProp = System.getProperty("dtr.format");
            if (formatProp != null && !formatProp.trim().isEmpty()) {
                this.format = DtrConfig.OutputFormat.fromSystemProperty(formatProp);
            }

            // LaTeX Template
            String latexProp = System.getProperty("dtr.latex.template");
            if (latexProp != null && !latexProp.trim().isEmpty()) {
                try {
                    this.latexTemplate = DtrConfig.LatexTemplate.valueOf(
                        latexProp.trim().toUpperCase());
                } catch (IllegalArgumentException e) {
                    // Keep default if invalid value
                }
            }

            // Output Directory
            String outputDirProp = System.getProperty("dtr.output.dir");
            if (outputDirProp != null && !outputDirProp.trim().isEmpty()) {
                this.outputDir = outputDirProp.trim();
            }

            // Include Environment Profile
            String envProfileProp = System.getProperty("dtr.include.env.profile");
            if (envProfileProp != null) {
                this.includeEnvProfile = Boolean.parseBoolean(envProfileProp.trim());
            }

            // Generate Table of Contents
            String tocProp = System.getProperty("dtr.generate.toc");
            if (tocProp != null) {
                this.generateToc = Boolean.parseBoolean(tocProp.trim());
            }

            return this;
        }

        /**
         * Applies configuration from a {@link DtrConfig} annotation.
         *
         * <p>Annotation values override builder state but not system properties
         * (which are applied after annotations in the resolution algorithm).</p>
         *
         * @param annotation the annotation to apply
         * @return this builder for method chaining
         */
        public Builder applyAnnotation(DtrConfig annotation) {
            if (annotation == null) {
                return this;
            }

            // Only override if annotation has non-default value
            if (annotation.format() != DtrConfig.OutputFormat.MARKDOWN) {
                this.format = annotation.format();
            }
            if (annotation.latexTemplate() != DtrConfig.LatexTemplate.ARTICLE) {
                this.latexTemplate = annotation.latexTemplate();
            }
            if (!annotation.outputDir().isEmpty()) {
                this.outputDir = annotation.outputDir();
            }
            this.includeEnvProfile = annotation.includeEnvProfile();
            this.generateToc = annotation.generateToc();

            return this;
        }

        /**
         * Sets the output format.
         *
         * @param format the output format
         * @return this builder for method chaining
         */
        public Builder format(DtrConfig.OutputFormat format) {
            this.format = format;
            return this;
        }

        /**
         * Sets the LaTeX template.
         *
         * @param latexTemplate the LaTeX template
         * @return this builder for method chaining
         */
        public Builder latexTemplate(DtrConfig.LatexTemplate latexTemplate) {
            this.latexTemplate = latexTemplate;
            return this;
        }

        /**
         * Sets the output directory.
         *
         * @param outputDir the output directory path
         * @return this builder for method chaining
         */
        public Builder outputDir(String outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        /**
         * Sets whether to include environment profile.
         *
         * @param includeEnvProfile {@code true} to include environment profile
         * @return this builder for method chaining
         */
        public Builder includeEnvProfile(boolean includeEnvProfile) {
            this.includeEnvProfile = includeEnvProfile;
            return this;
        }

        /**
         * Sets whether to generate table of contents.
         *
         * @param generateToc {@code true} to generate table of contents
         * @return this builder for method chaining
         */
        public Builder generateToc(boolean generateToc) {
            this.generateToc = generateToc;
            return this;
        }

        /**
         * Builds the configuration with the current builder state.
         *
         * @return a new DtrConfiguration instance
         */
        public DtrConfiguration build() {
            return new DtrConfiguration(
                format,
                latexTemplate,
                outputDir,
                includeEnvProfile,
                generateToc
            );
        }
    }

    @Override
    public String toString() {
        return "DtrConfiguration{" +
            "format=" + format +
            ", latexTemplate=" + latexTemplate +
            ", outputDir='" + outputDir + '\'' +
            ", includeEnvProfile=" + includeEnvProfile +
            ", generateToc=" + generateToc +
            '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DtrConfiguration that = (DtrConfiguration) o;

        if (includeEnvProfile != that.includeEnvProfile) return false;
        if (generateToc != that.generateToc) return false;
        if (format != that.format) return false;
        if (latexTemplate != that.latexTemplate) return false;
        return outputDir.equals(that.outputDir);
    }

    @Override
    public int hashCode() {
        int result = format.hashCode();
        result = 31 * result + latexTemplate.hashCode();
        result = 31 * result + outputDir.hashCode();
        result = 31 * result + (includeEnvProfile ? 1 : 0);
        result = 31 * result + (generateToc ? 1 : 0);
        return result;
    }
}
