"""Chicago-style TDD tests for reporters module.

Tests real behavior with real collaborators -- no mocks.
Uses real temp files and real reporter instances.
"""

import json
from pathlib import Path

import pytest

from dtr_cli.model import ReportConfig, ReportResult
from dtr_cli.reporters.base_reporter import BaseReporter
from dtr_cli.reporters.html_reporter import HtmlReporter
from dtr_cli.reporters.markdown_reporter import MarkdownReporter
from dtr_cli.reporters.summary_reporter import SummaryReporter


# ---------------------------------------------------------------------------
# Fixtures: sample HTML export content
# ---------------------------------------------------------------------------

SAMPLE_EXPORT_HTML = """\
<!DOCTYPE html>
<html>
<head><title>UserServiceTest</title></head>
<body>
    <h1>User Service Tests</h1>
    <h2>Authentication</h2>
    <div class="test-method">testLogin</div>
    <div class="test-method">testLogout</div>
    <div class="test-method">testTokenRefresh</div>
    <div class="assertion">status is 200</div>
    <div class="assertion">body contains token</div>
</body>
</html>
"""

SAMPLE_EXPORT_HTML_2 = """\
<!DOCTYPE html>
<html>
<head><title>OrderServiceTest</title></head>
<body>
    <h1>Order Service Tests</h1>
    <div class="test-method">testCreateOrder</div>
    <div class="assertion">order id is not null</div>
    <div class="assertion">status is CREATED</div>
    <div class="assertion">total matches expected</div>
</body>
</html>
"""


@pytest.fixture()
def export_dir(tmp_path: Path) -> Path:
    """Create a temp directory with two sample HTML export files."""
    export_path = tmp_path / "exports"
    export_path.mkdir()
    (export_path / "UserServiceTest.html").write_text(SAMPLE_EXPORT_HTML, encoding="utf-8")
    (export_path / "OrderServiceTest.html").write_text(SAMPLE_EXPORT_HTML_2, encoding="utf-8")
    return export_path


@pytest.fixture()
def empty_export_dir(tmp_path: Path) -> Path:
    """Create an empty temp export directory."""
    export_path = tmp_path / "empty_exports"
    export_path.mkdir()
    return export_path


# ===========================================================================
# BaseReporter.scan_exports
# ===========================================================================


class TestBaseReporterScanExports:
    """Tests for BaseReporter.scan_exports -- finds .html files, ignores others."""

    def test_scan_finds_html_files(self, export_dir: Path) -> None:
        reporter = SummaryReporter()  # concrete subclass
        results = reporter.scan_exports(export_dir)

        names = {p.name for p in results}
        assert len(results) == 2
        assert "UserServiceTest.html" in names
        assert "OrderServiceTest.html" in names

    def test_scan_ignores_non_html_files(self, export_dir: Path) -> None:
        # Add non-HTML files that should be ignored
        (export_dir / "notes.txt").write_text("some notes")
        (export_dir / "data.json").write_text("{}")
        (export_dir / "image.png").write_bytes(b"\x89PNG")
        (export_dir / "style.css").write_text("body {}")

        reporter = HtmlReporter()
        results = reporter.scan_exports(export_dir)

        names = {p.name for p in results}
        assert len(results) == 2
        assert "notes.txt" not in names
        assert "data.json" not in names
        assert "image.png" not in names
        assert "style.css" not in names

    def test_scan_empty_directory(self, empty_export_dir: Path) -> None:
        reporter = MarkdownReporter()
        results = reporter.scan_exports(empty_export_dir)
        assert results == []

    def test_scan_nonexistent_directory(self, tmp_path: Path) -> None:
        reporter = SummaryReporter()
        results = reporter.scan_exports(tmp_path / "does_not_exist")
        assert results == []

    def test_base_reporter_is_abstract(self) -> None:
        with pytest.raises(TypeError):
            BaseReporter()  # type: ignore


# ===========================================================================
# SummaryReporter
# ===========================================================================


class TestSummaryReporterMarkdown:
    """SummaryReporter generating markdown format reports."""

    def test_generate_markdown_report(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "summary.md"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            format="markdown",
            report_type="summary",
        )

        reporter = SummaryReporter()
        result = reporter.generate(config)

        assert isinstance(result, ReportResult)
        assert result.output_file == output_file
        assert output_file.exists()

        content = output_file.read_text(encoding="utf-8")
        # Verify markdown structure
        assert "# DTR Summary Report" in content
        assert "## Statistics" in content
        assert "## Exports" in content
        # Verify stats in content
        assert "Total Exports: 2" in content
        assert "Test Count: 4" in content  # 3 from UserService + 1 from OrderService
        assert "Assertion Count: 5" in content  # 2 + 3

    def test_generate_markdown_stats_object(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.md",
            format="markdown",
        )

        result = SummaryReporter().generate(config)

        assert result.stats["export_count"] == 2
        assert result.stats["test_count"] == 4
        assert result.stats["assertion_count"] == 5

    def test_generate_markdown_lists_each_export(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.md",
            format="markdown",
        )

        SummaryReporter().generate(config)
        content = (tmp_path / "summary.md").read_text(encoding="utf-8")

        assert "UserServiceTest" in content
        assert "OrderServiceTest" in content
        assert "UserServiceTest.html" in content
        assert "OrderServiceTest.html" in content


class TestSummaryReporterHtml:
    """SummaryReporter generating HTML format reports."""

    def test_generate_html_report(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "summary.html"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            format="html",
            report_type="summary",
        )

        result = SummaryReporter().generate(config)

        assert result.output_file == output_file
        assert output_file.exists()

        content = output_file.read_text(encoding="utf-8")
        assert "<!DOCTYPE html>" in content
        assert "<title>DTR Summary Report</title>" in content
        assert "<h1>DTR Summary Report</h1>" in content

    def test_html_report_contains_statistics(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.html",
            format="html",
        )

        SummaryReporter().generate(config)
        content = (tmp_path / "summary.html").read_text(encoding="utf-8")

        assert "Total Exports: 2" in content
        assert "Test Count: 4" in content
        assert "Assertion Count: 5" in content

    def test_html_report_has_table_rows(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.html",
            format="html",
        )

        SummaryReporter().generate(config)
        content = (tmp_path / "summary.html").read_text(encoding="utf-8")

        # Table header
        assert "<th>Test Class</th>" in content
        assert "<th>Tests</th>" in content
        assert "<th>Assertions</th>" in content
        # Table rows for each export
        assert "<td>UserServiceTest</td>" in content
        assert "<td>OrderServiceTest</td>" in content


class TestSummaryReporterJson:
    """SummaryReporter generating JSON format reports."""

    def test_generate_json_report(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "summary.json"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            format="json",
            report_type="summary",
        )

        result = SummaryReporter().generate(config)

        assert result.output_file == output_file
        assert output_file.exists()

        data = json.loads(output_file.read_text(encoding="utf-8"))
        assert data["title"] == "DTR Summary Report"
        assert "timestamp" in data

    def test_json_report_statistics(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.json",
            format="json",
        )

        SummaryReporter().generate(config)
        data = json.loads((tmp_path / "summary.json").read_text(encoding="utf-8"))

        stats = data["statistics"]
        assert stats["export_count"] == 2
        assert stats["test_count"] == 4
        assert stats["assertion_count"] == 5

    def test_json_report_exports_list(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.json",
            format="json",
        )

        SummaryReporter().generate(config)
        data = json.loads((tmp_path / "summary.json").read_text(encoding="utf-8"))

        exports = data["exports"]
        assert len(exports) == 2
        names = {e["name"] for e in exports}
        assert "UserServiceTest" in names
        assert "OrderServiceTest" in names

        # Each export should have expected keys
        for export in exports:
            assert "name" in export
            assert "file" in export
            assert "test_count" in export
            assert "assertion_count" in export
            assert "sections" in export

    def test_json_report_sections_extracted(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "summary.json",
            format="json",
        )

        SummaryReporter().generate(config)
        data = json.loads((tmp_path / "summary.json").read_text(encoding="utf-8"))

        # UserServiceTest has h1 "User Service Tests" and h2 "Authentication"
        user_export = next(e for e in data["exports"] if e["name"] == "UserServiceTest")
        assert "User Service Tests" in user_export["sections"]
        assert "Authentication" in user_export["sections"]


class TestSummaryReporterEmpty:
    """SummaryReporter with empty export directory."""

    def test_empty_export_markdown(self, empty_export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=empty_export_dir,
            output_path=tmp_path / "summary.md",
            format="markdown",
        )

        result = SummaryReporter().generate(config)

        assert result.stats["export_count"] == 0
        assert result.stats["test_count"] == 0
        assert result.stats["assertion_count"] == 0
        assert (tmp_path / "summary.md").exists()

    def test_empty_export_json(self, empty_export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=empty_export_dir,
            output_path=tmp_path / "summary.json",
            format="json",
        )

        SummaryReporter().generate(config)
        data = json.loads((tmp_path / "summary.json").read_text(encoding="utf-8"))

        assert data["statistics"]["export_count"] == 0
        assert data["exports"] == []


# ===========================================================================
# HtmlReporter
# ===========================================================================


class TestHtmlReporterCoverage:
    """HtmlReporter generating coverage reports."""

    def test_generate_coverage_report(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "coverage.html"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            format="html",
            report_type="coverage",
        )

        result = HtmlReporter().generate(config)

        assert isinstance(result, ReportResult)
        assert result.output_file == output_file
        assert result.stats["exports_analyzed"] == 2
        assert output_file.exists()

    def test_coverage_report_html_structure(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "coverage.html",
            report_type="coverage",
        )

        HtmlReporter().generate(config)
        content = (tmp_path / "coverage.html").read_text(encoding="utf-8")

        assert "<!DOCTYPE html>" in content
        assert "<title>API Coverage Report</title>" in content
        assert "<h1>API Endpoint Coverage Report</h1>" in content

    def test_coverage_report_table_rows(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "coverage.html",
            report_type="coverage",
        )

        HtmlReporter().generate(config)
        content = (tmp_path / "coverage.html").read_text(encoding="utf-8")

        # Each export gets a table row with name, covered status, and link
        assert "UserServiceTest" in content
        assert "OrderServiceTest" in content
        assert 'class="covered"' in content
        assert "View" in content
        # Links should reference original files
        assert "UserServiceTest.html" in content
        assert "OrderServiceTest.html" in content


class TestHtmlReporterDefault:
    """HtmlReporter generating default reports."""

    def test_generate_default_report(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "report.html"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            report_type="default",
        )

        result = HtmlReporter().generate(config)

        assert result.output_file == output_file
        assert result.stats["exports_analyzed"] == 2

    def test_default_report_html_structure(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "report.html",
            report_type="default",
        )

        HtmlReporter().generate(config)
        content = (tmp_path / "report.html").read_text(encoding="utf-8")

        assert "<!DOCTYPE html>" in content
        assert "<title>DTR Report</title>" in content
        assert "<h1>DTR Report</h1>" in content

    def test_default_report_shows_file_sizes(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "report.html",
            report_type="default",
        )

        HtmlReporter().generate(config)
        content = (tmp_path / "report.html").read_text(encoding="utf-8")

        # Default report includes size column
        assert "<th>Size</th>" in content
        assert "bytes" in content
        assert "UserServiceTest" in content


class TestHtmlReporterEmpty:
    """HtmlReporter with empty export directory."""

    def test_empty_directory_coverage(self, empty_export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=empty_export_dir,
            output_path=tmp_path / "coverage.html",
            report_type="coverage",
        )

        result = HtmlReporter().generate(config)

        assert result.stats["exports_analyzed"] == 0
        content = (tmp_path / "coverage.html").read_text(encoding="utf-8")
        assert "<!DOCTYPE html>" in content

    def test_empty_directory_default(self, empty_export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=empty_export_dir,
            output_path=tmp_path / "report.html",
            report_type="default",
        )

        result = HtmlReporter().generate(config)

        assert result.stats["exports_analyzed"] == 0
        assert (tmp_path / "report.html").exists()


# ===========================================================================
# MarkdownReporter
# ===========================================================================


class TestMarkdownReporterChangelog:
    """MarkdownReporter generating changelog reports."""

    def test_generate_changelog(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "changelog.md"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            format="markdown",
            report_type="changelog",
        )

        result = MarkdownReporter().generate(config)

        assert isinstance(result, ReportResult)
        assert result.output_file == output_file
        assert result.stats["exports_analyzed"] == 2
        assert output_file.exists()

    def test_changelog_content_structure(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "changelog.md",
            report_type="changelog",
        )

        MarkdownReporter().generate(config)
        content = (tmp_path / "changelog.md").read_text(encoding="utf-8")

        assert "# Changelog" in content
        assert "**Generated:**" in content
        assert "## New / Updated Tests" in content
        assert "## Summary" in content
        assert "Total Exports: 2" in content

    def test_changelog_lists_exports(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "changelog.md",
            report_type="changelog",
        )

        MarkdownReporter().generate(config)
        content = (tmp_path / "changelog.md").read_text(encoding="utf-8")

        assert "**UserServiceTest**" in content
        assert "**OrderServiceTest**" in content
        assert "UserServiceTest.html" in content
        assert "OrderServiceTest.html" in content
        # Modified timestamps should be present
        assert "Modified:" in content

    def test_changelog_with_since_parameter(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "changelog.md",
            report_type="changelog",
            since="2.4.0",
        )

        MarkdownReporter().generate(config)
        content = (tmp_path / "changelog.md").read_text(encoding="utf-8")

        assert "**Since Version:** 2.4.0" in content

    def test_changelog_without_since_omits_version_line(
        self, export_dir: Path, tmp_path: Path
    ) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "changelog.md",
            report_type="changelog",
            since=None,
        )

        MarkdownReporter().generate(config)
        content = (tmp_path / "changelog.md").read_text(encoding="utf-8")

        assert "Since Version" not in content


class TestMarkdownReporterDefault:
    """MarkdownReporter generating default table-format reports."""

    def test_generate_default_report(self, export_dir: Path, tmp_path: Path) -> None:
        output_file = tmp_path / "report.md"
        config = ReportConfig(
            export_path=export_dir,
            output_path=output_file,
            report_type="default",
        )

        result = MarkdownReporter().generate(config)

        assert result.output_file == output_file
        assert result.stats["exports_analyzed"] == 2

    def test_default_report_has_table(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "report.md",
            report_type="default",
        )

        MarkdownReporter().generate(config)
        content = (tmp_path / "report.md").read_text(encoding="utf-8")

        assert "# DTR Exports Report" in content
        assert "## Exports" in content
        # Markdown table structure
        assert "| Export | Size | Modified |" in content
        assert "|--------|------|----------|" in content
        # Export entries in table rows
        assert "UserServiceTest" in content
        assert "OrderServiceTest" in content

    def test_default_report_shows_sizes(self, export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=export_dir,
            output_path=tmp_path / "report.md",
            report_type="default",
        )

        MarkdownReporter().generate(config)
        content = (tmp_path / "report.md").read_text(encoding="utf-8")

        # Files are small so sizes should be in bytes
        assert " B |" in content


class TestMarkdownReporterEmpty:
    """MarkdownReporter with empty export directory."""

    def test_empty_directory_changelog(self, empty_export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=empty_export_dir,
            output_path=tmp_path / "changelog.md",
            report_type="changelog",
        )

        result = MarkdownReporter().generate(config)

        assert result.stats["exports_analyzed"] == 0
        content = (tmp_path / "changelog.md").read_text(encoding="utf-8")
        assert "# Changelog" in content
        assert "Total Exports: 0" in content

    def test_empty_directory_default(self, empty_export_dir: Path, tmp_path: Path) -> None:
        config = ReportConfig(
            export_path=empty_export_dir,
            output_path=tmp_path / "report.md",
            report_type="default",
        )

        result = MarkdownReporter().generate(config)

        assert result.stats["exports_analyzed"] == 0
        content = (tmp_path / "report.md").read_text(encoding="utf-8")
        assert "# DTR Exports Report" in content
        # Table header should still be present, just no data rows
        assert "| Export | Size | Modified |" in content
