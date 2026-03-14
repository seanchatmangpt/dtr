#!/bin/bash
set -euo pipefail

VERSION=$(scripts/current-version.sh)

# Stage all pom.xml files updated by bump-version.sh
git add $(find . -name 'pom.xml' -not -path '*/target/*')
git commit -m "chore: bump to v${VERSION}"
git tag -a "v${VERSION}" -m "Release v${VERSION}"
git push origin HEAD "v${VERSION}"

echo "Released v${VERSION} — pipeline is running."
