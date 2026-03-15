#!/bin/bash
# Quick test script to verify saySystemProperties() implementation

cd /Users/sac/dtr/dtr-core

echo "Testing saySystemProperties() implementation..."

# Create a simple test class
cat > /tmp/SysPropsTest.java << 'EOF'
import io.github.seanchatmangpt.dtr.rendermachine.RenderMachineImpl;

public class SysPropsTest {
    public static void main(String[] args) {
        RenderMachineImpl rm = new RenderMachineImpl();

        System.out.println("=== Test 1: All System Properties ===");
        rm.saySystemProperties();

        System.out.println("\n=== Test 2: Java Properties Only ===");
        rm.saySystemProperties("java.*");

        System.out.println("\n=== Test 3: User Properties Only ===");
        rm.saySystemProperties("user.*");

        System.out.println("\n=== Test 4: Invalid Filter ===");
        rm.saySystemProperties("nonexistent.*");
    }
}
EOF

# Compile and run
javac --enable-preview --release 26 -cp "target/classes:$(find ~/.m2/repository -name '*.jar' -type f | tr '\n' ':')" /tmp/SysPropsTest.java 2>&1 | head -20

if [ $? -eq 0 ]; then
    echo "Compilation successful!"
    java --enable-preview -cp "target/classes:$(find ~/.m2/repository -name '*.jar' -type f | tr '\n' ':'):/tmp" SysPropsTest 2>&1 | head -100
else
    echo "Compilation failed!"
fi
