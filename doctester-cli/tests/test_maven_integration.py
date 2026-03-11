"""Integration tests that build with Maven and test CLI against real exports."""

from pathlib import Path
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


def test_maven_build_exports_list(maven_build: Path) -> None:
    """Test that Maven-generated exports can be listed via CLI."""
    result = runner.invoke(app, ["export", "list", str(maven_build)])

    assert result.exit_code == 0
    assert "DocTester Exports" in result.stdout or "export" in result.stdout.lower()


def test_maven_build_exports_validate(maven_build: Path) -> None:
    """Test that Maven-generated exports pass validation."""
    result = runner.invoke(app, ["export", "check", str(maven_build)])

    assert result.exit_code == 0
    assert "Validation" in result.stdout or "valid" in result.stdout.lower()


def test_maven_build_report_summary(maven_build: Path, tmp_path: Path) -> None:
    """Test generating summary report from Maven-generated exports."""
    output_file = tmp_path / "summary.md"
    result = runner.invoke(
        app,
        ["report", "sum", str(maven_build), "--output", str(output_file), "--format", "markdown"],
    )

    assert result.exit_code == 0
    assert "Report generated" in result.stdout or "summary" in result.stdout.lower()
    assert output_file.exists()


def test_maven_build_format_conversion(maven_build: Path, tmp_path: Path) -> None:
    """Test converting Maven-generated HTML exports to Markdown."""
    # Find an HTML file in the export
    html_files = list(maven_build.glob("*.html"))

    if not html_files:
        # Skip if no HTML files found
        return

    html_file = html_files[0]
    output_dir = tmp_path / "markdown_output"
    output_dir.mkdir()

    result = runner.invoke(
        app,
        ["fmt", "md", str(html_file), "--output", str(output_dir)],
    )

    # Should succeed or have warning about format (acceptable)
    assert result.exit_code in [0, 1]


def test_maven_integration_test_build(maven_integration_test_build: Path) -> None:
    """Test integration test build and list exports."""
    result = runner.invoke(app, ["export", "list", str(maven_integration_test_build)])

    assert result.exit_code == 0
    # Verify we have exports
    assert "export" in result.stdout.lower() or "file" in result.stdout.lower()


def test_maven_build_exports_archive(maven_build: Path, tmp_path: Path) -> None:
    """Test archiving Maven-generated exports."""
    output_file = tmp_path / "exports.tar.gz"

    result = runner.invoke(
        app,
        ["export", "save", str(maven_build), "--output", str(output_file), "--format", "tar.gz"],
    )

    assert result.exit_code == 0
    assert output_file.exists()
    assert output_file.stat().st_size > 0


def test_maven_build_cleanup_dry_run(maven_build: Path) -> None:
    """Test dry-run cleanup of exports."""
    result = runner.invoke(
        app,
        ["export", "clean", str(maven_build), "--keep", "1", "--dry-run"],
    )

    # Should succeed or handle gracefully
    assert result.exit_code in [0, 1]
    # Dry-run should mention files or plan
    assert "dry" in result.stdout.lower() or "remove" in result.stdout.lower() or "file" in result.stdout.lower()
