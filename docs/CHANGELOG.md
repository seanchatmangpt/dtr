# DTR Changelog

All releases follow CalVer **YYYY.MINOR.PATCH**.
See [docs/contributing/releasing.md](contributing/releasing.md) for the release process.

DTR uses Calendar Versioning (YYYY.MINOR.PATCH). The year component resets
the minor counter — 2026.7.0 to 2027.1.0 is not a breaking change. Breaking
changes are signaled by @Deprecated annotations with a minimum one-year removal
window. Use year-bounded Maven ranges: `[2026.1.0,2027)`.

---

## v2026.1.0 — 2026-03-14

- Migrated from SemVer to CalVer (YYYY.MINOR.PATCH)
- Introduced Make-based release control surface
- Added scripts/bump.sh with year-aware version derivation
- Added RC pipeline via publish-rc.yml → GitHub Packages
- Fixed javadoc plugin release (26→25), SCM tag (HEAD), re-enabled enforcer
- Simplified publish.yml to 3 jobs: build → deploy → release
