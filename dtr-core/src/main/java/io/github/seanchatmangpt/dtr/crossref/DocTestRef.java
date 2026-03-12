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
 * Immutable record representing a reference to another DocTest's section.
 *
 * Enables formal linking between DocTests with resolved section numbers and
 * page references for LaTeX/PDF compilation.
 *
 * @param docTestClass the target DocTest class
 * @param anchor the section/anchor name within that DocTest (e.g., "user-creation")
 * @param resolvedLabel resolved label after compilation (e.g., "Section 3.2")
 */
public record DocTestRef(
        Class<?> docTestClass,
        String anchor,
        Optional<String> resolvedLabel) {

    /**
     * Creates a new DocTestRef with the given DocTest class and anchor.
     * The resolved label is initially empty and populated after compilation.
     *
     * @param docTestClass the target DocTest class
     * @param anchor the section/anchor name
     * @return a new DocTestRef
     */
    public static DocTestRef of(Class<?> docTestClass, String anchor) {
        return new DocTestRef(docTestClass, anchor, Optional.empty());
    }

    /**
     * Returns the simple name of the target DocTest class.
     *
     * @return the simple class name (e.g., "ApiControllerDocTest")
     */
    public String docTestClassName() {
        return docTestClass.getSimpleName();
    }

    /**
     * Returns a human-readable string representation of this reference.
     *
     * After resolution, renders as "Section X.Y" from the resolved label.
     * Before resolution, renders as "See ApiControllerDocTest#user-creation".
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
