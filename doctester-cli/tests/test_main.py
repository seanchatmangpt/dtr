"""Tests for main CLI module."""

from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


def test_version_command() -> None:
    """Test version command."""
    result = runner.invoke(app, ["version"])
    assert result.exit_code == 0
    assert "DocTester CLI" in result.stdout


def test_help_command() -> None:
    """Test help command."""
    result = runner.invoke(app, ["--help"])
    assert result.exit_code == 0
    assert "fmt" in result.stdout
    assert "export" in result.stdout
    assert "report" in result.stdout
    assert "push" in result.stdout
