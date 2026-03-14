#!/bin/bash
# Direct version setter — used by bump.sh. Not a primary interface.
# Usage: scripts/set-version.sh <new-version>
set -euo pipefail

NEW=$1
CURRENT=$(scripts/current-version.sh)

find . -name 'pom.xml' -not -path '*/target/*' \
  -exec sed -i "s|<version>${CURRENT}</version>|<version>${NEW}</version>|g" {} +
