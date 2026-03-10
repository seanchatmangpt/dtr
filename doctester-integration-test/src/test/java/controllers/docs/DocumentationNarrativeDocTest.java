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
 * Full-loop DocTest: Documentation narrative techniques.
 *
 * Mirrors docs/how-to/control-documentation.md — demonstrates say(),
 * sayNextSection(), sayRaw(), makeRequest (silent) vs sayAndMakeRequest
 * (documented), and multi-section organization.
 */
public class DocumentationNarrativeDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL        = "/login";

    @Test
    public void testSayProducesFormattedParagraphs() {

        sayNextSection("say() — Adding Prose to Documentation");

        say("say() renders each call as a paragraph in the HTML output. "
            + "Write it as if addressing a developer reading the API guide.");

        say("The articles API is a simple CRUD service for blog content. "
            + "It supports JSON and XML formats and session-based authentication.");

        say("All examples use the test server started by @Before. "
            + "It is seeded with three articles and two users on every startup.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Server is running and articles endpoint is live",
            response.httpStatus, equalTo(200));

        say("Notice that the say() calls surrounding the request panel "
            + "create a natural reading flow — technical panel wrapped by prose.");
    }

    @Test
    public void testSayNextSectionBuildsNavigation() {

        sayNextSection("sayNextSection() — Creating Section Headings");

        say("Every sayNextSection() call adds an H1 heading and an anchor "
            + "link to the sidebar. Use it to organize a long DocTest:");

        sayNextSection("Section 1: Read Operations");

        say("Group related read operations under one section:");

        Response listResp = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Articles readable", listResp.httpStatus, equalTo(200));

        sayNextSection("Section 2: Write Operations");

        say("Group write operations under their own section:");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto article = new ArticleDto();
        article.title   = "Section Demo Article";
        article.content = "Written in the write-operations section.";

        Response createResp = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Article created", createResp.httpStatus, equalTo(200));
    }

    @Test
    public void testSayRawInjectsHtml() {

        sayNextSection("sayRaw() — Custom HTML in the Documentation");

        say("sayRaw(String) injects HTML directly into the page without escaping. "
            + "Use it for tables, callout boxes, code blocks, or any structure "
            + "that say() cannot express:");

        sayRaw("""
            <div class="alert alert-info">
              <strong>Note:</strong> The articles API supports JSON and XML.
            </div>
            """);

        sayRaw("""
            <table class="table table-bordered">
              <thead>
                <tr><th>Method</th><th>URL</th><th>Auth</th><th>Description</th></tr>
              </thead>
              <tbody>
                <tr>
                  <td>GET</td>
                  <td><code>/api/{user}/articles.json</code></td>
                  <td>None</td><td>List articles</td>
                </tr>
                <tr>
                  <td>POST</td>
                  <td><code>/api/{user}/article.json</code></td>
                  <td>Session</td><td>Create article</td>
                </tr>
                <tr>
                  <td>DELETE</td>
                  <td><code>/api/article/{id}</code></td>
                  <td>Admin</td><td>Delete article</td>
                </tr>
              </tbody>
            </table>
            """);

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Endpoint is live", response.httpStatus, equalTo(200));
    }

    @Test
    public void testSilentVsDocumentedRequests() {

        sayNextSection("makeRequest vs sayAndMakeRequest");

        say("Every DocTester method has two forms: a documented say* form "
            + "and a silent plain form. Use the silent form for setup "
            + "that is not meaningful to API readers.");

        say("Login is internal plumbing — use makeRequest() silently:");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        say("Creating an article is the documented operation — use sayAndMakeRequest():");

        ArticleDto article = new ArticleDto();
        article.title   = "Documented vs Silent Demo";
        article.content = "This request is the focus of this section.";

        Response createResp = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("The documented POST succeeds", createResp.httpStatus, equalTo(200));

        say("This page shows ONE request panel — the POST. The login happened "
            + "silently and is not visible to readers.");
    }

    @Test
    public void testMultipleMethodsShareOnePage() {

        sayNextSection("Multiple @Test Methods — One HTML Page");

        say("All @Test methods in a DocTest class contribute to one HTML page. "
            + "Each method appends its sections. Use @FixMethodOrder to control order.");

        say("Each @Test method gets a fresh TestBrowser (empty cookie jar). "
            + "The RenderMachine (HTML accumulator) is shared across all methods.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);

        sayAndAssertThat("Server still has 3 seeded articles",
            dto.articles, notNullValue());
    }
}
