#!/bin/bash
# DocTester Session Start Hook
# Enforces ONLY: Java 25, Maven 4 (4.0.0-rc-5), and mvnd 2 (Maven Daemon).
# No other Java or Maven version is acceptable.
set -euo pipefail

# Only run in remote (Claude Code on the web) sessions
if [ "${CLAUDE_CODE_REMOTE:-}" != "true" ]; then
    exit 0
fi

JAVA_25_HOME="/usr/lib/jvm/java-25-openjdk-amd64"
MAVEN4_VERSION="4.0.0-rc-5"
MAVEN4_HOME="/opt/apache-maven-${MAVEN4_VERSION}"
MVND_VERSION="2.0.0-rc-3"
MVND_DIR_NAME="maven-mvnd-${MVND_VERSION}-linux-amd64"
MVND_HOME="/opt/mvnd"

log() { echo "[session-start] $*"; }
fail() { echo "[session-start] ERROR: $*" >&2; exit 1; }

# ─── 1. Install Java 25 ───────────────────────────────────────────────────────
log "Checking Java 25..."
if [ ! -d "$JAVA_25_HOME" ]; then
    log "Installing Java 25 via apt..."
    apt-get update -qq
    DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-25-jdk
fi

if [ ! -d "$JAVA_25_HOME" ]; then
    fail "Java 25 installation failed — $JAVA_25_HOME not found"
fi

INSTALLED_JAVA_VERSION=$("$JAVA_25_HOME/bin/java" -version 2>&1 | grep -i "openjdk version" | awk -F'"' '{print $2}' | cut -d. -f1)
if [ "$INSTALLED_JAVA_VERSION" != "25" ]; then
    fail "Expected Java 25, got version '$INSTALLED_JAVA_VERSION'"
fi
log "Java 25 OK: $("$JAVA_25_HOME/bin/java" -version 2>&1 | grep -i 'openjdk version')"

# ─── 2. Install Maven 4 ───────────────────────────────────────────────────────
log "Checking Maven ${MAVEN4_VERSION}..."
if [ ! -f "$MAVEN4_HOME/bin/mvn" ]; then
    log "Installing Maven ${MAVEN4_VERSION}..."
    MAVEN_URL="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN4_VERSION}/apache-maven-${MAVEN4_VERSION}-bin.tar.gz"
    curl -fsSL "$MAVEN_URL" -o /tmp/maven4.tar.gz
    tar -xzf /tmp/maven4.tar.gz -C /opt
    rm -f /tmp/maven4.tar.gz
fi

if [ ! -f "$MAVEN4_HOME/bin/mvn" ]; then
    fail "Maven 4 installation failed — $MAVEN4_HOME/bin/mvn not found"
fi

INSTALLED_MVN_VERSION=$("$MAVEN4_HOME/bin/mvn" --version 2>/dev/null | grep -oP 'Apache Maven \K[0-9]+[^ ]+' | head -1 || echo "unknown")
if [[ "$INSTALLED_MVN_VERSION" != 4.* ]]; then
    fail "Expected Maven 4.x, got: $INSTALLED_MVN_VERSION"
fi
log "Maven 4 OK: $INSTALLED_MVN_VERSION"

# ─── 3. Install mvnd 2 (Maven Daemon — Maven 4 edition) ──────────────────────
log "Checking mvnd ${MVND_VERSION}..."
if [ ! -f "$MVND_HOME/bin/mvnd" ]; then
    log "Installing mvnd ${MVND_VERSION}..."
    MVND_URL="https://github.com/apache/maven-mvnd/releases/download/${MVND_VERSION}/${MVND_DIR_NAME}.tar.gz"
    curl -fsSL "$MVND_URL" -o /tmp/mvnd.tar.gz
    tar -xzf /tmp/mvnd.tar.gz -C /opt
    # Rename to canonical path
    if [ -d "/opt/${MVND_DIR_NAME}" ] && [ "/opt/${MVND_DIR_NAME}" != "$MVND_HOME" ]; then
        mv "/opt/${MVND_DIR_NAME}" "$MVND_HOME"
    fi
    rm -f /tmp/mvnd.tar.gz
fi

if [ ! -f "$MVND_HOME/bin/mvnd" ]; then
    fail "mvnd installation failed — $MVND_HOME/bin/mvnd not found"
fi
log "mvnd OK: $("$MVND_HOME/bin/mvnd" --version 2>&1 | grep -v 'Picked up' | head -1 || echo "${MVND_VERSION}")"

# ─── 4. Configure PATH and JAVA_HOME (Java 25 + Maven 4 + mvnd ONLY) ─────────
log "Configuring environment (Java 25 + Maven 4 + mvnd only)..."
{
    echo "export JAVA_HOME=${JAVA_25_HOME}"
    # Prepend Java 25, Maven 4, and mvnd — they shadow any system defaults
    echo "export PATH=${JAVA_25_HOME}/bin:${MAVEN4_HOME}/bin:${MVND_HOME}/bin:\$PATH"
    # Ensure shell hash table is cleared so new paths take effect
    echo "hash -r 2>/dev/null || true"
} >> "$CLAUDE_ENV_FILE"

# ─── 5. Configure mvnd and Maven proxy settings ──────────────────────────────
log "Configuring mvnd for Java 25..."
mkdir -p "$HOME/.m2"
cat > "$HOME/.m2/mvnd.properties" << EOF
mvnd.javaHome=${JAVA_25_HOME}
mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
EOF

# Configure Maven proxy settings from JAVA_TOOL_OPTIONS (Maven 4 native HTTP transport
# does not read Java system proxy properties — it requires settings.xml)
log "Configuring Maven proxy settings..."
if [ ! -f "$HOME/.m2/settings.xml" ]; then
    PROXY_HOST=$(echo "${JAVA_TOOL_OPTIONS:-}" | grep -oP '(?<=proxyHost=)[^ ]+' | head -1 || true)
    PROXY_PORT=$(echo "${JAVA_TOOL_OPTIONS:-}" | grep -oP '(?<=proxyPort=)[^ ]+' | head -1 || true)
    PROXY_USER=$(echo "${JAVA_TOOL_OPTIONS:-}" | grep -oP '(?<=proxyUser=)[^ ]+' | head -1 || true)
    PROXY_PASS=$(echo "${JAVA_TOOL_OPTIONS:-}" | grep -oP '(?<=proxyPassword=)[^ ]+' | head -1 || true)
    NOPROXY=$(echo "${JAVA_TOOL_OPTIONS:-}" | grep -oP '(?<=nonProxyHosts=)[^ ]+' | head -1 || true)

    if [ -n "$PROXY_HOST" ] && [ -n "$PROXY_PORT" ]; then
        log "Writing Maven settings.xml with proxy: ${PROXY_HOST}:${PROXY_PORT}"
        cat > "$HOME/.m2/settings.xml" << XMLEOF
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd">
  <proxies>
    <proxy>
      <id>env-proxy-http</id>
      <active>true</active>
      <protocol>http</protocol>
      <host>${PROXY_HOST}</host>
      <port>${PROXY_PORT}</port>
      <username>${PROXY_USER}</username>
      <password>${PROXY_PASS}</password>
      <nonProxyHosts>${NOPROXY}</nonProxyHosts>
    </proxy>
    <proxy>
      <id>env-proxy-https</id>
      <active>true</active>
      <protocol>https</protocol>
      <host>${PROXY_HOST}</host>
      <port>${PROXY_PORT}</port>
      <username>${PROXY_USER}</username>
      <password>${PROXY_PASS}</password>
      <nonProxyHosts>${NOPROXY}</nonProxyHosts>
    </proxy>
  </proxies>
</settings>
XMLEOF
    fi
fi

# ─── 6. Warm up Maven local repository cache ─────────────────────────────────
log "Warming up Maven dependency cache (downloading project dependencies)..."
cd "${CLAUDE_PROJECT_DIR}"

# Run warm-up as best-effort (proxy may block in some environments)
JAVA_HOME="$JAVA_25_HOME" \
PATH="$JAVA_25_HOME/bin:$MAVEN4_HOME/bin:$MVND_HOME/bin:$PATH" \
"$MAVEN4_HOME/bin/mvn" \
    --no-transfer-progress \
    -B \
    dependency:resolve \
    -pl doctester-core \
    -DincludeScope=test \
    2>&1 | tail -10 || log "Cache warm-up skipped (network/proxy issue — will resolve on first build)"

log "Cache warm-up complete."

# ─── 7. Final verification ────────────────────────────────────────────────────
log "=== Toolchain Verification ==="
log "Java:  $("$JAVA_25_HOME/bin/java" -version 2>&1 | grep -i 'openjdk version')"
log "Maven: $("$MAVEN4_HOME/bin/mvn" --version 2>/dev/null | grep 'Apache Maven' | head -1)"
log "mvnd:  $MVND_HOME/bin/mvnd (${MVND_VERSION})"
log "=== Environment ready: Java 25 + Maven ${MAVEN4_VERSION} + mvnd ${MVND_VERSION} ==="
