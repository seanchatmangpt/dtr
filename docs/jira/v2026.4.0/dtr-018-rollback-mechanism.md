# DTR-018: Release Rollback Mechanism

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,release,automation

## Description

Create automated rollback mechanism for emergency recovery when a release fails or needs to be undone. This provides safety net for release automation and allows quick recovery from failed releases.

The rollback mechanism should:
- Delete git tag (local and remote)
- Create rollback commit (revert version bump)
- Provide option to drop Maven Central version (if needed)
- Document manual rollback steps for Maven Central
- Validate rollback was successful
- Provide clear rollback report

## Acceptance Criteria

- [ ] Create `scripts/rollback-release.sh` with comprehensive rollback logic
- [ ] Rollback script validates environment before proceeding
- [ ] Rollback script confirms action (interactive confirmation)
- [ ] Rollback script removes local and remote git tags
- [ ] Rollback script creates revert commit for version changes
- [ ] Rollback script provides Maven Central deletion instructions
- [ ] Add `rollback-release` target to Makefile
- [ ] Add tests for rollback script (test on dummy tags/commits)
- [ ] Document rollback procedures in README.md

## Technical Notes

### File to Create

**`scripts/rollback-release.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-}
if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 2026.4.0"
  exit 1
fi

TAG="v$VERSION"

echo "=== Release Rollback: $VERSION ==="
echo ""
echo "WARNING: This will:"
echo "  - Delete local git tag: $TAG"
echo "  - Delete remote git tag: $TAG"
echo "  - Create revert commit for version changes"
echo ""
read -p "Are you sure you want to rollback release $VERSION? (yes/no): " confirm

if [ "$confirm" != "yes" ]; then
  echo "Rollback cancelled."
  exit 0
fi

# Step 1: Check current state
echo ""
echo "Checking current state..."

# Verify tag exists locally
if ! git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "❌ ERROR: Tag $TAG does not exist locally"
  exit 1
fi
echo "✓ Tag $TAG exists locally"

# Get the commit that the tag points to
TAG_COMMIT=$(git rev-parse "$TAG^{}")
echo "   Tag points to commit: $TAG_COMMIT"

# Step 2: Delete local tag
echo ""
echo "Deleting local tag..."
git tag -d "$TAG"
echo "✓ Local tag deleted"

# Step 3: Delete remote tag
echo ""
echo "Deleting remote tag..."
if git ls-remote --tags origin | grep -q "refs/tags/$TAG"; then
  git push origin ":refs/tags/$TAG"
  echo "✓ Remote tag deleted"
else
  echo "⚠ WARNING: Remote tag $TAG not found (may have been deleted already)"
fi

# Step 4: Create revert commit
echo ""
echo "Creating revert commit for version changes..."

# Get files changed in the release commit
CHANGED_FILES=$(git diff --name-only "$TAG_COMMIT^" "$TAG_COMMIT")

if [ -n "$CHANGED_FILES" ]; then
  echo "Files changed in release:"
  echo "$CHANGED_FILES"
  echo ""

  # Revert the release commit
  git revert --no-commit "$TAG_COMMIT"
  git commit -m "Rollback release $VERSION

This reverts commit $TAG_COMMIT which released version $VERSION.

Rollback performed by: $(git config user.name) <$(git config user.email)>
Rollback date: $(date -u +"%Y-%m-%d %H:%M:%S UTC")
"
  echo "✓ Revert commit created"
else
  echo "⚠ WARNING: No files changed in release commit, skipping revert"
fi

# Step 5: Maven Central rollback instructions
echo ""
echo "=== Maven Central Rollback Instructions ==="
echo ""
echo "NOTE: Maven Central does NOT support deleting released artifacts."
echo "Once published, artifacts remain on Maven Central permanently."
echo ""
echo "Options for Maven Central:"
echo ""
echo "1. DO NOTHING (Recommended if release is valid)"
echo "   - Leave artifact on Maven Central"
echo "   - Release a new patch version with fixes"
echo "   - Mark problematic version as deprecated in Javadoc"
echo ""
echo "2. REQUEST REMOVAL (Only for security issues or legal problems)"
echo "   - Email: repository-admin@maven.apache.org"
echo "   - Subject: URGENT: Remove io.github.dtr_project:dtr-core:$VERSION"
echo "   - Explain: Security issue, legal problem, or critical bug"
echo "   - Note: This is exceptional and requires valid reason"
echo ""
echo "3. WAIT FOR NEW RELEASE"
echo "   - Release a new version quickly (patch or minor)"
echo "   - New version will supersede old version"
echo "   - Users will migrate to new version"
echo ""
echo "Maven Central artifact URL:"
echo "https://repo.maven.apache.org/maven2/io/github/dtr_project/dtr-core/$VERSION/"
echo ""

# Step 6: Verification
echo "=== Rollback Summary ==="
echo ""
echo "Git status:"
git status --short
echo ""

# Verify tag is gone
if git rev-parse "$TAG" >/dev/null 2>&1; then
  echo "❌ ERROR: Local tag still exists"
  exit 1
fi
echo "✓ Local tag successfully removed"

# Verify remote tag is gone
if git ls-remote --tags origin | grep -q "refs/tags/$TAG"; then
  echo "❌ ERROR: Remote tag still exists"
  exit 1
fi
echo "✓ Remote tag successfully removed"

echo ""
echo "=== Rollback Complete ==="
echo ""
echo "Next steps:"
echo "1. Review revert commit"
echo "2. Push revert commit: git push origin $(git branch --show-current)"
echo "3. Follow Maven Central instructions above"
echo "4. Fix issues and prepare new release"
echo ""
exit 0
```

### Makefile Addition

**`Makefile`**
```makefile
.PHONY: rollback-release
rollback-release:
	./scripts/rollback-release.sh $(VERSION)
```

### Maven Central Removal Request Template

**`docs/maven-central-removal-request.txt`**
```text
Subject: URGENT: Remove io.github.dtr_project:dtr-core:VERSION

To: repository-admin@maven.apache.org

Dear Maven Central Repository Team,

I request the removal of the following artifact from Maven Central:

- Group ID: io.github.dtr_project
- Artifact ID: dtr-core
- Version: VERSION

Reason for removal:
[Select one and explain]
- Security vulnerability: [describe issue]
- Legal issue: [describe issue]
- Critical bug: [describe issue]
- Other: [describe issue]

The artifact was released on DATE but must be removed due to [reason].

I understand that artifact removal is exceptional and is only granted for valid reasons.

I have prepared a new version (NEW_VERSION) that fixes the issue and will release it shortly.

Thank you for your assistance.

Best regards,
[Your Name]
DTR Project Maintainer
```

## Dependencies

- **DTR-016** (Pre-Release Validation) - reduces likelihood of needing rollback
- **DTR-017** (Post-Release Verification) - early detection may prevent rollback

## References

- Release script: `/Users/sac/dtr/scripts/release.sh`
- Makefile: `/Users/sac/dtr/Makefile`
- Maven Central removal policy: https://maven.apache.org/repository-admin.html
- Email for removal requests: repository-admin@maven.apache.org

## Notes

**Important: Maven Central artifacts cannot be deleted once published.** The rollback mechanism primarily handles git operations. For Maven Central, options are:

1. **Do nothing** - Leave artifact, release new version with fixes (most common)
2. **Request removal** - Only for security issues or legal problems (exceptional)
3. **Wait and supersede** - Release new version quickly

**When to use rollback:**
- Failed deployment (artifact never reached Maven Central)
- Wrong version released (caught before Maven Central sync)
- Critical bug discovered before users adopt version
- Security issue that requires removal (exceptional)

**Rollback safety:**
- Script requires interactive confirmation ("yes")
- Validates environment before proceeding
- Creates revert commit for version changes
- Provides clear rollback report

**Testing rollback:**
- Test on dummy tags/commits in development branch
- Verify tag deletion works locally and remotely
- Verify revert commit is created correctly
- Verify rollback report is accurate

**Recovery after rollback:**
1. Fix the issue that caused rollback
2. Prepare new release (increment version)
3. Run pre-release validation (DTR-016)
4. Perform new release
5. Verify new release (DTR-017)
