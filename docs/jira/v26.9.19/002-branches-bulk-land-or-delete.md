# doctester: triage 15 PR-less unmerged remote branches

- Standing: OPEN
- Created: 2026-09-19 (v26.9.19 gh survey wave)
- Source: origin branches not merged into `master` with no open PR
- Evidence: `git branch -r --no-merged origin/master`: claude/add-claude-documentation-c5zqy claude/add-vision-2030-utils-i6kuc claude/armstrong-agi-checklist-WxUq8 claude/doctest-stress-testing-x9XrG claude/document-agentic-loop-7GPZn claude/fix-macos-bash-tools-eQomB claude/fix-version-sync-pom-Rcgyw claude/implement-dtr-publish-4i0t8 claude/launch-agent-testing-XDEUd claude/openapi-rdf-guide-73rP7 claude/openapi-rdf-guide-Ob0Sg claude/openapi-rdf-guide-cEoGB claude/reactive-messaging-thesis-yBbJw claude/rename-doctester-to-dtr-m79sP revert-22-claude/java-26-showcase-93GCp

## Work to complete
- Triage each branch: land (open a PR) or delete (`git push origin --delete <branch>`). Work in batches; record decisions in History.

## Acceptance
- `git branch -r --no-merged origin/master` is empty after `git fetch --prune`.

## History
- 2026-09-19 | OPEN | survey found 15 PR-less branches | full list above | triage pending
