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

import java.util.Optional;

/**
 * Immutable record representing a cross-reference to another DocTest's section.
 *
 * <p>Enables formal linking between DocTests with automatic section number resolution and
 * page references for LaTeX/PDF compilation. Cross-references are tracked in the
 * {@link CrossReferenceIndex} and resolved after document generation completes.</p>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * // Create a reference to a specific section in another DocTest
 * DocTestRef ref = DocTestRef.of(UserApiDocTest.class, "user-registration");
 *
 * // Render in documentation
 * ctx.sayRef(ref);
 *
 * // After LaTeX compilation, toString() returns resolved section number
 * // Before: "See UserApiDocTest#user-registration"
 * // After:  "Section 3.2"
 * }</pre>
 *
 * <p><strong>Rendering:</strong></p>
 * <ul>
 *   <li><strong>Markdown:</strong> renders as a markdown link to the target section</li>
 *   <li><strong>LaTeX:</strong> renders as {@code \ref{label}} command, resolved to section number after compilation</li>
 *   <li><strong>Other formats:</strong> delegates to the render machine implementation</li>
 * </ul>
 *
 * <p><strong>Reference Resolution:</strong></p>
 * <p>The {@code resolvedLabel} is initially empty. During LaTeX compilation (via
 * {@link io.github.seanchatmangpt.dtr.assembly.DocumentAssembler}), the label is populated
 * with the actual section number (e.g., "Section 3.2", "page 42").</p>
 *
 * @param docTestClass the target DocTest class (must not be null)
 * @param anchor the section/anchor name within that DocTest, e.g., "user-registration"
 *               (typically matches @DocSection annotation value with spaces-to-hyphens conversion)
 * @param resolvedLabel the resolved label after compilation (e.g., "Section 3.2"), initially empty
 * @see DocTester#sayRef(Class, String) for convenience method
 * @see CrossReferenceIndex for reference tracking and resolution
 * @since 1.0.0
 */
public record DocTestRef(
        Class<?> docTestClass,
        String anchor,
        Optional<String> resolvedLabel) {

    /**
     * Creates a new DocTestRef with the given DocTest class and anchor.
     *
     * <p>The resolved label starts as empty and is populated after LaTeX compilation.</p>
     *
     * @param docTestClass the target DocTest class (must not be null)
     * @param anchor the section/anchor name (e.g., "user-registration")
     * @return a new DocTestRef with empty resolvedLabel
     */
    public static DocTestRef of(Class<?> docTestClass, String anchor) {
        return new DocTestRef(docTestClass, anchor, Optional.empty());
    }

    /**
     * Returns the simple class name of the target DocTest.
     *
     * <p>Useful for constructing the reference in unresolved form.</p>
     *
     * @return the simple class name (e.g., "UserApiDocTest")
     */
    public String docTestClassName() {
        return docTestClass.getSimpleName();
    }

    /**
     * Returns a human-readable string representation of this reference.
     *
     * <p><strong>Behavior:</strong></p>
     * <ul>
     *   <li>If {@code resolvedLabel} is present: returns the resolved label
     *       (e.g., "Section 3.2", "page 42")</li>
     *   <li>If {@code resolvedLabel} is empty: returns the unresolved form
     *       (e.g., "See UserApiDocTest#user-registration")</li>
     * </ul>
     *
     * @return human-readable reference string
     */
    @Override
    public String toString() {
        if (resolvedLabel.isPresent()) {
            return resolvedLabel.get();
        }
        return "See %s#%s".formatted(docTestClassName(), anchor);
    }
}
