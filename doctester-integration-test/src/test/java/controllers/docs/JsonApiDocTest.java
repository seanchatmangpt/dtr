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
import models.Article;
import models.ArticleDto;
import models.ArticlesDto;
import org.junit.jupiter.api.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: JSON API endpoints.
 *
 * Mirrors docs/how-to/test-json-endpoints.md — covers listing, creating, and
 * deserializing articles via the JSON API.
 */
public class JsonApiDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL    = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL       = "/login";

    @Test
    public void testListArticlesAsJson() {

        sayNextSection("Listing Articles (JSON GET)");

        say("GET /api/{username}/articles.json returns all articles as JSON. "
            + "The response has a top-level 'articles' array. "
            + "This endpoint is public — no authentication required.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Status is 200 OK", response.httpStatus, equalTo(200));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);

        sayAndAssertThat("Articles list is present", dto.articles, notNullValue());
        sayAndAssertThat("Three articles are seeded", dto.articles.size(), equalTo(3));

        Article first = dto.articles.get(0);
        sayAndAssertThat("Each article has an ID",    first.id,    notNullValue());
        sayAndAssertThat("Each article has a title",  first.title, notNullValue());
        sayAndAssertThat("Each article has content",  first.content, notNullValue());
        sayAndAssertThat("Each article has postedAt", first.postedAt, notNullValue());
    }

    @Test
    public void testPostArticleAsJson() {

        sayNextSection("Creating an Article (JSON POST)");

        say("POST /api/{username}/article.json creates a new article. "
            + "The request body must be JSON with 'title' and 'content' fields. "
            + "Authentication via session cookie is required.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto newArticle = new ArticleDto();
        newArticle.title   = "Introduction to DocTester";
        newArticle.content = "DocTester makes API documentation effortless "
                           + "by generating HTML from your JUnit tests.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(newArticle));

        sayAndAssertThat("Article created — 200 OK", response.httpStatus, equalTo(200));

        say("After a successful POST the article is available via GET:");

        Response listResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto updated = listResponse.payloadAs(ArticlesDto.class);

        sayAndAssertThat("Article count increased from 3 to 4",
            updated.articles.size(), equalTo(4));
    }

    @Test
    public void testValidationAnnotationsDocumented() {

        sayNextSection("JSR-303 Validation Annotations on ArticleDto");

        say("The ArticleDto class carries @Size annotations documenting the "
            + "intended minimum lengths for title and content fields:");

        sayRaw("""
            <pre><code>public class ArticleDto {
                @Size(min = 5)
                public String title;

                @Size(min = 5)
                public String content;
            }</code></pre>
            """);

        say("Validation enforcement is controller-dependent. "
            + "This API accepts the payload and stores it regardless of length. "
            + "The annotations serve as documentation of the intended contract.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto article = new ArticleDto();
        article.title   = "Short title article";
        article.content = "Content demonstrating the API contract.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Request accepted", response.httpStatus, equalTo(200));
    }

    @Test
    public void testArticleFields() {

        sayNextSection("Article Object Fields");

        sayRaw("""
            <table class="table table-bordered table-condensed">
              <thead><tr><th>Field</th><th>Type</th><th>Description</th></tr></thead>
              <tbody>
                <tr><td>id</td>      <td>number</td><td>Unique identifier (auto-assigned)</td></tr>
                <tr><td>title</td>   <td>string</td><td>Article headline</td></tr>
                <tr><td>content</td> <td>string</td><td>Article body text</td></tr>
                <tr><td>postedAt</td><td>number</td><td>Epoch-millisecond timestamp</td></tr>
              </tbody>
            </table>
            """);

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);
        Article article = dto.articles.get(0);

        sayAndAssertThat("id present",      article.id,       notNullValue());
        sayAndAssertThat("title present",   article.title,    notNullValue());
        sayAndAssertThat("content present", article.content,  notNullValue());
        sayAndAssertThat("postedAt set",    article.postedAt, notNullValue());
    }
}
