#!/bin/bash
# Final release: commit pom changes + docs, create annotated tag, push.
# Reads version from .release-version (written by scripts/bump.sh).
# Triggers GitHub Actions → mvnd verify → Maven Central deploy → gh release create.
#
# Usage: release.sh [--dry-run]
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

# Generate changelog before committing so docs are baked into the release commit
scripts/changelog.sh "$VERSION"

# Stage all pom.xml files updated by bump.sh + generated docs
POMS=$(find . -name 'pom.xml' -not -path '*/target/*')

if [ "$DRY_RUN" = true ]; then
  echo "" >&2
  echo "==> DRY RUN: Final Release v${VERSION}" >&2
  echo "" >&2
  echo "Would stage the following files:" >&2
  # shellcheck disable=SC2086
  echo $POMS >&2
  echo "  docs/CHANGELOG.md" >&2
  echo "  docs/releases/${VERSION}.md" >&2
  echo "" >&2
  echo "Would execute:" >&2
  echo "  git add [pom files, changelog, release docs]" >&2
  echo "  git commit -m \"chore: release v${VERSION}\"" >&2
  echo "  git tag -a \"v${VERSION}\" -m \"Release v${VERSION}\"" >&2
  echo "  git push origin HEAD \"v${VERSION}\"" >&2
  echo "" >&2
  echo "Would trigger on GitHub:" >&2
  echo "  GitHub Actions: verify → sign → publish to Maven Central" >&2
  echo "  Track at: https://github.com/seanchatmangpt/dtr/actions" >&2
  echo "" >&2
  # Clean up in dry-run mode too
  rm -f .release-version
  exit 0
fi

# shellcheck disable=SC2086
git add $POMS docs/CHANGELOG.md "docs/releases/${VERSION}.md"

git commit -m "chore: release v${VERSION}"
git tag -a "v${VERSION}" -m "Release v${VERSION}"
git push origin HEAD "v${VERSION}"

rm -f .release-version
echo "" >&2
echo "==> Released v${VERSION} — pipeline is running." >&2
echo "==> GitHub Actions: verify → sign → publish to Maven Central" >&2
echo "==> Track at: https://github.com/seanchatmangpt/dtr/actions" >&2
