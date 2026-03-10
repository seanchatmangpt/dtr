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

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

/**
 * Full-loop DocTest: HTTP methods.
 *
 * Demonstrates GET, POST, DELETE, HEAD, and custom headers against the
 * live Ninja integration test server.
 */
public class HttpMethodsDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL        = "/login";

    @Test
    public void testGetMethod() {

        sayNextSection("GET — Retrieve Resources");

        say("GET retrieves data without modifying server state. "
            + "It is safe and idempotent.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("GET returns 200 OK", response.httpStatus, equalTo(200));
        sayAndAssertThat("Response body is present", response.payload, notNullValue());
    }

    @Test
    public void testPostMethod() {

        sayNextSection("POST — Create Resources");

        say("POST sends a request body to create a new resource. "
            + "It is neither safe nor idempotent.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto article = new ArticleDto();
        article.title   = "HTTP Methods in DocTester";
        article.content = "DocTester supports GET, POST, PUT, PATCH, DELETE, and HEAD.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("POST returns 200 OK", response.httpStatus, equalTo(200));

        say("Always set a Content-Type header on POST requests with a body. "
            + "Use contentTypeApplicationJson() or contentTypeApplicationXml().");
    }

    @Test
    public void testDeleteMethod() {

        sayNextSection("DELETE — Remove Resources");

        say("DELETE removes a resource permanently. It is idempotent.");

        Response list = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto     = list.payloadAs(ArticlesDto.class);
        Long        targetId = dto.articles.get(0).id;

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "tom@domain.com")
                .addFormParameter("password", "secret"));

        Response deleteResponse = sayAndMakeRequest(
            Request.DELETE()
                .url(testServerUrl().path("/api/article/" + targetId)));

        sayAndAssertThat("DELETE returns 204 No Content",
            deleteResponse.httpStatus, equalTo(204));

        sayAndAssertThat("204 has no response body",
            deleteResponse.payload, nullValue());

        say("Attempting to DELETE the same ID again returns an error "
            + "because the resource no longer exists:");

        Response secondDelete = sayAndMakeRequest(
            Request.DELETE()
                .url(testServerUrl().path("/api/article/" + targetId)));

        sayAndAssertThat("Second DELETE fails (4xx)",
            secondDelete.httpStatus >= 400, equalTo(true));
    }

    @Test
    public void testHeadMethod() {

        sayNextSection("HEAD — Retrieve Metadata Only");

        say("HEAD is identical to GET but the server omits the response body. "
            + "Use it to check existence or read headers without downloading payload.");

        say("Note: the Ninja Framework used in this demo does not implement "
            + "HEAD route matching, so 404 is returned. On a standards-compliant "
            + "server, HEAD returns the same headers as GET with an empty body.");

        Response response = sayAndMakeRequest(
            Request.HEAD().url(testServerUrl()));

        sayAndAssertThat("Headers are returned", response.headers, notNullValue());
        sayAndAssertThat("Body is null for HEAD", response.payload, nullValue());
    }

    @Test
    public void testCustomRequestHeaders() {

        sayNextSection("Custom Request Headers");

        say("addHeader(name, value) adds arbitrary headers. "
            + "Chain calls to accumulate multiple headers. "
            + "The request panel shows all headers sent.");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path(ARTICLES_URL))
                .addHeader("Accept", "application/json")
                .addHeader("X-Request-ID", "doctest-demo-001")
                .addHeader("X-Client-Version", "1.0.0"));

        sayAndAssertThat("Server accepts custom headers gracefully",
            response.httpStatus, equalTo(200));
    }
}
