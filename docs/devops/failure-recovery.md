# Failure Recovery & Rollback Procedures

## Overview

This document covers failure scenarios, recovery procedures, and rollback strategies for the DTR project's release pipeline. Understanding these procedures is critical for maintaining release integrity and handling deployment failures gracefully.

---

## The Failure Recovery Philosophy

**Principle**: Git history is immutable. Releases are receipts. When things fail, we document and move forward, never delete or hide failures.

### Key Invariants

1. **Never delete tags** - Failed releases are part of history
2. **Never rewrite git history** - No force pushes, no rebase
3. **Always increment versions** - Next release fixes the previous
4. **Document failures** - Changelog should explain what happened

---

## Failure Scenario Matrix

| Scenario | Impact | Recovery Strategy | Maven Central? |
|----------|--------|-------------------|----------------|
| Tag push fails | No artifact published | Fix issue, re-run release | No |
| CI gate fails | No artifact published | Fix issue, create patch | No |
| Deploy fails | No artifact published | Fix issue, create patch | No |
| GitHub release fails | Artifact published, no release | Manual release creation | Yes |
| Artifact published with bug | Public artifact available | Patch release with fix | Yes |

---

## Scenario 1: Tag Push Fails

### Symptoms

```bash
make release-minor
# ...
# error: failed to push some refs to 'https://github.com/...'
# ! [rejected]        v2026.3.0 -> v2026.3.0 (would clobber existing tag)
```

### Causes

- Tag already exists on remote (concurrent release attempt)
- Network connectivity issue
- Authentication failure
- Branch protection rules

### Recovery Procedure

```bash
# 1. Check tag status locally
git tag -l "v*"

# 2. Check remote tags
git ls-remote --tags origin

# 3. Pull latest changes
git pull origin main

# 4. Determine if tag exists on remote
git tag -d v2026.3.0  # Only if exists locally and needs to be removed

# 5. If tag exists on remote, DO NOT delete it
#    - This means someone else released this version
#    - Create a patch instead:
make release-patch
```

### Prevention

- Coordinate releases in team communication channels
- Check `git ls-remote --tags origin` before releasing
- Use branch protection to require PR review

---

## Scenario 2: CI Gate Fails

### Symptoms

GitHub Actions shows `ci-gate.yml` failed with:
- Test failure
- Quality check failure (Spotless, Checkstyle, PMD)
- Security scan finding
- Build verification failure

### Impact

- **`publish.yml` does NOT run** (only triggered on successful CI gate)
- No artifact is deployed to Maven Central
- Git tag exists but no corresponding release

### Recovery Procedure

```bash
# 1. Identify the failure
gh run view --log-failed

# 2. Fix the issue locally
#    - Fix failing test
#    - Run spotlessApply
#    - Address security finding

# 3. Verify fix locally
mvnd verify --enable-preview

# 4. Create a patch release
#    The failed tag remains in history (as documentation)
#    The patch release contains the fix
make release-patch

# Example:
# Failed: v2026.3.0 (tests fail)
# Fix: v2026.2.1 (fixes included)
```

### What NOT to Do

**Do NOT delete the failed tag**
```bash
# WRONG:
git tag -d v2026.3.0
git push origin :refs/tags/v2026.3.0
```

### Changelog Entry

```markdown
## v2026.2.1 (2026-03-15)

### Bug Fixes
- Fix failing integration test that blocked v2026.3.0 release

### Notes
- v2026.3.0 tag exists but was not published due to test failure
```

---

## Scenario 3: Maven Central Deploy Fails

### Symptoms

- `ci-gate.yml` passes
- `publish.yml` runs but `mvnd deploy` fails
- Error in "Deploy to Maven Central" step

### Common Failure Causes

| Error | Cause | Solution |
|-------|-------|----------|
| `401 Unauthorized` | Wrong `CENTRAL_USERNAME` or `CENTRAL_TOKEN` | Update secrets in GitHub settings |
| `Missing POM` | Invalid `pom.xml` configuration | Fix POM and retry with patch |
| `Signature verification failed` | GPG key issue | See [GPG Management](gpg-management.md) |
| `Staging failed` | Maven Central validation error | Check Nexus UI for details |

### Recovery Procedure

#### Step 1: Check Staging Repository

```bash
# Go to Sonatype Central Portal
# https://central.sonatype.com/

# Check if staging repository was created
# Look for validation errors
```

#### Step 2: Fix the Issue

```bash
# Fix the issue (POM, GPG, credentials)
# Create patch release
make release-patch
```

#### Step 3: Drop Failed Staging Repo

```bash
# In Sonatype Central Portal UI:
# 1. Find the failed staging repository
# 2. Click "Drop" to remove it
# 3. Confirm deletion
```

### If Artifact Was Promoted

**Critical**: If the artifact was successfully promoted to Maven Central, it **cannot be deleted**.

```bash
# OPTIONS:
# 1. If the issue is minor (wrong metadata), document and move on
# 2. If the issue is severe (security bug), create patch release ASAP
# 3. Contact Sonatype support for critical issues (rare, requires justification)
```

---

## Scenario 4: GitHub Release Creation Fails

### Symptoms

- Maven Central deployment succeeded
- `gh release create` command failed
- Artifact is on Maven Central but no GitHub Release

### Recovery Procedure

#### Step 1: Verify Maven Central Success

```bash
# Check if artifact is available
curl -I https://repo1.maven.org/maven2/io/github/seanchatmangpt/dtr/2026.3.0/

# If 200 OK, artifact is published
# If 404, deployment failed - go to Scenario 3
```

#### Step 2: Create GitHub Release Manually

```bash
# Option A: Auto-generate notes from commits
gh release create v2026.3.0 --generate-notes

# Option B: Use custom notes
gh release create v2026.3.0 \
  --title "v2026.3.0" \
  --notes "Release notes here..."

# Option C: Read from changelog
gh release create v2026.3.0 \
  --notes-file docs/releases/2026.3.0.md
```

#### Step 3: Verify

```bash
gh release view v2026.3.0
```

---

## Scenario 5: Year Boundary Release

### The Edge Case

Releasing at 23:59 on December 31 can cause version inconsistencies:

```bash
# scripts/bump.sh uses `date +%Y` at execution time
# If year rolls over during release:
# First script run: date +%Y → 2026
# ... release continues ...
# Second script run: date +%Y → 2027
# Result: Inconsistent version numbers
```

### Prevention

```bash
# Avoid releasing within 1 hour of year boundary
# Or explicitly specify year:

# In January, after year boundary:
make release-year  # Explicitly sets new year
```

### Recovery

```bash
# If version has wrong year:
make release-year  # Creates v2027.1.0

# Then fix the incorrect version if needed
# (v2026.13.0 would be wrong - should be v2027.1.0)
```

---

## Scenario 6: Concurrent Release Attempts

### The Problem

Two developers run `make release-minor` simultaneously, creating conflicting tags.

### Symptoms

```bash
# Developer A
make release-minor
# Creates v2026.3.0, pushes successfully

# Developer B (simultaneous)
make release-minor
# Error: ! [rejected] v2026.3.0 -> v2026.3.0 (would clobber existing tag)
```

### What Happens

1. Both developers compute the same NEXT version
2. Developer A's push succeeds first
3. Developer B's push is rejected by Git

### Recovery Procedure

```bash
# Developer B: Pull latest changes
git pull origin main

# Check if tag exists on remote
git ls-remote --tags origin | grep v2026.3.0

# Tag exists - DO NOT delete it
# Create a patch release instead:
make release-patch
# Creates v2026.2.1 with your changes
```

### Prevention

1. **Communicate** in team channels before releasing
2. **Check existing tags** before running release:
   ```bash
   git ls-remote --tags origin | grep "v2026"
   ```
3. **Use branch protection** requiring PR review
4. **Create RC first** to reserve the version:
   ```bash
   make release-rc-minor  # Reserves v2026.3.0-rc.1
   ```

### Team Coordination

```
🚨 RELEASE IN PROGRESS: v2026.3.0
Started by: @username
ETA: 5 minutes
Please wait until complete before releasing.
```

---

## Scenario 7: RC to Final Promotion

### When to Promote

After testing an RC and verifying it works correctly:

```bash
# RC is good, promote to final
make release-minor
```

### What Happens During Promotion

1. `scripts/bump.sh` computes NEXT version
2. The `-rc.N` suffix is **stripped** from the version
3. The minor number is **NOT** incremented again
4. Maven Central receives the final artifact

### Version Number Behavior

```bash
# Step 1: Create RC
make release-rc-minor
# Result: v2026.3.0-rc.1

# Step 2: RC needs fixes
# (make code changes)
make release-rc-minor
# Result: v2026.3.0-rc.2 (N auto-incremented)

# Step 3: Promote to final
make release-minor
# Result: v2026.3.0 (same minor number, -rc.N stripped)
```

### Verification Before Promotion

```bash
# 1. Verify RC in GitHub Packages
gh api /orgs/YOUR_ORG/packages/maven/io.github.seanchatmangpt.dtr/versions

# 2. Download and test RC
mvn dependency:get \
  -Dartifact=io.github.seanchatmangpt.dtr:dtr-core:2026.3.0-rc.2

# 3. Run integration tests with RC
mvnd verify -Prc-test

# 4. If all good, promote
make release-minor
```

### What NOT to Do

**Don't** run `make release-minor` twice - it will increment to v2026.3.0
**Don't** manually delete RC tags before promotion
**Don't** skip RC testing - RCs exist to catch issues before Maven Central

---

## Rollback Procedures

### GitHub Release Rollback

GitHub Releases can be deleted (the tag remains):

```bash
# Delete release only (tag stays)
gh release delete v2026.3.0 --yes

# Delete both release and tag (NOT RECOMMENDED)
gh release delete v2026.3.0 --yes
git push origin :refs/tags/v2026.3.0
git tag -d v2026.3.0
```

### RC Rollback (GitHub Packages)

```bash
# List package versions
gh api /orgs/YOUR_ORG/packages/maven/io.github.seanchatmangpt.dtr/versions

# Delete specific version
VERSION_ID="12345678"
gh api -X DELETE /orgs/YOUR_ORG/packages/maven/io.github.seanchatmangpt.dtr/versions/$VERSION_ID
```

### Maven Central Rollback

**Maven Central does NOT support artifact deletion or retraction.**

If a bad artifact is on Maven Central:

```bash
# OPTIONS:
# 1. Create a patch release with the fix (preferred)
# 2. Mark artifact as deprecated in next release
# 3. Contact Sonatype support for security issues only
```

---

## Recovery Quick Reference

| Situation | Command |
|-----------|---------|
| CI gate failed | `make release-patch` |
| Tag already exists | `git pull` + `make release-patch` |
| Deploy failed, fix ready | `make release-patch` |
| GitHub release failed | `gh release create vVERSION --generate-notes` |
| RC needs removal | `gh api -X DELETE /orgs/.../versions/ID` |
| Maven Central artifact bad | Create patch, cannot delete |

---

## Prevention Strategies

### Pre-Release Checklist

```bash
# 1. Verify all tests pass
mvnd verify --enable-preview

# 2. Check quality gates
mvnd spotless:check
mvnd checkstyle:check
mvnd pmd:check
mvnd spotbugs:check

# 3. Verify version
./scripts/current-version.sh

# 4. Check for existing tags
git ls-remote --tags origin | grep v2026

# 5. Test with RC first
make release-rc-minor
# Verify in GitHub Actions
# Then promote:
make release-minor
```

### CI/CD Monitoring

```bash
# Watch for failing workflows
gh run list --workflow=ci-gate.yml

# Get detailed failure info
gh run view --log-failed

# Set up notifications (GitHub UI)
# Settings → Notifications → Workflow failures
```

---

## Communication Protocol

When a release fails:

1. **Immediate**: Notify team in designated channel
2. **Investigation**: Identify root cause using `gh run view`
3. **Documentation**: Update CHANGELOG with explanation
4. **Recovery**: Execute appropriate recovery procedure
5. **Post-mortem**: Document lessons learned (if needed)

### Example Team Communication

```
🚨 RELEASE FAILURE: v2026.3.0

Workflow: https://github.com/org/repo/actions/runs/12345
Cause: Integration test failure in PaymentProcessorTest
Impact: No artifact published to Maven Central
Recovery: Will create patch release v2026.2.1 after fix

Assigned: @username
ETA: 2 hours
```

---

## Appendix: Complete Recovery Example

```bash
# === SCENARIO: CI gate fails for v2026.3.0 ===

# 1. Investigate failure
gh run list --workflow=ci-gate.yml --limit 5
gh run view 123456789 --log-failed

# Output shows:
# PaymentProcessorTest > testRefund() FAILED
# Expected: 200, Actual: 500

# 2. Fix the issue
# Edit code, fix bug

# 3. Verify locally
mvnd verify --enable-preview

# 4. Check if version needs increment
./scripts/current-version.sh
# Output: 2026.3.0

# 5. Create patch release
make release-patch

# This creates v2026.2.1 with the fix
# v2026.3.0 tag remains (as historical record)

# 6. Monitor deployment
gh run watch

# 7. Verify success
gh release view v2026.2.1
curl -I https://repo1.maven.org/maven2/.../2026.2.1/
```

---

**Last Updated:** March 14, 2026
**Related:** [GPG Management](gpg-management.md) | [Infrastructure Research](infrastructure-research.md)
