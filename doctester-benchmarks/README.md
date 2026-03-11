# DocTester Benchmarks Module

Performance benchmarks for DocTester using JMH (Java Microbenchmark Harness).

## Overview

The benchmarks module contains comprehensive performance tests separated from unit tests to allow:
- Detailed performance profiling and monitoring
- Resource contention scenario testing
- Large file and concurrent operation load testing
- Regression detection via reproducible benchmark runs

## Benchmark Categories

### 1. Large File Benchmarks (`LargeFileBenchmark.java`)
Tests streaming performance with files of varying sizes:
- **100MB file streaming** - Basic large file handling
- **500MB file streaming** - Scale testing

Key metrics:
- Time to process file (milliseconds)
- Memory efficiency (streaming vs loading)
- I/O throughput

**Run:** `java -jar target/benchmarks.jar LargeFileBenchmark`

### 2. Many Files Directory Benchmarks (`ManyFilesDirectoryBenchmark.java`)
Tests directory traversal and enumeration with large file counts:
- **1000-file enumeration** - Standard bulk operation
- **10000-file enumeration** - Heavy load test

Key metrics:
- Enumeration time (milliseconds)
- Files processed per second
- Memory footprint per file

**Run:** `java -jar target/benchmarks.jar ManyFilesDirectoryBenchmark`

### 3. Concurrent Rendering Benchmarks (`ConcurrentRenderingBenchmark.java`)
Tests multi-format parallel rendering:
- **Parallel document sections** - Thread-safe say() calls
- **Concurrent table rendering** - Shared buffer safety
- **Mixed concurrent operations** - Real-world load

Key metrics:
- Average time per operation (milliseconds)
- Throughput under concurrency
- Race condition detection

**Run:** `java -jar target/benchmarks.jar ConcurrentRenderingBenchmark -t 4`

## Building

```bash
# Build benchmarks module with all dependencies
mvnd clean package -pl doctester-benchmarks -am

# This produces: target/benchmarks.jar (fat JAR, all dependencies included)
```

## Running Benchmarks

### Quick Run (All Benchmarks)
```bash
java -jar target/benchmarks.jar
```

### Run Specific Benchmark Class
```bash
java -jar target/benchmarks.jar LargeFileBenchmark
java -jar target/benchmarks.jar ManyFilesDirectoryBenchmark
java -jar target/benchmarks.jar ConcurrentRenderingBenchmark
```

### Run with Custom Parameters
```bash
# Increase threads for concurrent tests
java -jar target/benchmarks.jar ConcurrentRenderingBenchmark -t 8

# Run specific benchmark method only
java -jar target/benchmarks.jar LargeFileBenchmark.benchmark100MBFileStreaming

# Custom warmup/measurement iterations
java -jar target/benchmarks.jar -w 5 -i 10 -f 2
```

### Save Results to File
```bash
java -jar target/benchmarks.jar -rf json -rff results.json
java -jar target/benchmarks.jar -rf csv -rff results.csv
```

## Interpreting Results

Each benchmark output includes:
- **Mode**: AverageTime (milliseconds per operation)
- **Score**: The measured metric value
- **Error**: Statistical error margin (+/- ms)
- **Units**: TimeUnit (typically milliseconds)

Example output:
```
Benchmark                                           Mode  Cnt   Score   Error  Units
LargeFileBenchmark.benchmark100MBFileStreaming      avgt    3  245.123 ±12.4  ms/op
LargeFileBenchmark.benchmark500MBFileStreaming      avgt    3  1089.45 ±25.6  ms/op
```

Lower scores are better (faster operations).

## Differences from Unit Tests (test_cli_stress.py)

| Aspect | Unit Tests | Benchmarks |
|--------|-----------|-----------|
| **Purpose** | Functional verification | Performance monitoring |
| **Coverage** | Essential operations only | Detailed variations and edge cases |
| **Location** | doctester-cli/tests/ | doctester-benchmarks/ |
| **Framework** | pytest | JMH |
| **Language** | Python | Java |
| **Run Time** | <30 seconds | Variable (typically 1-5 min) |
| **Scope** | 5 consolidated tests | 10+ detailed benchmarks |

## Performance Targets

Expected performance ranges (for reference):
- **100MB file streaming**: 200-300ms
- **500MB file streaming**: 1000-1200ms
- **1000-file enumeration**: 10-50ms
- **10000-file enumeration**: 100-300ms
- **Parallel rendering (4 threads)**: 10-30ms per operation

## Customizing Benchmarks

To add new benchmarks:

1. Create a new benchmark class in `src/jmh/java/org/r10r/doctester/`
2. Add JMH annotations:
   ```java
   @Benchmark
   public void myBenchmark() {
       // Benchmark code here
   }
   ```
3. Build and run: `mvnd clean package -pl doctester-benchmarks && java -jar target/benchmarks.jar`

## Dependencies

- **JMH 1.37**: Harness for writing and running benchmarks
- **DocTester Core 2.5.0-SNAPSHOT**: Core library being benchmarked
- **Java 25**: With `--enable-preview` flag

## Troubleshooting

**Q: "Cannot find symbol" errors when compiling?**
A: Run `mvnd clean package -pl doctester-benchmarks -am` to build with dependencies.

**Q: Results vary between runs?**
A: JMH includes warmup iterations to stabilize JIT compilation. Variance is normal; compare averages across multiple runs.

**Q: How do I compare benchmark results?**
A: Save results as JSON:
   ```bash
   java -jar target/benchmarks.jar -rf json -rff baseline.json
   java -jar target/benchmarks.jar -rf json -rff current.json
   ```
   Then compare `baseline.json` and `current.json` using diff tools or custom scripts.

## Further Reading

- JMH User Guide: https://github.com/openjdk/jmh/wiki/Userguide
- JMH Samples: https://github.com/openjdk/jmh/tree/master/jmh-samples
- Java 25 Preview Features: https://openjdk.org/projects/jdk/25/
