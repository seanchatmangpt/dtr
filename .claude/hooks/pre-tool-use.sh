#!/bin/bash
# DTR PreToolUse Safety Gate Hook
# Blocks destructive, irreversible operations before they execute.
# Only PreToolUse hooks can block tool execution (non-zero exit = blocked).
set -euo pipefail

TOOL="${CLAUDE_TOOL_NAME:-}"
INPUT="${CLAUDE_TOOL_INPUT:-}"

# ─── H-Guard scan for Write/Edit tool calls on Java source files ─────────────
# Scans proposed content BEFORE it is written, blocking semantic lies at write time.
# Patterns: H_TODO, H_MOCK, H_MOCK_CLASS, H_STUB, H_EMPTY, H_FALLBACK, H_SILENT
GUARD_BIN="$(cd "$(dirname "$0")/../.." && pwd)/scripts/rust/dtr-guard/target/release/dtr-guard-scan"

if [[ "$TOOL" == "Write" || "$TOOL" == "Edit" ]] && [[ -x "$GUARD_BIN" ]]; then
    # Extract file_path and proposed content from the JSON tool input
    FILE_PATH=$(python3 -c "import sys,json; d=json.loads(sys.argv[1]); print(d.get('file_path',''))" "$INPUT" 2>/dev/null || true)

    if [[ "$FILE_PATH" == *.java ]]; then
        # Extract the content to scan: 'content' for Write, 'new_string' for Edit
        PROPOSED=$(python3 -c "
import sys, json
d = json.loads(sys.argv[1])
# For Write: full file content; for Edit: the replacement string
print(d.get('content', d.get('new_string', '')))
" "$INPUT" 2>/dev/null || true)

        if [[ -n "$PROPOSED" ]]; then
            TMPFILE=$(mktemp /tmp/dtr-guard-XXXXXX.java)
            echo "$PROPOSED" > "$TMPFILE"
            if ! "$GUARD_BIN" --content "$TMPFILE" "$FILE_PATH" 2>&1; then
                rm -f "$TMPFILE"
                exit 2
            fi
            rm -f "$TMPFILE"
        fi
    fi
fi

# Only gate remaining checks for Bash tool calls
if [ "$TOOL" != "Bash" ]; then
    exit 0
fi

# Extract the command string from JSON input (python3 for cross-platform compatibility)
CMD=$(python3 -c "import sys,json; d=json.loads(sys.argv[1]); print(d.get('command',''))" "$INPUT" 2>/dev/null || echo "$INPUT")

# ─── Blocked: destructive git operations ─────────────────────────────────────
if echo "$CMD" | grep -qE 'git[[:space:]]+push[[:space:]]+.*(-f|--force)'; then
    echo "[safety-gate] BLOCKED: Force push is disabled. Use 'git push' without --force." >&2
    exit 1
fi

if echo "$CMD" | grep -qE 'git[[:space:]]+reset[[:space:]]+--hard'; then
    echo "[safety-gate] BLOCKED: 'git reset --hard' discards uncommitted work. Commit or stash first." >&2
    exit 1
fi

if echo "$CMD" | grep -qE 'git[[:space:]]+(checkout|restore)[[:space:]]+--[[:space:]]*\.?[[:space:]]*$'; then
    echo "[safety-gate] BLOCKED: Discarding all working tree changes requires explicit user confirmation." >&2
    exit 1
fi

if echo "$CMD" | grep -qE 'git[[:space:]]+clean[[:space:]]+.*-f'; then
    echo "[safety-gate] BLOCKED: 'git clean -f' permanently deletes untracked files." >&2
    exit 1
fi

# ─── Blocked: destructive filesystem operations ───────────────────────────────
if echo "$CMD" | grep -qE '(^|[[:space:]])rm[[:space:]]+.*-rf?[[:space:]].*/(src|dtr-core|dtr-integration-test|\.claude|\.github|scripts)'; then
    echo "[safety-gate] BLOCKED: Recursive delete of project directories is not allowed." >&2
    exit 1
fi

# ─── Blocked: publish/deploy outside the pipeline ────────────────────────────
if echo "$CMD" | grep -qE 'mvnd?[[:space:]]+deploy' && ! echo "$CMD" | grep -qE '\-Prelease'; then
    echo "[safety-gate] BLOCKED: Use 'make release-minor' or 'make snapshot' — never run 'mvn deploy' directly." >&2
    exit 1
fi

exit 0
