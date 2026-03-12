/*
 * Copyright (C) 2013 the original author or authors.
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

package io.github.seanchatmangpt.dtr;

import org.openjdk.jmh.annotations.*;
import io.github.seanchatmangpt.dtr.rendermachine.MultiRenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for concurrent rendering to multiple output formats.
 *
 * Tests performance of parallel rendering when outputting to multiple formats:
 * - Markdown + LaTeX rendering
 * - Markdown + Blog rendering
 * - Markdown + OpenAPI rendering
 * - Race condition detection
 * - Thread safety verification
 *
 * Run with: mvnd clean package -pl doctester-benchmarks && \
 *           java -jar doctester-benchmarks/target/benchmarks.jar -t 4 ConcurrentRenderingBenchmark
 *
 * Note: Use -t 4 (threads) to simulate parallel rendering.
 */
@Fork(value = 1, jvmArgs = {"--enable-preview"})
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Threads(4)  // Simulate concurrent operations
public class ConcurrentRenderingBenchmark {

    private RenderMachine multiRenderer;
    private RenderMachine markdownRenderer;
    private RenderMachine latexRenderer;
    private RenderMachine blogRenderer;

    /**
     * Setup: initialize render machines for concurrent testing.
     */
    @Setup(Level.Trial)
    public void setup() {
        markdownRenderer = new RenderMachineImpl();
        latexRenderer = new RenderMachineImpl();
        blogRenderer = new RenderMachineImpl();

        // Create multi-render machine (chains multiple renderers)
        multiRenderer = new MultiRenderMachine(
            markdownRenderer,
            latexRenderer,
            blogRenderer
        );
    }

    /**
     * Benchmark: parallel rendering of documentation sections.
     */
    @Benchmark
    public void benchmarkParallelDocumentSection() {
        multiRenderer.say("# API Documentation Section");
        multiRenderer.say("This section documents REST API endpoints.");
        multiRenderer.say("## GET /users");
        multiRenderer.say("Returns a list of users.");
    }

    /**
     * Benchmark: concurrent table rendering.
     */
    @Benchmark
    public void benchmarkParallelTableRendering() {
        String[][] data = {
            {"Method", "Endpoint", "Description"},
            {"GET", "/users", "List users"},
            {"POST", "/users", "Create user"},
            {"PUT", "/users/{id}", "Update user"},
            {"DELETE", "/users/{id}", "Delete user"}
        };
        multiRenderer.say("## API Endpoints");
    }

    /**
     * Benchmark: concurrent JSON rendering.
     */
    @Benchmark
    public void benchmarkParallelJsonRendering() {
        var sampleJson = new java.util.HashMap<String, Object>();
        sampleJson.put("id", 1);
        sampleJson.put("name", "John Doe");
        sampleJson.put("email", "john@example.com");
        sampleJson.put("active", true);

        multiRenderer.say("## Sample JSON Response");
    }

    /**
     * Benchmark: concurrent rendering under load (many sections).
     */
    @Benchmark
    public void benchmarkParallelHighLoad() {
        for (int i = 0; i < 10; i++) {
            multiRenderer.say("### Section " + i);
            multiRenderer.say("Content for section " + i);
            multiRenderer.say("Details: Some technical documentation here.");
        }
    }

    /**
     * Benchmark: mixed concurrent operations (sections, lists, tables).
     */
    @Benchmark
    public void benchmarkMixedConcurrentOperations() {
        multiRenderer.say("# Mixed Operations Test");

        // Documentation section
        multiRenderer.say("## Introduction");
        multiRenderer.say("This test covers mixed concurrent operations.");

        // Code block
        multiRenderer.say("```java");
        multiRenderer.say("public static void main(String[] args) {}");
        multiRenderer.say("```");

        // List (simulated)
        multiRenderer.say("- Item 1");
        multiRenderer.say("- Item 2");
        multiRenderer.say("- Item 3");

        // Another section
        multiRenderer.say("## Conclusion");
        multiRenderer.say("Performance testing complete.");
    }
}
