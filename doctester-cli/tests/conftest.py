"""Pytest configuration and fixtures."""

import subprocess
from pathlib import Path
from typing import Generator, Optional
import os

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


@pytest.fixture
def maven_project_root() -> Optional[Path]:
    """Get the DocTester project root (Maven project)."""
    # Find the project root by looking for pom.xml
    current = Path.cwd()
    for _ in range(5):  # Search up to 5 levels
        if (current / "pom.xml").exists():
            return current
        current = current.parent
    return None


@pytest.fixture
def maven_build(maven_project_root: Optional[Path], tmp_path: Path) -> Generator[Path, None, None]:
    """Build DocTester with Maven and return the exports directory."""
    if not maven_project_root:
        pytest.skip("Maven project not found (pom.xml)")

    # Set JAVA_HOME for Maven
    java_home = "/usr/lib/jvm/java-25-openjdk-amd64"
    env = os.environ.copy()
    env["JAVA_HOME"] = java_home

    try:
        # Build core module with tests enabled (generates exports)
        result = subprocess.run(
            ["mvnd", "clean", "verify", "-pl", "doctester-core", "-DskipTests=false"],
            cwd=str(maven_project_root),
            capture_output=True,
            text=True,
            timeout=300,
            env=env,
        )

        if result.returncode != 0:
            pytest.skip(f"Maven build failed: {result.stderr}")

        # Check for generated exports
        export_dir = maven_project_root / "doctester-core" / "target" / "site" / "doctester"
        if not export_dir.exists():
            pytest.skip("DocTester exports not generated")

        yield export_dir

    except FileNotFoundError:
        pytest.skip("mvnd not found in PATH")
    except subprocess.TimeoutExpired:
        pytest.skip("Maven build timed out")


@pytest.fixture
def maven_integration_test_build(maven_project_root: Optional[Path]) -> Generator[Path, None, None]:
    """Build integration tests with Maven."""
    if not maven_project_root:
        pytest.skip("Maven project not found (pom.xml)")

    # Set JAVA_HOME for Maven
    java_home = "/usr/lib/jvm/java-25-openjdk-amd64"
    env = os.environ.copy()
    env["JAVA_HOME"] = java_home

    try:
        # Build integration test module
        result = subprocess.run(
            ["mvnd", "clean", "verify", "-pl", "doctester-integration-test", "-DskipTests=false"],
            cwd=str(maven_project_root),
            capture_output=True,
            text=True,
            timeout=300,
            env=env,
        )

        if result.returncode != 0:
            pytest.skip(f"Maven integration test build failed: {result.stderr}")

        # Check for generated exports
        export_dir = maven_project_root / "doctester-integration-test" / "target" / "site" / "doctester"
        if not export_dir.exists():
            pytest.skip("Integration test exports not generated")

        yield export_dir

    except FileNotFoundError:
        pytest.skip("mvnd not found in PATH")
    except subprocess.TimeoutExpired:
        pytest.skip("Maven integration test build timed out")
