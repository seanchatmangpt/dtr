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
package io.github.seanchatmangpt.dtr.crossref;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parses .tex files to extract sayNextSection calls and build mappings
 * between anchors and LaTeX labels.
 *
 * Supports two-pass LaTeX compilation:
 * - Pass 1: scan .tex files, assign \label{} to every sayNextSection
 * - Pass 2: resolve all \ref{} commands to corresponding labels
 *
 * Generates \ref{} LaTeX commands for cross-references and validates
 * that referenced sections exist before compilation.
 */
public class ReferenceResolver {

    private static final Logger logger = LoggerFactory.getLogger(ReferenceResolver.class);

    private static final Pattern SECTION_PATTERN = Pattern.compile(
            "\\\\section\\{([^}]+)\\}",
            Pattern.DOTALL);

    private static final Pattern LABEL_PATTERN = Pattern.compile(
            "\\\\label\\{([^}]+)\\}",
            Pattern.DOTALL);

    private final Map<Class<?>, Map<String, String>> sectionLabels = new HashMap<>();
    private final Map<String, String> anchorToLabel = new HashMap<>();
    private final Map<String, Class<?>> anchorToDocTest = new HashMap<>();

    /**
     * Parse a single .tex file and extract section-to-label mappings.
     *
     * Searches for patterns like:
     * \section{User Creation}
     * \label{section:user-creation}
     *
     * @param texFile path to the .tex file
     * @param docTestClass the DocTest class that generated this file
     * @throws Exception if the file cannot be read
     */
    public void parseTexFile(Path texFile, Class<?> docTestClass) throws Exception {
        if (!Files.exists(texFile)) {
            logger.warn("TeX file not found: {}", texFile);
            return;
        }

        String content = Files.readString(texFile);
        Map<String, String> classLabels = new HashMap<>();

        Matcher sectionMatcher = SECTION_PATTERN.matcher(content);
        while (sectionMatcher.find()) {
            String sectionName = sectionMatcher.group(1);
            String anchor = convertTextToAnchor(sectionName);

            // Look for label immediately after section
            int sectionEnd = sectionMatcher.end();
            int searchEnd = Math.min(sectionEnd + 500, content.length());
            String afterSection = content.substring(sectionEnd, searchEnd);

            Matcher labelMatcher = LABEL_PATTERN.matcher(afterSection);
            if (labelMatcher.find()) {
                String label = labelMatcher.group(1);
                classLabels.put(anchor, label);
                anchorToLabel.put(anchor, label);
                anchorToDocTest.put(anchor, docTestClass);
            }
        }

        sectionLabels.put(docTestClass, classLabels);
        logger.debug("Parsed {} sections from {}", classLabels.size(), texFile);
    }

    /**
     * Build an index by scanning all .tex files in the given paths.
     *
     * Called as first pass of compilation to map all sayNextSection calls
     * to their corresponding \label{} commands.
     *
     * @param texFiles list of .tex file paths to scan
     * @param docTestClass the DocTest class corresponding to these files
     * @throws Exception if files cannot be read
     */
    public void buildIndex(List<Path> texFiles, Class<?> docTestClass) throws Exception {
        for (Path file : texFiles) {
            parseTexFile(file, docTestClass);
        }
    }

    /**
     * Resolve a DocTestRef to its LaTeX label.
     *
     * Returns the actual \label{} identifier that will be used in the compiled PDF.
     *
     * @param ref the reference to resolve
     * @return the LaTeX label (e.g., "section:user-creation")
     * @throws InvalidDocTestRefException if the DocTest class is not found
     * @throws InvalidAnchorException if the anchor is not found in the target DocTest
     */
    public String resolveLabel(DocTestRef ref) {
        Class<?> targetClass = ref.docTestClass();
        String anchor = ref.anchor();

        Map<String, String> labels = sectionLabels.get(targetClass);
        if (labels == null) {
            throw new InvalidDocTestRefException(
                    "DocTest class not found in index: " + targetClass.getName());
        }

        String label = labels.get(anchor);
        if (label == null) {
            throw new InvalidAnchorException(
                    "Anchor not found in " + targetClass.getSimpleName() + ": " + anchor);
        }

        return label;
    }

    /**
     * Generate a LaTeX \ref{} command for a resolved reference.
     *
     * The returned string is a complete LaTeX command that can be embedded
     * in the document. It references the label that will be resolved during
     * two-pass compilation.
     *
     * @param ref the reference to generate a \ref{} for
     * @return LaTeX command like "\ref{section:user-creation}"
     * @throws InvalidDocTestRefException if the DocTest class is not found
     * @throws InvalidAnchorException if the anchor is not found
     */
    public String generateRefCommand(DocTestRef ref) {
        String label = resolveLabel(ref);
        return "\\ref{%s}".formatted(label);
    }

    /**
     * Validate that all registered references target valid DocTests and anchors.
     *
     * Called before compilation to catch invalid references early.
     *
     * @param refs the references to validate
     * @throws InvalidDocTestRefException if a DocTest class is not found
     * @throws InvalidAnchorException if an anchor is not found
     */
    public void validateReferences(List<DocTestRef> refs) {
        for (DocTestRef ref : refs) {
            try {
                resolveLabel(ref);
            } catch (InvalidDocTestRefException | InvalidAnchorException e) {
                logger.error("Invalid reference: {} -> {}", ref, e.getMessage());
                throw e;
            }
        }
        logger.info("Validated {} cross-references successfully", refs.size());
    }

    /**
     * Convert a section title to an anchor slug for use in markdown/LaTeX.
     *
     * Example: "User Creation" -> "user-creation"
     *
     * @param text the section title
     * @return the anchor slug
     */
    private String convertTextToAnchor(String text) {
        return text.toLowerCase()
                .replaceAll("\\s+", "-")
                .replaceAll("[^a-z0-9-]", "")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    /**
     * Get all registered section labels for a DocTest class.
     *
     * @param docTestClass the DocTest class
     * @return map of anchor -> label
     */
    public Map<String, String> getLabelsForClass(Class<?> docTestClass) {
        return sectionLabels.getOrDefault(docTestClass, Map.of());
    }

    /**
     * Get all anchors registered in the index.
     *
     * @return map of anchor -> label
     */
    public Map<String, String> getAllAnchors() {
        return Map.copyOf(anchorToLabel);
    }
}
