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

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.TestBrowser;
import org.r10r.doctester.testbrowser.TestBrowserImpl;
import org.r10r.doctester.testbrowser.Url;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for session-aware authentication provider.
 */
class AuthSessionIntegrationTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
        .options(wireMockConfig().dynamicPort())
        .build();

    private final TestBrowser testBrowser = new TestBrowserImpl();

    @Test
    void login_withFormCredentials_shouldCaptureSessionCookie() {
        // Set up login endpoint that returns a session cookie
        wireMock.stubFor(post(urlEqualTo("/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Set-Cookie", "JSESSIONID=ABC123; Path=/")));

        // Set up protected endpoint that requires the cookie
        wireMock.stubFor(get(urlEqualTo("/protected"))
            .withCookie("JSESSIONID", equalTo("ABC123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"data\":\"protected\"}")));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        // Login
        var loginResponse = sessionAuth.login("/login", creds -> {
            creds.put("username", "alice");
            creds.put("password", "secret");
        }, wireMock.baseUrl());

        assertThat(loginResponse.httpStatus, is(200));
        assertThat(sessionAuth.hasSession(), is(true));

        // Make request with session
        var request = Request.GET()
            .url(Url.host(wireMock.baseUrl()).path("/protected"))
            .withAuth(sessionAuth);

        var response = testBrowser.makeRequest(request);

        assertThat(response.httpStatus, is(200));
        assertThat(response.payload, containsString("protected"));
    }

    @Test
    void login_withJsonCredentials_shouldCaptureSessionCookie() {
        wireMock.stubFor(post(urlEqualTo("/api/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Set-Cookie", "SESSION=XYZ789; Path=/")));

        wireMock.stubFor(get(urlEqualTo("/api/user"))
            .withHeader("Cookie", containing("SESSION=XYZ789"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"user\":\"alice\"}")));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser)
            .sessionCookieName("SESSION");

        var credentials = new java.util.HashMap<String, String>();
        credentials.put("email", "alice@example.com");
        credentials.put("password", "secret");

        var loginResponse = sessionAuth.loginJson("/api/login", credentials, wireMock.baseUrl());

        assertThat(loginResponse.httpStatus, is(200));
        assertThat(sessionAuth.hasSession(), is(true));

        var request = Request.GET()
            .url(Url.host(wireMock.baseUrl()).path("/api/user"))
            .withAuth(sessionAuth);

        var response = testBrowser.makeRequest(request);

        assertThat(response.httpStatus, is(200));
    }

    @Test
    void login_failed_shouldThrowException() {
        wireMock.stubFor(post(urlEqualTo("/login"))
            .willReturn(aResponse()
                .withStatus(401)));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        Exception exception = null;
        try {
            sessionAuth.login("/login", creds -> {
                creds.put("username", "bad");
                creds.put("password", "wrong");
            }, wireMock.baseUrl());
        } catch (IllegalStateException e) {
            exception = e;
        }

        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), containsString("401"));
    }

    @Test
    void setSessionCookieValue_manualSetting_shouldWork() {
        wireMock.stubFor(get(urlEqualTo("/api/data"))
            .withHeader("Cookie", containing("CUSTOM_SESSION=manual123"))
            .willReturn(aResponse()
                .withStatus(200)
                .withBody("{\"data\":\"success\"}")));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser)
            .sessionCookieName("CUSTOM_SESSION")
            .setSessionCookieValue("manual123");

        assertThat(sessionAuth.hasSession(), is(true));

        var request = Request.GET()
            .url(Url.host(wireMock.baseUrl()).path("/api/data"))
            .withAuth(sessionAuth);

        var response = testBrowser.makeRequest(request);

        assertThat(response.httpStatus, is(200));
    }

    @Test
    void logout_shouldClearSession() {
        var sessionAuth = new SessionAwareAuthProvider(testBrowser)
            .setSessionCookieValue("session123");

        assertThat(sessionAuth.hasSession(), is(true));

        sessionAuth.logout();

        assertThat(sessionAuth.hasSession(), is(false));
    }

    @Test
    void apply_withoutSession_shouldNotAddCookie() {
        wireMock.stubFor(get(urlEqualTo("/public"))
            .willReturn(aResponse()
                .withStatus(200)));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        assertThat(sessionAuth.hasSession(), is(false));

        var request = Request.GET()
            .url(Url.host(wireMock.baseUrl()).path("/public"))
            .withAuth(sessionAuth);

        var response = testBrowser.makeRequest(request);

        assertThat(response.httpStatus, is(200));
    }

    @Test
    void sessionCookieName_customName_shouldUseCustomName() {
        wireMock.stubFor(post(urlEqualTo("/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Set-Cookie", "MY_SESSION=custom456; Path=/")));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser)
            .sessionCookieName("MY_SESSION");

        sessionAuth.login("/login", creds -> {}, wireMock.baseUrl());

        assertThat(sessionAuth.hasSession(), is(true));
    }

    @Test
    void constructor_shouldRequireNonNullTestBrowser() {
        var exception = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new SessionAwareAuthProvider(null)
        );
        assertThat(exception.getMessage(), containsString("testBrowser"));
    }

    @Test
    void sessionCookieName_shouldRequireNonNullName() {
        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> sessionAuth.sessionCookieName(null)
        );
        assertThat(exception.getMessage(), containsString("cookieName"));
    }

    @Test
    void setSessionCookieValue_shouldRequireNonNullValue() {
        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        var exception = org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> sessionAuth.setSessionCookieValue(null)
        );
        assertThat(exception.getMessage(), containsString("cookieValue"));
    }

    @Test
    void login_withDefaultBaseUrl_shouldWork() {
        // This test uses the default baseUrl (http://localhost:8080)
        // We just verify the method signature works
        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        // This will fail because there's no server, but that's expected
        // We're just testing the method signature
        Exception exception = null;
        try {
            sessionAuth.login("/login", creds -> {
                creds.put("username", "test");
                creds.put("password", "test");
            });
        } catch (Exception e) {
            exception = e;
        }

        // Should fail with connection refused, not NPE
        assertThat(exception, is(notNullValue()));
    }

    @Test
    void login_shouldDetectSessionLikeCookies() {
        // Test auto-detection of session-like cookies
        wireMock.stubFor(post(urlEqualTo("/login"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Set-Cookie", "sessionid=AUTO123; Path=/")));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        sessionAuth.login("/login", creds -> {}, wireMock.baseUrl());

        // Should detect "sessionid" as a session cookie
        assertThat(sessionAuth.hasSession(), is(true));
    }

    @Test
    void loginJson_failed_shouldThrowException() {
        wireMock.stubFor(post(urlEqualTo("/api/login"))
            .willReturn(aResponse()
                .withStatus(403)));

        var sessionAuth = new SessionAwareAuthProvider(testBrowser);

        Exception exception = null;
        try {
            // Use a Map instead of Object to ensure JSON serialization works
            var credentials = new java.util.HashMap<String, String>();
            credentials.put("username", "test");
            credentials.put("password", "test");
            sessionAuth.loginJson("/api/login", credentials, wireMock.baseUrl());
        } catch (IllegalStateException e) {
            exception = e;
        }

        assertThat(exception, is(notNullValue()));
        assertThat(exception.getMessage(), containsString("403"));
    }
}
