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
package io.github.seanchatmangpt.dtr.bibliography;

import io.github.seanchatmangpt.dtr.util.StringEscapeUtils;
import java.util.Collections;
import java.util.Map;

/**
 * Record representing a BibTeX entry.
 *
 * Supports formats: @article, @book, @inproceedings, @techreport.
 * Parses and extracts structured field data from .bib files.
 */
public record BibTeXEntry(
    String type,
    String key,
    Map<String, String> fields
) {

    /**
     * Creates a BibTeX entry with the given type, key, and fields.
     *
     * @param type the entry type (article, book, inproceedings, etc.)
     * @param key the citation key
     * @param fields a map of field names to values
     */
    public BibTeXEntry {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry type cannot be null or empty");
        }
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Entry key cannot be null or empty");
        }
        fields = Collections.unmodifiableMap(fields != null ? fields : Map.of());
    }

    /**
     * Retrieves a field value by name (case-insensitive).
     *
     * @param fieldName the field to retrieve
     * @return the field value, or empty string if not present
     */
    public String getField(String fieldName) {
        if (fieldName == null) {
            return "";  // hguard-ok: null guard — empty string is the correct contract for missing fields
        }
        for (var entry : fields.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(fieldName)) {
                return entry.getValue() != null ? entry.getValue() : "";  // hguard-ok: missing field value — empty string per BibTeX convention
            }
        }
        return "";  // hguard-ok: field not found — empty string per BibTeX convention (absent = empty)
    }

    /**
     * Renders the entry in standard BibTeX format.
     *
     * @return a formatted BibTeX entry string
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(type).append("{").append(key).append(",\n");

        fields.forEach((name, value) -> {
            if (value != null && !value.isEmpty()) {
                sb.append("  ").append(name).append(" = \"").append(StringEscapeUtils.escapeBibValue(value)).append("\",\n");
            }
        });

        sb.append("}\n");
        return sb.toString();
    }

    /**
     * Escapes special characters in BibTeX field values.
     *
     * @param value the raw value to escape
     * @return escaped value safe for BibTeX
     */
}
