# DTR-032: Release Health Dashboard

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,automation,monitoring

## Description
Create `scripts/release-health.sh` to provide comprehensive release status monitoring, showing the health of upcoming or recent releases at a glance.

The dashboard should display:
- Current version vs latest released version
- Uncommitted changes status
- Test pass rate
- CI/CD pipeline status
- Dependency updates available
- Time since last release
- Blocking issues for next release

## Acceptance Criteria
- [ ] Create `scripts/release-health.sh` standalone script
- [ ] Display current version (from pom.xml) and latest release (from Git tags)
- [ ] Show uncommitted changes count and modified files summary
- [ ] Report test pass rate (last test run results)
- [ ] Check CI/CD pipeline status (GitHub Actions if available)
- [ ] List outdated dependencies (Maven dependency updates)
- [ ] Calculate time since last release
- [ ] Identify blocking issues (TODO/FIXME comments, failing tests)
- [ ] Support `--json` output for CI integration
- [ ] Include unit tests for health check logic

## Technical Notes

### Script Location
```
scripts/release-health.sh              # Main health dashboard script
scripts/lib/release-health-lib.sh      # Helper functions
src/test/bash/release-health-test.sh   # BATS tests
```

### Health Check Categories
```bash
#!/usr/bin/env bash
# scripts/release-health.sh

check_version_health() {
    local current_version=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
    local latest_release=$(git describe --tags --abbrev=0 2>/dev/null || echo "none")
    echo "Version: $current_version (latest: $latest_release)"
}

check_git_health() {
    local uncommitted=$(git status --porcelain | wc -l)
    local branch=$(git branch --show-current)
    local commits_ahead=$(git rev-list --count "@{u}.." 2>/dev/null || echo "0")
    echo "Git: $branch ($uncommitted uncommitted, $commits_ahead ahead)"
}

check_test_health() {
    if [[ -f "target/surefire-reports/TEST-*.xml" ]]; then
        local tests=$(grep -h "tests=" target/surefire-reports/TEST-*.xml | \
                     awk -F'"' '{sum+=$2;fail+=$4} END {print sum-fail "/" sum}')
        echo "Tests: $tests passed"
    else
        echo "Tests: No recent results"
    fi
}

check_dependency_health() {
    local outdated=$(mvn versions:display-dependency-updates \
                    -DincludeRemoteRepositories=false \
                    | grep -c "newer version available" || echo "0")
    echo "Dependencies: $outdated updates available"
}

check_time_since_release() {
    local last_release=$(git log --tags --simplify-by-decoration --pretty="format:%ci" -1 | head -1)
    if [[ -n "$last_release" ]]; then
        local days_since=$(( ( $(date +%s) - $(date -d "$last_release" +%s) ) / 86400 ))
        echo "Last Release: $days_since days ago"
    else
        echo "Last Release: Never"
    fi
}

check_blocking_issues() {
    local todos=$(grep -r "TODO\|FIXME" src/main/java src/test/java | wc -l)
    local failing_tests=$(grep -r "fail(" src/test/java | wc -l)
    echo "Issues: $todos TODOs/FIXMEs, $failing_tests potential failures"
}
```

### Output Format
```bash
$ scripts/release-health.sh

DTR Release Health Dashboard
=============================

Version Information
-------------------
Current Version: 2026.4.0-SNAPSHOT
Latest Release: 2026.3.0
Status: Pre-release (0 releases behind)

Git Status
----------
Branch: docs-update
Uncommitted Changes: 3 files
Ahead of Origin: 0 commits
Status: Dirty (commit or stash changes before release)

Test Health
-----------
Last Test Run: 2026-03-15 10:30:00
Tests Passed: 142/145 (97.9%)
Status: ⚠️  3 tests failing

CI/CD Status
------------
GitHub Actions: All checks passing
Last Pipeline Run: 2 hours ago
Status: ✓ Green

Dependency Health
-----------------
Outdated Dependencies: 2
  - org.junit:junit-bom 5.11.4 → 5.11.5
  - com.fasterxml.jackson:jackson-bom 2.18.2 → 2.18.3
Status: ⚠️  Updates available

Release Timeline
----------------
Last Release: 45 days ago (2026-03-15)
Average Release Interval: 30 days
Status: ⚠️  Overdue (15 days past average)

Blocking Issues
---------------
TODO/FIXME Comments: 12
Potential Test Failures: 3
Status: ⚠️  Resolve before release

Overall Health: ⚠️  CAUTION
Recommendations:
  1. Fix 3 failing tests before release
  2. Update 2 outdated dependencies
  3. Resolve 12 TODO/FIXME comments
  4. Commit or stash uncommitted changes
```

### JSON Output for CI
```bash
$ scripts/release-health.sh --json
{
  "version": {
    "current": "2026.4.0-SNAPSHOT",
    "latest": "2026.3.0",
    "status": "pre-release"
  },
  "git": {
    "branch": "docs-update",
    "uncommitted": 3,
    "status": "dirty"
  },
  "tests": {
    "passed": 142,
    "total": 145,
    "pass_rate": 97.9,
    "status": "failing"
  },
  "overall": {
    "status": "caution",
    "ready_for_release": false
  }
}
```

### CI Integration
```yaml
# .github/workflows/release-check.yml
name: Release Health Check
on:
  schedule:
    - cron: "0 9 * * 1"  # Every Monday at 9am
  workflow_dispatch:

jobs:
  health-check:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Check Release Health
        run: scripts/release-health.sh --json > health.json
      - name: Comment on Issues
        if: contains(fromJson(readFile('health.json')).overall.status, 'caution')
        uses: actions/github-script@v7
        with:
          script: |
            // Create issue for blocking items
```

## Dependencies
- None (standalone monitoring tool)

## References
- DTR Improvement Plan: Priority 9 - Automation Enhancements
- Related: DTR-025 (Documentation Status Dashboard)
- Related: DTR-031 (Documentation Freshness Checker)
