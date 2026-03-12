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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;

/**
 * Immutable HTTP response object from a {@link TestBrowser} request.
 *
 * <p>Contains the HTTP status code, headers, and payload. Provides convenient methods
 * for parsing JSON and XML payloads into Java objects or TypeReferences.</p>
 *
 * <p><strong>Usage Patterns:</strong></p>
 * <pre>{@code
 * Response resp = browser.makeRequest(Request.GET().url(...));
 *
 * // Check status
 * int status = resp.httpStatus;  // e.g., 200
 *
 * // Get raw payload
 * String raw = resp.payloadAsString();
 *
 * // Auto-detect JSON/XML from Content-Type header
 * User user = resp.payloadAs(User.class);
 *
 * // Force JSON parsing
 * User user = resp.payloadJsonAs(User.class);
 *
 * // Force XML parsing
 * User user = resp.payloadXmlAs(User.class);
 *
 * // Parse collections with TypeReference
 * List<User> users = resp.payloadJsonAs(new TypeReference<List<User>>() {});
 *
 * // Pretty-printed payload for documentation
 * String pretty = resp.payloadAsPrettyString();
 * }</pre>
 *
 * <p><strong>Content Type Detection:</strong></p>
 * <p>The {@link #payloadAs(Class)} method inspects the response Content-Type header
 * to determine the payload format:</p>
 * <ul>
 *   <li>{@code application/json} → JSON deserialization</li>
 *   <li>{@code application/xml}, {@code application/xmlObject} → XML deserialization</li>
 *   <li>Other types → returns null and logs error</li>
 * </ul>
 *
 * @author Raphael A. Bauer
 * @since 1.0.0
 * @see Request for request building
 * @see TestBrowser#makeRequest(Request)
 */
public class Response {

    private final Logger logger = LoggerFactory.getLogger(Response.class);

    /** HTTP response headers (Content-Type, Content-Length, etc.). */
    public final Map<String, String> headers;

    /** HTTP status code (200, 404, 500, etc.). */
    public final int httpStatus;

    /** Response body as a raw string (raw JSON, XML, HTML, etc.). */
    public final String payload;

    private final XmlMapper xmlMapper;

    private final ObjectMapper objectMapper;

    /**
     * Creates a new Response object.
     *
     * <p>Initializes Jackson ObjectMapper (for JSON) and XmlMapper (for XML) with
     * sensible defaults. The XML mapper is configured with {@code setDefaultUseWrapper(false)}
     * to produce output similar to JSON deserialization.</p>
     *
     * @param headers the HTTP response headers (Content-Type, Content-Length, etc.)
     * @param httpStatus the HTTP status code (200, 404, 500, etc.)
     * @param payload the raw response body as a string
     */
    public Response(Map<String, String> headers, int httpStatus, String payload) {

        // configure the JacksonXml mapper in a cleaner way...
        JacksonXmlModule module = new JacksonXmlModule();
        // Check out: https://github.com/FasterXML/jackson-dataformat-xml
        // setDefaultUseWrapper produces more similar output to
        // the Json output. You can change that with annotations in your
        // models.
        module.setDefaultUseWrapper(false);
        this.xmlMapper = new XmlMapper(module);

        this.objectMapper = new ObjectMapper();

        this.headers = headers;
        this.httpStatus = httpStatus;
        this.payload = payload;

    }

    /**
     *
     * @return The payload of this response as String. Just the raw String.
     *
     */
    public String payloadAsString() {

        return payload;

    }

    /**
     *
     * @return The payload of this response as String. It tries to determine the
     * content and format the content in a pretty way. Currently works for json.
     *
     */
    public String payloadAsPrettyString() {

        try {
            return PayloadUtils.prettyPrintResponsePayload(payload, headers);
        } catch (IOException ex) {
            logger.error("Something went wrong when pretty printing response payload: " + ex.toString());
            return "Error pretty printing the payload.";
        }
    }

    /**
     * Parses the response payload into a Java object, auto-detecting format from Content-Type header.
     *
     * <p>Inspects the {@code Content-Type} header to determine the payload format and delegates
     * to {@link #payloadJsonAs(Class)} or {@link #payloadXmlAs(Class)} accordingly.
     * If the Content-Type is neither JSON nor XML, returns null and logs an error.</p>
     *
     * <p>For explicit control over the parsing format, use {@link #payloadJsonAs(Class)}
     * or {@link #payloadXmlAs(Class)} directly.</p>
     *
     * @param <T> the target type
     * @param clazz the class to deserialize the response body into (must not be null)
     * @return an instance of the class populated from the response payload,
     *         or null if the content type is unsupported or parsing fails
     * @see #payloadJsonAs(Class) to force JSON parsing
     * @see #payloadXmlAs(Class) to force XML parsing
     */
    public <T> T payloadAs(Class<T> clazz) {

        T parsedBody = null;

        if (PayloadUtils.isContentTypeApplicationXml(headers)) {
            parsedBody = payloadXmlAs(clazz);
        } else if (PayloadUtils.isContentTypeApplicationJson(headers)) {
            parsedBody = payloadJsonAs(clazz);
        } else {
            logger.error("Could neither find application/json or application/xml content type in response. Returning null.");
        }

        return parsedBody;

    }

    /**
     * Parses the response payload as XML into the specified class type.
     *
     * <p>Uses Jackson XmlMapper with {@code setDefaultUseWrapper(false)} for deserialization.
     * Errors during parsing are logged and null is returned.</p>
     *
     * @param <T> the target type
     * @param clazz the class to deserialize the XML payload into (must not be null)
     * @return an instance of clazz populated from the XML payload, or null if parsing fails
     * @see #payloadAs(Class) for automatic format detection
     */
    public <T> T payloadXmlAs(Class<T> clazz) {

        T parsedBody = null;

        try {
            parsedBody = this.xmlMapper.readValue(payload, clazz);

        } catch (Exception e) {

            logger.error("Something went wrong parsing the payload of this response into Xml", e);

        }

        return parsedBody;
    }

    /**
     * Parses the response payload as XML into the specified TypeReference.
     *
     * <p>Allows parsing complex generic types like {@code List<User>} or {@code Map<String, Order>}.
     * Errors during parsing are logged and null is returned.</p>
     *
     * @param <T> the target type
     * @param typeReference the TypeReference describing the target type structure
     *                       (e.g., {@code new TypeReference<List<User>>() {}})
     * @return an instance of the target type populated from the XML payload, or null if parsing fails
     */
    public <T> T payloadXmlAs(TypeReference<T> typeReference) {

        T parsedBody = null;

        try {
            parsedBody = xmlMapper.readValue(payload, typeReference);

        } catch (IOException e) {

            logger.error("Something went wrong parsing the payload of this response into Xml", e);
        }

        return parsedBody;
    }

    /**
     * Parses the response payload as JSON into the specified class type.
     *
     * <p>Uses Jackson ObjectMapper with default configuration. Errors during parsing
     * are logged and null is returned.</p>
     *
     * @param <T> the target type
     * @param clazz the class to deserialize the JSON payload into (must not be null)
     * @return an instance of clazz populated from the JSON payload, or null if parsing fails
     * @see #payloadAs(Class) for automatic format detection
     * @see #payloadJsonAs(TypeReference) for parsing generic types
     */
    public <T> T payloadJsonAs(Class<T> clazz) {

        T parsedBody = null;

        try {
            parsedBody = objectMapper.readValue(payload, clazz);
        } catch (IOException e) {
            logger.error("Something went wrong parsing the payload of this response into Json", e);
        }

        return parsedBody;

    }

    /**
     * Parses the response payload as JSON into the specified TypeReference.
     *
     * <p>Allows parsing complex generic types like {@code List<User>}, {@code Map<String, Order>},
     * or any other generic structure. Example:</p>
     * <pre>{@code
     * List<User> users = response.payloadJsonAs(new TypeReference<List<User>>() {});
     * }</pre>
     * <p>Errors during parsing are logged and null is returned.</p>
     *
     * @param <T> the target type
     * @param typeReference the TypeReference describing the target type structure
     * @return an instance of the target type populated from the JSON payload, or null if parsing fails
     * @see #payloadJsonAs(Class) for parsing non-generic types
     */
    public <T> T payloadJsonAs(TypeReference<T> typeReference) {

        T parsedBody = null;

        try {
            parsedBody = objectMapper.readValue(payload, typeReference);
        } catch (IOException e) {
            logger.error("Something went wrong parsing the payload of this response into Json", e);
        }

        return parsedBody;
    }

}
