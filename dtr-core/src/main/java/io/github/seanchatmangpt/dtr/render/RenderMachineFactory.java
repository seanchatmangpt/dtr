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
package io.github.seanchatmangpt.dtr.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import io.github.seanchatmangpt.dtr.render.blog.BlogRenderMachine;
import io.github.seanchatmangpt.dtr.render.blog.DevToTemplate;
import io.github.seanchatmangpt.dtr.render.blog.HashnodeTemplate;
import io.github.seanchatmangpt.dtr.render.blog.LinkedInTemplate;
import io.github.seanchatmangpt.dtr.render.blog.MediumTemplate;
import io.github.seanchatmangpt.dtr.render.blog.SubstackTemplate;
import io.github.seanchatmangpt.dtr.render.slides.RevealJsTemplate;
import io.github.seanchatmangpt.dtr.render.slides.SlideRenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.MultiRenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.rendermachine.latex.RenderMachineLatex;
import io.github.seanchatmangpt.dtr.rendermachine.latex.ArXivTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.UsPatentTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.IEEETemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.ACMTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.NatureTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.LatexTemplate;
import io.github.seanchatmangpt.dtr.metadata.DocMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating configured render machine instances.
 *
 * Supports selecting output format(s) via Maven system properties:
 * - -Ddtr.output=markdown,blog,slides,latex,all
 * - -Ddtr.latex.format=arxiv|patent|ieee|acm|nature (for LaTeX output)
 *
 * LaTeX Format Selection:
 * - arxiv (default): arXiv pre-print submissions
 * - patent: USPTO patent exhibit format
 * - ieee: IEEE journal articles
 * - acm: ACM conference proceedings
 * - nature: Nature scientific reports
 *
 * Java 26 Enhancement (JEP 526 - Lazy Constants):
 * Template instances are cached and reused across all test invocations.
 * The JIT compiler will inline these constants after first access, eliminating
 * allocation overhead for subsequent factory calls.
 *
 * Examples:
 * - -Ddtr.output=markdown (default, single render machine)
 * - -Ddtr.output=all (all formats simultaneously)
 * - -Ddtr.output=latex -Ddtr.latex.format=patent (USPTO patents)
 * - -Ddtr.output=blog,slides (blog posts + slides only)
 */
public final class RenderMachineFactory {

    private static final Logger logger = LoggerFactory.getLogger(RenderMachineFactory.class);

    // Java 26 JEP 526 - Lazy Constants
    // Each template is computed once, then cached for reuse across all tests
    // The JIT compiler will hoist and inline these after first access.
    private static final Supplier<DevToTemplate> DEV_TO =
        LazyValue.of(DevToTemplate::new);
    private static final Supplier<MediumTemplate> MEDIUM =
        LazyValue.of(MediumTemplate::new);
    private static final Supplier<LinkedInTemplate> LINKEDIN =
        LazyValue.of(LinkedInTemplate::new);
    private static final Supplier<SubstackTemplate> SUBSTACK =
        LazyValue.of(SubstackTemplate::new);
    private static final Supplier<HashnodeTemplate> HASHNODE =
        LazyValue.of(HashnodeTemplate::new);

    // LaTeX templates
    private static final Supplier<ArXivTemplate> ARXIV =
        LazyValue.of(ArXivTemplate::new);
    private static final Supplier<UsPatentTemplate> PATENT =
        LazyValue.of(UsPatentTemplate::new);
    private static final Supplier<IEEETemplate> IEEE =
        LazyValue.of(IEEETemplate::new);
    private static final Supplier<ACMTemplate> ACM =
        LazyValue.of(ACMTemplate::new);
    private static final Supplier<NatureTemplate> NATURE =
        LazyValue.of(NatureTemplate::new);

    // Slides template
    private static final Supplier<RevealJsTemplate> REVEALJS =
        LazyValue.of(RevealJsTemplate::new);

    private RenderMachineFactory() {
    }

    /**
     * Create a render machine instance based on system property configuration.
     *
     * The -Ddtr.output property controls which format(s) to generate:
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
        String output = System.getProperty("dtr.output", "markdown").toLowerCase();

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
                selectLatexTemplateLazy(),
                docMetadata) : new RenderMachineImpl();
            case "blog" -> createBlogMachines(testClassName);
            case "slides" -> new SlideRenderMachine(REVEALJS.get());
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
                selectLatexTemplateLazy(),
                docMetadata));
        }

        // Include all blog platforms (JEP 526 - cached instances)
        machines.add(new BlogRenderMachine(DEV_TO.get()));
        machines.add(new BlogRenderMachine(MEDIUM.get()));
        machines.add(new BlogRenderMachine(LINKEDIN.get()));
        machines.add(new BlogRenderMachine(SUBSTACK.get()));
        machines.add(new BlogRenderMachine(HASHNODE.get()));

        // Include slides (JEP 526 - cached instance)
        machines.add(new SlideRenderMachine(REVEALJS.get()));

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
                            selectLatexTemplateLazy(),
                            docMetadata));
                    }
                    break;
                case "blog":
                    machines.add(createBlogMachines(testClassName));
                    break;
                case "slides":
                    machines.add(new SlideRenderMachine(REVEALJS.get()));
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
        // JEP 526 - Use cached blog template instances
        blogMachines.add(new BlogRenderMachine(DEV_TO.get()));
        blogMachines.add(new BlogRenderMachine(MEDIUM.get()));
        blogMachines.add(new BlogRenderMachine(LINKEDIN.get()));
        blogMachines.add(new BlogRenderMachine(SUBSTACK.get()));
        blogMachines.add(new BlogRenderMachine(HASHNODE.get()));

        if (blogMachines.size() == 1) {
            return blogMachines.get(0);
        }

        return new MultiRenderMachine(blogMachines);
    }

    /**
     * Select LaTeX template based on system property (uses cached instances for JEP 526).
     *
     * The -Ddtr.latex.format property controls which academic/patent format:
     * - arxiv (default): arXiv pre-print submissions
     * - patent: USPTO patent exhibit format
     * - ieee: IEEE journal articles
     * - acm: ACM conference proceedings
     * - nature: Nature scientific reports
     *
     * @return selected cached LaTeX template
     */
    private static LatexTemplate selectLatexTemplateLazy() {
        String format = System.getProperty("dtr.latex.format", "arxiv").toLowerCase();

        return switch (format.trim()) {
            case "patent" -> PATENT.get();
            case "ieee" -> IEEE.get();
            case "acm" -> ACM.get();
            case "nature" -> NATURE.get();
            case "arxiv" -> ARXIV.get();
            default -> {
                logger.warn("Unknown LaTeX format: {}, defaulting to ArXiv", format);
                yield ARXIV.get();
            }
        };
    }

    /**
     * Internal lazy value container (approximates Java 26 JEP 526 StableValue).
     *
     * On Java 26+, this would be replaced with StableValue<T> for JIT optimization.
     * The cached instance is computed once and reused, with the JIT compiler
     * inlining it as a compile-time constant after first access.
     */
    private static final class LazyValue {
        private static <T> Supplier<T> of(Supplier<T> initializer) {
            return new SingletonSupplier<>(initializer);
        }

        private static final class SingletonSupplier<T> implements Supplier<T> {
            private volatile T value;
            private final Supplier<T> initializer;

            SingletonSupplier(Supplier<T> initializer) {
                this.initializer = initializer;
            }

            @Override
            public T get() {
                T result = value;
                if (result == null) {
                    synchronized (this) {
                        result = value;
                        if (result == null) {
                            value = result = initializer.get();
                        }
                    }
                }
                return result;
            }
        }
    }
}
