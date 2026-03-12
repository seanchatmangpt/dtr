# Tutorials

Tutorials are **learning-oriented**. They take you through a series of steps to complete a project. When you finish, you'll have a working DTR setup and a clear mental model of how it works.

## Available Tutorials

### Java 25 Fundamentals

| Tutorial | What you'll learn |
|---|---|
| [Virtual Threads for Lightweight Concurrency](virtual-threads-lightweight-concurrency.md) | How to spawn thousands of concurrent tasks using Java 25 virtual threads |
| [Records and Sealed Classes](records-sealed-classes.md) | How to eliminate boilerplate with records and enforce type safety with sealed classes |

### Real-Time Protocols

| Tutorial | What you'll learn |
|---|---|
| [WebSockets for Real-Time Communication](websockets-realtime.md) | Bidirectional persistent connections for chat and collaboration |
| [gRPC Streaming](grpc-streaming.md) | Efficient RPC with unary, streaming, and bidirectional patterns |
| [Server-Sent Events](server-sent-events.md) | One-way HTTP-based push notifications and live updates |

### API Testing with DTR

| Tutorial | What you'll build |
|---|---|
| [Your First DocTest](your-first-doctest.md) | A complete, runnable DocTest for a public API |
| [Your First DTR Test: From Test to Living Docs](01-basic-test-documentation.md) | A simple test that generates HTML documentation automatically |
| [Testing REST APIs and Generating API Documentation](02-http-api-testing.md) | A complete REST API test suite with multi-endpoint documentation |
| [Generating OpenAPI 3.0 Specs from Tests](03-openapi-generation.md) | Machine-readable API specs generated directly from tests |
| [Advanced: Multiple Formats, LaTeX, and PDF](04-advanced-rendering.md) | Generate HTML, LaTeX, PDF, Markdown, and JSON from a single test |
| [Testing a REST API](testing-a-rest-api.md) | A full CRUD API documented end-to-end |

---

## Before You Start

**Prerequisites:**

- Java 25 installed (`java -version` shows `openjdk 25.x.x`)
- Maven 4 or Maven Daemon available (`mvnd --version`)
- A Maven project with JUnit 4

If you're starting from scratch, the [Your First DocTest](your-first-doctest.md) tutorial walks you through project setup too.
