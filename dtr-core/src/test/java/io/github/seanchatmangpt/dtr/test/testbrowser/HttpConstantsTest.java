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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link HttpConstants}.
 */
@DisplayName("HttpConstants")
class HttpConstantsTest {

    @Nested
    @DisplayName("Header Constants")
    class HeaderConstantsTests {

        @Test
        @DisplayName("HEADER_ACCEPT should be 'Accept'")
        void headerAccept_shouldBeAccept() {
            assertEquals("Accept", HttpConstants.HEADER_ACCEPT);
        }

        @Test
        @DisplayName("HEADER_ACCEPT should not be null or empty")
        void headerAccept_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.HEADER_ACCEPT);
            assertFalse(HttpConstants.HEADER_ACCEPT.isEmpty());
        }

        @Test
        @DisplayName("HEADER_CONTENT_TYPE should be 'Content-Type'")
        void headerContentType_shouldBeContentType() {
            assertEquals("Content-Type", HttpConstants.HEADER_CONTENT_TYPE);
        }

        @Test
        @DisplayName("HEADER_CONTENT_TYPE should not be null or empty")
        void headerContentType_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.HEADER_CONTENT_TYPE);
            assertFalse(HttpConstants.HEADER_CONTENT_TYPE.isEmpty());
        }
    }

    @Nested
    @DisplayName("Content Type Constants")
    class ContentTypeConstantsTests {

        @Test
        @DisplayName("APPLICATION_JSON should be 'application/json'")
        void applicationJson_shouldBeApplicationJson() {
            assertEquals("application/json", HttpConstants.APPLICATION_JSON);
        }

        @Test
        @DisplayName("APPLICATION_JSON should not be null or empty")
        void applicationJson_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.APPLICATION_JSON);
            assertFalse(HttpConstants.APPLICATION_JSON.isEmpty());
        }

        @Test
        @DisplayName("APPLICATION_JSON_WITH_CHARSET_UTF8 should be 'application/json; charset=utf-8'")
        void applicationJsonWithCharsetUtf8_shouldBeCorrectValue() {
            assertEquals("application/json; charset=utf-8", HttpConstants.APPLICATION_JSON_WITH_CHARSET_UTF8);
        }

        @Test
        @DisplayName("APPLICATION_JSON_WITH_CHARSET_UTF8 should not be null or empty")
        void applicationJsonWithCharsetUtf8_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.APPLICATION_JSON_WITH_CHARSET_UTF8);
            assertFalse(HttpConstants.APPLICATION_JSON_WITH_CHARSET_UTF8.isEmpty());
        }

        @Test
        @DisplayName("APPLICATION_XML should be 'application/xml'")
        void applicationXml_shouldBeApplicationXml() {
            assertEquals("application/xml", HttpConstants.APPLICATION_XML);
        }

        @Test
        @DisplayName("APPLICATION_XML should not be null or empty")
        void applicationXml_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.APPLICATION_XML);
            assertFalse(HttpConstants.APPLICATION_XML.isEmpty());
        }

        @Test
        @DisplayName("APPLICATION_XML_WITH_CHARSET_UTF_8 should be 'application/xml; charset=utf-8'")
        void applicationXmlWithCharsetUtf8_shouldBeCorrectValue() {
            assertEquals("application/xml; charset=utf-8", HttpConstants.APPLICATION_XML_WITH_CHARSET_UTF_8);
        }

        @Test
        @DisplayName("APPLICATION_XML_WITH_CHARSET_UTF_8 should not be null or empty")
        void applicationXmlWithCharsetUtf8_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.APPLICATION_XML_WITH_CHARSET_UTF_8);
            assertFalse(HttpConstants.APPLICATION_XML_WITH_CHARSET_UTF_8.isEmpty());
        }
    }

    @Nested
    @DisplayName("HTTP Method Constants")
    class HttpMethodConstantsTests {

        @Test
        @DisplayName("HEAD should be 'HEAD'")
        void head_shouldBeHead() {
            assertEquals("HEAD", HttpConstants.HEAD);
        }

        @Test
        @DisplayName("HEAD should not be null or empty")
        void head_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.HEAD);
            assertFalse(HttpConstants.HEAD.isEmpty());
        }

        @Test
        @DisplayName("HEAD should be uppercase")
        void head_shouldBeUppercase() {
            assertEquals(HttpConstants.HEAD.toUpperCase(), HttpConstants.HEAD);
        }

        @Test
        @DisplayName("GET should be 'GET'")
        void get_shouldBeGet() {
            assertEquals("GET", HttpConstants.GET);
        }

        @Test
        @DisplayName("GET should not be null or empty")
        void get_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.GET);
            assertFalse(HttpConstants.GET.isEmpty());
        }

        @Test
        @DisplayName("GET should be uppercase")
        void get_shouldBeUppercase() {
            assertEquals(HttpConstants.GET.toUpperCase(), HttpConstants.GET);
        }

        @Test
        @DisplayName("DELETE should be 'DELETE'")
        void delete_shouldBeDelete() {
            assertEquals("DELETE", HttpConstants.DELETE);
        }

        @Test
        @DisplayName("DELETE should not be null or empty")
        void delete_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.DELETE);
            assertFalse(HttpConstants.DELETE.isEmpty());
        }

        @Test
        @DisplayName("DELETE should be uppercase")
        void delete_shouldBeUppercase() {
            assertEquals(HttpConstants.DELETE.toUpperCase(), HttpConstants.DELETE);
        }

        @Test
        @DisplayName("POST should be 'POST'")
        void post_shouldBePost() {
            assertEquals("POST", HttpConstants.POST);
        }

        @Test
        @DisplayName("POST should not be null or empty")
        void post_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.POST);
            assertFalse(HttpConstants.POST.isEmpty());
        }

        @Test
        @DisplayName("POST should be uppercase")
        void post_shouldBeUppercase() {
            assertEquals(HttpConstants.POST.toUpperCase(), HttpConstants.POST);
        }

        @Test
        @DisplayName("PUT should be 'PUT'")
        void put_shouldBePut() {
            assertEquals("PUT", HttpConstants.PUT);
        }

        @Test
        @DisplayName("PUT should not be null or empty")
        void put_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.PUT);
            assertFalse(HttpConstants.PUT.isEmpty());
        }

        @Test
        @DisplayName("PUT should be uppercase")
        void put_shouldBeUppercase() {
            assertEquals(HttpConstants.PUT.toUpperCase(), HttpConstants.PUT);
        }

        @Test
        @DisplayName("PATCH should be 'PATCH'")
        void patch_shouldBePatch() {
            assertEquals("PATCH", HttpConstants.PATCH);
        }

        @Test
        @DisplayName("PATCH should not be null or empty")
        void patch_shouldNotBeNullOrEmpty() {
            assertNotNull(HttpConstants.PATCH);
            assertFalse(HttpConstants.PATCH.isEmpty());
        }

        @Test
        @DisplayName("PATCH should be uppercase")
        void patch_shouldBeUppercase() {
            assertEquals(HttpConstants.PATCH.toUpperCase(), HttpConstants.PATCH);
        }
    }
}
