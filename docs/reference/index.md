# Reference

Reference documentation is **information-oriented**. It is the authoritative, exhaustive description of DTR's API. Use it when you need to look up a specific method, class, or configuration option.

**Version:** 2026.2.0 | **Maven:** `io.github.seanchatmangpt:dtr-core:2026.2.0` | **Java:** 26+ with `--enable-preview`

> **See also:** [Architecture](../architecture.md) | [Tutorials](../tutorials/)

---

## Quick Reference

| Topic | Description |
|-------|-------------|
| [80/20 Quick Reference](80-20-quick-reference.md) | One-page cheat sheet — the 50+ say* methods you'll use 80% of the time |
| [say* API Complete Reference](say-api-methods.md) | Complete API reference for all say* methods with signatures and examples |
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
| [DtrTest API Reference](doctester-base-class.md) | Base class for documentation tests — test lifecycle, assertion helpers |
| [RenderMachine API Reference](rendermachine-api.md) | Abstract renderer base class, implementations, virtual thread dispatch |

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
