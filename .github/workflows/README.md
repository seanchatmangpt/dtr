# GitHub Actions Workflows

This directory contains comprehensive GitHub Actions workflows for DTR's CI/CD pipeline, with a focus on flawless Maven Central publishing.

## 📁 Workflow Overview

### Core Publishing Workflows

| Workflow | Purpose | Trigger | Key Features |
|----------|---------|---------|-------------|
| **`publish.yml`** | Main publishing pipeline | Push/main, tags, manual | Multi-Java builds, GPG signing, Central Portal |
| **`publish-validation.yml`** | Pre-deployment validation | Manual/workflow_call | Structure, config, GPG, Central checks |
| **`test-deploy.yml`** | Test deployment pipeline | Manual/workflow_call | Dry-run, staging, production, rollback tests |
| **`ci-gate.yml`** | Quality gate | Push/PR | Code quality, dependencies, coverage, security |
| **`deployment-automation.yml`** | Automated deployments | Manual/workflow_call | Environment management, approvals, rollbacks |
| **`monitor-deployment.yml`** | Deployment monitoring | Manual/workflow_call | Real-time tracking, artifact propagation |
| **`update-publish.yml`** | Update management | Manual/workflow_call | Security, dependencies, plugins, versioning |
| **`quality-gates.yml`** | Enhanced quality gates | Push/PR/Manual | Guard validation, coverage, security, integration |
| **`gpg-key-management.yml`** | GPG key lifecycle | Manual/workflow_call | Rotation, renewal, revocation, secure updates |

### Supporting Documentation

| Document | Purpose |
|----------|---------|
| **`PUBLISHING-CHECKLIST.md`** | Comprehensive publishing checklist |
| **`COMPLETE-PUBLISHING-GUIDE.md`** | Complete publishing system guide |

## 🚀 Quick Start

### 1. Publishing to Maven Central

#### Automatic Publishing (Tags)
```bash
# Create and push a tag
git tag v2.5.0
git push origin v2.5.0
```

#### Manual Publishing
1. Go to Actions tab
2. Select "Publish to Maven Central"
3. Choose release type (snapshot/release/dry-run)
4. Click "Run workflow"

### 2. Local Testing
```bash
# Build locally
./mvnw clean verify -T1C

# Test with release profile
./mvnw clean verify -Prelease -T1C
```

## 🔧 Configuration Requirements

### Required Secrets
Set these in GitHub repository settings:

```yaml
GPG_PRIVATE_KEY: |-
  -----BEGIN PGP PRIVATE KEY BLOCK-----
  ... your GPG private key here ...
  -----END PGP PRIVATE KEY BLOCK-----

GPG_PASSPHRASE: your-gpg-passphrase
GPG_KEY_ID: your-key-id

CENTRAL_USERNAME: your-central-username
CENTRAL_TOKEN: your-central-token
```

### Project Configuration
Ensure these are in `pom.xml`:

```xml
<distributionManagement>
  <repository>
    <id>central</id>
    <url>https://central.sonatype.com/repository/maven-releases/</url>
  </repository>
  <snapshotRepository>
    <id>central</id>
    <url>https://central.sonatype.com/repository/maven-snapshots/</url>
  </snapshotRepository>
</distributionManagement>

<build>
  <plugins>
    <plugin>
      <groupId>org.sonatype.central</groupId>
      <artifactId>central-publishing-maven-plugin</artifactId>
      <version>0.6.0</version>
    </plugin>
    <!-- Source and Javadoc plugins -->
    <!-- GPG plugin -->
  </plugins>
</build>
```

## 📊 Workflow Details

### 1. publish.yml - Main Pipeline
- **Build**: Tests on Java 21, 22, 26
- **Package**: Generates and signs artifacts
- **Deploy**: Publishes to Central Portal
- **Act Test**: Local simulation testing
- **Release Notes**: Generates release documentation

### 2. publish-validation.yml - Validation
Validates before deployment:
- Project structure
- pom.xml configuration
- GPG setup
- Central Portal credentials
- Build integrity

### 3. test-deploy.yml - Testing Tests
Tests deployment scenarios:
- Dry-run deployments
- Staging environment
- Production environment
- Rollback procedures

### 4. ci-gate.yml - Quality Gate
Ensures quality before deployment:
- Code formatting
- Static analysis
- Dependency management
- Test coverage
- Security scanning

### 5. deployment-automation.yml - Automation
Automates deployment with:
- Environment management
- Manual approval workflows
- Rollback capabilities
- Post-deployment tasks

### 6. monitor-deployment.yml - Monitoring
Monitors after deployment:
- Real-time status tracking
- Artifact propagation
- Success/failure notifications

### 7. update-publish.yml - Updates
Manages updates:
- Security dependency updates
- Plugin updates
- Version management
- Validation after updates

## 🎯 Publishing Workflow

### Snapshot Publishing
1. Push to main branch
2. CI gate runs automatically
3. If passes, publishes snapshot

### Release Publishing
1. Create tag: `git tag v2.5.0`
2. Push tag: `git push origin v2.5.0`
3. Full pipeline runs
4. Publishes to Central Portal
5. Creates GitHub release

### Manual Publishing
1. Go to Actions → Workflows
2. Select "Publish to Maven Central"
3. Choose release type
4. Click "Run workflow"

## 🔍 Monitoring and Troubleshooting

### Checking Deployment Status
1. Central Portal: https://central.sonatype.com
2. GitHub Actions logs
3. Monitor workflow for artifacts

### Common Issues
- **GPG errors**: Check key and passphrase
- **Authentication**: Verify Central Portal credentials
- **Build failures**: Check Java versions and dependencies
- **Propagation**: Wait 10-15 minutes

### Debug Commands
```bash
# Check GPG key
gpg --list-secret-keys

# Test Central Portal connectivity
curl -u "$CENTRAL_USERNAME:$CENTRAL_TOKEN" https://central.sonatype.com/api/v1/publisher

# Build locally
./mvnw clean verify -T1C
```

## 📈 Best Practices

### Version Management
- Use semantic versioning (SemVer)
- Test snapshots before releases
- Keep versions in sync across dependencies

### Security
- Rotate GPG keys annually
- Use different tokens for different environments
- Monitor for vulnerabilities regularly

### Quality Assurance
- Always run CI gate before publishing
- Keep test coverage above 80%
- Address security vulnerabilities immediately

### Monitoring
- Monitor deployment statuses
- Track download statistics
- Set up notifications for failures

## 🚨 Emergency Procedures

### Deployment Failure
1. Check logs for specific errors
2. Verify all secrets
3. Run validation workflow
4. Fix issues and retry

### Security Incident
1. Revoke compromised credentials
2. Generate new GPG key via `gpg-key-management.yml`
3. Update secrets
4. Rebuild and republish

### Rollback
1. Use deployment automation workflow
2. Close staging repositories
3. Notify users
4. Fix underlying issues

## 📚 Resources

- [Maven Central Portal](https://central.sonatype.com/)
- [GitHub Actions Documentation](https://docs.github.com/actions)
- [GPG Key Management Guide](https://central.sonatype.org/publish/requirements/gpg/)

## 🤝 Contributing

When modifying workflows:
1. Test changes locally
2. Update documentation
3. Test the full pipeline
4. Get approval before merging

---

*For questions about publishing, contact the platform engineering team.*
