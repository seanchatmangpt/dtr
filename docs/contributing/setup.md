# Contributing: Development Setup

## Required toolchain

| Tool | Version | Notes |
|---|---|---|
| Java | 25 (LTS) | OpenJDK 25+ |
| Maven | 4.0.0-rc-5+ | `mvn` from `/opt/apache-maven-4.0.0-rc-5/bin/mvn` |
| mvnd | 2.0.0-rc-3+ | Maven Daemon, preferred for local development |

**Do not use** `./mvnw` — the Maven wrapper downloads Maven 3.

## Verify the toolchain

```bash
java -version
# Expected: openjdk version "25.x.x"

mvnd --version
# Expected: mvnd 2.0.0-rc-3 / Maven 4.0.0-rc-5

echo $JAVA_HOME
# Expected: /usr/lib/jvm/java-25-openjdk-amd64
```

If `JAVA_HOME` is wrong, set it:

```bash
export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

## Clone and build

```bash
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr

mvnd clean install
```

Expected output:
```
[INFO] BUILD SUCCESS
```

## Run all tests

```bash
# Core tests only (fast)
mvnd test -pl dtr-core

# Integration tests (starts Jetty server — ~30 seconds)
mvnd verify -pl dtr-integration-test
```

## Run a specific test

```bash
mvnd test -pl dtr-core -Dtest=DTRTest
```

## mvnd configuration

mvnd stores settings in `~/.m2/mvnd.properties`. The recommended configuration for DTR development:

```properties
mvnd.javaHome=/usr/lib/jvm/java-25-openjdk-amd64
mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
```

If tests behave strangely after changing configuration, restart the daemon:

```bash
mvnd --stop
mvnd clean test
```

## IDE setup

### IntelliJ IDEA

1. Open the root `pom.xml` as a Maven project
2. In **File → Project Structure → SDK**, select Java 25
3. In **Build → Compiler**, ensure "Target bytecode version" is 25
4. Enable preview features: **VM Options** → `--enable-preview` in run configurations

### VS Code

Install the "Extension Pack for Java". Open the project root. The `.mvn/maven.config` file provides `--enable-preview` to the compiler automatically.

## Project structure reminder

```
dtr-core/
  src/main/java/org/r10r/dtr/   # The library source
  src/test/java/org/r10r/dtr/   # Unit tests for the library

dtr-integration-test/
  src/test/java/controllers/           # Integration tests (require server)
  src/main/java/                       # Ninja web app used as test server
```

Changes to `dtr-core` should include unit tests in `dtr-core/src/test/`. Changes that affect end-to-end behavior should be verified with the integration tests.
