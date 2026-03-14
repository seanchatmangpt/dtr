# Local Development Setup

This guide covers setting up a complete local development environment for DTR, including IDE configuration, tooling setup, and common troubleshooting.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [IDE Configuration](#ide-configuration)
- [Maven mvnd Setup](#maven-mvnd-setup)
- [Java 26 Installation](#java-26-installation)
- [Local CI Testing with act](#local-ci-testing-with-act)
- [Common Issues & Solutions](#common-issues--solutions)

---

## Prerequisites

### Required Tools

| Tool | Minimum Version | Recommended |
|------|-----------------|-------------|
| Java | 26.ea.13 | 26.ea.13-graal |
| Maven | 4.0.0-rc-3 | Latest 4.x |
| mvnd | 2.0.0 | Latest 2.x |
| Git | 2.x | Latest |
| act | 0.2.x | Latest |

---

## IDE Configuration

### IntelliJ IDEA

#### Project Setup

1. **Open Project**: File → Open → Select the DTR project root
2. **Trust Project**: Click "Trust Project" when prompted
3. **Wait for Indexing**: Let IntelliJ complete Maven import

#### JDK Configuration

```
File → Project Structure → Project Settings → Project
```

Set the following:
- **SDK**: 26.ea.13-graal (must be added manually)
- **Language Level**: 26 (Preview features)
- **Project compiler output**: `target/classes`

#### Adding Java 26 to IntelliJ

1. File → Project Structure → Platform Settings → SDKs
2. Click **+** → **Add JDK**
3. Navigate to your Java 26 installation:
   - SDKMAN: `~/.sdkman/candidates/java/26.ea.13-graal`
   - Manual: `/usr/lib/jvm/java-26-openjdk-amd64`

#### VM Options for Run Configuration

For each test/run configuration, add VM options:

```
--enable-preview
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.lang.reflect=ALL-UNNAMED
```

**Configure globally**:
```
Run → Edit Configurations → Edit Defaults → JUnit
Add VM options above to "VM options" field
```

#### Code Style Settings

DTR uses Spotless for code formatting. Import the style:

```
Settings → Editor → Code Style → Java
```

Or run before committing:
```bash
mvnd spotless:apply
```

#### Recommended Plugins

| Plugin | Purpose |
|--------|---------|
| Maven Runner | Faster Maven integration |
| String Manipulation | Case conversion, sorting |
| Rainbow Brackets | Bracket matching |
| GitToolBox | Git commit helper |

---

### VS Code

#### Workspace Settings

Create or edit `.vscode/settings.json`:

```json
{
  "java.configuration.runtimes": [
    {
      "name": "JavaSE-26",
      "default": true,
      "path": "/usr/lib/jvm/java-26-openjdk-amd64"
    }
  ],
  "java.compiler.release": "26",
  "java.jdt.ls.java.enablePreview": true,
  "java.format.settings.url": ".vscode/eclipse-java-style.xml",
  "editor.formatOnSave": true,
  "files.associations": {
    "*.md": "markdown"
  }
}
```

#### Launch Configuration

Create `.vscode/launch.json`:

```json
{
  "version": "0.2.0",
  "configurations": [
    {
      "type": "java",
      "name": "DTR Test (with preview)",
      "request": "launch",
      "mainClass": "io.github.seanchatmangpt.dtr.test.TestRunner",
      "vmArgs": "--enable-preview --add-opens java.base/java.lang=ALL-UNNAMED",
      "classPaths": ["dtr-core/target/test-classes"],
      "projectName": "dtr-core"
    }
  ]
}
```

---

### Eclipse IDE

#### Java 26 Setup in Eclipse

1. **Install Java 26** (see [Java 26 Installation](#java-26-installation))
2. **Add JDK**:
   - Window → Preferences → Java → Installed JREs
   - Click **Add** → **Standard VM**
   - Point to Java 26 installation directory
3. **Set Compiler Compliance**:
   - Window → Preferences → Java → Compiler
   - Set "Compiler compliance level" to 26
   - Enable "Enable preview features"

#### Project Import

1. File → Import → Maven → Existing Maven Projects
2. Select DTR project root
3. Click **Finish**

---

## Maven mvnd Setup

### What is mvnd?

mvnd (Maven Daemon) is an embeddable Maven that runs as a native process. It's significantly faster than standard Maven because the JVM is kept alive between builds.

### Installation

#### macOS (Homebrew)

```bash
brew install mvnd
mvnd --version
```

#### Linux

```bash
wget https://github.com/apache/maven-mvnd/releases/download/2.0.0/mvnd-2.0.0-linux-amd64.tar.gz
tar -xzf mvnd-2.0.0-linux-amd64.tar.gz
export PATH="$PATH:$PWD/mvnd-2.0.0-linux-amd64/bin"

# Add to .bashrc or .zshrc
echo 'export PATH="$PATH:$HOME/mvnd-2.0.0-linux-amd64/bin"' >> ~/.bashrc
```

#### Windows

```powershell
# Download from https://github.com/apache/maven-mvnd/releases
# Extract and add to PATH
```

### mvnd Configuration

Create `~/.m2/mvnd.properties` (optional but recommended):

```properties
# Maven Daemon configuration
mvnd.enableJavadoc=true
mvnd.maxHeapSize=2g
mvnd.minHeapSize=1g
mvnd.threadCount=1
mvnd.noDaemon=false
mvnd.idleTimeout=3600000
mvnd.keepAlive=10000
```

### Using mvnd vs mvn

| Command | Standard Maven | mvnd |
|---------|----------------|------|
| Clean build | `mvn clean install` | `mvnd clean install` |
| Skip tests | `mvn install -DskipTests` | `mvnd install -DskipTests` |
| Stop daemon | N/A | `mvnd --stop` |
| Status | N/A | `mvnd --status` |

### Performance Comparison

```bash
# Standard Maven (cold start)
time mvn clean install
# real: 0m45.000s

# mvnd (warm daemon)
time mvnd clean install
# real: 0m15.000s
```

---

## Java 26 Installation

### Option 1: SDKMAN (Recommended)

SDKMAN is the version manager for Java, Groovy, and other JVM tools.

#### Installation

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
```

#### Install Java 26

```bash
sdk list java | grep 26
sdk install java 26.ea.13-graal
sdk use java 26.ea.13-graal

# Set as default
sdk default java 26.ea.13-graal
```

#### Verify

```bash
java -version
# openjdk version "26.ea.13-graal" 2026-03-17
# OpenJDK Runtime Environment (build 26.ea.13-graal+0-build)
# OpenJDK 64-Bit Server VM (build 26.ea.13-graal+0-build, mixed mode)
```

### Option 2: Manual Installation

#### Linux (Ubuntu/Debian)

```bash
wget https://download.java.net/java/EA/ea26/graal/openjdk-26-ea+13_graal/0/GPL/openjdk-26-ea+13_graal-linux-x64_bin.tar.gz
tar -xzf openjdk-26-ea+13_graal-linux-x64_bin.tar.gz
sudo mv jdk-26 /usr/lib/jvm/java-26-openjdk-amd64

# Add to PATH (add to ~/.bashrc or ~/.zshrc)
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
```

#### macOS

```bash
brew tap homebrew/cask-versions
brew install --cask java26ea

# Or use SDKMAN (recommended)
sdk install java 26.ea.13-graal
```

### .sdkmanrc File

The DTR project includes a `.sdkmanrc` file for automatic SDKMAN setup:

```bash
cat .sdkmanrc
# java=26.ea.13-graal
# maven=4.0.0-rc-3

# Install and use project-specified versions
sdk env
```

---

## Local CI Testing with act

### What is act?

**act** runs GitHub Actions workflows locally using Docker. It's essential for testing CI/CD pipelines before pushing.

### Installation

```bash
# macOS
brew install act

# Linux
curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# Via cargo
cargo install act
```

### .actrc Configuration

The DTR project includes an `.actrc` file:

```
-P ubuntu-latest=catthehacker/ubuntu:act-latest
--container-architecture linux/amd64
--secret GITHUB_TOKEN=
--env JAVA_VERSION=26
-W .github/workflows/
```

### catthehacker/ubuntu:act-latest Image

This Docker image includes:

| Tool | Version | Purpose |
|------|---------|---------|
| Docker | Latest | Container operations |
| Node.js | 20.x | JavaScript tools |
| Python | 3.x | Python scripts |
| Ruby | 3.x | Ruby gems |
| Go | Latest | Go tools |
| Rust | Latest | Rust cargo |
| Git | Latest | Version control |
| curl/wget | Latest | HTTP tools |
| GitHub CLI | Latest | gh commands |

**NOT included: Java** - must be installed via SDKMAN in workflows.

### Setting Up Secrets for act

Create `.secrets` file (git-ignored):

```bash
cat > .secrets << 'EOF'
CENTRAL_USERNAME=your_username
CENTRAL_TOKEN=your_token
GPG_PRIVATE_KEY=$(cat ~/.gnupg/private.key | base64)
GPG_PASSPHRASE=your_passphrase
GPG_KEY_ID=your_key_id
EOF
chmod 600 .secrets
```

For non-deployment testing, use dummy values:

```bash
cat > .secrets << 'EOF'
CENTRAL_USERNAME=test_user
CENTRAL_TOKEN=test_token
GPG_PRIVATE_KEY=dGVzdF9rZXk=
GPG_PASSPHRASE=test_pass
GPG_KEY_ID=ABCD1234
EOF
```

### Running Workflows Locally

```bash
# List all workflows
act -l

# Run quality check job
act -j quality-check

# Run test coverage with secrets
act --secret-file .secrets -j test-coverage

# Run entire CI gate
act -j quality-check -j dependency-check -j test-coverage -j security-scan -j build-verification

# Dry run (show what would execute)
act -n -j quality-check

# Verbose output
act -v -j test-coverage
```

### act Troubleshooting

| Issue | Solution |
|-------|----------|
| Docker not running | Start Docker Desktop or `sudo systemctl start docker` |
| Platform mismatch | Add `-P ubuntu-latest=catthehacker/ubuntu:act-latest` |
| Java not found in container | Ensure workflow installs Java via SDKMAN |
| Secrets not loaded | Use `--secret-file .secrets` |
| Container cache issues | Run `docker system prune -f` |

---

## Common Issues & Solutions

### Port Conflicts

| Port | Used By | Conflict? |
|------|---------|-----------|
| 8080 | Integration test server | Yes |
| 5005 | Debug port | Sometimes |
| 5006 | Debug port (Java 26) | Sometimes |

**Solution**: Kill process or change port:

```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or run on different port
mvnd -Dserver.port=8081 test
```

### Docker Issues

#### Docker Desktop Not Running (macOS/Windows)

```bash
# Start Docker Desktop
open -a Docker

# Verify
docker ps
```

#### Docker Permission Denied (Linux)

```bash
sudo usermod -aG docker $USER
newgrp docker
```

#### act Container Image Issues

```bash
# Pull latest images
act pull

# Clear Docker cache
docker system prune -af

# Use specific image
act -P ubuntu-latest=catthehacker/ubuntu:act-latest
```

### Network/Proxy Problems

#### Corporate Proxy

```bash
# Maven proxy settings
mvnd clean install \
  -Dhttp.proxyHost=proxy.company.com \
  -Dhttp.proxyPort=8080 \
  -Dhttps.proxyHost=proxy.company.com \
  -Dhttps.proxyPort=8080
```

#### Git Proxy

```bash
git config --global http.proxy http://proxy.company.com:8080
git config --global https.proxy https://proxy.company.com:8080
```

### Compilation Failures

#### --enable-preview Errors

```bash
# Ensure .mvn/maven.config contains:
--enable-preview
-Dmaven.compiler.release=26
```

#### Maven OutOfMemory

```bash
# Increase Maven heap
mvnd clean install -Dmaven.test.skip=true \
  -Dmaven.compiler.maxHeapSize=4096m
```

#### mvnd Daemon Issues

```bash
# Stop daemon
mvnd --stop

# Check status
mvnd --status

# Restart
mvnd clean install
```

### IDE-Specific Issues

#### IntelliJ: "Cannot resolve symbol"

```bash
# Invalidate caches and restart
File → Invalidate Caches → Invalidate and Restart

# Reimport Maven
Right-click pom.xml → Maven → Reload Project
```

#### VS Code: "Java 26 not found"

```bash
# Ensure java.jdt.ls.java.enablePreview is true
# Check Java home in settings.json
# Reload VS Code window
```

---

## Quick Start Checklist

```bash
# 1. Clone repository
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

# 2. Install Java 26 via SDKMAN
sdk install java 26.ea.13-graal
sdk use java 26.ea.13-graal

# 3. Install mvnd
brew install mvnd  # macOS
# or download from GitHub for Linux

# 4. Verify environment
java -version    # Should be 26.ea.13+
mvnd --version   # Should be Maven 4.0.0-rc-3+
act --version    # Should be 0.2.x+

# 5. Build project
mvnd clean install

# 6. Run tests
mvnd test

# 7. Generate documentation
mvnd test -pl dtr-integration-test

# 8. Run local CI
act -j quality-check
```

---

## Next Steps

- [Contributing Guide](../CONTRIBUTING.md) — How to contribute to DTR
- [Infrastructure Research](infrastructure-research.md) — DevOps architecture details
- [Failure Recovery](failure-recovery.md) — Handling release failures

---

**Last Updated:** March 14, 2026
**Branch:** feat/java-26-with-calver
