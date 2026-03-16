# DTR-025: Expand Virtual Thread Usage for I/O Operations

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,java26,virtual-threads,performance,io

## Description
Expand virtual thread usage beyond current scope to cover more I/O operations throughout DTR. Create centralized VirtualIoExecutor.java for management of virtual thread executors dedicated to I/O-bound tasks. This leverages Java 21+ virtual threads (Project Loom) for improved scalability.

## Acceptance Criteria
- [ ] Create VirtualIoExecutor.java class with centralized virtual thread management
- [ ] Identify additional I/O operations suitable for virtual threads (file I/O, network I/O, doc generation)
- [ ] Migrate identified I/O operations to use VirtualIoExecutor
- [ ] Add benchmarks comparing platform threads vs virtual threads (throughput, latency, thread count)
- [ ] Update documentation with usage examples and best practices
- [ ] All existing tests pass
- [ ] Performance metrics show improvement in high-concurrency scenarios

## Technical Notes
**New File**: `src/main/java/org/sperlogar/dte/concurrent/VirtualIoExecutor.java`

**Executor Pattern**:
```java
// Virtual thread per task executor
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

// Usage
executor.submit(() -> {
    // I/O-bound operation
    Files.readString(path);
    // or network I/O, doc generation, etc.
});
```

**Target Operations**:
- File I/O: `Files.read*()`, `Files.write*()`, file traversal
- Network I/O: HTTP requests (if any), external service calls
- Doc generation: Markdown rendering, slide generation
- Log writing: Asynchronous logging operations

**Benchmarks Required**:
- Measure: Thread count, latency (ms), throughput (ops/sec)
- Compare: Platform threads vs virtual threads under load
- Report: metric + units + Java version + iterations + environment
- Example: "Virtual threads: 10K concurrent I/O ops in 50ms vs platform threads: 500ms"

**Implementation Notes**:
- Use `try-with-resources` for executor lifecycle
- Pinning avoidance: Don't use synchronized blocks or native code in virtual threads
- Monitoring: Add metrics for active virtual thread count

## Dependencies
None - this ticket can be implemented independently

## References
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 451: Prepare to Disallow the Dynamic Loading of Agents](https://openjdk.org/jeps/451)
- [Virtual Threads - Java 21 Guide](https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html)
- [DTR Concurrency Documentation](/Users/sac/dtr/docs/concurrency.md)
- Related: DTR-023 (Native CodeReflection), DTR-024 (Pattern Matching)
