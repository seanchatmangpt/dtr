---
name: verify
description: Run the CI gate locally with mvnd verify. Use this skill when the user asks to "run tests", "check the build", "run verify", "does it compile", "check CI gate", "run the pipeline locally", or any variation of running the full build.
tools: Bash, Read
---

Run the full `mvnd verify` CI gate and report results clearly.

## Steps

1. Run the build:

```bash
cd $CLAUDE_PROJECT_DIR
mvnd verify --enable-preview --no-transfer-progress -B 2>&1
```

2. If the build **passes**:
   - Report: `BUILD SUCCESS`
   - Show test count from surefire summary
   - Show total elapsed time

3. If the build **fails**:
   - Report: `BUILD FAILURE`
   - Read `target/surefire-reports/*.txt` for the failing test details
   - Identify the root cause (compilation error vs test failure vs enforcer violation)
   - Suggest the fix based on the error type:
     - Compilation: check Java 26 preview syntax, `--enable-preview` flag
     - Test failure: read the test class, check assertion messages
     - Enforcer: check Java/Maven version constraints

## Output Format

```
CI Gate: PASS ✓ (47 tests, 12.3s)
```
or
```
CI Gate: FAIL ✗
  Test: DtrCoreTest#myTest
  Error: AssertionError: expected <200> but was <404>
  File: dtr-core/src/test/java/.../DtrCoreTest.java:87
```

## Constraints

- Never skip tests (`-DskipTests` is forbidden in verify runs)
- Use `mvnd` (daemon), not `mvn` or `./mvnw`
- `--enable-preview` is always required
- Report real elapsed time from build output, not estimated
