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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Hierarchical configuration for DTR documentation output.
 *
 * <p>This annotation allows fine-grained control over DTR's documentation generation
 * behavior at the package, class, or method level. When multiple configuration
 * annotations are present, DTR resolves the final configuration using a hierarchical
 * precedence system:</p>
 *
 * <p><strong>Precedence (highest to lowest):</strong></p>
 * <ol>
 *   <li>System properties (e.g., {@code -Ddtr.format=markdown})</li>
 *   <li>{@code @DtrConfig} on test method</li>
 *   <li>{@code @DtrConfig} on test class</li>
 *   <li>{@code @DtrConfig} on package (package-info.java)</li>
 *   <li>Default values</li>
 * </ol>
 *
 * <p><strong>Example Usage:</strong></p>
 * <pre>{@code
 * // Package-level configuration (package-info.java)
 * @DtrConfig(format = OutputFormat.HTML, outputDir = "docs/api")
 * package com.example.docs;
 *
 * // Class-level override
 * @DtrConfig(format = OutputFormat.PDF, latexTemplate = LatexTemplate.ARTICLE)
 * public class PdfDocumentationTest { }
 *
 * // Method-level override
 * @Test
 * @DtrConfig(format = OutputFormat.MARKDOWN, includeEnvProfile = true)
 * public void testWithEnvProfile() { }
 * }</pre>
 *
 * <p><strong>System Property Override:</strong></p>
 * <p>Any configuration can be overridden at runtime via system properties:</p>
 * <pre>{@code
 * mvn test -Ddtr.format=latex -Ddtr.output.dir=build/docs
 * }</pre>
 *
 * <p><strong>Supported System Properties:</strong></p>
 * <ul>
 *   <li>{@code dtr.format} — Output format (markdown, html, latex, pdf)</li>
 *   <li>{@code dtr.output.dir} — Output directory path</li>
 *   <li>{@code dtr.include.env.profile} — Include environment profile (true/false)</li>
 *   <li>{@code dtr.generate.toc} — Generate table of contents (true/false)</li>
 * </ul>
 *
 * @see DtrConfiguration for runtime configuration resolution
 * @see OutputFormat
 * @see LatexTemplate
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DtrConfig {

    /**
     * Output format for generated documentation.
     *
     * <p>Defaults to {@link OutputFormat#MARKDOWN}. Can be overridden via
     * system property {@code dtr.format}.</p>
     *
     * @return the output format
     */
    OutputFormat format() default OutputFormat.MARKDOWN;

    /**
     * LaTeX template for PDF generation.
     *
     * <p>Only applicable when {@link #format()} is {@link OutputFormat#LATEX}
     * or {@link OutputFormat#PDF}. Defaults to {@link LatexTemplate#ARTICLE}.</p>
     *
     * @return the LaTeX template type
     */
    LatexTemplate latexTemplate() default LatexTemplate.ARTICLE;

    /**
     * Output directory relative to project root.
     *
     * <p>Defaults to {@code "docs/test"}. The directory will be created if it
     * doesn't exist. Can be overridden via system property {@code dtr.output.dir}.</p>
     *
     * @return the output directory path
     */
    String outputDir() default "docs/test";

    /**
     * Whether to include environment profile in output.
     *
     * <p>When {@code true}, DTR automatically includes Java version, OS details,
     * processor count, heap size, timezone, and DTR version at the start of
     * documentation. Defaults to {@code false}. Can be overridden via system
     * property {@code dtr.include.env.profile}.</p>
     *
     * @return {@code true} to include environment profile
     */
    boolean includeEnvProfile() default false;

    /**
     * Whether to generate table of contents.
     *
     * <p>When {@code true}, DTR generates a table of contents from all
     * {@code sayNextSection()} calls. Defaults to {@code true}. Can be overridden
     * via system property {@code dtr.generate.toc}.</p>
     *
     * @return {@code true} to generate table of contents
     */
    boolean generateToc() default true;

    /**
     * Supported output formats for DTR documentation.
     *
     * <p>Each format has specific rendering capabilities and output file extensions:</p>
     * <ul>
     *   <li>{@link #MARKDOWN} — GitHub Flavored Markdown (.md)</li>
     *   <li>{@link #HTML} — Standalone HTML with embedded CSS (.html)</li>
     *   <li>{@link #LATEX} — LaTeX source files (.tex)</li>
     *   <li>{@link #PDF} — Compiled PDF documents via LaTeX (.pdf)</li>
     * </ul>
     */
    enum OutputFormat {
        /**
         * GitHub Flavored Markdown output.
         *
         * <p>Supports tables, code blocks, callouts, cross-references, and Mermaid diagrams.
         * Default format for DTR. Output files use {@code .md} extension.</p>
         */
        MARKDOWN,

        /**
         * Standalone HTML output with embedded CSS.
         *
         * <p>Generates self-contained HTML documents with syntax highlighting, responsive layout,
         * and print-friendly styling. Output files use {@code .html} extension.</p>
         */
        HTML,

        /**
         * LaTeX source output for academic or technical publishing.
         *
         * <p>Generates LaTeX source files compatible with the template specified in
         * {@link LatexTemplate}. Requires LaTeX toolchain (pdflatex, xelatex, etc.)
         * for compilation to PDF. Output files use {@code .tex} extension.</p>
         */
        LATEX,

        /**
         * Compiled PDF output via LaTeX.
         *
         * <p>Automatically compiles LaTeX source to PDF using the configured
         * {@link LatexTemplate}. Requires LaTeX toolchain (pdflatex, xelatex, latexmk)
         * to be installed and available on PATH. Output files use {@code .pdf} extension.</p>
         */
        PDF;

        /**
         * Parses an output format from a system property value.
         *
         * <p>Supports case-insensitive parsing: "markdown", "Markdown", "MARKDOWN" all
         * return {@link #MARKDOWN}.</p>
         *
         * @param value the system property value (may be {@code null})
         * @return the parsed output format, or {@link #MARKDOWN} if parsing fails
         */
        static OutputFormat fromSystemProperty(String value) {
            if (value == null || value.trim().isEmpty()) {
                return MARKDOWN;
            }
            try {
                return OutputFormat.valueOf(value.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                return MARKDOWN;
            }
        }

        /**
         * Parses an output format from system property {@code dtr.format}.
         *
         * @return the parsed output format, or {@link #MARKDOWN} if not set
         */
        static OutputFormat fromSystemProperty() {
            return fromSystemProperty(System.getProperty("dtr.format"));
        }

        /**
         * Returns the file extension for this output format.
         *
         * @return the file extension (including the dot)
         */
        String getExtension() {
            return switch (this) {
                case MARKDOWN -> ".md";
                case HTML -> ".html";
                case LATEX -> ".tex";
                case PDF -> ".pdf";
            };
        }
    }

    /**
     * LaTeX document templates for PDF/LaTeX output.
     *
     * <p>Each template provides a different document structure and set of packages
     * suitable for specific use cases. Templates are only applicable when
     * {@link OutputFormat#LATEX} or {@link OutputFormat#PDF} is selected.</p>
     */
    enum LatexTemplate {
        /**
         * Standard article template for general documentation.
         *
         * <p>Best for: Technical reports, API documentation, tutorials, articles.
         * Features: Section hierarchy, figure/table numbering, bibliography support.</p>
         */
        ARTICLE,

        /**
         * Book template for long-form documentation.
         *
         * <p>Best for: Multi-chapter guides, books, comprehensive documentation.
         * Features: Parts, chapters, front/back matter, index support.</p>
         */
        BOOK,

        /**
         * Beamer presentation template for slides.
         *
         * <p>Best for: Conference talks, training materials, slide decks.
         * Features: Slide layouts, overlays, themes, transitions.</p>
         */
        BEAMER,

        /**
         * Report template for formal documents.
         *
         * <p>Best for: Academic papers, technical reports, theses.
         * Features: Abstract, title page, dedicated chapter structure.</p>
         */
        REPORT;

        /**
         * Returns the LaTeX document class name for this template.
         *
         * @return the document class (e.g., "article", "book", "beamer", "report")
         */
        String getDocumentClass() {
            return name().toLowerCase();
        }

        /**
         * Returns the default file extension for this template.
         *
         * @return the file extension (always ".tex" for LaTeX templates)
         */
        String getExtension() {
            return ".tex";
        }
    }
}
