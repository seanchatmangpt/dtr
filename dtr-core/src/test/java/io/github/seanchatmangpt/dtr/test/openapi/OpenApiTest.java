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
package io.github.seanchatmangpt.dtr.openapi;

import org.junit.jupiter.api.Test;
import io.github.seanchatmangpt.dtr.sse.SseEvent;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OpenAPI output support.
 */
class OpenApiTest {

    @Test
    void openApiSpec_canBeCreated() {
        var spec = OpenApiSpec.create("Test API", "1.0.0");

        assertNotNull(spec);
        assertEquals("3.1.0", spec.openapi());
        assertEquals("Test API", spec.info().title());
        assertEquals("1.0.0", spec.info().version());
    }

    @Test
    void openApiSpec_canAddPaths() {
        var spec = OpenApiSpec.create("Test API", "1.0.0");

        var operation = new OpenApiSpec.Operation(
            "Get users",
            "Returns a list of users",
            new java.util.LinkedHashMap<>(),
            null,
            new java.util.LinkedHashMap<>()
        );

        var updatedSpec = spec.addPath("/api/users", "get", operation);

        assertTrue(updatedSpec.paths().containsKey("/api/users"));
    }

    @Test
    void outputFormat_hasCorrectExtensions() {
        assertEquals("json", OutputFormat.OPENAPI_JSON.getExtension());
        assertEquals("yaml", OutputFormat.OPENAPI_YAML.getExtension());
        assertEquals("md", OutputFormat.MARKDOWN.getExtension());
    }

    @Test
    void outputFormat_hasCorrectContentTypes() {
        assertEquals("application/json", OutputFormat.OPENAPI_JSON.getContentType());
        assertEquals("application/x-yaml", OutputFormat.OPENAPI_YAML.getContentType());
        assertEquals("text/markdown", OutputFormat.MARKDOWN.getContentType());
    }

    @Test
    void openApiCollector_canRecordInteractions() {
        var collector = new OpenApiCollector("Test API", "1.0.0");

        assertEquals(0, collector.size());
    }

    @Test
    void sseEvent_canBeCreated() {
        var event = SseEvent.of("test data");

        assertTrue(event.id().isEmpty());
        assertTrue(event.event().isEmpty());
        assertEquals("test data", event.data());
        assertNotNull(event.timestamp());
    }

    @Test
    void sseEvent_canHaveEventType() {
        var event = SseEvent.of("custom-event", "test data");

        assertTrue(event.id().isEmpty());
        assertTrue(event.event().isPresent());
        assertEquals("custom-event", event.event().get());
        assertEquals("test data", event.data());
    }

    @Test
    void sseEvent_canHaveAllFields() {
        var event = SseEvent.of("id-123", "custom-event", "test data");

        assertTrue(event.id().isPresent());
        assertEquals("id-123", event.id().get());
        assertTrue(event.event().isPresent());
        assertEquals("custom-event", event.event().get());
        assertEquals("test data", event.data());
    }
}
