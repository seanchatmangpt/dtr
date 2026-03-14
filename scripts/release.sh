#!/bin/bash
# Final release: commit pom changes + docs, create annotated tag, push.
# Reads version from .release-version (written by scripts/bump.sh).
# Triggers GitHub Actions → mvnd verify → Maven Central deploy → gh release create.
set -euo pipefail

VERSION=$(cat .release-version)

# Generate changelog before committing
scripts/changelog.sh "$VERSION"

# Stage all pom.xml files updated by bump.sh + generated docs
POMS=$(find . -name 'pom.xml' -not -path '*/target/*')
git add $POMS docs/CHANGELOG.md "docs/releases/${VERSION}.md"

git commit -m "chore: release v${VERSION}"
git tag -a "v${VERSION}" -m "Release v${VERSION}"
git push origin HEAD "v${VERSION}"

rm -f .release-version
echo "Released v${VERSION} — pipeline is running."
