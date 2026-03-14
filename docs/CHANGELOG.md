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
