# DTR — Claude Code Quick Reference | 2026.1.0 | Java 26

## RELEASE (one command, no manual steps)
```bash
make release-minor   # new say* methods → YYYY.(N+1).0
make release-patch   # bug fix          → YYYY.MINOR.(N+1)
make release-year    # year boundary    → YYYY.1.0
```
Tag fires GitHub Actions → `mvnd verify` → `mvnd deploy -Prelease` → Maven Central.
**Never type a version. Never run `mvn deploy` directly.**

## GATE: `mvnd verify`
Design for CI, not localhost. CI uses Java 26 + `--enable-preview`, GPG loopback
pinentry, secrets from GitHub (`CENTRAL_USERNAME`, `CENTRAL_TOKEN`, `GPG_PRIVATE_KEY`,
`GPG_PASSPHRASE`, `GPG_KEY_ID`). No `~/.m2/settings.xml`. No TTY. No proxy.
Output must land in `target/docs/` — anywhere else is wrong.

## RULES
1. **Real code only.** No fakes, stubs, hardcoded numbers. Measure with `System.nanoTime()`.
2. **Real DTR CLI.** JUnit 5 + `DtrContext`. Never bypass `RenderMachine`.
3. **Toolchain:** Java 26 `/usr/lib/jvm/java-26-openjdk-amd64` · mvnd `/opt/mvnd/bin/mvnd`

## DX PIPELINE (Ψ→H→Λ→Ω) — run before declaring done
```bash
make dx        # Observatory → H-Guards → mvnd verify → Git
make dx-fast   # skip verify (Ψ+H+Ω only)
```
Receipt: `.claude/dx-receipt.json`. Stop hook blocks session on dirty git state.

## say* API
`sayNextSection` · `say` · `sayCode` · `sayTable` · `sayJson` · `sayWarning` ·
`sayNote` · `sayKeyValue` · `sayUnorderedList` · `sayOrderedList`

## CALVER: YYYY.MINOR.PATCH
Year = major (calendar owns it). `release-major` does not exist. Breaking changes
use `@Deprecated` with ≥1-year removal window. Range: `[2026.1.0,2027)`.

## KEY FILES
| Path | Purpose |
|------|---------|
| `Makefile` | All targets — `make help` |
| `scripts/dx.sh` | Five-phase validation pipeline |
| `.claude/hooks/` | Stop / PreToolUse / SessionStart hooks |
| `pom.xml` | Root Maven config, release profile, GPG |

**Invariant:** Human picks change type. Script owns version. Tag is the trigger. `mvnd verify` is the gate.
