# DTR-029: Implicit Knowledge Documentation

**Priority**: P1
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,cognitive-load,docs,onboarding

## Description
DTR has numerous "tribal knowledge" practices that exist only in word-of-mouth or implicit behavior. This task creates explicit documentation for all hidden conventions, behaviors, and workflows.

## Current State Analysis
- **Implicit conventions**: File naming, test structure, commit patterns
- **Hidden behaviors**: Default config values, fallback mechanisms
- **Undocumented workflows**: Release process, contribution flow
- **Mentor dependency**: New contributors require extensive hand-holding

## Target Artifacts

### 1. docs/CONVENTIONS.md (NEW)
Complete reference for all DTR conventions:
```markdown
# DTR Conventions & Standards

## File Naming Conventions
- Test files: `*DocTest.java` (e.g., `DtrContextDocTest.java`)
- Documentation: `*.md` in `docs/`
- Configuration: `*.config` in `.mvn/` or root

## Code Structure Conventions
- One `@Test` method = One documentation section
- Use `sayNextSection()` for H1 headings
- Assertions use `sayAndAssertThat()` pattern
- All public methods must have `@Doc` or Javadoc

## Commit Message Conventions
- Format: `type: description`
- Types: feat, fix, docs, chore, refactor, test
- Examples: `feat: add sayBenchmark() method`

## Testing Conventions
- Arrange-Act-Assert pattern
- One assertion per `sayAndAssertThat()` call
- Use descriptive test method names
- Mock external dependencies only when necessary
...
```

### 2. Hidden Behaviors to Document
- **Default values**: What happens if config is missing?
- **Fallback mechanisms**: How does DTR handle missing resources?
- **Environment detection**: How does it detect Java version?
- **Output paths**: Where do files land if not specified?
- **Error handling**: What exceptions are thrown internally?

### 3. Workflow Documentation
- **Release process**: Step-by-step `make release-*` flow
- **Contribution flow**: Fork → Branch → Test → PR → Review
- **Triage process**: How bugs are prioritized
- **Decision making**: How features are approved

## Acceptance Criteria
- [ ] `docs/CONVENTIONS.md` created with complete conventions
- [ ] All implicit behaviors documented with code examples
- [ ] Hidden default values listed in one location
- [ ] Workflow diagrams for release, contribution, triage
- [ ] Every "unwritten rule" now written
- [ ] New contributor can complete PR without mentor help
- [ ] All sections linked from `docs/SITEMAP.md`
- [ ] `sayDocCoverage()` includes convention documentation

## Technical Notes

### Files to Create
- `/Users/sac/dtr/docs/CONVENTIONS.md` - Master conventions document
- `/Users/sac/dtr/docs/workflows/release.md` - Release process guide
- `/Users/sac/dtr/docs/workflows/contributing.md` - Contribution guide
- `/Users/sac/dtr/docs/workflows/triage.md` - Bug triage guide

### Files to Update
- `/Users/sac/dtr/README.md` - Link to CONVENTIONS.md
- `/Users/sac/dtr/docs/SITEMAP.md` - Add conventions section

### Content Sources
Extract implicit knowledge from:
- `.claude/session-log.md` - Historical conversation patterns
- `.git/` - Commit message patterns
- `Makefile` - Build/release workflows
- Test files - Naming and structure patterns
- CI/CD configs - Quality gate behaviors

### Hidden Behaviors Audit
```bash
# Find default values in code
grep -r "default" src/main/java --include="*.java" | grep -i "value\|config"

# Find fallback mechanisms
grep -r "fallback\|else.*return\|default.*return" src/main/java

# Find environment detection
grep -r "System.getProperty\|getenv\|version" src/main/java

# Find error handling
grep -r "throw new\|catch.*Exception" src/main/java
```

### CONVENTIONS.md Outline
```markdown
# DTR Conventions & Standards

## Philosophy
- Tests are documentation
- Calendar owns the year
- Human owns semantics

## File Naming
### Test Files
- Pattern: `*DocTest.java`
- Location: `src/test/java/com/sacryc/dtr/`
- Example: `DtrContextDocTest.java`

### Documentation Files
- Pattern: `kebab-case.md`
- Location: `docs/{category}/`
- Example: `docs/api/say-methods.md`

## Code Structure
### Test Methods
- One method = One documentation section
- Use `sayNextSection()` for H1 headings
- Assertions use `sayAndAssertThat()`

### Classes
- Public methods require `@Doc` or Javadoc
- Package-private for implementation details
- Sealed classes for hierarchies

## Commit Conventions
- Format: `type: description`
- Types: feat, fix, docs, chore, refactor, test
- Subject line: 50 chars max
- Body: Explain why, not what

## Testing Conventions
- Arrange-Act-Assert pattern
- Descriptive method names
- One assertion per call
- Mock only when necessary

## Hidden Behaviors
### Default Values
| Config | Default | Location |
|--------|---------|----------|
| `dtr.output.dir` | `target/docs/` | DtrContext |

### Fallback Mechanisms
- Missing `@Doc` → Use Javadoc
- Missing config → Use internal defaults
- Missing Java 26 → Warn and continue

## Workflows
### Release Process
1. Run `make release-TYPE`
2. Review generated version
3. Push tag to trigger CI
4. Verify Maven Central publication

### Contribution Flow
1. Fork repository
2. Create feature branch
3. Write tests as documentation
4. Submit PR with description
5. Address review feedback
6. Wait for merge
```

## Dependencies
- **DTR-027** (Documentation Rationalization) - Integrate into new docs structure
- **DTR-026** (Config Consolidation) - Document new config conventions
- **DTR-028** (Annotation Simplification) - Document new annotation usage

## References
- Implicit knowledge inventory: `/Users/sac/dtr/docs/dx-qol-improvement-plan.md#implicit-knowledge-audit`
- Convention documentation patterns: https://www.conventionalcommits.org/
- Team onboarding best practices: https://github.com/github/scratch-github/wiki

## Impact Assessment
- **Cognitive Load**: Critical (eliminates tribal knowledge)
- **Risk**: Low (documentation only)
- **User Visible**: Yes (better onboarding)
- **Migration Path**: Immediate adoption

## Success Metrics
- Time to first PR: Reduced by 50%
- Mentor questions per new contributor: Reduced by 70%
- "How do I?" questions: Reduced by 60%
- Convention compliance rate: 95%+

## Validation Plan
- New contributor completes PR without mentor assistance
- All hidden behaviors have explicit documentation
- Every convention has example code
- Workflow docs match actual implementation (audit check)

## Maintenance
- Review CONVENTIONS.md quarterly
- Update when new patterns emerge
- Vote on convention changes (team decision)
- Archive obsolete conventions (don't delete history)
