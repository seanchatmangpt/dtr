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
package org.r10r.doctester.openapi;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Represents an OpenAPI 3.1 specification being built from test execution.
 *
 * <p>This record collects HTTP request/response pairs encountered during test runs
 * and provides methods to generate valid OpenAPI YAML/JSON output.
 *
 * @param openapi the OpenAPI version (typically "3.1.0")
 * @param info the API info section
 * @param paths the collected API paths with their operations
 * @param components reusable components (schemas, security schemes, etc.)
 */
public record OpenApiSpec(
    String openapi,
    Info info,
    SequencedMap<String, PathItem> paths,
    SequencedMap<String, Object> components
) {

    /**
     * Creates a new OpenApiSpec with default OpenAPI 3.1.0 version.
     */
    public static OpenApiSpec create(String title, String version) {
        return new OpenApiSpec(
            "3.1.0",
            new Info(title, version, null),
            new LinkedHashMap<>(),
            new LinkedHashMap<>()
        );
    }

    /**
     * API info section.
     */
    public record Info(
        String title,
        String version,
        String description
    ) {}

    /**
     * Represents a path item with its HTTP operations.
     */
    public record PathItem(
        String summary,
        String description,
        SequencedMap<String, Operation> operations
    ) {
        public PathItem() {
            this(null, null, new LinkedHashMap<>());
        }

        public PathItem withOperation(String method, Operation operation) {
            var newOps = new LinkedHashMap<>(operations);
            newOps.put(method.toLowerCase(), operation);
            return new PathItem(summary, description, newOps);
        }
    }

    /**
     * Represents an HTTP operation.
     */
    public record Operation(
        String summary,
        String description,
        SequencedMap<String, Response> responses,
        RequestBody requestBody,
        SequencedMap<String, Parameter> parameters
    ) {
        public Operation() {
            this(null, null, new LinkedHashMap<>(), null, new LinkedHashMap<>());
        }

        public Operation withResponse(String status, Response response) {
            var newResponses = new LinkedHashMap<>(responses);
            newResponses.put(status, response);
            return new Operation(summary, description, newResponses, requestBody, parameters);
        }
    }

    /**
     * Represents a response definition.
     */
    public record Response(
        String description,
        Content content
    ) {}

    /**
     * Represents request body.
     */
    public record RequestBody(
        String description,
        boolean required,
        Content content
    ) {}

    /**
     * Represents content with media type and schema.
     */
    public record Content(
        String mediaType,
        Schema schema
    ) {}

    /**
     * Represents a schema definition.
     */
    public record Schema(
        String type,
        String description,
        SequencedMap<String, Schema> properties
    ) {}

    /**
     * Represents a parameter definition.
     */
    public record Parameter(
        String name,
        String in,
        String description,
        boolean required,
        Schema schema
    ) {}

    /**
     * Adds a path to this spec.
     */
    public OpenApiSpec addPath(String path, String method, Operation operation) {
        var newPaths = new LinkedHashMap<>(paths);
        var pathItem = newPaths.getOrDefault(path, new PathItem());
        newPaths.put(path, pathItem.withOperation(method, operation));
        return new OpenApiSpec(openapi, info, newPaths, components);
    }
}
