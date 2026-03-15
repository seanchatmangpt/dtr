#!/bin/bash
# DTR Precondition Validator
# Checks all required tools exist, have correct versions, and proper permissions.
# Exit codes: 0 = all green, 1 = precondition failed, 2 = internal error
#
# Usage:
#   scripts/check-preconditions.sh                 # check all required tools
#   scripts/check-preconditions.sh --tool java     # check single tool
#   scripts/check-preconditions.sh --verbose       # detailed output
#   scripts/check-preconditions.sh --fix-action    # show fix commands without checking

set -euo pipefail

# ─── Configuration ─────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
RESET='\033[0m'

# Global state
VERBOSE=false
SHOW_FIX_ONLY=false
SINGLE_TOOL=""
FAILED_CHECKS=0
PASSED_CHECKS=0
OS="$(uname -s)"
ARCH="$(uname -m)"

# ─── Helper functions (define early) ───────────────────────────────────────
show_help() {
    cat << EOF
DTR Precondition Validator — Check toolchain before build/release

Usage:
  scripts/check-preconditions.sh                 Check all required tools
  scripts/check-preconditions.sh --tool java     Check specific tool (java|mvn|git|rust)
  scripts/check-preconditions.sh --verbose       Show detailed diagnostics
  scripts/check-preconditions.sh --fix-action    Display fix commands (no checks)

Tools Validated:
  • Java 26+ (required for all builds)
  • Maven 4.0.0+ or mvnd 2.0.0+ (required for compile/test/release)
  • Git (required for version/tag management)
  • Rust + Cargo (optional, required only for Rust tools)

Exit Codes:
  0 = all checks passed
  1 = one or more checks failed (see FIX section)
  2 = internal error (script issue)

Examples:
  make check                             # Run from Makefile
  scripts/check-preconditions.sh         # Full validation
  scripts/check-preconditions.sh --verbose --tool java
EOF
}

# ─── Argument parsing ──────────────────────────────────────────────────────
while [[ $# -gt 0 ]]; do
    case "$1" in
        --verbose)     VERBOSE=true ;;
        --fix-action)  SHOW_FIX_ONLY=true ;;
        --tool)        shift; SINGLE_TOOL="$1" ;;
        --help)        show_help; exit 0 ;;
        *) echo "Unknown argument: $1" >&2; exit 2 ;;
    esac
    shift
done

# ─── Logging helpers ──────────────────────────────────────────────────────
log_header() {
    echo ""
    echo -e "${BLUE}=== $* ===${RESET}"
}

log_pass() {
    echo -e "  ${GREEN}✓${RESET} $*"
    (( PASSED_CHECKS++ ))
}

log_fail() {
    echo -e "  ${RED}✗${RESET} $*"
    (( FAILED_CHECKS++ ))
}

log_warn() {
    echo -e "  ${YELLOW}⚠${RESET} $*"
}

log_info() {
    [[ "$VERBOSE" == "true" ]] && echo "  ℹ $*"
}

log_fix() {
    echo -e "  ${YELLOW}FIX:${RESET} $*"
}

# ─── Version parsing helpers ──────────────────────────────────────────────
compare_versions() {
    local min_version="$1"
    local actual_version="$2"

    # Extract major.minor.patch (e.g., "26.0.1" → "26 0 1")
    local min_major=$(echo "$min_version" | cut -d. -f1)
    local min_minor=$(echo "$min_version" | cut -d. -f2 || echo "0")
    local min_patch=$(echo "$min_version" | cut -d. -f3 || echo "0")

    local actual_major=$(echo "$actual_version" | cut -d. -f1)
    local actual_minor=$(echo "$actual_version" | cut -d. -f2 || echo "0")
    local actual_patch=$(echo "$actual_version" | cut -d. -f3 || echo "0")

    # Coerce to integers (remove non-digit prefixes)
    min_major=$((${min_major//[^0-9]/}))
    min_minor=$((${min_minor//[^0-9]/}))
    min_patch=$((${min_patch//[^0-9]/}))
    actual_major=$((${actual_major//[^0-9]/}))
    actual_minor=$((${actual_minor//[^0-9]/}))
    actual_patch=$((${actual_patch//[^0-9]/}))

    if (( actual_major > min_major )); then
        return 0  # OK: actual > min
    elif (( actual_major < min_major )); then
        return 1  # FAIL: actual < min
    fi

    # Major versions equal, check minor
    if (( actual_minor > min_minor )); then
        return 0
    elif (( actual_minor < min_minor )); then
        return 1
    fi

    # Major and minor equal, check patch
    if (( actual_patch >= min_patch )); then
        return 0
    else
        return 1
    fi
}

# ─── Java validation ──────────────────────────────────────────────────────
check_java() {
    log_header "Java 26+"

    local java_bin=""

    # Find java executable
    if command -v java &>/dev/null; then
        java_bin="$(command -v java)"
    elif [ -n "${JAVA_HOME:-}" ] && [ -x "${JAVA_HOME}/bin/java" ]; then
        java_bin="${JAVA_HOME}/bin/java"
    else
        log_fail "Java not found in PATH or JAVA_HOME"
        log_fix "Install Java 26:"
        case "$OS" in
            Darwin)
                log_fix "  brew install openjdk@26"
                log_fix "  OR: sdk install java 26-open"
                log_fix "  OR: https://jdk.java.net/26/"
                ;;
            Linux)
                log_fix "  apt-get update && apt-get install -y openjdk-26-jdk"
                log_fix "  OR: sdk install java 26-open"
                ;;
            *)
                log_fix "  See https://jdk.java.net/26/"
                ;;
        esac
        return 1
    fi

    # Check if executable
    if [ ! -x "$java_bin" ]; then
        log_fail "Java executable not accessible: $java_bin"
        log_fix "Check file permissions: chmod +x $java_bin"
        return 1
    fi

    log_info "Using: $java_bin"

    # Extract version
    local version_output=$("$java_bin" -version 2>&1 || true)
    log_info "Version output: $version_output"

    local java_version=$(echo "$version_output" | grep -oP 'version "\K[^"]+' | head -1 || echo "")
    if [ -z "$java_version" ]; then
        log_fail "Could not parse Java version from: $version_output"
        return 1
    fi

    # Extract major version (first number)
    local major=$(echo "$java_version" | cut -d. -f1)
    major=$((${major//[^0-9]/}))

    if (( major < 26 )); then
        log_fail "Java version too old: $java_version (need 26+)"
        log_fix "Install Java 26+:"
        case "$OS" in
            Darwin)
                log_fix "  brew install openjdk@26"
                log_fix "  OR: sdk install java 26-open"
                ;;
            Linux)
                log_fix "  apt-get update && apt-get install -y openjdk-26-jdk"
                log_fix "  OR: sdk install java 26-open"
                ;;
        esac
        return 1
    fi

    log_pass "Java $java_version (binary: $java_bin)"
    return 0
}

# ─── Maven validation (mvn or mvnd) ──────────────────────────────────────
check_maven() {
    log_header "Maven 4.0.0+ (mvn or mvnd)"

    local mvnd_bin=""
    local mvn_bin=""
    local preferred=""

    # Prefer mvnd, fall back to mvn
    if command -v mvnd &>/dev/null; then
        mvnd_bin="$(command -v mvnd)"
    elif [ -x "/opt/mvnd/bin/mvnd" ]; then
        mvnd_bin="/opt/mvnd/bin/mvnd"
    fi

    if command -v mvn &>/dev/null; then
        mvn_bin="$(command -v mvn)"
    elif [ -x "/opt/apache-maven-4.0.0-rc-5/bin/mvn" ]; then
        mvn_bin="/opt/apache-maven-4.0.0-rc-5/bin/mvn"
    fi

    # Neither found
    if [ -z "$mvnd_bin" ] && [ -z "$mvn_bin" ]; then
        log_fail "Maven not found (neither mvn nor mvnd)"
        log_fix "Install Maven 4.0.0+:"
        case "$OS" in
            Darwin)
                log_fix "  brew install maven"
                log_fix "  OR: https://maven.apache.org/download.cgi (Maven 4.0.0-rc-5+)"
                log_fix "  OR: github.com/apache/maven-mvnd"
                ;;
            Linux)
                log_fix "  apt-get install -y maven"
                log_fix "  OR: https://maven.apache.org/download.cgi (Maven 4.0.0-rc-5+)"
                log_fix "  OR: github.com/apache/maven-mvnd"
                ;;
        esac
        return 1
    fi

    # mvnd preferred for speed
    if [ -n "$mvnd_bin" ]; then
        preferred="mvnd"

        if [ ! -x "$mvnd_bin" ]; then
            log_fail "mvnd not executable: $mvnd_bin"
            log_fix "Check permissions: chmod +x $mvnd_bin"
            return 1
        fi

        log_info "Using: $mvnd_bin"

        local version_output=$("$mvnd_bin" --version 2>&1 | grep -v "Picked up" || true)
        log_info "Version output: $version_output"

        local mvnd_version=$(echo "$version_output" | grep -oP 'Maven Multi-threaded Driver \(mvnd\) \K[0-9]+\.[0-9]+\.[0-9]+' || echo "")
        if [ -z "$mvnd_version" ]; then
            mvnd_version=$(echo "$version_output" | grep -oP '\b[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "")
        fi

        if [ -z "$mvnd_version" ]; then
            log_fail "Could not parse mvnd version from: $version_output"
            return 1
        fi

        if compare_versions "2.0.0" "$mvnd_version"; then
            log_pass "mvnd $mvnd_version (binary: $mvnd_bin)"
            return 0
        else
            log_fail "mvnd version too old: $mvnd_version (need 2.0.0+)"
            log_fix "Update mvnd:"
            log_fix "  https://github.com/apache/maven-mvnd/releases/download/2.0.0/maven-mvnd-2.0.0-linux-amd64.tar.gz"
            return 1
        fi
    fi

    # Fallback to mvn
    if [ -n "$mvn_bin" ]; then
        preferred="mvn"

        if [ ! -x "$mvn_bin" ]; then
            log_fail "mvn not executable: $mvn_bin"
            log_fix "Check permissions: chmod +x $mvn_bin"
            return 1
        fi

        log_info "Using: $mvn_bin"

        local version_output=$("$mvn_bin" --version 2>&1 || true)
        log_info "Version output: $version_output"

        local mvn_version=$(echo "$version_output" | grep -oP 'Apache Maven \K[0-9]+\.[0-9]+\.[0-9]+' | head -1 || echo "")
        if [ -z "$mvn_version" ]; then
            log_fail "Could not parse Maven version from: $version_output"
            return 1
        fi

        if compare_versions "4.0.0" "$mvn_version"; then
            log_pass "Maven $mvn_version (binary: $mvn_bin)"
            return 0
        else
            log_fail "Maven version too old: $mvn_version (need 4.0.0+)"
            log_fix "Update Maven:"
            log_fix "  https://maven.apache.org/download.cgi (Maven 4.0.0-rc-5+)"
            log_fix "  OR: brew install maven"
            return 1
        fi
    fi

    return 1
}

# ─── Git validation ───────────────────────────────────────────────────────
check_git() {
    log_header "Git (for version/tag management)"

    local git_bin=""

    if command -v git &>/dev/null; then
        git_bin="$(command -v git)"
    else
        log_fail "Git not found in PATH"
        log_fix "Install Git:"
        case "$OS" in
            Darwin)
                log_fix "  brew install git"
                ;;
            Linux)
                log_fix "  apt-get install -y git"
                ;;
        esac
        return 1
    fi

    if [ ! -x "$git_bin" ]; then
        log_fail "Git executable not accessible: $git_bin"
        log_fix "Check file permissions: chmod +x $git_bin"
        return 1
    fi

    log_info "Using: $git_bin"

    local version=$("$git_bin" --version 2>&1 | head -1 || true)
    log_info "Version output: $version"

    log_pass "$version (binary: $git_bin)"
    return 0
}

# ─── Rust validation (optional) ───────────────────────────────────────────
check_rust() {
    log_header "Rust + Cargo (optional, for Rust tools)"

    # Check if any Rust tools are needed
    local needs_rust=false
    for rust_tool_dir in "$PROJECT_DIR"/scripts/rust/*/; do
        if [ -f "${rust_tool_dir}Cargo.toml" ]; then
            needs_rust=true
            break
        fi
    done

    if [ "$needs_rust" == "false" ]; then
        log_warn "Rust tools not detected (optional)"
        return 0
    fi

    local rustc_bin=""
    local cargo_bin=""

    if command -v rustc &>/dev/null; then
        rustc_bin="$(command -v rustc)"
    else
        log_fail "Rust not found in PATH (needed for Rust tools)"
        log_fix "Install Rust:"
        log_fix "  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh"
        log_fix "  OR: brew install rust"
        return 1
    fi

    if [ ! -x "$rustc_bin" ]; then
        log_fail "Rust compiler not executable: $rustc_bin"
        return 1
    fi

    if ! command -v cargo &>/dev/null; then
        log_fail "Cargo not found (installed with Rust)"
        return 1
    fi

    local rustc_version=$("$rustc_bin" --version 2>&1 | head -1 || true)
    log_info "rustc version: $rustc_version"

    local cargo_version=$(cargo --version 2>&1 | head -1 || true)
    log_info "cargo version: $cargo_version"

    log_pass "Rust toolchain ready"
    return 0
}

# ─── Project validation ───────────────────────────────────────────────────
check_project() {
    log_header "Project Structure"

    local all_ok=true

    # Check for Makefile
    if [ -f "$PROJECT_DIR/Makefile" ]; then
        log_pass "Makefile found"
    else
        log_fail "Makefile not found at $PROJECT_DIR/Makefile"
        all_ok=false
    fi

    # Check for pom.xml
    if [ -f "$PROJECT_DIR/pom.xml" ]; then
        log_pass "pom.xml found"
    else
        log_fail "pom.xml not found at $PROJECT_DIR/pom.xml"
        all_ok=false
    fi

    # Check for .mvn directory
    if [ -d "$PROJECT_DIR/.mvn" ]; then
        log_pass ".mvn directory found"
        if [ -f "$PROJECT_DIR/.mvn/maven.config" ]; then
            log_pass ".mvn/maven.config found"
        else
            log_warn ".mvn/maven.config not found"
        fi
    else
        log_fail ".mvn directory not found"
        all_ok=false
    fi

    # Check for scripts directory
    if [ -d "$PROJECT_DIR/scripts" ]; then
        log_pass "scripts directory found"
    else
        log_fail "scripts directory not found"
        all_ok=false
    fi

    [ "$all_ok" == "true" ] && return 0 || return 1
}

# ─── Summary report ───────────────────────────────────────────────────────
print_summary() {
    echo ""
    echo -e "${BLUE}=== Summary ===${RESET}"
    echo "Passed: $PASSED_CHECKS"
    echo "Failed: $FAILED_CHECKS"
    echo ""

    if (( FAILED_CHECKS == 0 )); then
        echo -e "${GREEN}✓ All preconditions met!${RESET}"
        echo ""
        echo "Next steps:"
        echo "  make verify      — Full verification (compile + test + checks)"
        echo "  make compile     — Compile only"
        echo "  make test        — Run tests"
        echo "  make dx          — Full validation pipeline"
        return 0
    else
        echo -e "${RED}✗ Preconditions NOT met — see FIX sections above${RESET}"
        return 1
    fi
}

# ─── Show fix actions only (no checks) ────────────────────────────────────
show_fix_actions() {
    cat << EOF

${BLUE}=== Fix Actions for Common Issues ===${RESET}

${YELLOW}Java 26 not found:${RESET}
  brew install openjdk@26                    # macOS
  apt-get install -y openjdk-26-jdk          # Linux/Debian
  sdk install java 26-open                   # Any platform (SDKMAN)

${YELLOW}Maven 4 not found:${RESET}
  brew install maven                         # macOS
  apt-get install -y maven                   # Linux/Debian
  # Or download from: https://maven.apache.org/download.cgi (4.0.0-rc-5+)

${YELLOW}mvnd (Maven Daemon) not found:${RESET}
  # Install from: https://github.com/apache/maven-mvnd/releases
  # Extract to /opt/mvnd (Linux) or ~/.dtr/tools/mvnd (macOS)

${YELLOW}Git not found:${RESET}
  brew install git                           # macOS
  apt-get install -y git                     # Linux/Debian

${YELLOW}Rust not found (for Rust tools):${RESET}
  curl --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh
  # Then: source \$HOME/.cargo/env

${YELLOW}Version issues:${RESET}
  # Update Java: Use your Java version manager (brew, sdk, etc.)
  # Update Maven: Re-download from https://maven.apache.org/download.cgi
  # Update Rust: rustup update

EOF
}

# ─── Main execution ───────────────────────────────────────────────────────
main() {
    echo ""
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════════╗${RESET}"
    echo -e "${BLUE}║  DTR Precondition Validator — Check Toolchain Before Build     ║${RESET}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════════╝${RESET}"

    if [ "$SHOW_FIX_ONLY" == "true" ]; then
        show_fix_actions
        return 0
    fi

    echo "Environment: $OS ($ARCH)"
    echo "Project: $PROJECT_DIR"

    # Run checks based on --tool filter
    local failed=0

    if [ -z "$SINGLE_TOOL" ] || [ "$SINGLE_TOOL" = "java" ]; then
        check_java || failed=1
    fi

    if [ -z "$SINGLE_TOOL" ] || [ "$SINGLE_TOOL" = "maven" ] || [ "$SINGLE_TOOL" = "mvn" ]; then
        check_maven || failed=1
    fi

    if [ -z "$SINGLE_TOOL" ] || [ "$SINGLE_TOOL" = "git" ]; then
        check_git || failed=1
    fi

    if [ -z "$SINGLE_TOOL" ] || [ "$SINGLE_TOOL" = "rust" ]; then
        check_rust || failed=1
    fi

    if [ -z "$SINGLE_TOOL" ]; then
        check_project || failed=1
    fi

    print_summary || failed=1

    exit $failed
}

main "$@"
