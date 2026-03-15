# Contributing to DTR (Documentation Testing Runtime)

Thank you for your interest in contributing to DTR! We welcome contributions of all kinds including bug reports, documentation improvements, feature requests, and pull requests.

## Quick Start

**New to DTR?** Start with our **[30-Minute Quickstart Guide](docs/CONTRIBUTING_QUICKSTART.md)** to get from zero to your first contribution in under 30 minutes.

**Experienced contributor?** Jump to the section you need:

- [Development Setup](#development-setup) - Get your environment ready
- [Code Style](#code-style) - Follow our conventions
- [Testing](#testing) - Ensure quality before submitting
- [Pull Request Process](#pull-request-process) - Submit your changes

## Table of Contents

- [Quick Start](#quick-start)
- [Development Setup](#development-setup)
- [Development Workflow](#development-workflow)
- [Code Style](#code-style)
- [Testing](#testing)
- [Pull Request Process](#pull-request-process)
- [Local CI Testing](#local-ci-testing)
- [Additional Resources](#additional-resources)

## Development Setup

### Prerequisites

- **Java 26** (Java 26.ea.13-graal or later) with preview features
- **Maven 4.0.0-rc-3+** or **mvnd 2.0.0+** (recommended for speed)
- **Git** for version control

For detailed setup instructions, see **[Environment Setup Guide](docs/contributing/setup.md)**.

### Quick Installation

```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/dtr.git
cd dtr

# Verify Java version (must be 26+)
java -version
# Expected: openjdk version "26.ea.13" or higher

# Verify Maven (mvnd recommended)
mvnd --version
# Expected: Maven 4.0.0-rc-3+ or mvnd 2.0.0+

# Build the project
mvnd clean verify

# Run tests
mvnd test
```

### Installing Java 26 via SDKMAN

```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 26
sdk install java 26.ea.13-graal
sdk use java 26.ea.13-graal

# Verify
java -version
```

## Development Workflow

### Making Changes

1. **Create a feature branch** from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
   > **Note:** Agent/automated branches follow `claude/description-XXXXX` pattern and are managed by tooling.

2. **Make your changes** following code style guidelines

3. **Test your changes**:
   ```bash
   # Run all tests
   mvnd test

   # Run specific module
   mvnd test -pl dtr-core

   # Full verification
   mvnd clean verify
   ```

4. **Commit with clear messages**:
   ```bash
   git commit -m "feat: Add support for new feature

   - Implements feature X
   - Adds tests for Y
   - Closes #123"
   ```

5. **Push to your fork**:
   ```bash
   git push origin feature/your-feature-name
   ```

For detailed workflow guidance, see **[Making Changes Guide](docs/contributing/making-changes.md)**.

### Project Structure

```
dtr/
├── dtr-core/                 # Main library with RenderMachine, say* API
├── dtr-integration-test/     # Integration tests (Ninja Framework app)
├── dtr-benchmarks/           # JMH performance benchmarks
├── dtr-cli/                  # Python CLI wrapper
├── docs/                     # User documentation (Diataxis structure)
├── pom.xml                   # Root Maven configuration
└── CLAUDE.md                 # Developer quick reference
```

For a detailed codebase tour, see **[Codebase Tour](docs/contributing/codebase-tour.md)**.

## Code Style

### Java Conventions

- Follow standard Java naming and structure conventions
- Use the formatting configured in `.mvn/maven.config`
- Keep methods focused and concise (single responsibility)
- Add meaningful Javadoc for public APIs
- **Java 26+ features are encouraged**: records, sealed classes, pattern matching, gatherers

### Commit Message Format

Use conventional commit prefixes:

- `feat:` - New feature
- `fix:` - Bug fix
- `docs:` - Documentation changes
- `test:` - Adding or updating tests
- `refactor:` - Code refactoring without feature changes
- `chore:` - Build process, dependencies, etc.

**Example:**
```bash
# Good
git commit -m "feat: Add WebSocket streaming support

- Implements RFC 6455 WebSocket protocol
- Adds WebSocketClient interface
- Includes integration tests
- Closes #456"

# Bad
git commit -m "fixed stuff"
```

### Documentation Style

Documentation follows the [Diataxis framework](https://diataxis.fr/):
- **Tutorials** - Step-by-step learning lessons
- **How-to guides** - Solution-oriented instructions
- **Reference** - Technical information (API docs)
- **Explanation** - Conceptual context

## Testing

### Requirements

All PRs must:

1. **Include tests** for new features
2. **Pass all tests**: `mvnd test`
3. **Maintain coverage** - Don't reduce test coverage
4. **Generate documentation**: `mvnd test -pl dtr-integration-test`

### Running Tests

```bash
# Run all tests
mvnd test

# Run specific test class
mvnd test -Dtest=PhDThesisDocTest

# Run integration tests
mvnd test -pl dtr-integration-test

# Run with coverage
mvnd clean verify -Djacoco.skip=false

# Run specific module
mvnd test -pl dtr-core
```

### Testing from Documentation

DTR's unique feature: documentation is generated from tests:

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

## Pull Request Process

### Before Submitting

1. **Test locally**: Ensure `mvnd clean verify` passes
2. **Update documentation**: Add/ update docs for new features
3. **Check style**: Follow code style guidelines
4. **Write clear description**: Explain what changed and why

### Creating a Pull Request

1. **Push your branch** to your fork
2. **Create PR** with:
   - Clear title (e.g., "feat: Add WebSocket streaming support")
   - Description of changes
   - Reference to related issues (e.g., "Fixes #123")
   - Test coverage information

3. **Fill in PR template**:
   ```markdown
   ## What Changed
   - Added feature X
   - Fixed bug Y

   ## Why
   Explain the motivation for this change

   ## Testing
   - Added unit tests for X
   - Verified integration tests pass
   - Manual testing steps

   ## Checklist
   - [x] Tests pass locally
   - [x] Documentation updated
   - [x] Commit messages follow conventions
   ```

### After Submitting

1. **Watch CI results** - All checks must pass (green ✓)
2. **Address feedback** - Respond to reviewer comments
3. **Celebrate!** - Once merged, you're a DTR contributor!

## Local CI Testing

**act** allows you to run GitHub Actions workflows locally before pushing.

### Installation

```bash
# macOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Verify
act --version
```

### Running Workflows Locally

```bash
# List all workflows
act -l

# Run quality checks
act -j quality-check

# Run all CI gate jobs
act -j quality-check -j test-coverage -j build-verification

# Run with secrets file (create .secrets first)
act --secret-file .secrets -j test-coverage
```

### Creating Secrets File

```bash
# Create .secrets file (git-ignored)
cat > .secrets << 'EOF'
CENTRAL_USERNAME=test_user
CENTRAL_TOKEN=test_token
GPG_PRIVATE_KEY=test_key
GPG_PASSPHRASE=test_pass
GPG_KEY_ID=test_id
EOF

chmod 600 .secrets
```

## Additional Resources

### For New Contributors

- **[30-Minute Quickstart](docs/CONTRIBUTING_QUICKSTART.md)** - Get started fast
- **[Environment Setup](docs/contributing/setup.md)** - Detailed setup instructions
- **[Codebase Tour](docs/contributing/codebase-tour.md)** - Project architecture overview

### For Developers

- **[Making Changes Guide](docs/contributing/making-changes.md)** - Development workflow details
- **[CLAUDE.md](CLAUDE.md)** - Developer quick reference
- **[say* API Reference](docs/reference/say-api.md)** - Complete API documentation
- **[Java 26 Features](docs/explanation/java26-code-reflection.md)** - Advanced Java 26 features in DTR

### For Maintainers

- **[Release Process](docs/contributing/releasing.md)** - How releases are managed
- **[Architecture Overview](docs/ARCHITECTURE.md)** - System design documentation
- **[Troubleshooting](docs/TROUBLESHOOTING.md)** - Common issues and solutions

### Community

- **[GitHub Issues](https://github.com/seanchatmangpt/dtr/issues)** - Bug reports and feature requests
- **[GitHub Discussions](https://github.com/seanchatmangpt/dtr/discussions)** - General questions and ideas
- **[Full Documentation Index](docs/index.md)** - All DTR documentation

---

**Thank you for contributing to DTR!** 🎉

Your contributions help make documentation and testing better for everyone. Every contribution matters, whether it's a typo fix, a new test, or a major feature.

**Questions?** Open a [GitHub Discussion](https://github.com/seanchatmangpt/dtr/discussions) or reach out to maintainers.
