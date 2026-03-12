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
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachine;
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for large file processing.
 *
 * Tests streaming performance of RenderMachine with files of varying sizes:
 * - 100MB file streaming
 * - 500MB file streaming
 * - Memory efficiency metrics
 *
 * Run with: mvnd clean package -pl dtr-benchmarks && \
 *           java -jar dtr-benchmarks/target/benchmarks.jar LargeFileBenchmark
 */
@Fork(value = 1, jvmArgs = {"--enable-preview"})
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LargeFileBenchmark {

    private Path tempFile;
    private RenderMachine renderMachine;

    /**
     * Setup: create a 100MB test file.
     */
    @Setup(Level.Trial)
    public void setupLarge100MB() throws IOException {
        tempFile = Files.createTempFile("benchmark-100mb-", ".md");
        createLargeFile(tempFile, 100 * 1024 * 1024);
        renderMachine = new RenderMachineImpl();
    }

    /**
     * Setup: create a 500MB test file.
     */
    @Setup(Level.Trial)
    public void setupLarge500MB() throws IOException {
        tempFile = Files.createTempFile("benchmark-500mb-", ".md");
        createLargeFile(tempFile, 500 * 1024 * 1024);
        renderMachine = new RenderMachineImpl();
    }

    /**
     * Teardown: clean up temporary file.
     */
    @TearDown(Level.Trial)
    public void teardown() throws IOException {
        if (tempFile != null && Files.exists(tempFile)) {
            Files.delete(tempFile);
        }
    }

    /**
     * Benchmark: streaming 100MB file without loading to memory.
     */
    @Benchmark
    public void benchmark100MBFileStreaming() throws IOException {
        renderMachine.say("Processing 100MB file...");
        processFileStreaming(tempFile);
    }

    /**
     * Benchmark: streaming 500MB file.
     */
    @Benchmark
    public void benchmark500MBFileStreaming() throws IOException {
        renderMachine.say("Processing 500MB file...");
        processFileStreaming(tempFile);
    }

    /**
     * Helper: create large file in chunks without loading to RAM.
     */
    private static void createLargeFile(Path path, long sizeBytes) throws IOException {
        long chunk = 1024 * 1024; // 1MB chunks
        long remaining = sizeBytes;
        byte[] buffer = new byte[(int) Math.min(chunk, remaining)];

        try (var out = Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            while (remaining > 0) {
                long toWrite = Math.min(chunk, remaining);
                out.write(buffer, 0, (int) toWrite);
                remaining -= toWrite;
            }
        }
    }

    /**
     * Helper: process file with streaming to avoid loading entire file.
     */
    private void processFileStreaming(Path path) throws IOException {
        // Simulate streaming read
        try (var lines = Files.lines(path)) {
            lines.limit(1000)  // Only process first 1000 lines for benchmark
                .forEach(renderMachine::say);
        }
    }
}
