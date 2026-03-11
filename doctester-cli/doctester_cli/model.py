"""Data models for DocTester CLI."""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass
class ConversionConfig:
    """Configuration for format conversion operations."""

    input_path: Path
    output_path: Path
    recursive: bool = False
    force: bool = False
    pretty: bool = True
    template: str | None = None


@dataclass
class ConversionResult:
    """Result of a format conversion operation."""

    files_processed: int
    files_failed: int = 0
    warnings: list[str] = field(default_factory=list)
    errors: list[str] = field(default_factory=list)


@dataclass
class ReportConfig:
    """Configuration for report generation."""

    export_path: Path
    output_path: Path
    format: str = "markdown"
    report_type: str = "summary"
    since: str | None = None


@dataclass
class ReportResult:
    """Result of a report generation operation."""

    output_file: Path
    stats: dict[str, Any] = field(default_factory=dict)
    warnings: list[str] = field(default_factory=list)


@dataclass
class ExportInfo:
    """Information about a single export file."""

    name: str
    path: Path
    size: int
    modified: str


@dataclass
class ManageConfig:
    """Configuration for export directory management."""

    export_path: Path
    detailed: bool = False
    archive_path: Path | None = None
    archive_format: str = "tar.gz"
    keep_latest: int = 5
    dry_run: bool = True


@dataclass
class ManageResult:
    """Result of a management operation."""

    removed_files: list[str] = field(default_factory=list)
    stats: dict[str, Any] = field(default_factory=dict)
    issues: list[str] = field(default_factory=list)


@dataclass
class PublishConfig:
    """Configuration for publishing operations."""

    export_path: Path
    platform: str
    target: str | None = None
    branch: str | None = None
    token: str | None = None
    repo: str | None = None
    bucket: str | None = None
    prefix: str = "docs/"
    region: str = "us-east-1"
    project: str | None = None
    target_path: Path | None = None
    public: bool = False


@dataclass
class PublishResult:
    """Result of a publishing operation."""

    platform: str
    url: str | None = None
    files_count: int = 0
    status: str = "success"
    warnings: list[str] = field(default_factory=list)
