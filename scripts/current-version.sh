#!/bin/bash
# Print the current project version from pom.xml, stripping any -SNAPSHOT suffix.
# Reads directly from XML — no Maven plugin download required.
set -euo pipefail

python3 - <<'EOF'
import xml.etree.ElementTree as ET
ns = 'http://maven.apache.org/POM/4.0.0'
version = ET.parse('pom.xml').getroot().find(f'{{{ns}}}version').text
print(version.replace('-SNAPSHOT', ''))
EOF
