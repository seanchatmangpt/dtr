#!/bin/bash
# Extract project version directly from pom.xml — no Maven invocation required.
grep -m1 '<version>' pom.xml | sed 's/.*<version>\(.*\)<\/version>.*/\1/' | tr -d '[:space:]'
