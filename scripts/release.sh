#!/bin/bash
# Tag and push the current version. Triggers GitHub Actions publish to Maven Central.
# Run after scripts/bump-version.sh has updated pom.xml.
set -euo pipefail

VERSION="$(scripts/current-version.sh)"
BRANCH="$(git branch --show-current)"

echo "==> Releasing v${VERSION} from branch ${BRANCH}" >&2

# Stage all pom.xml changes (root + all modules)
git add -u

git commit -m "chore: release v${VERSION}"
git tag -a "v${VERSION}" -m "Release v${VERSION}"

# Push branch and tag together — one network round trip, one trigger
git push origin "${BRANCH}" "v${VERSION}"

echo "" >&2
echo "==> v${VERSION} tagged and pushed." >&2
echo "==> GitHub Actions will sign, package, and publish to Maven Central." >&2
echo "==> Track at: https://github.com/seanchatmangpt/dtr/actions" >&2
