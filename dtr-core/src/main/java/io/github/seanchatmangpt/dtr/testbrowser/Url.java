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

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.hc.core5.net.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fluent builder for constructing HTTP URLs with a more intuitive API than JDK {@link java.net.URI}.
 *
 * <p>Provides method chaining for building URLs component-by-component: host, path, and query
 * parameters. Handles URL encoding and construction automatically.</p>
 *
 * <p><strong>Advantages over {@code java.net.URI}:</strong></p>
 * <ul>
 *   <li>Fluent API with method chaining: {@code Url.host(...).path(...).addQueryParameter(...)}</li>
 *   <li>Automatic handling of leading/trailing slashes</li>
 *   <li>Natural parameter building (not string parsing)</li>
 *   <li>Less exception-prone than URI constructor</li>
 * </ul>
 *
 * <p><strong>Usage Examples:</strong></p>
 * <pre>{@code
 * // Simple host + path
 * Url url1 = Url.host("http://api.example.com").path("/users");
 *
 * // With query parameters
 * Url url2 = Url.host("http://api.example.com")
 *     .path("/users")
 *     .addQueryParameter("limit", "10")
 *     .addQueryParameter("offset", "0");
 *
 * // Host with trailing slash and path without leading slash
 * Url url3 = Url.host("http://api.example.com/v1/")
 *     .path("users");  // Handles slash automatically
 *
 * // Convert to URI for use with HTTP clients
 * URI uri = url2.uri();  // Can pass to Request.url(URI)
 *
 * // String representation
 * String fullUrl = url2.toString();
 * }</pre>
 *
 * <p><strong>Slash Handling:</strong></p>
 * <p>The builder automatically handles leading/trailing slashes:
 * <ul>
 *   <li>Hosts always have trailing slash removed</li>
 *   <li>Paths always have leading slash added if missing</li>
 *   <li>Query parameters are properly URL-encoded</li>
 * </ul>
 *
 * @author Raphael A. Bauer
 * @since 1.0.0
 * @see Request for HTTP request building
 * @see java.net.URI for standard JDK URI class
 */
public class Url {

    private static Logger logger = LoggerFactory.getLogger(Url.class);

    /** URL builder accumulating scheme, host, and path components. */
    private StringBuilder simpleUrlBuilder;

    /** Query parameters to be appended to the URL. */
    private Map<String, String> queryParameters;

    /**
     * Private constructor. Use {@link #host(String)} to create instances.
     */
    private Url() {
        simpleUrlBuilder = new StringBuilder();
        queryParameters = new HashMap<>();

    }

    /**
     * Creates a new URL builder from a host specification.
     *
     * <p>The host parameter should be a complete scheme + host, optionally with port and
     * optional path prefix. Trailing slashes are automatically removed.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code Url.host("http://localhost:8080")} → can add paths</li>
     *   <li>{@code Url.host("https://api.example.com/v1/")} → trailing / removed</li>
     *   <li>{@code Url.host("http://192.168.1.1:3000")} → IP addresses work too</li>
     * </ul>
     *
     * @param host the host specification (scheme + host + optional port, e.g., "http://localhost:8080")
     * @return a new Url builder ready for path and parameter configuration
     * @see #path(String) to add the path component
     * @see #addQueryParameter(String, String) to add query parameters
     */
    public static Url host(String host) {

        Url url = new Url();

        String hostWithoutTrailingSlash;

        if (host.endsWith("/")) {
            hostWithoutTrailingSlash = host.substring(0, host.length() - 1);
        } else {
            hostWithoutTrailingSlash = host;
        }

        url.simpleUrlBuilder.append(hostWithoutTrailingSlash);

        return url;

    }

    /**
     * Appends a path component to this URL.
     *
     * <p>The path is automatically prefixed with a leading slash if missing.
     * Handles both {@code /my/path} and {@code my/path} equally.</p>
     *
     * <p>Examples:</p>
     * <ul>
     *   <li>{@code Url.host("http://api.example.com").path("/users")}</li>
     *   <li>{@code Url.host("http://api.example.com").path("users")}</li> (slash added automatically)
     *   <li>{@code Url.host("http://api.example.com").path("/users/123/profile")}</li>
     * </ul>
     *
     * @param path the path component, with or without leading slash (e.g., "/users" or "users")
     * @return this Url builder for method chaining
     * @see #addQueryParameter(String, String) to add query parameters
     */
    public Url path(String path) {

        String pathWithLeadingSlash;

        if (!path.startsWith("/")) {
            pathWithLeadingSlash = "/" + path;
        } else {
            pathWithLeadingSlash = path;
        }

        simpleUrlBuilder.append(pathWithLeadingSlash);

        return this;

    }

    /**
     * Adds a query parameter to this URL.
     *
     * <p>Multiple parameters can be chained: parameters are automatically URL-encoded
     * and separated by {@code &}. The leading {@code ?} is added by {@link #uri()}.</p>
     *
     * <p>Examples:</p>
     * <pre>{@code
     * Url url = Url.host("http://api.example.com")
     *     .path("/search")
     *     .addQueryParameter("q", "java")
     *     .addQueryParameter("limit", "10");
     * // Produces: http://api.example.com/search?q=java&limit=10
     * }</pre>
     *
     * @param key the parameter name (e.g., "q", "limit", "offset")
     * @param value the parameter value (automatically URL-encoded)
     * @return this Url builder for method chaining
     */
    public Url addQueryParameter(String key, String value) {

        queryParameters.put(key, value);

        return this;

    }

    /**
     * Converts this URL builder into a {@link java.net.URI} object.
     *
     * <p>Uses Apache {@code URIBuilder} to construct a properly encoded URI
     * from all accumulated components (host, path, query parameters).</p>
     *
     * @return a URI object suitable for use with HTTP clients (e.g., {@link Request})
     * @throws IllegalStateException if the URI cannot be constructed (syntax error)
     */
    public URI uri() {

        URI uri = null;

        try {

            // In HttpClient 5, URIBuilder constructor takes a String but may handle it differently
            // We create a base URI first, then build upon it
            URIBuilder uriBuilder = new URIBuilder(new URI(simpleUrlBuilder.toString()));

            for (Map.Entry<String, String> queryParameter : queryParameters.entrySet()) {

                uriBuilder.addParameter(queryParameter.getKey(), queryParameter.getValue());

            }

            uri = uriBuilder.build();

        } catch (URISyntaxException e) {

            String message = "Something strange happend when creating a URI from your Url (host, query parameters, path and so on)";
            logger.error(message);

            throw new IllegalStateException(message, e);
        }

        return uri;

    }

    /**
     * The real life Uri in human readable form.
     */
    @Override
    public String toString() {

        return uri().toString();

    }

}
