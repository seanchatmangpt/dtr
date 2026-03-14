#!/bin/bash
# CalVer bump: YYYY.MINOR.PATCH
# Usage: bump.sh <minor|patch|year> [rc]
#
# Rules:
#   minor — increment MINOR, reset PATCH to 0.
#           If calendar year has advanced since current version, reset to YYYY.1.0.
#           If current version is an RC (-rc.N), promote to final (strip suffix).
#   patch — increment PATCH within current MINOR.
#           If current version is an RC (-rc.N), promote to final (strip suffix).
#   year  — explicit year boundary: force YYYY.1.0 for current calendar year.
#           Used when January 1 has passed but no minor bump has been done yet.
#   [rc]  — optional second argument. Tags as -rc.N where N is computed by
#           counting existing rc tags for the target version.
#
# Writes the new version to .release-version for release.sh to consume.
set -euo pipefail

BUMP="${1:?usage: bump.sh <minor|patch|year> [rc]}"
RC_FLAG="${2:-}"

CURRENT=$(scripts/current-version.sh)
# Strip any existing -rc.N suffix to get arithmetic base
BASE=$(echo "$CURRENT" | sed 's/-rc\.[0-9]*//')
YEAR=$(echo "$BASE"  | cut -d. -f1)
MINOR=$(echo "$BASE" | cut -d. -f2)
PATCH=$(echo "$BASE" | cut -d. -f3)
NOW_YEAR=$(date +%Y)

case "$BUMP" in
  minor)
    if echo "$CURRENT" | grep -q '\-rc\.'; then
      # Promoting an RC to final — strip the suffix, do not bump
      NEXT="$BASE"
    elif [ "$NOW_YEAR" != "$YEAR" ]; then
      # Year boundary: calendar year has advanced, reset to YYYY.1.0
      NEXT="${NOW_YEAR}.1.0"
    else
      NEXT="${YEAR}.$((MINOR + 1)).0"
    fi
    ;;
  patch)
    if echo "$CURRENT" | grep -q '\-rc\.'; then
      # Promoting an RC to final — strip the suffix, do not bump
      NEXT="$BASE"
    else
      NEXT="${YEAR}.${MINOR}.$((PATCH + 1))"
    fi
    ;;
  year)
    NEXT="${NOW_YEAR}.1.0"
    ;;
  *)
    echo "error: bump must be minor, patch, or year (got: $BUMP)" >&2
    exit 1
    ;;
esac

# Append RC qualifier if requested
if [ -n "$RC_FLAG" ]; then
  RC_N=$(git tag --list "v${NEXT}-rc.*" 2>/dev/null | wc -l | tr -d ' ')
  NEXT_RC_N=$((RC_N + 1))
  NEXT="${NEXT}-rc.${NEXT_RC_N}"
fi

echo "==> ${CURRENT} → ${NEXT}" >&2
scripts/set-version.sh "$NEXT" "$BASE"

# Write to temp file for release.sh / release-rc.sh to consume
echo "$NEXT" > .release-version
echo "$NEXT"
