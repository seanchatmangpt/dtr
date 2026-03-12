"""Chicago-style TDD tests for DTR CLI commands.

Tests real behavior with real collaborators -- no mocks, no fakes.
Uses typer CliRunner to invoke the real CLI app end-to-end,
and pytest tmp_path for real file I/O.
"""

import json
import tarfile
from pathlib import Path

import pytest
from typer.testing import CliRunner

from dtr_cli.main import app

runner = CliRunner()

# ---------------------------------------------------------------------------
# Shared fixtures: real HTML and Markdown content used across tests
# ---------------------------------------------------------------------------

SAMPLE_HTML = """\
<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>UserServiceDocTest</title>
</head>
<body>
    <h1>User Service API</h1>
    <p>Tests for the user registration endpoint.</p>
    <h2>POST /api/users</h2>
    <pre><code>curl -X POST /api/users</code></pre>
    <table>
        <tr><th>Field</th><th>Type</th></tr>
        <tr><td>name</td><td>String</td></tr>
        <tr><td>email</td><td>String</td></tr>
    </table>
</body>
</html>
"""

SAMPLE_HTML_MINIMAL = """\
<!DOCTYPE html>
<html>
<head><title>Minimal</title></head>
<body><p>Hello world</p></body>
</html>
"""

SAMPLE_MARKDOWN = """\
# Integration Guide

Welcome to the integration guide.

## Getting Started

Follow these steps to set up.

```java
System.out.println("Hello DTR");
```

| Feature | Status |
|---------|--------|
| Auth    | Done   |
| Export  | WIP    |
"""


# ===================================================================
# 1. dtr fmt md -- Convert HTML to Markdown
# ===================================================================

class TestFmtMd:
    """Test `dtr fmt md` with real file I/O."""

    def test_converts_html_to_markdown(self, tmp_path: Path):
        html_file = tmp_path / "UserServiceDocTest.html"
        html_file.write_text(SAMPLE_HTML, encoding="utf-8")
        out_dir = tmp_path / "md_output"

        result = runner.invoke(app, ["fmt", "md", str(html_file), "-o", str(out_dir)])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Converted" in result.stdout
        assert "1 file(s)" in result.stdout

        md_file = out_dir / "UserServiceDocTest.md"
        assert md_file.exists(), "Markdown output file was not created"

        content = md_file.read_text(encoding="utf-8")
        assert "User Service API" in content
        assert "POST /api/users" in content
        assert "curl -X POST /api/users" in content

    def test_preserves_table_content(self, tmp_path: Path):
        html_file = tmp_path / "tables.html"
        html_file.write_text(SAMPLE_HTML, encoding="utf-8")
        out_dir = tmp_path / "md_out"

        result = runner.invoke(app, ["fmt", "md", str(html_file), "-o", str(out_dir)])

        assert result.exit_code == 0
        content = (out_dir / "tables.md").read_text(encoding="utf-8")
        assert "Field" in content
        assert "name" in content
        assert "String" in content

    def test_fails_on_missing_file(self, tmp_path: Path):
        missing = tmp_path / "nonexistent.html"
        result = runner.invoke(app, ["fmt", "md", str(missing), "-o", str(tmp_path)])

        assert result.exit_code != 0

    def test_output_dir_created_if_absent(self, tmp_path: Path):
        html_file = tmp_path / "doc.html"
        html_file.write_text(SAMPLE_HTML_MINIMAL, encoding="utf-8")
        deep_dir = tmp_path / "a" / "b" / "c"

        result = runner.invoke(app, ["fmt", "md", str(html_file), "-o", str(deep_dir)])

        assert result.exit_code == 0
        assert (deep_dir / "doc.md").exists()


# ===================================================================
# 2. dtr fmt json -- Convert HTML to JSON
# ===================================================================

class TestFmtJson:
    """Test `dtr fmt json` with real file I/O."""

    def test_converts_html_to_json(self, tmp_path: Path):
        html_file = tmp_path / "ApiDocTest.html"
        html_file.write_text(SAMPLE_HTML, encoding="utf-8")
        out_dir = tmp_path / "json_output"

        result = runner.invoke(app, ["fmt", "json", str(html_file), "-o", str(out_dir)])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Converted" in result.stdout
        assert "1 file(s)" in result.stdout

        json_file = out_dir / "ApiDocTest.json"
        assert json_file.exists(), "JSON output file was not created"

        data = json.loads(json_file.read_text(encoding="utf-8"))
        assert data["title"] == "UserServiceDocTest"
        assert "sections" in data
        assert len(data["sections"]) > 0
        assert data["sections"][0]["title"] == "User Service API"

    def test_json_contains_structured_content(self, tmp_path: Path):
        html_file = tmp_path / "structured.html"
        html_file.write_text(SAMPLE_HTML, encoding="utf-8")
        out_dir = tmp_path / "json_out"

        result = runner.invoke(app, ["fmt", "json", str(html_file), "-o", str(out_dir)])

        assert result.exit_code == 0
        data = json.loads((out_dir / "structured.json").read_text(encoding="utf-8"))

        section = data["sections"][0]
        content_types = [c["type"] for c in section["content"]]
        assert "heading" in content_types
        assert "paragraph" in content_types or "code" in content_types

    def test_fails_on_missing_file(self, tmp_path: Path):
        missing = tmp_path / "ghost.html"
        result = runner.invoke(app, ["fmt", "json", str(missing)])

        assert result.exit_code != 0


# ===================================================================
# 3. dtr fmt html -- Convert Markdown to HTML
# ===================================================================

class TestFmtHtml:
    """Test `dtr fmt html` with real file I/O."""

    def test_converts_markdown_to_html(self, tmp_path: Path):
        md_file = tmp_path / "guide.md"
        md_file.write_text(SAMPLE_MARKDOWN, encoding="utf-8")
        out_dir = tmp_path / "html_output"

        result = runner.invoke(app, ["fmt", "html", str(md_file), "-o", str(out_dir)])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Converted" in result.stdout
        assert "1 file(s)" in result.stdout

        html_file = out_dir / "guide.html"
        assert html_file.exists(), "HTML output file was not created"

        content = html_file.read_text(encoding="utf-8")
        assert "<html>" in content or "<!DOCTYPE html>" in content
        assert "Integration Guide" in content
        assert "Getting Started" in content

    def test_html_output_contains_code_blocks(self, tmp_path: Path):
        md_file = tmp_path / "code.md"
        md_file.write_text(SAMPLE_MARKDOWN, encoding="utf-8")
        out_dir = tmp_path / "html_out"

        result = runner.invoke(app, ["fmt", "html", str(md_file), "-o", str(out_dir)])

        assert result.exit_code == 0
        content = (out_dir / "code.html").read_text(encoding="utf-8")
        assert "Hello DTR" in content

    def test_html_output_contains_table(self, tmp_path: Path):
        md_file = tmp_path / "tables.md"
        md_file.write_text(SAMPLE_MARKDOWN, encoding="utf-8")
        out_dir = tmp_path / "html_out"

        result = runner.invoke(app, ["fmt", "html", str(md_file), "-o", str(out_dir)])

        assert result.exit_code == 0
        content = (out_dir / "tables.html").read_text(encoding="utf-8")
        assert "<table>" in content or "<table" in content
        assert "Auth" in content

    def test_fails_on_missing_file(self, tmp_path: Path):
        missing = tmp_path / "no_such.md"
        result = runner.invoke(app, ["fmt", "html", str(missing)])

        assert result.exit_code != 0


# ===================================================================
# 4. dtr export list -- List exports in a directory
# ===================================================================

class TestExportList:
    """Test `dtr export list` with real temp directory and HTML files."""

    def test_lists_html_exports(self, tmp_path: Path):
        # Create several HTML export files
        for name in ["OrderServiceDocTest", "UserServiceDocTest", "PaymentDocTest"]:
            (tmp_path / f"{name}.html").write_text(
                f"<html><head><title>{name}</title></head><body><p>Test</p></body></html>",
                encoding="utf-8",
            )

        result = runner.invoke(app, ["export", "list", str(tmp_path)])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Exports" in result.stdout
        assert "Total:" in result.stdout
        assert "3 file(s)" in result.stdout

    def test_lists_with_details(self, tmp_path: Path):
        (tmp_path / "SampleTest.html").write_text(SAMPLE_HTML, encoding="utf-8")

        result = runner.invoke(app, ["export", "list", str(tmp_path), "-d"])

        assert result.exit_code == 0
        assert "SampleTest" in result.stdout

    def test_empty_directory_shows_no_exports(self, tmp_path: Path):
        result = runner.invoke(app, ["export", "list", str(tmp_path)])

        assert result.exit_code == 0
        assert "No exports found" in result.stdout

    def test_ignores_index_html(self, tmp_path: Path):
        (tmp_path / "index.html").write_text("<html></html>", encoding="utf-8")
        (tmp_path / "RealTest.html").write_text(SAMPLE_HTML, encoding="utf-8")

        result = runner.invoke(app, ["export", "list", str(tmp_path)])

        assert result.exit_code == 0
        assert "1 file(s)" in result.stdout

    def test_fails_on_missing_directory(self, tmp_path: Path):
        missing_dir = tmp_path / "does_not_exist"
        result = runner.invoke(app, ["export", "list", str(missing_dir)])

        assert result.exit_code != 0


# ===================================================================
# 5. dtr export check -- Validate HTML exports
# ===================================================================

class TestExportCheck:
    """Test `dtr export check` with valid and invalid HTML files."""

    def test_valid_html_passes_check(self, tmp_path: Path):
        (tmp_path / "ValidTest.html").write_text(SAMPLE_HTML, encoding="utf-8")

        result = runner.invoke(app, ["export", "check", str(tmp_path)])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Validation Results" in result.stdout
        assert "Files checked: 1" in result.stdout
        assert "Valid files: 1" in result.stdout
        assert "All exports are valid" in result.stdout

    def test_invalid_html_reports_issues(self, tmp_path: Path):
        # Write HTML missing the <html> tag -- the validator flags this
        (tmp_path / "BrokenTest.html").write_text(
            "<p>No html root element</p>", encoding="utf-8"
        )

        result = runner.invoke(app, ["export", "check", str(tmp_path)])

        # exit_code is 1 when issues are found
        assert result.exit_code == 1
        assert "Issues found: 1" in result.stdout
        assert "Missing HTML tag" in result.stdout

    def test_detects_broken_internal_links(self, tmp_path: Path):
        html_with_broken_link = """\
<!DOCTYPE html>
<html>
<head><title>Links</title></head>
<body>
    <a href="missing_page.html">Broken link</a>
</body>
</html>
"""
        (tmp_path / "LinksTest.html").write_text(html_with_broken_link, encoding="utf-8")

        result = runner.invoke(app, ["export", "check", str(tmp_path)])

        assert result.exit_code == 1
        assert "Broken link" in result.stdout

    def test_multiple_files_mixed_validity(self, tmp_path: Path):
        (tmp_path / "Good.html").write_text(SAMPLE_HTML, encoding="utf-8")
        (tmp_path / "Bad.html").write_text("<p>no root</p>", encoding="utf-8")

        result = runner.invoke(app, ["export", "check", str(tmp_path)])

        assert result.exit_code == 1
        assert "Files checked: 2" in result.stdout
        assert "Valid files: 1" in result.stdout
        assert "Issues found: 1" in result.stdout

    def test_empty_directory_passes(self, tmp_path: Path):
        result = runner.invoke(app, ["export", "check", str(tmp_path)])

        assert result.exit_code == 0
        assert "Files checked: 0" in result.stdout
        assert "All exports are valid" in result.stdout


# ===================================================================
# 6. dtr export save -- Archive exports to tar.gz
# ===================================================================

class TestExportSave:
    """Test `dtr export save` creates a real archive."""

    def test_creates_tar_gz_archive(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        (export_dir / "Test1.html").write_text(SAMPLE_HTML, encoding="utf-8")
        (export_dir / "Test2.html").write_text(SAMPLE_HTML_MINIMAL, encoding="utf-8")

        archive_path = tmp_path / "backup.tar.gz"

        result = runner.invoke(
            app, ["export", "save", str(export_dir), "-o", str(archive_path)]
        )

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Archive created" in result.stdout
        assert archive_path.exists()
        assert archive_path.stat().st_size > 0

        # Verify the archive is a valid tar.gz and contains our files
        with tarfile.open(archive_path, "r:gz") as tf:
            names = tf.getnames()
            assert any("Test1.html" in n for n in names)
            assert any("Test2.html" in n for n in names)

    def test_creates_zip_archive(self, tmp_path: Path):
        import zipfile

        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        (export_dir / "Doc.html").write_text(SAMPLE_HTML, encoding="utf-8")

        archive_path = tmp_path / "backup.zip"

        result = runner.invoke(
            app,
            ["export", "save", str(export_dir), "-o", str(archive_path), "-f", "zip"],
        )

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert archive_path.exists()

        with zipfile.ZipFile(archive_path, "r") as zf:
            names = zf.namelist()
            assert any("Doc.html" in n for n in names)

    def test_fails_on_missing_directory(self, tmp_path: Path):
        missing = tmp_path / "nope"
        result = runner.invoke(app, ["export", "save", str(missing)])

        assert result.exit_code != 0


# ===================================================================
# 7. dtr export clean -- Dry-run cleanup of old exports
# ===================================================================

class TestExportClean:
    """Test `dtr export clean` with dry-run mode."""

    def test_dry_run_lists_files_to_remove(self, tmp_path: Path):
        # Create more files than the default keep count (5)
        import time

        for i in range(8):
            f = tmp_path / f"Test_{i:02d}.html"
            f.write_text(SAMPLE_HTML, encoding="utf-8")
            # Small delay so modification times differ
            time.sleep(0.05)

        result = runner.invoke(app, ["export", "clean", str(tmp_path), "--keep", "5"])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "DRY RUN" in result.stdout
        # Should report 3 files would be removed (8 - 5 = 3)
        assert "--no-dry-run" in result.stdout

    def test_dry_run_does_not_delete_files(self, tmp_path: Path):
        import time

        for i in range(8):
            f = tmp_path / f"Test_{i:02d}.html"
            f.write_text(SAMPLE_HTML, encoding="utf-8")
            time.sleep(0.05)

        result = runner.invoke(app, ["export", "clean", str(tmp_path), "--keep", "5"])

        assert result.exit_code == 0
        # All 8 files should still exist
        html_files = list(tmp_path.glob("*.html"))
        assert len(html_files) == 8

    def test_clean_with_fewer_files_than_keep(self, tmp_path: Path):
        # Only 2 files, keep 5 -- nothing to remove
        (tmp_path / "A.html").write_text(SAMPLE_HTML, encoding="utf-8")
        (tmp_path / "B.html").write_text(SAMPLE_HTML, encoding="utf-8")

        result = runner.invoke(app, ["export", "clean", str(tmp_path), "--keep", "5"])

        assert result.exit_code == 0
        # No files listed for removal
        assert "DRY RUN" in result.stdout

    def test_fails_on_missing_directory(self, tmp_path: Path):
        missing = tmp_path / "gone"
        result = runner.invoke(app, ["export", "clean", str(missing)])

        assert result.exit_code != 0


# ===================================================================
# 8. dtr report sum -- Generate summary report
# ===================================================================

class TestReportSum:
    """Test `dtr report sum` generates a real summary from exports."""

    def test_generates_markdown_summary(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        (export_dir / "LoginTest.html").write_text(SAMPLE_HTML, encoding="utf-8")
        (export_dir / "SignupTest.html").write_text(SAMPLE_HTML_MINIMAL, encoding="utf-8")

        output_file = tmp_path / "summary.md"

        result = runner.invoke(
            app, ["report", "sum", str(export_dir), "-o", str(output_file)]
        )

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Report generated" in result.stdout

        assert output_file.exists()
        content = output_file.read_text(encoding="utf-8")
        assert "Summary Report" in content
        assert "LoginTest" in content
        assert "SignupTest" in content

    def test_generates_html_summary(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        (export_dir / "ApiTest.html").write_text(SAMPLE_HTML, encoding="utf-8")

        output_file = tmp_path / "summary.html"

        result = runner.invoke(
            app,
            ["report", "sum", str(export_dir), "-o", str(output_file), "-f", "html"],
        )

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert output_file.exists()

        content = output_file.read_text(encoding="utf-8")
        assert "<html>" in content
        assert "DTR Summary Report" in content
        assert "ApiTest" in content

    def test_summary_reports_stats(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        for name in ["A", "B", "C"]:
            (export_dir / f"{name}.html").write_text(SAMPLE_HTML, encoding="utf-8")

        output_file = tmp_path / "report.md"

        result = runner.invoke(
            app, ["report", "sum", str(export_dir), "-o", str(output_file)]
        )

        assert result.exit_code == 0
        # The CLI prints stats to stdout
        assert "Tests:" in result.stdout
        assert "Assertions:" in result.stdout

    def test_empty_export_dir(self, tmp_path: Path):
        export_dir = tmp_path / "empty"
        export_dir.mkdir()
        output_file = tmp_path / "summary.md"

        result = runner.invoke(
            app, ["report", "sum", str(export_dir), "-o", str(output_file)]
        )

        assert result.exit_code == 0
        assert output_file.exists()


# ===================================================================
# 9. dtr report log -- Generate changelog
# ===================================================================

class TestReportLog:
    """Test `dtr report log` generates a real changelog from exports."""

    def test_generates_changelog(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        (export_dir / "FeatureA.html").write_text(SAMPLE_HTML, encoding="utf-8")
        (export_dir / "FeatureB.html").write_text(SAMPLE_HTML_MINIMAL, encoding="utf-8")

        output_file = tmp_path / "changelog.md"

        result = runner.invoke(
            app, ["report", "log", str(export_dir), "-o", str(output_file)]
        )

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "Changelog" in result.stdout

        assert output_file.exists()
        content = output_file.read_text(encoding="utf-8")
        assert "Changelog" in content
        assert "FeatureA" in content
        assert "FeatureB" in content

    def test_changelog_with_since_filter(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        (export_dir / "Test.html").write_text(SAMPLE_HTML, encoding="utf-8")

        output_file = tmp_path / "changelog.md"

        result = runner.invoke(
            app,
            [
                "report",
                "log",
                str(export_dir),
                "-o",
                str(output_file),
                "--since",
                "v1.0.0",
            ],
        )

        assert result.exit_code == 0
        content = output_file.read_text(encoding="utf-8")
        assert "v1.0.0" in content

    def test_changelog_includes_summary_section(self, tmp_path: Path):
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        for name in ["X", "Y"]:
            (export_dir / f"{name}.html").write_text(SAMPLE_HTML, encoding="utf-8")

        output_file = tmp_path / "changelog.md"

        result = runner.invoke(
            app, ["report", "log", str(export_dir), "-o", str(output_file)]
        )

        assert result.exit_code == 0
        content = output_file.read_text(encoding="utf-8")
        assert "Summary" in content
        assert "Total Exports: 2" in content


# ===================================================================
# 10. dtr version -- Display CLI version
# ===================================================================

class TestVersion:
    """Test `dtr version` shows version info."""

    def test_shows_dtr_cli(self):
        result = runner.invoke(app, ["version"])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "DTR CLI" in result.stdout

    def test_shows_version_number(self):
        from dtr_cli import __version__

        result = runner.invoke(app, ["version"])

        assert result.exit_code == 0
        assert __version__ in result.stdout


# ===================================================================
# 11. dtr config --show -- Display configuration
# ===================================================================

class TestConfig:
    """Test `dtr config --show` shows configuration."""

    def test_config_show(self):
        result = runner.invoke(app, ["config", "--show"])

        assert result.exit_code == 0, f"stdout: {result.stdout}"
        assert "DTR CLI Configuration" in result.stdout

    def test_config_without_show(self):
        result = runner.invoke(app, ["config"])

        assert result.exit_code == 0
        assert "--show" in result.stdout


# ===================================================================
# Cross-cutting: end-to-end workflow test
# ===================================================================

class TestEndToEndWorkflow:
    """Test a realistic multi-step workflow through the real CLI."""

    def test_html_to_markdown_to_html_roundtrip(self, tmp_path: Path):
        """HTML -> Markdown -> HTML roundtrip preserves key content."""
        # Step 1: Start with HTML
        html_file = tmp_path / "original.html"
        html_file.write_text(SAMPLE_HTML, encoding="utf-8")
        md_dir = tmp_path / "step1_md"

        result = runner.invoke(app, ["fmt", "md", str(html_file), "-o", str(md_dir)])
        assert result.exit_code == 0

        md_file = md_dir / "original.md"
        assert md_file.exists()

        # Step 2: Convert Markdown back to HTML
        html_dir = tmp_path / "step2_html"
        result = runner.invoke(app, ["fmt", "html", str(md_file), "-o", str(html_dir)])
        assert result.exit_code == 0

        roundtrip_html = html_dir / "original.html"
        assert roundtrip_html.exists()

        # Key content should survive the roundtrip
        content = roundtrip_html.read_text(encoding="utf-8")
        assert "User Service API" in content

    def test_export_list_then_save_then_check(self, tmp_path: Path):
        """List exports, save to archive, check validity."""
        export_dir = tmp_path / "exports"
        export_dir.mkdir()
        for name in ["Service1", "Service2"]:
            (export_dir / f"{name}.html").write_text(SAMPLE_HTML, encoding="utf-8")

        # List
        result = runner.invoke(app, ["export", "list", str(export_dir)])
        assert result.exit_code == 0
        assert "2 file(s)" in result.stdout

        # Check
        result = runner.invoke(app, ["export", "check", str(export_dir)])
        assert result.exit_code == 0
        assert "All exports are valid" in result.stdout

        # Save
        archive = tmp_path / "archive.tar.gz"
        result = runner.invoke(
            app, ["export", "save", str(export_dir), "-o", str(archive)]
        )
        assert result.exit_code == 0
        assert archive.exists()
        assert archive.stat().st_size > 0
