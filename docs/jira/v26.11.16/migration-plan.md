# dtr migration plan v26.11.16

Standing: UNVERIFIED

## Identity
- Path: /Users/sac/dtr
- Remote: https://github.com/seanchatmangpt/dtr.git
- Branch: docs-update-2026.4.1@9823c92 (upstream origin/docs-update-2026.4.1: GONE)
- Last commit: 2026-03-15; commits: 593; size: 2003 MB (.git only 30 MB; dtr-core/docs 1.9 GB)
- Languages/build: Java 26 / Maven (Makefile, mvnd), modules dtr-core, dtr-cli, dtr-benchmarks
- Nested repos: jotp (submodule, uninitialized: `-25c8c163`)

## Observed state
- Dirty 6: 4 deleted `.claude/agents/*.md`, modified `.claude/session-log.md`,
  `docs/meta/javadoc.json`. Untracked 4: `docs/jira/v26.9.19/` etc.
- Stash: 580 (census) — very large; contents unknown (not listed, per lane rule).
- Branches with upstream GONE: docs-update-2026.4.1, environment-validation,
  feat/java-26-migration-github-actions; feat/java-26-with-calver has no upstream (3 unpushed).
- Build dirs: none named target/, but `dtr-core/docs` is 1.9 GB (generated docs/artifacts).

## What it is
DTR, Documentation Testing Runtime: turns Java documentation into executable tests;
published to Maven Central as io.github.seanchatmangpt.dtr, version 2026.4.1.

## Classification
DORMANT (census agrees; last commit March 2026).

## Migration risk
HIGH. 580 stashes, 3 branches whose remotes were deleted, uncommitted .claude changes.
1.9 GB of generated docs would bloat any copy.

## Recommended action
COMMIT-PUSH
1. `cd /Users/sac/dtr && git stash list | wc -l` ; DECISION: triage or discard 580 stashes.
   Safe default: `git bundle create /Volumes/archive/dtr-full.bundle --all` (bundles do not
   include stashes; export stashes via `git stash show -p stash@{N}` if any matter).
2. `git add -A && git commit -m "chore: pending .claude + docs state"`
3. `git push -u origin docs-update-2026.4.1 feat/java-26-with-calver environment-validation feat/java-26-migration-github-actions`
4. `du -sh dtr-core/docs` and DECISION: delete generated 1.9 GB before any copy.
5. CLONE-FRESH afterwards with `--recurse-submodules` (jotp).

## First commands on the new machine
`make check` (or `mvnd -q verify`)

## See Also
- ../v26.9.19/
- /Users/sac/docs/jira/v26.11.16/README.md
