"""Pytest configuration and fixtures for Chicago-style TDD.

Fixtures are divided into three categories:

1. SAMPLE FIXTURES (for unit tests):
   - tmp_export_dir: Sample HTML files for unit testing
   - tmp_markdown_file: Sample Markdown file for unit testing

2. PROJECT FIXTURES (for CLI integration tests):
   - tmp_project_dir: Minimal Maven project with pom.xml
   - tmp_project_with_tests: Project dir with sample DocTest and Unit test Java files
   - mock_mvnd_success: Monkeypatched subprocess.run returning exit code 0
   - mock_mvnd_failure: Monkeypatched subprocess.run returning exit code 1
   - dtr_app: The main Typer app for testing

3. REAL MAVEN FIXTURES (for integration tests):
   - project_root: DTR repository root (auto-detected)
   - maven_dtr_core_exports: Real HTML exports from dtr-core build
   - maven_integration_test_exports: Real API documentation from integration tests

Tests fail loudly if Maven is unavailable or builds fail - no graceful skipping.
"""

import subprocess
from pathlib import Path
from typing import Generator
import os

import pytest
from typer.testing import CliRunner


# ============================================================================
# SAMPLE FIXTURES FOR UNIT TESTS
# ============================================================================


@pytest.fixture
def tmp_export_dir(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a temporary export directory with sample HTML files for unit testing."""
    export_dir = tmp_path / "exports"
    export_dir.mkdir()

    # Create sample HTML file
    sample_html = """<!DOCTYPE html>
<html>
<head>
    <title>Test Export</title>
</head>
<body>
    <h1>Test Documentation</h1>
    <p>This is a test export.</p>
    <h2>API Endpoints</h2>
    <table>
        <tr><th>Method</th><th>Path</th></tr>
        <tr><td>GET</td><td>/api/users</td></tr>
    </table>
    <pre><code>System.out.println("hello");</code></pre>
    <ul>
        <li>Feature 1</li>
        <li>Feature 2</li>
    </ul>
</body>
</html>"""
    (export_dir / "test_doc.html").write_text(sample_html)

    yield export_dir


@pytest.fixture
def tmp_markdown_file(tmp_path: Path) -> Generator[Path, None, None]:
    """Create a temporary Markdown file for unit testing."""
    md_file = tmp_path / "test.md"
    md_file.write_text("""# Test Document

This is a test markdown file.

## Section 1

Some content here.

## Section 2

| Column A | Column B |
|----------|----------|
| Value 1  | Value 2  |

```java
public class Hello {
    public static void main(String[] args) {
        System.out.println("Hello DTR!");
    }
}
```
""")
    yield md_file


# ============================================================================
# PROJECT FIXTURES FOR CLI INTEGRATION TESTS
# ============================================================================


@pytest.fixture
def cli_runner() -> CliRunner:
    """Typer CliRunner for invoking CLI commands in tests."""
    return CliRunner(mix_stderr=False)


@pytest.fixture
def tmp_project_dir(tmp_path: Path) -> Path:
    """
    Real temporary project directory with a minimal pom.xml.

    Chicago TDD: use real files, not mocks of file system.
    """
    pom = tmp_path / "pom.xml"
    pom.write_text("""<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
</project>
""")
    src_test = tmp_path / "src" / "test" / "java" / "com" / "example"
    src_test.mkdir(parents=True)
    return tmp_path


@pytest.fixture
def tmp_project_with_tests(tmp_project_dir: Path) -> Path:
    """Project dir with sample DocTest and Unit test Java files."""
    test_dir = tmp_project_dir / "src" / "test" / "java" / "com" / "example"
    (test_dir / "ApiDocTest.java").write_text(
        "package com.example;\npublic class ApiDocTest {}\n"
    )
    (test_dir / "UtilityTest.java").write_text(
        "package com.example;\npublic class UtilityTest {}\n"
    )
    return tmp_project_dir


@pytest.fixture
def mock_mvnd_success(monkeypatch):
    """
    Monkeypatch subprocess.run to simulate a successful mvnd build.

    Chicago TDD: mock only the external process, not our code.
    Returns a fake CompletedProcess with returncode=0.
    """
    import subprocess as sp

    def fake_run(cmd, *args, **kwargs):
        return sp.CompletedProcess(args=cmd, returncode=0, stdout=b"BUILD SUCCESS", stderr=b"")

    monkeypatch.setattr("subprocess.run", fake_run)
    return fake_run


@pytest.fixture
def mock_mvnd_failure(monkeypatch):
    """Monkeypatch subprocess.run to simulate a failed mvnd build."""
    import subprocess as sp

    def fake_run(cmd, *args, **kwargs):
        return sp.CompletedProcess(args=cmd, returncode=1, stdout=b"", stderr=b"BUILD FAILURE")

    monkeypatch.setattr("subprocess.run", fake_run)
    return fake_run


@pytest.fixture
def dtr_app():
    """Return the main DTR Typer app for testing."""
    from dtr_cli.main import app
    return app


# ============================================================================
# REAL MAVEN FIXTURES FOR INTEGRATION TESTS
# ============================================================================


def _find_project_root() -> Path:
    """Find DTR project root by searching for pom.xml.

    Raises:
        RuntimeError: If not in a DTR repository
    """
    current = Path.cwd()
    for _ in range(5):
        if (current / "pom.xml").exists():
            return current
        current = current.parent
    raise RuntimeError(
        "Cannot find DTR project root (pom.xml). "
        "Tests must be run from within the DTR repository."
    )


def _run_maven_build(project_root: Path, module: str, skip_tests: bool = False) -> Path:
    """Execute Maven build and return the exports directory.

    This is a REAL end-to-end test:
    1. Invokes mvnd clean verify -pl {module}
    2. Runs all JUnit tests in the module
    3. DTR generates HTML exports during test execution
    4. Verifies exports exist and are non-empty

    Args:
        project_root: Path to pom.xml root
        module: Maven module name (e.g., 'dtr-core')
        skip_tests: If True, builds without running tests

    Returns:
        Path to generated exports directory (target/site/dtr/)

    Raises:
        RuntimeError: If Java not found, mvnd not in PATH, build fails, or exports not generated
    """
    env = os.environ.copy()

    # Build Maven command
    skip_tests_flag = "-DskipTests" if skip_tests else "-DskipTests=false"
    cmd = ["mvnd", "clean", "verify", "-pl", module, skip_tests_flag]

    # Execute Maven build
    try:
        result = subprocess.run(
            cmd,
            cwd=str(project_root),
            capture_output=True,
            text=True,
            timeout=600,
            env=env,
        )
    except FileNotFoundError:
        raise RuntimeError(
            "mvnd (Maven Daemon) not found in PATH.\n"
            "Install mvnd 2.x or set: PATH=/opt/mvnd/bin:$PATH"
        )
    except subprocess.TimeoutExpired:
        raise RuntimeError(f"Maven build timed out (>600s) for module: {module}")

    # Verify build succeeded
    if result.returncode != 0:
        raise RuntimeError(
            f"Maven build FAILED for {module}.\n\n"
            f"Command: {' '.join(cmd)}\n"
            f"CWD: {project_root}\n\n"
            f"STDERR:\n{result.stderr}\n\n"
            f"STDOUT:\n{result.stdout}"
        )

    # Verify exports were generated
    export_dir = project_root / module / "target" / "site" / "dtr"
    if not export_dir.exists():
        raise RuntimeError(
            f"Exports directory not found: {export_dir}\n"
            f"Maven build succeeded but DTR did not generate exports.\n"
            f"Verify tests ran: mvnd test -pl {module}"
        )

    # Verify exports are non-empty
    export_files = list(export_dir.glob("*.html"))
    if not export_files:
        raise RuntimeError(
            f"Exports directory is empty: {export_dir}\n"
            f"Expected .html files but found none.\n"
            f"Verify tests ran and generated documentation."
        )

    return export_dir


@pytest.fixture(scope="session")
def project_root() -> Path:
    """DTR project root directory.

    Fails immediately if not in a DTR repository.
    Used by other fixtures to locate Maven modules and exports.
    """
    return _find_project_root()


@pytest.fixture(scope="session")
def maven_dtr_core_exports(project_root: Path) -> Path:
    """Real Maven build of dtr-core with DTR exports.

    REAL END-TO-END TEST:
    - Runs: mvnd clean verify -pl dtr-core -DskipTests=false
    - Executes all JUnit tests in dtr-core
    - DTR generates HTML exports in target/site/dtr/
    - Returns path to actual generated documentation

    FAILS LOUDLY if:
    - Java not found
    - mvnd not in PATH
    - Maven build fails
    - Exports not generated

    Use in tests to verify CLI operations on REAL exports.
    """
    return _run_maven_build(project_root, "dtr-core", skip_tests=False)


@pytest.fixture(scope="session")
def maven_integration_test_exports(project_root: Path) -> Path:
    """Real Maven build of dtr-integration-test with API documentation.

    REAL END-TO-END TEST:
    - Runs: mvnd clean verify -pl dtr-integration-test -DskipTests=false
    - Executes integration tests against embedded Ninja framework server
    - Tests make real HTTP calls to API endpoints
    - DTR generates documentation from live HTTP interactions
    - Returns path to actual generated API documentation

    This tests the FULL STACK:
    1. Java application compiles and runs
    2. Embedded server starts
    3. HTTP endpoints respond
    4. Tests execute and pass
    5. DTR captures and documents interactions
    6. HTML exports are generated

    FAILS LOUDLY if ANY step fails.

    Use in tests to verify CLI operations on REAL integration test exports.
    """
    return _run_maven_build(project_root, "dtr-integration-test", skip_tests=False)
