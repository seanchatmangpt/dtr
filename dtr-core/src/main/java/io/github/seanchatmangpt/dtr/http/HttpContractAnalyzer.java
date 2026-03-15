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
package io.github.seanchatmangpt.dtr.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Analyzes live HTTP endpoints and validates their contract against expected field shapes.
 *
 * <p>Makes a real HTTP GET request to a given URL, parses the JSON response, and checks
 * that each declared field exists and matches the expected JSON type. Latency is measured
 * with {@code System.nanoTime()} and reported in milliseconds. All results are returned
 * as a {@link ContractResult} record, which can be formatted to Markdown via
 * {@link #toMarkdown(ContractResult)}.</p>
 *
 * <p>Supported JSON type names (case-insensitive): {@code string}, {@code number},
 * {@code boolean}, {@code array}, {@code object}, {@code null}.</p>
 *
 * <p>Used by {@code sayHttpContract()} in {@code RenderMachineImpl}.</p>
 *
 * @since 2026.1.0
 */
public final class HttpContractAnalyzer {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    // Non-instantiable utility class
    private HttpContractAnalyzer() {}

    /**
     * The result of a single HTTP contract analysis run.
     *
     * @param statusCode  HTTP response status code, or -1 on connection failure
     * @param latencyMs   round-trip latency in milliseconds (measured with System.nanoTime())
     * @param passed      {@code true} when the request succeeded and all field checks passed
     * @param table       a 2D String table ready for {@code sayTable()}: first row is headers,
     *                    subsequent rows are one field-check per row
     * @param violations  human-readable description of every failed check; empty when passed
     */
    public record ContractResult(
            int statusCode,
            long latencyMs,
            boolean passed,
            String[][] table,
            List<String> violations) {}

    /**
     * Makes a live HTTP GET request to {@code url} and validates the JSON response
     * body against {@code expectedFields}.
     *
     * <p>Each row in {@code expectedFields} must be a two-element array
     * {@code [fieldName, expectedJsonType]}. The supported type names are:
     * {@code string}, {@code number}, {@code boolean}, {@code array}, {@code object},
     * {@code null}.</p>
     *
     * <p>On any network or parse error the method returns a {@link ContractResult}
     * with {@code statusCode=-1}, {@code passed=false}, and the error message as the
     * single violation — it never throws.</p>
     *
     * @param url            the HTTP endpoint to probe (must be an absolute URL)
     * @param expectedFields field contract: each row is {@code [fieldName, expectedType]}
     * @return a fully populated {@link ContractResult}
     */
    public static ContractResult analyze(String url, String[][] expectedFields) {
        long startNs = System.nanoTime();
        try {
            var client = HttpClient.newBuilder()
                    .connectTimeout(TIMEOUT)
                    .build();

            var request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .GET()
                    .header("Accept", "application/json")
                    .build();

            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000L;

            int status = response.statusCode();
            String body = response.body();

            // Parse JSON body
            JsonNode root;
            try {
                root = MAPPER.readTree(body);
            } catch (Exception parseEx) {
                var violation = "Response body is not valid JSON: " + parseEx.getMessage();
                return failResult(status, latencyMs, expectedFields, violation);
            }

            // Validate each expected field
            var violations = new ArrayList<String>();
            var tableRows = buildTableRows(root, expectedFields, violations);

            boolean passed = (status >= 200 && status < 300) && violations.isEmpty();
            return new ContractResult(status, latencyMs, passed, tableRows, List.copyOf(violations));

        } catch (Exception ex) {
            long latencyMs = (System.nanoTime() - startNs) / 1_000_000L;
            var violation = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            return failResult(-1, latencyMs, expectedFields, violation);
        }
    }

    /**
     * Formats a {@link ContractResult} as a list of Markdown lines suitable for
     * appending to a document or passing to a render machine.
     *
     * <p>The output includes a summary line (status code, latency, pass/fail) and
     * any recorded violations as a bullet list.</p>
     *
     * @param result the contract result to render
     * @return ordered list of Markdown lines (never null, may be empty only on null input)
     */
    public static List<String> toMarkdown(ContractResult result) {
        var lines = new ArrayList<String>();

        String statusLabel = result.statusCode() == -1 ? "N/A" : String.valueOf(result.statusCode());
        String passLabel   = result.passed() ? "PASS" : "FAIL";

        lines.add("**HTTP Contract Check** — status: `%s`, latency: `%dms`, result: `%s`"
                .formatted(statusLabel, result.latencyMs(), passLabel));

        if (!result.violations().isEmpty()) {
            lines.add("");
            lines.add("**Violations:**");
            for (var v : result.violations()) {
                lines.add("- " + v);
            }
        }

        return List.copyOf(lines);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Builds the 2D table of field check results. The header row is always
     * {@code ["Field", "Expected Type", "Actual Type", "Status"]}.
     * One data row is produced per entry in {@code expectedFields}.
     */
    private static String[][] buildTableRows(
            JsonNode root,
            String[][] expectedFields,
            List<String> violations) {

        // +1 for header row
        var rows = new String[expectedFields.length + 1][4];
        rows[0] = new String[]{"Field", "Expected Type", "Actual Type", "Status"};

        for (int i = 0; i < expectedFields.length; i++) {
            String fieldName     = expectedFields[i][0];
            String expectedType  = expectedFields[i][1].toLowerCase();

            JsonNode fieldNode = root.get(fieldName);

            if (fieldNode == null) {
                rows[i + 1] = new String[]{fieldName, expectedType, "missing", "FAIL"};
                violations.add("Field '%s' is missing from response".formatted(fieldName));
                continue;
            }

            String actualType = jsonTypeName(fieldNode);
            boolean typeMatch = actualType.equals(expectedType);

            rows[i + 1] = new String[]{
                fieldName,
                expectedType,
                actualType,
                typeMatch ? "PASS" : "FAIL"
            };

            if (!typeMatch) {
                violations.add(
                    "Field '%s': expected type '%s' but got '%s'"
                        .formatted(fieldName, expectedType, actualType));
            }
        }

        return rows;
    }

    /**
     * Maps a {@link JsonNode} to a normalized type name compatible with the
     * {@code expectedFields} contract strings.
     */
    private static String jsonTypeName(JsonNode node) {
        return switch (node.getNodeType()) {
            case STRING  -> "string";
            case NUMBER  -> "number";
            case BOOLEAN -> "boolean";
            case ARRAY   -> "array";
            case OBJECT  -> "object";
            case NULL    -> "null";
            default      -> node.getNodeType().name().toLowerCase();
        };
    }

    /**
     * Constructs a failed {@link ContractResult} when no valid JSON body is available
     * to check against. All expected fields are listed as "N/A" in the table.
     */
    private static ContractResult failResult(
            int statusCode,
            long latencyMs,
            String[][] expectedFields,
            String violation) {

        var rows = new String[expectedFields.length + 1][4];
        rows[0] = new String[]{"Field", "Expected Type", "Actual Type", "Status"};
        for (int i = 0; i < expectedFields.length; i++) {
            rows[i + 1] = new String[]{
                expectedFields[i][0],
                expectedFields[i][1],
                "N/A",
                "FAIL"
            };
        }
        return new ContractResult(statusCode, latencyMs, false, rows, List.of(violation));
    }
}
