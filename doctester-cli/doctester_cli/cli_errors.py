"""Custom exceptions for CLI with user-friendly error messages.

Provides exceptions that display helpful messages to users instead of
Python stack traces. These follow the 80/20 principle of error handling.
"""


class CLIError(Exception):
    """Base exception for user-facing CLI errors.

    Subclasses should provide clear messages about what went wrong
    and how to fix it, without exposing Python internals.
    """

    def __init__(self, message: str, hint: str | None = None):
        """Initialize CLI error.

        Args:
            message: User-friendly error message (what went wrong)
            hint: Optional hint about how to fix the problem
        """
        self.message = message
        self.hint = hint
        super().__init__(message)

    def format_message(self) -> str:
        """Format error message for display to user."""
        msg = f"❌ {self.message}"
        if self.hint:
            msg += f"\n💡 {self.hint}"
        return msg


class FileNotFoundError_(CLIError):
    """Raised when a required file or directory is not found."""

    def __init__(self, path: str, file_type: str = "file"):
        """Initialize file not found error.

        Args:
            path: Path to the missing file/directory
            file_type: Type of file ("file", "directory", "export")
        """
        message = f"{file_type.capitalize()} not found: {path}"
        hint = f"Check that the path exists and is accessible"
        super().__init__(message, hint)


class InvalidPathError(CLIError):
    """Raised when a path is invalid or inaccessible."""

    def __init__(self, path: str, reason: str):
        """Initialize invalid path error.

        Args:
            path: Invalid path
            reason: Why the path is invalid
        """
        message = f"Invalid path: {path}\nReason: {reason}"
        super().__init__(message)


class DirectoryExpectedError(CLIError):
    """Raised when a file is provided instead of a directory."""

    def __init__(self, path: str):
        """Initialize directory expected error.

        Args:
            path: Path to the file
        """
        message = f"Expected a directory, got file: {path}"
        hint = "Check that you're pointing to a directory, not a file"
        super().__init__(message, hint)


class FileExpectedError(CLIError):
    """Raised when a directory is provided instead of a file."""

    def __init__(self, path: str):
        """Initialize file expected error.

        Args:
            path: Path to the directory
        """
        message = f"Expected a file, got directory: {path}"
        hint = "Check that you're pointing to a file, not a directory"
        super().__init__(message, hint)


class InvalidFormatError(CLIError):
    """Raised when an invalid format is specified."""

    def __init__(self, format_name: str, valid_formats: list[str]):
        """Initialize invalid format error.

        Args:
            format_name: Name of the invalid format
            valid_formats: List of valid format names
        """
        formats_str = ", ".join(valid_formats)
        message = f"Invalid format: {format_name}"
        hint = f"Valid formats are: {formats_str}"
        super().__init__(message, hint)


class PermissionDeniedError(CLIError):
    """Raised when permission is denied for a file/directory."""

    def __init__(self, path: str, operation: str = "access"):
        """Initialize permission denied error.

        Args:
            path: Path with permission issue
            operation: Operation that failed ("read", "write", "execute")
        """
        message = f"Permission denied: cannot {operation} {path}"
        hint = "Check file permissions and try again"
        super().__init__(message, hint)


class InvalidArgumentError(CLIError):
    """Raised when an argument value is invalid."""

    def __init__(self, arg_name: str, value: str, reason: str):
        """Initialize invalid argument error.

        Args:
            arg_name: Name of the argument
            value: Invalid value
            reason: Why the value is invalid
        """
        message = f"Invalid value for {arg_name}: {value}"
        hint = f"Expected: {reason}"
        super().__init__(message, hint)


class EmptyDirectoryError(CLIError):
    """Raised when an empty directory is not allowed."""

    def __init__(self, path: str, what: str = "exports"):
        """Initialize empty directory error.

        Args:
            path: Path to the empty directory
            what: What was expected to find ("exports", "files")
        """
        message = f"No {what} found in: {path}"
        hint = "Check that the directory contains the expected files"
        super().__init__(message, hint)


class ConversionError(CLIError):
    """Raised when format conversion fails."""

    def __init__(self, input_path: str, output_format: str, reason: str):
        """Initialize conversion error.

        Args:
            input_path: Path to file being converted
            output_format: Target format
            reason: Why conversion failed
        """
        message = f"Failed to convert {input_path} to {output_format}"
        hint = f"Reason: {reason}"
        super().__init__(message, hint)


class OutputError(CLIError):
    """Raised when output cannot be written."""

    def __init__(self, output_path: str, reason: str):
        """Initialize output error.

        Args:
            output_path: Path where output should be written
            reason: Why output failed
        """
        message = f"Cannot write output to: {output_path}"
        hint = f"Reason: {reason}"
        super().__init__(message, hint)


class ArchiveError(CLIError):
    """Raised when archive creation/reading fails."""

    def __init__(self, archive_path: str, operation: str, reason: str):
        """Initialize archive error.

        Args:
            archive_path: Path to archive
            operation: Operation that failed ("create", "read", "extract")
            reason: Why it failed
        """
        message = f"Failed to {operation} archive: {archive_path}"
        hint = f"Reason: {reason}"
        super().__init__(message, hint)


class ConfigurationError(CLIError):
    """Raised when configuration is invalid."""

    def __init__(self, config_name: str, reason: str):
        """Initialize configuration error.

        Args:
            config_name: Name of invalid configuration
            reason: Why it's invalid
        """
        message = f"Invalid configuration: {config_name}"
        hint = f"Reason: {reason}"
        super().__init__(message, hint)


class DiskSpaceError(CLIError):
    """Raised when there's not enough disk space."""

    def __init__(self, path: str, needed_mb: int):
        """Initialize disk space error.

        Args:
            path: Path where disk space is needed
            needed_mb: Megabytes needed
        """
        message = f"Not enough disk space for operation in {path}"
        hint = f"Free up at least {needed_mb}MB and try again"
        super().__init__(message, hint)


class TimeoutError_(CLIError):
    """Raised when an operation times out."""

    def __init__(self, operation: str, timeout_seconds: int):
        """Initialize timeout error.

        Args:
            operation: Operation that timed out
            timeout_seconds: Timeout duration
        """
        message = f"Operation timed out: {operation}"
        hint = f"Increase timeout (currently {timeout_seconds}s) or try with smaller input"
        super().__init__(message, hint)


class ConcurrencyError(CLIError):
    """Raised when concurrent operations interfere."""

    def __init__(self, resource: str, action: str = "access"):
        """Initialize concurrency error.

        Args:
            resource: Resource with conflict
            action: Action that conflicted
        """
        message = f"Cannot {action} {resource}: it's being used by another process"
        hint = "Wait for the other operation to complete and try again"
        super().__init__(message, hint)


class MavenBuildFailedError(CLIError):
    """Raised when Maven build fails."""

    def __init__(self, exit_code: int, reason: str = "Unknown"):
        """Initialize Maven build failed error.

        Args:
            exit_code: Maven exit code
            reason: Brief explanation of failure
        """
        message = f"Maven build failed with exit code {exit_code}"
        hint = f"Reason: {reason}\nRun with --verbose to see full output"
        super().__init__(message, hint)


class MavenNotFoundError(CLIError):
    """Raised when Maven/mvnd not found in PATH."""

    def __init__(self):
        """Initialize Maven not found error."""
        message = "Maven (mvnd or mvn) not found in PATH"
        hint = (
            "Install Maven from https://maven.apache.org/ or "
            "mvnd from https://maven.apache.org/mvnd/"
        )
        super().__init__(message, hint)


class PomNotFoundError(CLIError):
    """Raised when pom.xml not found."""

    def __init__(self, path: str):
        """Initialize pom.xml not found error.

        Args:
            path: Directory where pom.xml was expected
        """
        message = f"pom.xml not found in {path}"
        hint = "Make sure you're in the root directory of a Maven project"
        super().__init__(message, hint)


class LatexCompilationError(CLIError):
    """Raised when LaTeX compilation fails."""

    def __init__(self, compiler: str, exit_code: int, details: str | None = None):
        """Initialize LaTeX compilation error.

        Args:
            compiler: Name of the LaTeX compiler that failed
            exit_code: Exit code from compiler
            details: Optional compilation error details/log excerpt
        """
        message = f"LaTeX compilation failed with {compiler} (exit code {exit_code})"
        hint = "Check your LaTeX syntax or ensure texlive/miktex is installed"
        super().__init__(message, hint)
        self.details = details


class NoLatexCompilerError(CLIError):
    """Raised when no LaTeX compiler is found in PATH."""

    def __init__(self):
        """Initialize no LaTeX compiler error."""
        message = "No LaTeX compiler found in PATH"
        hint = (
            "Install texlive: apt-get install texlive-latex-base\n"
            "Or install miktex: https://miktex.org/download"
        )
        super().__init__(message, hint)


class LatexTemplateMissingError(CLIError):
    """Raised for invalid LaTeX template selection."""

    def __init__(self, template_name: str, valid_templates: list[str]):
        """Initialize LaTeX template missing error.

        Args:
            template_name: Name of the invalid template
            valid_templates: List of valid template names
        """
        templates_str = ", ".join(valid_templates)
        message = f"Unknown LaTeX template: {template_name}"
        hint = f"Valid templates are: {templates_str}"
        super().__init__(message, hint)


class MissingGPGKeyError(CLIError):
    """Raised when GPG key is not found."""

    def __init__(self, key_id: str | None = None):
        """Initialize missing GPG key error.

        Args:
            key_id: ID of the missing GPG key (optional)
        """
        if key_id:
            message = f"GPG key not found: {key_id}"
            hint = f"Import or create GPG key: gpg --import <keyfile>\nOr use: gpg --gen-key"
        else:
            message = "No GPG key found"
            hint = "Set up GPG: gpg --gen-key\nExport public key: gpg --keyserver keyserver.ubuntu.com --send-keys <KEY_ID>"
        super().__init__(message, hint)


class InvalidCredentialsError(CLIError):
    """Raised when OSSRH credentials are missing or invalid."""

    def __init__(self, credential_type: str = "OSSRH"):
        """Initialize invalid credentials error.

        Args:
            credential_type: Type of credential (OSSRH, GPG, etc.)
        """
        message = f"{credential_type} credentials not configured"
        hint = (
            f"Configure {credential_type} credentials in ~/.m2/settings.xml\n"
            "Or set environment variables: OSSRH_USERNAME, OSSRH_PASSWORD"
        )
        super().__init__(message, hint)


class InvalidPOMError(CLIError):
    """Raised when pom.xml is invalid for Maven Central publishing."""

    def __init__(self, missing_elements: list[str] | str, requirement: str = ""):
        """Initialize invalid POM error.

        Args:
            missing_elements: List of missing POM elements or error message
            requirement: What's required to fix it (optional)
        """
        if isinstance(missing_elements, list):
            elements_str = ", ".join(missing_elements)
            message = f"Invalid pom.xml: Missing required elements: {elements_str}"
            hint = (
                f"Add missing elements to pom.xml\n"
                f"See PUBLISHING.md for complete pom.xml structure"
            )
        else:
            message = f"Invalid pom.xml: {missing_elements}"
            hint = f"Required for Maven Central: {requirement}" if requirement else ""
        super().__init__(message, hint)


class PublishValidationError(CLIError):
    """Raised when pre-flight publish validation fails."""

    def __init__(self, failures: list[str]):
        """Initialize publish validation error.

        Args:
            failures: List of validation failures
        """
        failures_str = "\n".join(f"  - {f}" for f in failures)
        message = f"Pre-flight validation failed:\n{failures_str}"
        hint = "Run 'dtr publish check' to validate your environment before publishing"
        super().__init__(message, hint)


class MavenCentralNotFoundError(CLIError):
    """Raised when artifact is not found on Maven Central after publishing."""

    def __init__(self, artifact: str, version: str = ""):
        """Initialize Maven Central not found error.

        Args:
            artifact: Artifact coordinates
            version: Version of the artifact
        """
        ver_str = f" (v{version})" if version else ""
        message = f"Artifact not found on Maven Central: {artifact}{ver_str}"
        hint = "Artifact may still be syncing. Wait 15-30 minutes and try again."
        super().__init__(message, hint)
