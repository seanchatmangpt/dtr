# DTR-004: Developer Build Optimizations

**Priority**: P1
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,build

## Description

Local development currently uses the same heavyweight build configuration as CI (Java 26.ea with `--enable-preview`, strict verification, full plugin execution). This ticket adds developer-friendly optimizations for faster iteration during feature development, while maintaining CI gate integrity.

## Acceptance Criteria

- [ ] Create `.mvn/maven.config.local.template` with developer-friendly settings
- [ ] Add `dev` profile to `pom.xml` that skips non-essential plugins
- [ ] Add `quick` profile to `pom.xml` that skips tests and docs generation
- [ ] Add Make targets: `make dev-build`, `make quick-build`, `make dev-test`
- [ ] Document in README how to enable local dev mode
- [ ] Verify CI still uses full `mvnd verify --enable-preview` (no changes to CI gate)

## Technical Notes

### Files to Create/Modify

#### 1. `.mvn/maven.config.local.template`
```bash
# Template for local developer overrides
# Usage: cp .mvn/maven.config.local.template .mvn/maven.config.local

# Faster builds for local development (NOT for CI)
-DskipTests=false
-DskipITs=true
-Dmaven.javadoc.skip=false
-DdryRun=false

# Disable strict checks during development
-Dcheckstyle.skip=false
-Dfmt.skip=true
```

#### 2. `pom.xml` Profile Additions
```xml
<profile>
  <id>dev</id>
  <activation>
    <property>
      <name>env</name>
      <value>dev</value>
    </property>
  </activation>
  <properties>
    <skipTests>false</skipTests>
    <skipITs>true</skipITs>
  </properties>
</profile>

<profile>
  <id>quick</id>
  <properties>
    <skipTests>true</skipTests>
    <maven.javadoc.skip>true</maven.javadoc.skip>
    <checkstyle.skip>true</checkstyle.skip>
  </properties>
</profile>
```

#### 3. `Makefile` Additions
```makefile
# Developer build targets
.PHONY: dev-build quick-build dev-test

dev-build:
	mvn clean compile -Pdev

quick-build:
	mvn clean package -Pquick

dev-test:
	mvn test -Pdev

# Preserve existing release targets
```

### Developer Workflow

**Feature Development** (fast iteration):
```bash
make dev-build          # Compile only (no tests)
make quick-build        # Package without tests/docs
make dev-test           # Run unit tests only
```

**Pre-Commit Verification** (full check):
```bash
mvnd verify             # Full verification (CI-equivalent)
```

**CI/CD** (no changes):
```bash
mvnd verify --enable-preview  # Existing CI gate
```

### Key Invariant
**DO NOT modify `.mvn/maven.config`** - This file is used by CI. All local optimizations MUST be in `.mvn/maven.config.local` (gitignored).

### Documentation Updates
- Update `/Users/sac/dtr/README.md` with "Developer Quick Start" section
- Document when to use each profile
- Emphasize that `mvnd verify` is required before committing

## Dependencies

- None (can be implemented independently)

## References

- Maven build configuration: `/Users/sac/dtr/pom.xml`
- Maven config: `/Users/sac/dtr/.mvn/maven.config`
- Makefile: `/Users/sac/dtr/Makefile`
- CI Configuration: `/Users/sac/dtr/.github/workflows/`
