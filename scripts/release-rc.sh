#!/bin/bash
# Tag and push a release candidate. Triggers GitHub Actions publish to GitHub Packages.
# RC builds do NOT publish to Maven Central.
# Run after scripts/bump.sh <type> rc has updated pom.xml and written .release-version.
set -euo pipefail

if [ -f .release-version ]; then
  VERSION=$(cat .release-version)
  rm -f .release-version
else
  VERSION=$(scripts/current-version.sh)
fi

# Verify this is actually an RC version
if ! echo "$VERSION" | grep -q '\-rc\.'; then
  echo "error: expected an RC version (e.g. 2026.3.0-rc.1), got: $VERSION" >&2
  echo "       use scripts/release.sh for final releases" >&2
  exit 1
fi

BRANCH=$(git branch --show-current)
FINAL_VERSION=$(echo "$VERSION" | sed 's/-rc\.[0-9]*//')

echo "==> Release candidate v${VERSION} from ${BRANCH}" >&2

# Stage pom.xml changes from bump.sh
git add -u

git commit -m "chore: release-candidate v${VERSION}"
git tag -a "v${VERSION}" -m "Release candidate v${VERSION}"

# Push branch and RC tag — GitHub Actions routes to GitHub Packages
git push origin "${BRANCH}" "v${VERSION}"

echo "" >&2
echo "==> v${VERSION} tagged and pushed." >&2
echo "==> GitHub Actions: verify → publish to GitHub Packages (not Maven Central)" >&2
echo "==> To promote to final: make release-minor (or release-patch)" >&2
echo "==> Final version will be: ${FINAL_VERSION}" >&2
