# Act Testing Guide for DTR

**Last Updated:** March 14, 2026

This guide covers local testing of DTR's GitHub Actions workflows using [act](https://github.com/nektos/act).

---

## What is Act?

Act is a command-line tool that runs GitHub Actions workflows locally using Docker. It allows you to:

- Test workflows before pushing to GitHub
- Debug workflow issues without waiting for CI
- Validate workflow syntax and logic
- Run specific jobs or entire workflows
- Test with different event types (push, pull_request, etc.)

---

## Installation

### macOS

```bash
# Using Homebrew (recommended)
brew install act

# Using MacPorts
sudo port install act

# Using curl
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash
```

### Linux

```bash
# Using curl
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Using Homebrew (Linuxbrew)
brew install act

# Arch Linux
yay -S act

# Ubuntu/Debian (from GitHub releases)
curl -sLO https://github.com/nektos/act/releases/latest/download/act_Linux_x86_64.tar.gz
tar xzf act_Linux_x86_64.tar.gz
sudo mv act /usr/local/bin/
```

### Windows

```powershell
# Using Chocolatey
choco install act-cli

# Using Scoop
scoop install act

# Using winget
winget install nektos.act
```

### Verify Installation

```bash
act --version
```

---

## DTR Configuration

DTR uses a `.actrc` file in the repository root for default act configuration:

```
-P ubuntu-latest=catthehacker/ubuntu:act-latest
--container-architecture linux/amd64
--secret-file .secrets
--env JAVA_VERSION=26
--env MAVEN_OPTS="-Xmx4g -Xms2g"
--env ACT=true
-W .github/workflows/
```

This configuration:
- Uses the `catthehacker/ubuntu:act-latest` Docker image for `ubuntu-latest`
- Sets the container architecture to `linux/amd64` (required for macOS ARM)
- Uses `.secrets` file for secret management (never inline secrets)
- Sets Java version to 26
- Allocates 4GB heap for Maven builds (increased from 2GB)
- Sets `ACT=true` environment variable for workflow conditional execution
- Points to the `.github/workflows/` directory

---

## Secret Management

### Creating the .secrets File

Create a `.secrets` file in the repository root (NEVER commit this file):

```bash
cat > .secrets << 'EOF'
CENTRAL_USERNAME=your-maven-central-username
CENTRAL_TOKEN=your-maven-central-token
GPG_PRIVATE_KEY=$(cat ~/.gnupg/private.key | base64)
GPG_PASSPHRASE=your-gpg-passphrase
GPG_KEY_ID=your-gpg-key-id-last-8-chars
EOF
```

### Required Secrets by Workflow

| Workflow | Secrets Required | Notes |
|----------|------------------|-------|
| `ci-gate.yml` | None (for most jobs) | Trivy scan skipped in act |
| `quality-gates.yml` | None | PR comments skipped in act |
| `gpg-key-management.yml` | `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE` | PR creation skipped in act |
| `publish.yml` | All 5 secrets | Deploy skipped in act |
| `publish-rc.yml` | `GITHUB_TOKEN` | Deploy skipped in act |
| `deployment-automation.yml` | None | GitHub API calls skipped in act |
| `publish-validation.yml` | GPG secrets | GPG validation skipped in act |
| `test-deploy.yml` | All secrets | GPG/deploy skipped in act |

### Base64 Encoding GPG Key

```bash
# Export and encode your GPG private key
gpg --armor --export-secret-keys YOUR_KEY_ID | base64 > gpg-key-base64.txt

# Or inline for .secrets file
echo "GPG_PRIVATE_KEY=$(gpg --armor --export-secret-keys YOUR_KEY_ID | base64 -w 0)"
```

### .gitignore Entry

Ensure `.secrets` is in your `.gitignore`:

```gitignore
# Act secrets
.secrets
```

---

## Working Act Commands for DTR

### List Available Workflows

```bash
# List all workflows
act -l

# List workflows with more detail
act -lW .github/workflows/
```

### Quality Gates Workflow (Fully Compatible)

```bash
# Run entire quality gates workflow
act -W .github/workflows/quality-gates.yml

# Run specific jobs
act -j validate-guards
act -j quality-check
act -j dependency-check
act -j security-scan
act -j integration-tests
```

### GPG Key Management Workflow (Fully Compatible)

```bash
# Run with secrets
act -W .github/workflows/gpg-key-management.yml --secret-file .secrets

# Test specific action
act -W .github/workflows/gpg-key-management.yml --input action=backup
```

### CI Gate Workflow

```bash
# Run individual jobs (most compatible)
act -j quality-check --secret-file .secrets
act -j dependency-check
act -j test-coverage
act -j security-scan

# Note: build-verification job uses matrix strategy
# Run specific matrix combination
act -j build-verification --matrix java-version:21
```

### Publish Workflow

```bash
# Build job only (no secrets needed)
act -W .github/workflows/publish.yml -j build

# Deploy job requires real Maven Central secrets
act -W .github/workflows/publish.yml -j deploy --secret-file .secrets

# Release job
act -W .github/workflows/publish.yml -j release --secret-file .secrets
```

### RC Publish Workflow

```bash
# After SDKMAN conversion
act -W .github/workflows/publish-rc.yml --secret-file .secrets
```

### Dry Run Mode

```bash
# Show what would execute without running
act -n

# Dry run specific workflow
act -n -W .github/workflows/ci-gate.yml
```

### Debug Mode

```bash
# Verbose output
act -v

# Debug specific job
act -j quality-check -v
```

### Event Types

```bash
# Simulate push event
act push

# Simulate pull request
act pull_request

# Simulate tag push
act push --ref refs/tags/v2026.3.0
```

---

## SDKMAN Java 26 Setup

DTR requires Java 26 (early access) which is not available through `actions/setup-java@v4`. Workflows must use SDKMAN.

### The SDKMAN Pattern

```yaml
- name: Set up Java 26 via SDKMAN
  run: |
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 26.ea.13-graal
    echo "JAVA_HOME=$HOME/.sdkman/candidates/java/current" >> $GITHUB_ENV
    echo "$HOME/.sdkman/candidates/java/current/bin" >> $GITHUB_PATH
```

### Why SDKMAN?

1. **Early Access Support**: Java 26 EA is not in actions/setup-java
2. **GraalVM**: DTR uses GraalVM builds for performance
3. **Consistency**: Same setup in CI and local act testing

### Current SDKMAN-Compatible Workflows

- `ci-gate.yml` (reference implementation)
- `publish.yml`
- `quality-gates.yml`

### Workflows Needing SDKMAN Conversion

The following workflows still use `actions/setup-java@v4` and need conversion:

- `ci.yml`
- `deployment-automation.yml`
- `publish-rc.yml`
- `test-deploy.yml`
- `update-publish.yml`
- `publish-validation.yml`
- `monitor-deployment.yml`

---

## Troubleshooting

### Common Issues

#### 1. Docker Permission Denied

```bash
# Add user to docker group (Linux)
sudo usermod -aG docker $USER

# Log out and back in, or:
newgrp docker
```

#### 2. Container Architecture Mismatch (macOS ARM)

```bash
# Use the --container-architecture flag
act --container-architecture linux/amd64

# Or ensure .actrc contains:
# --container-architecture linux/amd64
```

#### 3. Out of Docker Images

```bash
# Pull latest Docker images
act pull

# Clear Docker cache
docker system prune -f

# Remove all act containers
docker rm -f $(docker ps -aq --filter label=com.github.act)
```

#### 4. Job Name Interpolation Error

Some workflows use `${{ env.VARIABLE }}` in job names, which act cannot parse:

```yaml
# This fails in act:
name: Build & Test (Java ${{ env.JAVA_VERSION }})

# Fix: Use static name
name: Build & Test
```

#### 5. Matrix Strategy Issues

```bash
# Run specific matrix combination
act -j build-verification --matrix java-version:21

# List matrix options
act -j build-verification -l
```

#### 6. Secrets Not Loading

```bash
# Verify secrets file syntax (no spaces around =)
cat .secrets

# Use absolute path to secrets file
act --secret-file /full/path/to/.secrets

# Pass individual secrets
act -s GPG_PASSPHRASE="your-passphrase"
```

#### 7. Java Version Mismatch

```bash
# Verify .actrc has correct version
cat .actrc | grep JAVA_VERSION

# Should show: --env JAVA_VERSION=26
```

#### 8. Workflow Not Found

```bash
# Specify workflow file explicitly
act -W .github/workflows/ci-gate.yml

# Check working directory
pwd
```

### Debugging Commands

```bash
# Show Docker containers being used
act --dryrun

# List all jobs in a workflow
act -W .github/workflows/ci-gate.yml -l

# Show environment variables
act -j quality-check --env MY_VAR=value

# Run with different shell
act --shell bash
```

### Log Analysis

```bash
# View act logs
cat /tmp/act-*.log

# View Docker container logs
docker logs $(docker ps -q --filter label=com.github.act | head -1)
```

---

## Workflow Compatibility Matrix

| Workflow | Act Status | Notes |
|----------|------------|-------|
| `quality-gates.yml` | Fully Compatible | No secrets required |
| `gpg-key-management.yml` | Fully Compatible | Requires GPG secrets |
| `monitor-deployment.yml` | Compatible | Uses setup-java (needs SDKMAN) |
| `update-publish.yml` | Compatible | Uses setup-java (needs SDKMAN) |
| `ci-gate.yml` | Partial | Matrix strategy requires specific combinations |
| `publish-validation.yml` | Partial | Matrix strategy |
| `publish-rc.yml` | Partial | Uses setup-java (needs SDKMAN) |
| `ci.yml` | Incompatible | Job name interpolation, setup-java |
| `publish.yml` | Incompatible | Requires real Maven Central secrets |
| `deployment-automation.yml` | Incompatible | Environment interpolation, setup-java |
| `test-deploy.yml` | Incompatible | Environment interpolation, setup-java |

---

## Best Practices

1. **Always use `.secrets` file** - Never pass secrets on command line
2. **Test incrementally** - Run individual jobs first, then full workflows
3. **Use dry run** - Always `act -n` before running expensive operations
4. **Keep Docker clean** - Run `docker system prune` periodically
5. **Match CI environment** - Use the same Java version and Maven version as CI
6. **Check .actrc** - Ensure configuration matches current project requirements

---

## Quick Reference

```bash
# Install
brew install act

# List workflows
act -l

# Run quality gates
act -W .github/workflows/quality-gates.yml

# Run with secrets
act --secret-file .secrets

# Dry run
act -n

# Debug
act -v

# Specific job
act -j quality-check

# Pull images
act pull

# Clean up
docker system prune -f
```

---

## Related Documentation

- [CLAUDE.md](/CLAUDE.md) - DTR development guide
- [GitHub Actions Workflows](/.github/workflows/) - Workflow definitions
- [Act Documentation](https://github.com/nektos/act) - Official act docs
- [SDKMAN](https://sdkman.io/) - Java version manager

---

## SDKMAN Caching (New)

All workflows now include SDKMAN caching for faster Java 26 setup:

```yaml
- name: Cache SDKMAN
  uses: actions/cache@v4
  with:
    path: |
      ~/.sdkman/candidates/java
      ~/.sdkman/archives
    key: ${{ runner.os }}-sdkman-java-26-ea-13-${{ hashFiles('.github/workflows/*.yml') }}
```

**Performance improvements:**
- First run: ~60s (download Java 26)
- Cached runs: ~5-10s (restore from cache)

## Act Environment Detection

The `.actrc` sets `ACT=true` which workflows can use to skip GitHub API-dependent steps:

```yaml
- name: Upload to GitHub
  if: ${{ !env.ACT }}
  uses: github/codeql-action/upload-sarif@v2
```

## Updated Troubleshooting

### SDKMAN OOM (Exit 137)
If you see exit 137 errors, the SDKMAN cache should help. If still failing:
```bash
# Increase Docker memory in Docker Desktop settings
# Or use --env MAVEN_OPTS="-Xmx3g -Xms1g"
```

### Cached SDKMAN Not Working
```bash
# Clear act's cache
rm -rf ~/.cache/act
act -j quality-check  # First run populates cache
act -j quality-check  # Second run should be fast
```
