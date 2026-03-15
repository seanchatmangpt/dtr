# DTR (Documentation Testing Runtime) — Claude Code Quick Reference

**Project:** Markdown documentation generator for Java 26 | **Version:** 2026.1.0

---

## THE PIPELINE IS THE SPECIFICATION OF DONE

March 23 is not a deadline. It is a trigger.

```
make release-minor
```

That command fires a git tag. The tag fires GitHub Actions. Actions runs
`mvnd verify` then `mvnd deploy -Prelease`. If verify passes, the artifact
publishes to Maven Central, the GitHub Release is created, and the receipt
exists. If verify fails, nothing publishes.

**Everything generated in this repo during the next 10 days must be designed
to pass `mvnd verify` in a GitHub Actions runner — not on your laptop.**

The right question is never "what features does X need by March 23?"
It is: "What must be true for `make release VERSION=1.0` to succeed in CI
on March 23 and produce a complete, receipted, published release artifact?"
That question has a finite, enumerable answer. The pipeline answers it.

---

## THE ARMSTRONG INVARIANT

One command releases. No manual steps at release time. Ever.

**The only release path is:**
```
make tag          # creates v<VERSION> tag, pushes to origin
                  # GitHub Actions fires automatically
                  # mvnd verify → mvnd deploy -Prelease → gh release create
```

**These paths are closed — do not suggest them:**
- `mvn deploy` run locally
- Uploading JARs by hand to Sonatype
- Setting up `~/.m2/settings.xml` with real credentials
- Any step a human must perform between `make tag` and a published artifact

If you find yourself suggesting a manual deploy step, stop. The pipeline
is broken. Fix the pipeline, not the workaround.

---

## THE CI GATE: `mvnd verify`

Every piece of code generated — DTR tests, benchmarks, JOTP implementation,
documentation — must pass this gate:

```bash
mvnd verify --enable-preview
```

**CI environment constraints (design for these, not for localhost):**

| Constraint | CI value | What it means for generated code |
|---|---|---|
| Java | 26.ea.13, `--enable-preview` | All preview syntax must compile under `--enable-preview` |
| GPG | loopback pinentry, key from `$GPG_PRIVATE_KEY` secret | No interactive passphrase prompts; no `~/.gnupg` assumed |
| Maven Central credentials | `$CENTRAL_USERNAME` / `$CENTRAL_TOKEN` secrets | Never reference `~/.m2/settings.xml` |
| Maven settings | Written by Actions at deploy time | No local settings assumed |
| Network | No corporate proxy | Don't generate proxy-dependent code paths |
| Enforcer | Java ≥ 26, Maven ≥ 4.0.0-rc-3 | These versions are hard requirements, not suggestions |

**GPG loopback is load-bearing context.** This pipeline has been through real
GPG failure in CI. The `--pinentry-mode loopback` configuration in the release
profile exists because interactive pinentry breaks headless runners. Do not
remove it, and do not generate signing code that assumes a TTY.

---

## OUTPUT CONTRACT

Generated DTR tests must land artifacts where the pipeline expects them:

```
target/docs/test-results/    ← Markdown documentation output
target/docs/blog/            ← Blog post output
target/docs/pdf/             ← LaTeX/PDF output
```

A DTR test that generates output somewhere else produces nothing visible in
the release artifact. The pipeline doesn't pick it up. The release is incomplete.

**Configure via system properties (already set in surefire config):**
```xml
<dtr.output.dir>docs/test</dtr.output.dir>
<dtr.format>markdown</dtr.format>
```

Do not hardcode output paths in test code. The pipeline owns the output contract.

---

## FAILURE SEMANTICS

`mvnd verify` fails → nothing publishes.

This is not a bug. This is the invariant enforced.

Consequences for generated code:
- Tests that pass locally but fail in CI (due to preview flags, missing secrets,
  path assumptions) silently break the release. There is no partial publish.
- Optimistic code — "this will probably work in CI" — violates the invariant.
- If you cannot make a test pass under the CI constraints above, do not generate
  the test. Generate a failing test that documents what is missing instead.

When `mvnd verify` fails in CI, the question is always:
"What assumption about the local environment did this code make?"

---

## ⚡ CRITICAL RULES

### 1. REAL CODE, REAL MEASUREMENTS ONLY
- ❌ NO simulation, NO fakes, NO hard-coded numbers
- ✅ Use actual DTR code (RenderMachine + say* methods)
- ✅ Measure with System.nanoTime() on real execution
- ✅ Report: metric + units + Java version + iterations + environment
- **Example:** "JEP 516: 78ns avg (10M accesses, 100 iter, Java 26.ea.13)" NOT "6667x faster"

### 2. ALWAYS USE REAL DTR CLI
- JUnit 5 tests with `DtrContext`
- Output through RenderMachine rendering pipeline
- Never bypass with standalone generators

### 3. Toolchain (Non-Negotiable)
- Java 26: `/usr/lib/jvm/java-26-openjdk-amd64` or SDKMAN: `26.ea.13-graal`
- Maven 4.0.0-rc-3+: `/opt/apache-maven-4.0.0-rc-3/bin/mvn`
- mvnd 2.0.0+: `/opt/mvnd/bin/mvnd` (preferred)
- Flag: `--enable-preview` in `.mvn/maven.config`
- act: Local GitHub Actions testing (brew install act)

---

## VERSION SCHEME: CalVer YYYY.MINOR.PATCH

`2026.3.1` = third feature release of 2026, first patch fix within it.

- **YYYY** — the year. Reads as a timestamp. `2026` dependency is two years old in 2028.
- **MINOR** — feature iterations within the year. Starts at 1. Resets to 1 on year boundary.
- **PATCH** — fixes within a MINOR. Resets to 0 on every MINOR bump.

Year boundary is automatic: `scripts/bump.sh minor` reads `date +%Y`. If the year changed, MINOR resets to 1. No human decides when 2027 starts.

`release-major` does not exist. Breaking changes use deprecation cycle + year boundary.

## RELEASE COMMANDS

The human decides the type of change. The version number is derived.

```bash
make release-minor      # new say* methods, additive features  → YYYY.(N+1).0
make release-patch      # bug fix, no API change               → YYYY.MINOR.(N+1)
make release-year       # explicit year boundary (January)     → YYYY.1.0

make release-rc-minor   # RC for minor                         → YYYY.(N+1).0-rc.N
make release-rc-patch   # RC for patch                         → YYYY.MINOR.(N+1)-rc.N

make snapshot           # deploy SNAPSHOT (no tag, no release)
make version            # print current version
```

**Never type a version number. Never run `mvn deploy` directly.**
The scripts own the arithmetic. You own the semantics.

Release sequence (all automated after `make release-*`):
1. `scripts/bump.sh` — computes NEXT (CalVer + year-aware), updates all pom.xml, writes to `.release-version`
2. `scripts/release.sh` — generates `docs/CHANGELOG.md`, commits, tags `v<VERSION>`, pushes
3. GitHub Actions fires on tag → `mvnd verify` → `mvnd deploy -Prelease` → `gh release create`

RC sequence: same flow but `scripts/release-rc.sh` → `publish-rc.yml` → GitHub Packages only.

---

## HOW TO ADD A TEST

```java
@ExtendWith(DtrExtension.class)
class MyDocTest {
    @Test
    void myFeature(DtrContext ctx) {
        ctx.sayNextSection("Feature Name");
        ctx.say("Description.");
        ctx.sayCode("System.out.println(\"example\");", "java");
        ctx.sayTable(new String[][] {{"Col1", "Col2"}, {"V1", "V2"}});
        ctx.sayWarning("Critical constraint.");
        ctx.sayNote("Context.");
    }
}
```

**Output:** `target/docs/test-results/MyDocTest.md` (+ .tex, .html, .json)

**Before writing the test, ask:** Will this pass `mvnd verify --enable-preview`
in a headless GitHub Actions runner with no local credentials? If not, fix
that first.

---

## say* API

| Method | Output | Use For |
|--------|--------|---------|
| `sayNextSection(String)` | H1 heading | Chapter/section titles |
| `say(String)` | Paragraph | Body text |
| `sayCode(String, lang)` | Fenced block | Code examples |
| `sayTable(String[][])` | Markdown table | Data comparison |
| `sayJson(Object)` | JSON pretty | Payload examples |
| `sayWarning(String)` | Alert block | Critical info |
| `sayNote(String)` | Note block | Tips/context |
| `sayKeyValue(Map)` | 2-col table | Metadata |
| `sayUnorderedList(List)` | Bullets | Features/checklist |
| `sayOrderedList(List)` | Numbered | Steps/sequence |

---

## DTR ARCHITECTURE

**Input:** JUnit 5 test calls `say*` methods
**Process:** RenderMachine captures calls → formats → routes to output engines
**Output:** Markdown + LaTeX + HTML + OpenAPI + Blog (all written to `target/docs/`)

```java
ctx.sayAndMakeRequest(Request.GET().url(api));       // HTTP + documents
ctx.sayAndAssertThat("Status", actual, is(200));     // Assert + documents
```

---

## 🚀 JAVA 26 FEATURES (Use These)

```java
// Records
record Response(int status, String body) {}

// Sealed classes + pattern matching
sealed interface Result permits Success, Error {}
String msg = switch(result) {
    case Success(var d) -> "OK: " + d;
    case Error(var m) -> "FAIL: " + m;
};

// Virtual threads
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> generateDoc());
}

// Text blocks
String html = """
    <div>Content</div>
    """;

// Gatherers (Java 26) - Stream processing
List<String> result = stream.gather(
    gatherer(windowFixed(5))
).toList();
```

All preview syntax requires `--enable-preview` at compile and runtime.
Both are configured in `.mvn/maven.config` and `maven-surefire-plugin`.

---

## BEFORE CODING

1. `java -version` → 26.ea.13+
2. `mvnd --version` → Maven 4.0.0-rc-3+
3. `.mvn/maven.config` contains `--enable-preview`
4. Ask: **will this pass `mvnd verify` in CI?**

---

## 🎭 ACT - LOCAL GITHUB ACTIONS TESTING

**Full Guide:** [docs/devops/act-testing-guide.md](docs/devops/act-testing-guide.md)

### Quick Start
```bash
brew install act                           # Install act
act -l                                     # List workflows
act -j quality-check                       # Run specific job
act -W .github/workflows/quality-gates.yml # Run full workflow
act -n                                     # Dry run
```

### Act Environment Detection
The `.actrc` sets `ACT=true`. Workflows use this to skip GitHub API-dependent steps:

```yaml
- name: Deploy to Maven Central
  if: ${{ !env.ACT }}
  run: ./mvnw deploy -Prelease

- name: Skip deployment (act)
  if: ${{ env.ACT }}
  run: echo "🎭 Running in act - skipping"
```

### Workflow Compatibility

| Workflow | Status | Notes |
|----------|--------|-------|
| `quality-gates.yml` | ✅ Fully Compatible | No secrets required |
| `ci-gate.yml` | ✅ Compatible | Trivy skipped in act |
| `publish.yml` | ✅ Compatible | Deploy skipped in act |
| `publish-rc.yml` | ✅ Compatible | Deploy skipped in act |
| `deployment-automation.yml` | ✅ Compatible | GitHub API skipped |
| `gpg-key-management.yml` | ✅ Compatible | PR creation skipped |

### Secret Management
Create `.secrets` file (never commit):
```bash
cat > .secrets << 'EOF'
CENTRAL_USERNAME=your-username
CENTRAL_TOKEN=your-token
GPG_PRIVATE_KEY=$(cat ~/.gnupg/private.key | base64)
GPG_PASSPHRASE=your-passphrase
GPG_KEY_ID=your-key-id
EOF
```

### Common Commands
```bash
act -j quality-check                                # Test quality gates
act -j build-verification --matrix java-version:26  # Test matrix build
act -W .github/workflows/publish.yml -n             # Dry run publish
act --secret-file .secrets                          # Run with secrets
act pull                                            # Pull latest Docker images
docker system prune -f                              # Clear cache if jobs fail
```

See [docs/devops/act-testing-guide.md](docs/devops/act-testing-guide.md) for full documentation

---

## 🔄 GITHUB ACTIONS CI/CD PIPELINE

### Workflow Files
- `ci-gate.yml` — Quality gates, multi-Java builds, security scans
- `publish.yml` — Maven Central deployment
- `quality-gates.yml` — Code quality checks
- `deployment-automation.yml` — Automated deployments

### CI Gate Triggers
```yaml
on:
  push:
    branches: [ main, master, develop ]
    tags: ['v*']
  pull_request:
    branches: [ main, master ]
```

### Jobs Overview
1. **quality-check** — Spotless, Checkstyle, PMD, SpotBugs
2. **dependency-check** — OWASP dependency checks, version updates
3. **test-coverage** — Unit tests with JaCoCo coverage
4. **security-scan** — Trivy vulnerability scanner
5. **build-verification** — Matrix build: Java 21, 22, 26
6. **deployment-ready** — Checks secrets before release

### Java Setup in Workflows
```yaml
# Java 26 (Early Access via SDKMAN)
- name: Set up Java 26 via SDKMAN
  run: |
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 26.ea.13-graal
    echo "JAVA_HOME=$HOME/.sdkman/candidates/java/current" >> $GITHUB_ENV
    echo "$HOME/.sdkman/candidates/java/current/bin" >> $GITHUB_PATH

# Java 21/22 (Stable via Temurin)
- name: Set up Java
  uses: actions/setup-java@v4
  with:
    distribution: 'temurin'
    java-version: '21'
```

### Required Secrets for Release
Set these in GitHub repo settings (Settings → Secrets and variables → Actions):
- `CENTRAL_USERNAME` — Maven Central username
- `CENTRAL_TOKEN` — Maven Central password/token
- `GPG_PRIVATE_KEY` — Base64-encoded GPG private key
- `GPG_PASSPHRASE` — GPG key passphrase
- `GPG_KEY_ID` — GPG key ID (last 8 chars)

### Local Testing Before Push
```bash
# 1. Run full CI gate locally
act -W .github/workflows/ci-gate.yml --secret-file .secrets

# 2. Test specific job
act -j build-verification --matrix java-version:26

# 3. Dry run to check workflow syntax
act -n -j deployment-ready

# 4. Verify secrets are loaded
act -j deployment-ready --secret-file .secrets -v
```

### Java 26 Migration Notes
- Updated from Java 25 → 26.ea.13-graal
- Works with act using SDKMAN in workflows
- Maven compiler: `-Dmaven.compiler.release=26`
- Multi-Java testing: 21, 22, 26 matrix builds
- `--enable-preview` flag required in `.mvn/maven.config`

---

## 🔍 TROUBLESHOOTING

```bash
mvnd verify                    # The gate — run this before anything else
mvnd --stop                    # Stop daemon if stale
mvnd -X clean verify           # Verbose — find the real failure
cat target/surefire-reports/*  # Test details
echo $JAVA_HOME                # Verify Java 26

# If Maven Central auth fails locally (not in CI):
python3 maven-proxy-auth.py &
```

---

## KEY FILES

| File | Purpose |
|------|---------|
| `Makefile` | Release control surface — start here |
| `.github/workflows/publish.yml` | CI pipeline — tag-triggered, 3 jobs |
| `pom.xml` | Root config, release profile, enforcer |
| `dtr-core/` | Core library (deployed to Maven Central) |
| `dtr-integration-test/` | Integration tests (not deployed) |
| `.mvn/maven.config` | `--enable-preview`, `--batch-mode` |
| `.github/workflows/ci-gate.yml` | GitHub Actions CI/CD pipeline |
| `.secrets` | Local act secrets (NEVER commit) |
| `maven-proxy-auth.py` | Enterprise proxy solution |

---

**Last Updated:** March 14, 2026
**Branch:** feat/java-26-with-calver
**Version:** 2026.1.0 (CalVer YYYY.MINOR.PATCH)
**Invariant:** One command releases. The pipeline is the specification of done.
**Rule:** Always measure real, report real, use real DTR code.
