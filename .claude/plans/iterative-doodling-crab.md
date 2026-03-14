# Java 26 GitHub Actions & Act Compatibility Plan

## Context

The DTR project needs all GitHub Actions workflows to run locally using `/opt/homebrew/bin/act` (version 0.2.84) while maintaining Java 26 compatibility. The project's `pom.xml` is already configured for Java 26, but the workflows were downgraded to Java 25 for act compatibility. This plan aims to restore Java 26 support for both GitHub Actions and local testing with act.

## Current State

- **Project**: Already configured for Java 26 (`pom.xml` line 70: `<maven.compiler.release>26</maven.compiler.release>`)
- **Workflows**: Currently using Java 25 (downgraded from Java 26)
- **Act**: Using .actrc with `JAVA_VERSION=25`
- **Maven**: Uses mvnd 2.0.0+ with Maven 4.0.0-rc-3

## Implementation Plan

### Phase 1: Restore Java 26 in GitHub Actions (Priority: HIGH)

Update all workflow files to use Java 26:

**Files to Update:**
- `.github/workflows/quality-gates.yml` - Lines 34, 50, 106, 169, 235
- `.github/workflows/ci-gate.yml` - Lines 13, 34, 78, 112, 162, 184, 193
- `.github/workflows/publish.yml` - Lines 29, 37, 46, matrix strategy
- `.github/workflows/publish-validation.yml` - Lines 23, 119, 199, 294, 299
- `.github/workflows/test-deploy.yml` - Lines 28, 46, 111, 115, 148, 155, 199, 202, 246, 250, 275, 280, 311, 315
- `.github/workflows/deployment-automation.yml` - Lines 41, 145, 189
- `.github/workflows/update-publish.yml` - Lines 42, 70, 99, 125, 184

**Changes Required:**
```yaml
# Change from:
env:
  JAVA_VERSION: '25'
  MAVEN_OPTS: -Dmaven.compiler.release=25

# To:
env:
  JAVA_VERSION: '26'
  MAVEN_OPTS: -Dmaven.compiler.release=26
```

**Matrix Updates:**
```yaml
# Change matrix from:
matrix:
  java-version: ['21', '22', '25']

# To:
matrix:
  java-version: ['21', '22', '26']
```

### Phase 2: Update .actrc Configuration (Priority: HIGH)

Update the `.actrc` file to support Java 26:

**File: `.actrc`**
```bash
-P ubuntu-latest=catthehacker/ubuntu:act-latest
--container-architecture linux/amd64
--secret GITHUB_TOKEN=
--env JAVA_VERSION=26  # Changed from 25 to 26
-W .github/workflows/
```

### Phase 3: Verify Java 26 Support (Priority: HIGH)

Confirm that:
1. `actions/setup-java@v4` supports Java 26
2. Docker images have Java 26 available
3. All Maven plugins are compatible with Java 26

### Phase 4: Testing Strategy (Priority: MEDIUM)

**Local Testing Commands:**
```bash
# Dry run test
act -n --container-architecture linux/amd64

# Test CI gate workflow
act -W .github/workflows/ci-gate.yml push

# Test specific job with Java 26
act -W .github/workflows/ci-gate.yml -j build-verification

# Test matrix with different Java versions
act -W .github/workflows/publish.yml -j build-java21
act -W .github/workflows/publish.yml -j build-java22
act -W .github/workflows/publish.yml -j build-java26
```

### Phase 5: Final Verification (Priority: HIGH)

**Production Testing:**
- Verify workflows still run on GitHub Actions
- Confirm Java 26 is available in GitHub runners
- Test build and deployment processes

**Local Testing:**
- Verify act can download and use Java 26
- Test all workflow features locally
- Performance comparison between GitHub Actions and act

## Critical Files

1. **`.github/workflows/*.yml`** - All workflow files need Java 26 restored
2. **`.actrc`** - Must be updated to use Java 26
3. **`pom.xml`** - Already configured for Java 26

## Success Criteria

- ✅ All workflows use Java 26
- ✅ Act runs workflows successfully with Java 26
- ✅ No bash syntax errors in any workflow
- ✅ Java 26 setup works in both GitHub Actions and act
- ✅ Multi-Java testing includes Java 26
- ✅ Production builds use Java 26

## Notes

- The project already uses Java 26 locally (pom.xml)
- Act requires proper Docker images that support Java 26
- actions/setup-java@v4 supports Java 26
- The change maintains compatibility with both GitHub Actions and local testing