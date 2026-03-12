"""Custom exceptions for CLI with user-friendly error messages.

Provides exceptions that display helpful messages to users instead of
Python stack traces. Each error has a unique identity (error code) and
structured context — following Joe Armstrong's principle that every
error must carry enough information to act on it.
"""

import json
from dataclasses import dataclass, field
from typing import Any


@dataclass
class CLIError(Exception):
    """Base class for all DTR CLI errors.

    Every error carries:
    - A unique error_code (DTR-XXX) for programmatic identification.
    - A human-readable message describing what went wrong.
    - An optional hint explaining how to fix it.
    - A structured context dict for machine-readable metadata.
    """

    message: str
    hint: str = ""
    error_code: str = "DTR-000"
    context: dict[str, Any] = field(default_factory=dict)

    def __post_init__(self) -> None:
        super().__init__(self.message)

    def format_message(self) -> str:
        """Format error message for display to user."""
        parts = [f"❌ [{self.error_code}] {self.message}"]
        if self.hint:
            parts.append(f"💡 {self.hint}")
        return "\n".join(parts)

    def format_json(self) -> str:
        """Format error as JSON for --json mode / machine consumption."""
        return json.dumps(
            {
                "error": self.error_code,
                "message": self.message,
                "hint": self.hint,
                "context": self.context,
            },
            indent=2,
        )

    def __str__(self) -> str:
        return self.format_message()


# ---------------------------------------------------------------------------
# File-system errors  (DTR-1xx)
# ---------------------------------------------------------------------------


class FileNotFoundError_(CLIError):
    """Raised when a required file or directory is not found."""

    def __init__(self, path: str = "", file_type: str = "file", **kwargs: Any):
        super().__init__(
            message=f"{file_type.capitalize()} not found: {path}",
            hint="Check that the path exists and is accessible",
            error_code="DTR-101",
            context={"path": path, "file_type": file_type},
        )


class InvalidPathError(CLIError):
    """Raised when a path is invalid or inaccessible."""

    def __init__(self, path: str = "", reason: str = "", **kwargs: Any):
        super().__init__(
            message=f"Invalid path: {path}\nReason: {reason}",
            hint="Ensure the path string is well-formed and the filesystem location exists",
            error_code="DTR-102",
            context={"path": path, "reason": reason},
        )


class DirectoryExpectedError(CLIError):
    """Raised when a file is provided instead of a directory."""

    def __init__(self, path: str = "", **kwargs: Any):
        super().__init__(
            message=f"Expected a directory, got file: {path}",
            hint="Check that you're pointing to a directory, not a file",
            error_code="DTR-103",
            context={"path": path},
        )


class FileExpectedError(CLIError):
    """Raised when a directory is provided instead of a file."""

    def __init__(self, path: str = "", **kwargs: Any):
        super().__init__(
            message=f"Expected a file, got directory: {path}",
            hint="Check that you're pointing to a file, not a directory",
            error_code="DTR-104",
            context={"path": path},
        )


# ---------------------------------------------------------------------------
# Maven / build errors  (DTR-2xx)
# ---------------------------------------------------------------------------


class PomNotFoundError(CLIError):
    """Raised when pom.xml is not found in the expected location."""

    def __init__(self, path: str = "", **kwargs: Any):
        super().__init__(
            message=f"pom.xml not found in {path}" if path else "pom.xml not found",
            hint="Make sure you're in the root directory of a Maven project",
            error_code="DTR-201",
            context={"path": path},
        )


class MavenNotFoundError(CLIError):
    """Raised when Maven / mvnd is not found in PATH."""

    def __init__(self, **kwargs: Any):
        super().__init__(
            message="Maven (mvnd or mvn) not found in PATH",
            hint=(
                "Install Maven from https://maven.apache.org/ or "
                "mvnd from https://maven.apache.org/mvnd/"
            ),
            error_code="DTR-202",
            context={},
        )


class MavenBuildFailedError(CLIError):
    """Raised when a Maven build exits with a non-zero status."""

    def __init__(self, returncode: int = 1, goals: str = "", **kwargs: Any):
        super().__init__(
            message=(
                f"Maven build failed (exit {returncode})"
                + (f" running '{goals}'" if goals else "")
            ),
            hint="Run with --verbose to see full Maven output. Check pom.xml for syntax errors.",
            error_code="DTR-203",
            context={"returncode": returncode, "goals": goals},
        )


# ---------------------------------------------------------------------------
# LaTeX errors  (DTR-3xx)
# ---------------------------------------------------------------------------


class LatexCompilationError(CLIError):
    """Raised when LaTeX compilation fails."""

    def __init__(
        self,
        compiler: str = "",
        exit_code: int = 1,
        details: str | None = None,
        **kwargs: Any,
    ):
        super().__init__(
            message=f"LaTeX compilation failed with {compiler} (exit code {exit_code})",
            hint="Check your LaTeX syntax or ensure texlive/miktex is installed",
            error_code="DTR-301",
            context={"compiler": compiler, "exit_code": exit_code, "details": details or ""},
        )
        self.details = details


class NoLatexCompilerError(CLIError):
    """Raised when no LaTeX compiler is found in PATH."""

    def __init__(self, **kwargs: Any):
        super().__init__(
            message="No LaTeX compiler found in PATH",
            hint=(
                "Install texlive: apt-get install texlive-latex-base\n"
                "Or install miktex: https://miktex.org/download"
            ),
            error_code="DTR-302",
            context={},
        )


class LatexTemplateMissingError(CLIError):
    """Raised for invalid LaTeX template selection."""

    def __init__(self, template_name: str = "", valid_templates: list[str] | None = None, **kwargs: Any):
        valid = valid_templates or []
        super().__init__(
            message=f"Unknown LaTeX template: {template_name}",
            hint=f"Valid templates are: {', '.join(valid)}",
            error_code="DTR-303",
            context={"template_name": template_name, "valid_templates": valid},
        )


# ---------------------------------------------------------------------------
# Publishing / credentials errors  (DTR-4xx)
# ---------------------------------------------------------------------------


class InvalidCredentialsError(CLIError):
    """Raised when OSSRH credentials are missing or invalid."""

    def __init__(self, credential_type: str = "OSSRH", **kwargs: Any):
        super().__init__(
            message=f"{credential_type} credentials not configured",
            hint=(
                f"Configure {credential_type} credentials in ~/.m2/settings.xml\n"
                "Or set environment variables: OSSRH_USERNAME, OSSRH_PASSWORD"
            ),
            error_code="DTR-401",
            context={"credential_type": credential_type},
        )


class InvalidPOMError(CLIError):
    """Raised when pom.xml is invalid for Maven Central publishing."""

    def __init__(
        self,
        missing_elements: list[str] | str = "",
        requirement: str = "",
        **kwargs: Any,
    ):
        if isinstance(missing_elements, list):
            elements_str = ", ".join(missing_elements)
            message = f"Invalid pom.xml: Missing required elements: {elements_str}"
            hint = "Add missing elements to pom.xml\nSee PUBLISHING.md for complete pom.xml structure"
            ctx: dict[str, Any] = {"missing_elements": missing_elements}
        else:
            message = f"Invalid pom.xml: {missing_elements}"
            hint = f"Required for Maven Central: {requirement}" if requirement else ""
            ctx = {"missing_elements": missing_elements, "requirement": requirement}
        super().__init__(
            message=message,
            hint=hint,
            error_code="DTR-402",
            context=ctx,
        )


class PublishValidationError(CLIError):
    """Raised when pre-flight publish validation fails."""

    def __init__(self, failures: list[str] | None = None, **kwargs: Any):
        failures = failures or []
        failures_str = "\n".join(f"  - {f}" for f in failures)
        super().__init__(
            message=f"Pre-flight validation failed:\n{failures_str}",
            hint="Run 'dtr publish check' to validate your environment before publishing",
            error_code="DTR-403",
            context={"failures": failures},
        )


class MavenCentralNotFoundError(CLIError):
    """Raised when artifact is not found on Maven Central after publishing."""

    def __init__(self, artifact: str = "", version: str = "", **kwargs: Any):
        ver_str = f" (v{version})" if version else ""
        super().__init__(
            message=f"Artifact not found on Maven Central: {artifact}{ver_str}",
            hint="Artifact may still be syncing. Wait 15-30 minutes and try again.",
            error_code="DTR-404",
            context={"artifact": artifact, "version": version},
        )


# ---------------------------------------------------------------------------
# Format / configuration errors  (DTR-5xx)
# ---------------------------------------------------------------------------


class InvalidFormatError(CLIError):
    """Raised when an invalid format is specified."""

    def __init__(
        self,
        format_name: str = "",
        valid_formats: list[str] | None = None,
        **kwargs: Any,
    ):
        valid = valid_formats or []
        super().__init__(
            message=f"Invalid format: {format_name}",
            hint=f"Valid formats are: {', '.join(valid)}",
            error_code="DTR-501",
            context={"format_name": format_name, "valid_formats": valid},
        )


class ConfigurationError(CLIError):
    """Raised when configuration is invalid."""

    def __init__(
        self,
        message: str = "",
        config_name: str = "",
        reason: str = "",
        context: dict[str, Any] | None = None,
        **kwargs: Any,
    ):
        # Allow callers to pass either the new-style (message + context) or
        # old-style (config_name + reason) arguments for backwards compatibility.
        if not message and config_name:
            message = f"Invalid configuration: {config_name}"
        if not message:
            message = "Invalid configuration"
        hint = f"Reason: {reason}" if reason else ""
        ctx = context if context is not None else {}
        if config_name and "config_name" not in ctx:
            ctx = {"config_name": config_name, "reason": reason, **ctx}
        super().__init__(
            message=message,
            hint=hint,
            error_code="DTR-502",
            context=ctx,
        )


# ---------------------------------------------------------------------------
# Runtime / resource errors  (DTR-6xx)
# ---------------------------------------------------------------------------


class PermissionDeniedError(CLIError):
    """Raised when permission is denied for a file/directory."""

    def __init__(self, path: str = "", operation: str = "access", **kwargs: Any):
        super().__init__(
            message=f"Permission denied: cannot {operation} {path}",
            hint="Check file permissions and try again",
            error_code="DTR-601",
            context={"path": path, "operation": operation},
        )


class TimeoutError_(CLIError):
    """Raised when an operation times out."""

    def __init__(self, operation: str = "", timeout_seconds: int = 0, **kwargs: Any):
        super().__init__(
            message=f"Operation timed out: {operation}",
            hint=f"Increase timeout (currently {timeout_seconds}s) or try with smaller input",
            error_code="DTR-602",
            context={"operation": operation, "timeout_seconds": timeout_seconds},
        )


class DiskSpaceError(CLIError):
    """Raised when there is not enough disk space."""

    def __init__(self, path: str = "", needed_mb: int = 0, **kwargs: Any):
        super().__init__(
            message=f"Not enough disk space for operation in {path}",
            hint=f"Free up at least {needed_mb}MB and try again",
            error_code="DTR-603",
            context={"path": path, "needed_mb": needed_mb},
        )


class ConcurrencyError(CLIError):
    """Raised when concurrent operations interfere."""

    def __init__(self, resource: str = "", action: str = "access", **kwargs: Any):
        super().__init__(
            message=f"Cannot {action} {resource}: it's being used by another process",
            hint="Wait for the other operation to complete and try again",
            error_code="DTR-604",
            context={"resource": resource, "action": action},
        )


# ---------------------------------------------------------------------------
# Additional errors preserved from original (no code range assigned above)
# ---------------------------------------------------------------------------


class InvalidArgumentError(CLIError):
    """Raised when an argument value is invalid."""

    def __init__(self, arg_name: str = "", value: str = "", reason: str = "", **kwargs: Any):
        super().__init__(
            message=f"Invalid value for {arg_name}: {value}",
            hint=f"Expected: {reason}",
            error_code="DTR-503",
            context={"arg_name": arg_name, "value": value, "reason": reason},
        )


class EmptyDirectoryError(CLIError):
    """Raised when an empty directory is not allowed."""

    def __init__(self, path: str = "", what: str = "exports", **kwargs: Any):
        super().__init__(
            message=f"No {what} found in: {path}",
            hint="Check that the directory contains the expected files",
            error_code="DTR-105",
            context={"path": path, "what": what},
        )


class ConversionError(CLIError):
    """Raised when format conversion fails."""

    def __init__(self, input_path: str = "", output_format: str = "", reason: str = "", **kwargs: Any):
        super().__init__(
            message=f"Failed to convert {input_path} to {output_format}",
            hint=f"Reason: {reason}",
            error_code="DTR-504",
            context={"input_path": input_path, "output_format": output_format, "reason": reason},
        )


class OutputError(CLIError):
    """Raised when output cannot be written."""

    def __init__(self, output_path: str = "", reason: str = "", **kwargs: Any):
        super().__init__(
            message=f"Cannot write output to: {output_path}",
            hint=f"Reason: {reason}",
            error_code="DTR-605",
            context={"output_path": output_path, "reason": reason},
        )


class ArchiveError(CLIError):
    """Raised when archive creation/reading fails."""

    def __init__(self, archive_path: str = "", operation: str = "process", reason: str = "", **kwargs: Any):
        super().__init__(
            message=f"Failed to {operation} archive: {archive_path}",
            hint=f"Reason: {reason}",
            error_code="DTR-606",
            context={"archive_path": archive_path, "operation": operation, "reason": reason},
        )


class MissingGPGKeyError(CLIError):
    """Raised when GPG key is not found."""

    def __init__(self, key_id: str | None = None, **kwargs: Any):
        if key_id:
            message = f"GPG key not found: {key_id}"
            hint = "Import or create GPG key: gpg --import <keyfile>\nOr use: gpg --gen-key"
        else:
            message = "No GPG key found"
            hint = (
                "Set up GPG: gpg --gen-key\n"
                "Export public key: gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>"
            )
        super().__init__(
            message=message,
            hint=hint,
            error_code="DTR-405",
            context={"key_id": key_id or ""},
        )
