# Contributing: Releasing

This guide is for maintainers with push access to the repository.

---

## Version Scheme: CalVer YYYY.MINOR.PATCH

DTR uses Calendar Versioning. The version number is a temporal receipt.

| Component | Meaning | Rule |
|-----------|---------|------|
| `YYYY` | Year of release | Set by calendar. `2026` means this was the truth in 2026. |
| `MINOR` | Feature iteration within the year | Starts at 1. Resets to 1 on year boundary. |
| `PATCH` | Fix counter within a MINOR | Starts at 0. Resets to 0 on every MINOR bump. |

**`2026.7.2`** means: seventh feature release of 2026, second patch fix within it.

Year boundary is automatic. `scripts/bump.sh minor` reads `date +%Y` at bump time.
If the year has changed, MINOR resets to 1.

---

## The Only Release Interface

```bash
make release-minor   # new features, additive changes    → YYYY.(N+1).0
make release-patch   # bug fix, no API change            → YYYY.MINOR.(N+1)
make release-year    # explicit year boundary (January)  → YYYY.1.0

make release-rc-minor  # RC for a minor bump → YYYY.(N+1).0-rc.N
make release-rc-patch  # RC for a patch fix  → YYYY.MINOR.(N+1)-rc.N
```

**No human types a version number. No human does arithmetic.**
The only decision is: what kind of change is this?

---

## What Each Target Does

```
make release-minor
  └─ scripts/bump.sh minor
       reads CURRENT from pom.xml
       computes NEXT (year-aware)
       sed-updates all pom.xml files
       writes NEXT to .release-version
  └─ scripts/release.sh
       reads VERSION from .release-version
       runs scripts/changelog.sh → docs/CHANGELOG.md + docs/releases/VERSION.md
       git add pom.xml files + docs
       git commit "chore: release vVERSION"
       git tag -a vVERSION
       git push origin HEAD vVERSION
       → GitHub Actions fires (publish.yml)
       → mvnd verify
       → mvnd deploy -Prelease → Maven Central
       → gh release create vVERSION --generate-notes
```

---

## Release Candidates

RC builds go to GitHub Packages only. Maven Central receives final versions only.

```bash
# Create first RC
make release-rc-minor          # → v2026.3.0-rc.1

# If RC needs fixes, push code changes, then:
make release-rc-minor          # → v2026.3.0-rc.2 (N auto-increments from git tags)

# Promote to final when RC is good:
make release-minor             # → v2026.3.0 (strips -rc.N, publishes to Maven Central)
```

RC promotion (`make release-minor` from a `-rc.N` state) strips the RC suffix and
publishes the final version. The minor number is not incremented again — it was
already bumped when the RC was created.

---

## Breaking Changes

`release-major` does not exist. **The calendar owns the major version.**

Breaking API changes are handled by deprecation cycle:
1. Mark the old method `@Deprecated` in release `2026.X.0`
2. Include a migration guide in `docs/releases/2026.X.0.md`
3. Remove the method no earlier than `2027.1.0` (one full year of warning)

The year boundary IS the breaking change window. Downstream users who pin
`[2026.3.0,2027)` are protected by the range.

---

## Downstream Maven Ranges

Document this in README once:
```xml
<!-- Pinned to 2026 — protected from year-boundary changes -->
<version>[2026.3.0,2027)</version>

<!-- From specific release onward within 2026 -->
<version>[2026.3.0,2027)</version>
```

---

## Version Lifecycle

| Pattern | Status | Policy |
|---------|--------|--------|
| `2026.x.x-rc.*` | Candidate | GitHub Packages only |
| `2026.x.x` | Current | Maven Central |
| `2025.x.x` | Maintained | Security patches only, until Jan 2027 |
| `2024.x.x` | EOL | No patches |

---

## Prerequisites

GPG signing, Maven Central credentials, and GitHub Packages auth are injected by
GitHub Actions as secrets. There is no `~/.m2/settings.xml` required locally to
do a release. The release is triggered by the tag. The pipeline owns credentials.

To add a new maintainer GPG key, update `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`,
and `GPG_KEY_ID` in GitHub repository Settings → Secrets → Actions.
