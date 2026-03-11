"""Generate summary reports from exports."""

import json
from pathlib import Path
from bs4 import BeautifulSoup

from doctester_cli.model import ReportConfig, ReportResult
from doctester_cli.reporters.base_reporter import BaseReporter


class SummaryReporter(BaseReporter):
    """Generate summary reports of test exports."""

    def generate(self, config: ReportConfig) -> ReportResult:
        """Generate a summary report."""
        exports = self.scan_exports(config.export_path)

        stats = {
            "test_count": 0,
            "assertion_count": 0,
            "export_count": len(exports),
        }

        sections = []

        # Analyze each export
        for export_file in exports:
            try:
                summary = self._analyze_export(export_file)
                stats["test_count"] += summary.get("test_count", 0)
                stats["assertion_count"] += summary.get("assertion_count", 0)
                sections.append(summary)
            except Exception:
                pass

        # Generate report based on format
        if config.format == "markdown":
            content = self._generate_markdown_report(sections, stats)
        elif config.format == "html":
            content = self._generate_html_report(sections, stats)
        elif config.format == "json":
            content = self._generate_json_report(sections, stats)
        else:
            content = self._generate_markdown_report(sections, stats)

        config.output_path.write_text(content, encoding="utf-8")

        return ReportResult(output_file=config.output_path, stats=stats)

    def _analyze_export(self, export_file: Path) -> dict:
        """Analyze a single export file."""
        content = export_file.read_text(encoding="utf-8")
        soup = BeautifulSoup(content, "html.parser")

        return {
            "name": export_file.stem,
            "file": export_file.name,
            "test_count": self._count_tests(soup),
            "assertion_count": self._count_assertions(soup),
            "sections": self._extract_sections(soup),
        }

    def _count_tests(self, soup: BeautifulSoup) -> int:
        """Count test methods in a document."""
        # Look for test method indicators
        methods = soup.find_all(class_="test-method")
        return len(methods)

    def _count_assertions(self, soup: BeautifulSoup) -> int:
        """Count assertions in a document."""
        assertions = soup.find_all(class_="assertion")
        return len(assertions)

    def _extract_sections(self, soup: BeautifulSoup) -> list[str]:
        """Extract section headings."""
        sections = []
        for heading in soup.find_all(["h1", "h2"]):
            sections.append(heading.get_text())
        return sections

    def _generate_markdown_report(self, sections: list, stats: dict) -> str:
        """Generate Markdown format report."""
        lines = [
            "# DocTester Summary Report\n",
            f"**Generated:** {self._timestamp()}\n",
            f"## Statistics\n",
            f"- Total Exports: {stats['export_count']}\n",
            f"- Test Count: {stats['test_count']}\n",
            f"- Assertion Count: {stats['assertion_count']}\n",
            f"\n## Exports\n",
        ]

        for section in sections:
            lines.append(f"### {section.get('name', 'Unknown')}\n")
            lines.append(f"File: `{section.get('file', 'N/A')}`\n")
            lines.append(f"Tests: {section.get('test_count', 0)}\n")
            lines.append(f"Assertions: {section.get('assertion_count', 0)}\n\n")

        return "".join(lines)

    def _generate_html_report(self, sections: list, stats: dict) -> str:
        """Generate HTML format report."""
        rows = ""
        for section in sections:
            rows += f"""
            <tr>
                <td>{section.get('name', 'Unknown')}</td>
                <td>{section.get('test_count', 0)}</td>
                <td>{section.get('assertion_count', 0)}</td>
            </tr>
            """

        return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>DocTester Summary Report</title>
    <style>
        body {{ font-family: sans-serif; margin: 20px; }}
        h1 {{ color: #333; }}
        table {{ border-collapse: collapse; width: 100%; }}
        th, td {{ border: 1px solid #ddd; padding: 12px; text-align: left; }}
        th {{ background: #4CAF50; color: white; }}
        tr:nth-child(even) {{ background: #f9f9f9; }}
    </style>
</head>
<body>
    <h1>DocTester Summary Report</h1>
    <p><strong>Generated:</strong> {self._timestamp()}</p>

    <h2>Statistics</h2>
    <ul>
        <li>Total Exports: {stats['export_count']}</li>
        <li>Test Count: {stats['test_count']}</li>
        <li>Assertion Count: {stats['assertion_count']}</li>
    </ul>

    <h2>Exports</h2>
    <table>
        <tr>
            <th>Test Class</th>
            <th>Tests</th>
            <th>Assertions</th>
        </tr>
        {rows}
    </table>
</body>
</html>"""

    def _generate_json_report(self, sections: list, stats: dict) -> str:
        """Generate JSON format report."""
        data = {
            "title": "DocTester Summary Report",
            "timestamp": self._timestamp(),
            "statistics": stats,
            "exports": sections,
        }
        return json.dumps(data, indent=2)

    @staticmethod
    def _timestamp() -> str:
        """Get current timestamp."""
        from datetime import datetime

        return datetime.now().isoformat()
