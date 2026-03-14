# Contributing to DTR

This section is for developers who want to contribute to DTR itself — fix bugs, add `say*` methods, improve documentation, or cut a release.

## Contribution Guides

| Guide | When to read it |
|---|---|
| [Development Setup](setup.md) | First time setting up the project |
| [Codebase Tour](codebase-tour.md) | Orientation before making changes |
| [Making Changes](making-changes.md) | Adding a `say*` method, coding standards, PR process |
| [Releasing](releasing.md) | How to publish a release to Maven Central |

---

## Required Toolchain

DTR 2.6.0 requires:

- **Java 26** with `--enable-preview` (Java 24 and below are not supported)
- **Maven 4.0.0-rc-5+** or **mvnd 2.x** (the Maven Daemon is preferred for local development)

Do not use `./mvnw` — the Maven wrapper downloads Maven 3, which is incompatible.

---

## Quick Start for Contributors

```bash
# Clone
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# Verify toolchain
java -version          # must be openjdk 25.x.x
mvnd --version         # must be mvnd 2.x / Maven 4.x

# Build and test
mvnd clean test -pl dtr-core
mvnd test -pl dtr-integration-test
```

See [Development Setup](setup.md) for full installation instructions.

---

## Branch Naming Convention

Work on feature branches, never directly on `main`:

```
feature/<short-description>
fix/<short-description>
docs/<short-description>
```

Examples:
- `feature/say-ascii-chart`
- `fix/record-component-null-handling`
- `docs/improve-setup-guide`

Internal branches created by automation use the convention `claude/<task-name>-<ID>`.

---

## Where to Get Help

- **GitHub Issues** — bug reports and feature requests
- **GitHub Discussions** — design questions and contributor Q&A
- Open an issue before starting significant work; design discussion before code saves time for everyone.

---

## What We're Looking For

Good contributions:

- **New `say*` methods** with Javadoc, a no-op default, a `RenderMachineImpl` implementation, and a test
- **Bug fixes** with a failing test that passes after the fix
- **Java 26 modernization** — records, sealed classes, pattern matching where appropriate
- **Documentation improvements** — examples, corrections, clarifications
- **Performance** — JMH benchmarks in `dtr-benchmarks/` with real measurements

We are unlikely to accept:

- Re-introduction of HTTP/WebSocket/SSE functionality (removed deliberately in 2.6.0)
- Large refactors without prior discussion
- Changes that add new external runtime dependencies
- Breaking API changes without a migration path and prior discussion
