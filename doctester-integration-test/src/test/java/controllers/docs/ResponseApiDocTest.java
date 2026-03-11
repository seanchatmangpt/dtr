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
import models.ArticlesDto;
import org.junit.jupiter.api.Test;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Response API reference.
 *
 * Mirrors docs/reference/response-api.md — exercises every Response field
 * and method (httpStatus, headers, payload, deserialization) live.
 */
public class ResponseApiDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL = "/api/bob@gmail.com/articles.json";

    @Test
    public void testHttpStatusField() {

        sayNextSection("response.httpStatus — The Status Code");

        say("Every Response has an httpStatus int field. Use it in assertions "
            + "to verify the API's behaviour:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Status is 200 OK", response.httpStatus, equalTo(200));

        sayTable(new String[][] {
            {"Status", "Meaning"},
            {"200", "OK — successful GET or POST"},
            {"201", "Created — resource successfully created"},
            {"204", "No Content — success with no body (DELETE)"},
            {"400", "Bad Request — validation failure"},
            {"403", "Forbidden — authentication or role failure"},
            {"404", "Not Found — unknown resource or endpoint"}
        });

        sayNote("Always check httpStatus before accessing the response body. "
            + "An error status may have a different body structure or none at all.");
    }

    @Test
    public void testHeadersMap() {

        sayNextSection("response.headers — Response Headers");

        say("The headers field is a Map&lt;String,String&gt; of all response headers. "
            + "Use it to assert on Content-Type, Location, caching, etc.:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Headers map is present",   response.headers,         notNullValue());
        sayAndAssertThat("Headers map is not empty", response.headers.isEmpty(), equalTo(false));

        String contentType = response.headers.get("Content-Type");

        sayAndAssertThat("Content-Type is present",        contentType, notNullValue());
        sayAndAssertThat("Content-Type signals JSON",
            contentType.contains("application/json"), equalTo(true));

        var headerPurposes = Map.of(
            "Content-Type", "Specifies the response format (application/json, application/xml, etc.)",
            "Content-Length", "Size of the response body in bytes",
            "Cache-Control", "Directives for caching behaviour",
            "Set-Cookie", "Instructs the client to store authentication tokens or session IDs",
            "Location", "URL of a newly created resource (in 201/302 responses)"
        );
        sayKeyValue(headerPurposes);
    }

    @Test
    public void testPayloadStringMethods() {

        sayNextSection("payloadAsString() and payloadAsPrettyString()");

        say("payloadAsString() returns the raw response body. "
            + "payloadAsPrettyString() reformats it — JSON is indented, "
            + "XML is pretty-printed. The documentation panel always shows "
            + "the pretty-printed version.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        String raw    = response.payloadAsString();
        String pretty = response.payloadAsPrettyString();

        sayAndAssertThat("Raw payload is not null",    raw,    notNullValue());
        sayAndAssertThat("Pretty payload is not null", pretty, notNullValue());
        sayAndAssertThat("Raw payload contains 'articles'",
            raw.contains("articles"), equalTo(true));

        var codeExample = """
            // Three ways to access the response body:
            byte[] raw = response.payload;
            String text = response.payloadAsString();
            String pretty = response.payloadAsPrettyString();
            """;
        sayCode(codeExample, "java");

        sayNote("Use payloadAsString() for raw access (e.g., to log or store). "
            + "Use payloadAsPrettyString() to display formatted output in assertions.");
    }

    @Test
    public void testPayloadAsAutoDetect() {

        sayNextSection("payloadAs(Class) — Auto-Detecting JSON or XML");

        say("payloadAs(Class) reads the Content-Type header and dispatches "
            + "to the JSON or XML deserializer automatically. "
            + "It is the recommended method for most use cases:");

        sayTable(new String[][] {
            {"Method", "Auto-detects?", "Use case"},
            {"payloadAs(Class)", "Yes (Content-Type)", "General-purpose deserialization"},
            {"payloadJsonAs(Class)", "No (forces JSON)", "When you know the format"},
            {"payloadXmlAs(Class)", "No (forces XML)", "Override Content-Type header"},
            {"payloadAsString()", "N/A", "Raw access to body text"}
        });

        Response jsonResp = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto json = jsonResp.payloadAs(ArticlesDto.class);

        sayAndAssertThat("JSON deserialized", json.articles.size(), equalTo(3));

        Article first = json.articles.get(0);
        sayAndAssertThat("article.id present",    first.id,    notNullValue());
        sayAndAssertThat("article.title present", first.title, notNullValue());

        say("Example JSON structure:");
        sayJson(json);

        say("XML variant — same method, different endpoint:");

        Response xmlResp = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path("/api/bob@gmail.com/articles.xml")));

        sayAndAssertThat("XML 200 OK", xmlResp.httpStatus, equalTo(200));
        sayAndAssertThat("XML body present", xmlResp.payload, notNullValue());

        sayNote("payloadAs() is preferred because it respects the server's Content-Type header. "
            + "This ensures correct deserialization even if the format changes.");
    }

    @Test
    public void testForceFormatDeserialization() {

        sayNextSection("payloadJsonAs() and payloadXmlAs() — Force Format");

        say("Use payloadJsonAs() or payloadXmlAs() to force a specific "
            + "deserializer regardless of the Content-Type header:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto = response.payloadJsonAs(ArticlesDto.class);

        sayAndAssertThat("payloadJsonAs() deserializes correctly",
            dto.articles.size(), equalTo(3));

        sayWarning("Format override methods bypass Content-Type inspection. "
            + "Use them sparingly — only when the server sends incorrect Content-Type headers.");

        var forceFormatCode = """
            // Force JSON (ignores Content-Type):
            MyDto dto = response.payloadJsonAs(MyDto.class);

            // Force XML:
            MyDto dto = response.payloadXmlAs(MyDto.class);

            // With TypeReference for generic collections:
            List<MyDto> list = response.payloadJsonAs(new TypeReference<>() {});
            """;
        sayCode(forceFormatCode, "java");
    }
}
