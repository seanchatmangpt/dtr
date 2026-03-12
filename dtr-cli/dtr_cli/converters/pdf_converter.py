"""Compile LaTeX files to PDF format."""

import logging
import subprocess
import tempfile
from pathlib import Path

from dtr_cli.cli_errors import LatexCompilationError, NoLatexCompilerError
from dtr_cli.converters.base_converter import BaseConverter
from dtr_cli.model import CompilerStrategy, ConversionResult, PdfExportConfig

logger = logging.getLogger(__name__)


class PdfConverter(BaseConverter):
    """Compile LaTeX files to PDF using compiler strategy chain."""

    # Compiler precedence: latexmk (best), pdflatex, xelatex, pandoc (fallback)
    COMPILER_CHAIN = [
        CompilerStrategy.LATEXMK,
        CompilerStrategy.PDFLATEX,
        CompilerStrategy.XELATEX,
        CompilerStrategy.PANDOC,
    ]

    def convert(self, config: PdfExportConfig) -> ConversionResult:
        """Compile LaTeX to PDF.

        Args:
            config: PDF export configuration

        Returns:
            ConversionResult with PDF output file path

        Raises:
            LatexCompilationError if all compiler strategies fail
        """
        if not config.input_path.exists():
            raise LatexCompilationError(
                "unknown", 1, f"Input file not found: {config.input_path}"
            )

        if config.input_path.suffix.lower() != ".tex":
            raise LatexCompilationError(
                "unknown",
                1,
                f"Expected .tex file, got: {config.input_path.suffix}",
            )

        # Determine output path
        output_path = config.output_path or (
            config.input_path.parent / f"{config.input_path.stem}.pdf"
        )

        # Check if we should overwrite
        if not self.should_overwrite(output_path, config.force):
            return ConversionResult(
                files_processed=0,
                warnings=[f"Output file exists, skipping: {output_path}"],
            )

        # Ensure output directory exists
        output_path.parent.mkdir(parents=True, exist_ok=True)

        # Select compiler strategy
        if config.compiler_strategy == CompilerStrategy.AUTO:
            return self._compile_with_fallback_chain(
                config.input_path, output_path, config.compilation_timeout
            )
        else:
            return self._compile_with_strategy(
                config.input_path,
                output_path,
                config.compiler_strategy,
                config.compilation_timeout,
            )

    def _compile_with_fallback_chain(
        self, tex_file: Path, output_file: Path, timeout: int
    ) -> ConversionResult:
        """Try compiler chain: latexmk → pdflatex → xelatex → pandoc.

        Args:
            tex_file: Path to LaTeX file
            output_file: Path to output PDF
            timeout: Compilation timeout in seconds

        Returns:
            ConversionResult

        Raises:
            NoLatexCompilerError if all strategies fail
            LatexCompilationError if user's preferred compiler fails
        """
        for compiler_strategy in self.COMPILER_CHAIN:
            logger.debug(f"Trying compiler: {compiler_strategy.value}")

            if not self._compiler_available(compiler_strategy):
                logger.debug(f"{compiler_strategy.value} not available")
                continue

            try:
                result = self._compile_with_strategy(
                    tex_file, output_file, compiler_strategy, timeout
                )
                logger.info(f"✓ Compiled with {compiler_strategy.value}")
                return result

            except LatexCompilationError as e:
                logger.debug(f"{compiler_strategy.value} failed: {e.message}")
                continue

        # All strategies failed
        logger.error("No LaTeX compiler available in fallback chain")
        raise NoLatexCompilerError()

    def _compile_with_strategy(
        self,
        tex_file: Path,
        output_file: Path,
        strategy: CompilerStrategy,
        timeout: int,
    ) -> ConversionResult:
        """Compile LaTeX using specific strategy.

        Args:
            tex_file: Path to LaTeX file
            output_file: Path to output PDF
            strategy: Compiler strategy to use
            timeout: Compilation timeout in seconds

        Returns:
            ConversionResult

        Raises:
            LatexCompilationError if compilation fails
        """
        if strategy == CompilerStrategy.LATEXMK:
            return self._compile_latexmk(tex_file, output_file, timeout)
        elif strategy == CompilerStrategy.PDFLATEX:
            return self._compile_pdflatex(tex_file, output_file, timeout)
        elif strategy == CompilerStrategy.XELATEX:
            return self._compile_xelatex(tex_file, output_file, timeout)
        elif strategy == CompilerStrategy.PANDOC:
            return self._compile_pandoc(tex_file, output_file, timeout)
        else:
            raise LatexCompilationError(
                strategy.value, 1, f"Unknown compiler strategy: {strategy.value}"
            )

    def _compile_latexmk(
        self, tex_file: Path, output_file: Path, timeout: int
    ) -> ConversionResult:
        """Compile using latexmk (recommended).

        Args:
            tex_file: Path to LaTeX file
            output_file: Path to output PDF
            timeout: Compilation timeout

        Returns:
            ConversionResult

        Raises:
            LatexCompilationError if compilation fails
        """
        try:
            result = subprocess.run(
                [
                    "latexmk",
                    "-pdf",
                    "-interaction=nonstopmode",
                    "-halt-on-error",
                    str(tex_file),
                ],
                cwd=tex_file.parent,
                capture_output=True,
                text=True,
                check=False,
                timeout=timeout,
            )

            if result.returncode != 0:
                error_details = result.stderr or result.stdout
                raise LatexCompilationError(
                    "latexmk", result.returncode, error_details[:500]
                )

            # Check if PDF was created
            pdf_file = tex_file.parent / f"{tex_file.stem}.pdf"
            if not pdf_file.exists():
                raise LatexCompilationError(
                    "latexmk", 1, "PDF not generated by latexmk"
                )

            # Move PDF to output location if different
            if pdf_file != output_file:
                pdf_file.rename(output_file)

            return ConversionResult(files_processed=1)

        except subprocess.TimeoutExpired:
            raise LatexCompilationError(
                "latexmk", 124, f"Compilation timed out after {timeout}s"
            )

    def _compile_pdflatex(
        self, tex_file: Path, output_file: Path, timeout: int
    ) -> ConversionResult:
        """Compile using pdflatex.

        Args:
            tex_file: Path to LaTeX file
            output_file: Path to output PDF
            timeout: Compilation timeout

        Returns:
            ConversionResult

        Raises:
            LatexCompilationError if compilation fails
        """
        try:
            with tempfile.TemporaryDirectory() as tmpdir:
                result = subprocess.run(
                    [
                        "pdflatex",
                        "-interaction=nonstopmode",
                        "-halt-on-error",
                        "-output-directory",
                        tmpdir,
                        str(tex_file),
                    ],
                    capture_output=True,
                    text=True,
                    check=False,
                    timeout=timeout,
                )

                if result.returncode != 0:
                    error_details = result.stderr or result.stdout
                    raise LatexCompilationError(
                        "pdflatex", result.returncode, error_details[:500]
                    )

                # Check if PDF was created
                pdf_file = Path(tmpdir) / f"{tex_file.stem}.pdf"
                if not pdf_file.exists():
                    raise LatexCompilationError(
                        "pdflatex", 1, "PDF not generated by pdflatex"
                    )

                # Copy PDF to output location
                output_file.parent.mkdir(parents=True, exist_ok=True)
                output_file.write_bytes(pdf_file.read_bytes())

                return ConversionResult(files_processed=1)

        except subprocess.TimeoutExpired:
            raise LatexCompilationError(
                "pdflatex", 124, f"Compilation timed out after {timeout}s"
            )

    def _compile_xelatex(
        self, tex_file: Path, output_file: Path, timeout: int
    ) -> ConversionResult:
        """Compile using xelatex (Unicode support).

        Args:
            tex_file: Path to LaTeX file
            output_file: Path to output PDF
            timeout: Compilation timeout

        Returns:
            ConversionResult

        Raises:
            LatexCompilationError if compilation fails
        """
        try:
            with tempfile.TemporaryDirectory() as tmpdir:
                result = subprocess.run(
                    [
                        "xelatex",
                        "-interaction=nonstopmode",
                        "-halt-on-error",
                        "-output-directory",
                        tmpdir,
                        str(tex_file),
                    ],
                    capture_output=True,
                    text=True,
                    check=False,
                    timeout=timeout,
                )

                if result.returncode != 0:
                    error_details = result.stderr or result.stdout
                    raise LatexCompilationError(
                        "xelatex", result.returncode, error_details[:500]
                    )

                # Check if PDF was created
                pdf_file = Path(tmpdir) / f"{tex_file.stem}.pdf"
                if not pdf_file.exists():
                    raise LatexCompilationError(
                        "xelatex", 1, "PDF not generated by xelatex"
                    )

                # Copy PDF to output location
                output_file.parent.mkdir(parents=True, exist_ok=True)
                output_file.write_bytes(pdf_file.read_bytes())

                return ConversionResult(files_processed=1)

        except subprocess.TimeoutExpired:
            raise LatexCompilationError(
                "xelatex", 124, f"Compilation timed out after {timeout}s"
            )

    def _compile_pandoc(
        self, tex_file: Path, output_file: Path, timeout: int
    ) -> ConversionResult:
        """Compile using pandoc (fallback).

        Args:
            tex_file: Path to LaTeX file
            output_file: Path to output PDF
            timeout: Compilation timeout

        Returns:
            ConversionResult

        Raises:
            LatexCompilationError if compilation fails
        """
        try:
            result = subprocess.run(
                [
                    "pandoc",
                    str(tex_file),
                    "-o",
                    str(output_file),
                    "-f",
                    "latex",
                    "-t",
                    "pdf",
                ],
                capture_output=True,
                text=True,
                check=False,
                timeout=timeout,
            )

            if result.returncode != 0:
                error_details = result.stderr or result.stdout
                raise LatexCompilationError(
                    "pandoc", result.returncode, error_details[:500]
                )

            if not output_file.exists():
                raise LatexCompilationError(
                    "pandoc", 1, "PDF not generated by pandoc"
                )

            return ConversionResult(files_processed=1)

        except subprocess.TimeoutExpired:
            raise LatexCompilationError(
                "pandoc", 124, f"Compilation timed out after {timeout}s"
            )

    def _compiler_available(self, strategy: CompilerStrategy) -> bool:
        """Check if compiler is available in PATH.

        Args:
            strategy: Compiler strategy

        Returns:
            True if compiler is available, False otherwise
        """
        compiler_map = {
            CompilerStrategy.LATEXMK: "latexmk",
            CompilerStrategy.PDFLATEX: "pdflatex",
            CompilerStrategy.XELATEX: "xelatex",
            CompilerStrategy.PANDOC: "pandoc",
        }

        compiler_name = compiler_map.get(strategy)
        if not compiler_name:
            return False

        try:
            subprocess.run(
                [compiler_name, "--version"],
                capture_output=True,
                check=True,
                timeout=5,
            )
            return True
        except (subprocess.CalledProcessError, FileNotFoundError):
            return False
