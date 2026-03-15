#!/bin/bash
# Release candidate: commit pom changes, create rc tag, push.
# Reads version from .release-version (written by scripts/bump.sh).
# Triggers RC GitHub Actions → mvnd verify → GitHub Packages deploy.
# Does NOT publish to Maven Central.
#
# Usage: release-rc.sh [--dry-run]
#   --dry-run: Show what would be done without executing git operations
set -euo pipefail

DRY_RUN=false

# Parse command-line arguments
while [[ $# -gt 0 ]]; do
  case "$1" in
    --dry-run)
      DRY_RUN=true
      shift
      ;;
    *)
      echo "error: unknown option: $1" >&2
      exit 1
      ;;
  esac
done

VERSION=$(cat .release-version)

# Verify this is actually an RC version
if [[ "$VERSION" != *"-rc."* ]]; then
  echo "error: expected an RC version (e.g. 2026.3.0-rc.1), got: $VERSION" >&2
  echo "       use scripts/release.sh for final releases" >&2
  exit 1
fi

FINAL_VERSION=$(echo "$VERSION" | sed 's/-rc\.[0-9]*//')

# Stage pom.xml files updated by bump.sh
POMS=$(find . -name 'pom.xml' -not -path '*/target/*')

if [ "$DRY_RUN" = true ]; then
  echo "" >&2
  echo "==> DRY RUN: Release Candidate v${VERSION}" >&2
  echo "" >&2
  echo "Would stage the following files:" >&2
  # shellcheck disable=SC2086
  echo $POMS >&2
  echo "" >&2
  echo "Would execute:" >&2
  echo "  git add [pom files]" >&2
  echo "  git commit -m \"chore: release candidate v${VERSION}\"" >&2
  echo "  git tag -a \"v${VERSION}\" -m \"Release candidate v${VERSION}\"" >&2
  echo "  git push origin HEAD \"v${VERSION}\"" >&2
  echo "" >&2
  echo "Would trigger on GitHub:" >&2
  echo "  GitHub Actions: verify → publish to GitHub Packages (not Maven Central)" >&2
  echo "" >&2
  echo "To promote to final: make release-minor (or release-patch)" >&2
  echo "Final version will be: ${FINAL_VERSION}" >&2
  echo "" >&2
  # Clean up in dry-run mode too
  rm -f .release-version
  exit 0
fi

# shellcheck disable=SC2086
git add $POMS

git commit -m "chore: release candidate v${VERSION}"
git tag -a "v${VERSION}" -m "Release candidate v${VERSION}"
git push origin HEAD "v${VERSION}"

rm -f .release-version
echo "" >&2
echo "==> v${VERSION} tagged and pushed." >&2
echo "==> GitHub Actions: verify → publish to GitHub Packages (not Maven Central)" >&2
echo "==> To promote to final: make release-minor (or release-patch)" >&2
echo "==> Final version will be: ${FINAL_VERSION}" >&2
