#!/bin/bash
# Tag and push a final release. Triggers GitHub Actions publish to Maven Central.
# Run after scripts/bump.sh has updated pom.xml and written .release-version.
set -euo pipefail

# Read version — prefer .release-version written by bump.sh
if [ -f .release-version ]; then
  VERSION=$(cat .release-version)
  rm -f .release-version
else
  VERSION=$(scripts/current-version.sh)
fi

BRANCH=$(git branch --show-current)

echo "==> Releasing v${VERSION} from ${BRANCH}" >&2

# Generate changelog and per-release docs before the commit
scripts/changelog.sh "$VERSION"

# Stage: pom.xml changes (from bump.sh) + generated docs
git add -u
git add docs/CHANGELOG.md "docs/releases/${VERSION}.md" 2>/dev/null || true

git commit -m "chore: release v${VERSION}"
git tag -a "v${VERSION}" -m "Release v${VERSION}"

# Push branch and tag in one command — one network round trip, one trigger
git push origin "${BRANCH}" "v${VERSION}"

echo "" >&2
echo "==> v${VERSION} tagged and pushed." >&2
echo "==> GitHub Actions: verify → sign → publish to Maven Central" >&2
echo "==> Track at: https://github.com/seanchatmangpt/dtr/actions" >&2
