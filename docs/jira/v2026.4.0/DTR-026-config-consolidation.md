# DTR-026: Configuration Consolidation

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,cognitive-load,config

## Description
DTR currently has 25+ configuration files spread across the project, creating unnecessary cognitive load for contributors. This task aims to reduce configuration complexity by consolidating redundant configs and merging related settings into centralized locations.

## Current State Analysis
- **POM files**: 1 root + 4 modules = 5 files
- **GitHub Actions**: 7 workflow files (could be 3)
- **Maven config**: `.mvn/maven.config`, `.mvn/jvm.config`, `.mvn/extensions.xml`
- **IDE configs**: `.idea/`, `.vscode/`, `.eclipse/`
- **Build metadata**: `maven-version.txt`, `.gitversion`, `pom.xml` properties
- **CI/CD settings**: Multiple workflow files with duplicated environment setup

## Target State
**Reduce from 25+ to 12 config files** by:
1. Merging `.mvn/maven.config` options into `pom.xml` properties
2. Consolidating GitHub Actions workflows (test, release, quality)
3. Standardizing IDE config into single source of truth
4. Eliminating redundant version tracking files

## Acceptance Criteria
- [ ] Total configuration files ≤ 12 (down from 25+)
- [ ] `.mvn/maven.config` merged into `pom.xml` <properties> section
- [ ] GitHub Actions reduced to 3 workflows: `ci.yml`, `release.yml`, `quality.yml`
- [ ] All environment variables defined in single `.github/workflows/env-vars.yml`
- [ ] `maven-version.txt` and `.gitversion` consolidated into one source
- [ ] `mvnd verify` still passes with consolidated config
- [ ] `make release-*` commands work without changes
- [ ] IDE configurations documented in `docs/CONTRIBUTING.md#setup`

## Technical Notes

### Files to Modify
- **Merge into**: `/Users/sac/dtr/pom.xml`
  - `.mvn/maven.config` → `<properties><maven.compiler.args></properties>`
  - `maven-version.txt` → `<properties><dtr.build.version></properties>`

### Files to Create
- `/Users/sac/dtr/.github/workflows/env-vars.yml` - Centralized environment variables
- `/Users/sac/dtr/.github/workflows/ci.yml` - Combined test + build workflow
- `/Users/sac/dtr/.github/workflows/quality.yml` - Combined coverage + lint workflow

### Files to Delete
- `.mvn/maven.config` (merge into pom.xml)
- `.github/workflows/test.yml` (merge into ci.yml)
- `.github/workflows/build.yml` (merge into ci.yml)
- `.github/workflows/coverage.yml` (merge into quality.yml)
- `.github/workflows/lint.yml` (merge into quality.yml)
- `maven-version.txt` (merge into pom.xml properties)

### Validation Commands
```bash
# Pre-consolidation baseline
find . -name "*.config" -o -name "*.xml" -o -name "*.yml" | wc -l

# Post-consolidation verification
mvnd verify --enable-preview
make release-patch  # Should work without changes
```

## Dependencies
- **DTR-028** (CONVENTIONS.md) - Document consolidated config structure
- **DTR-027** (Documentation Rationalization) - Update docs to reflect new structure

## References
- Current config inventory: `/Users/sac/dtr/docs/dx-qol-improvement-plan.md#configuration-audit`
- Maven property reference: https://maven.apache.org/pom.html#Properties
- GitHub Actions reusable workflows: https://docs.github.com/en/actions/using-workflows/reusing-workflows

## Impact Assessment
- **Cognitive Load**: High (removes 50% of config files)
- **Risk**: Medium (changes to build system)
- **User Visible**: No (internal build changes only)
- **Migration Path**: Automatic (no user action required)

## Success Metrics
- Configuration file count: 25+ → 12
- Build time: No regression
- Test pass rate: 100% maintained
- New contributor setup time: Reduced by 40%
