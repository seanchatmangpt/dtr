#!/bin/bash
# Compute and set the next CalVer version. Writes result to .release-version.
# Usage: scripts/bump.sh [minor|patch|year] [rc]
#
# CalVer scheme: YYYY.MINOR.PATCH
#   minor — new features, additive changes; resets PATCH to 0; resets MINOR on year boundary
#   patch — bug fixes within a MINOR; increments PATCH
#   year  — explicit year boundary (January trigger); sets YYYY.1.0
#
# RC flag: appends -rc.N where N = count of existing rc tags for this base version + 1
# Promote RC to final: call bump.sh without rc flag; strips -rc suffix, no bump.
set -euo pipefail

BUMP=${1:?Usage: scripts/bump.sh [minor|patch|year] [rc]}
RC=${2:-}
THIS_YEAR=$(date +%Y)

CURRENT=$(scripts/current-version.sh)

# If current version is a release candidate, extract the base version
if [[ "$CURRENT" == *"-rc."* ]]; then
  BASE=$(echo "$CURRENT" | sed 's/-rc\.[0-9]*//')
else
  BASE="$CURRENT"
fi

IFS='.' read -r YEAR MINOR PATCH <<< "$BASE"

if [[ "$CURRENT" == *"-rc."* && -z "$RC" ]]; then
  # Promote RC to final: strip -rc.N, no arithmetic
  NEXT="$BASE"
else
  case $BUMP in
    minor)
      if [ "$YEAR" != "$THIS_YEAR" ]; then
        NEXT="${THIS_YEAR}.1.0"    # year boundary: reset
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
      echo "Usage: scripts/bump.sh [minor|patch|year] [rc]" >&2
      exit 1
      ;;
  esac
fi

if [ -n "$RC" ]; then
  # Compute N = existing rc tags for this base version + 1
  RC_COUNT=$(git tag -l "v${NEXT}-rc.*" 2>/dev/null | wc -l | tr -d ' ')
  NEXT="${NEXT}-rc.$((RC_COUNT + 1))"
fi

scripts/set-version.sh "$NEXT"
echo "$NEXT" > .release-version
echo "$NEXT"
