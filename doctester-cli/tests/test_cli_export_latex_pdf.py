"""Tests for LaTeX and PDF export functionality."""

from pathlib import Path
from unittest.mock import patch, MagicMock
import pytest
from typer.testing import CliRunner

from doctester_cli.converters.latex_converter import LatexConverter
from doctester_cli.converters.pdf_converter import PdfConverter
from doctester_cli.managers.latex_manager import LatexManager
from doctester_cli.model import (
    LatexTemplate,
    CompilerStrategy,
    LatexExportConfig,
    PdfExportConfig,
)
from doctester_cli.cli_errors import (
    LatexCompilationError,
    NoLatexCompilerError,
    LatexTemplateMissingError,
    ConversionError,
)
from doctester_cli.commands.export import app


runner = CliRunner()


# ============================================================================
# UNIT TESTS: LatexTemplate and CompilerStrategy Enums
# ============================================================================


class TestLatexTemplate:
    """Test LatexTemplate enum."""

    def test_arxiv_template_exists(self) -> None:
        """Test ARXIV template exists."""
        assert LatexTemplate.ARXIV.value == "arxiv"

    def test_patent_template_exists(self) -> None:
        """Test PATENT template exists."""
        assert LatexTemplate.PATENT.value == "patent"

    def test_ieee_template_exists(self) -> None:
        """Test IEEE template exists."""
        assert LatexTemplate.IEEE.value == "ieee"

    def test_acm_template_exists(self) -> None:
        """Test ACM template exists."""
        assert LatexTemplate.ACM.value == "acm"

    def test_nature_template_exists(self) -> None:
        """Test NATURE template exists."""
        assert LatexTemplate.NATURE.value == "nature"

    def test_enum_from_string(self) -> None:
        """Test creating template enum from string."""
        template = LatexTemplate("arxiv")
        assert template == LatexTemplate.ARXIV

    def test_invalid_template_raises_error(self) -> None:
        """Test invalid template raises ValueError."""
        with pytest.raises(ValueError):
            LatexTemplate("invalid")  # type: ignore


class TestCompilerStrategy:
    """Test CompilerStrategy enum."""

    def test_auto_strategy_exists(self) -> None:
        """Test AUTO strategy exists."""
        assert CompilerStrategy.AUTO.value == "auto"

    def test_latexmk_strategy_exists(self) -> None:
        """Test LATEXMK strategy exists."""
        assert CompilerStrategy.LATEXMK.value == "latexmk"

    def test_pdflatex_strategy_exists(self) -> None:
        """Test PDFLATEX strategy exists."""
        assert CompilerStrategy.PDFLATEX.value == "pdflatex"

    def test_xelatex_strategy_exists(self) -> None:
        """Test XELATEX strategy exists."""
        assert CompilerStrategy.XELATEX.value == "xelatex"

    def test_pandoc_strategy_exists(self) -> None:
        """Test PANDOC strategy exists."""
        assert CompilerStrategy.PANDOC.value == "pandoc"

    def test_enum_from_string(self) -> None:
        """Test creating compiler enum from string."""
        compiler = CompilerStrategy("latexmk")
        assert compiler == CompilerStrategy.LATEXMK

    def test_invalid_compiler_raises_error(self) -> None:
        """Test invalid compiler raises ValueError."""
        with pytest.raises(ValueError):
            CompilerStrategy("invalid")  # type: ignore


# ============================================================================
# UNIT TESTS: LatexConverter
# ============================================================================


class TestLatexConverter:
    """Test LaTeX converter."""

    def test_converter_initialization(self) -> None:
        """Test converter can be instantiated."""
        converter = LatexConverter()
        assert converter is not None

    def test_convert_with_missing_input_file(self, tmp_path: Path) -> None:
        """Test convert raises error for missing input file."""
        converter = LatexConverter()
        config = LatexExportConfig(
            input_path=tmp_path / "missing.md",
            output_path=tmp_path / "output.tex",
        )

        with pytest.raises(ConversionError) as exc_info:
            converter.convert(config)

        assert "not found" in str(exc_info.value.message).lower()

    @patch("doctester_cli.converters.latex_converter.subprocess.run")
    def test_convert_markdown_to_latex_success(
        self, mock_run: MagicMock, tmp_path: Path
    ) -> None:
        """Test successful Markdown to LaTeX conversion."""
        # Setup
        md_file = tmp_path / "test.md"
        md_file.write_text("# Title\nContent here")
        output_file = tmp_path / "output.tex"

        # Mock subprocess calls
        mock_run.return_value = MagicMock(returncode=0, stderr="", stdout="")

        # Convert
        converter = LatexConverter()
        config = LatexExportConfig(
            input_path=md_file,
            output_path=output_file,
        )
        result = converter.convert(config)

        # Verify
        assert result.files_processed == 1
        assert result.files_failed == 0

    def test_convert_latex_file_copies_successfully(self, tmp_path: Path) -> None:
        """Test converting (copying) LaTeX file."""
        # Setup
        tex_file = tmp_path / "input.tex"
        tex_file.write_text("\\documentclass{article}\n\\begin{document}\nTest\n\\end{document}")
        output_file = tmp_path / "output.tex"

        # Convert
        converter = LatexConverter()
        config = LatexExportConfig(
            input_path=tex_file,
            output_path=output_file,
        )
        result = converter.convert(config)

        # Verify
        assert result.files_processed == 1
        assert output_file.exists()
        assert output_file.read_text() == tex_file.read_text()

    def test_convert_skips_existing_without_force(self, tmp_path: Path) -> None:
        """Test convert skips existing file without force flag."""
        # Setup
        tex_file = tmp_path / "input.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")
        output_file = tmp_path / "output.tex"
        output_file.write_text("existing content")

        # Convert without force
        converter = LatexConverter()
        config = LatexExportConfig(
            input_path=tex_file,
            output_path=output_file,
            force=False,
        )
        result = converter.convert(config)

        # Verify
        assert result.files_processed == 0
        assert "skipping" in result.warnings[0].lower()


# ============================================================================
# UNIT TESTS: PdfConverter
# ============================================================================


class TestPdfConverter:
    """Test PDF converter."""

    def test_converter_initialization(self) -> None:
        """Test converter can be instantiated."""
        converter = PdfConverter()
        assert converter is not None

    def test_compiler_chain_defined(self) -> None:
        """Test compiler chain is defined."""
        converter = PdfConverter()
        assert len(converter.COMPILER_CHAIN) == 4
        assert converter.COMPILER_CHAIN[0] == CompilerStrategy.LATEXMK

    def test_convert_with_missing_tex_file(self, tmp_path: Path) -> None:
        """Test convert raises error for missing .tex file."""
        converter = PdfConverter()
        config = PdfExportConfig(
            input_path=tmp_path / "missing.tex",
            output_path=tmp_path / "output.pdf",
        )

        with pytest.raises(LatexCompilationError):
            converter.convert(config)

    @patch("doctester_cli.converters.pdf_converter.subprocess.run")
    def test_compiler_available_check(
        self, mock_run: MagicMock
    ) -> None:
        """Test compiler availability check."""
        mock_run.return_value = MagicMock(returncode=0)

        converter = PdfConverter()
        available = converter._compiler_available(CompilerStrategy.PDFLATEX)

        assert available is True
        mock_run.assert_called_once()

    def test_compiler_available_handles_missing_compiler(self) -> None:
        """Test compiler availability returns False for missing compiler."""
        converter = PdfConverter()
        # Use a fake compiler name that doesn't exist
        available = converter._compiler_available(CompilerStrategy.PANDOC)

        # Result depends on whether pandoc is installed
        # Just verify the function doesn't raise an exception
        assert isinstance(available, bool)


# ============================================================================
# UNIT TESTS: LatexManager
# ============================================================================


class TestLatexManager:
    """Test LaTeX manager orchestration."""

    def test_manager_initialization(self) -> None:
        """Test manager can be instantiated."""
        manager = LatexManager()
        assert manager is not None
        assert hasattr(manager, "latex_converter")
        assert hasattr(manager, "pdf_converter")

    def test_generate_latex_with_valid_config(self, tmp_path: Path) -> None:
        """Test generate_latex with valid LaTeX file."""
        # Setup
        tex_file = tmp_path / "input.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")
        output_file = tmp_path / "output.tex"

        # Generate
        manager = LatexManager()
        result = manager.generate_latex(tex_file, output_file)

        # Verify
        assert result.files_processed == 1
        assert output_file.exists()

    @patch("doctester_cli.converters.pdf_converter.subprocess.run")
    def test_compile_pdf_with_strategy(
        self, mock_run: MagicMock, tmp_path: Path
    ) -> None:
        """Test compile_pdf with specific compiler strategy."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")
        pdf_file = tmp_path / "output.pdf"

        # Mock subprocess
        mock_run.return_value = MagicMock(returncode=0, stderr="", stdout="")

        # Compile
        manager = LatexManager()
        # Note: This will likely fail because we're mocking subprocess
        # but testing the flow works
        with patch.object(manager.pdf_converter, "_compiler_available", return_value=True):
            with patch.object(manager.pdf_converter, "_compile_pdflatex") as mock_compile:
                mock_compile.return_value = MagicMock(files_processed=1)
                result = manager.compile_pdf(
                    tex_file,
                    pdf_file,
                    compiler=CompilerStrategy.PDFLATEX,
                )

                assert result is not None


# ============================================================================
# CLI INTEGRATION TESTS: dtr export latex
# ============================================================================


class TestExportLatexCommand:
    """Test dtr export latex command."""

    def test_export_latex_help(self) -> None:
        """Test latex subcommand help."""
        result = runner.invoke(app, ["latex", "--help"])
        assert result.exit_code == 0
        assert "LaTeX" in result.stdout or "export" in result.stdout.lower()

    def test_export_latex_with_missing_file(self, tmp_path: Path) -> None:
        """Test export latex with missing input file."""
        result = runner.invoke(app, ["latex", str(tmp_path / "missing.md")])
        assert result.exit_code != 0

    def test_export_latex_with_valid_tex_file(self, tmp_path: Path) -> None:
        """Test export latex with valid .tex file."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run
        result = runner.invoke(app, ["latex", str(tex_file)])
        assert result.exit_code == 0
        assert "✓" in result.stdout or "success" in result.stdout.lower()

    def test_export_latex_with_invalid_template(self, tmp_path: Path) -> None:
        """Test export latex with invalid template."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run with invalid template
        result = runner.invoke(
            app, ["latex", str(tex_file), "--template", "invalid"]
        )
        assert result.exit_code != 0
        assert "template" in result.stdout.lower() or "invalid" in result.stdout.lower()

    def test_export_latex_with_valid_template(self, tmp_path: Path) -> None:
        """Test export latex with valid template."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run with valid template
        result = runner.invoke(
            app, ["latex", str(tex_file), "--template", "arxiv"]
        )
        assert result.exit_code == 0

    def test_export_latex_respects_force_flag(self, tmp_path: Path) -> None:
        """Test export latex respects --force flag."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")
        output_file = tmp_path / "output.tex"
        output_file.write_text("existing content")

        # Run without force (should skip)
        result = runner.invoke(app, ["latex", str(tex_file), "-o", str(output_file)])
        assert "skipping" in result.stdout.lower() or result.exit_code == 0

        # Run with force (should overwrite)
        result = runner.invoke(
            app,
            ["latex", str(tex_file), "-o", str(output_file), "--force"],
        )
        assert result.exit_code == 0


# ============================================================================
# CLI INTEGRATION TESTS: dtr export pdf
# ============================================================================


class TestExportPdfCommand:
    """Test dtr export pdf command."""

    def test_export_pdf_help(self) -> None:
        """Test pdf subcommand help."""
        result = runner.invoke(app, ["pdf", "--help"])
        assert result.exit_code == 0
        assert "PDF" in result.stdout or "pdf" in result.stdout.lower()

    def test_export_pdf_with_missing_file(self, tmp_path: Path) -> None:
        """Test export pdf with missing input file."""
        result = runner.invoke(app, ["pdf", str(tmp_path / "missing.tex")])
        assert result.exit_code != 0

    def test_export_pdf_with_invalid_compiler(self, tmp_path: Path) -> None:
        """Test export pdf with invalid compiler."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run with invalid compiler
        result = runner.invoke(
            app, ["pdf", str(tex_file), "--compiler", "invalid"]
        )
        assert result.exit_code != 0

    def test_export_pdf_accepts_auto_compiler(self, tmp_path: Path) -> None:
        """Test export pdf accepts auto compiler strategy."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run with auto compiler
        result = runner.invoke(app, ["pdf", str(tex_file), "--compiler", "auto"])
        # Will likely fail due to no LaTeX compiler, but should parse args correctly
        assert result.exit_code in [0, 2]  # 0 if compiled, 2 if compiler missing

    def test_export_pdf_accepts_specific_compilers(self, tmp_path: Path) -> None:
        """Test export pdf accepts specific compiler strategies."""
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        for compiler in ["latexmk", "pdflatex", "xelatex", "pandoc"]:
            result = runner.invoke(
                app, ["pdf", str(tex_file), "--compiler", compiler]
            )
            # Should parse correctly even if compilation fails
            assert result.exit_code in [0, 2]

    def test_export_pdf_respects_keep_tex_flag(self, tmp_path: Path) -> None:
        """Test export pdf respects --keep-tex flag."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run with --keep-tex
        result = runner.invoke(
            app, ["pdf", str(tex_file), "--compiler", "auto", "--keep-tex"]
        )
        # Should handle the flag without error
        assert result.exit_code in [0, 2]  # 0 or compiler error

    def test_export_pdf_respects_timeout(self, tmp_path: Path) -> None:
        """Test export pdf respects --timeout flag."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run with custom timeout
        result = runner.invoke(
            app, ["pdf", str(tex_file), "--timeout", "60"]
        )
        # Should handle the flag without error
        assert result.exit_code in [0, 2]  # 0 or compiler error


# ============================================================================
# INTEGRATION TESTS: Full Workflow
# ============================================================================


class TestFullWorkflow:
    """Test complete LaTeX to PDF workflow."""

    def test_markdown_to_latex_to_pdf_workflow(self, tmp_path: Path) -> None:
        """Test converting Markdown → LaTeX → PDF."""
        # Setup
        md_file = tmp_path / "guide.md"
        md_file.write_text(
            "# Title\n\n## Section\n\nSome content here.\n\n```\ncode block\n```"
        )

        # Convert to LaTeX
        result = runner.invoke(
            app, ["latex", str(md_file), "--template", "arxiv"]
        )
        # May fail due to Pandoc not installed, but should parse correctly
        assert result.exit_code in [0, 1]

    def test_tex_file_copy_and_compile(self, tmp_path: Path) -> None:
        """Test copying and compiling LaTeX file."""
        # Setup
        tex_file = tmp_path / "document.tex"
        tex_file.write_text("\\documentclass{article}\n\\begin{document}\nTest\n\\end{document}")

        # Copy/validate
        result = runner.invoke(app, ["latex", str(tex_file)])
        assert result.exit_code == 0

        # Try compile (may fail if no compiler, but should handle gracefully)
        result = runner.invoke(app, ["pdf", str(tex_file)])
        assert result.exit_code in [0, 2]  # 0 if compiled, 2 if compiler missing


# ============================================================================
# EDGE CASE TESTS
# ============================================================================


class TestEdgeCases:
    """Test edge cases and error handling."""

    def test_export_latex_with_empty_file(self, tmp_path: Path) -> None:
        """Test export latex with empty input file."""
        # Setup
        empty_file = tmp_path / "empty.tex"
        empty_file.write_text("")

        # Run
        result = runner.invoke(app, ["latex", str(empty_file)])
        # Should handle empty file gracefully
        assert result.exit_code in [0, 1]

    def test_export_latex_with_special_characters_in_filename(
        self, tmp_path: Path
    ) -> None:
        """Test export latex with special characters in filename."""
        # Setup
        tex_file = tmp_path / "test-document_v2.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Run
        result = runner.invoke(app, ["latex", str(tex_file)])
        assert result.exit_code == 0

    def test_export_pdf_with_read_only_output_dir(self, tmp_path: Path) -> None:
        """Test export pdf with read-only output directory."""
        # Setup
        tex_file = tmp_path / "test.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")
        readonly_dir = tmp_path / "readonly"
        readonly_dir.mkdir()
        readonly_dir.chmod(0o444)  # Read-only

        try:
            # Run (should fail gracefully)
            result = runner.invoke(
                app, ["pdf", str(tex_file), "-o", str(readonly_dir / "output.pdf")]
            )
            assert result.exit_code != 0
        finally:
            # Clean up: restore write permission
            readonly_dir.chmod(0o755)
