"""Generate Markdown reports."""

from pathlib import Path
from datetime import datetime

from dtr_cli.model import ReportConfig, ReportResult
from dtr_cli.reporters.base_reporter import BaseReporter


class MarkdownReporter(BaseReporter):
    """Generate Markdown reports."""

    def generate(self, config: ReportConfig) -> ReportResult:
        """Generate a Markdown report."""
        exports = self.scan_exports(config.export_path)

        if config.report_type == "changelog":
            content = self._generate_changelog(exports, config.since)
        else:
            content = self._generate_default_report(exports)

        config.output_path.write_text(content, encoding="utf-8")

        return ReportResult(
            output_file=config.output_path,
            stats={"exports_analyzed": len(exports)},
        )

    def _generate_changelog(self, exports: list[Path], since: str = None) -> str:
        """Generate changelog from test exports."""
        lines = [
            "# Changelog\n",
            f"**Generated:** {datetime.now().isoformat()}\n",
        ]

        if since:
            lines.append(f"**Since Version:** {since}\n")

        lines.append("\n## New / Updated Tests\n")

        for export in exports:
            lines.append(f"- **{export.stem}**\n")
            lines.append(f"  - File: `{export.name}`\n")
            lines.append(f"  - Modified: {datetime.fromtimestamp(export.stat().st_mtime).isoformat()}\n")

        lines.append("\n## Summary\n")
        lines.append(f"- Total Exports: {len(exports)}\n")

        return "".join(lines)

    def _generate_default_report(self, exports: list[Path]) -> str:
        """Generate default Markdown report."""
        lines = [
            "# DTR Exports Report\n",
            f"**Generated:** {datetime.now().isoformat()}\n",
            "\n## Exports\n",
        ]

        lines.append("| Export | Size | Modified |\n")
        lines.append("|--------|------|----------|\n")

        for export in exports:
            size = self._format_size(export.stat().st_size)
            mtime = datetime.fromtimestamp(export.stat().st_mtime).isoformat()
            lines.append(f"| {export.stem} | {size} | {mtime} |\n")

        return "".join(lines)

    @staticmethod
    def _format_size(bytes: int) -> str:
        """Format bytes to human-readable size."""
        for unit in ["B", "KB", "MB"]:
            if bytes < 1024:
                return f"{bytes:.1f} {unit}"
            bytes /= 1024
        return f"{bytes:.1f} GB"
