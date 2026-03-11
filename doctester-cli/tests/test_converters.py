"""Tests for format converters."""

from pathlib import Path

import pytest

from doctester_cli.converters.base_converter import BaseConverter
from doctester_cli.model import ConversionConfig, ConversionResult


# Test BaseConverter interface (converters are called via CLI, not instantiated directly)


def test_converter_base_class_has_convert_method() -> None:
    """Test that BaseConverter requires a convert method."""
    # Verify BaseConverter is abstract
    assert hasattr(BaseConverter, "convert")
    # BaseConverter shouldn't be instantiatable directly
    with pytest.raises(TypeError):
        BaseConverter()  # type: ignore


def test_conversion_result_creation() -> None:
    """Test creating a ConversionResult."""
    result = ConversionResult(files_processed=5, warnings=["warning1", "warning2"])

    assert result.files_processed == 5
    assert len(result.warnings) == 2
    assert "warning1" in result.warnings


def test_conversion_config_with_paths(tmp_path: Path) -> None:
    """Test ConversionConfig with file paths."""
    input_path = tmp_path / "input.html"
    output_path = tmp_path / "output"
    input_path.write_text("<html><body>test</body></html>")

    config = ConversionConfig(
        input_path=input_path,
        output_path=output_path,
        recursive=False,
        force=True,
    )

    assert config.input_path == input_path
    assert config.output_path == output_path
    assert config.recursive is False
    assert config.force is True
