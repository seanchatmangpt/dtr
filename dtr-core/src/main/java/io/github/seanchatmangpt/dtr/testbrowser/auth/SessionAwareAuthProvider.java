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
import io.github.seanchatmangpt.dtr.testbrowser.Response;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;

import java.util.Map;
import java.util.Objects;

/**
 * Authentication provider that manages session-based authentication using cookies.
 *
 * <p>This provider handles the common pattern of:
 * <ol>
 *   <li>Logging in with credentials</li>
 *   <li>Capturing the session cookie from the response</li>
 *   <li>Automatically adding the session cookie to subsequent requests</li>
 * </ol>
 *
 * <p>Usage:
 * <pre>{@code
 * var sessionAuth = new SessionAwareAuthProvider(testBrowser);
 *
 * // Login and capture session cookie
 * sessionAuth.login("/auth/login", credentials -> {
 *     credentials.put("username", "alice");
 *     credentials.put("password", "secret");
 * });
 *
 * // Now all requests with this provider will include the session cookie
 * var request = Request.GET()
 *     .url(url)
 *     .withAuth(sessionAuth);
 * }</pre>
 *
 * @see AuthProvider
 */
public final class SessionAwareAuthProvider implements AuthProvider {

    private final TestBrowser testBrowser;
    private volatile String sessionCookieName = "JSESSIONID";
    private volatile String sessionCookieValue;

    /**
     * Creates a new SessionAwareAuthProvider with the specified test browser.
     *
     * @param testBrowser the test browser to use for login requests
     * @throws NullPointerException if testBrowser is null
     */
    public SessionAwareAuthProvider(TestBrowser testBrowser) {
        this.testBrowser = Objects.requireNonNull(testBrowser, "testBrowser must not be null");
    }

    /**
     * Sets the name of the session cookie to capture and send.
     *
     * @param cookieName the session cookie name (default: "JSESSIONID")
     * @return this provider for chaining
     */
    public SessionAwareAuthProvider sessionCookieName(String cookieName) {
        this.sessionCookieName = Objects.requireNonNull(cookieName, "cookieName must not be null");
        return this;
    }

    /**
     * Performs a login request and captures the session cookie.
     *
     * <p>The login request is a POST to the specified path with form parameters.
     * After a successful login, the session cookie is captured and automatically
     * added to subsequent requests.
     *
     * @param loginPath the login endpoint path
     * @param credentials a consumer that populates the credentials map
     * @return the login response
     * @throws IllegalStateException if login fails or no session cookie is received
     */
    public Response login(String loginPath, java.util.function.Consumer<Map<String, String>> credentials) {
        return login(loginPath, credentials, "http://localhost:8080");
    }

    /**
     * Performs a login request and captures the session cookie.
     *
     * @param loginPath the login endpoint path
     * @param credentials a consumer that populates the credentials map
     * @param baseUrl the base URL of the server
     * @return the login response
     */
    public Response login(String loginPath, java.util.function.Consumer<Map<String, String>> credentials, String baseUrl) {
        var credMap = new java.util.HashMap<String, String>();
        credentials.accept(credMap);

        var request = Request.POST()
            .url(io.github.seanchatmangpt.dtr.testbrowser.Url.host(baseUrl).path(loginPath));

        for (var entry : credMap.entrySet()) {
            request.addFormParameter(entry.getKey(), entry.getValue());
        }

        var response = testBrowser.makeRequest(request);

        if (response.httpStatus >= 400) {
            throw new IllegalStateException("Login failed with status: " + response.httpStatus);
        }

        // Extract session cookie
        var cookie = testBrowser.getCookieWithName(sessionCookieName);
        if (cookie == null) {
            // Try to find any cookie that looks like a session cookie
            for (var c : testBrowser.getCookies()) {
                if (c.getName().toLowerCase().contains("session") ||
                    c.getName().equalsIgnoreCase("sessionid")) {
                    sessionCookieName = c.getName();
                    sessionCookieValue = c.getValue();
                    return response;
                }
            }
            // No session cookie found, but login might still be successful
            // (e.g., token-based auth in response body)
        } else {
            sessionCookieValue = cookie.getValue();
        }

        return response;
    }

    /**
     * Performs a login request with JSON credentials.
     *
     * @param loginPath the login endpoint path
     * @param credentials the credentials object (will be serialized as JSON)
     * @param baseUrl the base URL of the server
     * @return the login response
     */
    public Response loginJson(String loginPath, Object credentials, String baseUrl) {
        var request = Request.POST()
            .url(io.github.seanchatmangpt.dtr.testbrowser.Url.host(baseUrl).path(loginPath))
            .contentTypeApplicationJson()
            .payload(credentials);

        var response = testBrowser.makeRequest(request);

        if (response.httpStatus >= 400) {
            throw new IllegalStateException("Login failed with status: " + response.httpStatus);
        }

        // Extract session cookie
        var cookie = testBrowser.getCookieWithName(sessionCookieName);
        if (cookie != null) {
            sessionCookieValue = cookie.getValue();
        }

        return response;
    }

    /**
     * Sets the session cookie value directly.
     *
     * <p>Use this method if you obtained a session cookie through other means.
     *
     * @param cookieValue the session cookie value
     * @return this provider for chaining
     */
    public SessionAwareAuthProvider setSessionCookieValue(String cookieValue) {
        this.sessionCookieValue = Objects.requireNonNull(cookieValue, "cookieValue must not be null");
        return this;
    }

    /**
     * Checks if a session has been established.
     *
     * @return true if a session cookie has been captured
     */
    public boolean hasSession() {
        return sessionCookieValue != null;
    }

    /**
     * Clears the current session.
     */
    public void logout() {
        sessionCookieValue = null;
    }

    @Override
    public void apply(Request request) {
        if (sessionCookieValue != null) {
            request.addHeader("Cookie", sessionCookieName + "=" + sessionCookieValue);
        }
    }
}
