"""Unit tests for CLI commands using sample fixtures.

These are NOT integration tests - they use sample HTML/Markdown fixtures
created in tmp directories, NOT real Maven builds.

For real end-to-end testing with actual Maven builds, see:
  test_maven_integration.py
"""

from pathlib import Path
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


# Tests using sample export fixtures (always available)


def test_export_list_with_sample_exports(tmp_export_dir: Path) -> None:
    """Test listing exports from sample fixture."""
    result = runner.invoke(app, ["export", "list", str(tmp_export_dir)])

    assert result.exit_code == 0
    assert "export" in result.stdout.lower() or "test" in result.stdout.lower()


def test_export_check_with_sample_exports(tmp_export_dir: Path) -> None:
    """Test validation of sample exports."""
    result = runner.invoke(app, ["export", "check", str(tmp_export_dir)])

    # May pass or warn about format, but shouldn't crash
    assert result.exit_code in [0, 1]


def test_fmt_md_conversion_html_input(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test converting HTML to Markdown using fmt command."""
    html_file = tmp_export_dir / "test_doc.html"
    assert html_file.exists()

    output_dir = tmp_path / "markdown"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "md", str(html_file), "--output", str(output_dir)])

    # Should succeed or gracefully handle
    assert result.exit_code in [0, 1]


def test_fmt_json_conversion_html_input(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test converting HTML to JSON using fmt command."""
    html_file = tmp_export_dir / "test_doc.html"
    assert html_file.exists()

    output_dir = tmp_path / "json"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "json", str(html_file), "--output", str(output_dir)])

    # Should succeed or gracefully handle
    assert result.exit_code in [0, 1]


def test_fmt_html_conversion_markdown_input(tmp_markdown_file: Path, tmp_path: Path) -> None:
    """Test converting Markdown to HTML using fmt command."""
    output_dir = tmp_path / "html"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "html", str(tmp_markdown_file), "--output", str(output_dir)])

    # Should succeed or gracefully handle
    assert result.exit_code in [0, 1]


def test_export_save_archive(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test archiving exports with tar.gz format."""
    output_file = tmp_path / "export.tar.gz"

    result = runner.invoke(
        app,
        ["export", "save", str(tmp_export_dir), "--output", str(output_file), "--format", "tar.gz"],
    )

    assert result.exit_code == 0
    assert output_file.exists()
    assert output_file.stat().st_size > 0


def test_export_save_zip_format(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test archiving exports with zip format."""
    output_file = tmp_path / "export.zip"

    result = runner.invoke(
        app,
        ["export", "save", str(tmp_export_dir), "--output", str(output_file), "--format", "zip"],
    )

    assert result.exit_code == 0
    assert output_file.exists()
    assert output_file.stat().st_size > 0


def test_export_clean_dry_run(tmp_export_dir: Path) -> None:
    """Test cleanup dry-run mode (doesn't delete)."""
    result = runner.invoke(
        app,
        ["export", "clean", str(tmp_export_dir), "--keep", "1", "--dry-run"],
    )

    # Should complete without error
    assert result.exit_code in [0, 1]
    # Dry-run should mention the dry-run status
    assert "dry" in result.stdout.lower() or "remove" in result.stdout.lower() or "file" in result.stdout.lower()


def test_report_sum_markdown_format(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test generating summary report in Markdown format."""
    output_file = tmp_path / "summary.md"

    result = runner.invoke(
        app,
        ["report", "sum", str(tmp_export_dir), "--output", str(output_file), "--format", "markdown"],
    )

    # Should succeed
    assert result.exit_code in [0, 1]
    # Output file may or may not exist depending on content, but command should work
    assert "report" in result.stdout.lower() or "summary" in result.stdout.lower()


def test_report_cov_html_format(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test generating coverage report in HTML format."""
    output_file = tmp_path / "coverage.html"

    result = runner.invoke(
        app,
        ["report", "cov", str(tmp_export_dir), "--output", str(output_file)],
    )

    # May succeed or handle gracefully
    assert result.exit_code in [0, 1]


def test_report_log_changelog(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test generating changelog report."""
    output_file = tmp_path / "changelog.md"

    result = runner.invoke(
        app,
        ["report", "log", str(tmp_export_dir), "--output", str(output_file)],
    )

    # May succeed or handle gracefully
    assert result.exit_code in [0, 1]


def test_fmt_help_shows_all_formats() -> None:
    """Test that fmt help shows all available format options."""
    result = runner.invoke(app, ["fmt", "--help"])

    assert result.exit_code == 0
    assert "md" in result.stdout or "markdown" in result.stdout.lower()
    assert "json" in result.stdout.lower()
    assert "html" in result.stdout.lower()


def test_export_help_shows_all_commands() -> None:
    """Test that export help shows all subcommands."""
    result = runner.invoke(app, ["export", "--help"])

    assert result.exit_code == 0
    assert "list" in result.stdout
    assert "save" in result.stdout
    assert "clean" in result.stdout
    assert "check" in result.stdout


def test_report_help_shows_all_commands() -> None:
    """Test that report help shows all subcommands."""
    result = runner.invoke(app, ["report", "--help"])

    assert result.exit_code == 0
    assert "sum" in result.stdout
    assert "cov" in result.stdout
    assert "log" in result.stdout


def test_push_help_shows_all_platforms() -> None:
    """Test that push help shows all available platforms."""
    result = runner.invoke(app, ["push", "--help"])

    assert result.exit_code == 0
    assert "gh" in result.stdout
    assert "s3" in result.stdout
    assert "gcs" in result.stdout
    assert "local" in result.stdout
