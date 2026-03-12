"""Convert Markdown and HTML to LaTeX format."""

import subprocess
from pathlib import Path

from doctester_cli.cli_errors import ConversionError, OutputError
from doctester_cli.converters.base_converter import BaseConverter
from doctester_cli.model import ConversionResult, LatexExportConfig, LatexTemplate


class LatexConverter(BaseConverter):
    """Convert Markdown and HTML files to LaTeX format using Pandoc."""

    def convert(self, config: LatexExportConfig) -> ConversionResult:
        """Convert input file to LaTeX using Pandoc.

        Args:
            config: LaTeX export configuration

        Returns:
            ConversionResult with files processed and any warnings
        """
        if not config.input_path.exists():
            raise ConversionError(
                str(config.input_path),
                "LaTeX",
                f"Input file not found: {config.input_path}",
            )

        # Determine output path
        output_path = config.output_path or (
            config.input_path.parent / f"{config.input_path.stem}.tex"
        )

        # Check if we should overwrite
        if not self.should_overwrite(output_path, config.force):
            return ConversionResult(
                files_processed=0,
                warnings=[f"Output file exists, skipping: {output_path}"],
            )

        # Handle input file type
        if config.input_path.suffix.lower() in [".md", ".markdown"]:
            return self._convert_markdown_to_latex(
                config.input_path, output_path, config.template
            )
        elif config.input_path.suffix.lower() in [".tex"]:
            return self._copy_latex_file(config.input_path, output_path)
        elif config.input_path.suffix.lower() in [".html"]:
            return self._convert_html_to_latex(
                config.input_path, output_path, config.template
            )
        else:
            raise ConversionError(
                str(config.input_path),
                "LaTeX",
                f"Unsupported input format: {config.input_path.suffix}",
            )

    def _convert_markdown_to_latex(
        self, md_file: Path, output_file: Path, template: LatexTemplate
    ) -> ConversionResult:
        """Convert Markdown file to LaTeX using Pandoc.

        Args:
            md_file: Path to Markdown file
            output_file: Path to output LaTeX file
            template: LaTeX template to use

        Returns:
            ConversionResult

        Raises:
            ConversionError if Pandoc is not installed or conversion fails
        """
        try:
            # Check if Pandoc is available
            subprocess.run(
                ["pandoc", "--version"],
                capture_output=True,
                check=True,
                timeout=5,
            )
        except (subprocess.CalledProcessError, FileNotFoundError):
            raise ConversionError(
                str(md_file),
                "LaTeX",
                "Pandoc not installed. Install with: apt-get install pandoc",
            )

        try:
            # Convert Markdown to LaTeX
            result = subprocess.run(
                [
                    "pandoc",
                    str(md_file),
                    "-o",
                    str(output_file),
                    "--standalone",
                    f"--metadata=template:{template.value}",
                ],
                capture_output=True,
                text=True,
                check=False,
                timeout=30,
            )

            if result.returncode != 0:
                error_msg = result.stderr or result.stdout or "Unknown error"
                raise ConversionError(
                    str(md_file), "LaTeX", f"Pandoc conversion failed: {error_msg}"
                )

            return ConversionResult(files_processed=1)

        except subprocess.TimeoutExpired:
            raise ConversionError(
                str(md_file), "LaTeX", "Conversion timed out (30s)"
            )
        except Exception as e:
            raise ConversionError(str(md_file), "LaTeX", str(e))

    def _convert_html_to_latex(
        self, html_file: Path, output_file: Path, template: LatexTemplate
    ) -> ConversionResult:
        """Convert HTML file to LaTeX using Pandoc.

        Args:
            html_file: Path to HTML file
            output_file: Path to output LaTeX file
            template: LaTeX template to use

        Returns:
            ConversionResult

        Raises:
            ConversionError if conversion fails
        """
        try:
            # Check if Pandoc is available
            subprocess.run(
                ["pandoc", "--version"],
                capture_output=True,
                check=True,
                timeout=5,
            )
        except (subprocess.CalledProcessError, FileNotFoundError):
            raise ConversionError(
                str(html_file),
                "LaTeX",
                "Pandoc not installed. Install with: apt-get install pandoc",
            )

        try:
            # Convert HTML to LaTeX
            result = subprocess.run(
                [
                    "pandoc",
                    "-f",
                    "html",
                    "-t",
                    "latex",
                    str(html_file),
                    "-o",
                    str(output_file),
                    "--standalone",
                ],
                capture_output=True,
                text=True,
                check=False,
                timeout=30,
            )

            if result.returncode != 0:
                error_msg = result.stderr or result.stdout or "Unknown error"
                raise ConversionError(
                    str(html_file), "LaTeX", f"Pandoc conversion failed: {error_msg}"
                )

            return ConversionResult(files_processed=1)

        except subprocess.TimeoutExpired:
            raise ConversionError(
                str(html_file), "LaTeX", "Conversion timed out (30s)"
            )
        except Exception as e:
            raise ConversionError(str(html_file), "LaTeX", str(e))

    def _copy_latex_file(self, tex_file: Path, output_file: Path) -> ConversionResult:
        """Copy and validate LaTeX file.

        Args:
            tex_file: Path to source LaTeX file
            output_file: Path to output LaTeX file

        Returns:
            ConversionResult

        Raises:
            OutputError if copy fails
        """
        try:
            # Ensure output directory exists
            output_file.parent.mkdir(parents=True, exist_ok=True)

            # Copy file
            with open(tex_file, "r", encoding="utf-8") as src:
                content = src.read()

            with open(output_file, "w", encoding="utf-8") as dst:
                dst.write(content)

            return ConversionResult(files_processed=1)

        except Exception as e:
            raise OutputError(str(output_file), str(e))
