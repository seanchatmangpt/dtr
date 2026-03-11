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
package org.r10r.doctester.bibliography;

/**
 * Immutable record representing a citation key with optional page reference.
 *
 * <p>CitationKey is a record that encapsulates a bibliography entry identifier
 * and an optional page reference for pinpointing specific content within a source.
 *
 * <p>Example:
 * <pre>{@code
 * var citation = new CitationKey("Knuth1997", "pp. 42-47");
 * var simpleCitation = new CitationKey("Smith2020", null);
 * }</pre>
 */
public record CitationKey(String key, String pageRef) {

    /**
     * Compact constructor for CitationKey validation.
     * Validates that key is not null or empty.
     *
     * @throws IllegalArgumentException if key is null or empty
     */
    public CitationKey {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Citation key cannot be null or blank");
        }
    }
}
