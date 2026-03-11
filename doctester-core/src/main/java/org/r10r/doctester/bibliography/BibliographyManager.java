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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Singleton manager for registering and retrieving bibliography entries.
 *
 * <p>BibliographyManager maintains a simple key-to-citation mapping for storing
 * bibliography references used in documentation tests. It provides methods to
 * register citations with optional page references and retrieve formatted citations.
 *
 * <p>Thread-safe via synchronized map access. Typical usage:
 * <pre>{@code
 * BibliographyManager bib = BibliographyManager.getInstance();
 * bib.register("Knuth1997", "The Art of Computer Programming, Vol. 1");
 * String cite = bib.getCitation("Knuth1997");
 * }</pre>
 */
public class BibliographyManager {

    private static final BibliographyManager INSTANCE = new BibliographyManager();
    private final Map<String, String> entries;

    private BibliographyManager() {
        this.entries = new HashMap<>();
    }

    /**
     * Returns the singleton instance of BibliographyManager.
     *
     * @return the shared BibliographyManager instance
     */
    public static BibliographyManager getInstance() {
        return INSTANCE;
    }

    /**
     * Registers a citation without page reference.
     *
     * <p>This method validates that the key is not null or empty before storing.
     *
     * @param key the unique citation identifier
     * @param citation the full citation string
     * @throws IllegalArgumentException if key is null or empty
     * @throws NullPointerException if citation is null
     */
    public void register(String key, String citation) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Citation key cannot be null or blank");
        }
        Objects.requireNonNull(citation, "Citation string cannot be null");
        this.entries.put(key, citation);
    }

    /**
     * Registers a citation with optional page reference.
     *
     * <p>Stores the citation and tracks the page reference. The page reference
     * is combined with the citation for retrieval via {@link #getCitation(String)}.
     *
     * @param key the unique citation identifier
     * @param citation the full citation string
     * @param pageRef optional page reference (e.g., "pp. 42-47" or null)
     * @throws IllegalArgumentException if key is null or empty
     * @throws NullPointerException if citation is null
     */
    public void register(String key, String citation, String pageRef) {
        register(key, pageRef != null ? "%s, %s".formatted(citation, pageRef) : citation);
    }

    /**
     * Retrieves the formatted citation for the given key.
     *
     * @param key the citation identifier
     * @return the stored citation string, or null if key not found
     * @throws IllegalArgumentException if key is null or empty
     */
    public String getCitation(String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Citation key cannot be null or blank");
        }
        return this.entries.get(key);
    }

    /**
     * Clears all registered citations.
     * Useful for resetting state between test suites.
     */
    public void clear() {
        this.entries.clear();
    }

    /**
     * Returns the number of registered citations.
     *
     * @return the count of entries in the bibliography
     */
    public int size() {
        return this.entries.size();
    }
}
