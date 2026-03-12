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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central registry for all cross-references between DocTests.
 *
 * Manages:
 * - Recording references during test execution
 * - Resolving references to their labels after compilation
 * - Two-pass compilation with LaTeX \label{} and \ref{} commands
 * - Validation of all references before compilation
 *
 * Implements singleton pattern via getInstance() for global access.
 * Thread-safe for concurrent test execution.
 */
public class CrossReferenceIndex {

    private static final Logger logger = LoggerFactory.getLogger(CrossReferenceIndex.class);

    private static CrossReferenceIndex instance;

    private final List<DocTestRef> registeredReferences = new ArrayList<>();
    private final ReferenceResolver resolver = new ReferenceResolver();
    private boolean compiled = false;

    /**
     * Private constructor for singleton pattern.
     */
    private CrossReferenceIndex() {
    }

    /**
     * Get the singleton instance of CrossReferenceIndex.
     *
     * @return the global CrossReferenceIndex instance
     */
    public static synchronized CrossReferenceIndex getInstance() {
        if (instance == null) {
            instance = new CrossReferenceIndex();
        }
        return instance;
    }

    /**
     * Reset the singleton for testing purposes.
     */
    public static synchronized void reset() {
        instance = null;
    }

    /**
     * Register a cross-reference during test execution.
     *
     * Called by DTR.sayRef() to record each cross-reference made
     * during test execution.
     *
     * @param ref the reference to register
     */
    public synchronized void register(DocTestRef ref) {
        Objects.requireNonNull(ref, "Reference cannot be null");
        registeredReferences.add(ref);
        logger.trace("Registered reference: {} -> {}", ref.docTestClassName(), ref.anchor());
    }

    /**
     * Get all registered references.
     *
     * Returns an unmodifiable copy of the current references list.
     *
     * @return immutable list of all registered references
     */
    public synchronized List<DocTestRef> getReferences() {
        return List.copyOf(registeredReferences);
    }

    /**
     * Resolve a reference to its label after compilation.
     *
     * @param ref the reference to resolve
     * @return the resolved label (e.g., "Section 3.2")
     * @throws InvalidDocTestRefException if the DocTest class is not found
     * @throws InvalidAnchorException if the anchor is not found
     */
    public synchronized String resolve(DocTestRef ref) {
        if (!compiled) {
            logger.warn("Index not yet compiled; reference may not resolve: {}", ref);
        }
        return resolver.resolveLabel(ref);
    }

    /**
     * Generate a LaTeX \ref{} command for a cross-reference.
     *
     * @param ref the reference to generate a command for
     * @return LaTeX command like "\ref{section:user-creation}"
     * @throws InvalidDocTestRefException if the DocTest class is not found
     * @throws InvalidAnchorException if the anchor is not found
     */
    public synchronized String generateLatexRef(DocTestRef ref) {
        return resolver.generateRefCommand(ref);
    }

    /**
     * Build the cross-reference index from .tex files (first pass).
     *
     * Scans all .tex files to extract section-to-label mappings before
     * resolving cross-references. This is typically called during a
     * pre-compilation step.
     *
     * @param texFiles list of .tex file paths to scan
     * @param docTestClass the DocTest class that generated these files
     * @throws Exception if files cannot be read
     */
    public synchronized void buildIndex(List<Path> texFiles, Class<?> docTestClass) throws Exception {
        resolver.buildIndex(texFiles, docTestClass);
        compiled = true;
        logger.info("Built cross-reference index from {} .tex files", texFiles.size());
    }

    /**
     * Validate all registered references against the index.
     *
     * Called before LaTeX compilation to ensure all referenced sections exist.
     * Throws an exception on the first invalid reference found.
     *
     * @throws InvalidDocTestRefException if a DocTest class is not found
     * @throws InvalidAnchorException if an anchor is not found
     */
    public synchronized void validateReferences() {
        resolver.validateReferences(registeredReferences);
    }

    /**
     * Get the underlying ReferenceResolver for advanced operations.
     *
     * @return the ReferenceResolver instance
     */
    public ReferenceResolver getResolver() {
        return resolver;
    }

    /**
     * Get all anchors registered in the index.
     *
     * @return immutable map of anchor -> label
     */
    public java.util.Map<String, String> getAllAnchors() {
        return resolver.getAllAnchors();
    }

    /**
     * Check if the index has been compiled.
     *
     * @return true if buildIndex has been called successfully
     */
    public boolean isCompiled() {
        return compiled;
    }

    /**
     * Clear all registered references and reset the index.
     *
     * Useful for testing or when resetting between test runs.
     */
    public void clear() {
        registeredReferences.clear();
        compiled = false;
        logger.info("Cleared all registered references");
    }
}
