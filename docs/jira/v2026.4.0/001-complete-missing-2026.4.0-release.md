# DTR-001: Complete Missing 2026.4.0 Release

**Priority**: P1
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,release,qol

## Description

The 2026.4.0 DX/QoL release has been developed but the formal release process has not been completed. This ticket covers all remaining steps to finalize the release, including documentation updates, version tagging, and deployment verification.

## Acceptance Criteria

- [ ] Release notes generated and committed to `docs/releases/2026.4.0.md`
- [ ] `CHANGELOG.md` updated with 2026.4.0 release entry
- [ ] Git tag `v2026.4.0` created and pushed to remote
- [ ] GitHub Actions workflow triggered by tag succeeds
- [ ] Artifact deployed to Maven Central (verify at https://repo1.maven.org/maven2/org/sparsevoid/dtr/)
- [ ] Website/docs updated (if applicable)

## Technical Notes

### Version Calculation
- **Current Year**: 2026 (from Calendar invariant)
- **Minor Version**: 4 (DX/QoL feature release)
- **Patch Version**: 0 (initial release)

### Release Process
1. Run `make release-minor` (should compute `2026.4.0`)
2. Script handles: version bump, commit, tag, push
3. CI gate: `mvnd verify --enable-preview` must pass
4. Deployment: `mvnd deploy -Prelease` publishes to Maven Central

### Key Files
- `/Users/sac/dtr/CHANGELOG.md` - Update with release notes
- `/Users/sac/dtr/pom.xml` - Version will be auto-bumped by Make target
- `/Users/sac/dtr/Makefile` - Contains release automation
- `/Users/sac/dtr/.github/workflows/*` - CI/CD pipelines

### Verification Commands
```bash
# Verify tag exists
git tag -l | grep v2026.4.0

# Verify Maven Central deployment
curl -I https://repo1.maven.org/maven2/org/sparsevoid/dtr/dtr-2026.4.0.pom

# Check GitHub Actions run
gh run list --tags=v2026.4.0
```

## Dependencies

- DTR-002 (Add Missing sayAndAssertThat Methods) - Should be included in release
- DTR-003 (Add sayRef Convenience Overload) - Should be included in release
- DTR-004 (Developer Build Optimizations) - Should be included in release
- DTR-005 (Fix CLI Python Version Documentation) - Should be included in release

## References

- DTR Release Manual: See `CLAUDE.md` section "RELEASE SEMANTICS"
- CI Configuration: `.github/workflows/`
- Maven Central: https://central.sonatype.com/
