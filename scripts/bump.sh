#!/bin/bash
# CalVer bump: YYYY.MINOR.PATCH
# Usage: bump.sh <minor|patch|year> [rc]
#
# CalVer scheme: YYYY.MINOR.PATCH
#   minor — new features, additive changes; resets PATCH to 0; resets MINOR on year boundary
#   patch — bug fixes within a MINOR; increments PATCH
#   year  — explicit year boundary (January trigger); sets YYYY.1.0
#
# RC flag: appends -rc.N where N = count of existing rc tags for this base version + 1
# Promote RC to final: call bump.sh without rc flag; strips -rc suffix, no arithmetic.
set -euo pipefail

BUMP="${1:?usage: bump.sh <minor|patch|year> [rc]}"
RC="${2:-}"
THIS_YEAR=$(date +%Y)

CURRENT=$(scripts/current-version.sh)

# Extract base version (strip any -rc.N suffix)
if [[ "$CURRENT" == *"-rc."* ]]; then
  BASE=$(echo "$CURRENT" | sed 's/-rc\.[0-9]*//')
else
  BASE="$CURRENT"
fi

IFS='.' read -r YEAR MINOR PATCH <<< "$BASE"

# If currently on an RC and no rc flag → promote to final (no arithmetic)
if [[ "$CURRENT" == *"-rc."* && -z "$RC" ]]; then
  NEXT="$BASE"
else
  case "$BUMP" in
    minor)
      if [ "$YEAR" != "$THIS_YEAR" ]; then
        NEXT="${THIS_YEAR}.1.0"    # year boundary: reset MINOR to 1
      else
        NEXT="${YEAR}.$((MINOR + 1)).0"
      fi
      ;;
    patch)
      NEXT="${YEAR}.${MINOR}.$((PATCH + 1))"
      ;;
    year)
      NEXT="${THIS_YEAR}.1.0"
      ;;
    *)
      echo "error: bump must be minor, patch, or year (got: $BUMP)" >&2
      exit 1
      ;;
  esac
fi

# Append RC qualifier if requested
if [ -n "$RC" ]; then
  RC_COUNT=$(git tag --list "v${NEXT}-rc.*" 2>/dev/null | wc -l | tr -d ' ')
  NEXT="${NEXT}-rc.$((RC_COUNT + 1))"
fi

echo "==> ${CURRENT} → ${NEXT}" >&2
scripts/set-version.sh "$NEXT" "$BASE"

# Write to temp file for release.sh / release-rc.sh to consume
echo "$NEXT" > .release-version
echo "$NEXT"
