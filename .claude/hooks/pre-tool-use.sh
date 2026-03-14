#!/bin/bash
# DTR PreToolUse Safety Gate Hook
# Blocks destructive, irreversible operations before they execute.
# Only PreToolUse hooks can block tool execution (non-zero exit = blocked).
set -euo pipefail

TOOL="${CLAUDE_TOOL_NAME:-}"
INPUT="${CLAUDE_TOOL_INPUT:-}"

# Only gate Bash tool calls
if [ "$TOOL" != "Bash" ]; then
    exit 0
fi

# Extract the command string from JSON input (simple grep, no jq dependency)
CMD=$(echo "$INPUT" | grep -oP '"command"\s*:\s*"\K[^"]+' | head -1 || echo "$INPUT")

# ─── Blocked: destructive git operations ─────────────────────────────────────
if echo "$CMD" | grep -qP 'git\s+push\s+.*(-f|--force)\b'; then
    echo "[safety-gate] BLOCKED: Force push is disabled. Use 'git push' without --force." >&2
    exit 1
fi

if echo "$CMD" | grep -qP 'git\s+reset\s+--hard\b'; then
    echo "[safety-gate] BLOCKED: 'git reset --hard' discards uncommitted work. Commit or stash first." >&2
    exit 1
fi

if echo "$CMD" | grep -qP 'git\s+(checkout|restore)\s+--\s*\.?\s*$'; then
    echo "[safety-gate] BLOCKED: Discarding all working tree changes requires explicit user confirmation." >&2
    exit 1
fi

if echo "$CMD" | grep -qP 'git\s+clean\s+.*-f\b'; then
    echo "[safety-gate] BLOCKED: 'git clean -f' permanently deletes untracked files." >&2
    exit 1
fi

# ─── Blocked: destructive filesystem operations ───────────────────────────────
if echo "$CMD" | grep -qP '\brm\s+.*-rf?\b.*/(src|dtr-core|dtr-integration-test|\.claude|\.github|scripts)\b'; then
    echo "[safety-gate] BLOCKED: Recursive delete of project directories is not allowed." >&2
    exit 1
fi

# ─── Blocked: publish/deploy outside the pipeline ────────────────────────────
if echo "$CMD" | grep -qP 'mvn(d?)\s+deploy\b' && ! echo "$CMD" | grep -qP '\-Prelease\b'; then
    echo "[safety-gate] BLOCKED: Use 'make release-minor' or 'make snapshot' — never run 'mvn deploy' directly." >&2
    exit 1
fi

exit 0
