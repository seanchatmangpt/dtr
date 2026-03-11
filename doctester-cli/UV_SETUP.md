# Using `uv` with DocTester CLI

This project uses [`uv`](https://docs.astral.sh/uv/), a modern, ultra-fast Python package manager written in Rust. It replaces `pip`, `pip-tools`, and `venv` with a single tool.

## Installation

### macOS/Linux

```bash
curl -LsSf https://astral.sh/uv/install.sh | sh
```

### Windows

```bash
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

### With Homebrew (macOS)

```bash
brew install uv
```

See [uv installation guide](https://docs.astral.sh/uv/getting-started/) for other methods.

## Quick Start

### Install Project

```bash
# Install with locked dependencies
uv sync

# Install with development dependencies
uv sync --all-extras

# Install specific extra
uv sync --extra aws
uv sync --extra gcs
```

### Run Commands

```bash
# CLI commands work directly
dtr --version
dtr convert html-to-markdown target/site/doctester -o ./docs -r

# Run scripts
uv run python script.py

# Run tests
uv run pytest

# Run specific module
uv run python -m doctester_cli.commands.convert
```

### Manage Dependencies

```bash
# Update all dependencies
uv lock

# Add a new dependency (edit pyproject.toml first, then sync)
uv sync

# Show dependency tree
uv pip show -r doctester-cli  # or any package name

# List installed packages
uv pip list
```

## Key Benefits

| Feature | Before (`pip`) | After (`uv`) |
|---------|----------------|--------------|
| **Installation** | ~30-60s | <2s |
| **Dependency Resolution** | Slow, can fail | Fast, reliable |
| **Lock File** | pip-compile (external) | Built-in `uv.lock` |
| **Virtual Environment** | `python -m venv` | Automatic |
| **Dependency Caching** | Yes (slow) | Yes (fast) |
| **Python Management** | External (pyenv) | Can manage too |

## How It Works

### `uv sync`

The `uv sync` command:

1. Reads `pyproject.toml` for dependencies
2. Resolves all transitive dependencies
3. Locks versions in `uv.lock` (commit this file!)
4. Creates/updates `.venv` virtual environment
5. Installs all dependencies

### Reproducible Builds

`uv.lock` is like `Pipfile.lock` or `package-lock.json`:

```bash
# First developer
uv sync  # Creates uv.lock

# Commit uv.lock to git
git add uv.lock
git commit -m "Lock dependencies"

# Second developer
uv sync  # Uses exact versions from uv.lock
```

## Integration with Make

The `Makefile` wraps common `uv` operations:

```bash
make install        # uv sync
make install-dev    # uv sync --all-extras
make lock           # uv lock
```

## Advanced Usage

### Virtual Environment

`uv` automatically manages `.venv`:

```bash
# The venv is created automatically in `.venv`
uv sync

# Activate it manually if needed
source .venv/bin/activate  # Linux/macOS
.venv\Scripts\activate      # Windows

# Or use `uv run` to auto-activate
uv run dtr --version
```

### Python Version Management

Control which Python version to use:

```bash
# In pyproject.toml
requires-python = ">=3.12"

# Use specific Python version
uv sync --python 3.12
uv sync --python /usr/bin/python3.12

# Check available Python versions
uv python list
```

### Offline Mode

```bash
# Download all dependencies
uv sync

# Work offline (dependencies already cached)
uv sync --offline
```

### Custom Index

```bash
# Use private PyPI index
uv sync --index-url https://pypi.example.com/simple/
```

## Troubleshooting

### "command not found: uv"

Install `uv` (see Installation section above) and ensure it's in your `$PATH`:

```bash
# Check installation
uv --version

# If not found, add to PATH
export PATH="$HOME/.local/bin:$PATH"  # Linux
export PATH="$HOME/.cargo/bin:$PATH"  # macOS with Homebrew
```

### "Failed to resolve dependencies"

```bash
# Clear cache and retry
rm -rf uv.lock .venv
uv sync

# Or check for conflicts
uv pip compile --dry-run  # See what would be installed
```

### "Python version not found"

```bash
# See available Python versions
uv python list

# Install specific version
uv python install 3.12

# Set as default
uv sync --python 3.12
```

## Comparison with pip

| Task | pip | uv |
|------|-----|-----|
| `pip install .` | `uv sync` |
| `pip install -r requirements.txt` | `uv sync` (from `pyproject.toml`) |
| `pip freeze > requirements.txt` | Automatic via `uv.lock` |
| `pip install package==1.2.3` | Edit `pyproject.toml`, then `uv sync` |
| `python -m venv .venv` | Automatic via `uv sync` |

## Learning More

- [uv Documentation](https://docs.astral.sh/uv/)
- [uv GitHub](https://github.com/astral-sh/uv)
- [PEP 621 - pyproject.toml](https://www.python.org/dev/peps/pep-0621/)
- [uv vs pip comparison](https://docs.astral.sh/uv/pip/)

## Why This Project Uses uv

1. **Speed** — 10-100x faster than pip for most operations
2. **Reliability** — Better dependency resolution
3. **Simplicity** — Single tool, no need for venv + pip-tools
4. **Modern** — Follows current Python packaging standards
5. **Future-proof** — Aligns with Python packaging evolution
