# DTR (Documentation Testing Runtime) — Claude Code Quick Reference

**Project:** Markdown documentation generator for Java 25 | **Version:** 2.6.0

---

## THE PIPELINE IS THE SPECIFICATION

This is the most important section. Read it before writing any code.

### The Armstrong Invariant: One-Command Release

The only release path is:

```bash
make release-patch   # bug fix, no API change
make release-minor   # new say* methods, backward compatible
make release-major   # breaking API change
```

The human decides the **type of change**. That is the only decision
a human is qualified to make that a script cannot.
The version number is a mechanical consequence — `scripts/bump-version.sh`
owns the arithmetic, `scripts/release.sh` owns the tag and push,
GitHub Actions owns the signing and Maven Central publish.

No human ever types a version number. No version number is ever wrong.

`mvn deploy` as a manual step is **forbidden**. That path is closed.
`make release VERSION=x.y.z` is **forbidden** — it requires a human to
know the current version and do arithmetic. That is unnecessary cognitive
load and a drift surface.
If you find yourself suggesting either — stop. You are breaking the invariant.
The tag is the trigger. The trigger is the specification of done.

### The Gate: `mvnd verify`

Everything generated in this project — DTR tests, benchmarks, JOTP code,
documentation, generated sources — must pass `mvnd verify` in CI.
Not locally. In CI.

CI environment facts that are load-bearing:
- Java 25 + `--enable-preview` (enforced by pom.xml compiler plugin)
- GPG signing uses `--pinentry-mode loopback` (non-interactive, no TTY)
- `~/.gnupg/gpg-agent.conf` must contain `allow-loopback-pinentry`
- Credentials come from **GitHub secrets**, never from `~/.m2/settings.xml`
- Required secrets: `CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`,
  `GPG_PASSPHRASE`, `GPG_KEY_ID`
- Maven cache: `.m2/repository` in `${{ github.workspace }}`

If `mvnd verify` fails, nothing publishes. Design for CI, not the laptop.

### The GPG Loopback Scar

The `--pinentry-mode loopback` configuration is not a workaround.
It is load-bearing context from real CI failure.
It tells you: this pipeline has been through real failure and survived.
Do not remove it. Do not "simplify" it. It is correct.

### The Output Contract

DTR tests must produce artifacts where the Actions pipeline expects them:

| Output | Path |
|--------|------|
| Documentation | `target/docs/` |
| Blog posts | `target/blog/` |
| PDF/LaTeX | `target/pdf/` |
| Test results | `target/docs/test-results/` |
| Surefire reports | `target/surefire-reports/` |

A DTR test that produces output at any other path is wrong.
An agent that doesn't know this generates tests that produce output nowhere.

### Reasoning About Dates as Triggers, Not Deadlines

March 23 is not "when JOTP is ready."
March 23 is when `make release VERSION=1.0` fires.

The right question is always:
> **What needs to be true for `make release-minor` (or patch/major) to
> succeed in a GitHub Actions runner and produce a complete receipted release?**

That question has a finite, enumerable answer:
1. `mvnd verify` passes on all modules
2. All DTR test output lands in `target/docs/`
3. GPG key is loaded and loopback-capable
4. `CENTRAL_USERNAME` and `CENTRAL_TOKEN` secrets are set
5. The `release` Maven profile signs, packages, and publishes via
   `central-publishing-maven-plugin`
6. GitHub Release is created with `gh release create`

If those six things are true, the release succeeds. That is the scope.

---

## ⚡ CRITICAL RULES

### 1. REAL CODE, REAL MEASUREMENTS ONLY
- ❌ NO simulation, NO fakes, NO hard-coded numbers
- ✅ Use actual DTR code (RenderMachine + say* methods)
- ✅ Measure with System.nanoTime() on real execution
- ✅ Report: metric + units + Java version + iterations + environment
- **Example:** "JEP 516: 78ns avg (10M accesses, 100 iter, Java 25.0.2)" NOT "6667x faster"

### 2. ALWAYS USE REAL DTR CLI
- ✅ JUnit 5 tests with DtrContext
- ✅ Output through RenderMachine rendering pipeline
- ❌ Never bypass with standalone generators

### 3. Toolchain (Non-Negotiable)
- Java 25: `/usr/lib/jvm/java-25-openjdk-amd64`
- Maven 4.0.0-rc-5: `/opt/apache-maven-4.0.0-rc-5/bin/mvn`
- mvnd 2.0.0+: `/opt/mvnd/bin/mvnd` (preferred)
- Flag: `--enable-preview` in `.mvn/maven.config`

---

## 🔧 QUICK BUILD

```bash
make test              # run unit tests (mvnd verify)
make verify            # compile + test + checks
make tag VERSION=2.7.0 # bump version, commit, tag (then push to release)

# Run a specific DTR test
mvnd test -pl dtr-integration-test -Dtest=PhDThesisDocTest

# Check output
ls target/docs/test-results/
cat target/docs/test-results/PhDThesisDocTest.md
```

---

## 🌐 MAVEN PROXY SOLUTION

**Problem:** "too many authentication attempts" from Maven Central (local dev only)

**Fix:**
```bash
python3 maven-proxy-auth.py &
```

Listens on 127.0.0.1:3128, handles HTTPS CONNECT tunneling, injects
Proxy-Authorization header automatically.

**This is a local dev problem only.** CI uses direct Maven Central access
with token credentials from GitHub secrets.

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

The test must pass `mvnd verify` in CI before it is correct.
Output anywhere other than `target/docs/` is wrong.

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

## 🚀 JAVA 25 FEATURES (Use These)

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
```

---

## ✅ BEFORE CODING

Ask this first: **"Does this pass `mvnd verify` in CI?"**

Then:
1. `java -version` → 25.0.2+
2. `mvnd --version` → Maven 4.0.0-rc-5+
3. `.mvn/maven.config` contains `--enable-preview`
4. Output goes to `target/docs/` — not anywhere else
5. No manual deploy steps — the tag triggers everything
6. Remember: **REAL CODE + REAL MEASUREMENTS + REAL DOCTESTER CLI**

---

## 🔍 TROUBLESHOOTING

```bash
mvnd --stop                    # Stop daemon on auth issues
mvnd -X clean install          # Verbose output
cat target/surefire-reports/*  # Test details
echo $JAVA_HOME                # Verify Java path
ps aux | grep maven-proxy      # Check proxy running (local dev)
make check                     # Verify entire toolchain
```

---

## 📚 FILES YOU NEED

- `Makefile` — **start here**: `make help` shows all targets
- `scripts/current-version.sh` — reads version from pom.xml (no Maven plugin)
- `scripts/bump-version.sh` — updates all pom.xml files, owns the arithmetic
- `scripts/release.sh` — commits, tags, pushes; fires the pipeline
- `.github/workflows/publish.yml` — tag-triggered release to Maven Central
- `.github/workflows/ci.yml` — `mvnd verify` gate for every push/PR
- `maven-proxy-auth.py` — local dev proxy (not used in CI)
- `dtr-core/` — Core library
- `dtr-integration-test/` — Integration tests
- `.mvn/maven.config` — Build flags (--enable-preview)
- `pom.xml` — `<release>25</release>`, release profile, GPG config

---

**Last Updated:** March 14, 2026
**Branch:** claude/setup-makefile-github-actions-M0Kiz
**Invariant:** The human decides the change type. The script derives the number. The tag is the trigger. The pipeline is the specification. mvnd verify is the gate.
