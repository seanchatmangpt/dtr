## v2026.2.1 — 2026-03-15

- feat: precondition validation - fail fast with clear error messages
- feat: say* API enhancements - sayDiff() and sayBreakingChange() methods
- feat: DX innovations - --dry-run flag and build cache optimization
- docs: final session log - 10-agent swarm complete, all blue ocean deliverables documented
- docs: final session log - all 10 agents delivered, comprehensive blue ocean strategy
- docs: session log - 10-agent swarm complete, all deliverables finalized
- docs: add Java 26 modernization master index
- docs: final swarm reports - all 10 agents complete with actionable findings
- docs: add Java 26 modernization executive summary
- docs: final swarm analysis - 9 of 10 agents complete
- docs: update session log with agent swarm analysis
- docs: update session log for validation work
- docs: add build fixes documentation and fix flaky benchmark test
- fix: reorder POM elements - parent before metadata, add relativePath
- fix: version mismatch in dtr-core module and set-version.sh script
- docs: update session log
- fix(ci): update GitHub Actions to use correct Maven Central publish goal
- fix: add explicit dependency versions for Maven Central deployment
- chore(ci): add Maven Central deployment metadata and flatten plugin
- docs: update session log
- docs: update session log
- docs: update session log
- docs: regenerate test documentation for v2026.2.0-rc.1
- docs: update session log
- chore(ci): simplify workflows - remove redundant CI gates
- docs: update session log
- docs: update session log
- feat: add workflow_dispatch to publish.yml for manual triggering
- fix(ci): disable Trivy scan and remove Java 21/22 builds
- fix(ci): handle partial SDKMAN installation on GitHub Actions

### Install

```xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2026.2.1</version>
</dependency>
```

Year-bounded range (recommended for libraries): `[2026.1.0,2027)`

## v2026.3.0 — 2026-03-14

- docs: update session log
- fix(ci): update GitHub Actions to use correct Maven Central publish goal
- fix: add explicit dependency versions for Maven Central deployment
- chore(ci): add Maven Central deployment metadata and flatten plugin
- docs: update session log
- docs: update session log
- docs: update session log

### Install

```xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2026.3.0</version>
</dependency>
```

Year-bounded range (recommended for libraries): `[2026.1.0,2027)`

## v2026.2.0 — 2026-03-14

- docs: regenerate test documentation for v2026.2.0-rc.1
- docs: update session log
- chore(ci): simplify workflows - remove redundant CI gates
- docs: update session log
- docs: update session log
- feat: add workflow_dispatch to publish.yml for manual triggering

### Install

```xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2026.2.0</version>
</dependency>
```

Year-bounded range (recommended for libraries): `[2026.1.0,2027)`

## v2026.1.0 — 2026-03-14

- docs: update javadoc extraction and test docs
- docs: update session log
- fix(ci): always clean reinstall SDKMAN to fix partial installation
- fix(ci): handle partial SDKMAN installation on GitHub Actions
- docs: update session log + generated docs
- fix(latex): RenderMachineLatex updates
- docs: update session log + RenderMachine classes
- fix(ci): update workflow secret names to match GitHub secrets
- docs: update session log
- docs: update generated test documentation
- docs: update generated documentation
- chore: remove temporary implementation doc
- docs: complete documentation and fix Maven Central config
- docs: update CHANGELOG with release history
- chore: clean up temporary reports and organize docs
- docs: add extended say* API reference guide (80/20)
- feat(validation): Maven Central publishing validation complete
- docs: final doc regeneration
- docs: regenerate javadoc and test docs
- feat(validation): environment-validation session results
- fix(config): add PATH env for non-interactive shells + new say* methods
- refactor(docs): CLAUDE.md from first principles — encode agent autonomy
- refactor(docs): condense CLAUDE.md to 50 LOC reference guide
- docs: add act compatibility and update documentation
- refactor(oracle): optimize Naive Bayes for <50µs/file risk scoring
- feat(oracle): add Clone + Debug derive to RiskScorer
- refactor(remediate): optimize Layer 4 diff generation and edit capacity
- refactor(remediate): optimize crop + similar + tempfile for <100µs/edit
- fix(oracle): remove nonexistent cache module declaration
- fix(bench): simplify cache benchmarks to isolate blake3_hash results

### Install

```xml
<dependency>
  <groupId>io.github.seanchatmangpt.dtr</groupId>
  <artifactId>dtr-core</artifactId>
  <version>2026.1.0</version>
</dependency>
```

Year-bounded range (recommended for libraries): `[2026.1.0,2027)`

# DTR Changelog

All releases follow CalVer **YYYY.MINOR.PATCH**.
See [docs/contributing/releasing.md](contributing/releasing.md) for the release process.

DTR uses Calendar Versioning (YYYY.MINOR.PATCH). The year component resets
the minor counter — 2026.7.0 to 2027.1.0 is not a breaking change. Breaking
changes are signaled by @Deprecated annotations with a minimum one-year removal
window. Use year-bounded Maven ranges: `[2026.1.0,2027)`.

---

## [v2026.1.0](releases/2026.1.0.md) — 2026-03-14

- Migrated from SemVer `2.6.0` to CalVer (YYYY.MINOR.PATCH)
- Established YYYY.MINOR.PATCH scheme with calendar-year major boundary
- Introduced Make-based release control surface: `make release-minor`, `make release-patch`
- Added `scripts/bump.sh` with year-aware version derivation and RC promotion
- Added `scripts/release.sh`, `scripts/release-rc.sh`, `scripts/changelog.sh`
- Added `scripts/set-version.sh` for direct version set (used by bump.sh and hotfix)
- RC builds route to GitHub Packages; final releases route to Maven Central
- Fixed SCM tag in pom.xml to `HEAD` (updated at release time by set-version.sh)
- Simplified publish.yml to classify → build → deploy/deploy-rc → release
