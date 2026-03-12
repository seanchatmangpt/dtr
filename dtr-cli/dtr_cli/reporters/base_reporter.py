"""Base reporter class for report generation."""

from abc import ABC, abstractmethod
from pathlib import Path

from dtr_cli.model import ReportConfig, ReportResult


class BaseReporter(ABC):
    """Abstract base class for report generators."""

    @abstractmethod
    def generate(self, config: ReportConfig) -> ReportResult:
        """Generate report according to configuration."""
        pass

    def scan_exports(self, export_path: Path) -> list[Path]:
        """Scan export directory for HTML files."""
        if not export_path.exists():
            return []
        return list(export_path.glob("*.html"))
