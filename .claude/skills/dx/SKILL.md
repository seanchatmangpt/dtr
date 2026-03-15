---
name: dx
description: >
  Run the DTR five-phase validation pipeline (Observatory → Guards → Build → Git).
  Use when you want to check all phases pass, before declaring work complete,
  or to implement the completion-promise loop. Reports phase results and receipt.
tools: Bash, Read
---

# /dx — DTR Validation Pipeline

Run the five-phase Ψ→H→Λ→Ω validation pipeline and report results.

## Steps

1. Run the pipeline:

```bash
bash scripts/dx.sh --skip-verify
```

(Use `bash scripts/dx.sh` for the full pipeline including `mvnd verify`.)

2. Read the receipt:

```bash
cat .claude/dx-receipt.json
```

3. Report results as a phase table:

| Phase | Symbol | Status | Notes |
|-------|--------|--------|-------|
| Observatory | Ψ | `<psi_status>` | Facts refreshed / binary not built |
| H-Guards | H | `<guard_status>` | N violations in M files |
| Build/Verify | Λ | `<build_status>` | mvnd verify pass/fail/skipped |
| Git | Ω | `<git_status>` | Clean / uncommitted / unpushed |

**Overall:** `<overall>` (`<elapsed>`s)

## Completion-Promise Loop

This skill implements the ralph-loop pattern:

- If **all phases green**: work is done. Declare completion.
- If **any phase red**: diagnose the failure, fix it, then run `/dx` again.
- Never declare work complete while any phase is red.

## Phase Remedies

| Phase | Red Condition | Fix |
|-------|--------------|-----|
| Ψ (Observatory) | Binary not built | `make build-observatory` |
| H (Guards) | Semantic lies in source | Fix violations — `make guard-scan` for details |
| Λ (Build) | mvnd verify failed | Check `target/surefire-reports/` for failures |
| Ω (Git) | Uncommitted/unpushed changes | `git add`, `git commit`, `git push` |

## Fast Mode

Skip Phase Λ (build/verify) for faster iteration:

```bash
bash scripts/dx.sh --skip-verify
```

Equivalent: `make dx-fast`

## Single Phase

Run one phase only:

```bash
bash scripts/dx.sh --phase H       # H-Guards only
bash scripts/dx.sh --phase Ω       # Git only
bash scripts/dx.sh --phase omega   # same (aliases: psi/observatory, h/guard, lambda/build, omega/git)
```

## Receipt

`.claude/dx-receipt.json` — machine-readable phase results written after every run:

```json
{
  "overall": "green",
  "generated": "2026-03-14T10:00:00Z",
  "elapsed_seconds": 12,
  "violations": 0,
  "skip_verify": false,
  "phases": {
    "observatory": "green",
    "guard": "green",
    "build": "green",
    "git": "green"
  }
}
```

Status values: `green` | `red` | `skip`
