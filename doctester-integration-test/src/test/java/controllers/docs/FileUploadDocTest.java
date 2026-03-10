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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.r10r.doctester.testbrowser.Request;
import org.r10r.doctester.testbrowser.Response;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;

/**
 * Full-loop DocTest: File upload API.
 *
 * Mirrors docs/how-to/upload-files.md — demonstrates addFileToUpload().
 * The integration test server does not expose an upload endpoint, so the
 * live example sends a file to /login to show the multipart Content-Type
 * header DocTester generates. Replace the URL with your upload endpoint.
 */
public class FileUploadDocTest extends NinjaApiDoctester {

    @TempDir
    Path tempDir;

    static final String LOGIN_URL    = "/login";
    static final String ARTICLES_URL = "/api/bob@gmail.com/articles.json";

    @Test
    public void testFileUploadApi() throws IOException {

        sayNextSection("addFileToUpload() — Multipart File Uploads");

        say("addFileToUpload(paramName, file) adds a file part to a "
            + "multipart/form-data request. DocTester uses Apache HttpMime "
            + "to build the multipart body. The Content-Type header is set "
            + "automatically — do NOT call contentTypeApplicationJson() "
            + "when uploading files.");

        sayRaw("""
            <pre><code>// Single file upload:
            File avatar = new File("src/test/resources/avatar.png");

            Response response = sayAndMakeRequest(
                Request.POST()
                    .url(testServerUrl().path("/api/users/42/avatar"))
                    .addFileToUpload("avatar", avatar));

            sayAndAssertThat("Uploaded", response.httpStatus, equalTo(200));</code></pre>
            """);

        say("Mix files with form parameters in one multipart body:");

        sayRaw("""
            <pre><code>Response response = sayAndMakeRequest(
                Request.POST()
                    .url(testServerUrl().path("/api/documents"))
                    .addFormParameter("title", "Q4 Report")
                    .addFormParameter("category", "finance")
                    .addFileToUpload("file", new File("report.pdf")));

            sayAndAssertThat("Document uploaded", response.httpStatus, equalTo(201));</code></pre>
            """);

        say("Send multiple files by chaining addFileToUpload() calls:");

        sayRaw("""
            <pre><code>Response response = sayAndMakeRequest(
                Request.POST()
                    .url(testServerUrl().path("/api/gallery"))
                    .addFileToUpload("photos", new File("photo1.jpg"))
                    .addFileToUpload("photos", new File("photo2.jpg")));

            sayAndAssertThat("Both uploaded", response.httpStatus, equalTo(201));</code></pre>
            """);

        say("Live example — sending a file to /login to show the "
            + "multipart/form-data Content-Type header in the request panel. "
            + "Replace the URL with your own upload endpoint:");

        Path tempFile = tempDir.resolve("doctest-upload-demo.txt");
        Files.writeString(tempFile,
            "DocTester multipart demo\n",
            StandardCharsets.UTF_8);

        Response liveDemo = sayAndMakeRequest(
            Request.POST()
                .url(testServerUrl().path(LOGIN_URL))
                .addFormParameter("username", "bob@gmail.com")
                .addFormParameter("password", "secret")
                .addFileToUpload("attachment", tempFile.toFile()));

        sayAndAssertThat("Multipart request sent", liveDemo.httpStatus, notNullValue());

        say("Notice the Content-Type: multipart/form-data; boundary=... "
            + "header in the panel above. DocTester sets this automatically "
            + "whenever addFileToUpload() is used.");
    }

    @Test
    public void testFileUploadNotes() {

        sayNextSection("Important Notes on File Uploads");

        sayRaw("""
            <div class="alert alert-warning">
              <ul>
                <li>
                  <strong>Don't call contentTypeApplicationJson()</strong> when
                  uploading files. addFileToUpload() sets multipart/form-data
                  automatically. Calling another content-type method overrides it.
                </li>
                <li>
                  <strong>The file must exist</strong> at test runtime.
                  Use <code>src/test/resources/</code> for fixtures or
                  JUnit's <code>@Rule TemporaryFolder</code> for generated files.
                </li>
                <li>
                  <strong>Mix freely</strong> with addFormParameter(). Both
                  become parts of the same multipart body.
                </li>
                <li>
                  <strong>Multiple files</strong>: chain addFileToUpload() calls.
                  Use the same parameter name to send a list, or different names
                  for distinct form fields.
                </li>
              </ul>
            </div>
            """);

        Response sanity = sayAndMakeRequest(
            Request.GET().url(testServerUrl().path(ARTICLES_URL)));

        sayAndAssertThat("Server is running", sanity.httpStatus, equalTo(200));
    }
}
