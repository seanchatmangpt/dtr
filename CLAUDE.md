# DTR — Documentation Testing Runtime | Operating Manual for Claude

## MISSION

DTR transforms Java documentation into executable tests that prove code works as documented. Release via `make release-TYPE` — one command, no manual steps, CI owns the pipeline.

---

## INVARIANTS (Non-Negotiable)

1. **Calendar owns the year** — 2026.X.Y means "released in 2026". No manual major versions.
2. **Human owns semantics** — Human decides: "This is a feature" (minor) vs "This is a fix" (patch).
3. **Script owns arithmetic** — `make release-patch` computes YYYY.MINOR.(N+1). Humans never type versions.
4. **Tag triggers release** — Only `git tag v<VERSION>` fires CI. No local `mvn deploy`.
5. **Tests are the contract** — Code passes `mvnd verify` in CI before artifact exists.

---

## AGENT TEAM AUTHORITY (This is yours)

### ✅ Decide Alone
- **Design approach:** Choose architecture, split work across 5+ agents, parallelize
- **Scope & tooling:** Pick Rust vs Java, launch agents for optimization, select strategies
- **Code quality:** Validate via testing before declaring done (don't ask permission for this)
- **Ask forgiveness later:** Implement iteratively; get feedback in next session if needed

### ❌ Escalate Immediately
- **Ambiguous requirements** — "Make this faster" without target. Ask what "done" means.
- **Conflicting principles** — Real code vs quick stub. Ask which takes priority.
- **Destructive git ops** — Force push, hard reset, branch deletion. Always ask first.
- **Release decisions** — User picks minor/patch/year. You execute (not decide).
- **Security or legal** — Credentials, license changes, data handling. Always escalate.

---

## VERIFICATION FRAMEWORK (Before declaring done)

**Work is DONE only when:**
1. **Tests pass** — `cargo test` and `mvnd verify` both succeed (locally, in CI)
2. **Validation is explicit** — Benchmarks, stress tests, or real-world measurement (not estimates)
3. **Code is real** — No stubs, no synthetic data, no "this will probably work" assumptions
4. **TODO list complete** — Track what needs validation; don't defer validation
5. **CI gate passes** — `mvnd verify --enable-preview` in headless runner with secrets from GitHub

**Not validated? → Add to TODO list, don't declare done.**

---

## DECISION FRAMEWORK

### Scope: What belongs in DTR?
**Yes:** Java 26 features, documentation, testing, benchmarking, Maven/GitHub Actions integration
**No:** Python, build systems unrelated to Maven, deployment to non-Maven-Central targets

### Quality: Real code only
- Measure with `System.nanoTime()`, not estimates
- Report: metric + units + Java version + iterations + environment
- Example: "JEP 516: 78ns avg (10M accesses, 100 iter, Java 26.ea.13)" NOT "6667x faster"

### Complexity: Favor simplicity
- No premature optimization or abstraction
- Three similar lines of code > One premature utility
- One command does one thing well > Many commands with options

### Tools: Use the right tool
- Rust for performance-critical scanning/analysis → Agent teams for parallelism
- Java for DTR core → Unit tests as documentation
- Benchmarking: Criterion.rs (.xx microsecond precision), not rough estimates

---

## RELEASE SEMANTICS (Human chooses type, script derives version)

```bash
make release-minor   # "New capability" or "new say* method"  → YYYY.(N+1).0
make release-patch   # "Bug fix" or "dependency update"      → YYYY.MINOR.(N+1)
make release-year    # "Explicit year boundary" (Jan 1)     → YYYY.1.0
```

**Flow:**
1. Human runs `make release-TYPE`
2. Script: Bumps version, commits, tags `v<VERSION>`, pushes
3. GitHub Actions fires on tag → `mvnd verify` → `mvnd deploy -Prelease` → Maven Central
4. Artifact exists or nothing publishes (all-or-nothing)

---

## CI GATE: `mvnd verify`

**Design for CI, not localhost:**
- Java: 26.ea.13 with `--enable-preview` (configured in `.mvn/maven.config`)
- Secrets: GitHub-provided (`CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`, etc.)
- No assumptions: No `~/.m2/settings.xml`, no TTY, no corporate proxy, no loopback file system
- Output: Must land in `target/docs/` — anywhere else means release is incomplete
- Passes gate → artifact publishable. Fails gate → nothing publishes.

---

## BUILD CACHE STRATEGY (Smart Invalidation)

**mvnd persistent cache reduces build time 30-40s → 8-12s via incremental compilation.**

Cache persists in `.mvnd/` directory across sessions. Smart invalidation detects environment changes:

```bash
# Enable cache via .mvn/maven.config:
# -Dmaven.compiler.parameters=true    # faster incremental compilation
# (mvnd defaults to .buildDir=.mvnd)

# View cache status:
make cache-stats                         # Show cache status, metrics, baseline

# Typical build cycle:
mvnd clean verify                        # First run: 35-40s (cold cache)
mvnd verify                              # Second run: 8-10s (warm cache)
mvnd verify                              # Nth run: 8-10s (cache hit)

# Cache auto-invalidates (and triggers rebuild) when:
# - pom.xml changes (MD5 hash mismatch)
# - .mvn/maven.config changes (MD5 hash mismatch)
# - Java version changes (detected via java -version)
# - mvnd version changes (detected via mvnd --version)

# Force fresh build (if needed):
rm -rf .mvnd && mvnd clean verify        # Expensive but clears all cached state
```

**Compound effect:** 2.5-3 min saved per build = 12-15 faster iterations/hour = 96-120 extra builds/day.

---

## TOOLCHAIN (Verified)

| Tool | Version | Location |
|------|---------|----------|
| Java | 26.ea.13+ | `/usr/lib/jvm/java-26-openjdk-amd64` or SDKMAN |
| Maven | 4.0.0-rc-3+ | `/opt/apache-maven/` or `brew install maven` |
| mvnd | 2.0.0+ | `/opt/mvnd/bin/mvnd` (preferred for CI speed) |
| Rust | Latest stable | `rustup` (for scanner/cache/oracle layers) |

**Before coding:** `java -version` + `mvnd --version`. Both must be ≥ required versions.

---

## KEY DECISION: Team Structure

**Pattern:** Launch 5+ agents for major work (optimization, new layers, refactoring)
- Each agent owns a subsystem
- Agents run in parallel
- Sync results → one commit per layer
- Validation happens before integration

---

## say* API Reference

Complete reference for all 50+ documentation methods available on `DtrContext`.

### Core API

| Method | Signature | Description |
|--------|-----------|-------------|
| `say` | `void say(String text)` | Render a paragraph (supports markdown) |
| `sayNextSection` | `void sayNextSection(String headline)` | H1 heading with TOC entry |
| `sayRaw` | `void sayRaw(String rawMarkdown)` | Inject raw markdown/HTML bypassing formatting |

### Formatting & Structure

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayTable` | `void sayTable(String[][] data)` | 2D array → table (first row = headers) |
| `sayCode` | `void sayCode(String code, String language)` | Fenced code block with syntax highlighting |
| `sayWarning` | `void sayWarning(String message)` | GitHub-style [!WARNING] alert block |
| `sayNote` | `void sayNote(String message)` | GitHub-style [!NOTE] alert block |
| `sayKeyValue` | `void sayKeyValue(Map<String, String> pairs)` | 2-column metadata table |
| `sayUnorderedList` | `void sayUnorderedList(List<String> items)` | Bullet list |
| `sayOrderedList` | `void sayOrderedList(List<String> items)` | Numbered list |
| `sayJson` | `void sayJson(Object object)` | Pretty-printed JSON in code block |
| `sayAssertions` | `void sayAssertions(Map<String, String> assertions)` | Table of assertions (Check ✓ / Result) |

### Cross-References

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayRef` | `void sayRef(DocTestRef ref)` | Link to another DocTest section |
| `sayRef` | `void sayRef(Class<?> docTestClass, String anchor)` | Link to specific test class + anchor |
| `sayCite` | `void sayCite(String citationKey)` | BibTeX citation reference |
| `sayCite` | `void sayCite(String citationKey, String pageRef)` | BibTeX citation with page number |
| `sayFootnote` | `void sayFootnote(String text)` | Footnote content |

### Code Model (Reflection-Based)

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayCodeModel` | `void sayCodeModel(Class<?> clazz)` | Class structure: sealed hierarchy, methods, signatures |
| `sayCodeModel` | `void sayCodeModel(Method method)` | Method structure with Java 26 Code Reflection |
| `sayCallSite` | `void sayCallSite()` | Document caller location (class, method, line) via StackWalker |
| `sayAnnotationProfile` | `void sayAnnotationProfile(Class<?> clazz)` | All annotations on class and methods |
| `sayClassHierarchy` | `void sayClassHierarchy(Class<?> clazz)` | Inheritance tree (superclass + interfaces) |
| `sayStringProfile` | `void sayStringProfile(String text)` | Word count, line count, Unicode metrics |
| `sayReflectiveDiff` | `void sayReflectiveDiff(Object before, Object after)` | Field-by-field comparison table |

### Java 26 Code Reflection (JEP 516)

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayControlFlowGraph` | `void sayControlFlowGraph(Method method)` | Mermaid flowchart of method CFG (requires @CodeReflection) |
| `sayCallGraph` | `void sayCallGraph(Class<?> clazz)` | Mermaid graph of method-to-method calls |
| `sayOpProfile` | `void sayOpProfile(Method method)` | Operation count table from Code Reflection IR |

### Benchmarking

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayBenchmark` | `void sayBenchmark(String label, Runnable task)` | Measure with default 50 warmup / 500 measure rounds |
| `sayBenchmark` | `void sayBenchmark(String label, Runnable task, int warmupRounds, int measureRounds)` | Measure with explicit round counts |

### Mermaid Diagrams

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayMermaid` | `void sayMermaid(String diagramDsl)` | Raw Mermaid diagram (flowchart, sequence, etc.) |
| `sayClassDiagram` | `void sayClassDiagram(Class<?>... classes)` | Auto-generated classDiagram from reflection |

### Coverage & Quality

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayDocCoverage` | `void sayDocCoverage(Class<?>... classes)` | Report which public methods were documented |

### 80/20 Low-Hanging Fruit

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayEnvProfile` | `void sayEnvProfile()` | Java version, OS, processors, heap, timezone, DTR version |
| `sayRecordComponents` | `void sayRecordComponents(Class<? extends Record> recordClass)` | Record schema table (names, types, annotations) |
| `sayException` | `void sayException(Throwable t)` | Exception type, message, cause chain, top 5 frames |
| `sayAsciiChart` | `void sayAsciiChart(String label, double[] values, String[] xLabels)` | Horizontal bar chart with Unicode blocks |

### Bonus Features

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayContractVerification` | `void sayContractVerification(Class<?> contract, Class<?>... implementations)` | Interface implementation coverage (✓ direct, ↗ inherited, ❌ missing) |
| `sayEvolutionTimeline` | `void sayEvolutionTimeline(Class<?> clazz, int maxEntries)` | Git log --follow for class source file (commit, date, author, subject) |
| `sayJavadoc` | `void sayJavadoc(Method method)` | Extracted Javadoc from docs/meta/javadoc.json |

### Slide/Blog-Specific Methods

| Method | Signature | Description |
|--------|-----------|-------------|
| `saySlideOnly` | `void saySlideOnly(String text)` | Text appears only in slide deck, not docs |
| `sayDocOnly` | `void sayDocOnly(String text)` | Text appears only in docs, not slides |
| `saySpeakerNote` | `void saySpeakerNote(String text)` | Presenter notes (slides only) |
| `sayHeroImage` | `void sayHeroImage(String altText)` | Hero image section |
| `sayTweetable` | `void sayTweetable(String text)` | Social-media quote box |
| `sayTldr` | `void sayTldr(String text)` | TL;DR summary box |
| `sayCallToAction` | `void sayCallToAction(String url)` | CTA button/link |

### Assertion Combos (DtrTest Base Class)

| Method | Signature | Description |
|--------|-----------|-------------|
| `sayAndAssertThat` | `<T> void sayAndAssertThat(String label, T actual, Matcher<? super T> matcher)` | Assert + document in one call (generic) |
| `sayAndAssertThat` | `void sayAndAssertThat(String label, long actual, Matcher<Long> matcher)` | Assert + document (long primitive) |
| `sayAndAssertThat` | `void sayAndAssertThat(String label, int actual, Matcher<Integer> matcher)` | Assert + document (int primitive) |
| `sayAndAssertThat` | `void sayAndAssertThat(String label, boolean actual, Matcher<Boolean> matcher)` | Assert + document (boolean primitive) |

---

## Extended say* API Reference (80/20 Guide)

### Quick Start: When to Use Each Method

#### 🎯 Core Methods (Use These 80% of the Time)
- **`say(text)`** - Simple paragraphs, most common use case
- **`sayCode(code, language)`** - Code blocks with syntax highlighting
- **`sayTable(data)`** - Structured data tables
- **`sayWarning(message)`** - Important warnings
- **`sayNote(message)`** - Additional context
- **`sayKeyValue(pairs)`** - Metadata, configuration, key facts

#### 📊 Formatting & Structure
- **`sayUnorderedList(items)`** - Bullet points for lists
- **`sayOrderedList(items)`** - Numbered sequences
- **`sayJson(object)`** - JSON data dumps
- **`sayAssertions(assertions)`** - Test results summary

#### 🔗 Cross-References
- **`sayRef(otherClass, anchor)`** - Link to other documentation sections
- **`sayCite(citationKey)`** - Academic citations

#### 🛠️ Code Analysis (Advanced)
- **`sayCodeModel(clazz)`** - Class structure overview
- **`sayCallSite()`** - Current execution context
- **`sayEnvProfile()`** - System environment snapshot

#### 🎨 Presentation-Specific
- **`saySlideOnly(text)`** - Content for slides only
- **`sayDocOnly(text)`** - Content for docs only
- **`saySpeakerNote(text)`** - Presenter notes
- **`sayTweetable(text)`** - Social media quotes
- **`sayTldr(text)`** - Summary boxes
- **`sayHeroImage(altText)`** - Featured images
- **`sayCallToAction(url)`** - Clickable links

#### ✅ Testing & Validation
- **`sayAndAssertThat(label, actual, matcher)`** - Assert + document in one call

### Method Selection Decision Tree

```
Are you documenting simple text?
  ├── Yes → use `say(text)`
  └── No → Continue

Is it code or configuration?
  ├── Yes → use `sayCode(code, language)` or `sayJson(object)`
  └── No → Continue

Is it structured data?
  ├── Yes → use `sayTable(data)` for tables or `sayKeyValue(pairs)` for metadata
  └── No → Continue

Is it a list?
  ├── Yes → `sayUnorderedList(items)` or `sayOrderedList(items)`
  └── No → Continue

Is it important context?
  ├── Yes → `sayNote(message)` for notes
  └── Is it a warning?
       ├── Yes → `sayWarning(message)`
       └── No → Continue

Are you testing something?
  ├── Yes → `sayAndAssertThat(label, actual, matcher)`
  └── No → Continue

Are you creating slides/blogs?
  ├── Yes → Choose from presentation-specific methods above
  └── No → Use core methods
```

### Common Patterns

#### Basic Documentation
```java
say("This is a simple paragraph of documentation.");
sayCode("int x = 42;", "java");
```

#### Warning and Notes
```java
sayWarning("This feature is experimental and may change.");
sayNote("For more details, see the full API documentation.");
```

#### Data Tables
```java
sayTable(new String[][]{
    {"Feature", "Status", "Priority"},
    {"REST API", "✓ Implemented", "High"},
    {"GraphQL", "🔄 In Progress", "Medium"}
});
```

#### Testing with Documentation
```java
sayAndAssertThat("HTTP Status", response.getStatusCode(), equalTo(200));
```

#### Cross-References
```java
sayRef(OtherDocumentation.class, "user-registration");
sayCite("smith2023");
```

**Invariant:** The pipeline is the specification of done. Everything else is execution detail.

---

## DX IMPROVEMENTS: --dry-run Safety Flag

### Problem
Developers can accidentally push to `main` with a single `make release-minor` command. There's no confirmation or preview of what will happen before git operations execute.

### Solution: --dry-run flag
Preview release operations without executing any git changes. All destructive operations (git push, git tag, git commit) are wrapped with dry-run checks that print "Would..." messages instead of executing.

### Usage

**Preview a minor release:**
```bash
make release-minor ARGS="--dry-run"
```

**Output (example):**
```
==> DRY RUN: Final Release v2026.3.0

Would stage the following files:
./pom.xml ./dtr-core/pom.xml ./dtr-cli/pom.xml
  docs/CHANGELOG.md
  docs/releases/2026.3.0.md

Would execute:
  git add [pom files, changelog, release docs]
  git commit -m "chore: release v2026.3.0"
  git tag -a "v2026.3.0" -m "Release v2026.3.0"
  git push origin HEAD "v2026.3.0"

Would trigger on GitHub:
  GitHub Actions: verify → sign → publish to Maven Central
  Track at: https://github.com/seanchatmangpt/dtr/actions
```

**Preview release candidates:**
```bash
make release-rc-minor ARGS="--dry-run"
make release-rc-patch ARGS="--dry-run"
```

### Implementation Details
- **release.sh** — Accepts `--dry-run` flag; prints git operations without executing
- **release-rc.sh** — Accepts `--dry-run` flag; prints RC operations without executing
- **Makefile** — Propagates `ARGS` variable to release scripts; updated help text
- **Exit code 0** — Both dry-run and real operations return success on completion
- **No side effects** — Dry-run mode removes `.release-version` temp file even in preview mode

### Safety First
This feature prevents "oops, I pushed to main" accidents by making the release impact transparent before execution. Use `--dry-run` to verify the release plan, then run the actual release without the flag.
