# DTR-031: Documentation Freshness Checker

**Priority**: P3
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,automation,validation,monitoring

## Description
Add documentation staleness detection to `dx.sh` and create `scripts/check-docs-freshness.sh` to warn users when documentation is significantly out of sync with source code.

The freshness checker should:
- Compare last-modified timestamps of source files vs generated documentation
- Detect stale JavaDoc metadata (`docs/meta/javadoc.json`)
- Warn when documentation hasn't been regenerated after code changes
- Provide commands to refresh stale documentation
- Integrate with `dx.sh status` command

## Acceptance Criteria
- [ ] Create `scripts/check-docs-freshness.sh` script
- [ ] Script detects stale source files (newer than generated docs)
- [ ] Detects stale JavaDoc metadata (test methods added/modified without regeneration)
- [ ] Provides clear output: "Fresh", "Stale (N files)", or "Unknown"
- [ ] Integrates freshness check into `dx.sh status` output
- [ ] Adds `--fresh` flag to `dx.sh` to check only freshness
- [ ] Includes unit tests for staleness detection logic
- [ ] Warns when staleness exceeds threshold (configurable, default: 24 hours)

## Technical Notes

### Script Location
```
scripts/check-docs-freshness.sh    # Standalone freshness checker
scripts/lib/dx-freshness.sh        # Functions for dx.sh integration
```

### Freshness Check Logic
```bash
#!/usr/bin/env bash
# Check if source files are newer than generated docs

check_freshness() {
    local src_dir="${1:-src/main/java}"
    local docs_dir="${2:-target/docs}"

    # Find newest source file
    local newest_src=$(find "$src_dir" -type f -name "*.java" -printf '%T@\n' | sort -n | tail -1)

    # Find newest doc file
    local newest_doc=$(find "$docs_dir" -type f -printf '%T@\n' | sort -n | tail -1)

    if (( $(echo "$newest_src > $newest_doc" | bc -l) )); then
        return 1  # Stale
    fi
    return 0  # Fresh
}
```

### JavaDoc Metadata Freshness
```bash
check_javadoc_freshness() {
    local javadoc_meta="docs/meta/javadoc.json"

    if [[ ! -f "$javadoc_meta" ]]; then
        echo "❌ JavaDoc metadata missing (run: mvnd process-test-resources)"
        return 1
    fi

    # Check if any test file is newer than metadata
    local newest_test=$(find src/test/java -type f -name "*DocTest.java" -printf '%T@\n' | sort -n | tail -1)
    local javadoc_mtime=$(stat -c '%Y' "$javadoc_meta")

    if (( $(echo "$newest_test > $javadoc_mtime" | bc -l) )); then
        echo "⚠️  JavaDoc metadata stale (run: mvnd process-test-resources)"
        return 1
    fi

    return 0
}
```

### Integration with dx.sh
```bash
# Add to dx.sh status
dx_status() {
    echo "DTR Environment Status:"
    echo "========================"
    # ... existing checks ...

    echo ""
    echo "Documentation Freshness:"
    if check_freshness; then
        echo "✓ Generated docs up-to-date"
    else
        echo "⚠️  Generated docs stale (run: mvnd clean verify)"
    fi

    if check_javadoc_freshness; then
        echo "✓ JavaDoc metadata current"
    else
        echo "⚠️  JavaDoc metadata needs refresh"
    fi
}
```

### Usage Examples
```bash
# Standalone check
$ scripts/check-docs-freshness.sh
✓ Documentation fresh (last generated: 2 hours ago)

# Integrated status
$ dx.sh status
DTR Environment Status:
========================
Java Version: ✓ 26.ea.13
Maven Version: ✓ 4.0.0-rc-3

Documentation Freshness:
✓ Generated docs up-to-date
✓ JavaDoc metadata current

# Freshness-only check
$ dx.sh --fresh
⚠️  Documentation stale (3 files changed since last generation)
  Run: mvnd clean verify
```

## Dependencies
- None (standalone enhancement)

## References
- DTR Improvement Plan: Priority 9 - Automation Enhancements
- Related: DTR-025 (Documentation Status Dashboard)
- Related: DTR-026 (Diagnostic Commands for dx.sh)
