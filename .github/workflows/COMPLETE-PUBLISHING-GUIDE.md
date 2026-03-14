# Complete Maven Central Publishing Guide for DTR

This guide provides a comprehensive overview of all GitHub Actions workflows for flawless Maven Central publishing for the DTR project.

## 🚀 Publishing Workflows Overview

### 1. Main Publishing Pipeline (`publish.yml`)

**Primary workflow for deploying to Maven Central**

```yaml
Triggers:
- Push to main/master (snapshots)
- Tags (releases)
- Manual dispatch

Jobs:
1. build: Multi-Java version testing (21, 22, 26)
2. package: Artifact generation and GPG signing
3. deploy: Central Portal deployment
4. act-test: Local Act testing
5. release-notes: Documentation generation
```

### 2. Validation Workflows

#### `publish-validation.yml` - Pre-Deployment Validation
Validates everything before deployment:
- Project structure compliance
- `pom.xml` configuration checks
- GPG setup verification
- Central Portal credentials
- Build integrity and signing capability

#### `ci-gate.yml` - Quality Gate
Mandatory quality checks:
- Code formatting (Spotless)
- Static analysis (Checkstyle, PMD)
- Dependency analysis
- Test coverage (JaCoCo)
- Security scanning (OWASP)

#### `quality-gates.yml` - Enhanced Quality System
Comprehensive quality gates:
- Guard validation (no TODOs, no mocks)
- Compilation with coverage
- Code quality checks
- Dependency vulnerability scanning
- Security scanning
- Integration tests

### 3. Testing Workflows

#### `test-deploy.yml` - Test Deployment Pipeline
Tests all deployment scenarios:
- Dry-run deployments
- Staging environment testing
- Production deployment simulation
- Rollback procedures
- Performance testing

### 4. Deployment Workflows

#### `deployment-automation.yml` - Automated Deployments
Automates the deployment process:
- Environment management (staging → production)
- Manual approval workflows
- Automatic rollback capabilities
- Post-deployment verification

#### `monitor-deployment.yml` - Real-Time Monitoring
Monitors deployments after they start:
- Real-time status tracking
- Artifact propagation checking
- Success/failure notifications
- Recovery procedures

### 5. Maintenance Workflows

#### `update-publish.yml` - Update Management
Manages project updates:
- Security dependency updates
- Plugin updates
- Version management
- Validation after updates

#### `gpg-key-management.yml` - GPG Key Lifecycle
Manages GPG keys securely:
- Key rotation and renewal
- Key revocation
- Secure backups
- Secret updates

## 📋 Publishing Process

### Step 1: Pre-Publishing Validation
```bash
# Run validation manually
gh workflow run publish-validation.yml

# Or let it run automatically on push to main
```

### Step 2: Quality Gate Check
```bash
# Quality gate runs automatically on push/PR
# Must pass before deployment
```

### Step 3: Test Deployment
```bash
# Test the deployment process
gh workflow run test-deploy.yml

# Choose:
# - Dry run: Test without actual deployment
# - Staging: Deploy to Sonatype staging
# - Production: Full deployment to Maven Central
```

### Step 4: Actual Publishing
```bash
# Automatic publishing (snapshots)
git push origin main

# Release publishing (tags)
git tag v2.5.0
git push origin v2.5.0

# Manual publishing
gh workflow run publish.yml
```

### Step 5: Monitoring
```bash
# Monitor deployment status
gh workflow run monitor-deployment.yml

# Provides real-time tracking and notifications
```

## 🔐 Security Best Practices

### GPG Key Management
1. **Key Rotation**: Rotate keys annually
2. **Secure Storage**: Store backups in secure locations
3. **Access Control**: Use code owner approval for key operations
4. **Key Servers**: Upload to multiple key servers

### Secrets Management
1. **Least Privilege**: Grant minimal permissions
2. **Regular Rotation**: Rotate secrets quarterly
3. **No Hardcoding**: Never hardcode secrets in code
4. **Environment Protection**: Use environment rules for sensitive operations

## 🚨 Emergency Procedures

### Deployment Failure
1. Check logs for specific errors
2. Run `test-deploy.yml` in dry-run mode
3. Fix issues and retry
4. Use rollback if necessary

### Security Incident
1. Revoke compromised credentials
2. Generate new GPG key via `gpg-key-management.yml`
3. Update GitHub secrets
4. Rebuild and republish

### Rollback Procedure
1. Use `deployment-automation.yml` with rollback action
2. Close staging repositories in Central Portal
3. Notify users
4. Fix underlying issues

## 📊 Monitoring and Reporting

### Quality Metrics
- Test coverage > 80%
- No security vulnerabilities
- All quality gates passing
- Code formatting clean

### Deployment Metrics
- Artifact propagation time
- Success rate
- Performance benchmarks
- Error rates

### Security Metrics
- Key rotation status
- Secret freshness
- Vulnerability scan results
- Compliance status

## 🤔 Troubleshooting

### Common Issues

#### GPG Signing Problems
```
Solution:
- Verify GPG key is correct
- Check passphrase matches secret
- Ensure key is not expired
- Run gpg-key-management.yml for testing
```

#### Central Portal Authentication
```
Solution:
- Verify username and token
- Check token permissions
- Ensure token hasn't expired
- Reset token if needed
```

#### Build Failures
```
Solution:
- Check Java version compatibility
- Verify Maven configuration
- Clear Maven cache
- Run locally first
```

#### Artifacts Not Found
```
Solution:
- Wait 10-15 minutes for propagation
- Check staging repository status
- Verify artifact URLs
- Check for typos in version
```

## 🎯 Next Steps

### Implementation Checklist
1. [ ] Set up GitHub Secrets
2. [ ] Configure GPG keys
3. [ ] Test validation workflows
4. [ ] Monitor quality gates
5. [ ] Practice deployment process

### Best Practices
1. Always run validation before publishing
2. Keep test coverage above 80%
3. Monitor deployment status
4. Regular key rotation
5. Document all changes

## 📚 Related Resources

- [Maven Central Portal](https://central.sonatype.com/)
- [GitHub Actions Documentation](https://docs.github.com/actions)
- [GPG Key Management Guide](https://central.sonatype.org/publish/requirements/gpg/)
- [Act Documentation](https://nektosact.com/)

---

**Note**: This comprehensive system ensures flawless Maven Central publishing through multiple layers of validation, testing, and monitoring.
