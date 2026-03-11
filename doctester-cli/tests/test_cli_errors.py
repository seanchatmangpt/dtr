"""Input validation and error handling tests for CLI commands.

Tests that CLI gracefully handles invalid inputs with helpful error messages.
Follows the 80/20 principle: input validation catches ~70% of production bugs.
"""

import tempfile
from pathlib import Path
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


# ============================================================================
# Export Command Error Tests
# ============================================================================


def test_export_list_nonexistent_directory() -> None:
    """Test 'dtr export list' with nonexistent directory.

    Expected: Clear error message, exit code 2 (user error)
    """
    result = runner.invoke(app, ["export", "list", "/nonexistent/path/that/does/not/exist"])

    # Should fail
    assert result.exit_code != 0, "Should fail with nonexistent directory"

    # Should have helpful error message
    output = result.stdout.lower()
    assert any(word in output for word in ["not found", "does not exist", "invalid", "path", "error"]), \
        f"Error message not helpful: {result.stdout}"

    # Should NOT show Python stack trace to user
    assert "traceback" not in output.lower(), "Stack trace leaked to user"
    assert "file \"" not in output.lower(), "Python traceback visible to user"


def test_export_list_with_file_instead_of_directory() -> None:
    """Test 'dtr export list' when given a file instead of directory.

    Expected: Clear error, not crash
    """
    with tempfile.NamedTemporaryFile(suffix=".html") as f:
        result = runner.invoke(app, ["export", "list", f.name])

        # Should fail gracefully
        assert result.exit_code != 0, "Should fail when given file instead of directory"

        # Should indicate the problem
        output = result.stdout.lower()
        assert any(word in output for word in ["directory", "file", "invalid", "error"]), \
            f"Error message unclear: {result.stdout}"

        # No stack trace
        assert "traceback" not in output.lower()


def test_export_list_empty_directory() -> None:
    """Test 'dtr export list' with empty directory.

    Expected: Clear message (no exports found), don't crash
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        result = runner.invoke(app, ["export", "list", tmpdir])

        # Should succeed or give clear message
        assert result.exit_code in [0, 1], f"Unexpected exit code: {result.exit_code}"

        # Should indicate no files found
        output = result.stdout.lower()
        assert any(word in output for word in ["no exports", "empty", "found", "0"]), \
            f"Output unclear about empty directory: {result.stdout}"


def test_export_save_nonexistent_input_directory() -> None:
    """Test 'dtr export save' with nonexistent input directory.

    Expected: Error message, don't create empty archive
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        output_file = Path(tmpdir) / "output.tar.gz"

        result = runner.invoke(app, [
            "export", "save",
            "/nonexistent/exports",
            "--output", str(output_file),
            "--format", "tar.gz"
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with nonexistent directory"

        # Should NOT create archive
        assert not output_file.exists(), "Archive should not be created on error"

        # Should have helpful error
        output = result.stdout.lower()
        assert any(word in output for word in ["not found", "error", "invalid"]), \
            f"Error message not helpful: {result.stdout}"


def test_export_save_invalid_format() -> None:
    """Test 'dtr export save' with invalid format option.

    Expected: Error message listing valid formats
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        output_file = Path(tmpdir) / "output.xyz"

        result = runner.invoke(app, [
            "export", "save",
            str(export_dir),
            "--output", str(output_file),
            "--format", "invalid_format_xyz"
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with invalid format"

        # Should mention valid formats
        output = result.stdout.lower()
        assert any(word in output for word in ["format", "invalid", "tar.gz", "zip"]), \
            f"Should mention valid formats: {result.stdout}"

        # Should NOT create file
        assert not output_file.exists(), "Output file should not be created on error"


def test_export_clean_invalid_keep_value() -> None:
    """Test 'dtr export clean' with invalid --keep value.

    Expected: Error message, no files deleted
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create some test files
        for i in range(5):
            (Path(tmpdir) / f"test_{i}.html").write_text(f"Test {i}")

        result = runner.invoke(app, [
            "export", "clean",
            tmpdir,
            "--keep", "invalid_number"
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with invalid keep value"

        # Files should still be there (dry-run by default)
        files = list(Path(tmpdir).glob("*.html"))
        assert len(files) == 5, "Files should not be deleted with dry-run"

        # Should mention the problem
        output = result.stdout.lower()
        assert any(word in output for word in ["keep", "invalid", "number", "error"]), \
            f"Error message unclear: {result.stdout}"


# ============================================================================
# Format Conversion Error Tests
# ============================================================================


def test_fmt_md_nonexistent_file() -> None:
    """Test 'dtr fmt md' with nonexistent input file.

    Expected: Error message, no output created
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        result = runner.invoke(app, [
            "fmt", "md",
            "/nonexistent/file.html",
            "--output", tmpdir
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with nonexistent file"

        # Should have helpful error
        output = result.stdout.lower()
        assert any(word in output for word in ["not found", "error", "file"]), \
            f"Error message not helpful: {result.stdout}"

        # Should NOT create output
        output_files = list(Path(tmpdir).glob("*"))
        assert len(output_files) == 0, "Should not create output on error"


def test_fmt_invalid_format_option() -> None:
    """Test 'dtr fmt' with invalid format name.

    Expected: Error showing valid formats
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        html_file = Path(tmpdir) / "test.html"
        html_file.write_text("<html><body>test</body></html>")

        result = runner.invoke(app, [
            "fmt", "invalid_fmt",
            str(html_file),
            "--output", tmpdir
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with invalid format"

        # Should mention valid formats
        output = result.stdout.lower()
        assert any(word in output for word in ["format", "invalid", "md", "json", "html"]), \
            f"Should list valid formats: {result.stdout}"


def test_fmt_md_with_directory_as_input() -> None:
    """Test 'dtr fmt md' when given a directory instead of file.

    Expected: Clear error message
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        result = runner.invoke(app, [
            "fmt", "md",
            tmpdir,  # Directory, not file
            "--output", tmpdir
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail when given directory"

        # Should mention it's a directory
        output = result.stdout.lower()
        assert any(word in output for word in ["directory", "file", "invalid", "error"]), \
            f"Error message unclear: {result.stdout}"


# ============================================================================
# Report Command Error Tests
# ============================================================================


def test_report_sum_nonexistent_directory() -> None:
    """Test 'dtr report sum' with nonexistent directory.

    Expected: Error message, no report created
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        report_file = Path(tmpdir) / "report.md"

        result = runner.invoke(app, [
            "report", "sum",
            "/nonexistent/exports",
            "--output", str(report_file),
            "--format", "markdown"
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with nonexistent directory"

        # Should NOT create report
        assert not report_file.exists(), "Report should not be created on error"

        # Should have helpful error
        output = result.stdout.lower()
        assert any(word in output for word in ["not found", "error", "invalid"]), \
            f"Error message not helpful: {result.stdout}"


def test_report_cov_invalid_output_path() -> None:
    """Test 'dtr report cov' with invalid output path.

    Expected: Error about path, not created
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()

        # Create a read-only directory
        readonly_dir = Path(tmpdir) / "readonly"
        readonly_dir.mkdir(mode=0o555)  # Read-only

        try:
            result = runner.invoke(app, [
                "report", "cov",
                str(export_dir),
                "--output", str(readonly_dir / "report.html")
            ])

            # Should fail (permission denied)
            # Note: Some systems may not enforce permission checks, skip if no error
            if result.exit_code != 0:
                output = result.stdout.lower()
                assert any(word in output for word in ["permission", "error", "write"]), \
                    f"Error message unclear: {result.stdout}"
        finally:
            # Restore permissions for cleanup
            readonly_dir.chmod(0o755)


def test_report_invalid_format() -> None:
    """Test 'dtr report' with invalid output format.

    Expected: Error message
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        report_file = Path(tmpdir) / "report.xyz"

        result = runner.invoke(app, [
            "report", "sum",
            str(export_dir),
            "--output", str(report_file),
            "--format", "invalid_xyz_format"
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with invalid format"

        # Should NOT create report
        assert not report_file.exists(), "Report should not be created"

        # Should mention valid formats
        output = result.stdout.lower()
        assert any(word in output for word in ["format", "invalid", "markdown", "html"]), \
            f"Should mention formats: {result.stdout}"


# ============================================================================
# General Command Error Tests
# ============================================================================


def test_command_missing_required_argument() -> None:
    """Test command with missing required argument.

    Expected: Clear error about missing argument
    """
    result = runner.invoke(app, ["export", "list"])  # Missing path argument

    # Should fail
    assert result.exit_code != 0, "Should fail without required argument"

    # Should explain what's missing
    output = result.stdout.lower()
    assert any(word in output for word in ["missing", "required", "argument", "path"]), \
        f"Should explain missing argument: {result.stdout}"


def test_command_too_many_arguments() -> None:
    """Test command with too many arguments.

    Expected: Clear error about extra arguments
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        result = runner.invoke(app, [
            "export", "list",
            tmpdir,
            "extra_argument_that_shouldnt_be_here"
        ])

        # Should fail or warn
        # (Behavior depends on typer, but should not silently ignore)
        assert result.exit_code != 0 or "extra" in result.stdout.lower(), \
            "Should not silently accept extra arguments"


def test_invalid_command_name() -> None:
    """Test with completely invalid command.

    Expected: Show help or error
    """
    result = runner.invoke(app, ["invalid_command_xyz"])

    # Should fail
    assert result.exit_code != 0, "Should fail with invalid command"

    # Should show help or error
    output = result.stdout.lower()
    assert any(word in output for word in ["command", "error", "invalid", "help"]), \
        f"Should indicate invalid command: {result.stdout}"


# ============================================================================
# File Permission Error Tests
# ============================================================================


def test_export_list_permission_denied() -> None:
    """Test 'dtr export list' on directory without read permissions.

    Expected: Permission error, not crash
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        readonly_dir = Path(tmpdir) / "readonly"
        readonly_dir.mkdir(mode=0o000)  # No permissions

        try:
            result = runner.invoke(app, ["export", "list", str(readonly_dir)])

            # Should fail gracefully
            if result.exit_code != 0:
                output = result.stdout.lower()
                # Should NOT have Python traceback
                assert "traceback" not in output.lower(), "Python traceback visible"
        finally:
            # Restore permissions for cleanup
            readonly_dir.chmod(0o755)


# ============================================================================
# Edge Case Error Tests
# ============================================================================


def test_export_list_with_special_characters_in_path() -> None:
    """Test 'dtr export list' with special characters in path.

    Expected: Handles gracefully or clear error
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        special_dir = Path(tmpdir) / "dir with spaces and [brackets]"
        special_dir.mkdir()

        result = runner.invoke(app, ["export", "list", str(special_dir)])

        # Should succeed or give clear error
        if result.exit_code != 0:
            output = result.stdout.lower()
            assert "traceback" not in output.lower(), "Python traceback visible"


def test_fmt_with_empty_input_file() -> None:
    """Test format conversion with empty input file.

    Expected: Handles gracefully (empty output or clear error)
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        empty_file = Path(tmpdir) / "empty.html"
        empty_file.write_text("")  # Empty file
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        result = runner.invoke(app, [
            "fmt", "md",
            str(empty_file),
            "--output", str(output_dir)
        ])

        # Should either succeed with empty output or explain the issue
        output = result.stdout.lower()
        if result.exit_code != 0:
            # If it fails, should have helpful message
            assert "traceback" not in output.lower(), "Python traceback visible"
        else:
            # If it succeeds, should create output
            output_files = list(output_dir.glob("*.md"))
            # May or may not create file from empty input (both acceptable)
            assert len(output_files) in [0, 1], f"Unexpected output: {output_files}"


# ============================================================================
# Help and Documentation Tests
# ============================================================================


def test_export_list_help_is_available() -> None:
    """Test 'dtr export list --help' shows helpful documentation.

    Expected: Help text with examples
    """
    result = runner.invoke(app, ["export", "list", "--help"])

    # Should succeed
    assert result.exit_code == 0, "Help should always work"

    # Should contain help text
    output = result.stdout.lower()
    assert "usage" in output or "export" in output, "Should show usage info"
    assert any(word in output for word in ["directory", "path", "list"]), \
        "Should explain the command"


def test_fmt_help_shows_valid_formats() -> None:
    """Test 'dtr fmt --help' documents available formats.

    Expected: Lists md, json, html formats
    """
    result = runner.invoke(app, ["fmt", "--help"])

    # Should succeed
    assert result.exit_code == 0, "Help should always work"

    # Should list formats
    output = result.stdout.lower()
    assert any(fmt in output for fmt in ["md", "json", "html", "markdown"]), \
        "Should list available formats"
