"""Manage LaTeX generation and PDF compilation operations."""

import logging
from pathlib import Path

from doctester_cli.converters.latex_converter import LatexConverter
from doctester_cli.converters.pdf_converter import PdfConverter
from doctester_cli.model import (
    CompilerStrategy,
    ConversionResult,
    LatexExportConfig,
    LatexTemplate,
    PdfExportConfig,
)

logger = logging.getLogger(__name__)


class LatexManager:
    """Orchestrate LaTeX generation and PDF compilation operations."""

    def __init__(self):
        """Initialize LaTeX manager."""
        self.latex_converter = LatexConverter()
        self.pdf_converter = PdfConverter()

    def generate_latex(
        self,
        input_file: Path,
        output_file: Path | None = None,
        template: LatexTemplate = LatexTemplate.ARXIV,
        force: bool = False,
    ) -> ConversionResult:
        """Generate LaTeX from Markdown or HTML file.

        Args:
            input_file: Path to input file (Markdown, HTML, or LaTeX)
            output_file: Path to output .tex file (optional)
            template: LaTeX template to use
            force: Overwrite existing files

        Returns:
            ConversionResult with generated file information
        """
        config = LatexExportConfig(
            input_path=input_file,
            output_path=output_file,
            template=template,
            force=force,
        )

        logger.debug(f"Generating LaTeX from {input_file}")
        result = self.latex_converter.convert(config)
        logger.info(f"✓ Generated LaTeX: {output_file or input_file.stem}.tex")
        return result

    def compile_pdf(
        self,
        tex_file: Path,
        output_file: Path | None = None,
        template: LatexTemplate = LatexTemplate.ARXIV,
        compiler: CompilerStrategy = CompilerStrategy.AUTO,
        keep_tex: bool = False,
        timeout: int = 300,
        force: bool = False,
    ) -> ConversionResult:
        """Compile LaTeX file to PDF.

        Args:
            tex_file: Path to .tex file
            output_file: Path to output PDF (optional)
            template: LaTeX template (for future use)
            compiler: Compiler strategy to use
            keep_tex: Keep intermediate .tex file
            timeout: Compilation timeout in seconds
            force: Overwrite existing files

        Returns:
            ConversionResult with compiled PDF information
        """
        config = PdfExportConfig(
            input_path=tex_file,
            output_path=output_file,
            template=template,
            compiler_strategy=compiler,
            keep_tex=keep_tex,
            compilation_timeout=timeout,
            force=force,
        )

        logger.debug(f"Compiling LaTeX to PDF: {tex_file}")
        result = self.pdf_converter.convert(config)
        logger.info(f"✓ Compiled PDF: {output_file or tex_file.stem}.pdf")
        return result

    def generate_and_compile_latex(
        self,
        input_file: Path,
        output_pdf: Path | None = None,
        template: LatexTemplate = LatexTemplate.ARXIV,
        compiler: CompilerStrategy = CompilerStrategy.AUTO,
        keep_tex: bool = False,
        timeout: int = 300,
        force: bool = False,
    ) -> tuple[ConversionResult, ConversionResult]:
        """Generate LaTeX and compile to PDF in one operation.

        Args:
            input_file: Path to input file (Markdown or HTML)
            output_pdf: Path to output PDF (optional)
            template: LaTeX template to use
            compiler: Compiler strategy to use
            keep_tex: Keep intermediate .tex file
            timeout: Compilation timeout in seconds
            force: Overwrite existing files

        Returns:
            Tuple of (latex_result, pdf_result)
        """
        # Step 1: Generate LaTeX
        tex_file = input_file.parent / f"{input_file.stem}.tex"
        latex_result = self.generate_latex(input_file, tex_file, template, force)

        # Step 2: Compile PDF
        pdf_result = self.compile_pdf(
            tex_file,
            output_pdf,
            template,
            compiler,
            keep_tex,
            timeout,
            force,
        )

        return latex_result, pdf_result
