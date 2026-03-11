"""Pytest configuration and fixtures."""

from pathlib import Path
from typing import Generator

import pytest


@pytest.fixture
def tmp_export_dir(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a temporary export directory with sample HTML files."""
    export_dir = tmp_path / "exports"
    export_dir.mkdir()

    # Create sample HTML file
    sample_html = """
    <!DOCTYPE html>
    <html>
    <head>
        <title>Test Export</title>
    </head>
    <body>
        <h1>Test Documentation</h1>
        <p>This is a test export.</p>
    </body>
    </html>
    """
    (export_dir / "test_doc.html").write_text(sample_html)

    yield export_dir


@pytest.fixture
def tmp_markdown_file(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a temporary Markdown file."""
    md_file = tmp_path / "test.md"
    md_file.write_text("""# Test Document

This is a test markdown file.

## Section 1

Some content here.
""")
    yield md_file
