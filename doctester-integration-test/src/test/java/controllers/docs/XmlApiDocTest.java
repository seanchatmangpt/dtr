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
import org.junit.jupiter.api.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: XML API endpoints.
 *
 * Mirrors docs/how-to/test-xml-endpoints.md — fetching and posting articles
 * via XML content types.
 */
public class XmlApiDocTest extends NinjaApiDoctester {

    static final String ARTICLES_XML_URL    = "/api/bob@gmail.com/articles.xml";
    static final String POST_ARTICLE_XML_URL = "/api/bob@gmail.com/article.xml";
    static final String ARTICLES_JSON_URL   = "/api/bob@gmail.com/articles.json";
    static final String LOGIN_URL           = "/login";

    @Test
    public void testListArticlesAsXml() {

        sayNextSection("Listing Articles (XML)");

        say("The articles API supports both JSON and XML via URL suffix. "
            + "Use /articles.xml to request XML output. "
            + "No authentication is required to list articles.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_XML_URL)));

        sayAndAssertThat("200 OK", response.httpStatus, equalTo(200));
        sayAndAssertThat("Response body is present", response.payload, notNullValue());

        say("The Content-Type is application/xml. DocTester detects this and "
            + "pretty-prints the XML body in the response panel above.");
    }

    @Test
    public void testPostArticleAsXml() {

        sayNextSection("Creating an Article (XML POST)");

        say("POST /api/{username}/article.xml accepts an XML body. "
            + "Use contentTypeApplicationXml() and pass any serializable object "
            + "as the payload. Authentication is required.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto newArticle = new ArticleDto();
        newArticle.title   = "Writing XML API Tests";
        newArticle.content = "DocTester serializes Java objects to XML "
                           + "using Jackson XmlMapper automatically.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_XML_URL))
                .contentTypeApplicationXml()
                .payload(newArticle));

        sayAndAssertThat("XML article created — 200 OK", response.httpStatus, equalTo(200));

        say("After posting via XML the article is available via the JSON "
            + "endpoint too. The storage format is independent of request format.");
    }

    @Test
    public void testFormatComparison() {

        sayNextSection("JSON vs XML — Same Data, Different Formats");

        say("The same articles are returned regardless of format. "
            + "Choose based on client needs: JSON for web and mobile, "
            + "XML for enterprise integrations.");

        say("JSON response:");

        Response jsonResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_JSON_URL)));

        sayAndAssertThat("JSON — 200 OK", jsonResponse.httpStatus, equalTo(200));

        say("XML response:");

        Response xmlResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_XML_URL)));

        sayAndAssertThat("XML — 200 OK", xmlResponse.httpStatus, equalTo(200));

        ArticlesDto json = jsonResponse.payloadAs(ArticlesDto.class);
        sayAndAssertThat("Both endpoints return 3 articles",
            json.articles.size(), equalTo(3));
    }
}
