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

import java.util.ArrayList;
import java.util.List;

import io.github.seanchatmangpt.dtr.metadata.DocMetadata;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;
import io.github.seanchatmangpt.dtr.rendermachine.MultiRenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.latex.RenderMachineLatex;
import io.github.seanchatmangpt.dtr.rendermachine.latex.ArXivTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.UsPatentTemplate;
import io.github.seanchatmangpt.dtr.rendermachine.latex.LatexTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central configuration for render machine selection and instantiation.
 *
 * Parses system properties to determine which render machines to activate:
 * - {@code -Ddoctester.output=markdown}: Markdown only (default, backward compatible)
 * - {@code -Ddoctester.output=latex}: LaTeX/PDF only
 * - {@code -Ddoctester.output=markdown,latex}: Both formats simultaneously
 *
 * LaTeX template selection:
 * - {@code -Ddoctester.latex.template=arxiv}: ArXiv format (default)
 * - {@code -Ddoctester.latex.template=patent}: USPTO/patent format
 */
public final class RenderConfig {

    private static final Logger logger = LoggerFactory.getLogger(RenderConfig.class);

    private static final String PROP_OUTPUT_FORMATS = "doctester.output";
    private static final String PROP_LATEX_TEMPLATE = "doctester.latex.template";

    private static final String DEFAULT_FORMATS = "markdown";
    private static final String DEFAULT_TEMPLATE = "arxiv";

    /**
     * Create render machine(s) based on system properties.
     *
     * @return Single RenderMachine if one format selected, or MultiRenderMachine if multiple
     */
    public static RenderMachine createRenderMachines() {
        String formats = System.getProperty(PROP_OUTPUT_FORMATS, DEFAULT_FORMATS);
        String[] formatArray = formats.split(",");

        List<RenderMachine> machines = new ArrayList<>();
        DocMetadata metadata = DocMetadata.fromBuild();

        for (String format : formatArray) {
            String trimmed = format.trim().toLowerCase();

            switch (trimmed) {
                case "markdown" -> {
                    machines.add(new RenderMachineImpl());
                    logger.debug("Added Markdown render machine");
                }

                case "latex" -> {
                    String templateName = System.getProperty(PROP_LATEX_TEMPLATE, DEFAULT_TEMPLATE);
                    LatexTemplate template = selectTemplate(templateName);
                    machines.add(new RenderMachineLatex(template, metadata));
                    logger.debug("Added LaTeX render machine (template: {})", templateName);
                }

                case "pdf" -> {
                    // PDF is LaTeX compilation (handled post-test by Maven plugin)
                    String templateName = System.getProperty(PROP_LATEX_TEMPLATE, DEFAULT_TEMPLATE);
                    LatexTemplate template = selectTemplate(templateName);
                    machines.add(new RenderMachineLatex(template, metadata));
                    logger.debug("Added PDF render machine (template: {})", templateName);
                }

                default -> {
                    logger.warn("Unknown output format: {}. Ignoring.", trimmed);
                }
            }
        }

        if (machines.isEmpty()) {
            logger.warn("No valid render formats specified. Using default: markdown");
            machines.add(new RenderMachineImpl());
        }

        if (machines.size() == 1) {
            return machines.get(0);
        } else {
            logger.info("Multi-render mode activated: {} formats", machines.size());
            return new MultiRenderMachine(machines);
        }
    }

    /**
     * Select LaTeX template by name.
     */
    private static LatexTemplate selectTemplate(String templateName) {
        return switch (templateName.toLowerCase()) {
            case "arxiv" -> new ArXivTemplate();
            case "patent", "uspto" -> new UsPatentTemplate();
            default -> {
                logger.warn("Unknown template: {}. Using default: arxiv", templateName);
                yield new ArXivTemplate();
            }
        };
    }

    /**
     * Check if a specific output format is enabled.
     */
    public static boolean isFormatEnabled(String format) {
        String formats = System.getProperty(PROP_OUTPUT_FORMATS, DEFAULT_FORMATS);
        for (String f : formats.split(",")) {
            if (f.trim().equalsIgnoreCase(format)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the selected LaTeX template name.
     */
    public static String getLatexTemplate() {
        return System.getProperty(PROP_LATEX_TEMPLATE, DEFAULT_TEMPLATE);
    }
}
