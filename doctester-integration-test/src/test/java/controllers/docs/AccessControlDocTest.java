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

/**
 * Full-loop DocTest: Access control and authorization.
 *
 * Documents the three-tier security model:
 *   1. Anonymous  — can read articles
 *   2. Authenticated user  — can create; cannot delete
 *   3. Authenticated admin — can create and delete
 */
public class AccessControlDocTest extends NinjaApiDoctester {

    static final String ARTICLES_URL     = "/api/bob@gmail.com/articles.json";
    static final String POST_ARTICLE_URL = "/api/bob@gmail.com/article.json";
    static final String LOGIN_URL        = "/login";

    @Test
    public void testPublicReadAccess() {

        sayNextSection("Public Read Access");

        say("Reading articles requires no authentication. "
            + "Any client may GET article lists:");

        Response response = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Anonymous GET returns 200 OK",
            response.httpStatus, equalTo(200));
    }

    @Test
    public void testUnauthenticatedWriteIsRejected() {

        sayNextSection("Write Operations Require Authentication");

        say("Attempting to POST a new article without logging in is rejected "
            + "with 403 Forbidden by the SecureFilter:");

        ArticleDto article = new ArticleDto();
        article.title   = "Unauthorized attempt";
        article.content = "This should be rejected by the server.";

        Response response = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Unauthenticated POST returns 403 Forbidden",
            response.httpStatus, equalTo(403));
    }

    @Test
    public void testAuthenticatedUserCanWrite() {

        sayNextSection("Authenticated Users Can Create Articles");

        say("After login the session cookie is carried automatically and the "
            + "same POST returns 200 OK:");

        Response loginResponse = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        sayAndAssertThat("Login succeeds", loginResponse.httpStatus, equalTo(200));

        ArticleDto article = new ArticleDto();
        article.title   = "My Authenticated Article";
        article.content = "Created after a successful login with a session cookie.";

        Response createResponse = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(POST_ARTICLE_URL))
                .contentTypeApplicationJson()
                .payload(article));

        sayAndAssertThat("Authenticated POST returns 200 OK",
            createResponse.httpStatus, equalTo(200));
    }

    @Test
    public void testDeleteRequiresAdminRole() {

        sayNextSection("Delete Requires Admin Role");

        say("DELETE /api/article/{id} is protected by two filters: "
            + "SecureFilter (authentication) and AdminFilter (admin role). "
            + "Walking through all three access levels:");

        say("Step 1 — fetch an article ID:");

        Response listResponse = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto dto      = listResponse.payloadAs(ArticlesDto.class);
        Long        articleId = dto.articles.get(0).id;

        String deleteUrl = "/api/article/" + articleId;

        say("Step 2 — anonymous DELETE:");

        Response anonDelete = sayAndMakeRequest(
            Request.DELETE().url(testServerUrl().path(deleteUrl)));

        sayAndAssertThat("Anonymous DELETE returns 403 Forbidden",
            anonDelete.httpStatus, equalTo(403));

        say("Step 3 — regular user DELETE:");

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret"));

        Response userDelete = sayAndMakeRequest(
            Request.DELETE().url(testServerUrl().path(deleteUrl)));

        sayAndAssertThat("Regular user DELETE still 403 — admin required",
            userDelete.httpStatus, equalTo(403));

        say("Step 4 — admin DELETE:");

        clearCookies();

        makeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "tom@domain.com")
                .addFormParameter("password", "secret"));

        Response adminDelete = sayAndMakeRequest(
            Request.DELETE().url(testServerUrl().path(deleteUrl)));

        sayAndAssertThat("Admin DELETE returns 204 No Content",
            adminDelete.httpStatus, equalTo(204));

        say("Step 5 — verify article is gone:");

        Response afterDelete = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        ArticlesDto remaining = afterDelete.payloadAs(ArticlesDto.class);

        sayAndAssertThat("Article count decreased from 3 to 2",
            remaining.articles.size(), equalTo(2));
    }
}
