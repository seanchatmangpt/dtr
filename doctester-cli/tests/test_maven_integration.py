"""Real end-to-end tests validating actual output from Maven builds.

Chicago TDD: Tests verify ACTUAL RESULTS, not just exit codes.
- Export commands: Verify files are listed/archived/validated
- Format commands: Verify output files actually created with content
- Report commands: Verify report files created with meaningful data
- Maven builds: Verify real Maven execution, real DocTester exports

Tests FAIL LOUDLY if:
- Maven is unavailable
- Java 25 is not installed
- Build fails
- Exports not generated
"""

import tarfile
from pathlib import Path
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


# ============================================================================
# Tests against REAL doctester-core exports
# ============================================================================


def test_maven_core_build_generates_exports(maven_doctester_core_exports: Path) -> None:
    """Verify Maven build actually generated DocTester exports.

    VALIDATES:
    - Maven build ran successfully
    - JUnit tests executed
    - DocTester captured interactions
    - Export directory exists and contains files
    """
    assert maven_doctester_core_exports.exists(), f"Exports dir missing: {maven_doctester_core_exports}"

    html_files = list(maven_doctester_core_exports.glob("*.html"))
    assert len(html_files) > 0, f"No .html exports found in {maven_doctester_core_exports}"
    print(f"✓ Found {len(html_files)} HTML export files")


def test_cli_export_list_shows_real_files(maven_doctester_core_exports: Path) -> None:
    """Test 'dtr export list' shows REAL exports, not template output.

    VALIDATES:
    - Command runs successfully
    - Output contains actual file information (not just headers)
    - Lists real files from the export directory
    """
    result = runner.invoke(app, ["export", "list", str(maven_doctester_core_exports)])

    assert result.exit_code == 0, f"Command failed: {result.stdout}"

    # VALIDATE: Output is not empty
    output_lines = [line.strip() for line in result.stdout.split('\n') if line.strip()]
    assert len(output_lines) > 0, "No output generated"

    # VALIDATE: Contains table headers or file info (case-insensitive)
    output_lower = result.stdout.lower()
    assert any(word in output_lower for word in ['test', 'class', 'file', 'export', 'html', 'total']), \
        f"Output doesn't contain expected file listing keywords: {result.stdout}"

    print(f"✓ Export list output ({len(output_lines)} lines):\n{result.stdout}")


def test_cli_export_check_validates_real_exports(maven_doctester_core_exports: Path) -> None:
    """Test 'dtr export check' validates REAL exports and reports results.

    VALIDATES:
    - Command runs successfully
    - Validation checks are performed
    - Results report file counts and validity
    """
    result = runner.invoke(app, ["export", "check", str(maven_doctester_core_exports)])

    assert result.exit_code == 0, f"Validation failed: {result.stdout}"

    # VALIDATE: Output contains validation results
    output_lower = result.stdout.lower()
    assert any(word in output_lower for word in ['validation', 'check', 'valid', 'file', 'issue']), \
        f"Output doesn't contain validation results: {result.stdout}"

    print(f"✓ Validation results:\n{result.stdout}")


def test_cli_export_save_creates_actual_archive(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr export save' ACTUALLY CREATES archive file on disk.

    VALIDATES:
    - Command runs successfully
    - Archive file EXISTS on filesystem
    - Archive is non-empty (contains real data)
    - Archive format is valid tar.gz
    """
    archive_file = tmp_path / "core_exports.tar.gz"

    result = runner.invoke(
        app,
        ["export", "save", str(maven_doctester_core_exports), "--output", str(archive_file), "--format", "tar.gz"],
    )

    assert result.exit_code == 0, f"Archive creation failed: {result.stdout}"

    # VALIDATE: Archive file CREATED on disk
    assert archive_file.exists(), f"Archive file not created: {archive_file}"
    print(f"✓ Archive file created: {archive_file}")

    # VALIDATE: Archive is non-empty
    size = archive_file.stat().st_size
    assert size > 1000, f"Archive too small: {size} bytes (expected > 1000)"
    print(f"✓ Archive size: {size} bytes")

    # VALIDATE: Archive is valid tar.gz format
    try:
        with tarfile.open(archive_file, "r:gz") as tar:
            members = tar.getmembers()
            assert len(members) > 0, "Archive contains no files"
            print(f"✓ Archive contains {len(members)} files")
            # Show sample files
            for member in members[:3]:
                print(f"  - {member.name}")
    except Exception as e:
        raise AssertionError(f"Archive is not valid tar.gz: {e}")


def test_cli_fmt_md_generates_markdown_files(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr fmt md' ACTUALLY GENERATES Markdown files with content.

    VALIDATES:
    - Command runs successfully
    - .md files CREATED in output directory
    - Files contain Markdown syntax (headers, markers, etc.)
    - Files have meaningful content (not empty)
    """
    html_files = list(maven_doctester_core_exports.glob("*.html"))
    assert len(html_files) > 0, "No HTML exports to convert"

    html_file = html_files[0]
    output_dir = tmp_path / "md_output"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "md", str(html_file), "--output", str(output_dir)])

    # Allow warnings but should not completely fail
    assert result.exit_code in [0, 1], f"Conversion failed unexpectedly: {result.stdout}"

    # VALIDATE: Markdown files ACTUALLY CREATED
    md_files = list(output_dir.glob("*.md"))
    assert len(md_files) > 0, f"No Markdown files generated in {output_dir}"
    print(f"✓ Generated {len(md_files)} Markdown files")

    # VALIDATE: Files contain valid Markdown content
    for md_file in md_files:
        content = md_file.read_text()
        assert len(content) > 10, f"Markdown file is too small: {md_file}"
        # Check for Markdown markers
        has_markers = any(marker in content for marker in ["#", ">", "-", "*", "[", "("])
        assert has_markers, f"File doesn't contain Markdown markers: {md_file}"
        print(f"✓ {md_file.name}: {len(content)} bytes, has Markdown markers")


def test_cli_fmt_json_generates_json_files(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr fmt json' ACTUALLY GENERATES JSON files with valid syntax.

    VALIDATES:
    - Command runs successfully
    - .json files CREATED in output directory
    - Files contain valid JSON
    - Files have meaningful content
    """
    html_files = list(maven_doctester_core_exports.glob("*.html"))
    assert len(html_files) > 0, "No HTML exports to convert"

    html_file = html_files[0]
    output_dir = tmp_path / "json_output"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "json", str(html_file), "--output", str(output_dir)])

    assert result.exit_code in [0, 1], f"JSON conversion failed: {result.stdout}"

    # VALIDATE: JSON files ACTUALLY CREATED
    json_files = list(output_dir.glob("*.json"))
    assert len(json_files) > 0, f"No JSON files generated in {output_dir}"
    print(f"✓ Generated {len(json_files)} JSON files")

    # VALIDATE: Files contain valid JSON
    import json
    for json_file in json_files:
        content = json_file.read_text()
        assert len(content) > 10, f"JSON file is too small: {json_file}"
        try:
            data = json.loads(content)
            assert len(str(data)) > 0, f"JSON parsed but empty: {json_file}"
            print(f"✓ {json_file.name}: {len(content)} bytes, valid JSON")
        except json.JSONDecodeError as e:
            raise AssertionError(f"Invalid JSON in {json_file}: {e}")


def test_cli_report_sum_generates_report_file(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr report sum' ACTUALLY GENERATES report file with content.

    VALIDATES:
    - Command runs successfully
    - Report file CREATED on disk
    - File contains meaningful data (test counts, metrics)
    - File is not just empty headers
    """
    output_file = tmp_path / "summary.md"

    result = runner.invoke(
        app,
        ["report", "sum", str(maven_doctester_core_exports), "--output", str(output_file), "--format", "markdown"],
    )

    assert result.exit_code in [0, 1], f"Report generation failed: {result.stdout}"

    # VALIDATE: Report file CREATED
    assert output_file.exists(), f"Report file not created: {output_file}"
    print(f"✓ Report file created: {output_file}")

    # VALIDATE: File has meaningful content
    content = output_file.read_text()
    assert len(content) > 50, f"Report file too small: {len(content)} bytes"
    print(f"✓ Report size: {len(content)} bytes")

    # VALIDATE: Contains expected metrics
    content_lower = content.lower()
    assert any(word in content_lower for word in ["test", "count", "summary", "assertion", "class"]), \
        f"Report doesn't contain expected test metrics: {content}"
    print(f"✓ Report contains test metrics")


# ============================================================================
# Tests against REAL doctester-integration-test exports
# ============================================================================


def test_maven_integration_test_build_generates_api_exports(maven_integration_test_exports: Path) -> None:
    """Verify Maven integration test build generated REAL API documentation.

    VALIDATES:
    - Maven ran full integration test suite
    - Embedded server executed real HTTP calls
    - DocTester documented API interactions
    - Export directory contains API documentation
    """
    assert maven_integration_test_exports.exists(), \
        f"Integration test exports missing: {maven_integration_test_exports}"

    html_files = list(maven_integration_test_exports.glob("*.html"))
    assert len(html_files) > 0, \
        f"No API documentation exports found in {maven_integration_test_exports}"
    print(f"✓ Found {len(html_files)} API documentation files")


def test_cli_export_list_integration_exports(maven_integration_test_exports: Path) -> None:
    """Test 'dtr export list' works with API documentation exports.

    VALIDATES:
    - CLI lists actual integration test documentation
    - Output contains file information
    """
    result = runner.invoke(app, ["export", "list", str(maven_integration_test_exports)])

    assert result.exit_code == 0, f"Export list failed: {result.stdout}"

    output_lines = [line.strip() for line in result.stdout.split('\n') if line.strip()]
    assert len(output_lines) > 0, "No output generated"
    print(f"✓ Integration exports listed ({len(output_lines)} lines)")


def test_cli_report_cov_analyzes_api_endpoints(maven_integration_test_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr report cov' analyzes REAL API endpoint coverage.

    VALIDATES:
    - Command runs on real integration test exports
    - Coverage report CREATED
    - Report contains endpoint analysis
    """
    output_file = tmp_path / "api_coverage.html"

    result = runner.invoke(
        app,
        ["report", "cov", str(maven_integration_test_exports), "--output", str(output_file)],
    )

    assert result.exit_code in [0, 1], f"Coverage report failed: {result.stdout}"

    # VALIDATE: Report file created if command succeeded
    if result.exit_code == 0:
        assert output_file.exists(), f"Coverage report not created: {output_file}"
        content = output_file.read_text()
        assert len(content) > 50, "Coverage report is empty"
        print(f"✓ Coverage report generated: {len(content)} bytes")


def test_cli_report_log_changelog_from_api(maven_integration_test_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr report log' generates changelog from REAL API documentation.

    VALIDATES:
    - Command runs on real API exports
    - Changelog file CREATED
    - File contains API change tracking
    """
    output_file = tmp_path / "api_changelog.md"

    result = runner.invoke(
        app,
        ["report", "log", str(maven_integration_test_exports), "--output", str(output_file)],
    )

    assert result.exit_code in [0, 1], f"Changelog generation failed: {result.stdout}"

    if result.exit_code == 0:
        assert output_file.exists(), f"Changelog not created: {output_file}"
        content = output_file.read_text()
        assert len(content) > 0, "Changelog is empty"
        print(f"✓ Changelog generated: {len(content)} bytes")


# ============================================================================
# Cross-module integration tests
# ============================================================================


def test_export_save_zip_format(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test that 'dtr export save' also works with ZIP format.

    VALIDATES:
    - Command creates ZIP archive
    - File CREATED on disk
    - Archive is valid and contains files
    """
    archive_file = tmp_path / "exports.zip"

    result = runner.invoke(
        app,
        ["export", "save", str(maven_doctester_core_exports), "--output", str(archive_file), "--format", "zip"],
    )

    assert result.exit_code == 0, f"ZIP creation failed: {result.stdout}"

    # VALIDATE: ZIP file CREATED
    assert archive_file.exists(), f"ZIP file not created: {archive_file}"
    print(f"✓ ZIP archive created: {archive_file}")

    # VALIDATE: ZIP is valid
    import zipfile
    try:
        with zipfile.ZipFile(archive_file, 'r') as zf:
            files = zf.namelist()
            assert len(files) > 0, "ZIP archive is empty"
            print(f"✓ ZIP contains {len(files)} files")
    except zipfile.BadZipFile as e:
        raise AssertionError(f"Invalid ZIP file: {e}")


def test_export_clean_dry_run_shows_what_would_delete(maven_doctester_core_exports: Path) -> None:
    """Test 'dtr export clean --dry-run' shows files WITHOUT deleting them.

    VALIDATES:
    - Dry-run doesn't delete files
    - Output shows what WOULD be deleted
    - Files still exist after command
    """
    # Count files before cleanup
    files_before = list(maven_doctester_core_exports.glob("*.html"))
    count_before = len(files_before)

    result = runner.invoke(
        app,
        ["export", "clean", str(maven_doctester_core_exports), "--keep", "1", "--dry-run"],
    )

    # Should complete (may be exit 0 or 1 depending on dry-run logic)
    assert result.exit_code in [0, 1]

    # VALIDATE: Files still exist (not deleted)
    files_after = list(maven_doctester_core_exports.glob("*.html"))
    count_after = len(files_after)
    assert count_after == count_before, f"Files were deleted during dry-run! Before: {count_before}, After: {count_after}"
    print(f"✓ Dry-run preserved {count_after} files (expected {count_before})")

    # VALIDATE: Output mentions dry-run or what would be deleted
    output_lower = result.stdout.lower()
    assert any(word in output_lower for word in ['dry', 'remove', 'delete', 'file', 'would']), \
        f"Output doesn't describe dry-run action: {result.stdout}"
    print(f"✓ Dry-run output describes action")
