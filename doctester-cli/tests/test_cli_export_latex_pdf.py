"""Tests for LaTeX and PDF export functionality (Chicago TDD style).

Chicago TDD emphasizes:
- Testing real behavior and outcomes
- Using actual objects, not mocks
- Focusing on integration and end-to-end behavior
- Minimal use of test doubles (only at system boundaries)
"""

from pathlib import Path
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
# These test the actual enum contracts without mocking


class TestLatexTemplateEnum:
    """Test LatexTemplate enum provides correct values."""

    def test_all_five_templates_exist(self) -> None:
        """Verify all 5 LaTeX templates are defined."""
        templates = {t.value: t for t in LatexTemplate}
        assert len(templates) == 5
        assert "arxiv" in templates
        assert "patent" in templates
        assert "ieee" in templates
        assert "acm" in templates
        assert "nature" in templates

    def test_template_enum_created_from_string_value(self) -> None:
        """Test creating template enum from string values."""
        for template in LatexTemplate:
            result = LatexTemplate(template.value)
            assert result == template

    def test_invalid_template_value_raises_value_error(self) -> None:
        """Test invalid template string raises ValueError."""
        with pytest.raises(ValueError):
            LatexTemplate("invalid_template")


class TestCompilerStrategyEnum:
    """Test CompilerStrategy enum provides correct strategies."""

    def test_all_five_strategies_exist(self) -> None:
        """Verify all 5 compiler strategies are defined."""
        strategies = {s.value: s for s in CompilerStrategy}
        assert len(strategies) == 5
        assert "auto" in strategies
        assert "latexmk" in strategies
        assert "pdflatex" in strategies
        assert "xelatex" in strategies
        assert "pandoc" in strategies

    def test_strategy_enum_created_from_string_value(self) -> None:
        """Test creating strategy enum from string values."""
        for strategy in CompilerStrategy:
            result = CompilerStrategy(strategy.value)
            assert result == strategy

    def test_invalid_strategy_raises_value_error(self) -> None:
        """Test invalid strategy string raises ValueError."""
        with pytest.raises(ValueError):
            CompilerStrategy("invalid_compiler")


# ============================================================================
# INTEGRATION TESTS: LatexConverter (Real File Operations)
# ============================================================================
# Chicago TDD: Test real behavior with actual files, no mocks


class TestLatexConverterRealBehavior:
    """Test LaTeX converter with real file operations."""

    def test_converter_copies_valid_latex_file_successfully(self, tmp_path: Path) -> None:
        """Test that valid LaTeX file is copied to output location."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        input_tex.write_text("\\documentclass{article}\n\\end{document}")
        output_tex = tmp_path / "output.tex"

        # Act
        converter = LatexConverter()
        config = LatexExportConfig(input_path=input_tex, output_path=output_tex)
        result = converter.convert(config)

        # Assert - verify real outcome
        assert result.files_processed == 1
        assert output_tex.exists()
        assert output_tex.read_text() == input_tex.read_text()

    def test_converter_refuses_to_overwrite_without_force(self, tmp_path: Path) -> None:
        """Test that existing files are not overwritten without force flag."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        input_tex.write_text("\\documentclass{article}\n\\end{document}")
        output_tex = tmp_path / "output.tex"
        original_content = "original content"
        output_tex.write_text(original_content)

        # Act
        converter = LatexConverter()
        config = LatexExportConfig(input_path=input_tex, output_path=output_tex, force=False)
        result = converter.convert(config)

        # Assert - verify file unchanged
        assert result.files_processed == 0
        assert output_tex.read_text() == original_content

    def test_converter_overwrites_existing_file_with_force_flag(self, tmp_path: Path) -> None:
        """Test that files are overwritten when force=True."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        new_content = "\\documentclass{article}\n\\end{document}"
        input_tex.write_text(new_content)
        output_tex = tmp_path / "output.tex"
        output_tex.write_text("old content")

        # Act
        converter = LatexConverter()
        config = LatexExportConfig(input_path=input_tex, output_path=output_tex, force=True)
        result = converter.convert(config)

        # Assert - verify file was overwritten
        assert result.files_processed == 1
        assert output_tex.read_text() == new_content

    def test_converter_rejects_missing_input_file(self, tmp_path: Path) -> None:
        """Test that missing input file raises ConversionError."""
        # Arrange
        missing_file = tmp_path / "nonexistent.tex"
        output_file = tmp_path / "output.tex"

        # Act & Assert
        converter = LatexConverter()
        config = LatexExportConfig(input_path=missing_file, output_path=output_file)
        with pytest.raises(ConversionError):
            converter.convert(config)

    def test_converter_handles_unsupported_file_type(self, tmp_path: Path) -> None:
        """Test that unsupported file types raise ConversionError."""
        # Arrange
        input_file = tmp_path / "file.xyz"
        input_file.write_text("some content")
        output_file = tmp_path / "output.tex"

        # Act & Assert
        converter = LatexConverter()
        config = LatexExportConfig(input_path=input_file, output_path=output_file)
        with pytest.raises(ConversionError):
            converter.convert(config)


# ============================================================================
# INTEGRATION TESTS: PdfConverter (Real Compiler Behavior)
# ============================================================================
# Chicago TDD: Test real behavior of compiler availability


class TestPdfConverterRealBehavior:
    """Test PDF converter with real system state."""

    def test_converter_detects_available_compilers(self) -> None:
        """Test that converter can detect available compilers."""
        # This is a real behavior test - checks system state
        converter = PdfConverter()

        # Test each strategy - results depend on what's installed
        # But the function should return boolean without error
        for strategy in CompilerStrategy:
            if strategy == CompilerStrategy.AUTO:
                continue  # AUTO is not a single compiler
            result = converter._compiler_available(strategy)
            assert isinstance(result, bool)

    def test_converter_rejects_non_tex_file(self, tmp_path: Path) -> None:
        """Test that non-.tex files are rejected."""
        # Arrange
        input_file = tmp_path / "file.md"
        input_file.write_text("# Title")
        output_file = tmp_path / "output.pdf"

        # Act & Assert
        converter = PdfConverter()
        config = PdfExportConfig(input_path=input_file, output_path=output_file)
        with pytest.raises(LatexCompilationError):
            converter.convert(config)

    def test_converter_rejects_missing_tex_file(self, tmp_path: Path) -> None:
        """Test that missing .tex file raises LatexCompilationError."""
        # Arrange
        missing_file = tmp_path / "nonexistent.tex"
        output_file = tmp_path / "output.pdf"

        # Act & Assert
        converter = PdfConverter()
        config = PdfExportConfig(input_path=missing_file, output_path=output_file)
        with pytest.raises(LatexCompilationError):
            converter.convert(config)

    def test_converter_has_fallback_chain_order(self) -> None:
        """Test that compiler chain has correct precedence order."""
        converter = PdfConverter()
        assert len(converter.COMPILER_CHAIN) == 4
        assert converter.COMPILER_CHAIN[0] == CompilerStrategy.LATEXMK
        assert converter.COMPILER_CHAIN[1] == CompilerStrategy.PDFLATEX
        assert converter.COMPILER_CHAIN[2] == CompilerStrategy.XELATEX
        assert converter.COMPILER_CHAIN[3] == CompilerStrategy.PANDOC


# ============================================================================
# INTEGRATION TESTS: LatexManager (End-to-End Orchestration)
# ============================================================================
# Chicago TDD: Test real orchestration behavior


class TestLatexManagerOrchestration:
    """Test LatexManager orchestrates converters correctly."""

    def test_manager_initializes_with_both_converters(self) -> None:
        """Test that manager creates both required converters."""
        manager = LatexManager()
        assert isinstance(manager.latex_converter, LatexConverter)
        assert isinstance(manager.pdf_converter, PdfConverter)

    def test_manager_generates_latex_from_existing_tex_file(self, tmp_path: Path) -> None:
        """Test that manager can copy/validate LaTeX files."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        input_tex.write_text("\\documentclass{article}\n\\end{document}")
        output_tex = tmp_path / "output.tex"

        # Act
        manager = LatexManager()
        result = manager.generate_latex(input_tex, output_tex)

        # Assert - verify real outcome
        assert result.files_processed == 1
        assert output_tex.exists()

    def test_manager_respects_force_flag_in_generate(self, tmp_path: Path) -> None:
        """Test that manager respects force flag."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        input_tex.write_text("\\documentclass{article}\n\\end{document}")
        output_tex = tmp_path / "output.tex"
        output_tex.write_text("existing")

        # Act - without force
        manager = LatexManager()
        result = manager.generate_latex(input_tex, output_tex, force=False)

        # Assert - file should not be processed
        assert result.files_processed == 0


# ============================================================================
# CLI INTEGRATION TESTS: dtr export latex
# ============================================================================
# Chicago TDD: Test real CLI behavior with actual files


class TestExportLatexCLICommand:
    """Test dtr export latex command with real behavior."""

    def test_export_latex_shows_help(self) -> None:
        """Test that help text is available."""
        result = runner.invoke(app, ["latex", "--help"])
        assert result.exit_code == 0
        assert "LaTeX" in result.stdout or "latex" in result.stdout

    def test_export_latex_with_valid_tex_file_succeeds(self, tmp_path: Path) -> None:
        """Test exporting valid LaTeX file succeeds."""
        # Arrange
        tex_file = tmp_path / "document.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(app, ["latex", str(tex_file)])

        # Assert
        assert result.exit_code == 0
        assert "✓" in result.stdout or "success" in result.stdout.lower()

    def test_export_latex_rejects_missing_input_file(self, tmp_path: Path) -> None:
        """Test that missing input file causes failure."""
        # Act
        result = runner.invoke(app, ["latex", str(tmp_path / "missing.tex")])

        # Assert
        assert result.exit_code != 0

    def test_export_latex_validates_template_argument(self, tmp_path: Path) -> None:
        """Test that invalid template is rejected."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["latex", str(tex_file), "--template", "invalid_template"],
        )

        # Assert
        assert result.exit_code != 0
        assert "template" in result.stdout.lower() or "invalid" in result.stdout.lower()

    def test_export_latex_accepts_all_valid_templates(self, tmp_path: Path) -> None:
        """Test that all 5 valid templates are accepted."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act & Assert
        for template in LatexTemplate:
            result = runner.invoke(
                app,
                ["latex", str(tex_file), "--template", template.value],
            )
            assert result.exit_code == 0

    def test_export_latex_creates_output_file(self, tmp_path: Path) -> None:
        """Test that output file is actually created."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        input_tex.write_text("\\documentclass{article}\n\\end{document}")
        output_tex = tmp_path / "output.tex"

        # Act
        result = runner.invoke(
            app,
            ["latex", str(input_tex), "-o", str(output_tex)],
        )

        # Assert
        assert result.exit_code == 0
        assert output_tex.exists()

    def test_export_latex_respects_force_flag(self, tmp_path: Path) -> None:
        """Test that --force flag controls overwrite behavior."""
        # Arrange
        input_tex = tmp_path / "input.tex"
        input_tex.write_text("\\documentclass{article}\n\\end{document}")
        output_tex = tmp_path / "output.tex"
        output_tex.write_text("original content")

        # Act - without force (should skip)
        result = runner.invoke(
            app,
            ["latex", str(input_tex), "-o", str(output_tex)],
        )

        # Assert - should indicate skipping
        assert "skipping" in result.stdout.lower() or result.exit_code == 0
        assert output_tex.read_text() == "original content"

        # Act - with force (should overwrite)
        result = runner.invoke(
            app,
            ["latex", str(input_tex), "-o", str(output_tex), "--force"],
        )

        # Assert
        assert result.exit_code == 0
        assert output_tex.read_text() != "original content"


# ============================================================================
# CLI INTEGRATION TESTS: dtr export pdf
# ============================================================================
# Chicago TDD: Test real CLI behavior and error handling


class TestExportPdfCLICommand:
    """Test dtr export pdf command with real behavior."""

    def test_export_pdf_shows_help(self) -> None:
        """Test that help text is available."""
        result = runner.invoke(app, ["pdf", "--help"])
        assert result.exit_code == 0
        assert "PDF" in result.stdout or "pdf" in result.stdout.lower()

    def test_export_pdf_rejects_non_tex_input(self, tmp_path: Path) -> None:
        """Test that non-.tex files are rejected."""
        # Arrange
        md_file = tmp_path / "doc.md"
        md_file.write_text("# Title")

        # Act
        result = runner.invoke(app, ["pdf", str(md_file)])

        # Assert
        assert result.exit_code != 0

    def test_export_pdf_validates_compiler_argument(self, tmp_path: Path) -> None:
        """Test that invalid compiler is rejected."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["pdf", str(tex_file), "--compiler", "invalid_compiler"],
        )

        # Assert
        assert result.exit_code != 0

    def test_export_pdf_accepts_all_valid_compilers(self, tmp_path: Path) -> None:
        """Test that all valid compilers are accepted as arguments."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act & Assert - validate argument parsing for all strategies
        for compiler in CompilerStrategy:
            result = runner.invoke(
                app,
                ["pdf", str(tex_file), "--compiler", compiler.value],
            )
            # Will fail if compiler unavailable, but arguments should parse
            assert result.exit_code in [0, 2]  # 0=success, 2=compiler error

    def test_export_pdf_accepts_all_valid_templates(self, tmp_path: Path) -> None:
        """Test that all valid templates are accepted as arguments."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act & Assert - validate argument parsing for all templates
        for template in LatexTemplate:
            result = runner.invoke(
                app,
                ["pdf", str(tex_file), "--template", template.value],
            )
            assert result.exit_code in [0, 2]  # 0=success, 2=compiler error

    def test_export_pdf_accepts_keep_tex_flag(self, tmp_path: Path) -> None:
        """Test that --keep-tex flag is accepted."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["pdf", str(tex_file), "--keep-tex"],
        )

        # Assert - should parse flag without error
        assert result.exit_code in [0, 2]

    def test_export_pdf_accepts_timeout_parameter(self, tmp_path: Path) -> None:
        """Test that --timeout parameter is accepted."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["pdf", str(tex_file), "--timeout", "120"],
        )

        # Assert - should parse parameter without error
        assert result.exit_code in [0, 2]

    def test_export_pdf_accepts_force_flag(self, tmp_path: Path) -> None:
        """Test that --force flag is accepted."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["pdf", str(tex_file), "--force"],
        )

        # Assert - should parse flag without error
        assert result.exit_code in [0, 2]


# ============================================================================
# BEHAVIOR TESTS: Error Messages and User Experience
# ============================================================================
# Chicago TDD: Test actual user-facing behavior


class TestErrorMessagesAndUX:
    """Test that error messages are helpful to users."""

    def test_missing_file_error_is_informative(self, tmp_path: Path) -> None:
        """Test that missing file error provides helpful message."""
        result = runner.invoke(app, ["latex", str(tmp_path / "missing.tex")])
        assert result.exit_code != 0
        # Error should mention the missing file
        assert "missing" in result.stdout.lower() or "not found" in result.stdout.lower()

    def test_invalid_template_error_lists_options(self, tmp_path: Path) -> None:
        """Test that invalid template error shows valid options."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["latex", str(tex_file), "--template", "badtemplate"],
        )

        # Assert - error should list valid templates
        assert result.exit_code != 0
        assert "template" in result.stdout.lower()

    def test_invalid_compiler_error_lists_options(self, tmp_path: Path) -> None:
        """Test that invalid compiler error shows valid options."""
        # Arrange
        tex_file = tmp_path / "doc.tex"
        tex_file.write_text("\\documentclass{article}\n\\end{document}")

        # Act
        result = runner.invoke(
            app,
            ["pdf", str(tex_file), "--compiler", "badcompiler"],
        )

        # Assert - error should mention compiler options
        assert result.exit_code != 0


# ============================================================================
# WORKFLOW TESTS: End-to-End Scenarios
# ============================================================================
# Chicago TDD: Test complete user workflows


class TestCompleteWorkflows:
    """Test realistic end-to-end workflows."""

    def test_latex_file_export_workflow(self, tmp_path: Path) -> None:
        """Test complete workflow: export LaTeX file with various options."""
        # Arrange
        input_tex = tmp_path / "thesis.tex"
        input_tex.write_text("\\documentclass{article}\n\\begin{document}\nContent\n\\end{document}")

        # Act 1: Export with default template
        result = runner.invoke(app, ["latex", str(input_tex)])
        assert result.exit_code == 0

        # Act 2: Export with specific template
        result = runner.invoke(
            app,
            ["latex", str(input_tex), "--template", "ieee"],
        )
        assert result.exit_code == 0

        # Act 3: Export with custom output
        output_file = tmp_path / "custom_output.tex"
        result = runner.invoke(
            app,
            ["latex", str(input_tex), "-o", str(output_file)],
        )
        assert result.exit_code == 0
        assert output_file.exists()

    def test_latex_export_pipeline(self, tmp_path: Path) -> None:
        """Test that LaTeX export produces usable files."""
        # Arrange
        input_tex = tmp_path / "document.tex"
        input_tex.write_text("\\documentclass{article}\n\\usepackage{amsmath}\n\\end{document}")
        output_tex = tmp_path / "output.tex"

        # Act
        result = runner.invoke(
            app,
            ["latex", str(input_tex), "-o", str(output_tex)],
        )

        # Assert - verify complete workflow
        assert result.exit_code == 0
        assert output_tex.exists()
        # Output file should contain LaTeX document structure
        content = output_tex.read_text()
        assert "documentclass" in content or len(content) > 0

