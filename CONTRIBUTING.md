# Contributing to DTR (Documentation Testing Runtime)

Thank you for your interest in contributing to DTR! We welcome contributions of all kinds including bug reports, documentation improvements, feature requests, and pull requests.

## Code of Conduct

This project adheres to the Contributor Covenant Code of Conduct. By participating, you are expected to uphold this code. Please see [CODE_OF_CONDUCT.md](./CODE_OF_CONDUCT.md) for details.

## Getting Started

### Prerequisites

- **Java 25 or later** (Java 25.0.2+ recommended)
- **Maven 4.0.0-rc-5+** or **mvnd 2.0.0+**
- **Git** for version control

### Setup Development Environment

```bash
# Clone the repository
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# Verify Java version
java -version  # Should show openjdk version "25.0.2" or later

# Verify Maven
mvnd --version  # Should show Maven 4.0.0-rc-5+
```

### Building the Project

```bash
# Full clean build with all modules
mvnd clean install

# Build specific module
mvnd clean install -pl dtr-core

# Run integration tests
mvnd test -pl dtr-integration-test

# Build with proxy (if behind corporate firewall)
mvnd clean install \
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
```

### Running Tests

```bash
# Run all tests
mvnd test

# Run specific test class
mvnd test -Dtest=PhDThesisDocTest

# Run with verbose output
mvnd test -X
```

## How to Contribute

### Reporting Bugs

Before creating a bug report, please check the issue list. When reporting a bug, include:

1. Your environment (Java version, OS, Maven version)
2. Steps to reproduce the issue
3. Expected behavior vs. actual behavior
4. Code examples if applicable
5. Stack trace or error messages

**Use the GitHub Issues tab:** [DTR Issues](https://github.com/seanchatmangpt/dtr/issues)

### Suggesting Enhancements

Feature suggestions are welcome! When proposing a feature:

1. Use a clear, descriptive title
2. Explain the use case and benefits
3. Provide examples of how it would work
4. Note any potential implementation challenges

**Use the GitHub Discussions tab:** [DTR Discussions](https://github.com/seanchatmangpt/dtr/discussions)

### Submitting Pull Requests

1. **Fork the repository** on GitHub
2. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
   > **Note:** Agent/automated branches follow the pattern `claude/description-XXXXX` (e.g., `claude/fix-latex-errors-rzhxB`). These branches are managed by automated tooling and should not be pushed to manually.

3. **Make your changes** following the code style (see below)

4. **Commit with clear messages** (see Commit Guidelines below)

5. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

6. **Create a Pull Request** with:
   - Clear title describing the change
   - Description of what changed and why
   - Reference to any related issues (e.g., "Fixes #123")
   - Test coverage for new features

### Code Style

- Follow Java conventions for naming and structure
- Use the formatting configured in `.mvn/maven.config`
- Keep methods focused and concise
- Add meaningful comments for complex logic
- Java 25+ features (records, sealed classes, pattern matching) are encouraged

### Commit Guidelines

Write clear, atomic commits:

```bash
# Good commit message
git commit -m "feat: Add support for WebSocket streaming in tests

- Implements RFC 6455 WebSocket protocol
- Adds WebSocketClient interface
- Includes integration tests for bi-directional messaging
- Closes #456"

# Bad commit message
git commit -m "fixed stuff"
```

Use conventional commit prefixes:
- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `test:` - Adding or updating tests
- `refactor:` - Code refactoring without feature changes
- `chore:` - Build process, dependencies, etc.

## Development Workflows

### Java Module Development (dtr-core, dtr-integration-test, dtr-benchmarks)

```bash
# Edit Java source files in src/main/java/io/github/seanchatmangpt/dtr/

# Test your changes
mvnd clean test -pl dtr-core

# Run specific test
mvnd test -pl dtr-core -Dtest=DTRTest

# Build documentation from tests
mvnd clean test -pl dtr-integration-test -Dtest=PhDThesisDocTest
```

### CLI Development (dtr-cli - Python)

See [dtr-cli/CONTRIBUTING.md](./dtr-cli/CONTRIBUTING.md) for Python-specific guidelines.

## Documentation

### Adding to User Documentation

Documentation lives in `/docs/` with sections:
- `tutorials/` - Step-by-step guides
- `how-to/` - Solution-oriented guides
- `reference/` - API and configuration reference
- `explanation/` - Conceptual information

### Generating Documentation from Tests

The beauty of DTR is that documentation is generated from tests:

```java
@Test
void documentMyFeature() {
    sayNextSection("Feature Title");
    say("Feature description...");
    sayCode("example code", "java");
    sayJson(Map.of("key", "value"));
}
```

Run the test to generate markdown in `target/docs/test-results/`.

## Project Structure

```
dtr/
├── dtr-core/                    # Main library with RenderMachine, say* API
│   ├── src/main/java/io/github/seanchatmangpt/dtr/
│   └── src/test/java/
├── dtr-integration-test/        # Integration tests (Ninja Framework app)
├── dtr-benchmarks/              # JMH performance benchmarks
├── dtr-cli/               # Python CLI wrapper
├── docs/                        # User documentation (Diataxis structure)
├── pom.xml                      # Root Maven configuration
└── CLAUDE.md                    # Developer quick reference
```

## Key Files to Know

- **[CLAUDE.md](./CLAUDE.md)** — Quick reference for developers
- **[pom.xml](./pom.xml)** — Maven configuration, dependencies, versions
- **.mvn/maven.config** — Maven flags (includes `--enable-preview`)
- **LICENSE** — Apache 2.0 license text
- **BREAKING-CHANGES-2.0.0.md** — Major version breaking changes

## Testing Requirements

All PRs must:

1. Include tests for new features
2. Pass existing tests: `mvnd test`
3. Maintain or improve code coverage
4. Generate documentation: `mvnd test -pl dtr-integration-test`

## Review Process

1. A maintainer will review your PR
2. Feedback may be requested
3. Once approved, your PR will be merged
4. Your contribution will be credited in release notes

## Release Process

The project uses semantic versioning. Current version: `2.5.0-SNAPSHOT`

Releases to Maven Central follow:
- `SNAPSHOT` → Final testing
- `X.Y.Z` → Stable release
- Release notes document breaking changes

## Getting Help

- **GitHub Issues:** Bug reports and feature requests
- **GitHub Discussions:** General questions and feature ideas
- **Email:** Contact maintainers through GitHub profile

## Additional Resources

- [Java 25 Features Guide](./JAVA_26_DEVELOPER_GUIDE.md)
- [Architecture Overview](./docs/explanation/architecture.md)
- [API Reference](./docs/reference/)
- [Maven Central Release Guide](./RELEASE_PLAN_2.0.0.md)

---

**Thank you for contributing to DTR!** Your work helps make documentation and testing better for everyone.
