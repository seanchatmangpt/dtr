# Contributing: Releasing

This guide is for maintainers with write access, a GPG key, and Maven Central credentials.

---

## Versioning

DTR uses [Semantic Versioning](https://semver.org/):

- **Patch** (`2.6.x`): Bug fixes, no API changes
- **Minor** (`2.x.0`): New `say*` methods or backwards-compatible API additions
- **Major** (`x.0.0`): Breaking API changes (rare; requires migration guide)

---

## Prerequisites

### 1. GPG Key

Artifacts must be GPG-signed for Maven Central.

1. Generate a key if you do not have one:
   ```bash
   gpg --full-generate-key
   ```
2. Publish the public key to `keys.openpgp.org`:
   ```bash
   gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
   ```
3. Note your key ID:
   ```bash
   gpg --list-secret-keys --keyid-format LONG
   ```

### 2. Maven Central Credentials

DTR 2.5.0+ uses the **Central Publishing Maven Plugin** (v0.6.0), not the legacy Sonatype OSS workflow.

Add credentials to `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_CENTRAL_TOKEN_USERNAME</username>
      <password>YOUR_CENTRAL_TOKEN_PASSWORD</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>release</id>
      <properties>
        <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
        <gpg.passphrase>YOUR_PASSPHRASE</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>release</activeProfile>
  </activeProfiles>
</settings>
```

GPG uses loopback pinentry for non-interactive signing during the build. If your GPG agent does not support loopback, add to `~/.gnupg/gpg-agent.conf`:
```
allow-loopback-pinentry
```

---

## Pre-Release Checklist

Before cutting a release:

1. **All tests pass:**
   ```bash
   mvnd clean test
   ```
   All 325+ tests in `dtr-core` and `dtr-integration-test` must pass.

2. **`CHANGELOG.md` is up to date** — every notable change since the last release is documented under the new version heading.

3. **No `-SNAPSHOT` dependencies** — verify all `pom.xml` files reference release versions only.

4. **Version updated in all `pom.xml` files** — parent and all modules must share the same release version.

5. **`CLAUDE.md` version reference updated** — the version line at the top of `CLAUDE.md` reflects the new release.

---

## Version Bump

Update the version in every `pom.xml` (parent + modules):

```bash
mvnd versions:set -DnewVersion=2.7.0 -DgenerateBackupPoms=false
```

Verify:
```bash
grep -r '<version>' pom.xml dtr-core/pom.xml dtr-benchmarks/pom.xml dtr-integration-test/pom.xml
```

Commit the version bump:
```bash
git add -u
git commit -m "chore: bump version to 2.7.0"
```

---

## Build Verification

Run the full build against the release version to confirm nothing is broken:

```bash
mvnd clean test
```

Inspect integration test output:
```bash
cat dtr-integration-test/target/docs/test-results/PhDThesisDocTest.md
```

---

## Deploy to Maven Central

Activate the `release` profile, which enables GPG signing and the Central Publishing Maven Plugin:

```bash
mvnd -P release -DskipTests clean deploy
```

This builds, signs, and uploads artifacts to Maven Central's publishing endpoint. The plugin bundles and submits in one step — there is no separate staging promotion required (unlike the legacy Sonatype OSS workflow).

If you need to verify artifacts locally before deploying:

```bash
mvnd -P release -DskipTests clean package
ls dtr-core/target/*.jar
```

---

## Alternatively: release:prepare / release:perform

You can also use the Maven Release Plugin for a structured prepare/perform cycle:

```bash
mvnd -P release release:prepare
# Prompts for release version (e.g. 2.7.0) and next dev version (e.g. 2.7.1-SNAPSHOT)
# Creates a git tag and commits version changes

mvnd -P release release:perform
# Checks out the tag, builds, signs, and deploys
```

---

## Maven Central Propagation

After a successful deploy, allow 10–30 minutes for artifacts to appear in Maven Central search and mirrors.

Verify availability:
```bash
mvn dependency:get \
    -Dartifact=io.github.seanchatmangpt.dtr:dtr-core:2.7.0 \
    -Ddest=/tmp/dtr-verify.jar
```

Check the [Maven Central search page](https://search.maven.org/artifact/io.github.seanchatmangpt.dtr/dtr-core).

---

## Post-Release Steps

1. **Tag the release in git** (if not created by `release:prepare`):
   ```bash
   git tag -a v2.7.0 -m "Release 2.7.0"
   git push origin v2.7.0
   ```

2. **Create a GitHub Release** — attach the tag and paste the relevant `CHANGELOG.md` section as the release notes.

3. **Update version references** in `README.md`, `docs/index.md`, and `CLAUDE.md` to point to the new release version.

4. **Bump to next development version** in all `pom.xml` files:
   ```bash
   mvnd versions:set -DnewVersion=2.7.1-SNAPSHOT -DgenerateBackupPoms=false
   git add -u
   git commit -m "chore: begin development of 2.7.1-SNAPSHOT"
   ```

5. **Announce** on GitHub Discussions and any relevant channels.
