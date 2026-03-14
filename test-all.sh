#!/bin/bash
# Run all DTR tests with Java 26

export JAVA_HOME=/Users/sac/.sdkman/candidates/java/26.ea.13-graal
export MAVEN_OPTS="--enable-preview"

echo "Running all DTR tests with Java 26..."
echo "JAVA_HOME: $JAVA_HOME"
echo "MAVEN_OPTS: $MAVEN_OPTS"
echo ""

/Users/sac/.sdkman/candidates/maven/current/bin/mvn clean test "$@"
