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
package io.github.seanchatmangpt.dtr.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Example test demonstrating HttpDocTestHelper usage.
 *
 * <p>This test shows the fluent API for HTTP testing and documentation.
 * It demonstrates both GET and POST requests with assertions and documentation.</p>
 *
 * <p>Note: This test makes real HTTP requests to public APIs.
 * It may fail if the APIs are unavailable or change their behavior.</p>
 */
@ExtendWith(DtrExtension.class)
public class HttpDocTestHelperExampleTest {

    /**
     * Demonstrates a simple GET request with status assertion and documentation.
     *
     * <p>This example shows the most common pattern:</p>
     * <ol>
     *   <li>Create the helper</li>
     *   <li>Make a GET request</li>
     *   <li>Assert on status</li>
     *   <li>Document the response</li>
     * </ol>
     */
    @Test
    void testGetRequest(DtrContext ctx) throws Exception {
        ctx.sayNextSection("GET Request Example");
        ctx.say("This example demonstrates a simple GET request to a public API.");

        var helper = new HttpDocTestHelper(ctx);

        helper.get("https://httpbin.org/status/200")
              .send()
              .expectStatus(200)
              .documentResponse();
    }

    /**
     * Demonstrates a POST request with JSON body.
     *
     * <p>This example shows:</p>
     * <ul>
     *   <li>Setting headers (Content-Type)</li>
     *   <li>Sending a JSON request body</li>
     *   <li>Asserting on response status</li>
     *   <li>Documenting the JSON response</li>
     * </ul>
     */
    @Test
    void testPostRequest(DtrContext ctx) throws Exception {
        ctx.sayNextSection("POST Request Example");
        ctx.say("This example demonstrates a POST request with a JSON body.");

        var helper = new HttpDocTestHelper(ctx);

        String requestBody = """
            {
              "name": "Alice",
              "email": "alice@example.com"
            }
            """;

        helper.post("https://httpbin.org/post")
              .header("Content-Type", "application/json")
              .body(requestBody)
              .send()
              .expectStatus(200)
              .expectHeader("Content-Type", "application/json")
              .documentResponse()
              .documentJson("POST Response");
    }

    /**
     * Demonstrates custom HTTP client configuration.
     *
     * <p>This example shows how to create an HttpDocTestHelper
     * with a custom HttpClient for advanced use cases.</p>
     */
    @Test
    void testCustomClient(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Custom HTTP Client Example");
        ctx.say("This example demonstrates using a custom HTTP client with extended timeout.");

        import java.net.http.HttpClient;
        import java.time.Duration;

        HttpClient customClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();

        var helper = new HttpDocTestHelper(ctx, customClient);

        helper.get("https://httpbin.org/delay/1")
              .send()
              .expectStatus(200)
              .documentResponse();
    }

    /**
     * Demonstrates response body validation.
     *
     * <p>This example shows different ways to validate response content:</p>
     * <ul>
     *   <li>Checking for substring presence</li>
     *   <li>Getting the raw body for custom assertions</li>
     *   <li>Accessing response metadata</li>
     * </ul>
     */
    @Test
    void testResponseValidation(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Response Validation Example");
        ctx.say("This example demonstrates various response validation techniques.");

        var helper = new HttpDocTestHelper(ctx);

        var response = helper.get("https://httpbin.org/json")
                             .send()
                             .expectStatus(200)
                             .expectBodyContains("slideshow")
                             .documentJson("JSON Response");

        // Access raw response for custom assertions
        String body = response.getBody();
        ctx.sayKeyValue(Map.of(
            "Response length", String.valueOf(body.length()),
            "Contains JSON", String.valueOf(body.contains("{"))
        ));
    }

    /**
     * Demonstrates multiple headers.
     *
     * <p>This example shows how to set multiple headers at once
     * using the headers() method.</p>
     */
    @Test
    void testMultipleHeaders(DtrContext ctx) throws Exception {
        ctx.sayNextSection("Multiple Headers Example");
        ctx.say("This example demonstrates setting multiple headers at once.");

        var helper = new HttpDocTestHelper(ctx);

        helper.post("https://httpbin.org/post")
              .headers(Map.of(
                  "Content-Type", "application/json",
                  "X-Custom-Header", "custom-value",
                  "User-Agent", "HttpDocTestHelper/1.0"
              ))
              .body("{\"test\": true}")
              .send()
              .expectStatus(200)
              .documentResponse();
    }

    /**
     * Demonstrates PUT and DELETE requests.
     *
     * <p>This example shows the full range of HTTP methods supported:</p>
     * <ul>
     *   <li>PUT (for updating resources)</li>
     *   <li>DELETE (for removing resources)</li>
     *   <li>PATCH (for partial updates)</li>
     * </ul>
     */
    @Test
    void testPutAndDelete(DtrContext ctx) throws Exception {
        ctx.sayNextSection("PUT and DELETE Request Examples");
        ctx.say("This example demonstrates PUT and DELETE requests.");

        var helper = new HttpDocTestHelper(ctx);

        // PUT request
        ctx.say("PUT request to update a resource:");
        helper.put("https://httpbin.org/put")
              .header("Content-Type", "application/json")
              .body("{\"updated\": true}")
              .send()
              .expectStatus(200)
              .documentResponse();

        // DELETE request
        ctx.say("DELETE request to remove a resource:");
        helper.delete("https://httpbin.org/delete")
              .send()
              .expectStatus(200)
              .documentResponse();

        // PATCH request
        ctx.say("PATCH request for partial update:");
        helper.patch("https://httpbin.org/patch")
              .header("Content-Type", "application/json")
              .body("{\"patched\": true}")
              .send()
              .expectStatus(200)
              .documentResponse();
    }
}
