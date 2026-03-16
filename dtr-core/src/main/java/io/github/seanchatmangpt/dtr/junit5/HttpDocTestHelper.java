/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.seanchatmangpt.dtr.junit5;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Fluent helper for HTTP API documentation testing.
 * Reduces boilerplate when documenting HTTP interactions.
 *
 * <p>Designed for DTR-based API documentation tests where you need to:</p>
 * <ul>
 *   <li>Make HTTP requests (GET, POST, PUT, DELETE, PATCH)</li>
 *   <li>Assert on response status, headers, and body</li>
 *   <li>Document the request/response cycle in generated markdown</li>
 * </ul>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @ExtendWith(DtrExtension.class)
 * class UserApiDocTest {
 *
 *     @Test
 *     void testGetUsers(DtrContext ctx) throws Exception {
 *         var helper = new HttpDocTestHelper(ctx);
 *
 *         helper.get("https://api.example.com/users")
 *               .expectStatus(200)
 *               .documentResponse()
 *               .documentJson("User List Response");
 *     }
 *
 *     @Test
 *     void testCreateUser(DtrContext ctx) throws Exception {
 *         var helper = new HttpDocTestHelper(ctx);
 *
 *         String requestBody = """
 *             {
 *               "name": "Alice",
 *               "email": "alice@example.com"
 *             }
 *             """;
 *
 *         helper.post("https://api.example.com/users")
 *               .header("Content-Type", "application/json")
 *               .body(requestBody)
 *               .send()
 *               .expectStatus(201)
 *               .documentResponse();
 *     }
 * }
 * }</pre>
 *
 * <p>The fluent API supports method chaining for readable test code:</p>
 * <ul>
 *   <li><strong>Request phase:</strong> {@code get/post/put/delete/patch} → {@code header} → {@code body} → {@code send}</li>
 *   <li><strong>Response phase:</strong> {@code expectStatus} → {@code documentResponse} → {@code documentJson}</li>
 * </ul>
 *
 * <p>For advanced scenarios, you can access the underlying {@link HttpClient}
 * via {@link #getClient()} or provide a custom client via {@link #HttpDocTestHelper(DtrContext, HttpClient)}.</p>
 *
 * @see DtrContext
 * @see HttpClient
 */
public class HttpDocTestHelper {

    private final DtrContext ctx;
    private final HttpClient client;

    /**
     * Creates a new HttpDocTestHelper with a default HTTP client.
     *
     * <p>The default client is configured with:</p>
 *     <ul>
     *       <li>Connect timeout: 10 seconds</li>
     *       <li>Follow redirects: enabled (default HttpClient behavior)</li>
     *       <li>Cookie handling: enabled (default HttpClient behavior)</li>
     *     </ul>
     *
     * @param ctx the DTR context for documentation output
     */
    public HttpDocTestHelper(DtrContext ctx) {
        this.ctx = ctx;
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Creates a new HttpDocTestHelper with a custom HTTP client.
     *
     * <p>Use this constructor when you need custom client configuration:</p>
     * <ul>
     *   <li>Custom timeouts</li>
     *   <li>Custom executors (e.g., for async testing)</li>
     *   <li>Custom authenticators</li>
     *   <li>Custom proxy settings</li>
     *   <li>Disable redirect following</li>
     * </ul>
     *
     * <p>Example:</p>
     * <pre>{@code
     * HttpClient customClient = HttpClient.newBuilder()
     *     .connectTimeout(Duration.ofSeconds(30))
     *     .followRedirects(HttpClient.Redirect.NEVER)
     *     .build();
     *
     * var helper = new HttpDocTestHelper(ctx, customClient);
     * }</pre>
     *
     * @param ctx the DTR context for documentation output
     * @param client a custom HTTP client (must not be null)
     * @throws NullPointerException if client is null
     */
    public HttpDocTestHelper(DtrContext ctx, HttpClient client) {
        this.ctx = ctx;
        this.client = java.util.Objects.requireNonNull(client, "HttpClient must not be null");
    }

    /**
     * Starts building a GET request.
     *
     * @param url the target URL
     * @return a RequestBuilder for fluent configuration
     */
    public RequestBuilder get(String url) {
        return new RequestBuilder(ctx, client, "GET", url);
    }

    /**
     * Starts building a POST request.
     *
     * @param url the target URL
     * @return a RequestBuilder for fluent configuration
     */
    public RequestBuilder post(String url) {
        return new RequestBuilder(ctx, client, "POST", url);
    }

    /**
     * Starts building a PUT request.
     *
     * @param url the target URL
     * @return a RequestBuilder for fluent configuration
     */
    public RequestBuilder put(String url) {
        return new RequestBuilder(ctx, client, "PUT", url);
    }

    /**
     * Starts building a DELETE request.
     *
     * @param url the target URL
     * @return a RequestBuilder for fluent configuration
     */
    public RequestBuilder delete(String url) {
        return new RequestBuilder(ctx, client, "DELETE", url);
    }

    /**
     * Starts building a PATCH request.
     *
     * @param url the target URL
     * @return a RequestBuilder for fluent configuration
     */
    public RequestBuilder patch(String url) {
        return new RequestBuilder(ctx, client, "PATCH", url);
    }

    /**
     * Gets the underlying HTTP client.
     *
     * <p>Use this for advanced scenarios not covered by the fluent API,
     * such as making requests outside the RequestBuilder flow.</p>
     *
     * @return the HttpClient used by this helper
     */
    public HttpClient getClient() {
        return client;
    }

    /**
     * Fluent builder for HTTP requests.
     *
     * <p>Provides a chainable API for configuring request parameters
     * before sending. All configuration methods return {@code this}
     * for method chaining.</p>
     *
     * <p>Typical usage:</p>
     * <pre>{@code
     * helper.post("https://api.example.com/users")
     *       .header("Content-Type", "application/json")
     *       .header("Authorization", "Bearer token123")
     *       .body("{\"name\":\"Alice\"}")
     *       .send()
     *       .expectStatus(201);
     * }</pre>
     */
    public static class RequestBuilder {
        private final DtrContext ctx;
        private final HttpClient client;
        private final String method;
        private final String url;
        private final Map<String, String> headers;
        private final String body;

        RequestBuilder(DtrContext ctx, HttpClient client, String method, String url) {
            this.ctx = ctx;
            this.client = client;
            this.method = method;
            this.url = url;
            this.headers = Map.of();
            this.body = null;
        }

        private RequestBuilder(DtrContext ctx, HttpClient client, String method, String url,
                             Map<String, String> headers, String body) {
            this.ctx = ctx;
            this.client = client;
            this.method = method;
            this.url = url;
            this.headers = headers;
            this.body = body;
        }

        /**
         * Adds a header to the request.
         *
         * <p>Can be called multiple times to add multiple headers.
         * If the same header name is used twice, the last value wins.</p>
         *
         * @param name the header name (case-insensitive per HTTP spec)
         * @param value the header value
         * @return a new RequestBuilder with the header added
         */
        public RequestBuilder header(String name, String value) {
            var newHeaders = new HashMap<>(headers);
            newHeaders.put(name, value);
            return new RequestBuilder(ctx, client, method, url, newHeaders, body);
        }

        /**
         * Adds multiple headers to the request.
         *
         * <p>All headers in the map are added to the request.
         * Existing headers with the same names are overwritten.</p>
         *
         * @param headers map of header names to values
         * @return a new RequestBuilder with the headers added
         */
        public RequestBuilder headers(Map<String, String> headers) {
            var newHeaders = new HashMap<>(this.headers);
            newHeaders.putAll(headers);
            return new RequestBuilder(ctx, client, method, url, newHeaders, body);
        }

        /**
         * Sets the request body.
         *
         * <p>For methods that don't typically send a body (GET, HEAD),
         * the body is silently ignored by the HttpClient.</p>
         *
         * @param body the request body as a string (e.g., JSON, XML, plain text)
         * @return a new RequestBuilder with the body set
         */
        public RequestBuilder body(String body) {
            return new RequestBuilder(ctx, client, method, url, headers, body);
        }

        /**
         * Sends the HTTP request and returns a ResponseBuilder for assertions.
         *
         * <p>This is the terminal method in the RequestBuilder chain.
         * After calling {@code send()}, you can chain ResponseBuilder methods:</p>
         * <pre>{@code
         * helper.get("https://api.example.com/users")
         *       .send()
         *       .expectStatus(200)
         *       .documentResponse();
         * }</pre>
         *
         * @return a ResponseBuilder for asserting and documenting the response
         * @throws Exception if the request fails (network error, invalid URL, timeout)
         */
        public ResponseBuilder send() throws Exception {
            var builder = HttpRequest.newBuilder()
                .uri(new URI(url))
                .timeout(Duration.ofSeconds(10));

            headers.forEach(builder::header);

            if (body != null) {
                builder.method(method, HttpRequest.BodyPublishers.ofString(body));
            } else if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
                // GET and HEAD should not have a body
                builder.GET();
            } else {
                // For POST, PUT, DELETE, PATCH without explicit body, send no body
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            var response = client.send(builder.build(),
                HttpResponse.BodyHandlers.ofString());

            return new ResponseBuilder(ctx, response);
        }
    }

    /**
     * Fluent builder for HTTP response assertions and documentation.
     *
     * <p>Provides methods for asserting on response properties and
     * documenting them in the generated DTR output. All assertion methods
     * return {@code this} for method chaining.</p>
     *
     * <p>Typical usage:</p>
     * <pre>{@code
     * helper.get("https://api.example.com/users")
     *       .send()
     *       .expectStatus(200)
     *       .expectHeader("Content-Type", "application/json")
     *       .documentResponse()
     *       .documentJson("Users");
     * }</pre>
     */
    public static class ResponseBuilder {
        private final DtrContext ctx;
        private final HttpResponse<String> response;

        ResponseBuilder(DtrContext ctx, HttpResponse<String> response) {
            this.ctx = ctx;
            this.response = response;
        }

        /**
         * Asserts that the response status code matches the expected value.
         *
         * <p>The assertion is documented as a passing check in the DTR output.</p>
         *
         * @param expected the expected HTTP status code (e.g., 200, 404, 500)
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder expectStatus(int expected) {
            assertThat("HTTP Status", response.statusCode(), is(expected));
            ctx.sayKeyValue(Map.of(
                "HTTP Status", String.valueOf(response.statusCode())
            ));
            return this;
        }

        /**
         * Asserts that a response header has the expected value.
         *
         * <p>If the header is missing, the assertion fails with a clear message.
         * The assertion is documented in the DTR output.</p>
         *
         * @param name the header name (case-insensitive per HTTP spec)
         * @param expectedValue the expected header value
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder expectHeader(String name, String expectedValue) {
            String actualValue = response.headers()
                .firstValue(name)
                .orElse(null);

            assertThat("Header " + name, actualValue, is(expectedValue));
            return this;
        }

        /**
         * Asserts that a response header is present.
         *
         * <p>Use this when you only care that a header exists, not its value.</p>
         *
         * @param name the header name (case-insensitive per HTTP spec)
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder expectHeaderPresent(String name) {
            boolean present = response.headers()
                .firstValue(name)
                .isPresent();

            assertThat("Header " + name + " is present", present, is(true));
            return this;
        }

        /**
         * Asserts that the response body contains the expected substring.
         *
         * <p>This is a simple substring check, not a full JSON/XML validation.
         * For structured validation, extract the body with {@link #getBody()}
         * and use a JSON parser.</p>
         *
         * @param substring the expected substring (case-sensitive)
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder expectBodyContains(String substring) {
            String body = response.body();
            assertThat("Response body contains '" + substring + "'",
                body.contains(substring), is(true));
            return this;
        }

        /**
         * Asserts that the response body matches the expected value exactly.
         *
         * <p>Use this for APIs that return plain text responses.
         * For JSON responses, prefer a JSON comparison library.</p>
         *
         * @param expectedBody the expected response body (exact match)
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder expectBody(String expectedBody) {
            assertThat("Response body", response.body(), is(expectedBody));
            return this;
        }

        /**
         * Documents the response metadata as a key-value table.
         *
         * <p>Documents the following properties:</p>
         * <ul>
         *   <li>HTTP Status (e.g., "200")</li>
         *   <li>Content-Type (if present)</li>
         * </ul>
         *
         * <p>Example output:</p>
         * <pre>{@code
         * | HTTP Status | Content-Type |
         * |-------------|--------------|
         * | 200         | application/json |
         * }</pre>
         *
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder documentResponse() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("HTTP Status", String.valueOf(response.statusCode()));

            response.headers()
                .firstValue("Content-Type")
                .ifPresent(ct -> metadata.put("Content-Type", ct));

            ctx.sayKeyValue(metadata);
            return this;
        }

        /**
         * Documents the response body as a code block with the given label.
         *
         * <p>Use this for JSON, XML, or plain text responses. The content
         * is rendered as a fenced code block in the generated markdown.</p>
         *
         * <p>Example output:</p>
         * <pre>{@code
         * ### User List Response
         *
         * ```json
         * {
         *   "users": [...]
         * }
         * ```
         * }</pre>
         *
         * @param label the section headline (e.g., "User List Response")
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder documentJson(String label) {
            ctx.sayNextSection(label);
            ctx.sayCode(response.body(), "json");
            return this;
        }

        /**
         * Documents the response body as a code block with a custom language.
         *
         * <p>Use this for non-JSON responses (XML, HTML, plain text, etc.).
         * The language parameter controls syntax highlighting in the
         * generated markdown.</p>
         *
         * @param label the section headline
         * @param language the language identifier (e.g., "xml", "html", "text")
         * @return this ResponseBuilder for chaining
         */
        public ResponseBuilder documentBody(String label, String language) {
            ctx.sayNextSection(label);
            ctx.sayCode(response.body(), language);
            return this;
        }

        /**
         * Gets the raw response body as a string.
         *
         * <p>Use this for custom assertions or parsing:</p>
         * <pre>{@code
         * String json = helper.get(url).send().getBody();
         * JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
         * assertThat(parsed.get("count").getAsInt(), is(42));
         * }</pre>
         *
         * @return the response body as a string
         */
        public String getBody() {
            return response.body();
        }

        /**
         * Gets the underlying HttpResponse object.
         *
         * <p>Use this for advanced scenarios not covered by the fluent API,
         * such as accessing all headers, status code, or the request that
         * generated this response.</p>
         *
         * @return the HttpResponse object
         */
        public HttpResponse<String> getResponse() {
            return response;
        }

        /**
         * Gets the response status code.
         *
         * <p>Convenience method equivalent to {@code getResponse().statusCode()}.</p>
         *
         * @return the HTTP status code
         */
        public int getStatusCode() {
            return response.statusCode();
        }

        /**
         * Gets a response header value.
         *
         * <p>Convenience method for header access.</p>
         *
         * @param name the header name
         * @return the header value, or null if not present
         */
        public String getHeader(String name) {
            return response.headers()
                .firstValue(name)
                .orElse(null);
        }
    }
}
