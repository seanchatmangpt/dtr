"""Generate HTML reports."""

from pathlib import Path

from dtr_cli.model import ReportConfig, ReportResult
from dtr_cli.reporters.base_reporter import BaseReporter


class HtmlReporter(BaseReporter):
    """Generate HTML reports."""

    def generate(self, config: ReportConfig) -> ReportResult:
        """Generate an HTML report."""
        exports = self.scan_exports(config.export_path)

        if config.report_type == "coverage":
            content = self._generate_coverage_report(exports)
        else:
            content = self._generate_default_report(exports)

        config.output_path.write_text(content, encoding="utf-8")

        return ReportResult(
            output_file=config.output_path,
            stats={"exports_analyzed": len(exports)},
        )

    def _generate_coverage_report(self, exports: list[Path]) -> str:
        """Generate endpoint coverage report."""
        rows = ""
        for export in exports:
            rows += f"""
            <tr>
                <td>{export.stem}</td>
                <td class="covered">Yes</td>
                <td><a href="{export.name}">View</a></td>
            </tr>
            """

        return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>API Coverage Report</title>
    <style>
        body {{ font-family: sans-serif; margin: 20px; background: #f5f5f5; }}
        .container {{ max-width: 1000px; margin: 0 auto; background: white; padding: 20px; border-radius: 8px; }}
        h1 {{ color: #333; border-bottom: 3px solid #4CAF50; padding-bottom: 10px; }}
        table {{ border-collapse: collapse; width: 100%; margin-top: 20px; }}
        th, td {{ border: 1px solid #ddd; padding: 12px; text-align: left; }}
        th {{ background: #4CAF50; color: white; }}
        tr:nth-child(even) {{ background: #f9f9f9; }}
        .covered {{ color: #4CAF50; font-weight: bold; }}
        a {{ color: #0066cc; text-decoration: none; }}
        a:hover {{ text-decoration: underline; }}
    </style>
</head>
<body>
    <div class="container">
        <h1>API Endpoint Coverage Report</h1>
        <p>This report shows which API endpoints are documented with tests.</p>

        <table>
            <tr>
                <th>Endpoint</th>
                <th>Documented</th>
                <th>Action</th>
            </tr>
            {rows}
        </table>
    </div>
</body>
</html>"""

    def _generate_default_report(self, exports: list[Path]) -> str:
        """Generate default HTML report."""
        rows = ""
        for export in exports:
            rows += f"""
            <tr>
                <td><strong>{export.stem}</strong></td>
                <td>{export.stat().st_size} bytes</td>
                <td><a href="{export.name}">View</a></td>
            </tr>
            """

        return f"""<!DOCTYPE html>
<html>
<head>
    <meta charset="utf-8">
    <title>DTR Report</title>
    <style>
        body {{ font-family: sans-serif; margin: 20px; }}
        h1 {{ color: #333; }}
        table {{ border-collapse: collapse; width: 100%; }}
        th, td {{ border: 1px solid #ddd; padding: 12px; text-align: left; }}
        th {{ background: #4CAF50; color: white; }}
    </style>
</head>
<body>
    <h1>DTR Report</h1>
    <table>
        <tr>
            <th>Name</th>
            <th>Size</th>
            <th>Link</th>
        </tr>
        {rows}
    </table>
</body>
</html>"""
