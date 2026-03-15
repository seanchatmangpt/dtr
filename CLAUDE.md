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

**Invariant:** The pipeline is the specification of done. Everything else is execution detail.
