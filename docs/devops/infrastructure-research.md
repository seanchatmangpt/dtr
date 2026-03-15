# DTR DevOps/Makefile Infrastructure Research

## Context

This document provides a comprehensive research summary of the DTR project's DevOps infrastructure, including the Makefile-based build system, release automation, GitHub Actions CI/CD pipelines, Maven configuration, and quality gates.

---

## 1. Makefile & Build Scripts

### Makefile Structure

The `Makefile` is minimal and focused on javadoc extraction:

```makefile
# Targets:
build-dtr-javadoc     # Build the Rust dtr-javadoc binary
extract-javadoc       # Extract Javadoc to JSON + generate docs/api/ (TPS enforced)
check-javadoc         # CI audit mode - exits 0 even if docs missing
gen-javadoc-docs      # Generate docs/api/ without TPS violation check
```

**Key Integration**: The `extract-javadoc` target runs during Maven build via `maven-exec-plugin` in `dtr-core/pom.xml`. If any public class/method lacks Javadoc, the build **fails** (TPS - Test-Published-Software enforcement).

### Shell Scripts (`scripts/`)

| Script | Purpose |
|--------|---------|
| `bump.sh` | Compute next CalVer version (minor/patch/year) with optional RC suffix |
| `release.sh` | Final release: commit pom changes + docs, create tag, push |
| `release-rc.sh` | Release candidate: commit pom changes, create rc tag, push |
| `set-version.sh` | Direct version setter - updates all pom.xml files |
| `current-version.sh` | Extract version from pom.xml (no Maven invocation) |
| `changelog.sh` | Generate per-release notes and prepend to CHANGELOG.md |

### CalVer Version Scheme

```
YYYY.MINOR.PATCH
  │     │     │
  │     │     └─ Patch counter within MINOR (resets to 0 on MINOR bump)
  │     └─────── Feature iteration within year (resets to 1 on year boundary)
  └───────────── Calendar year (automatic via `date +%Y`)
```

**Example**: `2026.3.1` = third feature release of 2026, first patch fix within it.

#### Year Boundary Transitions

When the calendar year changes, CalVer automatically handles the transition:

| Last Release of 2026 | First Release of 2027 | Breaking Change? |
|---------------------|----------------------|------------------|
| `2026.12.5` | `2027.1.0` | No (automatic reset) |
| `2026.5.0` | `2027.1.0` | No (automatic reset) |

**Key Point**: The year boundary does NOT indicate breaking changes. CalVer uses the year as a timestamp, not as a signal for compatibility. Breaking changes are handled via deprecation cycles with a minimum one-year removal window.

#### Automatic Year Detection

The `scripts/bump.sh` script uses `date +%Y` to detect the current year:

```bash
# In scripts/bump.sh
CURRENT_YEAR=$(date +%Y)
# If current year != version year, reset MINOR to 1
```

**Examples:**
- December 2026: `make release-minor` → `v2026.12.0`
- January 2027: `make release-minor` → `v2027.1.0` (automatic)

#### When to Use `make release-year`

The `release-year` target exists for **explicit** year boundary handling:

```bash
# Use when you want to force the new year
make release-year
```

**Scenarios for `release-year`:**
1. **January releases** - Ensures new year is set correctly
2. **Year boundary rollover** - If a release spans Dec 31 → Jan 1
3. **Manual year correction** - If wrong year was used accidentally

#### Year Boundary Edge Case

**Releasing at 23:59 on December 31:**

```bash
# At 23:59 on Dec 31, 2026
make release-minor
# scripts/bump.sh runs: date +%Y → 2026
# Result: v2026.12.0

# At 00:01 on Jan 1, 2027 (before next release)
# No issue - year will be detected correctly on next release

# If you need to release immediately after midnight:
make release-year
# Explicitly sets: v2027.1.0
```

**Prevention:** Avoid releasing within 1 hour of year boundary unless using `release-year`.

---

## 2. Release Workflow

### The "One Command Releases" Invariant

```bash
make release-minor      # new features, additive changes    → YYYY.(N+1).0
make release-patch      # bug fixes, no API change          → YYYY.MINOR.(N+1)
make release-year       # explicit year boundary (January)  → YYYY.1.0
make release-rc-minor   # RC for a minor bump               → YYYY.(N+1).0-rc.N
make release-rc-patch   # RC for a patch fix                → YYYY.MINOR.(N+1)-rc.N
```

### Release Sequence (Final)

```
make release-minor
  │
  ├─ scripts/bump.sh minor
  │   ├─ reads CURRENT from pom.xml
  │   ├─ computes NEXT (year-aware)
  │   ├─ sed-updates all pom.xml files
  │   └─ writes NEXT to .release-version
  │
  ├─ scripts/release.sh
  │   ├─ reads VERSION from .release-version
  │   ├─ runs scripts/changelog.sh → docs/CHANGELOG.md + docs/releases/VERSION.md
  │   ├─ git add pom.xml files + docs
  │   ├─ git commit "chore: release vVERSION"
  │   ├─ git tag -a vVERSION
  │   └─ git push origin HEAD vVERSION
  │
  └─ GitHub Actions fires (publish.yml)
      ├─ mvnd verify
      ├─ mvnd deploy -Prelease → Maven Central
      └─ gh release create vVERSION --generate-notes
```

### Release Candidate Workflow

RC builds go to **GitHub Packages only**. Maven Central receives final versions only.

```bash
# Create first RC
make release-rc-minor          # → v2026.2.0-rc.1

# If RC needs fixes, push code changes, then:
make release-rc-minor          # → v2026.2.0-rc.2 (N auto-increments)

# Promote to final when RC is good:
make release-minor             # → v2026.2.0 (strips -rc.N, publishes to Maven Central)
```

**RC Promotion**: When promoting from `-rc.N` to final, the minor number is **not** incremented again — it was already bumped when the RC was created.

---

## 3. GitHub Actions CI/CD Pipeline

### Workflow Files (10 total)

| Workflow | Purpose | Trigger |
|----------|---------|---------|
| `ci-gate.yml` | Quality gates, multi-Java builds, security scans | push to main/master, PRs, tags |
| `publish.yml` | Maven Central deployment (final releases only) | tags matching `v*` (no `-rc.`) |
| `publish-rc.yml` | GitHub Packages deployment (RC only) | tags matching `v*-rc.*` |
| `quality-gates.yml` | Code quality checks | push to main/master, PRs |
| `deployment-automation.yml` | Manual/staged deployment | workflow_call, workflow_dispatch |
| `publish-validation.yml` | Pre-publish validation | workflow_dispatch |
| `test-deploy.yml` | Test deployment | workflow_dispatch |
| `update-publish.yml` | Update publishing | workflow_dispatch |
| `monitor-deployment.yml` | Deployment monitoring | workflow_run |
| `gpg-key-management.yml` | GPG key rotation | workflow_dispatch |

### CI Gate Jobs (`ci-gate.yml`)

| Job | Purpose |
|-----|---------|
| `quality-check` | Spotless, Checkstyle, PMD, SpotBugs |
| `dependency-check` | OWASP dependency-check, version updates |
| `test-coverage` | Unit tests with JaCoCo coverage |
| `security-scan` | Trivy vulnerability scanner |
| `build-verification` | Matrix build: Java 21, 22, 26 |
| `deployment-ready` | Checks secrets before release |

### Java 26 Setup (SDKMAN)

All workflows use SDKMAN for Java 26 EA:

```yaml
- name: Set up Java 26 via SDKMAN
  run: |
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 26.ea.13-graal
    echo "JAVA_HOME=$HOME/.sdkman/candidates/java/current" >> $GITHUB_ENV
    echo "$HOME/.sdkman/candidates/java/current/bin" >> $GITHUB_PATH
```

### Required Secrets (for Release)

Set in GitHub repo settings (Settings → Secrets and variables → Actions):

| Secret | Purpose |
|--------|---------|
| `CENTRAL_USERNAME` | Maven Central username |
| `CENTRAL_TOKEN` | Maven Central password/token |
| `GPG_PRIVATE_KEY` | Base64-encoded GPG private key |
| `GPG_PASSPHRASE` | GPG key passphrase |
| `GPG_KEY_ID` | GPG key ID (last 8 chars) |

---

## 4. Maven Configuration

### Project Structure

```
dtr/
├── pom.xml                      # Root Maven configuration
├── dtr-core/                    # Main library (deployed to Maven Central)
│   └── pom.xml
├── dtr-benchmarks/              # JMH benchmarks (not deployed)
│   └── pom.xml
└── .mvn/
    └── maven.config             # Maven flags
```

### `.mvn/maven.config`

```
--no-transfer-progress
--batch-mode
-Dmaven.compiler.enablePreview=true
-Dmaven.compiler.release=26
```

### Root `pom.xml` Key Properties

```xml
<properties>
    <maven.compiler.release>26</maven.compiler.release>
    <dtr.output.dir>docs/test</dtr.output.dir>
    <dtr.format>markdown</dtr.format>
    <dtr.javadoc.skip>false</dtr.javadoc.skip>
    ...
</properties>
```

### Maven Profiles

| Profile | Purpose |
|---------|---------|
| `release` | Maven Central deployment with GPG signing |
| `release-rc` | GitHub Packages deployment (RC) |
| `license` | License header checks |

### Compiler Configuration

```xml
<plugin>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.14.0</version>
    <configuration>
        <release>26</release>
        <compilerArgs>
            <arg>--enable-preview</arg>
        </compilerArgs>
        <enablePreview>true</enablePreview>
    </configuration>
</plugin>
```

### Surefire (Test) Configuration

```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.5.3</version>
    <configuration>
        <argLine>
            --enable-preview
            --add-opens java.base/java.lang=ALL-UNNAMED
            --add-opens java.base/java.lang.reflect=ALL-UNNAMED
        </argLine>
        <forkCount>1</forkCount>
        <reuseForks>false</reuseForks>
    </configuration>
</plugin>
```

### Enforcer Rules

```xml
<requireJavaVersion>
    <version>[26,)</version>
    <message>Java 26 or higher is required.</message>
</requireJavaVersion>
<requireMavenVersion>
    <version>[4.0.0-rc-3,)</version>
    <message>Maven 4.0.0-rc-3 or higher is required.</message>
</requireMavenVersion>
```

---

## 5. Quality Gates

### Code Quality Checks

| Tool | Purpose |
|------|---------|
| **Spotless** | Code formatting |
| **Checkstyle** | Code style checks |
| **PMD** | Code quality analysis |
| **SpotBugs** | Bug detection |

### Security Scanning

| Tool | Purpose |
|------|---------|
| **OWASP dependency-check** | Dependency vulnerability scanning |
| **Trivy** | Container/file vulnerability scanner |

### Test Coverage

- **JaCoCo** - Java Code Coverage
- Coverage requirements enforced during CI

### Multi-Java Testing

Matrix build tests across:
- Java 21 (stable LTS)
- Java 22 (stable)
- Java 26 (EA with SDKMAN)

---

## 6. Local Testing with act

### `.actrc` Configuration

```
-P ubuntu-latest=catthehacker/ubuntu:act-latest
--container-architecture linux/amd64
--secret GITHUB_TOKEN=
--env JAVA_VERSION=26
-W .github/workflows/
```

### Common act Commands

```bash
# List all workflows
act -l

# Run quality check job
act -j quality-check

# Run with secrets file
act --secret-file .secrets -j test-coverage

# Full CI: run all CI gate jobs
act -j quality-check -j dependency-check -j test-coverage -j security-scan -j build-verification
```

### Creating `.secrets` File

```bash
cat > .secrets << 'EOF'
CENTRAL_USERNAME=your_username
CENTRAL_TOKEN=your_token
GPG_PRIVATE_KEY=$(cat ~/.gnupg/private.key | base64)
GPG_PASSPHRASE=your_passphrase
GPG_KEY_ID=your_key_id
EOF
chmod 600 .secrets
```

---

## 7. Critical Files Reference

| File | Purpose |
|------|---------|
| `Makefile` | Javadoc extraction targets |
| `scripts/bump.sh` | CalVer version computation |
| `scripts/release.sh` | Final release automation |
| `scripts/release-rc.sh` | RC release automation |
| `scripts/changelog.sh` | Changelog generation |
| `pom.xml` | Root Maven configuration |
| `dtr-core/pom.xml` | Core module config |
| `.mvn/maven.config` | Maven flags |
| `.actrc` | act configuration |
| `.github/workflows/ci-gate.yml` | Main CI/CD pipeline |
| `.github/workflows/publish.yml` | Maven Central deployment |
| `.github/workflows/publish-rc.yml` | GitHub Packages deployment |

---

## 8. Invariants and Constraints

### The Armstrong Invariant

**One command releases. No manual steps at release time. Ever.**

The only release path is:
```
make release-* → tag → GitHub Actions → mvnd verify → mvnd deploy → gh release
```

### The CI Gate

Every piece of code must pass:
```
mvnd verify --enable-preview
```

### GPG Loopback

The pipeline uses `--pinentry-mode loopback` for non-interactive signing in CI.

### No Rebase

The project uses **merge only** to preserve history.

---

## 9. Verification Commands

```bash
# Verify all pom.xml files use Java 26
grep -r "maven.compiler.release\|<release>" **/pom.xml

# Verify maven.config
cat .mvn/maven.config

# Test build locally
mvnd clean verify --enable-preview

# List workflows
act -l

# Run specific workflow locally
act -j quality-check
```

---

## 10. Key Decisions and Trade-offs

| Decision | Rationale |
|----------|-----------|
| CalVer over SemVer | Version as temporal receipt, year boundary handles breaking changes |
| Makefile for javadoc only | Maven handles main build, Makefile handles Rust tool |
| SDKMAN for Java 26 | actions/setup-java doesn't support EA versions |
| GPG loopback mode | Required for headless CI/CD signing |
| Merge-only workflow | Preserves complete git history |
| RC to GitHub Packages | Allows testing without Maven Central pollution |
| TPS enforcement (javadoc) | Fails build if public APIs lack documentation |

---

**Last Updated:** March 14, 2026
**Branch:** feat/java-26-with-calver
**Version:** 2026.1.0 (CalVer YYYY.MINOR.PATCH)

---

## Appendix A: Quick Reference

### Version Bump Rules

| Current | Bump Type | Next |
|---------|-----------|------|
| `2026.3.1` | minor | `2026.4.0` |
| `2026.3.1` | patch | `2026.3.2` |
| `2026.12.5` | minor (year boundary) | `2027.1.0` |

### Release Workflow Decision Tree

```
Is this a breaking change?
├─ Yes → Use deprecation cycle, wait for year boundary
└─ No → Does it add new API?
    ├─ Yes → make release-minor
    └─ No (bug fix only) → make release-patch
```

### CI/CD Pipeline Triggers

```
Push to main/master      → ci-gate.yml, quality-gates.yml
Pull request            → ci-gate.yml, quality-gates.yml
Tag v* (no -rc.)        → ci-gate.yml + publish.yml
Tag v*-rc.*             → ci-gate.yml + publish-rc.yml
```
