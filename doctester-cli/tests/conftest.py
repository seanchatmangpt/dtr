"""Pytest configuration and fixtures for Chicago-style TDD.

These fixtures execute REAL Maven builds and verify REAL DocTester capabilities.
Tests fail loudly if Maven is unavailable or builds fail - no graceful skipping.

Fixtures:
- project_root: DocTester repository root (auto-detected)
- maven_doctester_core_exports: Real HTML exports from doctester-core build
- maven_integration_test_exports: Real API documentation from integration tests
"""

import subprocess
from pathlib import Path
import os

import pytest


def _find_project_root() -> Path:
    """Find DocTester project root by searching for pom.xml.

    Raises:
        RuntimeError: If not in a DocTester repository
    """
    current = Path.cwd()
    for _ in range(5):
        if (current / "pom.xml").exists():
            return current
        current = current.parent
    raise RuntimeError(
        "Cannot find DocTester project root (pom.xml). "
        "Tests must be run from within the DocTester repository."
    )


def _run_maven_build(project_root: Path, module: str, skip_tests: bool = False) -> Path:
    """Execute Maven build and return the exports directory.

    This is a REAL end-to-end test:
    1. Invokes mvnd clean verify -pl {module}
    2. Runs all JUnit tests in the module
    3. DocTester generates HTML exports during test execution
    4. Verifies exports exist and are non-empty

    Args:
        project_root: Path to pom.xml root
        module: Maven module name (e.g., 'doctester-core')
        skip_tests: If True, builds without running tests

    Returns:
        Path to generated exports directory (target/site/doctester/)

    Raises:
        RuntimeError: If Java 25 not found, mvnd not in PATH, build fails, or exports not generated
    """
    # Verify Java 25 environment
    java_home = "/usr/lib/jvm/java-25-openjdk-amd64"
    if not Path(java_home).exists():
        raise RuntimeError(
            f"Java 25 required but not found at {java_home}.\n"
            f"Set: export JAVA_HOME={java_home}"
        )

    env = os.environ.copy()
    env["JAVA_HOME"] = java_home

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
    export_dir = project_root / module / "target" / "site" / "doctester"
    if not export_dir.exists():
        raise RuntimeError(
            f"Exports directory not found: {export_dir}\n"
            f"Maven build succeeded but DocTester did not generate exports.\n"
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
    """DocTester project root directory.

    Fails immediately if not in a DocTester repository.
    Used by other fixtures to locate Maven modules and exports.
    """
    return _find_project_root()


@pytest.fixture(scope="session")
def maven_doctester_core_exports(project_root: Path) -> Path:
    """Real Maven build of doctester-core with DocTester exports.

    REAL END-TO-END TEST:
    - Runs: mvnd clean verify -pl doctester-core -DskipTests=false
    - Executes all JUnit tests in doctester-core
    - DocTester generates HTML exports in target/site/doctester/
    - Returns path to actual generated documentation

    FAILS LOUDLY if:
    - Java 25 not found
    - mvnd not in PATH
    - Maven build fails
    - Exports not generated

    Use in tests to verify CLI operations on REAL exports.
    """
    return _run_maven_build(project_root, "doctester-core", skip_tests=False)


@pytest.fixture(scope="session")
def maven_integration_test_exports(project_root: Path) -> Path:
    """Real Maven build of doctester-integration-test with API documentation.

    REAL END-TO-END TEST:
    - Runs: mvnd clean verify -pl doctester-integration-test -DskipTests=false
    - Executes integration tests against embedded Ninja framework server
    - Tests make real HTTP calls to API endpoints
    - DocTester generates documentation from live HTTP interactions
    - Returns path to actual generated API documentation

    This tests the FULL STACK:
    1. Java application compiles and runs
    2. Embedded server starts
    3. HTTP endpoints respond
    4. Tests execute and pass
    5. DocTester captures and documents interactions
    6. HTML exports are generated

    FAILS LOUDLY if ANY step fails.

    Use in tests to verify CLI operations on REAL integration test exports.
    """
    return _run_maven_build(project_root, "doctester-integration-test", skip_tests=False)
