#!/bin/bash
# DTR Session Start Hook
# Enforces ONLY: Java 26, Maven 4 (4.0.0-rc-5), and mvnd 2 (Maven Daemon).
# Supports: G Visor (remote/web, Linux) and macOS desktop sessions.
# No other Java or Maven version is acceptable.
set -euo pipefail

MAVEN4_VERSION="4.0.0-rc-5"
MVND_VERSION="2.0.0-rc-3"

log()  { echo "[session-start] $*"; }
fail() { echo "[session-start] ERROR: $*" >&2; exit 1; }

OS="$(uname -s)"    # Darwin or Linux
ARCH="$(uname -m)"  # arm64 / aarch64 / x86_64

# ─── Platform-specific install prefix ────────────────────────────────────────
# Linux (G Visor): write to /opt — root-owned, already writable in the sandbox
# macOS desktop:   write to ~/.dtr/tools — no sudo required
if [ "$OS" = "Darwin" ]; then
    TOOLS_PREFIX="$HOME/.dtr/tools"
else
    TOOLS_PREFIX="/opt"
fi
MAVEN4_HOME="${TOOLS_PREFIX}/apache-maven-${MAVEN4_VERSION}"
MVND_HOME="${TOOLS_PREFIX}/mvnd"
mkdir -p "$TOOLS_PREFIX"

# ─── 1. Resolve and verify Java 26 ───────────────────────────────────────────
log "Checking Java 26..."
JAVA26_HOME=""

if [ "$OS" = "Darwin" ]; then
    # macOS: /usr/libexec/java_home is the authoritative resolver
    if FOUND=$(/usr/libexec/java_home -v 26 2>/dev/null); then
        JAVA26_HOME="$FOUND"
    fi

    # Homebrew fallback — Apple Silicon (/opt/homebrew) and Intel (/usr/local)
    if [ -z "$JAVA26_HOME" ]; then
        for brew_prefix in /opt/homebrew /usr/local; do
            candidate="${brew_prefix}/opt/openjdk@26"
            if [ -x "${candidate}/bin/java" ]; then
                JAVA26_HOME="$candidate"
                break
            fi
        done
    fi

    # SDKMAN fallback
    if [ -z "$JAVA26_HOME" ]; then
        for dir in "$HOME/.sdkman/candidates/java"/26.*/; do
            if [ -x "${dir}/bin/java" ]; then
                JAVA26_HOME="$dir"
                break
            fi
        done
    fi

    if [ -z "$JAVA26_HOME" ]; then
        fail "Java 26 not found on macOS. Install with one of:
  Homebrew:  brew install openjdk@26
  SDKMAN:    sdk install java 26-open
  Official:  https://jdk.java.net/26/"
    fi
else
    # Linux (G Visor / Ubuntu/Debian)
    JAVA26_HOME="/usr/lib/jvm/java-26-openjdk-amd64"
    if [ ! -d "$JAVA26_HOME" ]; then
        log "Installing Java 26 via apt..."
        apt-get update -qq
        DEBIAN_FRONTEND=noninteractive apt-get install -y -qq openjdk-26-jdk
    fi
    if [ ! -d "$JAVA26_HOME" ]; then
        fail "Java 26 installation failed — $JAVA26_HOME not found"
    fi
fi

INSTALLED_JAVA_VERSION=$("$JAVA26_HOME/bin/java" -version 2>&1 \
    | grep -i "openjdk version" | awk -F'"' '{print $2}' | cut -d. -f1)
if [ "$INSTALLED_JAVA_VERSION" != "26" ]; then
    fail "Expected Java 26, got '$INSTALLED_JAVA_VERSION' at $JAVA26_HOME"
fi
log "Java 26 OK: $("$JAVA26_HOME/bin/java" -version 2>&1 | grep -i 'openjdk version')"

# ─── 2. Install / verify Maven 4 ─────────────────────────────────────────────
log "Checking Maven ${MAVEN4_VERSION}..."
if [ ! -f "${MAVEN4_HOME}/bin/mvn" ]; then
    log "Installing Maven ${MAVEN4_VERSION}..."
    MAVEN_URL="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN4_VERSION}/apache-maven-${MAVEN4_VERSION}-bin.tar.gz"
    curl -fsSL "$MAVEN_URL" -o /tmp/maven4.tar.gz
    tar -xzf /tmp/maven4.tar.gz -C "$TOOLS_PREFIX"
    rm -f /tmp/maven4.tar.gz
fi

if [ ! -f "${MAVEN4_HOME}/bin/mvn" ]; then
    fail "Maven 4 installation failed — ${MAVEN4_HOME}/bin/mvn not found"
fi

INSTALLED_MVN_VERSION=$("${MAVEN4_HOME}/bin/mvn" --version 2>/dev/null \
    | awk '/^Apache Maven/{print $3}' | head -1 || echo "unknown")
if [[ "$INSTALLED_MVN_VERSION" != 4.* ]]; then
    fail "Expected Maven 4.x, got: $INSTALLED_MVN_VERSION"
fi
log "Maven 4 OK: $INSTALLED_MVN_VERSION"

# ─── 3. Install / verify mvnd 2 ──────────────────────────────────────────────
log "Checking mvnd ${MVND_VERSION}..."
if [ ! -f "${MVND_HOME}/bin/mvnd" ]; then
    log "Installing mvnd ${MVND_VERSION}..."
    case "$OS-$ARCH" in
        Darwin-arm64|Darwin-aarch64) MVND_PLATFORM="darwin-aarch64" ;;
        Darwin-*)                    MVND_PLATFORM="darwin-amd64"   ;;
        Linux-aarch64)               MVND_PLATFORM="linux-aarch64"  ;;
        Linux-*)                     MVND_PLATFORM="linux-amd64"    ;;
        *) fail "Unsupported platform: $OS-$ARCH" ;;
    esac
    MVND_DIR_NAME="maven-mvnd-${MVND_VERSION}-${MVND_PLATFORM}"
    MVND_URL="https://github.com/apache/maven-mvnd/releases/download/${MVND_VERSION}/${MVND_DIR_NAME}.tar.gz"
    curl -fsSL "$MVND_URL" -o /tmp/mvnd.tar.gz
    tar -xzf /tmp/mvnd.tar.gz -C "$TOOLS_PREFIX"
    if [ -d "${TOOLS_PREFIX}/${MVND_DIR_NAME}" ] && [ "${TOOLS_PREFIX}/${MVND_DIR_NAME}" != "$MVND_HOME" ]; then
        mv "${TOOLS_PREFIX}/${MVND_DIR_NAME}" "$MVND_HOME"
    fi
    rm -f /tmp/mvnd.tar.gz
fi

if [ ! -f "${MVND_HOME}/bin/mvnd" ]; then
    fail "mvnd installation failed — ${MVND_HOME}/bin/mvnd not found"
fi
log "mvnd OK: $("${MVND_HOME}/bin/mvnd" --version 2>&1 | grep -v 'Picked up' | head -1 || echo "${MVND_VERSION}")"

# ─── 4. Configure PATH and JAVA_HOME ─────────────────────────────────────────
log "Configuring environment (Java 26 + Maven 4 + mvnd)..."
{
    echo "export JAVA_HOME=${JAVA26_HOME}"
    # Prepend Java 26, Maven 4, and mvnd — they shadow any system defaults
    echo "export PATH=${JAVA26_HOME}/bin:${MAVEN4_HOME}/bin:${MVND_HOME}/bin:\$PATH"
    # Clear shell hash table so new paths take effect immediately
    echo "hash -r 2>/dev/null || true"
} >> "$CLAUDE_ENV_FILE"

# ─── 5. Configure mvnd for Java 26 ───────────────────────────────────────────
log "Configuring mvnd for Java 26..."
mkdir -p "$HOME/.m2"
cat > "$HOME/.m2/mvnd.properties" << EOF
mvnd.javaHome=${JAVA26_HOME}
mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
EOF

# ─── 6. Configure Maven proxy settings (Linux / G Visor only) ────────────────
# Maven 4 native HTTP transport does not honour Java system proxy properties —
# it requires an explicit settings.xml proxy stanza.  macOS desktop users
# manage this themselves; only inject in sandboxed remote sessions.
if [ "$OS" != "Darwin" ] && [ ! -f "$HOME/.m2/settings.xml" ]; then
    log "Configuring Maven proxy settings..."
    PROXY_HOST=$(echo "${JAVA_TOOL_OPTIONS:-}" | sed -n 's/.*proxyHost=\([^ ]*\).*/\1/p' | head -1 || true)
    PROXY_PORT=$(echo "${JAVA_TOOL_OPTIONS:-}" | sed -n 's/.*proxyPort=\([^ ]*\).*/\1/p' | head -1 || true)
    PROXY_USER=$(echo "${JAVA_TOOL_OPTIONS:-}" | sed -n 's/.*proxyUser=\([^ ]*\).*/\1/p' | head -1 || true)
    PROXY_PASS=$(echo "${JAVA_TOOL_OPTIONS:-}" | sed -n 's/.*proxyPassword=\([^ ]*\).*/\1/p' | head -1 || true)
    NOPROXY=$(echo "${JAVA_TOOL_OPTIONS:-}" | sed -n 's/.*nonProxyHosts=\([^ ]*\).*/\1/p' | head -1 || true)

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

# ─── 7. Warm up Maven local repository cache ─────────────────────────────────
log "Warming up Maven dependency cache (downloading project dependencies)..."
cd "${CLAUDE_PROJECT_DIR}"
JAVA_HOME="$JAVA26_HOME" \
PATH="$JAVA26_HOME/bin:$MAVEN4_HOME/bin:$MVND_HOME/bin:$PATH" \
"${MAVEN4_HOME}/bin/mvn" \
    --no-transfer-progress \
    -B \
    dependency:resolve \
    -pl dtr-core \
    -DincludeScope=test \
    2>&1 | tail -10 \
    || log "Cache warm-up skipped (network/proxy issue — will resolve on first build)"
log "Cache warm-up complete."

# ─── 8. Observatory: generate compact codebase facts ─────────────────────────
OBSERVE_BIN="${CLAUDE_PROJECT_DIR}/scripts/rust/dtr-observatory/target/release/dtr-observe"
if [[ -x "$OBSERVE_BIN" ]]; then
    log "Observatory: generating docs/facts/ ..."
    "$OBSERVE_BIN" \
        --root "${CLAUDE_PROJECT_DIR}" \
        --output "${CLAUDE_PROJECT_DIR}/docs/facts" \
        --quiet 2>&1 || log "Observatory skipped (non-fatal)"
    log "Observatory: facts written to docs/facts/"
else
    log "Observatory binary not found — run: make build-observatory"
fi

# ─── 9. Final verification ────────────────────────────────────────────────────
log "=== Toolchain Verification ==="
log "OS:    $OS ($ARCH)"
log "Java:  $("$JAVA26_HOME/bin/java" -version 2>&1 | grep -i 'openjdk version')"
log "Maven: $("${MAVEN4_HOME}/bin/mvn" --version 2>/dev/null | grep 'Apache Maven' | head -1)"
log "mvnd:  ${MVND_HOME}/bin/mvnd (${MVND_VERSION})"
log "=== Environment ready: Java 26 + Maven ${MAVEN4_VERSION} + mvnd ${MVND_VERSION} on $OS ==="
