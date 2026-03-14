# Tutorials

Tutorials are **learning-oriented**. They take you through a series of steps to complete a project. When you finish, you will have a working DTR setup and a clear mental model of how it works.

**DTR version:** 2.6.0 | **Java:** 25+ with `--enable-preview`

## Available Tutorials

### Getting Started

| Tutorial | What you'll learn |
|---|---|
| [Your First DocTest](your-first-doctest.md) | Set up DTR 2.6.0, write a test, read the generated Markdown output |

### Java 25 Fundamentals with DTR

| Tutorial | What you'll learn |
|---|---|
| [Records and Sealed Classes](records-sealed-classes.md) | Model type-safe data with records, document schemas with `sayRecordComponents` and `sayCodeModel` |
| [Benchmarking with Virtual Threads](virtual-threads-lightweight-concurrency.md) | Measure performance with `sayBenchmark`, run warmup with virtual threads, compare platform vs virtual thread executors |

### API Documentation

| Tutorial | What you'll build |
|---|---|
| [Testing a REST API](testing-a-rest-api.md) | A full CRUD API documented end-to-end using `java.net.http.HttpClient` and DTR `say*` methods |

### Visualizing Code with DTR

| Tutorial | What you'll learn |
|---|---|
| [Diagrams and Mermaid with sayMermaid](websockets-realtime.md) | Render architecture diagrams, flowcharts, and class diagrams directly in documentation |
| [Contract Verification with sayContractVerification](server-sent-events.md) | Prove interface implementations are complete and document coverage matrices |
| [Code Evolution with sayEvolutionTimeline and sayClassDiagram](grpc-streaming.md) | Visualize git history and auto-generate class diagrams from reflection |

---

## Before You Start

**Prerequisites:**

- Java 25 installed (`java -version` shows `openjdk 25.x.x`)
- Maven 4 or Maven Daemon available (`mvnd --version`)
- A Maven project with DTR 2.6.0 on the test classpath

If you are starting from scratch, the [Your First DocTest](your-first-doctest.md) tutorial walks you through project setup too.

**Important:** DTR 2.6.0 removed the built-in HTTP client (`sayAndMakeRequest`, `Request`, `Response`, etc.). Use `java.net.http.HttpClient` from the JDK directly. See [Testing a REST API](testing-a-rest-api.md) for a complete example.
