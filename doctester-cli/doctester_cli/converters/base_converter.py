"""Base converter class for format conversions."""

from abc import ABC, abstractmethod
from pathlib import Path

from doctester_cli.model import ConversionConfig, ConversionResult


class BaseConverter(ABC):
    """Abstract base class for format converters."""

    @abstractmethod
    def convert(self, config: ConversionConfig) -> ConversionResult:
        """Convert files according to configuration."""
        pass

    def get_input_files(self, config: ConversionConfig) -> list[Path]:
        """Get list of input files to process."""
        if config.input_path.is_file():
            return [config.input_path]

        if config.recursive:
            return list(config.input_path.rglob("*"))
        return list(config.input_path.glob("*"))

    def should_overwrite(self, output_path: Path, force: bool) -> bool:
        """Check if file should be overwritten."""
        if not output_path.exists():
            return True
        return force
