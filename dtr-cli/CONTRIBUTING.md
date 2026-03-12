# Contributing to DTR CLI

Thank you for your interest in contributing! This guide explains how to set up your development environment and contribute to the project.

## Prerequisites

- Python 3.12+ (check `.python-version`)
- `uv` package manager — [Install `uv`](https://docs.astral.sh/uv/getting-started/)
- `make` for running development commands

## Getting Started

### 1. Clone and Setup

```bash
git clone https://github.com/seanchatmangpt/dtr.git
cd dtr/dtr-cli
```

### 2. Install Development Environment

```bash
# Install with all dependencies using uv
uv sync --all-extras

# Or use the convenience Make target
make install-dev
```

### 3. Verify Installation

```bash
# Check CLI is available
dtr --version

# Check development tools
make check
```

## Development Workflow

### Code Quality

Before submitting a PR, ensure code passes all checks:

```bash
make check          # Run all checks (lint + type)
make format         # Auto-format code
make test           # Run tests
make coverage       # Run tests with coverage
```

Alternatively, run individual checks:

```bash
ruff check dtr_cli tests
ruff format dtr_cli tests
mypy dtr_cli
pytest --cov=dtr_cli
```

### Code Style

- **Line length:** 100 characters
- **Python version:** 3.12+ (using modern syntax: `str | None`, `list[str]`, etc.)
- **Type hints:** Required for all public functions and methods
- **Docstrings:** Google-style docstrings for modules, classes, and functions

#### Example:

```python
"""Convert exports between formats.

Args:
    source_dir: Path to source directory
    target_format: Output format (html, markdown, json)
    recursive: Whether to process subdirectories

Returns:
    Number of files converted
"""

def convert_exports(
    source_dir: Path,
    target_format: str,
    recursive: bool = False,
) -> int:
    """Convert exports to specified format."""
```

### Running Tests

```bash
# Run all tests
pytest

# Run specific test file
pytest tests/test_main.py

# Run with coverage report
pytest --cov=dtr_cli --cov-report=html

# Run in verbose mode
pytest -v

# Run with specific markers
pytest -m "not slow"
```

### Adding Tests

- Place new tests in `tests/` directory
- Name files `test_*.py` or `*_test.py`
- Use fixtures from `conftest.py`
- Follow the existing test structure

Example test:

```python
def test_my_feature(tmp_path: Path) -> None:
    """Test my new feature."""
    # Arrange
    config = SomeConfig(path=tmp_path)

    # Act
    result = some_function(config)

    # Assert
    assert result.success is True
```

## Project Structure

```
dtr-cli/
├── dtr_cli/
│   ├── __init__.py
│   ├── main.py              # CLI entry point
│   ├── model.py             # Data models
│   ├── commands/            # Command groups
│   ├── converters/          # Format converters
│   ├── reporters/           # Report generators
│   ├── managers/            # Directory management
│   └── publishers/          # Publishing handlers
├── tests/
│   ├── conftest.py          # Pytest fixtures
│   ├── test_main.py
│   ├── test_models.py
│   └── test_converters.py
├── pyproject.toml           # Project config (dependencies, tools)
├── uv.lock                  # Locked dependencies
├── Makefile                 # Development commands
├── .editorconfig            # Editor settings
└── README.md
```

## Modern Python Practices

This project follows 2026 Python best practices:

### Type Hints

Use Python 3.10+ union syntax:

```python
# ✓ Modern
value: str | None = None
items: list[str] = []
mapping: dict[str, int] = {}

# ✗ Old
from typing import Optional, List, Dict
value: Optional[str] = None
items: List[str] = []
mapping: Dict[str, int] = {}
```

### Imports

Order imports with ruff's isort:

```python
# Standard library
import logging
from pathlib import Path

# Third-party
import typer
from rich.console import Console

# Local
from dtr_cli.model import ConversionConfig
```

### Dataclasses

Use dataclasses for configuration and results:

```python
from dataclasses import dataclass, field

@dataclass
class MyConfig:
    path: Path
    verbose: bool = False
    tags: list[str] = field(default_factory=list)
```

## Submitting Changes

1. **Create a feature branch:**
   ```bash
   git checkout -b feature/my-feature
   ```

2. **Make your changes** and commit with clear messages:
   ```bash
   git commit -m "feat: Add my feature

   Description of what this does and why.
   "
   ```

3. **Push and create a pull request:**
   ```bash
   git push origin feature/my-feature
   ```

4. **Ensure CI passes:**
   - All tests pass
   - Code quality checks pass
   - Coverage meets threshold

## Tools

### uv (Python Package Manager)

Fast, reliable dependency management:

```bash
uv sync                      # Install dependencies
uv sync --all-extras        # Install with all optional dependencies
uv lock                     # Update uv.lock
uv pip install package      # Add package (discouraged, use pyproject.toml)
```

### Ruff (Linter & Formatter)

Fast Python linter and formatter:

```bash
ruff check .                # Lint all files
ruff format .              # Format all files
ruff check --fix .         # Fix auto-fixable issues
```

### mypy (Type Checker)

Static type checking:

```bash
mypy dtr_cli         # Type check
mypy --strict dtr_cli  # Strict mode
```

### pytest (Test Framework)

Unit testing:

```bash
pytest                      # Run all tests
pytest -v                  # Verbose output
pytest -x                  # Stop on first failure
pytest -k "pattern"        # Run specific tests
```

## Need Help?

- Check existing issues: https://github.com/seanchatmangpt/dtr/issues
- Review similar code in the repository
- Read the [DTR documentation](https://github.com/seanchatmangpt/dtr/tree/master/dtr-cli)

## License

By contributing, you agree that your contributions will be licensed under the Apache 2.0 License.
