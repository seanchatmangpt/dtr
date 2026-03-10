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
import org.junit.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;
import org.r10r.doctester.testbrowser.Url;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Url builder API.
 *
 * Mirrors docs/reference/url-builder.md — exercises Url.host(), path(),
 * addQueryParameter(), uri(), and toString() against the live server.
 */
public class UrlBuilderDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL = "/api/bob@gmail.com/articles.json";

    @Test
    public void testUrlHostFactory() {

        sayNextSection("Url.host() — Creating a Base URL");

        say("Url.host(String) is the entry point for all URL building. "
            + "Pass the scheme, host, and optional port. "
            + "testServerUrl() returns a pre-built host Url for the running server:");

        Url base = testServerUrl();

        sayAndAssertThat("testServerUrl() returns a non-null Url", base, notNullValue());

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Request reaches server via Url builder",
            response.httpStatus, equalTo(200));

        sayRaw("""
            <pre><code>// Build a Url from a literal host:
            Url.host("http://localhost:8080")
            Url.host("https://api.example.com")
            Url.host("https://staging.example.com:8443")</code></pre>
            """);
    }

    @Test
    public void testPathChaining() {

        sayNextSection("path() — Adding Path Segments");

        say("path(String) appends a segment to the URL. "
            + "Always start a fresh Url from testServerUrl() for each request "
            + "to avoid accumulating segments across calls:");

        Response jsonResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("JSON articles endpoint", jsonResponse.httpStatus, equalTo(200));

        Response xmlResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path("/api/bob@gmail.com/articles.xml")));

        sayAndAssertThat("XML articles endpoint", xmlResponse.httpStatus, equalTo(200));

        sayRaw("""
            <pre><code>// Dynamic path segments:
            String user = "bob@gmail.com";
            testServerUrl().path("/api/" + user + "/articles.json")</code></pre>
            """);
    }

    @Test
    public void testUriConversion() {

        sayNextSection("uri() and toString() — Conversion Methods");

        say("uri() returns a java.net.URI. toString() returns the URL string. "
            + "Both are called internally by Request when executing the HTTP call.");

        Url articlesUrl = testServerUrl().path(ARTICLES_URL);

        sayAndAssertThat("toString() is non-null", articlesUrl.toString(), notNullValue());
        sayAndAssertThat("uri() is non-null",      articlesUrl.uri(),      notNullValue());

        say("Full URL: " + articlesUrl);

        Response response = sayAndMakeRequest(Request.GET().url(articlesUrl));

        sayAndAssertThat("URL resolves correctly", response.httpStatus, equalTo(200));
    }

    @Test
    public void testQueryParameters() {

        sayNextSection("addQueryParameter() — Query String Building");

        say("addQueryParameter(key, value) appends ?key=value to the URL. "
            + "Chain calls to add multiple parameters:");

        Response response = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl()
                    .path(ARTICLES_URL)
                    .addQueryParameter("page", "1")
                    .addQueryParameter("pageSize", "10")));

        sayAndAssertThat("Query parameters sent successfully",
            response.httpStatus, equalTo(200));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);
        sayAndAssertThat("Response still contains articles",
            dto.articles, notNullValue());

        say("The full URL including the query string is visible in the "
            + "request panel above. The server ignores unknown parameters "
            + "and returns the full list.");
    }
}
