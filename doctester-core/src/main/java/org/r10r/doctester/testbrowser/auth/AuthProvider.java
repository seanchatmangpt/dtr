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
 * <p>Built-in implementations:
 * <ul>
 *   <li>{@link BearerTokenAuth} - OAuth 2.0 Bearer token authentication</li>
 *   <li>{@link ApiKeyAuth} - API key authentication via header or query parameter</li>
 *   <li>{@link BasicAuth} - HTTP Basic authentication</li>
 * </ul>
 *
 * <p>Custom implementations can be created for application-specific auth schemes.
 *
 * @see BearerTokenAuth
 * @see ApiKeyAuth
 * @see BasicAuth
 */
@FunctionalInterface
public interface AuthProvider {

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
