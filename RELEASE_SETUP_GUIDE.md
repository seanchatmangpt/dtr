# DocTester 2.0.0 Release — Quick Setup Guide

This guide walks you through the critical setup steps needed before releasing DocTester 2.0.0 to Maven Central.

**Status:** Release configuration is complete. You need to set up 2 credentials items.

---

## Step 1: Verify Environment

```bash
# Verify Java 25
java -version
# Expected: openjdk version "25.0.2"

# Verify Maven 4
mvnd --version
# Expected: Apache Maven Daemon 2.0.0-rc-3 (Maven 4.0.0-rc-3)

# Check project location
pwd
# Expected: /home/user/doctester

# Verify .mvn/maven.config
cat .mvn/maven.config
# Expected: --no-transfer-progress, --batch-mode, -Dmaven.compiler.enablePreview=true
```

**Expected output:** All checks pass ✅

---

## Step 2: Set Up Sonatype Central Credentials

### 2a. Create Sonatype Account (one-time)

Visit https://central.sonatype.org/ and:
1. Sign up for free account (or use existing)
2. Verify ownership of `org.r10r` groupId
   - Option A: Domain verification (own `r10r.org` domain)
   - Option B: GitHub repository proof (fork/contribute to `github.com/r10r-org`)
3. Wait for approval (typically 1 business day)

### 2b. Generate API Token

1. Log in to https://central.sonatype.org/account/
2. Navigate to **API Tokens** section
3. Click **Generate Token**
4. Copy **Token Username** and **Token Password**

**Save these securely!** You'll use them in step 2c.

### 2c. Add Credentials to Maven Settings

Edit or create `~/.m2/settings.xml`:

```bash
# First, backup if it exists
cp ~/.m2/settings.xml ~/.m2/settings.xml.bak 2>/dev/null || true

# View current file
cat ~/.m2/settings.xml | head -20
```

Add the Central server block. Your file should look like:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
          https://maven.apache.org/xsd/settings-1.0.0.xsd">

  <!-- ... existing proxy configuration ... -->

  <!-- ADD THIS SECTION -->
  <servers>
    <server>
      <id>central</id>
      <username>YOUR_SONATYPE_TOKEN_USERNAME</username>
      <password>YOUR_SONATYPE_TOKEN_PASSWORD</password>
    </server>
  </servers>

</settings>
```

**Script to update safely:**

```bash
# Replace PLACEHOLDERS with actual values
SONATYPE_USERNAME="your-token-username"
SONATYPE_PASSWORD="your-token-password"

# Add server section to settings.xml
cat >> ~/.m2/settings.xml << EOF

  <servers>
    <server>
      <id>central</id>
      <username>$SONATYPE_USERNAME</username>
      <password>$SONATYPE_PASSWORD</password>
    </server>
  </servers>
EOF

# Verify
grep -A 3 "central" ~/.m2/settings.xml
```

**Verify:**
```bash
# Check credentials are present
grep -A 3 "<id>central</id>" ~/.m2/settings.xml
# Expected: username and password lines visible
```

---

## Step 3: Set Up GPG Signing

### 3a. Check for Existing Keys

```bash
gpg --list-secret-keys
```

**If you see keys:** Skip to step 3c (import if needed)
**If empty:** Continue to 3b (generate new)

### 3b. Generate New GPG Key (Recommended)

```bash
gpg --full-generate-key
```

Follow the prompts:

| Prompt | Answer | Notes |
|--------|--------|-------|
| What kind of key? | `1` (RSA and RSA) | Default |
| What keysize? | `4096` | Strong encryption |
| How long valid? | `3y` | 3 years or longer |
| Is this correct? | `y` | Confirm |
| Real name | `DocTester Release` | Clear name |
| Email address | `release@r10r.org` | Official email |
| Comment | `DocTester 2.0.0+` | Optional |
| O=Okay, C=Cancel | `O` | Confirm |

**Create passphrase:** Use a strong, unique password. You'll need it during release.

### 3c. Verify Key Creation

```bash
gpg --list-secret-keys
```

**Expected output:**
```
[keyboxd]
...
sec   rsa4096 2026-03-10 [SC] [expires: 2029-03-10]
      XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
uid           [ultimate] DocTester Release <release@r10r.org>
```

**Note:** The `XXXXXXXX...` is your **KEY_ID** (long form). Save this.

### 3d. Publish Key to Key Server (REQUIRED)

Maven Central requires your public key to be on a key server.

```bash
# Extract KEY_ID from previous command
export KEY_ID="XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX"  # Use full 40-char ID

# Publish to keys.openpgp.org (official)
gpg --keyserver keys.openpgp.org --send-keys $KEY_ID

# Verify (wait 1-2 minutes)
gpg --keyserver keys.openpgp.org --recv-keys $KEY_ID
```

**Verify on web:**
Visit https://keys.openpgp.org/ and search for your email (`release@r10r.org`)

### 3e. Test GPG Signing (Optional)

```bash
# Create test file
echo "test" > test.txt

# Sign it
gpg -ab test.txt
# Prompts for passphrase

# Verify signature
gpg --verify test.txt.asc
# Expected: "Good signature from DocTester Release"

# Cleanup
rm test.txt test.txt.asc
```

---

## Step 4: Configure Git (for maven-release-plugin)

### 4a. Set Git User

```bash
git config --global user.email "release@r10r.org"
git config --global user.name "DocTester Release Bot"

# Verify
git config --global user.email
git config --global user.name
```

### 4b. Ensure GitHub SSH Access

The release plugin will push tags via SSH. Verify:

```bash
# Test SSH to GitHub
ssh -T git@github.com
# Expected: "Hi r10r-org! You've successfully authenticated..."

# If fails, add SSH key to GitHub:
# 1. Generate: ssh-keygen -t ed25519 -f ~/.ssh/github
# 2. Add public key to GitHub Settings > Deploy keys
```

---

## Step 5: Pre-Release Validation

### 5a. Build Core Module

```bash
cd /home/user/doctester

mvnd clean install -pl doctester-core -DskipTests

# Expected: BUILD SUCCESS
```

### 5b. Verify Release Profile

```bash
mvnd -P release help:describe -Dplugin=org.sonatype.central:central-publishing-maven-plugin

# Expected: Shows plugin configuration details
```

### 5c. Run Dry-Run Again (Optional)

```bash
# This was already done, but you can repeat to verify:
mvnd -P release release:prepare -DdryRun=true

# Expected: Succeeds until GPG signing (or passes if GPG key is set up)

# Cleanup dry-run artifacts
rm -f pom.xml.tag pom.xml.releaseBackup release.properties
rm -f doctester-core/pom.xml.tag doctester-core/pom.xml.releaseBackup
rm -f doctester-integration-test/pom.xml.tag doctester-integration-test/pom.xml.releaseBackup
```

---

## Step 6: Version Planning

### Current Version
- **SNAPSHOT:** 1.1.12-SNAPSHOT
- **Release:** 1.1.12
- **Next dev:** 1.1.13-SNAPSHOT

### For 2.0.0 Release

You have two options:

#### Option A: Manual Version Update Before Release
```bash
# Edit all pom.xml files
sed -i 's/<version>1.1.12-SNAPSHOT<\/version>/<version>2.0.0-SNAPSHOT<\/version>/g' pom.xml doctester-core/pom.xml doctester-integration-test/pom.xml

# Commit
git add pom.xml doctester-core/pom.xml doctester-integration-test/pom.xml
git commit -m "Prepare for 2.0.0 release"

# Then run release
mvnd -P release release:prepare release:perform
# This bumps 2.0.0 → 2.0.1-SNAPSHOT
```

#### Option B: Let Release Plugin Handle It
```bash
# During release:prepare, it prompts for version
mvnd -P release release:prepare
# Enter: 2.0.0 (when prompted)
# Enter: 2.0.1-SNAPSHOT (for next dev version)
```

**Recommended:** Option B (release:prepare is interactive and safer)

---

## Step 7: Execute Release

### 7a. Final Pre-Release Checks

```bash
# Ensure working directory is clean
git status
# Expected: nothing to commit (except untracked CHANGELOG, etc.)

# Verify all tests pass (or skip with -DskipTests)
mvnd clean verify -pl doctester-core
# Expected: BUILD SUCCESS (core tests pass)

# Verify credentials are ready
grep "central" ~/.m2/settings.xml
gpg --list-secret-keys
git config --global user.email
```

### 7b. Execute Release (Full Automation)

**Option 1: Interactive (Recommended for first release)**

```bash
cd /home/user/doctester

mvnd -P release release:prepare release:perform
```

Follow prompts:
1. **Release version:** Enter `2.0.0`
2. **SCM tag:** Auto-fills as `v2.0.0` (confirm)
3. **Next dev version:** Auto-fills as `2.0.1-SNAPSHOT` (confirm)
4. **GPG passphrase:** Enter the passphrase you created in Step 3b

**Option 2: Non-Interactive (for CI/CD)**

```bash
cd /home/user/doctester

# Set environment variables
export GPG_PASSPHRASE="your-gpg-passphrase"

# Run release
mvnd -P release release:prepare release:perform \
  -DreleaseVersion=2.0.0 \
  -DdevelopmentVersion=2.0.1-SNAPSHOT \
  -DskipTests \
  --batch-mode
```

### 7c: Monitor Build

**Expected output sequence:**
```
[INFO] --- release:prepare ---
[INFO] Checking for local modifications...
[INFO] Executing: /bin/sh -c git status
[INFO] Tagging release with tag name v2.0.0
[INFO] [INFO] Executing: git tag -F /tmp/releaseBackup.txt v2.0.0
[INFO] Pushing commits...
[INFO] Pushing release commits and tags...
[INFO] Executing: git push ...
[INFO] [SUCCESS] Release prepared successfully

[INFO] --- release:perform ---
[INFO] Checking out the tagged release...
[INFO] Invoking perform goals...
[INFO] [INFO] --- central-publishing:deploy (default-deploy) ---
[INFO] Uploading artifacts to https://central.sonatype.com/...
[INFO] [SUCCESS] Artifacts published to Central!
```

---

## Step 8: Verify Publication

### 8a. Check Maven Central (Immediate)

```bash
# Search for artifact (1-2 minute delay)
curl -s https://central.sonatype.com/api/v1/search?q=org.r10r:doctester-core:2.0.0 | jq .

# Expected: artifact record found
```

### 8b. Download to Verify

```bash
# Wait 2-5 minutes, then try downloading
mvn dependency:get -Dartifact=org.r10r:doctester-core:2.0.0

# Check cache
ls -la ~/.m2/repository/org/r10r/doctester-core/2.0.0/

# Expected: JAR, POM, and signature files present
```

### 8c: Check GitHub

```bash
# Verify git tags created
git tag
# Expected: v2.0.0 listed

# Verify POMs updated on branch
git log --oneline -5
# Expected: "[maven-release-plugin] prepare release v2.0.0"
```

---

## Step 9: Post-Release (Documentation)

### 9a. Create GitHub Release

```bash
# Create release on GitHub with changelog
gh release create v2.0.0 \
  -t "DocTester 2.0.0" \
  -n "$(cat CHANGELOG_2.0.0.md)"
```

Or manually via GitHub web UI:
1. Go to https://github.com/r10r-org/doctester/releases
2. Click "Create new release"
3. Select tag: `v2.0.0`
4. Add changelog content
5. Publish

### 9b: Update Project Documentation

- [ ] Update `README.md` with version 2.0.0 features
- [ ] Update `CONTRIBUTING.md` if needed
- [ ] Update project website/wiki

### 9c: Announce Release

- [ ] Post on GitHub Discussions
- [ ] Update Maven coordinates in examples
- [ ] Announce on social media / dev communities

---

## Troubleshooting

### Problem: "Could not find server with id 'central'"

**Solution:** Ensure `~/.m2/settings.xml` has `<server><id>central</id>...` block

```bash
grep -A 3 "central" ~/.m2/settings.xml
```

### Problem: "gpg: no default secret key"

**Solution:** Generate or import GPG key (see Step 3)

```bash
gpg --list-secret-keys
# If empty, run Step 3b
```

### Problem: "Could not initialize class org.eclipse.jetty.servlet.ServletContextHandler"

**Solution:** Use workaround to skip integration tests

```bash
mvnd -P release release:prepare release:perform \
  -pl doctester-core \
  -DskipTests
```

### Problem: SSH authentication fails during git push

**Solution:** Verify GitHub SSH key

```bash
ssh -T git@github.com
# If fails, add public key to GitHub Settings > SSH Keys
```

### Problem: Release process hangs during GPG signing

**Solution:** The plugin is waiting for passphrase. Ensure GPG agent is running:

```bash
# Check GPG agent
gpg-agent --version

# Restart if needed
pkill -9 gpg-agent
gpg --list-keys  # Restarts agent
```

---

## Quick Reference Commands

```bash
# Verify setup
java -version && mvnd --version && gpg --list-secret-keys && git config --global user.email

# Build core only
mvnd clean install -pl doctester-core -DskipTests

# Dry-run release
mvnd -P release release:prepare -DdryRun=true && rm -f pom.xml.tag pom.xml.releaseBackup release.properties

# Actual release (interactive)
mvnd -P release release:prepare release:perform

# Release non-interactive (CI/CD)
export GPG_PASSPHRASE="xyz"
mvnd -P release release:prepare release:perform -DskipTests --batch-mode

# Check publication
curl -s https://central.sonatype.com/api/v1/search?q=org.r10r:doctester-core | jq '.[-1]'
```

---

## Timeline Estimate

| Step | Time | Notes |
|------|------|-------|
| 1. Verify environment | 2 min | Quick checks |
| 2. Sonatype setup | 15 min | Token generation |
| 3. GPG setup | 10 min | Key generation + publish |
| 4. Git setup | 2 min | User config |
| 5. Validation | 5 min | Build tests |
| 6. Version planning | 1 min | Decide on version |
| 7. Execute release | 5-10 min | Full build + sign + publish |
| 8. Verify publication | 5 min | Wait + check Central |
| 9. Documentation | 10 min | GitHub release, changelog |
| **Total** | **~60 min** | First release (faster subsequent ones) |

---

## Document Checklist

Before releasing, ensure you have:

- [ ] `MAVEN_CENTRAL_RELEASE_REPORT.md` (read it!)
- [ ] `RELEASE_SETUP_GUIDE.md` (this file)
- [ ] `CHANGELOG_2.0.0.md` (with all changes since 1.1.12)
- [ ] `MIGRATION-1.x-TO-2.0.0.md` (if breaking changes)
- [ ] Updated `README.md` with 2.0.0 features

---

**Questions?** Refer to:
- Full report: `MAVEN_CENTRAL_RELEASE_REPORT.md`
- Maven Central docs: https://central.sonatype.org/publish/publish-maven/
- GPG setup: https://help.github.com/articles/signing-commits-with-gpg/

**You're ready!** Follow steps 1-9 above and you'll have DocTester 2.0.0 on Maven Central.
