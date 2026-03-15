# GPG Key Management

## Overview

GPG signing is required for Maven Central deployment. The pipeline uses `--pinentry-mode loopback` for non-interactive signing in headless CI/CD environments.

---

## GPG Key Generation

### Generate a New GPG Key

```bash
# Generate a new GPG key for signing
gpg --full-generate-key

# Select: RSA and RSA
# Key size: 4096
# Validity: 2y (recommended for releases)
# Enter your name and email
# Set a strong passphrase

# Export the private key (base64 encoded for GitHub Secrets)
gpg --armor --export-secret-keys YOUR_KEY_ID | base64

# Export the public key (for verification)
gpg --armor --export YOUR_KEY_ID > public-key.asc

# Get the key ID (last 8 characters)
gpg --list-keys --keyid-format LONG
```

---

## Updating GitHub Secrets

When rotating GPG keys, update these secrets in GitHub repository settings:

| Secret | Value |
|--------|-------|
| `GPG_PRIVATE_KEY` | Base64-encoded private key: `gpg --armor --export-secret-keys KEY_ID \| base64` |
| `GPG_PASSPHRASE` | The passphrase set during key generation |
| `GPG_KEY_ID` | Last 8 characters of the key ID |

### Steps to Update Secrets

1. **Navigate to GitHub repo**: Settings → Secrets and variables → Actions
2. **Update each secret**:
   - Click on the secret name
   - Paste the new value
   - Click "Update secret"

---

## Verification Steps

After updating secrets, verify signing works:

### Test Locally

```bash
# Test locally with the same configuration
mvn deploy -Prelease -Dgpg.passphrase=YOUR_PASSPHRASE

# Verify the signed artifact
gpg --verify artifact.jar.asc artifact.jar
```

### Test with RC

```bash
# Create a release candidate to verify signing works
make release-rc-minor

# Verify in GitHub Actions
# Check the workflow logs for GPG signing success
```

---

## GPG Rotation Procedure

1. **Generate new key** — Follow generation steps above
2. **Update GitHub Secrets** — Replace `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`, `GPG_KEY_ID`
3. **Test in RC** — Create a release candidate to verify signing works
4. **Verify artifact** — Download and verify the signed artifact from GitHub Packages
5. **Publish final** — Once RC is verified, proceed with final release

---

## Troubleshooting GPG Issues

| Symptom | Cause | Solution |
|---------|-------|----------|
| "Inappropriate ioctl for device" | Missing `--pinentry-mode loopback` | Add to maven-gpg-plugin configuration |
| "Secret key not available" | Wrong key ID or missing import | Verify `GPG_KEY_ID` matches imported key |
| "Bad passphrase" | Incorrect `GPG_PASSPHRASE` | Re-enter secret in GitHub settings |
| "Signing failed" | Key expired or revoked | Generate new key and rotate |

### Debugging GPG Issues

```bash
# List available keys
gpg --list-keys --keyid-format LONG

# Check key expiration
gpg --list-keys --with-sig-check YOUR_KEY_ID

# Import private key (for testing)
gpg --import private-key.asc

# Test signing
echo "test" | gpg --clearsign
```

---

## Maven GPG Plugin Configuration

The `maven-gpg-plugin` is configured in the `release` profile:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-gpg-plugin</artifactId>
    <version>3.2.7</version>
    <configuration>
        <keyname>${gpg.keyname}</keyname>
        <passphrase>${gpg.passphrase}</passphrase>
        <pinentryMode>loopback</pinentryMode>
    </configuration>
</plugin>
```

The `pinentryMode>loopback` is critical for CI/CD environments where no TTY is available.

---

## Security Best Practices

1. **Use strong passphrases** — Minimum 16 characters with mixed case, numbers, and symbols
2. **Rotate keys annually** — Generate new GPG keys each year
3. **Never commit private keys** — Always use GitHub Secrets for CI/CD
4. **Use 4096-bit keys** — Minimum recommended key size for Maven Central
5. **Set key expiration** — 2 years is recommended for release keys

---

**Last Updated:** March 14, 2026
**Related:** [Failure Recovery](failure-recovery.md) | [Infrastructure Research](infrastructure-research.md)
