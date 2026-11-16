# doctester migration plan v26.11.16

Standing: UNVERIFIED

## Identity
- Path: /Users/sac/doctester
- Remote: https://github.com/seanchatmangpt/doctester (fork of r10r-org/doctester)
- Branch: master@9649ff6 (upstream origin/master, behind 392)
- Last commit: 2026-03-10 ("Merge pull request #16 ... claude/plan-major-release")
- Commits: 205; size: 11 MB
- Languages/build: Java / Maven (pom.xml, mvnw), modules doctester-core,
  doctester-integration-test; CLAUDE.md present; ~20 RELEASE_*/AUDIT_* markdown files at root
- Nested repos: none; `jotp` is a submodule gitlink (25c8c16), directory is empty locally

## Observed state
- Dirty: 5 (` D .claude/agents/java-25-expert.md`, ` D .claude/agents/maven-build-expert.md`,
  ` M pom.xml`, ` M doctester-integration-test/pom.xml`,
  ` M doctester-integration-test/src/main/java/conf/Module.java`), untracked: `docs/jira/`.
- Ahead 0 / behind 392 — local master is far behind origin (work landed via PRs on GitHub).
- Stash: 0. Unpushed branches: 0. Only one local branch.
- Build dirs: no `target/` present.
- Submodules: `jotp` registered but not checked out.

## What it is
DocTester: a Java/JUnit testing framework that generates Bootstrap-styled HTML API docs while
tests run. Sean's fork prepared a 2.0.0 / Java 25 major release (release plans, audits, Maven
Central checklist all at root).

## Classification
DORMANT (census agrees; last local commit March 2026, 392 commits behind its own remote).

## Migration risk
- 3 small pom/Module.java edits are uncommitted and probably stale relative to origin (392
  behind) — may be superseded. Two deleted `.claude/agents` files.
- `RELEASE_CREDENTIALS_CHECKLIST.md` name suggests credential handling; not inspected.
- Nothing else local-only.
Rating: LOW.

## Recommended action
CLONE-FRESH

1. `git -C /Users/sac/doctester diff > /Volumes/archive/doctester-local.diff` (keep the 3
   pom/Java edits as a patch in case they matter).
2. DECISION: review the diff against origin/master; if already upstream, discard.
3. Do not copy. New machine: `git clone https://github.com/seanchatmangpt/doctester` and
   `git submodule update --init jotp` if jotp is still needed.

## First commands on the new machine
`./mvnw -q -DskipTests package` then `./mvnw test` (JDK 25 per CLAUDE.md). Stated, not run.

## See Also
- ../v26.9.19/
- /Users/sac/docs/jira/v26.11.16/README.md
