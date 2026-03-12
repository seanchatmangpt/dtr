"""Tests for main CLI module."""

from typer.testing import CliRunner

from dtr_cli.main import app

runner = CliRunner()


def test_version_command() -> None:
    """Test version command."""
    result = runner.invoke(app, ["version"])
    assert result.exit_code == 0
    assert "DTR CLI" in result.stdout


def test_help_command() -> None:
    """Test help command."""
    result = runner.invoke(app, ["--help"])
    assert result.exit_code == 0
    assert "fmt" in result.stdout
    assert "export" in result.stdout
    assert "report" in result.stdout
    assert "push" in result.stdout
    assert "build" in result.stdout
    assert "publish" in result.stdout


def test_config_command_default() -> None:
    """Test config command without --show."""
    result = runner.invoke(app, ["config"])
    assert result.exit_code == 0
    assert "--show" in result.stdout


def test_config_command_show() -> None:
    """Test config command with --show."""
    result = runner.invoke(app, ["config", "--show"])
    assert result.exit_code == 0
    assert "DTR CLI Configuration" in result.stdout
