/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.docs;

import controllers.utils.NinjaApiDoctester;
import models.ArticleDto;
import models.ArticlesDto;
import org.apache.http.cookie.Cookie;
import org.junit.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Authentication and session cookies.
 *
 * Mirrors docs/how-to/use-cookies.md — covers the login flow, cookie
 * inspection, session persistence, and per-test-method cookie isolation.
 */
public class AuthenticationDocTest extends NinjaApiDoctester {

    static final String LOGIN_URL        = "/login";
    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";

    @Test
    public void testLoginFlow() {

        sayNextSection("Authenticating — Login");

        say("Write operations require an authenticated session. "
            + "Authenticate by POST-ing form credentials to /login. "
            + "The server sets a session cookie on success.");

        Response loginResponse = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        sayAndAssertThat("Login returns 200 OK",
            loginResponse.httpStatus, equalTo(200));

        say("DocTester stores the session cookie automatically in its cookie "
            + "jar and sends it with every subsequent request in this test method.");

        List<Cookie> cookies = sayAndGetCookies();

        sayAndAssertThat("At least one cookie was set",
            cookies.isEmpty() ? null : cookies.get(0), notNullValue());
    }

    @Test
    public void testSessionPersistence() {

        sayNextSection("Session Cookie Persistence Across Requests");

        say("Once authenticated, all subsequent requests in the same @Test "
            + "method automatically include the session cookie. "
            + "No manual cookie management is needed.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto article = new ArticleDto();
        article.title   = "Session Test Article";
        article.content = "Created in an authenticated session automatically.";

        Response createResponse = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Authenticated POST succeeds",
            createResponse.httpStatus, equalTo(200));

        Response listResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto = listResponse.payloadAs(ArticlesDto.class);

        sayAndAssertThat("Article count increased to 4",
            dto.articles.size(), equalTo(4));
    }

    @Test
    public void testInspectingCookies() {

        sayNextSection("Inspecting Cookies");

        say("getCookies() returns all cookies. "
            + "getCookieWithName(name) returns a specific cookie by name. "
            + "The say* variants include them in the HTML output.");

        sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        List<Cookie> allCookies = sayAndGetCookies();

        sayAndAssertThat("Cookie jar is not empty after login",
            allCookies.isEmpty() ? null : allCookies.get(0), notNullValue());

        Cookie sessionCookie = sayAndGetCookieWithName("NINJA_SESSION");

        sayAndAssertThat("NINJA_SESSION cookie is present",
            sessionCookie, notNullValue());
    }

    @Test
    public void testCookieScopeIsPerMethod() {

        sayNextSection("Cookie Scope — Per Test Method");

        say("DocTester creates a fresh HTTP client (and empty cookie jar) for "
            + "each @Test method. Cookies do not carry over between test methods.");

        List<Cookie> initial = getCookies();

        sayAndAssertThat("No cookies at start of fresh method",
            initial.size(), equalTo(0));

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        sayAndAssertThat("Cookie present after login",
            getCookieWithName("NINJA_SESSION"), notNullValue());

        say("This isolation prevents authentication state leaking between "
            + "test methods. Each method starts with a clean session.");
    }
}
