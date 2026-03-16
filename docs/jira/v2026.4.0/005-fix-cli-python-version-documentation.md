# DTR-005: Fix CLI Python Version Documentation

**Priority**: P1
**Status**: To Do
**Assignee**: Unassigned
**Labels**: dx,qol,documentation,cli

## Description

The DTR CLI tool documentation (README.md) and package metadata (pyproject.toml) specify incorrect or ambiguous Python version requirements. This creates friction for users trying to install and run the CLI, leading to failed installations and support requests. This ticket updates all Python version references to the correct supported version.

## Acceptance Criteria

- [ ] Verify actual minimum Python version required by CLI code
- [ ] Update `README.md` CLI section with correct Python version requirement
- [ ] Update `pyproject.toml` `requires-python` field to match actual requirement
- [ ] Update installation examples in README if needed
- [ ] Verify `pip install` works with documented version
- [ ] Add Python version check to CLI entry point (optional but recommended)

## Technical Notes

### Files to Modify

#### 1. `README.md`
**Location**: `/Users/sac/dtr/README.md`

**Section to Update**: "CLI Installation" or similar

```markdown
## Installation

### CLI Tool
Requires Python 3.{X}+ (verify actual minimum version)

```bash
pip install dtr-cli
```
```

#### 2. `pyproject.toml`
**Location**: `/Users/sac/dtr/cli/pyproject.toml` (or equivalent)

**Field to Update**:
```toml
[project]
name = "dtr-cli"
requires-python = ">=3.{X}"  # Update to actual minimum version
```

### Verification Steps

1. **Check Actual Requirements**:
```bash
# Find minimum version by scanning CLI code
grep -r "from typing import" cli/
grep -r "match.*case" cli/  # Python 3.10+
grep -r "type.*:" cli/      # Type hints
```

2. **Test Installation**:
```bash
# Create fresh venv with documented version
python3.{X} -m venv test_env
source test_env/bin/activate
pip install -e cli/
dtr --help
```

3. **Version Check Implementation** (optional enhancement):
```python
# cli/dtr/__main__.py
import sys

if sys.version_info < (3, {X}):
    print(f"Error: DTR CLI requires Python 3.{X}+, found {sys.version}")
    sys.exit(1)
```

### Common Python Version Patterns

| Feature | Minimum Python Version |
|---------|----------------------|
| Type hints | 3.5+ |
| f-strings | 3.6+ |
| walrus operator (`:=`) | 3.8+ |
| match/case statements | 3.10+ |
| parameter spec variables (`**kwargs`) | 3.11+ |

### Documentation Template

```markdown
### Python CLI Tool

**Requirements**: Python 3.{X} or later

```bash
# Install via pip
pip install dtr-cli

# Verify installation
dtr --version
```

**Upgrade to latest**:
```bash
pip install --upgrade dtr-cli
```
```

## Dependencies

- None (can be implemented independently)

## References

- README: `/Users/sac/dtr/README.md`
- CLI package: `/Users/sac/dtr/cli/` (verify actual location)
- Python Packaging: https://packaging.python.org/
