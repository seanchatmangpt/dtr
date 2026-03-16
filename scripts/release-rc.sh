#!/bin/bash
# Release candidate: commit pom changes, create rc tag, push.
# Reads version from .release-version (written by scripts/bump.sh).
# Triggers RC GitHub Actions → mvnd verify → GitHub Packages deploy.
# Does NOT publish to Maven Central.
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

# Verify this is actually an RC version
if [[ "$VERSION" != *"-rc."* ]]; then
  echo "error: expected an RC version (e.g. 2026.3.0-rc.1), got: $VERSION" >&2
  echo "       use scripts/release.sh for final releases" >&2
  exit 1
fi

FINAL_VERSION=$(echo "$VERSION" | sed 's/-rc\.[0-9]*//')

echo "" >&2
if [ "$DRY_RUN" = true ]; then
    echo "==> DRY-RUN MODE: Showing what would be done for v${VERSION}" >&2
else
    echo "==> Preparing release candidate v${VERSION}" >&2
fi
echo "" >&2

# Stage pom.xml files updated by bump.sh
POMS=$(find . -name 'pom.xml' -not -path '*/target/*')
# shellcheck disable=SC2086
run_cmd git add $POMS

run_cmd git commit -m "chore: release candidate v${VERSION}"
run_cmd git tag -a "v${VERSION}" -m "Release candidate v${VERSION}"
run_cmd git push origin HEAD "v${VERSION}"

run_cmd rm -f .release-version

echo "" >&2
if [ "$DRY_RUN" = true ]; then
    echo "==> DRY-RUN COMPLETE: No changes were made" >&2
    echo "==> To execute for real, run: scripts/release-rc.sh" >&2
else
    echo "==> v${VERSION} tagged and pushed." >&2
    echo "==> GitHub Actions: verify → publish to GitHub Packages (not Maven Central)" >&2
    echo "==> To promote to final: make release-minor (or release-patch)" >&2
    echo "==> Final version will be: ${FINAL_VERSION}" >&2
fi
