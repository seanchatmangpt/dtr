#!/bin/bash
# Generate CHANGELOG.md and per-release notes from git log.
# Usage: changelog.sh <version>
# Writes:
#   docs/releases/<version>.md    — release notes for this version
#   docs/CHANGELOG.md             — prepends this release to the full log
set -euo pipefail

VERSION="${1:?usage: changelog.sh <version>}"
RELEASE_DATE=$(date +%Y-%m-%d)
RELEASE_DIR="docs/releases"
RELEASE_FILE="${RELEASE_DIR}/${VERSION}.md"
CHANGELOG="docs/CHANGELOG.md"

mkdir -p "$RELEASE_DIR"

# Find the previous tag to bound the git log
PREV_TAG=$(git tag --sort=-version:refname \
    | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' \
    | head -1 2>/dev/null || true)

if [ -z "$PREV_TAG" ]; then
  LOG_RANGE="HEAD"
  LOG_HEADER="Initial release"
else
  LOG_RANGE="${PREV_TAG}..HEAD"
  LOG_HEADER="Changes since ${PREV_TAG}"
fi

# Collect commits, excluding chore: release commits
COMMITS=$(git log --oneline --no-merges \
    --pretty=format:"- %s ([%h](../../commit/%H))" \
    $LOG_RANGE \
    | grep -v '^- chore: release' || true)

if [ -z "$COMMITS" ]; then
  COMMITS="- No notable changes"
fi

# Write per-release notes
cat > "$RELEASE_FILE" << EOF
# DTR ${VERSION}

**Released:** ${RELEASE_DATE}

## ${LOG_HEADER}

${COMMITS}

## Install

\`\`\`xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>${VERSION}</version>
</dependency>
\`\`\`
EOF

# Prepend to CHANGELOG.md
ENTRY="## [${VERSION}](releases/${VERSION}.md) — ${RELEASE_DATE}

${COMMITS}

"

if [ -f "$CHANGELOG" ]; then
  EXISTING=$(cat "$CHANGELOG")
  # Replace or prepend after the header
  if grep -q '^# Changelog' "$CHANGELOG"; then
    HEADER=$(head -2 "$CHANGELOG")
    REST=$(tail -n +3 "$CHANGELOG")
    printf '%s\n\n%s\n%s' "$HEADER" "$ENTRY" "$REST" > "$CHANGELOG"
  else
    printf '%s\n%s' "$ENTRY" "$EXISTING" > "$CHANGELOG"
  fi
else
  cat > "$CHANGELOG" << EOF
# Changelog

${ENTRY}
EOF
fi

echo "==> docs/releases/${VERSION}.md written" >&2
echo "==> docs/CHANGELOG.md updated" >&2
