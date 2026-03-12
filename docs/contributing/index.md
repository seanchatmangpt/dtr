# Contributing to DTR

This section is for developers who want to contribute to DTR itself — fix bugs, add features, or improve the codebase.

## Guides

| Guide | When to read it |
|---|---|
| [Development Setup](setup.md) | First time setting up the project |
| [Codebase Tour](codebase-tour.md) | Orientation before making changes |
| [Making Changes](making-changes.md) | Coding standards, testing requirements, PR process |
| [Releasing](releasing.md) | How to cut a release to Maven Central |

---

## Quick start for contributors

```bash
# Clone
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# Verify toolchain
java -version          # openjdk 25.x.x
mvnd --version         # mvnd 2.x / Maven 4.x

# Build everything
mvnd clean verify

# Run integration tests (starts a Jetty server)
mvnd clean verify -pl dtr-integration-test
```

Open an issue before starting significant work. We use GitHub Issues for bugs, feature requests, and design discussion.

---

## What we're looking for

Good contributions:

- **Bug fixes** with a failing test that passes after the fix
- **Dependency updates** (Jackson, Apache HttpClient, Bootstrap, jQuery)
- **Java 25 modernization** — records, sealed classes, pattern matching
- **Documentation improvements** — examples, corrections, clarifications
- **JUnit 5 support** — a significant but well-scoped change
- **Java HttpClient migration** — replace Apache HttpClient with `java.net.http`

We're unlikely to accept:
- Large refactors without prior discussion
- Features that significantly increase the dependency footprint
- Breaking API changes without a clear migration path
