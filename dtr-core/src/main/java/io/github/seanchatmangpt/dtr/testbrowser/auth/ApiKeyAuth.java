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
package io.github.seanchatmangpt.dtr.testbrowser.auth;

import io.github.seanchatmangpt.dtr.testbrowser.Request;

/**
 * API Key authentication provider.
 *
 * <p>Adds an API key either as a header or as a query parameter, depending
 * on the configured location.
 *
 * <p>Usage:
 * <pre>{@code
 * // Header-based API key
 * var headerAuth = new ApiKeyAuth("X-API-Key", "my-api-key", ApiKeyAuth.Location.HEADER);
 *
 * // Query parameter API key
 * var queryAuth = new ApiKeyAuth("api_key", "my-api-key", ApiKeyAuth.Location.QUERY_PARAM);
 *
 * // Apply to a request
 * var request = Request.GET()
 *     .url(url)
 *     .withAuth(headerAuth);
 * }</pre>
 *
 * @param key the name of the header or query parameter
 * @param value the API key value
 * @param location where to place the API key (header or query parameter)
 */
public record ApiKeyAuth(String key, String value, Location location) implements AuthProvider {

    /**
     * Location where the API key should be placed.
     */
    public enum Location {
        /**
         * Add the API key as an HTTP header.
         */
        HEADER,

        /**
         * Add the API key as a query parameter.
         */
        QUERY_PARAM
    }

    /**
     * Creates a new ApiKeyAuth with the specified parameters.
     *
     * @param key the name of the header or query parameter (must not be null or blank)
     * @param value the API key value (must not be null)
     * @param location where to place the API key (must not be null)
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if key is blank
     */
    public ApiKeyAuth {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("key must not be null or blank");
        }
        if (value == null) {
            throw new NullPointerException("value must not be null");
        }
        if (location == null) {
            throw new NullPointerException("location must not be null");
        }
    }

    @Override
    public void apply(Request request) {
        if (location == Location.HEADER) {
            request.addHeader(key, value);
        } else {
            request.addQueryParameter(key, value);
        }
    }
}
