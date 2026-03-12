"""Chicago-style TDD tests for models and cli_errors modules.

Tests real behavior with real collaborators -- no mocks.
Verifies instantiation, defaults, field types, enum values,
inheritance, and format_message() output.
"""

from pathlib import Path

import pytest

from dtr_cli.cli_errors import (
    CLIError,
    ConversionError,
    DirectoryExpectedError,
    EmptyDirectoryError,
    FileExpectedError,
    FileNotFoundError_,
    InvalidArgumentError,
    InvalidFormatError,
    InvalidPathError,
    LatexCompilationError,
    LatexTemplateMissingError,
    MavenBuildFailedError,
    MavenCentralNotFoundError,
    MavenNotFoundError,
    NoLatexCompilerError,
    PermissionDeniedError,
    PomNotFoundError,
    PublishValidationError,
)
from dtr_cli.model import (
    CompilerStrategy,
    ConversionConfig,
    ConversionResult,
    ExportInfo,
    LatexTemplate,
    ManageConfig,
    ManageResult,
    PublishCheckConfig,
    PublishConfig,
    PublishDeployConfig,
    PublishReleaseConfig,
    PublishResult,
    PublishStatusConfig,
    ReportConfig,
    ReportResult,
    ValidationResult,
)


# ---------------------------------------------------------------------------
# LatexTemplate enum
# ---------------------------------------------------------------------------

class TestLatexTemplate:
    """Tests for the LatexTemplate str enum."""

    def test_has_all_five_members(self):
        assert len(LatexTemplate) == 5

    @pytest.mark.parametrize(
        "member, value",
        [
            (LatexTemplate.ARXIV, "arxiv"),
            (LatexTemplate.PATENT, "patent"),
            (LatexTemplate.IEEE, "ieee"),
            (LatexTemplate.ACM, "acm"),
            (LatexTemplate.NATURE, "nature"),
        ],
    )
    def test_enum_values(self, member, value):
        assert member.value == value

    def test_is_str_subclass(self):
        """str enum members are usable as plain strings."""
        assert isinstance(LatexTemplate.ARXIV, str)
        assert LatexTemplate.IEEE == "ieee"

    def test_lookup_by_value(self):
        assert LatexTemplate("arxiv") is LatexTemplate.ARXIV

    def test_invalid_value_raises(self):
        with pytest.raises(ValueError):
            LatexTemplate("nonexistent")


# ---------------------------------------------------------------------------
# CompilerStrategy enum
# ---------------------------------------------------------------------------

class TestCompilerStrategy:
    """Tests for the CompilerStrategy str enum."""

    def test_has_all_five_members(self):
        assert len(CompilerStrategy) == 5

    @pytest.mark.parametrize(
        "member, value",
        [
            (CompilerStrategy.AUTO, "auto"),
            (CompilerStrategy.LATEXMK, "latexmk"),
            (CompilerStrategy.PDFLATEX, "pdflatex"),
            (CompilerStrategy.XELATEX, "xelatex"),
            (CompilerStrategy.PANDOC, "pandoc"),
        ],
    )
    def test_enum_values(self, member, value):
        assert member.value == value

    def test_is_str_subclass(self):
        assert isinstance(CompilerStrategy.AUTO, str)
        assert CompilerStrategy.PANDOC == "pandoc"

    def test_lookup_by_value(self):
        assert CompilerStrategy("latexmk") is CompilerStrategy.LATEXMK


# ---------------------------------------------------------------------------
# ConversionConfig dataclass
# ---------------------------------------------------------------------------

class TestConversionConfig:
    def test_required_args_only(self):
        cfg = ConversionConfig(input_path=Path("/in"), output_path=Path("/out"))
        assert cfg.input_path == Path("/in")
        assert cfg.output_path == Path("/out")

    def test_defaults(self):
        cfg = ConversionConfig(input_path=Path("/in"), output_path=Path("/out"))
        assert cfg.recursive is False
        assert cfg.force is False
        assert cfg.pretty is True
        assert cfg.template is None

    def test_override_defaults(self):
        cfg = ConversionConfig(
            input_path=Path("/in"),
            output_path=Path("/out"),
            recursive=True,
            force=True,
            pretty=False,
            template="custom",
        )
        assert cfg.recursive is True
        assert cfg.force is True
        assert cfg.pretty is False
        assert cfg.template == "custom"


# ---------------------------------------------------------------------------
# ConversionResult dataclass
# ---------------------------------------------------------------------------

class TestConversionResult:
    def test_required_args_only(self):
        r = ConversionResult(files_processed=10)
        assert r.files_processed == 10

    def test_defaults(self):
        r = ConversionResult(files_processed=0)
        assert r.files_failed == 0
        assert r.warnings == []
        assert r.errors == []

    def test_mutable_defaults_are_independent(self):
        """Each instance gets its own list (field default_factory)."""
        r1 = ConversionResult(files_processed=1)
        r2 = ConversionResult(files_processed=2)
        r1.warnings.append("w")
        assert r2.warnings == []

    def test_with_all_fields(self):
        r = ConversionResult(
            files_processed=5,
            files_failed=2,
            warnings=["w1"],
            errors=["e1", "e2"],
        )
        assert r.files_failed == 2
        assert len(r.warnings) == 1
        assert len(r.errors) == 2


# ---------------------------------------------------------------------------
# ReportConfig dataclass
# ---------------------------------------------------------------------------

class TestReportConfig:
    def test_required_args_only(self):
        cfg = ReportConfig(export_path=Path("/exp"), output_path=Path("/out"))
        assert cfg.export_path == Path("/exp")
        assert cfg.output_path == Path("/out")

    def test_defaults(self):
        cfg = ReportConfig(export_path=Path("/e"), output_path=Path("/o"))
        assert cfg.format == "markdown"
        assert cfg.report_type == "summary"
        assert cfg.since is None


# ---------------------------------------------------------------------------
# ReportResult dataclass
# ---------------------------------------------------------------------------

class TestReportResult:
    def test_required_args_only(self):
        r = ReportResult(output_file=Path("/report.md"))
        assert r.output_file == Path("/report.md")

    def test_defaults(self):
        r = ReportResult(output_file=Path("/r"))
        assert r.stats == {}
        assert r.warnings == []

    def test_mutable_defaults_independent(self):
        r1 = ReportResult(output_file=Path("/a"))
        r2 = ReportResult(output_file=Path("/b"))
        r1.stats["k"] = "v"
        assert r2.stats == {}


# ---------------------------------------------------------------------------
# ExportInfo dataclass
# ---------------------------------------------------------------------------

class TestExportInfo:
    def test_all_required_fields(self):
        info = ExportInfo(name="doc.md", path=Path("/doc.md"), size=1024, modified="2026-03-12")
        assert info.name == "doc.md"
        assert info.path == Path("/doc.md")
        assert info.size == 1024
        assert info.modified == "2026-03-12"

    def test_no_optional_fields(self):
        """ExportInfo has no defaults -- all fields are required."""
        with pytest.raises(TypeError):
            ExportInfo(name="x")  # type: ignore[call-arg]


# ---------------------------------------------------------------------------
# ManageConfig dataclass
# ---------------------------------------------------------------------------

class TestManageConfig:
    def test_required_args_only(self):
        cfg = ManageConfig(export_path=Path("/exp"))
        assert cfg.export_path == Path("/exp")

    def test_defaults(self):
        cfg = ManageConfig(export_path=Path("/exp"))
        assert cfg.detailed is False
        assert cfg.archive_path is None
        assert cfg.archive_format == "tar.gz"
        assert cfg.keep_latest == 5
        assert cfg.dry_run is True


# ---------------------------------------------------------------------------
# ManageResult dataclass
# ---------------------------------------------------------------------------

class TestManageResult:
    def test_no_required_args(self):
        r = ManageResult()
        assert r.removed_files == []
        assert r.stats == {}
        assert r.issues == []

    def test_mutable_defaults_independent(self):
        r1 = ManageResult()
        r2 = ManageResult()
        r1.removed_files.append("f")
        assert r2.removed_files == []


# ---------------------------------------------------------------------------
# PublishConfig dataclass
# ---------------------------------------------------------------------------

class TestPublishConfig:
    def test_required_args_only(self):
        cfg = PublishConfig(export_path=Path("/exp"), platform="github")
        assert cfg.export_path == Path("/exp")
        assert cfg.platform == "github"

    def test_defaults(self):
        cfg = PublishConfig(export_path=Path("/e"), platform="s3")
        assert cfg.target is None
        assert cfg.branch is None
        assert cfg.token is None
        assert cfg.repo is None
        assert cfg.bucket is None
        assert cfg.prefix == "docs/"
        assert cfg.region == "us-east-1"
        assert cfg.project is None
        assert cfg.target_path is None
        assert cfg.public is False


# ---------------------------------------------------------------------------
# PublishResult dataclass
# ---------------------------------------------------------------------------

class TestPublishResult:
    def test_required_args_only(self):
        r = PublishResult(platform="github")
        assert r.platform == "github"

    def test_defaults(self):
        r = PublishResult(platform="s3")
        assert r.url is None
        assert r.files_count == 0
        assert r.status == "success"
        assert r.warnings == []


# ---------------------------------------------------------------------------
# ValidationResult dataclass
# ---------------------------------------------------------------------------

class TestValidationResult:
    def test_required_args_only(self):
        v = ValidationResult(name="check", passed=True)
        assert v.name == "check"
        assert v.passed is True

    def test_defaults(self):
        v = ValidationResult(name="c", passed=False)
        assert v.message == ""
        assert v.hint == ""

    def test_with_message_and_hint(self):
        v = ValidationResult(name="gpg", passed=False, message="missing", hint="install gpg")
        assert v.message == "missing"
        assert v.hint == "install gpg"


# ---------------------------------------------------------------------------
# PublishCheckConfig dataclass
# ---------------------------------------------------------------------------

class TestPublishCheckConfig:
    def test_required_args_only(self):
        cfg = PublishCheckConfig(project_dir=Path("/proj"))
        assert cfg.project_dir == Path("/proj")

    def test_defaults(self):
        cfg = PublishCheckConfig(project_dir=Path("/p"))
        assert cfg.ossrh_user is None
        assert cfg.ossrh_token is None
        assert cfg.gpg_key is None
        assert cfg.verbose is False


# ---------------------------------------------------------------------------
# PublishDeployConfig dataclass
# ---------------------------------------------------------------------------

class TestPublishDeployConfig:
    def test_required_args_only(self):
        cfg = PublishDeployConfig(project_dir=Path("/proj"))
        assert cfg.project_dir == Path("/proj")

    def test_defaults(self):
        cfg = PublishDeployConfig(project_dir=Path("/p"))
        assert cfg.ossrh_user is None
        assert cfg.ossrh_token is None
        assert cfg.gpg_key is None
        assert cfg.gpg_passphrase is None
        assert cfg.skip_tests is False
        assert cfg.dry_run is False
        assert cfg.auto_release is False


# ---------------------------------------------------------------------------
# PublishReleaseConfig dataclass
# ---------------------------------------------------------------------------

class TestPublishReleaseConfig:
    def test_no_required_args(self):
        cfg = PublishReleaseConfig()
        assert cfg.ossrh_user is None
        assert cfg.ossrh_token is None
        assert cfg.wait is False
        assert cfg.timeout == 1800


# ---------------------------------------------------------------------------
# PublishStatusConfig dataclass
# ---------------------------------------------------------------------------

class TestPublishStatusConfig:
    def test_no_required_args(self):
        cfg = PublishStatusConfig()
        assert cfg.version is None
        assert cfg.wait is False
        assert cfg.timeout == 1800


# ===========================================================================
# CLI ERRORS
# ===========================================================================


class TestCLIError:
    """Base CLIError tests."""

    def test_is_exception_subclass(self):
        assert issubclass(CLIError, Exception)

    def test_message_only(self):
        err = CLIError("something broke")
        assert err.message == "something broke"
        assert err.hint is None

    def test_format_message_without_hint(self):
        msg = CLIError("bad").format_message()
        assert "bad" in msg
        # Should not contain hint marker when no hint
        assert "\n" not in msg

    def test_format_message_with_hint(self):
        msg = CLIError("bad", hint="try this").format_message()
        assert "bad" in msg
        assert "try this" in msg

    def test_str_contains_message(self):
        err = CLIError("boom")
        assert str(err) == "boom"

    def test_can_be_raised_and_caught(self):
        with pytest.raises(CLIError):
            raise CLIError("test")


class TestFileNotFoundError_:
    def test_is_cli_error_subclass(self):
        assert issubclass(FileNotFoundError_, CLIError)

    def test_default_file_type(self):
        err = FileNotFoundError_("/missing.txt")
        assert "File not found" in err.message
        assert "/missing.txt" in err.message

    def test_custom_file_type(self):
        err = FileNotFoundError_("/dir", file_type="directory")
        assert "Directory not found" in err.message

    def test_has_hint(self):
        err = FileNotFoundError_("/x")
        assert err.hint is not None
        assert "path" in err.hint.lower()


class TestInvalidPathError:
    def test_message_contains_path_and_reason(self):
        err = InvalidPathError("/bad/path", "contains spaces")
        assert "/bad/path" in err.message
        assert "contains spaces" in err.message

    def test_no_hint(self):
        err = InvalidPathError("/p", "r")
        assert err.hint is None


class TestDirectoryExpectedError:
    def test_message_and_hint(self):
        err = DirectoryExpectedError("/some/file.txt")
        assert "/some/file.txt" in err.message
        assert err.hint is not None

    def test_is_cli_error(self):
        assert issubclass(DirectoryExpectedError, CLIError)


class TestFileExpectedError:
    def test_message_and_hint(self):
        err = FileExpectedError("/some/dir")
        assert "/some/dir" in err.message
        assert err.hint is not None


class TestInvalidFormatError:
    def test_message_and_hint_list_valid_formats(self):
        err = InvalidFormatError("xml", ["json", "yaml", "toml"])
        assert "xml" in err.message
        msg = err.format_message()
        assert "json" in msg
        assert "yaml" in msg
        assert "toml" in msg


class TestPermissionDeniedError:
    def test_default_operation(self):
        err = PermissionDeniedError("/secret")
        assert "access" in err.message
        assert "/secret" in err.message

    def test_custom_operation(self):
        err = PermissionDeniedError("/log", "write")
        assert "write" in err.message


class TestInvalidArgumentError:
    def test_message_and_hint(self):
        err = InvalidArgumentError("--count", "-5", "positive integer")
        assert "--count" in err.message
        assert "-5" in err.message
        assert "positive integer" in err.hint


class TestEmptyDirectoryError:
    def test_default_what(self):
        err = EmptyDirectoryError("/empty")
        assert "exports" in err.message
        assert "/empty" in err.message

    def test_custom_what(self):
        err = EmptyDirectoryError("/dir", "markdown files")
        assert "markdown files" in err.message


class TestConversionError:
    def test_message_and_hint(self):
        err = ConversionError("/doc.md", "pdf", "missing compiler")
        assert "/doc.md" in err.message
        assert "pdf" in err.message
        assert "missing compiler" in err.hint


class TestMavenBuildFailedError:
    def test_message(self):
        err = MavenBuildFailedError(1, "tests failed")
        assert "exit code 1" in err.message
        assert "tests failed" in err.hint

    def test_default_reason(self):
        err = MavenBuildFailedError(2)
        assert "Unknown" in err.hint


class TestMavenNotFoundError:
    def test_no_args(self):
        err = MavenNotFoundError()
        assert "not found" in err.message.lower()
        assert err.hint is not None

    def test_is_cli_error(self):
        assert issubclass(MavenNotFoundError, CLIError)


class TestPomNotFoundError:
    def test_message(self):
        err = PomNotFoundError("/project")
        assert "pom.xml" in err.message
        assert "/project" in err.message
        assert err.hint is not None


class TestLatexCompilationError:
    def test_message(self):
        err = LatexCompilationError("pdflatex", 1)
        assert "pdflatex" in err.message
        assert "exit code 1" in err.message
        assert err.hint is not None

    def test_details_attribute(self):
        err = LatexCompilationError("xelatex", 2, details="missing package")
        assert err.details == "missing package"

    def test_details_default_none(self):
        err = LatexCompilationError("latexmk", 1)
        assert err.details is None


class TestNoLatexCompilerError:
    def test_no_args(self):
        err = NoLatexCompilerError()
        assert "no latex compiler" in err.message.lower()
        assert err.hint is not None


class TestLatexTemplateMissingError:
    def test_message_and_hint(self):
        err = LatexTemplateMissingError("fancy", ["arxiv", "ieee", "acm"])
        assert "fancy" in err.message
        msg = err.format_message()
        assert "arxiv" in msg
        assert "ieee" in msg
        assert "acm" in msg


class TestPublishValidationError:
    def test_message_lists_failures(self):
        failures = ["no GPG key", "missing credentials"]
        err = PublishValidationError(failures)
        assert "no GPG key" in err.message
        assert "missing credentials" in err.message
        assert err.hint is not None

    def test_is_cli_error(self):
        assert issubclass(PublishValidationError, CLIError)


class TestMavenCentralNotFoundError:
    def test_with_version(self):
        err = MavenCentralNotFoundError("com.example:lib", "1.0.0")
        assert "com.example:lib" in err.message
        assert "1.0.0" in err.message

    def test_without_version(self):
        err = MavenCentralNotFoundError("com.example:lib")
        assert "com.example:lib" in err.message

    def test_hint_mentions_syncing(self):
        err = MavenCentralNotFoundError("x")
        assert "sync" in err.hint.lower() or "wait" in err.hint.lower()


# ---------------------------------------------------------------------------
# Cross-cutting: every error is an Exception and a CLIError
# ---------------------------------------------------------------------------

ALL_ERROR_CLASSES = [
    CLIError,
    FileNotFoundError_,
    InvalidPathError,
    DirectoryExpectedError,
    FileExpectedError,
    InvalidFormatError,
    PermissionDeniedError,
    InvalidArgumentError,
    EmptyDirectoryError,
    ConversionError,
    MavenBuildFailedError,
    MavenNotFoundError,
    PomNotFoundError,
    LatexCompilationError,
    NoLatexCompilerError,
    LatexTemplateMissingError,
    PublishValidationError,
    MavenCentralNotFoundError,
]


@pytest.mark.parametrize("cls", ALL_ERROR_CLASSES, ids=lambda c: c.__name__)
def test_all_errors_are_exception_and_cli_error(cls):
    assert issubclass(cls, Exception)
    assert issubclass(cls, CLIError)
