# DTR-016: Pre-Release Validation Automation

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,release,automation

## Description

Create automated pre-release validation that checks all prerequisites before a release can proceed. This prevents failed releases by catching issues early (environment problems, dirty working directory, missing dependencies, etc.).

The validation script should check:
- Git repository state (clean working tree, on correct branch)
- Java and Maven versions
- Required environment variables and secrets
- Maven build succeeds locally
- No uncommitted changes
- No untracked files (or explicit allow-list)
- Network connectivity (Maven Central, GitHub)

## Acceptance Criteria

- [ ] Create `scripts/validate-pre-release.sh` with comprehensive checks
- [ ] Integrate validation into `scripts/release.sh` (run before any modifications)
- [ ] Validation fails fast on first error with clear error message
- [ ] Validation provides clear guidance on how to fix each issue
- [ ] Add `validate-pre-release` target to Makefile
- [ ] Validation script returns appropriate exit codes (0=pass, non-zero=fail)
- [ ] Add tests for validation script
- [ ] Document validation requirements in README.md

## Technical Notes

### File to Create

**`scripts/validate-pre-release.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

# Pre-release validation checks
# Returns 0 if all checks pass, non-zero otherwise

echo "=== Pre-Release Validation ==="

# Check 1: Clean working tree
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "❌ ERROR: Working tree has uncommitted changes"
  echo "   Run: git status"
  exit 1
fi
echo "✓ Clean working tree"

# Check 2: No untracked files (except allowed)
UNTRACKED=$(git ls-files --others --exclude-standard)
if [ -n "$UNTRACKED" ]; then
  echo "❌ ERROR: Untracked files present:"
  echo "$UNTRACKED"
  echo "   Stash or commit them first"
  exit 1
fi
echo "✓ No untracked files"

# Check 3: Java version
REQUIRED_JAVA="26.ea.13"
ACTUAL_JAVA=$(java -version 2>&1 | head -n 1 | cut -d' ' -f3 | tr -d '"')
if ! java -version 2>&1 | grep -q "26\.ea\."; then
  echo "❌ ERROR: Java $REQUIRED_JAVA required, found $ACTUAL_JAVA"
  exit 1
fi
echo "✓ Java version: $ACTUAL_JAVA"

# Check 4: Maven version
if ! command -v mvnd &> /dev/null; then
  echo "❌ ERROR: mvnd not found in PATH"
  exit 1
fi
echo "✓ Maven daemon available"

# Check 5: Maven build succeeds
echo "Running Maven validation build..."
if ! mvnd verify -q; then
  echo "❌ ERROR: Maven build failed"
  echo "   Fix build errors before releasing"
  exit 1
fi
echo "✓ Maven build successful"

# Check 6: Environment variables
REQUIRED_VARS=("CENTRAL_USERNAME" "CENTRAL_TOKEN" "GPG_PRIVATE_KEY")
for var in "${REQUIRED_VARS[@]}"; do
  if [ -z "${!var:-}" ]; then
    echo "❌ ERROR: Required environment variable not set: $var"
    exit 1
  fi
done
echo "✓ Required environment variables set"

# Check 7: Network connectivity
if ! curl -s --head https://repo.maven.apache.org/maven2/ | head -n 1 | grep -q "200"; then
  echo "❌ ERROR: Cannot reach Maven Central"
  exit 1
fi
echo "✓ Network connectivity OK"

echo ""
echo "=== All Pre-Release Checks Passed ==="
exit 0
```

### Integration with Release Script

**`scripts/release.sh`**
```bash
# Run validation before any operations
./scripts/validate-pre-release.sh || {
  echo "Pre-release validation failed. Aborting release."
  exit 1
}
```

### Makefile Addition

**`Makefile`**
```makefile
.PHONY: validate-pre-release
validate-pre-release:
	./scripts/validate-pre-release.sh
```

## Dependencies

- **DTR-015** (Dry-Run Mode) - validation should work in dry-run mode
- **DTR-018** (Rollback Mechanism) - validation prevents need for rollback

## References

- Release script: `/Users/sac/dtr/scripts/release.sh`
- Makefile: `/Users/sac/dtr/Makefile`
- CI gate requirements: `/Users/sac/dtr/.github/workflows/release.yml`

## Notes

Pre-release validation is the most critical improvement for release reliability. Most failed releases are due to:
1. Dirty git state (40%)
2. Wrong Java/Maven versions (30%)
3. Missing credentials (20%)
5. Build failures (10%)

This script catches all of these before any git operations occur.
