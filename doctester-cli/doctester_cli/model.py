"""Data models for DocTester CLI."""

from dataclasses import dataclass, field
from pathlib import Path
from typing import Optional, Dict, List, Any


@dataclass
class ConversionConfig:
    """Configuration for format conversion operations."""

    input_path: Path
    output_path: Path
    recursive: bool = False
    force: bool = False
    pretty: bool = True
    template: Optional[str] = None


@dataclass
class ConversionResult:
    """Result of a format conversion operation."""

    files_processed: int
    files_failed: int = 0
    warnings: List[str] = field(default_factory=list)
    errors: List[str] = field(default_factory=list)


@dataclass
class ReportConfig:
    """Configuration for report generation."""

    export_path: Path
    output_path: Path
    format: str = "markdown"
    report_type: str = "summary"
    since: Optional[str] = None


@dataclass
class ReportResult:
    """Result of a report generation operation."""

    output_file: Path
    stats: Dict[str, Any] = field(default_factory=dict)
    warnings: List[str] = field(default_factory=list)


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
    archive_path: Optional[Path] = None
    archive_format: str = "tar.gz"
    keep_latest: int = 5
    dry_run: bool = True


@dataclass
class ManageResult:
    """Result of a management operation."""

    removed_files: List[str] = field(default_factory=list)
    stats: Dict[str, Any] = field(default_factory=dict)
    issues: List[str] = field(default_factory=list)


@dataclass
class PublishConfig:
    """Configuration for publishing operations."""

    export_path: Path
    platform: str
    target: Optional[str] = None
    branch: Optional[str] = None
    token: Optional[str] = None
    repo: Optional[str] = None
    bucket: Optional[str] = None
    prefix: str = "docs/"
    region: str = "us-east-1"
    project: Optional[str] = None
    target_path: Optional[Path] = None
    public: bool = False


@dataclass
class PublishResult:
    """Result of a publishing operation."""

    platform: str
    url: Optional[str] = None
    files_count: int = 0
    status: str = "success"
    warnings: List[str] = field(default_factory=list)
