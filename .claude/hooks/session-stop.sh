#!/bin/bash
# DTR Session Stop Hook
# Appends a brief session summary to .claude/session-log.md on session end.
# Runs as observe-only (Stop hooks cannot block execution).
set -euo pipefail

PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"
LOG_FILE="$PROJECT_DIR/.claude/session-log.md"
TIMESTAMP=$(date -u '+%Y-%m-%dT%H:%M:%SZ')
SESSION_ID="${CLAUDE_SESSION_ID:-unknown}"

# Collect git status (best effort — ignore errors)
GIT_STATUS=$(git -C "$PROJECT_DIR" status --short 2>/dev/null | head -20 || echo "(git unavailable)")
GIT_BRANCH=$(git -C "$PROJECT_DIR" branch --show-current 2>/dev/null || echo "unknown")
GIT_LOG=$(git -C "$PROJECT_DIR" log --oneline -3 2>/dev/null || echo "(no commits)")

# Check if surefire reports exist from this session (indicates verify was run)
SUREFIRE_DIR="$PROJECT_DIR/target/surefire-reports"
if [ -d "$SUREFIRE_DIR" ]; then
    TEST_COUNT=$(ls "$SUREFIRE_DIR"/*.txt 2>/dev/null | wc -l || echo 0)
    VERIFY_NOTE="Tests: $TEST_COUNT surefire report(s) in target/surefire-reports/"
else
    VERIFY_NOTE="Tests: mvnd verify not run this session (no surefire reports)"
fi

# Read Observatory guard status (best effort)
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

# Append to session log
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

EOF

echo "[session-stop] Summary appended to .claude/session-log.md" >&2
