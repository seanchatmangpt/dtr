# Performance Guide

DocTester CLI is designed for speed and efficiency. This guide explains how DocTester's performance features work and how to optimize your workflows.

## Overview

DocTester uses several strategies to keep performance high:

- **Parallel Processing** — Convert multiple files simultaneously
- **Caching** — Avoid redundant conversions
- **Incremental Builds** — Only process changed files
- **Memory Efficiency** — Stream large files instead of loading entirely
- **Smart Defaults** — Fast conversion without complex configuration

## Performance Benchmarks

Typical performance on a modern machine (Intel i7, 16GB RAM):

| Task | File Size | Time | Notes |
|------|-----------|------|-------|
| Markdown → HTML | 10 KB | <100ms | Simple conversion |
| Markdown → HTML | 1 MB | ~500ms | Large document |
| Markdown → PDF | 100 KB | ~1-2s | Includes LaTeX compilation |
| Batch (100 files) | 1 MB total | ~30s | Parallel processing |
| Maven build integration | N/A | ~2-5s overhead | Per build |

Performance varies based on system resources, file complexity, and output format.

## Optimization Strategies

### 1. Parallel Processing

By default, DocTester processes multiple files in parallel. Control parallelism:

```bash
# Use all CPU cores (default)
dtr batch --input '*.md' --output ./html/

# Limit to 4 threads
dtr batch --input '*.md' --output ./html/ --threads 4

# Sequential processing (single thread)
dtr batch --input '*.md' --output ./html/ --threads 1
```

**When to use:**
- **More threads** — Large number of files, multi-core system
- **Fewer threads** — Limited memory, other background processes

### 2. Caching

DocTester caches conversion results to avoid redundant work:

```bash
# Cache is enabled by default
dtr fmt html input.md -o output.html

# Disable cache for this conversion
dtr fmt html input.md -o output.html --no-cache

# Clear the cache
dtr cache clear
```

Cache files are stored in `~/.doctester/cache/`. The cache is invalidated when:
- Input file is modified
- Configuration changes
- DocTester version updates

**When to use:**
- **Enable caching** — Repeated conversions, batch processing, CI/CD pipelines
- **Disable caching** — Troubleshooting, testing content changes, temporary builds

### 3. Incremental Builds

For batch operations, only reprocess files that have changed:

```bash
# Full rebuild (process all files)
dtr batch --input '*.md' --output ./html/ --force

# Incremental build (only changed files)
dtr batch --input '*.md' --output ./html/
```

Incremental builds compare input file modification time with cached results. This is especially valuable in CI/CD pipelines where builds run frequently.

### 4. Output Format Selection

Some output formats are faster than others:

| Format | Speed | Quality | Use Case |
|--------|-------|---------|----------|
| JSON | ✅✅✅ Very fast | Structured | Data export, API responses |
| Markdown | ✅✅✅ Very fast | Portable | Documentation, version control |
| HTML | ✅✅ Fast | Rich | Websites, browsers |
| PDF | ✅ Slower | Print-ready | Reports, archives |
| LaTeX | ✅ Slower | Academic | Papers, presentations |

**Recommendation:** Use Markdown or JSON for fast batch conversions. Use HTML for web publishing. Use PDF for print or archive.

### 5. Streaming Large Files

DocTester streams large files to avoid loading entirely into memory:

```bash
# Streaming is automatic for files > 10 MB
dtr fmt html large-document.md -o output.html

# Disable streaming (not recommended for large files)
dtr fmt html large-document.md -o output.html --no-stream
```

This is automatic and transparent. Large files use less memory during conversion.

## Memory Management

### Memory Usage

Typical memory usage:

| Task | Memory | Notes |
|------|--------|-------|
| Single file conversion | 50-100 MB | Depends on file size |
| Batch (100 files) | 200-500 MB | Parallel processing |
| PDF generation | 100-200 MB | Includes LaTeX |

### Memory Limits

Set memory limits for batch operations:

```bash
# Limit to 512 MB
dtr batch --input '*.md' --output ./html/ --max-memory 512M

# Limit to 2 GB
dtr batch --input '*.md' --output ./html/ --max-memory 2G
```

DocTester will adjust parallelism if memory limit is reached.

## CI/CD Integration Performance

### GitHub Actions

```yaml
- name: Generate Documentation
  run: |
    dtr batch --input 'docs/*.md' --output './html/'
    # Takes: ~10-30 seconds depending on file count
```

### Maven Integration

```xml
<plugin>
    <groupId>org.r10r</groupId>
    <artifactId>doctester-maven-plugin</artifactId>
    <configuration>
        <parallel>true</parallel>
        <threads>4</threads>
        <cache>true</cache>
    </configuration>
</plugin>
```

### Jenkins Pipeline

```groovy
stage('Docs') {
    steps {
        sh 'dtr batch --input "docs/*.md" --output "./html/" --cache'
    }
}
```

Performance in CI/CD is typically 2-5x faster than local development due to caching and parallel processing.

## Performance Troubleshooting

### Slow Conversions

**Symptom:** Conversions take longer than expected.

**Solutions:**
1. Check if cache is disabled: `dtr fmt html file.md --cache`
2. Reduce parallelism: `dtr batch --threads 1 --input '*.md'`
3. Check system resources: `top`, `df -h`, `free -m`
4. Split large files: Convert separately instead of one huge file
5. Use faster output format: HTML instead of PDF

### High Memory Usage

**Symptom:** Out of memory errors or system becomes sluggish.

**Solutions:**
1. Limit memory: `dtr batch --max-memory 1G --input '*.md'`
2. Reduce threads: `dtr batch --threads 2 --input '*.md'`
3. Disable caching: `dtr batch --no-cache --input '*.md'`
4. Split batch into smaller groups: `dtr batch --input 'docs/part1/*.md' --output ./html/part1/`
5. Run when system is idle or use `nice` to lower priority

### Cache Ineffectiveness

**Symptom:** Cache hits reported but performance doesn't improve.

**Solutions:**
1. Clear cache: `dtr cache clear`
2. Check cache location: `ls -la ~/.doctester/cache/`
3. Verify file hasn't changed: `ls -la input.md`
4. Check disk I/O: `iostat` — slow disk will negate cache benefits

## Benchmarking Your Workflow

Measure your actual performance:

```bash
# Time a single conversion
time dtr fmt html doc.md -o output.html

# Time a batch conversion
time dtr batch --input 'docs/*.md' --output ./html/

# Compare with/without cache
time dtr fmt html doc.md --no-cache
time dtr fmt html doc.md --cache
```

Use these measurements to identify bottlenecks and optimize accordingly.

## Advanced Configuration

### Custom Thread Pool

For Maven builds with many modules:

```xml
<configuration>
    <threadPool>
        <coreSize>4</coreSize>
        <maxSize>8</maxSize>
    </threadPool>
</configuration>
```

### Compression

Enable output compression for web serving:

```bash
dtr fmt html input.md -o output.html --compress
# Creates: output.html.gz
```

## Performance Best Practices

1. **Enable caching** — Always use caching unless troubleshooting
2. **Use parallel processing** — Leverage multi-core systems for batch jobs
3. **Match format to purpose** — HTML for web, PDF only when needed
4. **Clean cache periodically** — `dtr cache clear` monthly
5. **Monitor system resources** — Watch memory and CPU during batch jobs
6. **Incremental builds in CI/CD** — Only rebuild when files change
7. **Compress for delivery** — Use `--compress` for web hosting

---

## See Also

- [Architecture](./architecture.md) — System design
- [Best Practices](./best-practices.md) — Workflow recommendations
- [How-To: Batch Export](../how-to/batch-export.md) — Batch conversion guide
- [How-To: Maven Integration](../how-to/maven-integration.md) — Build integration

*Last updated: 2026-03-11*
