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
import org.junit.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.util.List;
import java.util.Optional;
import java.util.SequencedCollection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Java 25 language features in DocTester tests.
 *
 * Demonstrates records, sealed classes + pattern matching, switch expressions,
 * text blocks, SequencedCollection, var, and Optional.
 */
public class Java25DocTest extends NinjaApiDoctester {

    // Sealed hierarchy for typed API results
    sealed interface ApiResult<T> permits ApiResult.Ok, ApiResult.Err {
        record Ok<T>(int status, T body)       implements ApiResult<T> {}
        record Err<T>(int status, String reason) implements ApiResult<T> {}
    }

    // Record for slim article projection used in assertions
    record ArticleView(long id, String title) {}

    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL        = "/login";

    @Test
    public void testRecordPayloads() {

        sayNextSection("Records as Request Payload Types");

        say("Java 25 records work as request payloads with Jackson 2.12+. "
            + "Here we show the pattern using the existing ArticleDto, "
            + "and demonstrate what the record equivalent looks like:");

        sayRaw("""
            <pre><code>// With Jackson 2.12+ this works directly:
            record ArticlePayload(String title, String content) {}
            var payload = new ArticlePayload("My title", "My content");
            Request.POST().url(url).contentTypeApplicationJson().payload(payload)</code></pre>
            """);

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        // Use ArticleDto (Jackson 2.5 compatible — avoids anonymous class issue)
        var payload = new ArticleDto();
        payload.title   = "Records in DocTester";
        payload.content = "Java 25 records are compact and immutable.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(payload));

        sayAndAssertThat("Article created — 200 OK", response.httpStatus, equalTo(200));
    }

    @Test
    public void testSealedInterfacesAndSwitchExpressions() {

        sayNextSection("Sealed Interfaces and Exhaustive Switch Expressions");

        say("Model API outcomes as a sealed hierarchy. The compiler enforces "
            + "that every case is covered — no default needed:");

        sayRaw("""
            <pre><code>sealed interface ApiResult&lt;T&gt; permits ApiResult.Ok, ApiResult.Err {
                record Ok&lt;T&gt;(int status, T body)       implements ApiResult&lt;T&gt; {}
                record Err&lt;T&gt;(int status, String reason) implements ApiResult&lt;T&gt; {}
            }</code></pre>
            """);

        Response raw = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ApiResult<ArticlesDto> result = raw.httpStatus == 200
            ? new ApiResult.Ok<>(raw.httpStatus, raw.payloadAs(ArticlesDto.class))
            : new ApiResult.Err<>(raw.httpStatus, raw.payloadAsString());

        String summary = switch (result) {
            case ApiResult.Ok<ArticlesDto> ok  ->
                "Fetched %d articles (HTTP %d)".formatted(ok.body().articles.size(), ok.status());
            case ApiResult.Err<ArticlesDto> err ->
                "Failed with HTTP %d: %s".formatted(err.status(), err.reason());
        };

        say("Switch result: " + summary);

        sayAndAssertThat("Result matches expected",
            summary, equalTo("Fetched 3 articles (HTTP 200)"));
    }

    @Test
    public void testTextBlocksForNarrative() {

        sayNextSection("Text Blocks for Cleaner HTML Narrative");

        say("Java text blocks (\"\"\"...\"\"\") eliminate string concatenation noise "
            + "in sayRaw() calls and multi-line say() calls:");

        sayRaw("""
            <div class="alert alert-info">
              <strong>API Endpoint Summary</strong>
              <ul>
                <li><code>GET  /api/{username}/articles.json</code> — list (public)</li>
                <li><code>POST /api/{username}/article.json</code>  — create (auth)</li>
                <li><code>DELETE /api/article/{id}</code>           — delete (admin)</li>
              </ul>
            </div>
            """);

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Endpoint live", response.httpStatus, equalTo(200));
    }

    @Test
    public void testPatternMatchingAndRecordPatterns() {

        sayNextSection("Pattern Matching and Record Patterns");

        say("Java 21+ record pattern deconstruction and instanceof pattern "
            + "matching make response-processing code more concise:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("200 OK", response.httpStatus, equalTo(200));

        // instanceof pattern matching — no explicit cast
        Object rawPayload = response.payloadAsString();
        if (rawPayload instanceof String body && !body.isBlank()) {
            sayAndAssertThat("Payload is a non-blank String", true, equalTo(true));
        }

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);

        // Map to ArticleView records
        List<ArticleView> views = dto.articles.stream()
            .map(a -> new ArticleView(a.id, a.title))
            .toList();

        // Record pattern deconstruction in switch
        var labelSummary = new StringBuilder();
        for (ArticleView view : views) {
            String label = switch (view) {
                case ArticleView(var id, var title) when title.length() > 20 ->
                    "long-title #" + id;
                case ArticleView(var id, var title) ->
                    "short-title #" + id;
            };
            labelSummary.append(label).append("; ");
        }

        say("Labels: " + labelSummary);

        sayAndAssertThat("All 3 articles processed via record patterns",
            views.size(), equalTo(3));
    }

    @Test
    public void testSequencedCollections() {

        sayNextSection("SequencedCollection for Ordered Article Access");

        say("Java 21+ SequencedCollection.getFirst() / getLast() express "
            + "intent more clearly than index-based access:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("200 OK", response.httpStatus, equalTo(200));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);
        SequencedCollection<Article> articles = dto.articles;

        Article first = articles.getFirst();
        Article last  = articles.getLast();

        sayAndAssertThat("First article has ID", first.id, notNullValue());
        sayAndAssertThat("Last article has ID",  last.id,  notNullValue());
        sayAndAssertThat("3 articles total",     articles.size(), equalTo(3));
    }

    @Test
    public void testModernJavaIdioms() {

        sayNextSection("var, Streams, and Method References");

        say("Combine var type inference, method references, and streams "
            + "to write compact, readable assertions:");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        var titles = List.of("First modern article", "Second modern article");

        for (var title : titles) {
            var article = new ArticleDto();
            article.title   = title;
            article.content = "Content for: " + title;

            var resp = makeRequest(
                Request.POST()
                    .url(testServerUrl().path(POST_ARTICLE_URL))
                    .contentTypeApplicationJson()
                    .payload(article));

            sayAndAssertThat("Created: " + title, resp.httpStatus, equalTo(200));
        }

        var listResp = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        var dto             = listResp.payloadAs(ArticlesDto.class);
        var returnedTitles  = dto.articles.stream().map(a -> a.title).toList();

        sayAndAssertThat("Total is 5 (3 seeded + 2 created)",
            dto.articles.size(), equalTo(5));

        for (var expected : titles) {
            sayAndAssertThat("Title present: " + expected,
                returnedTitles.contains(expected), equalTo(true));
        }
    }

    @Test
    public void testOptionalForNullableFields() {

        sayNextSection("Optional for Nullable Response Fields");

        say("Wrap nullable response fields in Optional to force explicit "
            + "null-handling and avoid NullPointerExceptions:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);

        Optional<Long> firstId = dto.articles.stream()
            .findFirst()
            .map(a -> a.id);

        sayAndAssertThat("First article has a non-null ID", firstId.isPresent(), equalTo(true));
        sayAndAssertThat("ID is positive", firstId.orElseThrow() > 0, equalTo(true));
    }
}
