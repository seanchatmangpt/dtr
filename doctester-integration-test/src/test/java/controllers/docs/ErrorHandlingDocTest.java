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
import org.junit.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Error handling and HTTP error status codes.
 *
 * Every documented error code is produced by a live request. DocTester
 * documents error responses exactly like success responses — the request and
 * response panels appear in the HTML output regardless of status code.
 */
public class ErrorHandlingDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL        = "/login";

    @Test
    public void test403MissingAuthentication() {

        sayNextSection("403 Forbidden — Authentication Required");

        say("Write operations require an authenticated session. "
            + "Without one, the SecureFilter returns 403 Forbidden before "
            + "the request reaches the controller:");

        ArticleDto article = new ArticleDto();
        article.title   = "Unauthorized attempt";
        article.content = "This article should never be created.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Unauthenticated POST returns 403 Forbidden",
            response.httpStatus, equalTo(403));

        say("The 403 body is empty. Authenticate via POST /login before retrying.");
    }

    @Test
    public void test403InsufficientRole() {

        sayNextSection("403 Forbidden — Insufficient Role");

        say("Some endpoints require a specific role in addition to authentication. "
            + "DELETE /api/article/{id} requires the 'admin' role. "
            + "Regular authenticated users receive 403:");

        Response list = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto  = list.payloadAs(ArticlesDto.class);
        Long articleId   = dto.articles.get(0).id;

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        Response deleteResp = sayAndMakeRequest(
            Request.DELETE().url(testServerUrl().path("/api/article/" + articleId)));

        sayAndAssertThat("Regular user DELETE returns 403 — admin required",
            deleteResp.httpStatus, equalTo(403));
    }

    @Test
    public void test404UnknownEndpoint() {

        sayNextSection("404 Not Found — Unsupported HTTP Method");

        say("404 is returned when the server has no route matching the request. "
            + "The Ninja framework in this demo has a catch-all GET route, "
            + "so unknown GET paths return 200. However, unsupported methods "
            + "(like HEAD, which Ninja does not implement) return 404:");

        Response response = sayAndMakeRequest(
            Request.HEAD().url(testServerUrl()));

        sayAndAssertThat("Unsupported method returns 404",
            response.httpStatus, equalTo(404));

        say("In a standards-compliant server without a catch-all route, "
            + "a GET to an unmapped path would also return 404.");
    }

    @Test
    public void testDeleteAndVerifyGone() {

        sayNextSection("204 No Content — Successful Delete");

        say("A successful DELETE returns 204 No Content — status code with no body:");

        Response list = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto = list.payloadAs(ArticlesDto.class);
        Long targetId   = dto.articles.get(0).id;

        sayAndAssertThat("3 articles before delete", dto.articles.size(), equalTo(3));

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "tom@domain.com")
                .addFormParameter("password", "secret"));

        Response deleteResp = sayAndMakeRequest(
            Request.DELETE().url(testServerUrl().path("/api/article/" + targetId)));

        sayAndAssertThat("DELETE returns 204 No Content",
            deleteResp.httpStatus, equalTo(204));

        Response afterList = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Article count decreased to 2",
            afterList.payloadAs(ArticlesDto.class).articles.size(), equalTo(2));
    }

    @Test
    public void testDocumentingErrorStructure() {

        sayNextSection("Documenting Error Responses");

        say("DocTester documents error responses exactly like successes. "
            + "Use sayAndAssertThat to make the expected failure explicit. "
            + "The request and response panels appear regardless of status:");

        ArticleDto article = new ArticleDto();
        article.title   = "Will not be saved";
        article.content = "Because the request lacks a session cookie.";

        Response forbidden = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Server correctly rejects unauthenticated write",
            forbidden.httpStatus, equalTo(403));

        sayAndAssertThat("Response has headers", forbidden.headers, notNullValue());

        say("Tip: document error paths alongside the happy path. "
            + "An API guide that shows both success and failure scenarios "
            + "is significantly more useful to integrators.");
    }
}
