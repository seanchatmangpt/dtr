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
 * Bearer token authentication provider for OAuth 2.0.
 *
 * <p>Adds an {@code Authorization: Bearer <token>} header to requests.
 * This is the standard authentication method for OAuth 2.0 protected APIs.
 *
 * <p>Usage:
 * <pre>{@code
 * var auth = new BearerTokenAuth("my-oauth-token");
 *
 * // Apply to a request
 * var request = Request.GET()
 *     .url(url)
 *     .withAuth(auth);
 *
 * // Or use directly
 * auth.apply(request);
 * }</pre>
 *
 * @param token the bearer token value (without the "Bearer " prefix)
 */
public record BearerTokenAuth(String token) implements AuthProvider {

    /**
     * Creates a new BearerTokenAuth with the specified token.
     *
     * @param token the bearer token value (must not be null)
     * @throws NullPointerException if token is null
     */
    public BearerTokenAuth {
        if (token == null) {
            throw new NullPointerException("token must not be null");
        }
    }

    @Override
    public void apply(Request request) {
        request.addHeader("Authorization", "Bearer " + token);
    }
}
