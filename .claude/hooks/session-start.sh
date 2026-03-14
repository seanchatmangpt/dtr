#!/bin/bash
# DTR Session Start Hook
# Enforces ONLY: Java 26, Maven 4 (4.0.0-rc-5), and mvnd 2 (Maven Daemon).
# No other Java or Maven version is acceptable.
#
# Platform support:
#   Linux (Web / Claude Code on the Web): installs toolchain if absent
#   macOS (Claude Code local):            locates existing toolchain, writes env
set -euo pipefail

MAVEN4_VERSION="4.0.0-rc-5"
MAVEN4_HOME="/opt/apache-maven-${MAVEN4_VERSION}"
MVND_VERSION="2.0.0-rc-3"
MVND_HOME="/opt/mvnd"

# Java 26 GA — build 26+35 (2026-02-13)
JAVA_26_BUILD="c3cc523845074aa0af4f5e1e1ed4151d"
JAVA_26_BUILD_NUM="35"
JAVA_26_URL="https://download.java.net/java/GA/jdk26/${JAVA_26_BUILD}/${JAVA_26_BUILD_NUM}/GPL/openjdk-26_linux-x64_bin.tar.gz"

log()  { echo "[session-start] $*"; }
fail() { echo "[session-start] ERROR: $*" >&2; exit 1; }

# ─── Detect platform ──────────────────────────────────────────────────────────
OS=$(uname -s)

case "$OS" in
    Linux)
        JAVA_26_HOME="/usr/lib/jvm/java-26-openjdk-amd64"
        MVND_ARCH="linux-amd64"
        IS_REMOTE="${CLAUDE_CODE_REMOTE:-false}"
        ;;
    Darwin)
        # macOS: discover Java 26 from Homebrew, SDKMAN, or direct install locations
        JAVA_26_HOME=""
        for candidate in \
            "/opt/homebrew/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home" \
            "/usr/local/opt/openjdk@26/libexec/openjdk.jdk/Contents/Home" \
            "/opt/homebrew/opt/openjdk/libexec/openjdk.jdk/Contents/Home" \
            "$HOME/.sdkman/candidates/java/26"* \
            "/Library/Java/JavaVirtualMachines/jdk-26.jdk/Contents/Home" \
            "/Library/Java/JavaVirtualMachines/openjdk-26.jdk/Contents/Home"
        do
            if [ -f "${candidate}/bin/java" ]; then
                ver=$("${candidate}/bin/java" -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1)
                if [ "$ver" = "26" ]; then
                    JAVA_26_HOME="$candidate"
                    break
                fi
            fi
        done
        # Fall back: check if java on PATH is version 26
        if [ -z "$JAVA_26_HOME" ] && command -v java &>/dev/null; then
            ver=$(java -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1)
            if [ "$ver" = "26" ]; then
                JAVA_26_HOME=$(java -XshowSettings:property -version 2>&1 | awk '/java.home/{print $3}')
            fi
        fi
        if [ -z "$JAVA_26_HOME" ]; then
            log "WARNING: Java 26 not found on macOS. Install with:"
            log "  brew install openjdk@26"
            log "  # or: sdk install java 26-open"
            # Don't fail — let the user install it; env will be incomplete but Claude can still help
            exit 0
        fi
        MVND_ARCH="darwin-amd64"
        # macOS mvnd lives in Homebrew or was installed manually
        for mvnd_candidate in \
            "/opt/homebrew/bin/mvnd" \
            "/usr/local/bin/mvnd" \
            "$HOME/.sdkman/candidates/mvnd/current/bin/mvnd" \
            "/opt/mvnd/bin/mvnd"
        do
            if [ -f "$mvnd_candidate" ]; then
                MVND_HOME=$(dirname $(dirname "$mvnd_candidate"))
                break
            fi
        done
        IS_REMOTE="false"
        ;;
    *)
        log "WARNING: Unsupported OS '$OS' — skipping toolchain setup"
        exit 0
        ;;
esac

# ─── 1. Install / verify Java 26 ─────────────────────────────────────────────
log "Checking Java 26..."
if [ ! -d "$JAVA_26_HOME" ]; then
    if [ "$OS" = "Linux" ]; then
        log "Java 26 not found at $JAVA_26_HOME — installing..."
        # apt doesn't carry Java 26 yet; download from jdk.java.net
        if [ ! -f "/usr/lib/jvm/jdk-26/bin/java" ]; then
            log "Downloading OpenJDK 26 from jdk.java.net..."
            curl -fsSL "$JAVA_26_URL" -o /tmp/openjdk-26.tar.gz
            tar -xzf /tmp/openjdk-26.tar.gz -C /usr/lib/jvm/
            rm -f /tmp/openjdk-26.tar.gz
            ln -sf /usr/lib/jvm/jdk-26 "$JAVA_26_HOME"
        else
            ln -sf /usr/lib/jvm/jdk-26 "$JAVA_26_HOME"
        fi
        # Register with update-alternatives
        update-alternatives --install /usr/bin/java  java  "${JAVA_26_HOME}/bin/java"  261 2>/dev/null || true
        update-alternatives --install /usr/bin/javac javac "${JAVA_26_HOME}/bin/javac" 261 2>/dev/null || true
        update-alternatives --set java  "${JAVA_26_HOME}/bin/java"  2>/dev/null || true
        update-alternatives --set javac "${JAVA_26_HOME}/bin/javac" 2>/dev/null || true
    else
        fail "Java 26 not found and cannot auto-install on $OS"
    fi
fi

INSTALLED_JAVA_VERSION=$("$JAVA_26_HOME/bin/java" -version 2>&1 | awk -F'"' '/version/{print $2}' | cut -d. -f1)
if [ "$INSTALLED_JAVA_VERSION" != "26" ]; then
    fail "Expected Java 26, got version '$INSTALLED_JAVA_VERSION' at $JAVA_26_HOME"
fi
log "Java 26 OK: $("$JAVA_26_HOME/bin/java" -version 2>&1 | grep -i 'openjdk version')"

# ─── 2. Install Maven 4 (Linux Web only — macOS uses Homebrew/SDKMAN) ────────
log "Checking Maven ${MAVEN4_VERSION}..."
if [ "$OS" = "Linux" ] && [ ! -f "$MAVEN4_HOME/bin/mvn" ]; then
    log "Installing Maven ${MAVEN4_VERSION}..."
    MAVEN_URL="https://repo1.maven.org/maven2/org/apache/maven/apache-maven/${MAVEN4_VERSION}/apache-maven-${MAVEN4_VERSION}-bin.tar.gz"
    curl -fsSL "$MAVEN_URL" -o /tmp/maven4.tar.gz
    tar -xzf /tmp/maven4.tar.gz -C /opt
    rm -f /tmp/maven4.tar.gz
fi

if [ -f "$MAVEN4_HOME/bin/mvn" ]; then
    INSTALLED_MVN_VERSION=$("$MAVEN4_HOME/bin/mvn" --version 2>/dev/null | grep -oP 'Apache Maven \K[0-9]+[^ ]+' | head -1 || echo "unknown")
    if [[ "$INSTALLED_MVN_VERSION" != 4.* ]]; then
        fail "Expected Maven 4.x, got: $INSTALLED_MVN_VERSION"
    fi
    log "Maven 4 OK: $INSTALLED_MVN_VERSION"
    MAVEN_BIN="$MAVEN4_HOME/bin/mvn"
else
    # macOS: fall back to system mvn (must be Maven 4)
    MAVEN_BIN=$(command -v mvn 2>/dev/null || echo "")
    if [ -n "$MAVEN_BIN" ]; then
        log "Maven (system): $($MAVEN_BIN --version 2>/dev/null | grep 'Apache Maven' | head -1)"
    fi
fi

# ─── 3. Install mvnd 2 (Linux Web only — macOS: expect on PATH or Homebrew) ──
log "Checking mvnd ${MVND_VERSION}..."
if [ "$OS" = "Linux" ] && [ ! -f "$MVND_HOME/bin/mvnd" ]; then
    log "Installing mvnd ${MVND_VERSION}..."
    MVND_DIR_NAME="maven-mvnd-${MVND_VERSION}-${MVND_ARCH}"
    MVND_URL="https://github.com/apache/maven-mvnd/releases/download/${MVND_VERSION}/${MVND_DIR_NAME}.tar.gz"
    curl -fsSL "$MVND_URL" -o /tmp/mvnd.tar.gz
    tar -xzf /tmp/mvnd.tar.gz -C /opt
    if [ -d "/opt/${MVND_DIR_NAME}" ] && [ "/opt/${MVND_DIR_NAME}" != "$MVND_HOME" ]; then
        mv "/opt/${MVND_DIR_NAME}" "$MVND_HOME"
    fi
    rm -f /tmp/mvnd.tar.gz
fi

MVND_BIN="${MVND_HOME}/bin/mvnd"
if [ ! -f "$MVND_BIN" ]; then
    MVND_BIN=$(command -v mvnd 2>/dev/null || echo "")
fi
if [ -n "$MVND_BIN" ] && [ -f "$MVND_BIN" ]; then
    log "mvnd OK: $("$MVND_BIN" --version 2>&1 | grep -v 'Picked up' | head -1 || echo "${MVND_VERSION}")"
    MVND_HOME=$(dirname $(dirname "$MVND_BIN"))
else
    log "WARNING: mvnd not found — builds will use Maven wrapper (./mvnw) as fallback"
    MVND_BIN=""
fi

# ─── 4. Configure PATH and JAVA_HOME ─────────────────────────────────────────
log "Configuring environment (Java 26 + Maven 4 + mvnd)..."
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
    {
        echo "export JAVA_HOME=${JAVA_26_HOME}"
        EXTRA_PATH="${JAVA_26_HOME}/bin"
        [ -f "$MAVEN4_HOME/bin/mvn" ] && EXTRA_PATH="${EXTRA_PATH}:${MAVEN4_HOME}/bin"
        [ -n "$MVND_BIN" ]            && EXTRA_PATH="${EXTRA_PATH}:$(dirname "$MVND_BIN")"
        echo "export PATH=${EXTRA_PATH}:\$PATH"
        echo "hash -r 2>/dev/null || true"
    } >> "$CLAUDE_ENV_FILE"
fi

# ─── 5. Write mvnd.properties with Java 26 ───────────────────────────────────
log "Writing ~/.m2/mvnd.properties for Java 26..."
mkdir -p "$HOME/.m2"
cat > "$HOME/.m2/mvnd.properties" << EOF
mvnd.javaHome=${JAVA_26_HOME}
mvnd.jvmArgs=-Xmx2g --enable-preview -Dfile.encoding=UTF-8
mvnd.minHeapSize=256m
mvnd.maxHeapSize=2g
mvnd.threads=4
EOF

# ─── 6. Configure Maven proxy settings (Web only — credentials in JAVA_TOOL_OPTIONS)
if [ "$IS_REMOTE" = "true" ] && [ ! -f "$HOME/.m2/settings.xml" ]; then
    log "Configuring Maven proxy settings..."
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

# ─── 7. Warm up Maven dependency cache (Web only) ────────────────────────────
if [ "$IS_REMOTE" = "true" ] && [ -n "$MAVEN_BIN" ]; then
    log "Warming up Maven dependency cache..."
    cd "${CLAUDE_PROJECT_DIR}"
    JAVA_HOME="$JAVA_26_HOME" \
    PATH="$JAVA_26_HOME/bin:${MAVEN4_HOME}/bin:${MVND_BIN:+$(dirname "$MVND_BIN"):}$PATH" \
    "$MAVEN_BIN" \
        --no-transfer-progress \
        -B \
        dependency:resolve \
        -pl dtr-core \
        -DincludeScope=test \
        2>&1 | tail -10 || log "Cache warm-up skipped (network/proxy issue — will resolve on first build)"
    log "Cache warm-up complete."
fi

# ─── 8. Final verification ────────────────────────────────────────────────────
log "=== Toolchain Verification ==="
log "OS:    $OS"
log "Java:  $("$JAVA_26_HOME/bin/java" -version 2>&1 | grep -i 'openjdk version')"
[ -n "$MAVEN_BIN" ] && log "Maven: $($MAVEN_BIN --version 2>/dev/null | grep 'Apache Maven' | head -1)"
[ -n "$MVND_BIN" ]  && log "mvnd:  $MVND_BIN (${MVND_VERSION})"
log "=== Environment ready: Java 26 + Maven ${MAVEN4_VERSION} + mvnd ${MVND_VERSION} ==="
