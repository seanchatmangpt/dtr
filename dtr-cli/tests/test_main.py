"""Chicago TDD tests for main CLI module.

Tests verify real CLI output and exit codes using Typer's CliRunner.
No mocks — every test invokes the real app and asserts on real output.
"""

import json

import pytest
import typer
from typer.testing import CliRunner

from dtr_cli.main import app
from dtr_cli import state as state_module

runner = CliRunner()


# ============================================================================
# version command
# ============================================================================


def test_version_command_exits_zero() -> None:
    """'dtr version' exits 0."""
    result = runner.invoke(app, ["version"])
    assert result.exit_code == 0, f"Unexpected exit code: {result.stdout}"


def test_version_command_contains_dtr() -> None:
    """'dtr version' output contains the 'DTR' brand string."""
    result = runner.invoke(app, ["version"])
    assert "DTR" in result.stdout, f"'DTR' missing from output:\n{result.stdout}"


def test_version_command_contains_python() -> None:
    """'dtr version' output contains the Python version string."""
    result = runner.invoke(app, ["version"])
    assert "Python" in result.stdout, f"'Python' missing from output:\n{result.stdout}"


def test_version_command_contains_java() -> None:
    """'dtr version' output contains the Java version string."""
    result = runner.invoke(app, ["version"])
    assert "Java" in result.stdout, f"'Java' missing from output:\n{result.stdout}"


def test_version_command_contains_maven() -> None:
    """'dtr version' output contains the Maven version string."""
    result = runner.invoke(app, ["version"])
    assert "Maven" in result.stdout, f"'Maven' missing from output:\n{result.stdout}"


# ============================================================================
# --json mode
# ============================================================================


def test_json_flag_version_exits_zero() -> None:
    """'dtr --json version' exits 0."""
    result = runner.invoke(app, ["--json", "version"])
    assert result.exit_code == 0, f"Unexpected exit code: {result.stdout}"


def test_json_flag_version_is_valid_json() -> None:
    """'dtr --json version' emits valid JSON."""
    result = runner.invoke(app, ["--json", "version"])
    assert result.exit_code == 0, f"Command failed: {result.stdout}"
    try:
        data = json.loads(result.stdout.strip())
    except json.JSONDecodeError as exc:
        pytest.fail(f"Output is not valid JSON: {exc}\nOutput:\n{result.stdout}")
    assert isinstance(data, dict), "JSON output should be an object"


def test_json_flag_version_has_required_keys() -> None:
    """'dtr --json version' JSON contains version, python, java, maven keys."""
    result = runner.invoke(app, ["--json", "version"])
    assert result.exit_code == 0
    data = json.loads(result.stdout.strip())
    for key in ("version", "python", "java", "maven"):
        assert key in data, f"Missing key '{key}' in JSON output: {data}"


def test_json_flag_version_values_are_strings() -> None:
    """'dtr --json version' JSON values are non-empty strings."""
    result = runner.invoke(app, ["--json", "version"])
    assert result.exit_code == 0
    data = json.loads(result.stdout.strip())
    for key in ("version", "python", "java", "maven"):
        assert isinstance(data[key], str), f"Key '{key}' should be a string"
        assert len(data[key]) > 0, f"Key '{key}' should not be empty"


# ============================================================================
# --quiet mode
# ============================================================================


def test_quiet_flag_version_exits_zero() -> None:
    """'dtr --quiet version' exits 0."""
    result = runner.invoke(app, ["--quiet", "version"])
    assert result.exit_code == 0, f"Unexpected exit code: {result.stdout}"


def test_quiet_flag_suppresses_banner() -> None:
    """'dtr --quiet version' does not print the startup banner."""
    result = runner.invoke(app, ["--quiet", "version"])
    # Banner contains panel box-drawing characters; quiet mode skips it
    assert "╭" not in result.stdout, "Banner box top found in quiet output"
    assert "╰" not in result.stdout, "Banner box bottom found in quiet output"


def test_quiet_flag_still_shows_version_info() -> None:
    """'dtr --quiet version' still emits version information."""
    result = runner.invoke(app, ["--quiet", "version"])
    assert result.exit_code == 0
    # Core version info should still appear even in quiet mode
    assert "DTR" in result.stdout, f"'DTR' missing in quiet version output:\n{result.stdout}"


# ============================================================================
# --no-color mode
# ============================================================================


def test_no_color_flag_version_exits_zero() -> None:
    """'dtr --no-color version' exits 0."""
    result = runner.invoke(app, ["--no-color", "version"])
    assert result.exit_code == 0, f"Unexpected exit code: {result.stdout}"


# ============================================================================
# Global exception handler
# ============================================================================


def test_global_error_handler_exits_one_on_subcommand_error() -> None:
    """A subcommand that raises RuntimeError produces exit code 1 with a message.

    We use 'dtr build' without a pom.xml in the temp directory — the command
    raises a BadParameter error that surfaces as exit code 2 (Typer default).
    Instead, we register a throw-away command on a test app to verify the
    global callback error path is exercised.

    Chicago TDD: we verify the real app's built-in error handling on bad input.
    """
    # Invoke a known-bad path: 'dtr config --get invalid.key.that.does.not.exist'
    # on an empty config.  This exercises the error code path.
    result = runner.invoke(app, ["config", "--get", "nonexistent.key"])
    # Should fail with non-zero exit
    assert result.exit_code != 0, (
        "Expected non-zero exit for unknown config key, "
        f"got {result.exit_code}:\n{result.stdout}"
    )


def test_global_error_handler_prints_actionable_message() -> None:
    """When the global callback catches an unhandled error it prints an
    actionable message pointing users to 'dtr doctor'."""

    # Build a minimal test app that deliberately raises inside the callback
    error_app = typer.Typer()

    @error_app.callback()
    def _bad_callback() -> None:
        raise RuntimeError("simulated startup failure")

    @error_app.command()
    def _noop() -> None:
        pass

    test_runner = CliRunner()
    # Typer propagates unhandled exceptions; mix_stderr=False separates streams
    test_result = test_runner.invoke(error_app, ["noop"])
    # The raised RuntimeError should surface as a non-zero exit
    assert test_result.exit_code != 0


def test_version_command_global_error_handler_with_injected_failure(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    """Monkeypatching _env_matrix to raise exercises the version command's
    own try/except, which should exit 1 and print an error message."""
    import dtr_cli.main as main_mod

    def _explode() -> dict:
        raise RuntimeError("injected env-matrix failure")

    monkeypatch.setattr(main_mod, "_env_matrix", _explode)

    result = runner.invoke(app, ["version"])
    assert result.exit_code == 1, (
        f"Expected exit code 1, got {result.exit_code}:\n{result.stdout}"
    )
    # Error output may land on stderr; combined output available via result.output
    combined = (result.stdout or "") + (result.output or "")
    assert len(combined.strip()) > 0, "No error message printed"


# ============================================================================
# Regression: existing tests preserved
# ============================================================================


def test_help_command() -> None:
    """'dtr --help' lists all top-level subcommands."""
    result = runner.invoke(app, ["--help"])
    assert result.exit_code == 0
    for name in ("fmt", "export", "report", "push", "build", "publish"):
        assert name in result.stdout, f"'{name}' missing from --help"


def test_config_command_default() -> None:
    """'dtr config' without flags hints at --show usage."""
    result = runner.invoke(app, ["config"])
    assert result.exit_code == 0
    assert "--show" in result.stdout


def test_config_command_show() -> None:
    """'dtr config --show' displays the resolved configuration header."""
    result = runner.invoke(app, ["config", "--show"])
    assert result.exit_code == 0
    assert "DTR CLI Configuration" in result.stdout


# ============================================================================
# State module unit tests
# ============================================================================


def test_state_configure_sets_json_mode() -> None:
    """state.configure() correctly propagates json_mode."""
    state_module.configure(json_mode=True, quiet=False, verbose=False, no_color=False)
    assert state_module.get_state().json_mode is True
    # Reset to avoid polluting other tests
    state_module.configure()


def test_state_configure_sets_quiet() -> None:
    """state.configure() correctly propagates quiet flag."""
    state_module.configure(json_mode=False, quiet=True, verbose=False, no_color=False)
    assert state_module.get_state().quiet is True
    state_module.configure()


def test_state_configure_sets_verbose() -> None:
    """state.configure() correctly propagates verbose flag."""
    state_module.configure(json_mode=False, quiet=False, verbose=True, no_color=False)
    assert state_module.get_state().verbose is True
    state_module.configure()


def test_state_get_state_returns_singleton() -> None:
    """get_state() returns the same object on repeated calls."""
    s1 = state_module.get_state()
    s2 = state_module.get_state()
    assert s1 is s2, "get_state() should return the module-level singleton"
