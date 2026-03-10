# Contributing: Releasing

This guide is for maintainers with write access and Sonatype credentials.

## Versioning

DocTester uses [Semantic Versioning](https://semver.org/):

- **Patch** (`1.1.x`): Bug fixes, no API changes
- **Minor** (`1.x.0`): New features, backwards-compatible API additions
- **Major** (`x.0.0`): Breaking API changes (rare)

## Pre-release checklist

Before cutting a release:

1. **All tests pass:**
   ```bash
   mvnd clean verify
   ```

2. **`changelog.md` is up to date** — every notable change since the last release is listed

3. **No `SNAPSHOT` dependencies** in `pom.xml`

4. **Version in parent `pom.xml`** reflects the release (remove `-SNAPSHOT`)

## Release process

DocTester releases to Maven Central via Sonatype OSS.

### Step 1: Prepare the release

```bash
mvn release:clean
mvn release:prepare
```

This will:
- Prompt for the release version (e.g., `1.1.12`)
- Prompt for the next development version (e.g., `1.1.13-SNAPSHOT`)
- Update `pom.xml` files
- Create a git tag (e.g., `doctester-1.1.12`)
- Commit and push the version changes

### Step 2: Perform the release

```bash
mvn release:perform
```

This will:
- Check out the release tag
- Build and sign the artifacts
- Deploy to Sonatype staging repository

### Step 3: Promote from staging

1. Log in at [oss.sonatype.org](https://oss.sonatype.org)
2. Navigate to **Staging Repositories**
3. Find the `orgdoctester-XXXX` staging repository
4. Click **Close** — Sonatype runs validation checks
5. Once closed, click **Release** to promote to Maven Central

Maven Central sync takes ~10–30 minutes.

### Step 4: Back to development

```bash
git checkout develop  # or main, depending on branch strategy
git merge master
```

Verify the `pom.xml` version is now `1.1.13-SNAPSHOT`.

## GPG signing

Maven release plugin signs artifacts with GPG. You need:

1. A GPG key registered with Sonatype
2. The key in your local keyring
3. Maven configured with the key ID and passphrase:

```xml
<!-- ~/.m2/settings.xml -->
<settings>
    <servers>
        <server>
            <id>ossrh</id>
            <username>your-sonatype-username</username>
            <password>your-sonatype-password</password>
        </server>
    </servers>
    <profiles>
        <profile>
            <id>ossrh</id>
            <properties>
                <gpg.keyname>YOUR_KEY_ID</gpg.keyname>
                <gpg.passphrase>YOUR_PASSPHRASE</gpg.passphrase>
            </properties>
        </profile>
    </profiles>
    <activeProfiles>
        <activeProfile>ossrh</activeProfile>
    </activeProfiles>
</settings>
```

## Verify the release

After Maven Central sync:

```bash
# Verify the artifact is available
mvn dependency:get \
    -Dartifact=org.doctester:doctester-core:1.1.12 \
    -Ddest=/tmp/doctester-verify.jar
```

Check the [Maven Central search](https://search.maven.org/artifact/org.doctester/doctester-core) page.

## Post-release

1. Create a GitHub Release with the tag and changelog excerpt
2. Update the version in `README.md` and `docs/index.md` if they reference a specific version
3. Announce on any relevant channels
