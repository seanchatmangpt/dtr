"""Chicago TDD: verify real error behavior, not mocks.

Joe Armstrong principle: every error has an identity, carries context,
and produces both human and machine-readable output.
"""
import inspect
import json

import pytest

from dtr_cli import cli_errors
from dtr_cli.cli_errors import (
    CLIError,
    ConfigurationError,
    FileNotFoundError_,
    InvalidPathError,
    DirectoryExpectedError,
    FileExpectedError,
    PomNotFoundError,
    MavenNotFoundError,
    MavenBuildFailedError,
    LatexCompilationError,
    NoLatexCompilerError,
    LatexTemplateMissingError,
    InvalidCredentialsError,
    InvalidPOMError,
    PublishValidationError,
    MavenCentralNotFoundError,
    InvalidFormatError,
    PermissionDeniedError,
    TimeoutError_,
    DiskSpaceError,
    ConcurrencyError,
    InvalidArgumentError,
    EmptyDirectoryError,
    ConversionError,
    OutputError,
    ArchiveError,
    MissingGPGKeyError,
)


# ---------------------------------------------------------------------------
# Identity — every error has a unique code
# ---------------------------------------------------------------------------


def test_every_error_has_unique_code():
    """Joe Armstrong: every error has an identity.

    Each concrete subclass must set a unique DTR-XXX error_code in its
    __init__ (not just inherit DTR-000 from the base). We instantiate
    every subclass with no positional arguments to verify this.
    """
    codes: dict[str, str] = {}
    for name, obj in inspect.getmembers(cli_errors, inspect.isclass):
        if not issubclass(obj, CLIError) or obj is CLIError:
            continue
        # Instantiate with all-defaults to trigger __init__ and capture the
        # error_code that the constructor hard-codes.
        try:
            instance = obj()
            code = instance.error_code
        except Exception as exc:
            # If the constructor requires mandatory positional args we can't
            # avoid, fall back to a __new__-only read plus a direct search in
            # the constructor's co_consts for a "DTR-" literal.
            code = None
            init_fn = getattr(obj, "__init__", None)
            if init_fn is not None:
                consts = getattr(getattr(init_fn, "__code__", None), "co_consts", ())
                dtr_consts = [c for c in consts if isinstance(c, str) and c.startswith("DTR-")]
                if dtr_consts:
                    code = dtr_consts[0]
        assert code is not None and code != "DTR-000", (
            f"{name} must have a unique error_code (not None and not 'DTR-000')"
        )
        assert code not in codes, (
            f"Duplicate error_code {code}: found in both {name} and {codes[code]}"
        )
        codes[code] = name


# ---------------------------------------------------------------------------
# Base class — format_message and format_json
# ---------------------------------------------------------------------------


def test_cli_error_format_message_contains_code():
    err = CLIError(message="test error", error_code="DTR-999")
    msg = err.format_message()
    assert "DTR-999" in msg
    assert "test error" in msg
    assert "❌" in msg


def test_cli_error_format_message_contains_hint_when_present():
    err = CLIError(message="test error", hint="do this instead", error_code="DTR-999")
    msg = err.format_message()
    assert "💡" in msg
    assert "do this instead" in msg


def test_cli_error_format_message_no_hint_when_empty():
    err = CLIError(message="bare error", error_code="DTR-999")
    msg = err.format_message()
    assert "💡" not in msg


def test_cli_error_str_equals_format_message():
    err = CLIError(message="str test", error_code="DTR-999")
    assert str(err) == err.format_message()


def test_cli_error_format_json_is_valid_json():
    err = CLIError(message="json test", hint="a hint", error_code="DTR-999", context={"k": "v"})
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-999"
    assert result["message"] == "json test"
    assert result["hint"] == "a hint"
    assert result["context"] == {"k": "v"}


def test_cli_error_is_exception():
    err = CLIError(message="must be raisable", error_code="DTR-999")
    with pytest.raises(CLIError):
        raise err


# ---------------------------------------------------------------------------
# File-system errors  (DTR-1xx)
# ---------------------------------------------------------------------------


def test_file_not_found_error_code_and_context():
    err = FileNotFoundError_(path="/some/file.txt", file_type="file")
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-101"
    assert result["context"]["path"] == "/some/file.txt"
    assert result["context"]["file_type"] == "file"


def test_invalid_path_error_code():
    err = InvalidPathError(path="/bad/path", reason="contains null byte")
    assert err.error_code == "DTR-102"
    assert "DTR-102" in err.format_message()


def test_directory_expected_error_code():
    err = DirectoryExpectedError(path="/some/file.txt")
    assert err.error_code == "DTR-103"
    assert "/some/file.txt" in err.message


def test_file_expected_error_code():
    err = FileExpectedError(path="/some/dir")
    assert err.error_code == "DTR-104"
    assert "/some/dir" in err.message


# ---------------------------------------------------------------------------
# Maven / build errors  (DTR-2xx)
# ---------------------------------------------------------------------------


def test_pom_not_found_error_has_hint():
    err = PomNotFoundError()
    msg = err.format_message()
    assert "DTR-201" in msg
    assert "❌" in msg
    assert "💡" in msg


def test_pom_not_found_error_with_path():
    err = PomNotFoundError(path="/workspace/myproject")
    assert "DTR-201" in err.format_message()
    assert err.context["path"] == "/workspace/myproject"


def test_maven_not_found_error_code():
    err = MavenNotFoundError()
    assert err.error_code == "DTR-202"
    assert "Maven" in err.message


def test_maven_build_failed_error_format_json():
    err = MavenBuildFailedError(returncode=1, goals="clean verify")
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-203"
    assert "Maven" in result["message"]
    assert result["context"]["returncode"] == 1
    assert result["context"]["goals"] == "clean verify"


def test_maven_build_failed_error_no_goals():
    err = MavenBuildFailedError(returncode=2)
    assert "exit 2" in err.message
    assert err.context["returncode"] == 2


# ---------------------------------------------------------------------------
# LaTeX errors  (DTR-3xx)
# ---------------------------------------------------------------------------


def test_latex_compilation_error_code_and_context():
    err = LatexCompilationError(compiler="pdflatex", exit_code=1, details="Undefined control sequence")
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-301"
    assert result["context"]["compiler"] == "pdflatex"
    assert result["context"]["exit_code"] == 1
    assert "Undefined" in result["context"]["details"]


def test_no_latex_compiler_error_code():
    err = NoLatexCompilerError()
    assert err.error_code == "DTR-302"
    assert "💡" in err.format_message()


def test_latex_template_missing_error_code_and_hint():
    err = LatexTemplateMissingError(template_name="fancy", valid_templates=["plain", "ieee"])
    assert err.error_code == "DTR-303"
    assert "fancy" in err.message
    assert "plain" in err.hint
    assert "ieee" in err.hint


# ---------------------------------------------------------------------------
# Publishing / credentials errors  (DTR-4xx)
# ---------------------------------------------------------------------------


def test_invalid_credentials_error_code():
    err = InvalidCredentialsError(credential_type="OSSRH")
    assert err.error_code == "DTR-401"
    assert err.context["credential_type"] == "OSSRH"


def test_invalid_pom_error_with_list():
    err = InvalidPOMError(missing_elements=["<description>", "<licenses>"])
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-402"
    assert "<description>" in result["context"]["missing_elements"]
    assert "<licenses>" in result["context"]["missing_elements"]


def test_invalid_pom_error_with_string():
    err = InvalidPOMError(missing_elements="malformed XML", requirement="valid XML")
    assert err.error_code == "DTR-402"
    assert "malformed XML" in err.message


def test_publish_validation_error_lists_failures():
    err = PublishValidationError(failures=["No GPG key", "Missing description"])
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-403"
    assert "No GPG key" in result["context"]["failures"]
    assert "Missing description" in result["context"]["failures"]


def test_maven_central_not_found_error_code():
    err = MavenCentralNotFoundError(artifact="com.example:mylib", version="1.0.0")
    assert err.error_code == "DTR-404"
    assert "com.example:mylib" in err.message
    assert err.context["version"] == "1.0.0"


# ---------------------------------------------------------------------------
# Format / configuration errors  (DTR-5xx)
# ---------------------------------------------------------------------------


def test_invalid_format_error_code_and_hint():
    err = InvalidFormatError(format_name="docx", valid_formats=["md", "html", "json"])
    assert err.error_code == "DTR-501"
    assert "docx" in err.message
    assert "md" in err.hint
    assert "html" in err.hint


def test_configuration_error_context():
    err = ConfigurationError(
        message="bad timeout",
        context={"field": "timeout_seconds", "value": -1},
    )
    result = json.loads(err.format_json())
    assert result["error"] == "DTR-502"
    assert result["context"]["field"] == "timeout_seconds"
    assert result["context"]["value"] == -1


def test_configuration_error_legacy_style():
    err = ConfigurationError(config_name="proxy_url", reason="must be a valid URL")
    assert err.error_code == "DTR-502"
    assert "proxy_url" in err.message
    assert err.context["config_name"] == "proxy_url"


# ---------------------------------------------------------------------------
# Runtime / resource errors  (DTR-6xx)
# ---------------------------------------------------------------------------


def test_permission_denied_error_code():
    err = PermissionDeniedError(path="/etc/secret", operation="write")
    assert err.error_code == "DTR-601"
    assert "write" in err.message
    assert "/etc/secret" in err.message


def test_timeout_error_code_and_context():
    err = TimeoutError_(operation="maven build", timeout_seconds=300)
    assert err.error_code == "DTR-602"
    assert err.context["timeout_seconds"] == 300
    assert "maven build" in err.message


def test_disk_space_error_code():
    err = DiskSpaceError(path="/tmp", needed_mb=500)
    assert err.error_code == "DTR-603"
    assert err.context["needed_mb"] == 500


def test_concurrency_error_code():
    err = ConcurrencyError(resource="build.lock", action="acquire")
    assert err.error_code == "DTR-604"
    assert "build.lock" in err.message


# ---------------------------------------------------------------------------
# Additional error classes
# ---------------------------------------------------------------------------


def test_invalid_argument_error_code():
    err = InvalidArgumentError(arg_name="--timeout", value="-1", reason="must be positive integer")
    assert err.error_code == "DTR-503"
    assert "--timeout" in err.message
    assert "-1" in err.message


def test_empty_directory_error_code():
    err = EmptyDirectoryError(path="/exports", what="HTML exports")
    assert err.error_code == "DTR-105"
    assert "/exports" in err.message


def test_conversion_error_code():
    err = ConversionError(input_path="/docs/report.html", output_format="pdf", reason="missing pandoc")
    assert err.error_code == "DTR-504"
    assert "report.html" in err.message


def test_output_error_code():
    err = OutputError(output_path="/readonly/dir/out.md", reason="permission denied")
    assert err.error_code == "DTR-605"
    assert "permission denied" in err.hint


def test_archive_error_code():
    err = ArchiveError(archive_path="/tmp/docs.tar.gz", operation="extract", reason="corrupt file")
    assert err.error_code == "DTR-606"
    assert "extract" in err.message


def test_missing_gpg_key_error_with_key_id():
    err = MissingGPGKeyError(key_id="DEADBEEF")
    assert err.error_code == "DTR-405"
    assert "DEADBEEF" in err.message


def test_missing_gpg_key_error_no_key_id():
    err = MissingGPGKeyError()
    assert err.error_code == "DTR-405"
    assert "No GPG key" in err.message
