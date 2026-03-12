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
import io.github.seanchatmangpt.dtr.assembly.DocumentAssembler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks for directory traversal with many files.
 *
 * Tests performance of enumerating and processing large numbers of files:
 * - 1000 file directory enumeration
 * - 10000 file directory enumeration
 * - Memory and I/O efficiency
 *
 * Run with: mvnd clean package -pl dtr-benchmarks && \
 *           java -jar dtr-benchmarks/target/benchmarks.jar ManyFilesDirectoryBenchmark
 */
@Fork(value = 1, jvmArgs = {"--enable-preview"})
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ManyFilesDirectoryBenchmark {

    private Path tempDir;
    private DocumentAssembler assembler;

    /**
     * Setup: create 1000 markdown files in temporary directory.
     */
    @Setup(Level.Trial)
    public void setup1000Files() throws IOException {
        tempDir = Files.createTempDirectory("benchmark-1k-");
        createTestFiles(tempDir, 1000);
        assembler = new DocumentAssembler();
    }

    /**
     * Setup: create 10000 markdown files (heavy load test).
     */
    @Setup(Level.Trial)
    public void setup10000Files() throws IOException {
        tempDir = Files.createTempDirectory("benchmark-10k-");
        createTestFiles(tempDir, 10000);
        assembler = new DocumentAssembler();
    }

    /**
     * Teardown: clean up temporary directory and files.
     */
    @TearDown(Level.Trial)
    public void teardown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a))  // Delete deepest first
                      .forEach(path -> {
                          try {
                              Files.delete(path);
                          } catch (IOException e) {
                              // Ignore in cleanup
                          }
                      });
            }
        }
    }

    /**
     * Benchmark: enumerate 1000 files in single directory.
     */
    @Benchmark
    public long benchmark1000FilesEnumeration() throws IOException {
        try (var stream = Files.list(tempDir)) {
            return stream.count();
        }
    }

    /**
     * Benchmark: enumerate 10000 files in single directory.
     */
    @Benchmark
    public long benchmark10000FilesEnumeration() throws IOException {
        try (var stream = Files.list(tempDir)) {
            return stream.count();
        }
    }

    /**
     * Benchmark: read metadata from 1000 files.
     */
    @Benchmark
    public long benchmark1000FilesMetadataRead() throws IOException {
        try (var stream = Files.list(tempDir)) {
            return stream.map(path -> {
                try {
                    return Files.size(path);
                } catch (IOException e) {
                    return 0L;
                }
            }).count();
        }
    }

    /**
     * Helper: create N test markdown files with unique content.
     */
    private static void createTestFiles(Path dir, int count) throws IOException {
        for (int i = 0; i < count; i++) {
            Path file = dir.resolve(String.format("doc_%06d.md", i));
            String content = """
                # Document %d

                This is test document number %d.

                ## Section 1
                Content for section 1.

                ## Section 2
                Content for section 2.
                """.formatted(i, i);
            Files.writeString(file, content);
        }
    }
}
