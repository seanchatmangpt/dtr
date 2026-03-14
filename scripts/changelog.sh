#!/bin/bash
# Generate per-release notes and prepend to docs/CHANGELOG.md.
# Usage: scripts/changelog.sh <version>
set -euo pipefail

VERSION=$1
DATE=$(date +%Y-%m-%d)
RELEASE_FILE="docs/releases/${VERSION}.md"

mkdir -p docs/releases

# Find previous tag for git log range
PREV_TAG=$(git describe --tags --abbrev=0 HEAD^ 2>/dev/null || echo "")

{
  echo "## v${VERSION} — ${DATE}"
  echo ""
  if [ -n "$PREV_TAG" ]; then
    git log "${PREV_TAG}..HEAD" --pretty=format:"- %s" --no-merges
  else
    git log HEAD --pretty=format:"- %s" --no-merges | head -30
  fi
  echo ""
} > "$RELEASE_FILE"

# Prepend to CHANGELOG.md
if [ -f docs/CHANGELOG.md ]; then
  { cat "$RELEASE_FILE"; echo ""; cat docs/CHANGELOG.md; } > docs/CHANGELOG.md.tmp
  mv docs/CHANGELOG.md.tmp docs/CHANGELOG.md
else
  cp "$RELEASE_FILE" docs/CHANGELOG.md
fi
