#!/bin/bash
# Generate per-release notes and prepend to docs/CHANGELOG.md.
# Usage: scripts/changelog.sh <version>
# Writes:
#   docs/releases/<version>.md    — release notes for this version
#   docs/CHANGELOG.md             — prepends this release to the full log
set -euo pipefail

VERSION="${1:?usage: changelog.sh <version>}"
DATE=$(date +%Y-%m-%d)
RELEASE_DIR="docs/releases"
RELEASE_FILE="${RELEASE_DIR}/${VERSION}.md"

mkdir -p "$RELEASE_DIR"

# Find the previous final tag (not RC) to bound the git log
PREV_TAG=$(git tag --sort=-version:refname \
    | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' \
    | head -1 2>/dev/null || true)

if [ -z "$PREV_TAG" ]; then
  LOG_CMD="git log HEAD --pretty=format:\"- %s\" --no-merges | head -30"
  LOG_HEADER="Initial release"
  COMMITS=$(git log HEAD --pretty=format:"- %s" --no-merges \
    | grep -v '^- chore: release' | head -30 || true)
else
  LOG_HEADER="Changes since ${PREV_TAG}"
  COMMITS=$(git log "${PREV_TAG}..HEAD" --pretty=format:"- %s" --no-merges \
    | grep -v '^- chore: release' || true)
fi

[ -z "$COMMITS" ] && COMMITS="- No notable changes"

# Write per-release notes
cat > "$RELEASE_FILE" << EOF
## v${VERSION} — ${DATE}

${COMMITS}

### Install

\`\`\`xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>${VERSION}</version>
</dependency>
\`\`\`

Year-bounded range (recommended for libraries): \`[${VERSION%.*.*}.1.0,$(( ${VERSION%%.*} + 1)))\`
EOF

# Prepend to CHANGELOG.md
if [ -f docs/CHANGELOG.md ]; then
  { cat "$RELEASE_FILE"; echo ""; cat docs/CHANGELOG.md; } > docs/CHANGELOG.md.tmp
  mv docs/CHANGELOG.md.tmp docs/CHANGELOG.md
else
  cat > docs/CHANGELOG.md << EOF
# DTR Changelog

All releases follow CalVer **YYYY.MINOR.PATCH**.
See [docs/contributing/releasing.md](contributing/releasing.md) for the release process.

DTR uses Calendar Versioning (YYYY.MINOR.PATCH). The year component resets
the minor counter — 2026.7.0 to 2027.1.0 is not a breaking change. Breaking
changes are signaled by @Deprecated annotations with a minimum one-year removal
window. Use year-bounded Maven ranges: \`[2026.1.0,2027)\`.

---

$(cat "$RELEASE_FILE")
EOF
fi

echo "==> docs/releases/${VERSION}.md written" >&2
echo "==> docs/CHANGELOG.md updated" >&2
