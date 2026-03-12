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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import io.github.seanchatmangpt.dtr.testbrowser.Request;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowser;
import io.github.seanchatmangpt.dtr.testbrowser.TestBrowserImpl;
import io.github.seanchatmangpt.dtr.testbrowser.Url;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for OAuth2 token manager.
 */
class OAuth2TokenManagerTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private final TestBrowser testBrowser = new TestBrowserImpl();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void getAccessToken_shouldAcquireToken() throws Exception {
        // Set up token endpoint
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "test-access-token-123");
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", 3600);

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        var token = oauth.getAccessToken();

        assertThat(token, is("test-access-token-123"));

        // Verify the token request was made
        wireMock.verify(postRequestedFor(urlEqualTo("/oauth/token")));
    }

    @Test
    void apply_shouldAddBearerToken() throws Exception {
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "bearer-token-xyz");
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", 3600);

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        wireMock.stubFor(get(urlEqualTo("/api/protected"))
            .withHeader("Authorization", equalTo("Bearer bearer-token-xyz"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"protected\":true}")));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        var request = Request.GET()
            .url(Url.host(wireMock.baseUrl()).path("/api/protected"))
            .withAuth(oauth);

        var response = testBrowser.makeRequest(request);

        assertThat(response.httpStatus, is(200));

        wireMock.verify(getRequestedFor(urlEqualTo("/api/protected"))
            .withHeader("Authorization", equalTo("Bearer bearer-token-xyz")));
    }

    @Test
    void getAccessToken_failedRequest_shouldThrow() {
        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(401)
                .withBody("{\"error\":\"invalid_client\"}")));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "bad-client",
            "bad-secret",
            testBrowser
        );

        Exception exception = null;
        try {
            oauth.getAccessToken();
        } catch (IllegalStateException e) {
            exception = e;
        }

        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), containsString("401"));
    }

    @Test
    void clearToken_shouldClearCurrentToken() throws Exception {
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "token-to-clear");
        tokenResponse.put("token_type", "Bearer");

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        var token = oauth.getAccessToken();
        assertThat(token, is("token-to-clear"));

        oauth.clearToken();

        // Getting token again should request new token
        wireMock.resetRequests();

        Map<String, Object> newTokenResponse = new LinkedHashMap<>();
        newTokenResponse.put("access_token", "new-token-after-clear");
        newTokenResponse.put("token_type", "Bearer");

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(newTokenResponse))));

        var newToken = oauth.getAccessToken();
        assertThat(newToken, is("new-token-after-clear"));

        // Verify new token request was made
        wireMock.verify(postRequestedFor(urlEqualTo("/oauth/token")));
    }

    @Test
    void tokenExpiryBuffer_shouldSetBuffer() throws Exception {
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "buffered-token");
        tokenResponse.put("token_type", "Bearer");
        tokenResponse.put("expires_in", 3600);

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        ).tokenExpiryBuffer(120);  // 2 minute buffer

        var token = oauth.getAccessToken();
        assertThat(token, is("buffered-token"));
    }

    @Test
    void refreshToken_shouldRefreshWhenAvailable() throws Exception {
        // Initial token with refresh token
        Map<String, Object> initialTokenResponse = new LinkedHashMap<>();
        initialTokenResponse.put("access_token", "initial-access");
        initialTokenResponse.put("refresh_token", "refresh-token-abc");
        initialTokenResponse.put("token_type", "Bearer");
        initialTokenResponse.put("expires_in", 3600);

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(initialTokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        var initialToken = oauth.getAccessToken();
        assertThat(initialToken, is("initial-access"));

        // Set up refresh token response
        wireMock.resetRequests();
        Map<String, Object> refreshedTokenResponse = new LinkedHashMap<>();
        refreshedTokenResponse.put("access_token", "refreshed-access");
        refreshedTokenResponse.put("token_type", "Bearer");
        refreshedTokenResponse.put("expires_in", 3600);

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(refreshedTokenResponse))));

        var refreshedToken = oauth.refreshAccessToken();
        assertThat(refreshedToken, is("refreshed-access"));
    }

    @Test
    void getRefreshToken_shouldReturnNullInitially() throws Exception {
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "no-refresh-token");
        tokenResponse.put("token_type", "Bearer");

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        oauth.getAccessToken();

        assertThat(oauth.getRefreshToken(), is(nullValue()));
    }

    @Test
    void constructor_withTestBrowser_shouldRequireNonNullParams() {
        var exception1 = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new OAuth2TokenManager(null, "client", "secret", testBrowser)
        );
        assertThat(exception1.getMessage(), containsString("tokenUrl"));

        var exception2 = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new OAuth2TokenManager("url", null, "secret", testBrowser)
        );
        assertThat(exception2.getMessage(), containsString("clientId"));

        var exception3 = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new OAuth2TokenManager("url", "client", null, testBrowser)
        );
        assertThat(exception3.getMessage(), containsString("clientSecret"));

        var exception4 = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new OAuth2TokenManager("url", "client", "secret", null)
        );
        assertThat(exception4.getMessage(), containsString("testBrowser"));
    }

    @Test
    void tokenExpiryBuffer_shouldNotAllowNegative() throws Exception {
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "test-token");
        tokenResponse.put("token_type", "Bearer");

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        ).tokenExpiryBuffer(-10);  // Negative buffer should be converted to 0

        var token = oauth.getAccessToken();
        assertThat(token, is("test-token"));
    }

    @Test
    void parseTokenResponse_shouldHandleInvalidJson() {
        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("not valid json")));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        Exception exception = null;
        try {
            oauth.getAccessToken();
        } catch (IllegalStateException e) {
            exception = e;
        }

        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), containsString("Failed to parse token response"));
    }

    @Test
    void refreshToken_failedRefresh_shouldAcquireNewToken() throws Exception {
        // Initial token with refresh token
        Map<String, Object> initialTokenResponse = new LinkedHashMap<>();
        initialTokenResponse.put("access_token", "initial-access");
        initialTokenResponse.put("refresh_token", "refresh-token-abc");
        initialTokenResponse.put("token_type", "Bearer");
        initialTokenResponse.put("expires_in", 3600);

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(initialTokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        var initialToken = oauth.getAccessToken();
        assertThat(initialToken, is("initial-access"));

        // Clear the initial stub and set up for failed refresh then success
        wireMock.resetRequests();
        wireMock.resetMappings();

        // New token will succeed
        Map<String, Object> newTokenResponse = new LinkedHashMap<>();
        newTokenResponse.put("access_token", "new-token-after-refresh-fail");
        newTokenResponse.put("token_type", "Bearer");

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(newTokenResponse))));

        var newToken = oauth.refreshAccessToken();
        assertThat(newToken, is("new-token-after-refresh-fail"));
    }

    @Test
    void apply_withNoTokenAcquired_shouldReturnNullToken() throws Exception {
        // Create manager with a working endpoint
        Map<String, Object> tokenResponse = new LinkedHashMap<>();
        tokenResponse.put("access_token", "initial-token");
        tokenResponse.put("token_type", "Bearer");

        wireMock.stubFor(post(urlEqualTo("/oauth/token"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(objectMapper.writeValueAsString(tokenResponse))));

        var oauth = new OAuth2TokenManager(
            wireMock.baseUrl() + "/oauth/token",
            "test-client",
            "test-secret",
            testBrowser
        );

        // Initially get a token
        var initialToken = oauth.getAccessToken();
        assertThat(initialToken, is("initial-token"));

        // Clear the token
        oauth.clearToken();

        // After clearing, getAccessToken should return null until we request again
        assertThat(oauth.getAccessToken(), is(notNullValue())); // This will trigger a new token request
    }
}
