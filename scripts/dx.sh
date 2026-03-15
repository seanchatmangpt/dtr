#!/bin/bash
# DTR dx.sh — Five-phase validation pipeline
#
# Implements the Ψ→H→Λ→Ω pipeline from the YAWL truth-enforcement architecture:
#   Phase Ψ (Psi)    — Observatory: refresh codebase facts
#   Phase H          — H-Guards: semantic lie scan (exits 2 on violations)
#   Phase Λ (Lambda) — Build/test: mvnd verify (skippable with --skip-verify)
#   Phase Ω (Omega)  — Git: uncommitted/unpushed state check
#
# Usage:
#   scripts/dx.sh                    # full pipeline
#   scripts/dx.sh --skip-verify      # skip Phase Λ (faster iteration)
#   scripts/dx.sh --phase H          # run a single phase only
#
# Exits: 0=all phases green, 2=one or more phases failed
# Receipt: .claude/dx-receipt.json (machine-readable phase results)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
RECEIPT_FILE="$PROJECT_DIR/.claude/dx-receipt.json"

# ─── Argument parsing ──────────────────────────────────────────────────────────
SKIP_VERIFY=false
SINGLE_PHASE=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --skip-verify) SKIP_VERIFY=true ;;
        --phase) shift; SINGLE_PHASE="$1" ;;
        *) echo "Unknown argument: $1" >&2; exit 1 ;;
    esac
    shift
done

# ─── Helpers ───────────────────────────────────────────────────────────────────
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
RESET='\033[0m'

phase_result_psi="skip"
phase_result_h="skip"
phase_result_lambda="skip"
phase_result_omega="skip"
overall="green"
total_violations=0
start_time=$(date +%s)

log_phase() {
    local phase="$1" status="$2" msg="$3"
    if [[ "$status" == "green" ]]; then
        echo -e "  ${GREEN}✓${RESET} Phase ${phase}: ${msg}"
    elif [[ "$status" == "skip" ]]; then
        echo -e "  ${YELLOW}—${RESET} Phase ${phase}: ${msg} (skipped)"
    else
        echo -e "  ${RED}✗${RESET} Phase ${phase}: ${msg}"
    fi
}

echo ""
echo "╔══════════════════════════════════════════════════════════════╗"
echo "║              DTR dx.sh — Validation Pipeline                 ║"
echo "╚══════════════════════════════════════════════════════════════╝"
echo ""

# ─── Phase Ψ: Observatory ─────────────────────────────────────────────────────
run_psi() {
    local t0=$(date +%s)
    OBSERVE_BIN="${PROJECT_DIR}/scripts/rust/dtr-observatory/target/release/dtr-observe"
    if [[ -x "$OBSERVE_BIN" ]]; then
        if "$OBSERVE_BIN" \
            --root "${PROJECT_DIR}" \
            --output "${PROJECT_DIR}/docs/facts" \
            --quiet 2>/dev/null; then
            local elapsed=$(( $(date +%s) - t0 ))
            log_phase "Ψ (Observatory)" "green" "facts refreshed in ${elapsed}s"
            phase_result_psi="green"
        else
            log_phase "Ψ (Observatory)" "red" "dtr-observe failed"
            phase_result_psi="red"
            overall="red"
        fi
    else
        log_phase "Ψ (Observatory)" "skip" "binary not built (make build-observatory)"
        phase_result_psi="skip"
    fi
}

# ─── Phase H: H-Guards ────────────────────────────────────────────────────────
run_h() {
    local t0=$(date +%s)
    GUARD_BIN="${PROJECT_DIR}/scripts/rust/dtr-guard/target/release/dtr-guard-scan"
    if [[ ! -x "$GUARD_BIN" ]]; then
        log_phase "H (Guards)" "skip" "binary not built (make build-guard)"
        phase_result_h="skip"
        return
    fi

    # Collect main Java files
    JAVA_FILES=$(find "${PROJECT_DIR}/dtr-core/src/main/java" -name "*.java" 2>/dev/null | sort)
    if [[ -z "$JAVA_FILES" ]]; then
        log_phase "H (Guards)" "green" "no Java files found in main source"
        phase_result_h="green"
        return
    fi

    FILE_COUNT=$(echo "$JAVA_FILES" | wc -l | tr -d ' ')
    local t_scan=$(date +%s)

    # Run with JSON output to capture violation count
    GUARD_OUTPUT=$(echo "$JAVA_FILES" | xargs "$GUARD_BIN" --json 2>/dev/null || true)
    local elapsed=$(( $(date +%s) - t0 ))

    if [[ -n "$GUARD_OUTPUT" ]]; then
        VIOLATIONS=$(echo "$GUARD_OUTPUT" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('violation_count', 0))
except:
    print(0)
" 2>/dev/null || echo "0")
        STATUS_STR=$(echo "$GUARD_OUTPUT" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('status', 'UNKNOWN'))
except:
    print('UNKNOWN')
" 2>/dev/null || echo "UNKNOWN")
    else
        VIOLATIONS=0
        STATUS_STR="GREEN"
    fi

    total_violations=$VIOLATIONS

    if [[ "$STATUS_STR" == "GREEN" ]]; then
        log_phase "H (Guards)" "green" "${FILE_COUNT} files clean in ${elapsed}s"
        phase_result_h="green"
    else
        log_phase "H (Guards)" "red" "${VIOLATIONS} violation(s) in ${FILE_COUNT} files — run: make guard-scan"
        phase_result_h="red"
        overall="red"
    fi
}

# ─── Phase Λ: Build ────────────────────────────────────────────────────────────
run_lambda() {
    if [[ "$SKIP_VERIFY" == "true" ]]; then
        log_phase "Λ (Build)" "skip" "--skip-verify flag set"
        phase_result_lambda="skip"
        return
    fi

    local t0=$(date +%s)
    MVND_BIN=$(command -v mvnd 2>/dev/null || echo "/opt/mvnd/bin/mvnd")

    if [[ ! -x "$MVND_BIN" ]]; then
        MVND_BIN=$(command -v mvn 2>/dev/null || echo "")
    fi

    if [[ -z "$MVND_BIN" ]]; then
        log_phase "Λ (Build)" "skip" "mvnd/mvn not found"
        phase_result_lambda="skip"
        return
    fi

    echo "  Running mvnd verify (this may take a minute)..."
    if (cd "$PROJECT_DIR" && "$MVND_BIN" verify --no-transfer-progress -B -q 2>&1); then
        local elapsed=$(( $(date +%s) - t0 ))
        log_phase "Λ (Build)" "green" "mvnd verify passed in ${elapsed}s"
        phase_result_lambda="green"
    else
        local elapsed=$(( $(date +%s) - t0 ))
        log_phase "Λ (Build)" "red" "mvnd verify FAILED in ${elapsed}s — see target/surefire-reports/"
        phase_result_lambda="red"
        overall="red"
    fi
}

# ─── Phase Ω: Git ─────────────────────────────────────────────────────────────
run_omega() {
    local t0=$(date +%s)

    if ! git -C "$PROJECT_DIR" rev-parse --git-dir >/dev/null 2>&1; then
        log_phase "Ω (Git)" "skip" "not a git repository"
        phase_result_omega="skip"
        return
    fi

    local git_issues=""

    # Uncommitted changes
    if ! git -C "$PROJECT_DIR" diff --quiet 2>/dev/null || \
       ! git -C "$PROJECT_DIR" diff --cached --quiet 2>/dev/null; then
        git_issues="uncommitted changes"
    fi

    # Untracked files
    if [[ -z "$git_issues" ]]; then
        UNTRACKED=$(git -C "$PROJECT_DIR" ls-files --others --exclude-standard 2>/dev/null | head -3)
        if [[ -n "$UNTRACKED" ]]; then
            git_issues="untracked files"
        fi
    fi

    # Unpushed commits
    if [[ -z "$git_issues" ]]; then
        BRANCH=$(git -C "$PROJECT_DIR" branch --show-current 2>/dev/null || echo "")
        if [[ -n "$BRANCH" ]]; then
            if git -C "$PROJECT_DIR" rev-parse "origin/$BRANCH" >/dev/null 2>&1; then
                UNPUSHED=$(git -C "$PROJECT_DIR" rev-list "origin/$BRANCH..HEAD" --count 2>/dev/null || echo "0")
            else
                UNPUSHED=$(git -C "$PROJECT_DIR" rev-list "origin/HEAD..HEAD" --count 2>/dev/null || echo "0")
            fi
            if [[ "$UNPUSHED" -gt 0 ]]; then
                git_issues="${UNPUSHED} unpushed commit(s)"
            fi
        fi
    fi

    if [[ -z "$git_issues" ]]; then
        log_phase "Ω (Git)" "green" "working tree clean, all commits pushed"
        phase_result_omega="green"
    else
        log_phase "Ω (Git)" "red" "$git_issues — commit and push"
        phase_result_omega="red"
        overall="red"
    fi
}

# ─── Run phases ────────────────────────────────────────────────────────────────
if [[ -n "$SINGLE_PHASE" ]]; then
    case "$SINGLE_PHASE" in
        "Ψ"|"psi"|"observatory") run_psi ;;
        "H"|"h"|"guard")         run_h ;;
        "Λ"|"lambda"|"build")    run_lambda ;;
        "Ω"|"omega"|"git")       run_omega ;;
        *) echo "Unknown phase: $SINGLE_PHASE (use Ψ, H, Λ, or Ω)" >&2; exit 1 ;;
    esac
else
    run_psi
    run_h
    run_lambda
    run_omega
fi

# ─── Summary ───────────────────────────────────────────────────────────────────
total_elapsed=$(( $(date +%s) - start_time ))
echo ""
if [[ "$overall" == "green" ]]; then
    echo -e "  ${GREEN}● dx.sh: ALL PHASES GREEN${RESET} (${total_elapsed}s)"
else
    echo -e "  ${RED}● dx.sh: RED — one or more phases failed${RESET} (${total_elapsed}s)"
fi
echo ""

# ─── Write receipt ─────────────────────────────────────────────────────────────
mkdir -p "$(dirname "$RECEIPT_FILE")"
GENERATED=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
python3 -c "
import json, sys
receipt = {
    'overall': sys.argv[1],
    'generated': sys.argv[2],
    'elapsed_seconds': int(sys.argv[3]),
    'violations': int(sys.argv[4]),
    'skip_verify': sys.argv[5] == 'true',
    'phases': {
        'observatory': sys.argv[6],
        'guard': sys.argv[7],
        'build': sys.argv[8],
        'git': sys.argv[9],
    }
}
print(json.dumps(receipt))
" "$overall" "$GENERATED" "$total_elapsed" "$total_violations" \
  "$SKIP_VERIFY" \
  "$phase_result_psi" "$phase_result_h" "$phase_result_lambda" "$phase_result_omega" \
  > "$RECEIPT_FILE"

# Exit 2 on failure (matches pre-tool-use.sh / guard conventions)
if [[ "$overall" != "green" ]]; then
    exit 2
fi
exit 0
