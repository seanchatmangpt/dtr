# Contributing: Development Setup

## Required Toolchain

| Tool | Minimum Version | Notes |
|---|---|---|
| Java | 25 (OpenJDK) | `--enable-preview` required; Java 24 and below are not supported |
| Maven | 4.0.0-rc-5+ | Use `/opt/apache-maven-4.0.0-rc-5/bin/mvn` directly |
| mvnd | 2.0.0+ | Maven Daemon; preferred for local development |

Do not use `./mvnw` — the Maven wrapper downloads Maven 3, which is incompatible.

---

## 1. Install Java 25

### Using SDKMAN (recommended)

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 25.0.2-open
sdk use java 25.0.2-open
```

### Using system packages (Debian/Ubuntu)

```bash
sudo apt-get install openjdk-25-jdk
```

### Set JAVA_HOME

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

Add these lines to `~/.bashrc` or `~/.zshrc` to make them permanent.

---

## 2. Install Maven 4 / mvnd 2

### Maven 4

```bash
# Download and extract
curl -LO https://downloads.apache.org/maven/maven-4/4.0.0-rc-5/binaries/apache-maven-4.0.0-rc-5-bin.tar.gz
tar -xzf apache-maven-4.0.0-rc-5-bin.tar.gz -C /opt
export PATH=/opt/apache-maven-4.0.0-rc-5/bin:$PATH
```

### mvnd 2 (preferred)

```bash
curl -LO https://github.com/apache/maven-mvnd/releases/download/2.0.0/maven-mvnd-2.0.0-linux-amd64.tar.gz
tar -xzf maven-mvnd-2.0.0-linux-amd64.tar.gz -C /opt
ln -sf /opt/maven-mvnd-2.0.0-linux-amd64 /opt/mvnd
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

The file `.mvn/maven.config` must contain `--enable-preview`. Check it:

```bash
cat .mvn/maven.config
```

Expected output includes:
```
--enable-preview
```

If it is missing, add it:

```bash
echo '--enable-preview' >> .mvn/maven.config
```

---

## 5. Verify the Toolchain

```bash
java -version
# Expected: openjdk version "25.x.x"

mvnd --version
# Expected: mvnd 2.x.x / Maven 4.0.0-rc-5

echo $JAVA_HOME
# Expected: /usr/lib/jvm/java-25-openjdk-amd64
```

---

## 6. Build and Test

```bash
# Build and run core tests
mvnd clean test -pl dtr-core

# Run integration tests
mvnd test -pl dtr-integration-test
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

## 7. Maven Proxy Setup (Enterprise Networks)

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

If still failing, restart the daemon and proxy:

```bash
mvnd --stop
pkill -f maven-proxy
python3 maven-proxy-auth.py &
mvnd clean test
```

---

## 8. IDE Configuration

### IntelliJ IDEA

1. Open the root `pom.xml` as a Maven project.
2. In **File > Project Structure > SDK**, select Java 25.
3. In **Build > Compiler**, set "Target bytecode version" to 25.
4. In run configurations, add `--enable-preview` to **VM Options**.

### VS Code

1. Install the "Extension Pack for Java".
2. Open the project root folder.
3. The `.mvn/maven.config` file provides `--enable-preview` to the compiler automatically when building through Maven tasks.
4. In `.vscode/settings.json`, set `"java.configuration.runtimes"` to point to your Java 25 installation.

---

## 9. mvnd Daemon Configuration

mvnd stores settings in `~/.m2/mvnd.properties`. Recommended configuration for DTR development:

```properties
mvnd.javaHome=/usr/lib/jvm/java-25-openjdk-amd64
mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
```

If tests behave unexpectedly after changing configuration, restart the daemon:

```bash
mvnd --stop
mvnd clean test
```

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

  .mvn/maven.config                                 # Must contain --enable-preview
  maven-proxy-auth.py                               # Enterprise proxy helper
```

Changes to `dtr-core` should include unit tests under `dtr-core/src/test/`. Changes that affect end-to-end output should also be verified with the integration tests.
