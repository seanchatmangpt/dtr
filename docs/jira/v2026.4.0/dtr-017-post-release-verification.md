# DTR-017: Post-Release Verification Automation

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,release,automation

## Description

Create automated post-release verification that confirms the release was successful and the artifact is available on Maven Central. This provides immediate feedback on release success and allows quick detection of deployment issues.

The verification script should:
- Confirm git tag was pushed successfully
- Verify artifact exists on Maven Central
- Check artifact metadata (pom.xml, signatures, javadoc)
- Validate Maven Central search indexing
- Test artifact can be consumed (minimal dependency test)
- Provide clear success/failure report

## Acceptance Criteria

- [ ] Create `scripts/verify-release.sh` with comprehensive checks
- [ ] Verification waits for Maven Central sync (with timeout)
- [ ] Verification confirms all artifact components (jar, pom, javadoc, sources, signatures)
- [ ] Verification returns appropriate exit codes
- [ ] Add `verify-release` target to Makefile
- [ ] Add tests for verification script
- [ ] Document verification process in README.md
- [ ] Integration with CI to auto-verify after release

## Technical Notes

### File to Create

**`scripts/verify-release.sh`**
```bash
#!/usr/bin/env bash
set -euo pipefail

VERSION=${1:-}
if [ -z "$VERSION" ]; then
  echo "Usage: $0 <version>"
  echo "Example: $0 2026.4.0"
  exit 1
fi

GROUP_ID="io.github.dtr_project"
ARTIFACT_ID="dtr-core"

echo "=== Verifying Release: $VERSION ==="

# Check 1: Tag exists on remote
echo "Checking git tag..."
if ! git ls-remote --tags origin | grep -q "refs/tags/v$VERSION"; then
  echo "❌ ERROR: Tag v$VERSION not found on remote"
  exit 1
fi
echo "✓ Tag v$VERSION exists on remote"

# Check 2: Maven Central metadata
echo "Checking Maven Central metadata..."
METADATA_URL="https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/maven-metadata.xml"
if ! curl -s -f "$METADATA_URL" | grep -q "$VERSION"; then
  echo "❌ ERROR: Version $VERSION not in Maven Central metadata"
  echo "   URL: $METADATA_URL"
  exit 1
fi
echo "✓ Version $VERSION in Maven Central metadata"

# Check 3: Main artifact exists
MAIN_JAR="https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.jar"
if ! curl -s -f -o /dev/null "$MAIN_JAR"; then
  echo "❌ ERROR: Main artifact not found"
  echo "   URL: $MAIN_JAR"
  exit 1
fi
echo "✓ Main artifact exists"

# Check 4: POM file exists
POM_FILE="https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.pom"
if ! curl -s -f -o /dev/null "$POM_FILE"; then
  echo "❌ ERROR: POM file not found"
  echo "   URL: $POM_FILE"
  exit 1
fi
echo "✓ POM file exists"

# Check 5: Javadoc exists
JAVADOC_JAR="https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION-javadoc.jar"
if ! curl -s -f -o /dev/null "$JAVADOC_JAR"; then
  echo "❌ ERROR: Javadoc JAR not found"
  echo "   URL: $JAVADOC_JAR"
  exit 1
fi
echo "✓ Javadoc JAR exists"

# Check 6: Sources exists
SOURCES_JAR="https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION-sources.jar"
if ! curl -s -f -o /dev/null "$SOURCES_JAR"; then
  echo "❌ ERROR: Sources JAR not found"
  echo "   URL: $SOURCES_JAR"
  exit 1
fi
echo "✓ Sources JAR exists"

# Check 7: Signature files exist
SIGNATURE_FILE="https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.jar.asc"
if ! curl -s -f -o /dev/null "$SIGNATURE_FILE"; then
  echo "❌ ERROR: Signature file not found"
  echo "   URL: $SIGNATURE_FILE"
  exit 1
fi
echo "✓ Signature files exist"

# Check 8: Maven Central search indexing (with retry)
echo "Checking Maven Central search indexing..."
MAX_RETRIES=10
RETRY_DELAY=30
for i in $(seq 1 $MAX_RETRIES); do
  SEARCH_URL="https://search.maven.org/solrsearch/select?q=g:$GROUP_ID+AND+a:$ARTIFACT_ID+AND+v:$VERSION&rows=1&wt=json"
  if curl -s "$SEARCH_URL" | grep -q "\"numFound\":1"; then
    echo "✓ Artifact indexed in Maven Central search"
    break
  fi

  if [ $i -eq $MAX_RETRIES ]; then
    echo "⚠ WARNING: Artifact not yet indexed in search (may take up to 10 minutes)"
    echo "   Verify manually at: https://search.maven.org/artifact/$GROUP_ID/$ARTIFACT_ID/$VERSION"
    break
  fi

  echo "   Search not yet updated, retrying in ${RETRY_DELAY}s... ($i/$MAX_RETRIES)"
  sleep $RETRY_DELAY
done

# Check 9: Minimal dependency test
echo "Testing artifact can be consumed..."
TEST_DIR=$(mktemp -d)
cd "$TEST_DIR"
cat > pom.xml <<EOF
<project>
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>test</artifactId>
  <version>1.0</version>
  <dependencies>
    <dependency>
      <groupId>$GROUP_ID</groupId>
      <artifactId>$ARTIFACT_ID</artifactId>
      <version>$VERSION</version>
    </dependency>
  </dependencies>
</project>
EOF

if ! mvn dependency:resolve -q > /dev/null 2>&1; then
  echo "❌ ERROR: Artifact cannot be consumed as dependency"
  cd -
  rm -rf "$TEST_DIR"
  exit 1
fi
cd -
rm -rf "$TEST_DIR"
echo "✓ Artifact can be consumed as dependency"

echo ""
echo "=== Release Verification Successful ==="
echo "Artifact: $GROUP_ID:$ARTIFACT_ID:$VERSION"
echo "Maven Central: https://repo.maven.apache.org/maven2/$GROUP_ID/$ARTIFACT_ID/$VERSION/"
echo "Search: https://search.maven.org/artifact/$GROUP_ID/$ARTIFACT_ID/$VERSION"
exit 0
```

### Makefile Addition

**`Makefile`**
```makefile
.PHONY: verify-release
verify-release:
	./scripts/verify-release.sh $(VERSION)
```

### Integration with Release Script

**`scripts/release.sh`**
```bash
# After successful release, run verification
./scripts/verify-release.sh $VERSION || {
  echo "WARNING: Release verification failed. Manual intervention required."
  echo "Tag pushed, but artifact may not be on Maven Central yet."
  exit 1
}
```

## Dependencies

- **DTR-015** (Dry-Run Mode) - verification can be tested with dry-run
- **DTR-016** (Pre-Release Validation) - reduces chance of verification failures
- **DTR-018** (Rollback Mechanism) - if verification fails, rollback may be needed

## References

- Release script: `/Users/sac/dtr/scripts/release.sh`
- Makefile: `/Users/sac/dtr/Makefile`
- Maven Central repository: https://repo.maven.apache.org/maven2/
- Maven Central search: https://search.maven.org/

## Notes

Maven Central synchronization typically takes:
- 1-2 minutes for initial metadata
- 5-10 minutes for search indexing
- Up to 30 minutes for all mirrors

The script uses retries with 30-second delay for search indexing, which is the most common sync delay.

Common verification failures:
1. Tag not pushed (git issue)
2. Maven Central sync delay (wait 10-15 minutes)
3. Signature files missing (GPG issue)
4. Metadata incomplete (POM issue)
