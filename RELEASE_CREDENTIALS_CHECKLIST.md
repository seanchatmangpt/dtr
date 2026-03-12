# DTR 2.0.0 — Credentials & Setup Checklist

Print this checklist and complete each item before attempting release.

---

## CRITICAL ITEMS (Release will fail without these)

### Item 1: Sonatype Central API Token

- [ ] Sonatype Central account exists (https://central.sonatype.org/)
- [ ] `io.github.seanchatmangpt.dtr` groupId verified
- [ ] API Token generated from account settings
- [ ] Token Username (OAuth username): ________________
- [ ] Token Password (OAuth password): ________________
- [ ] Credentials added to `~/.m2/settings.xml` under `<server><id>central</id>`
- [ ] Verified with: `grep -A 2 "central" ~/.m2/settings.xml`

**Test command:**
```bash
mvnd help:describe -Dplugin=org.sonatype.central:central-publishing-maven-plugin
# Should succeed if credentials are correct
```

---

### Item 2: GPG Signing Key

- [ ] GPG key generated or imported: `gpg --full-generate-key` or `gpg --import`
- [ ] Key ID (long form, 40 chars): ________________
- [ ] Key email address: ________________
- [ ] Key passphrase saved securely: ________________
- [ ] Public key published to keys.openpgp.org: `gpg --keyserver keys.openpgp.org --send-keys <KEY_ID>`
- [ ] Key published verified (check https://keys.openpgp.org/): ✓

**Test commands:**
```bash
# List keys
gpg --list-secret-keys
# Should show at least one [SC] key

# Test signing
echo "test" | gpg -ab
# Should prompt for passphrase and create signature
```

---

### Item 3: GitHub SSH Access

- [ ] SSH key configured for git@github.com
- [ ] SSH key tested: `ssh -T git@github.com`
- [ ] GitHub username: ________________
- [ ] Git user.name configured: ________________
- [ ] Git user.email configured: ________________

**Test command:**
```bash
ssh -T git@github.com
# Expected: "Hi <username>! You've successfully authenticated..."

git config --global user.name
git config --global user.email
# Both should return values
```

---

## CONFIGURATION VERIFICATION

### Maven Setup

- [ ] JAVA_HOME points to Java 25: `echo $JAVA_HOME`
  - Expected: `/usr/lib/jvm/java-25-openjdk-amd64`
- [ ] mvnd installed at `/opt/mvnd/bin/mvnd`
- [ ] Maven 4.0.0+ available
- [ ] `.mvn/maven.config` contains `--enable-preview`

**Verify:**
```bash
java -version
mvnd --version
cat .mvn/maven.config
```

### Repository Status

- [ ] Working directory is clean: `git status`
- [ ] All commits pushed: `git log --oneline | head -1` shows recent work
- [ ] Branch is up-to-date with remote: `git pull` shows no new changes
- [ ] No uncommitted changes: `git status` shows "nothing to commit"

**Verify:**
```bash
cd /home/user/doctester
git status
# Should show: "On branch <name>"
#              "nothing to commit, working tree clean"
```

### File Locations

- [ ] Root POM: `/home/user/doctester/pom.xml` ✓
- [ ] Core POM: `/home/user/doctester/dtr-core/pom.xml` ✓
- [ ] Maven settings: `~/.m2/settings.xml` ✓
- [ ] Maven local repo: `~/.m2/repository/` (has dtr-core)

---

## PRE-RELEASE CODE QUALITY

### Build & Tests

- [ ] Core module builds: `mvnd clean install -pl dtr-core -DskipTests`
  - Expected: `BUILD SUCCESS`
- [ ] All tests pass (or documented skip reason)
  - `mvnd test -pl dtr-core`
  - Expected: `BUILD SUCCESS` or integration tests skipped
- [ ] License headers present: `mvnd -P license license:check`
  - Expected: No license violations reported

### Documentation

- [ ] README.md updated with 2.0.0 features: ________________
- [ ] CHANGELOG.md exists with 2.0.0 changes: ________________
- [ ] MIGRATION guide (if breaking changes): ________________
- [ ] Javadoc generates without errors: `mvnd javadoc:jar -pl dtr-core`

---

## RELEASE PLANNING

### Version Numbers

- [ ] Current SNAPSHOT version: 1.1.12-SNAPSHOT
- [ ] Release version: 2.0.0
- [ ] Next dev version: 2.0.1-SNAPSHOT
- [ ] Git tag format: v2.0.0 (confirmed in pom.xml `<tagNameFormat>v@{project.version}</tagNameFormat>`)

### Release Timeline

- [ ] Scheduled release date: ________________
- [ ] Time allocated (60+ minutes): ________________
- [ ] Internet connection stable: ✓
- [ ] No network maintenance windows planned: ✓

---

## SONATYPE CENTRAL ACCOUNT DETAILS

Save these securely (in password manager if available):

| Item | Value | Saved? |
|------|-------|--------|
| Sonatype username | ________________ | [ ] |
| Sonatype password (API token) | ________________ | [ ] |
| GPG key ID | ________________ | [ ] |
| GPG passphrase | ________________ | [ ] |
| GitHub SSH key location | ~/.ssh/id_... | [ ] |

**Important:** Do not commit these to git or leave in shell history!

```bash
# Clear shell history
history -c
unset HISTFILE
```

---

## FINAL VERIFICATION (Day of Release)

### 1 Hour Before Release

- [ ] All items above completed and checked
- [ ] Verified Sonatype server is operational
- [ ] No critical GitHub outages reported
- [ ] Network connection stable
- [ ] Passphrase memorized or ready in secure manager

**Check server status:**
```bash
curl -s https://central.sonatype.com/api/v1/search?q=io.github.seanchatmangpt.dtr:junit | jq '.[-1].name'
# Should return a name, indicating API is working
```

### 15 Minutes Before Release

```bash
# Final build test
mvnd clean install -pl dtr-core -DskipTests
# Must succeed

# Verify git is ready
git status
# Must show "nothing to commit"

# Verify GPG
gpg --list-secret-keys
# Must show at least one key

# Verify credentials in settings
grep "central" ~/.m2/settings.xml
# Must show username and password
```

### Release Execution

**DO NOT PROCEED if any of the above checks fail.**

If all items above are verified:

```bash
# Run the release
mvnd -P release release:prepare release:perform

# OR non-interactive (CI/CD):
export GPG_PASSPHRASE="your-passphrase"
mvnd -P release release:prepare release:perform \
  -DreleaseVersion=2.0.0 \
  -DdevelopmentVersion=2.0.1-SNAPSHOT \
  -DskipTests \
  --batch-mode
```

**Monitor output for:**
```
[INFO] Executing: git tag -F ... v2.0.0
[INFO] Pushing release commits and tags...
[INFO] Uploading artifacts to https://central.sonatype.com/...
[SUCCESS] Artifacts published to Central!
```

---

## POST-RELEASE VERIFICATION

### Immediately After Release

- [ ] Command completed with `BUILD SUCCESS`
- [ ] No `[ERROR]` messages in output
- [ ] Git log shows release commit: `git log -1 --oneline`
  - Expected: `[maven-release-plugin] prepare release v2.0.0`
- [ ] Git tag created: `git tag | grep 2.0.0`
  - Expected: `v2.0.0`

### 5-10 Minutes After Release

- [ ] Check Maven Central search
  ```bash
  curl -s "https://central.sonatype.com/api/v1/search?q=io.github.seanchatmangpt.dtr:dtr-core:2.0.0"
  # Should return artifact record
  ```

- [ ] Verify signatures
  ```bash
  # Download artifact and check signature
  mvn dependency:get -Dartifact=io.github.seanchatmangpt.dtr:dtr-core:2.0.0
  ls -la ~/.m2/repository/org/r10r/dtr-core/2.0.0/
  # Should have: .jar, .pom, .jar.asc, .pom.asc
  ```

### GitHub Release (Optional but Recommended)

- [ ] Create GitHub Release
  - Title: `DocTester 2.0.0`
  - Tag: `v2.0.0`
  - Description: Copy from CHANGELOG_2.0.0.md
  - Assets: (none needed, JAR is on Maven Central)

**Command:**
```bash
gh release create v2.0.0 -t "DocTester 2.0.0" -n "Release 2.0.0: New features and improvements..."
```

---

## ROLLBACK PROCEDURE (If Something Goes Wrong)

If the release fails or needs to be rolled back:

```bash
# STOP here if unsure!

# 1. Delete local git tag
git tag -d v2.0.0

# 2. Delete remote git tag
git push origin --delete v2.0.0

# 3. Revert POM version to SNAPSHOT
git reset --hard origin/main

# 4. Contact Sonatype if artifacts were published
# See: https://central.sonatype.org/help/

# 5. Review error messages and MAVEN_CENTRAL_RELEASE_REPORT.md Section 13
```

---

## Sign-Off

**Release Manager Name:** ________________

**Date:** ________________

**I confirm:**
- [ ] All items in this checklist are completed
- [ ] I have read MAVEN_CENTRAL_RELEASE_REPORT.md
- [ ] I have read RELEASE_SETUP_GUIDE.md
- [ ] I understand the implications of releasing to Maven Central
- [ ] I am prepared to handle any issues that arise

**Signature/Approval:** ________________

---

## Contact & Resources

### If You Get Stuck

1. **Release Report:** Review `MAVEN_CENTRAL_RELEASE_REPORT.md` Section 13 (Troubleshooting)
2. **Setup Guide:** Review `RELEASE_SETUP_GUIDE.md` Step-by-step instructions
3. **Central Docs:** https://central.sonatype.org/publish/publish-maven/
4. **Release Plugin:** https://maven.apache.org/plugins/maven-release-plugin/
5. **GPG Help:** https://gnupg.org/documentation/

### Expected Support Response Times

- Sonatype support: 1-2 business days
- Maven Central search index: 2-15 minutes after publish
- GPG key server sync: 5-10 minutes

---

**This checklist is binding. Do not skip items.**

Good luck with the release!
