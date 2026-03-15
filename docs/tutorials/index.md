# Tutorials

Tutorials are **learning-oriented**. They take you through a series of steps to complete a project. When you finish, you will have a working DTR setup and a clear mental model of how it works.

**DTR version:** 2026.2.0 | **Java:** 26+ with `--enable-preview`

---

## Quick Start (5 minutes)

**New to DTR?** Start here for the fastest path to your first documentation test.

| Tutorial | Duration | What you'll learn |
|---|---|---|
| [QUICKSTART](../QUICKSTART.md) | 5 min | Zero to first DocTest in 5 minutes — setup, write test, run, verify output |

---

## Progressive Learning Path (125 minutes)

**Complete tutorial series** — start at Tutorial 1 and work through to Tutorial 6 for comprehensive DTR mastery.

### Tutorial 1: Hello DTR (15 min)

[**basics.md**](basics.md) | **Prerequisites:** Java 26, Maven/mvnd, basic JUnit Jupiter 6

**What you'll build:** Your first documentation test from scratch

- Understand the DTR mental model (tests → documentation)
- Write a complete DTR test with `@ExtendWith(DtrExtension.class)`
- Use core `say*` methods: `say()`, `sayCode()`, `sayTable()`
- Run tests and examine generated output
- Learn common patterns for everyday documentation

**Outcome:** Working DTR test that generates multi-format documentation (Markdown, HTML, LaTeX, JSON)

---

### Tutorial 2: REST API Documentation (20 min)

[**http-testing.md**](http-testing.md) | **Prerequisites:** Completed Tutorial 1

**What you'll build:** Complete REST API documentation for a user management service

- Document HTTP endpoints (GET, POST, PUT, DELETE)
- Specify request parameters, headers, and body schemas
- Document response structures with examples
- Cover error responses with status codes
- Add authentication and rate limiting documentation
- Test API contracts and validate schemas

**Outcome:** Production-ready API documentation generated from executable tests

---

### Tutorial 3: Java 26 Features (25 min)

[**java26-features.md**](java26-features.md) | **Prerequisites:** Completed Tutorial 2

**What you'll build:** Documentation for modern Java features using DTR's reflection capabilities

- Document records with `sayRecordComponents()` — automatic schema tables
- Visualize sealed class hierarchies with `sayCodeModel()`
- Document pattern matching with guards and exhaustiveness
- Generate control flow graphs using Java 26 Code Reflection (JEP 516)
- Create operation profiles from bytecode analysis

**Outcome:** Documentation that leverages Java 26's advanced reflection APIs

---

### Tutorial 4: Performance Documentation (20 min)

[**performance.md**](performance.md) | **Prerequisites:** Completed Tutorial 3

**What you'll build:** Performance benchmarks with automatic measurement and reporting

- Measure execution time with `sayBenchmark()` (50 warmup / 500 measure rounds)
- Create custom benchmarks with explicit round counts
- Compare multiple implementations (A/B testing)
- Interpret metrics: avg, min, max, p99, throughput
- Document performance regression tests
- Capture environment profiles with `sayEnvProfile()`

**Outcome:** Performance documentation with reproducible metrics and environment context

---

### Tutorial 5: Mermaid Diagrams (20 min)

[**diagrams.md**](diagrams.md) | **Prerequisites:** Completed Tutorial 4

**What you'll build:** Visual documentation with auto-generated and manual diagrams

- Create manual Mermaid diagrams with `sayMermaid()` (flowcharts, sequences)
- Auto-generate class diagrams from reflection with `sayClassDiagram()`
- Visualize control flow graphs using Java 26 Code Reflection
- Generate call graphs showing method interconnections
- Understand where Mermaid renders (GitHub, GitLab, Obsidian, VS Code)

**Outcome:** Rich visual documentation that renders natively in documentation platforms

---

### Tutorial 6: Contract Verification (25 min)

[**contracts.md**](contracts.md) | **Prerequisites:** Completed Tutorial 5

**What you'll build:** Automated contract compliance checking for interface implementations

- Document interface implementation coverage across multiple classes
- Detect contract violations (missing methods) automatically
- Use sealed interfaces for automatic subclass detection
- Verify plugin architectures and strategy patterns
- Generate implementation compliance reports
- Interpret symbols: ✓ direct, ↗ inherited, ❌ MISSING

**Outcome:** Contract verification documentation that catches violations before production

---

## Advanced Topics

### Virtual Threads & Concurrency

[**virtual-threads-lightweight-concurrency.md**](virtual-threads-lightweight-concurrency.md) | **Duration:** 30 min | **Prerequisites:** Completed Tutorial 4

**What you'll learn:**

- Document virtual thread performance vs platform threads
- Measure thread creation overhead with `sayBenchmark()`
- Compare executor services (fixed pool vs virtual thread per task)
- Document concurrency patterns and scaling behavior
- Capture thread pool metrics and throughput

**Use case:** Performance optimization for high-concurrency applications

---

## Deprecated Tutorials (Removed in v2.6.0)

The following tutorials have been removed because DTR 2.6.0 eliminated the built-in HTTP client:

- ~~[server-sent-events.md](server-sent-events.md)~~ — HTTP features removed
- ~~[websockets-realtime.md](websockets-realtime.md)~~ — HTTP features removed
- ~~[grpc-streaming.md](grpc-streaming.md)~~ — HTTP features removed

**Migration path:** Use `java.net.http.HttpClient` from the JDK directly. See [Tutorial 2: REST API Documentation](http-testing.md) for examples.

---

## Legacy Tutorials (Archived)

The following tutorials have been superseded by the new progressive series and are archived for historical reference:

- **[your-first-doctest-pre-2.6.md](../releases/archive/tutorials/your-first-doctest-pre-2.6.md)** — Archived version using `DtrContext` parameter injection (pre-2.6.0 API)
  → Use [Tutorial 1: Hello DTR](basics.md) for the current `extends DtrTest` pattern

- ~~[testing-a-rest-api.md](testing-a-rest-api.md)~~ → Use [Tutorial 2: REST API Documentation](http-testing.md) instead
- ~~[records-sealed-classes.md](records-sealed-classes.md)~~ → Use [Tutorial 3: Java 26 Features](java26-features.md) instead

---

## Before You Start

### Prerequisites

All tutorials require:

- **Java 26** installed (`java -version` shows `openjdk 26.ea.13` or higher)
- **Maven 4** or **mvnd** available (`mvnd --version`)
- **Basic JUnit Jupiter 6 knowledge** — `@Test` annotations, assertions, test structure

### Installation

Add DTR to your `pom.xml`:

```xml
<dependencies>
    <dependency>
        <groupId>io.github.seanchatmangpt.dtr</groupId>
        <artifactId>dtr-core</artifactId>
        <version>2026.2.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

Configure Maven plugins for Java 26 preview features:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.14.0</version>
            <configuration>
                <release>26</release>
                <compilerArgs>
                    <arg>--enable-preview</arg>
                </compilerArgs>
                <enablePreview>true</enablePreview>
            </configuration>
        </plugin>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-surefire-plugin</artifactId>
            <version>3.5.3</version>
            <configuration>
                <argLine>--enable-preview --add-opens java.base/java.lang=ALL-UNNAMED</argLine>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### Running Tutorials

Execute tutorial tests with:

```bash
# Run specific tutorial
mvnd test -Dtest=HelloDtrTest

# Run all tutorials
mvnd test

# View generated documentation
ls target/docs/test-results/
```

---

## Tutorial Metrics

| Tutorial | Duration | Difficulty | Key Methods |
|----------|----------|------------|-------------|
| Tutorial 1: Hello DTR | 15 min | Beginner | `say()`, `sayCode()`, `sayTable()` |
| Tutorial 2: REST API | 20 min | Beginner | `sayKeyValue()`, `sayCode()`, `sayTable()` |
| Tutorial 3: Java 26 | 25 min | Intermediate | `sayRecordComponents()`, `sayCodeModel()`, `sayControlFlowGraph()` |
| Tutorial 4: Performance | 20 min | Intermediate | `sayBenchmark()`, `sayEnvProfile()` |
| Tutorial 5: Diagrams | 20 min | Intermediate | `sayMermaid()`, `sayClassDiagram()`, `sayCallGraph()` |
| Tutorial 6: Contracts | 25 min | Advanced | `sayContractVerification()` |
| Virtual Threads | 30 min | Advanced | `sayBenchmark()` with concurrency |

**Total time for core series:** 125 minutes (2 hours 5 minutes)

---

## Next Steps

After completing the tutorial series:

1. **Explore the say* API Reference** — Complete guide to all 50+ documentation methods
2. **Read 80/20 Essentials** — Most-used DTR patterns for everyday work
3. **Check Example Tests** — Browse DTR's own test suite for real-world examples
4. **Join the Community** — GitHub discussions, issues, and contributions

---

## Getting Help

- **Documentation Index:** [docs/index.md](../index.md)
- **API Reference:** [docs/reference/say-api.md](../reference/say-api.md)
- **GitHub Issues:** [Report bugs or request features](https://github.com/seanchatmangpt/dtr/issues)
- **FAQ:** [Common questions and solutions](../reference/FAQ_AND_TROUBLESHOOTING.md)

---

**Version:** DTR 2026.2.0 | **Last Updated:** March 2026 | **Java:** 26+
