---
name: release-check
description: Run pre-release validation checklist before cutting a release. Use this skill when the user asks to "check if we're ready to release", "pre-release validation", "release checklist", "is it safe to release", or "validate before tagging".
disable-model-invocation: true
tools: Bash, Read, Glob
---

Run the pre-release validation checklist and report what is/isn't ready.

## Validation Steps

### 1. CI Gate
```bash
cd $CLAUDE_PROJECT_DIR
mvnd verify --enable-preview --no-transfer-progress -B
```
- PASS: proceed
- FAIL: stop, report what failed

### 2. Current Version
```bash
cd $CLAUDE_PROJECT_DIR
cat pom.xml | grep -m1 '<version>'
```
- Report current version (e.g., `2026.1.0`)
- Confirm it is NOT a SNAPSHOT

### 3. Git Status
```bash
cd $CLAUDE_PROJECT_DIR
git status --short
git log --oneline -5
```
- Report any uncommitted changes (should be clean)
- Report last 5 commits

### 4. Release Branch Check
```bash
cd $CLAUDE_PROJECT_DIR
git branch --show-current
```
- Warn if not on `main` or `master`

### 5. GPG Key Availability (local only)
```bash
gpg --list-secret-keys 2>&1 | grep -E 'sec|uid' || echo "NO GPG KEY FOUND"
```
- Note: CI uses `$GPG_PRIVATE_KEY` secret — this is for local awareness only

### 6. Makefile Release Targets
```bash
cd $CLAUDE_PROJECT_DIR
grep -E '^release-' Makefile | head -10
```
- Confirm `release-minor`, `release-patch` targets exist

## Output Format

```
Pre-Release Checklist
─────────────────────
✓ CI Gate: PASS (47 tests, 12.3s)
✓ Version: 2026.1.0 (not SNAPSHOT)
✓ Git: clean working tree
✓ Branch: main
⚠ GPG: no local key (CI uses $GPG_PRIVATE_KEY secret — OK for CI release)
✓ Makefile: release-minor, release-patch targets present

VERDICT: Ready to release. Run: make release-minor
```

## The Armstrong Invariant

NEVER suggest running `mvn deploy` directly.
The only release path is:
```bash
make release-minor   # → YYYY.(N+1).0
make release-patch   # → YYYY.MINOR.(N+1)
make snapshot        # → SNAPSHOT (no tag, no release)
```
