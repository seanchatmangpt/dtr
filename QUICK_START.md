# DTR Quick Start Guide - Java 26

## Prerequisites
- Java 26 (GraalVM recommended)
- Maven 4.0.0-rc-3 or higher

## Quick Commands

### Run All Tests
```bash
./test-all.sh
```

### Build the Project
```bash
./test-all.sh clean install
```

### Run Specific Test
```bash
./test-all.sh test -Dtest=DtrCoreTest
```

## Expected Results
- **Tests**: 531 tests pass
- **Build Time**: ~10-15 seconds
- **Status**: BUILD SUCCESS

## Troubleshooting

### Issue: "Preview features are not enabled"
**Solution:** Ensure you're using the test-all.sh script, which sets:
```bash
export JAVA_HOME=/Users/sac/.sdkman/candidates/java/26.ea.13-graal
export MAVEN_OPTS="--enable-preview"
```

### Issue: "release version 26 not supported"
**Solution:** Verify maven-compiler-plugin version is 3.14.0 or higher:
```bash
grep -A2 "maven-compiler-plugin" pom.xml | grep version
```

## Verification
Run this command to verify your setup:
```bash
./test-all.sh test -Dtest=DtrCoreTest
```

Expected output:
```
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Next Steps
- Read FINAL_REPORT.md for detailed migration information
- Review JAVA_26_MIGRATION_SUMMARY.md for configuration details
- Check CHANGELOG.md for project history
