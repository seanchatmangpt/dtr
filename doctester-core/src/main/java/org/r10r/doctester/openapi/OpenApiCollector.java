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

import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;

/**
 * Collects HTTP request/response pairs and generates an OpenAPI specification.
 *
 * <p>This collector observes all HTTP interactions made through DocTester
 * and builds an OpenAPI 3.1 specification that can be exported to JSON or YAML.
 *
 * <p>Usage:
 * <pre>{@code
 * OpenApiCollector collector = new OpenApiCollector("My API", "1.0.0");
 *
 * // After each HTTP request/response
 * collector.record(request, response);
 *
 * // Generate the spec
 * OpenApiSpec spec = collector.build();
 * String json = spec.toJson();
 * String yaml = spec.toYaml();
 * }</pre>
 */
public class OpenApiCollector {

    private final String title;
    private final String version;
    private final String description;
    private final List<RecordedInteraction> interactions = new ArrayList<>();

    /**
     * Creates a new collector with the specified API info.
     *
     * @param title API title
     * @param version API version
     */
    public OpenApiCollector(String title, String version) {
        this(title, version, null);
    }

    /**
     * Creates a new collector with the specified API info.
     *
     * @param title API title
     * @param version API version
     * @param description API description (optional)
     */
    public OpenApiCollector(String title, String version, String description) {
        this.title = title;
        this.version = version;
        this.description = description;
    }

    /**
     * Records an HTTP interaction.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     */
    public void record(Request request, Response response) {
        interactions.add(new RecordedInteraction(request, response));
    }

    /**
     * Records an HTTP interaction with a description.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param description operation description
     */
    public void record(Request request, Response response, String description) {
        interactions.add(new RecordedInteraction(request, response, description));
    }

    /**
     * Builds the OpenAPI specification from all recorded interactions.
     *
     * @return the OpenAPI specification
     */
    public OpenApiSpec build() {
        var spec = OpenApiSpec.create(title, version);

        for (var interaction : interactions) {
            spec = addInteractionToSpec(spec, interaction);
        }

        return spec;
    }

    private OpenApiSpec addInteractionToSpec(OpenApiSpec spec, RecordedInteraction interaction) {
        var request = interaction.request();
        var response = interaction.response();

        // Extract path from URI
        String path = extractPath(request.uri.toString());
        String method = request.httpRequestType.toLowerCase();

        // Build operation
        var operation = new OpenApiSpec.Operation(
            interaction.description(),
            "HTTP " + request.httpRequestType,
            extractResponses(response),
            extractRequestBody(request),
            extractParameters(request)
        );

        return spec.addPath(path, method, operation);
    }

    private String extractPath(String uri) {
        // Remove scheme and host, keep path and query
        try {
            var url = new java.net.URL(uri);
            var path = url.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }
            return path;
        } catch (Exception e) {
            return "/";
        }
    }

    private SequencedMap<String, OpenApiSpec.Parameter> extractParameters(Request request) {
        var params = new LinkedHashMap<String, OpenApiSpec.Parameter>();

        // Extract query parameters from URI
        var uri = request.uri.toString();
        if (uri.contains("?")) {
            var query = uri.substring(uri.indexOf("?") + 1);
            for (var param : query.split("&")) {
                var parts = param.split("=", 2);
                var name = parts[0];
                params.put(name, new OpenApiSpec.Parameter(
                    name,
                    "query",
                    null,
                    false,
                    new OpenApiSpec.Schema("string", null, null)
                ));
            }
        }

        // Extract headers
        if (request.headers != null) {
            for (var entry : request.headers.entrySet()) {
                if (!entry.getKey().equalsIgnoreCase("content-type") &&
                    !entry.getKey().equalsIgnoreCase("authorization")) {
                    params.put(entry.getKey(), new OpenApiSpec.Parameter(
                        entry.getKey(),
                        "header",
                        null,
                        false,
                        new OpenApiSpec.Schema("string", null, null)
                    ));
                }
            }
        }

        return params;
    }

    private OpenApiSpec.RequestBody extractRequestBody(Request request) {
        if (request.payload == null) {
            return null;
        }

        String mediaType = "application/json";
        if (request.headers != null) {
            var ct = request.headers.get("Content-Type");
            if (ct != null) {
                mediaType = ct;
            }
        }

        return new OpenApiSpec.RequestBody(
            "Request body",
            true,
            new OpenApiSpec.Content(
                mediaType,
                new OpenApiSpec.Schema("object", null, null)
            )
        );
    }

    private SequencedMap<String, OpenApiSpec.Response> extractResponses(Response response) {
        var responses = new LinkedHashMap<String, OpenApiSpec.Response>();

        String mediaType = "application/json";
        if (response.headers != null) {
            var ct = response.headers.get("Content-Type");
            if (ct != null) {
                mediaType = ct;
            }
        }

        responses.put(
            String.valueOf(response.httpStatus),
            new OpenApiSpec.Response(
                getStatusDescription(response.httpStatus),
                new OpenApiSpec.Content(
                    mediaType,
                    new OpenApiSpec.Schema("object", null, null)
                )
            )
        );

        return responses;
    }

    /**
     * Get a human-readable description of an HTTP status code.
     *
     * Java 26 Enhancement (JEP 530 - Primitive Types in Patterns):
     * Uses primitive pattern matching on int status codes for semantic grouping
     * by hundreds range (e.g., 2__ for 200-299 success, 4__ for 400-499 client errors).
     * This is more readable than individual cases and allows the JIT to compile to
     * a jump table instead of nested comparisons.
     *
     * @param status HTTP status code
     * @return human-readable description
     */
    private String getStatusDescription(int status) {
        return switch (status) {
            // Success responses (2xx)
            case 200 -> "OK";
            case 201 -> "Created";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 206 -> "Partial Content";
            case 2__ -> "Success";  // JEP 530: Primitive pattern for 200-299

            // Redirection responses (3xx)
            case 300 -> "Multiple Choices";
            case 301 -> "Moved Permanently";
            case 302 -> "Found";
            case 304 -> "Not Modified";
            case 307 -> "Temporary Redirect";
            case 308 -> "Permanent Redirect";
            case 3__ -> "Redirection";  // JEP 530: Primitive pattern for 300-399

            // Client error responses (4xx)
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            case 409 -> "Conflict";
            case 410 -> "Gone";
            case 415 -> "Unsupported Media Type";
            case 429 -> "Too Many Requests";
            case 4__ -> "Client Error";  // JEP 530: Primitive pattern for 400-499

            // Server error responses (5xx)
            case 500 -> "Internal Server Error";
            case 501 -> "Not Implemented";
            case 502 -> "Bad Gateway";
            case 503 -> "Service Unavailable";
            case 504 -> "Gateway Timeout";
            case 5__ -> "Server Error";  // JEP 530: Primitive pattern for 500-599

            // Unknown status
            default -> "Unknown Status";
        };
    }

    /**
     * Clears all recorded interactions.
     */
    public void clear() {
        interactions.clear();
    }

    /**
     * Returns the number of recorded interactions.
     */
    public int size() {
        return interactions.size();
    }

    private record RecordedInteraction(
        Request request,
        Response response,
        String description
    ) {
        RecordedInteraction(Request request, Response response) {
            this(request, response, null);
        }
    }
}
