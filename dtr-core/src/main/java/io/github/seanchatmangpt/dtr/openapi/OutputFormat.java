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
package io.github.seanchatmangpt.dtr.openapi;

/**
 * Supported output formats for DocTester documentation.
 *
 * <p>Each format produces a different type of output artifact:
 * <ul>
 *   <li>{@link #OPENAPI_JSON} - OpenAPI 3.1 specification in JSON format</li>
 *   <li>{@link #OPENAPI_YAML} - OpenAPI 3.1 specification in YAML format</li>
 *   <li>{@link #MARKDOWN} - GitHub-flavored Markdown documentation (default)</li>
 * </ul>
 */
public enum OutputFormat {

    /**
     * OpenAPI 3.1 specification in JSON format.
     * Output file: {@code target/site/dtr/openapi.json}
     */
    OPENAPI_JSON("json"),

    /**
     * OpenAPI 3.1 specification in YAML format.
     * Output file: {@code target/site/dtr/openapi.yaml}
     */
    OPENAPI_YAML("yaml"),

    /**
     * GitHub-flavored Markdown documentation.
     * Output file: {@code target/docs/<TestClass>.md}
     */
    MARKDOWN("md");

    private final String extension;

    OutputFormat(String extension) {
        this.extension = extension;
    }

    /**
     * Returns the file extension for this output format.
     *
     * @return file extension without leading dot
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns the content type for this output format.
     *
     * @return MIME content type
     */
    public String getContentType() {
        return switch (this) {
            case OPENAPI_JSON -> "application/json";
            case OPENAPI_YAML -> "application/x-yaml";
            case MARKDOWN -> "text/markdown";
        };
    }
}
