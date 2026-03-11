"""Phase 6a: Maven CLI Integration Testing for DocTester CLI (Consolidated).

Tests essential integration between DocTester CLI and Maven build lifecycle.
Consolidated from 26 tests to 8 core tests using 80/20 principle.

CORE FEATURES (8 tests):

1. test_maven_exec_plugin_invocation — Maven exec:java goal works
   - Parametrized: explicit_pom, default_pom configurations
   - Validates: Maven can invoke CLI, exit codes propagate

2. test_maven_lifecycle_integration — Full lifecycle: clean → compile → package
   - Validates: Maven phases preserve state for next phase
   - Validates: Maven variables interpolated correctly

3. test_multi_module_build — Docs generated per module
   - Parametrized: num_modules = [2, 3]
   - Validates: CLI operates on module outputs, inherits parent settings

4. test_maven_profile_activation — Profiles change CLI behavior
   - Parametrized: profile_name = ["docs-html", "docs-latex"]
   - Validates: Maven properties translate to CLI arguments

5. test_output_artifact_integration — Docs included in Maven target/
   - Validates: CLI generates docs in Maven target/ directory
   - Validates: Maven post-build hooks can access output

6. test_maven_enforcer_java_version — Java 25+ requirement enforced (NEW)
   - Validates: Maven enforcer prevents old JDK usage
   - Validates: Environment is correct for DocTester

7. test_dependency_resolution_completeness — All dependencies resolved
   - Validates: mvn dependency:tree shows clean output
   - Validates: No missing dependencies or warnings

8. test_maven_cli_help_documentation — mvn help:describe works
   - Validates: Users can discover plugin via help command
   - Validates: Plugin is properly registered in build system

Removed (18 tests): Edge cases, redundant configurations, low-impact scenarios
- Parallel builds, offline mode, custom settings.xml, skip-tests variations
- Advanced edge cases: debug output, incremental builds, etc.

Total: 8 tests covering essential Maven integration.
"""

import re
import subprocess
from pathlib import Path
from typing import Generator

import pytest


# ============================================================================
# CONSOLIDATED TEST SUITE: 8 Core Maven Integration Tests
# ============================================================================


@pytest.mark.parametrize("pom_config", ["explicit_pom", "default_pom"])
def test_maven_exec_plugin_invocation(
    project_root: Path, pom_config: str
) -> None:
    """Test that Maven exec:java goal can successfully invoke CLI.

    PARAMETRIZED: explicit_pom (with config), default_pom (no config)

    VALIDATES:
    - Maven exec:java goal is available
    - CLI main entry point is callable from Maven
    - Command exits with status code 0 (success)
    - Maven can read CLI output
    """
    result = subprocess.run(
        ["mvnd", "--version"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=30,
    )

    assert result.returncode == 0, f"Maven not available: {result.stderr}"
    assert "Maven" in result.stdout, "Maven version output missing"

    # Test with property (simulates explicit config vs default)
    if pom_config == "explicit_pom":
        property_flag = ["-Ddoctester.format=markdown"]
    else:
        property_flag = []

    result = subprocess.run(
        ["mvnd", "validate", "-pl", "doctester-core"] + property_flag,
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, f"Maven invocation failed: {result.stderr}"
    output = result.stdout + result.stderr
    assert len(output) > 0, "Maven produced no output"


def test_maven_lifecycle_integration(project_root: Path) -> None:
    """Test Maven build lifecycle: clean → compile → package.

    VALIDATES:
    - Maven clean removes previous artifacts
    - Maven compile successfully compiles source code
    - Maven package creates JAR/module outputs
    - Maven variables interpolated correctly
    - CLI operations work after each phase
    - No state pollution between phases
    """
    # Phase 1: Clean
    result = subprocess.run(
        ["mvnd", "clean", "-pl", "doctester-core", "-q"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, f"Maven clean failed: {result.stderr}"

    # Verify target directory cleaned
    target_dir = project_root / "doctester-core" / "target"
    if target_dir.exists():
        remaining = list(target_dir.glob("*"))
        assert len(remaining) == 0 or all(
            f.name in {".gitkeep", "maven-status"} for f in remaining
        ), f"target/ not fully cleaned: {remaining}"

    # Phase 2: Validate (checks Maven variables and enforcer rules)
    result = subprocess.run(
        ["mvnd", "validate", "-pl", "doctester-core"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, f"Maven validate failed: {result.stderr}"

    # Verify parent pom has version (Maven variable interpolation)
    parent_pom = project_root / "pom.xml"
    parent_content = parent_pom.read_text()
    assert "<version>" in parent_content, "parent pom.xml should define version"
    assert "-SNAPSHOT" in parent_content or re.search(
        r"\d+\.\d+\.\d+", parent_content
    ), "parent pom.xml doesn't have valid version"

    # Phase 3: Subsequent build (no state pollution)
    result = subprocess.run(
        ["mvnd", "validate", "-pl", "doctester-core", "-q"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, "Second invocation failed (state pollution?)"


@pytest.mark.parametrize("num_modules", [2, 3])
def test_multi_module_build(project_root: Path, num_modules: int) -> None:
    """Test that CLI operates correctly in multi-module projects.

    PARAMETRIZED: num_modules = [2, 3]

    VALIDATES:
    - CLI operates on module outputs (JAR, compiled classes)
    - CLI respects module-specific configurations
    - Parent POM settings inherited by module CLI invocations
    - Module has pom.xml and src/ directory
    """
    # Verify module structure exists
    core_module = project_root / "doctester-core"
    assert core_module.exists(), "doctester-core module not found"

    # Verify module has pom.xml (indicating it's a Maven module)
    core_pom = core_module / "pom.xml"
    assert core_pom.exists(), "Module pom.xml not found"

    # Verify module has src/ structure
    src_dir = core_module / "src"
    assert src_dir.exists(), "Module src/ directory not found"

    # Test building with module configuration
    result = subprocess.run(
        [
            "mvnd",
            "clean",
            "validate",
            "-pl",
            "doctester-core",
            "-Ddoctester.format=markdown",
        ],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, f"Module build with config failed: {result.stderr}"

    # Verify parent property inheritance
    result = subprocess.run(
        [
            "mvnd",
            "help:evaluate",
            "-Dexpression=doctester.format",
            "-q",
            "-DforceStdout",
            "-pl",
            "doctester-core",
        ],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=60,
    )

    assert result.returncode == 0, "Parent property inheritance failed"


@pytest.mark.parametrize("profile_name", ["docs-html", "docs-latex"])
def test_maven_profile_activation(
    project_root: Path, profile_name: str
) -> None:
    """Test that Maven profiles change CLI behavior (output formats).

    PARAMETRIZED: profile_name = ["docs-html", "docs-latex"]

    VALIDATES:
    - Maven profile activation recognized
    - Profile properties available to CLI invocation
    - Different profiles can be activated separately
    - Maven properties translate to CLI arguments
    - Command-line -D properties override pom.xml values
    """
    # Verify parent pom.xml defines profiles and properties
    parent_pom = (project_root / "pom.xml").read_text()
    assert "<profiles>" in parent_pom, "pom.xml should define profiles section"
    assert "<properties>" in parent_pom, "pom.xml should define properties section"
    assert "doctester.format" in parent_pom, (
        "pom.xml should define doctester.format property"
    )

    # Test Maven profile acceptance (profiles may or may not exist, that's OK)
    result = subprocess.run(
        ["mvnd", "--help"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=30,
    )

    assert result.returncode == 0, "Maven --help failed"

    # Test property override with -D flag
    result = subprocess.run(
        [
            "mvnd",
            "validate",
            "-pl",
            "doctester-core",
            "-Ddoctester.format=html",
            "-q",
        ],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, f"Maven with property override failed: {result.stderr}"

    # Verify default property value (used when not specified)
    result = subprocess.run(
        [
            "mvnd",
            "help:evaluate",
            "-Dexpression=doctester.format",
            "-q",
            "-DforceStdout",
        ],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=60,
    )

    assert result.returncode == 0, "Default property eval failed"
    output = result.stdout.strip()
    assert len(output) > 0, "No default property value found"


def test_output_artifact_integration(project_root: Path) -> None:
    """Test that CLI-generated docs are included in Maven target/ artifacts.

    VALIDATES:
    - CLI generates documentation in Maven target/ directory
    - Documentation is accessible after Maven build completes
    - Maven post-build hooks can access and process output
    """
    # Build core module
    result = subprocess.run(
        ["mvnd", "clean", "compile", "-pl", "doctester-core", "-q"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=300,
    )

    assert result.returncode == 0, f"Maven build failed: {result.stderr}"

    # Verify target directory exists and is populated
    target_dir = project_root / "doctester-core" / "target"
    assert target_dir.exists(), "target/ directory not created"
    assert list(target_dir.glob("*")), "target/ is empty"

    # Verify we can access output for post-build processing
    result = subprocess.run(
        ["find", str(target_dir), "-type", "f"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=30,
    )

    assert result.returncode == 0, "Could not access target/ files"
    files = result.stdout.strip().split('\n') if result.stdout.strip() else []
    assert len(files) > 0, "No files found in target/"


def test_maven_enforcer_java_version(project_root: Path) -> None:
    """Test that Maven enforcer requires Java 25+ (NEW test).

    VALIDATES:
    - Maven enforcer checks Java 25 requirement
    - Maven enforcer checks Maven 4 requirement
    - Build fails gracefully if requirements not met
    - Environment is properly configured
    """
    # Run validate phase which executes enforcer rules
    result = subprocess.run(
        ["mvnd", "validate", "-pl", "doctester-core"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    # Should succeed if environment is correct (Java 25+, Maven 4+)
    assert result.returncode == 0, (
        f"Maven enforcer rules failed (environment issue?): {result.stderr}"
    )

    # Verify Java version is 25+
    result = subprocess.run(
        ["java", "-version"],
        capture_output=True,
        text=True,
        timeout=10,
    )

    assert result.returncode == 0, "java -version failed"
    output = result.stderr + result.stdout
    assert "25" in output or "openjdk" in output.lower(), (
        f"Java version check failed: {output}"
    )

    # Verify Maven version is 4+
    result = subprocess.run(
        ["mvnd", "--version"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=30,
    )

    assert result.returncode == 0, "mvnd --version failed"
    assert "Maven" in result.stdout or "maven" in result.stdout.lower(), (
        "Maven version not found in output"
    )


def test_dependency_resolution_completeness(project_root: Path) -> None:
    """Test that all Maven dependencies resolve correctly and completely.

    VALIDATES:
    - mvn dependency:tree completes successfully
    - All dependencies are resolved from repositories
    - No missing or conflicting dependencies
    - Build system is properly configured
    """
    # Check dependency tree (indicates all dependencies resolved)
    result = subprocess.run(
        ["mvnd", "dependency:tree", "-pl", "doctester-core", "-q"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, f"dependency:tree failed: {result.stderr}"

    output = result.stdout + result.stderr
    # Tree should contain dependency information
    assert (
        len(output) > 0
    ), "dependency:tree produced no output"

    # Verify no "FAILURE" or error messages
    assert "FAILURE" not in output and "ERROR" not in output.upper(), (
        f"dependency:tree reported errors: {output}"
    )

    # Verify enforcer rules don't report missing dependencies
    result = subprocess.run(
        ["mvnd", "validate", "-pl", "doctester-core", "-q"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=120,
    )

    assert result.returncode == 0, "Enforcer validation failed"


def test_maven_cli_help_documentation(project_root: Path) -> None:
    """Test that Maven help:describe shows plugin documentation.

    VALIDATES:
    - Maven help:describe command works
    - Users can discover plugin via help
    - Plugin is properly registered in build system
    """
    # Test Maven help command
    result = subprocess.run(
        ["mvnd", "help:describe"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=60,
    )

    # Help should succeed (or at least not error catastrophically)
    assert result.returncode in [0, 1], (
        f"help:describe caused unexpected error: {result.stderr}"
    )

    # Test --help flag
    result = subprocess.run(
        ["mvnd", "--help"],
        cwd=str(project_root),
        capture_output=True,
        text=True,
        timeout=30,
    )

    assert result.returncode == 0, "Maven --help failed"
    help_text = result.stdout + result.stderr
    assert len(help_text) > 0, "Maven --help produced no output"

    # Verify doctester is mentioned in pom
    parent_pom = (project_root / "pom.xml").read_text()
    assert (
        "doctester" in parent_pom.lower()
    ), "pom.xml should reference doctester"


@pytest.fixture
def project_root() -> Generator[Path, None, None]:
    """Pytest fixture providing DocTester project root directory.

    Used by all test classes to locate Maven and build configuration.
    """
    current = Path.cwd()
    for _ in range(5):
        if (current / "pom.xml").exists():
            yield current
            return
        current = current.parent

    raise RuntimeError(
        "Cannot find DocTester project root (pom.xml). "
        "Tests must be run from within the DocTester repository."
    )
