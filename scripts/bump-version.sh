#!/bin/bash
set -euo pipefail

BUMP=$1
CURRENT=$2

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

case $BUMP in
  major) NEXT="$((MAJOR + 1)).0.0" ;;
  minor) NEXT="$MAJOR.$((MINOR + 1)).0" ;;
  patch) NEXT="$MAJOR.$MINOR.$((PATCH + 1))" ;;
  *) echo "Usage: bump-version.sh [major|minor|patch] <current-version>" >&2; exit 1 ;;
esac

# Update version in all pom.xml files (root project version + child parent references)
find . -name 'pom.xml' -not -path '*/target/*' \
  -exec sed -i "s|<version>${CURRENT}</version>|<version>${NEXT}</version>|g" {} +

echo "$NEXT"
