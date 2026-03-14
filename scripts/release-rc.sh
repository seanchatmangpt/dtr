#!/bin/bash
# Release candidate: commit pom changes, create rc tag, push.
# Reads version from .release-version (written by scripts/bump.sh).
# Triggers RC GitHub Actions → mvnd verify → GitHub Packages deploy.
# Does NOT publish to Maven Central.
set -euo pipefail

VERSION=$(cat .release-version)

# Stage pom.xml files updated by bump.sh
POMS=$(find . -name 'pom.xml' -not -path '*/target/*')
git add $POMS

git commit -m "chore: release candidate v${VERSION}"
git tag -a "v${VERSION}" -m "Release candidate v${VERSION}"
git push origin HEAD "v${VERSION}"

rm -f .release-version
echo "Released v${VERSION} — RC pipeline is running (GitHub Packages only)."
