# Reference

Reference documentation is **information-oriented**. It is the authoritative, exhaustive description of DTR 2.6.0's API. Use it when you need to look up a specific method, class, or configuration option.

**Version:** 2.6.0 | **Maven:** `io.github.seanchatmangpt.dtr:dtr-core:2.6.0` | **Java:** 25+ with `--enable-preview`

---

## Quick Reference

| Topic | Description |
|-------|-------------|
| [80/20 Quick Reference](80-20-quick-reference.md) | One-page cheat sheet — all 37 say* methods, RenderMachine implementations, removed APIs |
| [FAQ and Troubleshooting](FAQ_AND_TROUBLESHOOTING.md) | Migration from v2.5.x, removed API questions, build issues, rendering problems |
| [Known Issues and Limitations](KNOWN_ISSUES.md) | v2.6.0 breaking changes, WireMock warnings, workarounds |

---

## Core API

| Topic | Description |
|-------|-------------|
| [say* Core API Reference](request-api.md) | All 37 say* method signatures, parameters, return types, output formats |
| [DtrContext and DtrExtension API Reference](testbrowser-api.md) | JUnit 5 extension lifecycle, DtrContext methods, test class template |
| [DtrContext and DtrTest Reference](doctester-base-class.md) | DtrContext details, migration from DTR base class, full test class template |
| [RenderMachine API](rendermachine-api.md) | Abstract base, all implementations, virtual thread dispatch, custom renderers |

---

## v2.6.0 New API

| Topic | Description |
|-------|-------------|
| [Benchmarking API Reference](url-builder.md) | `sayBenchmark` overloads, warmup methodology, reporting guidelines |
| [Mermaid Diagram API Reference](http-constants.md) | `sayMermaid`, `sayClassDiagram`, `sayControlFlowGraph`, `sayCallGraph`, `sayOpProfile` |
| [Coverage and Contract API Reference](websockets-reference.md) | `sayDocCoverage`, `sayContractVerification`, `sayEvolutionTimeline` |
| [Utility API Reference](sse-reference.md) | `sayEnvProfile`, `sayRecordComponents`, `sayException`, `sayAsciiChart` |

---

## JVM and Code Introspection

| Topic | Description |
|-------|-------------|
| [JVM Introspection API Reference](realtime-protocols-reference.md) | `sayCallSite`, `sayAnnotationProfile`, `sayClassHierarchy`, `sayStringProfile`, `sayReflectiveDiff` |
| [Code Reflection API Reference](grpc-reference.md) | `sayCodeModel`, `sayControlFlowGraph`, `sayCallGraph`, `sayOpProfile` — JEP 516 integration |

---

## Java 25 Language Features

| Topic | Description |
|-------|-------------|
| [Java 25 Features Reference](java25-features-reference.md) | Records, sealed classes, pattern matching, text blocks, virtual threads, JEP 516, v2.6.0 examples |
| [Records and Sealed Classes](records-sealed-reference.md) | Records, sealed types, pattern matching syntax; RenderMachine is abstract (not sealed) since v2.5.0 |
| [Virtual Threads API](virtual-threads-reference.md) | ExecutorService, Future, Thread.ofVirtual(); MultiRenderMachine dispatch; sayBenchmark warmup |

---

## Configuration

| Topic | Description |
|-------|-------------|
| [Configuration](configuration.md) | Output directory, output formats, system properties, Maven settings, Java toolchain |

---

## Removed in v2.6.0

The following pages document APIs that no longer exist. They have been replaced with content for current v2.6.0 APIs.

> **Removed APIs:** `TestBrowser`, `TestBrowserImpl`, `Request`, `Response`, `Url`, `sayAndMakeRequest`, `sayAndAssertThat`, `WebSocketClient`, `WebSocketSession`, `ServerSentEventsClient`, SSE stream API, `BearerTokenAuth`, `ApiKeyAuth`, `BasicAuth`, `HttpConstants`

If you encounter compilation errors for any of these, see [FAQ and Troubleshooting — v2.6.0 Migration Questions](FAQ_AND_TROUBLESHOOTING.md).
