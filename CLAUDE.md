# DTR (Documentation Testing Runtime) — Claude Code Quick Reference

**Project:** Markdown documentation generator for Java 26 | **Version:** 2.5.0

---

## ⚡ CRITICAL RULES

### 1. REAL CODE, REAL MEASUREMENTS ONLY
- ❌ NO simulation, NO fakes, NO hard-coded numbers
- ✅ Use actual DTR code (RenderMachine + say* methods)
- ✅ Measure with System.nanoTime() on real execution
- ✅ Report: metric + units + Java version + iterations + environment
- **Example:** "JEP 516: 78ns avg (10M accesses, 100 iter, Java 26.ea.13)" NOT "6667x faster"

### 2. ALWAYS USE REAL DTR CLI
- ✅ JUnit 5 tests with DtrContext
- ✅ Output through RenderMachine rendering pipeline
- ❌ Never bypass with standalone generators

### 3. Toolchain (Non-Negotiable)
- Java 26: `/usr/lib/jvm/java-26-openjdk-amd64` or SDKMAN: `26.ea.13-graal`
- Maven 4.0.0-rc-3+: `/opt/apache-maven-4.0.0-rc-3/bin/mvn`
- mvnd 2.0.0+: `/opt/mvnd/bin/mvnd` (preferred)
- Flag: `--enable-preview` in `.mvn/maven.config`
- act: Local GitHub Actions testing (brew install act)

---

## 🔧 QUICK BUILD

```bash
# Run real DTR test
mvnd test -pl dtr-integration-test -Dtest=PhDThesisDocTest

# If Maven auth fails, start proxy first
python3 maven-proxy-auth.py &

# Then build with proxy
mvnd clean install -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128 \
  -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128

# Check output
ls target/docs/test-results/
cat target/docs/test-results/PhDThesisDocTest.md
```

---

## 🌐 MAVEN PROXY SOLUTION

**Problem:** "too many authentication attempts" from Maven Central

**Fix:** 3 steps
```bash
# 1. Start proxy
python3 maven-proxy-auth.py &

# 2. Add to .mvn/jvm.config
-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=3128
-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=3128
-Dhttp.nonProxyHosts=localhost|127.0.0.1

# 3. Set env var
export https_proxy=http://user:pass@proxy.company.com:8080
```

**What it does:** Listens on 127.0.0.1:3128, handles HTTPS CONNECT tunneling, injects Proxy-Authorization header automatically.

**Troubleshoot:**
- Still failing? `pkill -f maven-proxy && python3 maven-proxy-auth.py &`
- Check env? `echo $https_proxy`

---

## 📝 HOW TO ADD A TEST

```java
@ExtendWith(DtrExtension.class)
class PhDThesisDocTest {
    @Test
    void testThesis(DtrContext ctx) {
        ctx.sayNextSection("Chapter Title");
        ctx.say("Content here.");
        ctx.sayCode("System.out.println(\"code\");", "java");
        ctx.sayTable(new String[][] {{"Col1", "Col2"}, {"V1", "V2"}});
        ctx.sayJson(someObject);
        ctx.sayWarning("Important!");
        ctx.sayNote("FYI...");
    }
}
```

**Output:** `target/docs/test-results/PhDThesisDocTest.md` (+ .tex, .html, .json)

---

## 📊 say* API (Documentation Methods)

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

## 🎯 DTR ARCHITECTURE (80/20)

**Input:** JUnit 5 test calls `say*` methods
**Process:** RenderMachine captures calls → formats → routes to output engines
**Output:** Markdown + LaTeX + HTML + OpenAPI + Blog (auto-generated)

**Example:**
```java
ctx.sayAndMakeRequest(Request.GET().url(api));  // Executes HTTP + documents
ctx.sayAndAssertThat("Status", actual, is(200)); // Assert + document result
// Output: Complete API doc with request/response examples
```

---

## 🚀 JAVA 26 FEATURES (Use These)

```java
// Records (immutable data)
record Response(int status, String body) {}

// Sealed classes + pattern matching
sealed interface Result permits Success, Error {}
String msg = switch(result) {
    case Success(var d) -> "OK: " + d;
    case Error(var m) -> "FAIL: " + m;
};

// Virtual threads (millions of concurrent tasks)
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> generateDoc());
}

// Text blocks (multi-line strings)
String html = """
    <div>Content</div>
    """;

// Gatherers (Java 26) - Stream processing
List<String> result = stream.gather(
    gatherer(windowFixed(5))
).toList();
```

---

## ✅ BEFORE CODING

1. `java -version` → 26.ea.13+
2. `mvnd --version` → Maven 4.0.0-rc-3+
3. `.mvn/maven.config` contains `--enable-preview`
4. Proxy running (if needed): `python3 maven-proxy-auth.py &`
5. Remember: **REAL CODE + REAL MEASUREMENTS + REAL DOCTESTER CLI**

---

## 🎭 ACT - LOCAL GITHUB ACTIONS TESTING

**What is act?** Run GitHub Actions workflows locally on macOS/Linux/Windows

### Quick Start
```bash
# Install act
brew install act  # macOS
# or: curl https://raw.githubusercontent.com/nektos/act/master/install.sh | sudo bash

# List workflows
act -l

# Run CI gate workflow
act -j quality-check
act -j test-coverage

# Run all jobs in workflow
act -W .github/workflows/ci-gate.yml

# Dry run (show what would execute)
act -n
```

### Secret Management for act
```bash
# Create .secrets file (NEVER commit this)
cat > .secrets << EOF
CENTRAL_USERNAME=your-username
CENTRAL_TOKEN=your-token
GPG_PRIVATE_KEY=$(cat ~/.gnupg/private.key | base64)
GPG_PASSPHRASE=your-passphrase
GPG_KEY_ID=your-key-id
EOF

# Use secrets file
act --secret-file .secrets

# Or pass individual secrets
act -s GPG_PRIVATE_KEY="$(cat ~/.gnupg/private.key | base64)"
```

### Common Workflows
```bash
# Test Java 26 matrix build
act -j build-verification --matrix java-version:26

# Test specific job with secrets
act -j deployment-ready --secret-file .secrets

# Run with custom container
act -j quality-check -P ubuntu-latest=catthehacker/ubuntu:act-latest

# Debug mode (verbose output)
act -j test-coverage -v

# Use specific GitHub event (push, pull_request, etc.)
act push --secret-file .secrets
```

### Troubleshooting act
```bash
# Pull latest Docker images first
act pull

# Clear Docker cache if jobs fail
docker system prune -f

# Check act version
act --version

# Test workflow syntax
act -n -j <job-name>
```

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
mvnd --stop                    # Stop daemon on auth issues
mvnd -X clean install          # Verbose output
cat target/surefire-reports/*  # Test details
echo $JAVA_HOME                # Verify Java path
ps aux | grep maven-proxy      # Check proxy running
```

---

## 📚 FILES YOU NEED

- `maven-proxy-auth.py` — Enterprise proxy solution
- `dtr-core/` — Core library
- `dtr-integration-test/` — Integration tests
- `.mvn/maven.config` — Build flags (--enable-preview)
- `pom.xml` — `<release>26</release>`
- `.github/workflows/ci-gate.yml` — GitHub Actions CI/CD pipeline
- `.secrets` — Local act secrets (NEVER commit)

---

**Last Updated:** March 13, 2026
**Branch:** feat/java-26-migration-github-actions
**Rule:** Always measure real, report real, use real DTR code.
