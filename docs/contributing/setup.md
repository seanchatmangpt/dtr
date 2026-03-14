# Contributing: Development Setup

## Required Toolchain

| Tool | Minimum Version | Notes |
|---|---|---|
| Java | 26 (OpenJDK) | `--enable-preview` required; Java 25 and below are not supported |
| Maven | 4.0.0-rc-5+ | Use `mvnd` (daemon) locally; CI uses `./mvnw` |
| mvnd | 2.0.0+ | Maven Daemon; required for local development |

The build enforces Java 26 via `maven-enforcer-plugin` — it will fail immediately on any older JDK.

---

## 1. Install Java 26

### macOS (Desktop)

**Homebrew (recommended):**
```bash
brew install openjdk@26

# Apple Silicon (M1/M2/M3)
export JAVA_HOME=/opt/homebrew/opt/openjdk@26
# Intel Mac
export JAVA_HOME=/usr/local/opt/openjdk@26

export PATH=$JAVA_HOME/bin:$PATH
```

**SDKMAN:**
```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 26-open
sdk use java 26-open
```

**Official JDK (jdk.java.net/26):**
Download the macOS `.tar.gz`, extract to `~/Library/Java/JavaVirtualMachines/`, then:
```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 26)
export PATH=$JAVA_HOME/bin:$PATH
```

Add the `export` lines to `~/.zshrc` (or `~/.bashrc`) to make them permanent.

### Linux (Debian/Ubuntu)

```bash
sudo apt-get install openjdk-26-jdk
export JAVA_HOME=/usr/lib/jvm/java-26-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

---

## 2. Install Maven 4 / mvnd 2

### macOS

**Homebrew:**
```bash
brew install maven   # installs Maven 4 if available, or download manually (see below)
brew install mvnd    # Maven Daemon
```

**Manual install (both platforms):**
```bash
# Maven 4.0.0-rc-5
MAVEN_VERSION=4.0.0-rc-5
curl -LO "https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN_VERSION}/apache-maven-${MAVEN_VERSION}-bin.tar.gz"
tar -xzf "apache-maven-${MAVEN_VERSION}-bin.tar.gz" -C "$HOME/.dtr/tools"
export PATH="$HOME/.dtr/tools/apache-maven-${MAVEN_VERSION}/bin:$PATH"

# mvnd 2.0.0-rc-3 — pick your platform:
#   Apple Silicon:  maven-mvnd-2.0.0-rc-3-darwin-aarch64.tar.gz
#   Intel Mac:      maven-mvnd-2.0.0-rc-3-darwin-amd64.tar.gz
MVND_PLATFORM=darwin-aarch64   # change to darwin-amd64 for Intel
curl -LO "https://github.com/apache/maven-mvnd/releases/download/2.0.0-rc-3/maven-mvnd-2.0.0-rc-3-${MVND_PLATFORM}.tar.gz"
tar -xzf "maven-mvnd-2.0.0-rc-3-${MVND_PLATFORM}.tar.gz" -C "$HOME/.dtr/tools"
mv "$HOME/.dtr/tools/maven-mvnd-2.0.0-rc-3-${MVND_PLATFORM}" "$HOME/.dtr/tools/mvnd"
export PATH="$HOME/.dtr/tools/mvnd/bin:$PATH"
```

### Linux

```bash
# Maven 4.0.0-rc-5
curl -LO https://repo1.maven.org/maven2/org/apache/maven/apache-maven/4.0.0-rc-5/apache-maven-4.0.0-rc-5-bin.tar.gz
sudo tar -xzf apache-maven-4.0.0-rc-5-bin.tar.gz -C /opt
export PATH=/opt/apache-maven-4.0.0-rc-5/bin:$PATH

# mvnd 2.0.0-rc-3 (linux-amd64)
curl -LO https://github.com/apache/maven-mvnd/releases/download/2.0.0-rc-3/maven-mvnd-2.0.0-rc-3-linux-amd64.tar.gz
sudo tar -xzf maven-mvnd-2.0.0-rc-3-linux-amd64.tar.gz -C /opt
sudo mv /opt/maven-mvnd-2.0.0-rc-3-linux-amd64 /opt/mvnd
export PATH=/opt/mvnd/bin:$PATH
```

---

## 3. Clone the Repository

```bash
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr
```

---

## 4. Verify `.mvn/maven.config`

The file `.mvn/maven.config` must contain `--enable-preview` and `-Dmaven.compiler.release=26`:

```bash
cat .mvn/maven.config
```

Expected output:
```
--no-transfer-progress
--batch-mode
--enable-preview
-Dmaven.compiler.enablePreview=true
-Dmaven.compiler.release=26
```

---

## 5. Verify the Toolchain

```bash
java -version
# Expected: openjdk version "26.x.x"

mvnd --version
# Expected: mvnd 2.x.x / Maven 4.0.0-rc-5

echo $JAVA_HOME
# macOS Apple Silicon: /opt/homebrew/opt/openjdk@26
# macOS Intel:         /usr/local/opt/openjdk@26
# Linux:               /usr/lib/jvm/java-26-openjdk-amd64
```

---

## 6. Configure mvnd

Create or update `~/.m2/mvnd.properties`:

```properties
# macOS Apple Silicon:
mvnd.javaHome=/opt/homebrew/opt/openjdk@26
# macOS Intel:
# mvnd.javaHome=/usr/local/opt/openjdk@26
# Linux:
# mvnd.javaHome=/usr/lib/jvm/java-26-openjdk-amd64

mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
```

Set `mvnd.javaHome` to the path returned by:
```bash
# macOS
/usr/libexec/java_home -v 26

# Linux
echo /usr/lib/jvm/java-26-openjdk-amd64
```

---

## 7. Build and Test

```bash
# Build and run core tests
mvnd clean test -pl dtr-core

# Run integration tests
mvnd test -pl dtr-integration-test

# Full CI gate (what GitHub Actions runs)
mvnd verify
```

Expected output:
```
[INFO] BUILD SUCCESS
```

Run a specific test:
```bash
mvnd test -pl dtr-integration-test -Dtest=PhDThesisDocTest
cat target/docs/test-results/PhDThesisDocTest.md
```

---

## 8. Maven Proxy Setup (Enterprise Networks)

If Maven Central returns "too many authentication attempts":

```bash
# Start the proxy helper (included in the repo)
python3 maven-proxy-auth.py &

# Build through the proxy
mvnd clean test \
  -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
```

Or add proxy settings to `.mvn/jvm.config`:
```
-Dhttp.proxyHost=127.0.0.1
-Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1
-Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1
```

---

## 9. IDE Configuration

### IntelliJ IDEA

1. Open the root `pom.xml` as a Maven project.
2. In **File > Project Structure > SDK**, select Java 26.
3. In **Build > Compiler**, set "Target bytecode version" to 26.
4. In run configurations, add `--enable-preview` to **VM Options**.

### VS Code

1. Install the "Extension Pack for Java".
2. Open the project root folder.
3. The `.mvn/maven.config` file provides `--enable-preview` and `-Dmaven.compiler.release=26` automatically.
4. In `.vscode/settings.json`, set `"java.configuration.runtimes"` to point to your Java 26 installation:
   ```json
   {
     "java.configuration.runtimes": [
       {
         "name": "JavaSE-26",
         "path": "/opt/homebrew/opt/openjdk@26",
         "default": true
       }
     ]
   }
   ```

---

## 10. How the SessionStart Hook Works

When you open this project in Claude Code (web or desktop), `.claude/hooks/session-start.sh` runs automatically and:

| Platform | Java 26 | Maven 4 | mvnd 2 |
|---|---|---|---|
| **G Visor (web)** | Installs via `apt-get` to `/usr/lib/jvm/java-26-openjdk-amd64` | Downloads to `/opt/apache-maven-4.0.0-rc-5` | Downloads to `/opt/mvnd` |
| **macOS desktop** | Locates via `/usr/libexec/java_home -v 26`, Homebrew, or SDKMAN | Downloads to `~/.dtr/tools/` if not found | Downloads to `~/.dtr/tools/mvnd` if not found |

The hook writes `JAVA_HOME` and `PATH` into `CLAUDE_ENV_FILE` so the correct toolchain is active for the entire session. If Java 26 is not found on macOS, the hook **fails with install instructions** rather than silently using the wrong version.

---

## Project Structure Quick Reference

```
dtr/
  dtr-core/
    src/main/java/io/github/seanchatmangpt/dtr/   # Library source
    src/test/java/                                  # Unit tests (fast)

  dtr-benchmarks/
    src/main/java/                                  # JMH benchmark classes

  dtr-integration-test/
    src/test/java/                                  # Integration tests

  .mvn/maven.config                                 # --enable-preview + release=26
  .claude/hooks/session-start.sh                    # Auto-configures Java 26 toolchain
  maven-proxy-auth.py                               # Enterprise proxy helper
```

Changes to `dtr-core` should include unit tests under `dtr-core/src/test/`. Changes that affect end-to-end output should also be verified with the integration tests.
