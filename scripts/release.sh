#!/bin/bash
# Final release: commit pom changes + docs, create annotated tag, push.
# Reads version from .release-version (written by scripts/bump.sh).
# Triggers GitHub Actions → mvnd verify → Maven Central deploy → gh release create.
set -euo pipefail

DRY_RUN=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --dry-run)
            DRY_RUN=true
            shift
            ;;
        *)
            echo "Unknown option: $1" >&2
            echo "Usage: $0 [--dry-run]" >&2
            exit 1
            ;;
    esac
done

run_cmd() {
    if [ "$DRY_RUN" = true ]; then
        echo "[DRY-RUN] Would execute: $*" >&2
    else
        echo "[EXEC] $*" >&2
        "$@"
    fi
}

VERSION=$(cat .release-version)

echo "" >&2
if [ "$DRY_RUN" = true ]; then
    echo "==> DRY-RUN MODE: Showing what would be done for v${VERSION}" >&2
else
    echo "==> Preparing release v${VERSION}" >&2
fi
echo "" >&2

# Generate changelog before committing so docs are baked into the release commit
run_cmd scripts/changelog.sh "$VERSION"

# Stage all pom.xml files updated by bump.sh + generated docs
POMS=$(find . -name 'pom.xml' -not -path '*/target/*')
# shellcheck disable=SC2086
run_cmd git add $POMS docs/CHANGELOG.md "docs/releases/${VERSION}.md"

run_cmd git commit -m "chore: release v${VERSION}"
run_cmd git tag -a "v${VERSION}" -m "Release v${VERSION}"
run_cmd git push origin HEAD "v${VERSION}"

run_cmd rm -f .release-version

echo "" >&2
if [ "$DRY_RUN" = true ]; then
    echo "==> DRY-RUN COMPLETE: No changes were made" >&2
    echo "==> To execute for real, run: scripts/release.sh" >&2
else
    echo "==> Released v${VERSION} — pipeline is running." >&2
    echo "==> GitHub Actions: verify → sign → publish to Maven Central" >&2
    echo "==> Track at: https://github.com/seanchatmangpt/dtr/actions" >&2
fi
