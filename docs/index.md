# DTR Documentation

DTR (Documentation Testing Runtime) is a Java 26 library that generates rich Markdown, LaTeX, HTML, and blog documentation directly from JUnit 5 tests via a declarative `say*` API.

**Version:** 2026.2.0 | **Maven:** `io.github.seanchatmangpt.dtr:dtr-core:2026.2.0` | **Java:** 26 + `--enable-preview`

---

## Start Here

**New to DTR?** Begin with **[QUICKSTART.md](QUICKSTART.md)** (5-minute path) or **[Tutorial 1: Your First DocTest](tutorials/01-your-first-doctest.md)** (20-minute hands-on introduction).

---

## Documentation Overview

DTR's documentation follows the [Diátaxis Framework](https://diataxis.fr/), organizing content by purpose:

### 📚 Tutorials - Learn by Doing
Step-by-step lessons that build understanding progressively:

1. **[Your First DocTest](tutorials/01-your-first-doctest.md)** - 20-minute introduction to DTR basics
2. **[Core Documentation Methods](tutorials/02-core-documentation-methods.md)** - Master the essential `say*` API
3. **[Testing with Documentation](tutorials/03-testing-with-documentation.md)** - Assertions combined with docs
4. **[Code Model & Reflection](tutorials/04-code-model-reflection.md)** - Analyze and document code structure
5. **[Java 26 Code Reflection](tutorials/05-java26-code-reflection.md)** - Control flow graphs and call graphs
6. **[Advanced Patterns](tutorials/06-advanced-patterns.md)** - Benchmarking, diagrams, and quality metrics

### 🎯 How-To Guides - Solve Specific Problems
Task-focused recipes for common documentation scenarios:

- **[80/20 Essentials](how-to/80-20-essentials.md)** - The 8 methods you'll use 80% of the time
- **[Benchmarking](how-to/benchmarking.md)** - Document performance with real measurements
- **[Advanced Rendering](how-to/advanced-rendering-formats.md)** - LaTeX, slides, and blog posts
- **[Testing REST APIs](how-to/testing-rest-apis.md)** - Document HTTP endpoints
- **[Code Coverage](how-to/code-coverage.md)** - Track documentation completeness
- **[Custom Output Formats](how-to/custom-output-formats.md)** - Extend with custom renderers
- **[CI/CD Integration](how-to/ci-cd-integration.md)** - Automate docs in GitHub Actions

### 📖 Reference - Look Up Specifics
Complete API documentation and configuration:

- **[Complete API Reference](reference/complete-api-reference.md)** - All 50+ `say*` methods documented
- **[Configuration](reference/configuration.md)** - System properties and environment variables
- **[DtrTest Base Class](reference/doctester-base-class.md)** - Assertion methods
- **[RenderMachine API](reference/rendermachine-api.md)** - Custom output engines
- **[Annotation Reference](reference/annotation-reference.md)** - `@DocSection`, `@DocTag`, and more
- **[Glossary](reference/glossary.md)** - DTR terminology and concepts

### 💡 Explanation - Understand the Why
Design philosophy and architecture:

- **[Why DTR?](explanation/why-dtr.md)** - Problems we solve and our approach
- **[Architecture](../ARCHITECTURE.md)** - System design and module organization
- **[Testing Philosophy](explanation/testing-philosophy.md)** - Why tests are the contract
- **[Java 26 Features](explanation/java26-features.md)** - Records, sealed classes, pattern matching
- **[Performance](../PERFORMANCE.md)** - Optimization strategies and benchmarks
- **[Design Principles](explanation/design-principles.md)** - Single source of truth, progressive disclosure

### 🤝 Contributing - Developer Guides
Join the DTR project:

- **[Contributing Quickstart](../CONTRIBUTING_QUICKSTART.md)** - 30-minute setup guide
- **[Setup Guide](contributing/setup.md)** - Development environment configuration
- **[Codebase Tour](contributing/codebase-tour.md)** - Architecture overview
- **[Making Changes](contributing/making-changes.md)** - Pull request workflow
- **[Releasing](contributing/releasing.md)** - Version management and deployment
- **[Testing Strategy](contributing/testing-strategy.md)** - CI gates and validation

### 📋 Additional Resources

- **[EXAMPLES.md](../EXAMPLES.md)** - 8 real-world documentation patterns
- **[MIGRATING.md](../MIGRATING.md)** - Version upgrade guides
- **[TROUBLESHOOTING.md](../TROUBLESHOOTING.md)** - Symptom-based problem solving
- **[CHANGELOG.md](../CHANGELOG.md)** - Complete version history
- **[README.md](../README.md)** - Project overview and license

---

## Key Changes in 2026.2.0

DTR 2026.2.0 focuses on documentation generation and testing:

**50+ `say*` methods** for comprehensive documentation coverage:
- Core methods: `say()`, `sayCode()`, `sayTable()`, `sayKeyValue()`
- Formatting: `sayNote()`, `sayWarning()`, `sayList()`, `sayJson()`
- Code analysis: `sayCodeModel()`, `sayClassHierarchy()`, `sayAnnotationProfile()`
- Java 26 Code Reflection: `sayControlFlowGraph()`, `sayCallGraph()`, `sayOpProfile()`
- Benchmarking: `sayBenchmark()` with configurable rounds
- Diagrams: `sayMermaid()`, `sayClassDiagram()`
- Quality: `sayDocCoverage()`, `sayContractVerification()`
- Testing: `sayAndAssertThat()` - assert + document in one call

**Documentation overhaul:**
- Reorganized by Diátaxis framework
- 6 progressive tutorials (beginner → advanced)
- Task-focused how-to guides
- Complete API reference with examples
- Design philosophy and architecture docs
- 8 real-world example patterns

**Removed:** HTTP/WebSocket/gRPC testing - DTR is now a pure documentation-generation library. For HTTP testing, use dedicated libraries like REST Assured or WebTestClient.

---

## Quick Reference

### 80/20 API - The 8 Essential Methods

Most documentation uses just 8 methods:

| Method | Use Case |
|--------|----------|
| `say(text)` | Simple paragraphs, most common |
| `sayCode(code, lang)` | Code blocks with syntax highlighting |
| `sayTable(data)` | Structured data tables |
| `sayNextSection(headline)` | H1 heading with TOC entry |
| `sayRef(class, anchor)` | Link to other documentation sections |
| `sayNote(message)` | Additional context |
| `sayWarning(message)` | Critical warnings |
| `sayKeyValue(pairs)` | Metadata, configuration, key facts |

### Version: 2026.2.0

**CalVer versioning:** `YYYY.MINOR.PATCH`
- **Year:** Calendar year of release
- **Minor:** New features, new `say*` methods
- **Patch:** Bug fixes, dependency updates

See [CHANGELOG.md](../CHANGELOG.md) for complete version history.

---

## Links

- **Maven Central:** [io.github.seanchatmangpt.dtr:dtr-core](https://central.sonatype.com/artifact/io.github.seanchatmangpt.dtr/dtr-core)
- **GitHub:** [github.com/seanchatmangpt/dtr](https://github.com/seanchatmangpt/dtr)
- **Issues:** [github.com/seanchatmangpt/dtr/issues](https://github.com/seanchatmangpt/dtr/issues)
- **Discussions:** [github.com/seanchatmangpt/dtr/discussions](https://github.com/seanchatmangpt/dtr/discussions)
