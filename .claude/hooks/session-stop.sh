#!/bin/bash
# DTR Session Stop Hook — validation gate + session logger.
#
# Claude Code Stop hooks can BLOCK completion by outputting:
#   {"decision": "block", "reason": "..."}
# and exiting 0. The stop_hook_active flag prevents infinite loops.
#
# Block conditions (conservative — avoids blocking on pre-existing issues):
#   1. Uncommitted changes in the working tree
#   2. Unpushed commits on the current branch
#
# Observe-only (logged but never blocks):
#   - H-Guard status (pre-existing violations don't block; only new ones matter)
#   - Test run status (surefire report count)
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# ─── 1. Read the hook JSON payload ────────────────────────────────────────────
# Stop hooks receive JSON on stdin with session metadata including stop_hook_active.
INPUT=$(cat)

# ─── 2. stop_hook_active guard ────────────────────────────────────────────────
# When true, Claude is already in a forced-continuation state from a previous block.
# Exit cleanly to prevent an infinite loop.
STOP_HOOK_ACTIVE=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(str(d.get('stop_hook_active', False)).lower())
except:
    print('false')
" 2>/dev/null || echo "false")

if [[ "$STOP_HOOK_ACTIVE" == "true" ]]; then
    exit 0
fi

# ─── 3. Git validation — block on dirty state ─────────────────────────────────
block_reason=""

if git -C "$PROJECT_DIR" rev-parse --git-dir >/dev/null 2>&1; then
    # Check uncommitted changes (staged + unstaged)
    if ! git -C "$PROJECT_DIR" diff --quiet 2>/dev/null || \
       ! git -C "$PROJECT_DIR" diff --cached --quiet 2>/dev/null; then
        block_reason="Uncommitted changes detected. Commit and push before ending the session."
    fi

    # Check untracked files (excluding .gitignored)
    if [[ -z "$block_reason" ]]; then
        UNTRACKED=$(git -C "$PROJECT_DIR" ls-files --others --exclude-standard 2>/dev/null | head -5)
        if [[ -n "$UNTRACKED" ]]; then
            block_reason="Untracked files detected: ${UNTRACKED//$'\n'/, }. Commit and push before ending."
        fi
    fi

    # Check unpushed commits
    if [[ -z "$block_reason" ]]; then
        BRANCH=$(git -C "$PROJECT_DIR" branch --show-current 2>/dev/null || echo "")
        if [[ -n "$BRANCH" ]]; then
            if git -C "$PROJECT_DIR" rev-parse "origin/$BRANCH" >/dev/null 2>&1; then
                UNPUSHED=$(git -C "$PROJECT_DIR" rev-list "origin/$BRANCH..HEAD" --count 2>/dev/null || echo "0")
            else
                UNPUSHED=$(git -C "$PROJECT_DIR" rev-list "origin/HEAD..HEAD" --count 2>/dev/null || echo "0")
            fi
            if [[ "$UNPUSHED" -gt 0 ]]; then
                block_reason="$UNPUSHED unpushed commit(s) on '$BRANCH'. Push before ending the session."
            fi
        fi
    fi
fi

# ─── 4. Block if validation failed ────────────────────────────────────────────
if [[ -n "$block_reason" ]]; then
    python3 -c "
import json, sys
print(json.dumps({'decision': 'block', 'reason': sys.argv[1]}))
" "$block_reason"
    exit 0
fi

# ─── 5. Refresh Observatory facts (best effort, non-blocking) ─────────────────
OBSERVE_BIN="${PROJECT_DIR}/scripts/rust/dtr-observatory/target/release/dtr-observe"
if [[ -x "$OBSERVE_BIN" ]]; then
    "$OBSERVE_BIN" \
        --root "${PROJECT_DIR}" \
        --output "${PROJECT_DIR}/docs/facts" \
        --quiet 2>/dev/null || true
fi

# ─── 6. Collect session metadata ──────────────────────────────────────────────
LOG_FILE="$PROJECT_DIR/.claude/session-log.md"
TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
SESSION_ID=$(echo "$INPUT" | python3 -c "
import sys, json
try:
    d = json.loads(sys.stdin.read())
    print(d.get('session_id', 'unknown'))
except:
    print('unknown')
" 2>/dev/null || echo "unknown")

GIT_STATUS=$(git -C "$PROJECT_DIR" status --short 2>/dev/null | head -20 || echo "(git unavailable)")
GIT_BRANCH=$(git -C "$PROJECT_DIR" branch --show-current 2>/dev/null || echo "unknown")
GIT_LOG=$(git -C "$PROJECT_DIR" log --oneline -3 2>/dev/null || echo "(no commits)")

SUREFIRE_DIR="$PROJECT_DIR/target/surefire-reports"
if [ -d "$SUREFIRE_DIR" ]; then
    TEST_COUNT=$(ls "$SUREFIRE_DIR"/*.txt 2>/dev/null | wc -l || echo 0)
    VERIFY_NOTE="Tests: $TEST_COUNT surefire report(s) in target/surefire-reports/"
else
    VERIFY_NOTE="Tests: mvnd verify not run this session (no surefire reports)"
fi

FACTS_GUARD="$PROJECT_DIR/docs/facts/guard-status.json"
if [ -f "$FACTS_GUARD" ]; then
    GUARD_LINE=$(python3 -c "
import json, sys
try:
    d = json.load(open(sys.argv[1]))
    status = d.get('status', '?')
    v = d.get('violations', '?')
    f = d.get('scanned_files', '?')
    ts = d.get('generated', '')
    print(f'Guard: {status} | violations={v} | files_scanned={f} | as_of={ts}')
except Exception as e:
    print(f'Guard: unreadable ({e})')
" "$FACTS_GUARD" 2>/dev/null || echo "Guard: unavailable")
else
    GUARD_LINE="Guard: facts not generated (run: make observe)"
fi

# Check dx-receipt for pipeline status
DX_RECEIPT="$PROJECT_DIR/.claude/dx-receipt.json"
if [ -f "$DX_RECEIPT" ]; then
    DX_LINE=$(python3 -c "
import json, sys
try:
    d = json.load(open(sys.argv[1]))
    overall = d.get('overall', '?')
    phases = d.get('phases', {})
    print(f'dx pipeline: {overall} | {phases}')
except:
    print('dx pipeline: unreadable')
" "$DX_RECEIPT" 2>/dev/null || echo "dx pipeline: unavailable")
else
    DX_LINE="dx pipeline: not run this session (run: make dx-fast)"
fi

# ─── 7. Append session summary to log ─────────────────────────────────────────
mkdir -p "$(dirname "$LOG_FILE")"
cat >> "$LOG_FILE" << EOF

---

## Session: $TIMESTAMP
**Session ID:** $SESSION_ID
**Branch:** $GIT_BRANCH

### Last 3 Commits
\`\`\`
$GIT_LOG
\`\`\`

### Working Tree Status
\`\`\`
${GIT_STATUS:-clean}
\`\`\`

### Build
$VERIFY_NOTE

### Observatory
$GUARD_LINE

### Pipeline
$DX_LINE

EOF

echo "[session-stop] Validation passed. Summary appended to .claude/session-log.md" >&2
