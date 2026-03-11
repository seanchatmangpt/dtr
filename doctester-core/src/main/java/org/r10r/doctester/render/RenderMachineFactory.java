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
package org.r10r.doctester.render;

import java.util.ArrayList;
import java.util.List;

import org.r10r.doctester.render.blog.BlogRenderMachine;
import org.r10r.doctester.render.blog.DevToTemplate;
import org.r10r.doctester.render.blog.HashnodeTemplate;
import org.r10r.doctester.render.blog.LinkedInTemplate;
import org.r10r.doctester.render.blog.MediumTemplate;
import org.r10r.doctester.render.blog.SubstackTemplate;
import org.r10r.doctester.render.slides.RevealJsTemplate;
import org.r10r.doctester.render.slides.SlideRenderMachine;
import org.r10r.doctester.rendermachine.MultiRenderMachine;
import org.r10r.doctester.rendermachine.RenderMachine;
import org.r10r.doctester.rendermachine.RenderMachineImpl;
import org.r10r.doctester.rendermachine.latex.RenderMachineLatex;
import org.r10r.doctester.rendermachine.latex.ArXivTemplate;
import org.r10r.doctester.rendermachine.latex.UsPatentTemplate;
import org.r10r.doctester.rendermachine.latex.IEEETemplate;
import org.r10r.doctester.rendermachine.latex.ACMTemplate;
import org.r10r.doctester.rendermachine.latex.NatureTemplate;
import org.r10r.doctester.rendermachine.latex.LatexTemplate;
import org.r10r.doctester.metadata.DocMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating configured render machine instances.
 *
 * Supports selecting output format(s) via Maven system properties:
 * - -Ddoctester.output=markdown,blog,slides,latex,all
 * - -Ddoctester.latex.format=arxiv|patent|ieee|acm|nature (for LaTeX output)
 *
 * LaTeX Format Selection:
 * - arxiv (default): arXiv pre-print submissions
 * - patent: USPTO patent exhibit format
 * - ieee: IEEE journal articles
 * - acm: ACM conference proceedings
 * - nature: Nature scientific reports
 *
 * Examples:
 * - -Ddoctester.output=markdown (default, single render machine)
 * - -Ddoctester.output=all (all formats simultaneously)
 * - -Ddoctester.output=latex -Ddoctester.latex.format=patent (USPTO patents)
 * - -Ddoctester.output=blog,slides (blog posts + slides only)
 */
public final class RenderMachineFactory {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineFactory.class);

    private RenderMachineFactory() {
    }

    /**
     * Create a render machine instance based on system property configuration.
     *
     * The -Ddoctester.output property controls which format(s) to generate:
     * - "markdown" (default): Markdown documentation only
     * - "latex": LaTeX PDF only
     * - "blog": Blog posts (all platforms) only
     * - "slides": Presentation decks only
     * - "all": All formats simultaneously
     * - Comma-separated values: Multiple specific formats
     *
     * @param testClassName the test class name (used for output filenames)
     * @param docMetadata optional metadata for LaTeX rendering (may be null)
     * @return configured render machine instance
     */
    public static RenderMachine createRenderMachine(String testClassName, DocMetadata docMetadata) {
        String output = System.getProperty("doctester.output", "markdown").toLowerCase();

        logger.debug("Creating render machine for output format(s): {}", output);

        if (output.contains("all")) {
            return createAllFormats(testClassName, docMetadata);
        }

        if (output.contains(",")) {
            return createMultipleFormats(output, testClassName, docMetadata);
        }

        // Single format
        return switch (output.trim()) {
            case "markdown" -> new RenderMachineImpl();
            case "latex" -> docMetadata != null ? new RenderMachineLatex(
                selectLatexTemplate(),
                docMetadata) : new RenderMachineImpl();
            case "blog" -> createBlogMachines(testClassName);
            case "slides" -> new SlideRenderMachine(new RevealJsTemplate());
            default -> {
                logger.warn("Unknown output format: {}, defaulting to markdown", output);
                yield new RenderMachineImpl();
            }
        };
    }

    /**
     * Create a render machine for single format tests (simpler variant).
     *
     * @param testClassName the test class name
     * @return configured render machine
     */
    public static RenderMachine createRenderMachine(String testClassName) {
        return createRenderMachine(testClassName, null);
    }

    private static RenderMachine createAllFormats(String testClassName, DocMetadata docMetadata) {
        List<RenderMachine> machines = new ArrayList<>();

        // Always include Markdown
        machines.add(new RenderMachineImpl());

        // Include LaTeX if metadata available
        if (docMetadata != null) {
            machines.add(new RenderMachineLatex(
                selectLatexTemplate(),
                docMetadata));
        }

        // Include all blog platforms
        machines.add(new BlogRenderMachine(new DevToTemplate()));
        machines.add(new BlogRenderMachine(new MediumTemplate()));
        machines.add(new BlogRenderMachine(new LinkedInTemplate()));
        machines.add(new BlogRenderMachine(new SubstackTemplate()));
        machines.add(new BlogRenderMachine(new HashnodeTemplate()));

        // Include slides
        machines.add(new SlideRenderMachine(new RevealJsTemplate()));

        logger.info("Creating multi-render machine with {} format(s)", machines.size());
        return new MultiRenderMachine(machines);
    }

    private static RenderMachine createMultipleFormats(String formats, String testClassName, DocMetadata docMetadata) {
        List<RenderMachine> machines = new ArrayList<>();

        String[] formatList = formats.split(",");
        for (String format : formatList) {
            format = format.trim().toLowerCase();
            switch (format) {
                case "markdown":
                    machines.add(new RenderMachineImpl());
                    break;
                case "latex":
                    if (docMetadata != null) {
                        machines.add(new RenderMachineLatex(
                            selectLatexTemplate(),
                            docMetadata));
                    }
                    break;
                case "blog":
                    machines.add(createBlogMachines(testClassName));
                    break;
                case "slides":
                    machines.add(new SlideRenderMachine(new RevealJsTemplate()));
                    break;
                default:
                    logger.warn("Unknown output format: {}", format);
            }
        }

        if (machines.isEmpty()) {
            logger.warn("No valid output formats specified, using Markdown");
            machines.add(new RenderMachineImpl());
        }

        if (machines.size() == 1) {
            return machines.get(0);
        }

        logger.info("Creating multi-render machine with {} format(s)", machines.size());
        return new MultiRenderMachine(machines);
    }

    private static RenderMachine createBlogMachines(String testClassName) {
        List<RenderMachine> blogMachines = new ArrayList<>();
        blogMachines.add(new BlogRenderMachine(new DevToTemplate()));
        blogMachines.add(new BlogRenderMachine(new MediumTemplate()));
        blogMachines.add(new BlogRenderMachine(new LinkedInTemplate()));
        blogMachines.add(new BlogRenderMachine(new SubstackTemplate()));
        blogMachines.add(new BlogRenderMachine(new HashnodeTemplate()));

        if (blogMachines.size() == 1) {
            return blogMachines.get(0);
        }

        return new MultiRenderMachine(blogMachines);
    }

    /**
     * Select LaTeX template based on system property.
     *
     * The -Ddoctester.latex.format property controls which academic/patent format:
     * - arxiv (default): arXiv pre-print submissions
     * - patent: USPTO patent exhibit format
     * - ieee: IEEE journal articles
     * - acm: ACM conference proceedings
     * - nature: Nature scientific reports
     *
     * @return selected LaTeX template
     */
    private static LatexTemplate selectLatexTemplate() {
        String format = System.getProperty("doctester.latex.format", "arxiv").toLowerCase();

        return switch (format.trim()) {
            case "patent" -> new UsPatentTemplate();
            case "ieee" -> new IEEETemplate();
            case "acm" -> new ACMTemplate();
            case "nature" -> new NatureTemplate();
            case "arxiv" -> new ArXivTemplate();
            default -> {
                logger.warn("Unknown LaTeX format: {}, defaulting to ArXiv", format);
                yield new ArXivTemplate();
            }
        };
    }
}
