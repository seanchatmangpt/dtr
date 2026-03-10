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
package org.r10r.doctester.testbrowser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.common.collect.Sets;
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
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import static org.r10r.doctester.testbrowser.HttpConstants.*;

public class TestBrowserImpl implements TestBrowser {

    private static Logger logger = LoggerFactory.getLogger(TestBrowserImpl.class);

    private CloseableHttpClient httpClient;
    private BasicCookieStore cookieStore;

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

        if (Sets.newHashSet(HEAD, GET, DELETE).contains(httpRequest.httpRequestType)) {

            httpResponse = makeHeadGetOrDeleteRequest(httpRequest);

        } else if (Sets.newHashSet(POST, PUT, PATCH).contains(httpRequest.httpRequestType)) {

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

            response = convertFromApacheHttpResponseToDocTesterHttpResponse(apacheHttpClientResponse);

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

                apacheHttpRequest = new org.r10r.doctester.testbrowser.HttpPatch(httpRequest.uri);

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
            response = convertFromApacheHttpResponseToDocTesterHttpResponse(apacheHttpClientResponse);

            EntityUtils.consumeQuietly(apacheHttpClientResponse.getEntity());

        } catch (IOException e) {
            logger.error("Fatal problem creating PATCH, POST or PUT request in TestBrowser", e);
            throw new RuntimeException(e);
        }

        return response;

    }

    private org.r10r.doctester.testbrowser.Response convertFromApacheHttpResponseToDocTesterHttpResponse(ClassicHttpResponse httpResponse) {

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
        org.r10r.doctester.testbrowser.Response doctestJHttpResponse = new org.r10r.doctester.testbrowser.Response(
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
