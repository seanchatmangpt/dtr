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
package io.github.seanchatmangpt.dtr.testbrowser;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import java.io.IOException;
import io.github.seanchatmangpt.dtr.testbrowser.auth.AuthProvider;
import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.LoggerFactory;

/**
 * Fluent builder for HTTP requests with full support for all HTTP methods,
 * headers, payloads, form data, and file uploads.
 *
 * <p>Request objects are mutable by design and intended to be built via method
 * chaining. Each builder method returns {@code this} for fluent composition.</p>
 *
 * <p><strong>Basic Usage Patterns:</strong></p>
 * <pre>{@code
 * // Simple GET
 * Request.GET().url(Url.host("http://api.example.com").path("/users"))
 *
 * // POST with JSON
 * Request.POST()
 *     .url(testServerUrl().path("/api/users"))
 *     .contentTypeApplicationJson()
 *     .payload(new User("alice", "alice@example.com"))
 *
 * // PUT with multipart form + file upload
 * Request.PUT()
 *     .url(testServerUrl().path("/users/123"))
 *     .addFormParameter("name", "Alice")
 *     .addFileToUpload("avatar", new File("avatar.jpg"))
 *
 * // DELETE with custom headers
 * Request.DELETE()
 *     .url(testServerUrl().path("/users/123"))
 *     .addHeader("X-API-Key", "secret-key")
 *     .addHeader("X-Correlation-ID", "req-123")
 * }</pre>
 *
 * <p><strong>Payload Handling:</strong></p>
 * <ul>
 *   <li>String payloads are sent as-is</li>
 *   <li>Objects with Content-Type: application/json are serialized to JSON</li>
 *   <li>Objects with Content-Type: application/xml are serialized to XML</li>
 *   <li>For POST/PUT, if no Content-Type is set, defaults to application/json</li>
 * </ul>
 *
 * <p><strong>Form Parameters vs. Payloads:</strong></p>
 * <ul>
 *   <li>Form parameters: for application/x-www-form-urlencoded or multipart/form-data</li>
 *   <li>Payloads: for JSON, XML, or raw request bodies</li>
 *   <li>File uploads: use {@link #addFileToUpload(String, File)} with form parameters</li>
 * </ul>
 *
 * @author Raphael A. Bauer
 * @since 1.0.0
 * @see TestBrowser#makeRequest(Request)
 * @see Response for response handling
 */
public class Request {

    private final org.slf4j.Logger logger = LoggerFactory.getLogger(Response.class);

    /** HTTP method (GET, POST, PUT, PATCH, DELETE, HEAD). */
    public String httpRequestType;

    /** Target URI for the request. */
    public URI uri;

    /** Files to upload (multipart form-data). */
    public Map<String, File> filesToUpload;

    /** HTTP headers for this request. */
    public Map<String, String> headers;

    /** Form parameters (application/x-www-form-urlencoded or multipart). */
    public Map<String, String> formParameters;

    /** Request payload (JSON, XML, or raw body). */
    public Object payload;

    /** Whether to automatically follow HTTP redirects (3xx). */
    public boolean followRedirects;

    /**
     * I am private. Please use GET(), POST() and so on to get an instance of
     * this class.
     */
    private Request() {

        filesToUpload = null;
        headers = new HashMap<>();
        formParameters = null;
        followRedirects = true;

    }

    /**
     *
     * Get a request to perform a Http HEAD request via the TestBrowser.
     *
     * @return A request configured for a Http HEAD request.
     *
     */
    public static Request HEAD() {

        Request httpRequest = new Request();
        httpRequest.httpRequestType = HttpConstants.HEAD;

        return httpRequest;

    }

    /**
     *
     * Get a request to perform a Http GET request via the TestBrowser.
     *
     * @return A request configured for a Http GET request.
     *
     */
    public static Request GET() {

        Request httpRequest = new Request();
        httpRequest.httpRequestType = HttpConstants.GET;

        return httpRequest;

    }

    /**
     *
     * Get a request to perform a Http POST request via the TestBrowser.
     *
     * @return A request configured for a Http POST request.
     *
     */
    public static Request POST() {

        Request httpRequest = new Request();
        httpRequest.httpRequestType = HttpConstants.POST;

        return httpRequest;

    }

    /**
     *
     * Get a request to perform a Http PUT request via the TestBrowser.
     *
     * @return A request configured for a Http PUT request.
     *
     */
    public static Request PUT() {

        Request httpRequest = new Request();
        httpRequest.httpRequestType = HttpConstants.PUT;

        return httpRequest;

    }

    /**
     *
     * Get a request to perform a Http PATCH request via the TestBrowser.
     *
     * @return A request configured for a Http PATCH request.
     *
     */
    public static Request PATCH() {

        Request httpRequest = new Request();
        httpRequest.httpRequestType = HttpConstants.PATCH;

        return httpRequest;

    }

    /**
     *
     * Get a request to perform a Http DELETE request via the TestBrowser.
     *
     * @return A request configured for a DELETE request.
     *
     */
    public static Request DELETE() {

        Request httpRequest = new Request();
        httpRequest.httpRequestType = HttpConstants.DELETE;

        return httpRequest;

    }

    /**
     *
     * Set the Content-Type header to application/json; charset=utf-8.
     *
     * @return This request for joining.
     *
     */
    public Request contentTypeApplicationJson() {

        addHeader(
                HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.APPLICATION_JSON_WITH_CHARSET_UTF8);

        return this;

    }

    /**
     *
     * Set the Content-Type header to application/xml; charset=utf-8.
     *
     * @return This request for joining.
     *
     */
    public Request contentTypeApplicationXml() {

        addHeader(
                HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.APPLICATION_XML_WITH_CHARSET_UTF_8);

        return this;

    }

    /**
     *
     * Set the Uri of this request. Usually you may want to use testServerUrl()
     * from you Doctest and call toUri() at the end...
     *
     * @param url The Url of this request.
     * @return This Request for chaining.
     */
    public Request url(Url url) {
        if (url == null) {
            throw new NullPointerException("url must not be null");
        }
        this.uri = url.uri();
        return this;
    }

    /**
     *
     * Set the Uri of this request. Usually you may want to use testServerUrl()
     * from you Doctest and call toUri() at the end...
     *
     * @param uri The Uri of this request.
     * @return This Request for chaining.
     */
    public Request url(URI uri) {
        this.uri = uri;
        return this;
    }

    /**
     *
     * Add a file to be sent as multipart form post/put. Only makes sense for
     * POST and PUT Http requests.
     *
     * @param param The parameter for this file.
     * @param fileToUpload The file to upload
     * @return This request for chaining.
     */
    public Request addFileToUpload(String param, File fileToUpload) {
        if (filesToUpload == null) {
            filesToUpload = new HashMap<>();
        }
        filesToUpload.put(param, fileToUpload);

        return this;
    }

    /**
     *
     * Add an arbitrary header to this request.
     *
     * @param key The header key.
     * @param value The header value.
     * @return This request for chaining.
     */
    public Request addHeader(String key, String value) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, value);
        return this;
    }

    /**
     * Set headers for this request. This will wipe out all previously set
     * headers.
     *
     * @param headers A map of header keys and header values to use.
     * @return This request for chaining.
     */
    public Request headers(Map<String, String> headers) {
        this.headers = headers;
        return this;
    }

    /**
     *
     * Add an arbitrary form parameter to this request. Only makes sense for
     * POST or PUT requests.
     *
     * @param key The header key.
     * @param value The header value.
     * @return This request for chaining.
     */
    public Request addFormParameter(String key, String value) {
        if (formParameters == null) {
            formParameters = new HashMap<>();
        }
        formParameters.put(key, value);
        return this;
    }

    /**
     * Set form parameters for this request. Only makes sense for POST or PUT
     * requests. Wipes out all previously set formParameters.
     *
     * @param formParameters Map of formParemter keys and values to use.
     * @return This request for chaining.
     */
    public Request formParameters(Map<String, String> formParameters) {
        this.formParameters = formParameters;
        return this;
    }

    /**
     * Set the payload for this request. String will be just sent as strings. If
     * you set Content-Type json or xml this Object will converted into the
     * appropriate representation.
     *
     * Calling this method multiple times will overwrite the payload.
     *
     * @param payload The payload to use.
     * @return This request for chaining.
     */
    public Request payload(Object payload) {
        this.payload = payload;
        return this;
    }

    /**
     * Follow redirects automatically. Or simply do only one request and stop
     * then.
     *
     * @param followRedirects Whether to follow redirects or no.
     * @return This request for chaining.
     */
    public Request followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    /**
     * Applies authentication to this request using the specified auth provider.
     *
     * @param authProvider the authentication provider to apply
     * @return This request for chaining
     */
    public Request withAuth(AuthProvider authProvider) {
        if (authProvider != null) {
            authProvider.apply(this);
        }
        return this;
    }

    /**
     * Adds a query parameter to this request's URL.
     *
     * <p>The key and value are percent-encoded per RFC 3986 (spaces become {@code %20},
     * not {@code +}). Any query parameters already present in the URI are preserved.</p>
     *
     * @param key the parameter name (must not be null)
     * @param value the parameter value (must not be null)
     * @return This request for chaining
     */
    public Request addQueryParameter(String key, String value) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(value, "value must not be null");
        if (this.uri != null) {
            try {
                // URIBuilder preserves existing query parameters and handles encoding correctly.
                // Replace '+' with '%20' so that spaces satisfy RFC 3986 rather than
                // application/x-www-form-urlencoded conventions.
                String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8)
                        .replace("+", "%20");
                URIBuilder builder = new URIBuilder(this.uri);
                builder.addParameter(encodedKey, encodedValue);
                this.uri = builder.build();
            } catch (java.net.URISyntaxException e) {
                throw new IllegalStateException(
                        "Failed to add query parameter to URI: " + this.uri, e);
            }
        }
        return this;
    }

    /**
     *
     * @return The payload of this request as String. It tries to determine the
     * content and format the content in a pretty way. Currently works for json and xml;
     *
     */
    public String payloadAsPrettyString() {

        try {
            return PayloadUtils.prettyPrintRequestPayload(payload, headers);
        } catch (IOException ex) {
            logger.error("Something went wrong when pretty printing request payload: " + ex.toString());
            return "Error pretty printing the payload: \n" + ex.toString();
        }

    }

}
