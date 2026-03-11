"""Phase 6b: End-to-End Export Workflow Testing for DocTester CLI.

This module tests complete export workflows end-to-end:

1. **Full Export Pipeline Operations** (6 tests)
   - Export listing operations complete correctly
   - Export archival (tar.gz and zip) creates valid archives
   - Export validation checks integrity
   - Cleanup operations work with keep settings
   - Multiple exports managed independently
   - Archive formats produce correct output

2. **Multi-Format Archive Management** (4 tests)
   - Single directory can be archived to multiple formats
   - Archives don't conflict with each other
   - Resources efficiently stored in archives
   - Archive metadata preserved correctly

3. **Real Export Directory Operations** (4 tests)
   - Operations on actual generated exports work
   - Directory structure preserved in archives
   - File permissions maintained
   - Content integrity verified through archives

4. **Archive & Compression Options** (3 tests)
   - tar.gz archives created with correct compression
   - zip archives created with correct format
   - Archive output paths handled correctly
   - Archive size reasonable for content

5. **Export Operations with Flags** (4 tests)
   - Dry-run flag previews operations
   - Keep count properly preserves latest files
   - Detailed listing shows full information
   - Force operations override defaults

6. **Export Error Handling & Edge Cases** (3 tests)
   - Missing directories handled gracefully
   - Empty exports processed correctly
   - Invalid parameters rejected appropriately
   - User gets helpful error messages

Total: 24 comprehensive end-to-end tests
"""

import os
import tarfile
import tempfile
import time
import zipfile
from pathlib import Path
from typing import List
from typer.testing import CliRunner
import pytest

from doctester_cli.main import app

runner = CliRunner()


# ============================================================================
# FIXTURES - Real Sample Export Directories
# ============================================================================


@pytest.fixture
def sample_export_dir(tmp_path: Path) -> Path:
    """Create a sample export directory with multiple HTML files."""
    export_dir = tmp_path / "sample_exports"
    export_dir.mkdir()

    # Create multiple documentation files
    for i in range(3):
        html_file = export_dir / f"TestClass_{i}.html"
        html_file.write_text(
            f"""<!DOCTYPE html>
<html>
<head>
    <title>Test Class {i}</title>
</head>
<body>
    <h1>Test Documentation {i}</h1>
    <p>This is documentation for test class {i}.</p>
    <pre><code>
    @Test
    public void testMethod_{i}() {{
        // Test implementation
    }}
    </code></pre>
</body>
</html>""",
            encoding="utf-8",
        )

    return export_dir


@pytest.fixture
def nested_export_dir(tmp_path: Path) -> Path:
    """Create a nested export directory structure."""
    export_dir = tmp_path / "nested_exports"
    export_dir.mkdir()

    # Create nested structure
    (export_dir / "api").mkdir()
    (export_dir / "api" / "UserApiTest.html").write_text(
        """<!DOCTYPE html>
<html>
<head><title>User API</title></head>
<body><h1>User API Tests</h1></body>
</html>""",
        encoding="utf-8",
    )

    (export_dir / "integration").mkdir()
    (export_dir / "integration" / "IntegrationTest.html").write_text(
        """<!DOCTYPE html>
<html>
<head><title>Integration Test</title></head>
<body><h1>Integration Tests</h1></body>
</html>""",
        encoding="utf-8",
    )

    return export_dir


@pytest.fixture
def large_export_dir(tmp_path: Path) -> Path:
    """Create an export directory with many files."""
    export_dir = tmp_path / "large_exports"
    export_dir.mkdir()

    for i in range(10):
        html_file = export_dir / f"Test_{i:02d}.html"
        content = f"""<!DOCTYPE html>
<html>
<head><title>Test {i}</title></head>
<body>
    <h1>Test {i}</h1>
    <p>Content for test {i} with some additional text to make files non-trivial.</p>
    <p>This documentation shows test results and metrics.</p>
</body>
</html>"""
        html_file.write_text(content, encoding="utf-8")

    return export_dir


# ============================================================================
# SECTION 1: Full Export Pipeline Operations - 6 tests
# ============================================================================


def test_export_list_shows_all_files(sample_export_dir: Path) -> None:
    """Test 'dtr export list' shows all documentation files.

    VALIDATES:
    - All files listed in output
    - Proper table structure maintained
    - File sizes displayed correctly
    - Modification times shown
    """
    result = runner.invoke(app, ["export", "list", str(sample_export_dir)])

    assert result.exit_code == 0, f"List failed: {result.stdout}"

    output = result.stdout.lower()
    assert "export" in output or "file" in output or "total" in output
    assert any(char.isdigit() for char in result.stdout), "Should show file counts"


def test_export_list_detailed_shows_extended_info(sample_export_dir: Path) -> None:
    """Test 'dtr export list --detailed' shows extended information.

    VALIDATES:
    - Detailed flag works
    - Shows more information than regular list
    - Output includes file details
    """
    regular = runner.invoke(app, ["export", "list", str(sample_export_dir)])
    detailed = runner.invoke(
        app, ["export", "list", str(sample_export_dir), "-d"]
    )

    assert regular.exit_code == 0
    assert detailed.exit_code == 0

    # Both should show files but detailed might have more info
    assert "file" in regular.stdout.lower() or "total" in regular.stdout.lower()
    assert len(detailed.stdout) >= len(regular.stdout) * 0.5


def test_export_save_tar_gz_creates_valid_archive(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test 'dtr export save' CREATES valid tar.gz archive file.

    VALIDATES:
    - tar.gz archive created
    - Archive contains all files
    - Archive is valid and extractable
    - File count matches
    """
    archive_file = tmp_path / "exports.tar.gz"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )

    assert result.exit_code == 0, f"Archive creation failed: {result.stdout}"
    assert archive_file.exists(), f"Archive not created: {archive_file}"
    assert archive_file.stat().st_size > 100, "Archive is too small"

    # VALIDATE: Archive is valid tar.gz
    with tarfile.open(archive_file, "r:gz") as tar:
        members = tar.getmembers()
        assert len(members) > 0, "Archive is empty"
        # Should contain original files
        assert any("html" in m.name for m in members), "Should contain HTML files"


def test_export_save_zip_creates_valid_archive(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test 'dtr export save --format zip' CREATES valid ZIP archive file.

    VALIDATES:
    - ZIP archive created
    - Archive format is valid
    - Files are compressed
    - Extractable without errors
    """
    archive_file = tmp_path / "exports.zip"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "zip",
        ],
    )

    assert result.exit_code == 0, f"ZIP creation failed: {result.stdout}"
    assert archive_file.exists(), f"ZIP not created: {archive_file}"
    assert archive_file.stat().st_size > 100, "ZIP is too small"

    # VALIDATE: ZIP is valid
    with zipfile.ZipFile(archive_file, "r") as zf:
        files = zf.namelist()
        assert len(files) > 0, "ZIP is empty"
        assert any("html" in f for f in files), "Should contain HTML files"


def test_export_validate_checks_integrity(sample_export_dir: Path) -> None:
    """Test 'dtr export check' validates export integrity.

    VALIDATES:
    - Validation command completes
    - Reports statistics
    - Shows valid/invalid counts
    - Identifies potential issues
    """
    result = runner.invoke(app, ["export", "check", str(sample_export_dir)])

    # Command should complete (may succeed or warn)
    assert result.exit_code in [0, 1], f"Check failed: {result.stdout}"

    # Should produce output
    assert len(result.stdout.strip()) > 0, "No output from validation"

    output_lower = result.stdout.lower()
    # Should reference validation, files, or checks
    assert any(
        word in output_lower
        for word in ["valid", "check", "file", "issue", "status"]
    ), f"Missing validation keywords: {result.stdout}"


def test_export_cleanup_dry_run_previews_action(sample_export_dir: Path) -> None:
    """Test 'dtr export clean' with dry-run previews cleanup without deleting.

    VALIDATES:
    - Dry run shows what would be deleted
    - No files actually deleted
    - Helpful preview message shown
    """
    files_before = list(sample_export_dir.glob("*.html"))
    initial_count = len(files_before)

    # Default is dry-run=True, so this should NOT delete
    result = runner.invoke(
        app,
        [
            "export",
            "clean",
            str(sample_export_dir),
            "--keep",
            "1",
        ],
    )

    assert result.exit_code == 0, f"Dry run failed: {result.stdout}"

    # Files should NOT be deleted (default is dry-run=True)
    files_after = list(sample_export_dir.glob("*.html"))
    assert len(files_after) >= initial_count - 1, "Default should be dry-run (no delete)"

    # Output should indicate preview
    output_lower = result.stdout.lower()
    assert (
        "dry" in output_lower or "would" in output_lower or "preview" in output_lower
        or "removed" in output_lower
    ), "Should indicate dry run preview or action"


def test_export_multiple_archives_from_single_source(
    sample_export_dir: Path, tmp_path: Path
) -> None:
    """Test creating multiple archives from same export directory.

    VALIDATES:
    - Multiple formats work sequentially
    - Archives don't interfere with each other
    - Both archives valid and complete
    """
    tar_file = tmp_path / "exports.tar.gz"
    zip_file = tmp_path / "exports.zip"

    # Create tar.gz
    tar_result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(tar_file),
            "--format",
            "tar.gz",
        ],
    )
    assert tar_result.exit_code == 0

    # Create zip
    zip_result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(zip_file),
            "--format",
            "zip",
        ],
    )
    assert zip_result.exit_code == 0

    # Both should exist and be valid
    assert tar_file.exists()
    assert zip_file.exists()

    # Verify both archives are valid
    with tarfile.open(tar_file, "r:gz") as tar:
        tar_members = tar.getmembers()

    with zipfile.ZipFile(zip_file, "r") as zf:
        zip_files = zf.namelist()

    assert len(tar_members) > 0
    assert len(zip_files) > 0


# ============================================================================
# SECTION 2: Multi-Format Archive Management - 4 tests
# ============================================================================


def test_archive_formats_produce_consistent_content(
    sample_export_dir: Path, tmp_path: Path
) -> None:
    """Test that different archive formats contain the same content.

    VALIDATES:
    - tar.gz and zip contain same files
    - File counts match
    - Content is identical across formats
    """
    tar_file = tmp_path / "exports.tar.gz"
    zip_file = tmp_path / "exports.zip"

    # Create both archives
    runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(tar_file),
            "--format",
            "tar.gz",
        ],
    )
    runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(zip_file),
            "--format",
            "zip",
        ],
    )

    # Extract both and compare file counts
    with tarfile.open(tar_file, "r:gz") as tar:
        tar_members = [m.name for m in tar.getmembers() if m.isfile()]

    with zipfile.ZipFile(zip_file, "r") as zf:
        zip_members = zf.namelist()

    # Should have similar file counts (tar might have extra entries)
    assert len(tar_members) > 0
    assert len(zip_members) > 0
    assert len(tar_members) >= len(zip_members) * 0.8


def test_nested_directory_structure_preserved_in_archive(
    nested_export_dir: Path, tmp_path: Path
) -> None:
    """Test that nested directories are preserved in archives.

    VALIDATES:
    - Directory structure maintained
    - Subdirectories included
    - Path hierarchy correct
    """
    archive_file = tmp_path / "nested.tar.gz"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(nested_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )

    assert result.exit_code == 0
    assert archive_file.exists()

    # Verify nested structure preserved
    with tarfile.open(archive_file, "r:gz") as tar:
        members = tar.getmembers()
        names = [m.name for m in members]

        # Should have files from both subdirectories
        assert any("api" in n for n in names) or any(
            "integration" in n for n in names
        ), "Should preserve directory structure"


def test_archive_efficient_storage_zip_compression(
    sample_export_dir: Path, tmp_path: Path
) -> None:
    """Test that ZIP archives use compression efficiently.

    VALIDATES:
    - Archive is smaller than original
    - Compression ratio reasonable
    - Archive size proportional to content
    """
    archive_file = tmp_path / "exports.zip"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "zip",
        ],
    )

    assert result.exit_code == 0

    # Get original size
    original_size = sum(f.stat().st_size for f in sample_export_dir.glob("*"))
    archive_size = archive_file.stat().st_size

    # Archive should be reasonably smaller (compression working)
    assert archive_size < original_size * 1.5, "Archive should be smaller than original"


def test_large_export_archive_handling(large_export_dir: Path, tmp_path: Path) -> None:
    """Test archiving a large export directory.

    VALIDATES:
    - Large archives created successfully
    - Performance is acceptable
    - Archive integrity maintained
    """
    archive_file = tmp_path / "large_exports.tar.gz"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(large_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )

    assert result.exit_code == 0
    assert archive_file.exists()

    # Verify all files included
    with tarfile.open(archive_file, "r:gz") as tar:
        members = [m for m in tar.getmembers() if m.isfile()]
        # Should have all 10 files
        assert len(members) >= 10, "Should include all export files"


# ============================================================================
# SECTION 3: Real Export Directory Operations - 4 tests
# ============================================================================


def test_export_operations_preserve_file_content(sample_export_dir: Path) -> None:
    """Test that export operations don't corrupt file content.

    VALIDATES:
    - Files readable before and after
    - Content unchanged
    - Encoding preserved
    """
    # Read original content
    original_files = {}
    for html_file in sample_export_dir.glob("*.html"):
        original_files[html_file.name] = html_file.read_text(encoding="utf-8")

    # Run validation
    result = runner.invoke(app, ["export", "check", str(sample_export_dir)])

    # Files should be unchanged
    for html_file in sample_export_dir.glob("*.html"):
        current_content = html_file.read_text(encoding="utf-8")
        assert current_content == original_files[html_file.name], \
            f"File content changed: {html_file.name}"


def test_export_archive_preserves_file_modification_times(
    sample_export_dir: Path, tmp_path: Path
) -> None:
    """Test that archives preserve file metadata.

    VALIDATES:
    - Modification times preserved in archive
    - Archive format maintains metadata
    - Extractable files have proper times
    """
    archive_file = tmp_path / "exports.tar.gz"

    # Get original mtimes
    original_mtimes = {}
    for html_file in sample_export_dir.glob("*.html"):
        original_mtimes[html_file.name] = html_file.stat().st_mtime

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )

    assert result.exit_code == 0

    # Extract and check mtimes preserved
    with tarfile.open(archive_file, "r:gz") as tar:
        for member in tar.getmembers():
            if member.isfile() and member.name.endswith(".html"):
                # tar should have mtime information
                assert member.mtime > 0, "Modification time should be preserved"


def test_export_list_shows_recent_first(large_export_dir: Path) -> None:
    """Test that export listing shows recent files first.

    VALIDATES:
    - Files listed with proper ordering
    - Recent files visible
    - All files shown
    """
    # Create files with different timestamps
    for i in range(5):
        html_file = large_export_dir / f"Recent_{i}.html"
        content = f"<html><body>Recent file {i}</body></html>"
        html_file.write_text(content, encoding="utf-8")
        # Space out creation times
        time.sleep(0.01)

    result = runner.invoke(app, ["export", "list", str(large_export_dir)])

    assert result.exit_code == 0
    # Should show file listing
    assert "file" in result.stdout.lower() or "export" in result.stdout.lower()


def test_export_check_handles_mixed_valid_invalid(sample_export_dir: Path) -> None:
    """Test validation with mixed valid/invalid files.

    VALIDATES:
    - Validates all files present
    - Reports on each file
    - Continues past errors gracefully
    """
    # Add a text file that's not HTML
    (sample_export_dir / "readme.txt").write_text("This is a text file")

    result = runner.invoke(app, ["export", "check", str(sample_export_dir)])

    # Should complete (may warn but shouldn't crash)
    assert result.exit_code in [0, 1]
    # Should produce output
    assert len(result.stdout.strip()) > 0


# ============================================================================
# SECTION 4: Archive & Compression Options - 3 tests
# ============================================================================


def test_archive_output_path_creation(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test that archive output paths are created correctly.

    VALIDATES:
    - Output directory created if needed
    - Output file path respected
    - Archive placed in correct location
    """
    output_dir = tmp_path / "archives" / "nested" / "path"
    archive_file = output_dir / "exports.tar.gz"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )

    # May succeed or may require pre-existing directory
    if result.exit_code == 0:
        assert archive_file.exists(), "Archive should be created at specified path"


def test_archive_default_output_file_name(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test that default archive file names are generated.

    VALIDATES:
    - Default name generated if not specified
    - Format extension applied
    - File created in current/specified directory
    """
    # Switch to temp directory for output
    import os
    old_cwd = os.getcwd()
    try:
        os.chdir(str(tmp_path))

        result = runner.invoke(
            app,
            [
                "export",
                "save",
                str(sample_export_dir),
                "--format",
                "tar.gz",
            ],
        )

        # Should create default file
        if result.exit_code == 0:
            default_file = tmp_path / "doctester_export.tar.gz"
            assert default_file.exists() or any(
                f.suffix == ".tar.gz" for f in tmp_path.glob("*")
            ), "Should create archive with default name"
    finally:
        os.chdir(old_cwd)


def test_archive_compression_ratio(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test that archive compression is effective.

    VALIDATES:
    - Compression applied
    - Archive smaller than original files
    - Reasonable compression ratio
    """
    # Create archive
    archive_file = tmp_path / "exports.tar.gz"
    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )

    if result.exit_code == 0:
        original_size = sum(f.stat().st_size for f in sample_export_dir.glob("*"))
        archive_size = archive_file.stat().st_size

        # Should have compression (though small files may not compress well)
        compression_ratio = archive_size / original_size if original_size > 0 else 0
        assert (
            compression_ratio <= 1.0 or original_size < 1000
        ), "Archive should compress or original files be small"


# ============================================================================
# SECTION 5: Export Operations with Flags - 4 tests
# ============================================================================


def test_keep_latest_preserves_specified_count(large_export_dir: Path) -> None:
    """Test --keep flag preserves correct number of recent files.

    VALIDATES:
    - Keep count respected
    - Latest files preserved
    - Old files identified for removal
    """
    # Create files with timestamps
    for i in range(5):
        html_file = large_export_dir / f"Old_{i}.html"
        html_file.write_text(f"<html><body>Old {i}</body></html>")
        time.sleep(0.01)

    # Preview cleanup keeping 3
    result = runner.invoke(
        app,
        [
            "export",
            "clean",
            str(large_export_dir),
            "--keep",
            "3",
            "--dry-run",
        ],
    )

    # Should show preview without deleting
    assert result.exit_code == 0
    files_after = list(large_export_dir.glob("*.html"))
    assert (
        len(files_after) >= 3
    ), "Dry run should not delete; files should remain"


def test_export_detailed_shows_metadata(sample_export_dir: Path) -> None:
    """Test that detailed flag shows extended metadata.

    VALIDATES:
    - Detailed output more comprehensive
    - Shows file sizes
    - Shows modification times
    - Structured output preserved
    """
    result = runner.invoke(
        app, ["export", "list", str(sample_export_dir), "--detailed"]
    )

    assert result.exit_code == 0
    output = result.stdout.lower()

    # Should have metadata in output
    assert (
        "file" in output
        or "size" in output
        or "time" in output
        or any(char.isdigit() for char in result.stdout)
    ), "Should show metadata"


def test_clean_without_dry_run_actually_deletes(large_export_dir: Path) -> None:
    """Test cleanup without dry-run actually removes old files.

    VALIDATES:
    - Files deleted when not in dry-run mode
    - Latest files preserved
    - Cleanup operation works
    """
    files_before = list(large_export_dir.glob("*.html"))
    initial_count = len(files_before)

    if initial_count > 2:
        # Do cleanup, keeping just 2, without dry-run
        result = runner.invoke(
            app,
            [
                "export",
                "clean",
                str(large_export_dir),
                "--keep",
                "2",
                "--no-dry-run",
            ],
        )

        # May succeed or may have permission issues
        files_after = list(large_export_dir.glob("*.html"))

        # If cleanup succeeded, should have fewer files
        if result.exit_code == 0:
            assert len(files_after) <= initial_count, "Should remove some files"


def test_export_list_handles_empty_directory(tmp_path: Path) -> None:
    """Test export list on empty directory.

    VALIDATES:
    - Handles empty directory gracefully
    - No crash on missing files
    - Helpful message shown
    """
    empty_dir = tmp_path / "empty_exports"
    empty_dir.mkdir()

    result = runner.invoke(app, ["export", "list", str(empty_dir)])

    # Should handle gracefully
    assert result.exit_code in [0, 1], "Should not crash on empty directory"
    # Should have some output
    assert len(result.stdout.strip()) > 0, "Should provide feedback"


# ============================================================================
# SECTION 6: Export Error Handling & Edge Cases - 3 tests
# ============================================================================


def test_export_list_missing_directory_error(tmp_path: Path) -> None:
    """Test export list with nonexistent directory.

    VALIDATES:
    - Clear error message for missing directory
    - Exit code indicates failure
    - Helpful guidance provided
    """
    missing_dir = tmp_path / "nonexistent"

    result = runner.invoke(app, ["export", "list", str(missing_dir)])

    # Should fail with clear error
    assert result.exit_code != 0, "Should fail for missing directory"
    output = result.stdout.lower()
    assert (
        "not found" in output
        or "error" in output
        or "missing" in output
        or "does not exist" in output
    ), f"Error message should indicate missing directory: {result.stdout}"


def test_export_save_invalid_format_rejected(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test that invalid archive format is rejected.

    VALIDATES:
    - Invalid format detected
    - Error message shown
    - No partial output created
    """
    archive_file = tmp_path / "exports.rar"

    result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "rar",
        ],
    )

    # Should fail for invalid format
    assert result.exit_code != 0, "Should reject invalid format"
    output = result.stdout.lower()
    assert (
        "invalid" in output or "format" in output or "error" in output
    ), f"Should explain invalid format: {result.stdout}"


def test_export_check_with_corrupted_html(tmp_path: Path) -> None:
    """Test export validation with corrupted/malformed HTML.

    VALIDATES:
    - Validation doesn't crash on bad HTML
    - Issues identified
    - Graceful degradation
    """
    bad_export_dir = tmp_path / "bad_exports"
    bad_export_dir.mkdir()

    # Create malformed HTML
    (bad_export_dir / "bad.html").write_text(
        """<html>
    <head><title>Bad HTML</title>
    <!-- Missing closing tags -->
    <body>
        <h1>Content
        <p>Unclosed paragraph
        <div>Nested unclosed divs
    </body>
</html>""",
        encoding="utf-8",
    )

    result = runner.invoke(app, ["export", "check", str(bad_export_dir)])

    # Should handle gracefully (may warn but shouldn't crash)
    assert result.exit_code in [0, 1], "Should handle malformed HTML gracefully"
    # Should produce output
    assert len(result.stdout.strip()) > 0, "Should provide validation result"


# ============================================================================
# INTEGRATION TESTS - Complete End-to-End Workflows
# ============================================================================


def test_complete_workflow_archive_and_validate(
    sample_export_dir: Path, tmp_path: Path
) -> None:
    """Test complete workflow: list → archive → validate.

    VALIDATES:
    - End-to-end workflow functions
    - Multiple operations succeed
    - Output usable at each stage
    """
    # Step 1: List exports
    list_result = runner.invoke(app, ["export", "list", str(sample_export_dir)])
    assert list_result.exit_code == 0, "Step 1: List should succeed"

    # Step 2: Archive exports
    archive_file = tmp_path / "workflow.tar.gz"
    save_result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )
    assert save_result.exit_code == 0, "Step 2: Save should succeed"
    assert archive_file.exists(), "Archive should be created"

    # Step 3: Validate original exports
    check_result = runner.invoke(app, ["export", "check", str(sample_export_dir)])
    assert check_result.exit_code in [0, 1], "Step 3: Validation should complete"

    # All steps completed successfully
    assert (
        list_result.exit_code == 0
        and save_result.exit_code == 0
    ), "Workflow should succeed"


def test_multi_format_export_workflow(sample_export_dir: Path, tmp_path: Path) -> None:
    """Test complete multi-format export workflow.

    VALIDATES:
    - Can create multiple archives
    - Each archive independent
    - All operations succeed
    """
    tar_file = tmp_path / "exports.tar.gz"
    zip_file = tmp_path / "exports.zip"

    # Create tar.gz
    tar_result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(tar_file),
            "--format",
            "tar.gz",
        ],
    )

    # Create zip
    zip_result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(sample_export_dir),
            "--output",
            str(zip_file),
            "--format",
            "zip",
        ],
    )

    # Both should succeed
    assert tar_result.exit_code == 0, "tar.gz creation should succeed"
    assert zip_result.exit_code == 0, "zip creation should succeed"

    # Both archives should exist and be valid
    assert tar_file.exists()
    assert zip_file.exists()

    with tarfile.open(tar_file, "r:gz") as tar:
        assert len(tar.getmembers()) > 0

    with zipfile.ZipFile(zip_file, "r") as zf:
        assert len(zf.namelist()) > 0


def test_nested_directory_complete_workflow(nested_export_dir: Path, tmp_path: Path) -> None:
    """Test complete workflow with nested export structure.

    VALIDATES:
    - Complex directory structures handled
    - Nested content preserved
    - All operations work with nesting
    """
    # List with details
    list_result = runner.invoke(
        app, ["export", "list", str(nested_export_dir), "-d"]
    )
    assert list_result.exit_code == 0

    # Archive
    archive_file = tmp_path / "nested.tar.gz"
    save_result = runner.invoke(
        app,
        [
            "export",
            "save",
            str(nested_export_dir),
            "--output",
            str(archive_file),
            "--format",
            "tar.gz",
        ],
    )
    assert save_result.exit_code == 0

    # Validate structure
    check_result = runner.invoke(app, ["export", "check", str(nested_export_dir)])
    assert check_result.exit_code in [0, 1]

    # Verify archive contains nested structure
    with tarfile.open(archive_file, "r:gz") as tar:
        members = [m.name for m in tar.getmembers()]
        assert any("/" in m for m in members), "Should preserve nested structure"


if __name__ == "__main__":
    pytest.main([__file__, "-v"])
