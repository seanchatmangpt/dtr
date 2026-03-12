"""Data models for DocTester CLI."""

from dataclasses import dataclass, field
from enum import Enum
from pathlib import Path
from typing import Any


class LatexTemplate(str, Enum):
    """LaTeX document templates (mirrors Java sealed interface).

    These enums must match the Java LatexTemplate sealed interface:
    - ARXIV: ArXivTemplate - academic papers
    - PATENT: UsPatentTemplate - patent/USPTO format
    - IEEE: IEEETemplate - IEEE journal/conference
    - ACM: ACMTemplate - ACM conference papers
    - NATURE: NatureTemplate - Nature journal format
    """

    ARXIV = "arxiv"
    PATENT = "patent"
    IEEE = "ieee"
    ACM = "acm"
    NATURE = "nature"


class CompilerStrategy(str, Enum):
    """LaTeX to PDF compiler strategies.

    Mirrors Java LatexCompiler fallback chain and CompilerStrategy sealed interface.
    - AUTO: Try chain in order: latexmk → pdflatex → xelatex → pandoc
    - LATEXMK: Recommended (multipass, aux cleanup)
    - PDFLATEX: Direct compilation
    - XELATEX: Modern Unicode support
    - PANDOC: Fallback (reduced fidelity)
    """

    AUTO = "auto"
    LATEXMK = "latexmk"
    PDFLATEX = "pdflatex"
    XELATEX = "xelatex"
    PANDOC = "pandoc"


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


@dataclass
class LatexExportConfig:
    """Configuration for LaTeX export operations."""

    input_path: Path
    output_path: Path | None = None
    template: LatexTemplate = LatexTemplate.ARXIV
    validate_syntax: bool = True
    force: bool = False


@dataclass
class PdfExportConfig:
    """Configuration for PDF export (LaTeX compilation)."""

    input_path: Path
    output_path: Path | None = None
    template: LatexTemplate = LatexTemplate.ARXIV
    compiler_strategy: CompilerStrategy = CompilerStrategy.AUTO
    keep_tex: bool = False
    compilation_timeout: int = 300
    force: bool = False


@dataclass
class ValidationResult:
    """Result of a validation check."""

    name: str
    passed: bool
    message: str = ""
    hint: str = ""


@dataclass
class PublishCheckConfig:
    """Configuration for publish pre-flight validation."""

    project_dir: Path
    ossrh_user: str | None = None
    ossrh_token: str | None = None
    gpg_key: str | None = None
    verbose: bool = False


@dataclass
class PublishDeployConfig:
    """Configuration for deploying to OSSRH staging."""

    project_dir: Path
    ossrh_user: str | None = None
    ossrh_token: str | None = None
    gpg_key: str | None = None
    gpg_passphrase: str | None = None
    skip_tests: bool = False
    dry_run: bool = False
    auto_release: bool = False


@dataclass
class PublishReleaseConfig:
    """Configuration for releasing from OSSRH staging to Maven Central."""

    ossrh_user: str | None = None
    ossrh_token: str | None = None
    wait: bool = False
    timeout: int = 1800  # 30 minutes


@dataclass
class PublishStatusConfig:
    """Configuration for checking Maven Central availability."""

    version: str | None = None
    wait: bool = False
    timeout: int = 1800
