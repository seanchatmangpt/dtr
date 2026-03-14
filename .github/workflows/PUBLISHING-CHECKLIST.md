# GitHub Publishing Workflow Checklist

## Overview
This document provides a comprehensive checklist for ensuring flawless publishing to Maven Central through GitHub Actions for the DTR project.

## Workflow Structure

### 1. Main Publishing Workflow (`publish.yml`)
- **Trigger**: Push to main/master, tags, or manual dispatch
- **Jobs**:
  - `build`: Multi-Java version testing
  - `package`: Artifact generation and signing
  - `deploy`: Central Portal deployment
  - `act-test`: Act simulation testing
  - `release-notes`: Release generation

### 2. Validation Workflows
- **`publish-validation.yml`**: Pre-deployment validation
  - Project structure validation
  - pom.xml configuration
  - GPG configuration
  - Central Portal credentials
  - Build and testing

- **`test-deploy.yml`**: Test deployment pipeline
  - Dry run deployments
  - Staging deployments
  - Production deployments
  - Rollback testing

### 3. CI Gate (`ci-gate.yml`)
- Code quality checks
- Dependency analysis
- Test coverage
- Security scans
- Build verification
- Deployment readiness

### 4. Deployment Automation (`deployment-automation.yml`)
- Environment-specific deployments
- Manual approval workflows
- Rollback capabilities
- Post-deployment tasks

### 5. Monitoring (`monitor-deployment.yml`)
- Real-time deployment monitoring
- Artifact propagation checking
- Status reporting
- Notifications

### 6. Update Management (`update-publish.yml`)
- Security updates
- Dependency updates
- Plugin updates
- Version management

## Publishing Checklist

### Before Publishing

#### 1. Project Validation
- [ ] Project structure is correct
  - `pom.xml` exists
  - Required files present (README, LICENSE, .gitignore)
  - Proper project metadata

- [ ] pom.xml configuration
  - `central-publishing-maven-plugin` configured
  - Source and javadoc JAR plugins
  - GPG signing plugin
  - Proper distributionManagement

- [ ] Version format
  - Snapshot: `X.Y.Z-SNAPSHOT`
  - Release: `X.Y.Z`
  - Tag format: `vX.Y.Z`

#### 2. Credentials Check
- [ ] GPG secrets present
  - `GPG_PRIVATE_KEY`
  - `GPG_PASSPHRASE`
  - `GPG_KEY_ID`

- [ ] Central Portal credentials
  - `CENTRAL_USERNAME`
  - `CENTRAL_TOKEN`

- [ ] GitHub token
  - `GITHUB_TOKEN` (automatically provided)

#### 3. Code Quality
- [ ] Code formatting (Spotless)
- [ ] Static analysis (Checkstyle, PMD)
- [ ] No code smells (SpotBugs)
- [ ] Test coverage > 80%
- [ ] Security scans passed

#### 4. Build Verification
- [ ] Builds on Java 21, 22, 26
- [ ] All tests passing
- [ ] No dependency conflicts
- [ ] No security vulnerabilities

### Publishing Process

#### 1. Trigger the Workflow
```bash
# For snapshots (push to main)
git push origin main

# For releases (tagged)
git tag v2.5.0
git push origin v2.5.0

# Manual trigger
# Go to Actions → Publish to Maven Central → Run workflow
```

#### 2. Monitor the Workflow
- [ ] Build job completes successfully
- [ ] Package job signs artifacts
- [ ] Deploy job runs (only for tags)
- [ ] All artifacts uploaded

#### 3. Check Deployment Status
- [ ] Central Portal shows deployment
- [ ] Staging repository closed
- [ ] Artifacts in Maven Central
- [ ] Propagation complete

#### 4. Verify Artifacts
- [ ] JAR file available
- [ ] Source JAR available
- [ ] Javadoc JAR available
- [ ] GPG signatures present
- [ ] Checksum files generated

### After Publishing

#### 1. Check Links
- [ ] Maven Central: https://central.sonatype.com
- [ ] Repository: https://repo1.maven.org/maven2
- [ ] Project page updated

#### 2. Monitor Usage
- [ ] Downloads tracking
- [ ] No reported issues
- [ ] Dependencies resolving correctly

### Rollback Procedures

#### 1. If Deployment Fails
1. Check logs for specific errors
2. Verify all secrets are correct
3. Run `test-deploy.yml` in dry-run mode
4. Fix issues and retry

#### 2. If Artifacts Are Corrupted
1. Close staging repository in Central Portal
2. Tag new version with fix
3. Republish
4. Notify users

#### 3. If Security Issues Found
1. Revoke GPG key if compromised
2. Generate new GPG key
3. Update secrets
4. Rebuild with security fixes

## Common Issues and Solutions

### 1. GPG Signing Issues
```
Solution:
- Verify GPG key is correct
- Check passphrase
- Ensure key is not expired
- Try re-importing the key
```

### 2. Central Portal Authentication
```
Solution:
- Verify username and token
- Check token permissions
- Ensure token hasn't expired
- Reset token if needed
```

### 3. Build Failures
```
Solution:
- Check Java version compatibility
- Verify Maven configuration
- Clear Maven cache
- Run locally first
```

### 4. Artifacts Not Found
```
Solution:
- Wait 10-15 minutes for propagation
- Check staging repository status
- Verify artifact URLs
- Check for typos in version
```

## Best Practices

### 1. Version Management
- Use semantic versioning
- Test snapshots before releases
- Keep versions consistent

### 2. Security
- Rotate GPG keys periodically
- Use different tokens for different environments
- Monitor for vulnerabilities

### 3. Monitoring
- Set up notifications for deployments
- Monitor Central Portal status
- Track download statistics

### 4. Documentation
- Keep README updated
- Document breaking changes
- Provide migration guides

## DTR Multi-Module Specifics

### Module Structure
- `dtr-core` - Main library (published)
- `dtr-integration-test` - Integration tests (not published)
- `dtr-benchmarks` - Performance benchmarks (not published)

### Deployment Notes
- All modules build, but only `dtr-core` deploys to Maven Central
- Use `-pl dtr-core` to deploy only the core module if needed
- Multi-module builds use `-T1C` for parallel compilation

## Emergency Contacts

- **Platform Team**: For deployment issues
- **Security Team**: For GPG/key issues
- **Central Portal Support**: For platform issues

## Related Workflows

- `pom.xml` - Maven configuration
- `settings-template.xml` - Settings configuration
- Project docs for more details
