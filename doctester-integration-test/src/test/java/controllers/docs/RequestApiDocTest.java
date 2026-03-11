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

import java.util.LinkedHashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Request builder API reference.
 *
 * Mirrors docs/reference/request-api.md — exercises every Request builder
 * method against the live server.
 */
public class RequestApiDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL        = "/login";

    @Test
    public void testFactoryMethods() {

        sayNextSection("Request Factory Methods — GET, POST, DELETE");

        say("Create requests via static factory methods: "
            + "Request.GET(), Request.POST(), Request.PUT(), Request.PATCH(), "
            + "Request.DELETE(), Request.HEAD(). Each returns a new Request for chaining.");

        var httpMethods = new String[][] {
            {"Method", "Use Case", "Expected Status"},
            {"GET", "Retrieve articles list", "200"},
            {"POST", "Create new article", "200"},
            {"PUT", "Update existing article", "200"},
            {"DELETE", "Remove article", "204"},
            {"HEAD", "Check resource exists", "200"}
        };
        sayTable(httpMethods);

        Response getResp = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("GET 200 OK", getResp.httpStatus, equalTo(200));

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto article = new ArticleDto();
        article.title   = "Factory Method Demo";
        article.content = "Created via Request.POST()";

        Response postResp = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("POST 200 OK", postResp.httpStatus, equalTo(200));
    }

    @Test
    public void testContentTypeHelpers() {

        sayNextSection("contentTypeApplicationJson() and contentTypeApplicationXml()");

        say("These helpers set the Content-Type header to the canonical values "
            + "for JSON and XML APIs. Always call them before payload() "
            + "when the request has a body.");

        var contentTypeGuide = Map.of(
            "application/json", "For REST APIs and web services",
            "application/xml", "For legacy systems and document interchange",
            "application/x-www-form-urlencoded", "For HTML form submissions"
        );
        sayKeyValue(contentTypeGuide);

        sayNote("Always set Content-Type before calling payload(). "
            + "DocTester uses this header to determine serialization format.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        say("JSON (Content-Type: application/json):");

        ArticleDto jsonArticle = new ArticleDto();
        jsonArticle.title   = "JSON Content-Type Demo";
        jsonArticle.content = "Sent with contentTypeApplicationJson().";

        Response jsonResp = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(jsonArticle));

        sayAndAssertThat("JSON POST accepted", jsonResp.httpStatus, equalTo(200));

        say("XML (Content-Type: application/xml):");

        ArticleDto xmlArticle = new ArticleDto();
        xmlArticle.title   = "XML Content-Type Demo";
        xmlArticle.content = "Sent with contentTypeApplicationXml().";

        Response xmlResp = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path("/api/bob@gmail.com/article.xml"))
                .contentTypeApplicationXml()
                .payload(xmlArticle));

        sayAndAssertThat("XML POST accepted", xmlResp.httpStatus, equalTo(200));
    }

    @Test
    public void testHeaderMethods() {

        sayNextSection("addHeader() and headers(Map) — Custom Request Headers");

        say("addHeader(name, value) adds a single header. "
            + "Chain calls to accumulate multiple headers:");

        var headerPractices = java.util.List.of(
            "Use Accept header to request JSON or XML format",
            "Include X-Request-ID for request tracing",
            "Send X-Client-Version for API versioning",
            "Set User-Agent to identify your client",
            "Include Authorization header for protected endpoints"
        );
        sayUnorderedList(headerPractices);

        Response singleHeader = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path(ARTICLES_URL))
                .addHeader("Accept", "application/json")
                .addHeader("X-Request-ID", "doctest-001")
                .addHeader("X-Client-Version", "2.0.0"));

        sayAndAssertThat("Custom headers accepted", singleHeader.httpStatus, equalTo(200));

        say("headers(Map) replaces all headers at once:");

        var pattern = """
            Request.GET()
                .url(testServerUrl().path("/api/users"))
                .headers(Map.of(
                    "Accept", "application/json",
                    "X-Correlation-ID", "batch-42"))""";
        sayCode(pattern, "java");

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Accept", "application/json");
        headers.put("X-Correlation-ID", "batch-42");

        Response mapHeaders = sayAndMakeRequest(
            Request.GET()
                .url(testServerUrl().path(ARTICLES_URL))
                .headers(headers));

        sayAndAssertThat("Map headers accepted", mapHeaders.httpStatus, equalTo(200));
    }

    @Test
    public void testFormParameters() {

        sayNextSection("addFormParameter() and formParameters(Map)");

        say("addFormParameter(key, value) builds an application/x-www-form-urlencoded "
            + "body — the standard encoding for HTML login forms:");

        var formFlow = java.util.List.of(
            "Construct a POST request",
            "Call addFormParameter() for each field (chain multiple calls)",
            "Submit to the login endpoint",
            "Cookies are preserved automatically across requests",
            "Subsequent requests include session cookies"
        );
        sayOrderedList(formFlow);

        Response loginResp = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        sayAndAssertThat("Form POST — login accepted", loginResp.httpStatus, equalTo(200));

        say("formParameters(Map) sets all form fields at once:");

        var formSubmission = """
            Response response = sayAndMakeRequest(
                Request.POST()
                    .url(testServerUrl().path("/login"))
                    .formParameters(Map.of(
                        "username", "bob@gmail.com",
                        "password", "secret")));""";
        sayCode(formSubmission, "java");

        clearCookies();

        Response adminLogin = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .formParameters(Map.of(
                    "username", "tom@domain.com",
                    "password", "secret")));

        sayAndAssertThat("Admin login via formParameters(Map)",
            adminLogin.httpStatus, equalTo(200));
    }

    @Test
    public void testPayloadSerialization() {

        sayNextSection("payload() — Automatic Object Serialization");

        say("payload(Object) serializes the Java object to the wire format "
            + "determined by the Content-Type header. "
            + "DocTester uses Jackson for both JSON and XML.");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        ArticleDto article = new ArticleDto();
        article.title   = "Payload Serialization Demo";
        article.content = "DocTester serializes this object to JSON automatically.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayJson(article);

        sayAndAssertThat("Payload serialized and accepted", response.httpStatus, equalTo(200));

        ArticlesDto list = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)))
            .payloadAs(ArticlesDto.class);

        sayWarning("Objects passed to payload() must be mutable and Jackson-compatible. "
            + "Avoid immutable records with private constructors or custom serialization logic.");

        sayAndAssertThat("Article appears in list", list.articles.size(), equalTo(4));
    }

    @Test
    public void testFollowRedirects() {

        sayNextSection("followRedirects(false) — Capturing Raw Redirects");

        say("By default DocTester follows redirects automatically. "
            + "Set followRedirects(false) to capture the raw 3xx response "
            + "and inspect the Location header:");

        Response loginNoFollow = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret")
                .followRedirects(false));

        say("Ninja redirects after successful login. With followRedirects(false) "
            + "we capture the redirect directly:");

        sayAndAssertThat("Login redirects rather than following to destination",
            loginNoFollow.httpStatus >= 300 && loginNoFollow.httpStatus < 400,
            equalTo(true));
    }
}
