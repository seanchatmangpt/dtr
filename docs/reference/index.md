# Reference

Reference documentation is **information-oriented**. It is the authoritative, exhaustive description of DTR's API. Use it when you need to look up a specific method, class, or configuration option.

**Version:** 2026.4.1 | **Maven:** `io.github.seanchatmangpt:dtr-core:2026.4.1` | **Java:** 26+ with `--enable-preview`

> **See also:** [Architecture](../architecture.md) | [Tutorials](../tutorials/)

---

## Quick Reference

| Topic | Description |
|-------|-------------|
| [Field Injection Guide](../tutorials/field-injection-guide.md) | **Primary Pattern**: `@DtrContextField` + `@DtrTest` for cleaner tests |
| [say* API Complete Reference](say-api-methods.md) | Complete API reference for all say* methods with signatures and examples |
| [80/20 Quick Reference](80-20-quick-reference.md) | One-page cheat sheet — the 50+ say* methods you'll use 80% of the time |
| [FAQ and Troubleshooting](FAQ_AND_TROUBLESHOOTING.md) | Migration guide, build issues, rendering problems, common questions |
| [Known Issues and Limitations](KNOWN_ISSUES.md) | Current version limitations, workarounds, planned fixes |

---

## Core API

| Topic | Description |
|-------|-------------|
| [say* API Complete Reference](say-api-methods.md) | All 50+ say* method signatures, parameters, return types, usage examples |
| [80/20 Quick Reference](80-20-quick-reference.md) | Condensed guide to the most commonly used say* methods |

---

## Base Classes

| Topic | Description |
|-------|-------------|
| [DtrTest API Reference](dtr-test-api.md) | **Primary Pattern**: Field injection with `@DtrContextField` + `@DtrTest` annotation |
| [Field Injection Guide](../tutorials/field-injection-guide.md) | **Getting Started**: Step-by-step guide to field injection approach |
| [RenderMachine API Reference](rendermachine-api.md) | Abstract renderer base class, implementations, virtual thread dispatch |

**Note:** The `extends DtrTest` inheritance pattern is **legacy** and deprecated since 2026.4.1. Use field injection instead.

---

## Configuration

| Topic | Description |
|-------|-------------|
| [Configuration Reference](configuration.md) | Output directory, output formats, system properties, Maven settings, Java toolchain |

---

## Java 26 Features

| Topic | Description |
|-------|-------------|
| [Java 26 Features Reference](java25-features-reference.md) | Records, sealed classes, pattern matching, text blocks, virtual threads, JEP 516 |
| [Records and Sealed Classes](records-sealed-reference.md) | Records, sealed types, pattern matching syntax — RenderMachine design rationale |
| [Virtual Threads API](virtual-threads-reference.md) | ExecutorService, Future, Thread.ofVirtual() — MultiRenderMachine dispatch |

---

## Historical Reference

The following pages document APIs that were removed in v2.6.0. They are preserved for historical context only.

| Removed API | Description |
|-------------|-------------|
| [TestBrowser API](testbrowser-api.md) | ~~JUnit Jupiter 6 extension lifecycle, DtrContext methods~~ (removed in v2.6.0) |
| [HTTP Client APIs](request-api.md), [response-api.md), [http-constants.md](http-constants.md) | ~~Request/Response building, HTTP constants~~ (removed in v2.6.0) |
| [WebSocket APIs](websockets-reference.md) | ~~WebSocket client, session management~~ (removed in v2.6.0) |
| [Server-Sent Events APIs](sse-reference.md) | ~~SSE stream client, event handling~~ (removed in v2.6.0) |
| [gRPC APIs](grpc-reference.md) | ~~gRPC client, streaming~~ (removed in v2.6.0) |
| [Real-time Protocols](realtime-protocols-reference.md) | ~~Unified real-time protocol client~~ (removed in v2.6.0) |
| [URL Builder](url-builder.md) | ~~Fluent URL construction~~ (removed in v2.6.0) |

> **Note:** These APIs were removed to focus DTR on its core mission: documentation testing. If you need HTTP/WebSocket/gRPC testing, use dedicated libraries like [WireMock](https://wiremock.org/), [OkHttp](https://square.github.io/okhttp/), or [gRPC testing utilities](https://grpc.github.io/grpc-java/javadoc/io/grpc/testing/grpc-in-process.html).

## Documentation Testing Patterns

### Current Approach (2026.4.1+)

| Pattern | Description | Status |
|---------|-------------|--------|
| **Field Injection** | `@DtrContextField` + `@DtrTest` | ✅ **Primary** - Recommended for all new code |
| Parameter Injection | `DtrContext ctx` parameter | 🔄 Alternative for specialized cases |
| **Inheritance** | `extends DtrTest` | ❌ **Legacy** - Deprecated since 2026.4.1 |

### Quick Start Guide

- **New Projects**: Start with [Field Injection Guide](field-injection-guide.md)
- **API Reference**: [say* API Complete Reference](say-api-methods.md)
- **Annotations**: [Annotation Reference](annotation-reference.md) - `@DtrContextField`, `@DtrTest`
- **Migration**: [FAQ](FAQ_AND_TROUBLESHOOTING.md) - Legacy to field injection
