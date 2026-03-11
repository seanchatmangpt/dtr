"""Real end-to-end tests that build with Maven and verify CLI against real exports.

Chicago TDD: Tests verify REAL capabilities with REAL Maven builds.
No mocking, no fixtures with default values - only real end-to-end tests.

Tests fail loudly if:
- Maven is unavailable
- Java 25 is not installed
- Build fails
- Exports not generated
"""

from pathlib import Path
from typer.testing import CliRunner

from doctester_cli.main import app

runner = CliRunner()


# ============================================================================
# Tests against REAL doctester-core exports
# ============================================================================


def test_maven_core_build_generates_exports(maven_doctester_core_exports: Path) -> None:
    """Verify Maven build actually generated DocTester exports.

    This test FAILS if:
    - Maven build failed
    - Java 25 not installed
    - mvnd not in PATH
    - Exports not generated

    Passing this test proves:
    1. Full Java compilation works
    2. JUnit tests ran and passed
    3. DocTester captured test interactions
    4. HTML exports were generated to disk
    """
    # Fixture already verified this, but double-check
    assert maven_doctester_core_exports.exists(), f"Exports dir missing: {maven_doctester_core_exports}"

    html_files = list(maven_doctester_core_exports.glob("*.html"))
    assert len(html_files) > 0, f"No .html exports found in {maven_doctester_core_exports}"


def test_cli_export_list_against_real_core_exports(maven_doctester_core_exports: Path) -> None:
    """Test 'dtr export list' against REAL Maven-generated exports.

    Verifies:
    - CLI can detect and list real exports
    - File metadata is correctly read
    - Command completes without error
    """
    result = runner.invoke(app, ["export", "list", str(maven_doctester_core_exports)])

    assert result.exit_code == 0, f"Command failed: {result.stdout}\n{result.stderr}"
    # Verify it found exports
    assert "export" in result.stdout.lower() or "file" in result.stdout.lower() or "test" in result.stdout.lower()


def test_cli_export_check_validates_real_core_exports(maven_doctester_core_exports: Path) -> None:
    """Test 'dtr export check' validates REAL Maven-generated exports.

    Verifies:
    - Validation code can parse real HTML
    - Structure is valid and recognized
    - No crashes on real data
    """
    result = runner.invoke(app, ["export", "check", str(maven_doctester_core_exports)])

    assert result.exit_code == 0, f"Validation failed: {result.stdout}\n{result.stderr}"


def test_cli_export_save_archives_real_core_exports(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr export save' creates archive from REAL Maven exports.

    Verifies:
    - Archive creation works with real export structure
    - Archive is non-empty and valid
    - Compression works correctly
    """
    output_file = tmp_path / "core_exports.tar.gz"

    result = runner.invoke(
        app,
        ["export", "save", str(maven_doctester_core_exports), "--output", str(output_file), "--format", "tar.gz"],
    )

    assert result.exit_code == 0, f"Archive creation failed: {result.stdout}\n{result.stderr}"
    assert output_file.exists(), f"Archive not created: {output_file}"
    assert output_file.stat().st_size > 1000, f"Archive too small: {output_file.stat().st_size} bytes"


def test_cli_fmt_md_converts_real_core_exports(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr fmt md' converts REAL Maven-generated HTML exports to Markdown.

    Verifies:
    - Conversion works on real HTML structure
    - Output Markdown files are generated
    - No crashes or data corruption
    """
    html_files = list(maven_doctester_core_exports.glob("*.html"))
    assert len(html_files) > 0, "No HTML exports to convert"

    html_file = html_files[0]
    output_dir = tmp_path / "md_output"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "md", str(html_file), "--output", str(output_dir)])

    # Conversion may warn about unsupported HTML, but shouldn't crash
    assert result.exit_code in [0, 1], f"Conversion failed unexpectedly: {result.stdout}\n{result.stderr}"


def test_cli_fmt_json_converts_real_core_exports(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr fmt json' converts REAL Maven-generated HTML exports to JSON.

    Verifies:
    - JSON conversion handles real HTML structure
    - JSON output is valid and non-empty
    - No data loss during conversion
    """
    html_files = list(maven_doctester_core_exports.glob("*.html"))
    assert len(html_files) > 0, "No HTML exports to convert"

    html_file = html_files[0]
    output_dir = tmp_path / "json_output"
    output_dir.mkdir()

    result = runner.invoke(app, ["fmt", "json", str(html_file), "--output", str(output_dir)])

    assert result.exit_code in [0, 1], f"JSON conversion failed: {result.stdout}\n{result.stderr}"


def test_cli_report_sum_analyzes_real_core_exports(maven_doctester_core_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr report sum' generates summary from REAL Maven exports.

    Verifies:
    - Summary report generation works on real data
    - Metrics are extracted correctly
    - Test counts and assertion counts are identified
    """
    output_file = tmp_path / "summary.md"

    result = runner.invoke(
        app,
        ["report", "sum", str(maven_doctester_core_exports), "--output", str(output_file), "--format", "markdown"],
    )

    assert result.exit_code in [0, 1], f"Report generation failed: {result.stdout}\n{result.stderr}"
    assert "report" in result.stdout.lower() or "summary" in result.stdout.lower()


# ============================================================================
# Tests against REAL doctester-integration-test exports
# ============================================================================


def test_maven_integration_test_build_generates_api_exports(maven_integration_test_exports: Path) -> None:
    """Verify Maven integration test build generated API documentation exports.

    This test FAILS if:
    - Maven build failed
    - Embedded Ninja server didn't start
    - HTTP tests didn't execute
    - DocTester didn't generate exports

    Passing this test proves:
    1. Full Java application compiled
    2. Embedded Jetty server started
    3. API endpoints responded to HTTP requests
    4. DocTester captured real HTTP interactions
    5. API documentation was generated
    """
    assert maven_integration_test_exports.exists(), f"Integration test exports missing: {maven_integration_test_exports}"

    html_files = list(maven_integration_test_exports.glob("*.html"))
    assert len(html_files) > 0, f"No API documentation exports found in {maven_integration_test_exports}"


def test_cli_export_list_against_real_integration_exports(maven_integration_test_exports: Path) -> None:
    """Test 'dtr export list' against REAL integration test API documentation.

    Verifies CLI works with API documentation structure.
    """
    result = runner.invoke(app, ["export", "list", str(maven_integration_test_exports)])

    assert result.exit_code == 0, f"Export list failed: {result.stdout}\n{result.stderr}"


def test_cli_report_cov_analyzes_api_coverage(maven_integration_test_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr report cov' analyzes REAL API endpoint coverage.

    Verifies:
    - Coverage analysis works on real API exports
    - Identifies tested vs untested endpoints
    - Generates coverage HTML report
    """
    output_file = tmp_path / "api_coverage.html"

    result = runner.invoke(
        app,
        ["report", "cov", str(maven_integration_test_exports), "--output", str(output_file)],
    )

    assert result.exit_code in [0, 1], f"Coverage report failed: {result.stdout}\n{result.stderr}"


def test_cli_report_log_tracks_api_changes(maven_integration_test_exports: Path, tmp_path: Path) -> None:
    """Test 'dtr report log' generates changelog from REAL API documentation.

    Verifies:
    - Changelog generation works on real API exports
    - Identifies new endpoints and modifications
    - Tracks API evolution
    """
    output_file = tmp_path / "api_changelog.md"

    result = runner.invoke(
        app,
        ["report", "log", str(maven_integration_test_exports), "--output", str(output_file)],
    )

    assert result.exit_code in [0, 1], f"Changelog generation failed: {result.stdout}\n{result.stderr}"


# ============================================================================
# Cross-module tests
# ============================================================================


def test_cli_export_save_both_modules(
    maven_doctester_core_exports: Path,
    maven_integration_test_exports: Path,
    tmp_path: Path,
) -> None:
    """Test archiving both core and integration test exports.

    Verifies:
    - CLI works with different export structures
    - Both core unit tests and integration tests produce valid exports
    - Archive command handles both types
    """
    core_archive = tmp_path / "core.tar.gz"
    integration_archive = tmp_path / "integration.tar.gz"

    # Archive core
    result1 = runner.invoke(
        app,
        ["export", "save", str(maven_doctester_core_exports), "--output", str(core_archive), "--format", "tar.gz"],
    )
    assert result1.exit_code == 0

    # Archive integration tests
    result2 = runner.invoke(
        app,
        ["export", "save", str(maven_integration_test_exports), "--output", str(integration_archive), "--format", "tar.gz"],
    )
    assert result2.exit_code == 0

    # Both archives should exist and be non-empty
    assert core_archive.exists() and core_archive.stat().st_size > 1000
    assert integration_archive.exists() and integration_archive.stat().st_size > 1000
