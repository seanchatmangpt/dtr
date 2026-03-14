# Changelog

All releases follow [Calendar Versioning](https://calver.org/): `YYYY.MINOR.PATCH`.

The year component resets the minor counter — `2026.7.0` to `2027.1.0` is not a breaking change.
Breaking changes are signaled by `@Deprecated` annotations with a minimum one-year removal window.

Use year-bounded Maven ranges: `[2026.1.0,2027)`.

---

## [2026.1.0](releases/2026.1.0.md) — 2026-03-14

- Initial CalVer release. Migrated from SemVer 2.6.0.
- Established YYYY.MINOR.PATCH scheme with calendar-year major boundary.
- Added Makefile semantic release interface: `make release-minor`, `make release-patch`.
- Added `scripts/bump.sh`, `scripts/release.sh`, `scripts/release-rc.sh`.
- RC builds route to GitHub Packages; final releases route to Maven Central.
