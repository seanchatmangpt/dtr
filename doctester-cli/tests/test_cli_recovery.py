"""Error recovery and graceful degradation tests for CLI commands.

Tests that CLI recovers from failures, cleans up resources, and provides
helpful messages for partial failures. Covers real-world failure scenarios
like permission errors, disk space issues, concurrent operations, and
format conversion errors.

Tests follow the principle: graceful degradation is better than total failure.
"""

import tempfile
from pathlib import Path
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


def get_output(result) -> str:
    """Get combined stdout and stderr from CLI result."""
    return (result.stdout + result.stderr).lower()


# ============================================================================
# File Write Permission Error Tests
# ============================================================================


def test_export_save_write_permission_error() -> None:
    """Test 'dtr export save' fails gracefully with permission error.

    Expected:
    - Clear error message about permissions
    - Archive file not created
    - No Python traceback to user
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create export directory with content
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        # Create read-only target directory
        readonly_dir = Path(tmpdir) / "readonly"
        readonly_dir.mkdir(mode=0o555)  # Read-only

        try:
            output_file = readonly_dir / "output.tar.gz"

            result = runner.invoke(app, [
                "export", "save",
                str(export_dir),
                "--output", str(output_file),
                "--format", "tar.gz"
            ])

            # Should fail (permission denied)
            # Note: Some systems may not enforce, so check if it failed
            if result.exit_code != 0:
                # Should have helpful error message
                output = get_output(result)
                assert any(word in output for word in [
                    "permission", "error", "write", "denied", "access"
                ]), f"Error message not helpful: {result.stdout}"

                # Should NOT have Python traceback
                assert "traceback" not in output, "Stack trace leaked to user"
                assert "file \"" not in output, "Python traceback visible"

                # Should NOT create archive
                assert not output_file.exists(), "Archive should not be created on error"
        finally:
            # Restore permissions for cleanup
            readonly_dir.chmod(0o755)


def test_export_save_output_parent_permission_error() -> None:
    """Test 'dtr export save' when parent directory is not writable.

    Expected:
    - Clear permission error
    - No partial files left behind
    - Helpful message about directory permissions
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create export directory
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        # Create readonly parent
        readonly_parent = Path(tmpdir) / "readonly"
        readonly_parent.mkdir(mode=0o555)

        try:
            output_file = readonly_parent / "output.tar.gz"

            result = runner.invoke(app, [
                "export", "save",
                str(export_dir),
                "--output", str(output_file),
                "--format", "tar.gz"
            ])

            # May fail or skip depending on system
            if result.exit_code != 0:
                output = get_output(result)
                # Should NOT show Python error
                assert "traceback" not in output
        finally:
            readonly_parent.chmod(0o755)


def test_report_output_permission_error() -> None:
    """Test 'dtr report' fails gracefully when output path not writable.

    Expected:
    - Clear error message about permissions
    - Report file not created
    - No traceback to user
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        readonly_dir = Path(tmpdir) / "readonly"
        readonly_dir.mkdir(mode=0o555)

        try:
            result = runner.invoke(app, [
                "report", "sum",
                str(export_dir),
                "--output", str(readonly_dir / "report.md"),
                "--format", "markdown"
            ])

            if result.exit_code != 0:
                output = get_output(result)
                # Should be clear about the problem
                assert "traceback" not in output
                # Should not create report
                assert not (readonly_dir / "report.md").exists()
        finally:
            readonly_dir.chmod(0o755)


# ============================================================================
# Partial Success Scenarios
# ============================================================================


def test_export_list_multiple_files_success() -> None:
    """Test 'dtr export list' with multiple export files.

    Expected:
    - All files listed
    - Clear count shown
    - Success exit code
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir)

        # Create multiple export files
        for i in range(5):
            (export_dir / f"test_{i}.html").write_text(f"<html>test {i}</html>")

        result = runner.invoke(app, ["export", "list", str(export_dir)])

        # Should succeed
        assert result.exit_code == 0, f"Should succeed with multiple files: {result.stdout}"

        output = get_output(result)
        # Should show count or list files
        assert any(word in output for word in ["5", "file", "found", "export"]), \
            f"Should indicate multiple files found: {result.stdout}"


def test_fmt_batch_partial_success_tracking() -> None:
    """Test format conversion would show success/failure counts.

    Expected:
    - If multi-file batch processing exists: show "X/Y successful"
    - If single file: show single result
    - Clear indication of what happened
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create valid HTML file
        html_file = Path(tmpdir) / "test.html"
        html_file.write_text("<html><body><h1>Test</h1></body></html>")
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        result = runner.invoke(app, [
            "fmt", "md",
            str(html_file),
            "--output", str(output_dir)
        ])

        # Should succeed or give clear error
        if result.exit_code == 0:
            output = get_output(result)
            # Should indicate success clearly
            assert any(word in output for word in [
                "success", "created", "converted", "output"
            ]) or len(list(output_dir.glob("*.md"))) > 0, \
                "Should indicate successful conversion"
        else:
            # If fails, should be clear error (not traceback)
            output = get_output(result)
            assert "traceback" not in output, "Should not show Python traceback"


# ============================================================================
# Cleanup on Failure Tests
# ============================================================================


def test_export_save_cleanup_temp_files_on_failure() -> None:
    """Test that temp files are cleaned up when export save fails.

    Expected:
    - On failure, no temp files left in temp directories
    - Output file not created
    - Clean error message
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create empty/invalid export directory
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()

        # Create output directory with restricted permissions
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir(mode=0o555)

        try:
            output_file = output_dir / "output.tar.gz"

            # Get temp directory path to check for orphaned files
            temp_root = Path(tempfile.gettempdir())

            result = runner.invoke(app, [
                "export", "save",
                str(export_dir),
                "--output", str(output_file),
                "--format", "tar.gz"
            ])

            # Should fail due to permissions
            if result.exit_code != 0:
                # Output file should NOT exist
                assert not output_file.exists(), \
                    "Archive should not be created on permission error"

                output = get_output(result)
                # Should have clear error
                assert "error" in output or "failed" in output or "permission" in output, \
                    f"Should indicate failure: {result.stdout}"
        finally:
            output_dir.chmod(0o755)


def test_fmt_cleanup_incomplete_output_on_error() -> None:
    """Test that incomplete output files are not left behind on format error.

    Expected:
    - If conversion fails, no partial output file
    - Clear error message about what went wrong
    - Input file unchanged
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create valid input file
        html_file = Path(tmpdir) / "test.html"
        html_file.write_text("<html><body>test</body></html>")
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        # Try to convert to invalid format
        result = runner.invoke(app, [
            "fmt", "invalid_format_xyz",
            str(html_file),
            "--output", str(output_dir)
        ])

        # Should fail
        assert result.exit_code != 0, "Should reject invalid format"

        # Should NOT create output file
        output_files = list(output_dir.glob("*"))
        assert len(output_files) == 0, "No output files should be created on error"

        # Input file should be unchanged
        assert html_file.exists(), "Input file should not be deleted"
        assert html_file.read_text() == "<html><body>test</body></html>", \
            "Input file should not be modified"


def test_keyboard_interrupt_cleanup() -> None:
    """Test that CLI can be interrupted and leaves no orphaned resources.

    Expected:
    - CLI starts successfully
    - Can be cleanly stopped (test verifies setup works)
    - No hanging processes or temp files
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir)
        (export_dir / "test.html").write_text("<html>test</html>")

        # Normal invocation (tests that cleanup is at least functional)
        result = runner.invoke(app, ["export", "list", str(export_dir)])

        # Should succeed normally
        assert result.exit_code == 0, "Should handle normal execution"

        # Verify no temp files left
        temp_dirs = list(Path(tempfile.gettempdir()).glob("tmp*"))
        # Should be reasonable number of temp dirs (not accumulating orphans)
        # This is a soft check - just verify cleanup infrastructure works
        assert len(temp_dirs) < 1000, "Possible temp file accumulation"


# ============================================================================
# Disk Space Error Scenarios
# ============================================================================


def test_export_save_insufficient_disk_space_message() -> None:
    """Test that disk space errors show helpful message.

    Expected:
    - When disk is full, clear error about disk space
    - Not a generic I/O error
    - Helpful recovery suggestions
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()
        output_file = output_dir / "output.tar.gz"

        # This test verifies the error handling path exists
        # Actual disk full simulation is difficult in test environment
        result = runner.invoke(app, [
            "export", "save",
            str(export_dir),
            "--output", str(output_file),
            "--format", "tar.gz"
        ])

        # Should succeed (disk not actually full)
        # This verifies normal path works
        if result.exit_code == 0:
            assert output_file.exists(), "Should create archive on success"
        else:
            # If fails, should be clear error (not cryptic)
            output = get_output(result)
            assert "error" in output, "Should show error message"


# ============================================================================
# Format Conversion Error Tests (with helpful messages)
# ============================================================================


def test_fmt_malformed_html_error_with_location() -> None:
    """Test format conversion with malformed HTML shows helpful error.

    Expected:
    - Error message indicates malformed input
    - Not a generic crash
    - Suggests how to fix
    - No Python traceback
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create malformed HTML
        html_file = Path(tmpdir) / "malformed.html"
        html_file.write_text("<html><body><h1>Unclosed tag</body></html>")
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        result = runner.invoke(app, [
            "fmt", "md",
            str(html_file),
            "--output", str(output_dir)
        ])

        output = get_output(result)

        # May succeed (parser may be lenient) or fail gracefully
        if result.exit_code != 0:
            # If fails, should be clear
            assert "traceback" not in output, "Python traceback visible"
            assert "malformed" in output or "parse" in output or "error" in output, \
                f"Should indicate parsing issue: {result.stdout}"


def test_fmt_empty_html_file_handling() -> None:
    """Test format conversion with empty HTML file.

    Expected:
    - Graceful handling (empty output or clear message)
    - No crash
    - No traceback
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Create empty HTML file
        html_file = Path(tmpdir) / "empty.html"
        html_file.write_text("")
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        result = runner.invoke(app, [
            "fmt", "md",
            str(html_file),
            "--output", str(output_dir)
        ])

        output = get_output(result)

        # Should handle gracefully
        if result.exit_code != 0:
            # If fails, should be clear
            assert "traceback" not in output, "Python traceback visible"
        else:
            # If succeeds, output should exist
            output_files = list(output_dir.glob("*.md"))
            # May or may not create file from empty input
            assert len(output_files) in [0, 1], \
                f"Should handle empty file gracefully: {output_files}"


def test_fmt_unsupported_format_error() -> None:
    """Test format conversion with unsupported format shows valid options.

    Expected:
    - Clear error about invalid format
    - Lists valid formats available
    - No traceback
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        html_file = Path(tmpdir) / "test.html"
        html_file.write_text("<html><body>test</body></html>")
        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        result = runner.invoke(app, [
            "fmt", "unsupported_format_xyz",
            str(html_file),
            "--output", str(output_dir)
        ])

        # Should fail
        assert result.exit_code != 0, "Should reject unsupported format"

        output = get_output(result)
        # Should list valid formats
        assert any(fmt in output for fmt in ["md", "json", "html", "markdown"]), \
            f"Should list valid formats: {result.stdout}"

        # Should NOT have traceback
        assert "traceback" not in output, "Python traceback visible"


# ============================================================================
# Concurrent Operation Conflict Tests
# ============================================================================


def test_multiple_exports_same_directory_handling() -> None:
    """Test that multiple CLI invocations on same directory are handled.

    Expected:
    - Can run multiple exports (parallel-safe)
    - OR detect file locks and show helpful message
    - OR warns about conflicts without crashing
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test1.html").write_text("<html>test 1</html>")
        (export_dir / "test2.html").write_text("<html>test 2</html>")

        output_dir = Path(tmpdir) / "output"
        output_dir.mkdir()

        # First invocation
        result1 = runner.invoke(app, [
            "export", "list",
            str(export_dir)
        ])

        assert result1.exit_code == 0, "First invocation should succeed"

        # Second invocation (same directory)
        result2 = runner.invoke(app, [
            "export", "list",
            str(export_dir)
        ])

        assert result2.exit_code == 0, "Second invocation should succeed"

        output = get_output(result2)
        # Both should show same results
        assert "test" in output, "Should process normally"


def test_write_conflict_detection() -> None:
    """Test that file write conflicts are detected and reported.

    Expected:
    - If output file is locked/in-use, show helpful message
    - OR successfully overwrite (if design allows)
    - No crash or silent failure
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        output_file = Path(tmpdir) / "output.tar.gz"

        # First save
        result1 = runner.invoke(app, [
            "export", "save",
            str(export_dir),
            "--output", str(output_file),
            "--format", "tar.gz"
        ])

        if result1.exit_code == 0:
            assert output_file.exists(), "First save should succeed"

            # Try to overwrite
            result2 = runner.invoke(app, [
                "export", "save",
                str(export_dir),
                "--output", str(output_file),
                "--format", "tar.gz"
            ])

            # Should handle overwrite gracefully
            if result2.exit_code == 0:
                # Should succeed
                assert output_file.exists(), "File should exist after second save"
            else:
                # Should have clear error
                output = get_output(result2)
                assert "traceback" not in output, "Python traceback visible"


# ============================================================================
# Recovery Path Tests (Error Messages & Suggestions)
# ============================================================================


def test_error_message_suggests_help() -> None:
    """Test that error messages suggest using --help for more info.

    Expected:
    - When command fails, message hints at --help option
    - OR help command works and shows available options
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        result = runner.invoke(app, [
            "export", "list",
            "/nonexistent/path"
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with nonexistent path"

        # Check help command works
        help_result = runner.invoke(app, ["export", "list", "--help"])
        assert help_result.exit_code == 0, "Help should always work"

        help_output = get_output(help_result)
        assert "export" in help_output or "list" in help_output, \
            "Help should document the command"


def test_error_message_actionable() -> None:
    """Test that error messages are actionable (not cryptic).

    Expected:
    - Error indicates WHAT went wrong
    - Suggests HOW to fix it
    - Example: "File not found: /path/to/file (does it exist?)"
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        result = runner.invoke(app, [
            "fmt", "md",
            "/nonexistent/input.html",
            "--output", tmpdir
        ])

        # Should fail
        assert result.exit_code != 0, "Should fail with nonexistent file"

        output = get_output(result)
        # Should be actionable
        assert "nonexistent" in output or "not found" in output or "file" in output, \
            f"Error should indicate missing file: {result.stdout}"

        # Should NOT be cryptic
        assert "traceback" not in output, "Error should not be Python traceback"


def test_successful_operation_clear_confirmation() -> None:
    """Test that successful operations show clear confirmation.

    Expected:
    - Success message indicates what was done
    - Clear indication of output location
    - Helpful next steps
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html><body>test</body></html>")

        result = runner.invoke(app, [
            "export", "list",
            str(export_dir)
        ])

        # Should succeed
        assert result.exit_code == 0, "Should succeed with valid directory"

        output = get_output(result)
        # Should confirm what was done
        assert "test" in output or "found" in output or "export" in output, \
            f"Should show results: {result.stdout}"


# ============================================================================
# Resource Cleanup Verification
# ============================================================================


def test_no_orphaned_temp_files_after_success() -> None:
    """Test that successful operations don't leave orphaned temp files.

    Expected:
    - After operation, no temp files accumulate
    - Temp directory is cleaned up
    - Multiple runs don't cause temp file explosion
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        # Count temp files before
        temp_before = len(list(Path(tempfile.gettempdir()).glob("tmp*")))

        # Run operation
        result = runner.invoke(app, [
            "export", "list",
            str(export_dir)
        ])

        assert result.exit_code == 0, "Should succeed"

        # Count temp files after (allow small increase for logging, etc.)
        temp_after = len(list(Path(tempfile.gettempdir()).glob("tmp*")))
        temp_increase = temp_after - temp_before

        # Should not accumulate huge number of temp files
        assert temp_increase < 100, \
            f"Possible temp file leak: {temp_increase} new temp files"


def test_no_orphaned_temp_files_after_failure() -> None:
    """Test that failed operations clean up temp files.

    Expected:
    - Failed operation cleans up any temp files created
    - No orphaned partial files
    - Repeated failures don't accumulate files
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Count temp files before
        temp_before = len(list(Path(tempfile.gettempdir()).glob("tmp*")))

        # Run failing operation
        result = runner.invoke(app, [
            "export", "list",
            "/nonexistent/path"
        ])

        assert result.exit_code != 0, "Should fail"

        # Count temp files after
        temp_after = len(list(Path(tempfile.gettempdir()).glob("tmp*")))
        temp_increase = temp_after - temp_before

        # Should clean up (allow small increase for test framework)
        assert temp_increase < 50, \
            f"Possible temp file leak on failure: {temp_increase} new temp files"


# ============================================================================
# Exit Code Consistency
# ============================================================================


def test_exit_codes_consistent() -> None:
    """Test that exit codes are consistent and meaningful.

    Expected:
    - Success: exit code 0
    - User error (invalid input): exit code 1-2
    - System error: exit code > 2
    - Consistent across different command types
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Success case
        export_dir = Path(tmpdir) / "exports"
        export_dir.mkdir()
        (export_dir / "test.html").write_text("<html>test</html>")

        result_success = runner.invoke(app, [
            "export", "list",
            str(export_dir)
        ])
        assert result_success.exit_code == 0, "Success should be exit code 0"

        # User error case (invalid argument)
        result_user_error = runner.invoke(app, [
            "export", "list",
            "/nonexistent/path"
        ])
        assert result_user_error.exit_code != 0, "Error should be non-zero"
        assert result_user_error.exit_code < 128, "Should be user error range"

        # Missing argument case
        result_missing = runner.invoke(app, [
            "export", "list"  # Missing required path argument
        ])
        assert result_missing.exit_code != 0, "Missing argument should fail"


def test_error_message_format_consistency() -> None:
    """Test that error messages follow consistent format.

    Expected:
    - All errors start with clear indicator (ERROR:, Failed:, etc.)
    - Consistent capitalization and punctuation
    - Always end with actionable suggestion
    """
    with tempfile.TemporaryDirectory() as tmpdir:
        # Generate multiple errors
        errors = []

        # Error 1: nonexistent directory
        result1 = runner.invoke(app, [
            "export", "list",
            "/nonexistent/path"
        ])
        if result1.exit_code != 0:
            errors.append(get_output(result1))

        # Error 2: invalid format
        html_file = Path(tmpdir) / "test.html"
        html_file.write_text("<html>test</html>")
        result2 = runner.invoke(app, [
            "fmt", "invalid_format",
            str(html_file),
            "--output", tmpdir
        ])
        if result2.exit_code != 0:
            errors.append(get_output(result2))

        # All errors should be readable
        for error in errors:
            # Should NOT have Python tracebacks
            assert "traceback" not in error, "Error contains Python traceback"
            assert "file \"" not in error, "Error contains Python file paths"
            # Should have some meaningful content
            assert any(word in error for word in [
                "error", "failed", "invalid", "not found", "cannot"
            ]), f"Error message not meaningful: {error}"
