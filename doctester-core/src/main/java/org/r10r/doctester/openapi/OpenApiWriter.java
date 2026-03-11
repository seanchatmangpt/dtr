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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

/**
 * Writes OpenAPI specifications to files in JSON or YAML format.
 *
 * <p>This writer produces OpenAPI 3.1 compliant output that can be imported
 * into Swagger UI, Postman, or other API documentation tools.
 */
public final class OpenApiWriter {

    private static final String BASE_DIR = "target/site/doctester";

    private OpenApiWriter() {
        // Utility class
    }

    /**
     * Writes the OpenAPI spec to a JSON file.
     *
     * @param spec the OpenAPI specification
     * @param fileName the output file name (without extension)
     * @return the output file
     */
    public static File writeJson(OpenApiSpec spec, String fileName) {
        return write(spec, fileName, OutputFormat.OPENAPI_JSON);
    }

    /**
     * Writes the OpenAPI spec to a YAML file.
     *
     * @param spec the OpenAPI specification
     * @param fileName the output file name (without extension)
     * @return the output file
     */
    public static File writeYaml(OpenApiSpec spec, String fileName) {
        return write(spec, fileName, OutputFormat.OPENAPI_YAML);
    }

    /**
     * Writes the OpenAPI spec to a file in the specified format.
     *
     * @param spec the OpenAPI specification
     * @param fileName the output file name (without extension)
     * @param format the output format
     * @return the output file
     */
    public static File write(OpenApiSpec spec, String fileName, OutputFormat format) {
        File outputFile = new File(BASE_DIR + File.separator + fileName + "." + format.getExtension());

        try {
            outputFile.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {

                String content = switch (format) {
                    case OPENAPI_JSON -> toJson(spec);
                    case OPENAPI_YAML -> toYaml(spec);
                    default -> throw new IllegalArgumentException("Unsupported format: " + format);
                };

                writer.write(content);
            }

            return outputFile;

        } catch (IOException e) {
            throw new RuntimeException("Failed to write OpenAPI spec to file: " + outputFile, e);
        }
    }

    /**
     * Converts the OpenAPI spec to JSON format.
     *
     * @param spec the OpenAPI specification
     * @return JSON string
     */
    public static String toJson(OpenApiSpec spec) {
        var sb = new StringBuilder();
        sb.append("{\n");

        // openapi version
        sb.append("  \"openapi\": \"").append(escapeJson(spec.openapi())).append("\",\n");

        // info
        sb.append("  \"info\": {\n");
        sb.append("    \"title\": \"").append(escapeJson(spec.info().title())).append("\",\n");
        sb.append("    \"version\": \"").append(escapeJson(spec.info().version())).append("\"");
        if (spec.info().description() != null) {
            sb.append(",\n    \"description\": \"").append(escapeJson(spec.info().description())).append("\"");
        }
        sb.append("\n  },\n");

        // paths
        sb.append("  \"paths\": {\n");
        var pathEntries = spec.paths().entrySet().stream().toList();
        for (int i = 0; i < pathEntries.size(); i++) {
            var entry = pathEntries.get(i);
            sb.append("    \"").append(escapeJson(entry.getKey())).append("\": {\n");
            sb.append(pathItemToJson(entry.getValue(), 6));
            sb.append(i < pathEntries.size() - 1 ? "    },\n" : "    }\n");
        }
        sb.append("  }\n");

        sb.append("}");
        return sb.toString();
    }

    private static String pathItemToJson(OpenApiSpec.PathItem pathItem, int indent) {
        var spaces = " ".repeat(indent);
        var sb = new StringBuilder();

        var opEntries = pathItem.operations().entrySet().stream().toList();
        for (int i = 0; i < opEntries.size(); i++) {
            var entry = opEntries.get(i);
            sb.append(spaces).append("\"").append(entry.getKey()).append("\": {\n");
            sb.append(operationToJson(entry.getValue(), indent + 2));
            sb.append(i < opEntries.size() - 1 ? spaces + "},\n" : spaces + "}\n");
        }

        return sb.toString();
    }

    private static String operationToJson(OpenApiSpec.Operation op, int indent) {
        var spaces = " ".repeat(indent);
        var sb = new StringBuilder();

        if (op.summary() != null) {
            sb.append(spaces).append("\"summary\": \"").append(escapeJson(op.summary())).append("\",\n");
        }
        if (op.description() != null) {
            sb.append(spaces).append("\"description\": \"").append(escapeJson(op.description())).append("\",\n");
        }

        sb.append(spaces).append("\"responses\": {\n");
        var respEntries = op.responses().entrySet().stream().toList();
        for (int i = 0; i < respEntries.size(); i++) {
            var entry = respEntries.get(i);
            sb.append(spaces).append("  \"").append(entry.getKey()).append("\": {\n");
            sb.append(responseToJson(entry.getValue(), indent + 4));
            sb.append(i < respEntries.size() - 1 ? spaces + "  },\n" : spaces + "  }\n");
        }
        sb.append(spaces).append("}\n");

        return sb.toString();
    }

    private static String responseToJson(OpenApiSpec.Response resp, int indent) {
        var spaces = " ".repeat(indent);
        var sb = new StringBuilder();

        sb.append(spaces).append("\"description\": \"").append(escapeJson(resp.description())).append("\"");
        if (resp.content() != null) {
            sb.append(",\n").append(spaces).append("\"content\": {\n");
            sb.append(spaces).append("  \"").append(escapeJson(resp.content().mediaType())).append("\": {\n");
            sb.append(spaces).append("    \"schema\": { \"type\": \"object\" }\n");
            sb.append(spaces).append("  }\n");
            sb.append(spaces).append("}");
        }
        sb.append("\n");

        return sb.toString();
    }

    /**
     * Converts the OpenAPI spec to YAML format.
     *
     * @param spec the OpenAPI specification
     * @return YAML string
     */
    public static String toYaml(OpenApiSpec spec) {
        var sb = new StringBuilder();

        sb.append("openapi: '").append(spec.openapi()).append("'\n");
        sb.append("info:\n");
        sb.append("  title: '").append(escapeYaml(spec.info().title())).append("'\n");
        sb.append("  version: '").append(escapeYaml(spec.info().version())).append("'\n");
        if (spec.info().description() != null) {
            sb.append("  description: '").append(escapeYaml(spec.info().description())).append("'\n");
        }

        sb.append("paths:\n");
        for (var entry : spec.paths().entrySet()) {
            sb.append("  '").append(escapeYaml(entry.getKey())).append("':\n");
            sb.append(pathItemToYaml(entry.getValue(), 4));
        }

        return sb.toString();
    }

    private static String pathItemToYaml(OpenApiSpec.PathItem pathItem, int indent) {
        var spaces = " ".repeat(indent);
        var sb = new StringBuilder();

        for (var entry : pathItem.operations().entrySet()) {
            sb.append(spaces).append(entry.getKey()).append(":\n");
            sb.append(operationToYaml(entry.getValue(), indent + 2));
        }

        return sb.toString();
    }

    private static String operationToYaml(OpenApiSpec.Operation op, int indent) {
        var spaces = " ".repeat(indent);
        var sb = new StringBuilder();

        if (op.summary() != null) {
            sb.append(spaces).append("summary: '").append(escapeYaml(op.summary())).append("'\n");
        }
        if (op.description() != null) {
            sb.append(spaces).append("description: '").append(escapeYaml(op.description())).append("'\n");
        }

        sb.append(spaces).append("responses:\n");
        for (var entry : op.responses().entrySet()) {
            sb.append(spaces).append("  '").append(entry.getKey()).append("':\n");
            sb.append(spaces).append("    description: '").append(escapeYaml(entry.getValue().description())).append("'\n");
        }

        return sb.toString();
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private static String escapeYaml(String s) {
        if (s == null) return "";
        return s.replace("'", "''");
    }
}
