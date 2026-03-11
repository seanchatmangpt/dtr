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
import models.ArticlesDto;
import org.junit.jupiter.api.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Query parameters.
 *
 * Mirrors docs/how-to/test-with-query-parameters.md — demonstrates building
 * URLs with query parameters and special character encoding.
 */
public class QueryParametersDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL = "/api/bob@gmail.com/articles.json";

    @Test
    public void testSingleQueryParameter() {

        sayNextSection("addQueryParameter() — Single Parameter");

        say("addQueryParameter(key, value) appends ?key=value to the URL. "
            + "The Url builder handles percent-encoding automatically.");

        say("Here we add a 'format' parameter to show the pattern — "
            + "the server ignores unknown parameters and returns the full list:");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl()
                    .path(ARTICLES_URL)
                    .addQueryParameter("format", "compact")));

        sayAndAssertThat("Server responds with unknown query parameter",
            response.httpStatus, equalTo(200));

        say("The full URL including the query string is visible in the request panel.");
    }

    @Test
    public void testMultipleQueryParameters() {

        sayNextSection("Chaining Multiple Query Parameters");

        say("Chain addQueryParameter() calls to build complex query strings. "
            + "Parameters are appended in call order with & separators:");

        sayRaw("""
            <pre><code>testServerUrl()
                .path("/api/articles")
                .addQueryParameter("page", "1")
                .addQueryParameter("pageSize", "20")
                .addQueryParameter("sortBy", "createdAt")
            // Produces: /api/articles?page=1&amp;pageSize=20&amp;sortBy=createdAt</code></pre>
            """);

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl()
                    .path(ARTICLES_URL)
                    .addQueryParameter("page", "1")
                    .addQueryParameter("pageSize", "10")
                    .addQueryParameter("sortBy", "title")));

        sayAndAssertThat("Multi-parameter request succeeds", response.httpStatus, equalTo(200));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);
        sayAndAssertThat("Articles returned", dto.articles, notNullValue());
    }

    @Test
    public void testUrlEncodingOfSpecialCharacters() {

        sayNextSection("URL Encoding — Special Characters in Parameters");

        say("addQueryParameter() automatically percent-encodes special characters. "
            + "You never need to manually encode spaces, ampersands, or non-ASCII:");

        sayRaw("""
            <table class="table table-bordered table-condensed">
              <thead><tr><th>Raw value</th><th>Encoded form</th></tr></thead>
              <tbody>
                <tr><td>hello world</td><td>hello+world</td></tr>
                <tr><td>a&amp;b</td>    <td>a%26b</td></tr>
                <tr><td>100%</td>       <td>100%25</td></tr>
              </tbody>
            </table>
            """);

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl()
                    .path(ARTICLES_URL)
                    .addQueryParameter("search", "hello world")
                    .addQueryParameter("tag", "java & testing")));

        sayAndAssertThat("Special characters encoded and request accepted",
            response.httpStatus, equalTo(200));
    }

    @Test
    public void testFreshUrlPerRequest() {

        sayNextSection("Fresh URL Per Request");

        say("Always start a new URL from testServerUrl() for each request. "
            + "The Url builder appends to an internal StringBuilder, so "
            + "reusing the same Url object across requests would accumulate segments.");

        sayRaw("""
            <pre><code>// Correct — fresh Url each time:
            sayAndMakeRequest(Request.GET().url(testServerUrl().path(URL).addQueryParameter("page", "1")));
            sayAndMakeRequest(Request.GET().url(testServerUrl().path(URL).addQueryParameter("page", "2")));

            // Wrong — Url is reused and segments accumulate:
            // Url base = testServerUrl().path(URL);
            // sayAndMakeRequest(Request.GET().url(base.addQueryParameter("page", "1")));
            // sayAndMakeRequest(Request.GET().url(base.addQueryParameter("page", "2"))); // BUG</code></pre>
            """);

        say("Page 1:");

        Response page1 = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path(ARTICLES_URL).addQueryParameter("page", "1")));

        sayAndAssertThat("Page 1 request", page1.httpStatus, equalTo(200));

        say("Page 2 (fresh URL):");

        Response page2 = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path(ARTICLES_URL).addQueryParameter("page", "2")));

        sayAndAssertThat("Page 2 request", page2.httpStatus, equalTo(200));
    }
}
