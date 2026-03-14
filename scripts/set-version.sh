#!/bin/bash
# Set an explicit version in all pom.xml files.
# Used internally by bump.sh and for hotfix scenarios.
# Usage: set-version.sh <new-version> [old-version]
# If old-version is omitted, reads from pom.xml.
set -euo pipefail

NEW="${1:?usage: set-version.sh <new-version> [old-version]}"
OLD="${2:-$(scripts/current-version.sh)}"

python3 - "$OLD" "$NEW" << 'PYEOF'
import re, sys, pathlib

old, new = sys.argv[1], sys.argv[2]
# Strip any -rc.N suffix from old to match what is in pom.xml
old_base = re.sub(r'-rc\.\d+$', '', old)

def update_root(text, old, new):
    # Project version: first <version> at 4-space indent
    text = re.sub(
        r'^(    <version>)' + re.escape(old) + r'(-rc\.\d+)?(</version>)',
        r'\g<1>' + new + r'\3',
        text, count=1, flags=re.MULTILINE
    )
    # SCM <tag> — set to HEAD during development; set-version.sh writes actual tag at release
    text = re.sub(
        r'<tag>v?' + re.escape(old) + r'(?:-rc\.\d+)?</tag>',
        f'<tag>v{new}</tag>',
        text
    )
    return text

def update_child(text, old, new):
    return re.sub(
        r'(<parent>(?:(?!</parent>).)*?<version>)' + re.escape(old) + r'(?:-rc\.\d+)?(</version>(?:(?!</parent>).)*?</parent>)',
        r'\g<1>' + new + r'\2',
        text, flags=re.DOTALL
    )

root = pathlib.Path('pom.xml')
root.write_text(update_root(root.read_text(), old_base, new))

for child in pathlib.Path('.').glob('*/pom.xml'):
    child.write_text(update_child(child.read_text(), old_base, new))
PYEOF
