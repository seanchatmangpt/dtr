"""Tests for format converters."""

from pathlib import Path

import pytest

from doctester_cli.converters.html_converter import HtmlConverter
from doctester_cli.model import ConversionConfig


@pytest.mark.parametrize("force", [True, False])
def test_html_converter_should_overwrite(force: bool) -> None:
    """Test HTML converter overwrite logic."""
    converter = HtmlConverter()

    # Non-existent file should always be overwriteable
    assert converter.should_overwrite(Path("/tmp/nonexistent.md"), force=force)


def test_html_to_markdown_conversion(tmp_export_dir: Path, tmp_path: Path) -> None:
    """Test HTML to Markdown conversion."""
    converter = HtmlConverter()

    config = ConversionConfig(
        input_path=tmp_export_dir,
        output_path=tmp_path / "output",
        recursive=False,
        force=True,
    )

    result = converter.convert_to_markdown(config)

    assert result.files_processed > 0
    assert len(result.warnings) >= 0
