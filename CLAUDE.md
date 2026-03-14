# DTR (Documentation Testing Runtime) — Claude Code Quick Reference

**Project:** Markdown documentation generator for Java 25 | **Version:** 2026.1.0

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
| Java | 25.0.2, `--enable-preview` | All preview syntax must compile under `--enable-preview` |
| GPG | loopback pinentry, key from `$GPG_PRIVATE_KEY` secret | No interactive passphrase prompts; no `~/.gnupg` assumed |
| Maven Central credentials | `$CENTRAL_USERNAME` / `$CENTRAL_TOKEN` secrets | Never reference `~/.m2/settings.xml` |
| Maven settings | Written by Actions at deploy time | No local settings assumed |
| Network | No corporate proxy | Don't generate proxy-dependent code paths |
| Enforcer | Java ≥ 25, Maven ≥ 4.0.0-rc-3 | These versions are hard requirements, not suggestions |

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
- NO simulation, NO fakes, NO hard-coded numbers
- Use actual DTR code (RenderMachine + say* methods)
- Measure with `System.nanoTime()` on real execution
- Report: metric + units + Java version + iterations + environment
- **Example:** "JEP 516: 78ns avg (10M accesses, 100 iter, Java 25.0.2)" NOT "6667x faster"

### 2. ALWAYS USE REAL DTR CLI
- JUnit 5 tests with `DtrContext`
- Output through RenderMachine rendering pipeline
- Never bypass with standalone generators

### 3. Toolchain (Non-Negotiable)
- Java 25: `/usr/lib/jvm/java-25-openjdk-amd64`
- mvnd 2.0.0+: `/opt/mvnd/bin/mvnd` (preferred over bare `mvn`)
- Flag: `--enable-preview` in `.mvn/maven.config`
- Maven wrapper: `./mvnw` (use this, not system mvn)

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

## JAVA 25 FEATURES (Use These)

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
```

All preview syntax requires `--enable-preview` at compile and runtime.
Both are configured in `.mvn/maven.config` and `maven-surefire-plugin`.

---

## BEFORE CODING

1. `java -version` → 25.0.2+
2. `mvnd --version` → 2.0.0+
3. `.mvn/maven.config` contains `--enable-preview`
4. Ask: **will this pass `mvnd verify` in CI?**

---

## TROUBLESHOOTING

```bash
mvnd verify                    # The gate — run this before anything else
mvnd --stop                    # Stop daemon if stale
mvnd -X clean verify           # Verbose — find the real failure
cat target/surefire-reports/*  # Test details
echo $JAVA_HOME                # Verify Java 25

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

---

**Last Updated:** March 14, 2026
**Branch:** claude/maven-central-publishing-X03Au
**Version:** 2026.1.0 (CalVer YYYY.MINOR.PATCH)
**Invariant:** One command releases. The pipeline is the specification of done.
