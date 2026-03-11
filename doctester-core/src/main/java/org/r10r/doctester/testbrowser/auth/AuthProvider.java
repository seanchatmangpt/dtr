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
package org.r10r.doctester.testbrowser.auth;

import org.r10r.doctester.testbrowser.Request;

/**
 * Interface for authentication providers that can apply authentication to HTTP requests.
 *
 * <p>Implementations of this interface can add headers, query parameters, or other
 * authentication mechanisms to outgoing requests.
 *
 * Java 26 Enhancement (JEP 500 - Final Means Final):
 * This interface is sealed to only allow the specified implementations.
 * This enables the JVM to make static analysis guarantees:
 * - Devirtualization of authentication method calls
 * - Exhaustive pattern matching over sealed auth types
 * - Preparation for Valhalla value class flattening
 *
 * Permitted implementations:
 * - BasicAuth - HTTP Basic authentication (RFC 7617)
 * - BearerTokenAuth - OAuth 2.0 Bearer token authentication (RFC 6750)
 * - ApiKeyAuth - Custom API key headers or query parameters
 * - OAuth2TokenManager - OAuth2 token refresh and scope management
 * - SessionAwareAuthProvider - Automatic cookie jar and session management
 *
 * @see BearerTokenAuth
 * @see ApiKeyAuth
 * @see BasicAuth
 * @see OAuth2TokenManager
 * @see SessionAwareAuthProvider
 */
@FunctionalInterface
public sealed interface AuthProvider
    permits BasicAuth, BearerTokenAuth, ApiKeyAuth, OAuth2TokenManager, SessionAwareAuthProvider {

    /**
     * Applies authentication to the given request builder.
     *
     * <p>Implementations should modify the request to include the necessary
     * authentication credentials. This might involve adding headers,
     * query parameters, or modifying the request body.
     *
     * @param request the request to apply authentication to
     */
    void apply(Request request);
}
