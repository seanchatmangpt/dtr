#!/bin/bash
# CalVer bump: YYYY.MINOR.PATCH
# Usage: bump.sh [--dry-run] <minor|patch|year> [rc]
#
# CalVer scheme: YYYY.MINOR.PATCH
#   minor — new features, additive changes; resets PATCH to 0; resets MINOR on year boundary
#   patch — bug fixes within a MINOR; increments PATCH
#   year  — explicit year boundary (January trigger); sets YYYY.1.0
#
# RC flag: appends -rc.N where N = count of existing rc tags for this base version + 1
# Promote RC to final: call bump.sh without rc flag; strips -rc suffix, no arithmetic.
set -euo pipefail

DRY_RUN=false
BUMP=""
RC=""

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        minor|patch|year)
            BUMP="$1"
            shift
            ;;
        rc)
            RC="$1"
            shift
            ;;
        *)
            echo "Unknown option: $1" >&2
            echo "Usage: bump.sh [--dry-run] <minor|patch|year> [rc]" >&2
            exit 1
            ;;
    esac
done

if [ -z "$BUMP" ]; then
  echo "error: missing bump type" >&2
  echo "Usage: bump.sh [--dry-run] <minor|patch|year> [rc]" >&2
  exit 1
fi

run_cmd() {
    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] Would execute: $*" >&2
    else
        echo "[EXEC] $*" >&2
        "$@"
    fi
}

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

echo "" >&2
if [ "$DRY_RUN" = true ]; then
    echo "==> DRY-RUN MODE: Would bump version from ${CURRENT} → ${NEXT}" >&2
else
    echo "==> Bumping version: ${CURRENT} → ${NEXT}" >&2
fi
echo "" >&2

run_cmd scripts/set-version.sh "$NEXT" "$BASE"

# Write to temp file for release.sh / release-rc.sh to consume
if [ "$DRY_RUN" = true ]; then
    echo "[DRY-RUN] Would write version to .release-version: $NEXT" >&2
else
    echo "$NEXT" > .release-version
fi

echo "" >&2
if [ "$DRY_RUN" = true ]; then
    echo "==> DRY-RUN COMPLETE: No changes were made" >&2
    echo "==> To execute for real, run: bump.sh $BUMP $RC" >&2
else
    echo "==> Version bumped to ${NEXT}" >&2
    echo "==> Next step: run scripts/release.sh or scripts/release-rc.sh" >&2
fi

echo "$NEXT"
