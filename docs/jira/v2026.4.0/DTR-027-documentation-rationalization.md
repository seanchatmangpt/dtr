# DTR-027: Documentation Rationalization

**Priority**: P2
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,cognitive-load,docs

## Description
DTR has 180+ documentation files across multiple directories, creating discoverability issues and maintenance burden. This task reduces documentation to ~80 high-quality, current files while archiving historical content.

## Current State Analysis
- **Total docs**: 180+ files
- **Directories**: `docs/`, `target/docs/`, `src/javadoc/`, `.claude/`
- **Outdated content**: Pre-2026 architecture docs, deprecated API examples
- **Duplication**: Same content in `docs/` and Javadoc
- **Missing index**: No central navigation or sitemap

## Target Architecture
```
docs/
├── SITEMAP.md                    # NEW: Complete navigation index
├── api/                          # Core API documentation (20 files)
├── guides/                       # How-to guides (15 files)
├── architecture/                 # Design docs (10 files)
├── contributing/                 # Contributor guide (5 files)
├── release/                      # Release notes & changelogs (10 files)
├── archive/                      # NEW: Archived pre-2026 docs (20 files)
└── index.md                      # Main landing page
```

## Acceptance Criteria
- [ ] Total documentation files ≤ 80 (down from 180+)
- [ ] `docs/SITEMAP.md` created with complete file tree
- [ ] Outdated content moved to `docs/archive/`
- [ ] Duplicate content eliminated (single source of truth)
- [ ] All file paths in `SITEMAP.md` are valid
- [ ] Broken references fixed (no `[404]` links)
- [ ] `sayDocCoverage()` reports 100% coverage of public APIs
- [ ] Javadoc generation includes new `docs/api/` content
- [ ] `mvnd verify` generates `target/docs/` without errors

## Technical Notes

### Files to Create
- `/Users/sac/dtr/docs/SITEMAP.md` - Complete navigation tree with descriptions
- `/Users/sac/dtr/docs/archive/README.md` - Explains archived content policy

### Files to Archive
Move pre-2026 or deprecated content to `docs/archive/`:
- Old architecture docs (pre-Java 26 migration)
- Deprecated say* method examples
- Superseded build instructions
- Historical design discussions

### Files to Consolidate
- Merge multiple "Getting Started" guides into one
- Consolidate API reference docs (remove Javadoc duplication)
- Combine related how-to guides by topic

### Files to Delete
- Duplicate Javadoc in `docs/` (use generated Javadoc only)
- Placeholder files with "TODO" content
- Test/experiment documentation not relevant to users

### Validation Commands
```bash
# Count pre-rationalization
find docs -name "*.md" | wc -l

# Validate all sitemap links exist
grep -o '\](.*\.md)' docs/SITEMAP.md | sed 's/\]//' | while read f; do
  [ -f "$f" ] || echo "Missing: $f"
done

# Check for broken internal references
grep -r '\[.*\](.*\.md)' docs --include="*.md" | while read line; do
  link=$(echo "$line" | grep -o '(.*\.md)' | tr -d '()')
  [ -f "$link" ] || echo "Broken: $link"
done
```

### SITEMAP.md Template
```markdown
# DTR Documentation Site Map

## Quick Start
- [Installation](/docs/guides/installation.md)
- [Hello World](/docs/guides/hello-world.md)
- [API Overview](/docs/api/overview.md)

## API Reference
### Core Context
- [DtrContext](/docs/api/dtrcontext.md)
- [say() Methods](/docs/api/say-methods.md)
...

## Guides
- [Testing Best Practices](/docs/guides/testing.md)
- [Performance Benchmarking](/docs/guides/benchmarking.md)
...

## Architecture
- [Scanner Layer](/docs/architecture/scanner.md)
- [Oracle Layer](/docs/architecture/oracle.md)
...

## Contributing
- [Setup](/docs/contributing/setup.md)
- [Code Review](/docs/contributing/review.md)

## Archive
- [Pre-2026 Architecture](/docs/archive/old-architecture.md)
...
```

## Dependencies
- **DTR-028** (CONVENTIONS.md) - Document documentation structure conventions
- **DTR-026** (Config Consolidation) - Coordinate config docs cleanup

## References
- Current doc inventory: `/Users/sac/dtr/docs/dx-qol-improvement-plan.md#documentation-audit`
- Docs DiTTO analysis: See prior session findings
- Java documentation guidelines: https://docs.oracle.com/javase/8/docs/technotes/guides/javadoc/

## Impact Assessment
- **Cognitive Load**: High (removes ~100 redundant files)
- **Risk**: Medium (content reorganization)
- **User Visible**: Yes (better navigation)
- **Migration Path**: Update bookmarks, old URLs redirect

## Success Metrics
- Documentation file count: 180+ → 80
- Average time to find info: Reduced by 60%
- Orphaned files (no links): 0
- Documentation coverage: 100% of public APIs

## Rollback Plan
- Git preserves all deleted files
- Archive directory preserves historical content
- SITEMAP.md provides migration path from old structure
