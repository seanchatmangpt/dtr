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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: Getting Started tutorial.
 *
 * Every code example from docs/tutorials/your-first-doctest.md is a real,
 * executable assertion against the live Ninja test server.
 */
public class GettingStartedDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL = "/api/bob@gmail.com/articles.json";

    @Test
    public void testBasicGetRequest() {

        sayNextSection("Making Your First Request");

        say("The simplest thing DocTester can do is issue a GET request and "
            + "document the response. The request and response panels below are "
            + "generated automatically from a single sayAndMakeRequest() call.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Server responds with HTTP 200 OK",
            response.httpStatus, equalTo(200));

        sayAndAssertThat("Response body is not empty",
            response.payload, notNullValue());

        say("That is the full loop: one test method produces a complete "
            + "request/response document entry with live assertions.");
    }

    @Test
    public void testDeserializingTheResponse() {

        sayNextSection("Reading the Response Body");

        say("Use response.payloadAs(MyClass.class) to deserialize the JSON body "
            + "into a Java object. DocTester uses Jackson and auto-detects JSON "
            + "from the Content-Type header.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Status is 200", response.httpStatus, equalTo(200));

        ArticlesDto dto = response.payloadAs(ArticlesDto.class);

        sayAndAssertThat("The articles field is populated",
            dto.articles, notNullValue());

        sayAndAssertThat("The server seeds 3 articles for the test user",
            dto.articles.size(), equalTo(3));

        say("Each article has an id, title, content, and postedAt timestamp.");
    }

    @Test
    public void testAddingNarrative() {

        sayNextSection("Adding Narrative with say()");

        say("Use say() before and after requests to explain what the API does. "
            + "Each call produces a paragraph in the HTML output.");

        say("We are fetching the article list for bob@gmail.com. "
            + "This is a public endpoint — no authentication required.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        say("The response body is JSON. Articles are returned in insertion order.");

        sayAndAssertThat("Request succeeded", response.httpStatus, equalTo(200));

        say("Tip: write say() text as if explaining the API to a colleague. "
            + "The request/response panel provides the technical detail; "
            + "say() provides the context.");
    }

    @Test
    public void testOrganizingSections() {

        sayNextSection("Organizing Documentation into Sections");

        say("sayNextSection(title) creates an H1 heading and a clickable "
            + "sidebar entry. Use it to divide a long DocTest into chapters.");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Endpoint is live", response.httpStatus, equalTo(200));

        sayNextSection("What the Sidebar Looks Like");

        say("The sidebar now shows two entries: "
            + "'Organizing Documentation into Sections' and "
            + "'What the Sidebar Looks Like'. Each sayNextSection() call adds one.");
    }
}
