#!/bin/bash
# Bump the project version semantically and update all pom.xml files.
# Usage: scripts/bump-version.sh <major|minor|patch> <current-version>
# Prints the new version to stdout.
# No Maven plugin required — updates pom.xml directly via Python.
set -euo pipefail

BUMP="${1:?usage: bump-version.sh <major|minor|patch> <current-version>}"
CURRENT="${2:?usage: bump-version.sh <major|minor|patch> <current-version>}"

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"

case "$BUMP" in
  major) NEXT="$((MAJOR + 1)).0.0" ;;
  minor) NEXT="$MAJOR.$((MINOR + 1)).0" ;;
  patch) NEXT="$MAJOR.$MINOR.$((PATCH + 1))" ;;
  *) echo "error: bump must be major, minor, or patch (got: $BUMP)" >&2; exit 1 ;;
esac

echo "==> Bumping $CURRENT → $NEXT ($BUMP)" >&2

# Update all pom.xml files using Python — no Maven plugin, no network.
python3 - "$CURRENT" "$NEXT" <<'PYEOF'
import re, sys, pathlib

old, new = sys.argv[1], sys.argv[2]

def update_root(text, old, new):
    # Project version: first <version> at 4-space indent before <packaging>
    text = re.sub(
        r'^(    <version>)' + re.escape(old) + r'(</version>)',
        r'\g<1>' + new + r'\2',
        text, count=1, flags=re.MULTILINE
    )
    # SCM <tag>
    text = re.sub(
        r'<tag>v?' + re.escape(old) + r'</tag>',
        f'<tag>v{new}</tag>',
        text
    )
    return text

def update_child(text, old, new):
    # Parent version inside <parent>...</parent>
    return re.sub(
        r'(<parent>(?:(?!</parent>).)*?<version>)' + re.escape(old) + r'(</version>(?:(?!</parent>).)*?</parent>)',
        r'\g<1>' + new + r'\2',
        text, flags=re.DOTALL
    )

root = pathlib.Path('pom.xml')
root.write_text(update_root(root.read_text(), old, new))

for child in pathlib.Path('.').glob('*/pom.xml'):
    child.write_text(update_child(child.read_text(), old, new))
PYEOF

echo "$NEXT"
