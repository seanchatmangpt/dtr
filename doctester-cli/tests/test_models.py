"""Tests for data models."""

from pathlib import Path

from doctester_cli.model import (
    ConversionConfig,
    ConversionResult,
    PublishConfig,
    ReportConfig,
)


def test_conversion_config_creation(tmp_path: Path) -> None:
    """Test ConversionConfig dataclass."""
    config = ConversionConfig(
        input_path=tmp_path / "input",
        output_path=tmp_path / "output",
        recursive=True,
        force=False,
    )

    assert config.recursive is True
    assert config.force is False
    assert config.pretty is True
    assert config.template is None


def test_conversion_result_with_warnings() -> None:
    """Test ConversionResult with warnings."""
    result = ConversionResult(
        files_processed=5,
        files_failed=1,
        warnings=["Warning 1", "Warning 2"],
    )

    assert result.files_processed == 5
    assert result.files_failed == 1
    assert len(result.warnings) == 2


def test_report_config_defaults(tmp_path: Path) -> None:
    """Test ReportConfig with default values."""
    config = ReportConfig(
        export_path=tmp_path / "exports",
        output_path=tmp_path / "report.md",
    )

    assert config.format == "markdown"
    assert config.report_type == "summary"
    assert config.since is None


def test_publish_config_with_s3(tmp_path: Path) -> None:
    """Test PublishConfig for S3."""
    config = PublishConfig(
        export_path=tmp_path / "exports",
        platform="s3",
        bucket="my-bucket",
        prefix="docs/",
        region="us-west-2",
        public=True,
    )

    assert config.bucket == "my-bucket"
    assert config.region == "us-west-2"
    assert config.public is True
