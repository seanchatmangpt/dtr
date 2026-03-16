# DTR-015: Add Dry-Run Mode to Release Automation

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,release,automation

## Description

Add a dry-run mode to the release automation pipeline that allows maintainers to preview what a release will do without making any actual changes. This prevents accidental releases and provides confidence in the release process before executing it.

The dry-run mode should:
- Simulate version bumping without modifying files
- Show git commands that would be executed
- Display Maven commands that would run
- List all files that would be modified
- Validate the release workflow without side effects

## Acceptance Criteria

- [ ] Add `--dry-run` flag to `scripts/release.sh` that prevents actual modifications
- [ ] Add `dry-run-minor`, `dry-run-patch`, and `dry-run-year` targets to Makefile
- [ ] Dry-run mode prints all commands that would be executed
- [ ] Dry-run mode validates environment (Java version, Maven version, git status)
- [ ] Dry-run mode shows exact version that would be released
- [ ] Dry-run mode lists all files that would be modified
- [ ] Add documentation for dry-run mode in README.md
- [ ] Add tests to verify dry-run doesn't modify filesystem

## Technical Notes

### Files to Modify

**`scripts/release.sh`**
```bash
# Add dry-run flag parsing
DRY_RUN=false
while [[ $# -gt 0 ]]; do
  case $1 in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    # ... other flags
  esac
done

# Wrap all destructive operations
if [ "$DRY_RUN" = true ]; then
  echo "[DRY-RUN] Would run: git tag $TAG"
  echo "[DRY-RUN] Would run: git commit -m '$COMMIT_MSG'"
else
  git tag $TAG
  git commit -m "$COMMIT_MSG"
fi
```

**`Makefile`**
```makefile
.PHONY: dry-run-minor dry-run-patch dry-run-year
dry-run-minor:
	./scripts/release.sh --dry-run minor

dry-run-patch:
	./scripts/release.sh --dry-run patch

dry-run-year:
	./scripts/release.sh --dry-run year
```

### Test Strategy
- Create integration test that runs dry-run mode
- Verify no files are modified after dry-run
- Verify dry-run output contains expected commands
- Test with --dry-run flag in various release scenarios

## Dependencies

- **DTR-016** (Pre-Release Validation) - dry-run should integrate with pre-release validation
- **DTR-017** (Post-Release Verification) - dry-run can be used to test verification scripts

## References

- Release script: `/Users/sac/dtr/scripts/release.sh`
- Makefile: `/Users/sac/dtr/Makefile`
- Related to [2026.4.0 DX/QoL Release Plan](../../CHANGELOG.md)

## Notes

Dry-run mode is particularly valuable for:
- New maintainers learning the release process
- Testing release automation changes without risk
- Validating environment setup before actual release
- Debugging release script issues in production-like scenarios
