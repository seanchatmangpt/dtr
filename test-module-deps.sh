#!/bin/bash
# Quick manual test for sayModuleDependencies

cd /Users/sac/dtr

# Compile the test file
javac --enable-preview --release 26 \
  -cp "dtr-core/target/classes:dtr-core/target/test-classes" \
  -d /tmp/test-compile \
  dtr-core/src/test/java/io/github/seanchatmangpt/dtr/integration/JpmsModuleDependenciesIntegrationTest.java 2>&1

if [ $? -eq 0 ]; then
  echo "✓ Integration test compiled successfully"
else
  echo "✗ Integration test compilation failed"
  exit 1
fi

# Compile the unit test file
javac --enable-preview --release 26 \
  -cp "dtr-core/target/classes:dtr-core/target/test-classes:/tmp/test-compile" \
  -d /tmp/test-compile \
  dtr-core/src/test/java/io/github/seanchatmangpt/dtr/test/rendermachine/RenderMachineExtensionTest.java 2>&1

if [ $? -eq 0 ]; then
  echo "✓ Unit test compiled successfully"
else
  echo "✗ Unit test compilation failed"
  exit 1
fi

echo ""
echo "All compilation checks passed!"
echo "Note: Full test execution requires Maven 4.0.0-rc-3+ which is not currently available."
