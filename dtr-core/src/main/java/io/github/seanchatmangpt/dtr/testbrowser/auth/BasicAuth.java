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

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * HTTP Basic authentication provider.
 *
 * <p>Adds an {@code Authorization: Basic <credentials>} header to requests,
 * where credentials is the Base64 encoding of {@code username:password}.
 *
 * <p>Usage:
 * <pre>{@code
 * var auth = new BasicAuth("username", "password");
 *
 * // Apply to a request
 * var request = Request.GET()
 *     .url(url)
 *     .withAuth(auth);
 * }</pre>
 *
 * <p><strong>Security Note:</strong> Basic authentication sends credentials encoded
 * (not encrypted) with each request. Use only over HTTPS in production environments.
 *
 * @param username the username for authentication
 * @param password the password for authentication
 */
public record BasicAuth(String username, String password) implements AuthProvider {

    /**
     * Creates a new BasicAuth with the specified credentials.
     *
     * @param username the username (must not be null)
     * @param password the password (must not be null)
     * @throws NullPointerException if username or password is null
     */
    public BasicAuth {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(password, "password must not be null");
    }

    @Override
    public void apply(Request request) {
        String credentials = username + ":" + password;
        String encoded = Base64.getEncoder()
            .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        request.addHeader("Authorization", "Basic " + encoded);
    }
}
