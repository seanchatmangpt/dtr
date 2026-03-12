/**
 * Copyright (C) 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.github.seanchatmangpt.dtr.testbrowser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import org.apache.hc.core5.http.ParseException;
import java.util.Map.Entry;

import static io.github.seanchatmangpt.dtr.testbrowser.HttpConstants.*;

/**
 * Default HTTP test browser implementation using Apache HttpClient 5.
 *
 * <p>Provides a stateful HTTP client for making requests to test servers with
 * automatic cookie persistence, multipart file upload support, JSON/XML serialization,
 * and flexible request/response handling.</p>
 *
 * <p><strong>Features:</strong></p>
 * <ul>
 *   <li>Cookie jar persistence across requests</li>
 *   <li>All HTTP methods: GET, HEAD, POST, PUT, PATCH, DELETE</li>
 *   <li>Automatic redirect following (configurable per-request)</li>
 *   <li>Multipart form data and file uploads</li>
 *   <li>Form-encoded and JSON/XML payloads</li>
 *   <li>Custom header support with fluent API</li>
 *   <li>Response parsing to JSON/XML POJOs or TypeReferences</li>
 * </ul>
 *
 * <p><strong>Usage:</strong></p>
 * <pre>{@code
 * TestBrowser browser = new TestBrowserImpl();
 *
 * // GET request
 * Response resp1 = browser.makeRequest(
 *     Request.GET().url(Url.host("http://api.example.com").path("/users")));
 *
 * // POST with JSON payload
 * Response resp2 = browser.makeRequest(
 *     Request.POST()
 *         .url(Url.host("http://api.example.com").path("/users"))
 *         .contentTypeApplicationJson()
 *         .payload(new User("alice", "alice@example.com")));
 *
 * // Parse response
 * User user = resp2.payloadAs(User.class);
 *
 * // Cookies are automatically persisted
 * List<Cookie> cookies = browser.getCookies();
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong></p>
 * <p>The underlying {@link CloseableHttpClient} is thread-safe, but instances of
 * TestBrowserImpl should typically be used by a single thread per test method.</p>
 *
 * @see Request for request builder API
 * @see Response for response parsing methods
 * @see TestBrowser for interface contract
 * @since 1.0.0
 */
public class TestBrowserImpl implements TestBrowser {

    private static Logger logger = LoggerFactory.getLogger(TestBrowserImpl.class);

    private CloseableHttpClient httpClient;
    private BasicCookieStore cookieStore;

    /**
     * Creates a new TestBrowserImpl with a fresh HTTP client and empty cookie store.
     *
     * <p>Each test method should typically get its own TestBrowserImpl instance
     * to ensure cookie isolation between tests.</p>
     */
    public TestBrowserImpl() {
        cookieStore = new BasicCookieStore();
        httpClient = HttpClientBuilder.create()
                .setDefaultCookieStore(cookieStore)
                .build();
    }

    @Override
    public List<Cookie> getCookies() {
        return cookieStore.getCookies();
    }

    @Override
    public Cookie getCookieWithName(String name) {

        List<Cookie> cookies = getCookies();

        // skip through cookies and return cookie you want
        for (Cookie cookie : cookies) {
            if (cookie.getName().equals(name)) {
                return cookie;
            }
        }

        return null;
    }

    @Override
    public void clearCookies() {
        cookieStore.clear();
    }

    @Override
    public Response makeRequest(Request httpRequest) {

        Response httpResponse;

        if (Set.of(HEAD, GET, DELETE).contains(httpRequest.httpRequestType)) {

            httpResponse = makeHeadGetOrDeleteRequest(httpRequest);

        } else if (Set.of(POST, PUT, PATCH).contains(httpRequest.httpRequestType)) {

            httpResponse = makePatchPostOrPutRequest(httpRequest);

        } else {

            throw new IllegalArgumentException("HTTP request type '" + httpRequest.httpRequestType + "' is not supported");
        }

        return httpResponse;

    }

    private Response makeHeadGetOrDeleteRequest(Request request) {

        Response response;

        ClassicHttpResponse apacheHttpClientResponse;

        try {

            ClassicHttpRequest apacheHttpRequest;

            if (GET.equalsIgnoreCase(request.httpRequestType)) {

                apacheHttpRequest = new HttpGet(request.uri);

            } else if (DELETE.equalsIgnoreCase(request.httpRequestType)) {

                apacheHttpRequest = new HttpDelete(request.uri);

            } else {

                apacheHttpRequest = new HttpHead(request.uri);

            }

            if (request.headers != null) {

                // add all headers
                for (Entry<String, String> header : request.headers
                        .entrySet()) {
                    apacheHttpRequest.addHeader(header.getKey(), header.getValue());
                }

            }

            HttpClientContext context = createHttpContext(request.followRedirects);

            apacheHttpClientResponse = httpClient.execute(apacheHttpRequest, context);

            response = convertFromApacheHttpResponseToDtrHttpResponse(apacheHttpClientResponse);

            EntityUtils.consumeQuietly(apacheHttpClientResponse.getEntity());

        } catch (IOException e) {
            logger.error("Fatal problem creating GET or DELETE request in TestBrowser", e);
            throw new RuntimeException(e);
        }

        return response;
    }

    private Response makePatchPostOrPutRequest(Request httpRequest) {

        ClassicHttpResponse apacheHttpClientResponse;
        Response response = null;

        try {

            ClassicHttpRequest apacheHttpRequest;

            if (PATCH.equalsIgnoreCase(httpRequest.httpRequestType)) {

                apacheHttpRequest = new io.github.seanchatmangpt.dtr.testbrowser.HttpPatch(httpRequest.uri);

            } else if (POST.equalsIgnoreCase(httpRequest.httpRequestType)) {

                apacheHttpRequest = new HttpPost(httpRequest.uri);

            } else {

                apacheHttpRequest = new HttpPut(httpRequest.uri);
            }

            if (httpRequest.headers != null) {
                // add all headers
                for (Entry<String, String> header : httpRequest.headers
                        .entrySet()) {
                    apacheHttpRequest.addHeader(header.getKey(), header.getValue());
                }
            }

            ///////////////////////////////////////////////////////////////////
            // Either add form parameters...
            ///////////////////////////////////////////////////////////////////
            if (httpRequest.formParameters != null) {

                List<NameValuePair> formparams = new ArrayList<>();
                for (Entry<String, String> parameter : httpRequest.formParameters
                        .entrySet()) {

                    formparams.add(new BasicNameValuePair(parameter.getKey(),
                            parameter.getValue()));
                }

                // encode form parameters and add
                UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, StandardCharsets.UTF_8);
                apacheHttpRequest.setEntity(entity);

            }

            ///////////////////////////////////////////////////////////////////
            // Or add multipart file upload
            ///////////////////////////////////////////////////////////////////
            if (httpRequest.filesToUpload != null) {

                MultipartEntityBuilder builder = MultipartEntityBuilder.create();

                for (Map.Entry<String, File> entry : httpRequest.filesToUpload
                        .entrySet()) {

                    // For File parameters
                    builder.addPart(entry.getKey(),
                            new FileBody(entry.getValue()));

                }

                apacheHttpRequest.setEntity(builder.build());

            }

            ///////////////////////////////////////////////////////////////////
            // Or add payload and convert if Json or Xml
            ///////////////////////////////////////////////////////////////////
            if (httpRequest.payload != null) {

                if (httpRequest.headers.containsKey(HEADER_CONTENT_TYPE)
                        && httpRequest.headers.containsValue(APPLICATION_JSON_WITH_CHARSET_UTF8)) {

                    String string = new ObjectMapper().writeValueAsString(httpRequest.payload);

                    StringEntity entity = new StringEntity(string, ContentType.APPLICATION_JSON.withCharset(StandardCharsets.UTF_8));

                    apacheHttpRequest.setEntity(entity);

                } else if (httpRequest.headers.containsKey(HEADER_CONTENT_TYPE)
                        && httpRequest.headers.containsValue(APPLICATION_XML_WITH_CHARSET_UTF_8)) {

                    String string = new XmlMapper().writeValueAsString(httpRequest.payload);

                    StringEntity entity = new StringEntity(string, ContentType.APPLICATION_XML.withCharset(StandardCharsets.UTF_8));

                    apacheHttpRequest.setEntity(entity);

                } else if (httpRequest.payload instanceof String) {

                    StringEntity entity = new StringEntity((String) httpRequest.payload, StandardCharsets.UTF_8);
                    apacheHttpRequest.setEntity(entity);

                } else {

                    StringEntity entity = new StringEntity(httpRequest.payload.toString(), StandardCharsets.UTF_8);
                    apacheHttpRequest.setEntity(entity);

                }

            }

            HttpClientContext context = createHttpContext(httpRequest.followRedirects);

            // Here we go!
            apacheHttpClientResponse = httpClient.execute(apacheHttpRequest, context);
            response = convertFromApacheHttpResponseToDtrHttpResponse(apacheHttpClientResponse);

            EntityUtils.consumeQuietly(apacheHttpClientResponse.getEntity());

        } catch (IOException e) {
            logger.error("Fatal problem creating PATCH, POST or PUT request in TestBrowser", e);
            throw new RuntimeException(e);
        }

        return response;

    }

    private io.github.seanchatmangpt.dtr.testbrowser.Response convertFromApacheHttpResponseToDtrHttpResponse(ClassicHttpResponse httpResponse) {

        Map<String, String> headers = new HashMap<>();

        for (Header header : httpResponse.getHeaders()) {

            headers.put(header.getName(), header.getValue());

        }

        int httpStatus = httpResponse.getCode();

        String body = null;
        HttpEntity entity = httpResponse.getEntity();
        if (entity != null) {
            try {

                body = EntityUtils.toString(entity, StandardCharsets.UTF_8);

            } catch (IOException | ParseException e) {
                logger.error("Error while converting ApacheHttpClient response body to a String we can use", e);
            }
        }
        io.github.seanchatmangpt.dtr.testbrowser.Response doctestJHttpResponse = new io.github.seanchatmangpt.dtr.testbrowser.Response(
                headers, httpStatus, body);

        return doctestJHttpResponse;

    }

    /**
     * Creates an HttpClientContext with redirect configuration.
     */
    private HttpClientContext createHttpContext(boolean followRedirects) {
        HttpClientContext context = new HttpClientContext();
        RequestConfig config = RequestConfig.custom()
                .setRedirectsEnabled(followRedirects)
                .build();
        context.setRequestConfig(config);
        return context;
    }

}
