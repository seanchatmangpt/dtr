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

import org.junit.jupiter.api.Test;
import org.r10r.doctester.testbrowser.Request;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for authentication providers.
 */
class AuthProvidersTest {

    @Test
    void bearerTokenAuth_addsCorrectHeader() {
        var auth = new BearerTokenAuth("my-secret-token");
        var request = Request.GET();

        auth.apply(request);

        assertEquals("Bearer my-secret-token", request.headers.get("Authorization"));
    }

    @Test
    void bearerTokenAuth_requiresNonNullToken() {
        assertThrows(NullPointerException.class, () -> new BearerTokenAuth(null));
    }

    @Test
    void apiKeyAuth_inHeader_addsCorrectHeader() {
        var auth = new ApiKeyAuth("X-API-Key", "my-api-key", ApiKeyAuth.Location.HEADER);
        var request = Request.GET();

        auth.apply(request);

        assertEquals("my-api-key", request.headers.get("X-API-Key"));
    }

    @Test
    void apiKeyAuth_inQueryParam_addsQueryParameter() {
        var auth = new ApiKeyAuth("api_key", "my-api-key", ApiKeyAuth.Location.QUERY_PARAM);
        var request = Request.GET()
            .url(org.r10r.doctester.testbrowser.Url.host("http://localhost:8080").path("/test"));

        auth.apply(request);

        // The query parameter should be added to the URI
        assertTrue(request.uri.toString().contains("api_key=my-api-key"));
    }

    @Test
    void basicAuth_addsCorrectHeader() {
        var auth = new BasicAuth("user", "pass");
        var request = Request.GET();

        auth.apply(request);

        // "user:pass" in Base64 is "dXNlcjpwYXNz"
        assertEquals("Basic dXNlcjpwYXNz", request.headers.get("Authorization"));
    }

    @Test
    void basicAuth_requiresNonNullUsername() {
        assertThrows(NullPointerException.class, () -> new BasicAuth(null, "pass"));
    }

    @Test
    void basicAuth_requiresNonNullPassword() {
        assertThrows(NullPointerException.class, () -> new BasicAuth("user", null));
    }

    @Test
    void apiKeyAuth_requiresNonNullKey() {
        assertThrows(IllegalArgumentException.class, () ->
            new ApiKeyAuth(null, "value", ApiKeyAuth.Location.HEADER));
    }

    @Test
    void apiKeyAuth_requiresNonBlankKey() {
        assertThrows(IllegalArgumentException.class, () ->
            new ApiKeyAuth("", "value", ApiKeyAuth.Location.HEADER));
    }

    @Test
    void apiKeyAuth_requiresNonNullValue() {
        assertThrows(NullPointerException.class, () ->
            new ApiKeyAuth("key", null, ApiKeyAuth.Location.HEADER));
    }

    @Test
    void apiKeyAuth_requiresNonNullLocation() {
        assertThrows(NullPointerException.class, () ->
            new ApiKeyAuth("key", "value", null));
    }

    @Test
    void authProviders_canBeUsedWithRequest() {
        var request = Request.GET()
            .withAuth(new BearerTokenAuth("test-token"));

        assertEquals("Bearer test-token", request.headers.get("Authorization"));
    }
}
