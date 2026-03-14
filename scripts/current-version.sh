#!/bin/bash
# Extract project version directly from pom.xml — no Maven invocation required.
# Returns the version as-is (including any -rc.N suffix; no -SNAPSHOT strip needed for CalVer).
set -euo pipefail

python3 - <<'EOF'
import xml.etree.ElementTree as ET
ns = 'http://maven.apache.org/POM/4.0.0'
version = ET.parse('pom.xml').getroot().find(f'{{{ns}}}version').text
print(version.strip())
EOF
