"""Tests for dtr build command - Maven orchestration.

Tests verify:
- Maven runner initialization and Maven executable detection
- Module parsing from pom.xml
- Build command option handling (goals, profiles, properties, modules)
- Export validation after build
- Error handling and user-friendly messages
"""

import subprocess
from pathlib import Path
from typing import Generator
from unittest.mock import Mock, patch, MagicMock

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app
from dtr_cli.managers.maven_manager import (
    MavenRunner,
    MavenBuildConfig,
)
from dtr_cli.cli_errors import (
    PomNotFoundError,
    MavenNotFoundError,
)


# ============================================================================
# FIXTURES
# ============================================================================


@pytest.fixture
def runner() -> CliRunner:
    """Typer CLI test runner."""
    return CliRunner()


@pytest.fixture
def tmp_pom(tmp_path: Path) -> Path:
    """Create a temporary pom.xml with multi-module structure."""
    pom_content = """<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         http://maven.apache.org/maven-v4_0_0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test.org</groupId>
    <artifactId>test-project</artifactId>
    <version>1.0.0</version>
    <packaging>pom</packaging>
    <modules>
        <module>module-a</module>
        <module>module-b</module>
        <module>module-c</module>
    </modules>
</project>"""
    pom_file = tmp_path / "pom.xml"
    pom_file.write_text(pom_content)
    return pom_file


# ============================================================================
# UNIT TESTS: MavenRunner Initialization
# ============================================================================


def test_maven_runner_finds_pom(tmp_pom: Path) -> None:
    """Test that MavenRunner finds pom.xml correctly."""
    maven = MavenRunner(tmp_pom.parent)
    assert maven.pom_path == tmp_pom
    assert maven.project_root == tmp_pom.parent


def test_maven_runner_fails_without_pom(tmp_path: Path) -> None:
    """Test that MavenRunner fails loudly if pom.xml not found."""
    with pytest.raises(FileNotFoundError) as exc_info:
        MavenRunner(tmp_path)
    assert "pom.xml not found" in str(exc_info.value)


def test_maven_runner_parses_modules(tmp_pom: Path) -> None:
    """Test that MavenRunner correctly parses modules from pom.xml."""
    maven = MavenRunner(tmp_pom.parent)
    modules = maven.get_available_modules()
    assert modules == ["module-a", "module-b", "module-c"]


def test_maven_runner_detects_multi_module(tmp_pom: Path) -> None:
    """Test that MavenRunner detects multi-module projects."""
    maven = MavenRunner(tmp_pom.parent)
    assert maven.is_multi_module() is True


def test_maven_runner_single_module_project(tmp_path: Path) -> None:
    """Test that MavenRunner handles single-module projects."""
    pom_content = """<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>single</artifactId>
    <version>1.0</version>
</project>"""
    pom_file = tmp_path / "pom.xml"
    pom_file.write_text(pom_content)

    maven = MavenRunner(tmp_path)
    assert maven.is_multi_module() is False
    assert maven.get_available_modules() == []


def test_maven_runner_export_dir_path(tmp_pom: Path) -> None:
    """Test that MavenRunner returns correct export directory path."""
    maven = MavenRunner(tmp_pom.parent)
    export_dir = maven.get_export_dir()
    expected = tmp_pom.parent / "target" / "site" / "dtr"
    assert export_dir == expected


# ============================================================================
# UNIT TESTS: MavenBuildConfig
# ============================================================================


def test_build_config_defaults() -> None:
    """Test that MavenBuildConfig sets sensible defaults."""
    config = MavenBuildConfig()
    assert config.goals == ["clean", "verify"]
    assert config.profiles == []
    assert config.properties == {}
    assert config.modules is None
    assert config.verbose is False
    assert config.timeout == 600


def test_build_config_custom_values() -> None:
    """Test that MavenBuildConfig accepts custom values."""
    config = MavenBuildConfig(
        goals=["test"],
        profiles=["docs-html"],
        properties={"key": "value"},
        modules=["mod-a"],
        verbose=True,
        timeout=1200,
    )
    assert config.goals == ["test"]
    assert config.profiles == ["docs-html"]
    assert config.properties == {"key": "value"}
    assert config.modules == ["mod-a"]
    assert config.verbose is True
    assert config.timeout == 1200


# ============================================================================
# UNIT TESTS: Maven Executable Detection
# ============================================================================


@patch("subprocess.run")
def test_maven_runner_prefers_mvnd(mock_run: Mock, tmp_pom: Path) -> None:
    """Test that MavenRunner prefers mvnd over mvn."""
    # Mock mvnd succeeds
    mock_run.side_effect = [
        MagicMock(returncode=0),  # mvnd --version succeeds
    ]
    maven = MavenRunner(tmp_pom.parent)
    assert maven.maven_exe == "mvnd"


@patch("subprocess.run")
def test_maven_runner_fallback_to_mvn(
    mock_run: Mock, tmp_pom: Path
) -> None:
    """Test that MavenRunner falls back to mvn if mvnd unavailable."""
    # Mock mvnd fails, mvn succeeds
    mock_run.side_effect = [
        FileNotFoundError(),  # mvnd not found
        MagicMock(returncode=0),  # mvn --version succeeds
    ]
    maven = MavenRunner(tmp_pom.parent)
    assert maven.maven_exe == "mvn"


@patch("subprocess.run")
def test_maven_runner_fails_if_no_maven(
    mock_run: Mock, tmp_pom: Path
) -> None:
    """Test that MavenRunner fails if neither mvnd nor mvn available."""
    # Mock both fail
    mock_run.side_effect = FileNotFoundError()
    with pytest.raises(RuntimeError) as exc_info:
        MavenRunner(tmp_pom.parent)
    assert "Neither mvnd nor mvn found" in str(exc_info.value)


# ============================================================================
# UNIT TESTS: Build Command Construction
# ============================================================================


def test_build_command_default_goals(tmp_pom: Path) -> None:
    """Test that build command uses default goals (clean verify)."""
    config = MavenBuildConfig()
    assert "clean" in config.goals
    assert "verify" in config.goals


def test_build_command_custom_goals() -> None:
    """Test that build command accepts custom goals."""
    config = MavenBuildConfig(goals=["test", "package"])
    assert config.goals == ["test", "package"]


def test_build_command_profile_handling() -> None:
    """Test that profiles are handled correctly."""
    config = MavenBuildConfig(profiles=["docs-html", "docs-latex"])
    assert "docs-html" in config.profiles
    assert "docs-latex" in config.profiles


def test_build_command_property_handling() -> None:
    """Test that properties are passed correctly."""
    config = MavenBuildConfig(
        properties={"key1": "value1", "key2": "value2"}
    )
    assert config.properties["key1"] == "value1"
    assert config.properties["key2"] == "value2"


def test_build_command_module_selection() -> None:
    """Test that module selection works."""
    config = MavenBuildConfig(modules=["module-a", "module-b"])
    assert config.modules == ["module-a", "module-b"]


# ============================================================================
# CLI INTEGRATION TESTS
# ============================================================================


def test_cli_build_help(runner: CliRunner) -> None:
    """Test that dtr build --help works."""
    result = runner.invoke(app, ["build", "--help"])
    assert result.exit_code == 0
    # Check for key options instead of exact help text (which may vary by Typer version)
    assert "--goals" in result.stdout
    assert "--profiles" in result.stdout
    assert "--modules" in result.stdout
    assert "Maven" in result.stdout


def test_cli_build_missing_pom(runner: CliRunner, tmp_path: Path) -> None:
    """Test that dtr build fails gracefully if pom.xml missing."""
    result = runner.invoke(
        app, ["build", "--project-dir", str(tmp_path)]
    )
    assert result.exit_code != 0
    # Error could be in stdout or stderr depending on Typer version
    output = result.stdout + (result.stderr or "")
    assert "pom.xml not found" in output or "pom.xml" in output


@patch("dtr_cli.managers.maven_manager.MavenRunner")
def test_cli_build_shows_modules(
    mock_maven_class: Mock, runner: CliRunner, tmp_pom: Path
) -> None:
    """Test that dtr build shows available modules."""
    mock_maven = MagicMock()
    mock_maven.is_multi_module.return_value = True
    mock_maven.get_available_modules.return_value = [
        "module-a",
        "module-b",
    ]
    mock_maven.build.return_value = 0
    mock_maven.get_export_dir.return_value = tmp_pom.parent / "target"
    mock_maven_class.return_value = mock_maven

    result = runner.invoke(app, ["build", "--project-dir", str(tmp_pom.parent)])
    # Should show modules list
    assert "module-a" in result.stdout or "module-b" in result.stdout


@patch("dtr_cli.managers.maven_manager.MavenRunner.build")
def test_cli_build_success(
    mock_build: Mock, runner: CliRunner, tmp_pom: Path
) -> None:
    """Test that dtr build completes without error on successful build."""
    # Mock build returns 0 (success)
    mock_build.return_value = 0

    # Create exports directory so validation passes
    target_dir = tmp_pom.parent / "target" / "site" / "dtr"
    target_dir.mkdir(parents=True, exist_ok=True)
    (target_dir / "test.html").write_text("<html></html>")

    result = runner.invoke(app, ["build", "--project-dir", str(tmp_pom.parent)])
    # Should complete without error or show "Exports generated"
    assert "Exports generated" in result.stdout or "Maven build completed" in result.stdout


@patch("dtr_cli.managers.maven_manager.MavenRunner")
def test_cli_build_failure(
    mock_maven_class: Mock, runner: CliRunner, tmp_pom: Path
) -> None:
    """Test that dtr build fails appropriately when Maven build fails."""
    mock_maven = MagicMock()
    mock_maven.is_multi_module.return_value = False
    mock_maven.get_available_modules.return_value = []
    mock_maven.build.return_value = 1  # Non-zero = failure
    mock_maven_class.return_value = mock_maven

    result = runner.invoke(app, ["build", "--project-dir", str(tmp_pom.parent)])
    assert result.exit_code != 0
    assert "failed" in result.stdout.lower()


# ============================================================================
# PARAMETRIZED TESTS
# ============================================================================


@pytest.mark.parametrize(
    "goals",
    [
        ["test"],
        ["compile"],
        ["package"],
        ["clean", "verify"],
    ],
)
def test_build_config_various_goals(goals: list[str]) -> None:
    """Test that various Maven goal combinations work."""
    config = MavenBuildConfig(goals=goals)
    assert config.goals == goals


@pytest.mark.parametrize(
    "profiles",
    [
        ["docs-html"],
        ["docs-latex"],
        ["docs-html", "docs-latex"],
    ],
)
def test_build_config_various_profiles(profiles: list[str]) -> None:
    """Test that various profile combinations work."""
    config = MavenBuildConfig(profiles=profiles)
    assert config.profiles == profiles


@pytest.mark.parametrize(
    "modules",
    [
        ["module-a"],
        ["module-a", "module-b"],
        ["module-a", "module-b", "module-c"],
    ],
)
def test_build_config_various_modules(modules: list[str]) -> None:
    """Test that various module selections work."""
    config = MavenBuildConfig(modules=modules)
    assert config.modules == modules


# ============================================================================
# EDGE CASE TESTS
# ============================================================================


def test_pom_without_modules(tmp_path: Path) -> None:
    """Test handling of pom.xml without <modules> section."""
    pom_content = """<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>test</artifactId>
    <version>1.0</version>
</project>"""
    (tmp_path / "pom.xml").write_text(pom_content)

    maven = MavenRunner(tmp_path)
    assert maven.get_available_modules() == []
    assert maven.is_multi_module() is False


def test_pom_with_empty_modules(tmp_path: Path) -> None:
    """Test handling of pom.xml with empty <modules> section."""
    pom_content = """<?xml version="1.0"?>
<project xmlns="http://maven.apache.org/POM/4.0.0">
    <modelVersion>4.0.0</modelVersion>
    <groupId>test</groupId>
    <artifactId>test</artifactId>
    <version>1.0</version>
    <modules>
    </modules>
</project>"""
    (tmp_path / "pom.xml").write_text(pom_content)

    maven = MavenRunner(tmp_path)
    assert maven.get_available_modules() == []


def test_build_config_empty_properties() -> None:
    """Test that empty properties dict is handled correctly."""
    config = MavenBuildConfig(properties={})
    assert config.properties == {}


def test_build_config_zero_timeout() -> None:
    """Test that timeout can be set to 0 (infinite)."""
    config = MavenBuildConfig(timeout=0)
    assert config.timeout == 0
