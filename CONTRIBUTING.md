# Contributing to DTR (Documentation Testing Runtime)

Thank you for your interest in contributing to DTR! We welcome contributions of all kinds including bug reports, documentation improvements, feature requests, and pull requests.

## Table of Contents

- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Local CI Testing with act](#local-ci-testing-with-act)
- [Development Workflows](#development-workflows)
- [Code Style Guidelines](#code-style-guidelines)
- [Testing Requirements](#testing-requirements)
- [Pull Request Process](#pull-request-process)
- [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Tools

- **Java 26** (Java 26.ea.13-graal or later)
- **Maven 4.0.0-rc-3+** or **mvnd 2.0.0+** (preferred)
- **Git** for version control
- **act** (for local CI testing) - [Install act](https://github.com/nektos/act#installation)

### Installation

```bash
# Clone the repository
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# Verify Java version (must be 26+)
java -version
# Expected output: openjdk version "26.ea.13-graal" or later

# Verify Maven (mvnd recommended for speed)
mvnd --version
# Expected output: Maven 4.0.0-rc-3+ or mvnd 2.0.0+

# Verify act is installed
act --version
# Expected output: act version X.X.X
```

### Installing Java 26 via SDKMAN

```bash
# Install SDKMAN if not already installed
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 26 (Early Access)
sdk install java 26.ea.13-graal
sdk use java 26.ea.13-graal

# Verify installation
java -version
```

### Installing Maven mvnd (Maven Daemon)

```bash
# Download mvnd 2.0.0+
wget https://github.com/apache/maven-mvnd/releases/download/2.0.0/mvnd-2.0.0-linux-amd64.tar.gz
tar -xzf mvnd-2.0.0-linux-amd64.tar.gz
export PATH="$PATH:$PWD/mvnd-2.0.0-linux-amd64/bin"

# Or on macOS
brew install mvnd

# Verify installation
mvnd --version
```

## Quick Start

### Building the Project

```bash
# Full clean build with all modules
mvnd clean install

# Build specific module
mvnd clean install -pl dtr-core

# Build with all tests
mvnd clean verify

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

# Run integration tests only
mvnd test -pl dtr-integration-test

# Run with verbose output
mvnd test -X

# Run tests with coverage
mvnd clean verify -Djacoco.skip=false
```

## Local CI Testing with act

**act** allows you to run GitHub Actions workflows locally on your machine, speeding up development and catching issues before pushing.

### Installing act

```bash
# On macOS
brew install act

# On Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Via cargo
cargo install act

# Verify installation
act --version
```

### Setting Up act Secrets

Create a `.secrets` file in the project root (git-ignored) for local testing:

```bash
# Create .secrets file
cat > .secrets << 'EOF'
CENTRAL_USERNAME=your_username
CENTRAL_TOKEN=your_token
GPG_PRIVATE_KEY=your_gpg_private_key
GPG_PASSPHRASE=your_passphrase
GPG_KEY_ID=your_key_id
EOF

# Set proper permissions
chmod 600 .secrets
```

**Note:** For local act testing, you can use dummy values if you're not testing deployment:

```bash
cat > .secrets << 'EOF'
CENTRAL_USERNAME=test_user
CENTRAL_TOKEN=test_token
GPG_PRIVATE_KEY=test_key
GPG_PASSPHRASE=test_pass
GPG_KEY_ID=test_id
EOF
```

### Running Workflows Locally

```bash
# List all workflows
act -l

# Run the CI gate workflow (default job)
act -j quality-check

# Run specific job
act -j test-coverage

# Run with specific Java version matrix
act -j build-verification

# Run all jobs in CI gate workflow
act -j quality-check -j dependency-check -j test-coverage -j security-scan -j build-verification

# Run with secrets file
act --secret-file .secrets -j test-coverage

# Run with verbose output
act -v -j test-coverage

# Run with specific platform (useful if act defaults to wrong architecture)
act -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

### Common act Commands

```bash
# Quick test: Run quality checks only
act -j quality-check

# Full CI: Run all CI gate jobs
act -j quality-check -j dependency-check -j test-coverage -j security-scan -j build-verification

# Test deployment readiness (requires real secrets)
act -j deployment-ready --secret-file .secrets

# Run specific workflow by event type
act push -j quality-check
act pull_request -j test-coverage

# Dry run (show what would be executed)
act -n -j quality-check
```

### Troubleshooting act Issues

#### Docker Not Running

```bash
# Start Docker Desktop or Docker daemon
sudo systemctl start docker  # Linux
# Or open Docker Desktop on macOS/Windows

# Verify Docker is running
docker ps
```

#### Platform/Architecture Issues

```bash
# Use pre-built act images
act -P ubuntu-latest=catthehacker/ubuntu:act-latest

# Or use official Ubuntu image
act -P ubuntu-latest=node:16-buster-slim
```

#### Java Installation Issues

The CI workflow uses SDKMAN to install Java 26. If this fails locally:

```bash
# Install Java 26 locally first
sdk install java 26.ea.13-graal
sdk use java 26.ea.13-graal

# Then run act with local Java
act -j build-verification
```

#### Maven Repository Cache

```bash
# Clear Maven cache if needed
rm -rf ~/.m2/repository

# Use act's container cache
act --reuse --container-architecture linux/amd64 -j test-coverage
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
