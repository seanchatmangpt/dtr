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

import com.fasterxml.jackson.core.type.TypeReference;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.testbrowser.Url;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * OAuth 2.0 token manager that handles automatic token acquisition and refresh.
 *
 * <p>This provider manages OAuth 2.0 access tokens, including:
 * <ul>
 *   <li>Initial token acquisition via client credentials flow</li>
 *   <li>Automatic token refresh when expired</li>
 *   <li>Thread-safe token management</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * var oauth = new OAuth2TokenManager(
 *     "http://localhost:8080/oauth/token",
 *     "my-client-id",
 *     "my-client-secret"
 * );
 *
 * // Use with test browser
 * var request = Request.GET()
 *     .url(url)
 *     .withAuth(oauth);  // Automatically adds Bearer token
 *
 * // Or get token directly
 * String token = oauth.getAccessToken();
 * }</pre>
 *
 * @see BearerTokenAuth
 */
public final class OAuth2TokenManager implements AuthProvider {

    private static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;
    private final TestBrowser testBrowser;
    private final ReentrantLock lock = new ReentrantLock();

    private volatile TokenInfo currentToken;
    private volatile int tokenExpiryBufferSeconds = 60;

    /**
     * Creates a new OAuth2TokenManager for client credentials flow.
     *
     * @param tokenUrl the OAuth 2.0 token endpoint URL
     * @param clientId the OAuth client ID
     * @param clientSecret the OAuth client secret
     * @param testBrowser the test browser to use for token requests
     * @throws NullPointerException if any parameter is null
     */
    public OAuth2TokenManager(String tokenUrl, String clientId, String clientSecret, TestBrowser testBrowser) {
        this.tokenUrl = Objects.requireNonNull(tokenUrl, "tokenUrl must not be null");
        this.clientId = Objects.requireNonNull(clientId, "clientId must not be null");
        this.clientSecret = Objects.requireNonNull(clientSecret, "clientSecret must not be null");
        this.testBrowser = Objects.requireNonNull(testBrowser, "testBrowser must not be null");
    }

    /**
     * Creates a new OAuth2TokenManager with a new TestBrowser instance.
     *
     * @param tokenUrl the OAuth 2.0 token endpoint URL
     * @param clientId the OAuth client ID
     * @param clientSecret the OAuth client secret
     */
    public OAuth2TokenManager(String tokenUrl, String clientId, String clientSecret) {
        this(tokenUrl, clientId, clientSecret, new org.r10r.doctester.testbrowser.TestBrowserImpl());
    }

    /**
     * Sets the buffer time before token expiry when a refresh should occur.
     *
     * @param seconds the buffer time in seconds (default: 60)
     * @return this manager for chaining
     */
    public OAuth2TokenManager tokenExpiryBuffer(int seconds) {
        this.tokenExpiryBufferSeconds = Math.max(0, seconds);
        return this;
    }

    /**
     * Gets the current access token, acquiring a new one if necessary.
     *
     * @return the access token
     * @throws IllegalStateException if token acquisition fails
     */
    public String getAccessToken() {
        lock.lock();
        try {
            if (needsRefresh()) {
                acquireNewToken();
            }
            return currentToken != null ? currentToken.accessToken() : null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Gets the current refresh token, if available.
     *
     * @return the refresh token, or null if not available
     */
    public String getRefreshToken() {
        return currentToken != null ? currentToken.refreshToken() : null;
    }

    /**
     * Forces acquisition of a new token.
     *
     * @return the new access token
     * @throws IllegalStateException if token acquisition fails
     */
    public String refreshAccessToken() {
        lock.lock();
        try {
            if (currentToken != null && currentToken.refreshToken() != null) {
                refreshToken();
            } else {
                acquireNewToken();
            }
            return currentToken != null ? currentToken.accessToken() : null;
        } finally {
            lock.unlock();
        }
    }

    /**
     * Clears the current token.
     */
    public void clearToken() {
        lock.lock();
        try {
            currentToken = null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void apply(Request request) {
        var token = getAccessToken();
        if (token != null) {
            new BearerTokenAuth(token).apply(request);
        }
    }

    private boolean needsRefresh() {
        if (currentToken == null) {
            return true;
        }
        var now = Instant.now();
        var expiresAt = currentToken.expiresAt();
        return expiresAt != null && now.plusSeconds(tokenExpiryBufferSeconds).isAfter(expiresAt);
    }

    private void acquireNewToken() {
        var request = Request.POST()
            .url(Url.host(tokenUrl))
            .addFormParameter("grant_type", GRANT_TYPE_CLIENT_CREDENTIALS)
            .addFormParameter("client_id", clientId)
            .addFormParameter("client_secret", clientSecret);

        var response = testBrowser.makeRequest(request);

        if (response.httpStatus >= 400) {
            throw new IllegalStateException("Token acquisition failed with status: " + response.httpStatus);
        }

        currentToken = parseTokenResponse(response);
    }

    private void refreshToken() {
        if (currentToken == null || currentToken.refreshToken() == null) {
            acquireNewToken();
            return;
        }

        var request = Request.POST()
            .url(Url.host(tokenUrl))
            .addFormParameter("grant_type", GRANT_TYPE_REFRESH_TOKEN)
            .addFormParameter("refresh_token", currentToken.refreshToken())
            .addFormParameter("client_id", clientId)
            .addFormParameter("client_secret", clientSecret);

        var response = testBrowser.makeRequest(request);

        if (response.httpStatus >= 400) {
            // Refresh failed, try to acquire new token
            acquireNewToken();
            return;
        }

        currentToken = parseTokenResponse(response);
    }

    private TokenInfo parseTokenResponse(Response response) {
        try {
            var tokenResponse = response.payloadJsonAs(new TypeReference<Map<String, Object>>() {});

            var accessToken = (String) tokenResponse.get("access_token");
            var refreshToken = (String) tokenResponse.get("refresh_token");
            var expiresIn = tokenResponse.get("expires_in");

            Instant expiresAt = null;
            if (expiresIn instanceof Number n) {
                expiresAt = Instant.now().plusSeconds(n.longValue());
            }

            return new TokenInfo(accessToken, refreshToken, expiresAt);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse token response: " + e.getMessage(), e);
        }
    }

    private record TokenInfo(
        String accessToken,
        String refreshToken,
        Instant expiresAt
    ) {}
}
